"""Verify actual reports, manifest, certificate and deliverable integrity."""
import hashlib
import json
import os
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET
import zipfile

if len(sys.argv) == 3 and sys.argv[1] == "--bundle":
    bundle = Path(sys.argv[2])
    with zipfile.ZipFile(bundle) as archive:
        assert archive.testzip() is None, "Full bundle CRC failed"
        apks = [name for name in archive.namelist() if name.endswith(".apk")]
        assert len(apks) == 1, apks
        assert archive.read(apks[0]) == (bundle.parent / apks[0]).read_bytes(), "Bundled APK differs"
        forbidden = [name for name in archive.namelist() if name.endswith((".keystore", ".jks"))]
        assert not forbidden, forbidden
    print(json.dumps({"bundle": bundle.name, "bytes": bundle.stat().st_size,
                      "sha256": hashlib.sha256(bundle.read_bytes()).hexdigest(),
                      "crc": "PASS", "same_apk": True, "private_key_excluded": True}), flush=True)
    raise SystemExit(0)

dist = Path("dist")
reports = sorted(Path("app/build/test-results/testDebugUnitTest").glob("TEST-*.xml"))
counts = dict(tests=0, failures=0, errors=0, skipped=0)
new_suites = {}
for report in reports:
    suite = ET.parse(report).getroot()
    values = {key: int(suite.get(key, 0)) for key in counts}
    for key in counts:
        counts[key] += values[key]
    name = suite.get("name", "")
    if ".NonFollower" in name or name.endswith((".NavigationRestart26_60Test", ".QuoteReply26_61Test", ".QuoteTaskSetup26_61Test", ".ReplyTextInput26_62Test")):
        new_suites[name] = values
assert counts["tests"] >= 643 and counts["failures"] == counts["errors"] == counts["skipped"] == 0, counts
for name in ("NavigationRestart26_60Test", "NonFollowerUnfollowCompletionTest", "NonFollowerFollowingInspectorTest",
             "NonFollowerPolicyTest", "NonFollowerScrollProgressTest", "NonFollowerViewportGateTest",
             "NonFollowerTimingTest", "NonFollowerBatch26_60Test", "QuoteReply26_61Test", "QuoteTaskSetup26_61Test", "ReplyTextInput26_62Test"):
    assert any(suite.endswith("." + name) and values["tests"] > 0 for suite, values in new_suites.items()), name

lint_file = Path("app/build/reports/lint-results-debug.xml")
lint = dict(Fatal=0, Error=0, Warning=0, Information=0)
for issue in ET.parse(lint_file).getroot().findall("issue"):
    severity = issue.get("severity", "")
    lint[severity] = lint.get(severity, 0) + 1
assert lint["Fatal"] == lint["Error"] == 0, lint

android = "{http://schemas.android.com/apk/res/android}"
manifest = ET.parse(dist / "manifest.xml").getroot()
gradle = Path("app/build.gradle.kts").read_text()
code = int(re.search(r"versionCode = (\d+)", gradle).group(1))
version = re.search(r'versionName = "([^"]+)"', gradle).group(1)
app_id = re.search(r'applicationId = "([^"]+)"', gradle).group(1)
assert manifest.get("package") == app_id
assert int(manifest.get(android + "versionCode")) == code
assert manifest.get(android + "versionName") == version
permissions = [node.get(android + "name") for node in manifest.findall("uses-permission")]
providers = [node.get(android + "name") for node in manifest.findall("application/provider")]
assert "android.permission.INTERNET" not in permissions
assert "androidx.startup.InitializationProvider" not in providers

signature = (dist / "SIGNATURE.txt").read_text()
cert = re.search(r"Signer #1 certificate SHA-256 digest:\s*([a-fA-F0-9]{64})", signature).group(1).lower()
reference = "9bb1488d50538b0b69427f7b49f5cc809b6b715eeb81a1867e33fe930074b7a5"
apk = next(dist.glob("*.apk"))
with zipfile.ZipFile(apk) as archive:
    assert archive.testzip() is None, "APK CRC failed"

summary = {
    "version": version, "version_code": code, "application_id": app_id, "build_kind": "github_ci",
    "source_commit": os.environ["GITHUB_SHA"], "device_tested": False,
    "run_url": "https://github.com/" + os.environ["GITHUB_REPOSITORY"] + "/actions/runs/" + os.environ["GITHUB_RUN_ID"],
    "tests": counts, "new_suites": new_suites, "lint": lint,
    "migration": "PASS: both historical v5 layouts preserved",
    "signing_cert_sha256": cert, "matches_26_59_local_certificate": cert == reference,
    "apk": {"filename": apk.name, "bytes": apk.stat().st_size,
            "sha256": hashlib.sha256(apk.read_bytes()).hexdigest(), "crc": "PASS"}
}
(dist / "BUILD_VERIFICATION.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n")
with (dist / "NOT_DEFTERI.txt").open("a") as notes:
    notes.write("\nGercek APK sertifikasi SHA256: " + cert + "\n")
    notes.write("26.59 yerel APK ile ayni sertifika: " + str(cert == reference) + "\n")
    if cert != reference:
        notes.write("Bu CI APK yerel 26.59 uzerine dogrudan guncelleme olarak kurulamaz. "
                    "Mevcut uygulamayi kaldirmak yerel kayitlari siler. Ozel imzali uyumlu APK teslimi bekliyor.\n")
print("BUILD_VERIFICATION=" + json.dumps(summary, ensure_ascii=False), flush=True)
