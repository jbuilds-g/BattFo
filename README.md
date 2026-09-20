## BattFo

> A lightweight, privacy-focused Android battery monitor.

BattFo is a modern Android battery monitoring app designed to make battery telemetry easy to understand without the bloat found in typical battery-monitoring and "battery optimizer" apps.

Built with **Material 3 Expressive**, BattFo focuses on real device telemetry, useful history, charging information, and a clean native Android experience.

---

> [!NOTE]
> **Status:**
>
> - BattFo is currently in active development. Some features may be incomplete, unstable, or have rough edges, and you may encounter bugs or unfinished UI.

---

## Features

- Real-time battery percentage and charging state
- Charging and discharging telemetry
- Battery current and voltage
- Power consumption and charging power
- Battery temperature
- Estimated charging and remaining runtime
- Charging session information
- Battery history and telemetry graphs
- Battery health and device information where supported
- Adaptive UI based on available device telemetry
- Dynamic Color
- Light and dark themes
- Local-first data storage
- No ads
- No account required
- No unnecessary permissions
- No battery optimizer or RAM-cleaning gimmicks
  
---

## Screenshots

_Coming soon._

---

## Requirements

- Android 7.0 (API 24) or newer
- Some battery metrics depend on device hardware and Android/OEM support.

---

## Download

Download the latest release from the **GitHub Releases** page.

Choose the APK matching your device architecture:

- `arm64-v8a` - Most modern Android phones
- `armeabi-v7a` - Older 32-bit ARM devices
- `x86_64` - Compatible x86 devices and emulators
- `universal` - Supports all included architectures

----

## Building

Clone the repository and open the project in Android Studio.

    git clone https://github.com/jbuilds-g/BattFo.git
    cd BattFo

Then build the project with Gradle.

    ./gradlew assembleDebug

On Windows:

    .\gradlew.bat assembleDebug
    
---

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3 Expressive
- Android Jetpack
- Room
- Kotlin Coroutines
- Android BatteryManager APIs
  
---

## Privacy

BattFo is designed to keep battery data on your device.

The app does not require an account or cloud synchronization to provide its core functionality.

---

## Philosophy

BattFo is a **battery monitor, not a battery optimizer**.

It doesn't pretend to magically improve battery life, clean RAM, kill apps, or provide fake "AI-powered" battery health scores.

If Android cannot provide reliable information for a metric, BattFo does not fabricate it.

---

## Contributing

Contributions, bug reports, and improvements are welcome.

Please open an issue or pull request on GitHub.

---

## License

BattFo is licensed under the **MIT License**.

See [LICENSE](LICENSE) for the full license text.

---

> [!NOTE]
> The core logic and application code were generated with **AI** under my supervision and direction.
