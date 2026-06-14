# Agent Simulator

> A native Android **serious game** for customer-support training. You run a bank's live-chat
> support desk: read each customer's message, look up the answer across banking tabs, verify
> identity where needed, and reply before their patience runs out. Every customer is **voiced by an
> on-device language model** that runs **100% offline** — no server, no API key, no account.

<p>
  <img alt="Platform" src="https://img.shields.io/badge/Platform-Android%207.0%2B-3DDC84?logo=android&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.2.20-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white">
  <img alt="On-device LLM" src="https://img.shields.io/badge/On--device%20LLM-Qwen%202.5--1.5B-FF6F00">
</p>

---

## Overview

Agent Simulator is a single-player, portrait, tap-only Android game built as a **serious game /
advergame**: a real, enjoyable game whose primary purpose is *training* rather than entertainment.
The player learns the practical and interpersonal skills of a front-line support agent — speed,
accuracy, empathy, de-escalation, identity verification and fraud awareness — by actually doing the
job in a faithful, fast-paced simulation.

It is written as a **pure-native** app in **Kotlin + Jetpack Compose (Material 3)** on an **MVVM**
architecture, and persists everything locally with **Jetpack DataStore** (no backend whatsoever).

## How it plays

The game is built on three nested loops:

- **Minute loop** — a chat arrives in the inbox with a draining patience bar. Open it, read the
  request, look up the answer (copy the account number, paste it into the **Accounts / Cards /
  Bookings / Payments** tab), verify identity if the action is sensitive, then reply. The customer
  rates you **1–5 stars** based on speed, accuracy and empathy, paying tokens and building a combo.
- **Session loop** — a **shift** of twelve served customers, tracked by a live progress bar and
  timer, ends in a **Shift Report**: tokens and XP are paid out, the rank bar fills, and the game's
  mascot delivers one **mentor lesson** from a bank of 36 hand-written customer-service tips.
- **Day loop** — login streaks, a "first win of the day" bonus, three rotating daily objectives, and
  a **career ladder** (Trainee → Junior → Agent → Senior → Specialist → Team Lead → Lead ★) that
  unlocks progressively harder customer types as you improve.

Tokens are a single persistent currency spent in a **levelled upgrade shop** (Pay Raise, Patience
Coach, Big Tipper, Thicker Skin, and more). When the queue overwhelms you, a summonable **AI
Assistant** power-up clears the current backlog for a coin cost.

## Features

- 🤖 **On-device LLM customers** — every customer's opening line, replies and end-of-chat review are
  written live by **Qwen 2.5-1.5B-Instruct** running locally via **Google MediaPipe LLM Inference**.
  Fully offline after a one-time model download.
- 🧩 **Procedural content** — customers, names, account numbers, balances, bookings, payments and a
  planted fraudulent transaction are generated in code, so no two shifts are the same.
- 🛡️ **Robust by construction** — all game-critical data is code-generated and resolution is checked
  against that state, never against the model's text, so a stray model reply can never break the
  game. A scripted brain transparently substitutes on devices that can't load the model.
- 🎓 **Teaches as you play** — star ratings reward speed, accuracy and empathy; complaints require
  *acknowledge-then-act* de-escalation; sensitive actions are gated behind identity verification; and
  fraud reports train you to scan a transaction list.
- 📈 **Meta-progression** — XP, ranks, a levelled upgrade shop, daily objectives and login streaks.
- 🔊 **Procedural audio** — all sound effects are synthesised in code (`SoundManager`), so the build
  ships with zero audio asset files.
- 🎨 **Custom art + juice** — hand-made rank/medal/mascot art, confetti, combo flames, token fly-ups
  and tactile press feedback.

## On-device AI

The standout technical feature: the game voices customers with a **1.5-billion-parameter language
model running on the phone**. The model (`qwen2.5-1.5b-it-q8.task`) is downloaded once (~1.6 GB) and
then runs entirely offline through MediaPipe. To stay reliable on a small model:

- a heavily **role-locked prompt** keeps the model speaking as the customer, never the agent;
- generation is **serialised behind a mutex and bounded by timeouts** with template fallbacks, so the
  LLM can never stall the UI;
- a **device gate** (`util/DeviceGate.kt`) checks available memory and falls back to a deterministic
  scripted brain when the model can't load.

## Tech stack

| Component | Version / choice |
|---|---|
| Language / UI | Kotlin 2.2.20 · Jetpack Compose (BOM 2025.11.01, Material 3) |
| Architecture | MVVM — single `AppViewModel` exposing `StateFlow<GameState>`, coroutine game loop |
| Persistence | Jetpack DataStore (Preferences) — fully offline, no backend |
| On-device LLM | Qwen 2.5-1.5B-Instruct via `com.google.mediapipe:tasks-genai:0.10.27` |
| Build | Android Gradle Plugin 8.13.0 · Gradle 8.13 · JDK 17 |
| SDK | compileSdk / targetSdk 36 (Android 16) · minSdk 24 (Android 7.0) |
| ABI | `arm64-v8a` only (MediaPipe ships native libs; keeps the APK lean) |

## Build & run

Requirements: Android SDK with the API-36 platform, JDK 17, and the bundled Gradle wrapper.

```bash
# Debug build (installs on any phone, no signing setup needed)
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# Build + install onto a connected device (USB debugging enabled)
./gradlew installDebug
```

> **Release builds** are signed with a private keystore that is intentionally **not committed**. To
> produce your own signed release, create a keystore and point the `release` `signingConfig` in
> [`app/build.gradle.kts`](app/build.gradle.kts) at it (ideally via environment variables), then run
> `./gradlew assembleRelease`. Debug builds need none of this.

On first launch the app downloads the language model once; after that it is fully offline. Devices
without enough memory automatically fall back to the scripted customer brain.

## Project structure

```
app/src/main/java/ai/sarj/agentsim/
├── MainActivity.kt                 # entry point
├── config/GameConfig.kt            # ★ all tunable numbers: ranks, XP curve, upgrades, model config, name/matric
├── model/Models.kt                 # immutable GameState, PlayerProfile, Rank, Upgrade + enums
├── game/AppViewModel.kt            # the brain: game loop, spawns, scoring, ranks, shop, daily hooks
├── data/
│   ├── CustomerFactory.kt          # procedural customer roster + banking data + planted fraud
│   ├── Personas.kt / PersonaLines.kt / ConversationSeeds.kt   # persona behaviour + dialogue
│   ├── AgentTips.kt                # 36 mentor lessons shown after each shift
│   └── ProfileRepository.kt        # DataStore persistence (tokens, XP, ranks, upgrades, streaks)
├── llm/
│   ├── CustomerBrain.kt            # interface
│   ├── LlmCustomerBrain.kt         # Qwen 2.5-1.5B via MediaPipe
│   ├── ScriptedCustomerBrain.kt    # deterministic fallback
│   └── ModelManager.kt             # one-time download + load
├── audio/SoundManager.kt           # procedural SFX (AudioTrack PCM synthesis)
├── util/DeviceGate.kt              # memory gate for the on-device model
└── ui/
    ├── AppRoot.kt                  # routes Splash → Download → Home → Playing → Summary / Shop
    ├── screens/                    # AppScaffold (shift), ChatsScreen, ToolTabs (banking), SummaryScreen, ShopScreen…
    ├── components/                 # chat bubbles, rating views, rank views, particles, press feedback…
    └── theme/                      # Color / Theme / Type
```

## Configuration

`config/GameConfig.kt` is the single source of truth for tunables. Notable knobs: the rank/XP curve
(`RANKS`), the upgrade catalog and prices (`UPGRADES`), the shift target, spawn cadence and patience
times, the AI-Assistant cost, and the on-device model URL/size. The player's on-screen credit
(`STUDENT_NAME` / `MATRIC`) is also set here.

## License

This project was built as a university serious-games assignment. You are welcome to read it and learn
from it. Qwen 2.5-1.5B is Apache-2.0 licensed by its authors.
