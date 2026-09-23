using UnityEngine;

// U00 reads only session identity from the Android host; it cannot modify the Java World.
public sealed class TrialEntry : MonoBehaviour
{
    private string scenario = "HOST_DATA_MISSING";
    private string source = "unknown";
    private int columns;
    private int rows;

    private void Start()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        using (var player = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
        using (var activity = player.GetStatic<AndroidJavaObject>("currentActivity"))
        using (var intent = activity.Call<AndroidJavaObject>("getIntent"))
        {
            scenario = intent.Call<string>("getStringExtra", "sanguo.scenario") ?? scenario;
            source = intent.Call<string>("getStringExtra", "sanguo.source") ?? source;
            columns = intent.Call<int>("getIntExtra", "sanguo.width", 0);
            rows = intent.Call<int>("getIntExtra", "sanguo.height", 0);
        }
#endif
    }

    private void OnGUI()
    {
        var scale = Mathf.Max(1f, Screen.dpi / 160f);
        var area = new Rect(24 * scale, 32 * scale, Screen.width - 48 * scale, Screen.height - 64 * scale);
        GUILayout.BeginArea(area);
        GUILayout.Label("Unity 试用 · U00");
        GUILayout.Label("Unity " + Application.unityVersion + " · 工程 " + Application.version);
        GUILayout.Label("正式剧本：" + scenario);
        GUILayout.Label("宿主地图：" + columns + " × " + rows);
        GUILayout.Label("宿主源码：" + source);
        if (GUILayout.Button("返回正式游戏", GUILayout.Height(56 * scale)))
            Application.Unload();
        GUILayout.EndArea();
    }
}
