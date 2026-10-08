package game.sanguo.mobile;

import java.util.IdentityHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Owner-thread resources for byte-identical immutable mesh index arrays.
 * Bounded live registry; never pins a zero-owner resource or shares across engines. */
final class SceneIndexLeasePool<T> {
    private final Thread owner=Thread.currentThread();
    private final int capacity;
    private final IdentityHashMap<int[],Entry<T>> shortEntries=new IdentityHashMap<>(),intEntries=new IdentityHashMap<>();
    private boolean closed;
    private long created,reused,destroyed;
    private int liveLeases,liveResourceCount;
    private static final class Entry<T> {
        final int[] indices;final boolean compact;final int maxIndex;T resource;final Consumer<T> destroy;
        final boolean pooled;int owners;
        Entry(int[] indices,boolean compact,int maxIndex,T resource,Consumer<T> destroy,boolean pooled){
            this.indices=indices;this.compact=compact;this.maxIndex=maxIndex;this.resource=resource;this.destroy=destroy;this.pooled=pooled;
        }
    }
    static final class Lease<T> {
        private final SceneIndexLeasePool<T> pool;private final Entry<T> entry;private boolean released;
        Lease(SceneIndexLeasePool<T> pool,Entry<T> entry){this.pool=pool;this.entry=entry;}
        T resource(){if(released)throw new IllegalStateException("Index lease released");return entry.resource;}
        void release(){pool.release(this);}
    }
    SceneIndexLeasePool(int capacity){if(capacity<0)throw new IllegalArgumentException("index registry capacity");this.capacity=capacity;}
    private void owner(){if(Thread.currentThread()!=owner)throw new IllegalStateException("Index resource owner thread required");}
    Lease<T> acquire(int vertices,int[] immutableIndices,Supplier<T> create,Consumer<T> destroy){
        owner();if(closed)throw new IllegalStateException("Index registry closed");
        boolean compact=MeshIndexBuffer.compact(vertices);
        IdentityHashMap<int[],Entry<T>> entries=compact?shortEntries:intEntries;
        Entry<T> entry=entries.get(immutableIndices);
        Lease<T> lease;
        if(entry!=null){
            // Arrays were range-checked once and are immutable after CPU publication.
            if(entry.maxIndex>=vertices)throw new IllegalArgumentException("mesh index outside vertex buffer");
            lease=new Lease<>(this,entry);reused++;
        }else{
            int maximum=-1;
            for(int index:immutableIndices){if(index<0||index>=vertices)throw new IllegalArgumentException("mesh index outside vertex buffer");maximum=Math.max(maximum,index);}
            boolean pooled=shortEntries.size()+intEntries.size()<capacity;
            // Allocate ownership metadata before native creation. A failed map
            // publication destroys its newly created resource, not an existing lease.
            entry=new Entry<>(immutableIndices,compact,maximum,null,destroy,pooled);
            lease=new Lease<>(this,entry);
            T resource=create.get();if(resource==null)throw new IllegalStateException("Missing index resource");
            entry.resource=resource;
            try{if(pooled)entries.put(immutableIndices,entry);}
            catch(RuntimeException|Error failure){entries.remove(immutableIndices);destroy.accept(resource);throw failure;}
            created++;liveResourceCount++;
        }
        entry.owners++;liveLeases++;return lease;
    }
    private void release(Lease<T> lease){
        owner();if(lease.pool!=this)throw new IllegalArgumentException("Foreign index lease");
        if(lease.released)return;lease.released=true;
        Entry<T> entry=lease.entry;entry.owners--;liveLeases--;
        if(entry.owners==0){
            if(entry.pooled)(entry.compact?shortEntries:intEntries).remove(entry.indices);
            entry.destroy.accept(entry.resource);destroyed++;liveResourceCount--;
        }
    }
    int liveResources(){owner();return liveResourceCount;}
    int registeredResources(){owner();return shortEntries.size()+intEntries.size();}
    int liveLeases(){owner();return liveLeases;}
    long created(){owner();return created;}
    long reused(){owner();return reused;}
    long destroyed(){owner();return destroyed;}
    void close(){owner();if(closed)return;if(liveLeases!=0)throw new IllegalStateException("Index resources still owned");closed=true;}
}
