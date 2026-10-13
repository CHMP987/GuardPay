# Informe de auditoría de GuardPay

| | |
| --- | --- |
| Fecha | 12 oct 2026 |
| Commit auditado | `d3cdb51` (rama `main`; la auditoría trabaja en la rama `auditoria`) |
| Alcance | Contratos (`contracts/`), cliente KMP (`app/`), shell iOS (`iosApp/`), scripts (`scripts/`), CI (`.github/workflows/`), documentación y evidencia |
| Tipo | Revisión de código, ejecución de pruebas locales sin red, una reproducción en una copia aislada y consultas de solo lectura a Stellar Testnet |
| Autor | Asistente de IA (Claude Code), a pedido del equipo. **No es una auditoría de seguridad profesional ni independiente.** |

> [!IMPORTANT]
> Este informe no certifica que GuardPay sea seguro ni que esté libre de fallos. Registra lo que se revisó, lo que se ejecutó y lo que quedó sin revisar. Lo que no se pudo comprobar está marcado como tal.

Etiquetas usadas:

- **Estado de una funcionalidad:** IMPLEMENTADA Y VERIFICADA · IMPLEMENTADA, NO VERIFICADA · PARCIALMENTE IMPLEMENTADA · SIMULADA · MOCK · PLANIFICADA · NO ENCONTRADA · INDETERMINADA.
- **Severidad:** CRÍTICA · ALTA · MEDIA · BAJA · INFORMATIVA.
- **Confianza:** Alta · Media · Baja.
- **Afirmaciones:** [FACT] comprobado; [INFERENCE] deducido del código; [UNVERIFIED] sin comprobar.
- **Evidencia:** REAL · SIMULATED · ILLUSTRATIVE, como en `evidence/`.

"Verificada" en este informe significa: comprobada por esta auditoría, ejecutando una prueba o leyendo la cadena. Lo que solo consta en evidencia previa del repositorio se marca así.

## Contenido

1. [Resumen](#1-resumen)
2. [Método y límites](#2-método-y-límites)
3. [Hallazgos](#3-hallazgos)
4. [Integración con Stellar, por etapa](#4-integración-con-stellar-por-etapa)
5. [Secretos e integridad del repositorio](#5-secretos-e-integridad-del-repositorio)
6. [Pruebas ejecutadas y no ejecutadas](#6-pruebas-ejecutadas-y-no-ejecutadas)
7. [Controles que funcionan](#7-controles-que-funcionan)
8. [Matriz de trazabilidad](#8-matriz-de-trazabilidad)

## 1. Resumen

GuardPay es un MVP en Stellar Testnet. El núcleo de la tesis está implementado y probado: una cuenta con una sola regla, una política que rechaza todo lo que no está en su lista, un registro de retenciones donde solo el guardián puede detener, y una app Android que firma con el Keystore y avisa al guardián sin servidor. Las 36 pruebas de los contratos y las 353 ejecuciones de pruebas del cliente pasaron en esta auditoría.

Hallazgos principales:

| ID | Severidad | Título | Estado |
| --- | --- | --- | --- |
| [F-01](#f-01) | ALTA | Un pago retenido a un contacto de confianza nunca se libera por el registro | Corregido en el código (rama `fix-f01`). Política nueva desplegada en testnet el 12 oct y probada con una cuenta nueva; las cuentas anteriores siguen con la política vieja |
| [F-02](#f-02) | ALTA | El registro recorre todos los registros de la cuenta en cada `queue` y cada liberación | Medido el 13 oct: pasa el límite de 400 entradas hacia los 390 registros. Corregido con un índice y desplegado en testnet el 13 oct (release B-2) para las cuentas nuevas; las anteriores siguen con el registro que recorre |
| [F-03](#f-03) | MEDIA | La simulación libera retenciones a contactos que el contrato rechaza | Corregido junto con F-01 (rama `fix-f01`) |
| [F-04](#f-04) | MEDIA | Un solo guardián fijo, sin cambio ni recuperación | Documentado en el repositorio |
| [F-05](#f-05) | MEDIA | Contratos sin auditoría profesional y OpenZeppelin fijado a un pre-release | Documentado en el repositorio |

No se encontraron semillas, claves privadas ni tokens de acceso en los archivos versionados ni en el historial de git ([sección 5](#5-secretos-e-integridad-del-repositorio)).

## 2. Método y límites

**Qué se hizo:**

- Lectura del código de los tres contratos, de las pruebas GH (`contracts/guardian_hold/tests/must.rs`), del cliente (`domain`, `stellar`, `simulation`, `ai`, `ui/owner`, `ui/guardian`), del shell Android (`signing`, `notifications`, `Wiring` de debug y release, manifiesto), de los scripts y de los workflows de CI.
- Ejecución de las pruebas sin red de contratos y cliente ([sección 6](#6-pruebas-ejecutadas-y-no-ejecutadas)).
- Una reproducción de F-01 en una **copia** de los contratos fuera del repositorio (`git archive` de `HEAD`), con dos pruebas añadidas solo a esa copia.
- Consultas `GET` de solo lectura a Horizon testnet para cinco hashes citados en la evidencia.
- Búsqueda de secretos en archivos versionados, en `vendor/` y en el historial de git.

**Qué no se hizo:**

- No se firmó, envió ni desplegó nada, y no se tocó mainnet.
- No se corrieron las pruebas `*LiveTest` (testnet), las instrumentadas, la IA en el teléfono, el emulador ni los scripts de ataque.
- No se compiló el wasm (`stellar contract build`).
- No se auditó `vendor/` (OpenZeppelin) línea por línea.
- No se midieron los límites de recursos de Soroban.
- No se modificó código, contratos, dependencias, archivos de bloqueo ni configuración.

## 3. Hallazgos

Cada hallazgo indica su estado de verificación y su confianza. Los hallazgos BAJA e INFORMATIVA documentados ya en el `README.md` se listan para que el informe sea completo, no porque sean nuevos.

<a id="f-01"></a>
### F-01 · Un pago retenido a un contacto de confianza nunca se libera por el registro

| | |
| --- | --- |
| Categoría | Integridad funcional (contrato y cliente) |
| Severidad | ALTA |
| Estado | Confirmado en una copia aislada de los contratos (prueba local, entorno de pruebas de Soroban). No reproducido en testnet. **Corregido en el código** después de la auditoría ([ver abajo](#f-01-correccion)); los contratos desplegados siguen con el comportamiento anterior. |
| Confianza | Alta |
| Dependencias | F-03 (la simulación oculta el caso) |

**Evidencia.**

- [FACT] En `guardian_hold::enforce`, un `transfer` a un contacto de confianza entra solo en la rama del tope diario (`contracts/guardian_hold/src/lib.rs:187-202`). Esa rama no llama a `find_retained` ni a `mark_executed` (líneas 205-217, que solo corren para destinos que no son de confianza).
- [FACT] La app sí crea retenciones a contactos de confianza: `predictLane` devuelve `MustHold(OwnerChoseHold)` (enlace "Retener este pago", `PayScreen.kt:221`) y `MustHold(OverDailyCap)` para contactos sobre el tope; `OwnerSession` llama entonces a `submitQueue` (`OwnerSession.kt:340`). Para liberar usa el mismo `submitTransfer` (`OwnerSession.kt:395`).
- [FACT] Reproducción en la copia aislada (`cargo test --locked -p guardian_hold --test must audit_repro`, 2 pruebas, 2 pasaron):
  - Retención a un contacto por `DAILY_CAP + 10`, madurada (más de 120 s): el `transfer` falla con `Contract(5)` = `CapExceeded`, y el registro sigue `Retained`.
  - Retención a un contacto por 10 (dentro del tope), madurada: el `transfer` se autoriza; el registro **sigue `Retained`**; después el guardián puede cancelarlo y queda `Stopped`.
- [FACT] Ninguna prueba GH ni archivo de `evidence/` cubre la liberación de una retención a un contacto de confianza (`gh_14_transfer_lanes` libera una retención a una dirección que no es contacto).

**Impacto.**

- Una retención a un contacto **por encima del tope** no se puede enviar nunca: el contrato la rechaza con `CapExceeded`, aunque haya madurado. Es justo el caso que el `README.md` presenta como "un monto por encima del tope queda retenido 120 segundos".
- Una retención a un contacto **dentro del tope** sale como pago normal de contacto, gasta tope y deja el registro `Retained`. [INFERENCE] El guardián seguiría viéndolo como "Retenido" y podría "Detenerlo" después de que el dinero salió, y el estado en cadena quedaría `Stopped` para un pago enviado. Un nuevo `queue` idéntico choca con `DuplicateActive`. [INFERENCE] Al releer el registro, la app podría volver a ofrecer enviarlo; cada envío gasta tope, así que el límite diario sigue aplicando.
- La tesis central no se rompe: ningún camino deja salir dinero a un destino nuevo sin retención, ni superar el tope diario. El defecto afecta a la coherencia entre el registro y lo que pasó, y a la promesa de "retener y luego enviar" para contactos.

**Recomendación.** [RECOMMENDATION] Decidir el comportamiento esperado (por ejemplo: que la política busque primero una retención madurada para cualquier destino, y que la rama del tope aplique solo si no hay retención; o que la app no permita retener pagos a contactos) y cubrirlo con pruebas GH nuevas para ambos montos. Como la cuenta no tiene `upgrade`, un cambio de contrato exige redesplegar cuentas.

<a id="f-01-correccion"></a>
**Corrección (12 oct 2026, rama `fix-f01`).** Las líneas citadas arriba son las de `19da401`, antes del cambio.

- [FACT] Comportamiento elegido: el que describe el security spike (línea 206). Un pago a un contacto que supera el tope se rechaza, la app ofrece retenerlo, y esa retención tiene que poder salir cuando madura.
- [FACT] Cambio en `try_enforce_usdc_transfer` (`contracts/guardian_hold/src/lib.rs:187-222`): la política consulta primero `find_retained` para **cualquier** destino.
  - Si hay una retención `Retained` para esa cuenta, token, destino y monto, exige `ready_at` (si no, `HoldNotReady`), llama a `mark_executed` y autoriza **sin gastar tope**.
  - Si no hay retención, un contacto pasa por el tope diario como antes, y cualquier otro destino se rechaza.
- [FACT] Pruebas nuevas en `contracts/guardian_hold/tests/must.rs` (`f01_*`; las 39 pasan):
  - una retención madurada sobre el tope sale y queda `Executed`, sin gastar tope y sin poder salir sin la firma de la dueña;
  - una retención madurada dentro del tope queda `Executed`; el guardián ya no puede cancelarla; se puede volver a retener el mismo pago; el tope sigue entero;
  - antes de `ready_at`, ese mismo pago se rechaza aunque quepa en el tope, mientras que otro monto al mismo contacto sigue pasando por el tope;
  - una retención detenida (`Stopped`) no se libera, y sobre el tope se rechaza.
- [FACT] Tres de esas cuatro pruebas fallan con el contrato anterior (`git show HEAD:…/lib.rs`). La cuarta (retención detenida) pasa con ambos: protege contra una regresión.
- [INFERENCE] Efecto secundario: un pago dentro del tope a un contacto con una retención aún no madurada **del mismo monto** se rechaza hasta que madure. Es coherente con que la dueña eligió retenerlo.
- [INFERENCE] Costo: ahora **todo** `transfer` de USDC recorre el registro, también los pagos a contactos. Eso agrava [F-02](#f-02).
- [FACT] Solo cambia `guardian_hold`. `account.wasm` y `hold_registry.wasm` no cambian. Pero ni la cuenta ni la política tienen `upgrade`, así que las cuentas desplegadas (`evidence/stellar/deployment.md`, política `b36588…85ce`) mantienen el comportamiento anterior. Hace falta desplegar una política nueva y cuentas nuevas que la usen.
- [FACT] Redespliegue (12 oct, REAL): el job `wasm` del CI construyó el wasm (`guardian_hold.wasm` `ceec440a…be34`) y se desplegó la política `CBTIF2BF7YGCR6OT7TQMHGGMY6BVHKPOAJV5BYZQ2P3IWKSEXYHLWFMW` (`evidence/stellar/deployment.md`, release F-01). `KmpStellarGatewayLiveTest` corrió el ciclo con una cuenta nueva: retuvo 60 para un contacto cuando quedaban 40 de tope; el envío antes de madurar dio `Rejected(code=7)`, y después de madurar salió (`ab668c03…6e57`) y quedó `Executed` sin gastar tope (`evidence/stellar/gateway-live.md`, corrida 6).
- [FACT] Reprovisión (13 oct, REAL): el teléfono (dueña) y el emulador (guardián) apuntan a la cuenta nueva `CB4PUYVY…`, con `CBTIF2BF…` (`evidence/demo/two-devices-f01.md`). `CDWPPT…` y `CA5Z7J…` siguen en testnet con `CCGIEMIU…`, pero ningún dispositivo las usa.
- [FACT] Caso dentro del tope en testnet (13 oct, REAL), desde la app: el teléfono retuvo 10 USDC a un contacto (`14782c60…6d12`), el guardián en el emulador no lo detuvo, y al madurar la dueña lo envió (`ed8d5531…22bd`). El tope siguió en 50 y el guardián pasó a ver "Enviado" (`evidence/demo/two-devices-f01.md`).
- No corrido: el caso sobre el tope desde la app, y leer el registro por RPC fuera de la app después del envío.

<a id="f-02"></a>
### F-02 · El registro recorre todos los registros de la cuenta en cada `queue` y cada liberación

| | |
| --- | --- |
| Categoría | Disponibilidad / límites de recursos |
| Severidad | ALTA |
| Estado | Medido en el entorno de pruebas local (13 oct 2026). **Corregido** ([ver abajo](#f-02-correccion)) y desplegado en testnet para las cuentas nuevas (release B-2, 13 oct). Las cuentas anteriores siguen con el registro que recorre. |
| Confianza | Media |
| Dependencias | Riesgo R6 del plan (recursos dentro de `__check_auth`) |

**Evidencia.**

- [FACT] `find_retained_id` (usado por `queue` para rechazar duplicados) y `find_retained` (usado por `enforce` en cada liberación) recorren `0..next_id` con una lectura `persistent` por registro (`contracts/hold_registry/src/lib.rs:191` y `:234`). `list_retained` también.
- [FACT] Los registros nunca se borran: `Stopped` y `Executed` siguen contando en el recorrido.
- [FACT] La cuenta no tiene `upgrade`, y el registro tampoco.

**Impacto.** [INFERENCE] Cada retención creada aumenta el costo de todas las siguientes. Con suficientes registros, `queue` y la liberación de retenciones superarían los límites de lecturas o instrucciones por transacción de Soroban, y la cuenta ya no podría pagar a destinos nuevos. Antes de la corrección de F-01, los pagos a contactos dentro del tope no consultaban el registro y habrían seguido funcionando. Con la corrección, todo `transfer` de USDC consulta el registro, así que también esos pagos dejarían de funcionar. [UNVERIFIED] El número de registros que lo dispara y el efecto de las entradas archivadas no se midieron (INDETERMINADA).

**Recomendación.** [RECOMMENDATION] Medir el costo de `queue` y de una liberación con 10, 50, 100 y 200 registros en una prueba local o en testnet. Si el límite es alcanzable en un uso normal, indexar las retenciones activas por `(token, destino, monto)` en lugar de recorrer.

<a id="f-02-correccion"></a>
**Medición y corrección (13 oct 2026, rama `registry-cost`).** Las líneas citadas arriba son las de `aae640f`, antes del cambio. Detalle en [`evidence/stellar/registry-cost.md`](evidence/stellar/registry-cost.md).

- [FACT] Medición (REAL, entorno de pruebas local, wasm del CI que está en testnet): cada registro previo suma una entrada de ledger a `queue`, a la liberación y a un pago a contacto. El límite de 400 entradas por transacción se supera desde 390 registros en el pago a contacto y en la liberación, y desde 400 en `queue`. Las instrucciones quedan por debajo de 65 M con 500 registros; el límite es de 400 M.
- [INFERENCE] En una transacción real el umbral es algo menor, porque se suman las entradas del SAC y de la autorización. Pasado el umbral, la cuenta no podría pagar a nadie ni retener, y sin `upgrade` no hay arreglo para esa cuenta. Unas 385 retenciones en la vida de una cuenta es un uso plausible, así que se aplicó el índice.
- [FACT] Cambio en `contracts/hold_registry/src/lib.rs`: dos claves nuevas, `Active(cuenta, token, destino, monto) → id` y `RetainedIds(cuenta) → [id]`.
  - `queue` rechaza duplicados con una lectura de `Active` y escribe las dos claves.
  - `cancel` y `mark_executed` las quitan.
  - `find_retained` lee `Active` y comprueba el registro que nombra.
  - `list_retained` devuelve `RetainedIds`.
  - Ninguna función recorre los registros.
  - `NextId`, `Hold`, las firmas públicas y los errores no cambian, así que la política y la app siguen compatibles.
- [FACT] Con el registro de esta rama (nativo, misma prueba), las entradas quedan fijas entre 0 y 500 registros: 14 en el pago a contacto, 13 en `queue` y 20 en la liberación. `queue` y la liberación escriben 5 entradas en lugar de 3.
- [FACT] Pruebas: 10 pruebas propias del registro ([F-13](#f-13)), entre ellas `lookups_do_not_grow_with_history`. Las 39 de `must.rs` siguen pasando.
- [FACT] Wasm del registro nuevo (REAL, entorno local, CI 38435932911 sobre `e3e8b4b`): entre 0 y 500 registros, 15 entradas en el pago a contacto, 14 en `queue` y 21 en la liberación. Con 500 registros, 2,97 M, 2,65 M y 4,46 M instrucciones, contra unos 62–64 M del registro anterior.
- [FACT] Despliegue (REAL, testnet, 13 oct): el wasm `6db027bc…` se subió en `057bfb48…1a35` y `TestnetProvisioner.HOLD_REGISTRY_WASM` apunta a él. La política y la cuenta no cambiaron.
  - Una cuenta nueva corrió el ciclo completo con ese registro ([`gateway-live.md`, corrida 7](evidence/stellar/gateway-live.md#corrida-7-registro-con-índice-b-2-13-oct)).
  - El teléfono (dueña) y el emulador (guardián) apuntan a la cuenta nueva `CDHRWCRP…` ([`two-devices-b2.md`](evidence/demo/two-devices-b2.md)).
- No corrido:
  - la lectura de los límites reales de testnet;
  - una transacción en testnet con cientos de registros.
- [FACT] Las cuentas creadas antes de la release B-2 siguen con el registro anterior y mantienen el límite, porque no tienen `upgrade`: `CB4PUYVY…`, `CA6VCI3O…`, `CA5Z7J…`, `CDWPPT…`, `CDBJMS…` y `CC6FFXGG…`.

<a id="f-03"></a>
### F-03 · La simulación libera retenciones a contactos que el contrato rechaza

| | |
| --- | --- |
| Categoría | Fidelidad de la simulación |
| Severidad | MEDIA |
| Estado | Por lectura de código durante la auditoría. **Corregido** junto con F-01 |
| Confianza | Alta |
| Dependencias | F-01 |

**Evidencia.** [FACT] `SimulatedStellarGateway.submitTransfer` (`app/shared/src/commonMain/kotlin/com/guardpay/shared/simulation/SimulatedStellarGateway.kt:110-124`) toma la rama de contacto si `total <= dailyCap`; si no, busca una retención madurada y la libera. El contrato, para un contacto, solo aplica el tope.

**Impacto.** La app en simulación (iOS en CI y Android sin `testnet.json`) muestra como enviable una retención a un contacto sobre el tope que en testnet fallaría. Las pruebas de UI y los XCUITest no detectan F-01.

**Recomendación.** [RECOMMENDATION] Alinear la simulación con el contrato después de decidir F-01, y añadir una prueba que compare ambos para ese caso.

**Corrección (12 oct 2026, rama `fix-f01`).** [FACT] `SimulatedStellarGateway.submitTransfer` y el modelo de pruebas `FakeStellarGateway` siguen ahora el mismo orden que el contrato. Primero buscan una retención activa para ese pago exacto: si no maduró, la rechazan; si maduró, la ejecutan sin gastar tope. Si no hay retención, aplican el tope solo a contactos. Pruebas nuevas: `SimulatedStellarGatewayTest` (4) y dos casos en `GatewayFlowTest`. [FACT] Los fakes conservan un vencimiento (`expiryWindowSeconds`) que el contrato no tiene (A-2 del roadmap).

<a id="f-04"></a>
### F-04 · Un solo guardián fijo, sin cambio ni recuperación

| | |
| --- | --- |
| Categoría | Diseño / disponibilidad |
| Severidad | MEDIA |
| Estado | IMPLEMENTADA Y VERIFICADA (por diseño); documentada en `README.md` |
| Confianza | Alta |

**Evidencia.** [FACT] El constructor de `hold_registry` guarda un guardián fijo; no existe función para cambiarlo. La cuenta no tiene `upgrade` ni administración. Un pago madurado no caduca en cadena.

**Impacto.** Si el guardián pierde su clave, no hay forma de cambiarlo. Si el guardián actúa de mala fe, puede detener todo pago a un destino nuevo indefinidamente; la dueña solo puede pagar a sus contactos dentro del tope. El guardián nunca puede mover fondos.

**Recomendación.** [RECOMMENDATION] Antes de cualquier uso con fondos reales, diseñar un procedimiento de cambio de guardián y de recuperación que no convierta al guardián en firmante.

<a id="f-05"></a>
### F-05 · Contratos sin auditoría profesional y OpenZeppelin fijado a un pre-release

| | |
| --- | --- |
| Categoría | Seguridad / dependencias |
| Severidad | MEDIA |
| Estado | Documentado en `README.md` y `docs/DEPENDENCIES.md` |
| Confianza | Alta |

**Evidencia.** [FACT] `vendor/` contiene OpenZeppelin `stellar-contracts` en el commit `b40c5ea`, previo a una versión estable. El diseño depende de que `context_rule_ids` entre en el digest firmado (cubierto por GH-31).

**Recomendación.** [RECOMMENDATION] Auditoría externa antes de mainnet; reevaluar el pin contra una versión estable de OpenZeppelin.

<a id="f-06"></a>
### F-06 · `queue` acepta otro token y rompe la lectura de retenciones en la app

| | |
| --- | --- |
| Categoría | Robustez / validación de entrada |
| Severidad | BAJA |
| Estado | Por lectura de código; no ejecutado |
| Confianza | Media |

**Evidencia.** [FACT] `try_enforce_queue` (`contracts/guardian_hold/src/lib.rs:221`) comprueba la cuenta y que el monto sea > 0, pero no el token. [FACT] `ChainDecoders.hold` lanza `"hold is in another token"` (`ChainDecoders.kt:87`), y esa excepción hace fallar toda la lectura `readHolds`.

**Impacto.** [INFERENCE] Una retención en otro token (que solo la dueña puede crear, firmando) deja las vistas de dueña y guardián sin poder leer el registro. No permite mover fondos: la política rechaza cualquier `transfer` de otro token.

**Recomendación.** [RECOMMENDATION] Exigir en `try_enforce_queue` que el token sea el configurado, o que el cliente ignore los registros de otro token en lugar de fallar.

<a id="f-07"></a>
### F-07 · Una transacción `FAILED` pierde el código de error del contrato

| | |
| --- | --- |
| Categoría | Observabilidad |
| Severidad | BAJA |
| Estado | PARCIALMENTE IMPLEMENTADA |
| Confianza | Alta |

**Evidencia.** [FACT] `SubmitClassifier.kt:46` tiene `TODO(day 4): read the contract error code from the result meta` y devuelve `Rejected(hash, null)`.

**Impacto.** La app no puede decirle a la dueña si el rechazo fue por tope, por retención no madurada o por destino. Combinado con F-01, un pago a contacto sobre el tope falla sin explicación. Con la corrección de F-01 aparece un motivo más: el pago tiene una retención que aún no maduró (`HoldNotReady`).

**Recomendación.** [RECOMMENDATION] Decodificar el error del contrato del result meta y mostrar un mensaje por código.

<a id="f-08"></a>
### F-08 · El archivo del modelo de IA se acepta solo por su tamaño si ya existe

| | |
| --- | --- |
| Categoría | Integridad de artefactos |
| Severidad | BAJA |
| Estado | Por lectura de código |
| Confianza | Alta |

**Evidencia.** [FACT] `GemmaModelFile.kt:30` y `:34`: si el archivo final ya mide `SIZE`, se da por bueno sin calcular el SHA-256. El SHA-256 se comprueba solo tras descargar (`:37`).

**Impacto.** [INFERENCE] Quien pueda escribir en el directorio externo de la app (acceso `adb` o con el teléfono desbloqueado) puede sustituir el modelo por otro del mismo tamaño. La IA no tiene autoridad financiera: no firma, no rellena destino ni monto, y la retención no depende de ella (GH y `evidence/ai/prompt-injection.md`). Un modelo sustituido solo podría dar avisos falsos o callar.

**Recomendación.** [RECOMMENDATION] Verificar el SHA-256 también al cargar un archivo existente (una vez por sesión o con una marca de verificación).

<a id="f-09"></a>
### F-09 · En debug, un archivo en almacenamiento externo decide el RPC y los contratos

| | |
| --- | --- |
| Categoría | Configuración |
| Severidad | BAJA |
| Estado | IMPLEMENTADA (solo en debug) |
| Confianza | Alta |

**Evidencia.** [FACT] `app/android/src/debug/Wiring.kt` lee `testnet.json` del directorio externo de la app. `TestnetConfig.parse` valida los formatos y `GuardPayNetwork` exige `https` y la red `TESTNET`, pero cualquier RPC `https` y cualquier contrato con el formato correcto se aceptan. El build de release no usa este archivo.

**Impacto.** [INFERENCE] Con acceso `adb`, alguien puede apuntar la app de debug a otros contratos. `AuthEntryGuard` y la firma de solo hashes de 32 bytes limitan lo que se puede firmar. Riesgo propio de un build de demostración.

**Recomendación.** [RECOMMENDATION] Antes de un build de release conectado, empaquetar la configuración en la app o firmarla, y no leerla de almacenamiento externo.

<a id="f-10"></a>
### F-10 · El CI no fija acciones por SHA ni compila el wasm

| | |
| --- | --- |
| Categoría | Cadena de suministro / CI |
| Severidad | BAJA |
| Estado | Por lectura de `.github/workflows/` |
| Confianza | Alta |

**Evidencia.** [FACT] Las acciones se usan por tag mayor (`@v4`, etc.). `ci.yml` no corre `stellar contract build`, `cargo clippy`, `cargo fmt --check` ni una auditoría de dependencias. Sí declara `permissions: contents: read`.

**Recomendación.** [RECOMMENDATION] Fijar acciones por SHA; compilar el wasm en CI y comparar su hash con el desplegado; añadir clippy y una auditoría de dependencias de Rust y Gradle.

<a id="f-11"></a>
### F-11 · No hay archivo de licencia

| | |
| --- | --- |
| Categoría | Legal / repositorio |
| Severidad | BAJA |
| Estado | NO ENCONTRADA |
| Confianza | Alta |

**Evidencia.** [FACT] No existe `LICENSE` en la raíz. El repositorio es público. El metadato `license = "MIT"` del workspace de Cargo viene de OpenZeppelin, cuyo código vendorizado conserva su propia licencia.

**Recomendación.** [RECOMMENDATION] El equipo debe elegir una licencia y añadirla. Este informe no elige una.

<a id="f-12"></a>
### F-12 · Contradicciones en la documentación

| | |
| --- | --- |
| Categoría | Documentación |
| Severidad | BAJA |
| Estado | Confirmado por lectura |
| Confianza | Alta |

**Evidencia.**

- [FACT] `README.md` listaba "la IA en el teléfono" entre lo que "no se demuestra", mientras la sección "Estado actual" la da por hecha con evidencia. **Corregido en esta rama.**
- [FACT] `README.md` decía que un monto de contacto sobre el tope "queda retenido"; ver F-01. **Matizado en esta rama.**
- [FACT] `docs/ESTADO-FASES.md`, fila P7: "Sin probar: descarga completa desde cero"; el párrafo posterior dice que se probó el 12 oct. No se editó (fuera del alcance de los seis documentos).
- [FACT] `docs/DEPENDENCIES.md` cita versiones antiguas (LiteRT-LM, cargo) y el Android SDK como no instalado.
- [FACT] El comentario de `GuardPayAI.kt` atribuye el aislamiento a `checkCommonMainArchitecture`; quien lo comprueba es `checkDependencyGraph`.
- [UNVERIFIED] `AGENTS.md` parece anterior al estado actual; no se revisó en detalle.

**Recomendación.** [RECOMMENDATION] Actualizar `ESTADO-FASES.md`, `DEPENDENCIES.md` y el comentario en un cambio separado.

<a id="f-13"></a>
### F-13 · `hold_registry` no tiene pruebas propias

| | |
| --- | --- |
| Categoría | Cobertura de pruebas |
| Severidad | INFORMATIVA |
| Estado | Confirmado (0 pruebas en el crate). **Corregido** el 13 oct (rama `registry-cost`): 10 pruebas propias |
| Confianza | Alta |

**Evidencia.** [FACT] En la auditoría, `cargo test -p hold_registry` corría 0 pruebas. El registro se ejercita a través de `contracts/guardian_hold/tests/must.rs` (35 pruebas en la auditoría; 39 desde la corrección de F-01).

**Recomendación.** [RECOMMENDATION] Pruebas unitarias directas de `queue`, `cancel`, `mark_executed` y del recorrido con muchos registros (ver F-02).

**Corrección (13 oct 2026).** [FACT] `contracts/hold_registry/src/tests.rs` tiene 10 pruebas, y las 10 pasan. Cubren:

- que `queue` calcula `ready_at` y numera las retenciones;
- que `queue` exige la autorización de la cuenta y rechaza montos ≤ 0;
- que un duplicado se rechaza mientras la retención siga activa;
- que solo el guardián cancela y solo la política marca `Executed`;
- que los estados terminales son definitivos;
- que un id inexistente da `HoldNotFound`;
- que el índice sigue a los registros, también entre cuentas;
- que `queue` y `find_retained` tocan las mismas entradas con 0 y con 60 retenciones terminadas.

<a id="f-14"></a>
### F-14 · El aislamiento de la IA se comprueba de forma textual

| | |
| --- | --- |
| Categoría | Arquitectura |
| Severidad | INFORMATIVA |
| Estado | IMPLEMENTADA Y VERIFICADA (la tarea corrió y pasó) |
| Confianza | Alta |

**Evidencia.** [FACT] `checkDependencyGraph` y `checkCommonMainArchitecture` son expresiones regulares sobre el texto del código (`app/shared/build.gradle.kts`). Un import con alias o un acceso indirecto podría no detectarse. [FACT] La interfaz `GuardPayAI` solo recibe texto y solo devuelve `SuggestHold | NoClearSignals`, lo que limita el riesgo por diseño.

<a id="f-15"></a>
### F-15 · El administrador del token de prueba es la dueña

Severidad INFORMATIVA · Documentado en `README.md` · Confianza Alta. [FACT] El SAC de prueba lo emite la dueña; `mint` y `clawback` no pasan por la política. Solo aplica a testnet; con USDC real el emisor sería Circle.

<a id="f-16"></a>
### F-16 · El diálogo de firma no muestra el pago

Severidad INFORMATIVA · Documentado en `README.md` y en `KeystoreSigner.kt` · Confianza Alta. [FACT] La clave firma un hash de 32 bytes; el sistema no puede mostrar destino ni monto. La mitigación es `AuthEntryGuard`, la retención en cadena y la vista del guardián.

<a id="f-17"></a>
### F-17 · El aviso al guardián depende de un servicio en primer plano

Severidad INFORMATIVA · Documentado en `README.md` · Confianza Alta. [FACT] `HoldWatchService` consulta cada 30 s (`INTERVAL_MS = 30_000`); no hay push. Si el sistema detiene el servicio, el aviso se retrasa hasta abrir la app.

<a id="f-18"></a>
### F-18 · El build de release no se conecta a nada

Severidad INFORMATIVA · Documentado en `README.md` · Confianza Alta. [FACT] `app/android/src/release/Wiring.kt` devuelve sesiones nulas.

<a id="f-19"></a>
### F-19 · No existe un canal para reportar vulnerabilidades

Severidad INFORMATIVA · Confianza Alta. [FACT] El repositorio no tenía `SECURITY.md` ni un canal privado declarado. Esta rama añade [`SECURITY.md`](SECURITY.md), que dice que el canal **debe establecerse**; no inventa uno.

## 4. Integración con Stellar, por etapa

| # | Etapa | Estado | Verificación de esta auditoría | Evidencia |
| --- | --- | --- | --- | --- |
| 1 | Interfaz simulada | SIMULADA (existe y funciona) | `OwnerSessionTest`, `GuardianSessionTest`, `GatewayFlowTest` y `ScreensAt360Test` corrieron y pasaron | `simulation/SimulatedStellarGateway.kt` |
| 2 | Integración del SDK | IMPLEMENTADA Y VERIFICADA (piezas, offline) | `ChainDecodersTest`, `AuthEntryGuardTest` y `SubmitClassifierTest` pasaron; usan tipos de stellar-sdk. `GatewayFlowTest` usa el modelo en memoria, no el SDK | `stellar/KmpStellarGateway.kt`, stellar-sdk 1.14.0 |
| 3 | Conexión a la red | IMPLEMENTADA, NO VERIFICADA por la app en esta auditoría | Solo se consultó Horizon testnet con `curl`; la app no se conectó | `GuardPayNetwork.kt` (https, `Network.TESTNET`) |
| 4 | Transacción construida | IMPLEMENTADA, NO VERIFICADA en esta auditoría | Solo `KmpStellarGatewayLiveTest` construye transacciones reales, y no se corrió (envía a testnet) | `KmpStellarGateway.kt`; evidencia previa en `evidence/stellar/gateway-live.md` |
| 5 | Transacción firmada | IMPLEMENTADA, NO VERIFICADA en esta auditoría | No se firmó nada. La firma real consta en evidencia previa (REAL) | `KeystoreSigner.kt`; `evidence/demo/scene-1.md` |
| 6 | Transacción enviada | IMPLEMENTADA Y VERIFICADA (por la cadena) | Los cinco hashes consultados existen en testnet | `evidence/demo/scene-1.md`, `scene-3.md`, `scene-4.md` |
| 7 | Transacción confirmada | IMPLEMENTADA Y VERIFICADA (por la cadena) | Horizon devuelve `successful: true` con el mismo ledger y hora que la evidencia | ver tabla siguiente |

Hashes consultados en `https://horizon-testnet.stellar.org/transactions/<hash>` el 12 oct 2026 (solo lectura):

| Hash (prefijo) | Archivo de evidencia | Ledger | Hora (UTC) | `successful` |
| --- | --- | --- | --- | --- |
| `01187e29…` | `evidence/demo/scene-1.md` | 5055445 | 2026-10-06T15:33:32Z | true |
| `b664e69d…` | `evidence/demo/scene-1.md` | 5055569 | 2026-10-06T15:43:52Z | true |
| `fb9eafad…` | `evidence/demo/scene-3.md` | 5055479 | 2026-10-06T15:36:22Z | true |
| `bb1f4dd7…` | `evidence/demo/scene-3.md` | 5055600 | 2026-10-06T15:46:27Z | true |
| `06565475…` | `evidence/demo/scene-4.md` | 5059691 | 2026-10-06T21:27:22Z | true |

Esto confirma que esas transacciones existen y se aplicaron. No confirma por sí solo qué hizo cada una dentro del contrato; eso consta en los archivos de evidencia y no se re-decodificó aquí.

## 5. Secretos e integridad del repositorio

- [FACT] Búsqueda de cadenas con forma de semilla Stellar (`S` + 55 caracteres base32), claves privadas PEM y tokens de GitHub, Hugging Face y AWS en los archivos versionados, en `vendor/` y en el historial (`git log --all -G`): **sin coincidencias**.
- [FACT] La palabra `secret_key` aparece dentro de `downloads/android-debug.apk`; por el contexto (nombres de constantes de librerías criptográficas) es un falso positivo.
- [FACT] `scripts/.testnet/` está en `.gitignore`; no está versionado y no existe en esta copia de trabajo.
- [FACT] `.gitignore` también excluye keystores, `.env`, `*.secret` y `/target/`.
- [FACT] El manifiesto Android declara `allowBackup="false"`; `HoldWatchService` no está exportado.
- [FACT] Las pruebas y los scripts imprimen solo direcciones públicas, según su código.
- [FACT] Estado del árbol al terminar: solo cambian los seis documentos de esta auditoría. Los archivos sin seguimiento del equipo (`guardpay-motion/`, `docs/GuardPay_Logo.png`, `docs/GuardPay_Eslogan.png`) no se tocaron.

## 6. Pruebas ejecutadas y no ejecutadas

Entorno: Windows 11, cargo 1.98.0, Gradle 8.14.3 (wrapper), JDK 17.

| Comando | Resultado observado | Nota |
| --- | --- | --- |
| `cargo test -p account -p guardian_hold -p hold_registry` | Salida 0. `account`: 1 pasó. `guardian_hold` (`must.rs`): 35 pasaron. `hold_registry`: 0 pruebas | REAL |
| `./gradlew :app:shared:allTests` | Salida 0, pero las tareas salieron de la caché (`UP-TO-DATE`/`FROM-CACHE`) | No cuenta como ejecución |
| `./gradlew :app:shared:jvmTest --rerun :app:shared:testDebugUnitTest --rerun :app:shared:testReleaseUnitTest --rerun` + las cuatro tareas `check*` con `--rerun` | `BUILD SUCCESSFUL`. jvmTest: 127 pruebas, 0 fallos. testDebugUnitTest: 113, 0 fallos. testReleaseUnitTest: 113, 0 fallos. Las cuatro comprobaciones corrieron y pasaron | REAL |
| `./gradlew :app:android:assembleDebug :app:android:compileReleaseKotlin` | Salida 0, desde la caché | Compila; no se recompiló desde cero |
| Reproducción F-01 en copia aislada: `cargo test --locked -p guardian_hold --test must audit_repro -- --nocapture` | 2 pasaron, 0 fallaron | REAL; las pruebas añadidas no están en el repositorio |
| Corrección F-01 (rama `fix-f01`): `cargo test --locked -p account -p guardian_hold -p hold_registry` | Salida 0. `account`: 1. `guardian_hold`: 39 (35 + 4 `f01_*`). `hold_registry`: 0 | REAL, 12 oct 2026 |
| B-2 (rama `registry-cost`): `cargo test --locked -p account -p guardian_hold -p hold_registry` | Salida 0. `account`: 1. `guardian_hold`: 39. `hold_registry`: 10. `registry_cost`: 1 ignorada (opt-in) | REAL, 13 oct 2026 |
| B-2: `registry_cost` con el wasm desplegado y con `GP_REGISTRY=source` | 33 filas cada una; ver `evidence/stellar/registry-cost.md` | REAL, entorno local, 13 oct 2026 |
| B-2: `registry_cost` con el wasm del registro nuevo (CI 38435932911) | 33 filas, todas dentro de los límites; entradas fijas entre 0 y 500 registros | REAL, entorno local, 13 oct 2026 |
| B-2: `RegistryReleaseLiveTest`, `KmpStellarGatewayLiveTest` y `DeviceProvisioningLiveTest` con `-PliveTestnet` | Upload del registro, corrida 7 (1 test, 0 fallos) y cuenta de los dispositivos; ver `deployment.md`, `gateway-live.md` y `two-devices-b2.md` | REAL, testnet, 13 oct 2026 |
| Corrección F-01: `./gradlew :app:shared:jvmTest --rerun`, `:app:shared:testDebugUnitTest --rerun`, `:app:shared:allTests`, `:app:android:assembleDebug :app:android:compileReleaseKotlin` | Todo `BUILD SUCCESSFUL`. jvmTest: 133, 0 fallos. testDebugUnitTest: 119, 0 fallos. Las cuatro comprobaciones pasaron | REAL, 12 oct 2026; `testReleaseUnitTest` no corrido |
| Consulta de 5 hashes en Horizon testnet | 5 existen, `successful: true` | REAL, solo lectura |

**No ejecutadas** (no corrido):

| Prueba | Motivo |
| --- | --- |
| `*LiveTest` con `-PliveTestnet` | Envían transacciones a testnet; prohibido en esta auditoría |
| Pruebas instrumentadas del Keystore | Requieren un dispositivo y firmar |
| `GemmaOnDeviceTest` (IA en el teléfono) | Requiere el teléfono y el modelo de 2,6 GB |
| Emulador, ensayos de demo (`rehearse.py`, `full.py`) | Requieren dispositivo y envían transacciones |
| Scripts de ataque (`scripts/attacks/`) | Envían transacciones |
| `stellar contract build` | No se pidió instalar ni cambiar la cadena de herramientas; no se ejecutó |
| iOS (`iosApp/`, XCUITest) | No hay Mac. El workflow `ios.yml` pasó en `d3cdb51` según GitHub Actions |
| GH-29, GH-30, GH-33 | Sin correr en el repositorio, y tampoco aquí |

## 7. Controles que funcionan

Comprobados por lectura de código y, cuando se indica, por pruebas que corrieron en esta auditoría:

- `account` tiene una sola regla `Default` y no expone `execute`, `upgrade` ni administración (código; GH-01…GH-33 que corren).
- La política rechaza llamadas a la propia cuenta, `approve`, `transfer_from`, otros tokens y sub-invocaciones (35 pruebas de `must.rs` pasaron).
- `ready_at` lo calcula el registro; `cancel` es solo del guardián; `mark_executed` solo de la política.
- El cliente firma solo la llamada que construyó: `AuthEntryGuard` rechaza sub-invocaciones y otras cuentas (`AuthEntryGuardTest` pasó).
- Tope de comisión de 2 XLM por transacción, validez de 60 s calculada con la hora del ledger, red fija `TESTNET`, RPC solo `https`.
- El guardián ve solo `GuardianChain` (lectura y `cancel`); `checkGuardianSurface` pasó.
- Ningún literal de interfaz dice "seguro", "protegido" ni "verificado"; `checkUiVocabulary` pasó.

## 8. Matriz de trazabilidad

| ID | Área | Funcionalidad o afirmación | Estado | Evidencia | Prueba ejecutada | Limitaciones |
| --- | --- | --- | --- | --- | --- | --- |
| T-01 | Contrato | La cuenta tiene una sola regla `Default` (dueña + política) | IMPLEMENTADA Y VERIFICADA | `contracts/account/src/lib.rs`; `evidence/smart-account/rules-onchain.md` | `cargo test` (GH) | Lectura en cadena no repetida aquí |
| T-02 | Contrato | Sin `execute`, `upgrade` ni administración | IMPLEMENTADA Y VERIFICADA | `contracts/account/src/lib.rs` | `cargo test` (GH) | — |
| T-03 | Contrato | Pago inmediato a contacto dentro del tope diario | IMPLEMENTADA Y VERIFICADA | `guardian_hold/src/lib.rs:205-220` (187-202 antes de `fix-f01`); `evidence/demo/scene-1.md` | `cargo test` (`gh_14_transfer_lanes`); Horizon | — |
| T-04 | Contrato | Destino nuevo exige retención madurada | IMPLEMENTADA Y VERIFICADA | `guardian_hold/src/lib.rs:187-203` (205-217 antes de `fix-f01`) | `cargo test` | — |
| T-05 | Contrato | Monto de contacto sobre el tope queda retenido y luego sale | IMPLEMENTADA Y VERIFICADA en el código (`fix-f01`) | F-01 | `f01_matured_trusted_hold_over_cap_releases`; `KmpStellarGatewayLiveTest` (corrida 6) | Probado en testnet solo con una cuenta desechable |
| T-06 | Contrato | Retención a contacto dentro del tope se marca ejecutada | IMPLEMENTADA Y VERIFICADA en el código (`fix-f01`) | F-01 | `f01_matured_trusted_hold_within_cap_is_executed_and_closed` | Política desplegada en testnet el 12 oct; este caso no se corrió en testnet |
| T-07 | Contrato | Solo el guardián cancela | IMPLEMENTADA Y VERIFICADA | `hold_registry/src/lib.rs`; `evidence/demo/scene-3.md` | `cargo test`; Horizon | — |
| T-08 | Contrato | Un pago detenido no sale | IMPLEMENTADA Y VERIFICADA | `evidence/demo/scene-4.md` | `cargo test`; Horizon (cola) | El `transfer` fallido no se consultó |
| T-09 | Contrato | `ready_at` lo calcula el registro (120 s) | IMPLEMENTADA Y VERIFICADA | `hold_registry/src/lib.rs:12`, `:124` | `cargo test` | — |
| T-10 | Contrato | El guardián no puede mover fondos | IMPLEMENTADA Y VERIFICADA | `evidence/demo/scene-5.md`; código | `cargo test` | — |
| T-11 | Contrato | Escala del registro con muchas retenciones | IMPLEMENTADA Y VERIFICADA (código y testnet, release B-2) | F-02; `evidence/stellar/registry-cost.md` | `registry_cost` (opt-in); `lookups_do_not_grow_with_history`; corrida 7 | Las cuentas anteriores a B-2 siguen recorriendo; sin transacción en testnet con cientos de registros |
| T-12 | Contrato | Cambio de guardián / recuperación | NO ENCONTRADA | F-04 | — | Documentado como limitación |
| T-13 | Cliente | 9 estados de pago y transiciones | IMPLEMENTADA Y VERIFICADA | `domain/` | `PaymentStateTest` | — |
| T-14 | Cliente | Predicción de carril | IMPLEMENTADA Y VERIFICADA | `domain/Lane.kt` | `LaneTest` | Informativa; el contrato decide |
| T-15 | Cliente | Firma solo de la llamada construida | IMPLEMENTADA Y VERIFICADA | `AuthEntryGuard.kt` | `AuthEntryGuardTest` | — |
| T-16 | Cliente | Construcción y simulación de transacciones | IMPLEMENTADA, NO VERIFICADA en esta auditoría | `KmpStellarGateway.kt`; `KmpStellarGatewayLiveTest` | Piezas: `AuthEntryGuardTest`, `ChainDecodersTest` | La prueba de extremo a extremo envía a testnet; no corrida |
| T-17 | Cliente | Código de error del contrato en un rechazo | PARCIALMENTE IMPLEMENTADA | F-07 | `SubmitClassifierTest` | TODO en el código |
| T-18 | Cliente | Simulación en memoria fiel al contrato | IMPLEMENTADA Y VERIFICADA para F-01 (`fix-f01`) | F-03 | `SimulatedStellarGatewayTest`, `GatewayFlowTest` | Los fakes tienen un vencimiento que el contrato no tiene |
| T-19 | Android | Clave ed25519 en Keystore con huella o PIN | IMPLEMENTADA, NO VERIFICADA en esta auditoría | `KeystoreSigner.kt`; evidencia previa | No corrida (instrumentada) | Requiere dispositivo |
| T-20 | Android | Aviso local al guardián sin servidor | IMPLEMENTADA, NO VERIFICADA en esta auditoría | `HoldWatchService.kt`; `evidence/guardian/notification.md` | `HoldWatcherTest` (lógica) | Servicio no ejecutado aquí |
| T-21 | Android | Build de release conectado | PLANIFICADA | `src/release/Wiring.kt` | `compileReleaseKotlin` (caché) | Sesiones nulas |
| T-22 | IA | Gemma 4 E2B en el teléfono sugiere retener | IMPLEMENTADA, NO VERIFICADA en esta auditoría | `AndroidGuardPayAI.kt`; `evidence/ai/benchmark.md` | `AnalysisParserTest` (parser) | Modelo no corrido aquí |
| T-23 | IA | La IA no influye en el carril ni en la firma | IMPLEMENTADA Y VERIFICADA (estructura) | `GuardPayAI.kt`; `evidence/ai/prompt-injection.md` | `checkDependencyGraph` | Comprobación textual (F-14) |
| T-24 | IA | Descarga del modelo con SHA-256 | PARCIALMENTE IMPLEMENTADA | F-08 | — | Archivo existente no se re-verifica |
| T-25 | IA | IA en iOS | NO ENCONTRADA | `IosGuardPayAI` siempre no disponible | — | Por diseño |
| T-26 | iOS | App en simulador con simulación | SIMULADA | `iosApp/`; `evidence/ios/simulator.md` | No corrida aquí (sin Mac) | CI `ios.yml` en verde en `d3cdb51` |
| T-27 | UI | Sin "seguro"/"protegido"/"verificado" | IMPLEMENTADA Y VERIFICADA | `checkUiVocabulary` | Corrida y pasó | Solo literales |
| T-28 | UI | Pantallas a 360 dp | IMPLEMENTADA Y VERIFICADA | `evidence/demo/screens/` | `ScreensAt360Test` | Render sin dispositivo |
| T-29 | Repositorio | Sin secretos versionados | IMPLEMENTADA Y VERIFICADA | Sección 5 | Escaneo | Patrones conocidos únicamente |
| T-30 | Repositorio | Licencia | NO ENCONTRADA | F-11 | — | — |
| T-31 | Repositorio | Canal de reporte de vulnerabilidades | NO ENCONTRADA | F-19 | — | `SECURITY.md` pide establecerlo |
| T-32 | CI | Contratos y app en CI sin red | IMPLEMENTADA Y VERIFICADA | `.github/workflows/ci.yml`; ejecuciones en `main` | Lectura de resultados de Actions | No compila el wasm (F-10) |
