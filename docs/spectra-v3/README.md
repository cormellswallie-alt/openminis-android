# Spectra v3 — 纯白 / 墨黑 UI 重构

基于 feat/noir-redesign（9aae68c）。设计稿：Spectra v3 预览稿 + 思考卡精修稿。

## 设计原则
- 字体：MiSans 全局（Regular/Medium/Semibold/Bold，按 GB2312 + 应用中文字符子集化，每档约 1.8MB），代码用 JetBrains Mono。
- 配色：纯白 #FFFFFF / 墨 #0A0A0B；深色 #0A0A0B / #F4F4F5。primary 为墨色（深色为白），不再用 Iris 靛紫。
- 无渐变按钮、无渐变文字、无扫光；彩色只属于助手头像（SpectraOrb）。工具类型色只出现在节点图标的字形上。
- 字号下限 12.5sp，次要文字对比度 ≥ 4.5。

## 改动
| 区域 | 文件 |
|---|---|
| 令牌 / 字体 | theme/NoirTokens.kt、Theme.kt、ChatColors.kt、res/font/misans_*.ttf |
| 光谱头像 | theme/SpectraOrb.kt（Idle/Think/Live/Done/Error/Muted） |
| 助手头部 | AssistantHeader：头像=SpectraOrb（流式时 Live）；自定义 Soul 图标仍优先 |
| 用户提问 | ChatUserMessageUI：提问即标题（无气泡、20sp 左对齐），排队/编辑/菜单等全部保留 |
| 执行轨 | ChatStepTimeline：中性方圆节点、运行步骤浮起白卡、折叠摘要卡带指纹条 |
| 思考卡 | ThinkingBlock：节点为思考态头像、1.5dp 紫色边线、笔记排版、计时、复制/全文 |
| 首页 | SessionListScreen：头像 + 问候 + 计数 Hero；分组标题去掉大写小字 |
| 开屏 | NoirLaunch.kt「Ignition」：发丝圆 → 光谱环描绘 → 珍珠核点亮 → 字标上浮 → 退场；系统开屏图标同为发丝圆无缝衔接 |

## 功能保留
`verify_no_feature_loss.py HEAD`：路由 61/61、Composable 537→542、字符串无删除、偏好键 76/76、manifest 32/32。
外观设置（字号、开屏动画开关、FAB 交换、分组等）全部原样生效；自定义 Soul 图标、思考自动展开开关、全文对话框、截断提示均保留。
脚本：docs/spectra-v3/tools/。
