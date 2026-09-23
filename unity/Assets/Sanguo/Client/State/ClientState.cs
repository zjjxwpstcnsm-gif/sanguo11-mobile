using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using Sanguo.Contracts;
namespace Sanguo.Client
{
    // Immutable display facts. No C# game rules, mutable Java entities, or scene objects.
    public sealed class EntityView
    {
        public readonly string Id, Kind, Name;
        public readonly int Q,R,Owner,Troops,Energy,OfficerId,Gold,Food,Order;
        public EntityView(BridgeEntity e) { Id=e.entityId;Kind=e.kind;Name=e.name;Q=e.q;R=e.r;Owner=e.owner;
            Troops=e.troops;Energy=e.energy;OfficerId=e.officerId;Gold=e.gold;Food=e.food;Order=e.order; }
    }
    public sealed class ClientState
    {
        private Dictionary<string,EntityView> entities=new Dictionary<string,EntityView>();
        public IReadOnlyDictionary<string,EntityView> Entities { get; private set; }
        public string SessionId { get; private set; }
        public string Terrain { get; private set; }
        public int Width { get; private set; }
        public int Height { get; private set; }
        public int Turn { get; private set; }
        public int Player { get; private set; }
        public long Revision { get; private set; }
        public long SnapshotSerial { get; private set; }
        public bool HasSnapshot { get; private set; }
        public ClientState(string sessionId) { SessionId=sessionId;Entities=new ReadOnlyDictionary<string,EntityView>(entities); }
        internal bool Apply(BridgeMessage message)
        {
            bool snapshot=message.type=="snapshot";
            if(!snapshot&&!HasSnapshot)return false;
            if(message.revision<0||(HasSnapshot&&message.revision<Revision))return false;
            if(snapshot&&(message.width<=0||message.height<=0||(long)message.width*message.height>60000||
                message.terrain==null||message.terrain.Length!=(long)message.width*message.height))return false;
            var next=snapshot?new Dictionary<string,EntityView>():new Dictionary<string,EntityView>(entities);
            if(message.entities!=null)foreach(var e in message.entities){
                if(e==null||string.IsNullOrEmpty(e.entityId))return false;
                if(snapshot&&next.ContainsKey(e.entityId))return false;
                next[e.entityId]=new EntityView(e);
            }
            if(message.removed!=null)foreach(var key in message.removed){if(string.IsNullOrEmpty(key))return false;next.Remove(key);}
            // Swap only after full message validation; malformed input never half-applies.
            entities=next;Entities=new ReadOnlyDictionary<string,EntityView>(entities);
            if(snapshot){Terrain=message.terrain;Width=message.width;Height=message.height;HasSnapshot=true;SnapshotSerial++;}
            Revision=message.revision;Turn=message.turn;Player=message.player;return true;
        }
    }
}
