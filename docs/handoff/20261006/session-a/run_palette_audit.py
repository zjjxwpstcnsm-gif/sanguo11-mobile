#!/usr/bin/env python3
"""Compile real core and production palette against all installed sources, in A-owned output."""
import pathlib, subprocess, shutil
ROOT=pathlib.Path(__file__).resolve().parents[4]
OUT=ROOT/'out/session-a/palette-audit'
OUT.mkdir(parents=True,exist_ok=True)
JAVA=shutil.which('java') or str(pathlib.Path('/Users/paopao/workspace/sanguo11-mobile/out/toolchain/jdk-17/bin/java'))
core=sorted((ROOT/'core/src/main/java').rglob('*.java'))
def compile_to(directory,palette,test):
 directory.mkdir(parents=True,exist_ok=True)
 sources=[*core,palette,test]
 (directory/'sources.txt').write_text('\n'.join(str(x) for x in sources)+'\n')
 subprocess.run([JAVA,'-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-encoding','UTF-8','-d',str(directory),'@'+str(directory/'sources.txt')],check=True)
 classpath=str(directory)+':'+str(ROOT/'core/src/main/resources')
 result=subprocess.run([JAVA,'-Xmx512m','-cp',classpath,'game.sanguo.mobile.'+test.stem],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,check=True)
 return result.stdout
baseline=OUT/'source/FactionColors.java';baseline.parent.mkdir(parents=True,exist_ok=True)
baseline.write_bytes(subprocess.check_output(['git','show','0e7b9bc2:app/src/main/java/game/sanguo/mobile/FactionColors.java'],cwd=ROOT))
before=compile_to(OUT/'before',baseline,ROOT/'docs/handoff/20261006/parallel-repair/PaletteAudit.java')
(ROOT/'docs/handoff/20261006/session-a/palette-before.txt').write_text(before)
after=compile_to(OUT/'after',ROOT/'app/src/main/java/game/sanguo/mobile/FactionColors.java',ROOT/'docs/handoff/20261006/session-a/PaletteRegression.java')
(ROOT/'docs/handoff/20261006/session-a/palette-after.txt').write_text(after)
print(before,end='');print(after,end='')
