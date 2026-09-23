using System;
using Sanguo.Client;
using UnityEngine;
namespace Sanguo.Presentation
{
public sealed class TrialDiagnostics : IDisposable
{
    private readonly BridgeSync sync;
    private ClientState state { get { return sync.State; } }
    private int width { get { return state.Width; } }
    private int height { get { return state.Height; } }
    private string terrain { get { return state.Terrain; } }
    private Texture2D map;
    private Vector2 scroll;
    private string diagnostic="";
    private long built=-1;
    public TrialDiagnostics(BridgeSync sync){this.sync=sync;}
    public void Refresh(){if(state.HasSnapshot&&built!=state.SnapshotSerial){built=state.SnapshotSerial;BuildMap();}}
    private void BuildMap()
    {
        if (map != null) UnityEngine.Object.Destroy(map);
        if (width <= 0 || height <= 0 || width * height > 60000 || terrain == null || terrain.Length != width * height)
        { diagnostic = "INVALID_TERRAIN"; return; }
        map = new Texture2D(width, height, TextureFormat.RGBA32, false) { filterMode = FilterMode.Point };
        var colors = new Color32[width * height];
        for (int r = 0; r < height; r++) for (int q = 0; q < width; q++)
        {
            char code = terrain[r * width + q];
            colors[(height - 1 - r) * width + q] = code == 'I' || code == 'D' || code == 'F' || code == 'O'
                ? new Color32(33, 88, 137, 255) : code == 'J' ? new Color32(17, 28, 37, 255)
                : code == 'C' ? new Color32(104, 105, 94, 255) : new Color32(77, 108, 74, 255);
        }
        map.SetPixels32(colors); map.Apply(false, true);
    }

    public void Draw()
    {
        float scale = Mathf.Max(1f, Screen.dpi / 160f);
        GUILayout.BeginArea(new Rect(16 * scale, 24 * scale, Screen.width - 32 * scale, Screen.height - 48 * scale));
        GUILayout.Label("Unity U01 · Java authoritative session" + (sync.ReadOnly ? " · EDITOR FIXTURE" : ""));
        GUILayout.Label(sync.Scenario + " · turn " + state.Turn + " · revision " + state.Revision + " · " + sync.Status);
        GUILayout.Label("session " + state.SessionId + " · dropped " + sync.Dropped);
        if (map != null)
        {
            float size = Mathf.Min(Screen.width - 48 * scale, 300 * scale);
            Rect rect = GUILayoutUtility.GetRect(size, size * height / width);
            GUI.DrawTexture(rect, map, ScaleMode.StretchToFill);
            foreach (var e in state.Entities.Values)
            {
                if (e.Q < 0 || e.R < 0 || e.Q >= width || e.R >= height) continue;
                var point = new Rect(rect.x + (e.Q + 0.5f) * rect.width / width - 3 * scale,
                    rect.y + (e.R + 0.5f) * rect.height / height - 3 * scale, 6 * scale, 6 * scale);
                GUI.color = e.Kind == "UNIT" ? Color.yellow : e.Owner == state.Player ? Color.cyan : Color.red;
                GUI.DrawTexture(point, Texture2D.whiteTexture); GUI.color = Color.white;
            }
        }
        scroll = GUILayout.BeginScrollView(scroll);
        foreach (var e in state.Entities.Values)
        {
            GUILayout.Label(e.Name + " [" + e.Id + "] " + e.Q + "," + e.R + " · troops " + e.Troops);
            if (e.Kind == "CITY" && e.Owner == state.Player && e.OfficerId >= 0 && !sync.ReadOnly)
            {
                if (GUILayout.Button("征兵 · Java 规则")) sync.Execute("recruit",e);
                if (e.Order < 100 && GUILayout.Button("巡察 · Java 规则")) sync.Execute("patrol",e);
                if (GUILayout.Button("错误命令回执 · 无效武将")) sync.Execute("patrol",e,true);
            }
        }
        GUILayout.EndScrollView();
        if (!sync.ReadOnly && GUILayout.Button("Resync")) sync.RequestSnapshot();
        if (GUILayout.Button("返回正式游戏")) Application.Unload();
        GUILayout.EndArea();
    }
    public void Dispose(){if(map!=null){UnityEngine.Object.Destroy(map);map=null;}}
}
}
