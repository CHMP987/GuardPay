# Revisión de la lista blanca de `enforce`

6 oct 2026. Recorrido de `contracts/guardian_hold/src/lib.rs` (`enforce_impl` y los dos carriles). `[FACT]` sobre el código de esta fecha. La política no llama a un token. El registro no mueve tokens.

Orden fijo: `account.require_auth()`, luego la dueña en `authenticated_signers`, luego la lista blanca. Un pánico revierte la transacción.

## Antes de la lista

| Rama | Resultado | Error | Test |
| --- | --- | --- | --- |
| La cuenta no autoriza la llamada a `enforce` | reject | auth de Soroban | GH-26 |
| No hay config instalada para esa cuenta | reject | `NotAllowed` (3) | cubierto al cargar config |
| La dueña no está en `authenticated_signers` | reject | `OwnerNotAuthenticated` (2) | GH-01, GH-09, GH-24 |
| Segundo `install` | reject | `AlreadyInstalled` (1) | GH-32 |
| `install` con 0 contactos, más de 3, o tope `<= 0` | reject | `InvalidConfig` (9) | el constructor de tests no toma ese camino; no hay un GH propio |
| `uninstall` | reject | `NotAllowed` (3) | no hay test con nombre propio |

## Contexto

| Rama | Resultado | Error |
| --- | --- | --- |
| El contexto no es `Contract` (incluye create) | reject | `NotAllowed` |
| El contrato llamado es la cuenta misma | reject | `NotAllowed` (cae al catch-all) |

## Carril USDC `transfer`

Solo si el contrato es el USDC de la config y la función es `transfer`.

| Rama | Resultado | Error | Test |
| --- | --- | --- | --- |
| `from` no es esta cuenta | reject | `NotAllowed` | — |
| `to` no convierte a `Address` (muxed) | reject | `InvalidDestination` (8) | GH-20b |
| `amount` no es `i128` | reject | `NotAllowed` | — |
| `amount <= 0` | reject | `InvalidAmount` (4) | — |
| Destino en los contactos, suma del día `<= daily_cap` | accept. Actualiza `spent` y, si cambió el día UTC (`timestamp / 86400`), pone `spent` en 0 antes de sumar | — | GH-02, reinicio de día |
| La suma desborda `i128` | reject. No se guarda el gasto | `Overflow` (6) | `overflow_near_i128_max_rejected` |
| La suma pasa el tope | reject. No se convierte en retención | `CapExceeded` (5) | GH-10, reinicio de día |
| Destino que no es contacto y no hay hold Retained con la misma cuenta, token, destino y monto | reject | `NotAllowed` | GH-03, GH-20, GH-22, GH-23 |
| Hay hold pero `now < ready_at`, u otro campo no coincide al releer | reject | `HoldNotReady` (7) | GH-05, GH-21 |
| Hold Retained, campos iguales y `now >= ready_at` | accept. `mark_executed` (solo lo puede llamar esta política) | — | GH-06 |
| Ese hold ya está Stopped o Executed | reject | `NotAllowed` (ya no está Retained) | GH-08, GH-19 |

La comparación del hold es campo a campo: token, destino, monto, y `ready_at` contra el ledger. `find_retained` no se llama en el carril de `queue`.

## Carril `HoldRegistry.queue`

Solo si el contrato es el registro de la config y la función es `queue`. No lee el registro.

| Rama | Resultado | Error | Test |
| --- | --- | --- | --- |
| El primer argumento es esta cuenta y el monto (argumento 3) es `> 0` | accept | — | GH-04, GH-27 (el rechazo de GH-27 es la firma, no esta rama) |
| El primer argumento es otra cuenta | reject | `NotAllowed` | — |
| Monto `<= 0` | reject | `InvalidAmount` | — |

El registro, en `queue`, rechaza un segundo Retained con la misma clave (GH-31) y calcula `ready_at`. Eso no es una rama de `enforce`.

## Catch-all

Todo lo que no entró en las dos ramas de arriba termina en `NotAllowed`. Incluye:

- `approve`, `burn`, `transfer_from` (GH-16, GH-17, GH-15)
- otro token (GH-22)
- otro contrato, llamada anidada, con o sin hold maduro (GH-18, GH-18b)
- `cancel` a través de `enforce`, incluido el de la dueña
- cualquier otra función

`cancel` del guardián no pasa por `enforce`. Es una llamada directa al registro: solo el guardián autoriza, el estado pasa de Retained a Stopped (GH-07). Un extraño no puede (GH-07b).

## Lo que esta lista no hace

`[FACT]` No hay rama de cancel para la dueña ni lectura de `expires_at`. T-049 no está hecha. Un pago maduro no vence dentro del contrato.
