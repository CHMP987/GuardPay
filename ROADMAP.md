# Hoja de ruta de GuardPay

Esta hoja de ruta no tiene fechas, estimaciones ni responsables: el repositorio no los define y este documento no los inventa. El orden dentro de cada bloque es una sugerencia de dependencia, no un calendario.

Tiene dos partes que no se mezclan:

- **[Parte A](#parte-a-requisitos-confirmados-pendientes)**: trabajo que los documentos del propio proyecto ya piden y que sigue abierto. La fuente se cita en cada punto.
- **[Parte B](#parte-b-recomendaciones-del-auditor)**: recomendaciones que salen de [`AUDIT_REPORT.md`](AUDIT_REPORT.md). Son **[RECOMMENDATION]**: el equipo decide si las adopta.

Lo ya hecho está en [`README.md`](README.md#estado-actual) y en [`docs/ESTADO-FASES.md`](docs/ESTADO-FASES.md).

## Parte A: requisitos confirmados pendientes

### A-1 · Pruebas adversarias sin correr: GH-29, GH-30 y GH-33

Fuente: `docs/GuardPay — Security spike y propuesta web definitiva.md` (tabla GH), `docs/ESTADO-FASES.md` (P9).

- **GH-29**: dos pagos en el mismo ledger no duplican el tope.
- **GH-30**: un pago madurado vence. Hoy no existe `expires_at` (depende de A-2).
- **GH-33**: firma con passkey. El cliente actual firma con ed25519 en el Keystore; GH-33 es SHOULD en el plan.

Criterios de aceptación:

- Cada prueba existe en `contracts/guardian_hold/tests/` o en un script de `scripts/attacks/`, y su resultado está en `evidence/security/bypass-matrix.md` con etiqueta REAL.
- Si una prueba no aplica al diseño actual (por ejemplo GH-33 sin passkeys), queda escrito como "no aplica" con el motivo, no como "pasa".

### A-2 · T-049: cancelación por la dueña y vencimiento (`expires_at`)

Fuente: `docs/WBS-AGENTES.md` (T-049, "pendiente"), `docs/ESTADO-FASES.md` ("T-049 no está autorizado"), plan de construcción (SHOULD).

Requiere autorización humana antes de empezar.

Criterios de aceptación:

- La dueña puede detener su propio pago retenido; el guardián sigue sin poder mover fondos ni volverse firmante.
- Un pago madurado sin enviar vence y la política lo rechaza después del vencimiento (GH-30 en verde).
- Las 39 pruebas existentes de `must.rs` siguen pasando.

### A-3 · Firmas humanas pendientes: CP-3, CP-7 y CP-9

Fuente: `README.md` ("Las firmas de revisión humana CP-3, CP-7 y CP-9 siguen pendientes"), `docs/ESTADO-FASES.md`.

Criterio de aceptación: cada firma queda registrada en `docs/ESTADO-FASES.md` con la base de evidencia que revisó la persona.

### A-4 · Entrevistas con usuarios

Fuente: `evidence/interviews.md` ("no corrido"), `docs/ESTADO-FASES.md` (P10).

Criterio de aceptación: las entrevistas se hacen y se registran en `evidence/interviews.md` con su etiqueta; ningún dato se completa sin una entrevista real.

### A-5 · Envío en Passport

Fuente: `docs/ESTADO-FASES.md` (P10: "Passport no enviado"), gate G1.

Criterio de aceptación: el envío se registra en `docs/ESTADO-FASES.md`. Este documento no repite la fecha límite; está en el gate G1.

### A-6 · Tareas de P0 abiertas

Fuente: `docs/ESTADO-FASES.md` ("P0 sigue INCOMPLETA … T-010, T-011, T-012, T-013, T-014 y T-018 no están hechas"), `docs/WBS-AGENTES.md`.

Observación: varias ya existen en el repositorio (`.gitignore`, `docs/ESTADO-FASES.md`, `README.md`), pero su estado en la WBS no se actualizó. `evidence/README.md` (T-012) no existe.

Criterios de aceptación:

- La WBS y `ESTADO-FASES.md` reflejan qué tareas de P0 están hechas.
- Las que faltan se hacen o se marcan como descartadas, con el motivo.

### A-7 · Build de release conectado

Fuente: `README.md` ("El build de release no se conecta a nada todavía"), `app/android/src/release/Wiring.kt`.

Criterios de aceptación:

- El build de release obtiene su configuración de testnet sin leer archivos de almacenamiento externo (ver B-7).
- `./gradlew :app:android:compileReleaseKotlin` y las pruebas siguen pasando.
- Sigue apuntando solo a testnet.

## Parte B: recomendaciones del auditor

Todas son **[RECOMMENDATION]**. Las de mayor severidad van primero.

### B-1 · Decidir y corregir la liberación de retenciones a contactos (F-01, F-03)

Criterios de aceptación:

- Está escrito qué debe pasar con una retención a un contacto de confianza, por encima y por debajo del tope.
- Hay pruebas GH nuevas para ambos casos, y pasan.
- Después de una liberación, el registro queda `Executed` (o la app ya no permite crear esa retención).
- `SimulatedStellarGateway` se comporta igual que el contrato en esos casos, con una prueba que lo compare.
- Si cambia el contrato, las cuentas desplegadas se redespliegan en testnet y la evidencia se actualiza.

Estado (12 oct 2026, rama `fix-f01`): los cuatro primeros criterios están cumplidos en el código. Ver la corrección en [F-01](AUDIT_REPORT.md#f-01-correccion). El último está cumplido en parte (12 oct): el CI compiló el wasm, la política nueva `CBTIF2BF…` está en testnet, una cuenta nueva corrió el ciclo con F-01 y `evidence/stellar/deployment.md` está actualizado. Falta reprovisionar las cuentas de los teléfonos, que siguen con la política anterior. Con la corrección, todo `transfer` de USDC recorre el registro, lo que hace más urgente B-2.

### B-2 · Medir y acotar el recorrido lineal del registro (F-02)

Criterios de aceptación:

- Hay una medición del costo de `queue` y de una liberación con un número creciente de registros por cuenta, guardada en `evidence/` con su etiqueta.
- Si el límite de Soroban se alcanza con un uso plausible, el registro deja de recorrer todos los registros (por ejemplo, un índice de retenciones activas).
- `hold_registry` tiene pruebas unitarias propias (F-13).

### B-3 · Cambio de guardián y recuperación (F-04)

Criterios de aceptación:

- Existe un diseño escrito, revisado contra la tesis: el guardián nunca se vuelve firmante ni puede mover fondos.
- Si se implementa, tiene pruebas adversarias propias.

### B-4 · Auditoría externa y dependencia de OpenZeppelin (F-05)

Criterios de aceptación:

- Antes de cualquier uso fuera de testnet, una auditoría externa de los tres contratos con su informe en el repositorio.
- El pin de OpenZeppelin se reevalúa frente a una versión estable, con GH-31 en verde.

### B-5 · Validar el token en `queue` y no romper la lectura (F-06)

Criterios de aceptación:

- La política rechaza `queue` con un token distinto del configurado, con una prueba.
- O bien el cliente ignora registros de otro token sin dejar de leer los demás, con una prueba en `ChainDecodersTest`.

### B-6 · Mostrar el motivo de un rechazo (F-07)

Criterio de aceptación: `SubmitClassifier` devuelve el código de error del contrato y la interfaz muestra un texto por código, sin usar las palabras vetadas por `checkUiVocabulary`.

### B-7 · Integridad de archivos locales (F-08, F-09)

Criterios de aceptación:

- `GemmaModelFile` verifica el SHA-256 también de un archivo ya presente.
- Un build conectado no toma su RPC ni sus contratos de un archivo editable en almacenamiento externo.

### B-8 · Endurecer el CI (F-10)

Criterios de aceptación:

- Las acciones se fijan por SHA.
- El CI compila el wasm con `stellar contract build` y publica su hash.
- Se agregan `cargo clippy`, `cargo fmt --check` y una auditoría de dependencias, sin acceso a la red de Stellar.

### B-9 · Licencia y canal de seguridad (F-11, F-19)

Criterios de aceptación:

- El equipo elige una licencia y agrega `LICENSE`.
- El equipo establece un canal privado de reporte y lo escribe en [`SECURITY.md`](SECURITY.md).

### B-10 · Poner al día la documentación (F-12)

Criterios de aceptación:

- `docs/ESTADO-FASES.md` deja de contradecirse sobre P7.
- `docs/DEPENDENCIES.md` cita las versiones actuales.
- El comentario de `GuardPayAI.kt` nombra la tarea correcta.
- `AGENTS.md` se revisa o se marca como histórico.

### B-11 · Cobertura en hardware y plataformas

Fuente: limitaciones del `README.md`.

Criterios de aceptación:

- El ciclo retener → esperar → enviar y GH-01/GH-24 se repiten desde un teléfono físico.
- Una prueba con dos teléfonos físicos queda registrada.
- El consumo de batería de la IA y del sondeo cada 30 s queda medido.

## Fuera de alcance

Ni la Parte A ni la Parte B incluyen mainnet, un backend, push (FCM/APNs), un pagador de comisiones ni que el guardián firme. Lo excluyen la tesis y las restricciones de [`CLAUDE.md`](CLAUDE.md).
