using System;
using Sanguo.Contracts;
namespace Sanguo.Client
{
    // Ordered display replication only. Rule decisions and command results belong to Java.
    public sealed class BridgeSync
    {
        public readonly ClientState State;
        private readonly IBridgeTransport transport;
        private long sequence,clientSequence;
        private bool waiting;
        public string Status { get; private set; } = "Connecting to Java host";
        public int Dropped { get; private set; }
        public bool ReadOnly { get { return transport.ReadOnly; } }
        public string Scenario { get { return transport.Scenario; } }
        public BridgeSync(IBridgeTransport transport){this.transport=transport;State=new ClientState(transport.SessionId);}
        public void Start(){if(ReadOnly)Poll();else RequestSnapshot();}
        public void RequestSnapshot()
        {
            if(ReadOnly)return;
            waiting=true;Status=transport.Send(new BridgeRequest {type="snapshot",sessionId=State.SessionId});
        }
        public void Poll(){var batch=transport.Poll();if(batch!=null)Receive(batch);}
        private void Resync(string error){Status=error;if(!waiting&&!ReadOnly){RequestSnapshot();Status=error;}}
        public void Receive(BridgeBatch batch)
        {
            if(batch==null){Status="INVALID_BATCH";return;}
            if(batch.status!="OK"){Status=batch.status??"INVALID_BATCH";return;}
            Dropped=batch.dropped;if(batch.messages==null)return;
            foreach(var m in batch.messages){
                if(m==null||m.schemaVersion!=1||m.sessionId!=State.SessionId){Status="SCHEMA_OR_SESSION_MISMATCH";continue;}
                if(m.sequence<=0||m.sequence<=sequence){Resync("INVALID_SEQUENCE");break;}
                if(m.type!="snapshot"&&((sequence!=0&&m.sequence!=sequence+1)||waiting||!State.HasSnapshot)){
                    // Resync markers are allowed while waiting; only a complete snapshot unblocks deltas.
                    if(m.type=="resync"){waiting=false;Resync("QUEUE_OVERFLOW");}else Resync("SEQUENCE_GAP");break;
                }
                if(m.type=="resync"){sequence=m.sequence;waiting=false;Resync("QUEUE_OVERFLOW");break;}
                if(m.type=="snapshot"||m.type=="delta"){
                    if(!State.Apply(m)){waiting=false;Resync("INVALID_STATE");break;}
                    sequence=m.sequence;waiting=false;Status=m.type+" · entities "+State.Entities.Count;
                }else{
                    sequence=m.sequence;
                    if(m.type=="receipt")Status=string.IsNullOrEmpty(m.error)?"COMMAND_OK "+m.commandId:"COMMAND_REJECTED "+m.error;
                    if(m.type=="event")Status="Java event: "+m.detail;
                }
            }
            if(ReadOnly)Status="EDITOR FIXTURE · read-only · no Java gameplay";
        }
        public void Execute(string operation,EntityView city,bool invalidOfficer=false)
        {
            if(ReadOnly){Status="EDITOR_FIXTURE_READ_ONLY";return;}
            if(waiting||!State.HasSnapshot){Status="SNAPSHOT_REQUIRED";return;}
            if(operation!="recruit"&&operation!="patrol"){Status="UNKNOWN_OPERATION";return;}
            int cityId;
            if(city==null||!city.Id.StartsWith("site:",StringComparison.Ordinal)||!int.TryParse(city.Id.Substring(5),out cityId)){
                Status="INVALID_ENTITY";return;
            }
            if(clientSequence==long.MaxValue){Status="CLIENT_SEQUENCE_EXHAUSTED";return;}
            Status=transport.Send(new BridgeRequest {type="command",sessionId=State.SessionId,commandId=Guid.NewGuid().ToString("N"),
                clientSequence=++clientSequence,revision=State.Revision,operation=operation,cityId=cityId,officerId=invalidOfficer?-1:city.OfficerId});
        }
    }
}
