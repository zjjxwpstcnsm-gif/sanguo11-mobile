package game.sanguo.mobile;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import game.sanguo.core.*;

/** Small original silhouettes in tile coordinates; no textures, downloads or 3D asset dependency. */
final class MapModels {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path shape=new Path();
    private static final int LIGHT=0xffead9ad,DARK=0xff233333,WOOD=0xff987145,STEEL=0xffb7c8cb,FIRE=0xffff9c47;
    private void box(Canvas c,float l,float t,float r,float b,int color){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRect(l,t,r,b,p);}
    private void disc(Canvas c,float x,float y,float r,int color){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawCircle(x,y,r,p);}
    private void line(Canvas c,float x,float y,float xx,float yy,float width,int color){p.setColor(color);p.setStrokeWidth(width);c.drawLine(x,y,xx,yy,p);}
    private void poly(Canvas c,int color,float... xy){shape.reset();shape.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)shape.lineTo(xy[i],xy[i+1]);shape.close();p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawPath(shape,p);}
    private void base(Canvas c,int color){p.setColor(0x900f2025);c.drawOval(-20,7,21,18,p);poly(c,color,-19,8,0,0,19,8,0,17);}
    private void house(Canvas c,int color,float x,float y,float w,float h){
        box(c,x-w,y-h,x+w,y,LIGHT);poly(c,WOOD,x+w,y-h,x+w+5,y-h-3,x+w+5,y-3,x+w,y);
        poly(c,color,x-w-3,y-h,x,y-h-9,x+w+5,y-h-3,x+w,y-h+2);box(c,x-2,y-6,x+2,y,DARK);
        line(c,x-w-3,y-h,x,y-h-9,1,LIGHT);line(c,x,y-h-9,x+w+5,y-h-3,1,DARK);
        if(w>9){box(c,x-w+3,y-7,x-w+6,y-3,WOOD);box(c,x+w-6,y-7,x+w-3,y-3,WOOD);}
    }
    private void flag(Canvas c,int color,float x,float y){line(c,x,y,x,y+17,1.2f,WOOD);poly(c,color,x,y,x+9,y+2,x,y+7);}
    private void wheels(Canvas c){disc(c,-10,12,4,DARK);disc(c,10,12,4,DARK);disc(c,-10,12,2,STEEL);disc(c,10,12,2,STEEL);box(c,-14,5,15,10,WOOD);}
    private void bow(Canvas c,float x,float y,boolean cross){line(c,x-5,y+6,x+1,y-1,2,WOOD);line(c,x+1,y-1,x+6,y+6,2,WOOD);line(c,x-5,y+6,x+6,y+6,1,LIGHT);if(cross)line(c,x,y+11,x,y-5,2,STEEL);}
    private void soldier(Canvas c,int color,float x,float y,World.Weapon weapon){
        line(c,x-2,y+5,x-3,y+11,2,DARK);line(c,x+2,y+5,x+3,y+11,2,DARK);
        box(c,x-4,y-2,x+4,y+6,color);disc(c,x,y-6,3,LIGHT);poly(c,STEEL,x-4,y-7,x,y-11,x+4,y-7);
        if(weapon==World.Weapon.CROSSBOW){bow(c,x+5,y-1,true);return;}
        line(c,x+6,y+9,x+6,y-15,1.5f,WOOD);
        if(weapon==World.Weapon.HALBERD)poly(c,STEEL,x+6,y-15,x+13,y-9,x+6,y-6);
        else if(weapon==World.Weapon.SWORD){box(c,x+5,y-12,x+7,y,STEEL);disc(c,x-5,y+2,4,STEEL);}
        else poly(c,STEEL,x+4,y-12,x+6,y-18,x+8,y-12);
    }
    private void horse(Canvas c,int color){
        poly(c,WOOD,-14,0,4,-2,9,-12,16,-10,17,-4,10,-2,8,6,-10,6);
        line(c,-10,5,-12,14,3,WOOD);line(c,6,4,9,14,3,WOOD);line(c,-13,0,-18,5,2,WOOD);
        box(c,-5,-4,4,2,color);disc(c,13,-9,1,DARK);poly(c,WOOD,10,-12,11,-17,14,-11);
    }
    private void siege(Canvas c,World.Weapon weapon,int color){
        wheels(c);
        switch(weapon){
            case RAM:house(c,color,0,4,12,10);box(c,-18,3,16,6,WOOD);disc(c,16,4,3,STEEL);break;
            case SIEGE_TOWER:box(c,-10,-18,10,5,WOOD);box(c,-12,-19,12,-14,color);box(c,-12,-6,12,-2,color);for(int y=-14;y<4;y+=4)line(c,-3,y,4,y,1,LIGHT);flag(c,color,7,-29);break;
            case WOODEN_BEAST:poly(c,color,-15,5,-10,-9,9,-10,17,-4,14,7);poly(c,STEEL,9,-10,18,-13,20,-4,11,-1);disc(c,15,-8,1,DARK);line(c,20,-3,24,-2,2,FIRE);break;
            case CATAPULT:poly(c,color,-9,5,-1,-12,9,5);line(c,-7,7,12,-19,3,WOOD);disc(c,12,-19,4,STEEL);line(c,-8,3,12,-17,1,LIGHT);break;
            default:break;
        }
    }
    private void ship(Canvas c,int color,int decks,boolean fire){
        poly(c,WOOD,-21,4,18,4,11,14,-12,14);line(c,-15,1,15,1,2,LIGHT);
        if(decks>0)house(c,color,0,3,8,7+decks*4);
        line(c,-5,4,-5,-24,2,WOOD);poly(c,fire?FIRE:LIGHT,-4,-23,10,-17,-4,-8);
        if(decks>1){line(c,12,4,12,-20,1.5f,WOOD);poly(c,color,12,-20,22,-14,12,-7);}
        line(c,-18,16,-8,16,1,STEEL);line(c,5,18,18,18,1,STEEL);
    }
    void unit(Canvas c,World.Unit u,boolean water,int color){
        base(c,color);
        if(water){ship(c,color,u.ship.ordinal(),false);return;}
        if(Army.siegeWeapon(u.weapon)){siege(c,u.weapon,color);return;}
        if(u.weapon==World.Weapon.CAVALRY){horse(c,color);disc(c,0,-11,3,LIGHT);box(c,-3,-8,3,-1,color);line(c,5,0,8,-21,2,STEEL);return;}
        soldier(c,color,-8,-5,u.weapon);soldier(c,color,6,0,u.weapon);soldier(c,color,-5,9,u.weapon);
    }
    void weaponIcon(Canvas c,World.Weapon weapon,int color){
        if(Army.siegeWeapon(weapon)){base(c,color);siege(c,weapon,color);return;}
        if(weapon==World.Weapon.CAVALRY){base(c,color);horse(c,color);return;}
        if(weapon==World.Weapon.CROSSBOW){bow(c,0,-5,true);line(c,0,14,0,-23,2,STEEL);poly(c,STEEL,-4,-19,0,-26,4,-19);return;}
        if(weapon==World.Weapon.SWORD){poly(c,STEEL,-3,9,-4,-19,0,-27,4,-19,3,9);box(c,-9,7,9,10,WOOD);box(c,-2,10,2,21,WOOD);return;}
        line(c,-7,20,4,-21,3,WOOD);poly(c,STEEL,0,-19,6,-29,8,-17);
        if(weapon==World.Weapon.HALBERD)poly(c,STEEL,6,-22,17,-14,14,-4,7,-8,11,-14,4,-14);
        else {line(c,10,20,-1,-18,2,WOOD);poly(c,STEEL,-5,-16,-4,-25,3,-19);}
    }
    void shipIcon(Canvas c,Army.Ship kind,int color){ship(c,color,kind.ordinal(),false);}
    void scaffolding(Canvas c){
        for(int x=-21;x<=21;x+=14){line(c,x,-17,x,17,1.5f,WOOD);line(c,x,-17,x+8,-23,1,WOOD);}
        line(c,-21,0,21,0,2,LIGHT);line(c,-21,12,21,12,2,LIGHT);line(c,-21,11,21,-15,1,WOOD);
    }
    void city(Canvas c,World.SiteKind kind,int color){
        if(BuildingAtlas.draw(c,kind==World.SiteKind.CITY?0:kind==World.SiteKind.GATE?1:2)){flag(c,color,21,-27);return;}
        base(c,color);
        if(kind==World.SiteKind.PORT){box(c,-20,7,18,12,WOOD);for(int x=-16;x<=16;x+=8)line(c,x,8,x,19,2,WOOD);ship(c,color,0,false);house(c,color,-10,-3,7,9);return;}
        poly(c,0xff70827e,-21,-3,19,-3,24,1,24,12,0,21,-21,12);box(c,-19,-3,19,12,LIGHT);box(c,-5,2,5,12,DARK);
        if(kind==World.SiteKind.GATE){house(c,color,-13,4,5,13);house(c,color,13,4,5,13);box(c,-7,-6,7,-2,WOOD);}
        else {house(c,color,-7,-6,9,9);house(c,color,8,0,10,14);for(int x=-18;x<=14;x+=6)box(c,x,-6,x+4,-1,STEEL);house(c,color,-17,6,4,8);house(c,color,18,6,4,8);}
        flag(c,color,13,-30);
    }
    void facility(Canvas c,Domestic.Kind kind,int color){
        int sprite=-1;switch(kind){case FARM:sprite=3;break;case MARKET:sprite=4;break;case GRANARY:sprite=5;break;case BARRACKS:sprite=6;break;case SMITH:sprite=7;break;case STABLE:sprite=8;break;case WORKSHOP:sprite=9;break;case SHIPYARD:sprite=10;break;case MINT:sprite=11;break;default:break;}
        if(BuildingAtlas.draw(c,sprite)){flag(c,color,21,-20);return;}
        base(c,color);
        switch(kind){
            case FARM:poly(c,0xff7b9c4c,-18,-5,10,-10,20,7,-9,14);for(int y=-3;y<12;y+=5)line(c,-14,y,15,y-3,2,LIGHT);break;
            case MARKET:case BLACK_MARKET:
                box(c,-14,-1,14,10,WOOD);poly(c,kind==Domestic.Kind.BLACK_MARKET?DARK:color,-18,-3,-12,-14,12,-14,18,-3);
                for(int x=-12;x<=12;x+=6)line(c,x,-13,x,-3,3,LIGHT);disc(c,-7,2,3,FIRE);disc(c,3,2,3,LIGHT);break;
            case GRANARY:house(c,color,0,9,15,11);box(c,-13,-7,-4,7,WOOD);box(c,4,-7,13,7,WOOD);poly(c,LIGHT,-15,-7,-8,-14,-2,-7);poly(c,LIGHT,2,-7,8,-14,15,-7);break;
            case STABLE:house(c,color,0,8,15,13);c.save();c.scale(.7f,.7f);horse(c,color);c.restore();break;
            case SMITH:house(c,color,-4,10,12,15);box(c,9,-23,15,-1,WOOD);poly(c,STEEL,-8,1,8,1,4,5,5,10,-5,10,-4,5);disc(c,12,-25,3,0xffaaaaaa);break;
            case MINT:house(c,color,0,10,15,16);for(int x=-8;x<=8;x+=8){disc(c,x,1,5,0xffe7bd5d);box(c,x-1,-1,x+1,3,DARK);}break;
            case WORKSHOP:house(c,color,0,10,14,16);disc(c,0,1,7,STEEL);disc(c,0,1,3,DARK);for(int i=0;i<4;i++){c.save();c.rotate(i*90,0,1);box(c,-2,-10,2,-5,STEEL);c.restore();}break;
            case SHIPYARD:box(c,-19,9,19,13,WOOD);ship(c,color,0,false);line(c,-17,-18,-17,9,2,WOOD);line(c,-17,-18,12,-18,2,WOOD);line(c,12,-18,12,-5,1,LIGHT);break;
            case BRONZE_TERRACE:house(c,color,0,11,17,6);house(c,color,0,0,11,8);poly(c,0xffa9bb80,-10,-21,0,-18,10,-25,4,-16,-4,-14);break;
            case BARRACKS:tent(c,color);flag(c,color,13,-27);line(c,-15,7,-10,-15,2,STEEL);line(c,-9,7,-16,-15,2,STEEL);break;
        }
    }
    private void tent(Canvas c,int color){poly(c,color,-18,10,0,-17,18,10);poly(c,LIGHT,0,-17,4,10,18,10);poly(c,DARK,-4,10,0,-1,4,10);}
    void structure(Canvas c,War.StructureKind kind,int color){
        int sprite=kind==War.StructureKind.FORTRESS?12:kind==War.StructureKind.ARROW_TOWER?13:kind==War.StructureKind.MUSIC?14:kind==War.StructureKind.DRUM?15:-1;
        if(BuildingAtlas.draw(c,sprite)){flag(c,color,21,-27);return;}
        base(c,color);
        switch(kind){
            case CAMP:tent(c,color);break;
            case FORT:case FORTRESS:
                house(c,color,0,6,kind==War.StructureKind.FORT?11:16,15);box(c,-18,4,18,11,STEEL);for(int x=-18;x<=16;x+=7)box(c,x,0,x+4,6,LIGHT);if(kind==War.StructureKind.FORTRESS){house(c,color,-13,2,4,11);house(c,color,13,2,4,11);}break;
            case ARROW_TOWER:case CROSSBOW_TOWER:case CATAPULT_TOWER:
                line(c,-8,-9,-13,12,3,WOOD);line(c,8,-9,13,12,3,WOOD);line(c,-10,8,8,-7,2,LIGHT);box(c,-13,-12,13,-6,color);
                if(kind==War.StructureKind.CATAPULT_TOWER){line(c,-8,-9,10,-25,3,WOOD);disc(c,10,-25,4,STEEL);}else{bow(c,0,-22,true);if(kind==War.StructureKind.CROSSBOW_TOWER){bow(c,-8,-18,true);bow(c,8,-18,true);}}break;
            case MUSIC:house(c,color,0,10,15,5);for(int x=-10;x<=10;x+=5){line(c,x,-19,x,0,1,WOOD);box(c,x-2,-13,x+2,-6,LIGHT);}line(c,-14,-20,14,-20,2,WOOD);break;
            case DRUM:box(c,-13,4,13,10,WOOD);disc(c,0,-3,11,color);disc(c,0,-3,8,LIGHT);line(c,-14,-17,-4,-3,2,WOOD);line(c,14,-17,4,-3,2,WOOD);break;
            case STONE_MAZE:for(int i=0;i<6;i++){double a=Math.PI*i/3;float x=(float)Math.cos(a)*13,y=(float)Math.sin(a)*8;box(c,x-3,y-10,x+3,y+4,STEEL);}break;
            case EARTH_WALL:case STONE_WALL:
                box(c,-18,-6,18,11,kind==War.StructureKind.EARTH_WALL?WOOD:STEEL);for(int y=-2;y<=7;y+=5)line(c,-18,y,18,y,1,DARK);for(int x=-14;x<18;x+=9)line(c,x,-6,x,10,1,DARK);break;
            case FIRE_SHIP:ship(c,color,0,true);break;
            default:
                boolean ball=kind==War.StructureKind.FIRE_BALL||kind==War.StructureKind.FLAME_BALL||kind==War.StructureKind.INFERNO_BALL;
                int level=kind==War.StructureKind.INFERNO_BALL||kind==War.StructureKind.INFERNO_SEED?3:kind==War.StructureKind.FLAME_BALL||kind==War.StructureKind.FLAME_SEED?2:1;
                if(ball){disc(c,0,1,12,WOOD);for(int x=-8;x<=8;x+=8)line(c,x,-8,x,10,2,LIGHT);}else{box(c,-11,-6,11,10,WOOD);box(c,-12,-7,12,-4,LIGHT);box(c,-12,7,12,10,LIGHT);}
                for(int i=0;i<level;i++)poly(c,FIRE,-8+i*6,-10,-4+i*6,-21,-1+i*6,-10);break;
        }
    }
}
