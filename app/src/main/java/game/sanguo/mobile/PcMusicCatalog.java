package game.sanguo.mobile;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Independent original media identity. No automatic scene selection or gameplay dependency. Load off main thread. */
final class PcMusicCatalog {
    static final class Track extends PcAudioSource {
        final int musicId,resourceId;
        final long loopStart,loopEnd;
        private Track(JSONObject row)throws Exception {
            super(row.getString("asset"),row.getString("oggSha256"),row.getString("referencePcmSha256"),row.getInt("sampleRate"),row.getInt("channels"),row.getLong("frames"));
            musicId=row.getInt("musicId");resourceId=row.getInt("resourceId");
            loopStart=row.isNull("loopStartFrame")?-1:row.getLong("loopStartFrame");loopEnd=row.isNull("loopEndFrame")?-1:row.getLong("loopEndFrame");
            if(musicId<0||musicId>=30||resourceId!=2237+musicId||!asset.equals("audio/pc/music/"+resourceId+".ogg")
                ||sampleRate!=44100||channels!=2||frames<1||!oggSha256.matches("[0-9a-f]{64}")||!referencePcmSha256.matches("[0-9a-f]{64}")
                ||!((loopStart==-1&&loopEnd==-1)||(loopStart>=0&&loopStart<loopEnd&&loopEnd==frames)))throw new IOException("Invalid original music manifest");
        }
    }
    private final Map<Integer,Track> tracks;
    PcMusicCatalog(Context context)throws IOException {
        try(var stream=context.getAssets().open("audio/pc/music-manifest.json")){
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[8192];for(int n;(n=stream.read(buffer))!=-1;)bytes.write(buffer,0,n);
            JSONObject root=new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
            if(root.getInt("schema")!=1||!root.getString("sourceArchiveSha256").equals("e61c97fee43ee23b1248c46a2eb3e50adb0db620b8c0bff6e8fd67c30ca9c31a"))throw new IOException("Unknown original music source");
            JSONArray rows=root.getJSONArray("tracks");Map<Integer,Track> found=new LinkedHashMap<>();
            for(int index=0;index<rows.length();index++){Track track=new Track(rows.getJSONObject(index));if(found.put(track.musicId,track)!=null)throw new IOException("Duplicate native music ID");}
            if(found.size()!=30)throw new IOException("Incomplete original music catalog");tracks=Collections.unmodifiableMap(found);
        }catch(IOException error){throw error;}catch(Exception error){throw new IOException("Original music catalog",error);}
    }
    Track track(int nativeMusicId){return tracks.get(nativeMusicId);}
    Map<Integer,Track> tracks(){return tracks;}
}
