package game.sanguo.core;
import java.io.*;
/** Original47b480/49e450 and4839f0. No engineering nearest-city route. */
final class PcPersonnelReturnRules {
    private static byte[]data;
    private static int[][]neighbors;
    private static synchronized int[][] neighbors()throws IOException {
        if(neighbors!=null)return neighbors;byte[]raw;
        try(var in=PcPersonnelReturnRules.class.getResourceAsStream("/pc-duel/personnel-neighbors.bin");var out=new ByteArrayOutputStream()){
            if(in==null)throw new IOException("原城市邻接资源缺失");byte[]block=new byte[1024];int n;while((n=in.read(block))!=-1){if(out.size()+n>1008)throw new IOException("原城市邻接资源过大");out.write(block,0,n);}raw=out.toByteArray();
        }
        try{if(raw.length!=1008||!PcCommandCapacityPolicy.hex(java.security.MessageDigest.getInstance("SHA-256").digest(raw)).equals("e8c5633deef89b1058f5560054d7ad84f80a8b5c60a54f7bd37f457a6c36133a"))throw new IOException("原城市邻接SHA不同");}catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}
        int[][]value=new int[42][6];var b=java.nio.ByteBuffer.wrap(raw).order(java.nio.ByteOrder.LITTLE_ENDIAN);for(int i=0;i<42;i++)for(int j=0;j<6;j++){int next=b.getInt();if(next< -1||next>=42)throw new IOException("原城市邻接引用越界");value[i][j]=next;}return neighbors=value;
    }
    /** Source0 original initialized topology only, not yet activated for other variants. */
    static int nextCitySource0(int origin,int destination)throws IOException {
        if(origin<0||origin>=42||destination<0||destination>=42)return -1;if(origin==destination)return destination;
        int best=-1,distance=Integer.MAX_VALUE;for(int next:neighbors()[origin])if(next>=0){int d=duration(next,destination);if(d<distance){best=next;distance=d;}}return best;
    }
    private static synchronized byte[] data()throws IOException {
        if(data!=null)return data;byte[]raw;
        try(var in=PcPersonnelReturnRules.class.getResourceAsStream("/pc-duel/personnel-travel.bin");var out=new ByteArrayOutputStream()){
            if(in==null)throw new IOException("原人员距离资源缺失");byte[]block=new byte[1024];int n;while((n=in.read(block))!=-1){if(out.size()+n>1851)throw new IOException("原人员距离资源过大");out.write(block,0,n);}raw=out.toByteArray();
        }
        try{if(raw.length!=1851||!PcCommandCapacityPolicy.hex(java.security.MessageDigest.getInstance("SHA-256").digest(raw)).equals("ff7a1c159d649cf24461b638214dcf07ca97e45fea93477dcbcb156cd6ba670c"))throw new IOException("原人员距离资源SHA不同");}catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}
        for(int n=0;n<87;n++)if((raw[1764+n]&255)>=42)throw new IOException("原据点地域父级不同");return data=raw;
    }
    static int duration(int originCityNative,int destinationCityNative)throws IOException {
        if(originCityNative<0||originCityNative>=42||destinationCityNative<0||destinationCityNative>=42)return -1;
        return data()[originCityNative*42+destinationCityNative]&255;
    }
    static int parent(int siteNative)throws IOException {return siteNative<0||siteNative>=87?-1:data()[1764+siteNative]&255;}
    static int cityAt(World w,Hex cell)throws IOException {
        if(!w.pcSourceFrame||!NationalMap.ID.equals(w.mapId)||PcScenarioIdentity.saved(w)==null||cell==null||!w.inside(cell))throw new IOException("原返程地域需要明确PC地图坐标");
        var coordinate=MapCoordinates.nationalSource(w,cell);if(!coordinate.isInside(200,200))throw new IOException("原返程坐标越界");
        int region=PcMap.get().region(coordinate.x,coordinate.y)&127;int city=parent(region);if(city<0)throw new IOException("原返程格子的据点地域未核实");return city;
    }
    static int projectSite(World w,int nativeId)throws IOException {
        var source=PcGovernorPolicy.data(w).source;return source.sites.values().stream().filter(s->s.nativeId==nativeId).map(s->s.id).findFirst().orElseThrow(()->new IOException("原返程据点稳定连接缺失"));
    }
    private PcPersonnelReturnRules(){}
}
