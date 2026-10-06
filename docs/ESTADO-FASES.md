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

T-034 se puede asignar. No está empezada. El spike no se copia a `contracts/`.

## Fases

| Fase | Estado |
| --- | --- |
| P0 | INCOMPLETA |
| P1 Spike A | cerrada. Evidencia GO y CP-1 GO. |
| P2 | en curso. T-034 hecha (workspace + vendor OZ). |
| P3–P10 | no iniciado |

Definition of Done: implementación + unit + integración + seguridad + criterios de aceptación + evidencia. Compilar no cierra una fase.
