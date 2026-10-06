# GuardPay

GuardPay es una billetera de USDC en Stellar para personas expuestas a estafas por mensaje. Una IA en el teléfono lee el mensaje que pide el pago y advierte. Si el destino no es un contacto de confianza o el monto pasa el tope diario, un contrato retiene el pago durante una espera. En esa espera, un guardián elegido por la dueña puede detenerlo, pero nunca mover el dinero.

> **La IA advierte. Tú firmas. Tu guardián puede detener. El contrato rechaza lo demás.**

Solo **Stellar Testnet**. Los contratos no están auditados. Esto es un MVP de hackathon, no un producto.

## Orden de autoridad de los documentos

Los documentos de `docs/` se contradicen. Cuando no coinciden, manda el primero de esta lista:

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
| P1 Spikes | A: GO. C: verde. | `evidence/security/spike-a.md`, `evidence/stellar/spike-c.md` |
| P2 Contratos | cerrada para el camino MUST | `evidence/policies/` |
| P3 Testnet | deploy hecho | `evidence/stellar/deployment.md`, `evidence/smart-account/rules-onchain.md` |
| P4 Núcleo KMP | hecho (Android). iOS solo compila klibs. | `evidence/architecture/kmp-layers.md`, `evidence/stellar/gateway-live.md` |
| P5 UI | 4 pantallas con datos de testnet, en **emulador** | `evidence/demo/screens/`, `evidence/smart-account/screens/` |
| P6 Firma | Keystore ed25519 con biometría o PIN; ciclo completo en testnet, en **emulador** | `evidence/smart-account/signing.md` |
| P7 IA on-device | **no iniciado** (hay un stub) | — |
| P8 Aviso al guardián | sondeo local sin backend; < 60 s medido en **emulador** | `evidence/guardian/notification.md` |
| Día 6 (P2 · Persona 2) | escenas 1, 2, 3 y 6 ensayadas dos veces contra testnet, en **emulador**; vídeo de respaldo grabado | `evidence/demo/rehearsal.md` |
| P9 Adversarial · P10 Entrega | no iniciado | — |

## Gates

- **G1:** la fecha límite es el 12 oct (Passport, 17:59). Respuesta humana del 5 oct.
- **G2:** sí. El pin `b40c5ea` de OZ compila con soroban-sdk 28 (`docs/DEPENDENCIES.md`).
- **G3:** no hay Mac. Respuesta humana del 5 oct. En Windows los klibs de iOS compilan, pero **nada se enlaza ni se ejecuta en iOS**.

## Limitaciones conocidas

- **Testnet únicamente.** El activo es un SAC de prueba, no el USDC de Circle.
- **Contratos sin auditar.** OZ está fijado a un commit pre-release (`b40c5ea`).
- **El guardián no se puede cambiar.** Tampoco hay recuperación de cuenta.
- **La clave firma un hash a ciegas.** El diálogo del sistema ("Firmar en Stellar") no muestra el destino, el monto ni la regla: la clave del Keystore (o una passkey, si se usara) firma un hash. Esto lo mitigan **la retención en cadena y la vista del guardián**, no la clave. No resuelve la ingeniería social por sí sola.
- **Aviso al guardián sin backend.** No hay servidor, FCM ni APNs. El teléfono del guardián consulta el RPC cada 30 s desde un servicio en primer plano. Si el sistema lo detiene, el aviso llega tarde: hasta que se abra la app o se toque "Actualizar". Puede detenerlo por ahorro de batería, por el fabricante, por un cierre forzado o por el tope de unas 6 h/día de `dataSync` en Android 15+. Consultar cada 30 s no es aceptable en batería para producción.
- **Demo en un teléfono.** En el build debug, las claves de la dueña y del guardián están en el mismo teléfono, bajo dos alias del Keystore. En uso real cada persona tiene el suyo. El aviso entre dos teléfonos no se ha probado.
- **Pruebas en emulador.** Lo de P5, P6 y P8 se corrió en un emulador Android (API 37), no en un teléfono físico.
- **La IA (P7) aún no existe.** Hoy hay un stub.
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
