# GuardPay — WBS para agentes de código

Oct 5, 2026. Documento de ejecución. **No reemplaza el WBS original.**

El WBS original está en [WBS.md](./WBS.md) (IDs 1 … 1.13.3). Es la fuente de verdad de alcance, horas y dependencias. Este archivo lo descompone en tareas atómicas para Cursor / Claude Code. Cada `T-xxx` apunta a un ID de origen. Si hay conflicto, mandan en este orden: (1) [WBS.md](./WBS.md), (2) `docs/GuardPay — Plan de construcción.md`, (3) `docs/GuardPay — Prompts de ejecución.md`, (4) este archivo.

**Reglas para el agente que tome una tarea**

- Ejecuta **una** `T-xxx` por sesión. No adelantes la siguiente.
- Lee `CLAUDE.md` y el "Contexto a leer primero" antes de tocar archivos.
- No abras P2 (T-033 en adelante, contratos de producción) si CP-1 no está GO.
- Si G1 = 5 oct: PARA. No abras P1.
- Testnet only. Nunca `latest`. Nunca inventes hashes. Test no corrido = `no corrido`.
- UI: nunca "seguro" ni "protegido". "Retenido" solo con registro on-chain.
- No implementes nada de `docs/GuardPay — Propuesta para el pitch.md` (TrustedPayee, Spending Limit de OZ, Channels).
- Si falta una decisión humana, no inventes: deja la tarea en bloqueo y señala la `T-00x` de tipo decisión.

Estados del checklist: `pendiente` · `en curso` · `bloqueada` · `hecha` · `omitida`.

---

## A. Checklist maestro

| ID | Título | Tipo | Estado | Depende de |
| --- | --- | --- | --- | --- |
| T-001 | Confirmar G1 (fecha, Passport, campos) | decisión | pendiente | ninguna |
| T-002 | Confirmar G3 (Mac / Xcode) | decisión | pendiente | ninguna |
| T-003 | Decidir alcance de Entrada | decisión | pendiente | ninguna |
| T-004 | Decidir lista de estados de pago | decisión | pendiente | ninguna |
| T-005 | Decidir entrega de la clave del guardián | decisión | pendiente | ninguna |
| T-006 | Priorizar SHOULD recortables | decisión | pendiente | T-001 |
| T-007 | Confirmar evento y elegibilidad | decisión | pendiente | T-001 |
| T-008 | Confirmar si existe logo | decisión | pendiente | ninguna |
| T-009 | Confirmar corpus Spike B y teléfono de demo | decisión | pendiente | ninguna |
| T-010 | Crear `.gitignore` | setup | pendiente | ninguna |
| T-011 | Crear árbol de carpetas | setup | pendiente | T-010 |
| T-012 | Escribir `evidence/README.md` | docs | pendiente | T-011 |
| T-013 | Escribir `docs/ESTADO-FASES.md` | docs | pendiente | T-001, T-002 |
| T-014 | Escribir registro de riesgos | docs | pendiente | T-013 |
| T-015 | Verificar toolchain local | setup | pendiente | T-010 |
| T-016 | Comprobar pin OZ (G2) | setup | pendiente | T-015 |
| T-017 | Escribir `docs/DEPENDENCIES.md` | docs | pendiente | T-015, T-016 |
| T-018 | Escribir README inicial | docs | pendiente | T-001, T-002, T-012, T-013, T-016, T-017 |
| **CP-0** | **Humano: gates + repo** | **checkpoint** | **pendiente** | **T-018** |
| T-019 | Correr test digest OZ (Spike A0) | test | hecha | CP-0, T-016 |
| T-020 | Probar reentrada/recursos R6 (Spike A1) | test | hecha | T-019 |
| T-021 | Escribir `account` mínimo del spike | feature | hecha | T-020 |
| T-022 | Escribir `hold_registry` mínimo del spike | feature | hecha | T-020 |
| T-023 | Escribir `guardian_hold` mínimo del spike | feature | hecha | T-021, T-022 |
| T-024 | Tests blocker GH-01, GH-13, GH-24, GH-25, GH-26 | test | hecha | T-023 |
| T-025 | Tests ciclo GH-04, GH-05, GH-06 | test | hecha | T-023 |
| T-026 | Test GH-09 (guardián no gasta) | test | hecha | T-023 |
| T-027 | Tests vínculo GH-20…GH-23 | test | hecha | T-023 |
| T-028 | Documentar Spike A y congelar firmas | docs | hecha | T-024, T-025, T-026, T-027 |
| **CP-1** | **Humano: Spike A GO / REDESIGN** | **checkpoint** | **GO** | **T-028** |
| T-029 | Crear app bench LiteRT-LM | setup | pendiente | CP-0, T-009 |
| T-030 | Medir Gemma en el teléfono de demo | test | pendiente | T-029 |
| T-031 | Escribir `evidence/ai/benchmark.md` | docs | pendiente | T-030 |
| T-032 | Firmar `transfer` ed25519 desde Kotlin | feature | pendiente | T-023 |
| T-033 | Documentar Spike C | docs | pendiente | T-032 |
| T-034 | Crear workspace Cargo y vendor OZ | setup | hecha | CP-1 |
| T-035 | Implementar `contracts/account` | feature | hecha | T-034 |
| T-036 | Implementar `queue` y clave del registro | feature | hecha | T-034 |
| T-037 | Implementar `cancel`, `mark_executed` y lecturas | feature | hecha | T-036 |
| T-038 | Implementar `guardian_hold.install` | feature | hecha | T-034 |
| T-039 | Implementar `enforce` carril de confianza | feature | hecha | T-035, T-038 |
| T-040 | Implementar `enforce` carril retenido | feature | hecha | T-037, T-038 |
| T-041 | Implementar `enforce` queue/cancel y rechazo | feature | hecha | T-036, T-037, T-038 |
| T-042 | Tests MUST GH-01…03, GH-14, GH-24 | test | hecha | T-039, T-041 |
| T-043 | Tests MUST GH-04…08, GH-07b, GH-31 | test | hecha | T-037, T-040 |
| T-044 | Tests MUST GH-09, GH-13, GH-25…27, GH-32 | test | hecha | T-035, T-038, T-041 |
| T-045 | Tests MUST GH-10…12, GH-15…19, GH-18b | test | hecha | T-039, T-040, T-041 |
| T-046 | Tests MUST GH-20…23, GH-20b | test | hecha | T-040 |
| T-047 | Grep de seguridad, persistencia y overflow | test | hecha | T-042, T-043, T-044, T-045, T-046 |
| T-048 | Escribir revisión manual de lista blanca | docs | hecha | T-041, T-047 |
| T-049 | Implementar cancel de dueña y `expires_at` | feature | pendiente | T-006, T-037, T-040 |
| **CP-2** | **Humano: 29 tests MUST + review** | **checkpoint** | **hecha** | **T-047, T-048** |
| T-050 | Escribir `deploy-testnet.ps1` | setup | hecha | CP-2, T-005 |
| T-051 | Escribir `deploy-testnet.sh` | setup | hecha | T-050 |
| T-052 | Escribir `read-rules` | feature | hecha | T-050 |
| T-053 | Correr tests ★ en testnet | test | hecha | T-050 |
| T-054 | Verificar reglas on-chain | test | hecha | T-052, T-053 |
| **CP-3** | **Humano: hashes REAL de deploy** | **checkpoint** | **pendiente** | **T-054** |
| T-055 | Bootstrap Gradle KMP | setup | pendiente | CP-0, T-002 |
| T-056 | Modelar `PaymentState` | feature | pendiente | T-004, T-055 |
| T-057 | Modelar dominio restante | feature | pendiente | T-056 |
| T-058 | Definir `StellarGateway` | feature | pendiente | T-057 |
| T-059 | Definir `expect Signer` y `GuardPayAI` | feature | pendiente | T-057 |
| T-060 | Implementar `FakeStellarGateway` | test | pendiente | T-058 |
| T-061 | Implementar lecturas KMP de cadena | feature | pendiente | T-033, T-058, CP-3 |
| T-062 | Implementar envíos KMP | feature | pendiente | T-061 |
| T-063 | Calcular carril previsto | feature | pendiente | T-057, T-058 |
| T-064 | Tests de dominio y arquitectura KMP | test | pendiente | T-056, T-059, T-060, T-063 |
| T-065 | Crear shells `app/android` y `app/ios` | setup | pendiente | T-002, T-008, T-055 |
| **CP-4** | **Humano: `allTests` verde** | **checkpoint** | **pendiente** | **T-064** |
| T-066 | Crear tokens de diseño Compose | feature | pendiente | CP-4 |
| T-067 | Crear chips de estado y línea de espera | feature | pendiente | T-066 |
| T-068 | Construir pantalla Entrada | feature | pendiente | T-003, T-067 |
| T-069 | Construir Inicio (saldo, lista, guardián) | feature | pendiente | T-067, T-061 |
| T-070 | Construir "Reglas de esta cuenta" | feature | pendiente | T-069, T-052 |
| T-071 | Construir pantalla Pagar | feature | pendiente | T-067, T-063 |
| T-072 | Construir hoja Revisar y firmar | feature | pendiente | T-071 |
| T-073 | Construir Detalle dueña y Ver en Stellar | feature | pendiente | T-067, T-061 |
| T-074 | Construir lista modo guardián | feature | pendiente | T-067 |
| T-075 | Construir Detalle guardián y confirmación | feature | pendiente | T-073, T-074 |
| T-076 | Ajustar layout 360 px y ≥1024 px | feature | pendiente | T-068, T-070, T-072, T-075 |
| T-077 | Aplicar accesibilidad WCAG 2.2 AA | feature | pendiente | T-076 |
| T-078 | Tests de UI (strings, snapshots, 360 px) | test | pendiente | T-077 |
| **CP-5** | **Humano: 4 pantallas en Android** | **checkpoint** | **pendiente** | **T-078** |
| T-079 | Implementar `Signer` Android Keystore | feature | pendiente | CP-4, T-033, T-005 |
| T-080 | Integrar firma de queue/transfer/cancel | feature | pendiente | T-062, T-079, CP-3 |
| T-081 | Reproducir GH-01 y GH-24 desde la app | test | pendiente | T-080 |
| T-082 | Añadir firmante passkey | feature | pendiente | T-006, T-080 |
| T-083 | Implementar `Signer` iOS | feature | pendiente | T-002, T-006, T-079 |
| **CP-6** | **Humano: ciclo queue→enviar en teléfono** | **checkpoint** | **pendiente** | **T-081** |
| T-084 | Implementar `GuardPayAI` Android | feature | pendiente | T-031, T-059, CP-4 |
| T-085 | Parser JSON estricto y filtros | feature | pendiente | T-084 |
| T-086 | Tests de injection y grafo AI | test | pendiente | T-085, T-071 |
| T-087 | Implementar `GuardPayAI` iOS | feature | pendiente | T-002, T-006, T-085 |
| **CP-7** | **Humano: IA MUST o degradar a NICE** | **checkpoint** | **pendiente** | **T-086** |
| T-088 | Implementar `HoldWatcher` | feature | pendiente | T-061, CP-3 |
| T-089 | Notificación local Android | feature | pendiente | T-088, T-075 |
| T-090 | Cronometrar aviso del guardián | test | pendiente | T-089, T-080 |
| **CP-8** | **Humano: escena 3 punta a punta** | **checkpoint** | **pendiente** | **T-090** |
| T-091 | Scripts: pago detenido y guardián gasta | test | pendiente | CP-3, CP-6 |
| T-092 | Scripts: destino, monto y muxed | test | pendiente | CP-3, CP-6 |
| T-093 | Scripts: segunda regla, execute, upgrade, enforce | test | pendiente | CP-3, CP-6 |
| T-094 | Scripts: approve, nested, replay, queue ajeno | test | pendiente | CP-3, CP-6 |
| T-095 | Cerrar cobertura GH-28, GH-29, GH-30, GH-33 | test | pendiente | T-006, T-047, T-082 |
| T-096 | Sesión atacante de dos horas | test | pendiente | T-091, T-092, T-093, T-094 |
| T-097 | Escribir matriz de bypass | docs | pendiente | T-095, T-096 |
| **CP-9** | **Humano: claims vs evidencia** | **checkpoint** | **pendiente** | **T-097** |
| T-098 | Completar árbol `evidence/` | docs | pendiente | CP-9 |
| T-099 | Reescribir README final | docs | pendiente | T-098 |
| T-100 | Ensayar escenas 1–3 | test | pendiente | CP-5, CP-8, T-098 |
| T-101 | Ensayar escenas 4–6 | test | pendiente | T-091, T-093, T-070, T-100 |
| T-102 | Grabar vídeo de respaldo | docs | pendiente | T-101 |
| T-103 | Hacer 3–5 entrevistas y prueba de 5 s | docs | pendiente | T-073 |
| T-104 | Corregir pitch y Describe your project | docs | pendiente | T-007, T-101, T-103 |
| T-105 | Revisar frases prohibidas y secretos | test | pendiente | T-078, T-099, T-104 |
| T-106 | Enviar entrega Passport | docs | pendiente | T-001, T-102, T-105 |
| T-107 | Redactar acta de aceptación | docs | pendiente | T-106 |
| T-108 | Documentar lecciones y backlog FUTURE | docs | pendiente | T-107 |
| T-109 | Crear tag de entrega | setup | pendiente | T-107 |

---

## B. Orden de ejecución recomendado

```text
FASE 0  Decisiones humanas (T-001…T-009) + fundación (T-010…T-018)
        ──► CP-0  SI G1 ≠ 12 oct: PARA

FASE 1  Camino crítico Spike A: T-019 → T-020 → (T-021 ∥ T-022) → T-023
        → (T-024 ∥ T-025 ∥ T-026 ∥ T-027) → T-028 → CP-1
        Paralelo no bloqueante: T-029…T-031 (Spike B) y, tras T-023, T-032…T-033 (Spike C)
        ──► CP-1  ROJO = REDESIGN 7.A o abandono. No abrir T-034.

FASE 2  Contratos producción: T-034 → (T-035 ∥ T-036 → T-037 ∥ T-038)
        → (T-039 ∥ T-040 ∥ T-041) → (T-042 ∥ T-043 ∥ T-044 ∥ T-045 ∥ T-046)
        → T-047 → T-048 → CP-2
        SHOULD T-049 solo si T-006 lo autorizó.
        Paralelo desde firmas congeladas (T-028): T-055…T-060, T-063, T-064 (núcleo KMP contra fake).

FASE 3  Deploy: T-050 → (T-051 ∥ T-052) → T-053 → T-054 → CP-3
        Tras CP-3: T-061, T-062 (KMP real).

FASE 4  UI: T-066 → T-067 → pantallas (T-068…T-075, varias en paralelo) → T-076 → T-077 → T-078 → CP-5

FASE 5  Auth MUST: T-079 → T-080 → T-081 → CP-6
        IA MUST: T-084 → T-085 → T-086 → CP-7
        Aviso: T-088 → T-089 → T-090 → CP-8
        SHOULD (T-082, T-083, T-087) solo si T-006 y tiempo.

FASE 6  Ataques: (T-091 ∥ T-092 ∥ T-093 ∥ T-094) → T-096 → T-095 → T-097 → CP-9

FASE 7  Entrega: T-098 → T-099 → T-100 → T-101 → T-102 → T-104 → T-105 → T-106 → T-107…T-109
        T-103 puede ir en paralelo desde que existe Detalle (T-073).
```

**Bloqueantes (no saltar):** T-001, CP-0, T-019, T-020, T-024, CP-1, T-047, CP-2, CP-3, T-064, CP-6, CP-9, T-106.

**Paralelo seguro:** T-001∥T-002∥T-003∥T-004∥T-005∥T-008∥T-009; T-021∥T-022; T-024∥T-025∥T-026∥T-027; Spike B y C frente a A (B/C no paran si fallan); T-035∥T-036∥T-038; T-039∥T-040∥T-041; grupos de tests P2; KMP fake vs contratos; pantallas tras T-067; T-091…T-094.

---

## Tareas

### T-001 · Confirmar G1 (fecha, Passport, campos)

- Origen: 1.1.1
- Tipo: decisión
- Tamaño: S
- Depende de: ninguna
- Paralelizable con: T-002, T-003, T-004, T-005, T-008, T-009, T-010
- Objetivo: Un humano confirma si la entrega vale por Passport (12 oct 17:59) o Luma (5 oct 16:00), si el equipo está inscrito y cuáles campos son obligatorios.
- Contexto a leer primero: `docs/GuardPay — Prompts de ejecución.md` (P0 EXIT G1); plan sección 1 y 18.
- Archivos a crear/modificar: `docs/ESTADO-FASES.md` (respuesta G1); más tarde `README.md` en T-018.
- Fuera de alcance: inventar la fecha; WebFetch a Passport (renderiza por JS).
- Criterios de aceptación: G1 escrito como `12 oct` / `5 oct` / `SIN CONFIRMAR — bloquea`. Si es 5 oct, el documento dice PARA y no se abre P1.
- Verificación: humano. El agente no marca esta tarea hecha sin respuesta del usuario.
- Notas técnicas: Hoy es 5 oct por la noche; el plazo de Luma ya pasó. Sin G1 no se escribe código de P1.

### T-002 · Confirmar G3 (Mac / Xcode)

- Origen: 1.1.1
- Tipo: decisión
- Tamaño: S
- Depende de: ninguna
- Paralelizable con: T-001, T-003…T-005, T-008, T-009
- Objetivo: Decidir si el target iOS compila de verdad o solo se declara.
- Contexto a leer primero: plan R10, Spike D; prompt P0 EXIT G3.
- Archivos a crear/modificar: `docs/ESTADO-FASES.md` (respuesta G3).
- Fuera de alcance: afirmar que iOS funciona sin Mac.
- Criterios de aceptación: `Mac disponible` o `iOS solo declarado`. Esa frase llega al README en T-018.
- Verificación: humano.
- Notas técnicas: El entorno de trabajo documentado es Windows 11.

### T-003 · Decidir alcance de Entrada

- Origen: 1.1.1 (duda abierta del WBS)
- Tipo: decisión
- Tamaño: S
- Depende de: ninguna
- Paralelizable con: T-001, T-002, T-004
- Objetivo: Resolver la contradicción: P5 pide "crear o recuperar la billetera"; recuperación es FUTURE; la cuenta la crea el script de P3; la visual dice "Entrar con passkey" y passkey es SHOULD.
- Contexto a leer primero: prompt P5 tarea 3; propuesta visual sección 7.A; plan DO NOT BUILD recuperación.
- Archivos a crear/modificar: `docs/ESTADO-FASES.md` (decisión Entrada).
- Fuera de alcance: implementar login, passkey o recuperación.
- Criterios de aceptación: Una de estas opciones escritas: (a) Entrada solo explica el producto y entra a Inicio/Guardián con la cuenta ya desplegada; (b) Entrada dispara el diálogo ed25519/passkey sobre claves ya registradas por el script. Queda prohibido "crear cuenta" o "recuperar semilla" en el MVP.
- Verificación: humano.
- Notas técnicas: Sin esta decisión, T-068 no arranca.

### T-004 · Decidir lista de estados de pago

- Origen: 1.6.2
- Tipo: decisión
- Tamaño: S
- Depende de: ninguna
- Paralelizable con: T-001…T-003
- Objetivo: Congelar los tipos sellados. P4 incluye "Firmando" y no "Vencido". La visual dice que Firmando no es estado y Vencido es SHOULD.
- Contexto a leer primero: prompt P4 tarea 2; propuesta visual sección 8; security spike sección 5 (3 estados on-chain).
- Archivos a crear/modificar: `docs/ESTADO-FASES.md` (lista congelada).
- Fuera de alcance: implementar la máquina de estados.
- Criterios de aceptación: Lista explícita de estados UI vs estados de contrato. El sello "En Stellar" queda marcado como marca, no como estado. Si Vencido entra, se etiqueta SHOULD y depende de T-049.
- Verificación: humano.
- Notas técnicas: El contrato solo guarda RETENIDO / EJECUTADO / DETENIDO. "Listo para enviar" se deriva del tiempo.

### T-005 · Decidir entrega de la clave del guardián

- Origen: 1.8.2
- Tipo: decisión
- Tamaño: S
- Depende de: ninguna
- Paralelizable con: T-001…T-004
- Objetivo: El script de P3 genera claves en un archivo ignorado; P6 genera la de la dueña en Keystore. Falta cómo llega la clave del guardián a su teléfono.
- Contexto a leer primero: prompt P3 tarea 1.a; prompt P6 tareas 1–3.
- Archivos a crear/modificar: `docs/ESTADO-FASES.md`.
- Fuera de alcance: commitear semillas; diseñar recuperación.
- Criterios de aceptación: Procedimiento escrito para la demo (p. ej. importar la semilla de prueba del archivo ignorado al segundo dispositivo, o usar dos keystores generados y registrar ambos en el deploy). `.gitignore` sigue cubriendo esas claves.
- Verificación: humano.
- Notas técnicas: El guardián **no** se registra como firmante de la cuenta.

### T-006 · Priorizar SHOULD recortables

- Origen: 1.1.2
- Tipo: decisión
- Tamaño: S
- Depende de: T-001
- Paralelizable con: T-002
- Objetivo: Si el tiempo aprieta, el humano elige el orden de T-049, T-082, T-083, T-087 (y GH-28/29/30).
- Contexto a leer primero: plan secciones 5 y 9 (MUST / SHOULD / recorte).
- Archivos a crear/modificar: `docs/ESTADO-FASES.md`.
- Fuera de alcance: implementar SHOULD.
- Criterios de aceptación: Lista ordenada o "ningún SHOULD hasta cerrar camino crítico".
- Verificación: humano.
- Notas técnicas: El camino crítico no incluye passkey, iOS ni `expires_at`.

### T-007 · Confirmar evento y elegibilidad

- Origen: 1.1.1
- Tipo: decisión
- Tamaño: S
- Depende de: T-001
- Paralelizable con: T-002
- Objetivo: Resolver si el evento es Find Your Way Costa Rica (TEC Cartago) o una Ideatón en Chile con residencia.
- Contexto a leer primero: MVP Proposal §3; Validación CR/Chile §21; Propuesta para el pitch (solo el encabezado del evento, no la arquitectura).
- Archivos a crear/modificar: `docs/ESTADO-FASES.md`.
- Fuera de alcance: reescribir el pitch (eso es T-104).
- Criterios de aceptación: Nombre del evento, sede y si el equipo es elegible. Si no es elegible: PARA.
- Verificación: humano.
- Notas técnicas: Cambia datos del pitch (CR vs Chile), no los contratos.

### T-008 · Confirmar si existe logo

- Origen: 1.7.1
- Tipo: decisión
- Tamaño: S
- Depende de: ninguna
- Paralelizable con: T-001…T-005
- Objetivo: La visual no rediseña el logo. Hay que saber si existe un archivo de marca para ícono y wordmark.
- Contexto a leer primero: propuesta visual §4 (Logo y wordmark).
- Archivos a crear/modificar: `docs/ESTADO-FASES.md`.
- Fuera de alcance: rediseñar un logo con escudo o candado.
- Criterios de aceptación: Ruta al asset, o "usar wordmark tipográfico IBM Plex Sans 600 + marca Teal sin pictograma".
- Verificación: humano.
- Notas técnicas: Favicon = marca Teal sobre cuadrado Navy.

### T-009 · Confirmar corpus Spike B y teléfono de demo

- Origen: 1.3.4
- Tipo: decisión
- Tamaño: S
- Depende de: ninguna
- Paralelizable con: T-001…T-008
- Objetivo: 10 mensajes reales en español de Costa Rica, o patrones documentados marcados SIMULATED; modelo del teléfono de la demo.
- Contexto a leer primero: prompt P1 Spike B; Validación CR (modalidades); plan Spike B.
- Archivos a crear/modificar: `docs/ESTADO-FASES.md`; más tarde los 10 textos en `spikes/ai-bench/` (T-029).
- Fuera de alcance: afirmar que Gemma "funciona bien en 8 GB".
- Criterios de aceptación: 10 textos listos + nombre del teléfono, o autorización explícita de usar patrones SIMULATED.
- Verificación: humano.
- Notas técnicas: Sin teléfono real, Spike B no puede ponerse verde.

### T-010 · Crear `.gitignore`

- Origen: 1.2.1
- Tipo: setup
- Tamaño: S
- Depende de: ninguna
- Paralelizable con: T-001…T-009
- Objetivo: Evitar que claves, keystores y artefactos de build entren a git. El repo ya está en GitHub.
- Contexto a leer primero: prompt P0 tarea 1 y SECURITY.
- Archivos a crear/modificar: `.gitignore`
- Fuera de alcance: Gradle, crates, commits de código; reescribir docs de investigación.
- Criterios de aceptación: Cubre `build/`, `.gradle/`, `local.properties`, `*.jks`, `*.keystore`, `target/`, `xcuserdata/`, `.env`, `*.key`, `*.seed`, `secrets/`, `Thumbs.db`, `.DS_Store`.
- Verificación: `git check-ignore -v .env .secret.key demo.jks demo.keystore demo.seed secrets/x local.properties`
- Notas técnicas: El commit de docs ya existe (`435fc45`). No hace falta "primer commit de docs".

### T-011 · Crear árbol de carpetas

- Origen: 1.2.3
- Tipo: setup
- Tamaño: S
- Depende de: T-010
- Paralelizable con: T-015
- Objetivo: Dejar el esqueleto vacío que piden P0 y la sección 12 del plan.
- Contexto a leer primero: prompt P0 tarea 4; plan §12 (árbol `evidence/`).
- Archivos a crear/modificar: `contracts/.gitkeep`, `app/.gitkeep`, `scripts/.gitkeep`, `spikes/.gitkeep`, `evidence/architecture/.gitkeep`, `evidence/guardian/.gitkeep`, `evidence/payments/.gitkeep`, `evidence/smart-account/.gitkeep`, `evidence/policies/.gitkeep`, `evidence/ai/.gitkeep`, `evidence/security/.gitkeep`, `evidence/stellar/.gitkeep`, `evidence/demo/screens/.gitkeep`
- Fuera de alcance: proyectos Gradle o crates; más de `.gitkeep` + README de evidencia (T-012).
- Criterios de aceptación: Las cinco raíces del prompt existen. Las subcarpetas de evidencia de la §12 existen.
- Verificación: `Get-ChildItem contracts,app,scripts,spikes,evidence -Recurse -Force | Select-Object FullName`
- Notas técnicas: P0 dice "solo carpetas". No inicializar Cargo ni Gradle aquí.

### T-012 · Escribir `evidence/README.md`

- Origen: 1.2.3
- Tipo: docs
- Tamaño: S
- Depende de: T-011
- Paralelizable con: T-013, T-015
- Objetivo: Definir cómo se lee la evidencia y las etiquetas obligatorias.
- Contexto a leer primero: plan §12; lista de frases prohibidas; prompt P0 tarea 6.
- Archivos a crear/modificar: `evidence/README.md`
- Fuera de alcance: rellenar hashes; crear `benchmark.md` o `bypass-matrix.md`.
- Criterios de aceptación: Define REAL / SIMULATED / ILLUSTRATIVE. Un test no corrido se registra como `no corrido`. Incluye las frases prohibidas. Describe el árbol de la §12.
- Verificación: El archivo existe y no contiene hashes de ejemplo presentados como REAL.
- Notas técnicas: Valores ilustrativos deben decir ILLUSTRATIVE.

### T-013 · Escribir `docs/ESTADO-FASES.md`

- Origen: 1.1.2, 1.1.4
- Tipo: docs
- Tamaño: S
- Depende de: T-001, T-002
- Paralelizable con: T-010, T-012
- Objetivo: Línea base de fases P0–P10 y sitio donde aterrizan G1–G3 y las decisiones T-003…T-009.
- Contexto a leer primero: plan §5 y §10; prompt P0 tarea 3.d.
- Archivos a crear/modificar: `docs/ESTADO-FASES.md`
- Fuera de alcance: implementar fases; declarar el MVP completo.
- Criterios de aceptación: Tabla P0–P10 con estado `no iniciado` / `INCOMPLETA` / `cerrada`. G1, G2 (puede ser SIN CONFIRMAR hasta T-016), G3. Definition of Done copiada: implementación + unit + integración + seguridad + criterios + evidencia.
- Verificación: El archivo existe. G1 no está inventado.
- Notas técnicas: Actualizar esta tabla al cerrar cada CP.

### T-014 · Escribir registro de riesgos

- Origen: 1.1.3
- Tipo: docs
- Tamaño: S
- Depende de: T-013
- Paralelizable con: T-015
- Objetivo: Dejar R0–R12 y el log GO/REDESIGN/degradación en un solo sitio.
- Contexto a leer primero: plan §6 y §18.
- Archivos a crear/modificar: `docs/RIESGOS.md`
- Fuera de alcance: mitigar riesgos en código.
- Criterios de aceptación: Una fila por R0–R12 con estado abierto. Espacio para el veredicto de cada spike.
- Verificación: R6, R1 y R0 están nombrados.
- Notas técnicas: Spike A rojo → 7.A o abandono, no improvisar.

### T-015 · Verificar toolchain local

- Origen: 1.2.5
- Tipo: setup
- Tamaño: M
- Depende de: T-010
- Paralelizable con: T-011…T-014
- Objetivo: Registrar versiones reales. P0 no instala dependencias de producto; sí exige que las herramientas respondan.
- Contexto a leer primero: prompt P0 tarea 7; `CLAUDE.md` (toolchain planeado).
- Archivos a crear/modificar: salida cruda en `docs/toolchain-versions.txt` (o sección temporal; T-017 la copia a DEPENDENCIES).
- Fuera de alcance: crear Cargo workspace o Gradle; pinnear OZ (T-016).
- Criterios de aceptación: Quedan registradas, con etiqueta `[FACT]` o `NO INSTALADO`: `stellar --version`, `cargo --version`, targets wasm de rustup, `java -version`, Android SDK. Si falta `stellar-cli`, wasm o Android SDK, se nombra el bloqueo en `docs/ESTADO-FASES.md` sin fingir que P0 está cerrado.
- Verificación: ejecutar esos comandos y pegar la salida literal.
- Notas técnicas: `[UNVERIFIED]` si el target correcto es `wasm32-unknown-unknown` o `wasm32v1-none` (Rust reciente). No elegir en silencio: registrar ambos intentos si hace falta. Java 23 está en la máquina; el plan no fija JDK, no cambiar el JDK en esta tarea.

### T-016 · Comprobar pin OZ (G2)

- Origen: 1.2.5
- Tipo: setup
- Tamaño: M
- Depende de: T-015
- Paralelizable con: T-013, T-014
- Objetivo: ¿`OpenZeppelin/stellar-contracts` @ `b40c5ea` compila con soroban-sdk 28?
- Contexto a leer primero: plan R1; prompt P0 EXIT G2.
- Archivos a crear/modificar: directorio gitignorado de prueba (p. ej. `.scratch/oz-g2/`, ya cubierto por ignore o añadido); `docs/ESTADO-FASES.md` (G2).
- Fuera de alcance: workspace de producción `contracts/`; vendorizar en `vendor/` (T-034); reescribir el diseño si falla (eso se reporta).
- Criterios de aceptación: G2 = sí / no + commit alternativo si no. Si no, PARA P2 y reporta; no uses v0.7.2 a menos que un humano lo apruebe.
- Verificación: `cargo build` contra el pin en el scratch. Pegar error o éxito literal.
- Notas técnicas: Estable publicado es v0.7.2 y **no** se usa por defecto. Si el test `do_check_auth_rule_selection_downgrade_fails` no existe en el pin, eso se confirma otra vez en T-019.

### T-017 · Escribir `docs/DEPENDENCIES.md`

- Origen: 1.2.4
- Tipo: docs
- Tamaño: S
- Depende de: T-015, T-016
- Paralelizable con: T-018 (después de G2)
- Objetivo: Pin exacto de cada dependencia crítica, con alternativa.
- Contexto a leer primero: prompt P0 tarea 5; plan §3.
- Archivos a crear/modificar: `docs/DEPENDENCIES.md`
- Fuera de alcance: `libs.versions.toml` o `Cargo.toml` de producción.
- Criterios de aceptación: Filas para Kotlin 2.x, Compose MP ≥ 1.8.0, `com.soneso.stellar:stellar-sdk:1.14.0`, OZ @ `b40c5ea`, soroban-sdk 28.x, LiteRT-LM ≥ 0.12.0, Gemma 4 E2B. Las dos dependencias críticas no oficiales/estables (Soneso y OZ pre-release) van marcadas y justificadas. La palabra `latest` no aparece. Versiones del toolchain con `[FACT]` y salida de comando.
- Verificación: `Select-String -Path docs/DEPENDENCIES.md -Pattern "latest"` → sin matches.
- Notas técnicas: Soneso se acepta porque no hay SDK KMP oficial. OZ pre-release se acepta con spike.

### T-018 · Escribir README inicial

- Origen: 1.2.2
- Tipo: docs
- Tamaño: M
- Depende de: T-001, T-002, T-012, T-013, T-016, T-017
- Paralelizable con: ninguna
- Objetivo: Ordenar documentos contradictorios y dejar los gates por escrito.
- Contexto a leer primero: prompt P0 tarea 3; `CLAUDE.md` (orden de autoridad, tesis).
- Archivos a crear/modificar: `README.md`
- Fuera de alcance: reescribir los docs de `docs/`; código.
- Criterios de aceptación: Tres frases sin "seguro"; frase guía; orden de autoridad; estado de fases; G1 G2 G3 (o SIN CONFIRMAR — bloquea); limitaciones (testnet, sin auditar, guardián no cambiable).
- Verificación: `Select-String -Path README.md -Pattern "seguro|protegido"` → vacío en prosa de producto. G1–G3 visibles.
- Notas técnicas: El pitch está obsoleto en lo técnico: TrustedPayee, Spending Limit, Channels eliminados.

### CP-0 · Humano revisa gates y repo

- Origen: 1.2 (exit P0)
- Tipo: decisión
- Tamaño: S
- Depende de: T-018
- Paralelizable con: ninguna
- Objetivo: Autorizar P1 o detener el plan.
- Contexto a leer primero: `README.md`, `docs/ESTADO-FASES.md`, `docs/DEPENDENCIES.md`.
- Archivos a crear/modificar: `docs/ESTADO-FASES.md` (P0 cerrada o INCOMPLETA).
- Fuera de alcance: empezar spikes sin este visto bueno si G1 no es 12 oct.
- Criterios de aceptación: Humano marca GO a P1, o PARA. G1=5 oct → no se asignan T-019+.
- Verificación: humano.
- Notas técnicas: G2 en rojo no impide documentar, pero bloquea T-019 si no hay pin usable.

---

### T-019 · Correr test digest OZ (Spike A0)

- Origen: 1.3.1
- Tipo: test
- Tamaño: M
- Depende de: CP-0, T-016
- Paralelizable con: T-029 (Spike B)
- Objetivo: Confirmar que el pin liga `context_rule_ids` al digest firmado.
- Contexto a leer primero: plan Spike A paso 1; security spike §3; código OZ `smart_account` tests en el pin.
- Archivos a crear/modificar: `spikes/guardian-hold/` (solo lo mínimo para correr el test de OZ); nota en `docs/RIESGOS.md`.
- Fuera de alcance: lógica de GuardPay; `contracts/` de producción.
- Criterios de aceptación: `do_check_auth_rule_selection_downgrade_fails` existe y pasa, o se reporta PARA. Sin eufemismos.
- Verificación: el `cargo test` del test de OZ, con salida literal.
- Notas técnicas: Si el test no existe o falla, toda la arquitectura se apoya en un comportamiento ausente. STOP.

### T-020 · Probar reentrada/recursos R6 (Spike A1)

- Origen: 1.3.2
- Tipo: test
- Tamaño: M
- Depende de: T-019
- Paralelizable con: T-029
- Objetivo: Un `enforce` mínimo que lea y escriba en un registro mínimo **dentro** de `__check_auth`. Primera hora del Spike A.
- Contexto a leer primero: plan R6 y Spike A paso 2; fallback 7.A; prompt P1 A1.
- Archivos a crear/modificar: `spikes/guardian-hold/` (contratos mínimos de prueba, ≤5 archivos).
- Fuera de alcance: lista blanca, tope, UI, producción.
- Criterios de aceptación: Medición de CPU e I/O. Veredicto: pasa / rechazado por reentrada / excede recursos. Si falla: PARA y propone 7.A; no lo implementa.
- Verificación: test de Soroban que invoque el `enforce` mínimo. Salida literal.
- Notas técnicas: Soroban prohíbe reentrar el mismo contrato; por eso hay dos. Esta prueba decide si el diseño se sostiene.

### T-021 · Escribir `account` mínimo del spike

- Origen: 1.3.3
- Tipo: feature
- Tamaño: M
- Depende de: T-020
- Paralelizable con: T-022
- Objetivo: Cuenta de una sola regla Default (firmante = dueña, política = GH). Sin execute, upgrade ni admin.
- Contexto a leer primero: prompt P1 A2; security spike §4. **No copiar** `multisig-smart-account`.
- Archivos a crear/modificar: `spikes/guardian-hold/account/` (lib + Cargo del spike).
- Fuera de alcance: `contracts/account` de producción; `execute` "solo para tests".
- Criterios de aceptación: Una regla. `rg "execute|upgrade" spikes/guardian-hold/account` vacío (salvo comentarios que digan que no existen).
- Verificación: `cargo test` del crate del spike (puede estar rojo hasta T-023). Grep anterior en verde.
- Notas técnicas: Piezas de OZ @ `b40c5ea`. El ejemplo oficial es el bypass.

### T-022 · Escribir `hold_registry` mínimo del spike

- Origen: 1.3.3
- Tipo: feature
- Tamaño: M
- Depende de: T-020
- Paralelizable con: T-021
- Objetivo: `queue`, `cancel`, `mark_executed` (solo GH). `ready_at` calculado.
- Contexto a leer primero: prompt P1 A2; security spike §5 (campos del intent).
- Archivos a crear/modificar: `spikes/guardian-hold/hold_registry/`
- Fuera de alcance: invocar tokens; `ready_at` como parámetro; producción.
- Criterios de aceptación: Ninguna función pública recibe `ready_at`. Ninguna llama a un token.
- Verificación: `rg "ready_at" spikes/guardian-hold/hold_registry` — no aparece en firmas públicas. `cargo test` del crate.
- Notas técnicas: Almacenamiento persistente desde el mínimo, o el spike miente sobre R8.

### T-023 · Escribir `guardian_hold` mínimo del spike

- Origen: 1.3.3
- Tipo: feature
- Tamaño: L
- Depende de: T-021, T-022
- Paralelizable con: ninguna
- Objetivo: `install` una vez + `enforce` con `account.require_auth()` y dueña en `authenticated_signers`, más la lógica mínima para los 13 tests del spike.
- Contexto a leer primero: prompt P1 A2–A3; plan §4 lista blanca (versión mínima).
- Archivos a crear/modificar: `spikes/guardian-hold/guardian_hold/`
- Fuera de alcance: producción; tool calling; UI.
- Criterios de aceptación: `enforce` llama `require_auth` **antes** de otra lógica. Segundo `install` se puede dejar para P2 si no cabe, pero GH-32 no es de este spike.
- Verificación: `rg "require_auth" spikes/guardian-hold/guardian_hold`. Compila con los otros dos crates.
- Notas técnicas: Escenario base: tope 50, hold 120 s, expiry 600 s, 500 USDC, contacto T, atacante X.

### T-024 · Tests blocker GH-01, GH-13, GH-24, GH-25, GH-26

- Origen: 1.3.3
- Tipo: test
- Tamaño: M
- Depende de: T-023
- Paralelizable con: T-025, T-026, T-027
- Objetivo: Los cinco blockers del security spike. Si uno falla, PARA.
- Contexto a leer primero: security spike §7 filas GH-01, 13, 24, 25, 26; prompt P1 STOP.
- Archivos a crear/modificar: `spikes/guardian-hold/**/src/test.rs` (o el archivo de test del spike, máximo 5).
- Fuera de alcance: GH-02, UI, testnet.
- Criterios de aceptación: Los cinco en verde o STOP con el ID en rojo. Registro literal.
- Verificación: `cargo test` filtrado a esos IDs.
- Notas técnicas: Rojo aquí = MAJOR REDESIGN, no "ajustar la UI".

### T-025 · Tests ciclo GH-04, GH-05, GH-06

- Origen: 1.3.3
- Tipo: test
- Tamaño: M
- Depende de: T-023
- Paralelizable con: T-024, T-026, T-027
- Objetivo: queue → no enviar antes → enviar después con nueva firma.
- Contexto a leer primero: security spike §7 GH-04…06.
- Archivos a crear/modificar: tests del spike (añadir casos, no reescribir contratos salvo bug del mínimo).
- Fuera de alcance: GH-07; vencimiento GH-30.
- Criterios de aceptación: Los tres verdes. `ready_at` no sale del cliente.
- Verificación: `cargo test` de esos IDs.
- Notas técnicas: Avanzar ledgers/tiempo en el entorno de tests de Soroban.

### T-026 · Test GH-09 (guardián no gasta)

- Origen: 1.3.3
- Tipo: test
- Tamaño: M
- Depende de: T-023
- Paralelizable con: T-024, T-025, T-027
- Objetivo: (a)(b)(c) de GH-09: transfer firmado por G, G como firmante extra, G llama funciones públicas de R y GH.
- Contexto a leer primero: security spike GH-09.
- Archivos a crear/modificar: tests del spike.
- Fuera de alcance: UI del guardián.
- Criterios de aceptación: (a) y (b) rechazo. (c) ninguna función mueve fondos ni cambia config.
- Verificación: `cargo test` GH-09.
- Notas técnicas: Es la tesis. Si esto no pasa, GuardPay es un multisig.

### T-027 · Tests vínculo GH-20…GH-23

- Origen: 1.3.3
- Tipo: test
- Tamaño: M
- Depende de: T-023
- Paralelizable con: T-024, T-025, T-026
- Objetivo: Otro destino, monto, token u origen no ejecutan el hold.
- Contexto a leer primero: security spike tabla C y GH-20…23.
- Archivos a crear/modificar: tests del spike.
- Fuera de alcance: GH-20b (muxed) es P2; no abrirlo aquí salvo que quepa en los mismos ≤5 archivos.
- Criterios de aceptación: Los cuatro rechazan.
- Verificación: `cargo test` GH-20 GH-21 GH-22 GH-23.
- Notas técnicas: Comparación campo a campo tipada, no hash de `Vec<Val>` crudo.

### T-028 · Documentar Spike A y congelar firmas

- Origen: 1.3.3
- Tipo: docs
- Tamaño: S
- Depende de: T-024, T-025, T-026, T-027
- Paralelizable con: T-031, T-033
- Objetivo: Evidencia literal + firmas públicas congeladas para P2 y P4.
- Contexto a leer primero: prompt P1 EVIDENCE y EXPECTED OUTPUT.
- Archivos a crear/modificar: `evidence/security/spike-a.md`; `docs/CONTRACT-SIGNATURES.md` (firmas `install`, `enforce`, `queue`, `cancel`, `mark_executed`, lecturas).
- Fuera de alcance: copiar el spike a `contracts/`.
- Criterios de aceptación: 13 filas con resultado real. Veredicto GO o ROJO. Firmas listadas. `rg execute|upgrade` del spike vacío.
- Verificación: 13 IDs listados. Ninguna fila dice "debería pasar".
- Notas técnicas: 13 tests, no 11 (el plan se corrige: ver WBS original supuestos).

### CP-1 · Humano: Spike A GO / REDESIGN

- Origen: 1.3 (exit)
- Tipo: decisión
- Tamaño: S
- Depende de: T-028
- Paralelizable con: T-031, T-033
- Objetivo: Autorizar contratos de producción.
- Contexto a leer primero: `evidence/security/spike-a.md`; plan 7.A.
- Archivos a crear/modificar: `docs/ESTADO-FASES.md`, `docs/RIESGOS.md`.
- Fuera de alcance: implementar 7.A sin aprobación.
- Criterios de aceptación: GO → se asigna T-034. ROJO → 7.A o abandono, escrito.
- Verificación: humano.
- Notas técnicas: B y C no bloquean este CP.

### T-029 · Crear app bench LiteRT-LM

- Origen: 1.3.4
- Tipo: setup
- Tamaño: M
- Depende de: CP-0, T-009
- Paralelizable con: T-019…T-028
- Objetivo: App Android desechable, no el MVP, API Kotlin oficial LiteRT-LM ≥ 0.12.0, Gemma 4 E2B.
- Contexto a leer primero: prompt P1 B1–B3; plan Spike B.
- Archivos a crear/modificar: `spikes/ai-bench/` (módulo mínimo).
- Fuera de alcance: `app/` de producción; wrappers de comunidad; segundo modelo; tool calling.
- Criterios de aceptación: Carga el modelo. Prompt exige JSON `{ whatItAsks, signals, suggestion }` con `SUGGEST_HOLD` | `NO_CLEAR_SIGNALS`.
- Verificación: la app bench arranca y carga (o se registra el fallo con números).
- Notas técnicas: Fallback de Gemma = quitar la función, no otro modelo.

### T-030 · Medir Gemma en el teléfono de demo

- Origen: 1.3.4
- Tipo: test
- Tamaño: L
- Depende de: T-029
- Paralelizable con: Spike A/C
- Objetivo: 10 mensajes en el teléfono **real**. Medir, no afirmar.
- Contexto a leer primero: prompt P1 B4–B5.
- Archivos a crear/modificar: logs brutos en `spikes/ai-bench/` (gitignorados si pesan).
- Fuera de alcance: integrar en P7; inventar RAM/latencia.
- Criterios de aceptación: Las 10 métricas del prompt medidas. Teléfono nombrado. JSON parseable contado.
- Verificación: Profiler + 10 corridas. Sin OOM o se registra OOM.
- Notas técnicas: Verde ≥8/10 parseable y primer token <3 s. Rojo → IA a NICE.

### T-031 · Escribir `evidence/ai/benchmark.md`

- Origen: 1.3.4
- Tipo: docs
- Tamaño: S
- Depende de: T-030
- Paralelizable con: T-028
- Objetivo: Tabla de números, no adjetivos.
- Contexto a leer primero: plan Spike B evidence.
- Archivos a crear/modificar: `evidence/ai/benchmark.md`
- Fuera de alcance: "funciona bien en 8 GB".
- Criterios de aceptación: Todas las columnas del prompt. Etiqueta REAL o SIMULATED en los mensajes. Veredicto verde/amarillo/rojo.
- Verificación: hay números (MB, ms, tok/s, %).
- Notas técnicas: P7 no arranca en MUST si esto está rojo.

### T-032 · Firmar `transfer` ed25519 desde Kotlin

- Origen: 1.3.5
- Tipo: feature
- Tamaño: L
- Depende de: T-023
- Paralelizable con: T-024…T-027, T-029
- Objetivo: Una firma Kotlin aceptada por `__check_auth` de la cuenta del spike. Digest = `sha256(signature_payload ‖ xdr(context_rule_ids))`.
- Contexto a leer primero: prompt P1 C1–C2; demo de smart accounts de Soneso 1.14.0 **antes de escribir**.
- Archivos a crear/modificar: `spikes/kmp-signing/`
- Fuera de alcance: passkey (GH-33, T-082); `app/shared` de producción.
- Criterios de aceptación: Paso ed25519 verde o fallback R5 documentado (XDR a mano → ed25519 crudo → CLI).
- Verificación: hash de tx o log del entorno de tests. Si no hay testnet aún, el spike contra la cuenta de A vale.
- Notas técnicas: Passkey es SHOULD y no bloquea.

### T-033 · Documentar Spike C

- Origen: 1.3.5
- Tipo: docs
- Tamaño: S
- Depende de: T-032
- Paralelizable con: T-028, T-031
- Objetivo: Dejar el hash y el veredicto de firma.
- Contexto a leer primero: prompt P1 evidence Spike C.
- Archivos a crear/modificar: `evidence/stellar/spike-c.md`
- Fuera de alcance: inventar hash.
- Criterios de aceptación: Hash REAL o `no corrido` / fallo literal. Veredicto para P6.
- Verificación: el hash es de la salida, no de un ejemplo.
- Notas técnicas: Si el último escalón R5 aplica, la app solo lee y se firma por CLI; hay que decirlo.

---

### T-034 · Crear workspace Cargo y vendor OZ

- Origen: 1.4.1
- Tipo: setup
- Tamaño: M
- Depende de: CP-1
- Paralelizable con: T-055 (KMP)
- Objetivo: Workspace de producción y OZ @ `b40c5ea` vendorizado y commiteado.
- Contexto a leer primero: prompt P2 tarea 1; `docs/CONTRACT-SIGNATURES.md`; `docs/DEPENDENCIES.md`.
- Archivos a crear/modificar: `Cargo.toml`, `contracts/account/Cargo.toml`, `contracts/guardian_hold/Cargo.toml`, `contracts/hold_registry/Cargo.toml`, `vendor/stellar-contracts/` (árbol del pin).
- Fuera de alcance: lógica de `enforce`; copiar el ejemplo multisig.
- Criterios de aceptación: `cargo build --target <target-wasm-registrado-en-G2>` de los tres crates (pueden ser stubs). Pin por git rev, no por rango.
- Verificación: `cargo build` del workspace. `Select-String -Path Cargo.toml,contracts/**/Cargo.toml -Pattern "latest"` vacío.
- Notas técnicas: Target wasm = el que T-015/T-016 dejaron escrito.

### T-035 · Implementar `contracts/account`

- Origen: 1.4.2
- Tipo: feature
- Tamaño: M
- Depende de: T-034
- Paralelizable con: T-036, T-038
- Objetivo: `__check_auth` y nada de administración. Una regla Default.
- Contexto a leer primero: prompt P2 tarea 2; security spike §4.
- Archivos a crear/modificar: `contracts/account/src/lib.rs` y como máximo el `Cargo.toml` del crate si hace falta.
- Fuera de alcance: `execute`, `upgrade`, `add_context_rule`, `add_signer`, `add_policy`, `remove_*`.
- Criterios de aceptación: `rg "execute|upgrade" contracts/account` → vacío.
- Verificación: ese `rg`; `cargo test -p account` si ya hay smoke.
- Notas técnicas: El constructor no crea una Default extra sin política.

### T-036 · Implementar `queue` y clave del registro

- Origen: 1.4.4
- Tipo: feature
- Tamaño: M
- Depende de: T-034
- Paralelizable con: T-035, T-038
- Objetivo: `queue(account, to, amount) -> id`. `ready_at = created_at + duración`. Un activo por clave `(account, token, destination, amount)`. Persistente. Evento.
- Contexto a leer primero: prompt P2 tarea 4; security spike §5 campos.
- Archivos a crear/modificar: `contracts/hold_registry/src/lib.rs` (y storage si se separa, ≤2 archivos).
- Fuera de alcance: `cancel` (T-037); mover tokens; `ready_at` de entrada.
- Criterios de aceptación: Segundo `queue` igual se rechaza (base de GH-31). `ready_at` no es parámetro.
- Verificación: unit del crate para `queue`. `rg "fn queue" -A 5 contracts/hold_registry`
- Notas técnicas: Token en la clave aunque el MVP sea solo USDC.

### T-037 · Implementar `cancel`, `mark_executed` y lecturas

- Origen: 1.4.4
- Tipo: feature
- Tamaño: M
- Depende de: T-036
- Paralelizable con: T-035, T-038
- Objetivo: `cancel` solo guardián (dueña = SHOULD en T-049). `mark_executed` solo GuardianHold. Lecturas para el cliente. Eventos. DETENIDO y EJECUTADO terminales.
- Contexto a leer primero: prompt P2 tarea 4–5.
- Archivos a crear/modificar: `contracts/hold_registry/src/lib.rs` (y test mínimo si cabe).
- Fuera de alcance: funciones que toquen el SAC.
- Criterios de aceptación: Extraño no puede `cancel` (GH-07b). GH no-invoker no puede `mark_executed`.
- Verificación: `cargo test -p hold_registry`
- Notas técnicas: Guardián se guarda en config de cuenta/registro al instalar, no en el intent.

### T-038 · Implementar `guardian_hold.install`

- Origen: 1.4.3
- Tipo: feature
- Tamaño: M
- Depende de: T-034
- Paralelizable con: T-035, T-036
- Objetivo: `install(account, config)` una vez. config = dueña, guardián, ≤3 contactos, tope, duración. Segundo install rechaza.
- Contexto a leer primero: prompt P2 tarea 3 (install).
- Archivos a crear/modificar: `contracts/guardian_hold/src/lib.rs`
- Fuera de alcance: cuerpo completo de `enforce` (T-039…T-041).
- Criterios de aceptación: GH-32 queda listo para el test. Persistente.
- Verificación: `cargo test -p guardian_hold install`
- Notas técnicas: Config la fija el script del equipo, no una pantalla.

### T-039 · Implementar `enforce` carril de confianza

- Origen: 1.4.3
- Tipo: feature
- Tamaño: M
- Depende de: T-035, T-038
- Paralelizable con: T-040, T-041
- Objetivo: Tras `require_auth` + dueña en signers: `USDC.transfer` a contacto, `to` Address no muxed, amount > 0, tope con overflow check, actualiza gastado (día UTC ledger).
- Contexto a leer primero: prompt P2 lista blanca primer bullet; `spending_limit.rs` como patrón de lectura, no como política.
- Archivos a crear/modificar: `contracts/guardian_hold/src/lib.rs` (y un módulo de args si se parte, ≤2).
- Fuera de alcance: carril retenido; `approve`.
- Criterios de aceptación: Contacto dentro de tope pasa. Sobre tope rechaza (no se convierte solo en hold). Muxed rechaza.
- Verificación: tests locales del carril; luego T-042.
- Notas técnicas: `to` que no convierte a `Address` = rechazo (R7).

### T-040 · Implementar `enforce` carril retenido

- Origen: 1.4.3
- Tipo: feature
- Tamaño: M
- Depende de: T-037, T-038
- Paralelizable con: T-039, T-041
- Objetivo: `transfer` a no-contacto solo si hay registro activo exacto RETENIDO, `now >= ready_at`, marca EJECUTADO. Comparación tipada campo a campo.
- Contexto a leer primero: prompt P2 segundo bullet; opción 2 del spike.
- Archivos a crear/modificar: `contracts/guardian_hold/src/lib.rs`
- Fuera de alcance: que el registro llame al token.
- Criterios de aceptación: Recalcula la clave desde args convertidos. Un solo uso.
- Verificación: tests del crate; T-043 y T-046.
- Notas técnicas: Si `expires_at` no está (T-049 omitida), no lo exijas.

### T-041 · Implementar `enforce` queue/cancel y rechazo

- Origen: 1.4.3
- Tipo: feature
- Tamaño: M
- Depende de: T-036, T-037, T-038
- Paralelizable con: T-039, T-040
- Objetivo: Permitir `HoldRegistry.queue` y `cancel` de la dueña (SHOULD) con primer arg = esta cuenta. Rechazar todo lo demás, incluidas llamadas a la cuenta.
- Contexto a leer primero: prompt P2 resto de la lista blanca.
- Archivos a crear/modificar: `contracts/guardian_hold/src/lib.rs`
- Fuera de alcance: leer el registro dentro de `queue` (reentrada).
- Criterios de aceptación: approve, burn, transfer_from, otros tokens, otros contratos, create, nested = rechazo.
- Verificación: `cargo test -p guardian_hold`
- Notas técnicas: Panic en `enforce` revierte todo; no "registrar y rechazar" en la misma tx.

### T-042 · Tests MUST GH-01…03, GH-14, GH-24

- Origen: 1.4.6
- Tipo: test
- Tamaño: M
- Depende de: T-039, T-041
- Paralelizable con: T-043…T-046
- Objetivo: Firma obligatoria, carril inmediato, desconocido sin hold, matriz de carriles, hold sin firma.
- Contexto a leer primero: security spike §7 esas filas.
- Archivos a crear/modificar: `contracts/*/src/test.rs` (solo los casos de este grupo).
- Fuera de alcance: testnet ★ (T-053).
- Criterios de aceptación: Esos IDs verdes o `no corrido` explícito.
- Verificación: `cargo test` filtrado.
- Notas técnicas: Cada rama de `enforce` necesita un accept y un reject a lo largo de T-042…T-046.

### T-043 · Tests MUST GH-04…08, GH-07b, GH-31

- Origen: 1.4.6
- Tipo: test
- Tamaño: M
- Depende de: T-037, T-040
- Paralelizable con: T-042, T-044…T-046
- Objetivo: Ciclo de hold, veto, tercero no cancela, detenido no sale, un registro activo.
- Contexto a leer primero: security spike §7.
- Archivos a crear/modificar: `contracts/*/src/test.rs`
- Fuera de alcance: GH-30 salvo T-049.
- Criterios de aceptación: IDs verdes.
- Verificación: `cargo test` filtrado.
- Notas técnicas: GH-07b es caso MUST aunque el plan a veces lo omita del conteo "29".

### T-044 · Tests MUST GH-09, GH-13, GH-25…27, GH-32

- Origen: 1.4.6
- Tipo: test
- Tamaño: M
- Depende de: T-035, T-038, T-041
- Paralelizable con: T-042, T-043, T-045, T-046
- Objetivo: Guardián, regla única, sin admin, enforce externo, queue ajeno, no reinstall.
- Contexto a leer primero: security spike §7.
- Archivos a crear/modificar: `contracts/*/src/test.rs`
- Fuera de alcance: añadir admin para hacer pasar un test (STOP del prompt P2).
- Criterios de aceptación: IDs verdes. Si un MUST exige execute/segunda regla, PARA y reporta.
- Verificación: `cargo test` filtrado.
- Notas técnicas: STOP condition P2 aplica aquí.

### T-045 · Tests MUST GH-10…12, GH-15…19, GH-18b

- Origen: 1.4.6
- Tipo: test
- Tamaño: M
- Depende de: T-039, T-040, T-041
- Paralelizable con: T-042…T-044, T-046
- Objetivo: App/backend/IA sin autoridad; transfer_from, approve, burn, nested, replay.
- Contexto a leer primero: security spike §7; GH-11 se interpreta como "cuenta que paga fees no autoriza" (no hay patrocinador).
- Archivos a crear/modificar: `contracts/*/src/test.rs` + contrato N mínimo para GH-18 si hace falta (un archivo extra en tests).
- Fuera de alcance: backend real.
- Criterios de aceptación: IDs verdes. GH-12 = revisión + GH-03 con análisis "sin señales" no cambia el rechazo.
- Verificación: `cargo test` filtrado.
- Notas técnicas: GH-18b = nested con registro maduro, igual rechazo.

### T-046 · Tests MUST GH-20…23, GH-20b

- Origen: 1.4.6
- Tipo: test
- Tamaño: M
- Depende de: T-040
- Paralelizable con: T-042…T-045
- Objetivo: Vínculo exacto + muxed.
- Contexto a leer primero: security spike tabla C; R7.
- Archivos a crear/modificar: `contracts/*/src/test.rs`
- Fuera de alcance: aceptar muxed "equivalente".
- Criterios de aceptación: Cinco rechazos (21 prueba 99 y 101).
- Verificación: `cargo test` filtrado.
- Notas técnicas: El WBS original exige no perder GH-20b.

### T-047 · Grep de seguridad, persistencia y overflow

- Origen: 1.4.6
- Tipo: test
- Tamaño: M
- Depende de: T-042, T-043, T-044, T-045, T-046
- Paralelizable con: ninguna
- Objetivo: Cerrar checks de P2 que no son un GH de flujo.
- Contexto a leer primero: prompt P2 SECURITY; plan R8.
- Archivos a crear/modificar: `contracts/*/src/test.rs` (overflow + avance de ledgers); `evidence/policies/tests.md`
- Fuera de alcance: scripts de testnet.
- Criterios de aceptación: `rg execute|upgrade contracts/account` vacío. `ready_at` no es parámetro público. Suma i128 cerca del máximo. Registro y tope viven tras avanzar ledgers. `evidence/policies/tests.md` tiene la salida literal de los 29 MUST (GH-01…27 + 31 + 32, incluyendo variantes 07b/18b/20b como filas).
- Verificación: `cargo test --workspace` y los `rg` anteriores.
- Notas técnicas: Registrar no corridos como `no corrido`.

### T-048 · Escribir revisión manual de lista blanca

- Origen: 1.4.7
- Tipo: docs
- Tamaño: M
- Depende de: T-041, T-047
- Paralelizable con: T-049
- Objetivo: Recorrer `enforce` rama por rama contra el plan §4.
- Contexto a leer primero: plan §4 tabla enforce; `contracts/guardian_hold/src/lib.rs`.
- Archivos a crear/modificar: `evidence/policies/whitelist-review.md`
- Fuera de alcance: cambiar el contrato sin reabrir tests.
- Criterios de aceptación: Cada rama accept/reject nombrada. Incluye la de rechazo catch-all.
- Verificación: humano en CP-2.
- Notas técnicas: GH nunca invoca un token.

### T-049 · Implementar cancel de dueña y `expires_at`

- Origen: 1.4.5
- Tipo: feature
- Tamaño: M
- Depende de: T-006, T-037, T-040
- Paralelizable con: T-048
- Objetivo: SHOULD: dueña cancela; `enforce` rechaza si `now > expires_at` (GH-30).
- Contexto a leer primero: security spike GH-30; plan SHOULD.
- Archivos a crear/modificar: `contracts/guardian_hold/src/lib.rs`, `contracts/hold_registry/src/lib.rs`, tests GH-30.
- Fuera de alcance: hacerlo si T-006 lo dejó fuera (entonces `omitida`).
- Criterios de aceptación: GH-30 verde o tarea omitida con nota.
- Verificación: `cargo test` GH-30.
- Notas técnicas: `expires_at` derivado, no parámetro de `queue`.

### CP-2 · Humano: 29 tests MUST + review

- Origen: 1.4 (exit)
- Tipo: decisión
- Tamaño: S
- Depende de: T-047, T-048
- Paralelizable con: T-049
- Objetivo: Cerrar P2 antes de testnet.
- Contexto a leer primero: `evidence/policies/tests.md`, `whitelist-review.md`.
- Archivos a crear/modificar: `docs/ESTADO-FASES.md`
- Fuera de alcance: deploy si un MUST está rojo.
- Criterios de aceptación: Humano firma la review. P2 cerrada o INCOMPLETA.
- Verificación: humano.
- Notas técnicas: "Compila" no es done.

---

### T-050 · Escribir `deploy-testnet.ps1`

- Origen: 1.5.1
- Tipo: setup
- Tamaño: L
- Depende de: CP-2, T-005
- Paralelizable con: ninguna
- Objetivo: Script idempotente de la demo en Windows.
- Contexto a leer primero: prompt P3 tarea 1.
- Archivos a crear/modificar: `scripts/deploy-testnet.ps1`, `evidence/stellar/deployment.md` (se llena al correr).
- Fuera de alcance: mainnet; commitear claves; fingir USDC oficial si se usó SAC propio.
- Criterios de aceptación: Friendbot, resolve/deploy SAC, tres contratos, firmante dueña, `install` (3 contactos, tope 50, 120 s, 600 s), 500 USDC, imprime direcciones. Segunda corrida no rompe. Claves en archivo gitignorado.
- Verificación: `git check-ignore` del archivo de claves. Correr el script dos veces.
- Notas técnicas: Testnet. Si faucet USDC falla, SAC de prueba y se dice.

### T-051 · Escribir `deploy-testnet.sh`

- Origen: 1.5.1
- Tipo: setup
- Tamaño: M
- Depende de: T-050
- Paralelizable con: T-052
- Objetivo: El prompt pide PowerShell y/o bash. Equivalente al `.ps1`.
- Contexto a leer primero: prompt P3 FILES.
- Archivos a crear/modificar: `scripts/deploy-testnet.sh`
- Fuera de alcance: divergir la config de demo.
- Criterios de aceptación: Misma secuencia que el `.ps1`. Si no hay entorno Unix, documentar `no corrido` y no afirmar que corre.
- Verificación: `bash scripts/deploy-testnet.sh` donde exista bash, o nota `no corrido`.
- Notas técnicas: Entorno primario = Windows.

### T-052 · Escribir `read-rules`

- Origen: 1.5.2
- Tipo: feature
- Tamaño: S
- Depende de: T-050
- Paralelizable con: T-051
- Objetivo: Lectura on-chain de cuántas rules, política y firmante. Fuente de la UI y escena 6.
- Contexto a leer primero: prompt P3 tarea 2.
- Archivos a crear/modificar: `scripts/read-rules.ps1` (y `.sh` solo si cabe en el tope de archivos).
- Fuera de alcance: hardcodear "1 regla" en la app.
- Criterios de aceptación: Imprime datos leídos de la cuenta desplegada.
- Verificación: salida = una rule y política GuardianHold, o STOP.
- Notas técnicas: Si hay dos rules, PARA (bypass).

### T-053 · Correr tests ★ en testnet

- Origen: 1.5.3
- Tipo: test
- Tamaño: L
- Depende de: T-050
- Paralelizable con: T-052
- Objetivo: GH-02, 03, 04, 05, 06, 07, 08, 09, 10, 16 en testnet real. Un hash cada uno.
- Contexto a leer primero: prompt P3 tarea 3; security spike ★.
- Archivos a crear/modificar: `evidence/stellar/deployment.md` o `evidence/policies/tests.md` (filas ★); logs locales no commiteados si estorban.
- Fuera de alcance: inventar hashes; marcar entorno de tests como testnet.
- Criterios de aceptación: 10 hashes REAL. Transfer a T pasa; a desconocido rechaza.
- Verificación: hashes en explorador de testnet.
- Notas técnicas: El plan a veces dice 11 ★; la tabla del spike marca 10. Usar estos 10.

### T-054 · Verificar reglas on-chain

- Origen: 1.5.4
- Tipo: test
- Tamaño: S
- Depende de: T-052, T-053
- Paralelizable con: ninguna
- Objetivo: Una rule, política GH, dueña única firmante, execute/upgrade error, segundo install rechaza.
- Contexto a leer primero: prompt P3 SECURITY.
- Archivos a crear/modificar: `evidence/smart-account/rules-onchain.md`
- Fuera de alcance: cambiar la cuenta para "arreglar" una segunda rule.
- Criterios de aceptación: Lectura REAL. Si hay dos rules: PARA.
- Verificación: output de `read-rules` + invocaciones execute/upgrade/install.
- Notas técnicas: Escenas 1 y 2 deben poder hacerse desde CLI al cerrar esto.

### CP-3 · Humano: hashes REAL de deploy

- Origen: 1.5 (exit)
- Tipo: decisión
- Tamaño: S
- Depende de: T-054
- Paralelizable con: T-055 si el KMP aún usa fake
- Objetivo: Autorizar integración real del cliente.
- Contexto a leer primero: `evidence/stellar/deployment.md`, `rules-onchain.md`.
- Archivos a crear/modificar: `docs/ESTADO-FASES.md`
- Fuera de alcance: mainnet.
- Criterios de aceptación: Direcciones y 10 hashes revisados por un humano.
- Verificación: humano.
- Notas técnicas: Testnet caída → no inventar; reintentar.

---

### T-055 · Bootstrap Gradle KMP

- Origen: 1.6.1
- Tipo: setup
- Tamaño: M
- Depende de: CP-0, T-002
- Paralelizable con: T-019…T-034 (tras CP-0; iOS se declara aunque G3 sea no)
- Objetivo: Proyecto Gradle con pines exactos. Targets android + iosArm64 + iosSimulatorArm64.
- Contexto a leer primero: prompt P4 tarea 1; `docs/DEPENDENCIES.md`.
- Archivos a crear/modificar: `settings.gradle.kts`, `gradle/libs.versions.toml`, `build.gradle.kts` raíz, `app/shared/build.gradle.kts` (ajustar si el tope de 5 obliga a dejar wrappers para otra sesión: wrapper cuenta).
- Fuera de alcance: UI; LiteRT en commonMain; `latest`.
- Criterios de aceptación: Módulo `shared` existe. Versiones exactas. iOS declarado; si G3=no, README ya dice que no se compiló (T-018 o nota aquí).
- Verificación: `./gradlew :shared:compileCommonMainKotlinMetadata` (o el task equivalente que genere el bootstrap). En Windows: `.\gradlew.bat`.
- Notas técnicas: Kotlin 2.x, CMP ≥ 1.8.0, stellar-sdk 1.14.0.

### T-056 · Modelar `PaymentState`

- Origen: 1.6.2
- Tipo: feature
- Tamaño: S
- Depende de: T-004, T-055
- Paralelizable con: T-059
- Objetivo: Tipos sellados según la lista congelada en T-004. Transiciones que el contrato no permite se rechazan (Detenido y Enviado finales).
- Contexto a leer primero: propuesta visual §8; `docs/ESTADO-FASES.md` (T-004).
- Archivos a crear/modificar: `app/shared/src/commonMain/kotlin/.../domain/PaymentState.kt` (+ test en T-064 si no cabe).
- Fuera de alcance: Compose; Stellar SDK.
- Criterios de aceptación: El sello En Stellar no es un case del sealed state.
- Verificación: el archivo compila en commonMain.
- Notas técnicas: commonMain sin `android.*`.

### T-057 · Modelar dominio restante

- Origen: 1.6.2
- Tipo: feature
- Tamaño: S
- Depende de: T-056
- Paralelizable con: T-059
- Objetivo: `HeldPayment`, `TrustedContact`, `Lane` (Immediate | MustHold), `RiskSignal`, funciones puras.
- Contexto a leer primero: prompt P4 tarea 2 domain.
- Archivos a crear/modificar: `app/shared/src/commonMain/kotlin/.../domain/*.kt` (agrupar en ≤4 archivos).
- Fuera de alcance: red; IA.
- Criterios de aceptación: Cero dependencias de plataforma.
- Verificación: compile commonMain.
- Notas técnicas: `Analysis` / `Suggestion` viven junto a AI expect (T-059), no aquí, para no mezclar.

### T-058 · Definir `StellarGateway`

- Origen: 1.6.3
- Tipo: feature
- Tamaño: S
- Depende de: T-057
- Paralelizable con: T-059
- Objetivo: Interfaz única al SDK.
- Contexto a leer primero: prompt P4 tarea 2 stellar; `docs/CONTRACT-SIGNATURES.md`.
- Archivos a crear/modificar: `app/shared/src/commonMain/kotlin/.../stellar/StellarGateway.kt`
- Fuera de alcance: implementación Soneso (T-061).
- Criterios de aceptación: Métodos: `readAccountRules`, `readBalance`, `readTrustedContacts`, `readDailySpent`, `readHolds`, `submitQueue`, `submitCancel`, `submitTransfer`.
- Verificación: compile commonMain.
- Notas técnicas: Sustituir el SDK no debe tocar domain ni UI.

### T-059 · Definir `expect Signer` y `GuardPayAI`

- Origen: 1.6.4
- Tipo: feature
- Tamaño: S
- Depende de: T-057
- Paralelizable con: T-058
- Objetivo: Contratos expect. IA = una función. Suggestion = dos valores.
- Contexto a leer primero: prompt P4 tarea 2 signing/ai; plan §4 AI.
- Archivos a crear/modificar: `.../signing/Signer.kt`, `.../ai/GuardPayAI.kt`
- Fuera de alcance: actuals; tool calling; `draft_payment_intent`.
- Criterios de aceptación: `analyzeMessage(text: String): Analysis` únicamente. `SuggestHold | NoClearSignals`.
- Verificación: compile. `rg "draft_payment|tool" app/shared/src/commonMain` vacío.
- Notas técnicas: IA no recibe saldo, contactos, política ni claves.

### T-060 · Implementar `FakeStellarGateway`

- Origen: 1.6.3
- Tipo: test
- Tamaño: M
- Depende de: T-058
- Paralelizable con: T-063
- Objetivo: Mismo modelo de estados en memoria. Solo `commonTest`.
- Contexto a leer primero: prompt P4 tarea 4.
- Archivos a crear/modificar: `app/shared/src/commonTest/kotlin/.../FakeStellarGateway.kt`
- Fuera de alcance: meter el fake en sourceSets de producción.
- Criterios de aceptación: No está en `commonMain`. Permite P5 contra fake si CP-3 no llegó.
- Verificación: `rg FakeStellarGateway app/shared/src/commonMain` vacío.
- Notas técnicas: P4 puede avanzar en paralelo a P2 con este fake.

### T-061 · Implementar lecturas KMP de cadena

- Origen: 1.6.3
- Tipo: feature
- Tamaño: M
- Depende de: T-033, T-058, CP-3
- Paralelizable con: T-066 si se usa fake en UI
- Objetivo: `KmpStellarGateway` lee rules, balance, contactos, spent, holds vía SDK 1.14.0.
- Contexto a leer primero: demo Soneso; `evidence/stellar/deployment.md`.
- Archivos a crear/modificar: `app/shared/src/commonMain/kotlin/.../stellar/KmpStellarGateway.kt` (+ mapper si se parte, ≤2).
- Fuera de alcance: submits (T-062); android imports.
- Criterios de aceptación: Parseo de lecturas cubierto en tests (T-064 o locales).
- Verificación: test de parseo en `commonTest`.
- Notas técnicas: Si CP-3 no está, esta tarea espera. La UI puede seguir con fake.

### T-062 · Implementar envíos KMP

- Origen: 1.6.3
- Tipo: feature
- Tamaño: M
- Depende de: T-061
- Paralelizable con: T-063
- Objetivo: `submitQueue`, `submitCancel`, `submitTransfer` detrás de la interfaz. Auth entry según Spike C.
- Contexto a leer primero: `evidence/stellar/spike-c.md`; fallback R5.
- Archivos a crear/modificar: `KmpStellarGateway.kt` (+ helper XDR si hace falta, ≤2).
- Fuera de alcance: Keystore (T-079); interpolar salida de IA.
- Criterios de aceptación: Si Spike C fue R5-último escalón, las submits no firman y el README lo dice; si no, construyen el auth entry que OZ acepta.
- Verificación: test contra Fake o, si CP-3 está, smoke de submit (puede fallar sin Signer; no fingir éxito).
- Notas técnicas: La salida de `GuardPayAI` nunca se interpola aquí.

### T-063 · Calcular carril previsto

- Origen: 1.6.5
- Tipo: feature
- Tamaño: S
- Depende de: T-057, T-058
- Paralelizable con: T-060, T-062
- Objetivo: Immediate vs MustHold **antes** de firmar, leyendo config. Informativo; la autoridad es el contrato.
- Contexto a leer primero: prompt P4 tarea 5; visual §7.C tarjeta de carril.
- Archivos a crear/modificar: `app/shared/src/commonMain/kotlin/.../domain/Lane.kt` (o `LaneCalculator.kt`)
- Fuera de alcance: que la IA elija el carril; acortar espera.
- Criterios de aceptación: Contacto bajo tope → Immediate. Resto → MustHold. Sobre tope de contacto → MustHold.
- Verificación: tests de borde en T-064.
- Notas técnicas: "Retener este pago" sobre un contacto también fuerza MustHold.

### T-064 · Tests de dominio y arquitectura KMP

- Origen: 1.6.6
- Tipo: test
- Tamaño: M
- Depende de: T-056, T-059, T-060, T-063
- Paralelizable con: T-065
- Objetivo: Transiciones, carril, parseo de cadena y JSON de IA (malformado/truncado/texto extra). Grep de arquitectura.
- Contexto a leer primero: prompt P4 TESTS y SECURITY.
- Archivos a crear/modificar: `app/shared/src/commonTest/kotlin/**` (≤5 archivos), `evidence/architecture/kmp-layers.md`
- Fuera de alcance: instrumentados Android; UI snapshots.
- Criterios de aceptación: `./gradlew :shared:allTests` (Windows: `.\gradlew.bat :shared:allTests`) verde en targets compilables. Falla si commonMain importa `android.*`, LiteRT-LM o WebAuthn. Falla si una clase tiene a la vez `GuardPayAI` y `Signer` o `StellarGateway`. Fake no está en producción.
- Verificación: el `gradlew` anterior + `rg "android\\.|LiteRt|WebAuthn" app/shared/src/commonMain`
- Notas técnicas: iOS `allTests` solo si G3 = Mac.

### T-065 · Crear shells `app/android` y `app/ios`

- Origen: 1.6.7
- Tipo: setup
- Tamaño: M
- Depende de: T-002, T-008, T-055
- Paralelizable con: T-056…T-064
- Objetivo: Arranque, permisos, hueco para modelo y notificaciones. Sin lógica de negocio.
- Contexto a leer primero: plan §4 mobile (app-android / app-ios).
- Archivos a crear/modificar: `app/android/**` (manifest + Activity), `app/ios/**` (si G3; si no, stub declarado).
- Fuera de alcance: pantallas Compose (T-066+); LiteRT; Keystore.
- Criterios de aceptación: Android arranca un placeholder. iOS: compila o está declarado no compilado.
- Verificación: `.\gradlew.bat :app-android:assembleDebug` (ajustar nombre real del módulo). iOS: `no corrido` si no hay Mac.
- Notas técnicas: El modelo Gemma no va en el APK.

### CP-4 · Humano: `allTests` verde

- Origen: 1.6 (exit)
- Tipo: decisión
- Tamaño: S
- Depende de: T-064
- Paralelizable con: T-065
- Objetivo: Autorizar UI sobre el núcleo.
- Contexto a leer primero: `evidence/architecture/kmp-layers.md`
- Archivos a crear/modificar: `docs/ESTADO-FASES.md`
- Fuera de alcance: rediseñar domain.
- Criterios de aceptación: Humano ve `allTests` verde y el grep de capas.
- Verificación: humano.
- Notas técnicas: UI no contiene lógica de seguridad.

---

### T-066 · Crear tokens de diseño Compose

- Origen: 1.7.1
- Tipo: feature
- Tamaño: S
- Depende de: CP-4
- Paralelizable con: ninguna
- Objetivo: Paleta y tipografía exactas. Acción = Navy. Teal = evidencia. Nunca blanco sobre Teal.
- Contexto a leer primero: propuesta visual §5 y §4 tipografía.
- Archivos a crear/modificar: `app/shared/src/commonMain/kotlin/ui/theme/Color.kt`, `Type.kt`, `Theme.kt` (rutas equivalentes, ≤3).
- Fuera de alcance: pantallas; modo oscuro; rediseñar paleta.
- Criterios de aceptación: Hex: Navy `#0B1220`, Teal `#00A99D`, Mint `#5EEAD4`, Ice `#F5F9FA`, Success `#16A34A`, Warning `#D97706`, Danger `#DC2626`, derivados Teal `#007E75`, Warning `#A25904`, Success `#107A37`. IBM Plex Sans; Mono solo hashes/direcciones.
- Verificación: los hex aparecen literales. `rg "Color\\(0xFF00A99D\\)"` (o equivalente) en botones de acción → no debe usarse como fondo de botón con texto blanco.
- Notas técnicas: Lucide, trazo 1.75, grid 24. Sin escudos ni candados.

### T-067 · Crear chips de estado y línea de espera

- Origen: 1.7.2
- Tipo: feature
- Tamaño: M
- Depende de: T-066
- Paralelizable con: ninguna
- Objetivo: Cada estado: color + ícono + forma + frase. Línea de espera con guardián en el tramo ámbar. Sello "En Stellar".
- Contexto a leer primero: propuesta visual §8 y §7.D (línea de espera).
- Archivos a crear/modificar: `ui/components/StateChip.kt`, `WaitLine.kt`, `StellarSeal.kt` (≤5).
- Fuera de alcance: pantallas completas; mostrar Retenido sin registro.
- Criterios de aceptación: Retenido ≠ error (ámbar que avanza). Detenido = chip relleno + persona. Rechazado = contorno. Sello solo si el dato es de cadena.
- Verificación: preview/composable compile. No hay estado solo por color.
- Notas técnicas: Set de 10 íconos Lucide del doc visual. Prohibidos: escudo, candado, robot.

### T-068 · Construir pantalla Entrada

- Origen: 1.7.3
- Tipo: feature
- Tamaño: S
- Depende de: T-003, T-067
- Paralelizable con: T-069, T-071
- Objetivo: Mensaje en 3 líneas + acción de entrar según T-003.
- Contexto a leer primero: propuesta visual §7.A; `docs/ESTADO-FASES.md` decisión Entrada.
- Archivos a crear/modificar: `ui/screens/EntryScreen.kt` (+ navegación mínima si aún no existe, ≤2).
- Fuera de alcance: onboarding largo; crear/recuperar cuenta; ajustes.
- Criterios de aceptación: Título y tres líneas del doc visual. Un botón. Sin "seguro".
- Verificación: la pantalla renderiza. `rg "seguro|protegido" app/shared/src/commonMain/kotlin/ui`
- Notas técnicas: Única pantalla dueña con bloque Navy grande.

### T-069 · Construir Inicio (saldo, lista, guardián)

- Origen: 1.7.4
- Tipo: feature
- Tamaño: M
- Depende de: T-067, T-061
- Paralelizable con: T-068, T-071
- Objetivo: Saldo, tarjeta del guardián, espera, recientes, contactos con tope. Datos de cadena (o fake etiquetado).
- Contexto a leer primero: visual §7.B.
- Archivos a crear/modificar: `ui/screens/HomeScreen.kt` (+ viewmodel si se parte, ≤3).
- Fuera de alcance: sección Reglas (T-070); gráficos de saldo.
- Criterios de aceptación: "En espera" solo si hay holds. "leído a las HH:MM". Botón Pagar.
- Verificación: navega desde Entrada. Sin scroll horizontal a 360 (se cierra en T-076).
- Notas técnicas: Refresh cada 15 s si hay holds.

### T-070 · Construir "Reglas de esta cuenta"

- Origen: 1.7.4
- Tipo: feature
- Tamaño: S
- Depende de: T-069, T-052
- Paralelizable con: T-072
- Objetivo: Sección plegable MUST leída de la cadena: 1 rule, firmante, política, enlaces a código y tests.
- Contexto a leer primero: visual §11.2; prompt P5 tarea 4.
- Archivos a crear/modificar: `ui/components/AccountRulesSection.kt`
- Fuera de alcance: constantes fingidas como on-chain; la palabra "Verificado".
- Criterios de aceptación: Valores vienen de `readAccountRules()`. Sello En Stellar solo si la lectura ok.
- Verificación: con fake, 1 rule; con testnet, coincide con `read-rules`.
- Notas técnicas: Es la prueba de propiedad para el jurado técnico.

### T-071 · Construir pantalla Pagar

- Origen: 1.7.5
- Tipo: feature
- Tamaño: M
- Depende de: T-067, T-063
- Paralelizable con: T-068, T-069
- Objetivo: Destino, monto, tarjeta de carril, campo opcional de mensaje, enlace "Retener este pago".
- Contexto a leer primero: visual §7.C y §10.
- Archivos a crear/modificar: `ui/screens/PayScreen.kt` (+ AI card placeholder, ≤3).
- Fuera de alcance: hoja firmar (T-072); que la IA rellene destino/monto.
- Criterios de aceptación: Carril se marca solo. Frases: "Sale en segundos" / "Se retendrá hasta las HH:MM; Diego puede detenerlo". Sin puntaje de riesgo.
- Verificación: compose compile. IA no escribe los campos.
- Notas técnicas: Tarjeta IA Navy, sin color semántico, plegada.

### T-072 · Construir hoja Revisar y firmar

- Origen: 1.7.5
- Tipo: feature
- Tamaño: S
- Depende de: T-071
- Paralelizable con: T-073
- Objetivo: Destino, monto, USDC, carril, verbo exacto. Firmando es momento del botón, no estado.
- Contexto a leer primero: visual §7.C2.
- Archivos a crear/modificar: `ui/screens/ReviewSheet.kt`
- Fuera de alcance: passkey real si P6 no está; no decir "seguro".
- Criterios de aceptación: "Firmar y enviar" o "Firmar y retener". Cancelar el diálogo no cambia cadena.
- Verificación: la hoja muestra destino completo disponible ("ver completa").
- Notas técnicas: Única superficie con sombra del flujo dueña (junto a confirmación Detener).

### T-073 · Construir Detalle dueña y Ver en Stellar

- Origen: 1.7.6
- Tipo: feature
- Tamaño: M
- Depende de: T-067, T-061
- Paralelizable con: T-072, T-074
- Objetivo: Estado, espera, acción por estado, filas de prueba con "qué prueba".
- Contexto a leer primero: visual §7.D y §11.1.
- Archivos a crear/modificar: `ui/screens/PaymentDetailScreen.kt`, `ui/components/OnChainProof.kt`
- Fuera de alcance: listar rechazos ajenos (no hay indexador; se dice).
- Criterios de aceptación: "Retenido" solo con registro. Listo = "Necesita tu firma para salir". Lectura fallida = último valor + "sin actualizar", sin sello.
- Verificación: los 9 chips según T-004 aparecen. Captura se hace en T-078.
- Notas técnicas: SHOULD "Detener mi pago" solo si T-049/T-006.

### T-074 · Construir lista modo guardián

- Origen: 1.7.7
- Tipo: feature
- Tamaño: S
- Depende de: T-067
- Paralelizable con: T-073
- Objetivo: Banda Navy, un verbo (abrir), sin Pagar/Aprobar/monto.
- Contexto a leer primero: visual §9.E.
- Archivos a crear/modificar: `ui/screens/GuardianHomeScreen.kt`
- Fuera de alcance: `submitTransfer` en este modo (verificar en T-078).
- Criterios de aceptación: Texto permanente: puede detener, no mover. Vacío = una frase, sin ilustración.
- Verificación: `rg "Pagar|Aprobar|submitTransfer" .../GuardianHomeScreen.kt` vacío.
- Notas técnicas: Lee RPC, no caché de la dueña.

### T-075 · Construir Detalle guardián y confirmación

- Origen: 1.7.7
- Tipo: feature
- Tamaño: M
- Depende de: T-073, T-074
- Paralelizable con: ninguna
- Objetivo: Orden fijo de datos + única confirmación de la app.
- Contexto a leer primero: visual §9.E2 y E3.
- Archivos a crear/modificar: variante en `PaymentDetailScreen.kt` o `GuardianDetailScreen.kt`, `StopConfirmSheet.kt`
- Fuera de alcance: botones Aprobar/Liberar/Adelantar, incluso desactivados.
- Criterios de aceptación: Destino completo en Mono. IA debajo y rotulada "puede equivocarse". Confirmación con verbo "Detener el pago".
- Verificación: no existen strings Aprobar/Liberar en modo guardián.
- Notas técnicas: El aviso push es T-089; esta pantalla no depende de él.

### T-076 · Ajustar layout 360 px y ≥1024 px

- Origen: 1.7.8
- Tipo: feature
- Tamaño: M
- Depende de: T-068, T-070, T-072, T-075
- Paralelizable con: ninguna
- Objetivo: Responsive del doc visual. Sin contenido nuevo en desktop.
- Contexto a leer primero: visual §13.
- Archivos a crear/modificar: layout/scaffold compartido (≤3 archivos de las pantallas ya creadas).
- Fuera de alcance: modo lado a lado NICE (≥1280).
- Criterios de aceptación: 360 px sin scroll horizontal. ≥1024: columna 480 + panel 360 con Ver en Stellar.
- Verificación: se formaliza en T-078 (test 360).
- Notas técnicas: La app no se vuelve dashboard.

### T-077 · Aplicar accesibilidad WCAG 2.2 AA

- Origen: 1.7.8
- Tipo: feature
- Tamaño: M
- Depende de: T-076
- Paralelizable con: ninguna
- Objetivo: Contraste, estado no solo por color, touch 44, foco, readers, reducir movimiento.
- Contexto a leer primero: visual §14 y tabla de contraste §5.
- Archivos a crear/modificar: componentes de T-066…T-075 (tocar los que fallen, ≤5 por sesión; si hay más, partir y dejar nota en ESTADO-FASES).
- Fuera de alcance: modo oscuro; i18n extra.
- Criterios de aceptación: Navy para texto; derivados para teal/ámbar/verde. Countdown: visual relativo + reader con hora fija, sin live cada segundo. `prefers-reduced-motion` sin transiciones de la línea.
- Verificación: checklist en `evidence/demo/screens/a11y.md` (ILLUSTRATIVE si no se midió con herramienta).
- Notas técnicas: Base 16 px; zoom 200 % una columna.

### T-078 · Tests de UI (strings, snapshots, 360 px)

- Origen: 1.7.9
- Tipo: test
- Tamaño: M
- Depende de: T-077
- Paralelizable con: ninguna
- Objetivo: Snapshots/instrumentados de estados; fail si "seguro"/"protegido"; fail scroll horizontal 360; modo guardián sin camino a `submitTransfer`.
- Contexto a leer primero: prompt P5 TESTS y SECURITY.
- Archivos a crear/modificar: tests UI en `app/shared` o `app/android`; `evidence/demo/screens/` capturas nombradas por estado.
- Fuera de alcance: inventar capturas de testnet si se usó fake (etiquetar SIMULATED).
- Criterios de aceptación: El test de literales existe y falla si alguien añade "seguro". Capturas de los estados de T-004.
- Verificación: task de test UI que deje el bootstrap (p. ej. `.\gradlew.bat :shared:allTests` + instrumentados si ya existen). `rg -i "seguro|protegido" app/shared/src/commonMain`
- Notas técnicas: Contrastar contra la tabla visual.

### CP-5 · Humano: 4 pantallas en Android

- Origen: 1.7 (exit)
- Tipo: decisión
- Tamaño: S
- Depende de: T-078
- Paralelizable con: T-079, T-084
- Objetivo: Navegación real en Android. Reglas leídas de cadena si CP-3 está.
- Contexto a leer primero: `evidence/demo/screens/`
- Archivos a crear/modificar: `docs/ESTADO-FASES.md`
- Fuera de alcance: añadir pantallas.
- Criterios de aceptación: Entrada, Inicio, Pagar, Detalle, modo Guardián visibles.
- Verificación: humano en dispositivo/emulador.
- Notas técnicas: Si una pantalla pide una función que el contrato no tiene, se cambia la pantalla.

---

### T-079 · Implementar `Signer` Android Keystore

- Origen: 1.8.1
- Tipo: feature
- Tamaño: M
- Depende de: CP-4, T-033, T-005
- Paralelizable con: T-084
- Objetivo: Par ed25519 en Android Keystore. Privada no sale.
- Contexto a leer primero: prompt P6 tarea 1; Spike C.
- Archivos a crear/modificar: `app/shared/src/androidMain/kotlin/signing/**` (≤3)
- Fuera de alcance: passkey (T-082); iOS (T-083); añadir guardián como signer.
- Criterios de aceptación: No logs de clave. Firma pide biometría o lock screen. Digest documentado.
- Verificación: test instrumentado o revisión: `rg "privateKey|seed|SecretKey" app/shared/src/androidMain` sin fugas.
- Notas técnicas: MUST. Misma propiedad on-chain que passkey.

### T-080 · Integrar firma de queue/transfer/cancel

- Origen: 1.8.2
- Tipo: feature
- Tamaño: M
- Depende de: T-062, T-079, CP-3
- Paralelizable con: ninguna
- Objetivo: Dueña firma queue y transfer; guardián firma cancel. Registrar firmante en deploy si aún no.
- Contexto a leer primero: prompt P6 tareas 2–4; T-005.
- Archivos a crear/modificar: glue androidMain + ajuste menor a `KmpStellarGateway` y script de deploy si el firmante cambia (≤5).
- Fuera de alcance: recovery; exportar clave.
- Criterios de aceptación: Ciclo queue → esperar → enviar posible en testnet. Digest = `sha256(payload ‖ ids)` o fallback R5 explícito.
- Verificación: hashes en T-081.
- Notas técnicas: Passkey firma a ciegas; no venderla como defensa del adversario A.

### T-081 · Reproducir GH-01 y GH-24 desde la app

- Origen: 1.8.3
- Tipo: test
- Tamaño: M
- Depende de: T-080
- Paralelizable con: ninguna
- Objetivo: Transfer sin firma de dueña se rechaza desde el cliente. Test que falle si el guardián está en firmantes.
- Contexto a leer primero: prompt P6 TESTS.
- Archivos a crear/modificar: `evidence/smart-account/signing.md`; test de firmantes.
- Fuera de alcance: GH-33.
- Criterios de aceptación: Dos hashes REAL (éxito con firma, rechazo sin). Grep/test de firmante guardián.
- Verificación: explorador testnet + `rg guardian` en paths de add_signer vacío.
- Notas técnicas: Evidencia REAL o `no corrido`.

### T-082 · Añadir firmante passkey

- Origen: 1.8.4
- Tipo: feature
- Tamaño: L
- Depende de: T-006, T-080
- Paralelizable con: T-083
- Objetivo: SHOULD GH-33. WebAuthn vía SDK 1.14.0 + verificador OZ.
- Contexto a leer primero: prompt P6 tarea 5; Spike C paso 3. Ejecutar desde Android, no "desde el navegador".
- Archivos a crear/modificar: androidMain signing passkey (≤5).
- Fuera de alcance: si T-006 la omite. No presentar passkey como si mostrara destino.
- Criterios de aceptación: GH-33 verde o tarea `omitida`.
- Verificación: test GH-33 / tx REAL.
- Notas técnicas: README debe decir que la passkey firma un hash a ciegas.

### T-083 · Implementar `Signer` iOS

- Origen: 1.8.5
- Tipo: feature
- Tamaño: M
- Depende de: T-002, T-006, T-079
- Paralelizable con: T-082
- Objetivo: SHOULD Keychain, solo si G3 y T-006.
- Contexto a leer primero: prompt P6 tarea 6.
- Archivos a crear/modificar: `app/shared/src/iosMain/kotlin/signing/**`
- Fuera de alcance: afirmar iOS sin Mac.
- Criterios de aceptación: actual existe o `omitida` con nota en README.
- Verificación: compile iOS o `no corrido`.
- Notas técnicas: Misma interfaz `Signer`.

### CP-6 · Humano: ciclo queue→enviar en teléfono

- Origen: 1.8 (exit)
- Tipo: decisión
- Tamaño: S
- Depende de: T-081
- Paralelizable con: CP-7
- Objetivo: Cerrar auth MUST.
- Contexto a leer primero: `evidence/smart-account/signing.md`
- Archivos a crear/modificar: `docs/ESTADO-FASES.md`
- Fuera de alcance: passkey obligatoria.
- Criterios de aceptación: Humano vio un transfer firmado en testnet y un rechazo sin firma.
- Verificación: humano.
- Notas técnicas: Si R5-último, la demo firma por CLI y se dice.

---

### T-084 · Implementar `GuardPayAI` Android

- Origen: 1.9.1
- Tipo: feature
- Tamaño: L
- Depende de: T-031, T-059, CP-4
- Paralelizable con: T-079
- Objetivo: actual LiteRT-LM oficial ≥ 0.12.0 + Gemma 4 E2B. Descarga en app con progreso. Liberar al salir.
- Contexto a leer primero: prompt P7; `evidence/ai/benchmark.md`. Si Spike B rojo: no hacer; degradar.
- Archivos a crear/modificar: `app/shared/src/androidMain/kotlin/ai/**`, glue de descarga en `app/android` (≤5).
- Fuera de alcance: empaquetar 1.1 GB en el APK; tool calling; visión; segundo modelo.
- Criterios de aceptación: Solo recibe el texto. Streaming "analizando…".
- Verificación: carga en el teléfono de demo o se marca NICE.
- Notas técnicas: API oficial, no wrappers comunitarios.

### T-085 · Parser JSON estricto y filtros

- Origen: 1.9.2
- Tipo: feature
- Tamaño: S
- Depende de: T-084
- Paralelizable con: ninguna
- Objetivo: JSON estricto. Fallo → `NoClearSignals`. Nunca texto crudo. Filtrar "seguro".
- Contexto a leer primero: prompt P7 tareas 3–4.
- Archivos a crear/modificar: parser en `commonMain` o `androidMain/ai` (1–2 archivos) + tests de parseo si no cubrió T-064.
- Fuera de alcance: mostrar output crudo "por debug" en UI.
- Criterios de aceptación: Malformado/truncado/extra → NoClearSignals. "seguro" no llega a UI.
- Verificación: tests unitarios del parser.
- Notas técnicas: El modelo no escribe la UI.

### T-086 · Tests de injection y grafo AI

- Origen: 1.9.3
- Tipo: test
- Tamaño: M
- Depende de: T-085, T-071
- Paralelizable con: ninguna
- Objetivo: 10 mensajes tasa parseo; 5 injection (GH-12); arquitectura: AI no comparte grafo con Signer/StellarGateway. Carril no cambia.
- Contexto a leer primero: prompt P7 TESTS.
- Archivos a crear/modificar: `evidence/ai/prompt-injection.md`; test de arquitectura.
- Fuera de alcance: afirmar detección de estafa.
- Criterios de aceptación: 5 casos con entrada/salida literales. Ninguno produce "seguro" ni cambia el carril. Señales < 10 s en el teléfono de demo o se degrada.
- Verificación: tests + evidencia. `rg GuardPayAI` en signing/stellar vacío.
- Notas técnicas: Si inestable → quitar campo de mensaje, escena 2a fuera.

### T-087 · Implementar `GuardPayAI` iOS

- Origen: 1.9.4
- Tipo: feature
- Tamaño: L
- Depende de: T-002, T-006, T-085
- Paralelizable con: T-083
- Objetivo: SHOULD SPM + puente Swift. Si no, `NoClearSignals` + copy "análisis no disponible en este dispositivo".
- Contexto a leer primero: prompt P7 tarea 7.
- Archivos a crear/modificar: `app/shared/src/iosMain/kotlin/ai/**`
- Fuera de alcance: forzar LiteRT en iOS sin Mac.
- Criterios de aceptación: actual real o fallback honesto.
- Verificación: compile o UI del fallback.
- Notas técnicas: No duplicar un modelo más pequeño.

### CP-7 · Humano: IA MUST o degradar a NICE

- Origen: 1.9 (exit)
- Tipo: decisión
- Tamaño: S
- Depende de: T-086
- Paralelizable con: CP-6
- Objetivo: Si rojo, quitar campo y escena 2a. Tesis intacta.
- Contexto a leer primero: benchmark + prompt-injection.
- Archivos a crear/modificar: `docs/ESTADO-FASES.md`, README (limitación).
- Fuera de alcance: añadir otro LLM.
- Criterios de aceptación: Veredicto MUST / MUST-lento / NICE escrito.
- Verificación: humano.
- Notas técnicas: Prohibido "Gemma funciona bien en 8 GB" sin tabla.

---

### T-088 · Implementar `HoldWatcher`

- Origen: 1.10.1
- Tipo: feature
- Tamaño: S
- Depende de: T-061, CP-3
- Paralelizable con: T-084
- Objetivo: Poll de holds activos de la cuenta vigilada en commonMain.
- Contexto a leer primero: prompt P8 tarea 1; plan §4 backend=ninguno.
- Archivos a crear/modificar: `app/shared/src/commonMain/kotlin/HoldWatcher.kt`
- Fuera de alcance: FCM/APNs/servidor; persistir destinos que escriba la dueña.
- Criterios de aceptación: Lee `StellarGateway.readHolds`. No importa Android.
- Verificación: test con Fake que emite un hold nuevo.
- Notas técnicas: 30–60 s en demo; documentar batería.

### T-089 · Notificación local Android

- Origen: 1.10.2
- Tipo: feature
- Tamaño: M
- Depende de: T-088, T-075
- Paralelizable con: ninguna
- Objetivo: WorkManager o foreground 30–60 s. Destino y monto del RPC. Deep link a Detalle guardián. Botón Actualizar.
- Contexto a leer primero: prompt P8 tareas 2–6.
- Archivos a crear/modificar: `app/android/src/main/kotlin/notifications/**` (≤3) + permiso en manifest.
- Fuera de alcance: backend, FCM, APNs.
- Criterios de aceptación: Limitación "si el SO mata el trabajo, el aviso se retrasa" queda para README (T-099). Notificación no es fuente de verdad.
- Verificación: T-090.
- Notas técnicas: Si el background es inútil, la demo usa Actualizar. No se añade servidor.

### T-090 · Cronometrar aviso del guardián

- Origen: 1.10.3
- Tipo: test
- Tamaño: S
- Depende de: T-089, T-080
- Paralelizable con: ninguna
- Objetivo: queue dueña → notificación guardián < 60 s. Destino = cadena.
- Contexto a leer primero: prompt P8 TESTS.
- Archivos a crear/modificar: `evidence/guardian/notification.md`
- Fuera de alcance: simular la notificación en la app de la dueña.
- Criterios de aceptación: Tiempo medido, capturas, destino comparado con `read-rules`/hold.
- Verificación: manual cronometrado. REAL.
- Notas técnicas: Dos dispositivos o emulador + teléfono.

### CP-8 · Humano: escena 3 punta a punta

- Origen: 1.10 (exit)
- Tipo: decisión
- Tamaño: S
- Depende de: T-090
- Paralelizable con: T-091
- Objetivo: Diego se entera y detiene.
- Contexto a leer primero: `evidence/guardian/notification.md`
- Archivos a crear/modificar: `docs/ESTADO-FASES.md`
- Fuera de alcance: push real de plataforma.
- Criterios de aceptación: Escena 3 reproducible o fallback Actualizar dicho en voz alta.
- Verificación: humano.
- Notas técnicas: El veto sin aviso no protege; si solo hay Actualizar, el pitch lo dice.

---

### T-091 · Scripts: pago detenido y guardián gasta

- Origen: 1.11.1
- Tipo: test
- Tamaño: M
- Depende de: CP-3, CP-6
- Paralelizable con: T-092, T-093, T-094
- Objetivo: Ataques K y D ejecutables ante el jurado.
- Contexto a leer primero: prompt P9 tarea 1 bullets 1–2; plan demo escenas 4–5.
- Archivos a crear/modificar: `scripts/attacks/held-after-cancel.ps1`, `scripts/attacks/guardian-transfer.ps1`
- Fuera de alcance: mainnet; arreglar un bypass en silencio.
- Criterios de aceptación: Cada script termina en rechazo con código de error. Hash REAL.
- Verificación: `stellar` invoke según el script. Salida en evidencia (T-097).
- Notas técnicas: Escena 5 es obligatoria en vivo.

### T-092 · Scripts: destino, monto y muxed

- Origen: 1.11.1
- Tipo: test
- Tamaño: M
- Depende de: CP-3, CP-6
- Paralelizable con: T-091, T-093, T-094
- Objetivo: Adversario G + R7.
- Contexto a leer primero: prompt P9 (cambiar destino/monto, muxed).
- Archivos a crear/modificar: `scripts/attacks/wrong-destination.ps1`, `wrong-amount.ps1`, `muxed-destination.ps1`
- Fuera de alcance: aceptar muxed.
- Criterios de aceptación: Tres rechazos con hash.
- Verificación: stellar-cli.
- Notas técnicas: Comparación tipada.

### T-093 · Scripts: segunda regla, execute, upgrade, enforce

- Origen: 1.11.1
- Tipo: test
- Tamaño: M
- Depende de: CP-3, CP-6
- Paralelizable con: T-091, T-092, T-094
- Objetivo: H, execute/upgrade, N. Alimenta escena 6.
- Contexto a leer primero: prompt P9; GH-13, 25, 26.
- Archivos a crear/modificar: `scripts/attacks/add-rule.ps1`, `execute.ps1`, `upgrade.ps1`, `direct-enforce.ps1`
- Fuera de alcance: añadir las funciones para "probarlas" en la cuenta.
- Criterios de aceptación: Error (no existen o GH rechaza).
- Verificación: stellar-cli + `read-rules` sigue en 1.
- Notas técnicas: Si execute existiera, PARA.

### T-094 · Scripts: approve, nested, replay, queue ajeno

- Origen: 1.11.1
- Tipo: test
- Tamaño: M
- Depende de: CP-3, CP-6
- Paralelizable con: T-091…T-093
- Objetivo: I/J, M, F/L, O.
- Contexto a leer primero: prompt P9 resto de bullets.
- Archivos a crear/modificar: `scripts/attacks/approve.ps1`, `nested-transfer.ps1`, `replay.ps1`, `queue-without-owner.ps1`
- Fuera de alcance: contrato N de producción; puede vivir bajo `scripts/attacks/nested/`.
- Criterios de aceptación: Rechazos con hash. Nested puede requerir contrato de ataque; si no se desplegó, `no corrido` (no "pendiente").
- Verificación: stellar-cli.
- Notas técnicas: El prompt pide también transfer_from; incluirlo en `approve.ps1` o archivo quinto solo si no se supera el tope — si no cabe, `approve-transfer-from.ps1` sustituye a uno por fusión.

### T-095 · Cerrar cobertura GH-28, GH-29, GH-30, GH-33

- Origen: 1.11.2
- Tipo: test
- Tamaño: M
- Depende de: T-006, T-047, T-082
- Paralelizable con: T-096
- Objetivo: Estado real de los 33 GH: verde / fallido / no corrido.
- Contexto a leer primero: security spike §7 extras; prompt P9 tarea 2.
- Archivos a crear/modificar: `evidence/policies/tests.md` (actualizar filas)
- Fuera de alcance: eufemismos ("pendiente").
- Criterios de aceptación: Tabla de 33 IDs (incl. 07b, 18b, 20b). SHOULD no corridos se ven como `no corrido`.
- Verificación: conteo de IDs = especificación del spike.
- Notas técnicas: GH-33 desde Android, no navegador.

### T-096 · Sesión atacante de dos horas

- Origen: 1.11.3
- Tipo: test
- Tamaño: L
- Depende de: T-091, T-092, T-093, T-094
- Paralelizable con: T-095
- Objetivo: Buscar un bypass **fuera** de la lista. Documentar intentos que no funcionaron.
- Contexto a leer primero: prompt P9 tarea 4; threat model plan §8.
- Archivos a crear/modificar: `evidence/security/attacker-session.md`
- Fuera de alcance: ocultar un bypass; si pasa, STOP, arreglar, re-correr toda la suite o retirar el claim.
- Criterios de aceptación: Bitácora con hora e intentos. Revisión: cada script de T-091…T-094 cae en una rama de `enforce` identificable.
- Verificación: humano en CP-9.
- Notas técnicas: Dos horas. No alargar en silencio.

### T-097 · Escribir matriz de bypass

- Origen: 1.11.4
- Tipo: docs
- Tamaño: M
- Depende de: T-095, T-096
- Paralelizable con: ninguna
- Objetivo: Una fila por ataque + verificación on-chain final (1 rule, GH, sin execute/upgrade, guardián no firmante, registro no toca tokens).
- Contexto a leer primero: prompt P9 EVIDENCE; plan §12.
- Archivos a crear/modificar: `evidence/security/bypass-matrix.md`
- Fuera de alcance: claims "no existe bypass".
- Criterios de aceptación: Columnas: precondición, acción, esperado, REAL, hash, error, propiedad P1…P5. Incluye intentos fallidos de T-096.
- Verificación: hashes copiados de salida. `rg "no existe bypass|único|primero" evidence/security` vacío.
- Notas técnicas: Si un ataque PASA y no se cierra, se retira el claim en T-104.

### CP-9 · Humano: claims vs evidencia

- Origen: 1.11 (exit)
- Tipo: decisión
- Tamaño: S
- Depende de: T-097
- Paralelizable con: T-098
- Objetivo: Qué se puede afirmar en pitch y README.
- Contexto a leer primero: `bypass-matrix.md`
- Archivos a crear/modificar: `docs/ESTADO-FASES.md`
- Fuera de alcance: dejar un claim huérfano.
- Criterios de aceptación: Lista de propiedades demostradas vs retiradas.
- Verificación: humano.
- Notas técnicas: Presentar de más es peor que presentar menos.

---

### T-098 · Completar árbol `evidence/`

- Origen: 1.12.1
- Tipo: docs
- Tamaño: M
- Depende de: CP-9
- Paralelizable con: T-103
- Objetivo: Cada evidencia relevante con Test, Expected, Actual, Contract, Tx, Hash, Timestamp, I/O, captura, conclusión. Etiquetas REAL/SIMULATED/ILLUSTRATIVE.
- Contexto a leer primero: plan §12.
- Archivos a crear/modificar: huecos en `evidence/**` (varios; si >5 archivos nuevos, una sesión por subcarpeta: architecture, guardian, payments, smart-account, policies, ai, security, stellar, demo — partir y anotar).
- Fuera de alcance: inventar un hash para llenar un hueco (borrar la afirmación).
- Criterios de aceptación: Árbol de la §12 poblado o con `no corrido`.
- Verificación: listar `evidence/` y cruzar con la §12.
- Notas técnicas: SAC de prueba ≠ USDC oficial.

### T-099 · Reescribir README final

- Origen: 1.12.2
- Tipo: docs
- Tamaño: M
- Depende de: T-098
- Paralelizable con: T-100
- Objetivo: Arquitectura, cero backend, cómo testear y redesplegar, pin OZ, qué se demuestra y qué no, fases INCOMPLETAS nombradas.
- Contexto a leer primero: prompt P10 tarea 2; frases prohibidas plan §12.
- Archivos a crear/modificar: `README.md`
- Fuera de alcance: "primero", "único", "Soroban lo garantiza", "solo en Stellar".
- Criterios de aceptación: Limitaciones: testnet, sin auditar, guardián fijo, aviso puede retrasarse, passkey ciega, si Diego no mira nadie detiene.
- Verificación: `rg -i "seguro|protegido|único|primero|no existe bypass" README.md`
- Notas técnicas: Meaningful use de Stellar, no exclusividad.

### T-100 · Ensayar escenas 1–3

- Origen: 1.12.3
- Tipo: test
- Tamaño: M
- Depende de: CP-5, CP-8, T-098
- Paralelizable con: T-099
- Objetivo: Pago confianza, mensaje+hold+rechazo inmediato, veto del guardián.
- Contexto a leer primero: plan §13 escenas 1–3.
- Archivos a crear/modificar: `evidence/demo/scene-1.md`, `scene-2.md`, `scene-3.md`
- Fuera de alcance: escena 2a si IA es NICE.
- Criterios de aceptación: Hash por escena. Tiempo parcial anotado.
- Verificación: hashes en explorador.
- Notas técnicas: Hold de demo = 120 s.

### T-101 · Ensayar escenas 4–6

- Origen: 1.12.3
- Tipo: test
- Tamaño: M
- Depende de: T-091, T-093, T-070, T-100
- Paralelizable con: T-102 (después de un primer pase)
- Objetivo: Ataque CLI al detenido, guardián gasta, approve + segunda regla + panel de reglas. Dos corridas seguidas < 5 min las seis.
- Contexto a leer primero: plan §13 escenas 4–6; prompt P10 tarea 3.
- Archivos a crear/modificar: `evidence/demo/scene-4.md`, `scene-5.md`, `scene-6.md`
- Fuera de alcance: omitir escena 5.
- Criterios de aceptación: Cronómetro < 5 min, dos veces. Escena 5 en vivo obligatoria.
- Verificación: humano + hashes.
- Notas técnicas: Guion de cierre del plan §13.

### T-102 · Grabar vídeo de respaldo

- Origen: 1.12.4
- Tipo: docs
- Tamaño: M
- Depende de: T-101
- Paralelizable con: T-104
- Objetivo: Grabar **el día anterior**. Hashes visibles. Cubre caída de testnet.
- Contexto a leer primero: prompt P10 tarea 4; plan R9.
- Archivos a crear/modificar: `evidence/demo/video.md` (link + fecha). El binario no se commitea si pesa; queda fuera de git o en release.
- Fuera de alcance: sustituir hashes reales por overlay falso.
- Criterios de aceptación: Fecha de grabación < día de pitch. Hashes coinciden con evidence.
- Verificación: humano abre el vídeo.
- Notas técnicas: El WBS dejó abierta si el vídeo de 3 min de Passport es el mismo; T-007/humano lo aclara. Si son dos, este es el de respaldo; el de 3 min se recorta del mismo material.

### T-103 · Hacer 3–5 entrevistas y prueba de 5 s

- Origen: 1.12.7
- Tipo: docs
- Tamaño: M
- Depende de: T-073
- Paralelizable con: T-091…T-101
- Objetivo: Demanda del guardián no validada + kill test visual de 5 s.
- Contexto a leer primero: pitch checklist; visual §16 kill test; Validación §38.
- Archivos a crear/modificar: `evidence/demo/interviews.md`
- Fuera de alcance: afirmar validación de mercado sin citas.
- Criterios de aceptación: 3–5 entrevistas cortas. 3 personas ajenas ven Detalle en Retenido 5 s y responden qué pasa. Sin inventar citas.
- Verificación: humano. SIMULATED si no se hicieron y entonces no se usan en el pitch.
- Notas técnicas: Una cita real para "¿la gente aceptaría un guardián?".

### T-104 · Corregir pitch y Describe your project

- Origen: 1.12.5
- Tipo: docs
- Tamaño: M
- Depende de: T-007, T-101, T-103
- Paralelizable con: T-102
- Objetivo: Láminas MOVA + texto Passport **sin** TrustedPayee, Spending Limit, Channels, claims de novedad. Nombrar Yandex Pay, OZ Timelock, Argent/Ready, Monzo.
- Contexto a leer primero: prompt P10 tareas 5–6; pitch (solo texto usable); plan §14 diferenciación.
- Archivos a crear/modificar: `docs/SUBMISSION.md` (checklist + Describe your project), notas de láminas en `docs/` (no reescribir la Propuesta para el pitch original).
- Fuera de alcance: tocar el doc de pitch histórico como si fuera spec; SINPE/fiat.
- Criterios de aceptación: Inglés en Describe. Enlaces reales o placeholders `[link]` no fingidos. Criterios oficiales cubiertos sin mentir.
- Verificación: `rg -i "TrustedPayee|Spending Limit|Channels|primero|único" docs/SUBMISSION.md` vacío (salvo "eliminamos TrustedPayee").
- Notas técnicas: "Se podría en EVM con Safe + Delay + guard".

### T-105 · Revisar frases prohibidas y secretos

- Origen: 1.12.8
- Tipo: test
- Tamaño: S
- Depende de: T-078, T-099, T-104
- Paralelizable con: ninguna
- Objetivo: Lectura cruzada: 5 claims del README → evidence. Git history sin secretos. Direcciones = testnet.
- Contexto a leer primero: prompt P10 TESTS y SECURITY; plan frases prohibidas.
- Archivos a crear/modificar: `docs/SUBMISSION.md` (checklist firmado).
- Fuera de alcance: inventar evidencia para salvar un claim (borrar el claim).
- Criterios de aceptación: rg de frases prohibidas en README, SUBMISSION, strings UI = vacío. `git log -p` / scan de `*.seed`, `*.jks`, `.env` limpio.
- Verificación:
  ```
  rg -i "seguro|protegido|único|primero|no existe bypass|Soroban lo garantiza" README.md docs/SUBMISSION.md app/shared/src/commonMain
  git ls-files "*.seed" "*.jks" "*.keystore" ".env"
  ```
- Notas técnicas: Testnet addresses only.

### T-106 · Enviar entrega Passport

- Origen: 1.12.6
- Tipo: docs
- Tamaño: S
- Depende de: T-001, T-102, T-105
- Paralelizable con: ninguna
- Objetivo: Repo público, video, Describe, checklist. Antes de G1.
- Contexto a leer primero: prompt P10; `docs/SUBMISSION.md`.
- Archivos a crear/modificar: `docs/SUBMISSION.md` (casillas).
- Fuera de alcance: enviar si G1 ya pasó (PARA).
- Criterios de aceptación: Confirmación de envío escrita. Fecha/hora.
- Verificación: humano (Passport).
- Notas técnicas: Crear cuenta ≠ inscribir equipo.

### T-107 · Redactar acta de aceptación

- Origen: 1.13.1
- Tipo: docs
- Tamaño: S
- Depende de: T-106
- Paralelizable con: ninguna
- Objetivo: Cada fase contra DoD. MVP no se declara completo si falta un eje.
- Contexto a leer primero: `docs/ESTADO-FASES.md`; prompt contexto común DoD.
- Archivos a crear/modificar: `docs/ACTA-CIERRE.md`
- Fuera de alcance: marcar cerrada una INCOMPLETA.
- Criterios de aceptación: Tabla P0–P10 con bloqueos nombrados.
- Verificación: humano.
- Notas técnicas: Compila ≠ done.

### T-108 · Documentar lecciones y backlog FUTURE

- Origen: 1.13.2
- Tipo: docs
- Tamaño: S
- Depende de: T-107
- Paralelizable con: T-109
- Objetivo: Aprendido + FUTURE sin construirlo.
- Contexto a leer primero: plan §9 FUTURE / DO NOT BUILD.
- Archivos a crear/modificar: `docs/ACTA-CIERRE.md` (sección lecciones)
- Fuera de alcance: implementar FUTURE.
- Criterios de aceptación: Lista FUTURE = la del plan, no ideas nuevas de alcance.
- Verificación: no aparecen SINPE, mainnet, tool calling como "siguiente sprint comprometido" salvo FUTURE explícito.
- Notas técnicas: Cambiar guardián con espera+veto es FUTURE.

### T-109 · Crear tag de entrega

- Origen: 1.13.3
- Tipo: setup
- Tamaño: S
- Depende de: T-107
- Paralelizable con: T-108
- Objetivo: Tag inmutable de lo enviado. Decidir destino de `spikes/` (quedan o se archivan; el plan permite borrarlas).
- Contexto a leer primero: prompt P1 rollback spikes.
- Archivos a crear/modificar: tag git (humano/agente con permiso). No force-push.
- Fuera de alcance: reescribir history; publicar semillas.
- Criterios de aceptación: Tag anotado. `spikes/` o bien en el tag o borradas con nota.
- Verificación: `git tag -l`
- Notas técnicas: Solo si el usuario pide el tag/commit. Este archivo no autoriza push.

---

## C. Matriz de trazabilidad (WBS original → tareas)

| ID WBS | Nombre (WBS original) | Tareas |
| --- | --- | --- |
| 1 | GuardPay MVP | T-001…T-109 + CP-0…CP-9 |
| 1.1 | Gestión del proyecto | T-001, T-002, T-006, T-007, T-013, T-014, CP-0…CP-9, T-107 |
| 1.1.1 | Gates G1 y G3 | T-001, T-002, T-007 |
| 1.1.2 | Plan y línea base | T-006, T-013 |
| 1.1.3 | Registro de riesgos | T-014 |
| 1.1.4 | Seguimiento de fases | T-013 (actualizar en cada CP) |
| 1.2 | Fundación P0 | T-010…T-018, CP-0 |
| 1.2.1 | `.gitignore` | T-010 |
| 1.2.2 | README inicial | T-018 |
| 1.2.3 | Carpetas + evidence README | T-011, T-012 |
| 1.2.4 | DEPENDENCIES | T-017 |
| 1.2.5 | Toolchain + G2 | T-015, T-016 |
| 1.3 | Spikes P1 | T-019…T-033, CP-1 |
| 1.3.1 | Spike A0 digest | T-019 |
| 1.3.2 | Spike A1 R6 | T-020 |
| 1.3.3 | Spike A2–A3 + 13 tests | T-021…T-028 |
| 1.3.4 | Spike B Gemma | T-009, T-029…T-031 |
| 1.3.5 | Spike C firma KMP | T-032, T-033 |
| 1.4 | Contratos P2 | T-034…T-049, CP-2 |
| 1.4.1 | Workspace + vendor | T-034 |
| 1.4.2 | `account` | T-035 |
| 1.4.3 | `guardian_hold` | T-038…T-041, T-048 |
| 1.4.4 | `hold_registry` | T-036, T-037 |
| 1.4.5 | SHOULD contrato | T-049 |
| 1.4.6 | Tests MUST | T-042…T-047 |
| 1.4.7 | Review lista blanca | T-048 |
| 1.5 | Deploy P3 | T-050…T-054, CP-3 |
| 1.5.1 | Script deploy | T-050, T-051 |
| 1.5.2 | `read-rules` | T-052 |
| 1.5.3 | Tests ★ testnet | T-053 |
| 1.5.4 | Rules on-chain | T-054 |
| 1.6 | Núcleo KMP P4 | T-055…T-065, CP-4 |
| 1.6.1 | Gradle KMP | T-055 |
| 1.6.2 | Domain | T-004, T-056, T-057 |
| 1.6.3 | StellarGateway | T-058, T-060…T-062 |
| 1.6.4 | expect Signer / AI | T-059 |
| 1.6.5 | Carril previsto | T-063 |
| 1.6.6 | Tests KMP + arquitectura | T-064 |
| 1.6.7 | Shells android/ios | T-065 |
| 1.7 | UI P5 | T-066…T-078, CP-5 |
| 1.7.1 | Tokens | T-008, T-066 |
| 1.7.2 | Chips + espera | T-067 |
| 1.7.3 | Entrada | T-003, T-068 |
| 1.7.4 | Inicio + reglas | T-069, T-070 |
| 1.7.5 | Pagar + hoja | T-071, T-072 |
| 1.7.6 | Detalle dueña | T-073 |
| 1.7.7 | Modo guardián | T-074, T-075 |
| 1.7.8 | Responsive + a11y | T-076, T-077 |
| 1.7.9 | Tests UI | T-078 |
| 1.8 | Auth P6 | T-079…T-083, CP-6 |
| 1.8.1 | Signer Android | T-079 |
| 1.8.2 | Firma + envío | T-005, T-080 |
| 1.8.3 | Tests auth | T-081 |
| 1.8.4 | Passkey SHOULD | T-082 |
| 1.8.5 | Signer iOS SHOULD | T-083 |
| 1.9 | IA P7 | T-084…T-087, CP-7 |
| 1.9.1 | actual Android | T-084 |
| 1.9.2 | Parser/filtros | T-085 |
| 1.9.3 | Tests IA | T-086 |
| 1.9.4 | actual iOS SHOULD | T-087 |
| 1.10 | Aviso P8 | T-088…T-090, CP-8 |
| 1.10.1 | HoldWatcher | T-088 |
| 1.10.2 | Notificación Android | T-089 |
| 1.10.3 | Cronómetro | T-090 |
| 1.11 | Adversarial P9 | T-091…T-097, CP-9 |
| 1.11.1 | Scripts de ataque | T-091…T-094 |
| 1.11.2 | Cierre GH extras | T-095 |
| 1.11.3 | 2 h atacante | T-096 |
| 1.11.4 | Matriz + verify on-chain | T-097 |
| 1.12 | Evidencia y entrega P10 | T-098…T-106 |
| 1.12.1 | evidence/ completa | T-098 |
| 1.12.2 | README final | T-099 |
| 1.12.3 | Ensayo 6 escenas | T-100, T-101 |
| 1.12.4 | Vídeo respaldo | T-102 |
| 1.12.5 | Pitch | T-104 |
| 1.12.6 | Envío | T-106 |
| 1.12.7 | Entrevistas | T-103 |
| 1.12.8 | Compliance final | T-105 |
| 1.13 | Cierre | T-107…T-109 |
| 1.13.1 | Acta DoD | T-107 |
| 1.13.2 | Lecciones + FUTURE | T-108 |
| 1.13.3 | Tag | T-109 |
| Dudas WBS sin paquete propio | Entrada / estados / clave guardián / logo / corpus AI | T-003, T-004, T-005, T-008, T-009 |

Cobertura: todos los paquetes 1.1.1…1.13.3 del WBS original tienen al menos una `T-xxx`. Los NICE, FUTURE y DO NOT BUILD del WBS original **siguen fuera**.

---

## D. Supuestos y decisiones pendientes

### Supuestos (heredados del WBS original + este desglose)

1. Este archivo no cambia horas ni alcance del WBS original. Las estimaciones siguen siendo las de aquella tabla (~205 h; camino crítico ~86 h).
2. MUST y SHOULD están descompuestos. SHOULD (T-049, T-082, T-083, T-087 y parte de T-095) solo se ejecutan si T-006 lo autoriza.
3. NICE / FUTURE / DO NOT BUILD no tienen `T-xxx` de implementación.
4. G1 debe ser 12 oct para abrir P1. Si no, el grafo se detiene en CP-0.
5. Spike A verde es condición de T-034. 7.A no está descompuesto: es contingencia humana.
6. Los spikes viven en `spikes/` y no se copian a producción; P2 se reescribe en `contracts/`.
7. GH-33 se ejecuta en Android (T-082), no en navegador.
8. GH-11 = "quien paga fees no autoriza"; no se construye patrocinador.
9. Tests ★ de testnet = los 10 marcados en el security spike (GH-02…10, GH-16).
10. Comandos Gradle/Cargo exactos del módulo pueden ajustarse al bootstrap real (T-055/T-034); no se inventa un CI.
11. Windows es el entorno primario; `.ps1` es MUST de scripts, `.sh` se declara `no corrido` si no hay bash.
12. Un agente no hace commit ni push salvo que el usuario lo pida. T-109 no autoriza force-push.
13. `docs/GuardPay — Propuesta para el pitch.md` y la arquitectura de Validación CR/Chile (TrustedPayee, approve del guardián, Channels, tool calling) no se implementan.

### Decisiones pendientes (bloquean las T- de tipo feature indicadas)

| Decisión | Tarea | Bloquea |
| --- | --- | --- |
| Fecha Passport vs Luma, inscripción, campos | T-001 | CP-0, P1, T-106 |
| ¿Hay Mac? | T-002 | T-083, T-087, compile iOS |
| Qué hace Entrada | T-003 | T-068 |
| Lista de estados UI (Firmando / Vencido) | T-004 | T-056, T-067, T-078 |
| Cómo llega la clave del guardián al segundo teléfono | T-005 | T-050, T-080 |
| Orden de recorte SHOULD | T-006 | T-049, T-082, T-083, T-087, T-095 |
| Evento real y elegibilidad | T-007 | T-104, T-106 |
| ¿Existe logo? | T-008 | T-065 / wordmark |
| 10 mensajes + modelo de teléfono | T-009 | T-029 |
| ¿Vídeo Passport 3 min = vídeo de respaldo? | (queda en T-007 / humano; no inventado) | T-102 |
| Layout ≥1024 en la demo | no hay T aparte: T-076 lo implementa porque P5 lo pide; el lado a lado ≥1280 sigue NICE y fuera | — |

### Requisitos del WBS original que no generaron tarea de código

Igual que en el WBS original: NICE (lado a lado, link compartible, ventana móvil 24 h, chips de monto rápido), FUTURE, revisión de apps bancarias (~1 h, nunca fue paquete), variante de pitch Chile (depende de T-007), listar rechazos ajenos en la app (el visual lo prohíbe: no hay indexador).

Si una decisión de la tabla de arriba no se toma, el agente **no elige por su cuenta**: deja la feature `bloqueada` y actualiza `docs/ESTADO-FASES.md`.
