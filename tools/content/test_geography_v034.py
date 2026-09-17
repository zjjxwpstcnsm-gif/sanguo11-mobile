import json, pathlib, sys
sys.path.insert(0,str(pathlib.Path(__file__).parent))
from national_geography import generate_geography
sites=json.loads(pathlib.Path("data/content/sites-source.json").read_text())
cities=[x for x in sites if x["kind"]=="city"]
t,_=generate_geography(cities)
assert t[69][80]=="W", "Yellow River north of Luoyang must remain water"
for x,y in [(79,76),(90,77),(65,72),(71,78)]: assert t[y][x] not in "MWOV", (x,y,t[y][x])
assert sum(t[87][x]=="M" for x in range(71,88))>=12, "Luoyang southern mountain rim reopened"
print("v0.34 geography chokepoints: ok")
