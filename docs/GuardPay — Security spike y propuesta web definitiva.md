# GuardPay — Security spike y propuesta web definitiva

Oct 5, 2026 · @Justin

## 1. Veredicto y respuestas cortas

**🟡 GO WITH CHANGES.** La respuesta a la pregunta central es **sí, pero solo con cuatro cambios de arquitectura que el documento anterior no tenía, y hoy está 🔴 NO VERIFICADO por tests.** Con la configuración que sugiere el ejemplo oficial de OpenZeppelin, GuardianHold es decorativo: se puede saltar por tres caminos distintos.

**Lo que encontró la auditoría en el código de OpenZeppelin** (repo `OpenZeppelin/stellar-contracts`, commit `b40c5ea` del 26 sep 2026):

1. 🟢 **El cliente elige la regla, pero no puede cambiarla después de firmar.** `context_rule_ids` entra en el hash que firma la dueña: `auth_digest = sha256(signature_payload || context_rule_ids.to_xdr())` (`smart_account/storage.rs`, `do_check_auth`). Existe un test que lo prueba: `do_check_auth_rule_selection_downgrade_fails`. Pero si existe *cualquier* otra regla que autorice un `transfer` sin GuardianHold, una app comprometida puede pedirle a la dueña que firme con esa regla y la passkey no muestra cuál es.
2. 🟢 **El ejemplo oficial crea justo esa otra regla.** El constructor de `multisig-smart-account` crea una regla `Default` con los firmantes que se le pasen. Si la dueña está en ella sin GuardianHold, cualquier pago sale sin retención.
3. 🟢 **El ejemplo trae `execute` y `upgrade`.** `execute(target, fn, args)` hace que la cuenta llame a cualquier contrato; como en Soroban una llamada directa de un contrato se considera autorizada por él (🟢 docs de autorización de Stellar), `execute(USDC, "transfer", …)` movería fondos sin que el token pida firma. `upgrade` cambiaría el código de la cuenta.
4. 🟢 **Una política no puede "registrar la retención y rechazar" a la vez.** `enforce` rechaza con *panic*, y un panic revierte todo lo que la transacción escribió. El documento anterior nunca dijo cómo nace el registro `HELD`; la forma intuitiva (el primer intento lo crea) es imposible. Hace falta una operación separada para crear el pago retenido.
5. 🟢 **Con políticas, nadie más comprueba la firma de la dueña.** `get_validated_context_by_id` solo exige todas las firmas si la regla no tiene políticas; con políticas, la decisión la toma `enforce`. Si la transacción no trae ninguna firma, el bucle de autenticación no corre y la política recibe una lista vacía. Si GuardianHold no exige a la dueña, cualquiera podría enviar.

**Los cuatro cambios obligatorios** (detalle en la sección 4):

- **Una cuenta propia, no el ejemplo:** una sola regla `Default` con la dueña como única firmante y GuardianHold como política; sin `execute` ni `upgrade`.
- **GuardianHold rechaza todo lo que no sea un `transfer` de USDC** o una llamada a las dos funciones de GuardPay. Eso incluye cualquier llamada a la propia cuenta, de modo que la configuración queda congelada para siempre.
- **GuardianHold exige la firma de la dueña** en `enforce` y exige que lo llame su propia cuenta (`smart_account.require_auth()`, el mismo patrón que Spending Limit).
- **Dos contratos:** GuardianHold (la política) y un registro de pagos retenidos con `queue` y `cancel`. Son dos porque Soroban no permite reentrar en un contrato (🟡 blog técnico de Stellar).

**Hallazgo competitivo que cambia el pitch:** el mecanismo ya existe. Yandex Pay permite que un contacto de confianza apruebe o rechace transferencias grandes sin poder mover el dinero (🟡 prensa; fecha de lanzamiento sin verificar). `TimelockController` de OpenZeppelin tiene un rol que solo cancela operaciones identificadas por el hash de sus parámetros (🟢 docs OZ). GuardPay no es una idea nueva; es ese patrón llevado a una billetera con passkey en Stellar, con la regla en el contrato y no en el servidor de un banco.

### Respuestas a las 26 preguntas

**Seguridad** (todas condicionadas a los cuatro cambios y a que pasen los tests de la sección 7)

| # | Pregunta | Respuesta |
| --- | --- | --- |
| 1 | ¿Quién puede mover fondos? | Solo una transacción que lleve la firma de la dueña y que GuardianHold acepte: un pago a un contacto de confianza dentro del tope, o un pago retenido que ya maduró, no fue detenido y coincide en todo. 🔵 deducido del código 🟢; 🔴 sin test. |
| 2 | ¿Quién crea un payment intent? | Solo la dueña: `queue` exige la autorización de la cuenta, que pasa por GuardianHold, y GuardianHold exige su firma. |
| 3 | ¿Quién lo cancela? | El guardián (con su propia firma, fuera de la cuenta) y la dueña (SHOULD). |
| 4 | ¿Quién lo ejecuta? | La dueña, con una segunda firma sobre el `transfer` exacto. La comisión la puede pagar un patrocinador, que no puede alterar lo firmado. |
| 5 | ¿Antes de `ready_at`? | `enforce` rechaza y la transacción completa se revierte. |
| 6 | ¿Después? | Sale si la dueña firma, el pago coincide, no fue detenido y no venció (SHOULD). No sale solo. |
| 7 | ¿Después de cancelar? | Ningún `transfer` con esos parámetros puede usar ese registro; para pagar habría que crear uno nuevo y esperar otra vez. |
| 8 | ¿Puede el guardián gastar? | No. No es firmante de la cuenta (`do_check_auth` rechaza firmantes ajenos con `UnauthorizedSigner` 🟢) y el registro no tiene ninguna función que toque tokens. |
| 9 | ¿Puede la IA gastar? | No. No firma y el contrato no la consulta. |
| 10 | ¿Puede el backend gastar? | No por sí solo. Puede callar avisos, mentir en pantallas que no lean la cadena y negarse a enviar transacciones. |
| 11 | ¿Una UI comprometida salta las reglas? | No las reglas; sí a la persona. Puede hacer que la dueña firme algo distinto de lo que ve. Ese pago queda retenido y su destino real se lee de la cadena en la vista del guardián. |
| 12 | ¿`approve`? | No, si GuardianHold rechaza cualquier función que no sea `transfer`. |
| 13 | ¿`transfer_from`? | Lo autoriza quien gasta, no la cuenta, y solo con un permiso previo. Sin `approve` posible, no hay permiso que usar. |
| 14 | ¿Otra context rule? | Sí con el ejemplo oficial; no con una cuenta de regla única que GuardianHold congela. |
| 15 | ¿Se puede modificar el intent? | No hay función que lo modifique; cualquier diferencia en origen, token, destino o monto no coincide con el registro. |

**Producto**

| # | Pregunta | Respuesta |
| --- | --- | --- |
| 16 | ¿Por qué lo necesita un usuario? | En las estafas de pago autorizado, la víctima firma. Un control que solo depende de que la víctima dude falla justo ahí. 🔵 Hay que validarlo con entrevistas. |
| 17 | ¿Por qué un guardián? | Es alguien que no está bajo la presión del estafador. Yandex Pay apostó por lo mismo 🟡. |
| 18 | ¿Por qué no multisig? | Multisig obliga a aprobar cada pago y le da al segundo firmante poder sobre los fondos. Aquí el guardián solo puede frenar, y los pagos a conocidos no lo esperan. |
| 19 | ¿Por qué no solo espera + límite? | Una espera sin nadie que mire depende de que la víctima reaccione sola. El guardián es lo que convierte la espera en una oportunidad real de detenerlo. |
| 20 | ¿Qué aporta la IA? | Explica en lenguaje simple las señales del mensaje que presiona a la dueña y puede sugerir retener un pago. No da garantías. |
| 21 | ¿Qué aporta Stellar? | USDC como contrato SAC, cuentas con passkey y políticas listas en OpenZeppelin, y comisiones bajas. Es un "uso significativo", no algo exclusivo de Stellar. |

**Hackathon**

| # | Pregunta | Respuesta |
| --- | --- | --- |
| 22 | ¿Qué se puede demostrar? | Transacciones rechazadas en el explorador: envío anticipado, envío detenido, guardián que intenta transferir, `approve` y destino alterado. |
| 23 | ¿Qué es diferenciador? | La regla está en el contrato: ni el operador puede saltarla. Esa es la diferencia frente a Yandex o Monzo. |
| 24 | ¿Qué es vulnerable a crítica? | "Ya existe" (Yandex, Timelock, Argent), que el guardián no se entere, y "un contrato sin auditar". |
| 25 | ¿Cuál es el demo killer? | El ataque en vivo desde la terminal, sin la app, rechazado por el contrato. |
| 26 | ¿Cuál es el mayor riesgo? | Técnico: que el flujo `queue` con la passkey y el hash de OZ no funcione con las librerías actuales. De producto: que el guardián no se entere a tiempo. |

## 2. Auditoría del documento base

Se audita "GuardPay — Propuesta de la app web". Lo que el documento acertó se mantiene: una sola app, la web no protege nada, el estado se lee del contrato, la segunda firma, el guardián sin botón de aprobar y el vocabulario prohibido.

**Afirmaciones del documento base, reclasificadas**

| Afirmación | Etiqueta correcta | Por qué |
| --- | --- | --- |
| "Con políticas, OZ delega la validación de firmantes a las políticas" | 🟢 | `get_validated_context_by_id`, confirmado de nuevo en el commit actual. |
| "GuardianHold acepta solo transfer; no existe otra regla que autorice transfer" | 🔴 NO VERIFICADO | Es un requisito de diseño, no un hecho. El ejemplo oficial lo incumple. |
| "Configuración fija al desplegar" | 🔴 NO VERIFICADO | Con `add_context_rule`, `remove_policy` y `upgrade` disponibles, "fija" depende de que nadie pueda autorizar llamadas a la propia cuenta. |
| "Registro HELD … Dueña (firma)" | 🔴 Incompleto | No dice qué transacción lo crea. No puede ser el intento rechazado, porque el rechazo revierte la escritura. |
| "Identificador = hash de token + destino + monto" | 🟡 Insuficiente | Falta la cuenta de origen. Tampoco resuelve dos pagos iguales al mismo destino. |
| "Ninguna capa puede saltarse la regla" | 🔵 | Es una consecuencia de los cambios de la sección 4, no algo que ya esté demostrado. |
| "Quedará retenido 6 h" | 🔴 Sin evidencia | Ningún dato justifica 6 h (sección 5). |

**Contradicciones internas**

- Dice "configuración fija" (MUST) y a la vez propone "regla 2-de-2 para editar contactos" (NICE). Si se pueden editar, no es fija, y la ruta de edición es una ruta de bypass.
- Pone el aviso al guardián como NICE, pero la propuesta de valor ("Diego puede detenerlo") depende de que Diego se entere.
- Dice "La IA puede llevar un pago al carril retenido", pero no existe ninguna operación para retener a propósito un pago a un contacto de confianza. Hace falta `queue`, que es la misma operación que falta para crear el registro.

**Cambios** (decisión actual → problema → evidencia → nueva decisión → impacto)

| Decisión actual | Problema | Evidencia | Nueva decisión | Impacto |
| --- | --- | --- | --- | --- |
| Smart account del ejemplo de OZ | Regla `Default` sin GuardianHold, más `execute` y `upgrade`: tres bypass | 🟢 `examples/multisig-smart-account/account/src/contract.rs` | Cuenta propia: una sola regla, sin `execute` ni `upgrade` | Crítico: sin esto GuardianHold es decorativo |
| El registro HELD "aparece" | `enforce` no puede escribir y rechazar en la misma transacción | 🟢 Un panic revierte la transacción; `enforce` rechaza con panic (policies) | Operación `queue` firmada por la dueña en un contrato registro | Una transacción más en el flujo; es la misma firma que ya se mostraba como "Retener" |
| GuardianHold y el registro en un solo contrato | `queue` pide autorización a la cuenta → `enforce` → mismo contrato: reentrada | 🟡 Soroban no permite reentrada (blog técnico de Stellar) | Dos contratos: política y registro | +1 contrato; 🔴 la alternativa con un solo contrato vía `execute` reabre el bypass |
| "GuardianHold verifica la firma de la dueña" | Correcto, pero falta exigir que solo su cuenta pueda llamar a `enforce` | 🟢 Spending Limit hace `smart_account.require_auth()` en `enforce` | Las dos verificaciones son MUST | Sin la segunda, cualquiera puede llamar a `enforce` para alterar contadores o registros |
| Clave = hash(token, destino, monto) | Choque entre cuentas y entre pagos repetidos | 🔵 | Registro por cuenta; un solo pago activo por (destino, monto) | Regla de UX: "ya tienes un pago igual en espera" |
| Aviso al guardián NICE | El veto sin aviso no protege | 🔵 | Aviso mínimo al guardián: MUST | Depende del backend; queda como riesgo residual |
| Editar contactos con 2-de-2 (NICE) | Es una ruta de cambio de configuración | 🔵 | DO NOT BUILD en el MVP; FUTURE con espera | Menos superficie |
| Hold de 6 h | Sin evidencia | 🔴 | Parámetro fijo por cuenta; minutos en la demo | Mensaje honesto en el pitch |
| Competidor más cercano: Argent | Hay competidores más cercanos | 🟡 Yandex Pay; 🟢 OZ Timelock CANCELLER | Rehacer el posicionamiento (sección 6) | Quita cualquier claim de novedad |

**Riesgos que el documento base no vio**

- **Firma a ciegas.** La passkey firma un hash; no muestra destino ni regla. Todo lo que la dueña "ve" lo pone la app.
- **Desalineación de librerías.** La firma debe cubrir `sha256(payload ‖ context_rule_ids)`. Si el kit de passkeys que se use firma solo el payload, nada funcionará. 🔴 NO VERIFICADO con `smart-account-kit`.
- **Destino "muxed".** En el estándar actual, el `to` de `transfer` es `MuxedAddress` (🟢 `tokens/fungible/mod.rs`). Hay que decidir si se aceptan direcciones muxed o se rechazan.
- **Archivado de datos.** Si el registro o el contador viven en almacenamiento temporal y expiran, el tope se reinicia o el pago desaparece. Debe usarse almacenamiento persistente.
- **Guardián que bloquea todo.** Sin forma de cambiar de guardián, uno malicioso puede detener todos los pagos a desconocidos para siempre.

## 3. Auditoría técnica: cómo autoriza una smart account de OZ

Todo lo marcado 🟢 sale del código fuente de `OpenZeppelin/stellar-contracts` (commit `b40c5ea`, 26 sep 2026, soroban-sdk 28) o de la documentación oficial de Stellar. Antes de construir, hay que fijar esa versión: el enlace de `context_rule_ids` al hash puede no existir en versiones anteriores (🔴 no se revisó el historial).

### Recorrido de un `transfer` de USDC desde la cuenta

1. La app arma `USDC.transfer(cuenta, destino, monto)`. El token llama a `cuenta.require_auth()`.
2. 🟢 El host de Soroban verifica la expiración de la firma y consume un *nonce* único. Luego llama a `__check_auth` de la cuenta con el `signature_payload` (hash de la invocación autorizada), el `AuthPayload` y la lista de contextos (docs de autorización de Stellar).
3. 🟢 `do_check_auth` exige un `context_rule_id` por contexto, valida cada contexto contra la regla elegida (vigencia, tipo `Default` o `CallContract(contrato)`) y calcula `auth_digest = sha256(signature_payload ‖ xdr(context_rule_ids))`.
4. 🟢 Rechaza cualquier firmante que no pertenezca a alguna regla elegida (`UnauthorizedSigner`) y verifica cada firma contra `auth_digest`. Si una falla, todo se rechaza.
5. 🟢 Llama a `enforce(contexto, firmantes_autenticados, regla, cuenta)` de cada política de la regla. Si una hace panic, se revierte la transacción entera.
6. Si todo pasa, el token mueve el saldo.

### Respuestas puntuales

| Tema | Hallazgo | Etiqueta |
| --- | --- | --- |
| `context_rule_ids` | Los elige el cliente, sin auto-descubrimiento. Están dentro del hash firmado, así que no se pueden cambiar después de firmar. Test: `do_check_auth_rule_selection_downgrade_fails`. | 🟢 |
| Regla con políticas | Si la regla tiene políticas, no se exige que firmen todos sus firmantes. La política recibe solo los firmantes de la regla que vinieron en la transacción. | 🟢 |
| Sin ninguna firma | Si el mapa de firmas está vacío, nada se autentica y la política recibe una lista vacía. Si la política no exige a la dueña, el pago pasa sin firma. | 🟢 por lectura del código; 🔴 sin test propio |
| `firmantes_autenticados` | Están autenticados: si una firma presentada falla, la transacción se rechaza antes de llegar a `enforce`. | 🟢 |
| Qué ve la política | `Context::Contract` trae contrato, función y argumentos. Spending Limit lee `args.get(2)` como monto de `transfer`. | 🟢 |
| Firma y argumentos | El `signature_payload` cubre la invocación autorizada con sus argumentos, el nonce y la expiración. Una firma no sirve para otro destino ni otro monto. | 🟡 docs de Stellar (no detallan cada campo del preimage) |
| Replay | El host consume el nonce de cada firma. | 🟢 docs de Stellar |
| Invocaciones anidadas | Si un contrato X llama a `USDC.transfer(cuenta, …)`, la cuenta debe haber autorizado un árbol con X y el `transfer` dentro. `__check_auth` recibe ambos contextos, y GuardianHold rechaza el de X por no estar permitido. | 🔵 por el modelo de árbol de autorización; 🔴 hay que probarlo |
| Llamada directa de la cuenta | "Todos los `require_auth` hechos en nombre del invocador **directo** se consideran autorizados." Por eso `execute` es un bypass. | 🟢 docs de Stellar + `ExecutionEntryPoint` |
| Cambiar reglas | `add_context_rule`, `remove_policy`, `add_signer`, etc. piden `current_contract_address().require_auth()`, así que pasan por `__check_auth` con el contexto `CallContract(cuenta)`. Si ninguna regla lo autoriza, la cuenta es inmutable. | 🟢 código; 🔵 conclusión |
| `transfer_from` y `burn_from` | Los autoriza quien gasta, no la dueña (SEP-41). Sin un `approve` previo no hay permiso. | 🟡 SEP-41 |
| `approve` y `burn` | Piden autorización de la dueña: pasan por GuardianHold, que debe rechazarlos. | 🟡 SEP-41; 🟢 Spending Limit rechaza toda función que no sea `transfer` |
| `to` muxed | `transfer(from: Address, to: MuxedAddress, amount: i128)`. | 🟢 `tokens/fungible/mod.rs` |
| Poderes del emisor | El emisor de USDC puede tener poderes de congelar o recuperar fondos (`clawback`) que GuardPay no controla. | 🔴 NO VERIFICADO para USDC testnet; fuera de alcance |

### Auditoría especial de context rules: conclusión

**🟡 GuardianHold necesita cambios arquitectónicos.** El mecanismo de OZ permite que GuardianHold sea obligatorio, pero no lo hace por defecto. La regla de oro es simple: **la cuenta debe tener exactamente una regla, y esa regla debe tener a GuardianHold.** Así, el único `context_rule_id` válido es el de esa regla, cualquier otro id falla en `get_context_rule`, y como GuardianHold rechaza las llamadas a la propia cuenta, nadie puede agregar una segunda regla después.

Alternativas evaluadas antes de llegar a esa regla:

| Configuración | ¿GuardianHold obligatorio? | Problema |
| --- | --- | --- |
| Ejemplo de OZ: `Default` con la dueña, sin política | No | Bypass total |
| `Default` con la dueña + `CallContract(USDC)` con GuardianHold | No | El cliente elige `Default` |
| Solo `CallContract(USDC)` con GuardianHold | Sí para USDC | No autoriza `queue` ni `cancel` (otro contrato); habría que añadir otra regla para el registro, que es otra superficie |
| **Una sola regla `Default` con la dueña + GuardianHold** | **Sí para todo contexto** | GuardianHold debe ser lista blanca estricta; un error ahí afecta a todo |

### Dónde se verifica exactamente la firma de la dueña

En dos puntos, y los dos son necesarios:

1. **Autenticidad**, en `do_check_auth` → `authenticate(auth_digest, firmante, firma)`. Para una passkey, la verifica el contrato verificador WebAuthn. Esto prueba que la firma presentada es válida, pero no que se haya presentado.
2. **Presencia**, en `GuardianHold.enforce`, que debe rechazar si la clave de la dueña no está en `firmantes_autenticados`. OZ no hace esta comprobación cuando la regla tiene políticas. Spending Limit solo comprueba que la lista no esté vacía, lo cual no basta si la regla tuviera más de un firmante.

Para `queue` y `cancel` hechos por la dueña, el registro llama a `cuenta.require_auth()`, que lleva al mismo `__check_auth` y al mismo punto 2. **Respuesta a la pregunta del punto 10: sí, pero solo porque GuardianHold lo exige; OZ no lo hace por sí solo.**

### Asimetría del guardián

El guardián **no es firmante de la cuenta de la dueña**. Es una dirección aparte (su propia passkey o cuenta) guardada en el registro al desplegar.

| Acción | ¿Puede? | Por qué (mecanismo) |
| --- | --- | --- |
| Ver pagos retenidos | Sí | Son públicos en la cadena |
| Detener un pago retenido | Sí | `registro.cancel` exige `guardian.require_auth()` y solo cambia el estado del registro |
| Transferir, `approve`, retirar | No | Su firma no pertenece a la regla → `UnauthorizedSigner`. Sin firma de la dueña, GuardianHold rechaza. |
| Liberar o adelantar un pago | No | No existe esa función; `ready_at` lo calcula el registro |
| Modificar un intent | No | No existe esa función |
| Cambiar dueña, guardián, contactos, límites o políticas | No | Configuración fija al desplegar y cuenta inmutable |

La propiedad "guardián = veto, nunca autoridad sobre los fondos" se cumple **por construcción** si el registro no tiene ninguna función que toque tokens y la política nunca llama a un token. Revisar eso en el código es parte del spike (TEST-GH-09).

## 4. Spike de GuardianHold, vinculación y matriz de bypass

### Arquitectura corregida (mínima)

| Pieza | Qué es | Qué puede hacer | Qué no tiene |
| --- | --- | --- | --- |
| **Cuenta GuardPay** | Smart account propia con las piezas de OZ. Una regla `Default`: firmante = passkey de la dueña, política = GuardianHold. | Recibir y enviar USDC si GuardianHold acepta. | `execute`, `upgrade`, una segunda regla, guardián como firmante. |
| **GuardianHold** (política) | Contrato con `enforce` y la configuración fija por cuenta: token, contactos de confianza, tope diario, duración de la retención y dirección del registro. | Aceptar o rechazar cada contexto; actualizar el gasto del día; marcar un pago retenido como ejecutado en el registro. | Funciones de administración; nunca llama a un token. |
| **Registro** (pagos retenidos) | Contrato con `queue`, `cancel` y lecturas. Guarda el guardián de cada cuenta. | Crear registros (si la cuenta autoriza), detenerlos (guardián o dueña) y marcarlos como ejecutados (solo si lo llama GuardianHold). | Funciones que muevan tokens o cambien `ready_at`. |

🔵 Por qué dos contratos: `queue` pide la autorización de la cuenta, eso llama a `GuardianHold.enforce`, y si `queue` viviera en GuardianHold sería reentrada, que Soroban prohíbe (🟡). La alternativa de un solo contrato usando `cuenta.execute(GuardianHold, "queue", …)` funciona en teoría (OZ dice que `execute` existe para eso), pero obliga a tener `execute` en la cuenta y a que GuardianHold lo filtre perfectamente. Se descarta por superficie.

### Qué acepta `GuardianHold.enforce` (lista blanca; todo lo demás se rechaza)

Primero, siempre: `cuenta.require_auth()` (solo su cuenta lo invoca) y la passkey de la dueña presente entre los firmantes autenticados.

| Contexto | Condiciones para aceptar |
| --- | --- |
| `USDC.transfer(from, to, monto)` a un **contacto de confianza** | `from` = cuenta; `to` es una dirección normal (no muxed) en la lista; 0 < monto; gastado hoy + monto ≤ tope (suma con control de desbordamiento). Actualiza el gastado del día. |
| `USDC.transfer(from, to, monto)` a **cualquier otro destino** | Existe un registro activo de esa cuenta con el mismo `to` y monto, en estado RETENIDO, no detenido, con ahora ≥ `ready_at` (y ahora ≤ vencimiento, SHOULD). Lo marca EJECUTADO en el registro. |
| `Registro.queue(cuenta, to, monto)` | El primer argumento es esta cuenta y 0 < monto. Esta ruta no lee el registro (sería reentrada). |
| `Registro.cancel(cuenta, id)` hecho por la dueña (SHOULD) | El primer argumento es esta cuenta. |
| Cualquier otro contexto: otro token, `approve`, `burn`, llamadas a la cuenta misma, otro contrato, creación de contratos | **Rechazo.** |

Un pago a un contacto de confianza que supera el tope se rechaza; no se convierte solo en retenido. La app ofrece entonces "Retener este pago" (`queue`). Es una regla más simple y deja claro quién decidió.

### A. ¿Puede GuardianHold saber qué transferencia se autoriza?

**Sí (🟢).** `Context::Contract` trae el contrato (token), el nombre de la función y los argumentos `[from, to, monto]`; la cuenta llega como parámetro y se confirma con `require_auth`. Spending Limit ya lee `args.get(2)` en el código de OZ. Queda abierto: cómo llega un `to` muxed dentro de los argumentos (🔴). La decisión segura es rechazar todo `to` que no se pueda convertir a una dirección normal.

### B. Opciones de vinculación entre el intent y el pago

| Opción | Viabilidad | Seguridad | Complejidad | Límites | Riesgo de bypass |
| --- | --- | --- | --- | --- | --- |
| 1. Hash de los argumentos crudos (`Vec<Val>` en XDR) | 🔵 Viable | Alta si el formato es idéntico | Media | La forma XDR de `Address` y `MuxedAddress` puede diferir: un pago legítimo podría no coincidir (falla cerrada, no bypass) | Bajo |
| **2. Comparación campo por campo (tipada)** | 🟢 Viable (los argumentos se pueden leer, como hace Spending Limit) | Alta y legible | Baja | Hay que convertir cada argumento a su tipo; si la conversión falla, se rechaza | Bajo |
| 3. Digest de autorización (`signature_payload`) | ❌ No sirve aquí | — | — | El digest de `queue` y el de la ejecución son distintos (nonce, invocación). Solo serviría con una sola firma pre-firmada (FUTURE) | — |
| 4. Ejecutar vía `Registro.execute(id)`, que llama al token | 🔵 Viable | Media | Alta | El registro tendría que llamar al token: rompe "el registro nunca toca tokens" | Medio |

**Decisión: opción 2.** El registro guarda los campos tipados (cuenta, token, destino, monto). La clave de búsqueda es un hash de esos mismos campos, que GuardianHold recalcula a partir de los argumentos convertidos. Solo puede haber un registro activo por clave: si la dueña intenta retener dos pagos iguales, el segundo `queue` se rechaza con "ya tienes un pago igual en espera". Un registro terminado (ejecutado, detenido o vencido) libera la clave para un pago nuevo, con un `intent_id` nuevo.

### C. ¿Puede un pago retenido modificarse? Cada variante debe fallar

| Variante respecto al registro | Qué la detiene | Test |
| --- | --- | --- |
| Otro destino | La clave no coincide → no hay registro → rechazo | GH-20 |
| Otro monto (mayor o menor) | Ídem | GH-21 |
| Otro token | El contexto no es USDC → rechazo | GH-22 |
| Otra cuenta de origen | El registro es por cuenta; además, `from` ≠ cuenta → rechazo | GH-23 |
| Otra función (`approve`, `burn`) | Fuera de la lista blanca | GH-16, GH-17 |
| Mismo pago, segunda vez | El registro ya está EJECUTADO | GH-19 |
| `to` muxed con la misma cuenta base | La conversión a `Address` falla → rechazo (decisión de diseño) | GH-20b |

### Matriz de bypass

Estado: ✅ bloqueado por diseño (🔵 deducido de código 🟢), pendiente del test indicado. Ninguna celda está demostrada por un test propio todavía: **todas son 🔴 NO VERIFICADO hasta correr la sección 7.**

| Ataque | ¿Puede ocurrir? | Por qué | Control | Evidencia | Test |
| --- | --- | --- | --- | --- | --- |
| `transfer` directo a un desconocido | Solo tras madurar, con la firma de la dueña | Toda autorización de la cuenta pasa por la única regla | Lista blanca + registro | 🟢 flujo OZ; 🔴 test | GH-14 |
| `transfer_from` | No | No hay permisos porque `approve` está bloqueado | Rechazo de `approve` | 🟡 SEP-41 | GH-15 |
| `approve` | No | Fuera de la lista blanca | `enforce` | 🟢 patrón Spending Limit | GH-16 |
| `burn` | No | Fuera de la lista blanca | `enforce` | 🟡 SEP-41 | GH-17 |
| Context rule alternativa | No con regla única; **sí con el ejemplo de OZ** | El id va dentro del hash; cualquier otro id no existe | Constructor propio + rechazo de llamadas a la cuenta | 🟢 código y test de OZ | GH-13 |
| Firmante alternativo (p. ej. el guardián) | No | `UnauthorizedSigner` + exigencia de la passkey de la dueña | `do_check_auth` + `enforce` | 🟢 | GH-09 |
| Política alternativa | No | No se puede añadir una política sin autorizar una llamada a la cuenta | Rechazo de llamadas a la cuenta | 🟢 `add_policy` pide `require_auth` | GH-25 |
| Invocación anidada | No | El contexto externo no está en la lista blanca | `enforce` | 🔵 | GH-18 |
| Llamada directa al token sin firma | No | El token pide la autorización de la cuenta | Host | 🟢 | GH-24 |
| `execute` o `upgrade` | No, porque no existen | — | Cuenta propia | 🟢 (el ejemplo sí los tiene) | GH-25 |
| Replay | No | Nonce del host + registro de un solo uso | Host + registro | 🟢 nonce; 🔵 registro | GH-19 |
| Cambio de destino, monto, token u origen | No | Ver tabla C | Opción 2 | 🔵 | GH-20 a GH-23 |
| Ejecución antes de `ready_at` | No | Comparación con el tiempo del ledger | `enforce` | 🔵 | GH-05 |
| Después de `cancel` | No | Estado DETENIDO | `enforce` | 🔵 | GH-08 |
| Sin firma de la dueña | No | `enforce` exige su passkey | `enforce` | 🟢 hueco documentado en OZ | GH-01, GH-24 |
| Guardián intentando gastar | No | No es firmante y no hay función que mueva fondos | Construcción | 🟢 | GH-09 |
| `enforce` llamado por un tercero | No | `cuenta.require_auth()` dentro de `enforce` | Patrón de OZ | 🟢 Spending Limit | GH-26 |
| `queue` creado por un tercero para "pre-madurar" un pago | No | `queue` exige la autorización de la cuenta → la firma de la dueña | `enforce` | 🔵 | GH-27 |
| Backend comprometido | No puede saltar reglas | No tiene ninguna firma | — | 🔵 | GH-11 |
| Frontend comprometido | No puede saltar reglas; sí puede engañar a la dueña | La passkey firma a ciegas | Retención + vista del guardián con datos de la cadena | 🔵 | GH-10 |
| IA comprometida | No | El contrato no la consulta | — | 🔵 | GH-12 |

## 5. Payment Intent, estados, contactos, duración e IA

### Payment Intent: definición formal

**Un Payment Intent es un registro en el contrato Registro, creado solo con la autorización de la cuenta de la dueña, que fija una transferencia exacta (cuenta, token, destino, monto) y el momento a partir del cual GuardianHold puede aceptarla.** No contiene fondos ni firmas.

| Campo | Clase | Quién lo pone | Ataque si se hace mal | Defensa |
| --- | --- | --- | --- | --- |
| `intent_id` | Derivado, obligatorio | El registro (contador por cuenta) | Si lo pone el cliente, podría pisar otro registro | Nunca es un parámetro de entrada |
| `source` (cuenta) | Obligatorio | Es el primer argumento de `queue`, autorizado por esa cuenta | Crear registros a nombre de otra cuenta | `cuenta.require_auth()` en `queue` |
| `token` | Redundante en el MVP | Configuración de la cuenta (solo USDC) | Ninguno mientras haya un solo token | Se guarda en la clave para no romper una versión con varios tokens |
| `destination` | Obligatorio | La dueña | Destino cambiado por la app (firma a ciegas) | Es público en la cadena y la vista del guardián lo lee de ahí |
| `amount` | Obligatorio | La dueña | Monto negativo o cero | `enforce` y `queue` exigen monto > 0 |
| `created_at` | Derivado, informativo | Tiempo del ledger en `queue` | — | No es un parámetro |
| `ready_at` | Derivado, **peligroso si fuera parámetro** | `created_at` + duración fija de la cuenta | Un cliente que pase `ready_at = ahora` anula la retención | Lo calcula el registro; ninguna función lo cambia |
| `expires_at` (SHOULD) | Derivado | `ready_at` + ventana fija | Un pago maduro olvidado que se envía semanas después | `enforce` rechaza después de esa hora |
| `status` | Obligatorio | Registro (`queue`, `cancel`) y GuardianHold (ejecutado) | Volver a RETENIDO un registro detenido | Solo hay transiciones hacia estados finales |
| `guardian` | **Peligroso en el intent**; innecesario | Configuración de la cuenta al desplegar | Si el cliente lo pasa en `queue`, el estafador se pone a sí mismo | No forma parte del intent; se lee de la cuenta |

**Ciclo de vida**

- **Creación:** la dueña firma `Registro.queue(cuenta, destino, monto)`. Se rechaza si ya hay uno activo con la misma clave.
- **Ejecución:** la dueña firma `USDC.transfer(cuenta, destino, monto)` después de `ready_at`. GuardianHold lo marca EJECUTADO en la misma transacción; si el `transfer` falla (por ejemplo, por falta de saldo), se revierte todo y el registro sigue igual.
- **Cancelación:** guardián o dueña, en cualquier momento antes de la ejecución, también después de `ready_at`. El guardián puede detener un pago hasta que salga.
- **Vencimiento (SHOULD):** sin transacción; se deriva del tiempo, y `enforce` lo rechaza.
- **Replay:** cubierto por el nonce del host y por el estado EJECUTADO.
- **Reutilización:** un registro terminado nunca vuelve a aceptarse; un pago igual necesita un nuevo `queue` y una nueva espera.
- **Modificación:** no existe ninguna función; cambiar algo equivale a crear otro intent.

### Modelo de estados (auditado)

El contrato guarda tres estados. La app deriva dos más y muestra dos resultados de transacción. Esto confirma el modelo del documento base, con dos correcciones: la entrada a "Retenido" es siempre un `queue` explícito, y el guardián puede detener también un pago "Listo para enviar".

| Estado | Dónde vive | Entra por | Sale hacia |
| --- | --- | --- | --- |
| Retenido | Registro | `queue` (dueña) | Listo (tiempo), Detenido |
| Listo para enviar | Derivado (ahora ≥ `ready_at`) | Tiempo | Enviado (dueña), Detenido, Vencido |
| Enviado | Registro EJECUTADO, o solo la transferencia si fue a un contacto | GuardianHold dentro del `transfer` | Final |
| Detenido | Registro | `cancel` (guardián o dueña) | Final |
| Vencido (SHOULD) | Derivado | Tiempo | Final |
| Rechazado por el contrato | Resultado de transacción | Cualquier intento inválido | — |
| Error de red | Resultado de transacción | Fallo técnico | — |

### Contactos de confianza y tope diario

| Pregunta | Respuesta |
| --- | --- |
| ¿Dónde se guardan? | En GuardianHold, por cuenta, en almacenamiento **persistente**. Se fijan en `install`, que se llama una sola vez cuando el constructor crea la regla; un segundo `install` para la misma cuenta debe rechazarse. |
| ¿Se pueden modificar? | No en el MVP. Agregar un contacto es exactamente lo que pediría un estafador ("agrégame como contacto"). |
| ¿Y con espera? | FUTURE: agregar un contacto como si fuera un pago retenido, con la misma espera y el mismo veto del guardián. |
| ¿Ventana de calendario o móvil? | MVP: día UTC por tiempo del ledger (simple). Límite conocido: se puede gastar hasta 2× el tope alrededor de medianoche. Ventana móvil de 24 h: SHOULD (Spending Limit de OZ ya implementa una ventana por ledgers, 🟢). |
| ¿Pagos simultáneos? | Dos transacciones que escriben el mismo contador no pueden aplicarse sobre el mismo valor viejo. 🔵 Soroban declara de antemano qué datos toca cada transacción; 🔴 hay que probarlo con dos pagos en el mismo ledger. |
| ¿Qué pasa al superar el tope? | Rechazo. La app ofrece "Retener este pago". Las sumas usan control de desbordamiento. |
| ¿Hace falta este carril? | Sí, para el producto: sin él todo pago espera horas y nadie usa la billetera. Se acota a 3 contactos y un tope bajo, todo fijo. Para el contrato es la parte más prescindible: si el tiempo aprieta, la demo puede mostrar solo el carril retenido. |

### Duración de la retención

- **Técnica:** `ready_at` usa el tiempo del ledger, que avanza en saltos de unos segundos por ledger (🟡). Cualquier duración de minutos en adelante es representable.
- **Demo:** 2–3 minutos, fijados al desplegar la cuenta de demo, para que el jurado vea el rechazo anticipado, la cuenta regresiva y el envío.
- **Producto:** 🔴 no hay evidencia para elegir 6 h, 12 h o 24 h. El único dato encontrado es que Yandex Pay impone 24 h para *desactivar* la verificación, no para retener pagos (🟡). Se presenta como un parámetro que se validará con usuarios, y la UI muestra la hora real de liberación en lugar de prometer un número.

### IA: mínimo obligatorio

- **MUST:** la dueña pega el mensaje que la presiona y la IA devuelve un formato fijo: qué pide el mensaje, qué señales de presión o suplantación ve, y una sugerencia ("Te sugerimos retener este pago" o "No vemos señales claras"). Nunca usa la palabra "seguro".
- **"Retener de todos modos":** si la IA ve señales en un pago a un contacto de confianza, la app ofrece "Retener este pago", que llama a `queue` en vez de a `transfer`. Lo decide la dueña con su firma. La IA no tiene ningún camino para hacer lo contrario, porque el contrato no la lee.
- **Autoridad cero:** no firma, no arma transacciones, no completa destino ni monto. El borrador lo arma el formulario, no la IA, para que una IA manipulada no pueda cambiar un destino.
- **Si falla o está comprometida:** el peor caso es un consejo equivocado. Un pago a un desconocido se retiene igual.

## 6. Modelo de amenazas, kill test competitivo y uso de Stellar

### Adversarios A–L

|  | Adversario | Capacidad | Ataque | Impacto | Defensa | Garantizado | NO garantizado |
| --- | --- | --- | --- | --- | --- | --- | --- |
| A | Frontend comprometido | Decide qué se le pide firmar a la passkey | Muestra "pagar a Diego" y hace firmar un `queue` a la cuenta del atacante | La dueña firma algo que no ve | El pago queda retenido; la vista del guardián muestra el destino real leído de la cadena | Ningún pago a un desconocido sale sin espera; la configuración no cambia | Que la dueña o el guardián detecten el engaño si ambos usan la misma app comprometida |
| B | Backend comprometido | Notificaciones, análisis de IA, patrocinio de comisiones | Silencia el aviso al guardián; deja de enviar transacciones | El guardián no se entera; el pago madura | Los eventos son públicos; la web lee el estado por RPC; el guardián puede enviar `cancel` por otra vía | No puede mover fondos ni detener pagos | **Que el guardián se entere a tiempo** |
| C | IA comprometida o manipulada | Texto de análisis | Dice "no vemos señales" ante un mensaje de estafa | La dueña confía más de la cuenta | Formato fijo; la retención no depende de la IA | Ninguna regla cambia | Que el consejo sea correcto |
| D | Guardián malicioso | Su firma, `cancel` | Detiene todos los pagos a desconocidos; o no detiene nada (connivencia) | Bloqueo de pagos o protección nula | No es firmante; no hay función de fondos | No puede robar ni redirigir | Disponibilidad: puede bloquear para siempre (no hay cambio de guardián en el MVP) |
| E | Atacante con el teléfono desbloqueado | Usa la passkey | Paga a contactos hasta el tope; retiene un pago hacia sí mismo | Pérdida hasta el tope | Tope diario; retención visible para el guardián | Lo demás espera y se puede detener | Lo enviado a contactos dentro del tope |
| F | Atacante con una firma válida | Una autorización ya firmada | Reenviarla | — | Nonce del host; registro de un solo uso; la firma cubre los argumentos | No hay replay | — |
| G | Manipular parámetros | Cambiar destino, monto, token u origen | Ejecutar un intent con otros datos | — | Comparación campo por campo | El pago ejecutado es el registrado | Que lo registrado sea lo que la dueña quería (ver A) |
| H | Otra context rule | Elegir otro id | Usar una regla sin GuardianHold | Bypass total si existe | Regla única + cuenta congelada | Con la arquitectura corregida, no existe otra regla | Con el ejemplo de OZ, **sí hay bypass** |
| I | `approve` | Pedir un permiso | Permiso + `transfer_from` | Vaciado de la cuenta | Lista blanca | Rechazado | — |
| J | `transfer_from` | Gastar un permiso | — | — | No hay permisos posibles | Rechazado | — |
| K | Ejecutar un intent detenido | Firma de la dueña | Enviar después de `cancel` | — | Estado DETENIDO | Rechazado | Que la dueña no cree un nuevo intent igual (es su derecho; vuelve a esperar) |
| L | Replay | Transacción vieja | Reenviar | — | Nonce + EJECUTADO | Rechazado | — |

Lo que **ninguna** pieza técnica garantiza: que la dueña sepa qué firma, que el guardián se entere y que el guardián actúe bien. Soroban garantiza la espera y el derecho a vetar, no que alguien use ese derecho.

### Kill test competitivo

*¿Existe un mecanismo donde un tercero sin capacidad de gastar pueda detener un pago específico durante una ventana temporal, con el pago vinculado on-chain a sus parámetros, sin convertirse en co-owner?*

**Sí, en partes, y una versión casi completa.**

| Mecanismo | Tercero sin poder de gasto | Pago específico | Ventana | Vinculado on-chain | Fuente |
| --- | --- | --- | --- | --- | --- |
| OZ `TimelockController` (Ethereum) | Sí: `CANCELLER_ROLE` puede ir solo | Sí: id = hash(target, value, data, predecessor, salt) | Sí: `minDelay` | Sí | 🟢 docs OZ |
| Yandex Pay, verificación por contacto de confianza | Sí | Sí: aprueba o rechaza esa transferencia | Hasta que responda | No: lo aplica el banco | 🟡 prensa |
| Monzo Added Security | Puede alertar, no bloquear | Sí | — | No | 🟢 ayuda de Monzo |
| Argent (guardianes) | Sí | Aprueba operaciones sobre el límite; puede bloquear la cuenta | Sí | Sí (Ethereum) | 🟡 TechCrunch 2020 |

**Conclusión:** el primitivo técnico existe (Timelock con canceller) y el producto existe en la banca tradicional (Yandex Pay). Lo que no se encontró es la combinación en una billetera personal con passkey, impuesta por contrato, para estafas de pago autorizado y en Stellar. 🔴 Que no se haya encontrado no prueba que no exista. Esto **no mata el proyecto**, porque un hackathon premia ejecución y uso significativo, y Yandex muestra que una fintech grande apuesta por el mismo comportamiento. Pero **sí mata** cualquier frase de "primero", "único" o "nuevo".

### ¿Por qué en Stellar? (uso significativo, no exclusivo)

| Capacidad de Stellar | Qué parte de GuardPay depende de ella | ¿Existe en EVM? |
| --- | --- | --- |
| USDC como contrato SAC: todo `transfer` pide la autorización de la cuenta | La política ve cada pago | Sí, con un Safe + guard |
| Smart accounts de OZ con reglas, políticas y verificador WebAuthn | No hay que escribir la cuenta ni el verificador de passkeys | Parcial: ERC-4337 + módulos |
| `__check_auth` con contexto y argumentos | La vinculación exacta | Sí, con guards |
| Comisiones bajas y patrocinables | Retener, detener y enviar cuestan centavos | Depende de la red |

🔵 Lectura honesta: se podría construir en EVM con Safe + Delay modifier + un guard. En Stellar se construye con menos código propio y con passkeys nativas en la cuenta. Eso es lo que se defiende.

### Auditoría de "Ver en la cadena"

| Lo que se muestra | Prueba que ocurrió algo | Prueba una propiedad |
| --- | --- | --- |
| Hash del `queue`, del `cancel` y del envío | Sí | No |
| Estado leído del registro | Sí (estado actual) | No |
| Transacción rechazada con código de error | Sí: ese intento falló | Solo para ese intento |
| Código del contrato verificado + configuración de la cuenta (una sola regla) | — | **Sí**, si el jurado puede comprobar que la cuenta tiene una sola regla, que GuardianHold es la política y que la cuenta no tiene `execute` ni `upgrade` |
| Tests publicados (sección 7) | — | Sí, para los casos probados |

La demo actual solo prueba que algo ocurrió. Para mostrar una propiedad hay que añadir: (1) un panel "Configuración de la cuenta" que lea de la cadena las reglas, el firmante y la política; (2) el enlace al código y a los tests en el repo; (3) al menos un ataque fuera de la app rechazado en vivo. Frase permitida: "estos intentos fueron rechazados por el contrato y estos tests cubren los demás casos". Frase prohibida: "no existe bypass".

## 7. Especificación de tests del spike (TEST-GH)

**Escenario base** (vale para todos los tests salvo que se indique otra cosa): cuenta **C** con una sola regla `Default` (firmante: passkey de la dueña **O**; política: **GH** = GuardianHold); registro **R** con guardián **G**; contacto de confianza **T**; tope diario 50 USDC; retención de 120 s; ventana de vencimiento de 600 s; C con 500 USDC; atacante **X**. Todo "rechazo" significa que la transacción falla on-chain y el saldo de C no cambia. Cada test se corre en el entorno de tests de Soroban, y los marcados con ★ también en testnet para la demo.

Esto es un spike: **ningún test está escrito ni corrido.** Hasta que pasen, todo lo que dicen es 🔴 NO VERIFICADO.

| Test ID | # obligatorio | Objetivo | Precondiciones | Acción | Resultado esperado | Propiedad demostrada | Vulnerabilidad que detectaría |
| --- | --- | --- | --- | --- | --- | --- | --- |
| GH-01 | 1 | Un pago sin la firma de O no pasa aunque GH esté en la regla | Base | `transfer` C→T de 10 con el mapa de firmas vacío y `context_rule_ids=[0]` | Rechazo | La presencia de O la exige GH | GH confía en OZ para validar la firma (hueco de políticas) |
| GH-02 | 2 ★ | Un pago a un contacto dentro del tope sale al instante | Base | O firma `transfer` C→T de 10 | Éxito; gastado hoy = 10 | Carril de confianza | GH demasiado restrictivo |
| GH-03 | 3 ★ | Un pago a un desconocido no sale directo | Base | O firma `transfer` C→X de 100 sin registro | Rechazo | No hay pago a un desconocido sin retención | Lista de contactos mal leída |
| GH-04 | 4 ★ | `queue` crea un registro con `ready_at` calculado por R | Base | O firma `R.queue(C, X, 100)` | Registro RETENIDO, `ready_at` = ahora + 120 s, evento emitido | La retención existe on-chain | `ready_at` tomado del cliente |
| GH-05 | 5 ★ | No se envía antes de `ready_at` | GH-04 | O firma `transfer` C→X de 100 en t+60 s | Rechazo | No hay bypass temporal | Comparación de tiempo invertida o en ledgers mal convertidos |
| GH-06 | 6 ★ | Después de `ready_at` hace falta una nueva firma de O | GH-04, t+130 s | (a) Enviar sin firma; (b) O firma | (a) Rechazo; (b) Éxito, registro EJECUTADO | No hay ejecución automática ni sin firma | Liberación automática o sin firma |
| GH-07 | 7 ★ | G detiene un pago | GH-04 | G firma `R.cancel(C, id)` | Registro DETENIDO | El veto del guardián | `cancel` sin control o mal ligado a la cuenta |
| GH-07b | 7 | Solo G (u O) puede detener | GH-04 | X firma `R.cancel(C, id)` | Rechazo | Nadie más puede detener | Cualquiera puede bloquear pagos |
| GH-08 | 8 ★ | Un pago detenido nunca sale | GH-07, t+130 s | O firma `transfer` C→X de 100 | Rechazo | Detenido es un estado final | El estado no se revisa en la ejecución |
| GH-09 | 9 ★ | G no puede gastar | Base | (a) `transfer` C→G firmado por G; (b) G incluido como firmante junto a O en un `approve`; (c) G llama a cada función pública de R y GH | (a) y (b) Rechazo (`UnauthorizedSigner` o GH); (c) ninguna mueve fondos ni cambia la configuración | Guardián = veto, nunca autoridad | El guardián agregado como firmante; funciones de R que mueven fondos |
| GH-10 | 10 ★ | Una app comprometida no salta reglas | Base | Desde la terminal (sin la web), con una firma válida de O, enviar `transfer` C→X de 100 sin registro | Rechazo | Las reglas viven en el contrato | Lógica de seguridad solo en el frontend |
| GH-11 | 11 | Un backend comprometido no salta reglas | Base | Con la cuenta que paga comisiones (sin firma de O): `transfer`, `queue` y `cancel` | Rechazo de todo | El patrocinador no tiene autoridad | Patrocinador agregado como firmante |
| GH-12 | 12 | La IA no influye en el contrato | Base | Revisión: ninguna función de GH o R recibe datos de la IA; repetir GH-03 con un análisis "sin señales" | Rechazo igual | La IA no tiene autoridad | Un flag "riesgo bajo" que acorte la espera |
| GH-13 | 13 | No hay regla alternativa | Base | (a) Pedir `context_rule_ids=[1]`; (b) firmar con `[0]` y cambiarlo a `[1]` después; (c) O firma `add_context_rule(Default, [O], {})` | (a) Rechazo (no existe); (b) Rechazo (firma inválida); (c) Rechazo (GH) | Regla única y congelada | Constructor del ejemplo de OZ; llamadas a la cuenta permitidas |
| GH-14 | 14 | `transfer` solo por los dos carriles | Base | Matriz: contacto/desconocido × dentro/fuera del tope × con/sin registro maduro | Solo pasan contacto + dentro del tope, y registro maduro exacto | La lista blanca de `transfer` | Carriles mezclados |
| GH-15 | 15 | `transfer_from` no es una ruta | Base | X llama a `transfer_from(X, C, X, 100)` | Rechazo por falta de permiso | No hay permisos | `approve` permitido en algún camino |
| GH-16 | 16 ★ | `approve` bloqueado | Base | O firma `approve(C, X, 100, exp)` | Rechazo | Lista blanca de funciones | GH acepta cualquier función del token |
| GH-17 | 17 | `burn` bloqueado | Base | O firma `burn(C, 10)` | Rechazo | Ídem | Ídem |
| GH-18 | 18 | Una invocación anidada no salta GH | Base + contrato N que llama a `USDC.transfer(C, X, 100)` | O firma un árbol N.pay → transfer | Rechazo (el contexto de N no está permitido) | Contextos anidados filtrados | GH revisa solo el contexto raíz o solo el `transfer` |
| GH-18b | 18 | Lo mismo con un registro maduro | GH-04 madurado | Igual que GH-18 con los datos del registro | Rechazo | Un intent solo se ejecuta como `transfer` directo | Ejecución por un intermediario |
| GH-19 | 19 | Sin replay | GH-06 hecho | (a) Reenviar la misma transacción; (b) O firma de nuevo el mismo `transfer` | Rechazo en ambos | Nonce + un solo uso | El registro no se marca EJECUTADO |
| GH-20 | 20 | Otro destino | GH-04 madurado | O firma `transfer` C→X2 de 100 | Rechazo | Vínculo exacto | Clave sin destino |
| GH-20b | 20 | Destino muxed | GH-04 madurado | O firma `transfer` C→(X con id muxed) de 100 | Rechazo (decisión de diseño) | Conversión estricta | Normalización que deje pasar destinos inesperados |
| GH-21 | 21 | Otro monto | GH-04 madurado | O firma 99 y después 101 | Rechazo en ambos | Vínculo exacto | Comparación ≤ en vez de = |
| GH-22 | 22 | Otro token | GH-04 madurado; C tiene XLM | O firma `XLM.transfer(C, X, 100)` | Rechazo | Token fijo | GH no revisa el contrato del contexto |
| GH-23 | 23 | Otro origen | Cuenta C2 de otra dueña con su registro maduro C2→X de 100 | O firma `transfer` C→X de 100 usando el registro de C2 | Rechazo | Registros por cuenta | Clave sin la cuenta |
| GH-24 | 24 | Sin firma de O no hay ejecución | GH-04 madurado | X envía el `transfer` sin firmas, y después con una firma de X | Rechazo en ambos | Firma obligatoria | Igual que GH-01 en el carril retenido |
| GH-25 | 25 | No hay ruta de autorización alternativa | Base | Intentar `execute`, `upgrade`, `add_policy`, `remove_policy`, `add_signer`, `remove_context_rule` firmados por O | Rechazo (no existen o GH los rechaza) | Cuenta inmutable y sin puertas traseras | La cuenta hereda `execute` o `upgrade` del ejemplo |
| GH-26 | extra | `enforce` solo lo llama su cuenta | Base | X llama a `GH.enforce(ctx, [O], regla, C)` directamente | Rechazo | No se pueden alterar contadores ni registros desde afuera | Falta `cuenta.require_auth()` en `enforce` |
| GH-27 | extra | Nadie "pre-madura" un pago | Base | X llama a `R.queue(C, X, 100)` sin la firma de O | Rechazo | Solo la dueña inicia la espera | `queue` sin autorización |
| GH-28 | extra | El tope se respeta con pagos seguidos | Base | 30 + 20 → éxito; luego 1 → rechazo; después del día siguiente, 1 → éxito | Según lo descrito | Tope diario | Contador no persistente o mal reiniciado |
| GH-29 | extra | Dos pagos en el mismo ledger no duplican el tope | Base | Dos `transfer` de 30 a T en el mismo ledger | Uno pasa, otro se rechaza | Sin carrera | Lectura del contador vieja |
| GH-30 | extra | Vencimiento (SHOULD) | GH-04 | O firma en t+800 s | Rechazo | Un pago maduro no queda abierto para siempre | Falta revisar `expires_at` |
| GH-31 | extra | Un solo registro activo por pago igual | GH-04 | O firma un segundo `R.queue(C, X, 100)` | Rechazo | Sin ambigüedad de vínculo | Dos registros iguales: ¿cuál se ejecuta? |
| GH-32 | extra | La configuración no se reinstala | Base | Llamar otra vez a `GH.install` para C | Rechazo | Configuración fija | Reescritura de contactos o tope |
| GH-33 | extra | La firma de la passkey sigue el formato de OZ | Base, firma real con WebAuthn desde el navegador | O firma con el kit de passkeys elegido | Éxito solo si firma `sha256(payload ‖ ids)` | Compatibilidad del flujo real | Kit que firma el payload sin los ids |

**Cobertura de los 25 temas obligatorios:** todos tienen al menos un test. Los números 10, 11 y 12 no se pueden "probar" como un ataque genérico; se prueban como lo que esa capa sí puede hacer (enviar transacciones sin la app, tener la cuenta que paga comisiones, producir texto). Lo que queda fuera de esos tests (engañar a la persona) está en el modelo de amenazas, no en los tests.

## 8. GuardPay — Propuesta de la app web (versión definitiva)

Se conserva lo que el documento base tenía bien y se aplica lo que encontró esta auditoría. Cada cambio respecto a la versión anterior está marcado con **\[nuevo\]**.

### Concepto

GuardPay es una billetera web en Stellar donde pagarle a un contacto de confianza es inmediato (hasta un tope), y pagarle a cualquier otra persona requiere retener el pago un tiempo fijo, durante el cual una persona de confianza puede detenerlo. Esa persona nunca puede mover el dinero. La regla la aplica un contrato en Soroban, no la app.

Frase guía corregida: **"La IA advierte. La dueña firma. El guardián puede detener. El contrato rechaza lo demás."**

### Usuarios

- **Dueña** (Laura): usa la billetera y firma con su passkey.
- **Guardián** (Diego): alguien de su confianza, con su propio firmante. Solo puede ver y detener.
- **Jurado / verificador:** cualquiera que quiera comprobar en la cadena lo que dice la app.

### Arquitectura

| Capa | Qué hace | Confianza que se le da |
| --- | --- | --- |
| Cuenta GuardPay **\[nuevo: propia, no el ejemplo de OZ\]** | Una regla única: passkey de la dueña + GuardianHold. Sin `execute` ni `upgrade`. | Total; es lo que se demuestra |
| GuardianHold (política) | Lista blanca: `transfer` de USDC por los dos carriles; `queue` y `cancel` del registro. Exige la firma de la dueña y que la llame su cuenta. | Total |
| Registro **\[nuevo\]** | Pagos retenidos: `queue`, `cancel`, lecturas. Guardián fijo por cuenta. | Total |
| Web (mobile-first) | Muestra el estado leído por RPC y pide firmas | Ninguna |
| Backend | IA, aviso al guardián, pago de comisiones | Ninguna para los fondos; necesaria para que el guardián se entere |
| IA | Análisis del mensaje | Ninguna |

La configuración (dueña, guardián, hasta 3 contactos, tope, duración) se fija al desplegar con un script del equipo. **El MVP no tiene pantalla de configuración.**

### Estructura web: cuatro pantallas

| Pantalla | Quién | Contenido | Acción principal |
| --- | --- | --- | --- |
| Inicio | Dueña | Saldo, guardián, contactos con lo que queda del tope, pagos retenidos con cuenta regresiva. **\[nuevo\]** Sección plegable "Reglas de esta cuenta", leída de la cadena: una regla, firmante, política y enlace al código. | Pagar |
| Pagar | Dueña | Destinatario, monto, carril previsto, análisis de IA opcional | "Enviar" (contacto) o "Retener este pago" (el resto) |
| Detalle del pago | Ambos | Estado leído de R, destino, monto, `ready_at`, transacciones y "Ver en la cadena" | Dueña: "Enviar" cuando esté listo. Guardián: "Detener este pago". |
| Guardián | Guardián | Pagos retenidos de quien protege, leídos de la cadena (no del backend) | Abrir detalle |

No se agrega una quinta pantalla: "Reglas de esta cuenta" es una sección de Inicio, porque es una lectura sin acciones.

### Funcionalidades y estados

Los estados son los de la sección 5: Retenido, Listo para enviar, Enviado, Detenido, Vencido (SHOULD), Rechazado por el contrato y Error de red. La web nunca muestra "Retenido" si el registro no existe on-chain.

### Flujo de la dueña

1. Entra con su passkey. Ve Inicio.
2. **Pagar.** Si el mensaje la apuró, lo pega y la IA lo analiza.
3. **Contacto de confianza dentro del tope:** "Sale en segundos" → firma el `transfer` → Enviado.
4. **Cualquier otro destino, o si ella lo elige:** "Se retendrá hasta las 14:32. Diego podrá detenerlo." → firma `queue` → Retenido. **\[nuevo: firma explícita para retener; no hay un "intento" que se convierta en retención.\]**
5. Al llegar la hora: "Listo para enviar" → firma el `transfer` exacto → Enviado. Si Diego lo detuvo: "Detenido por Diego", con su transacción.
6. Si cambia de opinión: "Detener mi pago" (SHOULD).

### Flujo del guardián

1. Recibe un aviso **\[nuevo: MUST\]** ("Laura retuvo un pago de 150 USDC a una cuenta nueva"), con un enlace al detalle.
2. El detalle lee destino, monto y hora de la cadena, y muestra el análisis de IA si existe.
3. "Detener este pago" (firma `cancel`) o no hacer nada. No hay botón de aprobar.
4. Texto fijo en su pantalla: "Puedes detener los pagos retenidos de Laura. No puedes mover su dinero ni cambiar sus reglas."

### Flujo de la IA

Mensaje pegado → análisis en formato fijo → si hay señales y el pago es a un contacto, se ofrece "Retener este pago" → la dueña decide. La IA nunca completa destino ni monto.

### Interacción con los contratos

| Acción en la web | Llamada | Firma | Comisión |
| --- | --- | --- | --- |
| Pagar a un contacto | `USDC.transfer(C, T, monto)` | Dueña | Patrocinador del equipo |
| Retener | `R.queue(C, destino, monto)` | Dueña | Patrocinador |
| Enviar un pago listo | `USDC.transfer(C, destino, monto)` | Dueña | Patrocinador |
| Detener | `R.cancel(C, id)` | Guardián (o dueña) | Patrocinador, o el mismo guardián desde cualquier herramienta |
| Leer estado y reglas | Lectura por RPC | — | — |

### Prueba de seguridad en la interfaz

Tres niveles, de más débil a más fuerte: (1) cada transacción tiene su enlace al explorador; (2) los rechazos se ven en el explorador con su código de error; (3) **\[nuevo\]** "Reglas de esta cuenta" muestra, leído de la cadena, que existe una sola regla y cuál es su política, y enlaza al código y a los tests. Solo el nivel 3 sostiene una afirmación de propiedad, y aun así solo para lo que cubren los tests.

### Alcance del MVP

**Reducción 1** (¿qué se puede quitar sin romper la propuesta?): edición de contactos, ventana móvil del tope, modo lado a lado, varios tokens, historial completo, onboarding. Fuera.

**Reducción 2** (¿la misma propiedad con menos?): un solo contrato en lugar de dos → no, porque exige `execute`, que es un bypass potencial. Quitar el carril de contactos → la propiedad se sigue demostrando, pero el producto pierde sentido; queda como MUST, pero es lo primero que se recorta si el tiempo aprieta. Passkey → SHOULD; la llave ed25519 con el verificador de OZ basta para demostrar la propiedad.

| Nivel | Qué entra |
| --- | --- |
| **MUST** | Cuenta propia de regla única; GuardianHold con lista blanca, firma de la dueña y `require_auth` de la cuenta; registro con `queue` y `cancel`; tests GH-01 a GH-27 y GH-31 a GH-32 en verde; cuatro pantallas; estado leído de la cadena; "Reglas de esta cuenta"; análisis de IA en formato fijo; aviso mínimo al guardián; carril de contactos con tope fijo |
| **SHOULD** | Passkey (GH-33); vencimiento; la dueña puede detener su propio pago; tests de tope GH-28 y GH-29 |
| **NICE** | Modo lado a lado para la demo; enlace compartible del detalle; ventana móvil de 24 h |
| **FUTURE** | Cambiar de guardián o de contactos con espera y veto; varios guardianes; recuperación de la cuenta; mainnet con auditoría; una sola firma con autorización pre-firmada |
| **DO NOT BUILD** | `execute` o `upgrade` en la cuenta; cualquier función de administración; segunda regla "de emergencia"; guardián como firmante; IA que complete destinos o montos; custodia; SINPE, fiat o KYC; mainnet |

### Demo (5 escenas + 1)

1. Laura paga 10 a Diego: Enviado en segundos.
2. Laura pega el mensaje del falso banco, la IA marca señales y ella retiene 150 a una cuenta nueva. Se intenta enviar ya: rechazado en el explorador.
3. Diego recibe el aviso y lo detiene.
4. Desde la terminal, sin la app, se intenta enviar el pago detenido: rechazado.
5. Diego intenta transferirse fondos de Laura: rechazado.
6. **\[nuevo\]** Desde la terminal se intenta `approve` y, después, agregar una regla sin política: ambos rechazados. Luego se abre "Reglas de esta cuenta". Esta escena responde la pregunta que haría un jurado técnico: "¿y si uso otra regla?".

### Riesgos y limitaciones (dichos en voz alta)

- Contratos sin auditar; solo testnet.
- La passkey firma a ciegas: la app puede engañar a la dueña; lo que la protege es la retención y el guardián.
- Si el aviso no llega, la espera pasa sin que nadie mire.
- Un guardián malicioso puede bloquear los pagos a desconocidos, y en el MVP no se puede reemplazar.
- Si la dueña pierde la passkey, no hay recuperación en el MVP.
- El emisor de USDC conserva sus propios poderes sobre el token.
- La duración de la retención en producto no está validada.

### Claims permitidos y prohibidos

| Prohibido | Por qué | Permitido en su lugar |
| --- | --- | --- |
| "Soroban lo garantiza" | Soroban ejecuta lo que escribimos; la garantía depende de nuestro código y su configuración | "El contrato rechaza estos casos; estos tests lo cubren" |
| "El guardián no puede robar" | Cierto por diseño, pero sin auditoría | "El guardián no es firmante de la cuenta y no tiene ninguna función que mueva fondos; intentarlo falla, lo vemos en vivo" |
| "No existe bypass" | No se puede demostrar una negación con tests | "Probamos estas rutas de bypass y todas fallan: …" |
| "El pago está protegido" / "es seguro" | Lo que existe es una espera con veto | "El pago espera hasta las 14:32 y Diego puede detenerlo" |
| "La IA detecta la estafa" | La IA da señales, sin precisión medida | "La IA te explica las señales de presión del mensaje" |
| "GuardPay es único" / "el primero" | Yandex Pay, Timelock y Argent | "Llevamos a una billetera con passkey en Stellar un patrón que la banca ya usa, con la regla en el contrato" |
| "Solo es posible en Stellar" | Se puede hacer en EVM | "Stellar nos da USDC nativo, smart accounts con passkey y políticas listas" |
| "Retenido 6 horas" como verdad de producto | Sin evidencia | "La duración es un parámetro; en la demo son 2 minutos" |

## 9. VEREDICTO

**🟡 GO WITH CHANGES**

### Árbol de decisión

1. ¿Puede GuardianHold ver y vincular el pago exacto? → **Sí** (🟢 el contexto trae contrato, función y argumentos).
2. ¿Puede ser obligatorio para todo movimiento de fondos? → **No con el ejemplo de OZ; sí con una cuenta de regla única, sin `execute` ni `upgrade` y congelada** (🟢 código; 🔵 conclusión).
3. ¿Puede exigir la firma de la dueña? → **Sí, si lo hace la política.** OZ no lo hace por sí solo (🟢).
4. ¿Puede crearse la retención? → **No dentro del intento rechazado; sí con un `queue` aparte**, en otro contrato por la regla de no reentrada (🟢/🟡).
5. ¿Puede el guardián vetar sin gastar? → **Sí por construcción** (no es firmante; el registro no toca tokens).
6. ¿Está demostrado con tests? → **No.** Por eso no es GO.

No hizo falta llegar a REDESIGN: las alternativas (otra configuración de la cuenta, otra forma de vincular, otro contrato para el guardián) ya están incorporadas en los cambios.

### Por qué

El núcleo funciona con piezas que existen y están en el código de OZ. Pero la versión anterior tenía tres bypass (regla `Default`, `execute`, `upgrade`) y una imposibilidad (crear la retención dentro de `enforce`) que la habrían hecho fallar en el primer ataque de un jurado técnico. Los cambios son concretos y acotados. La respuesta a la pregunta central pasa de "no" a "sí, condicionado", y queda en 🔴 NO VERIFICADO hasta que pasen los tests.

### Evidencia crítica

- 🟢 `context_rule_ids` dentro del hash firmado, con test de OZ contra la degradación de regla.
- 🟢 Con políticas, la validación de firmantes queda en manos de la política; sin firmas, la política recibe una lista vacía.
- 🟢 El constructor del ejemplo crea `Default` con los firmantes dados; el ejemplo incluye `execute` y `upgrade`.
- 🟢 Una llamada directa de un contrato autoriza a ese contrato (docs de Stellar).
- 🟢 Spending Limit muestra los patrones necesarios: `smart_account.require_auth()` en `enforce`, lectura de argumentos y rechazo de funciones que no sean `transfer`.
- 🟡 Soroban no permite reentrada (blog técnico de Stellar).
- 🟢/🟡 Competencia: OZ Timelock CANCELLER, Yandex Pay y Monzo.

### Blockers (sin esto no se construye la web)

1. GH-01, GH-13, GH-24, GH-25 y GH-26 en verde: firma obligatoria, regla única y sin rutas alternativas.
2. GH-04 + GH-05 + GH-06 en verde: el ciclo `queue` → espera → envío funciona con dos contratos.
3. Una firma real desde el navegador aceptada por `__check_auth`: con ed25519 (MUST) o con passkey (GH-33, SHOULD). Si ningún kit firma `sha256(payload ‖ ids)`, hay que escribir esa parte.
4. Confirmar la fecha límite con Tellus (Luma 5 oct vs. Passport 12 oct), que sigue abierta.

### Cambios obligatorios

1. Cuenta propia con una sola regla `Default` (passkey de la dueña + GuardianHold), sin `execute` ni `upgrade`.
2. GuardianHold: `cuenta.require_auth()`, passkey de la dueña presente, lista blanca estricta, rechazo de toda llamada a la cuenta misma, conversión tipada de argumentos y rechazo de destinos muxed.
3. Registro aparte con `queue` (autorizado por la cuenta), `cancel` (guardián o dueña) y marca de ejecutado solo desde GuardianHold; `ready_at` calculado, nunca recibido.
4. Almacenamiento persistente para registros y contadores.
5. Aviso al guardián como MUST.
6. Mensajes y pitch sin claims de novedad; competidores reales en la respuesta al jurado.

### Qué permanece igual

La tesis (veto asimétrico contra estafas de pago autorizado), una sola app con modos de dueña y guardián, cuatro pantallas, los estados, la segunda firma, la IA sin autoridad, el vocabulario prohibido, la demo de cinco escenas (ahora seis) y USDC en testnet.

### Qué probar antes de implementar la web

Los blockers 1–3, en ese orden. Después, GH-18 (anidadas), GH-20b (muxed), GH-29 (concurrencia) y los límites de recursos de una política que llama a otro contrato dentro de `__check_auth` (🔴 no medidos).

### Qué demostrar en el demo

Rechazos reales en el explorador, no pantallas: envío anticipado, envío detenido, guardián que intenta transferir, `approve`, y regla nueva desde la terminal. Además, "Reglas de esta cuenta" leída de la cadena y el enlace a los tests.

### Riesgos residuales

- **Humano:** firma a ciegas, un guardián que no se entera o no actúa, y un guardián malicioso que bloquea.
- **Operativo:** el backend puede callar avisos; no hay recuperación si se pierde la passkey.
- **Técnico:** contratos sin auditar; dependencia de una versión concreta de OZ; poderes del emisor de USDC.
- **Producto:** la duración de la retención y la demanda real no están validadas; ya existen soluciones parecidas en la banca tradicional.

### Fuentes

- [OpenZeppelin stellar-contracts — smart\_account/storage.rs](https://github.com/OpenZeppelin/stellar-contracts/blob/main/packages/accounts/src/smart_account/storage.rs) (commit b40c5ea)
- [OpenZeppelin stellar-contracts — smart\_account/mod.rs](https://github.com/OpenZeppelin/stellar-contracts/blob/main/packages/accounts/src/smart_account/mod.rs)
- [Ejemplo multisig-smart-account (constructor, execute, upgrade)](https://github.com/OpenZeppelin/stellar-contracts/blob/main/examples/multisig-smart-account/account/src/contract.rs)
- [Política Spending Limit](https://github.com/OpenZeppelin/stellar-contracts/blob/main/packages/accounts/src/policies/spending_limit.rs)
- [Tests de context rules (incluye downgrade)](https://github.com/OpenZeppelin/stellar-contracts/blob/main/packages/accounts/src/smart_account/test/context_rules.rs)
- [Interfaz fungible: transfer con MuxedAddress](https://github.com/OpenZeppelin/stellar-contracts/blob/main/packages/tokens/src/fungible/mod.rs)
- [Stellar — Authorization](https://developers.stellar.org/docs/learn/fundamentals/contract-development/authorization)
- [Stellar — Soroban's technical design decisions (re-entrada)](https://stellar.org/developers-blog/sorobans-technical-design-decisions-learnings-from-ethereum)
- [OpenZeppelin — TimelockController](https://docs.openzeppelin.com/contracts/5.x/api/governance#TimelockController)
- [Yandex Pay — verificación por contacto de confianza](https://zamin.uz/en/technology/204838-yandex-pay-launches-verification-feature-for-large-transfers-via-trusted-contact.html)
- [Monzo — Added Security](https://monzo.com/help/Account%20Security/addedsecurityfaqs)
