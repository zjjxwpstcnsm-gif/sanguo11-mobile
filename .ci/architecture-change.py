from pathlib import Path
import subprocess
root = Path.cwd()
subprocess.run(['git','diff','--exit-code','d9d70c10fe97b36fabd3a47a7b9ca28cffb4e203','--','core','app','game-api','game-runtime','unity','scripts','settings.gradle'],check=True)
p=root/'app/src/main/java/game/sanguo/mobile/RealmUi.java'
s=p.read_text()
a='World.Result r=w.governance.nameNation(side,field.getText().toString());if(!r.ok){field.setError(r.message);return;}dialog.dismiss();a.apply(r);'
b='World.Result r=a.apply(w,()->w.governance.nameNation(side,field.getText().toString()));if(!r.ok){field.setError(r.message);return;}dialog.dismiss();'
assert a in s
p.write_text(s.replace(a,b))
