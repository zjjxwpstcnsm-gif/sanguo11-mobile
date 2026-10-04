using System;
namespace Sanguo.Contracts
{
    // Exact schema-1 fields. Int64 values must never pass through float/double.
    [Serializable] public sealed class BridgeRequest
    {
        public string type, sessionId, commandId, operation;
        public long revision, clientSequence;
        public int cityId, officerId;
    }
    [Serializable] public sealed class BridgeEntity
    {
        public string entityId, kind, name;
        public int q, r, owner, troops, energy, officerId, gold, food, order;
    }
    [Serializable] public sealed class StateToken
    {
        public string sessionId;
        public long generation, revision;
    }
    [Serializable] public sealed class TechniquePointsFact
    {
        public string id, parentId, presentationParentId, cause, phase;
        public StateToken state;
        public long sequence;
        public int owner, before, after, delta, cityId, officerId;
    }
    [Serializable] public sealed class BridgeMessage
    {
        public string type, sessionId, commandId, error, detail, terrain;
        public int schemaVersion, mapRevision, width, height, turn, player;
        public long sequence, revision;
        public BridgeEntity[] entities;
        public string[] removed;
        // Optional additive schema-1 fields; old fixtures/messages remain readable.
        public StateToken state;
        public TechniquePointsFact[] techniquePointsFacts;
    }
    [Serializable] public sealed class BridgeBatch
    {
        public string status;
        public BridgeMessage[] messages;
        public int dropped;
    }
    public interface IBridgeTransport : IDisposable
    {
        string SessionId { get; }
        string Scenario { get; }
        bool ReadOnly { get; }
        string Send(BridgeRequest request);
        BridgeBatch Poll();
    }
}
