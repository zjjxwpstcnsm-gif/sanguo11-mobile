#!/usr/bin/env python3
"""Reject misidentified/dirty release candidates; no source-archive fallback."""
import re
import subprocess
import sys


def verify(expected):
    if not re.fullmatch(r"[0-9a-f]{40}", expected):
        raise ValueError("candidateSource must be a full lowercase Git SHA")
    actual = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
    if actual != expected:
        raise ValueError(f"candidateSource mismatch: expected {expected}, actual {actual}")
    dirty = subprocess.check_output(
        ["git", "status", "--porcelain", "--untracked-files=normal"], text=True
    )
    if dirty:
        raise ValueError("candidate source is not clean:\n" + dirty)
    print("PASS clean candidateSource=" + actual)


if __name__ == "__main__":
    try:
        verify(sys.argv[1] if len(sys.argv) == 2 else "")
    except (ValueError, subprocess.CalledProcessError) as error:
        sys.exit(str(error))
