# GuardPay

GuardPay es una billetera en Stellar Testnet para personas expuestas a estafas por mensaje. El activo de la demo es un SAC de prueba emitido por la dueña. No es el USDC de Circle. Si el destino no es un contacto de confianza o el monto pasa el tope diario, el contrato retiene el pago 120 segundos. En esa espera, un guardián elegido por la dueña puede detenerlo. No puede mover el dinero y no es firmante de la cuenta. La IA, el día de esta entrega, no corre: el teléfono responde que el análisis no está disponible.

> **La IA advierte. Tú firmas. Tu guardián puede detener. El contrato rechaza lo demás.**

Solo **Stellar Testnet**. Los contratos no están auditados. Esto es un MVP de hackathon, no un producto.

Se podría construir en EVM con Safe, un Delay modifier y un guard. En Stellar se construye con menos código propio, porque la cuenta y el verificador salen de OpenZeppelin.

Patrones que ya existían y se nombran a propósito: Yandex Pay, OpenZeppelin TimelockController, Argent/Ready y Monzo.

## Orden de autoridad de los documentos

Los documentos de `docs/` se contradicen. Cuando no coinciden, manda el que está más arriba en esta lista. Si un prompt viejo contradice el ledger, gana el ledger.

1. `GuardPay — Plan de construcción.md`: el plan vigente (arquitectura KMP, fases P0–P10, riesgos).
2. `GuardPay — Prompts de ejecución.md`: un prompt por fase.
3. `GuardPay — Security spike y propuesta web definitiva.md` (5 oct): **manda en el diseño de contratos y en seguridad** (suite GH-01…GH-33). Su conclusión de "app web" quedó reemplazada por el plan (KMP).
4. `GuardPay — Propuesta visual.md`: **manda en diseño** (paleta, 9 estados, 4 pantallas).
5. `GuardPay — MVP Proposal.md` (4 oct): **manda en alcance y criterios de aceptación** donde lo anterior no diga otra cosa.
6. `GuardPay — Propuesta para el pitch.md`: **OBSOLETO en lo técnico.** Ya no existen el contrato `TrustedPayee` separado, la política Spending Limit de OZ ni Stellar Channels. Solo sirve su texto de pitch.
7. `GuardPay — Validación Costa Rica y Chile.md`: evidencia de mercado.

## Estado

Detalle y responsables en `docs/ESTADO-FASES.md`. Toda la evidencia está en `evidence/`, etiquetada REAL / SIMULATED / ILLUSTRATIVE. Lo no corrido sigue escrito no corrido.

| Fase | Estado | Evidencia |
| --- | --- | --- |
| P0 Fundación | **INCOMPLETA** | `docs/ESTADO-FASES.md` |
| P1 Spike A | cerrada, GO | `evidence/security/spike-a.md` |
| P1 Spike B | **rojo. La IA queda en NICE.** Sin números de un teléfono | `evidence/ai/benchmark.md` |
| P1 Spike C | verde | `evidence/stellar/spike-c.md` |
| P2 Contratos | cerrada para el camino MUST. T-049 no autorizado | `evidence/policies/` |
| P3 Testnet | deploy hecho. CP-3 sin firmar | `evidence/stellar/deployment.md`, `evidence/smart-account/rules-onchain.md` |
| P4 Núcleo KMP | hecho (Android). iOS: enlaza y abre en el simulador del CI, solo en simulación (`evidence/ios/simulator.md`) | `evidence/architecture/kmp-layers.md`, `evidence/stellar/gateway-live.md` |
| P5 UI | 4 pantallas con datos de testnet, en **emulador** y en el Galaxy A54 (7 oct UTC, `evidence/demo/physical-phone.md`) | `evidence/demo/screens/`, `evidence/smart-account/screens/` |
| P6 Firma | Keystore ed25519 con biometría o PIN; ciclo completo en testnet, en **emulador**. En el A54, `transfer`, `queue` y `cancel` firmados con huella | `evidence/smart-account/signing.md` |
| P7 IA on-device | **no abierto.** Spike B está rojo. Android e iOS devuelven análisis no disponible. CP-7 sin firmar | `evidence/ai/benchmark.md`, `evidence/ai/prompt-injection.md` |
| P8 Aviso al guardián | sondeo local sin backend; < 60 s medido en **emulador** y en el A54, con dueña y guardián en el mismo teléfono | `evidence/guardian/notification.md` |
| Día 6 | escenas 1, 2, 3 y 6 de la app, dos veces, en **emulador** | `evidence/demo/rehearsal.md` |
| P9 Adversarial | matriz de tres cuentas. Verde solo donde esa cuenta y ese wasm tienen hash. CP-9 sin firmar | `evidence/security/bypass-matrix.md` |
| P10 Entrega | README, escenas y texto escritos. Passport no enviado | `docs/SUBMISSION.md`, `evidence/demo/` |
| Entrevistas | **no corrido** | `evidence/interviews.md` |

CP-3, CP-7 y CP-9 no están firmados. No los firma esta nota.

## Tres cuentas, dos wasm

Un hash pertenece a la cuenta de su columna. El wasm va con la cuenta.

| | Publicada (`deployment.md`) | Ataques | Emulador (`rehearsal.md`) |
| --- | --- | --- | --- |
| account | `CDBJMSUIWL3RCA4POESJQSPHUJ4KOMQE7OTFAO27DDAFXKDBXRNTDEZX` | `CC6FFXGGF62OPCRCL4FQLABPUMEGZ6JQ7XVETDJ5MOLWQGNA4SQYPHFP` | `CDWPPTDMACOEVMBUHBBTPUW2EEDXY6YVUAOZJFTSGGOBAONCAJ7WGFV6` |
| wasm de `account` | `986956ccfd0f134bbbd56dfdcfca1def12f4bf61676ef0efd762c2eafd37cf3b` | `06226b3773107e33572671cd458ed34d4f00bf76f9b6dbc3bf6e84f60f05ea9c` (sin optimizar) | `986956ccfd0f134bbbd56dfdcfca1def12f4bf61676ef0efd762c2eafd37cf3b` |
| SAC de prueba | `CBUSK46YH5J4CKO6O46UU7QK4O23MTLBYI4WOGVBLDQDRG7OL56US77R` | `CAYYSVXHPQWELMONBJCFYKQORJS5IJGTEW7VBJGB7W5OV7QMNRLKSXOX` | `CC2K3NDVHI52YKCDORJVNIZYRYNO2TE6MN3LIK2LWDRIZMJDBEG3VDBH` |
| Nombres | contactos generados | deploy de la noche del 6 oct | Mamá y Diego |

Los ataques con hash propio de destino distinto, monto distinto, muxed, contrato intermedio y GH-28 están solo en `CC6FFXGG…` y en el wasm `06226b37…`. En la cuenta publicada y en el emulador esas filas siguen no corrido. GH-29, GH-30 y GH-33 están no corrido en las tres. La matriz es `evidence/security/bypass-matrix.md`.

La cuenta del emulador no se usó para firmar ataques: la semilla del Keystore no está en el repo.

## Gates

- **G1:** la fecha límite es el 12 oct (Passport, 17:59). Respuesta humana del 5 oct. La inscripción y los campos del formulario siguen sin contestar. Passport no se envió.
- **G2:** sí. El pin `b40c5ea` de OZ compila con soroban-sdk 28 (`docs/DEPENDENCIES.md`).
- **G3:** no hay Mac. Respuesta humana del 5 oct. En Windows los klibs de iOS compilan, pero nada se enlaza ni se ejecuta en iOS. Desde el 7 oct, un runner de macOS del CI enlaza `shared` y abre la app en el simulador, solo en simulación (`evidence/ios/`).

## Limitaciones conocidas

- **Solo testnet.** El activo es un SAC de prueba, no el USDC de Circle. El admin de ese SAC es la dueña. `mint` y `clawback` no pasan por la política y no se ejecutaron con esa clave.
- **Contratos sin auditar.** OZ está fijado al commit pre-release `b40c5eaefe6a29f0030f00bd2d730b7a91cce330`. No es el tag estable v0.7.2.
- **El guardián no se puede cambiar.** Tampoco hay recuperación de cuenta.
- **La clave firma un hash a ciegas.** El diálogo del sistema ("Firmar en Stellar") no muestra el destino, el monto ni la regla: la clave del Keystore firma un hash. Esto lo mitigan la retención en cadena y la vista del guardián, no la clave.
- **Aviso al guardián sin backend.** No hay servidor, FCM ni APNs. El teléfono del guardián consulta el RPC cada 30 s desde un servicio en primer plano. Si el sistema lo detiene, el aviso llega tarde: hasta que se abra la app o se toque "Actualizar". Puede detenerlo por ahorro de batería, por el fabricante, por un cierre forzado o por el tope de unas 6 h/día de `dataSync` en Android 15+. Consultar cada 30 s no es aceptable en batería para producción.
- **Demo en un teléfono.** En el build debug, las claves de la dueña y del guardián están en el mismo teléfono, bajo dos alias del Keystore. En uso real cada persona tiene el suyo. El aviso entre dos teléfonos no se ha probado.
- **Pruebas en emulador y en un solo teléfono.** Lo de P5, P6 y P8 se corrió en un emulador Android (API 37). En el teléfono físico (Galaxy A54) corrieron después las escenas 1, 2, 3 y 6 (`evidence/demo/physical-phone.md`). El ciclo encolar → esperar → enviar y GH-01/GH-24 no se repitieron en el teléfono.
- **La IA quedó en NICE.** No hay números de teléfono, ni modelo, ni `spikes/ai-bench/`. P7 no se abrió. La retención no consulta a la IA.
- **iOS solo corre en simulación.** No hay Mac: el CI de macOS abre la app en el simulador con la simulación en memoria (`evidence/ios/simulator.md`). Nada firmado con una clave real, nada contra testnet y nada en un iPhone.
- **La retención es la constante de 120 s.** No hay vencimiento a los 600 s. Un hold maduro no caduca dentro del contrato.
- **Si el guardián no mira, nadie detiene nada.**
- **Los pagos inmediatos no se releen.** Un pago a un contacto queda en cadena, pero desaparece de "Recientes" si se reinicia la app. Las retenciones sí se vuelven a leer del registro.
- **Las seis escenas seguidas, en una sola sentada y por debajo de 5 minutos, no se cronometraron.** Las de la app se midieron dos veces, aparte. Las de terminal se corrieron después, en otra cuenta.

## Construir y probar

JDK 17, el Android SDK en `local.properties` (`sdk.dir=C:/Users/<tú>/AppData/Local/Android/Sdk`) y Gradle por el wrapper.

```bash
./gradlew :app:shared:allTests
```

```bash
./gradlew :app:android:assembleDebug
```

Tests instrumentados (Keystore; necesitan Android 13+ con bloqueo de pantalla):

```bash
./gradlew :app:android:installDebugAndroidTest
```

```bash
adb shell am instrument -w -r com.guardpay.android.test/androidx.test.runner.AndroidJUnitRunner
```

Los contratos están en `contracts/`, con `cargo test`. Más comandos en `CLAUDE.md`.

```bash
cargo test -p account -p guardian_hold -p hold_registry
```

### Conectar el teléfono a testnet (build debug)

1. Instala el APK de debug en un Android 13+ con bloqueo de pantalla y ábrelo una vez. La app crea las dos claves en el Keystore. Después escribe sus direcciones G (públicas) en `/sdcard/Android/data/com.guardpay.android/files/keys.json`.
2. Lee ese archivo (`adb shell cat …/keys.json`) y despliega una cuenta para esas claves:
   ```bash
   ./gradlew :app:shared:testDebugUnitTest -PliveTestnet -Pgp.owner=G... -Pgp.guardian=G... --tests "*DeviceProvisioningLiveTest"
   ```
3. Sube el resultado y reabre la app:
   ```bash
   adb push app/shared/build/testnet.json /sdcard/Android/data/com.guardpay.android/files/testnet.json
   ```

Ningún paso usa una semilla: solo entran y salen direcciones públicas. Sin `testnet.json`, la app debug corre una simulación en memoria bajo el aviso "Simulación". El build de release no se conecta a nada todavía.

Redespliegue en testnet: `scripts/deploy-testnet.ps1` (Windows) o `scripts/deploy-testnet.sh`. Solo testnet. Las semillas quedan en `scripts/.testnet/`, que git ignora. No se imprimen. Esta nota no redesplegó la cuenta publicada ni la del emulador.

## Pines

- OpenZeppelin `stellar-contracts` commit `b40c5eaefe6a29f0030f00bd2d730b7a91cce330`, pre-release, vendorizado en `vendor/`. No es el tag estable v0.7.2.
- `soroban-sdk` 28.0.0.
- Target de los contratos: `wasm32v1-none`.
- `stellar-cli` 28.1.0.

## Ataques

Un script por caso, en `scripts/attacks/`.

Los `.ps1`, `run.mjs` e `invoke.mjs` leen `scripts/.testnet/state.json`. Con el estado del 6 oct por la noche enviaron las transacciones de `CC6FFXGG…`, wasm `06226b37…`. En Linux:

```bash
bash scripts/attacks/run.sh held-after-cancel
```

Los `.sh` de `common.sh` y `run-all.sh` se corrieron contra la cuenta publicada `CDBJMSUI…`, wasm `986956cc…`. Sin `state.json` citan el hash de la mañana o imprimen `no hay tx`. El log es `evidence/security/p9-cli-run.log`.

```bash
bash scripts/attacks/run-all.sh
```

`same-ledger.ps1` escribió una fila "mismo ledger" a partir de dos lecturas vacías. Esa fila no se usa. GH-29 sigue no corrido.

No imprimen semillas.

## Qué se demuestra y qué no

Se demuestra, con hash o con lectura de cadena, lo que citan estos puntos. Lo que no tiene archivo aquí no se afirma. La matriz cubre las filas que tienen hash.

1. La cuenta publicada tiene una sola regla Default, la dueña es la firmante y la política es `guardian_hold`. El guardián no es firmante. Evidencia: `evidence/smart-account/rules-onchain.md` y `evidence/security/chain-reread.md`. La cuenta del emulador, leída aparte, también tiene una regla y el mismo wasm `986956cc…`.
2. Un pago a un contacto dentro del tope se incluye, y un pago a un destino que no es contacto queda en el registro, en la cuenta del emulador. Evidencia: `evidence/payments/lane-immediate.md`, `evidence/payments/lane-held.md`, `evidence/demo/rehearsal.md`.
3. El guardián puede cancelar un hold. En el intento medido de la escena 5, en `CC6FFXGG…` y wasm `06226b37…`, no mueve el SAC. Evidencia: `evidence/guardian/cancel.md`, `evidence/demo/scene-5.md`. El caso de la mañana, en la cuenta publicada, es otro hash.
4. Un hold ya detenido no sale con una firma de la dueña, en la escena 4, cuenta `CC6FFXGG…`, wasm `06226b37…`. Evidencia: `evidence/demo/scene-4.md`. El GH-08 de la mañana es la cuenta publicada.
5. No hay servidor de pagos. El aviso es local. Evidencia: `evidence/architecture/cero-backend.md`, `evidence/guardian/notification.md`.

No se demuestra: auditoría, mainnet, iOS en un dispositivo, la IA en el teléfono, que el aviso llegue con la app cerrada, que el guardián se pueda cambiar, ni un vencimiento a los 600 s. GH-29, GH-30 y GH-33 están no corrido. GH-28 está verde solo para `CC6FFXGG…` y el wasm `06226b37…`. CP-3 no está firmado.

Las entrevistas están no corrido: `evidence/interviews.md`. No hay citas inventadas.
