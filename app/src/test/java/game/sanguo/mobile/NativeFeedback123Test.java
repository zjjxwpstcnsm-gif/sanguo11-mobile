package game.sanguo.mobile;
import java.nio.file.*;
import java.util.*;
import org.json.*;

/** Real decoder/rig tests. Host allocations/timing are not device frame rate. */
public final class NativeFeedback123Test {
 static int checks;
 static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception{
  FieldAssets assets=new FieldAssets(name->Files.newInputStream(Path.of("app/src/main/assets/3d/field",name)));
  JSONObject manifest=new JSONObject(Files.readString(Path.of("docs/native-pc-visual/feedback-v123-blender-assets.json")));
  long vertices=0,expanded=0,bytes=0;
  for(Object item:manifest.getJSONArray("assets")){
   JSONObject a=(JSONObject)item;Path p=Path.of(a.getString("path"));SceneMesh m;
   try(var in=Files.newInputStream(p)){m=SiteGlb.read(in);}
   check(m.vertices.length/7==a.getInt("vertices"),"manifest vertex count");
   check(m.indices.length/3==a.getInt("triangles"),"all authored triangles retained");
   check(m.vertices.length/7<=a.getInt("expandedVertices"),"compaction never expands");
   vertices+=m.vertices.length/7;expanded+=a.getInt("expandedVertices");bytes+=Files.size(p);
   if(!p.toString().contains("/field/"))continue;
   String model=p.getFileName().toString().replace(".glb","");SceneMesh rest=assets.mesh(model);float[] original=rest.vertices.clone();
   for(String clip:new String[]{"idle","turn","walk","prepare","attack","hit","defeat","enter"})for(int frame=0;frame<12;frame++){
    SceneMesh pose=assets.pose(model,clip,frame,1);
    check(pose.indices==rest.indices&&pose.uv==rest.uv,"immutable topology and UV shared");
    check(pose.vertices!=rest.vertices,"posed positions never alias rest");
    for(float v:pose.vertices)check(Float.isFinite(v),"finite articulated pose");
    for(int i:pose.indices)check(i>=0&&i*7<pose.vertices.length,"valid remapped index");
   }
   check(Arrays.equals(original,rest.vertices),"all clips preserve rest vertices");
  }
  check(vertices<expanded*.8,"meaningful vertex reduction despite added detail");
  System.out.println("PASS FEEDBACK123 checks="+checks+" vertices="+vertices+" expanded="+expanded+" glbBytes="+bytes);
 }
}
