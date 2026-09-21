#!/usr/bin/env python3
"""Apply small, idempotent officer entry hooks; refuse an unexpected source baseline."""
from pathlib import Path


def replace_once(path, before, after):
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    if after in text:
        return
    if text.count(before) != 1:
        raise RuntimeError(f"Expected exactly one integration anchor in {path}: {before[:100]}")
    p.write_text(text.replace(before, after, 1), encoding="utf-8")


replace_once(
    "app/src/main/java/game/sanguo/mobile/MainActivity.java",
    '        root.addView(button("新建游戏 · 选择剧本",v->scenarioPicker()));',
    '        root.addView(button("新建游戏 · 选择剧本",v->scenarioPicker()));\n'
    '        root.addView(button("武将自定义 · 模板与投放",v->startActivity(new Intent(this,CustomOfficerActivity.class))));',
)
replace_once(
    "app/src/main/java/game/sanguo/mobile/EditorUi.java",
    '"导入新武将模板","导出新武将模板"}',
    '"导入新武将模板","导出新武将模板","武将自定义 · 新战局模板库"}',
)
replace_once(
    "app/src/main/java/game/sanguo/mobile/EditorUi.java",
    '                case 8:a.importOfficerTemplate();break;case 9:templates(true);break;',
    '                case 8:a.importOfficerTemplate();break;case 9:templates(true);break;\n'
    '                case 10:a.startActivity(new android.content.Intent(a,CustomOfficerActivity.class));break;',
)
