package game.sanguo.mobile;

import android.content.Context;
import game.sanguo.core.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Versioned portable package, bounded ZIP reader and one atomic visible library transaction. */
final class CustomOfficerPack {
    static final int MAX_PACK=20*1024*1024;
    enum Conflict { SKIP, UPDATE_SAME_SOURCE, COPY }
    static final class Preview {
        final JSONObject root;final Map<String,byte[]> images;final String summary;
        Preview(JSONObject root,Map<String,byte[]> images,String summary){this.root=root;this.images=images;this.summary=summary;}
    }
    static byte[] exportPack(Context context,JSONObject root)throws IOException,JSONException{
        CustomOfficerLibrary.validate(root);JSONObject manifest=CustomOfficerLibrary.copy(root);manifest.put("packageVersion",1).put("baseCatalogRevision",ContentCatalog.get().revision);
        Set<String> names=new TreeSet<>();JSONArray entries=root.getJSONArray("entries");for(int i=0;i<entries.length();i++){String name=entries.getJSONObject(i).optString("portrait");if(name.endsWith(".png"))names.add(name);}
        byte[] json=manifest.toString().getBytes(StandardCharsets.UTF_8);if(json.length>CustomOfficerLibrary.MAX_BYTES)throw new IOException("清单超过16MiB限制");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();OutputStream bounded=new FilterOutputStream(bytes){
            private int size;
            @Override public void write(int b)throws IOException{if(size>=MAX_PACK)throw new IOException("数据包超过20MiB限制");out.write(b);size++;}
            @Override public void write(byte[] b,int off,int length)throws IOException{if(length>MAX_PACK-size)throw new IOException("数据包超过20MiB限制");out.write(b,off,length);size+=length;}
        };
        int unpacked=json.length;try(ZipOutputStream zip=new ZipOutputStream(bounded)){
            entry(zip,"manifest.json",json);for(String name:names){byte[] png=CustomOfficerImages.read(context,name);unpacked+=png.length;if(unpacked>MAX_PACK)throw new IOException("人物与头像合计超过20MiB限制");entry(zip,"portraits/"+name,png);}
        }return bytes.toByteArray();
    }
    private static void entry(ZipOutputStream zip,String name,byte[] bytes)throws IOException{ZipEntry e=new ZipEntry(name);e.setTime(0);zip.putNextEntry(e);zip.write(bytes);zip.closeEntry();}
    static Preview preview(Context c,InputStream input,CustomOfficerLibrary library)throws IOException,JSONException{
        byte[] bytes=CustomOfficerLibrary.readBounded(input,MAX_PACK);Map<String,byte[]> files=new LinkedHashMap<>();int total=0;
        if(bytes.length>0&&bytes[0]=='{')files.put("manifest.json",bytes);
        else try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes))){ZipEntry e;while((e=zip.getNextEntry())!=null){String name=e.getName();if(e.isDirectory()||!name.equals("manifest.json")&&!name.matches("portraits/[a-f0-9]{64}\\.png")||files.containsKey(name)||files.size()>1000)throw new IOException("ZIP包含非法/重复路径或过多文件");int max=name.equals("manifest.json")?CustomOfficerLibrary.MAX_BYTES:CustomOfficers.MAX_PORTRAIT;byte[] file=CustomOfficerLibrary.readBounded(zip,max);total+=file.length;if(total>MAX_PACK)throw new IOException("解压内容超过20MiB限制");files.put(name,file);zip.closeEntry();}}
        if(!files.containsKey("manifest.json"))throw new IOException("数据包缺少manifest.json");JSONObject root=new JSONObject(new String(files.remove("manifest.json"),StandardCharsets.UTF_8));
        if(root.getInt("packageVersion")!=1)throw new IOException("数据包版本不支持");CustomOfficerLibrary.validate(root);
        Map<String,byte[]> images=new HashMap<>();for(Map.Entry<String,byte[]> e:files.entrySet()){String name=e.getKey().substring(10);if(!name.equals(CustomOfficerImages.digest(e.getValue())+".png"))throw new IOException("头像摘要不匹配");CustomOfficerImages.validate(e.getValue());images.put(name,e.getValue());}
        int overlaps=0,overrides=0;JSONArray entries=root.getJSONArray("entries");for(int i=0;i<entries.length();i++){JSONObject o=entries.getJSONObject(i);if(library.find(o.getString("id"))!=null)overlaps++;if(o.optInt("targetId",-1)>=0)overrides++;String image=o.optString("portrait");if(image.endsWith(".png")&&!images.containsKey(image))throw new IOException("包内缺少头像："+o.getString("name"));}
        String warnings=root.optInt("baseCatalogRevision",-1)!=ContentCatalog.get().revision?"\n基础资料修订不同，已逐个校验历史覆盖目标ID和姓名。":"";
        Set<String> scenarios=new HashSet<>();for(ScenarioCatalog.Summary s:ScenarioCatalog.summaries())scenarios.add(s.id);JSONArray plans=root.getJSONArray("plans");for(int i=0;i<plans.length();i++)if(!scenarios.contains(plans.getJSONObject(i).getString("scenario")))warnings+="\n缺少剧本："+plans.getJSONObject(i).getString("scenario")+"（保留方案，开局时须解决）";
        return new Preview(root,images,"人物 "+entries.length()+" · 历史覆盖 "+overrides+" · 同ID冲突 "+overlaps+"\n头像 "+images.size()+" · 投放方案 "+plans.length()+warnings+"\n导入方案默认关闭；驻地与势力将在实际剧本启用时再次验证。同源库更新会替换该库方案；其他导入只补充缺少的剧本方案，已存在的同剧本方案保留，须手动添加新人物投放。");
    }
    static int apply(Context c,CustomOfficerLibrary library,Preview pack,Conflict policy)throws IOException,JSONException{
        JSONObject next=library.snapshot();JSONArray existing=next.getJSONArray("entries"),incoming=pack.root.getJSONArray("entries");JSONObject mappings=next.optJSONObject("imports");if(mappings==null){mappings=new JSONObject();next.put("imports",mappings);}
        String packId=pack.root.getString("libraryId");Map<String,String> ids=new HashMap<>();Map<String,Integer> positions=new HashMap<>();for(int i=0;i<existing.length();i++)positions.put(existing.getJSONObject(i).getString("id"),i);
        List<JSONObject> pending=new ArrayList<>();Set<String> skipped=new HashSet<>();int created=0;
        // Allocate the complete identity mapping BEFORE any relationship or placement rewriting.
        for(int i=0;i<incoming.length();i++){
            JSONObject src=incoming.getJSONObject(i),o=CustomOfficerLibrary.copy(src);String old=src.getString("id"),key=packId+":"+old+":"+policy.name();String remembered=mappings.optString(key,"");
            if(policy==Conflict.COPY&&!remembered.isEmpty()&&positions.containsKey(remembered)){ids.put(old,remembered);skipped.add(old);continue;}
            Integer position=positions.get(old);String target=old;
            if(position!=null&&policy==Conflict.SKIP){JSONObject current=existing.getJSONObject(position);if(!current.optString("origin",next.getString("libraryId")).equals(src.optString("origin",packId))||current.optInt("targetId",-1)!=src.optInt("targetId",-1))throw new IOException("同ID并非同一身份，不能将关系绑定到旧人物；请选择作为新人物导入："+src.getString("name"));ids.put(old,old);skipped.add(old);continue;}
            if(position!=null&&policy==Conflict.UPDATE_SAME_SOURCE){JSONObject current=existing.getJSONObject(position);String currentOrigin=current.optString("origin",next.getString("libraryId")),sourceOrigin=src.optString("origin",packId);if(!currentOrigin.equals(sourceOrigin)||current.optInt("targetId",-1)!=src.optInt("targetId",-1))throw new IOException("不是同源身份，不能更新："+src.getString("name"));o.put("runtimeId",current.getInt("runtimeId")).put("revision",Math.max(current.getInt("revision")+1,src.getInt("revision")));}
            else{
                if(policy==Conflict.COPY){target=UUID.randomUUID().toString();o.put("targetId",-1).put("baseName","");}
                o.put("runtimeId",next.getInt("nextId"));next.put("nextId",next.getInt("nextId")+1);o.put("revision",1);created++;
            }
            o.put("id",target).put("origin",src.optString("origin",packId));ids.put(old,target);mappings.put(key,target);o.put("_importOld",old);pending.add(o);
        }
        Map<String,String> historyCopies=new HashMap<>();Set<String> ambiguous=new HashSet<>();
        if(policy==Conflict.COPY)for(int i=0;i<incoming.length();i++){JSONObject src=incoming.getJSONObject(i);int target=src.optInt("targetId",-1);if(target>=0){String historical="h:"+target;if(historyCopies.put(historical,ids.get(src.getString("id")))!=null)ambiguous.add(historical);}}
        for(JSONObject o:pending){JSONArray links=o.getJSONArray("relationships");for(int j=0;j<links.length();j++){JSONObject link=links.getJSONObject(j);String target=link.getString("target");if(ids.containsKey(target))link.put("target",ids.get(target));else if(policy==Conflict.COPY&&historyCopies.containsKey(target)){if(ambiguous.contains(target))throw new IOException("包内多个历史覆盖对应同一关系目标，请先移除冲突覆盖再复制导入："+target);link.put("target",historyCopies.get(target));}}o.remove("_importOld");Integer at=positions.get(o.getString("id"));if(at==null){positions.put(o.getString("id"),existing.length());existing.put(o);}else existing.put(at,o);}
        JSONArray plans=pack.root.getJSONArray("plans");for(int i=0;i<plans.length();i++){
            JSONObject p=CustomOfficerLibrary.copy(plans.getJSONObject(i));p.put("enabled",false);JSONArray ps=p.getJSONArray("placements");for(int j=0;j<ps.length();j++){JSONObject placement=ps.getJSONObject(j);String target=ids.get(placement.getString("definition"));if(target==null)throw new IOException("投放引用映射缺失");placement.put("definition",target);if(policy==Conflict.COPY){placement.put("id",UUID.randomUUID().toString());if(placement.getString("mode").equals("KEEP"))placement.put("mode","WILD").put("owner",-1).put("loyalty",0);}}
            boolean already=false;JSONArray localPlans=next.getJSONArray("plans");for(int j=0;j<localPlans.length();j++)if(localPlans.getJSONObject(j).getString("scenario").equals(p.getString("scenario")))already=true;
            if(!already){p.put("id",UUID.randomUUID().toString());CustomOfficerSetup.putPlan(next,p);}else if(policy==Conflict.UPDATE_SAME_SOURCE&&packId.equals(next.getString("libraryId")))CustomOfficerSetup.putPlan(next,p);
        }
        CustomOfficerLibrary.validate(next);
        // Content-addressed assets are safe to retain if the visible atomic library commit fails.
        // Never delete a promoted asset: a concurrent writer or an existing campaign may share it.
        for(Map.Entry<String,byte[]> e:pack.images.entrySet()){File path=CustomOfficerImages.file(c,e.getKey());if(!path.exists())CustomOfficerImages.store(c,e.getValue());}
        library.replace(next);
        return pending.size();
    }
}
