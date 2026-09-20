package game.sanguo.mobile;

import game.sanguo.core.*;

/** Half-tile raster in the SAME projected world plane as MapView/MapOverview.
 * Every geographic tile paints 2x2 pixels; the extra odd-q/odd-r half-row is
 * explicit. No inferred land, padding or independently rounded source markers.
 * Pure Java so all source cells, edge pixels and legacy maps can be tested.
 */
final class MapRaster {
    static final float PIXEL=TileGeometry.DX/2;
    final World world;
    final int width,height;
    final float offset;
    MapRaster(World w){
        world=w;offset=w.sourceMapWidth>0?(w.height-1)/2:0;
        if(w.columnStaggered){width=2*w.sourceColumns();height=2*w.sourceRows()+(w.sourceColumns()>1?1:0);}
        else if(w.sourceMapWidth>0){width=2*w.sourceColumns()+(w.sourceRows()>1?1:0);height=2*w.sourceRows();}
        else {width=2*w.width+w.height-1;height=2*w.height;}
    }
    float worldX(Hex h){return TileGeometry.projectedX(h.q,h.r,offset,world.columnStaggered);}
    float worldY(Hex h){return TileGeometry.projectedY(h.q,h.r,offset,world.columnStaggered);}
    float rasterX(float wx){return wx/PIXEL+1;}
    float rasterY(float wy){return wy/PIXEL+1;}
    int left(Hex h){return Math.round(rasterX(worldX(h))-1);}
    int top(Hex h){return Math.round(rasterY(worldY(h))-1);}
    Hex at(float rx,float ry){
        if(!Float.isFinite(rx)||!Float.isFinite(ry)||rx<0||ry<0||rx>=width||ry>=height)return null;
        float wx=(rx-1)*PIXEL,wy=(ry-1)*PIXEL;
        int r=TileGeometry.projectedRow(wx,wy,world.columnStaggered);
        Hex h=new Hex(TileGeometry.projectedColumn(wx,wy,r,offset,world.columnStaggered),r);
        return world.inside(h)?h:null;
    }
}
