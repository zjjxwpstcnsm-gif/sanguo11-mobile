from pathlib import Path
import hashlib
r=Path('.')
checks={'app/src/main/java/game/sanguo/mobile/FilamentMapView.java':'3a83c6c455b152bccabf2a1b8b6a9280c1c980c2d5804ebb2dfc53848abb25da','app/src/main/java/game/sanguo/mobile/MainActivity.java':'62a981943699997f6a5bbfd0d7949d55f3de2bf4f5b5d5734e7468f6f1a5dd30','app/src/main/java/game/sanguo/mobile/ScenarioFactionPicker.java':'b5ecb2a006a70b1ce4b791e80a8c3b4962270c9e507469ab1b2c0f94a6ecd366','scripts/test-native-frame-admission.py':'d081ef85a50483afbfe799eacf0fb9ae085aacf5a5625d3bbb85045d5a2d2947'}
for path,sha in checks.items():assert hashlib.sha256((r/path).read_bytes()).hexdigest()==sha, 'concurrent source changed; reconcile rather than overwrite: '+path
assert (r/'version.properties').read_text()=='versionCode=110\nversionName=0.110.0-native-overview-material\n'
def edit(path,pairs):
 p=r/path;s=p.read_text()
 for old,new in pairs:
  assert s.count(old)==1,(path,old,s.count(old));s=s.replace(old,new)
 p.write_text(s)
for name in ('WindowSurfaceRecovery','WindowSurfaceRecoveryState'):
 p=r/f'app/src/main/java/game/sanguo/mobile/{name}.java';assert not p.exists();p.write_text((r/f'.ci/window-recovery/{name}.java').read_text())
p=r/'app/src/main/res/values/window_surface_recovery.xml';assert not p.exists();p.write_text('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <item name="window_surface_recovery" type="id" />\n</resources>\n')
edit('app/src/main/java/game/sanguo/mobile/MainActivity.java',[
 ('    private TurnPlayback playback;','    private TurnPlayback playback;\n    private WindowSurfaceRecovery windowSurfaceRecovery;'),
 ('        super.onCreate(state);gameHost=', '        super.onCreate(state);windowSurfaceRecovery=new WindowSurfaceRecovery(getWindow());gameHost='),
 ('        dateBanner.setContentDescription("当前日期 "+world.date());','        dateBanner.setContentDescription("当前日期 "+world.date());\n        WindowSurfaceRecovery.changed(dateBanner);'),
 ('        if(turnProgress==null)return;','        if(turnProgress==null)return;\n        WindowSurfaceRecovery.changed(turnProgress);'),
 ('    @Override protected void onDestroy(){if(sessionSubscription', '    @Override protected void onDestroy(){if(windowSurfaceRecovery!=null){windowSurfaceRecovery.close();windowSurfaceRecovery=null;}if(sessionSubscription'),
 ('    @Override protected void onResume(){super.onResume();if(map!=null)map.resume(true);}', '    @Override protected void onResume(){super.onResume();if(windowSurfaceRecovery!=null)windowSurfaceRecovery.request();if(map!=null)map.resume(true);}')])
edit('app/src/main/java/game/sanguo/mobile/ScenarioFactionPicker.java',[
 ('    private int selected;','    private int selected;\n    private WindowSurfaceRecovery windowSurfaceRecovery;'),
 ('dialog.setOnDismissListener(d->{map.criticalFrame', 'dialog.setOnDismissListener(d->{if(windowSurfaceRecovery!=null){windowSurfaceRecovery.close();windowSurfaceRecovery=null;}map.criticalFrame'),
 ('void show(){dialog.show();if(a.current3D())', 'void show(){dialog.show();if(dialog.getWindow()!=null)windowSurfaceRecovery=new WindowSurfaceRecovery(dialog.getWindow());if(a.current3D())')])
edit('app/src/main/java/game/sanguo/mobile/FilamentMapView.java',[
 ('            overlay.invalidate();schedule();\n            cpuSamples', '            overlay.invalidate();WindowSurfaceRecovery.changed(this);schedule();\n            cpuSamples'),
 ('    String report(){return "landscape="+LandscapeProfile.ID+"\n"+startupReport()', '    String report(){return "landscape="+LandscapeProfile.ID+"\n"+WindowSurfaceRecovery.report(this)+"\n"+startupReport()')])
edit('scripts/test-native-frame-admission.py',[
 ('  static final class UiMotion {static boolean enabled(){return true;}}', '  static final class UiMotion {static boolean enabled(){return true;}}\n  static final class WindowSurfaceRecovery {static void changed(Object view){}}')])
(r/'version.properties').write_text('versionCode=111\nversionName=0.111.0-native-window-surface-recovery\n')
print('Applied same-Window backing recovery to normal Activity, picker, date, playback and native frame paths; no renderer/Activity replacement and no core/assets/test assertion changes.')
