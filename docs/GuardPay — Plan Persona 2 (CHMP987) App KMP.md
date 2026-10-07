# GuardPay — Plan de la Persona 2 (CHMP987): App KMP

Plan personal derivado de `GuardPay — Plan de construcción.md` (sección 5, "Reparto del equipo"). Si este documento y el plan se contradicen, manda el plan. Los prompts ejecutables de cada fase están en `GuardPay — Prompts de ejecución.md`. Aquí van **el orden, las entregas y los cortes**; no se repiten los prompts.

**Etiquetas:** `[FACT]` · `[INFERENCE]` · `[RECOMMENDATION]` · `[UNVERIFIED]`.

---

## 1. Mi frente en una línea

Construyo la app que usan la dueña y el guardián: el núcleo compartido (`commonMain`), las 4 pantallas, la firma desde el teléfono y el aviso al guardián. **No escribo contratos ni la IA**; los consumo por interfaz.

| | |
| --- | --- |
| **Fases** | P1-C (Spike C) → P4 → P5 → P6 → P8 → iOS después de la congelación (§3, días 8–11, y §7) |
| **Carpetas mías** | `app/` salvo `app/.../ai/` (eso es de Ant) |
| **Archivo del que soy responsable** | `gradle/libs.versions.toml` (versiones exactas, nunca `latest` ni rangos) |
| **Consumo de Abraham** | `docs/INTERFACES.md` (firmas de contrato, errores, lecturas) y `evidence/stellar/deployment.md` (direcciones) |
| **Consumo de Ant** | el `actual` Android de `GuardPayAI` |
| **Entrego a Ant** | el `expect interface GuardPayAI` + un stub Android que devuelve `NoClearSignals` (día 2 temprano) |
| **Entrego a Ant para P9/P10** | una app que firma en testnet y capturas de los 9 estados |

**Riesgo propio:** `[INFERENCE]` es el frente con más fases y depende de dos entregas externas: las direcciones de Abraham y la IA de Ant. Por eso todo se construye primero contra `FakeStellarGateway` y un stub de IA. **Nunca espero a nadie para avanzar.**

---

## 2. Antes del día 1 (hoy, ≤ 1 h)

Estado del entorno comprobado en esta máquina `[FACT]`: JDK 17 instalado; Android SDK en `%LOCALAPPDATA%\Android\Sdk` (build-tools, cmake, cmdline-tools).

- [x] Android Studio instalado el 5 oct (build 262.9437.185, en `C:\Program Files\Android\Android Studio`). Usa el SDK que ya existe en `%LOCALAPPDATA%\Android\Sdk`.
- [x] Plugin de Kotlin Multiplatform 262.9437.115-AS cargado (verificado en `idea.log`, 5 oct). Android Studio apunta al SDK existente.
- [x] **Teléfono Android conectado y autorizado por `adb`:** Samsung Galaxy A54 (SM-A546E), Android 14 (API 34), 7,3 GB de RAM visibles. `[INFERENCE]` Cumple con margen la RAM que necesita Gemma 4 E2B (R4), pero su chip es Exynos 1380, no Snapdragon: las cifras de tok/s del plan no aplican directamente. `[UNVERIFIED]` hasta que Ant corra el Spike B en este teléfono.
- [ ] Segundo dispositivo para el guardián: otro Android, un emulador, o el iPhone por la ruta sin Mac de §7.
- [x] Leer la demo de smart accounts de `Soneso/kmp-stellar-sdk` v1.14.0. Es la referencia del Spike C.
- [ ] Leer `GuardPay — Propuesta visual.md` §5 (colores), §7 (pantallas), §8 (estados) y §9 (guardián).
- [x] G3 resuelto (5 oct): **nadie tiene Mac.** iOS se declara en Gradle pero no se compila, y el README lo dice.
- [x] Revisión del 6 oct: `[FACT]` las klibs de iOS **sí compilan en Windows** (`compileKotlinIosArm64`, `compileKotlinIosSimulatorArm64` y `compileTestKotlinIosSimulatorArm64` salen con código 0). Falta enlazar, empaquetar y correr: eso exige macOS, que se alquila en la nube (§7).

---

## 3. Calendario (fecha límite 12 oct)

### Día 1 — Spike C + esqueleto

| Bloque | Qué | Salida |
| --- | --- | --- |
| Mañana | Esqueleto Gradle KMP: `app/shared` con los paquetes `domain`, `stellar`, `signing` y `ai`; target Android; iOS declarado. `libs.versions.toml` con versiones exactas. | `./gradlew :app:shared:allTests` corre (aunque esté vacío) |
| Mañana | Test de arquitectura: falla si `commonMain` importa `android.*`, LiteRT-LM o WebAuthn. | Test verde |
| Tarde | **Spike C:** firmar un `transfer` en Kotlin con ed25519 contra la cuenta del Spike A de Abraham. Si aún no existe, contra la cuenta de la demo del SDK en testnet. Verificar el digest `sha256(signature_payload ‖ xdr(context_rule_ids))`. | `evidence/stellar/spike-c.md` con el hash REAL, o el escalón de R5 al que se llegó |
| Noche | **Sync 1** con Abraham y Ant: veredicto del Spike A y congelación de `INTERFACES.md`. | Firmas congeladas |

**Lo que tengo que exigir en el Sync 1.** Si no está en `INTERFACES.md`, el día 4 no cierra:
- Firmas exactas de `queue`, `cancel` y `transfer` (vía la cuenta), con los tipos de sus argumentos.
- **Las lecturas que necesita la UI:**
  - holds de una cuenta, cada uno con `id`, `to`, `amount`, `created_at`, `ready_at`, estado y quién canceló;
  - contactos de confianza, tope diario, gastado hoy y duración de la retención;
  - reglas de la cuenta, para "Reglas de esta cuenta".
- Códigos de error de `enforce` y del registro, para traducirlos a "Rechazado por el contrato" con la frase correcta.

### Días 2–3 — P4 núcleo compartido

- **Día 2, primera hora:** subir el `expect GuardPayAI` y el stub Android para que Ant enchufe P7 sin tocar mis carpetas.
- **Dominio:** los estados de la propuesta visual como tipos sellados (Borrador, Firmando, Retenido, Listo para enviar, Enviado, Detenido, Vencido (SHOULD), Rechazado por el contrato, Error de red). El sello "En Stellar" es una marca, no un estado.
- **Gateway:** `StellarGateway` (interfaz), `FakeStellarGateway` **solo en `commonTest`** y `KmpStellarGateway` (esqueleto, se completa el día 4).
- **Carril:** cálculo de Immediate | MustHold. Es informativo; la autoridad es el contrato.
- **Tests:** transiciones válidas e inválidas (de Detenido y de Enviado no se sale), bordes del tope y parseo de lecturas.
- **Fin del día 3:** P4 cerrado según su prompt, con `evidence/architecture/kmp-layers.md`.

### Día 4 — P5 UI + gateway real

- **Mañana:** UI contra el fake. El orden va por valor para la demo: **Pagar + "Revisar y firmar" → Detalle → Guardián (lista, detalle, confirmación de "Detener") → Inicio → Entrada**.
- **Sync 2,** cuando Abraham publica las direcciones: cambiar a `KmpStellarGateway` y leer datos reales de testnet.
- **Tokens exactos:** Navy `#0B1220` para la acción, Teal `#00A99D` solo para evidencia, nunca texto blanco sobre Teal. IBM Plex Sans/Mono. Pantallas desde 360 px.
- **Test de vocabulario:** falla si aparece "seguro" o "protegido" en los strings de la UI.

### Día 5 — P6 firma + P8 aviso (Abraham apoya P6)

- **P6:** `actual Signer` Android con ed25519 en Keystore.
  - Firmar `queue` y `transfer` y, en la app del guardián, `cancel`.
  - Reproducir GH-24 y GH-01 contra testnet desde la app.
- **Passkey (GH-33):** solo si sobra tiempo; es SHOULD.
- **P8:** `HoldWatcher` con consulta periódica al registro.
  - Notificación local con destino y monto leídos de la cadena, y enlace al Detalle.
  - Al abrirla, el Detalle vuelve a leer del RPC.
  - Objetivo: menos de 60 s.
- **Reparto con Abraham:** si a mediodía no funciona el ciclo queue → esperar → enviar, él toma el cableado de la firma contra el contrato.

### Día 6 — Sync 3: congelación

- Nada nuevo; solo arreglos de lo que encuentre Ant en P9.
- Capturas de los 9 estados en `evidence/demo/screens/`.
- Ensayar dos veces las escenas 1, 2, 3 y 6 del plan de demo, que son las que tocan la app. Grabar el vídeo de respaldo.

### Día 7 — Apoyo a la entrega

- Apoyar a Ant en P10: README de `app/`, y los comandos reales de build y test anotados en `CLAUDE.md`.

### Días 8–11 (7–11 oct) — Teléfono físico + iOS en la rama `ios`

Va **después** de la congelación y nunca bloquea la entrega. Todo el trabajo de iOS vive en la rama `ios`. `main` solo recibe un merge si `allTests` y el APK de Android siguen verdes. El detalle técnico está en §7.

| Día | Qué | Salida |
| --- | --- | --- |
| 8 (7 oct), mañana | Cerrar la depuración del Galaxy A54: la app actual con la cuenta del teléfono, los flujos de pagar, retener y detener, y logcat. | `evidence/demo/physical-phone.md` (REAL o "no corrido") |
| 8, tarde | **Hito 0, CI en macOS:** `.github/workflows/ios.yml` en `macos-15` con Xcode 16.x fijado. Enlaza el framework de `shared` para el simulador con libsodium. | El framework enlaza, o el error de enlace queda anotado |
| 9 (8 oct) | **Hito A, simulador:** `iosApp/` con XcodeGen y Compose `App()` en modo simulación. El CI arranca el simulador, abre la app y saca captura y log. | `evidence/ios/simulator.md` (SIMULATED) con la captura |
| 10 (9 oct) | `.ipa` sin firmar como artefacto del CI. **La instalo yo** con Sideloadly y mi Apple ID secundario: Hito A en el iPhone. Después **Hito B:** `KeychainSigner` en `iosMain`, `keys.json` y `testnet.json` por archivos compartidos. | El iPhone abre la app; el iPhone firma en testnet |
| 11 (10 oct) | **Variante de dos dispositivos:** el iPhone es el guardián y el A54 la dueña. Escenas 2 y 3 entre los dos teléfonos. | `evidence/ios/two-devices.md` (REAL), con hashes de Horizon |
| 11 oct | Congelación de iOS: README (fila "Plataformas") y `ESTADO-FASES.md` con lo que de verdad corrió. Merge de `ios` a `main` solo si todo está verde. | README honesto |
| 12 oct | Entrega. Nada nuevo. | — |

**Prioridad dentro de estos días:** primero el A54 y lo que pidan Abraham y Ant para la entrega (cuenta de la demo, Passport y vídeo); después iOS.

---

## 4. Definición de terminado por fase

| Fase | Terminado cuando |
| --- | --- |
| P1-C | Una firma producida en Kotlin es aceptada por `__check_auth` en testnet, con su hash en `evidence/`, o quedó registrado el escalón de R5 al que se llegó. |
| P4 | `./gradlew :app:shared:allTests` verde, y el test de arquitectura también. Ninguna clase que tenga `Signer` o `StellarGateway` recibe `GuardPayAI`. `FakeStellarGateway` queda fuera de producción. |
| P5 | Las 4 pantallas navegables en Android con datos reales de testnet. Ningún estado se distingue solo por color. Nunca "Retenido" sin registro en la cadena. El guardián no tiene monto, ni "Pagar", ni "Aprobar", ni ajustes. |
| P6 | Un `transfer` firmado desde el teléfono se ejecuta en testnet, y uno sin la firma de la dueña se rechaza. La clave no sale del Keystore ni aparece en logs. El guardián nunca se añade como firmante. |
| P8 | Un `queue` en el teléfono de la dueña genera una notificación en el del guardián en menos de 60 s, con el destino leído de la cadena. Capturas y tiempos en `evidence/guardian/notification.md`. |
| iOS-0 | El CI de macOS enlaza el framework de `shared` para `iosSimulatorArm64` con libsodium, y el log queda en `evidence/ios/`. |
| iOS-A | La app abre en el simulador del CI y en el iPhone, en modo simulación, con el aviso "Simulación: nada de esto está en Stellar". Captura en `evidence/ios/simulator.md` (SIMULATED). Android y `allTests` siguen verdes. |
| iOS-B | Un `cancel` firmado en el iPhone con `KeychainSigner` detiene en testnet un `queue` hecho en el A54. Hashes de Horizon en `evidence/ios/two-devices.md` (REAL). La clave no aparece en logs. El guardián sigue sin ser firmante: se comprueba al provisionar. |

"Compila" no es terminado.

---

## 5. Si algo falla

| Falla | Qué hago |
| --- | --- |
| **Spike C rojo** | Fallback de R5, en orden: (a) auth entry a mano con las primitivas XDR del SDK; (b) ed25519 crudo; (c) la app solo lee y las firmas van por `stellar-cli` en la demo, **dicho en voz alta**. P4 y P5 no cambian, porque todo está detrás de `StellarGateway` y `Signer`. |
| **Las direcciones llegan tarde** (el Sync 2 se mueve) | Sigo con el fake. El cambio a la red real es una línea de inyección. |
| **La IA de Ant no llega** | El stub devuelve `NoClearSignals` y la UI oculta la tarjeta de análisis. La escena 2 se hace sin mensaje. |
| **Voy atrasado el día 5** | Abraham toma P6, y P8 se reduce al botón "Actualizar" en la vista del guardián. |
| **Me quedo sin tiempo en P5** | Corto en este orden: Entrada → "Reglas de esta cuenta" plegable → animación de la línea de espera. **Nunca** corto Pagar, Detalle ni Guardián: sin ellas no hay escenas 1 a 3. |
| **libsodium no enlaza en iOS** | Probar en este orden: (a) `Clibsodium.xcframework` de `jedisct1/swift-sodium` en un tag fijo, con `linkerOpts`; (b) compilar libsodium para iOS en el CI con su script `dist-build/apple-xcframework.sh`; (c) quedarse en iOS-0 rojo, anotado como tal. |
| **El CI de macOS se queda sin cuota o va lento** | Codemagic (500 min gratis al mes en M2). Si tampoco alcanza, un Mac por horas (MacinCloud o Scaleway), solo si el equipo lo aprueba porque cuesta dinero. |
| **Sideloadly falla o no hay Apple ID secundario** | Me quedo en iOS-A con el simulador del CI. Se dice "iOS: corre en simulación"; no se afirma que corra en un iPhone. |
| **`KeychainSigner` no funciona a tiempo** | El iPhone se queda en iOS-A. La demo de dos dispositivos se hace con un segundo Android o con el emulador. |
| **iOS rompe Android** | Se revierte en la rama `ios`. `main` no recibe nada que no tenga `allTests` y el APK verdes. |

---

## 6. Reglas que no rompo

- `commonMain` sin Android, iOS, LiteRT-LM ni WebAuthn.
- `GuardPayAI` nunca toca `Signer` ni `StellarGateway`, y nunca rellena el destino ni el monto.
- El guardián nunca es firmante de la cuenta.
- Ni "seguro" ni "protegido" en la UI. "Retenido" solo con registro en la cadena.
- Testnet siempre. Versiones exactas. Ninguna clave ni semilla en el repo.
- No invento hashes: lo que no corrí se anota como "no corrido".
- Ningún Apple ID, contraseña, certificado ni perfil de aprovisionamiento entra en el repo ni en los secretos del CI. El Apple ID lo escribe su dueño en Sideloadly, no un script.

---

## 7. iOS sin Mac: macOS en la nube + iPhone (revisado 6 oct)

G3 sigue siendo "nadie tiene Mac", pero hay un iPhone. La ruta es compilar en un Mac alquilado en la nube e instalar desde Windows. iOS es lo primero que se recorta si falta tiempo.

### Qué hay hoy

- `[FACT]` Las klibs de iOS compilan en Windows. Hay tres avisos: `LocalClipboardManager` está obsoleto en `Cards.kt:110`, y el nombre del parámetro `hash` no coincide con `Signer` en `OwnerSession.kt:458` y `GatewayGuardianChain.kt:28`.
- `[FACT]` En `iosMain` solo está `IosGuardPayAI.kt`. Faltan la app de Xcode, el `Signer`, el wiring y los avisos. No hace falta ningún `expect`/`actual`: `Signer`, `GuardPayAI`, `StellarGateway` y `GuardianChain` son interfaces.
- `[FACT]` La klib cinterop de libsodium del SDK (`stellar-sdk-iosSimulatorArm64Cinterop-libsodiumMain-1.14.0`) no trae la librería. Su manifiesto solo enlaza `-lsodium` para macOS (`/opt/homebrew/lib`). `[INFERENCE]` Para enlazar en iOS hay que aportar `libsodium.a` del simulador y del dispositivo.
- `[UNVERIFIED]` La página del SDK pide Gradle 9.0+; usamos 8.14.3. No se ha probado en iOS.
- El código de plataforma de Android son unas 890 líneas. Es la referencia del tamaño de lo que falta en iOS.

### La ruta, por hitos

1. **iOS-0, CI en macOS.** `.github/workflows/ios.yml` en el runner `macos-15`, con Xcode 16.x fijado por versión exacta.
   - `[UNVERIFIED]` Gratis porque `CHMP987/GuardPay` es público. Se confirma en la primera corrida.
   - `[INFERENCE]` Xcode 16.x es compatible con Kotlin 2.2.20. Si no, se fija la versión que diga la tabla de compatibilidad de Kotlin.
   - libsodium: `Clibsodium.xcframework` de `jedisct1/swift-sodium` en un tag fijo, y `linkerOpts` en el `binaries.framework` de `app/shared/build.gradle.kts`. Es el único cambio en `shared`. Ese archivo también actualiza su comentario "Declared but not compiled".
2. **iOS-A, simulación.**
   - `iosApp/` generado con XcodeGen (`project.yml`). No se guarda ningún `.xcodeproj` a mano.
   - `MainViewController` envuelve el `App()` de Compose con `ComposeUIViewController`.
   - Wiring de iOS con la simulación en memoria. `[RECOMMENDATION]` Mover `SimulatedStellarGateway` (hoy en `app/android/src/debug`) a un source set compartido entre el debug de Android y iOS. Si cuesta más de una hora, se copia en `iosMain`, marcada SIMULATED.
   - El CI arranca el simulador con `xcrun simctl`, instala, abre y guarda captura y log como artefactos.
3. **`.ipa` sin firmar.**
   - `xcodebuild … CODE_SIGNING_ALLOWED=NO`, y después la carpeta `Payload/` en zip. Queda como artefacto del CI.
   - **Lo instalo yo** con Sideloadly en Windows y un Apple ID **secundario** gratis. Sideloadly necesita el iTunes que no es de la Store.
   - En el iPhone hay que activar el Modo desarrollador (iOS 16+) y confiar en el perfil. La firma gratis dura **7 días** y admite 3 apps como máximo.
   - Los logs del iPhone se leen con `idevicesyslog` (libimobiledevice para Windows).
4. **iOS-B, firma real.**
   - `KeychainSigner` en `iosMain`. La semilla ed25519 se genera con el SDK (libsodium) y se guarda con `SecItemAdd`.
   - Control de acceso: `SecAccessControlCreateWithFlags` con `.biometryCurrentSet` y `kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly`. `NSFaceIDUsageDescription` va en el `Info.plist`.
   - `[FACT]` **Es más débil que el Keystore de Android:** Secure Enclave no soporta ed25519, así que la semilla entra en la memoria de la app para firmar. Se dice tal cual en el README.
   - Provisión sin semillas, igual que en Android. La app escribe `keys.json` con sus direcciones G, `DeviceProvisioningLiveTest` despliega la cuenta y `testnet.json` vuelve al iPhone.
   - Los archivos se mueven con la app Dispositivos de Apple en Windows, gracias a `UIFileSharingEnabled` y `LSSupportsOpeningDocumentsInPlace`.
5. **Variante de dos dispositivos.** El iPhone es el guardián y el A54 la dueña. Cubre el "dos dispositivos: no corrido" de `evidence/demo/rehearsal.md`.
   - La pantalla del guardián no necesita IA, y en el iPhone solo firma `cancel`.
   - `[FACT]` Hoy `src/debug/Wiring.kt:91` exige que **las dos** claves del teléfono coincidan con `testnet.json`. La variante necesita un modo "solo dueña" en el A54. Es un cambio pequeño en el debug de Android, y va en la rama `ios`.
   - Provisión: `-Pgp.owner` con la G del A54 y `-Pgp.guardian` con la G del iPhone. Es una **quinta cuenta**, así que hay que acordar con el equipo cuál va en la demo.
6. **Aviso al guardián en iOS.** Sondeo del RPC solo con la app en primer plano, más `UNUserNotificationCenter`. No hay sondeo en segundo plano porque no hay backend ni APNs. En la demo la app está abierta, y se dice así.

### Riesgos

| Riesgo | Probabilidad | Qué lo cubre |
| --- | --- | --- |
| libsodium no enlaza en iOS | Media | Escalones (a)–(c) de §5 |
| Versión de Gradle o Xcode incompatible con el SDK o Kotlin | Media | Fijar la versión de Xcode; probar Gradle 9 solo en la rama `ios` |
| El instalador del iPhone (Sideloadly, Apple ID o Modo desarrollador) no funciona | Media | Quedarse en iOS-A con el simulador |
| La clave en Keychain es más débil que en el Keystore | Cierta | Se documenta; el contrato y la retención no cambian |
| iOS quita tiempo a la entrega | Alta | La rama `ios` y la prioridad de §3 (días 8–11) |

**Estimación:** `[INFERENCE]` 2–3 días de trabajo. **Lo que se dice si iOS-B no llega:** "iOS: corre en simulación", y solo si iOS-A quedó verde. Si no, "iOS: compila, no se ha ejecutado".

---

## 8. Pendiente de confirmar

- [x] G1: fecha límite. Resuelto el 5 oct: 12 oct (Passport, 17:59).
- [x] G3: nadie tiene Mac. La ruta de §7 lo cubre con macOS en la nube.
- [ ] ¿Cuántos teléfonos Android hay? Tengo uno. Para la demo hace falta otro dispositivo para el guardián: un segundo Android, un emulador o el iPhone (§7, variante).
- [ ] Modelo del iPhone y versión de iOS. Hacen falta iOS 14+ (SDK) y el Modo desarrollador (iOS 16+).
- [ ] ¿Hay un Apple ID secundario para firmar con Sideloadly? No uso el principal.
- [ ] Cuenta de la demo: con la variante serían cinco cuentas en testnet. Lo decide el equipo.
