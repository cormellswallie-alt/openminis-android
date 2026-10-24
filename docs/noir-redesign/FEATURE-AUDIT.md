## 1. 导航与页面

### 1.0 总量（`ui/navigation/AppNavigation.kt`，`object Routes` 在 76-202 行）

| 指标 | 数值 |
|---|---|
| `object Routes` 内 `const val` 声明 | **61 个**（其中 `BACKUP_INITIAL_TAB_KEY` 是 `SavedStateHandle` 的 key，不是导航目的地） |
| 真实路由 / 注册目的地 | **60 个**：59 个 `composable(...)` + 1 个 `dialog(...)`（`Routes.FILE_PREVIEW`） |
| 一一对应校验 | 60 个路由常量全部有注册，无「声明未注册」也无「注册了未声明」（脚本逐条比对） |

导航容器：`AppNavigation.kt` 的 `NavHost`，`startDestination` 逻辑（`startDestination` 计算块）：
- 带 `minis://session/<id>/<resource-path>` 的 HTML 快捷方式冷启动 → 直接 `Routes.chat(sessionId)` 并预置 `pendingHtmlPreview`；
- App 图标快捷方式（new / voice / camera chat，见 `res/xml/shortcuts.xml`）→ `Routes.chat("__new__<uuid>")`；
- 否则 → `Routes.SESSION_LIST`。

> 注意：本次盘点期间工作区正被并发修改（Noir 改造：`AppNavigation.kt` 的转场已改为 `NoirMotion.pushEnter/pushExit/popEnter/popExit`，`ui/theme/NoirMotion.kt` 新增）。**路由名、页面、Screen 函数均未变**；下文行号以当前工作区为准，若后续继续改动，以路由名 + Screen 名为锚点。

### 1.1 对话 / 会话（2）

| Route | Composable | 文件（Screen 定义行） |
|---|---|---|
| `SESSION_LIST = "sessions"` | `ChatSplitScaffoldRoute(initialSessionId = null)` → 列表窗格 `SessionListScreen`；双栏时详情窗格空态 `NoConversationSelected` | `ui/navigation/ChatSplitScaffold.kt:852`（`ChatSplitScaffoldRoute`）、`ui/sessions/SessionListScreen.kt:501`、`ChatSplitScaffold.kt:706`（`NoConversationSelected`） |
| `CHAT = "chat/{sessionId}"` | `ChatSplitScaffoldRoute(initialSessionId = {sessionId})` → `ChatScreen` | `ui/chat/ChatScreen.kt:512` |

> 两个路由都渲染**同一个** `ChatSplitScaffold`，区别只是初始会话；保留两条路由是为了深链 / 分享 / Move-to / 通知 / 桌面小组件的老路径不变（源码注释 550-559）。

### 1.2 设置主页（1）

| Route | Composable | 文件 |
|---|---|---|
| `SETTINGS = "settings"` | `SettingsScreen` | `ui/settings/SettingsScreen.kt:78` |

### 1.3 服务商与模型（12）

| Route | Composable | 文件（行） |
|---|---|---|
| `PROVIDER_LIST = "providers"` | `ProviderListScreen` | `ui/settings/ProviderListScreen.kt`（`fun ProviderListScreen`） |
| `ADD_PROVIDER = "add_provider"` | `AddProviderScreen` | `ui/settings/AddProviderScreen.kt` |
| `PROVIDER_DETAIL = "provider/{instanceId}"` | `ProviderDetailScreen` | `ui/settings/ProviderDetailScreen.kt:84` |
| `SHADOW_VOICE_DETAIL = "voice_service/{instanceId}"` | `ShadowVoiceDetailScreen`（只读 Voice Service 影子） | `ui/settings/ShadowVoiceDetailScreen.kt:42` |
| `MODEL_GROUPS = "model_groups"` | `ModelGroupsScreen`（含 Agent Loop 区段） | `ui/settings/ModelGroupsScreen.kt` |
| `MODEL_GROUP_DETAIL = "model_group/{groupId}"` | `ModelGroupDetailScreen` | `ui/settings/ModelGroupDetailScreen.kt` |
| `ADD_MODELS_TO_GROUP = "add_models_to_group/{groupId}"` | `AddModelsToGroupScreen` | `ui/settings/AddModelsToGroupScreen.kt` |
| `ADD_MODELS_TO_AGENT_LOOP = "add_models_to_agent_loop"` | `AddAgentLoopModelsScreen` | `ui/settings/AgentLoopAddSheets.kt` |
| `ADD_GROUPS_TO_AGENT_LOOP = "add_groups_to_agent_loop"` | `AddAgentLoopGroupsScreen` | `ui/settings/AgentLoopAddSheets.kt` |
| `AGENT_LOOP_MODELS = "agent_loop_models"` | **兼容空壳**：`LaunchedEffect { safePopBackStack() }`（早期深链兼容，Agent Loop 已并入 ModelGroups） | `AppNavigation.kt`（该 composable 体内） |
| `MODEL_ENTRY_DETAIL = "model_entry/{instanceId}/{entryId}"` | `ModelEntryDetailScreen`（参数 `Uri.decode`，id 内含 `/`） | `ui/settings/ModelEntryDetailScreen.kt:48` |
| `ADD_CUSTOM_MODEL = "add_custom_model/{instanceId}"` | `AddCustomModelScreen` | `ui/settings/AddCustomModelScreen.kt` |

### 1.4 智能体 / 子代理（5）

| Route | Composable | 文件（行） |
|---|---|---|
| `AGENTS = "agents"` | `SubAgentsScreen`（与 `SUB_AGENTS` 同一屏） | `ui/settings/SubAgentsScreen.kt:69` |
| `SUB_AGENTS = "sub_agents"` | `SubAgentsScreen` | 同上 |
| `SUB_AGENT_DETAIL = "sub_agent/{agentId}"` | `SubAgentDetailScreen` | `ui/settings/SubAgentsScreen.kt:271` |
| `AGENT_TOOLS = "agent_tools"` | `AgentToolsSettingsScreen` | `ui/settings/AgentToolsSettingsScreen.kt:27` |
| `AGENT_TRANSCRIPT = "agent_transcript/{sessionId}"` | `AgentTranscriptScreen`（子代理只读全文） | `ui/chat/HelperUi.kt`（`fun AgentTranscriptScreen`） |

### 1.5 技能 / 记忆 / MCP / Soul / 环境变量（9）

| Route | Composable | 文件（行） |
|---|---|---|
| `SKILLS = "skills"` | `SkillsManagementScreen` | `ui/settings/SkillsManagementScreen.kt:115` |
| `SKILL_DETAIL = "skill/{skillId}"` | `SkillDetailScreen` | `SkillsManagementScreen.kt:573` |
| `SKILL_FILE = "skill_file/{skillId}/{relativePath}"` | `SkillFileViewerScreen` | `SkillsManagementScreen.kt:1119` |
| `MINIS_SKILLS_BROWSER = "minis_skills_browser"` | `MinisSkillsBrowserScreen`（在线技能库） | `ui/settings/MinisSkillsBrowserScreen.kt` |
| `MEMORY = "memory"` | `MemoryManagementScreen` | `ui/settings/MemoryManagementScreen.kt:60` |
| `MEMORY_FILE_EDIT = "memory_file/{fileName}/{isGlobal}"` | `MemoryFileEditScreen` | `MemoryManagementScreen.kt:287` |
| `MCP = "mcp"` | `MCPIntegrationsScreen` | `ui/settings/MCPIntegrationsScreen.kt:66` |
| `SOUL = "soul"` | `SoulSettingsScreen` | `ui/settings/SoulSettingsScreen.kt:93` |
| `ENV_VARS = "env_vars"` | `EnvironmentVariablesScreen` | `ui/settings/EnvironmentVariablesScreen.kt:54` |

### 1.6 存储 / 文件 / 备份（12）

| Route | Composable | 文件（行） |
|---|---|---|
| `STORAGE = "storage"` | `StorageManagementScreen` | `ui/settings/StorageManagementScreen.kt:104` |
| `SESSION_STORAGE_DETAIL = "session_storage/{sessionId}"` | `SessionStorageDetailScreen` | `ui/settings/StorageManagementScreen.kt` |
| `BACKUP = "backup"` | `BackupAndRestoreScreen`（Backup / Restore 双 Tab） | `ui/settings/backup/BackupAndRestoreScreen.kt` |
| `BACKUP_HISTORY_DETAIL = "backup_history_detail"`（注册为 `…/{recordId}`） | `BackupHistoryDetailScreen` | `ui/settings/backup/BackupHistoryDetailScreen.kt` |
| `BACKUP_DESTINATIONS = "backup_destinations"` | `RcloneDestinationsScreen` | `ui/settings/backup/RcloneDestinationsScreen.kt` |
| `BACKUP_DESTINATION_BROWSE = "backup_destination_browse"`（`…/{remoteName}`） | `BackupDestinationBrowseScreen` | `ui/settings/backup/BackupDestinationBrowseScreen.kt` |
| `RESTORE_SERVERS = "restore_servers"` | `RestoreServersScreen` | `ui/settings/backup/RestoreServersScreen.kt` |
| `RESTORE_BROWSE = "restore_browse"`（`…/{remoteName}`） | `RestoreBrowseScreen` | `ui/settings/backup/RestoreBrowseScreen.kt` |
| `MOUNTED_FOLDERS = "mounted_folders"` | `MountedFoldersScreen` | `ui/settings/MountedFoldersScreen.kt` |
| `MOUNTED_FOLDERS_DETAIL = "mounted_folders_detail/{mountId}"` | `MountDetailScreen` | `ui/settings/MountDetailScreen.kt` |
| `SHARED_FOLDERS = "shared_folders"` | `SharedFoldersScreen` | `ui/settings/SharedFoldersScreen.kt` |
| `SHARED_FOLDERS_DETAIL = "shared_folders_detail/{folderId}"` | `SharedFolderDetailScreen` | `ui/settings/SharedFolderDetailScreen.kt` |

### 1.7 沙箱 / 终端 / 镜像 / 文件浏览（6）

| Route | Composable | 文件（行） |
|---|---|---|
| `ROOTFS_MANAGEMENT = "rootfs_management"` | `RootfsManagementScreen`（含镜像设置区） | `ui/sandbox/RootfsManagementScreen.kt:61` |
| `MIRROR_CATEGORY_DETAIL = "mirror_category/{categoryKey}"` | `MirrorCategoryDetailScreen` | `ui/sandbox/MirrorSettingsScreen.kt` |
| `FILE_BROWSER = "file_browser"` | `FileBrowserScreen` | `ui/sandbox/FileBrowserScreen.kt` |
| `FILE_PREVIEW = "file_preview"` | `FilePreviewScreen`，**注册为 `dialog(...)`**（`usePlatformDefaultWidth=false`、`decorFitsSystemWindows=false`；数据经 `FilePreviewHolder.currentItem` 传入，无 nav 参数） | `ui/sandbox/FilePreviewScreen.kt:99` |
| `CHAT_FILES = "chat_files/{sessionId}?path={path}"` | `FileBrowserScreen`（根目录 = `<rootfs>/var/minis`） | `ui/sandbox/FileBrowserScreen.kt` |
| `TERMINAL = "terminal?initCommand={initCommand}&sessionId={sessionId}"` | `TerminalScreen`（`TerminalSession(context)`，支持 initCommand 预填与 sessionId 绑定环境/chdir） | `ui/terminal/TerminalScreen.kt:81` |

### 1.8 权限（3）

| Route | Composable | 文件（行） |
|---|---|---|
| `PERMISSIONS = "permissions"` | `OffloadPermissionScreen` | `ui/settings/OffloadPermissionScreen.kt:43` |
| `SHIZUKU = "shizuku"` | `ShizukuPermissionScreen` | `ui/settings/ShizukuPermissionScreen.kt` |
| `SYSTEM_PERMISSIONS = "system_permissions"` | `SystemPermissionsScreen` | `ui/settings/SystemPermissionsScreen.kt` |

### 1.9 用量 / 外观 / 后台（3）

| Route | Composable | 文件（行） |
|---|---|---|
| `USAGE_STATS = "usage_stats"` | `UsageStatsScreen` | `ui/settings/UsageStatsScreen.kt` |
| `APPEARANCE = "appearance"` | `AppearanceScreen` | `ui/settings/AppearanceScreen.kt:255` |
| `BACKGROUND = "background"` | `BackgroundSettingsScreen` | `ui/settings/BackgroundSettingsScreen.kt:82` |

### 1.10 定时任务（3）

| Route | Composable | 文件（行） |
|---|---|---|
| `SCHEDULED_TASKS = "scheduled_tasks"` | `ScheduledTasksScreen` | `ui/scheduled/ScheduledTasksScreen.kt:73` |
| `SCHEDULED_TASK_EDIT = "scheduled_tasks/edit?taskId={taskId}"` | `ScheduledTaskEditScreen` | `ui/scheduled/ScheduledTaskEditScreen.kt` |
| `SCHEDULED_TASK_RUNS = "scheduled_tasks/runs/{taskId}"` | `ScheduledTaskRunsScreen`（单任务执行记录） | `ui/scheduled/ScheduledTaskRunsScreen.kt` |

### 1.11 日志 / 关于 / 引导（4）

| Route | Composable | 文件（行） |
|---|---|---|
| `LOGS = "logs"` | `LogManagementScreen`（Tab：Logs / Config Changes） | `ui/settings/LogManagementScreen.kt` |
| `LOG_DETAIL = "log_detail/{fileName}"` | `LogDetailScreen` | `ui/settings/LogManagementScreen.kt` |
| `ABOUT = "about"` | `AboutScreen`（含 `CheckUpdateSection`） | `ui/settings/AboutScreen.kt:40` |
| `ONBOARDING_MODELS = "onboarding_models"` | `OnboardingModelSelectionScreen`（空首页「选择模型」入口） | `ui/onboarding/OnboardingModelSelectionScreen.kt:68` |

### 1.12 引导页现状（重要）

- `ui/onboarding/OnboardingScreen.kt:59` 的 `OnboardingScreen`（Welcome → API Key → 模型选择三步，`WelcomeStep` 73 / `ApiKeyStep` 103 / `ModelSelectionStep` 214）**没有任何调用点**（`rg "OnboardingScreen("` 只命中定义本身）——这是 iOS 对齐遗留的完整引导流，重构时容易误删。
- 实际首启引导 = `SessionListScreen.kt:997` 的 `OnboardingLanding`（定义 3166）+ `SetupStepCard`（3284）：① 配置 Provider ② 选择模型 ③ 开始对话 / 恢复备份；`ONBOARDING_MODELS` 是其中「选择模型」的落地页。
- 另有 `ui/NewerDatabaseGuidanceScreen.kt`（`MainActivity.kt:255` 数据库版本过新时整屏拦截，带 `finishAndRemoveTask` 退出）。

### 1.13 不在 NavHost 内但属于界面的模块

- `ui/browser/BrowserSheet.kt`（ChatScreen 内 Sheet）、`ui/preview/WebPreviewBottomSheet.kt` + `WebPreviewFullscreenScreen.kt`（Sheet / 全屏）、`ui/media/InlineMediaPlayer.kt`（内联音视频 + 全屏视频）、`ui/components/FullscreenImageViewer.kt` / `ImageGalleryViewer.kt`。
- `webapp/WebAppActivity.kt`（`minis://` PWA 快捷方式宿主，manifest action `com.openminis.app.action.OPEN_WEBAPP`，`launchMode=singleInstance`）。
- `share/ShareReceiverActivity.kt`（系统分享接收器，`mimeType="*/*"`）。
- `ui/settings/ConfigAuditScreen.kt`（嵌在 LOGS 的 Tab）、`CheckUpdateSection.kt`（嵌在 About）、`ui/components/QuickTestSheet.kt`（模型连通性测试 Sheet）。

## 2. 对话页功能（`ui/chat/`）

骨架：`ChatScreen`（`ChatScreen.kt:512`）= `Scaffold`（2795，`containerColor = ChatColors.background`，`contentWindowInsets = WindowInsets(0)`）+ `TopAppBar`（2799，`expandedHeight = 68.dp`）+ 消息 `LazyColumn`（4177，`reverseLayout = true`，`AlwaysStretchOverscrollBox` 包裹）+ 底部 composer 列。

### 2.1 顶栏
- 居中三行标题：会话标题 → Soul 名 → `app_name`（2875-2896）；点击开 `SessionEditSheet`（5238，与会话列表同一 composable）。
- 第二行 provider · model 名 + `ThinkingLevelBadge`（3128 调用 / 7898 定义），窄屏时模型名 `weight(1f, fill=false)` 先截断、等级徽章保持完整。
- 右上角 `⋯` 菜单（`MinisMenu` @3223）：New Chat（3243）/ Clear Chat（3258，error 色）/ Open Terminal（3270）/ Open Browser（3281）/ Browse Chat Files（3292）/ Skills·MCPs·Memories 三个 Sheet 入口 / Token usage / Enhanced Cache / Fast Mode / Auto Compact / Debug: Trigger Crash（`debug_trigger_crash_menu`）。
- 侧栏折叠按钮：`chat_show_sidebar` / `chat_hide_sidebar`（3207-3209），双栏模式下控制 `sidebarCollapsed`。

### 2.2 Composer（输入区）
- `+` 附件菜单（`showAttachMenu`，`MinisMenu` @6795）：Take Photo / Choose Photos & Videos / Add File（iOS 对齐三项），照片/视频单次上限 50（常量注释 317）。
- 附件 chip：`AttachmentChip`（`ChatComposerWidgets.kt:314`）+ 作用域胶囊（workspace / skills / shared / memory / `<mountName>`，渲染于 `ChatScreen.kt` 附件行）。
- 长粘贴折叠：`PastedTextBuffer.kt` — `PastedTextChip`(380)、`isLongPastedText`(283)、`foldLongPasteIfNeeded`(318)、`PastedTextPreviewDialog`(462)。
- `/` 斜杠菜单：`showSlashMenuOverInput`（`ChatViewModelSlashExt.kt:219`）、`dismissSlashMenu`、`SLASH_PICKER_VISIBLE_ROWS = 4`；命令表 `ChatViewModel.availableSlashCommands`（`ChatViewModel.kt:3184` 起）：clear / compact / memory / …（skill 行只做输入补全，`executeSlashCommand` @3240）。
- `@` 提及文件：`ChatViewModelMentionExt.kt`，占位符提示 `chat_input_placeholder`（"Message %1$s (@ to mention files)"）与轮换提示 `ComposerPlaceholderRotation/ComposerPlaceholderUi`（含 `animationsDisabled(context)` 遵循系统「移除动画」）。
- 麦克风 `MicButton`（`ChatComposerWidgets.kt:434`）+ 语音面板 `InlineVoiceInputPanel`（`ui/chat/voice/InlineVoiceInputPanel.kt:180`，STT 状态机 RECORDING/STARTING/FINISHING、语言选择 `SpeechLanguagePickerSheet`、修正按钮、ASR 供应商切换 pill）；朗读回复 `SpeechPlayerCapsule`（`ui/chat/voice/SpeechPlayerCapsule.kt:116`）+ `LazyReadAloudPlayer`（流式朗读）。
- 输入圆钮 `InputCircleButton`（408）+ `ComposerSendButton`（发送/停止二态）。
- 回车行为菜单 `SendReturnModeMenu.kt`（`returnKeySendsMessage`、`tapShouldSend`、菜单定位 `placement`）。
- 模型选择：`ModelPickerSheet`（`ChatModelPickerSheet.kt:284`，搜索 `model_picker_search_placeholder`、分组区段、系统 TTS/ASR 条目），另一套 `UnifiedModelPickerSheet`（`ui/components/`）。
- 思考等级：`ThinkingLevelPicker`（`ChatComposerWidgets.kt:1266`）+ 全屏 `ThinkingLevelSheet`（`ChatScreen.kt:7943`，Off / 等级）。
- 发送手势：`SwipeToSendHint`（`ChatMiscViews.kt:1006`）——composer 外层 Box 支持上滑发送。
- 草稿/队列：流式中发送的排队提示与 `isQueued` 气泡样式（`ChatUserMessageUI.kt:375` 附近 queued-bubble）、`ChatViewModel` 的 `queued` 队列。
- 大内容导出保护：`LargeContentGuard.kt`（超限时走 `ACTION_SEND` chooser，`chat_message_large_export`）。

### 2.3 消息渲染
- 扁平化流水线：`ChatFlatItems.kt` — `buildFlatChatItems`(621)、`mergeStreamingOverlay`(542)、`streamingOverlaySubset`(581)、`ChatMessage.rendersAsUserBubble`(614)。
- 助手：`AssistantMessageView`（`ChatAssistantMessageUI.kt:432`）、`AssistantHeader`（281，Soul 图标 `SoulIconGlyph` + 名字）、`BoundsTrackedBlock`（选择工具栏定位）。
- 流式 Markdown：`StreamingMarkdownText`（`StreamingMarkdownText.kt:756`）+ `MarkdownDocument`(869)，按块增量解析、`coalesceMarkdownFragments`(1021)、宽公式/内联媒体分段（1538/1615）、`StreamingFade` 渐隐。静态渲染 `ui/markdown/MarkdownText.kt:87`（标题/粗斜/删除线/行内码/代码块+复制/引用/列表/表格 `TableView`/图片 `MinisImageBlock`/链接 `LinkAnnotation`）；代码高亮 `ui/markdown/SyntaxHighlighter.kt:44`。
- 表格：`BorderedMarkdownTable`（`ChatMiscViews.kt:292`），表格复制 / 表格转图片复制（`markdown_table_copied_toast`、`markdown_table_image_copied_toast`）。
- 思考块：`ThinkingBlock`（`ChatAssistantMessageUI.kt:1035`，可折叠 `AnimatedVisibility` @1186，`thinking_view_full` / `thinking_truncated_hint`）。
- 工具卡：`ToolCallPill`（694 起，`combinedClickable` 长按菜单 @598-600；运行态闪烁 `SHIMMER_PERIOD_MS`、`DecorativeSpinner` 共享 30fps 装饰时钟）；工具配色 `toolAccentColor`（`ChatToolFormatting.kt`：shell_execute/file_read/file_write/file_edit/browser_use/…）。
- 工具详情：`ToolDetailSheet`（`ChatScreen.kt:5064/5082` 两处调用）+ `ChatToolDetailUI.kt`（`ToolDetailContent`、shell 命令「在终端打开」按钮 → `onOpenTerminalWithCommand`）；记忆类详情 `MemoryDetailScreens.kt`（`MemoryFileViewerBody` 58 / `MemoryWriteDetailBody` 129 / `MemoryGetDetailBody` 195 / `RevokeConfirmDialog` 251 / `MutationResultDialog` 280）。
- 子代理卡：`HelperToolBlock`（`HelperUi.kt:511`）、`HelperControlRow`(346)、`helperStatusLabel`(428)、`AgentChip`(648)、`TierChip`(657)、`ThinkingLevelPill`(675)、`helperAccent()`(116)；只读全文 `AgentTranscriptScreen`（1269）。
- 用量胶囊：`UsageCapsule`（`ChatAssistantMessageUI.kt`），阈值与展开状态由 VM 控制；`TokenUsageSheet`（`TokenUsageSheet.kt:49`，Context Used / Context Window / Max Output / Thinking / Session Token 明细）。
- 压缩：`CompactProgressIndicator`（`ChatIndicators.kt:104`，可取消）→ `cancelCompact()`；`TypingIndicator`(155)、`BouncingDots`(47)、`StreamingDotsText`(70)。
- 上下文压力：`ContextUsageHintUi.kt`（`contextUsageAnnotatedText` 104、`contextUsageGlow` 138）+ 接近容量对话框（`context_near_capacity_*`，`ChatScreen.kt:7430`）。
- 降级/回退块：`FallbackInfoBlock`（`ChatMiscViews.kt:545`）、`ResumeBanner`(924，流中断续跑)。
- 用户消息：`UserMessageBubble`（`ChatUserMessageUI.kt:282`）、`UserAttachmentList`(589)、`FileAttachmentTile`(658)、`ImageGalleryDialog`(711)。
- 内联媒体/离线产物：`ui/media/InlineMediaPlayer.kt`（音视频 + 全屏视频）、`ui/components/FullscreenImageViewer.kt`(291-322 复制/分享/保存到相册)、`ImageGalleryViewer.kt`。

### 2.4 消息操作
- 用户气泡长按 `MinisMenu`（`ChatUserMessageUI.kt:493-560`）：Copy（`chat_longpress_copy`）/ Edit（`chat_longpress_edit`，进入编辑态，`chat_edit_exit_button`）/ Delete from here（`chat_longpress_delete_from_here`）/ Compact above（`chat_longpress_compact_above`）/ Retry；回调在 `ChatScreen.kt:4444-4482`（流式中禁用）。
- 工具卡长按：`tool_longpress_copy_details`（复制详情，toast `tool_longpress_copied_toast`）、`tool_longpress_rerun_from_here`、`stop_tool`（单卡停止 → `cancelStream()`）。
- 选中文本浮动工具栏：`MinisSelectionToolbarHost`（`MinisTextKitGesture.kt:...`，`ChatScreen.kt:4865`）+ `MinisMarkdownTextToolbar`（`ChatScreen.kt:4104`）：`selection_copy` / `selection_copy_markdown` / `selection_copy_rich_text` / `selection_add_to_chat_input` / `selection_read_aloud` / `selection_read_from_start` / `selection_copy_full_markdown` / `selection_copy_full_rich_text` / `selection_copy_full_plain_text`。
- 自定义文本选择引擎：`MinisTextKitGesture.kt`（`Modifier.minisTextKitSelectionGesture` 109，字符级拖选、双手柄 `SelectionHandleDot`、拖动自动滚动）、`MinisTextKitSelection.kt`（`SelectionController`）。
- 会话级动作（顶部标题 / ⋯ 菜单）：New Chat（有流时二次确认 `chat_new_chat_stop_dialog_*`）、Clear Chat（`chat_clear_dialog_body`）、Move to Session（`MoveToSessionSheet`，`ChatScreen.kt:7396`）。

### 2.5 Sheet / Dialog 清单（`ChatScreen.kt`）
`ToolDetailSheet`(5064/5082)、`SessionEditSheet`(5238)、`ThinkingLevelSheet`(7943)、`ModelPickerSheet`(7649)、`MoveToSessionSheet`(7396)、`TokenUsageSheet`(7570)、`SessionMemorySheet`(7578)、`SessionSkillsSheet`(7589)、`SessionMcpsSheet`、`SpeechLanguagePickerSheet`(6915)、`UrlPreviewSheet`(7747)、`WebPreviewBottomSheet`(7774)、`AddToHomeSheet`(7822)、`OffloadPermissionDialog`(7743)、`CompactSummarySheet`（`ChatMiscViews.kt:670`）、`ScheduledTaskDetailSheet`（`ui/chat/ScheduledTaskDetailSheet.kt:105`）；`MinisAlertDialog`：清空会话(7430)、New Chat 停止确认(7452)、上下文接近上限(7418)、Enhanced Cache 提示(7532)。

### 2.6 滚动与浮层
- `LazyColumn(reverseLayout = true)`；`isNearBottom`（1270）、`userScrolledAway`、`contentOverflows` 组合判定（496 注释块）；流式自动跟随 + T170 拖拽回到底吸附。
- 上/下浮动 FAB（`FilledIconButton`，`upFabVisible` / `downFabVisible`，5173 起；上一段 contentDescription "Scroll to previous message"），可见性参与 `FloatingToolStatusBar` 的避让计算（5020-5035）。
- `FloatingToolStatusBar`（`ChatComposerWidgets.kt:1018`）悬浮工具状态条 + `ToolPreviewThumbnail`（539，browser_use 实时快照）；`visualOverlayHeight = 65.dp` 为滚动留白。
- 粘性会话标题 pill（`T-chat-title-pill`，`ChatScreen.kt` 上方 overlay，滚动出现、点击可编辑）；`SwipeToSendHint`、`ResumeBanner`、`CompactProgressIndicator` 均为列表内条目/浮层。

### 2.7 双栏 / 平板
`ui/navigation/ChatSplitScaffold.kt`：`ChatSplitScaffold`(270)、`ChatSplitScaffoldRoute`(852)、`shouldUseTwoPane()`(177，`isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)` 且高度≥MEDIUM)、`chatPaneScaffoldDirective`(221)、可拖拽分隔条（`LIST_PANE_MIN_WIDTH = 340.dp`、`KEY_LIST_PANE_WIDTH`、`KEY_LIST_COLLAPSED`，`VerticalDivider`@561，宽度 `animateDpAsState`@375）、`Ctrl/⌘+F` 聚焦搜索（`SessionSearchRequest.focusSearch`，487）、空态 `NoConversationSelected`(706)、草稿占位行 `newDraftSessionId()`/`isDraftSessionId()`(834/841)。

## 3. 会话列表（`ui/sessions/`）

`SessionListScreen.kt:501`（3652 行）+ `SessionListViewModel.kt`。
- 搜索：`DualFabRow`（1602）内含搜索开关与输入框（`search_chats_placeholder`），`SessionSearchRequest` 计数驱动外部唤起（622）；退出搜索仅当 query 为空（`exitSearchIfQueryBlank` 607）。
- 顶栏（790-1000）：正常态 `Settings`(852) + `Language` 图标；多选态 `sessionlist_select_title` / `sessionlist_n_selected` / 全选-反选（`sessionlist_select_all` / `deselect_all`）；定时任务入口带 `Badge` 计数（877）。
- 多选：`enterSelection`(VM 389)、`toggleSelect`(375)、`selectAll`(394)、`clearSelection`(398)、`deleteSelected`(403)；`SelectionToolbar`（1844）= Export(JSON/PLAIN_TEXT) + Move to Group + Delete（`share/SessionExporter.kt`，`Format { JSON, PLAIN_TEXT }`）。
- 行操作：`combinedClickable` 长按/点击进多选；右键菜单含 Pin/Unpin（`sessionlist_pin`/`unpin`）、重命名/编辑（`SessionEditSheet`：标题 + 分类 + 「重新生成标题」按钮 `onRegenerate`）、Duplicate（`duplicateSession` VM 1057）、Delete。
- 分组（folder/group）：`partitionByFolder`(377)、`emptyGroupIsStillRelevant`(363)、`groupSessionsByDate`(453)、可展开分组头（`expandedFolderId`，`chevronRotation` 2439）、分组菜单（2648-2664：Pin/Unpin、`group_rename`、`group_new_chat_in`）、`GroupPickerSheet`（`ui/sessions/GroupPickerSheet.kt:92`，新建组名/描述、`group_picker_title_move/change(_n)`、`groupPickerOrder`）。
- 日期分组：`DatePeriod` + `relativeDate`(468)。
- 分类色标：`categoryStyle`(203)。
- 每个会话的徽章：`freshBadgedIds`（新内容）、`activeSessionIds`（运行中）、`scheduledTaskCount`、未读 sync 占位（源码注释「reserved」）。
- 导入/导出：`ChatImportSection`（`ui/settings/backup/ChatImportSection.kt`，`chat_import_done_title/body`、`chat_import_failed_title`）挂在备份页；`SessionExporter` 支持单条/多选导出与系统分享（`shareSessionExport`）。
- 空态/引导：`OnboardingLanding`(3166) + `SetupStepCard`(3284)。
- 浮动：`DualFabRow` 双 FAB（New Chat 变体 + 搜索），`PREF_FAB_SWAPPED` 可左右互换，`imePadding()` 避键盘。

## 4. 设置页（`ui/settings/SettingsScreen.kt:78`）

分区与行（按代码顺序）：
1. `settings_section_llm_providers`：Manage Providers（`Icons.Outlined.Lock`，`Color(0xFF9A7A48)`）→ `PROVIDER_LIST`；Model Groups（`settings_model_groups`）→ `MODEL_GROUPS`；Token Usage（`settings_token_usage`）→ `USAGE_STATS`。
2. `settings_section_appearance`：Appearance（`settings_appearance_subtitle`"Theme, font size, and display"）→ `APPEARANCE`。
3. `settings_section_agent_runtime`：Agent Tools（`settings_agent_tools`）→ `AGENT_TOOLS`；Agents（`settings_agents`，`HelperAccentStatic`）→ `AGENTS`；Skills（`settings_skills`）→ `SKILLS`；Memory（`settings_memory*`）→ `MEMORY`；MCP（`settings_mcp`，`Color(0xFF6F9A9E)`）→ `MCP`；Soul；Env Vars → `ENV_VARS`；Terminal（`Icons.Outlined.Terminal`）→ `TERMINAL`（注：`onTerminalClick` 形参存在，主页无对应 `SettingsItem` 调用）。
4. `settings_section_anthropic_cache`：`AnthropicCacheModeSection`(651) —— Standard / Extended(1h) 两档（`EnhancedCachePrefs`）。
5. Storage 区：Rootfs（`onRootfsClick`）、Shared Folders（`onSharedFoldersClick`）、Mounted Folders（`onMountedFoldersClick`）、Backup（`onBackupClick`）、Storage（`STORAGE`）。
6. `settings_section_permissions`：Permissions（`settings_permissions_subtitle`）→ `PERMISSIONS`；Background（`BACKGROUND`）。
7. Logs → `LOGS`；About（`settings_about_subtitle`）→ `ABOUT`；Privacy Policy（`settings_privacy_policy`，外链）。
8. Feedback 区（`showFeedbackSheet`）：Submit GitHub Issues（`buildBugReportUrl` 433）、Telegram（`FeedbackSheetItem`）、Email（`buildFeedbackMailto` 497）。
9. 图标色板：23 个 `iconColor`，其中 `Color(0xFF9A7A48)`×10、`Color(0xFFC98A3E)`×3、`Color(0xFF7C7FA6)`×3、`Color(0xFF6E9C7B)`×3、`Color(0xFF6F9A9E)`×1、`HelperAccentStatic`×1（**重构重点：硬编码**）。

### 4.1 外观（`ui/settings/AppearanceScreen.kt:255`，`PREF_APPEARANCE = "appearance_prefs"`）
- 主题：System / Light / Dark（`KEY_THEME_MODE`，0/1/2，`ThemeRow` 带 `BrightnessAuto`/`LightMode`/`DarkMode` 图标与彩色 tile）。
- 启动页：Auto / Last Session / New Chat / Home（`KEY_LAUNCH_SESSION` 0-3，`LaunchRow` + `appearance_launch_footer`）。
- 字体大小：三组独立滑杆 `KEY_FONT_CHAT_INPUT` / `KEY_FONT_MESSAGE` / `KEY_FONT_APP_BASE`，级别 `fontScaleValues = [-2,-1,0,1,2,3]` → 倍率 `[0.88,0.94,1.0,1.06,1.12,1.21]`（`fontScaleForLevel` 247），有「重置」按钮。
- 刷新率：`appearance_refresh_rate_*`（`MODE_HIGHEST` / `120` / `90` / `60` / System，footer 显示面板支持值 `N Hz`）→ `RefreshRateController.setMode` + `apply(activity)`。
- 语言：`KEY_LANGUAGE` + `LanguageOption(code, flag, label)` 列表（`appearance_section_language`，`"system"` 表示跟随系统；`locales_config.xml` + `values-*` 21 个语言目录）。
- 应用图标：`appearance_section_app_icon`，Auto / Classic Light / Classic Dark → `AppIconRepository` 切 `activity-alias`。
- 开关：`KEY_TOOL_PREVIEW`（工具预览）、`KEY_AUTO_EXPAND_THINKING`（深度思考自动展开）、`KEY_AUTO_FOCUS_AFTER_REPLY`（回复后自动聚焦）、`KEY_RETURN_KEY_BEHAVIOR`（回车=换行/发送）、`KEY_KEEP_SCREEN_AWAKE`、`KEY_SHOW_CHAT_TITLE`、`KEY_AUTO_GROUPING`（自动归档分组，默认开）。

## 5. 其他 UI 模块

**Terminal**（`ui/terminal/`）：`TerminalScreen.kt:81`，`TerminalTopBar`(322，关闭/清屏)、`KeyboardAccessoryBar`(385，可横向滚动：Ctrl 切换、显示/隐藏键盘、方向键、`C-c`/`C-d`/`C-z` 原始字节)、`TerminalCanvasView`/`TerminalNativeView`（原生 Canvas 渲染）、`TerminalBuffer`/`TerminalEmulator`/`AnsiParser`（`ui/terminal/emulator`）+ `AnsiParser.kt`（颜色）；`TerminalFonts.kt` 用 `jetbrains_mono_regular/bold`；`MinisOpenUrlBroker` 允许终端里弹链接；`initCommand` 预填、`sessionId` 绑定工作目录；`imePadding()` + `windowInsetsPadding(systemBars)`。

**Browser**（`ui/browser/`）：`BrowserSheet.kt:94`（`ModalBottomSheet` + 顶部标题/设置/新标签/历史按钮，NestedScroll 让页面滚动不拖动 sheet）、`BrowserWebView.kt`（WebView 复用、`onRenderProcessGone` 恢复）、`BrowserHistorySheet`、`BrowserDownloadsSheet`、`BrowserSettingsSheet`（自定义 UA、空闲超时等）、`BrowserExternalSchemeHandler`（intent://mailto: 等外跳）。

**File preview**（`ui/sandbox/FilePreviewScreen.kt:99`）：图片 `ImagePreview`(280)、文本 `TextPreview`(360)、Markdown `MarkdownPreview`(443)、HTML `HtmlPreview`(487，WebView + `onRenderProcessGone`)、音频 `AudioPreview`(525)、视频 `VideoPreview`(545)、PDF `PdfPreview`；`collectImageGallery`(1399) 图片左右滑、`saveImageToGallery`(1343)，`filePreviewDialogWindow`(1426) 为 dialog 窗口设置边到边；`isImageFile/isAudioFile/isVideoFile/isPdfFile` 扩展驱动分支。

**File browser**（`ui/sandbox/FileBrowserScreen.kt:76` + `FileBrowserViewModel.kt`）：路径面包屑（`updatePathComponents`）、排序（name/kind/modified/size + 升降序 + `folders_first`，`applySort` 401）、显示隐藏文件、复制绝对路径（`filebrowser_copy_abs_path`）、删除确认、进入预览、绑定 `/var/minis/{skills,memory,shared}` 与外部挂载点。

**Markdown renderer**：见 2.3；`ui/markdown/` 为静态版，`ui/chat/StreamingMarkdownText.kt` 为流式版（分块 + 分片哈希指纹 + 复用 RenderBlock），两套并存。

**Onboarding**：`OnboardingLanding`（列表首页，实际在用）+ 未被引用的 `ui/onboarding/OnboardingScreen.kt`（Welcome/API Key/模型选择三步）+ `OnboardingModelSelectionScreen`（路由 `ONBOARDING_MODELS`，模型搜索/批量勾选/`common_skip`）。

**Scheduled tasks**（`ui/scheduled/`）：`ScheduledTasksScreen`(73) 列表 + 新建 + 删除确认；`ScheduledTaskEditScreen`(91) 字段：label、target（`TargetKind.NEW/FOLLOW_UP/RERUN/AGENT`）、session、message、触发（`ScheduledTriggerKind.CALENDAR/AFTER/INTERVAL/ON_COMPLETION`）、重复（ONCE/DAILY/…）、起止日期（`scheduled_task_field_start_date/end_date`）；`ScheduledTaskRunsScreen`(56) 执行记录；`ScheduledTask.kt` 里 `isOneShot` 判定。

**Sandbox**（`ui/sandbox/`）：`RootfsManagementScreen`(61)（状态/大小/路径/浏览、安装与卸载、进度 Observable）、`MirrorSettingsScreen`（`applyMirror` 430 / `restoreOfficial` 470、`MirrorCategory`、`MirrorCategoryDetailScreen` 612）、`ContainerNetworkSection`、`RootfsManager`（安装进度供 Onboarding 使用）。

**WebView**：`ui/webview/WebViewRenderProcess.kt`（渲染进程崩溃统一处理）、`ui/preview/WebPreviewBottomSheet.kt:94` + `WebPreviewFullscreenScreen.kt:70`（`webpreview_more/stop/reload/open_external/close`、`FloatingMenuButton`）、`WebPreviewShortcut`（图钉到桌面，`generateMonogramBitmap` 生成字母图标）、`webapp/WebAppActivity`（PWA 式快捷方式、`WebAppPathResolver`）。

**Media**：`ui/media/InlineMediaPlayer.kt`（内联音视频、全屏视频、3 处 `AnimatedVisibility` 控件）、`ui/chat/AudioWaveformView.kt`（录音波形，`animateFloatAsState`）、`ui/components/ImageGalleryViewer` / `FullscreenImageViewer`（缩放、复制、分享、保存到相册）。

## 6. 全局 UI 行为

- **主题模式**：`MainActivity.kt` `setContent` 内 `themeMode = prefs.getInt(KEY_THEME_MODE, 0)` + `DisposableEffect(prefs)` 监听变更 → `darkTheme = when (themeMode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }`（544-547）→ `MinisTheme(darkTheme, fontScale)`（571）。`getThemeMode()` 在 `AppearanceScreen.kt:242`。系统栏：`SystemBarStyle.auto(TRANSPARENT, TRANSPARENT)` + `enableEdgeToEdge(statusBarStyle, navigationBarStyle)`（568）；夜间/浅色窗口底色见 `res/values/themes.xml` 与 `res/values-night/themes.xml`。
- **字体缩放**：`MinisTheme(fontScale)` 内 `Typography.scale(factor)`（`Theme.kt` 的 `scale()` 扩展，逐 text style 相乘）；三档独立偏好见 4.1；聊天输入/正文各自缩放。
- **120Hz**：`ui/RefreshRateController.kt:31` `object RefreshRateController`，`KEY_REFRESH_RATE_MODE`，`MODE_SYSTEM/HIGHEST/120/90/60`，首帧前 `RefreshRateController.apply(this)`（MainActivity 521），设置页 `setMode` + `apply(activity)`；有 ROM 掉模式的重试注释（159）。
- **Edge-to-edge / Insets**：`enableEdgeToEdge`；`ChatScreen` `contentWindowInsets = WindowInsets(0)`；`TerminalScreen` `windowInsetsPadding(WindowInsets.systemBars)` + `imePadding()`；`SessionListScreen` `imePadding()`(1758) / `navigationBarsPadding()`(3406)；`FilePreviewScreen` 用 `DialogProperties(decorFitsSystemWindows=false)` + `DisposableEffect` 设 `isAppearanceLightStatusBars`；`ui/util/BringIntoViewOnFocus.kt` 处理 IME 遮挡的滚动。
- **深链**：`deeplink/DeepLinkHandler.kt`（scheme 必须 `minis`）：`minis://share`、`minis://views/alarm`、`minis://open_terminal?init_command=`、`minis://session/<id>`、`minis://session/<id>/<resource-path>`（HTML 预览）、`minis://settings/<path>`（`resolveSettingsPath` 映射到路由，含 `environments?create_key=&create_value=`），未知路径回落 Settings 主页；`DeepLinkCoordinator` 分发，`MainActivity.onNewIntent`(727)/`handleDeepLink`(739)，冷启 `explicitDeepLink`(509)。聊天内链接：`ui/chat/ChatLinkResolver.kt`（`resolve` 33：minis 深链 → SandboxFile → 外链；`isSupportedScheme`）。
- **通知**：`service/AgentForegroundService.kt`（前台服务 + `NotificationChannel` + `NotificationCompat`，`service/AgentNotificationContent.kt`）、`notification/BackgroundTaskNotifier.kt`、`notification/ConfigConfirmNotifier.kt`、`res/drawable/ic_notification_completed.xml`；会话徽章 `service/SessionBadgeStore.kt`、`DynamicIslandSupport.kt`、`ToolOverlayController.kt`（悬浮窗进度）。
- **动画/转场**：NavHost 四条转场（`AppNavigation.kt` 537-560 区，原为 M3 共享轴 slide+fade 300/200ms，**现已被 Noir `NoirMotion.pushEnter/pushExit/popEnter/popExit` 替换**；`EmphasizedDecelerate/Accelerate` 曲线移入 `ui/theme/NoirMotion.kt`）。`AnimatedVisibility`：`SessionListScreen.kt:1716/1767`、`ChatAssistantMessageUI.kt:551/1186`、`ChatScreen.kt:4778`、`ui/components/MinisMenu.kt:159`、`FullscreenImageViewer`(3)、`ImageGalleryViewer`(3)、`InlineMediaPlayer`(3)、`UsageStatsScreen`(2)、`MinisSkillsBrowserScreen`(2)、`WebPreviewFullscreenScreen`(2)、`webapp/WebAppActivity`(2)、`InlineVoiceInputPanel`(1)、`ChatUserMessageUI`(1)、`ChatToolDetailUI`(1)、`ChatMiscViews`(1)、`ChatFlatItems`(1)、`ChatModelPickerSheet`(1)、`ChatScreenHelpers`(1)、`BrowserSheet`。`animate*AsState`：`ChatSplitScaffold.kt:375`（分隔条宽度）、`SessionListScreen.kt:2439`（chevron 旋转）、`ProviderListScreen.kt:225`（elevation）、`AudioWaveformView.kt:34`（波形）、`InlineVoiceInputPanel`（其中 `.animateContentSize(tween(280))` 603）。`rememberInfiniteTransition`：`ChatIndicators.kt:8`、`ChatMiscViews.kt:39`、`ui/components/DecorativeAnimation.kt:51`（共享装饰时钟 `rememberDecorativeTick`/`DecorativeSpinner`，避免 N 个无限动画各自失效）。`Crossfade`：无独立命中（占位符切换走 `ComposerPlaceholderUi` 自实现淡入淡出，并尊重 `animationsDisabled()`）。`Animatable`：`ui/theme/NoirLaunch.kt`（启动覆盖层进度）。**未见** `SharedTransitionLayout`/`AnimatedContent`（除 NavHost 内部）。

## 7. 视觉耦合点（对重构有用）

### 7.1 硬编码颜色（`rg -c 'Color\(0x'`，共 **411 处 / 62 个文件**）
| 文件 | 处数 | 说明 |
|---|---|---|
| `ui/theme/NoirTokens.kt` | 28 | **新 token 源**（`object Noir`：Onyx/Onyx1-3、OnyxHairline、Paper/PaperCard/PaperRaised/PaperSunk、Ink/InkSoft、Gold/GoldLight/GoldSheen/GoldDeep/GoldOnPaper/GoldOnOnyx、Sage、Ember；`goldBrush`/`goldHorizontal`） |
| `ui/settings/backup/BackupAndRestoreScreen.kt` | 24 | 备份成功/失败/警告色 |
| `ui/chat/ChatToolDetailUI.kt` | 22 | 工具详情配色 |
| `ui/chat/ChatModelPickerSheet.kt` | 22 | 供应商色标 |
| `ui/settings/SettingsScreen.kt` | 20 | 23 个 `iconColor` 图标底 |
| `ui/sessions/SessionListScreen.kt` | 19 | 分类色 / FAB / 未读点 |
| `ui/terminal/AnsiParser.kt` | 17 | ANSI 16 色（**应当保留**） |
| `ui/chat/MoveToSessionSheet.kt` | 17 | |
| `ui/theme/ChatColors.kt` | 13 | `ChatPalette` 的 Noir 值 + `userBubble = 0xFF2A2825` |
| `ui/chat/HelperUi.kt` | 13 | `HelperAccentLight/Dark`、状态色 |
| `ui/theme/Theme.kt` | 11 | `scrim`、`surface*` 显式覆盖 |
| `ui/components/ModelEntryPicker.kt` | 11 | |
| `ui/settings/AddProviderScreen.kt` | 10 | |
| `ui/chat/ChatScreen.kt` | 10 | |
| `ui/chat/ChatToolFormatting.kt` | 9 | `toolAccentColor()` 工具色（shell 绿/read 蓝/write 蓝/edit 橙…） |
| `ui/chat/ChatAssistantMessageUI.kt` | 9 | `chat_longpress_retry` 处 `Color(0xFFC8553D)`、sparkle 渐变两色 |
| `ui/sandbox/MirrorSettingsScreen.kt` | 8 | |
| `ui/terminal/TerminalScreen.kt` | 7 | `TerminalBg 0xFF000000` / `TerminalFg 0xFFD4D4D4` / `TerminalGreen 0xFF34C759` |
| `ui/components/UnifiedModelPickerSheet.kt` | 7 | |
| `ui/theme/NoirLaunch.kt` | 6 | 启动覆盖层 |
| 其余 42 个文件 | 1–5 | `ProviderDetailScreen`(2)、`OffloadPermissionScreen`(2)、`MountDetailScreen`(2)、`ModelGroupDetailScreen`(2)、`MarkdownText`(2)、`InlineVoiceInputPanel`(2)、`StreamingMarkdownText`(2)、`ScheduledTaskDetailSheet`(2)、`ChatMiscViews`(2)、`SharedFolderDetailScreen`(3)、`MinisSkillsBrowserScreen`(3)、`SkillRowItem`(4)、`ContextUsageHintUi`(4)、`SystemPermissionsScreen`(6) 等 |

另：`Color.White` / `Color.Black` 共 **120 处**（未计入上表）。`ChatColors.*` 引用 **286 处**（Top：`ChatScreen.kt` 52、`ChatToolDetailUI.kt` 48、`HelperUi.kt` 40、`ScheduledTaskDetailSheet.kt` 21、`InlineVoiceInputPanel.kt` 18、`ChatComposerWidgets.kt` 18）。`Noir.*` 引用 **119 处**。

绕过主题的典型点：
- `ui/theme/ChatColors.kt`：`ChatPalette`（35 个字段）由 `LocalChatPalette` 提供，`ChatColors` 组合取值（164/167）；浅色 `LightChatPalette`(64) 仍保留 iOS 灰阶（`#F2F2F7`/`#1C1C1E` 一类），与 `Theme.kt` 的 `Noir.Paper` 体系**不一致**——浅色聊天页与浅色设置页目前两套底色。
- `Theme.kt` 同时定义 `LightColorScheme`(=Noir Paper 系) 与 `DarkColorScheme`(=Noir Onyx 系)，但 `ChatColors.background` 在深色是 `Noir.Onyx`、浅色是 iOS `systemBackground`。
- 语义色直写：`MaterialTheme.colorScheme.error` 与硬编码 `Color(0xFFC8553D)` 混用（`ChatAssistantMessageUI.kt:640`）。
- 终端与 ANSI 调色板（`TerminalScreen.kt` / `AnsiParser.kt`）刻意独立于主题，改版时勿动。

### 7.2 字体（`res/font/`，8 个 ttf）
`jetbrains_mono_regular.ttf`、`jetbrains_mono_bold.ttf`（终端 + 代码，`ui/terminal/TerminalFonts.kt`、`ui/markdown` 代码块）；`noir_inter_regular/medium/semibold/bold.ttf`（Noir 无衬线正文）、`noir_serif_regular.ttf` / `noir_serif_italic.ttf`（Noir 衬线标题，`Noir.kt` 内 `NoirSerif`→`NoirDisplay` 34sp/38sp/-0.01em，`NoirTokens.kt:80+`）。**注意**：`jetbrains_mono` 是唯一在 `Theme.kt` 之前就存在的字体；Noir 字体仅被 `NoirTokens.kt` 引用。

### 7.3 启动图标 / 启动屏
- 自适应图标：`res/mipmap-anydpi-v26/ic_launcher.xml`（background `@color/ic_launcher_background`、foreground `@mipmap/ic_launcher_foreground` inset 15%、`<monochrome android:drawable="@drawable/ic_launcher_monochrome"/>`），夜间变体 `res/mipmap-night-anydpi-v26/`；另两套 `ic_launcher_classic_light.xml` / `ic_launcher_classic_dark.xml`（对应 `ic_launcher_foreground_light/dark`），由 `data/repository/AppIconRepository.kt` 通过 **3 个 `activity-alias`**（`AppNavigation`/manifest 220-290 区）切换，`Appearance` 里的应用图标选择器驱动。位图密度目录 `mipmap-{m,h,xh,xxh,xxxh}dpi` + `mipmap-night-*`。
- 快捷方式图标：`res/drawable/ic_shortcut_new_chat.xml` / `ic_shortcut_voice_chat.xml` / `ic_shortcut_camera_chat.xml`，配色 `color/shortcut_icon_fg = #1F2933`（固定色，launcher 进程无法解析 attr）；`res/xml/shortcuts.xml` 三条 `minis://action/{new_chat,voice_chat,camera_chat}`，`targetPackage=com.openminis.plus`。
- 工具图标：`res/drawable/ic_tool_{build,description,edit_note,globe,groups,image,note_add,psychology,search,terminal}.xml`；通知图标 `ic_notification_completed.xml`。
- **Splash**：`build.gradle.kts` **没有** `androidx.core:core-splashscreen` 依赖，`AndroidManifest.xml` 无 `windowSplashScreen*` 属性，`MainActivity` 无 `installSplashScreen()`。当前「启动屏」= ① 窗口主题底色（`values/themes.xml` 继承 `Material.Light.NoActionBar` + `values-night/themes.xml` 深色覆盖，T153 说明）② **Compose 层 `NoirLaunchOverlay`**（`ui/theme/NoirLaunch.kt:97`，`object NoirLaunch` 64 持时长/曲线，`drawNoirPearl` 217 画珍珠光晕；`MainActivity` 在 `setContent` 内 `if (launchOverlayVisible)` 渲染，**仅真·冷启动**，旋转/LMK 恢复/深链不显示；`isDark` 跟随主题；可点击跳过）。改版若换原生 splash，会与这层 overlay 叠加冲突。
- 其它主题相关资源：`res/values/colors.xml`（`ic_launcher_background`、`shortcut_icon_fg`）、`res/xml/locales_config.xml`、`res/raw/zh_background_wordfreq.txt`（中文分词）。

### 7.4 现有 Noir 改造痕迹（并发进行中，注意合并冲突）
- 新增：`ui/theme/NoirTokens.kt`（token + 字体 + `NoirDisplay`）、`ui/theme/NoirMotion.kt`（转场曲线与 push/pop 动画）。
- 改写：`ui/theme/Theme.kt`（`LightColorScheme`/`DarkColorScheme` 全面换 `Noir.Paper*`/`Noir.Onyx*`，`scrim`/`surfaceBright`/`surfaceDim`/`surfaceContainer*` 显式覆盖）、`ui/theme/ChatColors.kt`（`DarkChatPalette` 改 Onyx 系，深色 `userBubble = #2A2825`）、`AppNavigation.kt`（转场换成 `NoirMotion`）。
- 已改到的页面文件（`git diff --stat`，约 30 个）：`MainActivity.kt`（启动 overlay）、`ChatScreen`、`ChatAssistantMessageUI`、`ChatComposerWidgets`、`ChatMiscViews`、`ChatModelPickerSheet`、`ChatToolDetailUI`、`ChatToolFormatting`、`ChatUserMessageUI`、`HelperUi`、`MoveToSessionSheet`、`BrowserDownloadsSheet`、`CheckUpdateSection`、`ConfigAuditScreen`、`ConfigConfirmDialog`、`CopilotDisclaimerDialog` 等。
- `docs/noir-redesign/tools/patch_chatcolors.py` 为这次改造的批处理脚本（可参考其替换规则）。
