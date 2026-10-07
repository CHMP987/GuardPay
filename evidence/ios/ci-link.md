# Día 8: Hito 0, el módulo `shared` enlaza para iOS (CI en macOS)

**Veredicto: el framework de `shared` enlaza para el simulador y para el dispositivo (arm64), y los 109 tests de `commonTest` pasan en el simulador de iOS.** Todo corrió en un runner de GitHub Actions con macOS. No hay app de iOS todavía (`iosApp/` es iOS-A, día 9) y nada se ejecutó en un iPhone.

| | |
| --- | --- |
| Etiqueta | **REAL**: CI en macOS. Sin red de Stellar: los tests de `commonTest` usan fakes. |
| Corrida | [actions/runs/37576517767](https://github.com/CHMP987/GuardPay/actions/runs/37576517767), rama `ios`, 2026-10-07 05:28–05:50 UTC, `success` |
| Runner | `macos-15` (arm64), Xcode 16.4 (16F6) fijado con `xcode-select`, JDK 17 (Temurin) |
| Workflow | `.github/workflows/ios.yml` |
| Gradle | 8.14.3, Kotlin 2.2.20, Compose Multiplatform 1.9.1, stellar-sdk 1.14.0 |

## libsodium

El cinterop de stellar-sdk para iOS no trae libsodium. Su manifiesto solo tiene `linkerOpts` de macOS (Homebrew). La documentación del SDK pide añadir `Clibsodium` de `jedisct1/swift-sodium` por SPM en Xcode. Para enlazar desde Gradle sin Xcode, el workflow baja el `libsodium.a` de cada slice del `Clibsodium.xcframework` de ese repo y se lo pasa al enlazador con `-Pgp.sodium`.

| | |
| --- | --- |
| Fuente | swift-sodium **0.11.0**, commit `cfd195c76882aa9b997560ca7cb95d72fbf5db00` |
| `ios-arm64_arm64e/libsodium.a` | arm64 arm64e, sha256 `d678dc76dbc6e34d30954f3644622aae8cbbacf3da03226416e7684196e65393` |
| `ios-arm64_arm64e_x86_64-simulator/libsodium.a` | x86_64 arm64 arm64e, sha256 `b59ff5ecd5d00674d1dd52c8155b613ebbcaf630cf6a7b29e0710dff7dd38e17` |

Sin `-Pgp.sodium`, `build.gradle.kts` no añade nada. Por eso el build de Windows no cambia: `allTests` dio 341 tests y 0 fallos tras el cambio, y `compileKotlinIosSimulatorArm64` y `:app:android:assembleDebug` siguieron pasando.

## Pasos

| Paso | Tarea | Resultado |
| --- | --- | --- |
| 1 | `linkDebugFrameworkIosSimulatorArm64` (framework estático `Shared`) | OK |
| 2 | `linkDebugTestIosSimulatorArm64` (ejecutable de test, enlaza libsodium) | OK. `BUILD SUCCESSFUL in 6m 50s` |
| 3 | `iosSimulatorArm64Test` | OK: **109 tests, 0 fallos, 0 omitidos**, 14 clases |
| 4 | `linkDebugFrameworkIosArm64` (dispositivo, sin firmar) | OK |

Clases que corrieron en el simulador: `AnalysisParserTest`, `LaneTest`, `PaymentStateTest`, `ValuesTest`, `AuthEntryGuardTest`, `ChainDecodersTest`, `GatewayFlowTest`, `SubmitClassifierTest`, `ContrastTest`, `FormatTest`, `GuardianSessionTest`, `HoldWatcherTest`, `NavigatorTest` y `OwnerSessionTest`. Son las mismas 109 de `commonTest` que corren en Android y en JVM.

## Lo que esto prueba y lo que no

- [FACT] El código de `commonMain` (dominio, gateway, decodificadores, `AuthEntryGuard`, sesiones y UI de Compose) enlaza en binarios de iOS, junto con stellar-sdk y libsodium.
- [FACT] La lógica de `commonTest` da los mismos resultados en el simulador de iOS que en JVM y Android.
- [INFERENCE] El ejecutable de test enlazó con `-lsodium`, así que los símbolos de libsodium que pide el SDK se resuelven. Pero **ningún test firma ni verifica con ed25519**. `commonTest` solo usa `KeyPair.fromPublicKey`, que no llama a libsodium. Que la firma funcione en iOS sigue sin probarse.
- [FACT] El enlace del framework estático no comprueba los símbolos externos. La comprobación real de libsodium es el paso 2.
- [UNVERIFIED] La documentación del SDK pide Gradle 9.0 o más. Aquí se usó 8.14.3 y enlazó. No se probó nada que dependa de esa versión.

## Avisos del log (no fallan el build)

- En `commonTest` (`OwnerSessionTest` y `GuardianSessionTest`) hay usos de `ExperimentalCoroutinesApi` sin `@OptIn`. También salen en JVM.
- `FakeStellarGateway.kt:141`: el parámetro se llama distinto que en `Signer` (`hash`).

## Corrida fallida anterior

La [corrida 37576400338](https://github.com/CHMP987/GuardPay/actions/runs/37576400338) falló al bajar libsodium con un 404. Se había fijado el SHA del objeto tag (`df17ff8…`) y no el del commit. Se arregló fijando el commit. No llegó a compilar nada.

## No corrido

- Una app de iOS (`iosApp/`, XcodeGen) y la simulación en el simulador: es iOS-A, el día 9.
- Firmar con ed25519 en iOS, con libsodium o con el Keychain (iOS-B).
- Cualquier cosa en un iPhone físico.
- Tests contra testnet desde iOS.
