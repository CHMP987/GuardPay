# Estado de fases

5 oct 2026, noche. Respuestas humanas de esta fecha. Lo no contestado queda sin contestar.

## Gates

| Gate | Respuesta | Fuente |
| --- | --- | --- |
| G1 | `12 oct` (Passport, 17:59) | Humano, 5 oct 2026. La entrega no es por Luma (5 oct 16:00). |
| Inscripción en Passport y campos del formulario | sin contestar | Siguen abiertos dentro de T-001. No inventados. |
| G2 | sí | `[FACT]` 5 oct 2026. `stellar contract build` de `stellar-accounts` @ `b40c5ea` con soroban-sdk 28.0.0, target `wasm32v1-none`, stellar-cli 28.1.0. Exit 0. Detalle en `docs/DEPENDENCIES.md`. |
| G3 | no hay Mac | Humano (Persona 2), 5 oct 2026: nadie del equipo tiene Mac. Los klibs de iOS compilan en Windows; nada se enlaza ni corre en iOS. |

## CP-0

Veredicto humano: **GO** a P1. Registrado el 5 oct 2026 a pedido del humano.

P0 sigue **INCOMPLETA**. (6 oct: `README.md` ya existe; las demás tareas de P0 no se revisaron en esta nota.) T-010, T-011, T-012, T-013, T-014 y T-018 no están hechas.

T-016 está hecha. G2 ya no bloquea T-019. T-019 hecha (A0 digest OZ pasa; ver `evidence/security/spike-a.md`). T-020 hecha (A1 R6 veredicto **pasa**; ver misma evidencia).

## CP-1

Veredicto humano: **GO** a contratos de producción. Registrado el 6 oct 2026 a pedido del humano («firma»).

Base: `evidence/security/spike-a.md` (A0, A1 y GH-01, 04, 05, 06, 09, 13, 20, 21, 22, 23, 24, 25, 26 en pasa) y firmas en `docs/INTERFACES.md`. 7.A no está autorizado.

T-034 está hecha. El spike no se copió a `contracts/`.

## CP-2

Veredicto humano: **GO** a testnet. Registrado el 6 oct 2026. El humano dijo «haz lo que falta», y el primer paso de esa lista era esta firma. Base: `evidence/policies/tests.md` y `evidence/policies/whitelist-review.md`. T-049 no entra.

## T-005 · clave del guardián en la demo

Procedimiento escrito el 6 oct 2026 para poder desplegar. Las semillas de prueba viven en `scripts/.testnet/`, ignorado por git. En la demo, la semilla del guardián de ese directorio se importa en el segundo teléfono. El guardián no es firmante de la cuenta. La dueña de la demo sale del mismo directorio; el keystore del teléfono es de una fase posterior.

## Fases

| Fase | Estado |
| --- | --- |
| P0 | INCOMPLETA |
| P1 Spike A | cerrada. Evidencia GO y CP-1 GO. |
| P2 | cerrada para el camino MUST. CP-2 GO. T-049 pendiente. |
| P3 | deploy en testnet hecho. CP-3 pendiente del humano. |
| P4 | hecho para Android (6 oct). `evidence/architecture/kmp-layers.md`, `evidence/stellar/gateway-live.md` |
| P5 | 4 pantallas con datos de testnet, probadas en emulador (6 oct) |
| P6 | Keystore + biometría o PIN; ciclo completo en testnet desde la app, en emulador. GH-01/GH-24 con el código del cliente, no desde el teléfono. `evidence/smart-account/signing.md` |
| P7 | no abierto. Spike B rojo, sin números de un teléfono. La IA queda en NICE. CP-7 sin firmar. |
| P8 | aviso local sin backend, < 60 s medido en emulador. `evidence/guardian/notification.md` |
| P9 | matriz de tres cuentas en `evidence/security/bypass-matrix.md`. GH-29, GH-30 y GH-33: no corrido. GH-28 verde solo en la cuenta de ataques y su wasm. CP-9 sin firmar. |
| P10 | README, escenas y `docs/SUBMISSION.md`. Passport no enviado. Entrevistas: no corrido. Las seis escenas seguidas no se cronometraron. |

Definition of Done: implementación + unit + integración + seguridad + criterios de aceptación + evidencia. Compilar no cierra una fase.

## P2 — contratos

6 oct 2026. Tres contratos en `contracts/`: una regla Default, política con lista blanca, registro que calcula `ready_at`. Tests nativos en verde: 35 en `guardian_hold` `tests/must.rs` más el smoke de `account`. Evidencia: `evidence/policies/tests.md` y `evidence/policies/whitelist-review.md`.

CP-2 GO el 6 oct 2026. `docs/INTERFACES.md` describe la forma de producción: hasta 3 contactos y `daily_cap`.

## P3 — testnet

6 oct 2026. `[FACT]` Los tres wasm están en testnet. Direcciones, hashes de deploy y los 10 ensayos ★ están en `evidence/stellar/deployment.md`. La lectura de la cuenta está en `evidence/smart-account/rules-onchain.md`: una regla Default, firmante la dueña, política GuardianHold.

El activo es un SAC de prueba emitido por la dueña. No es el USDC de Circle. La retención es la constante 120 s. No hay parámetro de caducidad de 600 s.

`scripts/deploy-testnet.ps1` se corrió dos veces. La segunda no desplegó contratos nuevos. `scripts/deploy-testnet.sh` corrió la rama de segunda ejecución. En ese bash no hay `stellar`, así que el primer deploy del `.sh` queda no corrido; lo hizo el `.ps1`.

CP-3 sigue pendiente. Lo firma un humano después de revisar los hashes.

## T-005 · nota del día 5

El teléfono no importa semillas. En el build debug, la app crea las claves de la dueña y del guardián dentro del Keystore de Android. `DeviceProvisioningLiveTest` despliega una cuenta propia para esas direcciones públicas. Las cuentas de `deployment.md`, con semillas en `scripts/.testnet/`, siguen siendo las de los ensayos por CLI. Las dos cosas conviven. Falta decidir cuál usa la demo final.

## Día 6 · congelación (Persona 2)

6 oct 2026. Solo arreglos: el sello "En Stellar" ya no se parte, un fallo de firma ya no se muestra como error de red (`SigningFailed`) y los textos dicen "huella o PIN" en vez de "passkey". Las escenas 1, 2, 3 y 6 se ensayaron dos veces en emulador contra testnet, con 6 hashes reales y el vídeo de respaldo: `evidence/demo/rehearsal.md`.

La demo usa una cuenta nueva para las claves del Keystore: `CDWPPTDMACOEVMBUHBBTPUW2EEDXY6YVUAOZJFTSGGOBAONCAJ7WGFV6`. Esto responde a la pregunta de T-005 para la parte de la app. Las partes de terminal de las escenas 2 y 6 no se corrieron en esta cuenta.

## Reconciliación P9 · 6 oct 2026

Dos corridas de ataques, dos cuentas, dos wasm. No se mezclaron. No se redesplegó la cuenta publicada ni la del emulador. No se firmó con semillas.

| Cuenta | Wasm de `account` | Qué tiene hash |
| --- | --- | --- |
| Publicada `CDBJMSUIWL3RCA4POESJQSPHUJ4KOMQE7OTFAO27DDAFXKDBXRNTDEZX` | `986956ccfd0f134bbbd56dfdcfca1def12f4bf61676ef0efd762c2eafd37cf3b` | Ensayos ★ de la mañana, más `enforce` directo `0e46055b…` y `queue` sin la dueña `20c5b598…`. `execute`, `upgrade` y una segunda regla: no hay tx |
| Ataques `CC6FFXGGF62OPCRCL4FQLABPUMEGZ6JQ7XVETDJ5MOLWQGNA4SQYPHFP` | `06226b3773107e33572671cd458ed34d4f00bf76f9b6dbc3bf6e84f60f05ea9c` (sin optimizar) | Destino distinto, monto distinto, muxed, contrato intermedio y GH-28. Verde solo para esta cuenta y este wasm |
| Emulador `CDWPPTDMACOEVMBUHBBTPUW2EEDXY6YVUAOZJFTSGGOBAONCAJ7WGFV6` | `986956ccfd0f134bbbd56dfdcfca1def12f4bf61676ef0efd762c2eafd37cf3b` | Escenas de `rehearsal.md`. No se firmaron ataques |

GH-29, GH-30 y GH-33 siguen no corrido. No hay `expires_at`, ni cancel de la dueña, ni passkey. T-049 no está autorizado. Spike B sigue rojo. P7 no se abre. La IA queda en NICE. Las entrevistas siguen no corrido. Passport no se envió. Los campos del formulario siguen sin contestar. CP-3, CP-7 y CP-9 siguen sin firmar.

Las seis escenas seguidas, en una sola sentada y por debajo de 5 minutos, no se cronometraron.
