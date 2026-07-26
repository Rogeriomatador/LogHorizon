from __future__ import annotations

import base64
import hashlib
import io
import json
import re
import shutil
import sys
import urllib.request
import zipfile
from pathlib import Path

PROJECT_ID = "ePND7F16"  # Garden no Modrinth
TARGET_VERSION = "2.0.1"
USER_AGENT = "LogHorizon-Garden-Bedrock-Fix/2.0"

ROOT = Path(__file__).resolve().parent
OUT = ROOT / "out"
DIST = ROOT.parent / "dist"

URL_HASH_RE = re.compile(r"textures\.minecraft\.net/texture/([0-9a-fA-F]{32,64})")
BASE64_RE = re.compile(r"(?<![A-Za-z0-9+/])([A-Za-z0-9+/]{80,}={0,2})(?![A-Za-z0-9+/])")


def request_json(url: str):
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=60) as response:
        return json.load(response)


def request_bytes(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=120) as response:
        return response.read()


def select_garden_file() -> tuple[dict, dict]:
    versions = request_json(f"https://api.modrinth.com/v2/project/{PROJECT_ID}/version")
    matches = [v for v in versions if str(v.get("version_number")) == TARGET_VERSION]
    if not matches:
        matches = [v for v in versions if TARGET_VERSION in str(v.get("name", ""))]
    if not matches:
        raise RuntimeError(f"Garden {TARGET_VERSION} não encontrado no Modrinth")

    version = matches[0]
    files = version.get("files", [])
    if not files:
        raise RuntimeError("Versão localizada sem arquivo para download")
    file_info = next((f for f in files if f.get("primary")), files[0])
    return version, file_info


def decode_candidates(text: str) -> set[str]:
    hashes: set[str] = {h.lower() for h in URL_HASH_RE.findall(text)}
    for candidate in BASE64_RE.findall(text):
        try:
            padded = candidate + "=" * ((4 - len(candidate) % 4) % 4)
            decoded = base64.b64decode(padded, validate=False).decode("utf-8", errors="ignore")
        except Exception:
            continue
        hashes.update(h.lower() for h in URL_HASH_RE.findall(decoded))
    return hashes


def discover_hashes(jar_bytes: bytes) -> tuple[set[str], list[tuple[str, str]]]:
    hashes: set[str] = set()
    origins: list[tuple[str, str]] = []
    with zipfile.ZipFile(io.BytesIO(jar_bytes)) as jar:
        for name in jar.namelist():
            if name.endswith("/"):
                continue
            lower = name.lower()
            if not lower.endswith((".yml", ".yaml", ".json", ".properties", ".txt")):
                continue
            try:
                text = jar.read(name).decode("utf-8", errors="ignore")
            except Exception:
                continue
            found = decode_candidates(text)
            for value in sorted(found):
                hashes.add(value)
                origins.append((name, value))
    return hashes, origins


def write_files(version: dict, file_info: dict, jar_bytes: bytes, hashes: set[str], origins: list[tuple[str, str]]):
    if OUT.exists():
        shutil.rmtree(OUT)
    OUT.mkdir(parents=True)
    DIST.mkdir(parents=True, exist_ok=True)

    mappings = '''# Log Horizon - Garden 2.0.1 / Bedrock
# Lido pelo GeyserDisplayEntity.
# Evita que displays vanilla do Garden sejam ocultados e aplica o tratamento
# de cabeças personalizadas após o registro dos hashes no Geyser.

mappings:
  garden_apple:
    type: "minecraft:apple"
    item-identifier: "none"
    displayentityoptions:
      y-offset: 0.0
      vanilla-scale: true
      vanilla-scale-multiplier: 1.0
      hand: false

  garden_sweet_berries:
    type: "minecraft:sweet_berries"
    item-identifier: "none"
    displayentityoptions:
      y-offset: 0.0
      vanilla-scale: true
      vanilla-scale-multiplier: 1.0
      hand: false

  garden_custom_fruit_heads:
    type: "minecraft:player_head"
    item-identifier: "none"
    displayentityoptions:
      y-offset: 0.0
      vanilla-scale: true
      vanilla-scale-multiplier: 1.0
      hand: false
'''
    (OUT / "garden_fruits.yml").write_text(mappings, encoding="utf-8")

    hash_lines = "\n".join(f"  - {h}" for h in sorted(hashes))
    (OUT / "garden-skin-hashes.txt").write_text(
        "# Cole somente estas linhas abaixo de skin-hashes: em custom-skulls.yml\n" + hash_lines + "\n",
        encoding="utf-8",
    )

    custom_skulls = f'''# Arquivo-base somente para instalações sem custom-skulls.yml.
# Caso o arquivo já exista, NÃO substitua: copie apenas as linhas de skin-hashes.
player-usernames: []
player-uuids: []
player-profiles: []
skin-hashes:
{hash_lines}
'''
    (OUT / "custom-skulls-GARDEN-ADICIONAR.yml").write_text(custom_skulls, encoding="utf-8")

    install = '''LOG HORIZON - GARDEN BEDROCK FIX2

CAUSA CONFIRMADA
- A maçã é um Item Display com minecraft:apple. Como a extensão estava configurada
  para ocultar displays vanilla sem mapeamento, ela sumia apenas no Bedrock.
- Uva e outras frutas usam cabeças personalizadas. Bedrock não suporta essas texturas
  nativamente; o Geyser precisa pré-registrar os hashes e gerar seu pacote de recursos.

ARQUIVO 1: garden_fruits.yml
Destino:
plugins/Geyser-Spigot/extensions/geyserdisplayentity/Mappings/garden_fruits.yml

Não apague animated_doors.yml.

ARQUIVO 2: garden-skin-hashes.txt
Abra:
plugins/Geyser-Spigot/custom-skulls.yml

Localize skin-hashes: e cole as linhas do arquivo abaixo dessa seção.
Não crie duas seções skin-hashes e não apague hashes existentes.

Se custom-skulls.yml não existir, copie custom-skulls-GARDEN-ADICIONAR.yml para:
plugins/Geyser-Spigot/custom-skulls.yml

CONFIGURAÇÃO OBRIGATÓRIA DO GEYSER
Em plugins/Geyser-Spigot/config.yml confirme:
gameplay:
  enable-custom-content: true

REINÍCIO
1. Desligue completamente o servidor.
2. Instale/mescle os arquivos.
3. Ligue o servidor. Não use /reload.
4. Feche completamente o Minecraft Bedrock.
5. Em Configurações > Armazenamento > Dados em cache, remova o pacote do servidor.
6. Entre novamente e aceite o pacote regenerado pelo Geyser.

O JAR GeyserDisplayEntity-1.0.11-LH-DOORS-FIX3 continua o mesmo.
Garden, GardenPlus, AuraSkills e GardenGifts não são alterados.
'''
    (OUT / "INSTALACAO.txt").write_text(install, encoding="utf-8")

    diag = f'''DIAGNÓSTICO E VALIDAÇÃO
Garden selecionado: {version.get("name")} / {version.get("version_number")}
Arquivo do Modrinth: {file_info.get("filename")}
SHA-512 informado: {file_info.get("hashes", {}).get("sha512", "não informado")}
SHA-256 baixado: {hashlib.sha256(jar_bytes).hexdigest()}
Hashes de texturas encontrados: {len(hashes)}

O pacote foi gerado diretamente dos recursos incorporados no JAR oficial do Garden 2.0.1.
Nenhum modelo novo foi inventado: maçã/sweet berries usam os itens vanilla e as demais
texturas são registradas como custom skulls do Geyser.
'''
    (OUT / "DIAGNOSTICO.txt").write_text(diag, encoding="utf-8")

    report_lines = [f"Garden {TARGET_VERSION} - origens dos hashes", ""]
    report_lines.extend(f"{path} -> {value}" for path, value in sorted(set(origins)))
    (OUT / "garden-resource-report.txt").write_text("\n".join(report_lines) + "\n", encoding="utf-8")

    if len(hashes) < 5:
        raise RuntimeError(f"Validação falhou: apenas {len(hashes)} hashes encontrados")

    checksum_lines = []
    for path in sorted(OUT.iterdir()):
        checksum_lines.append(f"{hashlib.sha256(path.read_bytes()).hexdigest()}  {path.name}")
    (OUT / "SHA256.txt").write_text("\n".join(checksum_lines) + "\n", encoding="utf-8")

    zip_path = DIST / "LogHorizon-Garden-Bedrock-FIX2.zip"
    if zip_path.exists():
        zip_path.unlink()
    with zipfile.ZipFile(zip_path, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for path in sorted(OUT.iterdir()):
            archive.write(path, arcname=path.name)

    print(f"Pacote criado: {zip_path}")
    print(f"Hashes encontrados: {len(hashes)}")
    for h in sorted(hashes):
        print(h)


def main() -> int:
    version, file_info = select_garden_file()
    jar_bytes = request_bytes(file_info["url"])
    if not zipfile.is_zipfile(io.BytesIO(jar_bytes)):
        raise RuntimeError("O download do Garden não é um JAR/ZIP válido")
    hashes, origins = discover_hashes(jar_bytes)
    write_files(version, file_info, jar_bytes, hashes, origins)
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(f"ERRO: {exc}", file=sys.stderr)
        raise
