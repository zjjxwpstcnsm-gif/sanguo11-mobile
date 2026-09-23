from pathlib import Path
import re
root=Path.cwd()
def put(p,s):
 p=root/p;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s)
def update(p,a,b):
 p=root/p;s=p.read_text();assert a in s,(p,a);p.write_text(s.replace(a,b))
src=root/'core/src/main/java/game/sanguo/core/BridgeSession.java'
s=src.read_text()
e=s[s.index('    public static final class Entity {'):s.index('    public static final class Message {')]
m=s[s.index('    public static final class Message {'):s.index('    private final World world;')]
e=e.replace('public static final class Entity','public final class BridgeEntity').replace('Entity(', 'BridgeEntity(').replace('instanceof Entity','instanceof BridgeEntity').replace('Entity e=(Entity)other','BridgeEntity e=(BridgeEntity)other').replace('String name,Hex hex,','String name,int q,int r,').replace('this.q=hex.q;this.r=hex.r;', 'this.q=q;this.r=r;').replace('        BridgeEntity(', '        public BridgeEntity(')
m=m.replace('public static final class Message','public final class BridgeMessage').replace('Message(', 'BridgeMessage(').replace('List<Entity>','List<BridgeEntity>').replace('        BridgeMessage(', '        public BridgeMessage(').replace('Collections.unmodifiableList(entities)','List.copyOf(entities)').replace('Collections.unmodifiableList(removed)','List.copyOf(removed)')
put('game-api/src/main/java/game/sanguo/api/bridge/BridgeEntity.java','package game.sanguo.api.bridge;\n\nimport java.util.Objects;\n\n/** Detached schema-1 entity data; no rule-world or platform references. */\n'+ '\n'.join(x[4:] if x.startswith('    ') else x for x in e.rstrip().splitlines())+'\n')
put('game-api/src/main/java/game/sanguo/api/bridge/BridgeMessage.java','package game.sanguo.api.bridge;\n\nimport java.util.List;\n\n/** Immutable schema-1 transport message. Long counters are never converted to floating point. */\n'+ '\n'.join(x[4:] if x.startswith('    ') else x for x in m.rstrip().splitlines())+'\n')
s=s[:s.index('    public static final class Entity {')]+s[s.index('    private final World world;'):]
s=s.replace('package game.sanguo.core;', 'package game.sanguo.runtime.bridge;\n\nimport game.sanguo.core.*;\nimport game.sanguo.api.bridge.BridgeEntity;\nimport game.sanguo.api.bridge.BridgeMessage;')
s=re.sub(r'\bEntity\b','BridgeEntity',s);s=re.sub(r'\bMessage\b','BridgeMessage',s)
s=s.replace('c.name,c.hex,c.owner','c.name,c.hex.q,c.hex.r,c.owner').replace('u.hex,u.owner','u.hex.q,u.hex.r,u.owner')
put('game-runtime/src/main/java/game/sanguo/runtime/bridge/BridgeSession.java',s);src.unlink()
for name in ['BridgeSessionTest','BridgeFixtureRecorder']:
 src=root/f'core/src/test/java/game/sanguo/core/{name}.java'
 s=src.read_text().replace('package game.sanguo.core;', 'package game.sanguo.runtime.bridge;\n\nimport game.sanguo.core.*;\nimport game.sanguo.api.bridge.BridgeEntity;\nimport game.sanguo.api.bridge.BridgeMessage;').replace('BridgeSession.Message','BridgeMessage').replace('BridgeSession.Entity','BridgeEntity')
 put(f'game-runtime/src/test/java/game/sanguo/runtime/bridge/{name}.java',s);src.unlink()
for src in (root/'app/src').rglob('*.java'):
 s=src.read_text()
 if 'BridgeSession' not in s:continue
 s=s.replace('import game.sanguo.core.BridgeSession;', 'import game.sanguo.runtime.bridge.BridgeSession;\nimport game.sanguo.api.bridge.BridgeEntity;\nimport game.sanguo.api.bridge.BridgeMessage;').replace('BridgeSession.Message','BridgeMessage').replace('BridgeSession.Entity','BridgeEntity')
 if 'BridgeSession' in s and 'import game.sanguo.runtime.bridge.BridgeSession;' not in s:
  s=s.replace('package game.sanguo.mobile;','package game.sanguo.mobile;\nimport game.sanguo.runtime.bridge.BridgeSession;\nimport game.sanguo.api.bridge.BridgeEntity;\nimport game.sanguo.api.bridge.BridgeMessage;')
 src.write_text(s)
put('game-api/build.gradle',"plugins { id 'java-library' }\njava { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }\ntasks.withType(JavaCompile).configureEach { options.encoding = 'UTF-8' }\n")
put('game-runtime/build.gradle',"plugins { id 'java-library' }\njava { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }\ntasks.withType(JavaCompile).configureEach { options.encoding = 'UTF-8' }\ndependencies {\n    api project(':game-api')\n    implementation project(':core')\n}\ntasks.register('verifyBridge', JavaExec) {\n    dependsOn testClasses\n    classpath = sourceSets.test.runtimeClasspath\n    mainClass = 'game.sanguo.runtime.bridge.BridgeSessionTest'\n}\ntasks.named('check') { dependsOn 'verifyBridge' }\ntasks.named('test') { dependsOn 'verifyBridge' }\n")
update('settings.gradle',"include ':core', ':app'", "include ':core', ':game-api', ':game-runtime', ':app'")
update('app/build.gradle',"implementation project(':core')", "implementation project(':core') // Explicit legacy presentation/editor migration dependency.\n    implementation project(':game-api')\n    implementation project(':game-runtime')")
p=root/'scripts/test-unity-u01.sh';s=p.read_text().replace('find core/src/main/java','find core/src/main/java game-api/src/main/java game-runtime/src/main/java').replace('core/src/test/java/game/sanguo/core/Bridge','game-runtime/src/test/java/game/sanguo/runtime/bridge/Bridge').replace('game.sanguo.core.Bridge','game.sanguo.runtime.bridge.Bridge');p.write_text(s)
