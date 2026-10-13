# Cómo contribuir a GuardPay

Este documento recoge solo lo que el repositorio ya hace o exige. Lo que aún no está acordado se marca como **Propuesta**. Las reglas del proyecto en detalle están en [`CLAUDE.md`](CLAUDE.md); este archivo no las reemplaza.

## Antes de empezar

- Lee la [tesis del producto](README.md#la-solución). No se cambia: el guardián puede detener dinero, nunca moverlo, y nunca es firmante de la cuenta.
- Lee el orden de autoridad de los documentos de diseño en [`README.md`](README.md#documentación). Si dos documentos se contradicen, manda el que está más arriba.
- Revisa [`ROADMAP.md`](ROADMAP.md) y [`AUDIT_REPORT.md`](AUDIT_REPORT.md) para ver lo abierto.

## Entorno

Versiones que usa el repositorio (fuentes: `gradle/wrapper/gradle-wrapper.properties`, `gradle/libs.versions.toml`, `Cargo.toml`, `.github/workflows/ci.yml`):

- JDK 17 y Gradle 8.14.3 por el wrapper (`./gradlew`). No instales otra versión de Gradle.
- Android SDK, con la ruta en `local.properties` (ignorado por git) usando barras normales.
- Rust 1.98.0 (la que fija el CI), `stellar-cli` 28.1.0 y el target `wasm32v1-none` para compilar los contratos.

## Ramas y pull requests

Lo que se observa en el historial:

- `main` es la rama principal. Los cambios entran por pull request a `main`.
- El CI (`.github/workflows/ci.yml`) corre en cada pull request a `main` y en cada push a `main` y a `ios`.
- La rama `ios` dispara además `.github/workflows/ios.yml` (simulador de iOS en macOS).
- Los mensajes de commit del historial están en inglés y empiezan por el área (por ejemplo `README: …`, `Evidence: …`).

**Propuesta:** un pull request por tema, con una descripción que diga qué pruebas se corrieron y cuáles no.

## Pruebas obligatorias antes de abrir un pull request

Son las mismas que corre el CI, sin red:

```bash
cargo test --locked -p account -p guardian_hold -p hold_registry
```

```bash
./gradlew :app:shared:allTests
```

```bash
./gradlew :app:android:assembleDebug :app:android:compileReleaseKotlin
```

`allTests` también corre cuatro comprobaciones que fallan el build:

| Tarea | Qué rechaza |
| --- | --- |
| `checkCommonMainArchitecture` | imports de Android, iOS, LiteRT-LM o WebAuthn en `commonMain` |
| `checkDependencyGraph` | que `ai/` dependa de firma o de Stellar, o un `Fake*` fuera de las pruebas |
| `checkUiVocabulary` | "seguro", "protegido" o "verificado" en textos de la interfaz |
| `checkGuardianSurface` | que la interfaz del guardián toque firma, envío o `StellarGateway` |

Si Gradle responde `UP-TO-DATE` o `FROM-CACHE`, las pruebas no corrieron. Para forzarlas, añade `--rerun` a cada tarea de prueba o usa `--rerun-tasks`.

Las pruebas `*LiveTest` van contra Stellar **Testnet** y solo corren con `-PliveTestnet`. No son obligatorias para un pull request; si las corres, escribe el resultado.

## Reglas que no se negocian

Vienen de [`CLAUDE.md`](CLAUDE.md):

- **Solo testnet.** Nunca mainnet.
- **Versiones exactas.** Ninguna dependencia con `latest` ni rangos abiertos. No cambies archivos de bloqueo (`Cargo.lock`) sin explicarlo en el pull request.
- **Contratos:** la cuenta mantiene una sola regla `Default` y ninguna función `execute`, `upgrade` ni de administración. El guardián nunca es firmante.
- **IA:** `GuardPayAI` tiene una sola función, no ve saldos, contactos, política ni claves, y nunca propone destino ni monto.
- **Interfaz:** "Retenido" solo aparece si existe el registro en cadena. No uses "seguro" ni "protegido".
- **Secretos:** nunca subas semillas, claves privadas ni tokens. Las semillas de los scripts viven en `scripts/.testnet/`, que git ignora. Las pruebas generan sus claves en memoria y no las imprimen.

## Evidencia y documentación

- La documentación del proyecto está en español; el código y sus comentarios, en inglés.
- La evidencia va en `evidence/` con una etiqueta: **REAL**, **SIMULATED** o **ILLUSTRATIVE**.
- Nunca inventes un hash de transacción, una medición ni un resultado. Lo que no se corrió se escribe "no corrido".
- En los documentos, las afirmaciones llevan **[FACT]**, **[INFERENCE]**, **[RECOMMENDATION]** o **[UNVERIFIED]**. No se afirma "primero", "único" ni "no existe forma de saltarlo".
- Si cambias el estado de una fase, actualiza [`docs/ESTADO-FASES.md`](docs/ESTADO-FASES.md).

## Qué significa "hecho"

Según [`CLAUDE.md`](CLAUDE.md): implementación + pruebas unitarias + integración + pruebas de seguridad + criterios de aceptación + evidencia. Que compile no basta.

## Cambios en los contratos

- Agrega o actualiza una prueba GH en `contracts/guardian_hold/tests/must.rs` por cada cambio de comportamiento.
- La cuenta no se puede actualizar en cadena: un cambio de contrato exige redesplegar con `scripts/deploy-testnet.ps1` o `scripts/deploy-testnet.sh` (solo testnet) y actualizar `evidence/stellar/deployment.md`.
- **Propuesta:** compilar el wasm (`stellar contract build`) y anotar su hash en el pull request, ya que el CI no lo hace todavía (ver F-10 en [`AUDIT_REPORT.md`](AUDIT_REPORT.md#f-10)).

## Seguridad

Para reportar una vulnerabilidad, lee [`SECURITY.md`](SECURITY.md). No abras un issue público con detalles explotables.
