#!/usr/bin/env python3
"""Wires the step timeline into ChatScreen's message LazyColumn:
run computation, per-row gap (so folded rows take zero space), and the
StepRunSlot wrapper around thinking / tool-pill rows. Idempotent."""
import sys

p = sys.argv[1]
s = open(p, encoding="utf-8").read()
if "computeStepRuns(" in s:
    print("already patched"); sys.exit(0)


def rep(old, new, count=1):
    global s
    n = s.count(old)
    assert n == count, (n, old[:120])
    s = s.replace(old, new)


# 1) run computation, in composable scope right before the LazyColumn.
rep("""                val perfFirstLayoutFired = remember(sessionId) { java.util.concurrent.atomic.AtomicBoolean(false) }
""", """                val perfFirstLayoutFired = remember(sessionId) { java.util.concurrent.atomic.AtomicBoolean(false) }
                // [T-noir-step-timeline] Group consecutive tool / thinking rows
                // into runs: rail flags per row, and finished runs of 3+ fold
                // into one summary card. Expansion is per run, per session.
                val stepExpandedRuns = remember(sessionId) { androidx.compose.runtime.mutableStateMapOf<String, Boolean>() }
                val stepThinkingLevel = viewModel.thinkingLevel.value
                val stepRuns = remember(flatItems, isStreaming, stepThinkingLevel) {
                    computeStepRuns(flatItems, isStreaming) { row ->
                        when (row) {
                            is FlatChatItem.AssistantThinking ->
                                (row.messageThinkingLevel ?: stepThinkingLevel).isEnabled
                            is FlatChatItem.AssistantToolUse ->
                                !com.openminis.app.agent.jobs.HelperRunner.isSubAgentToolName(row.block.toolName) ||
                                    helperControlSummary(row.block) != null
                            else -> false
                        }
                    }
                }
""")

# 2) list spacing moves onto each row so a folded row can take none.
rep("""                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom),
                    overscrollEffect = sharedEffect,""",
    """                    // [T-noir-step-timeline] The 2dp gap is applied per row (see
                    // the row Box below) so folded step rows collapse to 0dp.
                    verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Bottom),
                    overscrollEffect = sharedEffect,""")

# 3) row wrapper gets the gap unless folded away.
rep("""                        val isNewestItem = item == flatItems.lastOrNull()
                        Box(
                            modifier = Modifier
                                .alpha(rowAlpha)
""", """                        val isNewestItem = item == flatItems.lastOrNull()
                        val stepInfo = stepRuns[item.key]
                        val stepFolded = stepInfo.isFoldedAway(stepExpandedRuns)
                        Box(
                            modifier = Modifier
                                .padding(top = if (stepFolded) 0.dp else StepItemGap)
                                .alpha(rowAlpha)
""")

# 4) thinking row
rep("""                                    ThinkingBlock(
                                        block = item.block,
                                        isStreaming = item.isLastBlockOverall && item.messageIsStreaming,
                                        isLast = item.isLast,
                                    )
""", """                                    StepRunSlot(stepInfo, stepExpandedRuns) { railUp, railDown ->
                                        ThinkingBlock(
                                            block = item.block,
                                            isStreaming = item.isLastBlockOverall && item.messageIsStreaming,
                                            isLast = item.isLast,
                                            railUp = railUp,
                                            railDown = railDown,
                                        )
                                    }
""")

# 5) control-call pill
rep("""                                ToolCallPill(
                                    item.block,
                                    allToolBlocks = item.allToolBlocks,
                                    onOpenDetail = { viewModel.openToolDetail(it) },
                                )
                            } else if (com.openminis.app.agent.jobs.HelperRunner.isSubAgentToolName(item.block.toolName)) HelperToolBlock(""",
    """                                StepRunSlot(stepInfo, stepExpandedRuns) { railUp, railDown ->
                                    ToolCallPill(
                                        item.block,
                                        allToolBlocks = item.allToolBlocks,
                                        onOpenDetail = { viewModel.openToolDetail(it) },
                                        railUp = railUp,
                                        railDown = railDown,
                                    )
                                }
                            } else if (com.openminis.app.agent.jobs.HelperRunner.isSubAgentToolName(item.block.toolName)) HelperToolBlock(""")

# 6) ordinary pill: `) else ToolCallPill(` ... closing `)` of that call.
start_tok = """                            ) else ToolCallPill(
                                block = item.block,
                                allToolBlocks = item.allToolBlocks,"""
a = s.index(start_tok)
# find matching paren for the ToolCallPill( call
i = s.index("ToolCallPill(", a) + len("ToolCallPill")
depth = 0
j = i
in_str = False
while True:
    c = s[j]
    if c == '"' and s[j - 1] != '\\':
        in_str = not in_str
    elif not in_str:
        if c == '(':
            depth += 1
        elif c == ')':
            depth -= 1
            if depth == 0:
                break
    j += 1
call_args = s[i + 1:j]  # between parens
assert "onCopyDetails" in call_args and "railUp" not in call_args
new_call = ("StepRunSlot(stepInfo, stepExpandedRuns) { railUp, railDown ->\n"
            "                                ToolCallPill(" + call_args.rstrip() +
            "\n                                railUp = railUp,\n"
            "                                railDown = railDown,\n"
            "                                )\n"
            "                            }")
# ensure the last real arg ends with a comma before we append
body = call_args.rstrip()
if not body.endswith(","):
    new_call = new_call.replace(call_args.rstrip() + "\n                                railUp",
                                call_args.rstrip() + ",\n                                railUp")
prefix = "                            ) else "
s = s[:a] + prefix + new_call + s[j + 1:]

open(p, "w", encoding="utf-8").write(s)
print("patched ChatScreen")
