from __future__ import annotations

import hashlib
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent
DIST = ROOT.parent / "dist"
SOURCE = DIST / "LogHorizon-Garden-Bedrock-FIX2.zip"
TARGET = DIST / "LogHorizon-Garden-Bedrock-FIX3.zip"


def main() -> None:
    if not SOURCE.exists():
        raise FileNotFoundError(SOURCE)

    with zipfile.ZipFile(SOURCE, "r") as src:
        files = {name: src.read(name) for name in src.namelist() if not name.endswith("/")}

    mapping_name = "garden_fruits.yml"
    mapping = files[mapping_name].decode("utf-8")

    apple_old = '''  garden_apple:\n    type: "minecraft:apple"\n    item-identifier: "none"\n    displayentityoptions:\n      y-offset: 0.0\n      vanilla-scale: true'''
    apple_new = '''  garden_apple:\n    type: "minecraft:apple"\n    item-identifier: "none"\n    displayentityoptions:\n      y-offset: 0.0\n      translation: [0.0, -0.45, 0.0]\n      vanilla-scale: true'''

    berries_old = '''  garden_sweet_berries:\n    type: "minecraft:sweet_berries"\n    item-identifier: "none"\n    displayentityoptions:\n      y-offset: 0.0\n      vanilla-scale: true'''
    berries_new = '''  garden_sweet_berries:\n    type: "minecraft:sweet_berries"\n    item-identifier: "none"\n    displayentityoptions:\n      y-offset: 0.0\n      translation: [0.0, -0.45, 0.0]\n      vanilla-scale: true'''

    if apple_old not in mapping or berries_old not in mapping:
        raise RuntimeError("Blocos esperados não encontrados em garden_fruits.yml")

    mapping = mapping.replace(apple_old, apple_new).replace(berries_old, berries_new)
    files[mapping_name] = mapping.encode("utf-8")

    install_name = "INSTALACAO.txt"
    install = files[install_name].decode("utf-8")
    install = install.replace("GARDEN BEDROCK FIX2", "GARDEN BEDROCK FIX3")
    install += '''\nAJUSTE DO FIX3\n- Maçã e oxicoco foram deslocados 0,45 bloco para baixo apenas no Bedrock.\n- Isso reproduz a aparência pendurada do Java e evita que fiquem dentro das folhas.\n- Jogadores Java não são afetados.\n'''
    files[install_name] = install.encode("utf-8")

    diag_name = "DIAGNOSTICO.txt"
    diag = files[diag_name].decode("utf-8")
    diag += '''\nFIX3: translation [0.0, -0.45, 0.0] aplicada somente a minecraft:apple e minecraft:sweet_berries.\n'''
    files[diag_name] = diag.encode("utf-8")

    checksum_lines = []
    for name in sorted(files):
        if name == "SHA256.txt":
            continue
        checksum_lines.append(f"{hashlib.sha256(files[name]).hexdigest()}  {name}")
    files["SHA256.txt"] = ("\n".join(checksum_lines) + "\n").encode("utf-8")

    if TARGET.exists():
        TARGET.unlink()
    with zipfile.ZipFile(TARGET, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as dst:
        for name in sorted(files):
            dst.writestr(name, files[name])

    with zipfile.ZipFile(TARGET, "r") as check:
        check.testzip()
        final_mapping = check.read(mapping_name).decode("utf-8")
        if final_mapping.count("translation: [0.0, -0.45, 0.0]") != 2:
            raise RuntimeError("Validação do deslocamento falhou")

    print(f"Pacote criado e validado: {TARGET}")


if __name__ == "__main__":
    main()
