#!/usr/bin/env python3
"""Demonstração ponta a ponta da Eventos API (requisitos 01 a 16).

Por padrão o script empacota a API e o mock-enderecos, sobe os dois como jars (profile prod) em
portas livres e percorre todos os casos de uso: CRUD, regras de negócio, concorrência, métrica,
validação e erros padronizados, serviço externo UP/DOWN com fallback, health, persistência após
reinício e readiness em manutenção. Só usa a biblioteca padrão do Python (3.10+).

Funcionalidades ainda não integradas na branch aparecem como PENDENTE (não como FALHA).

Uso:
    python e2e/demonstracao_e2e.py                     # build + sobe tudo + demonstra
    python e2e/demonstracao_e2e.py --sem-build         # reaproveita os jars de target/
    python e2e/demonstracao_e2e.py --usar-servicos-existentes \
        --api-url http://localhost:8080 --mock-url http://localhost:8081
    python e2e/demonstracao_e2e.py --estrito           # PENDENTE também gera código de saída 1

Saída: relatório em target/e2e/relatorio.md e logs dos processos em target/e2e/*.log.
"""

from __future__ import annotations

import argparse
import contextlib
import json
import os
import re
import shutil
import socket
import subprocess
import sys
import time
import urllib.error
import urllib.request
from collections import Counter
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
SAIDA = RAIZ / "target" / "e2e"
CEP_CONHECIDO = "01310100"
CIDADE_CONHECIDA = "São Paulo"
CEP_DESCONHECIDO = "00000000"
CONTRATO_ERRO = {"codigo", "mensagem", "timestamp", "campos"}
METRICA = re.compile(r"^eventos_inscricoes_total(\{[^}]*\})?\s+(\S+)", re.MULTILINE)


class Falha(Exception):
    """Comportamento observado diferente do esperado."""


class Pendente(Exception):
    """Funcionalidade ainda não integrada ou que exige outro modo de execução."""


class Pulado(Exception):
    """Passo depende de outro que não produziu o dado necessário."""


def esperar(condicao: bool, mensagem: str) -> None:
    if not condicao:
        raise Falha(mensagem)


@dataclass
class Resposta:
    status: int
    headers: dict[str, str]
    texto: str
    ms: int

    def json(self):
        try:
            return json.loads(self.texto)
        except ValueError as erro:
            raise Falha(f"corpo não é JSON (HTTP {self.status}): {self.texto[:120]!r}") from erro

    def json_ou_none(self):
        try:
            return json.loads(self.texto)
        except ValueError:
            return None


class Relatorio:
    CORES = {"OK": "32", "FALHA": "31", "PENDENTE": "33", "PULADO": "90"}

    def __init__(self, verboso: bool):
        self.verboso = verboso
        self.itens: list[tuple[str, str, str, str]] = []
        self.secao_atual = ""
        self.cor = sys.stdout.isatty()
        if self.cor and os.name == "nt":
            os.system("")  # habilita sequências ANSI no console do Windows

    def _c(self, codigo: str, texto: str) -> str:
        return f"\033[{codigo}m{texto}\033[0m" if self.cor else texto

    def secao(self, titulo: str, requisitos: str) -> None:
        self.secao_atual = f"{titulo} (Req {requisitos})"
        print()
        print(self._c("1;36", f"== {self.secao_atual} =="))

    def info(self, texto: str) -> None:
        print(f"      {texto}")

    def http(self, metodo: str, url: str, resposta: Resposta, mostrar_corpo: bool) -> None:
        caminho = re.sub(r"^https?://[^/]+", "", url)
        print(self._c("90", f"      {metodo:6} {caminho} -> {resposta.status} ({resposta.ms} ms)"))
        if not mostrar_corpo or not resposta.texto:
            return
        corpo = resposta.json_ou_none()
        if corpo is None:
            texto = resposta.texto.strip()
        elif self.verboso:
            texto = json.dumps(corpo, ensure_ascii=False, indent=2)
        else:
            texto = json.dumps(corpo, ensure_ascii=False)
        if not self.verboso and len(texto) > 220:
            texto = texto[:217] + "..."
        for linha in texto.splitlines():
            print(self._c("90", f"        {linha}"))

    @contextlib.contextmanager
    def passo(self, nome: str):
        print(f"  > {nome}")
        status, detalhe = "OK", ""
        try:
            yield
        except Falha as erro:
            status, detalhe = "FALHA", str(erro)
        except Pendente as erro:
            status, detalhe = "PENDENTE", str(erro)
        except Pulado as erro:
            status, detalhe = "PULADO", str(erro)
        except (urllib.error.URLError, OSError) as erro:
            status, detalhe = "FALHA", f"erro de conexão: {erro}"
        except Exception as erro:  # noqa: BLE001 - o relatório precisa registrar qualquer erro
            status, detalhe = "FALHA", f"{type(erro).__name__}: {erro}"
        self.itens.append((self.secao_atual, nome, status, detalhe))
        sufixo = f" - {detalhe}" if detalhe else ""
        print(f"    {self._c(self.CORES[status], '[' + status + ']')} {nome}{sufixo}")

    def contagem(self) -> Counter:
        return Counter(status for _, _, status, _ in self.itens)

    def resumo(self) -> None:
        contagem = self.contagem()
        print()
        print(self._c("1", "== Resumo =="))
        for status in ("OK", "FALHA", "PENDENTE", "PULADO"):
            print(f"  {status:<9} {contagem.get(status, 0)}")
        for secao, nome, status, detalhe in self.itens:
            if status != "OK":
                print(f"  - [{status}] {secao} / {nome}: {detalhe}")

    def salvar(self, destino: Path, modo: str) -> None:
        destino.parent.mkdir(parents=True, exist_ok=True)
        contagem = self.contagem()
        linhas = [
            "# Relatório da demonstração ponta a ponta",
            "",
            f"- Execução: {datetime.now().isoformat(timespec='seconds')}",
            f"- Modo: {modo}",
            "- Resultado: "
            + ", ".join(f"{s} {contagem.get(s, 0)}" for s in ("OK", "FALHA", "PENDENTE", "PULADO")),
            "",
            "| Seção | Passo | Resultado | Detalhe |",
            "|-------|-------|-----------|---------|",
        ]
        for secao, nome, status, detalhe in self.itens:
            detalhe_md = detalhe.replace("|", "\\|").replace("\n", " ")
            linhas.append(f"| {secao} | {nome} | {status} | {detalhe_md} |")
        destino.write_text("\n".join(linhas) + "\n", encoding="utf-8")


class Cliente:
    def __init__(self, relatorio: Relatorio, timeout: float = 30):
        self.relatorio = relatorio
        self.timeout = timeout
        # Ignora proxies do ambiente: as chamadas são sempre para a máquina local.
        self._opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))

    def chamar(
        self,
        metodo: str,
        url: str,
        corpo=None,
        tipo: str = "application/json",
        silencioso: bool = False,
        mostrar_corpo: bool = True,
    ) -> Resposta:
        dados = None
        headers = {"Accept": "*/*"}
        if corpo is not None:
            texto = corpo if isinstance(corpo, str) else json.dumps(corpo, ensure_ascii=False)
            dados = texto.encode("utf-8")
            headers["Content-Type"] = tipo
        requisicao = urllib.request.Request(url, data=dados, method=metodo, headers=headers)
        inicio = time.monotonic()
        try:
            with self._opener.open(requisicao, timeout=self.timeout) as resposta:
                status, cabecalhos, bruto = resposta.status, resposta.headers, resposta.read()
        except urllib.error.HTTPError as erro:
            status, cabecalhos, bruto = erro.code, erro.headers, erro.read()
        resultado = Resposta(
            status,
            {chave.lower(): valor for chave, valor in cabecalhos.items()},
            bruto.decode("utf-8", "replace"),
            int((time.monotonic() - inicio) * 1000),
        )
        if not silencioso:
            self.relatorio.http(metodo, url, resultado, mostrar_corpo)
        return resultado


class Servico:
    """Processo java -jar com log em arquivo e espera ativa até responder HTTP."""

    def __init__(self, nome: str, comando: list[str], log: Path):
        self.nome = nome
        self.comando = comando
        self.log = log
        self.processo: subprocess.Popen | None = None
        self._inicio_log = 0
        self._arquivo = None

    def iniciar(self, env_extra: dict[str, str], url_pronto: str, timeout: float) -> None:
        self.log.parent.mkdir(parents=True, exist_ok=True)
        self._arquivo = open(self.log, "ab")
        self._inicio_log = self._arquivo.tell()
        cabecalho = f"\n===== {datetime.now().isoformat()} {self.nome} env={env_extra} =====\n"
        self._arquivo.write(cabecalho.encode("utf-8"))
        self._arquivo.flush()
        env = os.environ.copy()
        env.update(env_extra)
        self.processo = subprocess.Popen(
            self.comando, cwd=SAIDA, env=env, stdout=self._arquivo, stderr=subprocess.STDOUT
        )
        opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
        limite = time.monotonic() + timeout
        while time.monotonic() < limite:
            if self.processo.poll() is not None:
                self.parar()
                raise RuntimeError(f"{self.nome} encerrou ao iniciar; veja {self.log}")
            try:
                opener.open(url_pronto, timeout=2).close()
                return
            except urllib.error.HTTPError:
                return  # qualquer resposta HTTP indica que o servidor está no ar
            except (urllib.error.URLError, OSError):
                time.sleep(0.5)
        self.parar()
        raise RuntimeError(f"{self.nome} não respondeu em {timeout}s; veja {self.log}")

    def parar(self) -> None:
        if self.processo and self.processo.poll() is None:
            self.processo.terminate()
            try:
                self.processo.wait(timeout=20)
            except subprocess.TimeoutExpired:
                self.processo.kill()
                self.processo.wait()
        self.processo = None
        if self._arquivo:
            self._arquivo.close()
            self._arquivo = None

    def log_desde_inicio(self) -> str:
        with open(self.log, "rb") as arquivo:
            arquivo.seek(self._inicio_log)
            return arquivo.read().decode("utf-8", "replace")


@dataclass
class Contexto:
    args: argparse.Namespace
    relatorio: Relatorio
    cliente: Cliente
    api: str
    mock: str
    iniciou_servicos: bool
    api_srv: Servico | None = None
    mock_srv: Servico | None = None
    api_env: dict[str, str] = field(default_factory=dict)
    mock_env: dict[str, str] = field(default_factory=dict)
    crud: bool = False
    ids: dict[str, int] = field(default_factory=dict)
    criados: list[int] = field(default_factory=list)
    inscricoes_ok: int = 0

    def req(self, metodo: str, caminho: str, corpo=None, **kwargs) -> Resposta:
        return self.cliente.chamar(metodo, self.api + caminho, corpo, **kwargs)

    def requer_crud(self) -> None:
        if not self.crud:
            raise Pendente("EventoResource não integrado nesta branch (GET /eventos sem 200)")

    def requer_id(self, chave: str) -> int:
        if chave not in self.ids:
            raise Pulado(f"evento '{chave}' não foi criado em passo anterior")
        return self.ids[chave]

    def requer_servicos(self) -> None:
        if not self.iniciou_servicos:
            raise Pendente("exige que o script inicie os serviços (sem --usar-servicos-existentes)")


def novo_evento(nome: str, cep: str = CEP_CONHECIDO, capacidade: int = 2) -> dict:
    data = (datetime.now() + timedelta(days=30)).replace(microsecond=0)
    return {
        "nome": nome,
        "descricao": "Criado pela demonstração e2e",
        "cep": cep,
        "dataHora": data.isoformat(),
        "capacidade": capacidade,
    }


def verificar_erro(resposta: Resposta, status: int, codigo: str | None = None) -> dict:
    esperar(resposta.status == status, f"esperado HTTP {status}, obtido {resposta.status}")
    esperar(
        "application/json" in resposta.headers.get("content-type", ""),
        f"Content-Type do erro não é JSON: {resposta.headers.get('content-type')}",
    )
    corpo = resposta.json()
    esperar(isinstance(corpo, dict), "corpo de erro não é objeto JSON")
    esperar(set(corpo) == CONTRATO_ERRO, f"chaves do erro {sorted(corpo)} != {sorted(CONTRATO_ERRO)}")
    if codigo:
        esperar(corpo["codigo"] == codigo, f"codigo {corpo['codigo']!r} != {codigo!r}")
    esperar(isinstance(corpo["mensagem"], str) and corpo["mensagem"], "mensagem vazia")
    esperar(isinstance(corpo["campos"], list), "campos não é lista")
    try:
        datetime.fromisoformat(corpo["timestamp"])
    except (TypeError, ValueError) as erro:
        raise Falha(f"timestamp inválido: {corpo['timestamp']!r}") from erro
    return corpo


def criar(ctx: Contexto, chave: str, corpo: dict) -> dict:
    resposta = ctx.req("POST", "/eventos", corpo)
    esperar(resposta.status == 201, f"POST /eventos esperado 201, obtido {resposta.status}")
    evento = resposta.json()
    ctx.ids[chave] = evento["id"]
    ctx.criados.append(evento["id"])
    return evento


def ler_contador(ctx: Contexto) -> float | None:
    resposta = ctx.req("GET", "/q/metrics", mostrar_corpo=False)
    esperar(resposta.status == 200, f"/q/metrics retornou {resposta.status}")
    valores = [float(m.group(2)) for m in METRICA.finditer(resposta.texto)]
    return sum(valores) if valores else None


def consultar_endereco(ctx: Contexto, chave: str) -> Resposta:
    evento_id = ctx.requer_id(chave)
    resposta = ctx.req("GET", f"/eventos/{evento_id}/endereco")
    if resposta.status == 404:
        corpo = resposta.json_ou_none() or {}
        if corpo.get("codigo") != "EVENTO_NAO_ENCONTRADO":
            raise Pendente("GET /eventos/{id}/endereco não integrado nesta branch")
    return resposta


def verificar_fallback(resposta: Resposta, cep: str) -> None:
    esperar(resposta.status == 200, f"esperada resposta controlada 200, obtido {resposta.status}")
    corpo = resposta.json()
    esperar(corpo.get("cep") == cep, f"fallback deveria preservar o CEP {cep}")
    esperar(corpo.get("cidade") != CIDADE_CONHECIDA, "resposta veio do serviço real, não do fallback")


# --------------------------------------------------------------------------- cenários


def cenario_plataforma(ctx: Contexto) -> None:
    rel = ctx.relatorio
    rel.secao("Profile, configuração e plataforma", "04, 07")
    with rel.passo("API sobe no profile prod com configuração por variável de ambiente"):
        ctx.requer_servicos()
        log = ctx.api_srv.log_desde_inicio()
        esperar("Profile prod activated" in log, "log não mostra 'Profile prod activated'")
        rel.info("log: 'Profile prod activated'")
        rel.info(f"QUARKUS_HTTP_PORT={ctx.api_env['QUARKUS_HTTP_PORT']} atendeu em {ctx.api}")
        rel.info(f"QUARKUS_DATASOURCE_JDBC_URL={ctx.api_env['QUARKUS_DATASOURCE_JDBC_URL']}")
    with rel.passo("Contrato OpenAPI publicado"):
        resposta = ctx.req("GET", "/q/openapi?format=json", mostrar_corpo=False)
        esperar(resposta.status == 200, f"/q/openapi retornou {resposta.status}")
        caminhos = sorted(resposta.json().get("paths", {}))
        rel.info(f"rotas documentadas: {', '.join(caminhos) or '(nenhuma)'}")
    with rel.passo("Detecção do CRUD de eventos"):
        resposta = ctx.req("GET", "/eventos")
        ctx.crud = resposta.status == 200 and isinstance(resposta.json_ou_none(), list)
        if not ctx.crud:
            raise Pendente(f"GET /eventos retornou {resposta.status}; cenários de CRUD pendentes")


def cenario_health(ctx: Contexto) -> None:
    rel = ctx.relatorio
    rel.secao("Health checks", "08")
    for nome, caminho in (("Liveness", "/q/health/live"), ("Readiness", "/q/health/ready")):
        with rel.passo(f"{nome} UP"):
            resposta = ctx.req("GET", caminho)
            esperar(resposta.status == 200, f"{caminho} retornou {resposta.status}")
            corpo = resposta.json()
            esperar(corpo.get("status") == "UP", f"status {corpo.get('status')}")
            checks = [c.get("name") for c in corpo.get("checks", [])]
            rel.info(f"checks: {', '.join(checks) or '(nenhum)'}")
            if nome == "Liveness" and not checks:
                raise Pendente("UP agregado, mas sem @Liveness próprio registrado")


def cenario_crud(ctx: Contexto) -> None:
    rel = ctx.relatorio
    rel.secao("CRUD de eventos", "01, 02, 04, 12, 13")
    with rel.passo("POST /eventos cria evento: 201 + Location"):
        ctx.requer_crud()
        corpo = novo_evento("Evento e2e")
        resposta = ctx.req("POST", "/eventos", corpo)
        esperar(resposta.status == 201, f"esperado 201, obtido {resposta.status}")
        evento = resposta.json()
        ctx.ids["principal"] = evento["id"]
        ctx.criados.append(evento["id"])
        location = resposta.headers.get("location", "")
        esperar(location.endswith(f"/eventos/{evento['id']}"), f"Location inesperado: {location!r}")
        esperar(evento["inscritos"] == 0, "evento novo deveria ter 0 inscritos")
        esperar(evento["vagasDisponiveis"] == corpo["capacidade"], "vagas != capacidade")
        rel.info(f"Location: {location}")
    with rel.passo("GET no id criado devolve o evento"):
        evento_id = ctx.requer_id("principal")
        resposta = ctx.req("GET", f"/eventos/{evento_id}")
        esperar(resposta.status == 200, f"esperado 200, obtido {resposta.status}")
        esperar(resposta.json()["nome"] == "Evento e2e", "nome diferente do enviado")
    with rel.passo("GET /eventos lista o evento criado"):
        evento_id = ctx.requer_id("principal")
        resposta = ctx.req("GET", "/eventos", mostrar_corpo=False)
        ids = [e["id"] for e in resposta.json()]
        rel.info(f"{len(ids)} evento(s) na listagem")
        esperar(evento_id in ids, "evento criado ausente da listagem")
    with rel.passo("PUT altera o evento e o GET seguinte mostra o dado persistido"):
        evento_id = ctx.requer_id("principal")
        alterado = novo_evento("Evento e2e atualizado", capacidade=10)
        alterado["descricao"] = "Alterado pelo PUT"
        resposta = ctx.req("PUT", f"/eventos/{evento_id}", alterado)
        esperar(resposta.status == 200, f"PUT esperado 200, obtido {resposta.status}")
        consulta = ctx.req("GET", f"/eventos/{evento_id}").json()
        esperar(consulta["nome"] == "Evento e2e atualizado", "GET não reflete o nome do PUT")
        esperar(consulta["capacidade"] == 10, "GET não reflete a capacidade do PUT")
        esperar(consulta["descricao"] == "Alterado pelo PUT", "GET não reflete a descrição")


def cenario_regras(ctx: Contexto) -> None:
    rel = ctx.relatorio
    rel.secao("Regras de negócio, concorrência e métrica", "05, 10, 13")
    contador_inicial: float | None = None
    with rel.passo("Lê eventos_inscricoes_total antes das inscrições"):
        ctx.requer_crud()
        contador_inicial = ler_contador(ctx) or 0.0
        rel.info(f"contador inicial: {contador_inicial:g} (ausente conta como 0)")
    with rel.passo("Duas inscrições ocupam as duas vagas"):
        ctx.requer_crud()
        criar(ctx, "lotacao", novo_evento("Evento e2e lotação", capacidade=2))
        evento_id = ctx.ids["lotacao"]
        for esperado in (1, 2):
            resposta = ctx.req("POST", f"/eventos/{evento_id}/inscricoes")
            esperar(resposta.status == 200, f"inscrição {esperado}: HTTP {resposta.status}")
            corpo = resposta.json()
            esperar(corpo["inscritos"] == esperado, f"inscritos {corpo['inscritos']} != {esperado}")
            esperar(corpo["vagasDisponiveis"] == 2 - esperado, "vagas disponíveis incorretas")
            ctx.inscricoes_ok += 1
    with rel.passo("Terceira inscrição recusada: 409 EVENTO_LOTADO"):
        evento_id = ctx.requer_id("lotacao")
        verificar_erro(ctx.req("POST", f"/eventos/{evento_id}/inscricoes"), 409, "EVENTO_LOTADO")
    with rel.passo("PUT não reduz capacidade abaixo dos inscritos: 409"):
        evento_id = ctx.requer_id("lotacao")
        corpo = novo_evento("Evento e2e lotação", capacidade=1)
        resposta = ctx.req("PUT", f"/eventos/{evento_id}", corpo)
        verificar_erro(resposta, 409, "CAPACIDADE_ABAIXO_DOS_INSCRITOS")
    with rel.passo("Recusas não alteram o estado (transação sem efeito parcial)"):
        evento_id = ctx.requer_id("lotacao")
        corpo = ctx.req("GET", f"/eventos/{evento_id}").json()
        esperar(corpo["inscritos"] == 2 and corpo["capacidade"] == 2, f"estado alterado: {corpo}")
    with rel.passo("20 inscrições simultâneas em 5 vagas: exatamente 5 aceitas"):
        ctx.requer_crud()
        criar(ctx, "concorrencia", novo_evento("Evento e2e concorrência", capacidade=5))
        url = f"{ctx.api}/eventos/{ctx.ids['concorrencia']}/inscricoes"
        with ThreadPoolExecutor(max_workers=20) as executor:
            status = list(
                executor.map(
                    lambda _: ctx.cliente.chamar("POST", url, silencioso=True).status, range(20)
                )
            )
        contagem = Counter(status)
        rel.info(f"respostas: {dict(contagem)}")
        ctx.inscricoes_ok += contagem.get(200, 0)
        esperar(contagem == Counter({200: 5, 409: 15}), "esperado 5x200 e 15x409")
        final = ctx.req("GET", f"/eventos/{ctx.ids['concorrencia']}").json()
        esperar(final["inscritos"] == 5, f"inscritos finais {final['inscritos']} != 5")
    with rel.passo("Métrica conta só as inscrições aceitas"):
        ctx.requer_crud()
        if contador_inicial is None:
            raise Pulado("contador inicial não foi lido")
        atual = ler_contador(ctx)
        if atual is None:
            raise Pendente("eventos_inscricoes_total não exposto em /q/metrics")
        rel.info(f"contador: {contador_inicial:g} -> {atual:g} ({ctx.inscricoes_ok} aceitas)")
        esperar(atual - contador_inicial == ctx.inscricoes_ok, "variação diferente das aceitas")


def cenario_erros(ctx: Contexto) -> None:
    rel = ctx.relatorio
    rel.secao("Validação e erros padronizados", "02, 14, 15, 16")
    with rel.passo("POST inválido: 400 com cada campo violado"):
        ctx.requer_crud()
        invalido = {
            "nome": " ",
            "descricao": "x" * 501,
            "cep": "01310-100",
            "dataHora": (datetime.now() - timedelta(days=1)).replace(microsecond=0).isoformat(),
            "capacidade": 0,
        }
        corpo = verificar_erro(ctx.req("POST", "/eventos", invalido), 400, "REQUISICAO_INVALIDA")
        for campo in ("nome", "descricao", "cep", "dataHora", "capacidade"):
            esperar(
                any(v.startswith(campo + ": ") for v in corpo["campos"]), f"campo {campo} ausente"
            )
    with rel.passo("PUT sem campos obrigatórios: 400 com 4 violações"):
        ctx.requer_crud()
        evento_id = ctx.ids.get("principal", 1)
        resposta = ctx.req("PUT", f"/eventos/{evento_id}", {})
        corpo = verificar_erro(resposta, 400, "REQUISICAO_INVALIDA")
        esperar(len(corpo["campos"]) == 4, f"esperadas 4 violações, obtidas {corpo['campos']}")
    with rel.passo("JSON malformado: 400 no mesmo contrato"):
        ctx.requer_crud()
        verificar_erro(ctx.req("POST", "/eventos", '{"nome":'), 400, "REQUISICAO_INVALIDA")
    with rel.passo("Tipo incompatível (capacidade texto): 400"):
        ctx.requer_crud()
        corpo = novo_evento("Tipo errado")
        corpo["capacidade"] = "abc"
        verificar_erro(ctx.req("POST", "/eventos", corpo), 400, "REQUISICAO_INVALIDA")
    with rel.passo("Content-Type não suportado: 415"):
        ctx.requer_crud()
        resposta = ctx.req("POST", "/eventos", "texto", tipo="text/plain")
        verificar_erro(resposta, 415, "HTTP_415")
    with rel.passo("Método não permitido: 405"):
        ctx.requer_crud()
        verificar_erro(ctx.req("DELETE", "/eventos"), 405, "HTTP_405")
    with rel.passo("Evento inexistente: 404 EVENTO_NAO_ENCONTRADO"):
        ctx.requer_crud()
        verificar_erro(ctx.req("GET", "/eventos/999999999"), 404, "EVENTO_NAO_ENCONTRADO")
    with rel.passo("Inscrição em evento inexistente: 404"):
        ctx.requer_crud()
        resposta = ctx.req("POST", "/eventos/999999999/inscricoes")
        verificar_erro(resposta, 404, "EVENTO_NAO_ENCONTRADO")
    with rel.passo("Rota inexistente: 404 no mesmo contrato"):
        verificar_erro(ctx.req("GET", "/rota-inexistente"), 404, "HTTP_404")


def cenario_externo_up(ctx: Contexto) -> None:
    rel = ctx.relatorio
    rel.secao("Serviço externo no ar", "06")
    with rel.passo("Mock responde CEP conhecido"):
        resposta = ctx.cliente.chamar("GET", f"{ctx.mock}/enderecos/{CEP_CONHECIDO}")
        esperar(resposta.status == 200, f"mock retornou {resposta.status}")
        esperar(resposta.json()["cidade"] == CIDADE_CONHECIDA, "cidade inesperada no mock")
    with rel.passo("Mock responde 404 para CEP desconhecido"):
        resposta = ctx.cliente.chamar("GET", f"{ctx.mock}/enderecos/{CEP_DESCONHECIDO}")
        esperar(resposta.status == 404, f"mock retornou {resposta.status}")
    with rel.passo("API busca o endereço do evento via REST Client"):
        ctx.requer_crud()
        resposta = consultar_endereco(ctx, "principal")
        esperar(resposta.status == 200, f"esperado 200, obtido {resposta.status}")
        corpo = resposta.json()
        esperar(corpo.get("cidade") == CIDADE_CONHECIDA, f"cidade {corpo.get('cidade')!r}")
    with rel.passo("CEP desconhecido no externo: resposta controlada, sem 5xx"):
        ctx.requer_crud()
        criar(ctx, "cep_desconhecido", novo_evento("Evento e2e CEP desconhecido", CEP_DESCONHECIDO))
        resposta = consultar_endereco(ctx, "cep_desconhecido")
        esperar(resposta.status < 500, f"API devolveu {resposta.status} ao receber 404 do externo")


def cenario_externo_down(ctx: Contexto) -> None:
    rel = ctx.relatorio
    rel.secao("Serviço externo fora do ar: retry, timeout e fallback", "09")
    with rel.passo("Mock parado: conexão recusada"):
        ctx.requer_servicos()
        ctx.mock_srv.parar()
        try:
            ctx.cliente.chamar("GET", f"{ctx.mock}/enderecos/{CEP_CONHECIDO}", silencioso=True)
        except (urllib.error.URLError, OSError) as erro:
            rel.info(f"conexão ao mock falhou como esperado: {erro}")
        else:
            raise Falha("mock ainda responde depois de parado")
    with rel.passo("API responde com fallback com o mock parado"):
        ctx.requer_servicos()
        ctx.requer_crud()
        resposta = consultar_endereco(ctx, "principal")
        verificar_fallback(resposta, CEP_CONHECIDO)
        rel.info(f"resposta controlada em {resposta.ms} ms (inclui as novas tentativas)")
    with rel.passo("Mock com MOCK_FALHAR=true responde 503"):
        ctx.requer_servicos()
        reiniciar_mock(ctx, falhar=True)
        resposta = ctx.cliente.chamar("GET", f"{ctx.mock}/enderecos/{CEP_CONHECIDO}")
        esperar(resposta.status == 503, f"esperado 503, obtido {resposta.status}")
    with rel.passo("API responde com fallback quando o externo devolve 503"):
        ctx.requer_servicos()
        ctx.requer_crud()
        verificar_fallback(consultar_endereco(ctx, "principal"), CEP_CONHECIDO)
    with rel.passo("Mock restabelecido: API volta a usar o serviço real"):
        ctx.requer_servicos()
        reiniciar_mock(ctx, falhar=False)
        ctx.requer_crud()
        resposta = consultar_endereco(ctx, "principal")
        esperar(resposta.status == 200, f"esperado 200, obtido {resposta.status}")
        esperar(resposta.json().get("cidade") == CIDADE_CONHECIDA, "não voltou ao serviço real")


def cenario_reinicio(ctx: Contexto) -> None:
    rel = ctx.relatorio
    rel.secao("Reinício em prod: persistência e readiness em manutenção", "07, 08, 13")
    with rel.passo("Reinicia a API com o mesmo banco e EVENTOS_MANUTENCAO=true"):
        ctx.requer_servicos()
        time.sleep(2)  # dá tempo ao H2 de gravar as últimas transações em disco
        ctx.api_srv.parar()
        ctx.api_env["EVENTOS_MANUTENCAO"] = "true"
        iniciar_api(ctx)
        esperar("Profile prod activated" in ctx.api_srv.log_desde_inicio(), "profile não é prod")
    with rel.passo("Dados criados antes do reinício continuam no banco em arquivo"):
        ctx.requer_servicos()
        ctx.requer_crud()
        principal = ctx.req("GET", f"/eventos/{ctx.requer_id('principal')}")
        esperar(principal.status == 200, f"evento principal sumiu: HTTP {principal.status}")
        esperar(principal.json()["nome"] == "Evento e2e atualizado", "nome do PUT perdido")
        lotacao = ctx.req("GET", f"/eventos/{ctx.requer_id('lotacao')}").json()
        esperar(lotacao["inscritos"] == 2, f"inscrições perdidas: {lotacao['inscritos']}")
    with rel.passo("Readiness DOWN (503) em manutenção"):
        ctx.requer_servicos()
        resposta = ctx.req("GET", "/q/health/ready")
        if resposta.status == 200:
            raise Pendente("EVENTOS_MANUTENCAO=true ignorado: readiness de manutenção não integrado")
        esperar(resposta.status == 503, f"esperado 503, obtido {resposta.status}")
        esperar(resposta.json().get("status") == "DOWN", "status diferente de DOWN")
    with rel.passo("Liveness continua UP em manutenção"):
        ctx.requer_servicos()
        resposta = ctx.req("GET", "/q/health/live")
        esperar(resposta.status == 200 and resposta.json().get("status") == "UP", "liveness caiu")


def cenario_limpeza(ctx: Contexto) -> None:
    rel = ctx.relatorio
    rel.secao("Remoção e limpeza", "01, 02, 12")
    with rel.passo("DELETE responde 204 sem corpo e o GET seguinte responde 404"):
        ctx.requer_crud()
        evento_id = ctx.requer_id("principal")
        resposta = ctx.req("DELETE", f"/eventos/{evento_id}")
        esperar(resposta.status == 204, f"esperado 204, obtido {resposta.status}")
        esperar(resposta.texto == "", "DELETE retornou corpo")
        ctx.criados.remove(evento_id)
        verificar_erro(ctx.req("GET", f"/eventos/{evento_id}"), 404, "EVENTO_NAO_ENCONTRADO")
    with rel.passo("DELETE repetido responde 404"):
        ctx.requer_crud()
        evento_id = ctx.requer_id("principal")
        verificar_erro(ctx.req("DELETE", f"/eventos/{evento_id}"), 404, "EVENTO_NAO_ENCONTRADO")
    with rel.passo("Remove os demais eventos criados pela demonstração"):
        ctx.requer_crud()
        for evento_id in list(ctx.criados):
            resposta = ctx.req("DELETE", f"/eventos/{evento_id}", silencioso=True)
            esperar(resposta.status == 204, f"DELETE {evento_id}: HTTP {resposta.status}")
            ctx.criados.remove(evento_id)
        rel.info("nenhum dado da demonstração permanece")


# --------------------------------------------------------------------------- infraestrutura


def porta_livre() -> int:
    with socket.socket() as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


def localizar_java(argumento: str | None) -> str:
    candidatos = [argumento] if argumento else []
    if os.environ.get("JAVA_HOME"):
        executavel = "java.exe" if os.name == "nt" else "java"
        candidatos.append(str(Path(os.environ["JAVA_HOME"]) / "bin" / executavel))
    candidatos.append(shutil.which("java"))
    for candidato in filter(None, candidatos):
        try:
            saida = subprocess.run(
                [candidato, "-version"], capture_output=True, text=True, check=True
            ).stderr
        except (OSError, subprocess.CalledProcessError):
            continue
        versao = re.search(r'version "(\d+)', saida)
        if versao and int(versao.group(1)) >= 21:
            return candidato
    raise SystemExit("JDK 21+ não encontrado. Configure JAVA_HOME ou use --java.")


def localizar_maven(argumento: str | None) -> str:
    if argumento:
        return shutil.which(argumento) or argumento
    instalado = shutil.which("mvn")
    if instalado:
        return instalado
    return str(RAIZ / ("mvnw.cmd" if os.name == "nt" else "mvnw"))


def empacotar(maven: str) -> None:
    for pom in (RAIZ / "pom.xml", RAIZ / "mock-enderecos" / "pom.xml"):
        print(f"Empacotando {pom.relative_to(RAIZ)} ...")
        comando = [maven, "-q", "-DskipTests", "-f", str(pom), "package"]
        if subprocess.run(comando, cwd=RAIZ).returncode != 0:
            raise SystemExit(f"Falha ao empacotar {pom}")


def reiniciar_mock(ctx: Contexto, falhar: bool) -> None:
    ctx.mock_srv.parar()
    ctx.mock_env["MOCK_FALHAR"] = "true" if falhar else "false"
    ctx.mock_srv.iniciar(
        ctx.mock_env, f"{ctx.mock}/enderecos/{CEP_CONHECIDO}", ctx.args.timeout_inicio
    )
    ctx.relatorio.info(f"mock reiniciado com MOCK_FALHAR={ctx.mock_env['MOCK_FALHAR']}")


def iniciar_api(ctx: Contexto) -> None:
    ctx.api_srv.iniciar(ctx.api_env, f"{ctx.api}/q/health/live", ctx.args.timeout_inicio)


def preparar_servicos(ctx: Contexto, java: str) -> Path:
    jars = {
        "api": RAIZ / "target" / "quarkus-app" / "quarkus-run.jar",
        "mock": RAIZ / "mock-enderecos" / "target" / "quarkus-app" / "quarkus-run.jar",
    }
    for nome, jar in jars.items():
        if not jar.exists():
            raise SystemExit(f"Jar da {nome} não encontrado em {jar}; rode sem --sem-build.")
    porta_api, porta_mock = porta_livre(), porta_livre()
    ctx.api, ctx.mock = f"http://127.0.0.1:{porta_api}", f"http://127.0.0.1:{porta_mock}"
    dados = SAIDA / f"dados-{datetime.now():%Y%m%d%H%M%S}"
    dados.mkdir(parents=True)
    ctx.mock_env = {"QUARKUS_HTTP_PORT": str(porta_mock), "MOCK_FALHAR": "false"}
    ctx.api_env = {
        "QUARKUS_HTTP_PORT": str(porta_api),
        "QUARKUS_DATASOURCE_JDBC_URL": f"jdbc:h2:file:{dados.as_posix()}/eventos;DB_CLOSE_DELAY=-1",
        "QUARKUS_REST_CLIENT_ENDERECOS_API_URL": ctx.mock,
        "EVENTOS_MANUTENCAO": "false",
    }
    ctx.mock_srv = Servico("mock-enderecos", [java, "-jar", str(jars["mock"])], SAIDA / "mock.log")
    ctx.api_srv = Servico("eventos-api", [java, "-jar", str(jars["api"])], SAIDA / "api.log")
    print(f"Subindo mock em {ctx.mock} e API em {ctx.api} (profile prod) ...")
    url_mock = f"{ctx.mock}/enderecos/{CEP_CONHECIDO}"
    ctx.mock_srv.iniciar(ctx.mock_env, url_mock, ctx.args.timeout_inicio)
    iniciar_api(ctx)
    return dados


def argumentos() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument(
        "--usar-servicos-existentes",
        action="store_true",
        help="não sobe processos; usa --api-url e --mock-url já em execução",
    )
    parser.add_argument("--api-url", default="http://localhost:8080")
    parser.add_argument("--mock-url", default="http://localhost:8081")
    parser.add_argument("--sem-build", action="store_true", help="reaproveita os jars existentes")
    parser.add_argument("--mvn", help="comando Maven (padrão: mvn do PATH ou o wrapper)")
    parser.add_argument("--java", help="executável java 21+ (padrão: JAVA_HOME ou PATH)")
    parser.add_argument("--timeout-inicio", type=float, default=120)
    parser.add_argument("--verboso", action="store_true", help="mostra corpos JSON completos")
    parser.add_argument("--estrito", action="store_true", help="PENDENTE também falha a execução")
    parser.add_argument(
        "--manter-dados", action="store_true", help="não apaga o banco H2 desta execução"
    )
    return parser.parse_args()


def main() -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    args = argumentos()
    SAIDA.mkdir(parents=True, exist_ok=True)
    relatorio = Relatorio(args.verboso)
    ctx = Contexto(
        args,
        relatorio,
        Cliente(relatorio),
        args.api_url.rstrip("/"),
        args.mock_url.rstrip("/"),
        iniciou_servicos=not args.usar_servicos_existentes,
    )
    dados: Path | None = None
    try:
        if ctx.iniciou_servicos:
            java = localizar_java(args.java)
            if not args.sem_build:
                empacotar(localizar_maven(args.mvn))
            dados = preparar_servicos(ctx, java)
        for cenario in (
            cenario_plataforma,
            cenario_health,
            cenario_crud,
            cenario_regras,
            cenario_erros,
            cenario_externo_up,
            cenario_externo_down,
            cenario_reinicio,
            cenario_limpeza,
        ):
            cenario(ctx)
    except KeyboardInterrupt:
        print("\nInterrompido.")
    finally:
        for servico in (ctx.api_srv, ctx.mock_srv):
            if servico:
                servico.parar()
        if dados and not args.manter_dados:
            shutil.rmtree(dados, ignore_errors=True)
    relatorio.resumo()
    modo = "serviços existentes" if args.usar_servicos_existentes else "jars em profile prod"
    relatorio.salvar(SAIDA / "relatorio.md", modo)
    print(f"\nRelatório: {SAIDA / 'relatorio.md'}  |  logs: {SAIDA}")
    contagem = relatorio.contagem()
    reprovado = contagem.get("FALHA", 0) or (args.estrito and contagem.get("PENDENTE", 0))
    return 1 if reprovado else 0


if __name__ == "__main__":
    sys.exit(main())
