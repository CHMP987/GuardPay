# GuardPay — Propuesta visual

Oct 5, 2026 · @Justin

## 1. Executive Visual Direction

**Dirección elegida: "Calma verificable".** GuardPay se ve tranquilo y claro: fondo Ice, texto Navy, una sola acción por pantalla. El color solo aparece cuando le pasa algo al dinero, y Guard Teal solo cuando habla la cadena. La pieza propia de la marca es la **línea de espera**: una barra que muestra un pago retenido avanzando hacia su hora de liberación, con el nombre del guardián encima.

**Sensación objetivo:** "Sé qué pasa con mi dinero, quién puede frenarlo y dónde comprobarlo". Es más preciso que "esto protege mis pagos", porque cada palabra de la interfaz tiene que corresponder a algo que el contrato hace.

**Por qué esta dirección y no otra:**

- **Calma en lugar de alarma.** Un pago retenido es el producto funcionando, no un error. Si se ve como alerta roja o como escudo de antivirus, el usuario lo vive como un fallo y el jurado como "otra app de seguridad".
- **El color tiene significado o no aparece.** Teal es la marca y también significa "esto está en Stellar". Por eso no se gasta en botones ni en decoración: los botones principales van en Navy.
- **Lo que diferencia a GuardPay se ve, no se explica.** La línea de espera con el guardián y el sello "En Stellar" cuentan la historia sin texto largo, que es lo que necesita una demo de pocos minutos.
- **Viene de lo que funciona en MOVA:** una pregunta por pantalla, opciones grandes y una progresión en tres momentos (Preparar → Esperar → Resultado).

**Cuatro ajustes que esta propuesta hace al brief**, porque la auditoría de seguridad los exige:

1. **No hay un estado visible "Authorized".** Firmar la retención crea el pago ya retenido; no existe un paso intermedio en el contrato. "Firmando…" es un momento del botón, no una etiqueta de estado.
2. **"Released" no sucede solo.** Al llegar la hora, el pago pasa a "Listo para enviar" y la dueña firma otra vez. La interfaz lo muestra como un paso, no como algo automático.
3. **La interfaz no dice "protegido" ni "seguro".** Dice lo que pasa: "Retenido hasta las 14:32. Diego puede detenerlo".
4. **Frase guía en la interfaz:** "La IA advierte. Tú firmas. Tu guardián puede detener. El contrato rechaza lo demás." No usa "Soroban hace cumplir", porque lo que hace cumplir es el código de GuardPay corriendo en Soroban.

## 2. Audit of MOVA Reference

Se revisó `MOVA_presentacion.pdf` (5 láminas, Reto iTEC 2026). MOVA aporta dos cosas distintas: la **app** (las maquetas de teléfono) y la **presentación** (las láminas). Las dos sirven.

### Lo que funciona y se reutiliza

| Elemento de MOVA | Por qué funciona | Cómo se convierte en GuardPay |
| --- | --- | --- |
| Una pregunta como título de pantalla ("¿Qué quieres hacer con este tiempo?") | Dice qué se decide antes de mostrar controles | "¿A quién le pagas?" en Pagar; en el guardián, "¿Detienes este pago?" |
| Dos opciones grandes con título y subtítulo (Aprovechar / Delegar) | La elección se entiende sin leer ayuda | Las dos tarjetas de carril: "Sale ahora" (contacto de confianza) y "Con espera" (cualquier otro). La app marca cuál aplica; no se elige libremente salvo "retener de todos modos". |
| Progresión en tres momentos: Entrada → Durante → Al llegar | Cuenta una historia en el tiempo | **Preparar → Esperar → Resultado.** Es la columna vertebral del pago retenido y de la demo. |
| Pantalla "Al llegar" con el resultado arriba y una acción | Primero qué pasó, después qué hacer | El Detalle del pago empieza por el estado y su siguiente paso. |
| Un solo botón ancho abajo ("Ver 3 opciones") | Una acción principal, fácil de alcanzar con el pulgar | Un solo botón Navy por pantalla, fijo abajo en móvil. |
| Chips segmentados para valores cerrados (15/30/45/60, Bus/Carro/A pie) | Elegir sin escribir | Montos rápidos para contactos de confianza (solo si hay tiempo; NICE). |
| Láminas numeradas con encabezado + 3 bloques | El jurado lee la estructura en segundos | El pitch de GuardPay repite el patrón: Problema → Qué hace el contrato → Demo → Qué no sabemos. |
| Honestidad visible ("Cambios de v2: propuestos, aún sin probar") | Da credibilidad | Etiquetas como "Análisis de IA · puede equivocarse" y "leído del contrato a las 13:05". |

### Lo que se descarta

| Elemento de MOVA | Por qué se descarta |
| --- | --- |
| Azul rey + verde lima | No es la paleta de GuardPay. Además, en MOVA el lima es decorativo, y en GuardPay cada color significa algo. |
| Encuesta al llegar ("¿Valió la pena?" Sí/No) | Es investigación de MOVA, no una función de GuardPay. |
| Barra de navegación de 3 pestañas | Con cuatro pantallas y una acción principal, las pestañas solo suman ruido. Se navega desde Inicio. |
| Patrón de íconos de fondo | Decoración que compite con el estado del pago. |
| La misma maqueta repetida en todas las láminas, con texto ilegible al proyectar | En el pitch de GuardPay, cada lámina muestra solo la pantalla de su escena, a un tamaño legible. |
| Duración elegida por la persona (15–60 min) | En GuardPay la duración la fija el contrato. Mostrar un selector de tiempo sería una mentira visual. |
| Hora falsa "7:45" en la barra de estado | En GuardPay el tiempo es dato real (hora de liberación), no decorado. |

**Regla resultante:** de MOVA se toma la gramática (pregunta → opciones grandes → un botón → resultado) y el ritmo en tres momentos. Nada de su contenido, color ni navegación.

## 3. Visual Research

Solo lo que justifica una decisión. Cada hallazgo termina en lo que GuardPay hace con él.

| # | Hallazgo | Evidencia | Qué hace GuardPay |
| --- | --- | --- | --- |
| 1 | **Las advertencias que dependen de la víctima fallan.** Revolut mostró advertencias, cinco preguntas (incluida "¿alguien te dice qué responder?") y cinco pantallas de cautela. El cliente, guiado por el estafador, respondió con falsedades y el pago salió. | 🟢 Financial Ombudsman Service, decisión DRN-5886851 | Nada de cuestionarios ni muros de advertencias. La protección visible es una persona (el guardián) y un tiempo, no un formulario. |
| 2 | **Un tercero de confianza ya es un patrón de banca.** Monzo deja que un contacto de confianza revise pagos grandes y alerte, pero no bloquear. Yandex Pay deja que lo apruebe o rechace, sin mover el dinero. | 🟢 ayuda de Monzo; 🟡 prensa sobre Yandex Pay | El guardián se muestra como una persona con nombre y foto, no como un "sistema". Su poder único (detener) es lo único que su pantalla permite. |
| 3 | **El seguimiento por etapas da tranquilidad.** Wise muestra la transferencia en etapas con frases humanas ("Your money's being processed", "Transfer sent"). | 🟢 ayuda de Wise | La línea de espera usa etapas con frases humanas ("Retenido", "Listo para enviar", "Enviado"), no códigos. |
| 4 | **La interfaz puede mentir aunque el contrato no.** En el robo a Bybit (21 feb 2025, unos US$1.460 millones), el frontend de Safe{Wallet} fue alterado: los firmantes vieron una transferencia rutinaria y firmaron otra. | 🟡 Communications of the ACM | Nada de lo crítico se muestra solo desde el backend. El estado y los datos se leen de la cadena, y el guardián ve el destino real leído del contrato, con enlace al explorador. |
| 5 | **Las wallets multisig ponen la cola de transacciones en el centro.** Safe recomienda verificar destino, valor y datos antes de firmar ("If you can't verify it, don't sign it"), y propone llevar la confianza de la UI a la "transaction queue". | 🟢 ayuda de Safe; 🟡 foro de Safe, jul 2025 | Los pagos retenidos son la lista principal de Inicio y del guardián. El detalle muestra destino y monto completos antes de cualquier firma. |
| 6 | **Las confirmaciones cansan si se abusa de ellas.** Se reservan para lo destructivo o irreversible, con un verbo concreto en el botón ("Delete repository", no "Sí"). | 🟢 GitHub Primer, guía de Confirmation dialog | Una sola confirmación en toda la app: "Detener este pago". Pagar a un contacto no pide confirmación extra; la firma con passkey ya es la confirmación. |
| 7 | **Contraste:** el texto normal necesita 4,5:1, el texto grande 3:1, y los componentes de interfaz 3:1 contra lo que los rodea. | 🟢 W3C, WCAG 2.2 | Ver sección 5: Teal y ámbar no sirven como color de texto sobre blanco, así que se usan tonos derivados. |
| 8 | **Las wallets de Stellar muestran el detalle de la operación antes de firmar**, y existen kits de interfaz para passkeys financiados por el Stellar Community Fund. | 🟡 tiendas de extensiones y SCF (no se revisó cada interfaz) | GuardPay no imita una wallet de Stellar; usa la passkey del sistema operativo y explica antes de la firma qué se va a firmar. |

### Respuestas a las 7 preguntas del brief

1. **¿Qué genera confianza?** Datos concretos (nombre, monto, hora exacta) y la posibilidad de comprobarlos afuera, más que símbolos de seguridad. 🔵 Inferencia de 3, 4 y 5.
2. **¿Qué simplifica pagos?** Una decisión por pantalla y no pedir confirmaciones redundantes. 🟢 Hallazgo 6.
3. **¿Cómo se comunica el riesgo?** Mejor por lo que ocurrirá ("esperará hasta las 14:32") que por un puntaje. 🟣 Recomendación: GuardPay no muestra puntajes de riesgo.
4. **¿Cómo se muestran las acciones irreversibles?** Con un botón de peligro, el verbo exacto y la consecuencia. 🟢 Hallazgo 6.
5. **¿Cómo mostrar seguridad sin abrumar?** Con divulgación progresiva: el dato humano arriba y la prueba técnica plegada abajo. 🟣
6. **¿Qué errores cometen fintechs y wallets?** Muros de advertencias que el estafador enseña a saltar (1), firma de lo que la interfaz muestra sin verificación independiente (4), y hashes crudos como "prueba" sin explicar qué prueban. 🔵
7. **¿Qué puede hacer GuardPay diferente?** Hacer visible a la persona que puede frenar, el tiempo real que queda y el enlace a la regla que lo impone. 🟣

## 4. GuardPay Visual Identity

### Personalidad

**Sereno, preciso y transparente.** Habla como una persona tranquila que sabe exactamente qué pasa: frases cortas en segunda persona, horas exactas y nombres propios. No usa metáforas de guerra ("amenaza", "ataque"), ni escudos, candados gigantes o el lenguaje de la jerga cripto.

### Tipografía: IBM Plex Sans + IBM Plex Mono

- **Por qué esta:** es una familia con versión monoespaciada hermana. Los hashes, direcciones e IDs van en Mono y el resto en Sans, sin mezclar dos estilos ajenos. Tiene cifras alineadas para montos, soporta español, es variable (100–700) y tiene licencia libre OFL (🟢 Fontsource). Se siente técnica sin ser fría, y se distingue de la Inter que usan casi todas las wallets (🔵).
- **Uso de Mono:** solo para lo que se puede comprobar en la cadena: hashes, direcciones, IDs y la hora de liberación en la prueba. Así la tipografía misma dice "esto es verificable".

| Rol | Tamaño / alto de línea | Peso | Ejemplo |
| --- | --- | --- | --- |
| Monto principal | 40 / 48 | 600, cifras tabulares | 150,00 USDC |
| Título de pantalla (pregunta) | 24 / 32 | 600 | ¿A quién le pagas? |
| Título de tarjeta | 17 / 24 | 600 | Retenido hasta las 14:32 |
| Texto | 16 / 24 | 400 | Diego puede detenerlo. |
| Etiqueta y chip | 13 / 18 | 500 | En Stellar |
| Dato verificable | 13 / 20, Mono | 400 | 7f3a…c91e |

Nunca menos de 13 px. El texto base de móvil es 16 px.

### Iconografía

- **Estilo:** línea, trazo de 1,75 px en una cuadrícula de 24, terminaciones redondeadas y sin relleno. Se recomienda la librería abierta Lucide (ISC), que se usa tal cual sin crear íconos propios de marca.
- **Abstracción:** literal. Cada ícono nombra una acción o un estado, siempre acompañado de texto.
- **Set cerrado (10 íconos):** reloj de arena (Retenido), reloj con check (Listo), check (Enviado), mano abierta (Detenido), círculo tachado (Rechazado por el contrato), wifi tachado (Error de red), usuario (Guardián), mensaje con líneas (IA), enlace externo (En Stellar), copiar.
- **Prohibidos:** escudos, candados, rayos, destellos "mágicos" para la IA y robots.

### Formas y bordes

| Elemento | Radio | Tratamiento |
| --- | --- | --- |
| Tarjetas | 12 | Fondo White sobre Ice, borde de 1 px Navy al 10 %, sin sombra |
| Botones | 12; alto de 52 en móvil | Principal: relleno Navy, texto White. Secundario: borde Navy, texto Navy. Peligro: borde y texto Danger; relleno Danger solo en la confirmación. |
| Inputs | 10; alto de 52 | Borde Navy al 50 % (3,5:1); con foco, borde Navy de 2 px |
| Chips de estado | Píldora | Fondo del color semántico al 12 %, texto en su tono derivado (sección 5), ícono a la izquierda |
| Hoja inferior (revisar y firmar, confirmar detención) | 20 arriba | Única superficie con sombra |

### Espaciado

Base de 4 px. Escala: 4, 8, 12, 16, 24, 32, 48. Margen lateral en móvil: 16 (20 a partir de 390 px de ancho). 24 entre tarjetas, 16 dentro de ellas y 48 antes del botón fijo.

### Sombras

Una sola sombra en toda la app (y = 2, desenfoque 8, Navy al 8 %), solo en hojas inferiores y diálogos. Las tarjetas se separan por borde, no por sombra.

### Logo y wordmark

- **Convivencia:** el wordmark "GuardPay" en Plex Sans 600 Navy, con la marca en Teal, a 24 px de alto arriba a la izquierda en Inicio. No aparece dentro de los estados ni en la confirmación de detención, para que nada compita con la decisión.
- **En móvil:** solo la marca (sin texto) en pantallas internas; el wordmark completo solo en Entrada e Inicio.
- **Favicon e ícono de app:** la marca en Teal sobre un cuadrado Navy de esquinas redondeadas. A 16 px se dibuja sin detalles finos.
- Esta propuesta no rediseña el logo. Si el actual usa un escudo o un candado, se recomienda no repetirlo en la interfaz.

## 5. Color System

Se usa exactamente la paleta oficial. Los únicos tonos nuevos son versiones más oscuras del mismo color, necesarias para que el texto sea legible (WCAG), y transparencias. Los contrastes se calcularon con la fórmula de WCAG.

### Roles

| Color | Hex | Significa | Dónde va | Dónde nunca va |
| --- | --- | --- | --- | --- |
| Navy | `#0B1220` | La voz del producto | Texto, botón principal, banda del modo guardián | Fondos de página completos en el modo dueña |
| Guard Teal | `#00A99D` | **Marca + "esto está en Stellar"** | Marca, sello "En Stellar", borde de la sección de prueba, regla activa | Botones, fondos grandes, decoración |
| Mint | `#5EEAD4` | Teal sobre fondo oscuro | Detalles y foco dentro de la banda Navy del guardián | Sobre fondo claro (contraste 1,5:1) |
| Ice | `#F5F9FA` | Calma | Fondo de página | — |
| White | `#FFFFFF` | Superficie | Tarjetas, hojas inferiores | — |
| Success | `#16A34A` | Pagó y salió | Estado Enviado; carril "Sale ahora" | Decoración; IA |
| Warning / Held | `#D97706` | Está esperando | Retenido y Listo para enviar; la línea de espera | Errores; advertencias de la IA |
| Danger / Cancelled | `#DC2626` | Alguien lo frenó, o el contrato lo negó | Detenido (relleno), Rechazado por el contrato (contorno), botón Detener | Pagos retenidos; errores de red |
| Verified / Stellar | `#00A99D` | Igual que Teal | Sello "En Stellar" | — |

**Decisión clave:** como el brief da el mismo hex a la marca y a "Verificado", Teal no puede usarse a la vez para botones. Si se usara, cualquier botón parecería "verificado". Por eso **la acción es Navy y la evidencia es Teal.** Los botones no verifican nada; la cadena sí.

### Contraste (calculado)

| Combinación | Contraste | Resultado |
| --- | --- | --- |
| Navy sobre White / Ice | 18,7 / 17,7 | Texto ✔ |
| White sobre Navy | 18,7 | Botón principal ✔ |
| Teal sobre White | 2,93 | ✘ texto, ✘ componente (necesita 3:1) |
| White sobre Teal | 2,93 | ✘: **nunca texto blanco sobre Teal** |
| Navy sobre Teal | 6,39 | ✔: si hay algo relleno de Teal, su texto es Navy |
| Mint sobre Navy | 12,7 | ✔ |
| Warning sobre White | 3,19 | ✘ texto; ✔ solo como relleno o barra grande |
| Success sobre White | 3,30 | ✘ texto; ✔ ícono grande |
| Danger sobre White / Ice | 4,83 / 4,56 | ✔ texto (justo en Ice) |

### Tonos derivados (solo para texto e íconos pequeños)

| Derivado de | Hex derivado | Sobre White | Sobre Ice | Uso |
| --- | --- | --- | --- | --- |
| Teal | `#007E75` | 4,95 | 4,67 | Texto del sello "En Stellar", enlaces a la prueba |
| Warning | `#A25904` | 5,29 | 4,99 | Texto de "Retenido" y de la cuenta regresiva |
| Success | `#107A37` | 5,44 | 5,13 | Texto de "Enviado" |

Cada derivado es su color oficial al 75 % de luminosidad: mismo tono, más oscuro. En pantalla se perciben como la misma familia.

### Neutrales

Todos salen de Navy con transparencia: texto secundario al 64 % (5,5:1 sobre Ice), bordes de inputs al 50 % (3,5:1, cumple el 3:1 de componentes), bordes decorativos de tarjetas al 10 %, deshabilitado al 38 % (solo en elementos no interactivos). Los estados neutros (Vencido, Error de red) usan Navy al 64 %, sin color semántico.

### Modo oscuro

Fuera del MVP. La banda del guardián ya usa Navy + Mint, lo que deja probada la base de una futura versión oscura.

## 6. Information Architecture

Una sola app con dos modos, que se eligen según quién inicia sesión. Cuatro pantallas más la entrada, sin barra de pestañas. Todo nace en Inicio.

| Área | Modo | Contiene | Se llega desde |
| --- | --- | --- | --- |
| **A. Entrada** | Ambos | Qué es GuardPay en 3 líneas; "Entrar con passkey" | Primera visita |
| **B. Inicio** | Dueña | Saldo, tu guardián, pagos retenidos con su línea de espera, pagos recientes y la sección plegable "Reglas de esta cuenta" | Entrada |
| **C. Pagar** | Dueña | Destinatario, monto, carril, análisis de IA opcional, y la hoja "Revisar y firmar" | Botón "Pagar" de Inicio |
| **D. Detalle del pago** | Ambos | Estado y siguiente paso, línea de espera, destino y monto, guardián, análisis de IA (si existe) y la sección plegable "Ver en Stellar" | Cualquier pago de Inicio o de Guardián; resultado de Pagar |
| **E. Guardián** | Guardián | A quién proteges, sus pagos retenidos y lo que ya detuviste | Entrada (con su passkey) o el enlace del aviso |

**Lo que no existe** (por decisión de producto y de seguridad): configuración, edición de contactos, gestión de guardianes, historial completo con filtros, perfil, notificaciones dentro de la app y chat con la IA.

**Recorrido principal:**

- **Pago confiable:** Inicio → Pagar → Revisar y firmar → Detalle (Enviado) → Inicio.
- **Pago con espera:** Inicio → Pagar → Revisar y firmar → Detalle (Retenido) → (tiempo) → Detalle (Listo) → firmar → Detalle (Enviado).
- **Guardián:** aviso → Detalle (modo guardián) → Detener este pago → Detalle (Detenido).

Este es el ritmo de MOVA en tres momentos (Preparar → Esperar → Resultado), con la diferencia de que el "Esperar" lo impone el contrato.

## 7. Screen-by-Screen Proposal

Todas las pantallas usan el mismo esqueleto móvil: arriba la marca o "Atrás"; luego una pregunta o un estado como título; luego el contenido en tarjetas White sobre Ice; y abajo, fijo, un solo botón principal.

### A. Entrada

- **Objetivo:** que en 5 segundos se entienda qué hace GuardPay y cómo entrar.
- **Usuario:** dueña o guardián, en su primera visita.
- **Información principal:** wordmark; título "Paga al instante a quien conoces. A los demás, con una espera que tu guardián puede detener."; tres líneas con ícono: "Tus contactos: sale ahora", "Desconocidos: espera y tu guardián puede detenerlo", "Tu guardián no puede mover tu dinero".
- **Acción principal:** "Entrar con passkey".
- **Elementos secundarios:** "Soy guardián de alguien", que lleva al mismo login; el modo lo decide la cuenta.
- **Estado:** sin sesión.
- **Comportamiento:** el sistema operativo muestra el diálogo de la passkey; la app no pide contraseñas.
- **Principio de seguridad comunicado:** la asimetría del guardián se dice antes de entrar.
- **Relación con MOVA:** como el lado izquierdo de sus láminas: una frase grande y tres puntos.
- **Decisión visual:** Ice con un único bloque Navy que contiene el título. Es la única pantalla del modo dueña con un bloque oscuro grande; da presencia de marca sin escudos.

### B. Inicio (dueña)

- **Objetivo:** responder "¿qué está pasando con mi dinero ahora?". No responde "¿está seguro?", porque la app no puede afirmarlo.
- **Usuario:** dueña.
- **Información principal:** saldo en USDC (40 px); debajo, una línea de estado como "Reglas activas en Stellar · 1 pago retenido", con el sello Teal "En Stellar" que lleva a "Reglas de esta cuenta"; la tarjeta del guardián ("Diego te acompaña. Puede detener pagos retenidos, no moverlos").
- **Acción principal:** "Pagar".
- **Elementos secundarios:** la lista "En espera" (cada pago con su línea de espera, destino, monto y hora); "Recientes" (5 como máximo, con chip de estado); "Contactos de confianza" (hasta 3, con lo que queda del tope de hoy); y "Reglas de esta cuenta" plegada.
- **Estado:** si no hay pagos retenidos, la sección "En espera" no aparece. No hay estados vacíos con ilustración.
- **Comportamiento:** los datos se leen de la cadena al abrir y cada 15 s mientras haya pagos retenidos. Muestra "leído a las 13:05" en gris.
- **Principio de seguridad comunicado:** lo que espera, quién lo cuida y dónde comprobarlo están en la primera pantalla.
- **Relación con MOVA:** el saldo y la línea de estado cumplen el papel de su pregunta-título; el botón único abajo es el mismo patrón.
- **Decisión visual:** sin gráficos de saldo ni métricas. El único color fuerte es el ámbar de un pago retenido, si existe. Sin pagos retenidos, la pantalla es casi monocroma, y eso es lo correcto.

### C. Pagar

- **Objetivo:** armar un pago y saber antes de firmar si sale ahora o con espera.
- **Usuario:** dueña.
- **Información principal:** título "¿A quién le pagas?"; los contactos de confianza como fichas con avatar, o un campo de dirección con la etiqueta "Otra cuenta"; después, el monto en grande.
- **Acción principal:** "Revisar".
- **Elementos secundarios:** la tarjeta de carril, que se marca sola. Verde: "Sale ahora · contacto de confianza · te quedan 40 USDC de tu tope de hoy". Ámbar: "Con espera · saldrá después de las 14:32 si Diego no lo detiene y tú lo confirmas". Debajo, un enlace plegado: "¿Te pidieron este pago por mensaje? Pégalo y lo revisamos" (IA, sección 10).
- **Estado:** borrador ("Este pago aún no se envió").
- **Comportamiento:** el carril lo anticipa la app con las mismas reglas que el contrato, y lo confirma el contrato al firmar. Si un pago a un contacto supera el tope, la tarjeta pasa a ámbar y lo explica. En un pago a un contacto, "Retener este pago" aparece como enlace secundario.
- **Principio de seguridad comunicado:** la espera se anuncia antes de firmar, así que nunca llega como sorpresa ni como error.
- **Relación con MOVA:** pregunta como título + tarjetas de opción con título y subtítulo (Aprovechar / Delegar → Sale ahora / Con espera).
- **Decisión visual:** la tarjeta de carril lleva una barra de color de 4 px a la izquierda y su ícono. Es el único color de la pantalla.

### C2. Revisar y firmar (hoja inferior)

- **Objetivo:** que la dueña vea exactamente qué va a firmar.
- **Usuario:** dueña.
- **Información principal:** destino (el nombre si es contacto; si no, los primeros y últimos 4 caracteres en Mono con "ver completa"), monto, activo (USDC), carril y, en una frase, qué pasará después.
- **Acción principal:** "Firmar y enviar" o "Firmar y retener". El verbo dice qué crea la firma.
- **Elementos secundarios:** "Cambiar".
- **Estado:** firmando (transitorio). El botón muestra "Esperando tu passkey…" y luego "Enviando a Stellar…". No es un estado del pago.
- **Comportamiento:** si se cancela la passkey, se vuelve a la hoja y nada cambió. Si el contrato rechaza, se pasa al Detalle con "Rechazado por el contrato".
- **Principio de seguridad comunicado:** se firma algo concreto. La hoja no dice "seguro".
- **Relación con MOVA:** su "Ver 3 opciones" es un paso intermedio antes de comprometerse; aquí ese paso es la revisión.
- **Decisión visual:** una hoja inferior con sombra, la única superficie elevada del flujo.

### D. Detalle del pago (dueña)

- **Objetivo:** decir en qué estado está un pago y qué puede pasar después.
- **Usuario:** dueña (el modo guardián está en la sección 9).
- **Información principal:** arriba, el chip de estado y una frase ("Retenido hasta las 14:32"); la línea de espera con tres etapas (Retenido → Listo → Enviado) y el avatar del guardián sobre el tramo de espera; destino y monto.
- **Acción principal:** depende del estado. En Listo para enviar: "Firmar y enviar". En Retenido: ninguna (el botón aparece desactivado con la hora en que se activa). En Detenido: "Crear un pago nuevo".
- **Elementos secundarios:** "Detener mi pago" (SHOULD), el análisis de IA si existe y "Ver en Stellar" plegado al final.
- **Estado:** todos los de la sección 8.
- **Comportamiento:** el estado se lee del contrato. Si la lectura falla, se muestra el último valor conocido con "sin actualizar desde las 13:05"; nunca un estado inventado.
- **Principio de seguridad comunicado:** el tiempo es real y alguien con nombre puede frenar el pago.
- **Relación con MOVA:** su pantalla "Al llegar": resultado arriba, una acción y luego el detalle.
- **Decisión visual:** la línea de espera es el elemento distintivo de GuardPay: un riel Navy al 10 %, un relleno ámbar que avanza, puntos para las etapas, la hora de liberación en el extremo y el avatar del guardián con "puede detenerlo" sobre el tramo ámbar.

### E. Guardián

Descrita completa en la sección 9.

## 8. Payment State System

Los estados son la identidad del producto, así que cada uno tiene color, ícono, forma y frase propios. Nunca se distinguen solo por color. Solo se muestran estados que el contrato puede confirmar.

&#91;embedded content: estados de un pago · 3 en el contrato, 2 derivados del tiempo, 1 solo en la app\]

El camino ámbar es lo que diferencia a GuardPay: un pago que espera no es un error, es una etapa con hora de llegada. Para que un pago a un desconocido salga hacen falta tres cosas: que llegue la hora, que nadie lo detenga y que la dueña firme otra vez.

| Estado (en la UI) | Brief | Origen | Color | Ícono y forma | Frase | Acción disponible |
| --- | --- | --- | --- | --- | --- | --- |
| Borrador | Draft | Solo app | Ninguno (Navy) | Sin chip | "Este pago aún no se envió" | Revisar |
| *Firmando…* | Authorized | Momento del botón, no estado | — | Indicador dentro del botón | "Esperando tu passkey…" | Cancelar en el diálogo del sistema |
| Retenido | Held | Contrato (registro) | Warning: relleno 12 % + texto `#A25904` | Reloj de arena + línea de espera avanzando | "Retenido hasta las 14:32. Diego puede detenerlo." | Dueña: Detener mi pago (SHOULD). Guardián: Detener este pago. |
| Listo para enviar | (Held maduro) | Derivado: ahora ≥ hora de liberación | Warning + botón Navy activo | Reloj con check; la línea de espera llena | "Listo. Necesita tu firma para salir." | Firmar y enviar |
| Enviado | Released / Executed | Contrato (marcado) o transferencia | Success: relleno 12 % + texto `#107A37` | Check | "Enviado a Ana · 13:40" | Ver en Stellar |
| Detenido | Cancelled | Contrato | Danger: chip **relleno** | Mano abierta + avatar de quien lo detuvo | "Detenido por Diego a las 13:10" | Crear un pago nuevo |
| Vencido (SHOULD) | — | Derivado | Neutral (Navy 64 %) | Reloj tachado | "Venció sin enviarse" | Crear un pago nuevo |
| Rechazado por el contrato | Rejected | Resultado de la transacción | Danger: solo **contorno** | Círculo tachado | "El contrato no permitió este envío: el pago fue detenido" | Ver en Stellar |
| Error de red | — | No llegó a la cadena | Neutral | Wifi tachado | "No llegó a Stellar. Nada cambió." | Reintentar |

**Sello "En Stellar" (Verificado).** No es un estado: es una marca Teal que se añade a cualquier estado que se leyó de la cadena o tiene una transacción. Lleva el texto "En Stellar", el ícono de enlace externo y abre el explorador. Si el dato viene de la app (Borrador) o la lectura falló, el sello no aparece.

**Tres distinciones que el sistema debe dejar claras:**

- **Retenido ≠ error.** El ámbar avanza (la línea de espera se llena), tiene hora de llegada y un nombre. El error es gris, no tiene hora y ofrece "Reintentar". Nunca se usa rojo ni un indicador de carga girando para un pago retenido.
- **Detenido = decisión de una persona.** El chip está relleno, muestra el avatar y el nombre de quien lo detuvo y la hora. No hay botón de "reintentar", porque no fue un fallo.
- **Rechazado = la regla actuó.** El contorno rojo sin relleno y la frase "el contrato no permitió" lo separan de Detenido. Lleva el sello En Stellar, porque el rechazo es evidencia.

## 9. Guardian Experience

**Idea central:** el modo guardián se reconoce en un segundo y solo tiene un verbo. Todo lo que el guardián no puede hacer simplemente no existe en su pantalla: no hay campo de monto, ni botón "Pagar", ni "Aprobar", ni ajustes.

**Recurso visual de la asimetría: la banda del guardián.** Una banda Navy fija arriba en todas sus pantallas, con texto White y detalles en Mint: "Modo guardián · Proteges a Laura". Debajo, en una línea permanente: "Puedes detener sus pagos retenidos. No puedes mover su dinero." Es la versión visual de "el guardián no puede gastar tu dinero, solo puede detenerlo". Y como el modo dueña nunca lleva banda Navy, nadie confunde en qué modo está.

### E0. Aviso (fuera de la app)

- **Objetivo:** que el guardián se entere. Sin esto, la espera no protege.
- **Contenido:** "Laura retuvo un pago de 150 USDC a una cuenta nueva. Se liberará a las 14:32. Puedes detenerlo hasta que ella lo envíe." + enlace al detalle.
- **Decisión visual:** texto plano, sin urgencia artificial (sin "¡URGENTE!" ni rojo). El aviso no contiene un botón de detener: la decisión se toma viendo los datos leídos de la cadena, no desde un correo que podría ser falso.
- **Límite honesto:** el aviso depende del backend. Si falla, la pantalla Guardián igual muestra los pagos retenidos leídos de la cadena.

### E. Guardián (lista)

- **Objetivo:** ver qué pagos de Laura están esperando.
- **Usuario:** guardián.
- **Información principal:** "Pagos de Laura en espera": cada uno con destino, monto, la línea de espera con el tiempo restante y la marca "Cuenta nueva" si no es un contacto.
- **Acción principal:** abrir un pago. No hay acción en la lista para no detener sin mirar.
- **Elementos secundarios:** "Ya detuviste" (los últimos 5).
- **Estado:** si no hay pagos esperando, una sola frase: "Ningún pago de Laura está esperando ahora".
- **Comportamiento:** se lee de la cadena (registro), no del backend.
- **Principio de seguridad comunicado:** el guardián ve lo mismo que la cadena, aunque el backend mienta.
- **Relación con MOVA:** lista corta de tarjetas grandes, una por elemento.
- **Decisión visual:** banda Navy arriba, tarjetas White con la línea de espera ámbar. Nada más.

### E2. Detalle del pago (modo guardián)

- **Objetivo:** decidir si detener.
- **Información principal, en este orden:** (1) tiempo restante y estado; (2) destino completo en Mono con "Cuenta nueva: Laura nunca le ha pagado" o "Contacto de confianza"; (3) monto; (4) cuándo lo creó Laura; (5) análisis de IA si existe, rotulado "Análisis de IA · puede equivocarse"; (6) "Ver en Stellar".
- **Acción principal:** "Detener este pago": botón de peligro con contorno, fijo abajo.
- **Lo que no aparece:** "Aprobar", "Liberar", "Adelantar", "Editar". 🟣 Tampoco un botón gris desactivado, porque sugeriría que existe ese poder.
- **Decisión visual:** los datos leídos de la cadena llevan el sello "En Stellar"; el análisis de IA va después y sin color, para que la decisión se apoye en los hechos.

### E3. Confirmación "Detener este pago"

- **Formato:** hoja inferior, la única confirmación de toda la app.
- **Texto:** "¿Detienes este pago de 150 USDC? Laura no podrá enviarlo. Si de verdad quiere pagar, tendrá que crear uno nuevo y esperar otra vez. Esto no se puede deshacer."
- **Botones:** "Detener el pago" (relleno Danger, texto White, 4,83:1) y "Volver" (secundario).
- **Resultado (E4):** el Detalle pasa a Detenido, con su avatar, la hora y el sello En Stellar de su transacción. Laura ve exactamente lo mismo.

### Qué pasa si el guardián intenta otra cosa

Desde la app no puede: no hay controles. Desde fuera (escena 5 de la demo), el intento falla en el contrato. La app no simula ese rechazo; se muestra en el explorador.

## 10. AI Experience

**La IA aparece como una lectura, no como un actor.** No tiene nombre, avatar, voz propia ni chat. Es una tarjeta que analiza un mensaje y se puede ignorar.

### Dónde aparece

| Lugar | Forma | Por qué ahí |
| --- | --- | --- |
| Pagar, plegada | Enlace: "¿Te pidieron este pago por mensaje? Pégalo y lo revisamos" | Aparece en el momento de presión, sin estorbar a quien paga a su hermana |
| Detalle del pago (dueña y guardián) | La misma tarjeta, debajo de los datos de la cadena | El guardián entiende el contexto, pero decide por los hechos |
| Ningún otro lugar | — | No hay "asistente" flotante ni burbuja de chat |

### La tarjeta de análisis (formato fijo)

- **Encabezado:** ícono de mensaje con líneas + "Análisis de IA · puede equivocarse".
- **Qué pide el mensaje:** una línea ("Pide transferir 150 USDC a una cuenta nueva hoy").
- **Señales que vemos:** hasta 3, en lista neutra ("Dice ser tu banco", "Pide urgencia", "Pide no contarle a nadie").
- **Sugerencia:** "Te sugerimos retener este pago" o "No vemos señales claras". Nunca "Es seguro" ni "Es una estafa".
- **Acción (solo si el pago iba a un contacto de confianza):** "Retener este pago", que cambia el carril a "Con espera". Lo confirma la firma de la dueña.

### Decisiones visuales

- **Sin color semántico.** La tarjeta usa Navy sobre White, con un borde izquierdo Navy al 20 %. Si la IA usara rojo o verde, parecería que su opinión es un estado del pago. El color es del contrato, no de la IA.
- **Sin destellos mágicos, robots ni porcentajes de confianza.** Todo eso comunica un poder y una precisión que no tiene.
- **Siempre debajo de los hechos** en el Detalle, nunca encima del estado.
- **La IA no completa el formulario.** El destino y el monto los escribe la dueña, para que una IA engañada por el mensaje no pueda cambiar a quién se paga.

### Límites que la interfaz hace evidentes

| La IA puede | Cómo se ve | La IA no puede | Cómo se evita que lo parezca |
| --- | --- | --- | --- |
| Analizar un mensaje y explicar señales | La tarjeta | Firmar, ejecutar, mover fondos | No hay botón de la IA que lleve a una firma; solo los botones de la dueña |
| Sugerir retener | Un botón secundario que la dueña confirma | Liberar o acortar una espera | La hora de liberación nunca aparece cerca de la tarjeta de IA |
| Ayudar a entender | Lenguaje simple | Agregar contactos, cambiar límites o reglas | Esas funciones no existen en la app |
| — | — | Ser guardián | El guardián siempre es una persona con nombre y foto |

**Frase visual:** la IA te ayuda a decidir; el contrato decide qué puede salir.

## 11. Security Proof

**Demostrar, no impresionar.** Son dos piezas plegables, en el mismo estilo: tarjeta White, borde izquierdo Teal de 3 px, título con el sello "En Stellar", filas "etiqueta en Sans → valor en Mono", botón de copiar y enlace al explorador por fila. No hay globos terráqueos, líneas de código cayendo ni animaciones de bloques.

### 1. "Ver en Stellar" (al final de cada Detalle)

Cada fila dice **qué prueba**, en gris debajo del valor. Sin eso, un hash no le dice nada al jurado.

| Fila | Valor (Mono, truncado al medio) | Qué prueba (texto gris) |
| --- | --- | --- |
| ID del pago | `#12` | "El registro existe en el contrato" |
| Estado | Retenido · leído a las 13:05 | "Lo dice el contrato, no la app" |
| Clave del pago | `7f3a…c91e` | "Une este registro a este destino y este monto exactos" |
| Hora de liberación | 2026-10-05 14:32:10 (ledger) | "La espera la fijó el contrato" |
| Contratos | Registro `CDX4…` · GuardianHold `CB7Q…` | "Qué código aplica la regla" |
| Transacciones | Retención `a91c…` · Detención `44be…` · Envío `—` | "Quién hizo qué y cuándo" |
| Intentos rechazados | Solo los que hizo esta app, con su código de error | "La regla actuó" |

Los valores de esta tabla son ilustrativos.

### 2. "Reglas de esta cuenta" (plegada en Inicio)

Es la prueba más fuerte, porque muestra una **propiedad** y no solo un hecho:

- "Esta cuenta tiene **1 regla**" (leído de la cadena).
- "Firmante: tu passkey" · "Política: GuardianHold".
- "Sin funciones para cambiar reglas, actualizar el código ni ejecutar llamadas arbitrarias."
- "Tu guardián: Diego (`GBX…`). No es firmante de esta cuenta."
- Enlaces: "Ver el código" y "Ver los tests" (repositorio público).

### Reglas de honestidad

- **El sello "En Stellar" solo aparece cuando el dato se leyó de la cadena.** Si la lectura falla, el sello desaparece y aparece "sin actualizar desde las 13:05".
- **La palabra "Verificado" no aparece en la app.** Se usa "En Stellar", que dice dónde está la evidencia sin prometer qué se verificó.
- **Límite técnico:** una transacción fallida no deja eventos del contrato, así que la app no puede listar los rechazos que hizo otra persona (por ejemplo, desde la terminal en la demo) sin un indexador. 🔴 Esos rechazos se muestran en el explorador, y la app no finge mostrarlos.

## 12. Hackathon Demo Flow

**Montaje recomendado:** dos teléfonos reales, o en una pantalla grande el modo "lado a lado" (NICE): Laura a la izquierda con fondo Ice y Diego a la derecha con su banda Navy. Así el jurado distingue los dos roles por el color, sin explicación. Para las escenas 4 a 6 se usa una terminal visible con el explorador al lado. La retención de la cuenta de demo dura 2 minutos.

| Escena | Lo que hace | Pantalla | Lo que el jurado ve | Prueba |
| --- | --- | --- | --- | --- |
| 1. Pago confiable | Laura paga 10 USDC a Diego | Pagar → Revisar → Detalle | Tarjeta verde "Sale ahora", chip Enviado en segundos | Sello En Stellar → transacción |
| 2. Pago sospechoso | Pega el mensaje del falso banco; la IA marca señales; paga 150 a una cuenta nueva | Pagar (tarjeta ámbar) → Revisar ("Firmar y retener") → Detalle | La línea de espera arranca con "Diego puede detenerlo"; el botón de enviar está inactivo hasta las 14:32 | Transacción de retención |
| 3. Guardián | Diego recibe el aviso y abre el pago | Guardián → Detalle (modo guardián) → confirmación | La banda Navy, el destino "Cuenta nueva" y un solo botón: Detener | En ambos teléfonos aparece el chip rojo relleno "Detenido por Diego" |
| 4. Intento de bypass | Desde la terminal se intenta enviar el pago detenido | Explorador (y el Detalle sigue en Detenido) | Transacción fallida con el error del contrato | Explorador |
| 5. El guardián intenta gastar | Diego intenta transferirse fondos de Laura | Explorador | Rechazo | Explorador |
| 6. Otra regla | Se intenta agregar una regla sin política | Inicio → "Reglas de esta cuenta" | "1 regla" antes y después del intento | Explorador + panel de reglas |

**Qué hace que se entienda sin hablar mucho:** tres colores con tres significados (verde sale, ámbar espera, rojo detenido), una persona con nombre en el tramo ámbar y un sello Teal que siempre lleva a la cadena.

**Láminas de apoyo:** se reutiliza la gramática de MOVA (título + tres bloques numerados + una pantalla grande legible), con una pantalla por lámina, no la misma repetida.

## 13. Responsive Strategy

**Se diseña a 360 px y se escala hacia arriba.** La app nunca se convierte en un tablero con columnas de métricas.

| Ancho | Layout | Cambios |
| --- | --- | --- |
| 320–389 px | Una columna, margen de 16 | Botón principal fijo abajo; hojas inferiores a ancho completo; hashes truncados al medio |
| 390–767 px | Una columna, margen de 20 | Igual; el monto puede crecer a 48 px |
| 768–1023 px | Columna centrada de 480 px sobre Ice | El botón deja de estar fijo y va al final del contenido; las hojas inferiores pasan a diálogos centrados de 480 px |
| ≥ 1024 px | Columna de 480 + panel lateral de 360 | El panel lateral muestra "Ver en Stellar" desplegado al abrir un Detalle, para que la prueba esté a la vista en la demo |
| Demo ≥ 1280 px (NICE) | Dos columnas de 480: dueña \| guardián | Cada columna es la app móvil real, no una versión de escritorio |

**Reglas:**

- Ningún contenido nuevo aparece en escritorio que no exista en móvil; solo cambia la disposición.
- La línea de espera mantiene su forma en todos los anchos: solo se estira.
- No hay scroll horizontal. Las direcciones largas se truncan al medio, con "ver completa" y copiar.

## 14. Accessibility

Objetivo: WCAG 2.2 nivel AA.

| Tema | Decisión |
| --- | --- |
| Contraste de texto (4,5:1) | Navy para el texto; tonos derivados para el texto Teal, ámbar y verde (sección 5). Nunca texto blanco sobre Teal (2,93:1). |
| Contraste de componentes (3:1) | Bordes de inputs con Navy al 50 % (3,5:1). Las tarjetas no dependen de su borde para identificarse. |
| Estado sin depender del color | Cada estado lleva ícono, palabra y una forma propia: Detenido relleno, Rechazado con contorno, Retenido con barra que avanza. Antes de la demo hay que comprobarlo con la interfaz en escala de grises. |
| Cuenta regresiva | Visual: "Faltan 1 min 20 s". Para lectores de pantalla: la hora fija ("se libera a las 14:32"). Solo se anuncia el cambio de estado (`aria-live` cortés), nunca cada segundo. |
| Movimiento | La línea de espera avanza sin animación continua: se actualiza cada 15 s. Con "reducir movimiento" activado, no hay transiciones. |
| Áreas táctiles | Mínimo 44 × 44 px (WCAG exige 24; se usa 44 por ser una app de dinero). Botones de 52 de alto. |
| Foco | Anillo Navy de 2 px con separación de 2 px; dentro de la banda del guardián, anillo Mint (12,7:1 sobre Navy). |
| Datos técnicos | Hashes y direcciones truncados al medio, con "ver completa" y "copiar" etiquetados. Los lectores de pantalla leen "dirección que termina en C91E", no 56 caracteres. |
| Lenguaje | Español simple, segunda persona, frases de menos de 20 palabras, sin jerga cripto ("firmar" sí; "autorizar el intent" no). |
| Passkey | Antes del diálogo del sistema, una línea dice qué se va a firmar, porque el diálogo de la passkey no lo muestra. |
| Tiempo | La espera es una regla del producto, no un límite de la interfaz: la dueña puede salir y volver sin perder nada. |
| Tamaño de texto | Base de 16 px; admite zoom de 200 % sin perder contenido (una columna). |

## 15. DO NOT BUILD

| No construir | Por qué |
| --- | --- |
| Escudos, candados gigantes, "100 % seguro", sellos de "protegido" | Seguridad de apariencia; además, es el lenguaje de los antivirus |
| Puntaje o velocímetro de riesgo | Precisión que la IA no tiene, y parece una decisión automática |
| Avatar, nombre, chat o destellos para la IA | Le da presencia y poder de actor |
| Botones "Aprobar" o "Liberar" para el guardián, ni siquiera desactivados | Sugieren un poder que no existe |
| Selector de duración de la espera | La duración la fija el contrato |
| Gráficos de saldo, métricas, tablero | No es un dashboard financiero |
| Barra de pestañas, perfil, ajustes, edición de contactos o guardián | Fuera del MVP; algunas serían rutas de ataque |
| Gradientes cripto, neón, modo oscuro "hacker" | Comunica casino o herramienta hacker |
| Confeti o celebraciones al enviar | Celebrar un pago es justo lo que un estafador querría |
| Rojo o un indicador girando para un pago retenido | Lo haría parecer un error |
| Teal en botones o fondos grandes | Diluye su significado ("En Stellar") |
| La palabra "Verificado" o "Seguro" en la interfaz | Promete más de lo que se comprueba |
| SINPE, fiat, KYC, custodia, mainnet, multichain, token, NFT, staking, voz, app nativa, notificaciones completas | Fuera del concepto (brief, sección 20) |

## 16. Final Visual Proposal

### La propuesta, en una ficha

- **Dirección:** Calma verificable.
- **Lienzo:** Ice; tarjetas White; texto y acción en Navy.
- **Color con significado:** verde = salió; ámbar = espera; rojo relleno = alguien lo detuvo; rojo con contorno = el contrato lo negó; Teal = está en Stellar y es la marca. Nada más lleva color.
- **Tipografía:** IBM Plex Sans para la interfaz y IBM Plex Mono solo para lo verificable.
- **Elemento distintivo:** la línea de espera, con el guardián sobre el tramo ámbar.
- **Modo guardián:** banda Navy con Mint, un solo verbo ("Detener") y una sola confirmación en toda la app.
- **IA:** una tarjeta neutra, rotulada "puede equivocarse", siempre debajo de los hechos.
- **Prueba:** "Ver en Stellar" en cada pago y "Reglas de esta cuenta" en Inicio; cada fila dice qué prueba.
- **Estructura:** Entrada + 4 pantallas, sin pestañas, con el ritmo Preparar → Esperar → Resultado tomado de MOVA.

**En una frase visual:** así se ve una app de pagos donde tú firmas, la IA te ayuda a leer el riesgo, una persona con nombre puede frenar un pago mientras una línea ámbar avanza, y un sello Teal te lleva a la regla en Stellar que lo hace cumplir.

### Auditoría adversarial

| Pregunta | Veredicto | Por qué / corrección aplicada |
| --- | --- | --- |
| ¿Podría pertenecer a cualquier wallet? | ⚠️ Riesgo en Pagar | Saldo + botón Pagar es universal. **Corrección:** la tarjeta del guardián y la línea de estado "Reglas activas en Stellar" van fijas en Inicio, y la tarjeta de carril aparece antes de firmar. |
| ¿Comunica propiedades reales o apariencia? | ✔ | Sin "seguro" ni "verificado"; cada afirmación tiene sello o enlace; el sello desaparece si no hay lectura de la cadena. |
| ¿El guardián puede detener pero no gastar? | ✔ | No hay controles de gasto en su modo y la banda lo dice siempre. |
| ¿La IA controla fondos? | ✔ | Tarjeta neutra, sin acceso a la firma ni al formulario. |
| ¿Los estados se entienden? | ✔ con una salvedad | "Listo para enviar" puede leerse como "ya salió". **Corrección:** siempre va con "Necesita tu firma para salir". |
| ¿Se puede demostrar la infraestructura on-chain? | ⚠️ Parcial | Reglas de la cuenta + explorador sí; los rechazos hechos por otros no se listan en la app (sección 11). Se dice en voz alta. |
| ¿Hay elementos innecesarios? | ✔ | Contactos con montos rápidos y modo lado a lado son NICE; el resto es MUST. |
| ¿MOVA sin copiar? | ✔ | Se tomó la gramática y el ritmo; no el color, el contenido ni la navegación. |
| ¿Se respetó Guard Teal? | ✔ | Solo los colores oficiales más tonos más oscuros del mismo color para texto. Teal reservado a marca y evidencia. |
| ¿El jurado lo entiende rápido? | 🔴 No probado | Debe probarse con 3 personas ajenas al equipo, mostrando el Detalle en Retenido 5 segundos y preguntando "¿qué está pasando con este pago?". |

### Kill test visual

| # | Pregunta | Resultado |
| --- | --- | --- |
| 1 | ¿Parece una wallet genérica? | No en Inicio ni en Detalle (guardián y línea de espera visibles). En Pagar, solo hasta que aparece la tarjeta de carril. |
| 2 | ¿Parece una app bancaria? | No: no tiene menús de productos, tarjetas de crédito ni logos bancarios. |
| 3 | ¿Parece una herramienta de ciberseguridad? | No: sin escudos, alertas rojas ni lenguaje de amenazas. |
| 4 | ¿El guardián aporta algo que una multisig no explique? | Sí: no hay "aprobar 1 de 2"; los pagos a contactos no lo esperan y el guardián solo tiene "Detener". |
| 5 | ¿La IA parece tener demasiado poder? | No: sin nombre ni color y siempre debajo de los hechos. |
| 6 | ¿El usuario entiende qué pasa con su dinero? | Sí: cada estado es una frase con hora y nombre. |
| 7 | ¿HELD se distingue de ERROR? | Sí: ámbar que avanza con hora y nombre, frente a gris con "Reintentar". |
| 8 | ¿CANCELLED comunica una acción deliberada? | Sí: chip relleno con avatar, nombre y hora, sin "reintentar". |
| 9 | ¿La seguridad puede demostrarse? | Sí para la configuración y los hechos de la cadena; parcialmente para rechazos ajenos. |
| 10 | ¿Demasiadas funciones? | No: 4 pantallas, una confirmación, sin ajustes. |
| 11 | ¿La paleta es consistente? | Sí. El riesgo inverso es que Teal se vea poco; se compensa con la marca en Entrada e Inicio y con el sello en cada pago. |
| 12 | ¿Se presenta sin explicación extensa? | Probablemente, por los tres colores y el guardián con nombre. 🔴 Hay que confirmarlo con la prueba de 5 segundos antes de la demo. |

### Fuentes

- [Financial Ombudsman Service — decisión DRN-5886851 (intervención de Revolut)](https://www.financial-ombudsman.org.uk/decision/DRN-5886851.pdf)
- [Monzo — Added Security](https://monzo.com/help/Account%20Security/addedsecurityfaqs)
- [Yandex Pay — verificación por contacto de confianza](https://zamin.uz/en/technology/204838-yandex-pay-launches-verification-feature-for-large-transfers-via-trusted-contact.html)
- [Wise — estado de la transferencia](https://wise.com/help/articles/2452305/how-do-i-check-my-transfers-status)
- [Communications of the ACM — Behind the Bybit crypto theft](https://cacm.acm.org/news/behind-the-bybit-crypto-theft)
- [Safe — verificaciones básicas antes de firmar](https://help.safe.global/en/articles/276343-how-to-perform-basic-transactions-checks-on-safe-wallet)
- [Safe Foundation — Moving trust beyond the UI](https://forum.safefoundation.org/t/discussion-the-future-of-self-custody-wallets-moving-trust-beyond-the-ui/6651)
- [GitHub Primer — Confirmation dialog](https://primer.style/product/components/confirmation-dialog/guidelines)
- [W3C — WCAG 2.2, contraste no textual](https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html)
- [Fontsource — IBM Plex Sans](https://fontsource.org/fonts/ibm-plex-sans/about)
- [Stellar Community Fund — Passkey UI](https://communityfund.stellar.org/project/passkey-ui-sj6)
