package game.sanguo.mobile;

import java.util.Arrays;
import java.util.Locale;

/** Owner-only, bounded raw samples. No allocation in record; never reports these as GPU/FPS. */
final class SceneFrameMetrics {
    static final int CAPACITY=300;
    private final long[][] values=new long[6][CAPACITY];
    private int cursor,count;private long attempts,submitted,rejected;
    void record(long wall,long cpu,long begin,long upload,long render,boolean admitted){
        values[0][cursor]=wall;values[1][cursor]=cpu;values[2][cursor]=begin;values[3][cursor]=upload;values[4][cursor]=render;values[5][cursor]=admitted?1:0;
        cursor=(cursor+1)%CAPACITY;count=Math.min(CAPACITY,count+1);attempts++;if(admitted)submitted++;else rejected++;
    }
    String summary(){
        StringBuilder s=new StringBuilder("R16 attempts=").append(attempts).append(" submitted=").append(submitted).append(" rejected=").append(rejected).append(" window=").append(count);
        String[] names={"owner_wall","owner_cpu","begin_wall","mesh_upload_wall","render_submit_wall"};
        for(int i=0;i<5;i++){
            long[] sorted=Arrays.copyOf(values[i],count);Arrays.sort(sorted);s.append(' ').append(names[i]).append("_ms_p50_p95_p99=");
            for(double p:new double[]{.5,.95,.99})s.append(count==0?"NA":String.format(Locale.ROOT,"%.3f",sorted[(int)Math.ceil(count*p)-1]/1e6)).append('/');
        }
        return s.append(" presented_interval=NOT_AVAILABLE gpu_duration=NOT_AVAILABLE").toString();
    }
    String csv(){
        StringBuilder s=new StringBuilder("attempt,owner_wall_ns,owner_cpu_ns,begin_wall_ns,mesh_upload_wall_ns,render_submit_wall_ns,admitted\n");
        for(int i=0;i<count;i++){int at=(cursor-count+i+CAPACITY)%CAPACITY;s.append(attempts-count+i+1);for(long[] series:values)s.append(',').append(series[at]);s.append('\n');}
        return s.toString();
    }
}
