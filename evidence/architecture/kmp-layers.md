# Capas KMP: tests de arquitectura y dependencias de `commonMain` (P4 día 3, P5 día 4, P6/P8 día 5)

| | |
| --- | --- |
| Etiqueta | **REAL** (salida literal de Gradle en esta máquina) |
| Fecha | 2026-10-06 |
| Entorno | Windows 11, JDK 17.0.20, Gradle 8.14.3, Kotlin 2.2.20, AGP 8.12.3 |
| Módulo | `app/shared` |

## 1. Capas

| Paquete (`commonMain`) | Contenido | Depende de |
| --- | --- | --- |
| `domain` | 9 estados de pago (tipos sellados), transiciones, carril, `HeldPayment`, valores | nada |
| `ai` | `GuardPayAI.analyzeMessage(text): Analysis` y el parser | `domain` |
| `signing` | `Signer` (firma digests de auth) | nada |
| `stellar` | `StellarGateway` y `KmpStellarGateway`; decodificadores de cadena, guardia de auth, clasificador de envíos, ABI de los contratos desplegados (`ContractAbi`) y `TestnetConfig` (direcciones leídas de `testnet.json`) | `domain`, `signing`, stellar-sdk 1.14.0 |
| `ui` (día 4) | Compose Multiplatform: tokens, componentes, `OwnerSession` (Entrada, Inicio, Pagar + Revisar, Detalle) y `GuardianSession` (lista, detalle, Detener) | `domain`, `ai`, `signing`, `stellar` |
| `ui/guardian` | Las pantallas del guardián. Solo ven `GuardianChain`: leer el ledger, las retenciones y los contactos, y `submitCancel`. `HoldWatcher` (día 5) solo usa `readHolds` | `domain`; de `stellar` solo los tipos de resultado `SubmitResult` y `SubmissionOutcomeUnknownException` |

`ai` no ve a `signing` ni a `stellar`, y ningún archivo combina `GuardPayAI` con `Signer`/`StellarGateway`. `checkDependencyGraph` lo hace cumplir.

La UI recibe la lectura de la IA como un `MessageReader` (texto → `Analysis`). El código de `ui/` no usa `GuardPayAI` (solo lo nombra un comentario): el cableado vive en `app/android` (`AiWiring.kt`). La lectura nunca toca el formulario (`OwnerSessionTest.theAiReadingNeverTouchesTheForm`).

La app Android cablea la simulación solo en `src/debug` (`SimulatedStellarGateway`, claves aleatorias en memoria). En `src/release` las sesiones son `null` hasta que existan las direcciones de los contratos (Sync 2), así que un build de release no muestra datos inventados.

## 2. Tests de arquitectura

Las cuatro tareas cuelgan de `check` y `allTests`.

| Tarea | Regla |
| --- | --- |
| `checkCommonMainArchitecture` | `src/commonMain` no importa `android.*`, `androidx.*`, `platform.*`, LiteRT-LM (`com.google.ai.edge.*`, `*litert*`) ni WebAuthn/passkey |
| `checkDependencyGraph` | `ai/` no referencia `signing`/`stellar`/`Signer`/`StellarGateway`; `GuardPayAI` no comparte archivo con ellos; ningún `Fake*` en código de producción |
| `checkUiVocabulary` (día 4) | Ningún literal de cadena en `ui/` ni en `app/android/src` (más los `strings*.xml`) dice "seguro", "protegido" o "verificado", sin distinguir mayúsculas. Los comentarios no cuentan, y "Proteges a Laura" pasa porque la regla busca la palabra completa |
| `checkGuardianSurface` (día 4) | `ui/guardian/**` no menciona `StellarGateway`, `Signer`, `submitTransfer`, `submitQueue` ni el paquete `signing` |

Además, `GuardianSurfaceTest` (jvmTest) comprueba por reflexión que `GuardianChain` declara exactamente `latestLedgerTime`, `readHolds`, `readTrustedContacts` y `submitCancel`.

### Controles negativos

Se inyectó una violación temporal y se borró después; `git status` quedó limpio.

```
=== NEG 1: checkCommonMainArchitecture ===   (domain/ZzViolation.kt con `import android.util.Log`)
Execution failed for task ':app:shared:checkCommonMainArchitecture'.
> commonMain must not depend on platform, LiteRT-LM or WebAuthn APIs:
  src\commonMain\kotlin\com\guardpay\shared\domain\ZzViolation.kt:3: import android.util.Log
exit=1

=== NEG 2: checkDependencyGraph ===   (ai/ZzLeak.kt con `class ZzLeak(val s: Signer)`)
Execution failed for task ':app:shared:checkDependencyGraph'.
> Dependency graph violations:
  src/commonMain/kotlin/com/guardpay/shared/ai/ZzLeak.kt: ai/ references signing or stellar
exit=1

=== POS (violaciones borradas) ===
> Task :app:shared:checkCommonMainArchitecture
> Task :app:shared:checkDependencyGraph
BUILD SUCCESSFUL in 1s
```

Día 4, mismo método. `ui/ZzProbe.kt` contenía un comentario con "seguro" y los literales "Proteges a Laura" y "Tu dinero está PROTEGIDO". `ui/guardian/ZzReach.kt` contenía `import com.guardpay.shared.signing.Signer`. La consola de Windows imprime "está" como `est�`.

```
=== NEG 3: checkUiVocabulary ===
Execution failed for task ':app:shared:checkUiVocabulary'.
> UI strings must not say seguro, protegido or verificado:
  app/shared/src/commonMain/kotlin/com/guardpay/shared/ui/ZzProbe.kt: "Tu dinero est� PROTEGIDO"
exit=1

=== NEG 4: checkGuardianSurface ===
Execution failed for task ':app:shared:checkGuardianSurface'.
> The guardian UI reaches beyond read + stop:
  src/commonMain/kotlin/com/guardpay/shared/ui/guardian/ZzReach.kt: \bSigner\b
  src/commonMain/kotlin/com/guardpay/shared/ui/guardian/ZzReach.kt: com\.guardpay\.shared\.signing
exit=1

=== POS (archivos borrados) ===
> Task :app:shared:checkUiVocabulary
> Task :app:shared:checkGuardianSurface
BUILD SUCCESSFUL in 1s
```

El comentario y "Proteges a Laura" no se reportaron: solo cuenta el literal prohibido.

## 3. Tests unitarios (`./gradlew :app:shared:allTests`)

Día 4: `BUILD SUCCESSFUL`. **105 tests** en `testDebugUnitTest` (Android unit tests) y **119** en `jvmTest`, con 0 fallos y 0 omitidos en ambos. El target `jvm()` existe solo para tests; no hay app de escritorio.

| Suite | Android | JVM |
| --- | --- | --- |
| `ai.AnalysisParserTest` | 13 | 13 |
| `domain.LaneTest` | 8 | 8 |
| `domain.PaymentStateTest` | 17 | 17 |
| `domain.ValuesTest` | 7 | 7 |
| `stellar.GatewayFlowTest` (contra `FakeStellarGateway`) | 9 | 9 |
| `stellar.ChainDecodersTest` (parseo de lecturas de cadena) | 14 | 14 |
| `stellar.SubmitClassifierTest` | 5 | 5 |
| `stellar.AuthEntryGuardTest` | 5 | 5 |
| `ui.ContrastTest` (tabla de contraste de la Propuesta visual) | 4 | 4 |
| `ui.FormatTest` (montos, horas, direcciones, frases de estado) | 7 | 7 |
| `ui.OwnerSessionTest` (flujos de la dueña) | 9 | 9 |
| `ui.GuardianSessionTest` (Detener, atrás) | 4 | 4 |
| `ui.NavigatorTest` | 3 | 3 |
| `ui.GuardianSurfaceTest` (reflexión sobre `GuardianChain`) | | 1 |
| `ui.ScreensAt360Test` (Compose a 360 dp, 9 estados; ver `evidence/demo/screens/`) | | 13 |

El día 3 eran 78 tests; los 27 nuevos de `commonTest` son de `ui`.

Día 5: `BUILD SUCCESSFUL`, **338 tests** en total: 108 en `testDebugUnitTest`, 108 en `testReleaseUnitTest` y 122 en `jvmTest`. Hay 0 fallos y 0 omitidos. Los nuevos son `ui.HoldWatcherTest` (3), que comprueba que la alerta sale del registro leído, que no se repite tras un reinicio y que un fallo de lectura no parece silencio. Frente al día 4 (105 + 119) son 3 más por target. Los tests instrumentados de Android (`KeystoreSignerTest`, 5) corren aparte, en un dispositivo: ver `evidence/smart-account/signing.md`.

`:app:android:assembleDebug` y `:app:android:compileReleaseKotlin` pasan. **No corrido:** la app con esta UI no se ha abierto en un teléfono.

El test vivo de testnet (`KmpStellarGatewayLiveTest`) queda fuera de `allTests` y solo corre con `-PliveTestnet`. Ver `evidence/stellar/gateway-live.md`.

### Qué cubren los tests de la capa `stellar`

- `ChainDecodersTest`:
  - Un registro `Hold` completo.
  - Atribución de la cancelación: solo guardián o dueña; un tercero es un error.
  - Opciones `Void` o ausentes.
  - Un campo obligatorio ausente.
  - Un registro de otra cuenta o de otro token.
  - Un i128 negativo o mayor que `Long.MAX`: se rechaza, no se recorta.
  - Un u64 mayor que `Long.MAX`.
  - Una dirección muxed (M).
  - Un estado desconocido o mal formado.
  - Registros incoherentes, como `Held` con `cancelled_by` o `ready_at ≤ created_at`.
  - Tipos equivocados.
  - Contactos duplicados.
  - `hold_secs = 0`.
- `AuthEntryGuardTest`:
  - Solo se firma la llamada idéntica, byte a byte, a la que construyó la app. Se rechaza otro destino, otro monto, otra función, otro contrato y una sub-invocación oculta.
  - Solo se firma para la propia cuenta o para la cuenta G del firmante.
  - La configuración de red exige https, direcciones C y reglas no vacías.
- `SubmitClassifierTest`:
  - Mapeo de `send`/`getTransaction` a `SubmitResult`.
  - `NOT_FOUND` es un fallo solo cuando el ledger ya pasó `maxTime`; con `closeTime` nulo o igual a `maxTime` se sigue consultando.

## 4. Dependencias de `commonMain`

Comando: `./gradlew :app:shared:dependencies --configuration commonMainImplementationDependenciesMetadata`.

Dependencias declaradas (versiones exactas, sin `latest`):

```
+--- org.jetbrains.kotlin:kotlin-stdlib:2.2.20 -> 2.2.21
+--- com.soneso.stellar:stellar-sdk:1.14.0
+--- org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2
+--- org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0
```

Grafo resuelto completo, sin repetir módulos:

```
com.ionspin.kotlin:bignum:0.3.10
com.soneso.stellar:stellar-sdk:1.14.0
io.ktor:ktor-client-content-negotiation:3.3.2
io.ktor:ktor-client-core:3.3.2
io.ktor:ktor-client-darwin:3.3.2
io.ktor:ktor-events:3.3.2
io.ktor:ktor-http-cio:3.3.2
io.ktor:ktor-http:3.3.2
io.ktor:ktor-io:3.3.2
io.ktor:ktor-network-tls:3.3.2
io.ktor:ktor-network:3.3.2
io.ktor:ktor-serialization-kotlinx-json:3.3.2
io.ktor:ktor-serialization-kotlinx:3.3.2
io.ktor:ktor-serialization:3.3.2
io.ktor:ktor-sse:3.3.2
io.ktor:ktor-utils:3.3.2
io.ktor:ktor-websocket-serialization:3.3.2
io.ktor:ktor-websockets:3.3.2
org.jetbrains.kotlin:kotlin-stdlib-common:2.2.21
org.jetbrains.kotlin:kotlin-stdlib:2.2.21
org.jetbrains.kotlinx:atomicfu:0.23.1
org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2
org.jetbrains.kotlinx:kotlinx-datetime:0.7.1
org.jetbrains.kotlinx:kotlinx-io-bytestring:0.8.0
org.jetbrains.kotlinx:kotlinx-io-core:0.8.0
org.jetbrains.kotlinx:kotlinx-serialization-core:1.9.0
org.jetbrains.kotlinx:kotlinx-serialization-json-io:1.9.0
org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0
```

- [FACT] No aparece ningún artefacto Android, LiteRT-LM, WebAuthn ni OkHttp. Para comprobarlo se buscó `android|litert|webauthn|passkey|okhttp` en la salida: 0 coincidencias.
- [FACT] `kotlin-stdlib` declarado 2.2.20 se resuelve a 2.2.21 porque lo arrastra una dependencia transitiva. Es una subida de parche que decide Gradle, no un `latest`.
- [INFERENCE] `ktor-client-darwin` (el motor HTTP de iOS) aparece porque stellar-sdk lo publica en un source set compartido que la metadata común ve. Nuestro código no lo importa, y el control 1 lo vigila.

## 5. Desviación: `interface` en lugar de `expect interface`

El prompt de P4 pide `expect Signer` y `expect GuardPayAI`. Se usan **interfaces comunes** con implementaciones por plataforma.

- `Signer` en Android necesitará en P6 un alias del Keystore y una `Activity` para el prompt biométrico o de passkey.
- Día 5: `KeystoreSigner(alias, prompt)` en `app/android` implementa `Signer` y recibe por constructor el alias y el `BiometricSigningPrompt`, que encuentra la `Activity` visible. Confirma que la interfaz basta.
- `GuardPayAI` necesitará en P7 un `Context` y la ruta del modelo de LiteRT-LM. Hoy `AndroidGuardPayAI` es un stub sin parámetros.

Un `expect class` obliga a que el constructor tenga la misma firma en todas las plataformas, así que esos objetos tendrían que pasar por estado global. Con una interfaz cada plataforma recibe lo suyo por constructor. El aislamiento que buscaba el `expect` (que `commonMain` no vea la plataforma) lo garantizan igual los controles de la sección 2. `ai/` conserva un único método, `analyzeMessage`.

## 6. iOS

- [FACT] Los targets `iosArm64` e `iosSimulatorArm64` están declarados, y en Windows se compilan sus klibs: `compileKotlinIosSimulatorArm64` y `compileTestKotlinIosSimulatorArm64` pasan.
- [FACT] El enlace y la ejecución (`linkDebugTestIosSimulatorArm64`, `iosSimulatorArm64Test`) salen como `SKIPPED`: hace falta un Mac, y nadie del equipo tiene uno (gate G3).
- La app iOS **no está compilada como binario ni probada en ningún dispositivo ni simulador.**
