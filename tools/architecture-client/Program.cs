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
        public string SessionId { get; }
        public RecordingTransport(string session="test"){SessionId=session;}
        public string Scenario=>"protocol-test";public bool ReadOnly=>false;
        public string Send(BridgeRequest r){Requests.Add(r);return "QUEUED";}
        public BridgeBatch Poll()=>null;
        public void Dispose(){}
    }
    static void Main(string[] args)
    {
        string root=args.Length>=1?args[0]:Directory.GetCurrentDirectory();
        var json=JsonSerializer.Deserialize<BridgeMessage>("{\"sequence\":9007199254740993,\"revision\":9007199254740994,\"error\":\"\"}",Json);
        Require(json.sequence==9007199254740993L&&json.revision==9007199254740994L,"Int64 exact JSON parse");
        Require(JsonSerializer.Serialize(json,Json).Contains("9007199254740993"),"Int64 exact JSON output");
        MediaFacts();
        if(args.Length>=2){
            var actual=JsonSerializer.Deserialize<BridgeBatch>(File.ReadAllText(args[1]),Json);
            var actualTransport=new RecordingTransport(actual.messages[0].sessionId);var actualSync=new BridgeSync(actualTransport);
            actualSync.Start();actualSync.Receive(actual);var facts=actualSync.DrainTechniquePoints();
            Require(facts.Length==2&&facts[0].Delta==20&&facts[1].Delta==-20,"actual Java JSON consumed by real C# client");
            Require(facts[0].ParentId==facts[1].ParentId&&facts[0].Id!=facts[1].Id,"actual parent and unique IDs survive");
            Require(actualSync.Status.StartsWith("COMMAND_REJECTED")&&actualSync.DrainTechniquePoints().Length==0,"actual rejected receipt adds no sound");
        }
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
    static TechniquePointsFact Fact(long revision,long sequence,int before,int after,string parent="p")
    {return new TechniquePointsFact {id=parent+":technique:"+sequence,parentId=parent,presentationParentId="journal:1",
        cause="EDITOR",phase="COMMIT",sequence=sequence,owner=0,before=before,after=after,delta=after-before,
        state=new StateToken {sessionId="test",generation=9007199254740993L,revision=revision}};}
    static BridgeMessage MediaMessage(string type,long sequence,long revision,params TechniquePointsFact[] facts)
    {var m=Message(type,sequence,revision);m.state=new StateToken {sessionId="test",generation=9007199254740993L,revision=revision};m.techniquePointsFacts=facts;return m;}
    static void MediaFacts()
    {
        var t=new RecordingTransport();var s=new BridgeSync(t);s.Start();
        s.Receive(Batch(MediaMessage("snapshot",1,1)));
        var gain=Fact(2,1,100,120);var loss=Fact(2,2,120,100);
        var wire=JsonSerializer.Deserialize<BridgeMessage>(JsonSerializer.Serialize(MediaMessage("delta",2,2,gain,loss),Json),Json);
        Require(wire.state.generation==9007199254740993L,"nested token exact beyond 2^53");
        s.Receive(Batch(wire));gain.after=999;wire.techniquePointsFacts[0].cause="mutated";
        var heard=s.DrainTechniquePoints();Require(heard.Length==2&&heard[0].Delta==20&&heard[1].Delta==-20,"zero-net actual sequence survives");
        Require(heard[0].Cause=="EDITOR"&&heard[0].After==120&&heard[0].PresentationParentId=="journal:1","immutable detached facts and parent");
        Require(s.DrainTechniquePoints().Length==0,"drain once");
        s.Receive(Batch(MediaMessage("snapshot",3,2,Fact(2,1,100,120),loss)));
        Require(s.DrainTechniquePoints().Length==0,"fact ID dedup at same revision");
        var corrupt=MediaMessage("delta",4,3,Fact(3,1,100,101));corrupt.techniquePointsFacts[0].state.generation++;
        s.Receive(Batch(corrupt));Require(s.State.Revision==2&&s.DrainTechniquePoints().Length==0&&s.Status=="INVALID_MEDIA_FACTS","foreign generation atomic rejection");
        s.Receive(Batch(MediaMessage("snapshot",5,3,Fact(3,1,100,101))));
        Require(s.DrainTechniquePoints().Length==0,"resync snapshot suppresses transient replay");
        s.Receive(Batch(MediaMessage("delta",6,4,Fact(4,1,101,102))));
        s.Receive(Batch(MediaMessage("resync",7,4)));
        Require(s.DrainTechniquePoints().Length==0&&s.Status=="QUEUE_OVERFLOW","overflow clears pending media");
        s.Receive(Batch(MediaMessage("snapshot",8,4)));
        var receipt=MediaMessage("receipt",9,4,Fact(4,2,102,103));receipt.error="DENIED";
        s.Receive(Batch(receipt));Require(s.DrainTechniquePoints().Length==0&&s.Status=="INVALID_MEDIA_FACTS","receipt cannot inject facts");
        s.Receive(Batch(MediaMessage("snapshot",10,4)));
        var bad=MediaMessage("delta",11,5,Fact(5,1,100,101));bad.techniquePointsFacts[0].delta=2;
        s.Receive(Batch(bad));Require(s.State.Revision==4&&s.DrainTechniquePoints().Length==0,"bad delta no partial application");
        s.Receive(Batch(MediaMessage("snapshot",12,5)));
        var invalidState=MediaMessage("snapshot",13,6,Fact(6,1,100,101));invalidState.terrain="X";
        s.Receive(Batch(invalidState));Require(s.State.Revision==5&&s.DrainTechniquePoints().Length==0,"invalid display state no media");
        s.Receive(Batch(MediaMessage("snapshot",14,5)));
        s.Receive(Batch(Message("delta",15,6)));Require(s.State.Revision==5,"token downgrade rejected after extension");
    }
}
