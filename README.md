# GuardPay

GuardPay es una billetera en Stellar Testnet para personas expuestas a estafas por mensaje. El activo de la demo es un SAC de prueba emitido por la dueña. No es el USDC de Circle. Si el destino no es un contacto de confianza o el monto pasa el tope diario, el contrato retiene el pago 120 segundos. En esa espera, un guardián elegido por la dueña puede detenerlo. No puede mover el dinero y no es firmante de la cuenta. La IA, el día de esta entrega, no corre: el teléfono responde que el análisis no está disponible.

> **La IA advierte. Tú firmas. Tu guardián puede detener. El contrato rechaza lo demás.**

Solo **Stellar Testnet**. Los contratos no están auditados. Esto es un MVP de hackathon, no un producto.

## Orden de autoridad de los documentos

Los documentos de `docs/` se contradicen. Cuando no coinciden, manda el que está más arriba en esta lista:

1. `GuardPay — Plan de construcción.md`: el plan vigente (arquitectura KMP, fases P0–P10, riesgos).
2. `GuardPay — Prompts de ejecución.md`: un prompt por fase.
3. `GuardPay — Security spike y propuesta web definitiva.md` (5 oct): **manda en el diseño de contratos y en seguridad** (suite GH-01…GH-33). Su conclusión de "app web" quedó reemplazada por el plan (KMP).
4. `GuardPay — Propuesta visual.md`: **manda en diseño** (paleta, 9 estados, 4 pantallas).
5. `GuardPay — MVP Proposal.md` (4 oct): **manda en alcance y criterios de aceptación** donde lo anterior no diga otra cosa.
6. `GuardPay — Propuesta para el pitch.md`: **OBSOLETO en lo técnico.** Ya no existen el contrato `TrustedPayee` separado, la política Spending Limit de OZ ni Stellar Channels. Solo sirve su texto de pitch.
7. `GuardPay — Validación Costa Rica y Chile.md`: evidencia de mercado.

## Estado

Detalle y responsables en `docs/ESTADO-FASES.md`. Toda la evidencia está en `evidence/`, etiquetada REAL / SIMULATED / ILLUSTRATIVE.

| Fase | Estado | Evidencia |
| --- | --- | --- |
| P0 Fundación | **INCOMPLETA** (ver `docs/ESTADO-FASES.md`) | `docs/DEPENDENCIES.md` |
| P1 Spikes | A: GO. B: rojo, la IA pasa a NICE. C: verde. | `evidence/security/spike-a.md`, `evidence/ai/benchmark.md`, `evidence/stellar/spike-c.md` |
| P2 Contratos | cerrada para el camino MUST | `evidence/policies/` |
| P3 Testnet | deploy hecho | `evidence/stellar/deployment.md`, `evidence/smart-account/rules-onchain.md` |
| P4 Núcleo KMP | hecho (Android). iOS solo compila klibs. | `evidence/architecture/kmp-layers.md`, `evidence/stellar/gateway-live.md` |
| P5 UI | 4 pantallas con datos de testnet, en **emulador** | `evidence/demo/screens/`, `evidence/smart-account/screens/` |
| P6 Firma | Keystore ed25519 con biometría o PIN; ciclo completo en testnet, en **emulador** | `evidence/smart-account/signing.md` |
| P7 IA on-device | **incompleta.** No se abrió: Spike B está rojo. Android e iOS devuelven análisis no disponible | `evidence/ai/benchmark.md`, `evidence/ai/prompt-injection.md` |
| P8 Aviso al guardián | sondeo local sin backend; < 60 s medido en **emulador** | `evidence/guardian/notification.md` |
| Día 6 (P2 · Persona 2) | escenas 1, 2, 3 y 6 ensayadas dos veces contra testnet, en **emulador**; vídeo de respaldo grabado | `evidence/demo/rehearsal.md` |
| P9 Adversarial | ataques corridos el 6 oct en una cuenta distinta, porque las semillas publicadas no estaban en el clon | `evidence/security/bypass-matrix.md` |
| P10 Entrega | README, escenas y texto de entrega escritos. El cronómetro de las seis escenas seguidas no se midió. CP-3, CP-7 y CP-9 los firma un humano | `docs/SUBMISSION.md`, `evidence/demo/` |

## Gates

- **G1:** la fecha límite es el 12 oct (Passport, 17:59). Respuesta humana del 5 oct.
- **G2:** sí. El pin `b40c5ea` de OZ compila con soroban-sdk 28 (`docs/DEPENDENCIES.md`).
- **G3:** no hay Mac. Respuesta humana del 5 oct. En Windows los klibs de iOS compilan, pero **nada se enlaza ni se ejecuta en iOS**.

## Limitaciones conocidas

- **Solo testnet.** El activo es un SAC de prueba, no el USDC de Circle.
- **Contratos sin auditar.** OZ está fijado a un commit pre-release (`b40c5ea`).
- **El guardián no se puede cambiar.** Tampoco hay recuperación de cuenta.
- **La clave firma un hash a ciegas.** El diálogo del sistema ("Firmar en Stellar") no muestra el destino, el monto ni la regla: la clave del Keystore (o una passkey, si se usara) firma un hash. Esto lo mitigan **la retención en cadena y la vista del guardián**, no la clave. No resuelve la ingeniería social por sí sola.
- **Aviso al guardián sin backend.** No hay servidor, FCM ni APNs. El teléfono del guardián consulta el RPC cada 30 s desde un servicio en primer plano. Si el sistema lo detiene, el aviso llega tarde: hasta que se abra la app o se toque "Actualizar". Puede detenerlo por ahorro de batería, por el fabricante, por un cierre forzado o por el tope de unas 6 h/día de `dataSync` en Android 15+. Consultar cada 30 s no es aceptable en batería para producción.
- **Demo en un teléfono.** En el build debug, las claves de la dueña y del guardián están en el mismo teléfono, bajo dos alias del Keystore. En uso real cada persona tiene el suyo. El aviso entre dos teléfonos no se ha probado.
- **Pruebas en emulador.** Lo de P5, P6 y P8 se corrió en un emulador Android (API 37), no en un teléfono físico.
- **La IA quedó en NICE.** No hay números de teléfono, ni modelo, ni `spikes/ai-bench/`. P7 no se abrió. La retención no consulta a la IA.
- **iOS no se enlazó ni se corrió.** No hay Mac. Los klibs pueden compilar en Windows; eso no es una app de iOS.
- **La retención es la constante de 120 s.** No hay vencimiento a los 600 s. Un hold maduro no caduca dentro del contrato.
- **El wasm de los ataques de P9 no es el wasm publicado.** La cuenta publicada sigue en `evidence/stellar/deployment.md`. Los hashes firmados de P9 están en `evidence/security/bypass-matrix.md`.
- **Si el guardián no mira, nadie detiene nada.** El aviso puede retrasarse si la app está cerrada.
- **Los pagos inmediatos no se releen.** Un pago a un contacto queda en cadena, pero desaparece de "Recientes" si se reinicia la app. Las retenciones sí se vuelven a leer del registro.

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

## Pines

- OpenZeppelin `stellar-contracts` commit `b40c5eaefe6a29f0030f00bd2d730b7a91cce330`, pre-release, vendorizado en `vendor/`. No es el tag estable v0.7.2.
- `soroban-sdk` 28.0.0.
- Target de los contratos: `wasm32v1-none`.
- `stellar-cli` 28.1.0.

## Tests y redespliegue

Contratos, en la raíz:

```bash
cargo test -p account -p guardian_hold -p hold_registry
```

El cliente:

```bash
./gradlew :app:shared:allTests
```

Redespliegue en testnet: `scripts/deploy-testnet.ps1` (Windows) o `scripts/deploy-testnet.sh`. Solo testnet. Las semillas quedan en `scripts/.testnet/`, que git ignora. No se imprimen.

Ataques, un script por caso, en `scripts/attacks/`. En Windows, con `stellar.exe` en `%USERPROFILE%\.local\bin`:

```powershell
./scripts/attacks/held-after-cancel.ps1
```

En esta máquina Linux el mismo caso es `bash scripts/attacks/run.sh held-after-cancel`. Hace falta un `state.json` de un deploy previo. Sin semillas, el script no firma.

## Qué se demuestra y qué no

Se demuestra, con hash o con lectura de cadena, lo que citan estos cinco puntos. Lo que no tiene archivo aquí no se afirma.

1. La cuenta publicada tiene una sola regla Default, la dueña es la firmante y la política es `guardian_hold`. El guardián no es firmante. Evidencia: `evidence/smart-account/rules-onchain.md`.
2. Un pago a un contacto dentro del tope se incluye, y un pago a un destino que no es contacto queda en el registro. Evidencia: `evidence/payments/lane-immediate.md`, `evidence/payments/lane-held.md`.
3. El guardián puede cancelar un hold y, en el intento medido, no mueve el SAC. Evidencia: `evidence/guardian/cancel.md`, `evidence/demo/scene-5.md`.
4. Un hold ya detenido no sale con una firma de la dueña. Evidencia: `evidence/demo/scene-4.md`.
5. No hay servidor de pagos. El aviso es local. Evidencia: `evidence/architecture/cero-backend.md`, `evidence/guardian/notification.md`.

No se demuestra: auditoría, mainnet, iOS en un dispositivo, la IA en el teléfono, que el aviso llegue con la app cerrada, que el guardián se pueda cambiar, ni un vencimiento a los 600 s. GH-29, GH-30 y GH-33 están no corrido. CP-3 no está firmado.

Las entrevistas (T-103) están no corrido: `evidence/demo/interviews.md`. No hay citas inventadas.

