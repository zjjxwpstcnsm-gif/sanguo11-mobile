"""Use identical observation-only timing routines against old and new installed APKs."""
from pathlib import Path
import sys
relative = Path("app/src/androidTest/java/game/sanguo/mobile/GameSmokeRunner.java")
current = relative.read_text()
old_path = Path(sys.argv[1]) / relative
old = old_path.read_text()
start = "    private void benchmarkExperience("
end = "    private void strategicManagementFlow("
new_method = current[current.index(start):current.index(end)]
old_path.write_text(old[:old.index(start)] + new_method + old[old.index(end):])
