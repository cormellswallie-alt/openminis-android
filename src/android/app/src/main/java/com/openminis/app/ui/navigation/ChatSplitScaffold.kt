package com.openminis.app.ui.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowSize
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import com.openminis.app.ui.theme.MaterialShapes
import com.openminis.app.ui.theme.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.openminis.app.R
import com.openminis.app.ui.components.MinisButton
import com.openminis.app.ui.components.MinisTonalButton
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private const val LIST_PANE_DEFAULT_FRACTION = 0.28f
private val EMPTY_STATE_ICON_SIZE = 64.dp
private val LIST_PANE_MIN_WIDTH = 340.dp
private val LIST_PANE_MAX_WIDTH = 500.dp
private val DIVIDER_TOUCH_WIDTH = 24.dp

object SessionSearchRequest {
    val requests = MutableStateFlow(0)
    fun focusSearch() { requests.value += 1 }
}

private const val UI_PREFS = "ui_prefs"
private const val KEY_LIST_PANE_WIDTH = "tablet_list_pane_width_dp"
private const val KEY_LIST_COLLAPSED = "tablet_list_pane_collapsed"

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun shouldUseTwoPane(): Boolean {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val expandedWidth = windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND,
    )
    val nonCompactHeight = windowSizeClass.isHeightAtLeastBreakpoint(
        WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND,
    )
    return expandedWidth && nonCompactHeight
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun chatPaneScaffoldDirective(twoPane: Boolean, listPaneWidth: Dp): PaneScaffoldDirective {
    val base = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo())
        .copy(
            horizontalPartitionSpacerSize = 0.dp,
            defaultPanePreferredWidth = listPaneWidth,
        )
    return if (twoPane) base else base.copy(maxHorizontalPartitions = 1)
}

/**
 * Material 3 Expressive Spatial Workspace Shell.
 *
 * Hosts:
 * - On expanded/tablet windows: a leading [ExpressiveWorkspaceRail] for instant
 *   spatial switching (New Chat, Chats, Automations, Terminal, Sandbox, Settings),
 *   plus a resizable two-pane [NavigableListDetailPaneScaffold] with a tactile pill
 *   resize seam and an expressive [NoConversationSelected] hero canvas.
 * - On compact/phone windows: seamless single-pane push/pop navigation.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ChatSplitScaffold(
    initialSessionId: String?,
    onNavigateScheduled: (() -> Unit)? = null,
    onNavigateTerminal: (() -> Unit)? = null,
    onNavigateRootfs: (() -> Unit)? = null,
    onNavigateSettings: (() -> Unit)? = null,
    listPane: @Composable (
        selectedSessionId: String?,
        draftPlaceholderId: String?,
        onSessionSelected: (String) -> Unit,
    ) -> Unit,
    detailPane: @Composable (
        sessionId: String,
        onBack: () -> Unit,
        onNewChatInPane: () -> Unit,
        onMoveToInPane: (String) -> Unit,
        onToggleSidebar: (() -> Unit)?,
        sidebarCollapsed: Boolean,
    ) -> Unit,
) {
    val twoPane = shouldUseTwoPane()
    val context = LocalContext.current
    val uiPrefs = remember(context) {
        context.getSharedPreferences(UI_PREFS, android.content.Context.MODE_PRIVATE)
    }
    val windowWidth = with(LocalDensity.current) { currentWindowSize().width.toDp() }
    var listPaneWidth by remember(windowWidth) {
        val default = (windowWidth * LIST_PANE_DEFAULT_FRACTION)
        mutableStateOf(
            uiPrefs.getFloat(KEY_LIST_PANE_WIDTH, default.value)
                .coerceIn(LIST_PANE_MIN_WIDTH.value, LIST_PANE_MAX_WIDTH.value)
                .dp,
        )
    }

    var selectedSessionId by rememberSaveable { mutableStateOf(initialSessionId) }

    var listCollapsed by remember {
        val stored = uiPrefs.getBoolean(KEY_LIST_COLLAPSED, false)
        val corrected = stored && initialSessionId != null
        if (stored != corrected) {
            uiPrefs.edit().putBoolean(KEY_LIST_COLLAPSED, corrected).apply()
        }
        mutableStateOf(corrected)
    }

    val effectiveListWidth by animateDpAsState(
        targetValue = if (listCollapsed) 0.dp else listPaneWidth,
        label = "listPaneWidth",
    )

    val directive = chatPaneScaffoldDirective(twoPane, effectiveListWidth)

    key(directive.maxHorizontalPartitions) {
        val navigator = rememberListDetailPaneScaffoldNavigator<String>(
            scaffoldDirective = directive,
            initialDestinationHistory = buildList {
                add(
                    ThreePaneScaffoldDestinationItem(
                        ListDetailPaneScaffoldRole.List,
                        null,
                    ),
                )
                selectedSessionId?.let {
                    add(
                        ThreePaneScaffoldDestinationItem(
                            ListDetailPaneScaffoldRole.Detail,
                            it,
                        ),
                    )
                }
            },
        )
        val scope = rememberCoroutineScope()

        val currentSessionId = navigator.currentDestination
            ?.takeIf { it.pane == ListDetailPaneScaffoldRole.Detail }
            ?.contentKey

        val openNewDraft: () -> Unit = {
            val draft = newDraftSessionId()
            selectedSessionId = draft
            scope.launch {
                navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, draft)
            }
        }

        val toggleSidebar: () -> Unit = {
            listCollapsed = !listCollapsed
            uiPrefs.edit().putBoolean(KEY_LIST_COLLAPSED, listCollapsed).apply()
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    if (!event.isCtrlPressed && !event.isMetaPressed) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.N -> { openNewDraft(); true }
                        Key.F -> { SessionSearchRequest.focusSearch(); true }
                        else -> false
                    }
                },
        ) {
            if (twoPane) {
                ExpressiveWorkspaceRail(
                    sidebarCollapsed = listCollapsed,
                    onToggleChats = {
                        if (listCollapsed) toggleSidebar()
                    },
                    onNewChat = openNewDraft,
                    onScheduledClick = onNavigateScheduled,
                    onTerminalClick = onNavigateTerminal,
                    onRootfsClick = onNavigateRootfs,
                    onSettingsClick = onNavigateSettings,
                )
            }

            Box(Modifier.weight(1f).fillMaxHeight()) {
                NavigableListDetailPaneScaffold(
                    navigator = navigator,
                    listPane = {
                        AnimatedPane(
                            modifier = if (twoPane) Modifier.width(effectiveListWidth) else Modifier,
                        ) {
                            listPane(
                                if (twoPane && currentSessionId != null) {
                                    com.openminis.app.ui.chat.ChatViewModelStore
                                        .rememberPersistedId(currentSessionId)
                                } else null,
                                if (twoPane && currentSessionId != null &&
                                    isDraftSessionId(
                                        com.openminis.app.ui.chat.ChatViewModelStore
                                            .rememberPersistedId(currentSessionId),
                                    )
                                ) currentSessionId else null,
                            ) { sessionId ->
                                selectedSessionId = sessionId
                                scope.launch {
                                    navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, sessionId)
                                }
                            }
                        }
                    },
                    detailPane = {
                        AnimatedPane {
                            val sessionId = navigator.currentDestination
                                ?.takeIf { it.pane == ListDetailPaneScaffoldRole.Detail }
                                ?.contentKey
                            Row(Modifier.fillMaxSize()) {
                                if (twoPane && !listCollapsed) {
                                    ExpressiveSplitSeam()
                                }
                                Box(Modifier.weight(1f).focusable()) {
                                    if (sessionId == null) {
                                        NoConversationSelected(
                                            onToggleSidebar = if (twoPane) toggleSidebar else null,
                                            sidebarCollapsed = listCollapsed,
                                            onNewChat = openNewDraft,
                                            onTerminalClick = onNavigateTerminal,
                                            onScheduledClick = onNavigateScheduled,
                                        )
                                    } else {
                                        detailPane(
                                            sessionId,
                                            {
                                                selectedSessionId = null
                                                scope.launch { navigator.navigateBack() }
                                            },
                                            openNewDraft,
                                            { targetId ->
                                                selectedSessionId = targetId
                                                scope.launch {
                                                    navigator.navigateTo(
                                                        ListDetailPaneScaffoldRole.Detail,
                                                        targetId,
                                                    )
                                                }
                                            },
                                            if (twoPane) toggleSidebar else null,
                                            listCollapsed,
                                        )
                                    }
                                }
                            }
                        }
                    },
                )

                if (twoPane && !listCollapsed) {
                    val density = LocalDensity.current
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .offset(x = effectiveListWidth - DIVIDER_TOUCH_WIDTH / 2)
                            .width(DIVIDER_TOUCH_WIDTH)
                            .pointerHoverIcon(
                                PointerIcon(
                                    android.view.PointerIcon.getSystemIcon(
                                        context,
                                        android.view.PointerIcon.TYPE_HORIZONTAL_DOUBLE_ARROW,
                                    ),
                                ),
                            )
                            .draggable(
                                orientation = Orientation.Horizontal,
                                state = rememberDraggableState { deltaPx ->
                                    listPaneWidth = (
                                        listPaneWidth + with(density) { deltaPx.toDp() }
                                        ).coerceIn(LIST_PANE_MIN_WIDTH, LIST_PANE_MAX_WIDTH)
                                },
                                onDragStopped = {
                                    uiPrefs.edit()
                                        .putFloat(KEY_LIST_PANE_WIDTH, listPaneWidth.value)
                                        .apply()
                                },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpressiveWorkspaceRail(
    sidebarCollapsed: Boolean,
    onToggleChats: () -> Unit,
    onNewChat: () -> Unit,
    onScheduledClick: (() -> Unit)?,
    onTerminalClick: (() -> Unit)?,
    onRootfsClick: (() -> Unit)?,
    onSettingsClick: (() -> Unit)?,
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxHeight()
            .statusBarsPadding()
            .navigationBarsPadding(),
        header = {
            Spacer(Modifier.height(8.dp))
            FloatingActionButton(
                onClick = onNewChat,
                shape = RoundedCornerShape(18.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp),
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.new_chat),
                )
            }
            Spacer(Modifier.height(12.dp))
        },
    ) {
        val railColors = NavigationRailItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onSurface,
            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NavigationRailItem(
            selected = !sidebarCollapsed,
            onClick = onToggleChats,
            icon = { Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null) },
            colors = railColors,
        )
        if (onScheduledClick != null) {
            NavigationRailItem(
                selected = false,
                onClick = onScheduledClick,
                icon = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                colors = railColors,
            )
        }
        if (onTerminalClick != null) {
            NavigationRailItem(
                selected = false,
                onClick = onTerminalClick,
                icon = { Icon(Icons.Outlined.Terminal, contentDescription = null) },
                colors = railColors,
            )
        }
        if (onRootfsClick != null) {
            NavigationRailItem(
                selected = false,
                onClick = onRootfsClick,
                icon = { Icon(Icons.Outlined.Dns, contentDescription = null) },
                colors = railColors,
            )
        }
        Spacer(Modifier.weight(1f))
        if (onSettingsClick != null) {
            NavigationRailItem(
                selected = false,
                onClick = onSettingsClick,
                icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                colors = railColors,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ExpressiveSplitSeam() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(8.dp)
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(36.dp)
                .background(
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
                    CircleShape,
                ),
        )
    }
}

@Composable
private fun NoConversationSelected(
    onToggleSidebar: (() -> Unit)? = null,
    sidebarCollapsed: Boolean = false,
    onNewChat: (() -> Unit)? = null,
    onTerminalClick: (() -> Unit)? = null,
    onScheduledClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(Modifier.fillMaxSize()) {
            if (onToggleSidebar != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .height(64.dp)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(
                        onClick = onToggleSidebar,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        Icon(
                            Icons.Filled.Menu,
                            contentDescription = stringResource(
                                if (sidebarCollapsed) {
                                    R.string.chat_show_sidebar
                                } else {
                                    R.string.chat_hide_sidebar
                                },
                            ),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val soulMeta by com.openminis.app.agent.SoulStore.cachedMetadata.collectAsState()
                val density = LocalDensity.current
                val iconBitmap = remember(soulMeta.icon) {
                    com.openminis.app.agent.SoulIcon.decode(soulMeta.icon)
                }
                val iconSize = remember(iconBitmap, density) {
                    if (iconBitmap == null) EMPTY_STATE_ICON_SIZE
                    else with(density) {
                        minOf(EMPTY_STATE_ICON_SIZE, iconBitmap.width.toDp())
                    }
                }

                // Layered Material 3 Expressive polygon emblem
                Box(
                    modifier = Modifier
                        .size(132.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            shape = MaterialShapes.Cookie12Sided.toShape(),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                shape = MaterialShapes.Sunny.toShape(),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        com.openminis.app.ui.settings.SoulIconGlyph(
                            icon = soulMeta.icon,
                            sizeDp = iconSize,
                            emojiSp = with(density) { EMPTY_STATE_ICON_SIZE.toSp() * 0.76f },
                            sparkleTint = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary,
                                ),
                            ),
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))
                Text(
                    text = soulMeta.name.ifBlank {
                        com.openminis.app.agent.SoulMetadata.DEFAULT.name
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.split_no_conversation_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )

                if (onNewChat != null) {
                    Spacer(Modifier.height(24.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MinisButton(onClick = onNewChat) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.new_chat),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        if (onTerminalClick != null) {
                            MinisTonalButton(onClick = onTerminalClick) {
                                Icon(
                                    Icons.Outlined.Terminal,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.sessionlist_shell_terminal))
                            }
                        }
                        if (onScheduledClick != null) {
                            MinisTonalButton(onClick = onScheduledClick) {
                                Icon(
                                    Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.scheduled_tasks_title))
                            }
                        }
                    }
                }
            }
        }
    }
}

fun newDraftSessionId(): String = "__new__${UUID.randomUUID()}"

fun isDraftSessionId(id: String?): Boolean = id?.startsWith("__new__") == true

@Composable
fun ChatSplitScaffoldRoute(
    initialSessionId: String?,
    navController: androidx.navigation.NavHostController,
    chatRepository: com.openminis.app.data.repository.ChatRepository,
    providerRepository: com.openminis.app.data.repository.ProviderRepository,
    memoryRepository: com.openminis.app.data.repository.MemoryRepository?,
    skillRepository: com.openminis.app.data.repository.SkillRepository?,
    mcpRepository: com.openminis.app.data.repository.MCPRepository?,
) {
    ChatSplitScaffold(
        initialSessionId = initialSessionId,
        onNavigateScheduled = { navController.safeNavigate(Routes.SCHEDULED_TASKS) },
        onNavigateTerminal = { navController.safeNavigate(Routes.terminal()) },
        onNavigateRootfs = { navController.safeNavigate(Routes.ROOTFS_MANAGEMENT) },
        onNavigateSettings = { navController.safeNavigate(Routes.SETTINGS) },
        listPane = { selectedSessionId, draftPlaceholderId, onSessionSelected ->
            com.openminis.app.ui.sessions.SessionListScreen(
                chatRepository = chatRepository,
                providerRepository = providerRepository,
                onSessionClick = onSessionSelected,
                onNewChat = onSessionSelected,
                onSettingsClick = { navController.safeNavigate(Routes.SETTINGS) },
                onAddProviderClick = { navController.safeNavigate(Routes.ADD_PROVIDER) },
                onSelectModelsClick = { navController.safeNavigate(Routes.ONBOARDING_MODELS) },
                onRestoreBackupClick = {
                    navController.currentBackStackEntry?.savedStateHandle?.set(Routes.BACKUP_INITIAL_TAB_KEY, 1)
                    navController.safeNavigate(Routes.BACKUP)
                },
                onTerminalClick = { navController.safeNavigate(Routes.terminal()) },
                onRootfsClick = { navController.safeNavigate(Routes.ROOTFS_MANAGEMENT) },
                onScheduledTasksClick = { navController.safeNavigate(Routes.SCHEDULED_TASKS) },
                selectedSessionId = selectedSessionId,
                draftPlaceholderId = draftPlaceholderId,
            )
        },
        detailPane = { sessionId, onBackInPane, onNewChatInPane, onMoveToInPane,
            onToggleSidebar, sidebarCollapsed ->
            com.openminis.app.ui.chat.ChatScreen(
                sessionId = sessionId,
                chatRepository = chatRepository,
                providerRepository = providerRepository,
                memoryRepository = memoryRepository,
                skillRepository = skillRepository,
                mcpRepository = mcpRepository,
                onBack = onBackInPane,
                isTwoPane = shouldUseTwoPane(),
                onToggleSidebar = onToggleSidebar,
                sidebarCollapsed = sidebarCollapsed,
                onNewChat = onNewChatInPane,
                onOpenTerminal = {
                    navController.safeNavigate(Routes.terminal(sessionId = sessionId))
                },
                onOpenTerminalWithCommand = { command ->
                    navController.safeNavigate(
                        Routes.terminal(initCommand = command, sessionId = sessionId),
                    )
                },
                onOpenAgentTranscript = { childId ->
                    navController.safeNavigate(Routes.agentTranscript(childId))
                },
                onMoveToSession = { targetId -> onMoveToInPane(targetId) },
                onBrowseChatFiles = { navController.safeNavigate(Routes.chatFiles(sessionId)) },
                onBrowseChatFolder = { path -> navController.safeNavigate(Routes.chatFiles(sessionId, path)) },
                onPreviewAttachment = { item ->
                    FilePreviewHolder.currentItem = item
                    navController.safeNavigate(Routes.FILE_PREVIEW)
                },
                onModelGroupsClick = { navController.safeNavigate(Routes.MODEL_GROUPS) },
                onEditProviderClick = { id -> navController.safeNavigate(Routes.providerDetail(id)) },
            )
        },
    )
}
