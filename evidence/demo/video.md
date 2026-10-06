# Vídeo

## App

**REAL.** Los mp4 de las escenas 1, 2, 3 y 6 están en `evidence/demo/rehearsal/`. El diálogo de huella o PIN sale en negro. La app no pinta el hash en pantalla: el detalle tiene "Ver en Stellar". Los hashes están en `rehearsal.md` y en `scene-1.md`, `scene-2.md`, `scene-3.md`.

## Terminal

**REAL.** `evidence/security/p9-cli-run.log` es la salida de `scripts/attacks/run-all.sh` el 6 oct 2026. Ahí se leen los hashes y las líneas `horizon successful=false`, `unrecognized subcommand` y `TxBadSeq`.

`evidence/demo/terminal-rejections.mp4` graba la terminal el 6 oct 2026 con `execute` (no hay tx), GH-08 y GH-09 (`successful=false` y el hash). El log de la corrida completa de los 13 scripts es `evidence/security/p9-cli-run.log`.
