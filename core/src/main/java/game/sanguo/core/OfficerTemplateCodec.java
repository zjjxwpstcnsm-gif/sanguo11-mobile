package game.sanguo.core;

import java.io.*;

/** Portable bounded custom officer template; identity, ownership, items and relationships are not copied. */
public final class OfficerTemplateCodec {
    private OfficerTemplateCodec(){}
    public static byte[] encode(Editor.Template t)throws IOException{
        t.validate();ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(b);
        d.writeInt(0x53474f46);d.writeInt(1);d.writeUTF(t.name);for(int i=0;i<5;i++)d.writeInt(t.stat(i));for(int i=0;i<6;i++)d.writeInt(t.aptitude(i));
        d.writeUTF(t.sex.name());d.writeUTF(t.skill);d.writeUTF(t.temper.name());d.writeInt(t.talkMask);d.flush();return b.toByteArray();
    }
    public static Editor.Template read(InputStream stream)throws IOException{
        if(stream==null)throw new IOException("无法读取武将模板");ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buffer=new byte[1024];int n;
        while((n=stream.read(buffer))!=-1){if(b.size()+n>4096)throw new IOException("武将模板超过4KB");b.write(buffer,0,n);}
        DataInputStream d=new DataInputStream(new ByteArrayInputStream(b.toByteArray()));
        try{
            if(d.readInt()!=0x53474f46||d.readInt()!=1)throw new IOException("不是受支持的新武将模板");
            String name=d.readUTF();int[] s=new int[5],a=new int[6];for(int i=0;i<5;i++)s[i]=d.readInt();for(int i=0;i<6;i++)a[i]=d.readInt();
            Editor.Template t=new Editor.Template(name,s,a,World.Sex.valueOf(d.readUTF()),d.readUTF(),Debate.Temper.valueOf(d.readUTF()),d.readInt());
            if(d.available()!=0)throw new IOException("模板有未知尾部");t.validate();return t;
        }catch(IllegalArgumentException e){throw new IOException("武将模板字段无效",e);}
    }
}
