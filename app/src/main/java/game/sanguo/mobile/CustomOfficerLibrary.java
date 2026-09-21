package game.sanguo.mobile;

import android.content.Context;
import android.util.AtomicFile;
import game.sanguo.core.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** The single reusable officer library. Legacy .sgof files are migrated, never used as identities. */
final class CustomOfficerLibrary {
    static final int VERSION=1, MAX_ENTRIES=1000, MAX_BYTES=16*1024*1024;
    final Context context;
    private final AtomicFile file;
    private JSONObject data;
    CustomOfficerLibrary(Context context)throws IOException{
        this.context=context.getApplicationContext();
        File dir=new File(context.getFilesDir(),"customOfficers");
        if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("无法创建武将库目录");
        file=new AtomicFile(new File(dir,"library.json"));
        try{
            if(file.getBaseFile().exists()||new File(file.getBaseFile()+".bak").exists()){
                try(InputStream in=file.openRead()){data=new JSONObject(new String(readBounded(in,MAX_BYTES),StandardCharsets.UTF_8));}
                validate(data);
            }else{
                data=new JSONObject().put("format","sg11-custom-officers").put("version",VERSION)
                    .put("libraryId",UUID.randomUUID().toString()).put("nextId",100000).put("entries",new JSONArray()).put("plans",new JSONArray());
            }
            migrateLegacy();
        }catch(JSONException|IllegalArgumentException e){throw new IOException("武将库无效，原文件保留："+e.getMessage(),e);}
    }
    static byte[] readBounded(InputStream in,int max)throws IOException{
        if(in==null)throw new IOException("无法打开文件");
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n,total=0;
        while((n=in.read(buffer))!=-1){total+=n;if(total>max)throw new IOException("文件超过大小限制");out.write(buffer,0,n);}return out.toByteArray();
    }
    static JSONObject copy(JSONObject value)throws JSONException{return new JSONObject(value.toString());}
    List<JSONObject> entries()throws JSONException{List<JSONObject> result=new ArrayList<>();JSONArray values=data.getJSONArray("entries");for(int i=0;i<values.length();i++)result.add(copy(values.getJSONObject(i)));return result;}
    JSONObject snapshot()throws JSONException{return copy(data);}
    JSONObject find(String id)throws JSONException{for(JSONObject value:entries())if(value.getString("id").equals(id))return value;return null;}
    static Editor.Template template(JSONObject o)throws JSONException{
        JSONArray s=o.getJSONArray("stats"),a=o.getJSONArray("aptitudes");int[] stats=new int[5],apt=new int[6];
        if(s.length()!=5||a.length()!=6)throw new IllegalArgumentException("五维或适性数量无效");
        for(int i=0;i<5;i++)stats[i]=s.getInt(i);for(int i=0;i<6;i++)apt[i]=a.getInt(i);
        Editor.Template t=new Editor.Template(o.getString("name"),stats,apt,World.Sex.valueOf(o.getString("sex")),o.getString("skill"),Debate.Temper.valueOf(o.getString("temper")),o.optInt("talkMask",0));t.validate();return t;
    }
    static JSONObject definition(Editor.Template t)throws JSONException{
        t.validate();JSONArray s=new JSONArray(),a=new JSONArray();for(int i=0;i<5;i++)s.put(t.stat(i));for(int i=0;i<6;i++)a.put(t.aptitude(i));
        return new JSONObject().put("name",t.name).put("stats",s).put("aptitudes",a).put("sex",t.sex.name()).put("skill",t.skill).put("temper",t.temper.name()).put("talkMask",t.talkMask)
            .put("targetId",-1).put("portrait","").put("affinity",75).put("birth",160).put("appearance",180).put("death",240).put("relationships",new JSONArray());
    }
    synchronized JSONObject save(JSONObject draft)throws IOException,JSONException{
        JSONObject next=copy(data),entry=copy(draft);JSONArray values=next.getJSONArray("entries");String id=entry.optString("id","");int at=-1;
        for(int i=0;i<values.length();i++)if(values.getJSONObject(i).getString("id").equals(id)){at=i;break;}
        if(at<0){if(values.length()>=MAX_ENTRIES)throw new IOException("模板库最多"+MAX_ENTRIES+"人");entry.put("id",UUID.randomUUID().toString());entry.put("revision",1);entry.put("runtimeId",next.getInt("nextId"));next.put("nextId",Math.addExact(next.getInt("nextId"),1));values.put(entry);}
        else{JSONObject old=values.getJSONObject(at);if(entry.getInt("revision")!=old.getInt("revision"))throw new IOException("模板已变化，请重新打开后编辑");entry.put("id",old.getString("id")).put("runtimeId",old.getInt("runtimeId")).put("revision",Math.addExact(old.getInt("revision"),1));values.put(at,entry);}
        commit(next);return copy(entry);
    }
    synchronized void replace(JSONObject next)throws IOException,JSONException{commit(copy(next));}
    private void commit(JSONObject next)throws IOException,JSONException{
        validate(next);byte[] bytes=next.toString().getBytes(StandardCharsets.UTF_8);if(bytes.length>MAX_BYTES)throw new IOException("武将库过大");
        FileOutputStream out=null;try{out=file.startWrite();out.write(bytes);file.finishWrite(out);data=next;}catch(IOException e){if(out!=null)file.failWrite(out);throw e;}
    }
    static void validate(JSONObject root)throws JSONException{
        if(!"sg11-custom-officers".equals(root.getString("format"))||root.getInt("version")!=VERSION)throw new IllegalArgumentException("不支持的武将库格式版本");
        UUID.fromString(root.getString("libraryId"));JSONArray values=root.getJSONArray("entries");if(values.length()>MAX_ENTRIES)throw new IllegalArgumentException("武将数量超限");
        Set<String> ids=new HashSet<>();Set<Integer> runtimeIds=new HashSet<>();int max=99999;
        for(int i=0;i<values.length();i++){
            JSONObject o=values.getJSONObject(i);template(o);String id=o.getString("id");UUID.fromString(id);
            int rid=o.getInt("runtimeId");if(!ids.add(id)||!runtimeIds.add(rid)||rid<100000||rid>1000000||o.getInt("revision")<1)throw new IllegalArgumentException("人物ID重复或非法");max=Math.max(max,rid);
            int affinity=o.optInt("affinity",75);if(affinity<0||affinity>149)throw new IllegalArgumentException("相性须为0—149");
            String portrait=o.optString("portrait","");if(!portrait.isEmpty()&&!portrait.matches("(?:builtin:[A-Za-z0-9_-]+|[a-f0-9]{64}\\.png)"))throw new IllegalArgumentException("头像引用无效");
        }
        if(root.getInt("nextId")<=max||root.getInt("nextId")>1000001)throw new IllegalArgumentException("ID分配器无效");root.getJSONArray("plans");
    }
    private void migrateLegacy()throws IOException,JSONException{
        if(data.optBoolean("legacyMigrated",false))return;
        File[] old=new File(context.getFilesDir(),"officer-templates").listFiles((d,n)->n.endsWith(".sgof"));
        JSONObject next=copy(data);JSONArray values=next.getJSONArray("entries");
        if(old!=null){Arrays.sort(old,Comparator.comparing(File::getName));for(File path:old){
            try(InputStream in=new FileInputStream(path)){
                JSONObject entry=definition(OfficerTemplateCodec.read(in));int id=next.getInt("nextId");
                entry.put("id",UUID.nameUUIDFromBytes(("legacy:"+path.getName()).getBytes(StandardCharsets.UTF_8)).toString()).put("runtimeId",id).put("revision",1);
                values.put(entry);next.put("nextId",id+1);
            }
        }}
        next.put("legacyMigrated",true);commit(next);
    }
}
