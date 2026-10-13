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
| [F-01](#f-01) | ALTA | Un pago retenido a un contacto de confianza nunca se libera por el registro | Confirmado en una copia aislada |
| [F-02](#f-02) | ALTA | El registro recorre todos los registros de la cuenta en cada `queue` y cada liberación | Por lectura de código; umbral sin medir |
| [F-03](#f-03) | MEDIA | La simulación libera retenciones a contactos que el contrato rechaza | Por lectura de código |
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
| Estado | Confirmado en una copia aislada de los contratos (prueba local, entorno de pruebas de Soroban). No reproducido en testnet. |
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

<a id="f-02"></a>
### F-02 · El registro recorre todos los registros de la cuenta en cada `queue` y cada liberación

| | |
| --- | --- |
| Categoría | Disponibilidad / límites de recursos |
| Severidad | ALTA |
| Estado | IMPLEMENTADA, NO VERIFICADA (el umbral no se midió) |
| Confianza | Media |
| Dependencias | Riesgo R6 del plan (recursos dentro de `__check_auth`) |

**Evidencia.**

- [FACT] `find_retained_id` (usado por `queue` para rechazar duplicados) y `find_retained` (usado por `enforce` en cada liberación) recorren `0..next_id` con una lectura `persistent` por registro (`contracts/hold_registry/src/lib.rs:191` y `:234`). `list_retained` también.
- [FACT] Los registros nunca se borran: `Stopped` y `Executed` siguen contando en el recorrido.
- [FACT] La cuenta no tiene `upgrade`, y el registro tampoco.

**Impacto.** [INFERENCE] Cada retención creada aumenta el costo de todas las siguientes. Con suficientes registros, `queue` y la liberación de retenciones superarían los límites de lecturas o instrucciones por transacción de Soroban, y la cuenta ya no podría pagar a destinos nuevos. Los pagos a contactos dentro del tope no consultan el registro y seguirían funcionando. [UNVERIFIED] El número de registros que lo dispara y el efecto de las entradas archivadas no se midieron (INDETERMINADA).

**Recomendación.** [RECOMMENDATION] Medir el costo de `queue` y de una liberación con 10, 50, 100 y 200 registros en una prueba local o en testnet. Si el límite es alcanzable en un uso normal, indexar las retenciones activas por `(token, destino, monto)` en lugar de recorrer.

<a id="f-03"></a>
### F-03 · La simulación libera retenciones a contactos que el contrato rechaza

| | |
| --- | --- |
| Categoría | Fidelidad de la simulación |
| Severidad | MEDIA |
| Estado | Por lectura de código; no ejecutado |
| Confianza | Alta |
| Dependencias | F-01 |

**Evidencia.** [FACT] `SimulatedStellarGateway.submitTransfer` (`app/shared/src/commonMain/kotlin/com/guardpay/shared/simulation/SimulatedStellarGateway.kt:110-124`) toma la rama de contacto si `total <= dailyCap`; si no, busca una retención madurada y la libera. El contrato, para un contacto, solo aplica el tope.

**Impacto.** La app en simulación (iOS en CI y Android sin `testnet.json`) muestra como enviable una retención a un contacto sobre el tope que en testnet fallaría. Las pruebas de UI y los XCUITest no detectan F-01.

**Recomendación.** [RECOMMENDATION] Alinear la simulación con el contrato después de decidir F-01, y añadir una prueba que compare ambos para ese caso.

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

**Impacto.** La app no puede decirle a la dueña si el rechazo fue por tope, por retención no madurada o por destino. Combinado con F-01, un pago a contacto sobre el tope falla sin explicación.

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
| Estado | Confirmado (0 pruebas en el crate) |
| Confianza | Alta |

**Evidencia.** [FACT] `cargo test -p hold_registry` corre 0 pruebas. El registro se ejercita a través de `contracts/guardian_hold/tests/must.rs` (35 pruebas).

**Recomendación.** [RECOMMENDATION] Pruebas unitarias directas de `queue`, `cancel`, `mark_executed` y del recorrido con muchos registros (ver F-02).

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
| T-03 | Contrato | Pago inmediato a contacto dentro del tope diario | IMPLEMENTADA Y VERIFICADA | `guardian_hold/src/lib.rs:187-202`; `evidence/demo/scene-1.md` | `cargo test` (`gh_14_transfer_lanes`); Horizon | — |
| T-04 | Contrato | Destino nuevo exige retención madurada | IMPLEMENTADA Y VERIFICADA | `guardian_hold/src/lib.rs:205-217` | `cargo test` | — |
| T-05 | Contrato | Monto de contacto sobre el tope queda retenido y luego sale | PARCIALMENTE IMPLEMENTADA | F-01 | Reproducción aislada (falla con `CapExceeded`) | Contradice el README anterior |
| T-06 | Contrato | Retención a contacto dentro del tope se marca ejecutada | NO ENCONTRADA | F-01 | Reproducción aislada (queda `Retained`) | — |
| T-07 | Contrato | Solo el guardián cancela | IMPLEMENTADA Y VERIFICADA | `hold_registry/src/lib.rs`; `evidence/demo/scene-3.md` | `cargo test`; Horizon | — |
| T-08 | Contrato | Un pago detenido no sale | IMPLEMENTADA Y VERIFICADA | `evidence/demo/scene-4.md` | `cargo test`; Horizon (cola) | El `transfer` fallido no se consultó |
| T-09 | Contrato | `ready_at` lo calcula el registro (120 s) | IMPLEMENTADA Y VERIFICADA | `hold_registry/src/lib.rs:12`, `:124` | `cargo test` | — |
| T-10 | Contrato | El guardián no puede mover fondos | IMPLEMENTADA Y VERIFICADA | `evidence/demo/scene-5.md`; código | `cargo test` | — |
| T-11 | Contrato | Escala del registro con muchas retenciones | INDETERMINADA | F-02 | Ninguna | Sin medir |
| T-12 | Contrato | Cambio de guardián / recuperación | NO ENCONTRADA | F-04 | — | Documentado como limitación |
| T-13 | Cliente | 9 estados de pago y transiciones | IMPLEMENTADA Y VERIFICADA | `domain/` | `PaymentStateTest` | — |
| T-14 | Cliente | Predicción de carril | IMPLEMENTADA Y VERIFICADA | `domain/Lane.kt` | `LaneTest` | Informativa; el contrato decide |
| T-15 | Cliente | Firma solo de la llamada construida | IMPLEMENTADA Y VERIFICADA | `AuthEntryGuard.kt` | `AuthEntryGuardTest` | — |
| T-16 | Cliente | Construcción y simulación de transacciones | IMPLEMENTADA, NO VERIFICADA en esta auditoría | `KmpStellarGateway.kt`; `KmpStellarGatewayLiveTest` | Piezas: `AuthEntryGuardTest`, `ChainDecodersTest` | La prueba de extremo a extremo envía a testnet; no corrida |
| T-17 | Cliente | Código de error del contrato en un rechazo | PARCIALMENTE IMPLEMENTADA | F-07 | `SubmitClassifierTest` | TODO en el código |
| T-18 | Cliente | Simulación en memoria fiel al contrato | PARCIALMENTE IMPLEMENTADA | F-03 | `OwnerSessionTest` | Diverge en retenciones a contactos sobre el tope |
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
