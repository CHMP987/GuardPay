# GuardPay app (Kotlin Multiplatform)

- `shared/`: toda la lógica, en `commonMain`, sin APIs de Android, iOS, LiteRT-LM ni WebAuthn. Paquetes `domain`, `ai`, `signing` y `stellar`.
- `android/`: la app de demostración (Compose). Android es el dispositivo de la demo.

## Plataformas

| Plataforma | Estado |
| --- | --- |
| Android | Se compila y corre (APK de debug; probado en un Galaxy A54). |
| iOS | **Declarada, no compilada ni ejecutada.** Los targets `iosArm64` e `iosSimulatorArm64` existen en Gradle y sus klibs compilan en Windows, pero nadie del equipo tiene un Mac: no se ha enlazado ningún binario ni se ha corrido nada en un iPhone o simulador. No afirmamos que iOS funcione. |

## Comandos

JDK 17. La ruta del Android SDK va en `local.properties`.

```bash
./gradlew :app:shared:allTests
```

```bash
./gradlew :app:shared:testDebugUnitTest -PliveTestnet --tests "*LiveTest"
```

```bash
./gradlew :app:android:assembleDebug
```

`allTests` ejecuta los tests unitarios y los dos tests de arquitectura. El segundo comando es opcional y usa **Stellar Testnet** (nunca mainnet). Evidencia: `evidence/architecture/kmp-layers.md`, `evidence/stellar/`.
