package game.sanguo.mobile;

import game.sanguo.core.*;
import game.sanguo.runtime.*;
import java.util.*;

/** Actual immutable projection + grid geometry + streamed-cache transitions. */
public final class NativeGrid129Test {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static boolean expected(World w,Hex h,int force){
        World.Terrain t=w.terrain[h.q][h.r];
        return w.inside(h)&&t!=World.Terrain.MOUNTAIN&&t!=World.Terrain.NON_NAVIGABLE_WATER&&!NationalMap.restricted(w,h)
            &&(w.campaign.has(force,Campaign.Tech.DIFFICULT_MARCH)||(t!=World.Terrain.SHALLOWS&&t!=World.Terrain.MOUNTAIN_PATH&&t!=World.Terrain.PLANK_ROAD));
    }
    private static int expectedCells(World w,int force,int q,int r){
        int cells=0;for(int rr=r;rr<Math.min(w.height,r+16);rr++)for(int qq=q;qq<Math.min(w.width,q+16);qq++)if(expected(w,new Hex(qq,rr),force))cells++;return cells;
    }
    private static void verify(World w,MapSceneSnapshot.Ground ground,int force){
        check(ground.gridForce==force&&ground.gridDifficultMarch==w.campaign.has(force,Campaign.Tech.DIFFICULT_MARCH),"detached force and authoritative technique bit");
        for(int r=0;r<w.height;r++)for(int q=0;q<w.width;q++){
            Hex h=new Hex(q,r);check(ground.gridCell(h,false)==expected(w,h,force),"every rendered cell matches exact tech/permanent restriction");
            check(ground.gridCell(h,true)==w.inside(h),"editor retains all valid cells");
            check((MapSceneSnapshot.gridTerrain(w.terrain[q][r],ground.gridDifficultMarch)&&!NationalMap.restricted(w,h))==expected(w,h,force),"2D uses identical terrain/force filter");
        }
        check(!ground.gridCell(new Hex(-1,0),true)&&!ground.gridCell(new Hex(w.width,0),false),"out-of-bounds never draws");
    }
    private static void geometry(World w,MapSceneSnapshot.Ground ground,List<SceneMesh> chunks){
        for(SceneMesh chunk:chunks){
            check(chunk.gridMatches(ground),"every new grid batch stamped for current capability");
            check(chunk.grid.indices.length==expectedCells(w,ground.gridForce,chunk.chunkQ,chunk.chunkR)*8*2*6,"grid has exactly two eight-edge ribbons per eligible cell, none for gated cells");
            // Each triangle's strict interior belongs to an eligible cell. Adjacent-cell
            // borders cannot leak a ribbon into the omitted terrain's interior.
            SceneMesh mesh=chunk.grid;
            for(int i=0;i<mesh.indices.length;i+=3){
                float x=0,z=0;for(int k=0;k<3;k++){int v=mesh.indices[i+k]*7;x+=mesh.vertices[v]/3;z+=mesh.vertices[v+2]/3;}
                Hex at=ground.shoreline.inverse(x,z).cell;check(ground.gridCell(at,false),"every grid triangle stays inside an eligible cell");
            }
        }
    }
    private static void transitions(boolean staggered)throws Exception{
        World w=NativeGrid129Fixture.world();w.columnStaggered=staggered;
        // Explicitly cover permanent VOID along with the ordinary land/naval family.
        w.terrain[0][15]=World.Terrain.VOID;
        byte[] before=SaveCodec.encode(w);MapSceneSnapshot.Ground blocked=new MapSceneSnapshot.Ground(w);verify(w,blocked,0);
        SceneMesh.TerrainWindow close=new SceneMesh.TerrainWindow(10,8,50,50,10),overview=new SceneMesh.TerrainWindow(10,8,50,50,60);
        List<SceneMesh> beforeClose=SceneMesh.ground(blocked,List.of(),close),beforeOverview=SceneMesh.ground(blocked,List.of(),overview);geometry(w,blocked,beforeClose);
        check(Arrays.equals(before,SaveCodec.encode(w)),"projection/grid generation leaves whole save/RNG untouched");
        NativeGrid129Fixture.learn(w,0);before=SaveCodec.encode(w);
        check(!blocked.matches(w)&&!blocked.matchesGridContext(w,0)&&blocked.matchesTerrain(w),"research invalidates grid context without invalidating surface data");
        MapSceneSnapshot.Ground learned=blocked.withGridContext(w,0);verify(w,learned,0);
        check(learned!=blocked&&learned.surface==blocked.surface&&learned.terrain==blocked.terrain&&learned.shoreline==blocked.shoreline,"research creates immutable context while reusing identical ground");
        check(!blocked.gridCell(new Hex(10,7),false)&&learned.gridCell(new Hex(10,7),false),"old snapshot remains blocked after research");
        List<SceneMesh> afterClose=SceneMesh.ground(learned,beforeClose,close);geometry(w,learned,afterClose);
        for(int i=0;i<afterClose.size();i++){
            SceneMesh old=beforeClose.get(i),next=afterClose.get(i);
            check(old!=next&&old.fingerprint!=next.fingerprint,"grid-bearing cache invalidates on technique change");
            check(!old.gridMatches(learned),"old GPU grid is rejected before async replacement arrives");
            check(Arrays.equals(old.vertices,next.vertices)&&Arrays.equals(old.indices,next.indices)&&Arrays.equals(old.surfaceData,next.surfaceData),"only grid changes; exact terrain mesh/material bytes retained");
        }
        List<SceneMesh> afterOverview=SceneMesh.ground(learned,beforeOverview,overview);
        for(int i=0;i<afterOverview.size();i++)check(beforeOverview.get(i)==afterOverview.get(i),"overview geometry cache reused across technology change");
        check(learned.withGridContext(w,0)==learned,"no-op projection reuses snapshot context");
        // AI playback must not borrow its temporary active side for the player's grid.
        w.active=1;check(learned.matchesGridContext(w,w.player),"AI active side cannot replace the viewing player");w.active=0;
        check(Arrays.equals(before,SaveCodec.encode(w)),"all learned-state projections preserve complete save/RNG");
        w.player=1;MapSceneSnapshot.Ground switched=learned.withGridContext(w,w.player);verify(w,switched,1);
        List<SceneMesh> switchClose=SceneMesh.ground(switched,afterClose,close);geometry(w,switched,switchClose);
        for(SceneMesh old:afterClose)check(!old.gridMatches(switched),"switch to unresearched force immediately rejects old unlocked grids");
        check(learned.gridCell(new Hex(10,7),false)&&!switched.gridCell(new Hex(10,7),false),"force switching cannot mutate previous snapshot");
        MapSceneSnapshot.Ground preview=switched.withGridContext(w,0);verify(w,preview,0);check(preview.gridForce==0&&w.player==1,"opening force preview projects explicitly without mutating player");
        World clone=SaveCodec.decode(SaveCodec.encode(w));check(switched.matches(clone),"identical detached world/save reuses correct force context");
        clone.terrain[10][7]=World.Terrain.MOUNTAIN;check(!switched.matchesTerrain(clone),"true terrain edit still invalidates geometry");
    }
    private static void sessionTransitions()throws Exception{
        try(GameSession session=new GameSession(NativeGrid129Fixture.researchReadyWorld())){
            World before=session.legacyView().draft;MapSceneSnapshot.Ground blocked=new MapSceneSnapshot.Ground(before);
            check(!blocked.gridDifficultMarch,"session projection starts with unfinished research");
            TurnTicket ticket=session.beginTurn();World computed=SaveCodec.decode(ticket.initial()),reference=SaveCodec.decode(ticket.initial());
            check(computed.nextTurn().ok&&reference.nextTurn().ok,"real authority-boundary turn computation succeeds");
            check(session.commitTurn(ticket,computed),"real turn ticket commits research");
            check(Arrays.equals(SaveCodec.encode(reference),session.captureSave()),"session research authority equals exact headless save/RNG");
            World after=session.legacyView().draft;check(blocked.matchesTerrain(after),"session revision/research retains unchanged terrain");
            MapSceneSnapshot.Ground learned=blocked.withGridContext(after,after.player);check(learned.gridDifficultMarch&&learned.surface==blocked.surface,"detached committed research updates capability without new surface");
            after.player=1;after.active=1;session.replace(after);World switched=session.legacyView().draft;
            MapSceneSnapshot.Ground other=learned.withGridContext(switched,switched.player);
            check(!other.gridDifficultMarch&&other.gridForce==1,"real session replacement projects unresearched player");
            check(learned.gridDifficultMarch&&!blocked.gridDifficultMarch,"all previous scene snapshots remain immutable across session changes");
        }
    }
    public static void main(String[] args)throws Exception{
        transitions(false);transitions(true);sessionTransitions();
        System.out.println("PASS NATIVE_GRID129 "+checks+" checks: 2D/3D cells, exact ribbon coverage, before/after/force/preview/AI context, immutable cache and save/RNG");
    }
}
