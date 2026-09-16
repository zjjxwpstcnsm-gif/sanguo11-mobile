"""Add only observation code to the pinned v0.28 test APK; never rebuild the old app."""
from pathlib import Path
import shutil, sys
old = Path(sys.argv[1])
runner = Path('app/src/androidTest/java/game/sanguo/mobile/GameSmokeRunner.java')
new = runner.read_text()
start = new.index('    // BEGIN DISPLACEMENT OBSERVER')
end = new.index('    // END DISPLACEMENT OBSERVER') + len('    // END DISPLACEMENT OBSERVER')
p = old / runner
s = p.read_text().replace('    private String recovery="";', '    private String displacement="";\n    private String recovery="";')
s = s.replace('super.onCreate(arguments);recovery=', 'super.onCreate(arguments);displacement=arguments==null?"":arguments.getString("displacement","");recovery=')
s = s.replace('        try {\n            if(!recovery', '        try {\n            if(!displacement.isEmpty()){displacementFlow();result.putString("stream","DISPLACEMENT PASS: official map selection, cancellation and execution recorded.\\n");finish(Activity.RESULT_OK,result);return;}\n            if(!recovery', 1)
s = s.replace('    private void recoveryFlow()',new[start:end]+'\n\n    private void recoveryFlow()')
p.write_text(s)
fixture = Path('core/src/testFixtures/java/game/sanguo/core/DisplacementFixture.java')
(old/fixture).parent.mkdir(parents=True,exist_ok=True)
shutil.copyfile(fixture,old/fixture)
p = old/'app/build.gradle'
p.write_text(p.read_text().replace('    compileSdk 35','    compileSdk 35\n    sourceSets.androidTest.java.srcDir rootProject.file("core/src/testFixtures/java")'))
