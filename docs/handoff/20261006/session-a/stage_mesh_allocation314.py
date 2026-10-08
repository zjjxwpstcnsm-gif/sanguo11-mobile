#!/usr/bin/env python3
"""Stage A exact-output mesh allocation reduction against immutable296."""
from pathlib import Path
import hashlib,json
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent
OUT=ROOT/'out/session-a/mesh-allocation314';PATH='app/src/main/java/game/sanguo/mobile/SceneMesh.java'
def sha(raw):return hashlib.sha256(raw).hexdigest()
def main():
 assert not OUT.exists();OUT.mkdir(parents=True)
 source=ROOT/'out/session-a/fire-cache-apk296/source'/PATH;before=source.read_bytes();s=before.decode()
 old='''    private static synchronized int[] pcIndices(int[] indices){
        for(int[] pattern:PC_INDEX_PATTERNS)if(Arrays.equals(pattern,indices))return pattern;
        if(PC_INDEX_PATTERNS.size()<16)PC_INDEX_PATTERNS.add(indices);
        return indices;
    }'''
 new='''    private static synchronized int[] pcIndices(int[] land,int landCount,int[] water,int waterCount){
        // Compare bounded task scratch before allocating a repeated immutable
        // topology. Never publish scratch; a miss owns an exact used-range copy.
        int count=landCount+waterCount;
        for(int[] pattern:PC_INDEX_PATTERNS){
            if(pattern.length!=count)continue;
            int n=0;while(n<landCount&&pattern[n]==land[n])n++;
            if(n!=landCount)continue;
            int w=0;while(w<waterCount&&pattern[landCount+w]==water[w])w++;
            if(w==waterCount)return pattern;
        }
        int[] indices=Arrays.copyOf(land,count);
        if(waterCount!=0)System.arraycopy(water,0,indices,landCount,waterCount);
        if(PC_INDEX_PATTERNS.size()<16)PC_INDEX_PATTERNS.add(indices);
        return indices;
    }'''
 assert s.count(old)==1;s=s.replace(old,new)
 old='''            int[] indices=Arrays.copyOf(land,landCount+waterCount);
            if(waterCount!=0)System.arraycopy(water,0,indices,landCount,waterCount);
            SceneMesh mesh=new SceneMesh(Arrays.copyOf(vertices,vertexCount*7),pcSlots==null?indices:pcIndices(indices),x,z,radius);'''
 new='''            int[] indices;
            if(pcSlots!=null)indices=pcIndices(land,landCount,water,waterCount);
            else{
                indices=Arrays.copyOf(land,landCount+waterCount);
                if(waterCount!=0)System.arraycopy(water,0,indices,landCount,waterCount);
            }
            SceneMesh mesh=new SceneMesh(Arrays.copyOf(vertices,vertexCount*7),indices,x,z,radius);'''
 assert s.count(old)==1;s=s.replace(old,new)
 old='''                fingerprint=(fingerprint^Float.floatToIntBits(g.surface.overrides.getOrDefault(h,-1f)))*1099511628211L;'''
 new='''                Float override=g.surface.overrides.get(h);
                fingerprint=(fingerprint^Float.floatToIntBits(override==null?-1f:override))*1099511628211L;'''
 assert s.count(old)==1;s=s.replace(old,new)
 target=OUT/PATH;target.parent.mkdir(parents=True);target.write_text(s)
 assert source.read_bytes()==before
 report={'paths':[{'path':PATH,'stagedPath':str(target),'beforeSha256':sha(before),'afterSha256':sha(target.read_bytes())}],'parent':'actual296','boundedIndexPatterns':16,'scratchNeverPublished':True,'canonicalChanged':False,'androidAccepted':False,'scope':'A mesh only. Exact topology matches scratch before repeated copy; unknown new patterns copy exact ranges. Same16 bounded immutable cache, geometry, materials, water, grid and rule ownership. Avoid per-cell boxed default Float. Needs raw output/cache/legacy/overrides parity and independent actual APK installation.','wholeGoalComplete':False}
 (DOC/'MESH_ALLOCATION_DELTA314.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report),flush=True)
if __name__=='__main__':main()
