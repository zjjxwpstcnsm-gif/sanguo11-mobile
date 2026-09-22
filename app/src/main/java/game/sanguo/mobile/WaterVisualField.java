package game.sanguo.mobile;

import game.sanguo.core.Hex;

/** Distance to the union of projected tile rectangles. Never uses storage q/r as world X/Z.
 * Exact silhouette intentionally protects every single-cell channel, island and port contact.
 * Smoothing is confined to material bands, not the gameplay mask or the pick surface.
 * Bounded support means local edits need no connected-component flood fill. */
final class WaterVisualField {
    static final int VERSION=1;
    static final float BAND=1.5f;
    private final MapSceneSnapshot.Ground ground;
    WaterVisualField(MapSceneSnapshot.Ground ground){this.ground=ground;}
    /** Principal local water extent, biased toward a fixed downstream reference.
     * Positive definite tensor keeps the direction in one hemisphere; no per-cell rotations.
     * Radius 2.4 plus the shore band's 1.5 fits inside the chunk's eight-cell halo. */
    float flowAngle(float x,float z){
        Hex center=ground.grid.cell(x,z);float xx=.2f,zz=.2f,xz=0;
        for(int r=center.r-4;r<=center.r+4;r++)for(int q=center.q-6;q<=center.q+6;q++){
            if(!ground.surface.water(q,r))continue;
            float dx=ground.grid.x(q,r)-x,dz=-(ground.grid.z(q,r)-z),d2=(dx*dx+dz*dz)/(2.4f*2.4f);
            if(d2>=1)continue;float k=(1-d2)*(1-d2)*(1-d2);
            xx+=dx*dx*k;zz+=dz*dz*k;xz+=dx*dz*k;
        }
        return (float)Math.atan2(.8f*xz+.6f*zz,.8f*xx+.6f*xz);
    }
    float distance(float x,float z){
        Hex center=ground.grid.cell(x,z);boolean wet=ground.surface.water(center);
        float best=BAND;
        for(int r=center.r-3;r<=center.r+3;r++)for(int q=center.q-5;q<=center.q+5;q++){
            if(ground.surface.water(q,r)==wet)continue;
            float dx=Math.max(0,Math.abs(x-ground.grid.x(q,r))-.5f);
            float dz=Math.max(0,Math.abs(z-ground.grid.z(q,r))-.5f);
            best=Math.min(best,(float)Math.sqrt(dx*dx+dz*dz));
        }
        return wet?best:-best;
    }
}
