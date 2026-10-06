# Cómo leer evidence/

Tres etiquetas de datos:

- **REAL:** salió de testnet, del emulador o de un comando de esta máquina, con hash o log.
- **SIMULATED:** la UI de los 9 estados en `demo/screens/`, generada por el test headless. No es una transacción.
- **ILLUSTRATIVE:** un dibujo o un ejemplo que no pretende ser una medición.

Un test que no se corrió dice **no corrido**. No hay una cuarta etiqueta.

Cada ficha nueva de P9 y P10 trae: Test, Expected, Actual, Contract, Transaction, Hash, Timestamp, Input, Output, Captura, Conclusión.

El activo, cada vez que se nombra un pago, es un SAC de prueba emitido por la dueña. No es el USDC de Circle.

Hay dos cuentas. Los hashes de `stellar/deployment.md` son los ensayos por CLI. Los de `demo/rehearsal.md` son la demo del emulador. No se intercambian.
