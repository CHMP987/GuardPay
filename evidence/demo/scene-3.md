# Escena 3 — El guardián detiene

**REAL.** Dos corridas. Cuenta del emulador. Guardián Diego. SAC de prueba, no el USDC de Circle.

| Campo | Valor |
| --- | --- |
| Test | Diego abre el detalle y detiene el hold de la escena 2. |
| Expected | Detenido. El guardián no mueve fondos. |
| Actual | "Detenido por ti". |
| Contract | Cuenta `CDWPPTDM…`. Registro `CC5NG54I…`. |
| Transaction | `cancel` firmado por el guardián del Keystore. |
| Hash | `fb9eafad4c9ec2934fd4711b933077a1ee9d91ae92314f25f3d193ed688fa56c` y `bb1f4dd7105ab3f0ebbbae3b5b45eb1e27e8feaf07755a04b8c262c420521a22`. |
| Timestamp | 15:36:22 UTC y 15:46:27 UTC. En el ensayo 2, retenido a las 15:45:27 y detenido a las 15:46:27. |
| Input | Hold 0 y hold 1. |
| Output | Estado detenido en la app. |
| Captura | `rN-s3-detenido.png` y los mp4 `rN-s23`. |
| Conclusión | El veto ocurrió dentro de la ventana de unos 2 minutos, dos veces. Tiempos desde el aviso hasta "Detenido": 43,0 s y 39,3 s. El aviso salió a los 0,2 s en estas corridas, con el servicio en primer plano. |
