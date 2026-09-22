package game.sanguo.mobile;

import game.sanguo.core.*;

/** Bounded, deterministic presentation samples. No World, RNG, command or damage calculation. */
final class CombatVisual {
    static final float LAUNCH=.22f, HIT=.62f, FEEDBACK=.68f;
    static final int CAPACITY=48, FIRE_BUDGET=16, TEXT_BUDGET=8;
    enum Style { NONE, MELEE, CHARGE, ARROW, STONE, FIRE, LIGHTNING, RECOVER, STATUS }
    static final class Particle {
        int mesh; float x,y,z,scale,yaw;
    }
    final Particle[] particles=new Particle[CAPACITY];
    int count;
    CombatVisual(){for(int i=0;i<CAPACITY;i++)particles[i]=new Particle();}
    static float fraction(float f){return Float.isFinite(f)?Math.max(0,Math.min(1,f)):0;}
    static Style style(TurnJournal.Event e){
        if(e==null)return Style.NONE;
        if(e.kind==TurnJournal.Kind.RECOVER)return Style.RECOVER;
        if(e.kind==TurnJournal.Kind.MOVE||e.kind==TurnJournal.Kind.ENTER||e.kind==TurnJournal.Kind.DEPLOY)return Style.NONE;
        if(e.label.equals("落雷"))return Style.LIGHTNING;
        if(e.label.contains("火")&&!e.label.contains("灭火"))return Style.FIRE;
        if(e.kind==TurnJournal.Kind.PLOT)return Style.STATUS;
        if(e.kind==TurnJournal.Kind.CHANGE)return e.impacts.isEmpty()?Style.NONE:Style.STATUS;
        if(e.sourceType.contains("CATAPULT"))return Style.STONE;
        if(e.sourceType.equals("CAVALRY"))return Style.CHARGE;
        if(e.sourceKey.startsWith("c")||e.sourceType.contains("BOW")||e.sourceType.equals("ARROW_TOWER")||e.sourceType.equals("SIEGE_TOWER"))return Style.ARROW;
        return Style.MELEE;
    }
    static TurnJournal.Strike strike(TurnJournal.Event e,float fraction){
        return e==null||e.strikes.isEmpty()?null:e.strikes.get(Math.min(e.strikes.size()-1,(int)(fraction(fraction)*e.strikes.size())));
    }
    static float phase(TurnJournal.Event e,float fraction){
        float f=fraction(fraction);if(e==null||e.strikes.size()<2)return f;
        float scaled=f*e.strikes.size();return f==1?1:scaled-(int)scaled;
    }
    static Style strikeStyle(TurnJournal.Strike s){
        return s.type.equals("CATAPULT")?Style.STONE:s.type.equals("CAVALRY")?Style.CHARGE:s.type.equals("CROSSBOW")||s.type.equals("SIEGE_TOWER")?Style.ARROW:Style.MELEE;
    }
    void sample(TurnJournal.Event e,float fraction,MapSceneSnapshot.Ground g){
        count=0;if(e==null||g==null)return;float f=phase(e,fraction);Style style=style(e);TurnJournal.Strike strike=strike(e,fraction);
        if(strike!=null)style=strikeStyle(strike);
        if(style==Style.NONE)return;
        Hex target=strike!=null?strike.target:e.target!=null?e.target:!e.impacts.isEmpty()?e.impacts.get(0).hex:e.start;
        if(target==null||!g.valid(target))return;
        Hex start=strike!=null?strike.start:e.start!=null&&g.valid(e.start)?e.start:target;
        float ax=g.grid.x(start),az=g.grid.z(start),bx=g.grid.x(target),bz=g.grid.z(target);
        float ay=g.surface.at(start)+.35f,by=g.surface.at(target)+.3f;
        if(f>=LAUNCH&&f<HIT){
            float t=(f-LAUNCH)/(HIT-LAUNCH),yaw=(float)Math.atan2(bx-ax,bz-az);
            if(style==Style.ARROW||style==Style.STONE||style==Style.FIRE){
                int n=style==Style.ARROW?7:style==Style.FIRE?4:1;
                for(int i=0;i<n;i++){
                    float offset=(i-(n-1)*.5f)*.07f;
                    add(style==Style.ARROW?0:style==Style.STONE?1:2,ax+(bx-ax)*t+offset,ay+(by-ay)*t+4*t*(1-t)*(style==Style.STONE?1.8f:.55f),az+(bz-az)*t+offset,style==Style.ARROW?1:.7f,yaw);
                }
            }else if(style==Style.LIGHTNING){
                for(int i=0;i<8;i++)add(4,bx+(i%2==0?.08f:-.08f),by+i*.28f,bz,.8f,0);
            }else if(style==Style.MELEE||style==Style.CHARGE)add(3,bx,by+.15f,bz,.5f+t*.7f,yaw+t*2);
        }
        if(f>=HIT&&f<1){
            float t=(f-HIT)/(1-HIT);
            // Only recorded changes produce hit bursts; a failed plot has no invented damage.
            if(strike!=null){
                if(strike.afterTroops<strike.beforeTroops)for(int i=0;i<6;i++){double a=i*Math.PI/3;add(1,bx+(float)Math.cos(a)*t*.6f,by+(float)Math.sin(t*Math.PI)*.4f,bz+(float)Math.sin(a)*t*.6f,(1-t)*.3f,0);}
                return;
            }
            int targets=0;
            for(TurnJournal.StateChange delta:e.states){
                TurnJournal.State before=delta.before,after=delta.after;
                TurnJournal.State at=after!=null?after:before;if(at==null||!g.valid(at.hex))continue;
                boolean damage=before!=null&&(after==null||after.troops<before.troops||after.hp<before.hp);
                boolean recovery=before!=null&&after!=null&&after.energy>before.energy;
                boolean fire=after!=null&&after.key.startsWith("f");
                if(!damage&&!recovery&&!fire)continue;
                if(targets++>=6)break;
                float x=g.grid.x(at.hex),z=g.grid.z(at.hex),y=g.surface.at(at.hex)+.22f;
                for(int i=0;i<5;i++){double a=i*Math.PI*2/5;add(recovery?5:fire?2:1,x+(float)Math.cos(a)*t*.65f,y+(float)Math.sin(t*Math.PI)*.45f,z+(float)Math.sin(a)*t*.65f,(1-t)*.32f,0);}
            }
            if(style==Style.STATUS||style==Style.RECOVER)add(5,bx,by,bz,.4f+t*.7f,t*3);
        }
    }
    void fire(MapSceneSnapshot.FireState fire,MapSceneSnapshot.Ground g,long millis,boolean motion){
        float pulse=motion?(float)Math.sin(millis*.006+fire.hex.q*3+fire.hex.r)*.12f:0;
        add(2,g.grid.x(fire.hex),g.surface.at(fire.hex)+.25f,g.grid.z(fire.hex),.5f+pulse,0);
    }
    private void add(int mesh,float x,float y,float z,float scale,float yaw){
        if(count==CAPACITY)return;Particle p=particles[count++];p.mesh=mesh;p.x=x;p.y=y;p.z=z;p.scale=Math.max(.001f,scale);p.yaw=yaw;
    }
    /** Original CC0 opaque effect geometry; six shared meshes, no runtime shader compiler. */
    static SceneMesh mesh(int kind){
        SceneMesh.Builder b=new SceneMesh.Builder();
        int color=kind==0?0xffd2c0a0:kind==1?0xffc8b59a:kind==2?0xffff9238:kind==3?0xffffdc98:kind==4?0xffcbe7ff:0xff82d5bc;
        if(kind==0){
            b.box(-.015f,-.22f,.03f,.44f,.035f,color);
            b.face(new float[]{0,0,.31f,-.07f,0,.15f,.07f,0,.15f,0,0,.31f},color);
        }else if(kind==3||kind==5){
            for(int i=0;i<12;i++){double a=i*Math.PI/6,c=(i+1)*Math.PI/6;float r=.5f,s=.43f;
                b.face(new float[]{(float)Math.cos(a)*r,0,(float)Math.sin(a)*r,(float)Math.cos(c)*r,0,(float)Math.sin(c)*r,(float)Math.cos(c)*s,0,(float)Math.sin(c)*s,(float)Math.cos(a)*s,0,(float)Math.sin(a)*s},color);}
        }else{
            float h=kind==2?.65f:kind==4?.38f:.22f,r=kind==4?.05f:.20f;
            for(int i=0;i<6;i++){double a=i*Math.PI/3,c=(i+1)*Math.PI/3;
                b.face(new float[]{0,h,0,(float)Math.cos(a)*r,0,(float)Math.sin(a)*r,0,-h*.3f,0,(float)Math.cos(c)*r,0,(float)Math.sin(c)*r},color);}
        }
        return b.mesh(0,0,1);
    }
}
