#!/usr/bin/env python3
"""Stage per-engine bounded index leases; no original buffers/geometry or budget loss."""
from pathlib import Path
import json,difflib,hashlib
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent;OUT=ROOT/'out/session-a/gpu-index324-checked'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 assert not OUT.exists();OUT.mkdir(parents=True);base=ROOT/'out/session-a/mesh-allocation-build316/source';path='app/src/main/java/game/sanguo/mobile/FilamentMapView.java';source=base/path;before=source.read_text();s=before
 old='    private final Map<SceneMesh,GpuMesh> terrain=new HashMap<>();';new=old+'\n    private final SceneIndexLeasePool<IndexBuffer> pcGroundIndices=new SceneIndexLeasePool<>(16);';assert s.count(old)==1;s=s.replace(old,new)
 old='''        GpuMesh waterChild;MaterialInstance waterInstance;String waterKey;''';new=old+'\n        SceneIndexLeasePool.Lease<IndexBuffer> indexLease;';assert s.count(old)==1;s=s.replace(old,new)
 old='''            Buffer indexData=MeshIndexBuffer.encode(m.vertices.length/7,m.indices);
            ib=new IndexBuffer.Builder().indexCount(m.indices.length).bufferType(MeshIndexBuffer.compact(m.vertices.length/7)?IndexBuffer.Builder.IndexType.USHORT:IndexBuffer.Builder.IndexType.UINT).build(engine);ib.setBuffer(engine,indexData);'''
 new='''            if(m.pcGround){
                indexLease=pcGroundIndices.acquire(m.vertices.length/7,m.indices,()->createIndexBuffer(m),engine::destroyIndexBuffer);
                ib=indexLease.resource();
            }else ib=createIndexBuffer(m);''';assert s.count(old)==1;s=s.replace(old,new)
 at=s.index('    private final class GpuMesh {');method='''    private IndexBuffer createIndexBuffer(SceneMesh mesh){
        Buffer data=MeshIndexBuffer.encode(mesh.vertices.length/7,mesh.indices);
        IndexBuffer result=new IndexBuffer.Builder().indexCount(mesh.indices.length)
            .bufferType(MeshIndexBuffer.compact(mesh.vertices.length/7)?IndexBuffer.Builder.IndexType.USHORT:IndexBuffer.Builder.IndexType.UINT).build(engine);
        try{result.setBuffer(engine,data);return result;}
        catch(RuntimeException|LinkageError|OutOfMemoryError error){engine.destroyIndexBuffer(result);throw error;}
    }
''';s=s[:at]+method+s[at:]
 old='''if(ib!=null){engine.destroyIndexBuffer(ib);ib=null;}''';new='''if(ib!=null){if(indexLease!=null){indexLease.release();indexLease=null;}else engine.destroyIndexBuffer(ib);ib=null;}''';assert s.count(old)==1;s=s.replace(old,new)
 old='''        if(backdrop!=null){backdrop.destroy();backdrop=null;}
        clearEffects();''';new='''        if(backdrop!=null){backdrop.destroy();backdrop=null;}
        pcGroundIndices.close();
        clearEffects();''';assert s.count(old)==1;s=s.replace(old,new)
 old='''        +" workerDeliveries="+meshWork.delivered()''';new='''        +" pcGroundIndexResources="+pcGroundIndices.liveResources()+" pcGroundIndexLeases="+pcGroundIndices.liveLeases()+" pcGroundIndexCreated="+pcGroundIndices.created()+" pcGroundIndexReused="+pcGroundIndices.reused()+" pcGroundIndexDestroyed="+pcGroundIndices.destroyed()
        +" workerDeliveries="+meshWork.delivered()''';assert s.count(old)==1;s=s.replace(old,new)
 old='        for(GpuMesh m:resident){bufferBytes+=(long)m.source.vertices.length*4+MeshIndexBuffer.bytes(m.source.vertices.length/7,m.source.indices.length)+(m.source.uv==null?0:(long)m.source.uv.length*4)+(m.source.surfaceData==null?0:(long)m.source.surfaceData.length*4)+(m.source.tangents==null?0:(long)m.source.tangents.length*4);';new='        Set<IndexBuffer> countedIndices=Collections.newSetFromMap(new IdentityHashMap<>());\n        for(GpuMesh m:resident){bufferBytes+=(long)m.source.vertices.length*4+(m.source.uv==null?0:(long)m.source.uv.length*4)+(m.source.surfaceData==null?0:(long)m.source.surfaceData.length*4)+(m.source.tangents==null?0:(long)m.source.tangents.length*4);\n            if(m.ib!=null&&countedIndices.add(m.ib))bufferBytes+=MeshIndexBuffer.bytes(m.source.vertices.length/7,m.source.indices.length);';assert s.count(old)==1;s=s.replace(old,new)
 target=OUT/path;target.parent.mkdir(parents=True);target.write_text(s);newpath='app/src/main/java/game/sanguo/mobile/SceneIndexLeasePool.java';pool=ROOT/newpath;copy=OUT/newpath;copy.write_bytes(pool.read_bytes())
 patch=''.join(difflib.unified_diff(before.splitlines(True),s.splitlines(True),fromfile='a/'+path,tofile='b/'+path));(DOC/'GPU_INDEX324.patch').write_text(patch)
 report={'paths':[{'path':path,'stagedPath':str(target),'beforeSha256':sha(source),'afterSha256':sha(target)},{'path':newpath,'stagedPath':str(copy),'beforeSha256':None,'afterSha256':sha(copy)}],'parent':'actual316','liveRegistryCapacity':16,'zeroOwnerEntriesRemoved':True,'sharedOnlyPcGroundAndSameEngine':True,'originalIndexEncodingAndDrawOffsetsUnchanged':True,'uploadsStillBounded8Count2msWall':True,'originalGpuGeometryAndTexturesDropped':False,'canonicalRendererChanged':False,'installedOrGpuAccepted':False,'scope':'A allocation/native resource duplicate reduction only; immutable int-array identity plus exact encoding format, per engine reference-counted lifetimes, capacity16 spills privately rather than dropping. Partial creation cleans native IB. No original4JNI/B/core/Bridge/Unity rules/RNG changes. Need owner lifetime host tests, Android compile, independent fullAPK installation and actual repeat/source/zoom/state/read/cold checks.','wholeGoalComplete':False}
 (DOC/'GPU_INDEX_DELTA324.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report),flush=True)
if __name__=='__main__':main()
