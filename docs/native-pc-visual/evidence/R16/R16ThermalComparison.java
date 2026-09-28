package game.sanguo.mobile;

import java.lang.reflect.Method;

/** Runs the exact input and candidate policies on identical synthetic sensor histories. */
public final class R16ThermalComparison {
    public static void main(String[] args)throws Exception{
        SceneQuality.Thermal policy=new SceneQuality.Thermal();Method update;
        try{update=SceneQuality.Thermal.class.getDeclaredMethod("update",int.class,long.class);}catch(NoSuchMethodException e){update=SceneQuality.Thermal.class.getDeclaredMethod("update",int.class);}
        int transitions=0;boolean previous=policy.constrained;
        for(int second=0;second<100;second++){
            int status=second<60?(second%2==0?3:1):1;
            if(update.getParameterCount()==2)update.invoke(policy,status,second*1_000_000_000L);else update.invoke(policy,status);
            if(previous!=policy.constrained)transitions++;previous=policy.constrained;
            System.out.println(args[0]+","+second+","+status+","+policy.constrained+","+policy.fps(SceneQuality.HIGH)+","+policy.scale(SceneQuality.HIGH)+","+transitions);
        }
    }
}
