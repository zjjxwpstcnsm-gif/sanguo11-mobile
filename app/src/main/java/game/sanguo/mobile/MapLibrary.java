package game.sanguo.mobile;

import android.content.Context;
import android.util.AtomicFile;
import game.sanguo.core.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

/** Serialized AtomicFile library. Revisions never overwrite another immutable identity.
 * Tombstones/high-water marks survive deletion; campaign saves contain their own geography. */
final class MapLibrary {
    private static final String LEDGER="revision-ledger.txt";
    private static final Pattern REVISION_FILE=Pattern.compile("([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})-r([0-9]+)\\.json");
    private final File directory,legacy;
    record Entry(String id,int revision,String name) {public String label(){return name+" · r"+revision;}}
    record ImportCheck(boolean forkRequired,String message) {}
    MapLibrary(Context context){directory=new File(context.getFilesDir(),"custom-maps-v1");legacy=new File(context.getFilesDir(),"map-editor-draft-v1.json");}
    private File path(String key)throws IOException {
        if(!key.matches("[a-z0-9.-]+"))throw new IOException("非法地图文件名");
        if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("无法创建地图目录");return new File(directory,key);
    }
    private static boolean present(File file){return file.exists()||new File(file+".bak").exists();}
    private static boolean occupied(File file){return present(file)||new File(file+".new").exists();}
    private static byte[] boundedRead(InputStream in,int limit)throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
        while((n=in.read(buffer))!=-1){if(out.size()+n>limit)throw new IOException("地图文件过大");out.write(buffer,0,n);}return out.toByteArray();
    }
    private byte[] read(String key)throws IOException {try(InputStream in=new AtomicFile(path(key)).openRead()){return boundedRead(in,key.equals(LEDGER)?MapRevisionLedger.MAX_BYTES:MapJson.LIMIT);}}
    private void write(String key,byte[] data)throws IOException {
        AtomicFile file=new AtomicFile(path(key));FileOutputStream out=null;
        try{out=file.startWrite();out.write(data);file.finishWrite(out);}
        catch(Exception e){if(out!=null)file.failWrite(out);throw new IOException("原子保存失败；原文件未覆盖",e);}
    }
    private MapRevisionLedger ledger()throws IOException {return present(path(LEDGER))?MapRevisionLedger.decode(read(LEDGER)):new MapRevisionLedger();}
    private void ledger(MapRevisionLedger ledger)throws IOException {write(LEDGER,ledger.encode());}
    private static String key(String id,int revision){return id+"-r"+revision+".json";}
    private static int number(String digits)throws IOException {
        try{int n=Integer.parseInt(digits);if(n<1||n>MapRevisionLedger.MAX_REVISION)throw new NumberFormatException();return n;}
        catch(NumberFormatException e){throw new IOException("地图库文件修订号无效，原文件已保留",e);}
    }
    /** Include backup-only and interrupted .new identities; only complete files are exposed. */
    private SortedSet<String> revisionKeys()throws IOException {
        path("draft.json");File[] files=directory.listFiles();if(files==null)throw new IOException("无法读取地图库目录");SortedSet<String> result=new TreeSet<>();
        for(File file:files){String name=file.getName();if(name.endsWith(".bak")||name.endsWith(".new"))name=name.substring(0,name.length()-4);Matcher m=REVISION_FILE.matcher(name);if(m.matches()){number(m.group(2));result.add(name);}}return result;
    }
    private int observedMaximum(String id)throws IOException {int max=0;for(String key:revisionKeys()){Matcher m=REVISION_FILE.matcher(key);if(m.matches()&&m.group(1).equals(id))max=Math.max(max,number(m.group(2)));}return max;}
    private MapPatch readRevision(String file)throws IOException {
        Matcher m=REVISION_FILE.matcher(file);if(!m.matches())throw new IOException("修订文件名无效");MapPatch p=MapPatch.decode(read(file));
        if(!p.id.equals(m.group(1))||p.revision!=number(m.group(2)))throw new IOException("不可变版本内容与文件身份不一致，原文件保留");return p;
    }
    void archiveDraft()throws IOException {synchronized(MapLibrary.class){if(present(path("draft.json")))write("draft-recovery-"+System.currentTimeMillis()+".json",read("draft.json"));}}
    MapPatch draft()throws IOException {synchronized(MapLibrary.class){if(present(path("draft.json"))){MapPatch p=MapPatch.decode(read("draft.json"));CustomMaps.verifyBase(p);return p;}if(present(legacy))return migrateLegacy();return CustomMaps.base().fresh();}}
    private MapPatch migrateLegacy()throws IOException {
        try{
            byte[] bytes;try(InputStream in=new AtomicFile(legacy).openRead()){bytes=boundedRead(in,MapJson.LIMIT);}
            JSONObject data=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
            if(data.getInt("format")!=1||!data.getString("base").equals(NationalMap.SHA256))throw new IOException("旧草稿内容基线不匹配，文件已保留");
            MapPatch p=CustomMaps.base().fresh();p.id=data.getString("id");World w=CustomMaps.preview(p,p.preview);Set<Hex> seen=new HashSet<>();JSONArray cells=data.getJSONArray("cells");
            for(int i=0;i<cells.length();i++){
                JSONObject item=cells.getJSONObject(i);String[] pair=item.getString("hex").split(",",-1);if(pair.length!=2)throw new IOException("旧草稿坐标错误");Hex h=new Hex(Integer.parseInt(pair[0]),Integer.parseInt(pair[1]));
                if(!seen.add(h))throw new IOException("旧草稿坐标重复，原文件已保留");if(!w.inside(h)||w.cityAt(h)!=null||w.domestic.at(h)!=null||w.war.at(h)!=null)throw new IOException("旧草稿受保护格冲突");
                SourceGridCoord at=MapCoordinates.nationalSource(w,h);World.Terrain before=CustomMaps.base().terrain(at.x,at.y),after=World.Terrain.valueOf(item.getString("terrain"));
                if(after==World.Terrain.VOID)throw new IOException("旧草稿含VOID修改");if(before!=after)p.terrain.put(at.x*200+at.y,new MapPatch.Cell(at.x,at.y,before,after));
            }
            CustomMaps.preview(p,p.preview);saveDraft(p);return p;
        }catch(JSONException|IllegalArgumentException e){throw new IOException("旧草稿未迁移；原文件保留",e);}
    }
    void saveDraft(MapPatch p)throws IOException {synchronized(MapLibrary.class){byte[] bytes=p.encode();MapPatch.decode(bytes);write("draft.json",bytes);}}
    List<Entry> entries()throws IOException {synchronized(MapLibrary.class){
        MapRevisionLedger ledger=ledger();Map<String,Entry> latest=new TreeMap<>();
        for(String name:revisionKeys()){Matcher m=REVISION_FILE.matcher(name);if(!m.matches()||ledger.deleted(m.group(1))||!present(path(name)))continue;MapPatch p=readRevision(name);Entry old=latest.get(p.id);if(old==null||old.revision<p.revision)latest.put(p.id,new Entry(p.id,p.revision,p.name));}
        List<Entry> out=new ArrayList<>(latest.values());out.sort(Comparator.comparing(Entry::name).thenComparing(Entry::id));return out;
    }}
    MapPatch load(Entry e)throws IOException {synchronized(MapLibrary.class){
        if(!MapRevisionLedger.validId(e.id)||e.revision<1||e.revision>MapRevisionLedger.MAX_REVISION)throw new IOException("地图版本身份错误");if(ledger().deleted(e.id))throw new IOException("该地图已从地图库删除；已有战局请从存档继续");MapPatch p=readRevision(key(e.id,e.revision));CustomMaps.verifyBase(p);return p;
    }}
    MapPatch publish(MapPatch source)throws IOException {
        MapPatch p=MapPatch.decode(source.encode());for(CustomMaps.Issue issue:CustomMaps.validateAll(p))if(issue.blocking())throw new IOException(issue.scenario()+"："+issue.message());
        synchronized(MapLibrary.class){
            MapRevisionLedger ledger=ledger();File requested=path(key(p.id,p.revision));
            if(!ledger.deleted(p.id)&&present(requested)){MapPatch existing=readRevision(requested.getName());if(Arrays.equals(existing.encode(),p.encode()))return existing;}
            // Reserve durably BEFORE writing. Even a failed write/deletion cannot recycle rN.
            p.revision=ledger.reserve(p.id,p.revision,observedMaximum(p.id));ledger(ledger);
            File target=path(key(p.id,p.revision));if(occupied(target))throw new IOException("修订身份已使用，不允许覆盖");write(target.getName(),p.encode());ledger.complete(p.id,p.revision);ledger(ledger);return p;
        }
    }
    ImportCheck checkImport(MapPatch incoming)throws IOException {synchronized(MapLibrary.class){
        MapRevisionLedger ledger=ledger();File file=path(key(incoming.id,incoming.revision));
        if(!ledger.deleted(incoming.id)&&present(file)){MapPatch known=readRevision(file.getName());return Arrays.equals(known.encode(),incoming.encode())?new ImportCheck(false,"该不可变版本已经存在；重复导入不会新增据点"):new ImportCheck(true,"同一地图ID及修订版本已有不同内容；请取消或明确作为新地图导入");}
        if(incoming.revision<=ledger.highest(incoming.id)||occupied(file))return new ImportCheck(true,"该地图修订身份已经使用或删除，不能复用；请取消或明确作为新地图导入");return new ImportCheck(false,"");
    }}
    String importConflict(MapPatch incoming)throws IOException {String message=checkImport(incoming).message();return message.isEmpty()?null:message;}
    void delete(String id)throws IOException {synchronized(MapLibrary.class){
        if(!MapRevisionLedger.validId(id))throw new IOException("地图ID错误");MapRevisionLedger ledger=ledger();int highest=Math.max(ledger.highest(id),observedMaximum(id));if(highest==0)return;
        // Hide every revision in one atomic operation before removing any physical file.
        ledger.hide(id,highest);ledger(ledger);List<String> failures=new ArrayList<>();
        for(String name:revisionKeys()){Matcher m=REVISION_FILE.matcher(name);if(!m.matches()||!m.group(1).equals(id))continue;File file=path(name);new AtomicFile(file).delete();File pending=new File(file+".new");if(pending.exists())pending.delete();if(occupied(file))failures.add(name);}
        if(!failures.isEmpty())throw new IOException("地图已隐藏且旧存档不受影响，但以下残留文件未能删除："+String.join("、",failures));
    }}
}
