using Sanguo.Contracts;
namespace Sanguo.Transport
{
    // The caller supplies the existing Java recording; this source never executes commands.
    public sealed class FixtureTransport : IBridgeTransport
    {
        private BridgeBatch recording;
        public string SessionId { get; private set; }
        public string Scenario { get { return "U01 recording"; } }
        public bool ReadOnly { get { return true; } }
        public FixtureTransport(BridgeBatch recording){
            this.recording=recording;SessionId=recording!=null&&recording.messages!=null&&recording.messages.Length>0?recording.messages[0].sessionId:"";
        }
        public string Send(BridgeRequest request){return "EDITOR_FIXTURE_READ_ONLY";}
        public BridgeBatch Poll(){var value=recording;recording=null;return value;}
        public void Dispose(){recording=null;}
    }
}
