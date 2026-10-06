# 供 B 页面消费的主题机制

A 提供 UiTheme.text(TextView) 默认可读字体/状态色、UiTheme.readable(TextView) 富文本文字与禁用状态、UiTheme.readableTree(View) 已构建树、UiTheme.dialog(AlertDialog) 窗口/按钮/普通文字。B 可在其唯一16页面建树或更新富文本后应用；A 不编辑 B 页面。现有语义色满足对比度会保留，低对比文字提升到 SURFACE 上4.5:1，文字alpha为255；禁用色MUTED。对白/列表异步新增或重新绑定的行需在绑定后调用。

地图 FactionColors.color 只提供势力填色，FactionColors.textColor 为LABEL_BACKGROUND不透明深色牌提供4.5:1文字，不将提亮文字写回领土颜色。选中牌保持金色文字。AppTheme补齐原生默认正文/次要/提示色。主题不调用规则/保存/RNG。

目前是源码与主机配色证据；新APK深底/选中/禁用/富文本/列表/弹窗/触控的实装证据另列，不能据主机测试称正常界面完成。
