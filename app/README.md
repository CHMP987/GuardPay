# GuardPay app (Kotlin Multiplatform)

Es el cliente de GuardPay: la dueña paga y su guardián puede detener un pago retenido. Toda la lógica está en `shared/commonMain`. Android es el dispositivo de la demo. Solo **Stellar Testnet**.

## Estructura

| Módulo | Qué hay |
| --- | --- |
| `shared/commonMain/domain` | Los 9 estados de pago como tipos sellados, sus transiciones y el carril (inmediato o con espera). |
| `shared/commonMain/stellar` | `StellarGateway` y `KmpStellarGateway`, sobre `com.soneso.stellar:stellar-sdk:1.14.0`. También `AuthEntryGuard`, que solo firma la llamada exacta que la app construyó; los decodificadores SCVal; `ContractAbi`; y `TestnetConfig`, que es el `testnet.json` con las direcciones. |
| `shared/commonMain/signing` | La interfaz `Signer`. Firma un hash de 32 bytes y nada más. |
| `shared/commonMain/ai` | `GuardPayAI.analyzeMessage(text)`. Hoy es un **stub** que devuelve `Unavailable` (P7 no se ha hecho). No ve saldos, contactos, claves ni la red. |
| `shared/commonMain/ui` | Compose Multiplatform: las 4 pantallas de la Propuesta visual, guiadas por `OwnerSession` y `GuardianSession`. El guardián solo ve `GuardianChain` (leer y cancelar). `HoldWatcher` convierte cada retención nueva en un aviso y solo lee. |
| `shared/androidUnitTest` | Tests contra testnet (`*LiveTest`, opcionales) y `TestnetProvisioner`, que despliega una cuenta para unas claves públicas. |
| `android/src/main` | `KeystoreSigner` (ed25519 en el Keystore, API 33+, huella o PIN en cada firma), `BiometricSigningPrompt` y `HoldWatchService` (lee el RPC cada 30 s y publica avisos locales). |
| `android/src/debug` | `Wiring`. Con `testnet.json` usa las claves del Keystore y los contratos de testnet; sin él, una simulación en memoria con el aviso "Simulación: nada de esto está en Stellar". |
| `android/src/release` | Sin sesiones: no se distribuyen direcciones de contratos. La Entrada muestra los botones desactivados. |

Las reglas de capas las imponen tareas de Gradle que corren dentro de `allTests` (ver abajo). Nada en `ai/` puede depender de `Signer` ni de `StellarGateway`.

## Plataformas

| Plataforma | Estado |
| --- | --- |
| Android | APK de debug probado en un **emulador** (API 37) contra testnet: firma, retención, aviso, detención y la pantalla de reglas (`evidence/demo/rehearsal.md`). En el teléfono físico (Galaxy A54, Android 14) corrieron el 7 oct (UTC) las escenas 1, 2, 3 y 6 contra testnet, con huella (`evidence/demo/physical-phone.md`). La dueña y el guardián estaban en el mismo teléfono. minSdk 28; la firma con Keystore pide Android 13+. |
| iOS | **Declarada, no compilada ni ejecutada.** Los targets `iosArm64` e `iosSimulatorArm64` están en Gradle y sus klibs compilan en Windows. Nadie del equipo tiene un Mac, así que no se ha enlazado ningún binario ni se ha corrido nada en un iPhone o en un simulador. No afirmamos que iOS funcione. |

Versiones fijadas en `gradle/libs.versions.toml`: Kotlin 2.2.20, AGP 8.12.3, Compose Multiplatform 1.9.1, stellar-sdk 1.14.0, compileSdk/targetSdk 35.

## Requisitos

- JDK 17.
- Android SDK, con su ruta en `local.properties` (archivo ignorado por git). Usa barras normales: `sdk.dir=C:/Users/<tú>/AppData/Local/Android/Sdk`.
- Gradle 8.14.3 viene con el wrapper; no hace falta instalarlo.

## Tests

```bash
./gradlew :app:shared:allTests
```

Corre los tests unitarios de Android y del target `jvm()` (que solo existe para tests). Resultado del 6 oct: 341 tests, 0 fallos. También ejecuta estos cuatro chequeos, que fallan el build:

| Tarea | Falla si… |
| --- | --- |
| `checkCommonMainArchitecture` | `commonMain` importa `android.*`, `androidx.*`, `platform.*`, LiteRT-LM o WebAuthn |
| `checkDependencyGraph` | algo en `ai/` toca `signing` o `stellar`; un archivo de producción nombra `GuardPayAI` junto a `Signer` o `StellarGateway`; o un `Fake*` aparece fuera de los tests |
| `checkUiVocabulary` | un literal de la UI usa, como palabra completa, una de las tres palabras que el plan prohíbe en la interfaz |
| `checkGuardianSurface` | `ui/guardian/**` nombra `StellarGateway`, `Signer`, `submitTransfer`, `submitQueue` o `signing` |

Una sola clase de test:

```bash
./gradlew :app:shared:testDebugUnitTest --tests "com.guardpay.shared.domain.LaneTest"
```

Las capturas de las pantallas a 360 dp se escriben en `evidence/demo/screens/` (SIMULATED):

```bash
./gradlew :app:shared:jvmTest
```

Tests contra **Testnet** (FriendBot; claves nuevas en memoria en cada corrida, nunca impresas). Sin `-PliveTestnet` quedan excluidos, así que `allTests` no usa la red:

```bash
./gradlew :app:shared:testDebugUnitTest -PliveTestnet --tests "*LiveTest"
```

Tests instrumentados del Keystore (Android 13+ con bloqueo de pantalla; dispositivo o emulador conectado):

```bash
./gradlew :app:android:installDebugAndroidTest
```

```bash
adb shell am instrument -w -r com.guardpay.android.test/androidx.test.runner.AndroidJUnitRunner
```

## Construir e instalar

```bash
./gradlew :app:android:assembleDebug
```

```bash
adb install -r -t app/android/build/outputs/apk/debug/android-debug.apk
```

Comprobar que el build de release compila (no se conecta a nada):

```bash
./gradlew :app:android:compileReleaseKotlin
```

## Conectar la app a testnet (build debug)

Sin estos pasos, la app de debug corre la simulación en memoria.

1. Abre la app una vez en Android 13+ con bloqueo de pantalla. La app crea las claves de la dueña y del guardián en el Keystore y escribe sus direcciones G (públicas) en `/sdcard/Android/data/com.guardpay.android/files/keys.json`.
2. Lee ese archivo con `adb shell cat` y despliega una cuenta para esas claves:
   ```bash
   ./gradlew :app:shared:testDebugUnitTest -PliveTestnet -Pgp.owner=G... -Pgp.guardian=G... --tests "*DeviceProvisioningLiveTest"
   ```
   El test comprueba que la cuenta tiene una sola regla, que la dueña es la única firmante y que el guardián no es firmante. Después escribe `app/shared/build/testnet.json`.
3. Sube el archivo y reabre la app:
   ```bash
   adb push app/shared/build/testnet.json /sdcard/Android/data/com.guardpay.android/files/testnet.json
   ```

En ningún paso hay una semilla: solo entran y salen direcciones públicas. La cuenta de la demo del 6 oct es `CDWPPTDMACOEVMBUHBBTPUW2EEDXY6YVUAOZJFTSGGOBAONCAJ7WGFV6`.

## Emulador (cómo se probó)

- AVD `Medium_Phone_API_37.0`, con PIN de pantalla 1234. El PIN libera cada firma.
- El host tiene 7,4 GB de RAM. Gradle y el emulador no caben a la vez: hay que correr `./gradlew --stop` antes de arrancar el emulador, y apagarlo (`adb emu kill`) antes de compilar.
- `adb screencap` y `screenrecord` muestran el diálogo de huella o PIN en negro.
- El ensayo de la demo está automatizado en `evidence/demo/rehearsal/rehearse.py` (`python rehearse.py TAG 1 23 6`). Graba cada escena y anota los tiempos. Deja los vídeos, capturas y logs junto al script.

## Evidencia

| Tema | Archivo |
| --- | --- |
| Capas y grafo de dependencias | `evidence/architecture/kmp-layers.md` |
| Gateway contra testnet, GH-01 y GH-24 | `evidence/stellar/gateway-live.md`, `evidence/smart-account/signing.md` |
| Firma con Keystore | `evidence/smart-account/signing.md` |
| Aviso al guardián sin backend | `evidence/guardian/notification.md` |
| Pantallas (SIMULATED) | `evidence/demo/screens/` |
| Ensayo de las escenas 1, 2, 3 y 6 (REAL) | `evidence/demo/rehearsal.md` |

## Limitaciones

- **La firma es a ciegas.** El diálogo del sistema no muestra ni el destino ni el monto: la clave firma un hash. Lo mitigan la retención en cadena y la vista del guardián, no la clave.
- **Pagar pide la huella dos veces:** una para la autorización de la cuenta y otra para la transacción, porque la clave de la dueña también paga la comisión. Detener la pide una vez (`evidence/demo/physical-phone.md`).
- **El aviso al guardián no tiene backend.** El sistema puede detener el servicio de sondeo, y entonces el aviso llega cuando se abre la app o se toca "Actualizar". Consultar cada 30 s no es aceptable en batería para producción.
- **Las dos claves están en un mismo teléfono** en debug, para la demo. El aviso entre dos teléfonos no se ha probado.
- **Los pagos inmediatos no se releen.** Un pago a un contacto sigue en cadena, pero desaparece de "Recientes" al reiniciar la app.
- **El guardián no se puede cambiar** y no hay recuperación de cuenta.
- **La IA es un stub.**
