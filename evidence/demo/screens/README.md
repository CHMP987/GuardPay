# Pantallas a 360 dp (P5, día 4)

| | |
| --- | --- |
| Etiqueta | **SIMULATED**: la UI real de `commonMain` dibujada contra `FakeStellarGateway`, el modelo del contrato en memoria. Nada de esto está en Stellar, y cada pantalla del dueño lo dice en el banner "Simulación: nada de esto está en Stellar". |
| Fecha | 2026-10-06 |
| Generador | `app/shared/src/jvmTest/kotlin/com/guardpay/shared/ui/ScreensAt360Test.kt` |
| Comando | `./gradlew :app:shared:jvmTest` (escribe aquí las PNG vía la propiedad `gp.screens`) |
| Lienzo | 720 × 1600 px a densidad 2 = **360 × 800 dp**, Skia sin dispositivo (Compose Multiplatform 1.9.1) |
| Reloj | El ledger del fake arranca en `1 000 000` s (13:46 UTC); la zona horaria es UTC. Las horas son las del ledger simulado, no la hora real. |

## Las 13 capturas

| Archivo | Estado o pantalla | Cómo se llega |
| --- | --- | --- |
| `00-entrada.png` | Entrada (elegir rol) | sesión nueva |
| `01-borrador.png` | **Borrador**: Pagar con "Con espera" | 150 USDC a una cuenta que no es contacto |
| `02-firmando.png` | **Firmando…**: "Esperando tu huella o PIN…" | un firmante que nunca responde |
| `03-retenido.png` | **Retenido** (Detalle) | la dueña firma; el registro crea la retención |
| `04-listo-para-enviar.png` | **Listo para enviar** | el ledger avanza 120 s |
| `05-enviado.png` | **Enviado** | la dueña libera tras la madurez |
| `06-detenido.png` | **Detenido** por Diego | el guardián cancela en el registro |
| `07-vencido.png` | **Vencido** | el ledger pasa 120 + 600 s sin liberar |
| `08-rechazado.png` | **Rechazado por el contrato** | saldo 10, pago de 20 a un contacto (código 5) |
| `09-error-de-red.png` | **Error de red** | la red cae al liberar |
| `10-inicio.png` | Inicio con un pago retenido | |
| `11-guardian-lista.png` | Guardián: lista | |
| `12-guardian-detener.png` | Guardián: hoja "¿Detienes este pago?" | |

Están los 9 estados de pago: Borrador, Firmando, Retenido, Listo para enviar, Enviado, Detenido, Vencido, Rechazado y Error de red.

## Qué afirma el test en cada captura

- [FACT] Algún nodo de semántica muestra el texto del estado (por ejemplo "Retenido", "Detenido por Diego", "Rechazado por el contrato").
- [FACT] Ningún nodo queda fuera del ancho de 360 dp (`boundsInRoot` dentro de 0…720 px).
- [FACT] Ningún texto visible, `contentDescription` ni texto editable usa, como palabra completa, una de las tres palabras que el plan prohíbe en la interfaz. Es la versión en ejecución de `checkUiVocabulary`, que solo mira el código fuente.

## Accesibilidad: qué está cubierto y qué no

- [FACT] Contraste: `ContrastTest` recalcula la tabla de la sección 5 de la Propuesta visual. Todo color de texto pasa 4,5:1 sobre Blanco e Ice, el borde de los campos pasa 3:1, y el blanco sobre Teal (2,93) está prohibido.
- [FACT] El estado nunca va solo por color: cada chip lleva texto y un ícono.
- No corrido: TalkBack en un teléfono real, escala de fuente al 200 %, tamaño mínimo de los objetivos táctiles y el modo oscuro.
- No corrido: la app no se ha abierto en el Galaxy A54 con esta UI. El APK de debug compila (`assembleDebug`), pero no hay un dispositivo conectado. (Se abrió después, el 7 oct UTC: `evidence/demo/physical-phone.md`.)
- [INFERENCE] Skia en JVM dibuja con las mismas fuentes IBM Plex que Android, pero el rasterizado y las métricas pueden diferir unos píxeles de un teléfono.
