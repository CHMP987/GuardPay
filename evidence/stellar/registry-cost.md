# Costo del registro con muchos registros por cuenta (F-02)

| | |
| --- | --- |
| Etiqueta | **REAL**, en el entorno de pruebas local de Soroban (`soroban-sdk` 28.0.0, `testutils`). No es una transacción en testnet. |
| Fecha | 2026-10-13 |
| Prueba | [`contracts/guardian_hold/tests/registry_cost.rs`](../../contracts/guardian_hold/tests/registry_cost.rs) (`#[ignore]`, opt-in) |
| Wasm medido | Artefacto `wasm` del CI, corrida 38343802866: el mismo que está en testnet ([`deployment.md`](deployment.md), release F-01) |

```
GP_WASM_DIR=<artefacto> cargo test -p guardian_hold --test registry_cost -- --ignored --nocapture
GP_WASM_DIR=<artefacto> GP_REGISTRY=source cargo test -p guardian_hold --test registry_cost -- --ignored --nocapture
```

`[FACT]` sha256 de los wasm cargados, iguales a los de `deployment.md`:

| Contrato | sha256 |
| --- | --- |
| `account.wasm` | `d60dd1d2…cf40f9` |
| `guardian_hold.wasm` | `ceec440a…6404abe34` |
| `hold_registry.wasm` | `a57f01b7…0438f93d0` |

## Método

- Se despliegan la cuenta, la política y el registro desde wasm, con un contacto de confianza y tope 1 000 000.
- Se escriben N registros `Executed` directamente en el almacenamiento del registro, con sus mismas claves (`NextId`, `Hold`). Sembrar no cuesta nada; lo que se mide es la llamada siguiente.
- Cada fila es un `Env` nuevo, con los límites desactivados para ver el costo pasado el límite. El veredicto se compara después con los límites.
- Operaciones:
  - `contact_payment`: `__check_auth` de la cuenta, firmado por la dueña, para un `transfer` de 10 a un contacto sin retención. Desde F-01 consulta el registro.
  - `queue`: `queue` del registro como llamada principal. La autorización de la cuenta está simulada, así que no incluye el costo fijo de la política para ese contexto.
  - `release`: `queue` de una retención, avance a `ready_at + 1`, y `__check_auth` del `transfer` madurado. Es la retención más nueva, así que el recorrido la encuentra al final.
- Límites por transacción con los que el SDK de pruebas compara por defecto (`InvocationResourceLimits::mainnet()`, instantánea del SDK del 2026-07-10):
  - 400 M instrucciones;
  - 400 entradas de ledger (footprint);
  - 200 lecturas de disco;
  - 200 escrituras.
- `ledger_entries` = lecturas en memoria + lecturas de disco + escrituras.

## Resultado con el registro desplegado

`[FACT]` Columnas: registros previos, instrucciones, entradas de ledger. Las lecturas de disco fueron 0 en todas las filas. Las escrituras fueron 2 en `contact_payment` y 3 en las otras dos operaciones.

| Registros | `contact_payment` | `queue` | `release` |
| ---: | --- | --- | --- |
| 0 | 1,88 M · 15 | 0,55 M · 10 | 2,85 M · 18 |
| 10 | 2,29 M · 25 | 0,98 M · 20 | 3,30 M · 28 |
| 50 | 4,46 M · 65 | 3,16 M · 60 | 5,52 M · 68 |
| 100 | 7,91 M · 115 | 6,68 M · 110 | 9,06 M · 118 |
| 200 | 17,20 M · 215 | 16,08 M · 210 | 18,48 M · 218 |
| 300 | 29,48 M · 315 | 28,46 M · 310 | 30,89 M · 318 |
| 350 | 36,70 M · 365 | 35,75 M · 360 | 38,16 M · 368 |
| 380 | 41,37 M · 395 | 40,47 M · 390 | 42,87 M · 398 |
| 390 | 42,99 M · **405** | 42,10 M · 400 | 44,50 M · **408** |
| 400 | 44,64 M · **415** | 43,76 M · **410** | 46,16 M · **418** |
| 500 | 62,75 M · **515** | 61,95 M · **510** | 64,39 M · **518** |

En **negrita**, lo que supera 400 entradas.

- [FACT] Cada registro previo suma una entrada de ledger y entre 0,1 M y 0,2 M instrucciones a las tres operaciones.
- [FACT] El límite que se alcanza primero es el de 400 entradas, no el de instrucciones:
  - `contact_payment` y `release` lo superan a partir de 390 registros;
  - `queue` lo supera a partir de 400 registros.
- [INFERENCE] En una transacción real el umbral es algo menor, porque se suman entradas que esta medición no incluye:
  - las del `transfer` del SAC (instancia del token y saldos);
  - en `queue`, las de la autorización de la cuenta y la política.
- [INFERENCE] Pasado ese punto, la cuenta ya no puede pagar a nadie, porque desde F-01 todo `transfer` de USDC consulta el registro. Tampoco puede crear retenciones. Los registros nunca se borran y ningún contrato tiene `upgrade`, así que el bloqueo sería permanente.
- [INFERENCE] Unas 385 retenciones en la vida de la cuenta son pocas para un uso real de varios años. Por eso se aplicó el índice (criterio 2 de B-2).

## Resultado con el índice (código de esta rama)

El registro ahora guarda dos entradas más:

- `Active(cuenta, token, destino, monto) → id`, para la única retención `Retained` con esos campos;
- `RetainedIds(cuenta) → [id]`, con las retenciones `Retained` de la cuenta.

`queue` las escribe. `cancel` y `mark_executed` las quitan. `find_retained` lee el índice y el registro que este nombra, sin recorrer nada. `NextId` y `Hold` no cambian, así que la app los sigue leyendo igual.

`[FACT]` Mismo método, con `GP_REGISTRY=source`: el registro se registra desde el código de esta rama, en nativo, y la cuenta y la política siguen siendo el wasm desplegado.

| Registros | `contact_payment` | `queue` | `release` |
| ---: | ---: | ---: | ---: |
| 0 | 14 | 13 | 20 |
| 100 | 14 | 13 | 20 |
| 200 | 14 | 13 | 20 |
| 390 | 14 | 13 | 20 |
| 500 | 14 | 13 | 20 |

La tabla muestra entradas de ledger. Las filas 10, 50, 300, 350, 380 y 400 dieron los mismos valores.

- [FACT] El número de entradas ya no depende de cuántos registros tiene la cuenta.
- [FACT] Un registro nativo no tiene entrada de código. Con el wasm sería una entrada más, como se ve en `contact_payment` con 0 registros: 15 con el wasm desplegado, 14 en nativo.
- [FACT] `queue` y `release` escriben 5 entradas en lugar de 3, por el índice.
- [INFERENCE] Las instrucciones en nativo no son comparables con las del wasm, porque el código nativo no se mide en la VM. Aun así crecieron un poco con N: de 1,49 M a 2,58 M en `contact_payment` entre 0 y 500 registros. Lo más probable es que sea el costo del entorno de pruebas, que mantiene en memoria todas las entradas sembradas. En una transacción real, el footprint incluye solo las entradas declaradas.
- [FACT] La prueba unitaria `lookups_do_not_grow_with_history` (`contracts/hold_registry/src/tests.rs`) lo comprueba en cada `cargo test`: con 0 retenciones terminadas y con 60, `queue` toca 12 entradas y `find_retained` 3.

## No corrido

- La medición de instrucciones con el **wasm** del registro nuevo. No hay `wasm32v1-none` ni `stellar-cli` en este equipo. El job `wasm` del CI lo compila. Para medirlo, se corre la misma prueba con `GP_WASM_DIR` apuntando a ese artefacto.
- Los límites reales de testnet. No se leyeron las `ConfigSetting` de la red; los números de arriba son la instantánea del SDK.
- Una transacción en testnet con muchos registros.
- El despliegue del registro nuevo. Su wasm no está subido a testnet, y `TestnetProvisioner.HOLD_REGISTRY_WASM` sigue fijado a `a57f01b7…`. Las cuentas existentes siguen con el registro anterior (sin `upgrade`), así que siguen expuestas a este límite. Una cuenta nueva recibe el registro nuevo solo si se sube el wasm y se cambia ese hash.
