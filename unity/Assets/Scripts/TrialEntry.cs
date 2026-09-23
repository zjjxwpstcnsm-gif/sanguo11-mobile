using System;
using System.Collections.Generic;
using UnityEngine;

// U01 view: presentation reads Java facts. Editor recording is read-only.
public sealed class TrialEntry : MonoBehaviour
{
    [Serializable] private sealed class Request
    {
        public string type, sessionId, commandId, operation;
        public long revision, clientSequence;
        public int cityId, officerId;
    }
    [Serializable] private sealed class Entity
    {
        public string entityId, kind, name;
        public int q, r, owner, troops, energy, officerId, gold, food, order;
    }
    [Serializable] private sealed class Message
    {
        public string type, sessionId, commandId, error, detail, terrain;
        public int schemaVersion, mapRevision, width, height, turn, player;
        public long sequence, revision;
        public Entity[] entities;
        public string[] removed;
    }
    [Serializable] private sealed class Batch
    {
        public string status;
        public Message[] messages;
        public int dropped;
    }
    private readonly Dictionary<string, Entity> entities = new Dictionary<string, Entity>();
    private string sessionId, scenario = "", status = "Connecting to Java host", terrain;
    private long sequence, revision, clientSequence;
    private int width, height, turn, player, dropped;
    private bool fixture;
    private Texture2D map;
    private Vector2 scroll;
    private float nextPoll;

    private void Start()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        using (var unity = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
        using (var activity = unity.GetStatic<AndroidJavaObject>("currentActivity"))
        using (var intent = activity.Call<AndroidJavaObject>("getIntent"))
        {
            sessionId = intent.Call<string>("getStringExtra", "sanguo.session");
            scenario = intent.Call<string>("getStringExtra", "sanguo.scenario");
        }
        if (string.IsNullOrEmpty(sessionId)) status = "HOST_DATA_MISSING";
        else Send(new Request { type = "snapshot", sessionId = sessionId });
#else
        fixture = true;
        var recording = Resources.Load<TextAsset>("U01_fixture");
        if (recording != null) Receive(JsonUtility.FromJson<Batch>(recording.text));
        status = "EDITOR FIXTURE · read-only · no Java gameplay";
#endif
    }

    private string Send(Request request)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        try
        {
            using (var host = new AndroidJavaClass("game.sanguo.mobile.UnityBridge"))
                return host.CallStatic<string>("request", JsonUtility.ToJson(request));
        }
        catch (Exception ex) { status = "Java bridge unavailable: " + ex.GetType().Name; return status; }
#else
        return "EDITOR_FIXTURE_READ_ONLY";
#endif
    }

    private void Update()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        if (string.IsNullOrEmpty(sessionId) || Time.unscaledTime < nextPoll) return;
        nextPoll = Time.unscaledTime + 0.15f;
        try
        {
            using (var host = new AndroidJavaClass("game.sanguo.mobile.UnityBridge"))
                Receive(JsonUtility.FromJson<Batch>(host.CallStatic<string>("poll", sessionId)));
        }
        catch (Exception ex) { status = "Bridge poll failed: " + ex.GetType().Name; }
#endif
    }

    private void Receive(Batch batch)
    {
        if (batch == null) { status = "INVALID_BATCH"; return; }
        if (batch.status != "OK") { status = batch.status; return; }
        dropped = batch.dropped;
        if (batch.messages == null) return;
        foreach (var m in batch.messages)
        {
            if (m.schemaVersion != 1 || (!fixture && m.sessionId != sessionId))
            { status = "SCHEMA_OR_SESSION_MISMATCH"; continue; }
            if (sequence != 0 && m.sequence != sequence + 1 && m.type != "snapshot")
            { status = "SEQUENCE_GAP · requesting resync"; RequestSnapshot(); break; }
            sequence = m.sequence;
            if (m.type == "resync") { status = "QUEUE_OVERFLOW · requesting resync"; RequestSnapshot(); break; }
            if (m.type == "snapshot") { entities.Clear(); terrain = m.terrain; width = m.width; height = m.height; BuildMap(); }
            if (m.type == "snapshot" || m.type == "delta")
            {
                if (m.entities != null) foreach (var e in m.entities) entities[e.entityId] = e;
                if (m.removed != null) foreach (var key in m.removed) entities.Remove(key);
                revision = m.revision; turn = m.turn; player = m.player;
                status = m.type + " · entities " + entities.Count;
            }
            if (m.type == "receipt")
            {
                revision = m.revision;
                status = m.error == null ? "COMMAND_OK " + m.commandId : "COMMAND_REJECTED " + m.error;
            }
            if (m.type == "event") status = "Java event: " + m.detail;
        }
    }

    private void RequestSnapshot()
    {
        if (!fixture) Send(new Request { type = "snapshot", sessionId = sessionId });
    }

    private void BuildMap()
    {
        if (map != null) Destroy(map);
        if (width <= 0 || height <= 0 || width * height > 60000 || terrain == null || terrain.Length != width * height)
        { status = "INVALID_TERRAIN"; return; }
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

    private void Patrol(Entity city, bool invalid)
    {
        if (fixture) return;
        var cmd = new Request { type = "command", sessionId = sessionId, commandId = Guid.NewGuid().ToString("N"),
            clientSequence = ++clientSequence, revision = revision, operation = "patrol",
            cityId = int.Parse(city.entityId.Substring(5)), officerId = invalid ? -1 : city.officerId };
        status = Send(cmd);
    }

    private void OnGUI()
    {
        float scale = Mathf.Max(1f, Screen.dpi / 160f);
        GUILayout.BeginArea(new Rect(16 * scale, 24 * scale, Screen.width - 32 * scale, Screen.height - 48 * scale));
        GUILayout.Label("Unity U01 · Java authoritative session" + (fixture ? " · EDITOR FIXTURE" : ""));
        GUILayout.Label(scenario + " · turn " + turn + " · revision " + revision + " · " + status);
        GUILayout.Label("session " + sessionId + " · dropped " + dropped);
        if (map != null)
        {
            float size = Mathf.Min(Screen.width - 48 * scale, 300 * scale);
            Rect rect = GUILayoutUtility.GetRect(size, size * height / width);
            GUI.DrawTexture(rect, map, ScaleMode.StretchToFill);
            foreach (var e in entities.Values)
            {
                if (e.q < 0 || e.r < 0 || e.q >= width || e.r >= height) continue;
                var point = new Rect(rect.x + (e.q + 0.5f) * rect.width / width - 3 * scale,
                    rect.y + (e.r + 0.5f) * rect.height / height - 3 * scale, 6 * scale, 6 * scale);
                GUI.color = e.kind == "UNIT" ? Color.yellow : e.owner == player ? Color.cyan : Color.red;
                GUI.DrawTexture(point, Texture2D.whiteTexture); GUI.color = Color.white;
            }
        }
        scroll = GUILayout.BeginScrollView(scroll);
        foreach (var e in entities.Values)
        {
            GUILayout.Label(e.name + " [" + e.entityId + "] " + e.q + "," + e.r + " · troops " + e.troops);
            if (e.kind == "CITY" && e.owner == player && e.officerId >= 0 && !fixture)
            {
                if (e.order < 100 && GUILayout.Button("巡察 · Java 规则")) Patrol(e, false);
                if (GUILayout.Button("错误命令回执 · 无效武将")) Patrol(e, true);
            }
        }
        GUILayout.EndScrollView();
        if (!fixture && GUILayout.Button("Resync")) RequestSnapshot();
        if (GUILayout.Button("返回正式游戏")) Application.Unload();
        GUILayout.EndArea();
    }
    private void OnDestroy() { if (map != null) Destroy(map); }
}
