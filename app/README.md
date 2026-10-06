# GuardPay app (Kotlin Multiplatform)

- `shared/`: toda la lógica, en `commonMain`, sin APIs de Android, iOS, LiteRT-LM ni WebAuthn. Paquetes `domain`, `ai`, `signing`, `stellar` y `ui` (Compose Multiplatform: las pantallas de la dueña y del guardián).
- `android/`: la app de demostración. Android es el dispositivo de la demo.
  - `src/debug`: cablea la UI a una **simulación** en memoria (`SimulatedStellarGateway`). Cada pantalla lo dice: "Simulación: nada de esto está en Stellar".
  - `src/release`: sin sesiones hasta tener las direcciones de los contratos en testnet. Un build de release no muestra datos inventados.

## Plataformas

| Plataforma | Estado |
| --- | --- |
| Android | Se compila (APK de debug y Kotlin de release). El esqueleto del día 1 corrió en un Galaxy A54; **la UI del día 4 aún no se ha abierto en un teléfono.** |
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

`allTests` ejecuta los tests unitarios (Android y JVM) y los cuatro chequeos: arquitectura de `commonMain`, grafo de dependencias, vocabulario de la UI y superficie del guardián. Los tests JVM dibujan las pantallas a 360 dp y guardan las capturas en `evidence/demo/screens/` (SIMULATED). El segundo comando es opcional y usa **Stellar Testnet** (nunca mainnet). Evidencia: `evidence/architecture/kmp-layers.md`, `evidence/stellar/`, `evidence/demo/screens/`.
