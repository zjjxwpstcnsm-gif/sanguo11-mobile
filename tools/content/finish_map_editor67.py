#!/usr/bin/env python3
"""Recover only complete, hash-verified entries from the interrupted source transfer.
Never applies a partial JSON entry, overwrites concurrent edits, or removes staged pieces.
One-time integration record; not an end-user Patch importer.
"""
import base64, hashlib, json, pathlib, subprocess, zlib
ROOT=pathlib.Path(__file__).resolve().parents[2]
PARTS=['a37564c39e0e85df460fa69664e79befd0e424ea7021f17d6a09849476f9c24e','2bee23accf67ca79e680586f1313ebc0e89e2b7efe844deeee6e7047e8210ce9','93a953167ac6761fbbba6d60ac1978b44735d1dc894b551da363513a3b40b3a7','83face6c99f40765a7c81c626a3f0364a5daa3a36ff43c66131391ed5859c681']
EXPECTED={
'app/src/main/AndroidManifest.xml':'4ed89eddd8a63bc49489ad7fc73d48a31bfd949a',
'app/src/main/java/game/sanguo/mobile/MainActivity.java':'de16c753394041e7ad336b457bf294ebe4d8138e',
'app/src/main/java/game/sanguo/mobile/MapEditorActivity.java':'7e78e97d84f84826fe2e58f73281931bcc51460b',
'app/src/main/java/game/sanguo/mobile/MapEditorUi.java':'8cbf2df08f00b600e3f9ab2c9d91d2434590926d',
'app/src/main/java/game/sanguo/mobile/MapLibrary.java':'9fac103db33bac977258a891110f2d0a63243e8c',
'app/src/main/java/game/sanguo/mobile/MapView.java':'4e631e1fc8b4e2087ef17b1d2875a149fb493cb1',
'core/src/main/java/game/sanguo/core/CustomMapSave.java':'e336d3ee89f427607f4c0d12ff21c51a3d7107d0',
'core/src/main/java/game/sanguo/core/CustomMaps.java':'ff53479bafdd853b1ff9e7db1b08c304e1db4870',
'core/src/main/java/game/sanguo/core/Development.java':'99bbb08b8e84258e313bc07febe0a8bb08babbc4',
'core/src/main/java/game/sanguo/core/MapEditSession.java':'c8f199037340dd4f7e84604fca0268455df1aefd',
'core/src/main/java/game/sanguo/core/MapJson.java':'b16abe9b5125045b45285c9c6fc37df925d4af84',
'core/src/main/java/game/sanguo/core/MapPatch.java':'65b85c9cc8edf109322b782f15d0066f77407737',
'core/src/main/java/game/sanguo/core/MapWater.java':'a65d44b7026d204e9cfed171e085dde697cb3523',
'core/src/main/java/game/sanguo/core/NationalMap.java':'b415436e096bc410458a109adbf525b5b3bd9f4c',
'core/src/main/java/game/sanguo/core/SaveCodec.java':'7ca5de03cab2cc6bde2f4520db4c7cd9ee29abc6',
'core/src/main/java/game/sanguo/core/ScenarioData.java':'c00cb5274680bc0291878d6f7fbc3562e35422ac',
'core/src/main/java/game/sanguo/core/SiteAffiliation.java':'470667003c797bbbbde47df4b281645c8d0a4ca1'}
def blob(p):
    if not p.exists():return None
    b=p.read_bytes();return hashlib.sha1(b'blob '+str(len(b)).encode()+b'\0'+b).hexdigest()
def replace(path,old,new):
    p=ROOT/path;s=p.read_text(encoding='utf-8');assert s.count(old)==1,('Anchor mismatch',path,old[:60]);p.write_text(s.replace(old,new),encoding='utf-8')
def main():
    subprocess.run(['git','merge-base','--is-ancestor','bce071599bf251107061ee213a64a42cdebe97bc','HEAD'],cwd=ROOT,check=True)
    parts=[]
    for i,expected in enumerate(PARTS):
        b=(ROOT/f'editor67-part-{i}.b64').read_bytes();assert hashlib.sha256(b).hexdigest()==expected,('Concurrent transfer part',i);parts.append(b.strip())
    raw=zlib.decompressobj(31).decompress(base64.b64decode(b''.join(parts),validate=True),2000000)
    text=raw.decode('utf-8');pos=text.index('"entries":[')+len('"entries":[');entries=[]
    while True:
        try:e,length=json.JSONDecoder().raw_decode(text[pos:])
        except json.JSONDecodeError:break
        entries.append(e);pos+=length
        if text[pos:pos+1]!=',':break
        pos+=1
    assert len(entries)==17 and set(e['path'] for e in entries)==set(EXPECTED),'Complete entry set changed'
    library='app/src/main/java/game/sanguo/mobile/MapLibrary.java'
    assert blob(ROOT/library)=='0542f084019a73688da22009053a72833ca892f5','Concurrent MapLibrary change'
    for e in entries:
        assert e['after']==EXPECTED[e['path']]
        if e['path']==library:continue
        actual=blob(ROOT/e['path']);assert actual in (e['before'],e['after']),('Concurrent source change',e['path'],actual)
    world=ROOT/'core/src/main/java/game/sanguo/core/World.java'
    assert blob(world)=='a83ebc5e151d9370e407af4ac7f6f9c1be9d5668','Concurrent World change'
    assert (ROOT/'version.properties').read_text()=='versionName=0.66.0\nversionCode=66\n','Concurrent version change'
    for e in entries:
        if e['path']==library or blob(ROOT/e['path'])==e['after']:continue
        p=ROOT/e['path']
        if 'content' in e:p.parent.mkdir(parents=True,exist_ok=True);p.write_text(e['content'],encoding='utf-8')
        else:
            subprocess.run(['git','apply','--check','-'],cwd=ROOT,input=e['patch'].encode(),check=True)
            subprocess.run(['git','apply','-'],cwd=ROOT,input=e['patch'].encode(),check=True)
        assert blob(p)==e['after'],('Postimage mismatch',e['path'])
    replace('core/src/main/java/game/sanguo/core/World.java','    public int mapRevision;\n','    public int mapRevision;\n    public String customMapId="",customMapName="",customMapBase="",customMapFingerprint="";\n    public int customMapRevision;\n    public final SortedMap<Integer,Integer> siteParents=new TreeMap<>();\n')
    path=ROOT/'core/src/main/java/game/sanguo/core/MapEditSession.java';lines=path.read_text().splitlines();out=[]
    for line in lines:
        if line.startswith('    public static List<Hex> line('):line='    public static List<Hex> line(Hex a,Hex b){return MapBrushGeometry.line(a,b);}'
        elif line.startswith('    public static Set<Hex> disk('):line='    public static Set<Hex> disk(Hex center,int radius){if(radius<0||radius>2)throw new IllegalArgumentException("画笔半径必须是0、1或2");return MapBrushGeometry.disk(center,radius);}'
        out.append(line)
    path.write_text('\n'.join(out)+'\n',encoding='utf-8')
    activity='app/src/main/java/game/sanguo/mobile/MapEditorActivity.java'
    replace(activity,'private boolean busy,destroyed,drawing,grid,coords,passability,footprints=true;','private boolean busy,destroyed,discardOnExit,drawing,grid,coords,passability,footprints=true;')
    replace(activity,'.setNeutralButton("不保存退出",(d,n)->finish())','.setNeutralButton("不保存退出",(d,n)->{discardOnExit=true;finish();})')
    replace(activity,'super.onPause();if(session!=null){','super.onPause();if(session!=null&&!discardOnExit){')
    replace(activity,'map.setEnabled(true);result.accept(value);','map.setEnabled(true);try{result.accept(value);}catch(Exception e){error(e);}')
    replace(activity,'private void change(String label,Callable<?> action){map.setVisibility(View.INVISIBLE);','private void change(String label,Callable<?> action){if(busy||destroyed)return;map.setVisibility(View.INVISIBLE);')
    # Keep the result typed: map names/messages must never decide conflict handling.
    p=ROOT/activity;s=p.read_text();start=s.index('    private void previewImport(MapPatch p)');end=s.index('\n    private String siteChanges',start)
    new='''    private record ImportPreview(boolean conflict,String detail){}
    private void previewImport(MapPatch p){work("检查导入影响（尚未应用）",()->{
        MapLibrary.ImportCheck conflict=library.checkImport(p);List<CustomMaps.Issue> found=CustomMaps.validateAll(p);long errors=found.stream().filter(CustomMaps.Issue::blocking).count();
        return new ImportPreview(conflict.forkRequired(),conflict.message()+"\\n"+p.summary()+"\\n阻断错误："+errors+"；有错误只能保留待修草稿。\\n"+siteChanges(p));
    },preview->{AlertDialog.Builder dialog=new AlertDialog.Builder(this).setTitle("导入预览 · 未修改当前数据").setMessage(preview.detail()).setNegativeButton("取消",null);
        if(!preview.conflict())dialog.setPositiveButton("替换草稿（可撤销）",(d,n)->change("导入Patch",()->{session.replace("导入Patch",p);return "导入完成；重复导入不会重复创建实体。请校验并发布后新开游戏";}));
        else dialog.setPositiveButton("明确作为新地图",(d,n)->{MapPatch fork=p.copy();fork.id=UUID.randomUUID().toString();fork.revision=1;change("作为新地图导入",()->{session.replace("作为新地图导入",fork);return "已使用新地图ID；原版本未覆盖";});});dialog.show();});}'''
    p.write_text(s[:start]+new+s[end:],encoding='utf-8')
    (ROOT/'version.properties').write_text('versionName=0.67.0-map-editor\nversionCode=67\n')
    print('Integrated 16 recovered production entries; retained durable MapLibrary; added World binding, brush wiring, typed import and exit safety.')
if __name__=='__main__':main()
