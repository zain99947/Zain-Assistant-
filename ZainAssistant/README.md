# Zain Assistant

A Kotlin + Jetpack Compose Android voice assistant (MVVM, Material 3, minSdk 26).

## Opening the project

1. Open Android Studio (Koala/2024.1 or newer recommended).
2. **File → Open** → select the `ZainAssistant` folder (the one containing `settings.gradle.kts`).
3. Let Gradle sync. It will download: AndroidX Core/Compose/Navigation, DataStore, Retrofit + OkHttp, kotlinx.serialization, kotlinx.coroutines.
4. Build → Run on a device or emulator running API 26+ (a real device is strongly recommended for testing voice recognition, contacts, and camera/flashlight — emulators have limited/no microphone and no flash).

## First run

- On first launch the app requests all standard runtime permissions (microphone, phone, contacts, SMS, camera, notifications) in one batch dialog.
- Go to **Settings → your AI provider's API key** and paste a key from OpenAI (or any OpenAI-compatible provider — see below). Without a key, offline device commands still work; only free-form AI conversation needs it.
- Go to **Settings → Manage app permissions** to turn on Notification Listener access and Accessibility Service access. **These two cannot be auto-granted by any app** — Android requires the user to flip them on manually in system settings as an anti-malware protection. The screen deep-links you straight to the right toggle.

## Switching AI providers

`Settings → API Base URL` and `Model name` let you point at any OpenAI-compatible `/v1/chat/completions` endpoint (OpenAI itself, OpenRouter, Groq, a self-hosted Ollama+LiteLLM proxy, etc.) without touching code. The Retrofit client (`data/remote/ApiClient.kt`) is rebuilt from these values at request time.

## What's implemented

- Continuous voice loop with an on-device wake-word approximation ("Hey Zain") built on Android's `SpeechRecognizer`, restarted in a loop and checked for the wake phrase (see the doc comment in `voice/SpeechRecognizerManager.kt` for the trade-off vs. a dedicated low-power wake-word engine like Porcupine).
- Text-to-Speech via Android's built-in engine, with voice selection in Settings.
- An offline command parser (`domain/CommandParser.kt`) that instantly handles calling contacts, sending SMS, opening ~13 named apps (WhatsApp, YouTube, Camera, Chrome, Calculator, Calendar, Clock, Files, Maps, Contacts, Gallery, Settings) plus any other installed app by name, alarms, timers, calendar reminders, date/time, battery/storage/RAM/network status, flashlight, volume, brightness, joke-telling, and web search — all before ever hitting the network.
- Anything the offline parser doesn't recognize is forwarded to the AI provider as a normal conversational turn, with the last 20 messages sent as context and replies capped to 1–3 sentences for natural TTS playback.
- Conversation history persisted as JSON in app-private storage (survives restarts; capped at the last 500 messages).
- A foreground service (`service/VoiceForegroundService.kt`) keeps the mic loop alive when the app is backgrounded.
- Accessibility Service and Notification Listener Service stubs with working `onAccessibilityEvent`/notification-capture logic, wired to a permission-manager screen.
- Full Compose UI: animated splash screen, home screen with a rotating glowing AI orb, pulsing mic button, live conversation bubbles, status indicator, and a Settings screen (API key, base URL, model, wake word, voice, language, dark mode, permission manager link).
- Adaptive launcher icon as scalable vector art (blue/black gradient background, cyan-glow AI face + mic foreground) under `res/mipmap-anydpi-v26` — since minSdk is 26, this vector adaptive icon is the modern, complete replacement for generating a full set of legacy raster PNGs at every density; you don't need separate mipmap-hdpi/xhdpi/etc. folders.

## Known limitations (read before assuming 100% feature parity)

I wrote and reviewed every file by hand, but I do not have Android Studio/Gradle/an SDK or network access in the environment I built this in, so **this project has not been compiled or run**. Treat this as a strong, complete-looking starting point, not a guaranteed zero-error build. The most likely friction points if something doesn't compile immediately:
- Exact Compose BOM / Kotlin / AGP version pinning drifting from what's current when you open it — bump versions in `build.gradle.kts` if Android Studio flags a mismatch.
- Device-specific package names for Calculator/Clock/Files apps (I included the common OEM package names, but some manufacturers ship different ones — the code already falls back gracefully with a spoken error rather than crashing).

Other honest constraints, by Android OS design rather than anything missing from this build:
- **Accessibility Service and Notification Listener access can never be silently auto-granted** by any app, including this one — only the user can flip these on, from the screens this app deep-links to.
- The wake-word engine here is a practical SpeechRecognizer-loop approximation, not a dedicated low-power always-listening model; it will use more battery than a purpose-built wake-word SDK.
- `WRITE_SETTINGS` (needed for brightness control) is also a special permission the user grants via a system screen, not a normal runtime dialog.

## Project structure

```
app/src/main/java/com/zain/assistant/
  ui/            Compose screens, navigation, theme
  data/          Retrofit API client, settings (DataStore), conversation history (JSON store)
  domain/        Command parser, message models
  actions/       Phone-control executors (calls, apps, system info, flashlight, volume…)
  voice/         SpeechRecognizer + TextToSpeech wrappers
  service/       Foreground service, Accessibility service, Notification listener
```
