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

## Resultado con el wasm del registro nuevo (13 oct, después del merge)

| | |
| --- | --- |
| Etiqueta | **REAL**, entorno de pruebas local, igual que arriba |
| Wasm | Artefacto `wasm` del CI, corrida 38435932911, sobre `main` en `e3e8b4b` (el merge de B-2) |
| Comando | `GP_WASM_DIR=<artefacto> cargo test -p guardian_hold --test registry_cost -- --ignored --nocapture` |

`[FACT]` sha256 del artefacto. `account` y `guardian_hold` son los mismos de testnet; solo cambia el registro.

| Contrato | sha256 |
| --- | --- |
| `account.wasm` | `d60dd1d2…cf40f9` |
| `guardian_hold.wasm` | `ceec440a…6404abe34` |
| `hold_registry.wasm` | `6db027bc…1988ffd4` |

`[FACT]` Columnas: registros previos, instrucciones, entradas de ledger. Las 33 filas dieron `within_limits=true`. Lecturas de disco: 0 en todas. Escrituras: 2 en `contact_payment` y 5 en `queue` y en `release`.

| Registros | `contact_payment` | `queue` | `release` |
| ---: | --- | --- | --- |
| 0 | 1,90 M · 15 | 0,69 M · 14 | 3,01 M · 21 |
| 10 | 1,92 M · 15 | 0,73 M · 14 | 3,07 M · 21 |
| 50 | 2,01 M · 15 | 0,90 M · 14 | 3,20 M · 21 |
| 100 | 2,12 M · 15 | 1,10 M · 14 | 3,34 M · 21 |
| 200 | 2,33 M · 15 | 1,49 M · 14 | 3,62 M · 21 |
| 300 | 2,55 M · 15 | 1,88 M · 14 | 3,89 M · 21 |
| 350 | 2,66 M · 15 | 2,08 M · 14 | 4,05 M · 21 |
| 380 | 2,72 M · 15 | 2,20 M · 14 | 4,14 M · 21 |
| 390 | 2,74 M · 15 | 2,23 M · 14 | 4,17 M · 21 |
| 400 | 2,76 M · 15 | 2,27 M · 14 | 4,19 M · 21 |
| 500 | 2,97 M · 15 | 2,65 M · 14 | 4,46 M · 21 |

- [FACT] Las entradas de ledger no cambian entre 0 y 500 registros. Con el registro desplegado antes, a 500 registros eran 515, 510 y 518.
- [FACT] Con 500 registros, las instrucciones son 2,97 M, 2,65 M y 4,46 M, contra 62,75 M, 61,95 M y 64,39 M del registro anterior. El límite es de 400 M.
- [FACT] Cada operación toca una entrada más que en nativo (15, 14 y 21 contra 14, 13 y 20): la del código del registro.
- [INFERENCE] Las instrucciones aún crecen algo con N (unas 0,2 M a 0,4 M por cada 100 registros). El wasm del registro ya no recorre, así que lo más probable es que sea el costo del entorno de pruebas, que guarda en memoria todas las entradas sembradas. En una transacción real, el host carga solo las entradas del footprint. No se comprobó en testnet.

Este wasm se subió a testnet y es el que usan las cuentas nuevas: [`deployment.md`, release B-2](deployment.md#release-b-2-13-oct-2026).

## Límites reales de testnet (13 oct)

| | |
| --- | --- |
| Etiqueta | **REAL**: `getLedgerEntries` de las `ConfigSetting` en el RPC de testnet, ledger 5179805 (13 oct, ~20:16 UTC) |
| Lectura | `RegistryScaleLiveTest.logNetworkLimits`, que las imprime en cada corrida |

| Límite por transacción | Testnet | Instantánea del SDK usada arriba |
| --- | ---: | ---: |
| Instrucciones (`tx_max_instructions`) | 400 000 000 | 400 M |
| Entradas de footprint (`tx_max_footprint_entries`) | 400 | 400 |
| Lecturas de disco (`tx_max_disk_read_entries`) | 200 | 200 |
| Escrituras (`tx_max_write_ledger_entries`) | 200 | 200 |
| Bytes leídos de disco (`tx_max_disk_read_bytes`) | 200 000 | — |
| Bytes escritos (`tx_max_write_bytes`) | 132 096 | — |
| Memoria (`tx_memory_limit`) | 41 943 040 | — |
| Tamaño de la transacción (`tx_max_size_bytes`) | 132 096 | — |

- [FACT] Los cuatro límites con los que se comparó la medición local son iguales en testnet. El veredicto de arriba (el registro anterior se bloquea hacia los 390 registros; el nuevo, no) vale con los límites reales.
- [FACT] Por ledger, testnet admite 580 M instrucciones y 1 000 lecturas y escrituras de disco.

## Resultado en testnet con 400 registros (13 oct)

| | |
| --- | --- |
| Etiqueta | **REAL**: testnet, `RegistryScaleLiveTest` con `-PliveTestnet` (`gp.records` = 400), claves desechables generadas en la corrida |
| Fecha | 2026-10-13, 20:22:02–22:03:17 UTC, ledgers 5179867–5181082. La prueba duró 6 114 s |
| Cuenta | `CBRFYHU6CM2XVFPIQ6VATN2F24LK737II6YGDEJ6ZFILCSFQHGTKK44Z`, registro `CAYBBDVW7FVDMQXQ6O43353NOMWA3LGIGBQINUMW24MVWQ6AZ2NXBDCW` (wasm `6db027bc…` de la release B-2) |
| Resultado | Pasó |

La prueba hace un pago a un contacto, luego 400 veces `queue` (dueña) más `cancel` (guardián), y después repite cada operación con los 400 registros detrás. Al final, una retención de 150 madura y se libera. Los recursos son los que declaró la transacción que entró (simulación más el margen del gateway, 8 M instrucciones), leídos con `getTransaction`.

| Operación | Registros detrás | Instrucciones | Footprint (ro + rw) | Bytes leídos de disco | Bytes escritos | Hash |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| Pago a contacto | 0 | 10 487 127 | 15 (11 + 4) | 116 | 904 | `db016862…843d` |
| `queue` | 0 | 10 544 219 | 15 (10 + 5) | 0 | 1 004 | `f5dfb2ca…031e` |
| `cancel` | 0 | 8 821 750 | 6 (3 + 3) | 0 | 544 | `99255b71…6697` |
| Pago a contacto | 400 | 10 482 949 | 15 (11 + 4) | 116 | 904 | `27246321…2ac8` |
| `queue` | 400 | 10 573 406 | 15 (10 + 5) | 0 | 1 004 | `0bfe0e9f…b1dc` |
| `cancel` | 401 | 8 821 750 | 6 (3 + 3) | 0 | 544 | `03999693…6cfb` |
| `queue` | 401 | 10 573 406 | 15 (10 + 5) | 0 | 1 004 | `e9b3e7d9…7551` |
| Liberación madurada | 402 | 11 755 429 | 18 (11 + 7) | 116 | 1 448 | `1ea7f73b…893e` |

Preparación: SAC de prueba `bbcaefee…df2a`, trustlines `c1d276fb…22b9` y `a49e7c1a…59a7`, mint `ed6ddbef…4919`.

- [FACT] Con 400 registros detrás, el footprint, los bytes leídos y los bytes escritos son los mismos que con 0. Las instrucciones de `queue` subieron 29 187 (0,28 %) y las del pago bajaron 4 178.
- [FACT] El ritmo fue estable: 25 registros cada 333–400 s, del primero al último tramo.
- [FACT] Tras los 400 ciclos, `readHolds` devolvió 400 registros, todos `Cancelled`. Al final, los dos últimos eran `Cancelled` y `Executed` (la liberación), y los saldos fueron 150 para el desconocido y 330 para la cuenta.
- [FACT] Las 12 transacciones de la tabla y la preparación devuelven `SUCCESS` en `getTransaction`.
- [INFERENCE] Con el registro anterior, 400 registros pasan el límite de 400 entradas de footprint (ver arriba, hacia los 390). Con el índice, la cuenta siguió operando sin acercarse al límite: 18 de 400 entradas en la operación más cara.
- [UNVERIFIED] No se probó con miles de registros. El índice no recorre registros terminados, así que no se espera que cambie; `readHolds` del cliente lee en lotes de 100 claves y su tiempo sí crece con el historial.

## No corrido
- [FACT] Las cuentas creadas antes de la release B-2 siguen con el registro anterior, `a57f01b7…` o el del 6 oct, y sin `upgrade` no pueden cambiarlo. Siguen expuestas a este límite: `CB4PUYVY…`, `CA6VCI3O…`, `CA5Z7J…`, `CDWPPT…`, `CDBJMS…` y `CC6FFXGG…`.
