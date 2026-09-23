using System;
using NUnit.Framework;
using Sanguo.Contracts;
using Sanguo.Client;
using UnityEngine;
namespace Sanguo.Tests
{
    public sealed class BridgeClientTests
    {
        [Test] public void JsonUtilityKeepsInt64AndEmptySuccessError()
        {
            var batch=JsonUtility.FromJson<BridgeBatch>("{\"status\":\"OK\",\"messages\":[{\"sequence\":9007199254740995,\"revision\":9007199254740997,\"error\":\"\"}]}");
            Assert.AreEqual(9007199254740995L,batch.messages[0].sequence);
            Assert.AreEqual(9007199254740997L,batch.messages[0].revision);
            Assert.IsTrue(string.IsNullOrEmpty(batch.messages[0].error));
        }
        [Test] public void ExistingJavaFixtureIsAvailableAndPureData()
        {
            var fixture=Resources.Load<TextAsset>("U01_fixture");Assert.IsNotNull(fixture);
            var batch=JsonUtility.FromJson<BridgeBatch>(fixture.text);
            Assert.AreEqual("OK",batch.status);Assert.AreEqual(1,batch.messages[0].schemaVersion);
            Assert.AreEqual(batch.messages[0].width*batch.messages[0].height,batch.messages[0].terrain.Length);
        }
    }
}
