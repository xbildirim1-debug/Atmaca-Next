"""Append actual CI results to the notebook bundled with each APK."""
import os
from pathlib import Path
import xml.etree.ElementTree as ET
import zipfile

counts = dict(tests=0, failures=0, errors=0, skipped=0)
reports = list(Path("app/build/test-results").rglob("TEST-*.xml"))
for report in reports:
    root = ET.parse(report).getroot()
    for key in counts:
        counts[key] += int(root.get(key, 0))
assert counts["tests"] > 0 and counts["failures"] == counts["errors"] == 0
with Path("dist/NOT_DEFTERI.txt").open("a") as notes:
    notes.write("\n\nBU APK'NIN GERCEK CI SONUCU\n")
    notes.write(f"Kaynak: {os.environ['GITHUB_SHA']}\n")
    notes.write(f"Derleme: https://github.com/{os.environ['GITHUB_REPOSITORY']}/actions/runs/{os.environ['GITHUB_RUN_ID']}\n")
    notes.write(f"Testler: {counts}\nTest, lint, APK ve imza/manifest kontrolu basarili. Fiziksel X testi yapilmadi.\n")
apk = next(Path("dist").glob("*.apk"))
with zipfile.ZipFile(apk.with_name(apk.stem + "-Testler.zip"), "w", zipfile.ZIP_DEFLATED) as archive:
    for report in reports:
        archive.write(report, report.relative_to("app/build"))
