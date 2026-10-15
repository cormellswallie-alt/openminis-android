# OpenMinis Plus 1.14-plus.1

Based on OpenMinis v1.14 (`b4c0661`). Package: `com.openminis.plus` (installs alongside the official app).

## 1. Performance / chat jank
- ChatScreen no longer re-composes its whole (~8k-line) top-level scope on every streaming flush: the floating-tool-bar check reads the streaming side-channel inside a `derivedStateOf` and no longer copies the entire history per flush.
- Per-composition / per-parse debug logging in the markdown render & parse paths (`MdRender`, `MdParser`, `MdStream`, `FreezeEdge`, …) is compiled out of release builds.
- Baseline profile + `androidx.profileinstaller`: Compose and the chat/markdown code are AOT-compiled at install time instead of running interpreted/JIT after each update.

## 2. 120 Hz refresh rate
- Settings › Appearance › Refresh rate: Highest available (default) / 120 / 90 / 60 Hz / System. Applied via `preferredDisplayModeId` on start, resume and when changed.

## 3. Sub agents
- No more fixed 3-at-once limit: Settings › Sub Agents › Concurrency (3 / 5 / 8 / 12 / 20 / Unlimited, default Unlimited). Per-turn allowance follows the same setting; the queue backlog was raised 10 → 100.
- Each sub agent can be pinned to a **specific model** (not just a model group). Falls back to group / Auto if that model becomes unusable.

## 4. Token usage
- Large-context models (≥128k): old tool outputs are offloaded to files at ~160k tokens instead of only ~40k below the window (a 1M model used to carry up to ~960k tokens of stale tool output on every request).
- Outbound images are capped by count with step hysteresis (oldest elided 8 at a time, ≥4 newest kept) so long browser sessions stop re-sending dozens of old screenshots — without busting the prompt cache on every new screenshot.
- Official OpenAI Chat Completions requests carry a stable `prompt_cache_key` for better cache hits.

## 5. Chat history import (data migration)
- Settings › Backup & Restore › Restore › Import chat history.
- Formats: ChatGPT export (conversations.json / .zip), Claude.ai export (.json / .zip), OpenAI-style JSON / JSONL, Gemini `contents` JSON, Markdown / text transcripts (User/Assistant, 用户/助手 headers).
- Re-importing the same file skips existing chats.

## Other
- In-app updater checks this fork's releases (`UPDATE_REPO`).
