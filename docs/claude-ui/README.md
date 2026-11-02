# Claude UI 重构（feat/claude-ui）

依据：`OpenMinis UI v2` 设计稿（12 轮画板）与 HTML 临摹版。业务层（ViewModel / 数据库 / 服务商 / 沙箱）未改动。

## 已落地
- 令牌：`ui/theme/NoirTokens.kt` 改为暖纸配色（#F5F4ED 底、#1F1E1C 墨、#C96442 陶土），深色 #262624；`ChatColors.kt`、`Theme.kt` 同步。
- 字体：Noto Serif SC 子集（`om_serif_*`，正文、标题、数字）+ Geist / Geist Mono（`om_sans_*`、`om_mono_*`）。MiSans 已移除。脚本 `tools/build_serif.py`。
- 正文：助手回复 15.5/1.75 衬线；代码块白卡 + 细边 + 顶栏（`tools/patch_markdown.py`）。
- 思维链：Claude 式折叠行（图标 + 灰色摘要 + 耗时 + 箭头），左侧 1.5dp 时间线，工具行为安静的图标 + 标题 + 目标 + 耗时（`patch_thought_header.py`、`patch_tool_row.py`）。
- 输入框：28dp 白卡；深度芯片按档位着色（Low/Medium/High/XHigh/Max），Max 流光换成设计稿渐变。
- 顶栏：☰ · 居中标题 + 模型名 · 更多；单栏模式下 ☰ 打开左侧会话抽屉（320dp，28dp 圆角）。
- 用户气泡：沙色 #E9E5DA、20dp 圆角。
- 用量面板：r12「本对话」排版（衬线大数字、发丝分隔）。
- 设置：白卡 + #EEEBE6 细边，无阴影；全局开关换成设计稿 40×24 样式；菜单纸色底。
- 厂商图标：`res/drawable/om_logo_*.xml`（`tools/svg2vd.py`）。

## CI
- `ui-compile.yml`：只编 Kotlin（rclone 用桩 aar），推送 feat/claude-ui 自动跑，约 6 分钟。
- 完整包：`build-apk.yml`，预发布 tag `v1.14-claude.N`。
