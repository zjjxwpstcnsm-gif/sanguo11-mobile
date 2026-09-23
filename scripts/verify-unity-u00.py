#!/usr/bin/env python3
"""Strict artifact inspection; no test result is synthesized when export is absent."""
import sys
import zipfile
from pathlib import Path


def verify_export(path):
    root = Path(path) / "unityLibrary"
    if not (root / "build.gradle").is_file():
        raise ValueError("Unity export has no unityLibrary/build.gradle")
    libraries = list(root.rglob("libunity.so"))
    if not libraries:
        raise ValueError("Unity export has no libunity.so")
    if not any(p.is_file() and p.stat().st_size for p in libraries):
        raise ValueError("Unity export has an empty Player library")
    print("Unity export: libunity.so present", len(libraries))


def verify_apk(path):
    apk = Path(path)
    if not apk.is_file() or apk.stat().st_size < 1024 * 1024:
        raise ValueError("No nonempty installable APK")
    with zipfile.ZipFile(apk) as bundle:
        names = bundle.namelist()
        if len(names) != len(set(names)):
            raise ValueError("APK contains duplicate entry names")
        if "lib/arm64-v8a/libunity.so" not in names:
            raise ValueError("APK lacks ARM64 Unity Player")
        if not any(n.startswith("assets/bin/Data/") for n in names):
            raise ValueError("APK lacks Unity Player data")
        for lib in ("libunity.so", "libfilament-jni.so"):
            locations = [n for n in names if n.endswith("/" + lib)]
            print(lib, [(n, bundle.getinfo(n).file_size) for n in locations])
        print("APK size:", apk.stat().st_size, "bytes")


if __name__ == "__main__":
    if len(sys.argv) != 3 or sys.argv[1] not in ("export", "apk"):
        sys.exit("usage: verify-unity-u00.py export|apk PATH")
    try:
        (verify_export if sys.argv[1] == "export" else verify_apk)(sys.argv[2])
    except (ValueError, OSError, zipfile.BadZipFile) as error:
        sys.exit(str(error))
