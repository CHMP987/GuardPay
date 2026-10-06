# GuardPay — MVP Proposal

Oct 4, 2026 · @Justin

## 1. Executive Verdict

**GuardPay MVP = una OpenZeppelin Smart Account en testnet protegida por una sola política Soroban propia, `GuardianHold`, con configuración fija al desplegar y dos vistas mínimas.** Con eso se demuestra la tesis: un pago a un desconocido queda retenido y atado a su destino y monto exactos, el guardián puede detenerlo, y nadie (backend, IA, guardián, ni el propio dueño bajo presión) puede saltarse la regla.

- **Construir:** cuenta OZ + `GuardianHold` (contactos de confianza con tope, retención atada por hash, cancelación del guardián, verificación de firmante y de función) + configuración fija al desplegar + vista usuario + vista guardián + `evidence.md`.
- **Eliminar o bajar:** `TrustedPayee` como contrato aparte (se funde en `GuardianHold`), Spending Limit de OZ (redundante), Stellar Channels, notificaciones, fast-track, freeze, espera para cambios de reglas (se reemplaza por configuración fija; una regla 2-de-2 dueño + guardián queda como SHOULD). La IA y la passkey pasan a SHOULD.
- **Núcleo:** veto asimétrico impuesto por contrato. “El guardián no puede gastar tu dinero. Solo puede detenerlo.”
- **Mayor riesgo técnico (nuevo):** en OZ, cuando una regla tiene políticas, **las firmas de la regla no se exigen solas**; la validación de firmantes se delega a las políticas ([smart\_account/mod.rs](https://raw.githubusercontent.com/OpenZeppelin/stellar-contracts/main/packages/accounts/src/smart_account/mod.rs), [Authorization flow](https://docs.openzeppelin.com/stellar-contracts/accounts/authorization-flow)). El diseño anterior (“passkey + Spending Limit + TrustedPayee”) habría permitido a **cualquiera** pagar a destinatarios de confianza sin firma. `GuardianHold` debe comprobar que el dueño firmó.
- **Mayor riesgo de producto:** demanda del guardián no validada y fecha de entrega en conflicto (Luma 5 oct vs Passport 12 oct).
- **Propiedad a demostrar:** *el pago que se ejecuta es exactamente el que se retuvo, solo después de la espera, solo si nadie lo detuvo, solo con la firma del dueño, y el guardián nunca puede moverlo a ningún lado.*

**Spike de `GuardianHold`: B — VIABLE CON CAMBIOS.** Detalle en la sección 4.

**Recomendación: 🟡 GO WITH CHANGES.**

## 2. What Changed From Current GuardPay

Comparado con [GuardPay — Propuesta para el pitch](https://claude.ai/code/artifact/946b02f7-6f80-412a-aa90-be0307135c04).

| Elemento actual | ¿Se mantiene? | ¿Se modifica? | ¿Se elimina? | Motivo |
| --- | --- | --- | --- | --- |
| Stellar + Soroban | ✓ |  |  | Autorización programable en `__check_auth` es el mecanismo |
| OpenZeppelin Smart Account | ✓ |  |  | Evita escribir la cuenta; ya resuelve reglas y firmantes |
| Passkey |  | ✓ (MUST → SHOULD) |  | La propiedad central no depende de passkey; un firmante ed25519 basta y reduce riesgo. Passkey mejora UX si da tiempo |
| Spending Limit de OZ |  |  | ✓ (a NICE) | Redundante con el tope por destinatario de `GuardianHold`; además, por sí solo no exige firma |
| `TrustedPayee` como contrato aparte |  | ✓ |  | Se funde en `GuardianHold`: una política en vez de dos, menos reglas que coordinar |
| `GuardianHold` | ✓ | ✓ |  | Añade verificación de firmante del dueño y restricción a `transfer` sobre un solo token |
| Payment Intent |  | ✓ |  | Deja de ser un objeto de app: es el registro on-chain del pago retenido (hash + hora + estado) |
| Guardián | ✓ |  |  | Es el diferenciador |
| Cancelación on-chain | ✓ |  |  | Parte de la propiedad central |
| Cambios de reglas con espera de 24–48 h |  | ✓ |  | Se reemplaza por regla 2-de-2 dueño + guardián con la política de umbral de OZ: más simple y ya existe |
| Freeze del guardián |  |  | ✓ (a FUTURE) | No es necesario para demostrar la tesis |
| IA |  | ✓ (MUST → SHOULD) |  | Quitarla no rompe la tesis; aporta a la demo y al criterio de UX |
| Stellar Channels / fees patrocinados |  |  | ✓ (a NICE) | En testnet las cuentas se fondean con Friendbot |
| USDC |  | ✓ |  | Cualquier token SAC demuestra lo mismo; USDC de testnet si el faucet funciona, si no un activo de prueba propio |
| Demo de 12 escenas |  | ✓ |  | 5 en vivo + 8 casos en `evidence.md` |
| Pitch “por qué Stellar” | ✓ | ✓ |  | Se precisa: lo que depende de Stellar es la autorización por políticas en la cuenta, no “velocidad y costo” |

## 3. Verified Research

**Solo los hallazgos que cambian el MVP.** Lectura de fuentes oficiales el 4 oct 2026.

### Hackathon

| Dato | Valor | Estado |
| --- | --- | --- |
| Nombre / organizador | Find Your Way: Hackathon — Costa Rica; Tellus Cooperative; sede TEC Cartago | 🟢 [Luma](https://luma.com/9h5jl18k) |
| Track | General Track (2.000 / 1.000 / 500 USDC + 2 × 250) | 🟢 [Passport](https://demo.stellarpassport.xyz/hackathons/find-your-way-meridian-hackathon) |
| Criterios oficiales (sin pesos) | Technical execution, meaningful use of Stellar, originality, potential impact, user experience, presentation quality | 🟢 Passport |
| Deadline | Luma: 5 oct, 4:00 p.m. Passport: 12 oct, 5:59 p.m. | 🔴 Contradicho |
| Equipos | 1–5 | 🟢 |
| Campos de entrega visibles | “Describe your project” | 🟢 Passport (proyectos públicos) |
| Video, repo, demo, límites de caracteres, reglas de descalificación | Requieren sesión | 🔴 No verificado |

🔵 Los proyectos más fuertes ya enviados adjuntan transacciones verificables y tests. 🟣 El MVP debe producir esa misma evidencia.

### Auditoría de afirmaciones previas

| Afirmación | Fuente anterior | ¿Sigue vigente? | Fuente primaria actual | Estado |
| --- | --- | --- | --- | --- |
| Una política recibe el contexto (`contract`, `fn_name`, `args`) | Auditoría final | Sí | [policies/mod.rs](https://raw.githubusercontent.com/OpenZeppelin/stellar-contracts/main/packages/accounts/src/policies/mod.rs) | 🟢 |
| Spending Limit lee el monto de `args[2]` en `transfer` | Auditoría final | Sí | [spending\_limit.rs](https://raw.githubusercontent.com/OpenZeppelin/stellar-contracts/main/packages/accounts/src/policies/spending_limit.rs) | 🟢 |
| “Spending Limit podría dejar pasar `approve`” | Auditoría final (riesgo) | **No**: rechaza toda función que no sea `transfer` y todo contexto que no sea de contrato | spending\_limit.rs | 🟢 Corregido |
| “Passkey + Spending Limit + TrustedPayee exige la firma del dueño” | Propuesta de pitch (implícito) | **No**: con políticas, la validación de firmantes se delega a ellas | [smart\_account/mod.rs](https://raw.githubusercontent.com/OpenZeppelin/stellar-contracts/main/packages/accounts/src/smart_account/mod.rs), [Authorization flow](https://docs.openzeppelin.com/stellar-contracts/accounts/authorization-flow) | 🔴 Contradicho |
| Las políticas deben autenticar a la cuenta en `enforce` | Nuevo | — | Spending Limit llama `smart_account.require_auth()` al inicio | 🟢 |
| `transfer_from` requiere la firma del spender, no del dueño | Nuevo | — | [Stellar token interface](https://developers.stellar.org/docs/tokens/token-interface) | 🟢 |
| Bancos de CR: topes por canal, sin espera ni contacto que frene | Auditoría final | Sí | [crhoy, oct 2025](https://www.crhoy.com/varios-bancos-ya-estan-aplicando-cambios-en-sinpe-movil-enterese-aqui-cuales-son/), [Ayuda BAC](https://ayuda.baccredomatic.com/transferencias_bancarias/enviar_dinero/como-guardar-y-administrar-cuentas-favoritas-para?country=es-cr) | 🟡 + 🟢 (BAC). Ausencias = “no encontramos evidencia” |
| 25.100 estafas informáticas 2018–ago 2025 | Auditoría final | Sí | UNA/Labcibe con datos del OIJ ([Semanario Universidad](https://semanariouniversidad.com/pais/estudio-una-25-mil-estafas-informaticas-y-8-mil-suplantaciones-de-identidad-los-delitos-que-acechan-a-los-costarricenses/)) | 🟡 |
| Ningún proyecto del evento tiene veto asimétrico | Auditoría final | Sí, a 36 proyectos el 4 oct | [Proyectos en Passport](https://demo.stellarpassport.xyz/hackathons/find-your-way-meridian-hackathon?tab=projects) | 🟢 a esa fecha; pueden llegar más |
| Demanda del guardián | — | — | — | 🔴 No validada |

**Costa Rica, en lo que importa al MVP:** revisamos BCCR, BN, BCR, Banco Popular, Davivienda, BCT y BAC. 🔴 No encontramos evidencia pública de que alguno retenga pagos a destinatarios nuevos o permita que un tercero detenga una transferencia. Chile solo como contraste: allá varios bancos limitan la primera transferencia a un destinatario nuevo durante 12–24 h (🟡, fuentes de 2019 y 2021).

## 4. Technical Feasibility Audit

**Conclusión del spike: B — VIABLE CON CAMBIOS.** La política puede atar el pago retenido al pago ejecutado, pero el diseño anterior tenía un agujero: no exigía la firma del dueño. Se corrige dentro de la misma política.

### Cómo se autoriza realmente un pago

1. 🟢 El token (contrato SAC de USDC) en `transfer(from, to, amount)` exige `from.require_auth()` ([token interface](https://developers.stellar.org/docs/tokens/token-interface)).
2. Como `from` es la smart account, Soroban llama a su `__check_auth` con el contexto de esa llamada: contrato, función y argumentos.
3. 🟢 La cuenta OZ busca la regla que el cliente indicó. Si la regla **no** tiene políticas, exige todas sus firmas. Si **tiene** políticas, “defer full signer validation to each policy’s `enforce()`” ([smart\_account/mod.rs](https://raw.githubusercontent.com/OpenZeppelin/stellar-contracts/main/packages/accounts/src/smart_account/mod.rs)).
4. 🟢 Cada política recibe `enforce(context, authenticated_signers, context_rule, smart_account)` ([policies/mod.rs](https://raw.githubusercontent.com/OpenZeppelin/stellar-contracts/main/packages/accounts/src/policies/mod.rs)).

### Qué puede y qué no puede inspeccionar `GuardianHold`

| Puede | No puede |
| --- | --- |
| Contrato llamado (el asset) | El motivo del pago o si el destinatario es un estafador |
| Nombre de la función | Otras llamadas de la misma transacción fuera de su contexto |
| Argumentos: `from`, `to`, `amount` | Datos fuera de la cadena (análisis de IA, mensajes) |
| Qué firmantes se autenticaron | Si el dueño está siendo presionado |
| Hora del ledger y su propio estado | Precios en colones |

🔵 El contexto no lo arma el cliente: lo arma el host de Soroban a partir de la invocación real, y las firmas cubren los argumentos. No es manipulable sin cambiar la llamada misma.

### Rutas de evasión revisadas

| Ruta | ¿Pasa por la política? | Cierre |
| --- | --- | --- |
| `transfer` desde la cuenta | Sí | `GuardianHold` decide |
| `approve` + `transfer_from` | `approve` sí (lo autoriza la cuenta); `transfer_from` no (lo autoriza el spender) | `GuardianHold` rechaza toda función distinta de `transfer`. Sin `approve`, `transfer_from` no tiene allowance. 🟢 Spending Limit de OZ hace lo mismo |
| `burn` | Sí | Rechazada por la misma regla |
| Elegir otra context rule | Solo si existe | El MVP tiene 3 reglas y ninguna deja pasar `transfer` sin `GuardianHold` o sin dueño + guardián |
| Llamar `enforce` directamente para consumir o marcar pagos | Sería posible si no se protege | `enforce` exige `smart_account.require_auth()`, como hace Spending Limit (🟢) |
| Desinstalar la política o cambiar reglas | Gestión de la cuenta | Solo por la regla de administración 2-de-2 |
| Cambiar contactos de confianza o guardián dentro de `GuardianHold` | Funciones de la política | Exigen firma de la cuenta **y** del guardián |
| Firmar sin el dueño | — | `GuardianHold` exige que el firmante del dueño esté en `authenticated_signers` |

### Mecanismo mínimo que garantiza la vinculación

- `queue(token, to, amount)` — firma: dueño (regla sin políticas → firma obligatoria). Guarda `id = hash(token, "transfer", from, to, amount)`, `ready_at`, estado `HELD`.
- `cancel(id)` — firma: guardián o dueño. Estado `CANCELLED`.
- `enforce` en cada `transfer`:
  - exige autenticación de la cuenta y firma del dueño;
  - exige token configurado y función `transfer`;
  - si `to` es de confianza y el monto cabe en su tope diario → aprueba y descuenta del tope;
  - si no, recalcula el hash y exige un registro `HELD`, con `ready_at` cumplido; lo pasa a `EXECUTED`.

| Debe ser inmodificable | Garantía |
| --- | --- |
| Asset, destino, monto, origen | Dentro del hash |
| Caller | El dueño debe firmar; la cuenta autoriza |
| Tiempo | `ready_at` contra el timestamp del ledger |
| Estado | `CANCELLED` y `EXECUTED` son terminales |

**Riesgos técnicos abiertos:**

- 🔵 Calcular el hash de los argumentos dentro de `enforce`: probable con el SDK de Soroban, no probado. Plan B: guardar token, destino y monto como campos y compararlos.
- 🔵 En versiones recientes del protocolo, el `to` de un `transfer` del SAC puede venir como dirección “muxed”. Si no coincide con la lista de confianza, el pago cae en retención: fallo seguro, pero hay que probarlo.
- 🔴 No revisamos límites de recursos (CPU/storage) de Soroban para esta política; es pequeña, el riesgo parece bajo.

## 5. Competitive Audit

**Cada pieza existe; la combinación “tercero que solo puede detener + pago retenido atado on-chain” no la encontramos.** No decimos “nadie lo hace”.

| Qué existe | Dónde | Qué le falta frente a GuardPay |
| --- | --- | --- |
| Multisig para familias y grupos | SoroSafe, Cosigna (este hackathon) | El segundo firmante es copropietario: puede proponer y aprobar gastos |
| Límites on-chain para agentes | AgentAllowance (este hackathon), Stellar Agent Guard, REAPP | Protege al dueño de su agente, no de un estafador; no hay tercero |
| Retención cancelable on-chain | Safe Delay Module (EVM) | Sin rol asimétrico; pensado para tesorerías |
| Contactos de confianza y guardianes | Argent / Ready (EVM, Starknet) | El guardián co-firma y recupera; no es un veto de pagos |
| Persona de confianza que recibe alertas | EverSafe (EE. UU.), “trusted contact” bancario | Alerta después; no detiene |
| Persona de confianza que revisa transferencias | Monzo (Reino Unido) | Lo controla el banco, no un contrato del usuario |

Fuentes: [Proyectos en Passport](https://demo.stellarpassport.xyz/hackathons/find-your-way-meridian-hackathon?tab=projects), [SafeDelay](https://github.com/gnosis/SafeDelay), [Argent / Ready](https://eco.com/support/en/articles/15254043), [EverSafe](https://www.eversafe.com/how-we-do-it/), [Monzo](https://monzo.com/help/Account%20Security/addedsecurityfaqs).

**¿Podría el jurado decir “es un multisig / spending limit / delay module”?** Sí, si la demo solo muestra un pago retenido y una cancelación. **Corrección:** la escena 5 (“el guardián intenta gastar y falla”) es obligatoria en vivo, porque es lo único que separa a GuardPay de un multisig, y la escena 4 (ejecutar lo cancelado falla) lo separa de una alerta.

## 6. Final Product Definition

**GuardPay es una wallet en Stellar donde los pagos a desconocidos quedan retenidos y una persona de confianza puede detenerlos sin poder tocar el dinero.**

|  |  |
| --- | --- |
| Problema | En estafas de ingeniería social la víctima autoriza el pago bajo presión; en Costa Rica no encontramos protecciones que permitan a otra persona intervenir antes de que sea irreversible |
| Usuario del MVP | Dueña de la cuenta (Laura) y su guardián (Diego) |
| Propuesta de valor | Paga al instante a quien conoces; si un pago parece peligroso, alguien de confianza puede detenerlo sin poder gastar tu dinero |
| Tesis | La autoridad sobre el dinero puede repartirse de forma asimétrica: el dueño decide, el guardián solo puede frenar, y la cadena lo hace cumplir aunque la app esté comprometida |
| Propiedad diferenciadora | Veto sin propiedad, ejecutado por contrato |

**Por qué Stellar, auditado:**

| Parte de GuardPay | ¿Depende de Stellar/Soroban? |
| --- | --- |
| Que la regla corra en la autorización de la cuenta (`__check_auth` + políticas) | Sí en esta implementación; en EVM habría que usar Safe + guard/módulo o ERC-4337 |
| Reglas por contexto y políticas listas (OZ Smart Accounts) | Ventaja práctica: no escribimos la cuenta |
| Firmantes WebAuthn/passkey en la cuenta | Ventaja práctica de OZ en Stellar |
| Token como contrato SAC con `require_auth` del `from` | Hace que todo `transfer` pase por la política |
| Velocidad y costo | Ayuda, pero no es el argumento |

🔵 **Respuesta honesta:** la idea se podría construir en Ethereum. En Stellar la construimos sin escribir la smart account, con la regla dentro de la autorización nativa de la cuenta, y con passkeys como firmantes. Eso es “meaningful use of Stellar”; “solo es posible en Stellar” sería falso.

## 7. MVP Scope

**Si solo tuviéramos tiempo para seis cosas, construiríamos exactamente las MUST de abajo, porque juntas demuestran que un tercero puede detener un pago sin poder gastar, y que ninguna capa fuera de la cadena puede saltarse eso.**

### Matriz (1–5; Complejidad y Riesgo: 5 = alto)

| Funcionalidad | Valor usuario | Valor hackathon | Complejidad | Riesgo | Dependencias | ¿MVP? |
| --- | --: | --: | --: | --: | --- | --- |
| Smart account OZ en testnet con firmante del dueño | 4 | 5 | 2 | 2 | OZ, testnet | MUST |
| `GuardianHold`: firma del dueño + solo `transfer` + un solo token | 3 | 5 | 2 | 3 | Smart account | MUST |
| `GuardianHold`: contactos de confianza con tope diario | 5 | 4 | 2 | 2 | Política | MUST |
| `GuardianHold`: pago retenido atado por hash, con hora y uso único | 5 | 5 | 3 | 3 | Política | MUST |
| Guardián puede detener (y nada más) | 5 | 5 | 2 | 2 | Pago retenido | MUST |
| Vista usuario + vista guardián + tests + `evidence.md` | 4 | 5 | 3 | 2 | Todo lo anterior | MUST |
| Análisis de mensaje con IA | 3 | 3 | 2 | 2 | LLM | SHOULD |
| Passkey | 4 | 3 | 3 | 3 | `smart-account-kit` | SHOULD |
| Dueño cancela su propio pago retenido | 3 | 2 | 1 | 1 | Pago retenido | SHOULD |
| Regla 2-de-2 (dueño + guardián) para cambiar contactos o límites | 3 | 3 | 2 | 2 | Política de umbral de OZ | SHOULD |
| Spending Limit global de OZ | 2 | 2 | 1 | 2 | — | NICE |
| Fees patrocinados (Stellar Channels) | 3 | 2 | 2 | 2 | API key | NICE |
| Notificaciones push al guardián | 4 | 2 | 3 | 2 | Servicio externo | NICE |
| Freeze, fast-track, espera para cambios de reglas | 3 | 2 | 3 | 3 | — | FUTURE |

### Ronda 1 de reducción: “si lo quitamos, ¿se sigue demostrando la tesis?”

| Quitado | ¿Tesis intacta? | Motivo |
| --- | --- | --- |
| `TrustedPayee` como contrato aparte | Sí | Se funde en `GuardianHold` |
| Spending Limit de OZ | Sí | El tope por destinatario ya existe |
| Stellar Channels | Sí | Friendbot fondea las cuentas de testnet |
| Notificaciones push | Sí | La vista del guardián consulta la cadena |
| Freeze, fast-track, espera para cambios de reglas | Sí | No son parte de la propiedad central |

### Ronda 2 de reducción

| Quitado | ¿Tesis intacta? | Motivo |
| --- | --- | --- |
| IA | Sí → SHOULD | La tesis es sobre autoridad, no sobre detección |
| Passkey | Sí → SHOULD | Un firmante ed25519 prueba lo mismo |
| Regla 2-de-2 de administración | Sí → SHOULD | Con la configuración **fija al desplegar** (sin regla de administración), nadie puede cambiar reglas: misma garantía, menos código |
| Cancelación del dueño | Sí → SHOULD | La del guardián es la que diferencia |
| Contactos de confianza | **No** | Sin carril inmediato, GuardPay es solo un delay module |
| Vista del guardián | **No** | El criterio de UX y la demo lo necesitan |

### MUST (6)

1. Smart account OZ en testnet con un firmante del dueño y **configuración fija al desplegar**.
2. `GuardianHold` exige: autenticación de la cuenta, firma del dueño, función `transfer`, token configurado.
3. Contactos de confianza con tope diario: pago inmediato.
4. Pago retenido atado por hash a token, origen, destino y monto, con hora de liberación y uso único.
5. El guardián puede detener un pago retenido y no tiene ninguna otra capacidad.
6. Vista usuario + vista guardián + suite de tests + `evidence.md` con un hash por caso.

### SHOULD

Análisis de mensaje con IA · passkey · cancelación por el dueño · regla 2-de-2 para cambiar contactos o límites · USDC oficial de testnet (si no, un activo de prueba propio).

### NICE

Spending Limit global de OZ · fees patrocinados · notificación push · manejo explícito de direcciones “muxed”.

### FUTURE

Freeze · fast-track dueño + guardián · espera para cambios de reglas · varios guardianes · recuperación de cuenta · auditoría + mainnet · rampa con un proveedor registrado ante SUGEF · integración con bancos · pagos de agentes.

### DO NOT BUILD

| Qué | Por qué |
| --- | --- |
| SINPE / integración bancaria | Requiere acuerdos con bancos; no demuestra la tesis |
| Fiat, KYC, rampa | Regulación (Ley 10961) y tiempo; testnet basta |
| Custodia o escrow | Agrega riesgo regulatorio y no es necesario: la política retiene sin mover fondos |
| Mainnet | Contratos sin auditar |
| Multichain, token propio, NFT, staking, yield, remesas | No aportan a la tesis |
| Agentes autónomos, IA que decide o firma | Aumentan riesgo; AgentAllowance ya ocupa ese espacio en el evento |
| Voz, app nativa, internacionalización, dashboards, analytics | No aportan a la demo |
| Sistema completo de notificaciones | La vista del guardián basta para la demo |

## 8. User Flow

1. Entrar con su firmante (passkey si está en SHOULD; si no, llave de prueba).
2. Elegir destinatario (de confianza o nuevo) e ingresar monto.
3. Ver el carril previsto: “Sale ya” o “Quedará retenido X min; Diego puede detenerlo”.
4. Confirmar con su firma.
5. Ver el resultado: enviado, o retenido con cuenta regresiva.
6. Si quedó retenido y nadie lo detuvo: ejecutarlo al vencer, con su firma.
7. (SHOULD) Pegar un mensaje sospechoso y ver el análisis.

## 9. Guardian Flow

1. Abrir su vista (lee los pagos retenidos directamente de la cadena).
2. Ver destinatario, monto, hora de liberación y, si existe, el análisis de la IA.
3. Tocar “Detener” y firmar.
4. Ver el pago como detenido.

No hay otra acción disponible para el guardián en el MVP.

## 10. AI Role

**La IA es SHOULD: aporta a la demo y a la experiencia, pero la tesis se sostiene sin ella.**

| Hace | No hace |
| --- | --- |
| Lee un mensaje pegado y devuelve señales (urgencia, suplantación, amenaza, cuenta nueva) y un nivel de riesgo | Elegir el carril (lo decide el contrato: destinatario de confianza o no) |
| Explica en lenguaje simple al usuario y al guardián | Crear, modificar o firmar pagos |
|  | Tener herramientas que escriban en la cadena, claves o acceso al backend de firma |

**¿Es suficiente la regla “la IA puede aumentar la cautela, pero nunca reducir las restricciones on-chain”?** 🔵 Sí para el dinero, porque la cadena no consulta a la IA. No es suficiente para la **persona**: una IA engañada por prompt injection puede decir “esto es seguro” y convencer a la víctima de ejecutar el pago al vencer. Mitigación: la interfaz nunca muestra “seguro”; muestra solo señales encontradas o “no se encontraron señales”, y el pago a un desconocido se retiene igual.

## 11. Security Model

**Lo que garantiza la cadena (5 propiedades):**

1. **P1 Vínculo exacto:** un pago retenido solo puede ejecutarse con el mismo token, origen, destino y monto.
2. **P2 Tiempo y estado:** nada se ejecuta antes de su hora, después de ser detenido, ni dos veces.
3. **P3 Guardián sin gasto:** el guardián no puede mover fondos por ninguna ruta.
4. **P4 Solo el dueño inicia:** todo `transfer` exige la firma del dueño; solo `transfer` sobre el token configurado.
5. **P5 Reglas fijas:** nadie puede quitar la protección ni agregar contactos de confianza unilateralmente.

**Lo que NO garantiza:** que alguien detenga el pago a tiempo; pagos fuera de GuardPay (SINPE, banco); que el contacto de confianza sea honesto (solo su tope lo limita); que el dueño no ejecute un pago al vencer si sigue engañado.

| Amenaza | ¿Puede ocurrir? | Capa que bloquea | On/off-chain | ¿MVP? | ¿Demostrar en público? |
| --- | --- | --- | --- | --- | --- |
| Backend comprometido | Sí | No firma por el dueño; P4 | On | Sí | Evidence |
| IA comprometida / prompt injection | Sí | La IA no tiene herramientas de escritura | Off + On | Sí (si hay IA) | Evidence |
| Frontend manipulado | Sí | El pago a otro destino queda retenido (P1) y el guardián lo ve | On | Sí | Evidence |
| Usuario se salta la UI | Sí | Política en `__check_auth` | On | Sí | **En vivo** (escena 4) |
| Guardián malicioso o comprometido | Sí | P3: solo puede detener | On | Sí | **En vivo** (escena 5) |
| Atacante con sesión de la app | Sí | Sin firma del dueño no hay pago (P4) | On | Sí | Evidence |
| Cambiar destino o monto | Sí | P1 | On | Sí | Evidence |
| `approve` | Sí | P4: solo `transfer` | On | Sí | Evidence |
| `transfer_from` | Solo con allowance previa | Sin `approve` no hay allowance | On | Sí | Evidence |
| Llamar al token directo | Sí | El token exige `require_auth` de la cuenta → política | On | Sí | Evidence |
| Ejecutar un pago detenido | Sí | P2 | On | Sí | **En vivo** (escena 4) |
| Ejecutar antes de tiempo | Sí | P2 | On | Sí | Evidence |
| Cambiar reglas, agregar contacto, subir límite | Sí | P5: configuración fija (MUST) o 2-de-2 (SHOULD) | On | Sí | Evidence |
| Llamar `enforce` directamente | Sí | `enforce` exige autenticación de la cuenta | On | Sí | Evidence |

## 12. Demo

**Cinco escenas en vivo, cada una con su transacción en testnet.** Lo demás va a `evidence.md`. La demo prueba seguridad real (la cadena rechaza), no una interfaz que parece segura.

| # | Escena | Acción | Resultado esperado | Propiedad |
| --- | --- | --- | --- | --- |
| 1 | Pago normal | Laura paga 10 a Diego (de confianza, dentro del tope) | Ejecutado en segundos | Carril inmediato |
| 2 | Pago riesgoso | Laura paga 150 a una cuenta nueva (si hay IA: antes pega el mensaje del falso banco). Se registra la retención. Acto seguido intentamos ejecutarlo | Retenido; la ejecución inmediata es **rechazada** | P1, P2 |
| 3 | Guardián | Diego ve el pago en su vista y lo detiene | Estado detenido | Veto |
| 4 | Ataque | Desde la terminal, saltando la app, intentamos ejecutar el pago detenido | **Rechazado por Soroban** | P2 |
| 5 | Guardián intenta gastar | Diego firma una transferencia desde la cuenta de Laura hacia sí mismo | **Rechazado por Soroban** | P3, P4 |

**En `evidence.md`:** pago retenido que madura y se ejecuta; destino cambiado; monto cambiado; `approve`; pago sobre el tope; intento de cambiar reglas; llamada directa a `enforce`; mensaje con prompt injection (si hay IA).

## 13. Acceptance Criteria

**El MVP está terminado cuando los 6 MUST cumplen su criterio, los 16 casos de prueba pasan y las 5 escenas tienen hash en `evidence.md`.**

| MUST | Dado | Cuando | Entonces | Prueba | Depende de |
| --- | --- | --- | --- | --- | --- |
| 1. Cuenta con configuración fija | Cuenta desplegada en testnet con `GuardianHold` instalado | Cualquiera intenta agregar o quitar reglas o políticas | Falla | T13 | OZ |
| 2. Firma del dueño y solo `transfer` | Un `transfer` o cualquier otra función del token desde la cuenta | Sin firma del dueño, o con función distinta de `transfer`, u otro token | Falla | T8, T9, T10, T14 | 1 |
| 3. Contactos de confianza | Diego en la lista con tope 50/día | Laura paga 10, luego 45 | El primero pasa; el segundo falla (o se retiene) | T1, T11 | 2 |
| 4. Pago retenido atado | Laura retiene 150 a X | Se intenta ejecutar antes de la hora, con otro destino, con otro monto, o dos veces | Todos fallan; solo pasa el exacto, al vencer, una vez | T2, T3, T5, T6, T7 | 2 |
| 5. Guardián solo detiene | Pago retenido | Diego llama “detener” | Estado `CANCELLED`; cualquier ejecución posterior falla; Diego no puede transferir | T4, T8 | 4 |
| 6. Vistas + evidencia | Todo lo anterior desplegado | Se corre la demo | Cada escena muestra su hash en Stellar Expert | Revisión manual | 1–5 |

### Casos de prueba (16)

| # | Caso | Debe demostrar |
| --- | --- | --- |
| T1 | Pago a contacto de confianza dentro del tope | Se ejecuta |
| T2 | Pago a destinatario nuevo sin retención previa | Falla |
| T3 | Pago retenido ejecutado antes de la hora | Falla |
| T4 | Pago retenido detenido y luego ejecutado | Falla |
| T5 | Pago retenido con destino distinto | Falla |
| T6 | Pago retenido con monto distinto | Falla |
| T7 | Pago retenido maduro ejecutado dos veces | El primero pasa, el segundo falla |
| T8 | Guardián firma un `transfer` desde la cuenta | Falla |
| T9 | `approve` desde la cuenta | Falla |
| T10 | `transfer_from` sin allowance | Falla |
| T11 | Pago a contacto de confianza sobre el tope | Falla o pasa a retención, según diseño elegido |
| T12 | Pago retenido maduro y no detenido | Se ejecuta con la firma del dueño |
| T13 | Agregar contacto de confianza, subir tope o quitar la política sin las firmas requeridas | Falla |
| T14 | Transacción armada por el backend sin firma del dueño (backend comprometido) | Falla |
| T15 | Llamada directa a `enforce` por un tercero | Falla |
| T16 | Mensaje con prompt injection (si hay IA) | La IA puede equivocarse; el pago a destinatario nuevo se retiene igual |

## 14. Decision Matrix

**Escala (🟣 juicio, no medición):** 1–3 débil o sin evidencia · 4–6 aceptable con dudas abiertas · 7–8 sólido con evidencia · 9–10 fuerte y verificado en fuente primaria.

| Dimensión | Evaluación | Evidencia | Riesgo | Decisión |
| --- | --: | --- | --- | --- |
| Problema | 7 | 🟡 OIJ/UNA; modalidades de estafa autorizada por la víctima | Sin montos públicos | Mantener |
| Usuario | 5 | 🔴 Demanda del guardián no validada | Rechazo a que otro detenga pagos | Entrevistas antes del video |
| Diferenciación | 7 | 🟢 36 proyectos del evento revisados; 🟡 bancos de CR | Que lo vean como multisig | Escena 5 obligatoria en vivo |
| Stellar fit | 7 | 🟢 Token SAC exige `require_auth` del `from` | “Se puede en EVM” | Decirlo con honestidad |
| Soroban fit | 9 | 🟢 Código de OZ: `Policy.enforce` con contexto | — | Núcleo |
| GuardianHold | 7 | 🟢 Mecanismo; 🔵 hash de args no probado | Firma del dueño debía verificarse en la política | B — viable con cambios |
| IA | 4 | 🔵 Solo análisis de mensajes | Puede dar falsa tranquilidad | SHOULD |
| Seguridad | 8 | 🟢 Rutas de evasión cerradas en diseño | Contrato propio sin auditar | Tests T1–T16 |
| UX | 6 | 🔵 Dos vistas, tres estados | La espera molesta | Mantener mínimo |
| Viabilidad MVP | 8 | 🔵 Una política + cuenta OZ | Fecha límite en conflicto | Confirmar hoy |
| Demo | 9 | 🔵 5 escenas con rechazo on-chain | Testnet lenta | Video de respaldo |
| Hackathon fit | 8 | 🟢 “wallets” y “payments” en el texto oficial; 6 criterios | Campos del formulario no vistos | Revisar al iniciar sesión |

## 15. Final Recommendation

**🟡 GO WITH CHANGES.** La tesis se puede demostrar con seguridad real en cadena y con un alcance pequeño. Los cambios son concretos: una sola política que verifique la firma del dueño y la función, configuración fija, IA y passkey como SHOULD, y la escena del guardián que intenta gastar como prueba central.

**GuardPay MVP = OZ Smart Account + `GuardianHold` + vista usuario + vista guardián + evidencia.** Con 6 funcionalidades MUST, 5 propiedades de seguridad, 2 flujos, 16 casos de prueba y 5 escenas de demo.

### Las diez decisiones

| # | Pregunta | Decisión |
| --- | --- | --- |
| 1 | ¿Construir como está planteado? | **Sí, con cambios** (los de la sección 2) |
| 2 | ¿`GuardianHold` en el MVP? | **Sí.** Es donde vive la propiedad central; sin él, GuardPay es una interfaz |
| 3 | ¿Payment Intent? | **Sí, pero como registro on-chain** (hash + hora + estado), no como objeto de app |
| 4 | ¿`TrustedPayee`? | **La función sí, el contrato no.** Se funde en `GuardianHold`; sin carril inmediato GuardPay es un delay module |
| 5 | ¿IA? | **SHOULD.** No sostiene la tesis; ayuda a la demo y a la UX si da tiempo |
| 6 | ¿El guardián es el diferenciador? | **Sí, solo si se muestra que no puede gastar.** Si no, el jurado lo leerá como multisig |
| 7 | ¿Qué propiedad demostrar? | El pago ejecutado es exactamente el retenido, solo después de la espera, solo si nadie lo detuvo, solo con firma del dueño; el guardián no puede mover fondos |
| 8 | ¿MVP mínimo? | Los 6 MUST de la sección 7 |
| 9 | ¿Qué eliminar aunque nos guste? | Spending Limit de OZ, Channels, notificaciones, freeze, fast-track, espera para cambios de reglas, el ángulo de agentes y la IA como pieza central |
| 10 | ¿Qué NO construir? | SINPE, bancos, fiat, KYC, custodia, mainnet, multichain, token, NFT, staking, yield, remesas, agentes autónomos, voz, app nativa, dashboards, analytics |

**Prueba final de calidad:** con este documento un equipo sabe qué construir (6 MUST) y qué no; cada funcionalidad que queda está porque la tesis se rompe sin ella; y la demo muestra transacciones rechazadas por la cadena, no una pantalla que dice “protegido”.

**Pendiente antes de empezar:** confirmar la fecha de entrega (Luma 5 oct vs Passport 12 oct) y abrir el formulario de Passport con sesión para ver los campos obligatorios.
