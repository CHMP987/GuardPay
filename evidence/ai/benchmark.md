# Spike B — benchmark de Gemma

12 oct 2026. **REAL**: medido en un teléfono físico. Los mensajes son **SIMULATED**.

`[FACT]` En GPU se cumple el resultado esperado del plan: JSON 10/10, primer token < 3 s en 10/10 y ningún OOM.

`[INFERENCE]` Cada análisis completo tarda de 6 a 9 s, por encima de la hipótesis de 4 a 7 s. Por eso se clasifica como **verde con la salvaguarda del amarillo**: la IA es MUST en Android, con streaming y el aviso "analizando…".

`[FACT]` En la repetición de la tarde, GPU **no** repitió ese resultado: un primer token tardó 129 s, por encima del umbral de fallo de 10 s. Ver [Repetición](#repetición-12-oct-1600).

P7 se abrió el mismo día. Lo medido dentro de la app está en [P7 en la app](#p7-en-la-app-12-oct). CP-7 sigue sin firmar.

## Test

Spike B: Gemma 4 E2B por la API Kotlin oficial de LiteRT-LM, en un teléfono físico. Se pasan diez mensajes y se miden disco, RAM, carga, latencia, tokens/s, temperatura y batería.

- Harness: [`spikes/ai-bench/`](../../spikes/ai-bench/). Es un build Gradle aparte, fuera del MVP. Ver su README.
- Modelo: `gemma-4-E2B-it.litertlm`, de HF `litert-community`, con **2 588 147 712 bytes** (2.59 GB) en disco. No está en el repo.
- Runtime: `com.google.ai.edge.litertlm:litertlm-android:0.17.1`.
- Teléfono:
  - Teléfono físico con Android 14.
  - Exynos 1380 (s5e8835) con GPU Mali-G68, arm64-v8a.
  - RAM: `MemTotal` 7 616 236 kB y `MemAvailable` de 2.86 a 3.05 GB antes de cada corrida.
- Configuración de cada análisis:
  - conversación nueva;
  - el system prompt de `Messages.kt`;
  - `topK=1`, `seed=0`;
  - `maxOutputToken=256`;
  - razonamiento (thinking) apagado.
- Orden de la prueba:
  1. carga en frío, con `cacheDir` vacío;
  2. cierre del motor;
  3. carga en caliente;
  4. 10 análisis en primer plano (fg);
  5. HOME;
  6. 3 análisis en segundo plano (bg).
- Backends: CPU (10:44–10:47) y GPU (10:48–10:51), hora del teléfono.

## Expected

Lo que fija el plan, desde la línea 412:

- **Resultado esperado:** JSON parseable en al menos 8 de 10, primer token en menos de 3 s y ningún OOM en 10 análisis seguidos.
- **Fallo:** un OOM, un primer token por encima de 10 s o JSON parseable en menos de 5 de 10.
- **Hipótesis `[INFERENCE]` previa:** 12–20 tok/s, unos 80 tokens y 4–7 s por análisis.

## Actual

| Métrica | CPU | GPU |
|---|---|---|
| Carga en frío (`cacheDir` vacío) | 8 324 ms | 47 400 ms |
| Carga en caliente (mismo proceso) | 441 ms | 4 625 ms |
| TTFT nativo, 10 fg | 2.10–3.80 s; **9/10 < 3 s** | 1.13–2.25 s; **10/10 < 3 s** |
| Primer fragmento (reloj de pared), 10 fg | 2 188–3 929 ms; 8/10 < 3 s | 1 246–3 085 ms; 9/10 < 3 s |
| Análisis completo, 10 fg | 8 550–11 596 ms | 6 318–9 099 ms |
| Decodificación | 8.58–10.56 tok/s (8 de 10 entre 8.77 y 8.92) | 8.67–9.88 tok/s (9 de 10 en 9.7–9.9) |
| Prefill | 75–129 tok/s | 115–240 tok/s |
| Tokens (prefill / salida) | 240–276 / 55–76 | 240–276 / 50–70 |
| JSON parseable con la regla de `AnalysisParser` | **10/10** | **10/10** |
| Sugerencia | 10/10 `SUGGEST_HOLD` | 10/10 `SUGGEST_HOLD` |
| OOM o muerte del proceso | ninguno | ninguno |
| VmHWM (pico de RSS) tras las cargas | 1 411 984 kB | 2 180 764 kB |
| VmHWM al terminar | 2 292 548 kB | 2 468 560 kB |
| `MemAvailable` más bajo | 2 657 244 kB | 1 896 500 kB |
| Temperatura de batería (antes → tras 10 → final) | 34.3 → 34.7 → 35.0 °C | 35.1 → 35.4 → 35.5 °C |
| `thermalStatus` | 1 todo el tiempo | 1, y 2 al final |
| Batería | **no medida** (ver más abajo) | **no medida** |
| 3 análisis en segundo plano | todos parseables. TTFT 9.4–9.8 s, decode 3.5–6.3 tok/s, total 18.0–28.7 s | todos parseables. bg 1: TTFT 10.66 s, total 19.5 s. bg 2 y 3: TTFT 1.46–1.51 s, total 7.4–9.7 s |

Datos crudos: [`bench/bench-cpu-1791823473210.jsonl`](bench/bench-cpu-1791823473210.jsonl) y [`bench/bench-gpu-1791823682422.jsonl`](bench/bench-gpu-1791823682422.jsonl). Cada análisis guarda la salida completa (`raw`).

Salvedades:

- **Batería:** el teléfono estuvo conectado por USB (`plugged=2`) durante todo el spike. El porcentaje subió de 72 a 73 % y `chargeCounter` no bajó, así que el consumo **no se pudo medir**. Para medirlo hay que correrlo sin cable, con adb por Wi-Fi: no corrido.
- **RAM pico:** se tomó como `VmHWM` de `/proc/self/status`, no con Android Studio Profiler. Incluye el modelo mapeado en memoria, así que no es lo mismo que la memoria que el sistema puede reclamar.
- **"Frío":** el `cacheDir` de LiteRT-LM estaba vacío, pero la caché de páginas del sistema no se vació porque eso requiere root. No es un arranque en frío del teléfono. En GPU la carga en frío incluye compilar los kernels (47 s), y esa compilación queda en `cacheDir`.
- **Sin control negativo:** los 10 mensajes son estafas. No se probó que un mensaje legítimo reciba `NO_CLEAR_SIGNALS`, así que la tasa de falsos positivos no se midió: no corrido.
- **Calidad de las señales:** no se evaluó. Solo se verificó la forma del JSON, el umbral de 200 caracteres y la ausencia de seguro/protegido. Un ejemplo de imprecisión: en el mensaje 10 por GPU, `whatItAsks` habla de "una cuenta de un amigo" cuando el mensaje es del nieto.
- **Segundo plano:** en CPU el primer plano se perdió y los análisis tardaron 2–3 veces más. En el bg 3 de CPU, `foregroundAtEnd=true`: la app volvió a primer plano por una causa no identificada. En GPU solo el primer análisis en segundo plano fue lento. `[INFERENCE]` El MVP analiza con la app abierta, así que el caso que importa es el de primer plano.
- **GPU:** logcat avisa que falta `libLiteRtTopKOpenClSampler.so`. El muestreo cae al sampler estático, y no se probó si eso limita la velocidad de decodificación.
- **Muestra:** son 10 análisis por backend en un solo teléfono y en un solo día. No se puede extrapolar a otros teléfonos.

## Contract

Ninguno. La IA no llama contratos.

## Transaction

no hay tx

## Hash

no hay tx

## Timestamp

2026-10-12, de 10:44 a 10:51 (hora del teléfono). La marca `t` de cada línea JSON está en ms epoch.

## Input

Diez mensajes **SIMULATED**, escritos a partir de los patrones documentados en `docs/GuardPay — Validación Costa Rica y Chile.md`:

1. familiar con número nuevo;
2. falso BN con "cuenta de resguardo";
3. comprobante falso;
4. adelanto por una PlayStation;
5. WhatsApp secuestrado;
6. premio con "impuestos";
7. falso OIJ con fianza;
8. inversión en USDC;
9. alquiler desde el extranjero;
10. nieto en la frontera.

Los textos están en [`spikes/ai-bench/src/main/kotlin/com/guardpay/aibench/Messages.kt`](../../spikes/ai-bench/src/main/kotlin/com/guardpay/aibench/Messages.kt). Ningún mensaje es de una víctima real.

## Output

Mensaje 1 por CPU, tal como salió del modelo:

```json
{"whatItAsks": "Solicitar un SINPE de ₡85.000 de forma urgente.", "signals": ["Solicitud de dinero urgente", "Revelación de número nuevo", "Petición de secreto"], "suggestion": "SUGGEST_HOLD"}
```

Las otras 25 salidas están en los `.jsonl`.

## Captura o log

Los registros completos son los `.jsonl` de [`bench/`](bench/). También se emitieron por logcat con el tag `AIBENCH`. No hay video.

## Conclusión

- `[FACT]` En GPU, el teléfono cumple los tres umbrales del resultado esperado: JSON 10/10, TTFT < 3 s en 10/10 y ningún OOM. En CPU, el TTFT queda bajo 3 s en 9 de 10 (el mensaje 8 tardó 3.80 s).
- `[FACT]` La decodificación (~9–10 tok/s) y el análisis completo (6–9 s en GPU) quedaron por debajo de la hipótesis previa (12–20 tok/s; 4–7 s).
- `[INFERENCE]` El spike queda en verde, con el riesgo de latencia del amarillo.
- `[RECOMMENDATION]` Si se abre P7:
  - usar GPU, con CPU como fallback;
  - mostrar el resultado en streaming junto al aviso "analizando…";
  - cargar el motor una sola vez por sesión: 4.6 s en caliente, y 47 s la primera vez en GPU;
  - nunca bloquear el pago esperando a la IA.
- `[RECOMMENDATION]` Antes de afirmar algo sobre la calidad del análisis:
  - correr un control negativo con mensajes legítimos;
  - medir la batería sin cable.
- Nada de esto prueba que la app detecte estafas reales. Solo prueba que el modelo corre en este teléfono con ese formato de salida.

P7: ver más abajo. CP-7 no está firmado.

## Repetición (12 oct, 16:00)

**REAL:** el mismo harness, modelo, teléfono, mensajes y orden de la prueba que arriba. Los mensajes son **SIMULATED**. Esta vez GPU corrió primero (16:00–16:07) y CPU después (16:07–16:11), hora del teléfono.

Antes de correrla se restauró el modelo: se bajó en el PC de la URL fijada en `GemmaModelFile`, se comprobaron el tamaño y el SHA-256 y se copió con `adb push`. En el teléfono, el SHA-256 coincidió con el fijado.

| Métrica | CPU | GPU |
|---|---|---|
| Carga en frío / en caliente | 6 143 / 424 ms | 102 393 / 9 868 ms |
| TTFT nativo, 10 fg | 2.37–4.10 s; **9/10 < 3 s** (el mensaje 8 tardó 4.10 s) | 1.31–129.49 s; **7/10 < 3 s**. Mensaje 1: 3.03 s; mensaje 3: 5.76 s; **mensaje 8: 129.49 s** |
| Primer fragmento (reloj de pared), 10 fg | 2 516–4 232 ms | 1 426–129 816 ms |
| Análisis completo, 10 fg | 8 582–12 027 ms | 6 642–15 058 ms, y 136 988 ms en el mensaje 8 |
| Decodificación | 8.35–9.76 tok/s | 5.32–9.77 tok/s |
| Prefill | 69–113 tok/s | 46–204 tok/s; 2 tok/s en el mensaje 8 |
| JSON parseable / sugerencia | **10/10** / 10/10 `SUGGEST_HOLD` | **10/10** / 10/10 `SUGGEST_HOLD` |
| OOM o muerte del proceso | ninguno | ninguno |
| VmHWM tras las cargas / al terminar | 1 381 444 / 2 276 280 kB | 2 147 916 / 2 333 048 kB |
| `MemAvailable` más bajo | 2 976 860 kB | 1 897 328 kB |
| Temperatura de batería (antes → tras 10 → final) | 36.5 → 36.7 → 37.2 °C | 35.8 → 36.3 → 36.5 °C |
| `thermalStatus` | 2 todo el tiempo | 1, y 2 desde el final de los 10 fg |
| Batería | **no medida** | **no medida** |
| 3 análisis en segundo plano | todos parseables. TTFT 10.13–15.31 s, decode 2.86–3.56 tok/s, total 26.7–34.3 s | todos parseables. TTFT 1.28–1.35 s, total 6.7–8.8 s |

Datos crudos: [`bench/bench-gpu-1791842452753.jsonl`](bench/bench-gpu-1791842452753.jsonl) y [`bench/bench-cpu-1791842839888.jsonl`](bench/bench-cpu-1791842839888.jsonl).

Salvedades:

- **Primer plano en GPU:** en los mensajes 1 a 7 por GPU, `foregroundAtEnd=false`, aunque eran análisis "en primer plano". Pasa a `true` justo en el mensaje 8, el de 129 s. `[INFERENCE]` La pantalla del teléfono se apagó o se bloqueó durante la prueba, y el mensaje 8 coincide con esa transición. `[UNVERIFIED]` No se registró el estado de la pantalla, así que la causa no está comprobada. En CPU, los 10 fg tienen `foregroundAtEnd=true`.
- **Carga en frío en GPU:** 102 s, contra 47 s en la mañana. No se investigó por qué.
- **Batería:** el teléfono volvió a estar conectado por USB (`plugged=2`) y cargando (63 → 65 %). El consumo no se midió: no corrido.
- **Térmica:** CPU corrió justo después de GPU, ya con `thermalStatus` 2.
- Valen las demás salvedades de arriba: no hay control negativo, la calidad de las señales no se evaluó y es un solo teléfono.

Conclusión de la repetición:

- `[FACT]` JSON 10/10 y ningún OOM en los dos backends, igual que en la mañana.
- `[FACT]` CPU repitió el resultado de la mañana: TTFT < 3 s en 9/10.
- `[FACT]` GPU no lo repitió: TTFT < 3 s en 7/10, y un TTFT de 129 s cae en el criterio de **fallo** del plan (primer token > 10 s).
- `[INFERENCE]` Con la causa probable (pantalla apagada o bloqueada), el riesgo para la demo es que la IA se atrase si el teléfono se bloquea con Pagar abierto. Pagar nunca espera a la IA, así que el pago no se bloquea.
- `[RECOMMENDATION]` Para cerrar la duda, repetir GPU con la pantalla forzada encendida (`svc power stayon usb`) y registrar el estado de la pantalla en cada análisis. No corrido.

## P7 en la app (12 oct)

**REAL:** en el teléfono, con la app de debug y la clase de la app `AndroidGuardPayAI`, no el harness del spike. Los mensajes son **SIMULATED**. El prompt, el sampler y el modelo son los del spike.

### Lecturas (`GemmaOnDeviceTest`)

Son 18 mensajes: las 10 estafas del spike, las 5 inyecciones de [prompt-injection.md](prompt-injection.md) y 3 mensajes legítimos de control. Hubo dos corridas, a las 11:19 y a las 11:22 (hora del teléfono). Las salidas fueron idénticas, porque con `topK=1` el resultado es determinista.

| Métrica | Valor |
|---|---|
| Backend | GPU en 18/18, en las dos corridas |
| JSON válido para el parser, estafas | **10/10** (umbral de la prueba: ≥ 8) |
| `SUGGEST_HOLD`, estafas | 10/10 |
| `SUGGEST_HOLD`, inyecciones | 4/5 (ver prompt-injection.md) |
| Controles legítimos | 2/3 `NO_CLEAR_SIGNALS`; 1/3 `SUGGEST_HOLD` (pizza, 20 USDC: **falso positivo**) |
| Palabras prohibidas en la salida mostrada | 0/18 |
| `analyzeMessage`, motor ya cargado (17 lecturas por corrida) | 6 224–9 684 ms; **17/17 < 10 s** en las dos corridas |
| Primera lectura de cada proceso (incluye cargar el motor) | 64 146 ms (caché vacía) y 49 348 ms (caché llena) |

`[FACT]` La caché de LiteRT-LM (`cacheDir/litertlm`, 783 MB entre el programa y los pesos) no evita la carga lenta en un proceso nuevo. Logcat muestra unos 31 s en "Weights preparation on Gpu is disabled for … Mali GPUs". `[INFERENCE]` En este teléfono, cada vez que se abre Pagar en un proceso nuevo, el motor tarda unos 40 s en cargarse en segundo plano.

### En la pantalla Pagar (uiautomator)

Medido con los scripts [`p7/p7ui.py`](p7/p7ui.py) y [`p7/p7video.py`](p7/p7video.py). El mensaje es la estafa 1 sin tildes, porque `adb input text` no las escribe.

- **Descarga dentro de la app:**
  - Se dejó un `.part` de 2 577 399 808 bytes, a unos 10 MB del final.
  - Al tocar "Descargar el modelo" se vio "Descargando el modelo… 99 %".
  - La app pidió el resto con `Range` a la URL fijada de Hugging Face, comprobó el tamaño y el SHA-256, y dejó el archivo en su lugar.
  - Tardó 16,2 s y el aviso desapareció.
  - `[FACT]` No se probó una descarga completa de 2,6 GB desde cero en la app (no corrido). Se probaron la reanudación, la redirección, la verificación y el renombre.
- **Tiempo de lectura en pantalla, con el motor ya cargado:**
  - Se esperó 55 s en Pagar y se grabó la pantalla.
  - Se midió del cuadro en que el botón pasa a "Analizando…" al cuadro en que vuelve a "Revisar el mensaje", con la tarjeta debajo.
  - Resultados: **7,52 s, 8,20 s y 7,55 s**. Criterio de aceptación: < 10 s.
- **Tocar pronto:** si se toca unos 13 s después de abrir Pagar, el motor aún está cargando. Desde el toque hasta la tarjeta fueron 16,4 s, medido con `uiautomator dump`, que suma hasta unos 3,3 s por sondeo.
- **Tarjeta:** "Solicitar un SINPE de 85000 de forma urgente." y las señales "Pedido de dinero", "Urgencia" y "Solicitud de secreto". Debajo dice "Te sugerimos retener este pago". En las 5 lecturas de pantalla no apareció ninguna palabra prohibida.
- **Liberación:** con el motor cargado, el RSS del proceso era de 2,03–2,40 GB. Al salir de Pagar bajó a 0,29–0,36 GB en las 7 veces que se midió.
- **Streaming:** la respuesta se recibe por partes (`sendMessageAsync`). Mientras tanto se ve "Analizando…" en el botón, y en pantalla solo aparece el resultado ya parseado. `[INFERENCE]` Mostrar el JSON a medias sería mostrar texto crudo del modelo, y P7 lo prohíbe.

Archivos:

- Capturas: [`p7/ui-1-no-descargado.png`](p7/ui-1-no-descargado.png), [`p7/ui-2-descargando.png`](p7/ui-2-descargando.png) y [`p7/ui-3-resultado.png`](p7/ui-3-resultado.png).
- Videos: [`p7/video-1.mp4`](p7/video-1.mp4) a [`p7/video-3.mp4`](p7/video-3.mp4), y los cuadros del botón en [`p7/video-boton-analizando.png`](p7/video-boton-analizando.png).
- Log: [`p7/ui.log`](p7/ui.log). Los `back at None` de las líneas `v1`–`v3` vienen de una primera versión del análisis. Los tiempos de arriba salen de volver a correr el análisis de `p7video.py` sobre los mismos videos.

### Conclusión de P7

- `[FACT]` Se cumple la aceptación de P7 en el teléfono: con el motor cargado, una estafa muestra señales en 7,5–8,2 s en pantalla, y `analyzeMessage` tarda menos de 10 s en 34/34 lecturas.
- `[FACT]` La primera lectura después de abrir Pagar puede tardar unos 40–50 s si el motor no terminó de cargar. Pagar nunca espera a la IA.
- `[FACT]` Hubo un falso positivo en 3 controles. La muestra es chica y de un solo teléfono, así que no mide la tasa real de falsos positivos.
- `[RECOMMENDATION]` En la demo, abrir Pagar y pegar el mensaje unos 45 s antes de tocar "Revisar el mensaje".
