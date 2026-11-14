package com.openminis.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.openminis.app.R
import com.openminis.app.tools.AgentToolSwitch

/**
 * [T-tools-granular-switches] Settings › Agent Runtime › Tools — port of iOS
 * `ToolsSettingsView` plus Android's interactive Model Questionnaire (`ask_user`).
 *
 * One switch per optional capability. The switches gate the tool SCHEMA, so a
 * disabled tool is not merely refused at call time — the model never sees it
 * and cannot plan around it. The remaining tools (shell, files, memory) are
 * core to the agent and deliberately have no switch.
 */
@Composable
fun AgentToolsSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var browserEnabled by remember {
        mutableStateOf(AgentToolSwitch.BROWSER.isEnabled(context))
    }
    var askUserEnabled by remember {
        mutableStateOf(AgentToolSwitch.ASK_USER.isEnabled(context))
    }

    SettingsScaffold(title = stringResource(R.string.settings_agent_tools), onBack = onBack) {
        SettingsSection(footer = stringResource(R.string.agent_tools_browser_footer)) {
            SettingsSwitchRow(
                title = stringResource(R.string.agent_tools_browser),
                checked = browserEnabled,
                onCheckedChange = {
                    browserEnabled = it
                    AgentToolSwitch.BROWSER.setEnabled(context, it)
                },
                showDivider = false,
            )
        }
        SettingsSection(footer = stringResource(R.string.agent_tools_ask_user_footer)) {
            SettingsSwitchRow(
                title = stringResource(R.string.agent_tools_ask_user),
                checked = askUserEnabled,
                onCheckedChange = {
                    askUserEnabled = it
                    AgentToolSwitch.ASK_USER.setEnabled(context, it)
                },
                showDivider = false,
            )
        }
    }
}
