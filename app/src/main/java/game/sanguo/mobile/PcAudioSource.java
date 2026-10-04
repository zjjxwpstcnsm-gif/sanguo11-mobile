package game.sanguo.mobile;

import java.io.IOException;

/** Immutable original compressed audio/timeline descriptor, shared by music and voice decoders. */
class PcAudioSource {
    final String asset,oggSha256,referencePcmSha256;
    final int sampleRate,channels;
    final long frames;
    final boolean strictFrameCount;
    PcAudioSource(String asset,String oggSha256,String referencePcmSha256,int sampleRate,int channels,long frames)throws IOException {
        this(asset,oggSha256,referencePcmSha256,sampleRate,channels,frames,true);
    }
    PcAudioSource(String asset,String oggSha256,String referencePcmSha256,int sampleRate,int channels,long frames,boolean strictFrameCount)throws IOException {
        if(asset==null||!asset.startsWith("audio/pc/")||asset.contains("..")||!oggSha256.matches("[0-9a-f]{64}")
            ||!referencePcmSha256.matches("[0-9a-f]{64}")||sampleRate!=44100||(channels!=1&&channels!=2)||frames<1)
            throw new IOException("Invalid original audio source descriptor");
        this.asset=asset;this.oggSha256=oggSha256;this.referencePcmSha256=referencePcmSha256;
        this.sampleRate=sampleRate;this.channels=channels;this.frames=frames;
        this.strictFrameCount=strictFrameCount;
    }
}
