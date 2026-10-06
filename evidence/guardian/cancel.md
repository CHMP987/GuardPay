# Cancel del guardián

**REAL.**

| Campo | Valor |
| --- | --- |
| Test | GH-07 en la cuenta CLI. Escena 3 en el emulador. |
| Expected | El hold pasa a Stopped. El guardián no recibe el SAC. |
| Actual | CLI: el registro dejó el hold 1 en Stopped. Emulador: "Detenido por ti" dos veces. |
| Contract | Registro CLI `CBEDM2DZ…`, guardián `GA7G4HPJ…`. Registro del emulador `CC5NG54I…`, guardián Diego `GCCKC6JF…`. |
| Transaction | `cancel`. No pasa por `enforce`. |
| Hash | CLI: `09838694770afb1d17d60f3a65f3ee94e92c2b804c4337672f5f99c2bef9e8e8`. Emulador: `fb9eafad4c9ec2934fd4711b933077a1ee9d91ae92314f25f3d193ed688fa56c` y `bb1f4dd7105ab3f0ebbbae3b5b45eb1e27e8feaf07755a04b8c262c420521a22`. |
| Timestamp | CLI 2026-10-06T09:00:47Z. Emulador 15:36:22Z y 15:46:27Z. |
| Input | id del hold. |
| Output | Stopped. |
| Captura | `deployment.md`, `scene-3.md`. |
| Conclusión | El guardián detuvo. El intento posterior de enviar ese hold está en la escena 4. |

Un extraño cancelando un hold todavía Retained: nativo GH-07b verde. En esta sesión, `cancel` sobre holds ya terminales solo se simuló. Hash de ese intento: no hay tx.
