from pathlib import Path
import base64, gzip, hashlib, subprocess
root = Path.cwd()
subprocess.run(['git', 'diff', '--exit-code', '4972cd6073ec57cacda80c331b722241b519fab8', '--', 'core', 'game-api', 'game-runtime', 'app', 'unity', 'scripts', 'settings.gradle'], check=True)
assert not (root/'game-runtime/src/main/java/game/sanguo/runtime/GameSession.java').exists()
parts = [(root/f'.ci/runtime-transfer/{i}.b64').read_text().strip() for i in range(4)]
# Correct the verified single-character transport transcription; decoded source is hash-gated.
parts[3] = parts[3].replace('tpbiMeXZN0', 'tpbiMeZN0')
source = gzip.decompress(base64.b64decode(''.join(parts)))
assert hashlib.sha256(source).hexdigest() == 'b6621de6c6170fa617b34fc68a19fb01167084418105eb0dd04c9fd16e814e2e'
exec(compile(source, 'verified-runtime-refactor', 'exec'))
