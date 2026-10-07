# Día 9: iOS-A, la app abre en el simulador de iOS en modo simulación

**Veredicto: la app de iOS (`iosApp/`, generada con XcodeGen) compila sin firmar, abre en el simulador del CI con el aviso "Simulación: nada de esto está en Stellar" y recorre las escenas 1, 2 y 3 de la demo. Los 3 tests de XCUITest pasan.** Todo es la simulación en memoria. No se envió nada a Stellar, no se firmó nada con una llave real y nada corrió en un iPhone.

| | |
| --- | --- |
| Etiqueta | **SIMULATED**: `SimulatedStellarGateway` y `SimulatedSigner` en memoria (`commonMain/simulation`). Ningún hash de estas capturas existe en Stellar. |
| Corrida | [actions/runs/37596034332](https://github.com/CHMP987/GuardPay/actions/runs/37596034332), rama `ios`, commit `1e7627f`, 2026-10-07 08:46–09:05 UTC, `success` |
| Runner | `macos-15`, Xcode 16.4 (16F6), simulador iPhone 16 con iOS 18.5 (22F77), JDK 17 |
| Proyecto | XcodeGen **2.46.0** (zip sha256 `4d9e34b62172d645eed6457cac13fc222569974098ef4ee9c3368bedf0196806`), `iosApp/project.yml`. El `.xcodeproj` no se versiona. |
| libsodium | SPM `jedisct1/swift-sodium` **0.11.0** exacta, producto `Clibsodium` (el log de build dice "Sodium … @ 0.11.0") |
| Framework de Kotlin | `embedAndSignAppleFrameworkForXcode` en un preBuildScript. Sin firma: `CODE_SIGNING_ALLOWED=NO`. |
| Workflow | `.github/workflows/ios.yml`, job `simulator` |

## Qué corre

`MainViewController()` (`app/shared/src/iosMain/.../ios/MainViewController.kt`) arma lo mismo que el modo simulación de Android: `simulatedSides(...)` y `GuardPayApp(..., simulation = true)`. El gesto de volver de iOS se mapea al mismo `handleBack` que el botón atrás de Android. La IA es el stub de iOS (`Analysis.Unavailable`).

## Pasos

| Paso | Resultado |
| --- | --- |
| `xcodegen generate` | OK |
| `xcodebuild build-for-testing` (Debug, simulador, sin firmar) | OK |
| `simctl install` + `launch`, esperar 25 s | Sigue viva: `launchctl` la lista con pid 37379. Captura: [launch.png](simulator/launch.png). `app.log`: 199 líneas, ninguna de la app; solo ruido del sistema. |
| `test1_entryShowsTheSimulationBanner` | OK (10,9 s) |
| `test2_ownerPaysAContactAndGoesBackBySwipe` | OK (51,1 s) |
| `test3_ownerHoldsAndGuardianStops` | OK (53,4 s) |

## Capturas (SIMULATED, reducidas al 50 %)

| Captura | Qué muestra |
| --- | --- |
| [01-entrada](simulator/01-entrada.png) | Entrada con el aviso de simulación |
| [02-inicio](simulator/02-inicio.png) | Inicio: 500 USDC, 3 contactos, "Te quedan 100 USDC de tu tope de hoy" |
| [03-enviado](simulator/03-enviado.png) | Escena 2: 10 USDC a Mamá, Enviado |
| [04-volver-a-entrada](simulator/04-volver-a-entrada.png) | Dos gestos de volver (desde el borde) llevan de Detalle a Entrada |
| [05-retenido](simulator/05-retenido.png) | Escena 3: 150 USDC a una cuenta nueva, Retenido hasta las 09:03, "Diego puede detenerlo" |
| [06-guardian-detalle](simulator/06-guardian-detalle.png) | Modo guardián: el mismo pago, "Detener este pago" |
| [07-detenido](simulator/07-detenido.png) | Detenido por el guardián |

## Bug real que encontró el test: crash al abrir "Revisa tu pago"

- [FACT] En iOS la app se cerraba unos 5 s después de tocar "Revisar" (corridas 37586641721 y 37588799502). La excepción, registrada en la corrida 37591382741, fue `IllegalStateException: merge function called on unmergeable property PaneTitle`, lanzada desde `AccessibilityMediator.sync` de Compose.
- [FACT] Causa: `GpSheet` ponía `paneTitle` en la hoja, y la hoja era hija del scrim `clickable`. Un `clickable` fusiona la semántica de sus descendientes, y `paneTitle` no se puede fusionar. iOS construye ese árbol en cuanto la accesibilidad está activa, y XCUITest la activa.
- [FACT] Arreglo (`bcb8030`): el scrim pasa a ser hermano de la hoja, no su padre. Sigue tragándose los toques. Tras el cambio, `allTests` local dio 341 tests y 0 fallos, y `:app:android:assembleDebug` pasó.
- [INFERENCE] Con VoiceOver activado, un usuario real de iOS habría tenido el mismo crash.
- [UNVERIFIED] Si TalkBack en Android también fallaba antes del arreglo. No se probó.

## Lo que costó el test y por qué

Compose para iOS se expone a XCUITest de una forma que obliga a tres ajustes en `iosApp/UITests/SimulationUITests.swift`:

- `GpTextField` aparece como `TextView` con la etiqueta del campo, no como `TextField`.
- Compose no responde a la acción de "scroll to visible" (`kAXErrorCannotComplete`). Por eso el test toca por coordenada y arrastra el formulario a mano si el teclado tapa el campo.
- El teclado de hardware del simulador se apaga antes de arrancarlo, para que aparezca el teclado en pantalla.

Kotlin/Native escribe las excepciones no capturadas en stderr, que el log del simulador no guarda. `MainViewController` las registra con `NSLog`, una línea por llamada y sin argumentos de formato. Pasar un `String` de Kotlin por los varargs de `NSLog` hacía crashear el propio log (corrida 37588799502).

## Historial de corridas del día 9

| Corrida | Resultado |
| --- | --- |
| 37580790907 | UI: el test no encontraba campos de texto |
| 37582645677 | UI: "Neither element nor any descendant has keyboard focus" |
| 37586641721 | UI: escribe en los campos; aparece el crash tras "Revisar" |
| 37588799502 | UI: el `NSLog` del hook crashea; no hay mensaje |
| 37591382741 | UI: mensaje de la excepción (`PaneTitle`) |
| 37593548333 | UI: 2 de 3 pasan tras el arreglo; test3 falla al tocar en modo guardián |
| **37596034332** | **UI: 3 de 3** |

En todas ellas el build y el arranque de la app pasaron. El paso de UI tenía `continue-on-error` mientras se diagnosticaba. Ahora que pasa, se quitó, para que una regresión falle el job.

## No corrido

- La app en un iPhone físico (`.ipa` sin firmar + Sideloadly): día 10.
- Firma con una llave real en iOS (`KeychainSigner`, iOS-B). Aquí firma `SimulatedSigner`.
- Nada contra Stellar testnet desde iOS.
- La IA en iOS: el stub devuelve `Analysis.Unavailable`.
- VoiceOver a mano y TalkBack en Android.
