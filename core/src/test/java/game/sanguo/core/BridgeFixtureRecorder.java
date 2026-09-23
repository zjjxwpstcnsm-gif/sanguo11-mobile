package game.sanguo.core;

/** Records an actual Java scenario snapshot for Unity Editor preview; never runs commands. */
public final class BridgeFixtureRecorder {
    private static String quote(String value){
        StringBuilder out=new StringBuilder("\"");
        for(int i=0;i<value.length();i++){
            char c=value.charAt(i);
            if(c=='"'||c=='\\')out.append('\\').append(c);
            else if(c<32)out.append(String.format("\\u%04x",(int)c));
            else out.append(c);
        }
        return out.append('"').toString();
    }
    public static void main(String[] args)throws Exception{
        World world=ScenarioCatalog.load("coalition-190",0,20260923L);
        BridgeSession session=new BridgeSession(world);session.snapshot();
        BridgeSession.Message m=session.drain().get(0);
        StringBuilder out=new StringBuilder("{\"status\":\"OK\",\"dropped\":0,\"messages\":[{\"type\":\"snapshot\",\"sessionId\":\"EDITOR-FIXTURE-READ-ONLY\",\"schemaVersion\":1,\"sequence\":1,\"revision\":0,\"mapRevision\":")
            .append(m.mapRevision).append(",\"width\":").append(m.width).append(",\"height\":").append(m.height)
            .append(",\"turn\":").append(m.turn).append(",\"player\":").append(m.player)
            .append(",\"terrain\":").append(quote(m.terrain)).append(",\"entities\":[");
        for(BridgeSession.Entity e:m.entities){
            if(out.charAt(out.length()-1)=='}')out.append(',');
            out.append("{\"entityId\":").append(quote(e.entityId)).append(",\"kind\":").append(quote(e.kind))
                .append(",\"name\":").append(quote(e.name)).append(",\"q\":").append(e.q)
                .append(",\"r\":").append(e.r).append(",\"owner\":").append(e.owner)
                .append(",\"troops\":").append(e.troops).append(",\"energy\":").append(e.energy)
                .append(",\"officerId\":").append(e.officerId).append(",\"gold\":").append(e.gold)
                .append(",\"food\":").append(e.food).append(",\"order\":").append(e.order).append('}');
        }
        System.out.println(out.append("],\"removed\":[]}]}"));
    }
}
