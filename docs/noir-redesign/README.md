# Minis Noir — 视觉重构说明

分支：`feat/noir-redesign`（基于 `feat/agent-efficiency`）

## 原则

1. **只换皮，不动骨**：全部 61 个路由、531 个 Composable、2114 个字符串 key（含 18 种语言）、75 个偏好 key、32 个 Manifest 组件都保留。可用 `python3 docs/noir-redesign/tools/verify_no_feature_loss.py` 复核，必须全部输出 `OK`。
2. **主题集中化**：全应用约 1029 处 `colorScheme.*`、276 处 `ChatColors.*` 都从 `Theme.kt` / `ChatColors.kt` 取色，所以改令牌就能覆盖到每一页，不需要逐页重写。
3. **金色要节制**：香槟金只用在少数地方（开屏、思考指示、字标细线），不进入 `colorScheme.primary`，以免 1000 多处调用一起变“金光闪闪”。

## 设计令牌（`ui/theme/NoirTokens.kt`）

| 令牌 | 浅色 | 深色 | 用途 |
|---|---|---|---|
| 背景 | Paper `#F3F0E9` | Onyx `#0B0B0C` | 页面 |
| 墨色 | Ink `#141413` | Ivory `#EFEBE3` | 主文字、主按钮、用户气泡 |
| 金 | Gold `#CDAE74`（GoldDeep `#A98754` 用于浅色文字） | 同 | 点缀 |
| 发丝线 | `outlineVariant` | 同 | 卡片 0.5dp 描边、分隔线 |

字体：Inter（400/500/600/700）负责正文和 UI；Instrument Serif（含斜体）负责字标和大标题；JetBrains Mono 负责代码（保留原有）。中文回落到系统字体。

形状：卡片 22dp，对话框 / 底部弹层 28dp，用户气泡为 22/22/6/22（尾角指向发送方）。

## 改动清单

| 区域 | 文件 | 内容 |
|---|---|---|
| 配色 | `ui/theme/Theme.kt` | M3 浅 / 深两套配色全部换成 Noir；Shapes 和 Typography 改用 Inter |
| 聊天语义色 | `ui/theme/ChatColors.kt` | 54 个语义色重新映射；新增 `userBubbleText` |
| iOS 系统色 | 50 个 UI 文件，237 处 | systemBlue / Green / Orange / Red 等饱和色改成低饱和的 Noir 珠宝色（`tools/remap_ios_colors.py`） |
| 白字压主色 | 5 处 | 深色模式下 primary 变成象牙白，原来写死的白字 / 白底改为 `onPrimary` |
| 设置页 | `SettingsScreen.kt`、`SettingsComponents.kt`、`components/SettingsSection.kt` | 眉题（11sp，0.18em 字距，全大写）+ 22dp 发丝线卡片；饱和色图标块改为单色图块，右上角留一颗 4dp 的原色“宝石点”，保留原有的分类辨识度 |
| 首页 | `SessionListScreen.kt` | 珍珠 + 衬线字标“Minis”；分组标题改用眉题样式；FAB 图标颜色跟随主题 |
| 对话 | `ChatUserMessageUI.kt` | 墨色气泡 + 纸色文字，带尾角 |
| 窗口 | `values*/themes.xml`、`colors.xml` | 系统绘制的第一帧就是 Paper / Onyx，不再先闪白 / 黑 |

## 动效

| 动效 | 位置 | 参数 |
|---|---|---|
| 开屏 | `ui/theme/NoirLaunch.kt` + `MainActivity` | 1.45s：珍珠升起（expo-out）→ 金色光晕 → 衬线字标上浮 → 金线展开 → 整体放大 6%、上移 18dp，同时幕布淡出。点击可跳过。只在从桌面图标冷启动时播放；屏幕旋转、深链接、分享、快捷方式都不播放。系统关闭动画时也不播放 |
| 系统开屏（Android 12+） | `values-v31/themes.xml`、`drawable*/noir_splash_pearl.xml` | 系统开屏直接画同一颗珍珠（窗口正中，76dp），Compose 叠层接手时不跳位 |
| 页面转场 | `ui/theme/NoirMotion.kt` → `AppNavigation` | 进入：新页面从 10% 位置滑入，380ms expo-out；被覆盖的页面视差后退 6%、缩小到 98.5% 并淡出。退出段（220ms）仍比进入段短，保留 T333/T342 的防连点保证 |
| 思考指示 | `ChatIndicators.kt::TypingIndicator` | 呼吸珍珠（1.6s）+ 金色光带扫过文字（2.2s）；沿用全局 30fps 装饰时钟，只在 draw 阶段读取，不触发重组 |
| 发送按钮 | `SendReturnModeMenu.kt::ComposerSendButton` | 按下回弹到 90%；可发送 / 不可发送之间颜色渐变；箭头在可发送时上升到位 |
| 会话列表入场 | `SessionListScreen.kt` | 首屏最多 12 行，依次上浮 14dp 并淡入，间隔 35ms；`rememberSaveable` 保证返回时不重播 |
| 开关 | 设置 → 外观 → 开屏动画 | 默认开启，18 种语言均已翻译 |

## 已知取舍

- `colorScheme.primary` 现在是墨色 / 象牙白，Material 组件（Switch、Slider、Checkbox 等）都会跟着变成单色；这是有意为之。
- 第三方 / 终端的 ANSI 颜色和代码高亮配色没有改动（`AnsiParser.kt`、`SyntaxHighlighter.kt` 不参与映射）。
