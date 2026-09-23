using System.Text.Json;
using System.Globalization;
using Sanguo.Contracts;
using Sanguo.Client;
using Sanguo.Transport;

// This compiles the real platform-free Unity sources, not UnityEngine or a Player.
internal static class Program
{
    static int checks;
    static void Require(bool ok,string text){checks++;if(!ok)throw new Exception(text);}
    static readonly JsonSerializerOptions Json=new JsonSerializerOptions { IncludeFields=true };
    static BridgeMessage Message(string type,long sequence,long revision,BridgeEntity[] entities=null,string[] removed=null)
    { return new BridgeMessage {schemaVersion=1,type=type,sessionId="test",sequence=sequence,revision=revision,
        width=2,height=2,terrain="ABCD",entities=entities??Array.Empty<BridgeEntity>(),removed=removed??Array.Empty<string>()}; }
    static BridgeBatch Batch(params BridgeMessage[] messages){return new BridgeBatch {status="OK",messages=messages};}
    sealed class RecordingTransport : IBridgeTransport
    {
        // Records requests only. It NEVER computes rules or acknowledges command success.
        public readonly List<BridgeRequest> Requests=new List<BridgeRequest>();
        public string SessionId=>"test";public string Scenario=>"protocol-test";public bool ReadOnly=>false;
        public string Send(BridgeRequest r){Requests.Add(r);return "QUEUED";}
        public BridgeBatch Poll()=>null;
        public void Dispose(){}
    }
    static void Main(string[] args)
    {
        string root=args.Length==1?args[0]:Directory.GetCurrentDirectory();
        var json=JsonSerializer.Deserialize<BridgeMessage>("{\"sequence\":9007199254740993,\"revision\":9007199254740994,\"error\":\"\"}",Json);
        Require(json.sequence==9007199254740993L&&json.revision==9007199254740994L,"Int64 exact JSON parse");
        Require(JsonSerializer.Serialize(json,Json).Contains("9007199254740993"),"Int64 exact JSON output");
        var transport=new RecordingTransport();var sync=new BridgeSync(transport);sync.Start();
        Require(transport.Requests.Count==1&&transport.Requests[0].type=="snapshot","startup requests snapshot");
        var city=new BridgeEntity {entityId="site:7",kind="CITY",name="城",officerId=21,troops=2000};
        sync.Receive(Batch(Message("snapshot",1,9007199254740993L,new[]{city})));
        Require(sync.State.HasSnapshot&&sync.State.Entities.Count==1,"complete snapshot");
        city.troops=1;Require(sync.State.Entities["site:7"].Troops==2000,"detached immutable display state");
        sync.Execute("recruit",sync.State.Entities["site:7"]);var command=transport.Requests.Last();
        Require(command.type=="command"&&command.operation=="recruit"&&command.cityId==7&&command.officerId==21,"real recruit routing");
        Require(command.revision==9007199254740993L&&command.clientSequence==1,"command counters exact");
        sync.Execute("patrol",sync.State.Entities["site:7"],true);command=transport.Requests.Last();
        Require(command.operation=="patrol"&&command.officerId==-1&&command.clientSequence==2,"patrol invalid request reaches Java, no local rules");
        sync.Receive(Batch(Message("delta",2,9007199254740994L)));
        Require(sync.State.Revision==9007199254740994L&&sync.State.Entities.Count==1,"empty authoritative delta still advances");
        var receipt=Message("receipt",3,9007199254740994L);receipt.commandId="id";receipt.error="";sync.Receive(Batch(receipt));
        Require(sync.Status.StartsWith("COMMAND_OK"),"Unity empty-string success receipt");
        receipt=Message("receipt",4,9007199254740994L);receipt.error="DENIED";sync.Receive(Batch(receipt));
        Require(sync.Status.Contains("COMMAND_REJECTED")&&sync.State.Revision==9007199254740994L,"error never applies state");
        sync.Receive(Batch(Message("delta",5,9007199254740995L,null,new[]{"site:7"})));
        Require(sync.State.Entities.Count==0,"deletion applied");
        var bad=Message("delta",6,9007199254740996L,new[]{new BridgeEntity {entityId="unit:2"},new BridgeEntity()});sync.Receive(Batch(bad));
        Require(sync.State.Entities.Count==0&&sync.State.Revision==9007199254740995L,"malformed delta atomic");
        Require(transport.Requests.Last().type=="snapshot","malformed delta requests resync");
        sync.Receive(Batch(Message("snapshot",8,9007199254740995L,new[]{city})));
        Require(sync.State.Entities.Count==1,"resync accepts complete forward sequence snapshot");
        sync.Receive(Batch(Message("delta",10,9007199254740996L)));
        Require(sync.State.Revision==9007199254740995L&&sync.Status=="SEQUENCE_GAP","gap rejected");
        sync.Receive(Batch(Message("snapshot",11,9007199254740996L,new[]{city})));
        var overflow=Message("resync",12,9007199254740996L);sync.Receive(Batch(overflow));
        Require(sync.Status=="QUEUE_OVERFLOW"&&transport.Requests.Last().type=="snapshot","overflow requests snapshot");
        sync.Receive(Batch(Message("snapshot",13,9007199254740997L,new[]{city})));
        var foreign=Message("delta",14,9007199254740998L);foreign.sessionId="old";sync.Receive(Batch(foreign));
        Require(sync.State.Revision==9007199254740997L,"old session rejected");
        sync.Receive(Batch(Message("snapshot",14,1)));Require(sync.State.Revision==9007199254740997L,"stale snapshot cannot roll back");
        var fixture=JsonSerializer.Deserialize<BridgeBatch>(File.ReadAllText(Path.Combine(root,"unity/Assets/Resources/U01_fixture.json")),Json);
        using(var fixtures=new FixtureTransport(fixture)){
            var preview=new BridgeSync(fixtures);preview.Start();Require(preview.State.HasSnapshot&&preview.ReadOnly,"existing fixture read-only");
            preview.Execute("recruit",preview.State.Entities.Values.First());Require(preview.Status=="EDITOR_FIXTURE_READ_ONLY","fixture cannot execute game");
        }
        foreach(string line in File.ReadAllLines(Path.Combine(root,"game-runtime/src/test/resources/architecture/grid-layout.csv"))){
            if(line.StartsWith("#")||string.IsNullOrWhiteSpace(line))continue;
            var n=line.Split(',').Select(x=>double.Parse(x,CultureInfo.InvariantCulture)).ToArray();
            var grid=new GridLayout(n[0]!=0,n[1],(int)n[2],(int)n[3]);
            Require(grid.X((int)n[4],(int)n[5])==n[6]&&grid.Z((int)n[4],(int)n[5])==n[7],"shared Java/C# forward coordinate");
            Require(grid.Column(n[6],n[7])==n[4]&&grid.Row(n[6],n[7])==n[5],"shared Java/C# inverse coordinate");
        }
        Console.WriteLine($"Client protocol PASS: {checks} assertions; real Contracts/Client/Fixture sources; NOT Unity Player validation.");
    }
}
