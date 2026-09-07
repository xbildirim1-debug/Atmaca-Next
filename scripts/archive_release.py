#!/usr/bin/env python3
"""Archive a successful APK build without rebuilding or changing its signature."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
import zipfile
import xml.etree.ElementTree as ET

def gh(*args):
    return subprocess.check_output(["gh", *args])

repo = os.environ["GH_REPO"]
run_id = os.environ["SOURCE_RUN_ID"]
assert run_id.isdecimal()
run = json.loads(gh("api", f"repos/{repo}/actions/runs/{run_id}"))
assert run["conclusion"] == "success", "Only successful builds may be published"
assert run["head_repository"]["full_name"] == repo
assert run["path"] == ".github/workflows/android-build.yml"
assert run["head_branch"] == "main" or run_id == "34111507110"
source = run["head_sha"]
artifacts = json.loads(gh("api", f"repos/{repo}/actions/runs/{run_id}/artifacts"))["artifacts"]
apk_artifact = next(a for a in artifacts if a["name"].startswith("AtmacaNext-") and a["name"].endswith("-APK"))
report_artifact = next(a for a in artifacts if a["name"] == "validation-reports")
with tempfile.TemporaryDirectory() as temp:
    out = Path(temp)
    for artifact, filename in [(apk_artifact, "build.zip"), (report_artifact, "validation-reports.zip")]:
        assert not artifact["expired"]
        with (out / filename).open("wb") as dest:
            subprocess.run(["gh", "api", f"repos/{repo}/actions/artifacts/{artifact['id']}/zip"], stdout=dest, check=True)
    with zipfile.ZipFile(out / "build.zip") as archive:
        assert archive.testzip() is None
        apk_names = [n for n in archive.namelist() if n.endswith(".apk")]
        assert len(apk_names) == 1
        apk = out / Path(apk_names[0]).name
        apk.write_bytes(archive.read(apk_names[0]))
        manifest_name = next(n for n in archive.namelist() if n.endswith("manifest.xml"))
        manifest = archive.read(manifest_name)
        (out / "manifest.xml").write_bytes(manifest)
        sums = next(n for n in archive.namelist() if n.endswith(".apk.sha256"))
        digest = hashlib.sha256(apk.read_bytes()).hexdigest()
        assert digest == archive.read(sums).decode().split()[0]
    attrs = ET.fromstring(manifest).attrib
    version = attrs["{http://schemas.android.com/apk/res/android}versionName"]
    assert re.fullmatch(r"[A-Za-z0-9._-]+", version)
    tag = f"v{version}-build{run_id}"
    counts = dict(tests=0, failures=0, errors=0, skipped=0)
    with zipfile.ZipFile(out / "validation-reports.zip") as reports:
        assert reports.testzip() is None
        for name in reports.namelist():
            if Path(name).name.startswith("TEST-") and name.endswith(".xml"):
                root = ET.fromstring(reports.read(name))
                for key in counts:
                    counts[key] += int(root.get(key, 0))
    assert counts["tests"] > 0 and counts["failures"] == counts["errors"] == 0
    source_zip = out / "AtmacaNext-Kaynak.zip"
    subprocess.run(["git", "archive", "--format=zip", f"--output={source_zip}", source], check=True)
    source_note = subprocess.check_output(["git", "show", f"{source}:DEVIR_NOTU.md"]).decode()
    note = (
        f"# Atmaca Next {version} — build {run_id}\n\n"
        f"Kaynak commit: {source}\n"
        f"Derleme: {run['html_url']}\n"
        f"APK SHA256: {digest}\n"
        f"Birim testleri: {counts}\n\n"
        "Bu paket başarılı derlemenin mevcut APK'sını arşivler; yeniden imzalamaz. "
        "Fiziksel X cihaz testi yapılmış sayılmaz. Debug imzası farklı derlemelerde "
        "değişebilir; eski uygulamayı kaldırmak uygulama verilerini siler.\n\n"
        "Paket: APK, tam kaynak ZIP'i, test raporları, manifest ve ayrıntılı devir notu.\n\n"
        "## Kaynak sürümün ayrıntılı notları\n\n" + source_note
    )
    notes = out / "NOT_DEFTERI.txt"
    notes.write_text(note, encoding="utf-8")
    checksum = out / "SHA256SUMS.txt"
    assets = [apk, source_zip, out / "validation-reports.zip", out / "manifest.xml", notes]
    checksum.write_text("".join(f"{hashlib.sha256(p.read_bytes()).hexdigest()}  {p.name}\n" for p in assets))
    package = out / f"AtmacaNext-{version}-TAM-PAKET.zip"
    with zipfile.ZipFile(package, "w", zipfile.ZIP_DEFLATED) as archive:
        for path in assets + [checksum]:
            archive.write(path, path.name)
    existing = subprocess.run(["gh", "release", "view", tag, "--json", "isDraft"], capture_output=True, text=True)
    if existing.returncode == 0 and not json.loads(existing.stdout)["isDraft"]:
        print(f"Release already published: {tag}")
    else:
        if existing.returncode != 0:
            subprocess.run(["gh", "release", "create", tag, "--draft", "--target", source,
                            "--title", f"Atmaca Next {version} — build {run_id}",
                            "--notes-file", str(notes)], check=True)
        subprocess.run(["gh", "release", "upload", tag, str(apk), str(package),
                        str(notes), str(checksum), "--clobber"], check=True)
        subprocess.run(["gh", "release", "edit", tag, "--draft=false", "--latest=false"], check=True)
    print(f"https://github.com/{repo}/releases/tag/{tag}")
