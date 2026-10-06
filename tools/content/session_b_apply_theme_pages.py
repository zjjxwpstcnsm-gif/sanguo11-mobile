#!/usr/bin/env python3
"""Apply A's completed theme API only in the sixteen B-owned pages."""
import json,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def expression_end(text,start):
 parens=braces=brackets=0;quote=None;escaped=False
 for pos in range(start,len(text)):
  c=text[pos]
  if quote:
   if escaped:escaped=False
   elif c=='\\':escaped=True
   elif c==quote:quote=None
   continue
  if c in '\"\'':quote=c;continue
  if c=='(':parens+=1
  elif c==')':parens-=1
  elif c=='{':braces+=1
  elif c=='}':braces-=1
  elif c=='[':brackets+=1
  elif c==']':brackets-=1
  elif c==';'and parens==braces==brackets==0:return pos
  if min(parens,braces,brackets)<0:return None
 return None
def main():
 rows=json.loads((ROOT/'docs/handoff/20261006/parallel-repair/APP_OWNERSHIP.json').read_text())['files'];report=[]
 for row in rows:
  if row['owner']!='B':continue
  p=ROOT/row['path'];before=p.read_text();text=before;changes=[]
  # Process inner/rightmost calls first, recomputing outer boundaries after each
  # insertion. Fixed original offsets cannot safely replace nested builders.
  positions=[m.start() for m in re.finditer(r'new AlertDialog\.Builder\(',text)]
  for start in reversed(positions):
   end=expression_end(text,start)
   if end is None:continue
   expr=text[start:end];prefix=text[max(text.rfind(';',0,start),text.rfind('{',0,start),text.rfind('}',0,start))+1:start]
   if not expr.endswith('.show()')or re.search(r'=\s*$',prefix)or re.search(r'\breturn\s*$',prefix)or 'UiTheme.dialog('in prefix:continue
   text=text[:start]+'UiTheme.dialog('+expr+')'+text[end:]
   changes.append(start)
  builders=set(re.findall(r'AlertDialog\.Builder\s+(\w+)\s*=',text))
  for builder in builders:
   text=re.sub(r'(?<![\w.])'+re.escape(builder)+r'\.show\(\);',lambda m:'UiTheme.dialog('+m.group(0)[:-1]+');',text)
  # Normalize explicit selected/status colors after the page binds them.
  text=re.sub(r'(\b(\w+)\.setTextColor\([^;]+\);)(?!UiTheme\.readable)',lambda m:m.group(1)+'UiTheme.readable('+m.group(2)+');',text)
  if text!=before:p.write_text(text)
  report.append(dict(path=row['path'],changed=text!=before,dialogs=len(changes),explicitInkBindings=text.count('UiTheme.readable(')))
 print(json.dumps(report,ensure_ascii=False,indent=2))
if __name__=='__main__':main()
