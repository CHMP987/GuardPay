# Estado de fases

5 oct 2026, noche. Respuestas humanas de esta fecha. Lo no contestado queda sin contestar.

## Gates

| Gate | Respuesta | Fuente |
| --- | --- | --- |
| G1 | `12 oct` (Passport, 17:59) | Humano, 5 oct 2026. La entrega no es por Luma (5 oct 16:00). |
| Inscripción en Passport y campos del formulario | sin contestar | Siguen abiertos dentro de T-001. No inventados. |
| G2 | sí | `[FACT]` 5 oct 2026. `stellar contract build` de `stellar-accounts` @ `b40c5ea` con soroban-sdk 28.0.0, target `wasm32v1-none`, stellar-cli 28.1.0. Exit 0. Detalle en `docs/DEPENDENCIES.md`. |
| G3 | `SIN CONFIRMAR — bloquea` | T-002 pendiente. Mac / Xcode sin respuesta. |

## CP-0

Veredicto humano: **GO** a P1. Registrado el 5 oct 2026 a pedido del humano.

P0 sigue **INCOMPLETA**. T-010, T-011, T-012, T-013, T-014 y T-018 no están hechas. `README.md` no existe. Android SDK no está (`ANDROID_HOME` vacío): bloquea la app, no el Spike A.

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
| P4–P10 | no iniciado |

Definition of Done: implementación + unit + integración + seguridad + criterios de aceptación + evidencia. Compilar no cierra una fase.

## P2 — contratos

6 oct 2026. Tres contratos en `contracts/`: una regla Default, política con lista blanca, registro que calcula `ready_at`. Tests nativos en verde: 35 en `guardian_hold` `tests/must.rs` más el smoke de `account`. Evidencia: `evidence/policies/tests.md` y `evidence/policies/whitelist-review.md`.

CP-2 GO el 6 oct 2026. `docs/INTERFACES.md` describe la forma de producción: hasta 3 contactos y `daily_cap`.

## P3 — testnet

6 oct 2026. `[FACT]` Los tres wasm están en testnet. Direcciones, hashes de deploy y los 10 ensayos ★ están en `evidence/stellar/deployment.md`. La lectura de la cuenta está en `evidence/smart-account/rules-onchain.md`: una regla Default, firmante la dueña, política GuardianHold.

El activo es un SAC de prueba emitido por la dueña. No es el USDC de Circle. La retención es la constante 120 s. No hay parámetro de caducidad de 600 s.

`scripts/deploy-testnet.ps1` se corrió dos veces. La segunda no desplegó contratos nuevos. `scripts/deploy-testnet.sh` corrió la rama de segunda ejecución. En ese bash no hay `stellar`, así que el primer deploy del `.sh` queda no corrido; lo hizo el `.ps1`.

CP-3 sigue pendiente. Lo firma un humano después de revisar los hashes.
