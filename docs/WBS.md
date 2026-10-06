# GuardPay — WBS (entregables)

Oct 5, 2026. Fuente de verdad de alcance, horas y dependencias.

Tres niveles: Proyecto → entregables principales → paquetes de trabajo. Las tareas de cada paquete van en la descripción, no como cuarto nivel. La versión para agentes está en [WBS-AGENTES.md](./WBS-AGENTES.md).

**Regla del 100%:** MUST y SHOULD del plan de construcción. NICE, FUTURE y DO NOT BUILD quedan fuera (sección 15 del plan: "Lo que se construye. Nada más."). Gestión, pruebas, documentación y cierre están incluidos.

Las horas no salen de los documentos: son estimación para una persona senior, con pruebas unitarias incluidas. Spikes: A = 1 día, B y C = medio día cada uno (tiempos del plan).

**Totales:** 205 h. 187,5 h obligatorias y 17,5 h SHOULD (1.4.5, 1.8.4, 1.8.5, 1.9.4). El camino crítico (1.2, 1.3.1–1.3.3, 1.4 sin SHOULD, 1.5, 1.11, 1.12) suma unas 86 h.

| ID WBS | Nombre | Descripción breve | Entregable | Estimación (horas) | Dependencias |
| --- | --- | --- | --- | --- | --- |
| **1** | **GuardPay MVP (Stellar Testnet)** | Billetera con retención y veto del guardián impuesta por contrato, app KMP, IA sin autoridad, evidencia y entrega al hackathon | MVP entregado con evidencia verificable | **205** | — |
| **1.1** | **Gestión del proyecto** | Gates, plan, riesgos y control | Línea base y decisiones documentadas | **9** | — |
| 1.1.1 | Resolución de gates G1 y G3 | Confirmar la fecha límite (Luma 5 oct o Passport 12 oct), la inscripción en Passport, los campos obligatorios del formulario y si hay Mac disponible | Respuestas escritas en el README (o "SIN CONFIRMAR — bloquea") | 1 | — |
| 1.1.2 | Plan y línea base de alcance | Este WBS, el cronograma de 7 días, el camino crítico P0 → P1-A → P2 → P3 → P9 → P10 y las reglas de recorte | Plan aprobado | 2 | 1.1.1 |
| 1.1.3 | Registro de riesgos y decisiones | Seguimiento de R0–R12 y de las decisiones GO / REDESIGN / degradación de cada spike | Registro de riesgos y log de decisiones | 2 | 1.1.2 |
| 1.1.4 | Seguimiento y control de cambios | Estado diario por fase; fases que no cumplen la definición de terminado marcadas INCOMPLETA en el README | Tabla de estado de fases actualizada | 4 | 1.1.2 |
| **1.2** | **Fundación del repositorio (P0)** | Repo versionado, autoridad documental y toolchain | Repo listo para P1 | **7,5** | 1.1.1 |
| 1.2.1 | `.gitignore` y commit de docs | Excluir claves, `.env`, `*.jks`, `*.keystore`, `*.seed`, `secrets/`, `target/`, `build/`; el commit de docs ya existe | `.gitignore` commiteado sin secretos | 0,5 | — |
| 1.2.2 | README inicial | Qué es GuardPay sin la palabra "seguro", frase guía, orden de autoridad, estado de fases, limitaciones y respuestas G1–G3 | `README.md` | 1,5 | 1.2.1, 1.1.1 |
| 1.2.3 | Estructura de carpetas y guía de evidencia | `contracts/`, `app/`, `evidence/`, `scripts/`, `spikes/` con `.gitkeep`; etiquetas REAL / SIMULATED / ILLUSTRATIVE y frases prohibidas | Árbol de carpetas + `evidence/README.md` | 1 | 1.2.1 |
| 1.2.4 | Registro de dependencias | Una fila por dependencia crítica: versión exacta, fuente, licencia, oficial o comunidad, riesgo y alternativa; nunca `latest` | `docs/DEPENDENCIES.md` | 1,5 | 1.2.5 |
| 1.2.5 | Toolchain verificado y gate G2 | `stellar-cli`, Rust + target wasm, JDK, Android SDK; comprobar que OZ `b40c5ea` compila con soroban-sdk 28 | Versiones registradas [FACT] + respuesta a G2 | 3 | 1.2.1 |
| **1.3** | **Spikes técnicos (P1)** | Convertir las incógnitas críticas en hechos o en un rediseño | Tres veredictos con evidencia | **20** | 1.2 |
| 1.3.1 | Spike A0: binding del digest | Correr el test de OZ `do_check_auth_rule_selection_downgrade_fails` contra el pin | Resultado literal; si falla, PARA | 1 | 1.2.5 |
| 1.3.2 | Spike A1: reentrada y recursos (R6) | `enforce` mínimo que lee y escribe en `HoldRegistry` dentro de `__check_auth`; medir CPU y lecturas/escrituras | Medición + veredicto GO o fallback 7.A | 2 | 1.3.1 |
| 1.3.3 | Spike A2–A3: contratos mínimos y 13 tests | Los tres contratos con lógica mínima; GH-01, 04, 05, 06, 09, 13, 20–23, 24, 25, 26; congelar las firmas de las funciones | `evidence/security/spike-a.md` + firmas congeladas | 7 | 1.3.2 |
| 1.3.4 | Spike B: Gemma 4 E2B en el teléfono real | App desechable: 10 mensajes; RAM, carga, latencia, tok/s, temperatura, batería, OOM, segundo plano, % de JSON parseable | `evidence/ai/benchmark.md` con números y modelo del teléfono | 5 | 1.2.5, 1.1.1 |
| 1.3.5 | Spike C: firma desde Kotlin | Leer la demo del SDK; firmar con ed25519 un `transfer` aceptado por `__check_auth`; verificar `sha256(payload ‖ ids)` | `evidence/stellar/spike-c.md` con hash REAL | 5 | 1.3.3 |
| **1.4** | **Contratos Soroban (P2)** | Los tres contratos completos con los tests MUST | Contratos + 29 tests MUST en verde | **26** | 1.3.3 |
| 1.4.1 | Workspace Cargo y OZ vendorizado | Workspace con los tres crates; OZ fijado por commit `b40c5ea` en `vendor/` | Workspace compilando | 1,5 | 1.3.3 |
| 1.4.2 | Contrato `account` | Una sola regla `Default` (firmante = dueña, política = GuardianHold); sin `execute`, `upgrade` ni funciones de administración | Crate `contracts/account` | 3 | 1.4.1 |
| 1.4.3 | Contrato `guardian_hold` | `install` una sola vez; `enforce`: `require_auth` de la cuenta, presencia de la dueña, lista blanca, conversión tipada, rechazo de destinos muxed, tope diario con control de desbordamiento | Crate `contracts/guardian_hold` | 6 | 1.4.1 |
| 1.4.4 | Contrato `hold_registry` | `queue`, `cancel` (guardián), `mark_executed` (solo GuardianHold) y lecturas; `ready_at` calculado; un registro activo por clave; almacenamiento persistente; eventos | Crate `contracts/hold_registry` | 4 | 1.4.1 |
| 1.4.5 | Funciones SHOULD del contrato | La dueña cancela su propio pago; `expires_at` (GH-30) | Ramas SHOULD con sus tests | 1,5 | 1.4.3, 1.4.4 |
| 1.4.6 | Suite de tests MUST | GH-01 a GH-27 (con 07b, 18b y 20b), GH-31, GH-32; test de persistencia al avanzar ledgers; test de desbordamiento con i128 | `evidence/policies/tests.md` con la salida literal | 8 | 1.4.2–1.4.4 |
| 1.4.7 | Revisión manual de la lista blanca | Revisar `enforce` línea por línea contra la tabla; grep de `execute`, `upgrade` y `ready_at` | `evidence/policies/whitelist-review.md` | 2 | 1.4.3, 1.4.6 |
| **1.5** | **Despliegue en testnet (P3)** | Demo reproducible con un comando | Contratos desplegados con direcciones y hashes | **10** | 1.4 |
| 1.5.1 | Script de despliegue idempotente | PowerShell y bash: claves en un archivo ignorado, Friendbot, SAC de USDC (o uno de prueba, declarado como tal), despliegue, firmante, `install` (3 contactos, tope 50, 120 s, 600 s), 500 USDC | `scripts/deploy-testnet.*` + `evidence/stellar/deployment.md` | 4 | 1.4.6, 1.4.7, 1.2.1 |
| 1.5.2 | Script de lectura de reglas | Leer de la cadena el número de reglas, la política y el firmante | `scripts/read-rules.*` | 1,5 | 1.5.1 |
| 1.5.3 | Tests ★ en testnet | GH-02 a GH-10 y GH-16 contra testnet, un hash por test | 10 hashes REAL | 3 | 1.5.1 |
| 1.5.4 | Verificación on-chain de la configuración | Una regla, política = GuardianHold, sin `execute` ni `upgrade`, segundo `install` rechazado | `evidence/smart-account/rules-onchain.md` | 1,5 | 1.5.2 |
| **1.6** | **Núcleo KMP compartido (P4)** | `commonMain` sin dependencias de plataforma | Módulo `shared` con tests en verde | **19,5** | 1.3.3 |
| 1.6.1 | Proyecto Gradle KMP | `libs.versions.toml` con versiones exactas; targets android, iosArm64, iosSimulatorArm64 | Proyecto compilando | 3 | 1.2.5, 1.3.3 |
| 1.6.2 | Capa `domain` | Estados del pago como tipos sellados, `HeldPayment`, `TrustedContact`, `Lane`, `RiskSignal`; máquina de estados | Paquete `domain` | 3 | 1.6.1 |
| 1.6.3 | Capa `stellar` | Interfaz `StellarGateway`, `KmpStellarGateway` sobre el SDK 1.14.0, `FakeStellarGateway` solo en tests | Paquete `stellar` | 6 | 1.6.2, 1.3.5 |
| 1.6.4 | Interfaces `expect` | `Signer`; `GuardPayAI` con una sola función, `analyzeMessage`; `Suggestion` con dos valores | Paquetes `signing` y `ai` | 1 | 1.6.2 |
| 1.6.5 | Cálculo del carril previsto | Leer la configuración on-chain y anticipar Immediate o MustHold (informativo; decide el contrato) | Función de carril con tests en los bordes | 1,5 | 1.6.3 |
| 1.6.6 | Tests de dominio y de arquitectura | Transiciones válidas e inválidas, parseo de la cadena y del JSON de la IA; `commonMain` sin android, LiteRT-LM ni WebAuthn | `allTests` verde + `evidence/architecture/kmp-layers.md` | 3 | 1.6.2–1.6.5 |
| 1.6.7 | Módulos de app Android e iOS | Arranque, permisos y espacio para descarga del modelo y notificaciones; iOS compila solo si G3 dice que hay Mac | `app/android`, `app/ios` | 2 | 1.6.1, 1.1.1 |
| **1.7** | **UI Compose Multiplatform (P5)** | Las 4 pantallas y el modo guardián según la propuesta visual | Pantallas navegables con datos de testnet | **31,5** | 1.6 |
| 1.7.1 | Tokens de diseño | Paleta exacta y tonos derivados, IBM Plex Sans/Mono, espaciado, radios, una sola sombra, los 10 íconos Lucide | Tema Compose | 2 | 1.6.1 |
| 1.7.2 | Componentes de estado | Chips de estado (color + ícono + forma + frase), sello "En Stellar", línea de espera | Biblioteca de componentes | 4 | 1.7.1, 1.6.2 |
| 1.7.3 | Pantalla A: Entrada | Mensaje en 3 líneas y acceso; el modo lo decide la cuenta | Pantalla Entrada | 1,5 | 1.7.2 |
| 1.7.4 | Pantalla B: Inicio | Saldo, tarjeta del guardián, pagos en espera, recientes, contactos con tope restante, "Reglas de esta cuenta" leída de la cadena | Pantalla Inicio | 4 | 1.7.2, 1.6.3, 1.5.2 |
| 1.7.5 | Pantalla C: Pagar + hoja "Revisar y firmar" | Destino, monto, tarjeta de carril, campo opcional para el mensaje, "Retener este pago", verbo exacto en el botón de firma | Pantalla Pagar + hoja | 5 | 1.7.2, 1.6.5 |
| 1.7.6 | Pantalla D: Detalle (dueña) | Estado, línea de espera, acción según el estado, "Ver en Stellar" con qué prueba cada fila, último valor conocido si falla la lectura | Pantalla Detalle | 4 | 1.7.2, 1.6.3 |
| 1.7.7 | Modo guardián | Banda Navy, lista, detalle del pago (orden fijo) y confirmación "Detener"; sin monto, sin Pagar ni Aprobar | Pantallas del guardián | 4 | 1.7.6 |
| 1.7.8 | Responsive y accesibilidad | Desde 360 px hasta columna + panel en ≥1024 px; WCAG 2.2 AA, áreas de 44 px, foco, lectores de pantalla | Layouts adaptativos | 3 | 1.7.3–1.7.7 |
| 1.7.9 | Pruebas de UI | Snapshots de los estados; test que falla si aparece "seguro" o "protegido"; test a 360 px; contraste; revisión en escala de grises | Tests en verde + `evidence/demo/screens/` | 4 | 1.7.8 |
| **1.8** | **Autenticación (P6)** | La dueña firma desde el teléfono y la cadena lo acepta | Ciclo queue → esperar → enviar desde el teléfono | **20** | 1.6, 1.5 |
| 1.8.1 | `Signer` Android | Clave ed25519 en Android Keystore, firma con biometría o bloqueo; no exportable ni en logs | `actual` de Android | 4 | 1.6.4, 1.3.5 |
| 1.8.2 | Firma y envío de transacciones | `queue` y `transfer` (dueña), `cancel` (guardián); verificación del digest; registro del firmante en el despliegue | Integración de firma | 4 | 1.8.1, 1.5.1 |
| 1.8.3 | Pruebas de autenticación | GH-24 y GH-01 desde la app contra testnet; test que falla si el guardián aparece como firmante | `evidence/smart-account/signing.md` | 2 | 1.8.2 |
| 1.8.4 | Passkey WebAuthn (SHOULD) | Firmante passkey con el soporte del SDK; test GH-33 | Firmante passkey + GH-33 | 6 | 1.8.2 |
| 1.8.5 | `Signer` iOS (SHOULD, depende de G3) | Clave en Keychain | `actual` de iOS | 4 | 1.8.1, 1.1.1 |
| **1.9** | **IA on-device (P7)** | Función texto → análisis, sin ninguna autoridad | Análisis en Android con números medidos | **18** | 1.3.4, 1.6 |
| 1.9.1 | `actual` Android con LiteRT-LM + Gemma | API oficial ≥ 0.12.0; descarga del modelo en la app con progreso; liberar el modelo al salir | `actual` de `GuardPayAI` | 6 | 1.3.4, 1.6.4 |
| 1.9.2 | Prompt, parser y filtros | JSON estricto; si no parsea, `NoClearSignals`; nunca texto crudo en pantalla; filtro de "seguro"; streaming | Pipeline de análisis | 3 | 1.9.1 |
| 1.9.3 | Pruebas de la IA | 10 mensajes (tasa de parseo); 5 casos de prompt injection (GH-12); test que falla si la IA comparte grafo con `Signer` o `StellarGateway` | `evidence/ai/prompt-injection.md` + tests en verde | 3 | 1.9.2, 1.7.5 |
| 1.9.4 | `actual` iOS (SHOULD) | SPM + puente Swift; si no sale, la UI dice "análisis no disponible en este dispositivo" | `actual` de iOS o fallback documentado | 6 | 1.9.2, 1.1.1 |
| **1.10** | **Aviso al guardián (P8)** | Notificación local sin servidor | Aviso funcionando entre dos dispositivos | **7** | 1.5, 1.7 |
| 1.10.1 | `HoldWatcher` compartido | Consulta periódica al registro de los pagos activos de la cuenta vigilada | `HoldWatcher.kt` | 2 | 1.6.3, 1.5.1 |
| 1.10.2 | Notificación local Android | Consulta cada 30–60 s; destino y monto leídos de la cadena; enlace al Detalle; botón "Actualizar" | Módulo de notificaciones | 4 | 1.10.1, 1.7.7 |
| 1.10.3 | Prueba cronometrada | Un `queue` produce la notificación en menos de 60 s; el destino coincide con la cadena | `evidence/guardian/notification.md` | 1 | 1.10.2, 1.8.2 |
| **1.11** | **Seguridad adversarial (P9)** | Atacar desde fuera de la app y registrar cada rechazo | Matriz de bypass completa | **14** | 1.4, 1.5, 1.8 |
| 1.11.1 | Scripts de ataque por CLI | 13 ataques: pago detenido, guardián gasta, destino o monto cambiado, `approve` + `transfer_from`, segunda regla, `execute`, `upgrade`, `enforce` directo, `queue` sin firma, contrato intermedio, replay, destino muxed | `scripts/attacks/*` | 6 | 1.5.1, 1.8.2 |
| 1.11.2 | Cierre de cobertura GH | GH-28, 29, 30 y 33; estado de los 33 tests (verde / fallido / no corrido) | Tabla de estado de los tests | 3 | 1.4.6, 1.8.4 |
| 1.11.3 | Dos horas en modo atacante + revisión | Buscar un bypass fuera de la lista y documentar todo lo intentado; confirmar que cada ataque cae en una rama de rechazo de `enforce` | Bitácora del ataque | 3 | 1.11.1 |
| 1.11.4 | Verificación on-chain final | Leído de la cadena: una regla, política GuardianHold, sin `execute` ni `upgrade`, el guardián no es firmante | `evidence/security/bypass-matrix.md` | 2 | 1.11.1, 1.11.3 |
| **1.12** | **Evidencia, demo y entrega (P10)** | Que un tercero pueda verificar todo; entrega enviada | Entrega en Passport | **20** | 1.11 |
| 1.12.1 | Carpeta de evidencia completa | Estructura de la sección 12 del plan; cada ítem con test, esperado, real, hash, hora y etiqueta | `evidence/**` | 3 | 1.11.4 |
| 1.12.2 | README final | Arquitectura, cero backend, cómo correr y redesplegar, pin de OZ, qué se demuestra y qué no, estado de fases | `README.md` final | 2 | 1.12.1 |
| 1.12.3 | Guion y ensayo de la demo | Las 6 escenas en menos de 5 minutos, dos veces seguidas sin fallo; la escena 5 en vivo es obligatoria | Guion + `evidence/demo/scene-1..6.md` | 3 | 1.11.1, 1.7.9, 1.10.3 |
| 1.12.4 | Vídeo de respaldo | Grabado el día anterior, con los hashes visibles | Vídeo + `evidence/demo/video.md` | 2 | 1.12.3 |
| 1.12.5 | Material de pitch | Láminas con la gramática de MOVA; guiones y respuestas a preguntas difíciles, corregidos (sin TrustedPayee, Spending Limit, Channels ni claims de novedad) | Deck + guiones | 3 | 1.12.3, 1.12.7 |
| 1.12.6 | Formulario y envío | "Describe your project" corregido; checklist de entrega; envío antes de la fecha de G1 | `docs/SUBMISSION.md` + envío confirmado | 1,5 | 1.12.8, 1.12.4, 1.1.1 |
| 1.12.7 | Validación con usuarios | 3–5 entrevistas cortas antes del vídeo; prueba de 5 segundos con 3 personas sobre el Detalle en Retenido | Notas de entrevistas + resultado de la prueba | 4 | 1.7.6 |
| 1.12.8 | Revisión final de cumplimiento | Frases prohibidas en README, pitch y UI; secretos en el historial de git; direcciones solo de testnet; 5 afirmaciones al azar con su evidencia | Checklist firmado | 1,5 | 1.12.2, 1.12.5, 1.7.9 |
| **1.13** | **Cierre del proyecto** | Aceptación formal y archivo | Acta de cierre | **2,5** | 1.12 |
| 1.13.1 | Aceptación contra la definición de terminado | Cada fase evaluada (implementación + tests + seguridad + criterios + evidencia); el MVP no se declara completo si falta algo | Acta de aceptación | 1 | 1.12.6 |
| 1.13.2 | Lecciones aprendidas y backlog FUTURE | Lo aprendido y los ítems FUTURE documentados, sin construirlos | Documento de cierre | 1 | 1.13.1 |
| 1.13.3 | Archivo del repositorio | Destino de `spikes/`, repo público, tag de la versión entregada | Tag de entrega | 0,5 | 1.13.1 |

---

## Supuestos

1. **Alcance incluido:** MUST y SHOULD. NICE, FUTURE y DO NOT BUILD quedan fuera, porque la sección 15 del plan no los incluye.
2. **Documentos que mandan:** orden de autoridad de `CLAUDE.md`. Todo lo que solo aparece en la Propuesta para el pitch o en la Validación CR/Chile y fue reemplazado después no está en el WBS: TrustedPayee, Spending Limit, Channels, backend validador, que el guardián apruebe o congele pagos, session keys y `draft_payment_intent`.
3. **G1 se responde "12 de octubre".** Si es el 5, el proyecto se detiene en 1.1.1.
4. **Spike A sale en verde.** Los fallbacks no están estimados porque son contingencias: el 7.A (un solo contrato con `release(id)`) y los escalones de R5 para la firma.
5. **El código de los spikes se descarta, pero sus tests se aprovechan en P2.** Por eso 1.4.6 cuesta menos de lo que costaría empezar de cero.
6. **GH-33 se ejecuta desde Android**, no "desde el navegador" como dice el security spike (resto de la versión web).
7. **GH-11 se conserva** como "la cuenta que paga comisiones no autoriza nada", aunque ya no exista un patrocinador de comisiones.
8. **Dispositivos:** hay un teléfono Android físico para la demo y para Spike B, y un segundo dispositivo o emulador para el guardián.
9. **Recursos de testnet:** Friendbot y el RPC están disponibles. Si el faucet de USDC no responde, se usa un SAC de prueba propio y se dice.
10. **Idiomas:** la UI va en español y "Describe your project" en inglés, como el borrador.
11. **Estimaciones:** son del WBS, no vienen en los documentos de producto. No incluyen espera de descargas (p. ej. 1,1 GB del modelo) ni coordinación entre varias personas.

---

## Preguntas abiertas

1. **Fecha y formulario (G1):** ¿la entrega vale por Passport (12 oct 17:59) o por Luma (5 oct 16:00)? El plazo de Luma ya pasó. ¿El equipo está inscrito en Passport? ¿Qué campos son obligatorios?
2. **Qué hackathon es:** la MVP Proposal y el pitch dicen Find Your Way Costa Rica (TEC Cartago). La Validación sugiere una Ideatón en Chile, con requisito de residencia. Esto decide si el equipo es elegible y si el pitch usa datos de Costa Rica o de Chile.
3. **Equipo:** ¿cuántas personas y con qué perfiles? Con ~205 h de alcance, una sola persona no llega ni al camino crítico.
4. **Mac (G3):** ¿hay un Mac con Xcode? Decide si 1.8.5 y 1.9.4 existen y si el target iOS compila de verdad.
5. **Entrada a la app:** el prompt de P5 dice "crear o recuperar la billetera", pero la recuperación es FUTURE y la cuenta la crea el script de despliegue. La propuesta visual dice "Entrar con passkey", pero la passkey es SHOULD. ¿Qué hace exactamente la pantalla Entrada en el MVP?
6. **Clave del guardián:** ¿cómo llega su clave a su teléfono? El script de P3 genera las claves en un archivo; P6 genera la de la dueña en el Keystore del teléfono.
7. **Cuáles son los "9 estados":** el prompt de P4 incluye "Firmando" y no incluye "Vencido". La propuesta visual dice que "Firmando" no es un estado e incluye "Vencido" como SHOULD. ¿Qué lista vale?
8. **Vídeos:** ¿el vídeo de 3 minutos de la entrega es el mismo que el vídeo de respaldo grabado el día anterior?
9. **Prioridad de los SHOULD:** si el tiempo aprieta, ¿cuáles se intentan primero? Passkey, cancelación por la dueña, `expires_at` o IA en iOS.
10. **Mensajes para Spike B:** ¿hay mensajes de estafa reales para probar la IA, o se usan los patrones documentados marcados SIMULATED? ¿Qué modelo de teléfono se usará en la demo?
11. **Logo:** la propuesta visual dice que no rediseña el logo. ¿Existe un archivo de la marca para el ícono y el wordmark?
12. **Repositorio:** ¿el repo de GitHub ya es público? La sección "Reglas de esta cuenta" y el README enlazan al código y a los tests.
13. **Layouts grandes:** ¿se espera que la app corra en tablet o pantalla grande? El layout de ≥1024 px está en P5; el modo lado a lado es NICE.

---

## Requisitos que no se ubicaron en el WBS

- **NICE (fuera por la sección 15 del plan):** modo lado a lado a ≥1280 px, enlace compartible del Detalle, ventana móvil de 24 h para el tope y chips de montos rápidos para contactos.
- **FUTURE:** cambiar guardián o contactos con espera y veto, varios guardianes, recuperación de cuenta, mainnet con auditoría, autorización pre-firmada, análisis de imágenes y modo oscuro.
- **Revisar las apps bancarias propias** antes del pitch: lo piden el checklist del pitch y la Validación, pero no está en el plan. Queda fuera a la espera de decisión (~1 h).
- **Variante del pitch para Chile:** depende de la pregunta 2.
- **Mostrar rechazos hechos por otros dentro de la app:** la propuesta visual dice que no se construye porque exigiría un indexador. Esos rechazos se ven en el explorador.
- **Comportamiento del carril ámbar** ("sale al vencer" o "modo estricto"): es una decisión abierta de la Validación; el security spike ya la resolvió (sale con una nueva firma de la dueña y no hay modo estricto).
