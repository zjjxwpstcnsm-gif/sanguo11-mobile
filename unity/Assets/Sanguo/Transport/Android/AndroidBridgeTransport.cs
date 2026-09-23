using System;
using Sanguo.Contracts;
using UnityEngine;
namespace Sanguo.Transport
{
    // The only Unity production file permitted to use JNI. No scene/game rules here.
    public sealed class AndroidBridgeTransport : IBridgeTransport
    {
        public string SessionId { get; private set; }
        public string Scenario { get; private set; }
        public bool ReadOnly { get { return false; } }
        public AndroidBridgeTransport()
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            using(var unity=new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
            using(var activity=unity.GetStatic<AndroidJavaObject>("currentActivity"))
            using(var intent=activity.Call<AndroidJavaObject>("getIntent")){
                SessionId=intent.Call<string>("getStringExtra","sanguo.session");Scenario=intent.Call<string>("getStringExtra","sanguo.scenario");
            }
#endif
        }
        public string Send(BridgeRequest request)
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            if(string.IsNullOrEmpty(SessionId))return "HOST_DATA_MISSING";
            try {using(var host=new AndroidJavaClass("game.sanguo.mobile.UnityBridge"))return host.CallStatic<string>("request",JsonUtility.ToJson(request));}
            catch(Exception ex){return "BRIDGE_UNAVAILABLE "+ex.GetType().Name;}
#else
            return "ANDROID_HOST_UNAVAILABLE";
#endif
        }
        public BridgeBatch Poll()
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            if(string.IsNullOrEmpty(SessionId))return new BridgeBatch{status="HOST_DATA_MISSING"};
            try {using(var host=new AndroidJavaClass("game.sanguo.mobile.UnityBridge"))return JsonUtility.FromJson<BridgeBatch>(host.CallStatic<string>("poll",SessionId));}
            catch(Exception ex){return new BridgeBatch{status="POLL_FAILED "+ex.GetType().Name};}
#else
            return new BridgeBatch{status="ANDROID_HOST_UNAVAILABLE"};
#endif
        }
        public void Dispose() { } // JNI references are scoped with using; host session is not destroyed.
    }
}
