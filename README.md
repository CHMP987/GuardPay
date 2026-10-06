# GuardPay

GuardPay es una billetera en Stellar Testnet. Un pago a un contacto de confianza, dentro del tope del día, sale en el momento. El resto queda retenido unos 120 segundos. En esa espera, un guardián elegido por la dueña puede detenerlo. No puede mover el dinero y no es firmante de la cuenta. La regla la aplica el contrato.

> La IA advierte. Tú firmas. Tu guardián puede detener. El contrato rechaza lo demás.

El activo de la demo es un SAC de prueba emitido por la dueña. No es el USDC de Circle. Solo Testnet. Los contratos no están auditados.

## Qué hay

Tres contratos Soroban y una app Kotlin Multiplatform. No hay servidor, base de datos, push ni patrocinador de comisiones. El teléfono del guardián se entera leyendo el RPC y muestra un aviso local. Así no existe un backend que pueda firmar por la dueña.

| Pieza | Qué hace |
| --- | --- |
| `account` | Una regla `Default`. Firmante: la dueña, `Delegated`. Política: `guardian_hold`. Sin `execute`, sin `upgrade` y sin funciones de administración. |
| `guardian_hold` | `install` una vez, y `enforce`. Lista blanca: `transfer` del SAC de prueba a un contacto bajo el tope, o `transfer` que coincide con un hold maduro, o `queue`, o nada. |
| `hold_registry` | `queue`, `cancel` del guardián, `mark_executed` solo si lo llama la política. `ready_at` lo calcula el registro: timestamp + 120 s. No mueve tokens. |
| App | Compose Multiplatform. Cuatro pantallas. Android firma con el Keystore. iOS no se enlaza ni se corre: no hay Mac. |

OpenZeppelin `stellar-contracts` está fijado al commit `b40c5eaefe6a29f0030f00bd2d730b7a91cce330`, un pre-release. No es v0.7.2. soroban-sdk 28.0.0. Target `wasm32v1-none`. stellar-cli 28.1.0.

Se podría construir en EVM con Safe, un Delay modifier y un guard. En Stellar se construye con menos código propio, porque la cuenta y el verificador salen de OpenZeppelin.

Patrones que ya existían y se nombran a propósito: Yandex Pay, OpenZeppelin TimelockController, Argent/Ready y Monzo.

## Orden de los documentos

Cuando `docs/` no coincide, manda el de más arriba en esta lista:

1. `GuardPay — Plan de construcción.md`
2. `GuardPay — Prompts de ejecución.md`
3. `GuardPay — Security spike y propuesta web definitiva.md` (contratos y GH-01…GH-33). Su conclusión de app web quedó reemplazada por este plan.
4. `GuardPay — Propuesta visual.md`
5. `GuardPay — MVP Proposal.md`
6. `GuardPay — Propuesta para el pitch.md`. El texto se puede usar. La técnica de ese archivo está obsoleta: no hay un contrato TrustedPayee aparte, ni Spending Limit de OpenZeppelin, ni Channels.
7. `docs/ESTADO-FASES.md` y `evidence/`. Si un prompt viejo contradice la evidencia, gana la evidencia.

## Estado

Detalle en `docs/ESTADO-FASES.md`. Datos en `evidence/`, con etiqueta REAL, SIMULATED o ILLUSTRATIVE.

| Fase | Estado | Dónde |
| --- | --- | --- |
| P0 | **INCOMPLETA** | `docs/ESTADO-FASES.md` |
| P1 Spike A | cerrada | `evidence/security/spike-a.md` |
| P1 Spike B | **rojo. La IA queda en NICE.** Sin números de un teléfono. | `evidence/ai/benchmark.md` |
| P1 Spike C | verde | `evidence/stellar/spike-c.md` |
| P2 | cerrada en el camino MUST. T-049 no autorizado. | `evidence/policies/` |
| P3 | deploy hecho. CP-3 sin firmar. | `evidence/stellar/deployment.md` |
| P4–P6, P8 | hechos en Android, en emulador | `evidence/architecture/`, `evidence/smart-account/`, `evidence/guardian/` |
| P7 | **no abierto.** El stub sigue en `Analysis.Unavailable`. | `evidence/ai/benchmark.md` |
| P9 | scripts y matriz. Hay rechazos con hash y huecos `no corrido`. | `evidence/security/bypass-matrix.md` |
| P10 | README, escenas y texto de entrega. Passport no se envió. | `docs/SUBMISSION.md` |
| Entrevistas | **no corrido** | `evidence/interviews.md` |

CP-3, CP-7 y CP-9 no están firmados.

## Qué se demuestra y qué no

Se demuestra, con hash o con lectura del ledger:

- Una sola regla, la política es `guardian_hold`, el guardián no es firmante (`evidence/security/chain-reread.md`).
- Un pago a un contacto dentro del tope sale. Un pago detenido no sale. El guardián no gasta con el payload a su nombre (`evidence/security/bypass-matrix.md`).
- `execute`, `upgrade` y una segunda regla no están en el wasm: la CLI responde que el subcomando no existe. Hash: no hay tx.
- La retención de la demo es la constante de 120 s.

No se demuestra:

- Mainnet, ni una auditoría.
- Que el guardián se pueda cambiar. No se puede.
- Que alguien detenga el pago si el guardián no mira.
- Que el aviso llegue si la app está cerrada o el sistema mata el servicio. Puede retrasarse.
- iOS. No se enlazó ni se corrió.
- La IA. Hoy no hay modelo. La escena 2 se hizo sin pegar un mensaje.
- GH-28, GH-29, GH-30 y GH-33. Quedan no corrido. No hay vencimiento a los 600 s ni firmante passkey.
- GH-20, GH-21, GH-20b y el contrato intermedio, en testnet, como hash propio. El nativo está verde. El hash de ledger de esos casos exactos es no corrido.
- Las seis escenas seguidas, en una sola sentada y por debajo de 5 minutos, no se cronometraron así. Las escenas de la app se midieron dos veces. Las de terminal son hashes de la cuenta CLI, no de la cuenta del emulador.

## Dos cuentas

| | CLI (`deployment.md`) | Emulador (`rehearsal.md`) |
| --- | --- | --- |
| Cuenta | `CDBJMSUIWL3RCA4POESJQSPHUJ4KOMQE7OTFAO27DDAFXKDBXRNTDEZX` | `CDWPPTDMACOEVMBUHBBTPUW2EEDXY6YVUAOZJFTSGGOBAONCAJ7WGFV6` |
| SAC de prueba | `CBUSK46YH5J4CKO6O46UU7QK4O23MTLBYI4WOGVBLDQDRG7OL56US77R` | `CC2K3NDVHI52YKCDORJVNIZYRYNO2TE6MN3LIK2LWDRIZMJDBEG3VDBH` |
| Nombres | contactos generados | Mamá y Diego |

## Limitaciones

- Testnet. SAC de prueba, no el USDC de Circle.
- Sin auditar. El pin de OpenZeppelin es un pre-release.
- El guardián no se cambia. No hay recuperación de cuenta.
- La clave firma un hash. El diálogo del sistema no muestra destino ni monto. La retención y la vista del guardián son lo que mitiga eso.
- Sin backend. El aviso es un sondeo local cada 30 s. Si el sistema lo detiene, el aviso llega cuando se abre la app o se toca actualizar.
- La dueña y el guardián de la demo están en el mismo emulador.
- Los pagos inmediatos desaparecen de Recientes al reiniciar la app. Siguen en la cadena. Las retenciones se vuelven a leer.
- El admin del SAC de prueba es la dueña. `mint` y `clawback` no pasan por la política. No se ejecutaron con esa clave. El saldo actual no aceptó `clawback` en simulación. Ver `evidence/security/attacker-session.md`.

## Construir y probar

JDK 17. Android SDK en `local.properties` (`sdk.dir` con barras normales). Gradle por el wrapper.

```bash
./gradlew :app:shared:allTests
```

```bash
cargo test -p account -p guardian_hold -p hold_registry
```

```bash
./gradlew :app:android:assembleDebug
```

Los tests que pegan a testnet piden `-PliveTestnet` y no entran en `allTests`. iOS no se enlaza en esta máquina.

Ataques, delante de un jurado, con stellar-cli 28.1.0. En Windows el ejecutable esperado es `%USERPROFILE%\.local\bin\stellar.exe`. En bash:

```bash
bash scripts/attacks/run-all.sh
```

Cada ataque también tiene un `.ps1` al lado. Si existe `scripts/.testnet/state.json`, los que necesitan la firma de la dueña llaman a `scripts/sign-delegated.mjs`. Si no existe, citan el hash ya publicado o dicen no corrido. No imprimen semillas.

Redesplegar en testnet: `scripts/deploy-testnet.ps1` o `scripts/deploy-testnet.sh`. La segunda corrida, si ya hay estado, no despliega contratos nuevos. Las semillas quedan en `scripts/.testnet/`, ignorado por git.

### Conectar el teléfono al build debug

1. Instala el APK de debug en un Android 13+ con bloqueo de pantalla y ábrelo una vez. La app escribe las direcciones públicas en `/sdcard/Android/data/com.guardpay.android/files/keys.json`.
2. Despliega una cuenta para esas direcciones:

```bash
./gradlew :app:shared:testDebugUnitTest -PliveTestnet -Pgp.owner=G... -Pgp.guardian=G... --tests "*DeviceProvisioningLiveTest"
```

3. Sube el resultado:

```bash
adb push app/shared/build/testnet.json /sdcard/Android/data/com.guardpay.android/files/testnet.json
```

Sin `testnet.json`, el debug corre una simulación en memoria y lo dice. El build de release no se conecta a nada.

## Cinco afirmaciones y su evidencia

1. Hay una sola regla y la política es `guardian_hold`. `evidence/security/chain-reread.md`.
2. Un pago detenido no sale. `evidence/demo/scene-4.md`, hash `2175e5124b9aeafef0768fb90d8fd333dcbdcb9ac9d3c4a24f1e471241eca92f`.
3. El guardián de la cuenta CLI no gastó al firmar un `transfer` hacia sí mismo. `evidence/demo/scene-5.md`, hash `ef32c3bdd5fc3874858deaa01aff4afe223314a7847bdb73bc3bf894716b10de`.
4. `ready_at` lo calcula el registro y la espera de ese ensayo fue 120 s. `evidence/payments/lane-held.md`.
5. No hay modelo en el teléfono y el Spike B no tiene números. `evidence/ai/benchmark.md`.
