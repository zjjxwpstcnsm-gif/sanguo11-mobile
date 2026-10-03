package game.sanguo.core;
import game.sanguo.core.map.SourceGridCoord;
import game.sanguo.core.map.MapBrushGeometry;

import java.io.*;
import java.util.*;

/** Isolated editing transactions. Strokes are collected without mutating the world, then
 * committed once. History stores changed entries, not 40,000-cell world snapshots. */
public final class MapEditSession {
    public static final int HISTORY_BYTES=8*1024*1024, HISTORY_OPERATIONS=100;
    private MapPatch patch;
    private World world;
    private final ArrayDeque<Change> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    private long historyBytes;
    private long generation;
    private String savedFingerprint;
    private Set<Hex> protectedCells;
    private record Change(String label,Map<Integer,MapPatch.Cell> before,Map<Integer,MapPatch.Cell> after,MapPatch oldMetadata,MapPatch newMetadata,long bytes){}
    public MapEditSession(MapPatch initial)throws IOException {patch=MapPatch.decode(initial.encode());world=CustomMaps.preview(patch,patch.preview);savedFingerprint=patch.fingerprint();rebuildProtection();}
    @FunctionalInterface public interface DraftWriter { void write(MapPatch value)throws IOException; }
    /** Publish an edit and its history only if durable draft storage succeeds. The editor
     * holds this monitor throughout; renderers keep their preceding immutable projection. */
    public synchronized <T> T persist(java.util.concurrent.Callable<T> edit,DraftWriter writer)throws Exception {
        MapPatch previous=patch;World previousWorld=world;
        ArrayDeque<Change> oldUndo=new ArrayDeque<>(undo),oldRedo=new ArrayDeque<>(redo);
        long oldBytes=historyBytes,oldGeneration=generation;String oldSaved=savedFingerprint;
        try {
            T result=edit.call();MapPatch next=patch.copy();writer.write(next);
            savedFingerprint=next.fingerprint();return result;
        } catch(Exception failure) {
            patch=previous;world=previousWorld;undo.clear();undo.addAll(oldUndo);redo.clear();redo.addAll(oldRedo);
            historyBytes=oldBytes;generation=oldGeneration;savedFingerprint=oldSaved;rebuildProtection();
            throw failure;
        }
    }
    public synchronized MapPatch patch(){return patch.copy();}
    public synchronized World world(){return world;}
    public synchronized long generation(){return generation;}
    public synchronized boolean dirty(){return !Objects.equals(savedFingerprint,patch.fingerprint());}
    public synchronized void saved(String fingerprint){savedFingerprint=fingerprint;}
    public synchronized String undoLabel(){return undo.isEmpty()?"":undo.peekLast().label;}
    public synchronized String redoLabel(){return redo.isEmpty()?"":redo.peekLast().label;}
    public synchronized long historyBytes(){return historyBytes;}
    public synchronized boolean protectedAt(Hex h){return !world.inside(h)||NationalMap.restricted(world,h)||protectedCells.contains(h);}
    private void rebuildProtection(){protectedCells=new HashSet<>();for(World.City c:world.cities){protectedCells.addAll(SiteFootprint.cells(c));if(c.kind!=World.SiteKind.CITY)protectedCells.addAll(c.hex.neighbors());protectedCells.addAll(world.development.parcels(c.id));}
        for(Domestic.Facility f:world.domestic.facilities)protectedCells.add(f.hex);for(War.Structure f:world.war.structures)protectedCells.add(f.hex);for(World.Unit u:world.fieldUnits())protectedCells.add(u.hex);}
    /** Exact axial interpolation, filling every neighbor step even at sparse touch sampling. */
    public static List<Hex> line(Hex a,Hex b){return MapBrushGeometry.line(a,b);}
    public static Set<Hex> disk(Hex center,int radius){if(radius<0||radius>2)throw new IllegalArgumentException("画笔半径必须是0、1或2");return MapBrushGeometry.disk(center,radius);}
    public synchronized Set<Hex> stroke(Hex from,Hex to,int radius){Set<Hex> out=new LinkedHashSet<>();for(Hex h:line(from,to))for(Hex at:disk(h,radius))if(!protectedAt(at))out.add(at);return out;}
    public synchronized Set<Hex> rectangle(Hex first,Hex last){Set<Hex> out=new LinkedHashSet<>();SourceGridCoord a=MapCoordinates.nationalSource(world,first),b=MapCoordinates.nationalSource(world,last);for(int x=Math.max(0,Math.min(a.x,b.x));x<=Math.min(199,Math.max(a.x,b.x));x++)for(int y=Math.max(0,Math.min(a.y,b.y));y<=Math.min(199,Math.max(a.y,b.y));y++){Hex h=MapCoordinates.fromNationalSource(world,new SourceGridCoord(x,y));if(!protectedAt(h))out.add(h);}return out;}
    public synchronized Set<Hex> fill(Hex start){Set<Hex> out=new LinkedHashSet<>();if(protectedAt(start))return out;World.Terrain target=world.terrain[start.q][start.r];ArrayDeque<Hex> q=new ArrayDeque<>();out.add(start);q.add(start);while(!q.isEmpty()){Hex h=q.removeFirst();for(Hex next:h.neighbors())if(!protectedAt(next)&&world.terrain[next.q][next.r]==target&&out.add(next))q.add(next);}return out;}
    public synchronized Set<Hex> differences(Collection<Hex> hexes,World.Terrain terrain){Set<Hex> result=new LinkedHashSet<>();if(terrain==World.Terrain.VOID)return result;for(Hex h:hexes)if(!protectedAt(h)&&world.terrain[h.q][h.r]!=terrain)result.add(h);return result;}
    public synchronized int paint(String label,Collection<Hex> hexes,World.Terrain terrain)throws IOException {
        if(terrain==World.Terrain.VOID)throw new IOException("VOID是地图边界，不是可绘制地形");Map<Integer,MapPatch.Cell> before=new TreeMap<>(),after=new TreeMap<>();for(Hex h:differences(hexes,terrain)){SourceGridCoord source=MapCoordinates.nationalSource(world,h);int key=source.x*200+source.y;MapPatch.Cell old=patch.terrain.get(key);before.put(key,old);World.Terrain base=CustomMaps.base().terrain(source.x,source.y);after.put(key,base==terrain?null:new MapPatch.Cell(source.x,source.y,base,terrain));}
        if(before.isEmpty())return 0;
        MapPatch next=patch.copy();for(var e:after.entrySet()){if(e.getValue()==null)next.terrain.remove(e.getKey());else next.terrain.put(e.getKey(),e.getValue());next.heights.remove(e.getKey());}
        replace(label,next);return before.size();
    }
    /** Caller constructs and previews a complete entity/reference transaction first. */
    public synchronized void replace(String label,MapPatch next)throws IOException {
        MapPatch clean=MapPatch.decode(next.encode());World candidate=CustomMaps.preview(clean,clean.preview);
        if(Arrays.equals(patch.encode(),clean.encode()))return;
        Map<Integer,MapPatch.Cell> before=new TreeMap<>(),after=new TreeMap<>();Set<Integer> keys=new TreeSet<>(patch.terrain.keySet());keys.addAll(clean.terrain.keySet());for(int key:keys)if(!Objects.equals(patch.terrain.get(key),clean.terrain.get(key))){before.put(key,patch.terrain.get(key));after.put(key,clean.terrain.get(key));}
        MapPatch oldMeta=metadata(patch),newMeta=metadata(clean);long bytes=128L+before.size()*96L+oldMeta.encode().length+newMeta.encode().length;
        Change change=new Change(label,before,after,oldMeta,newMeta,bytes);patch=clean;world=candidate;rebuildProtection();record(change);
    }
    private static MapPatch metadata(MapPatch source){MapPatch p=source.copy();p.terrain.clear();return p;}
    private void record(Change change){for(Change c:redo)historyBytes-=c.bytes;redo.clear();undo.addLast(change);historyBytes+=change.bytes;generation++;while(historyBytes>HISTORY_BYTES||undo.size()>HISTORY_OPERATIONS){Change first=undo.removeFirst();historyBytes-=first.bytes;if(undo.isEmpty())break;}}
    private void applyTerrain(Map<Integer,MapPatch.Cell> values)throws IOException {for(var e:values.entrySet()){int x=e.getKey()/200,y=e.getKey()%200;MapPatch.Cell c=e.getValue();if(c==null)patch.terrain.remove(e.getKey());else patch.terrain.put(e.getKey(),c);Hex h=MapCoordinates.fromNationalSource(world,new SourceGridCoord(x,y));world.terrain[h.q][h.r]=c==null?CustomMaps.base().terrain(x,y):c.after();}world.terrainRevision++;world.customMapFingerprint=patch.logicalFingerprint();world.visualMap=patch.copy();}
    private void apply(Change change,boolean forward)throws IOException {Map<Integer,MapPatch.Cell> values=forward?change.after:change.before;MapPatch meta=forward?change.newMetadata:change.oldMetadata;if(meta==null)applyTerrain(values);else{MapPatch next=meta.copy();next.terrain.putAll(patch.terrain);for(var e:values.entrySet())if(e.getValue()==null)next.terrain.remove(e.getKey());else next.terrain.put(e.getKey(),e.getValue());World resolved=CustomMaps.preview(next,next.preview);patch=next;world=resolved;rebuildProtection();}generation++;}
    public synchronized void undo()throws IOException {if(undo.isEmpty())return;Change c=undo.peekLast();apply(c,false);undo.removeLast();redo.addLast(c);}
    public synchronized void redo()throws IOException {if(redo.isEmpty())return;Change c=redo.peekLast();apply(c,true);redo.removeLast();undo.addLast(c);}
    public synchronized int newId()throws IOException {Set<Integer> ids=CustomMaps.sites(patch).keySet();for(int tries=0;tries<1000;tries++){int id=100000000+new java.security.SecureRandom().nextInt(1900000000);if(!ids.contains(id)&&!CustomMaps.base().sites.containsKey(id))return id;}throw new IOException("无法分配唯一据点ID");}
    public synchronized MapPatch putSite(MapPatch.Site value)throws IOException {MapPatch next=patch.copy();String error=CustomMaps.placement(world,value,value.id());if(error!=null)throw new IOException(error);next.putSite(CustomMaps.base().sites.get(value.id()),value);CustomMaps.preview(next,next.preview);return next;}
    public synchronized MapPatch initial(int site,MapPatch.Initial state)throws IOException {MapPatch next=patch.copy();next.scenarios.computeIfAbsent(next.preview,k->new TreeMap<>()).put(site,state);World.City old=world.city(site);if(old!=null&&(!state.enabled()||old.owner!=state.owner())){World.City target=CustomMaps.relocation(world,site,-1);if(target!=null)next.redirects.computeIfAbsent(next.preview,k->new TreeMap<>()).put(site,target.id);}
        CustomMaps.preview(next,next.preview);return next;}
}
