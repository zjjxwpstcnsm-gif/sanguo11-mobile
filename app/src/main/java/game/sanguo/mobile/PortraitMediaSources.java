package game.sanguo.mobile;

import game.sanguo.api.StateToken;
import game.sanguo.core.World;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Additive readonly DTO seam. Binding runs once at the host's snapshot boundary, never while drawing a row. */
final class PortraitMediaSources {
    private static final Map<World,Map<Integer,PortraitMediaIdentity>> sources=new WeakHashMap<>();
    private PortraitMediaSources(){}
    private static Object field(Object value,String name)throws ReflectiveOperationException{return value.getClass().getField(name).get(value);}
    /** Accepts the completed session1 OfficerSnapshot without a compile dependency on its newer optional DTO. */
    static synchronized void bind(World view,StateToken expected,Object snapshot){
        if(view==null||snapshot==null)throw new IllegalArgumentException("Readonly portrait snapshot required");
        try{
            if(!expected.equals(field(snapshot,"state")))throw new IllegalArgumentException("Portrait metadata token differs");
            Map<Integer,PortraitMediaIdentity> next=new HashMap<>();
            for(Object officer:(Iterable<?>)field(snapshot,"officers")){
                Object source=field(officer,"source");if(source==null)continue;int id=(Integer)field(officer,"id");
                PortraitMediaIdentity identity=new PortraitMediaIdentity(id,(Integer)field(source,"nativeId"),(String)field(source,"sourceVariant"),(String)field(source,"sourcePath"),(String)field(source,"sourceSha"),(String)field(source,"recordSha"));
                if(next.put(id,identity)!=null)throw new IllegalArgumentException("Duplicate portrait source identity");
            }
            sources.put(view,Collections.unmodifiableMap(next));
        }catch(ReflectiveOperationException error){throw new IllegalArgumentException("Completed source DTO contract missing",error);}
    }
    static synchronized PortraitMediaIdentity source(World view,int officerId){Map<Integer,PortraitMediaIdentity> bound=sources.get(view);return bound==null?null:bound.get(officerId);}
    static synchronized void discard(World view){sources.remove(view);}
}
