using Sanguo.Contracts;
using Sanguo.Client;
using Sanguo.Transport;
using Sanguo.Presentation;
using UnityEngine;

// Stable scene component and GUID. Composition only; no JNI, rules, or replicated state implementation.
public sealed class TrialEntry : MonoBehaviour
{
    private IBridgeTransport transport;
    private BridgeSync sync;
    private TrialDiagnostics diagnostics;
    private float nextPoll;
    private void Start()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        transport=new AndroidBridgeTransport();
#else
        var asset=Resources.Load<TextAsset>("U01_fixture");
        transport=new FixtureTransport(asset==null?null:JsonUtility.FromJson<BridgeBatch>(asset.text));
#endif
        sync=new BridgeSync(transport);diagnostics=new TrialDiagnostics(sync);sync.Start();diagnostics.Refresh();
    }
    private void Update(){if(sync!=null&&Time.unscaledTime>=nextPoll){nextPoll=Time.unscaledTime+.15f;sync.Poll();diagnostics.Refresh();}}
    private void OnGUI(){if(diagnostics!=null)diagnostics.Draw();}
    private void OnDestroy(){if(diagnostics!=null)diagnostics.Dispose();if(transport!=null)transport.Dispose();}
}
