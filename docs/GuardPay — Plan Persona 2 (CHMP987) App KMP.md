# GuardPay — Plan de la Persona 2 (CHMP987): App KMP

Plan personal derivado de `GuardPay — Plan de construcción.md` (sección 5, "Reparto del equipo"). Si este documento y el plan se contradicen, manda el plan. Los prompts ejecutables de cada fase están en `GuardPay — Prompts de ejecución.md`. Aquí van **el orden, las entregas y los cortes**; no se repiten los prompts.

**Etiquetas:** `[FACT]` · `[INFERENCE]` · `[RECOMMENDATION]` · `[UNVERIFIED]`.

---

## 1. Mi frente en una línea

Construyo la app que usan la dueña y el guardián: el núcleo compartido (`commonMain`), las 4 pantallas, la firma desde el teléfono y el aviso al guardián. **No escribo contratos ni la IA**; los consumo por interfaz.

| | |
| --- | --- |
| **Fases** | P1-C (Spike C) → P4 → P5 → P6 → P8 |
| **Carpetas mías** | `app/` salvo `app/.../ai/` (eso es de Ant) |
| **Archivo del que soy responsable** | `gradle/libs.versions.toml` (versiones exactas, nunca `latest` ni rangos) |
| **Consumo de Abraham** | `docs/INTERFACES.md` (firmas de contrato, errores, lecturas) y `evidence/stellar/deployment.md` (direcciones) |
| **Consumo de Ant** | el `actual` Android de `GuardPayAI` |
| **Entrego a Ant** | el `expect interface GuardPayAI` + un stub Android que devuelve `NoClearSignals` (día 2 temprano) |
| **Entrego a Ant para P9/P10** | una app que firma en testnet y capturas de los 9 estados |

**Riesgo propio:** `[INFERENCE]` es el frente con más fases y depende de dos entregas externas: las direcciones de Abraham y la IA de Ant. Por eso todo se construye primero contra `FakeStellarGateway` y un stub de IA. **Nunca espero a nadie para avanzar.**

---

## 2. Antes del día 1 (hoy, ≤ 1 h)

Estado del entorno comprobado en esta máquina `[FACT]`: JDK 17 instalado; Android SDK en `%LOCALAPPDATA%\Android\Sdk` (build-tools, cmake, cmdline-tools).

- [x] Android Studio instalado el 5 oct (build 262.9437.185, en `C:\Program Files\Android\Android Studio`). Usa el SDK que ya existe en `%LOCALAPPDATA%\Android\Sdk`.
- [x] Plugin de Kotlin Multiplatform 262.9437.115-AS cargado (verificado en `idea.log`, 5 oct). Android Studio apunta al SDK existente.
- [x] **Teléfono Android conectado y autorizado por `adb`:** Samsung Galaxy A54 (SM-A546E), Android 14 (API 34), 7,3 GB de RAM visibles. `[INFERENCE]` Cumple con margen la RAM que necesita Gemma 4 E2B (R4), pero su chip es Exynos 1380, no Snapdragon: las cifras de tok/s del plan no aplican directamente. `[UNVERIFIED]` hasta que Ant corra el Spike B en este teléfono.
- [ ] Segundo dispositivo para el guardián: otro Android, un emulador, o el iPhone si hay Mac (ver §7).
- [ ] Leer la demo de smart accounts de `Soneso/kmp-stellar-sdk` v1.14.0. Es la referencia del Spike C.
- [ ] Leer `GuardPay — Propuesta visual.md` §5 (colores), §7 (pantallas), §8 (estados) y §9 (guardián).
- [x] G3 resuelto (5 oct): **nadie tiene Mac.** iOS se declara en Gradle pero no se compila, y el README lo dice.

---

## 3. Calendario (fecha límite 12 oct)

### Día 1 — Spike C + esqueleto

| Bloque | Qué | Salida |
| --- | --- | --- |
| Mañana | Esqueleto Gradle KMP: `app/shared` con los paquetes `domain`, `stellar`, `signing` y `ai`; target Android; iOS declarado. `libs.versions.toml` con versiones exactas. | `./gradlew :shared:allTests` corre (aunque esté vacío) |
| Mañana | Test de arquitectura: falla si `commonMain` importa `android.*`, LiteRT-LM o WebAuthn. | Test verde |
| Tarde | **Spike C:** firmar un `transfer` en Kotlin con ed25519 contra la cuenta del Spike A de Abraham. Si aún no existe, contra la cuenta de la demo del SDK en testnet. Verificar el digest `sha256(signature_payload ‖ xdr(context_rule_ids))`. | `evidence/stellar/spike-c.md` con el hash REAL, o el escalón de R5 al que se llegó |
| Noche | **Sync 1** con Abraham y Ant: veredicto del Spike A y congelación de `INTERFACES.md`. | Firmas congeladas |

**Lo que tengo que exigir en el Sync 1.** Si no está en `INTERFACES.md`, el día 4 no cierra:
- Firmas exactas de `queue`, `cancel` y `transfer` (vía la cuenta), con los tipos de sus argumentos.
- **Las lecturas que necesita la UI:**
  - holds de una cuenta, cada uno con `id`, `to`, `amount`, `created_at`, `ready_at`, estado y quién canceló;
  - contactos de confianza, tope diario, gastado hoy y duración de la retención;
  - reglas de la cuenta, para "Reglas de esta cuenta".
- Códigos de error de `enforce` y del registro, para traducirlos a "Rechazado por el contrato" con la frase correcta.

### Días 2–3 — P4 núcleo compartido

- **Día 2, primera hora:** subir el `expect GuardPayAI` y el stub Android para que Ant enchufe P7 sin tocar mis carpetas.
- **Dominio:** los estados de la propuesta visual como tipos sellados (Borrador, Firmando, Retenido, Listo para enviar, Enviado, Detenido, Vencido (SHOULD), Rechazado por el contrato, Error de red). El sello "En Stellar" es una marca, no un estado.
- **Gateway:** `StellarGateway` (interfaz), `FakeStellarGateway` **solo en `commonTest`** y `KmpStellarGateway` (esqueleto, se completa el día 4).
- **Carril:** cálculo de Immediate | MustHold. Es informativo; la autoridad es el contrato.
- **Tests:** transiciones válidas e inválidas (de Detenido y de Enviado no se sale), bordes del tope y parseo de lecturas.
- **Fin del día 3:** P4 cerrado según su prompt, con `evidence/architecture/kmp-layers.md`.

### Día 4 — P5 UI + gateway real

- **Mañana:** UI contra el fake. El orden va por valor para la demo: **Pagar + "Revisar y firmar" → Detalle → Guardián (lista, detalle, confirmación de "Detener") → Inicio → Entrada**.
- **Sync 2,** cuando Abraham publica las direcciones: cambiar a `KmpStellarGateway` y leer datos reales de testnet.
- **Tokens exactos:** Navy `#0B1220` para la acción, Teal `#00A99D` solo para evidencia, nunca texto blanco sobre Teal. IBM Plex Sans/Mono. Pantallas desde 360 px.
- **Test de vocabulario:** falla si aparece "seguro" o "protegido" en los strings de la UI.

### Día 5 — P6 firma + P8 aviso (Abraham apoya P6)

- **P6:** `actual Signer` Android con ed25519 en Keystore.
  - Firmar `queue` y `transfer` y, en la app del guardián, `cancel`.
  - Reproducir GH-24 y GH-01 contra testnet desde la app.
- **Passkey (GH-33):** solo si sobra tiempo; es SHOULD.
- **P8:** `HoldWatcher` con consulta periódica al registro.
  - Notificación local con destino y monto leídos de la cadena, y enlace al Detalle.
  - Al abrirla, el Detalle vuelve a leer del RPC.
  - Objetivo: menos de 60 s.
- **Reparto con Abraham:** si a mediodía no funciona el ciclo queue → esperar → enviar, él toma el cableado de la firma contra el contrato.

### Día 6 — Sync 3: congelación

- Nada nuevo; solo arreglos de lo que encuentre Ant en P9.
- Capturas de los 9 estados en `evidence/demo/screens/`.
- Ensayar dos veces las escenas 1, 2, 3 y 6 del plan de demo, que son las que tocan la app. Grabar el vídeo de respaldo.

### Día 7 — Apoyo a la entrega

- Apoyar a Ant en P10: README de `app/`, y los comandos reales de build y test anotados en `CLAUDE.md`.

---

## 4. Definición de terminado por fase

| Fase | Terminado cuando |
| --- | --- |
| P1-C | Una firma producida en Kotlin es aceptada por `__check_auth` en testnet, con su hash en `evidence/`, o quedó registrado el escalón de R5 al que se llegó. |
| P4 | `./gradlew :shared:allTests` verde, y el test de arquitectura también. Ninguna clase que tenga `Signer` o `StellarGateway` recibe `GuardPayAI`. `FakeStellarGateway` queda fuera de producción. |
| P5 | Las 4 pantallas navegables en Android con datos reales de testnet. Ningún estado se distingue solo por color. Nunca "Retenido" sin registro en la cadena. El guardián no tiene monto, ni "Pagar", ni "Aprobar", ni ajustes. |
| P6 | Un `transfer` firmado desde el teléfono se ejecuta en testnet, y uno sin la firma de la dueña se rechaza. La clave no sale del Keystore ni aparece en logs. El guardián nunca se añade como firmante. |
| P8 | Un `queue` en el teléfono de la dueña genera una notificación en el del guardián en menos de 60 s, con el destino leído de la cadena. Capturas y tiempos en `evidence/guardian/notification.md`. |

"Compila" no es terminado.

---

## 5. Si algo falla

| Falla | Qué hago |
| --- | --- |
| **Spike C rojo** | Fallback de R5, en orden: (a) auth entry a mano con las primitivas XDR del SDK; (b) ed25519 crudo; (c) la app solo lee y las firmas van por `stellar-cli` en la demo, **dicho en voz alta**. P4 y P5 no cambian, porque todo está detrás de `StellarGateway` y `Signer`. |
| **Las direcciones llegan tarde** (el Sync 2 se mueve) | Sigo con el fake. El cambio a la red real es una línea de inyección. |
| **La IA de Ant no llega** | El stub devuelve `NoClearSignals` y la UI oculta la tarjeta de análisis. La escena 2 se hace sin mensaje. |
| **Voy atrasado el día 5** | Abraham toma P6, y P8 se reduce al botón "Actualizar" en la vista del guardián. |
| **Me quedo sin tiempo en P5** | Corto en este orden: Entrada → "Reglas de esta cuenta" plegable → animación de la línea de espera. **Nunca** corto Pagar, Detalle ni Guardián: sin ellas no hay escenas 1 a 3. |

---

## 6. Reglas que no rompo

- `commonMain` sin Android, iOS, LiteRT-LM ni WebAuthn.
- `GuardPayAI` nunca toca `Signer` ni `StellarGateway`, y nunca rellena el destino ni el monto.
- El guardián nunca es firmante de la cuenta.
- Ni "seguro" ni "protegido" en la UI. "Retenido" solo con registro en la cadena.
- Testnet siempre. Versiones exactas. Ninguna clave ni semilla en el repo.
- No invento hashes: lo que no corrí se anota como "no corrido".

---

## 7. iOS — G3 = no hay Mac (5 oct)

- **Sin Mac:** `iosMain` existe con sus `actual` y el target se declara. El README dice "no compilado en este entorno"; no se afirma que iOS funcione.
- **Con Mac + iPhone:** `[RECOMMENDATION]` el iPhone es **el teléfono del guardián**.
  - La pantalla del guardián no necesita IA. En la demo la app está abierta, así que el límite de iOS en segundo plano no afecta.
  - Se depura con Xcode y un Apple ID gratis; la instalación dura 7 días.
  - La firma en iOS va en Keychain como llave de software, porque Secure Enclave no soporta ed25519. Se dice tal cual.
- iOS es lo primero que se recorta si falta tiempo.

---

## 8. Pendiente de confirmar

- [ ] G1: fecha límite (5 o 12 oct). Lo resuelve Ant en la primera hora del día 1.
- [x] G3: nadie tiene Mac. Si alguien consigue uno antes del día 4, se aplica la opción "con Mac + iPhone" de §7.
- [ ] ¿Cuántos teléfonos Android hay? Tengo uno. Para la demo hace falta otro dispositivo para el guardián: un segundo Android (de Abraham o Ant) o un emulador.
