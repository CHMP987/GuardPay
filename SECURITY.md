# Política de seguridad de GuardPay

## Estado del proyecto

GuardPay es un MVP que corre **solo en Stellar Testnet**, con un token de prueba. No hay despliegue en mainnet ni fondos reales.

- Los contratos **no tienen una auditoría de seguridad profesional**.
- OpenZeppelin `stellar-contracts` está fijado a un commit previo a una versión estable (`b40c5ea`).
- La revisión de [`AUDIT_REPORT.md`](AUDIT_REPORT.md) la hizo un asistente de IA a pedido del equipo. No es una auditoría independiente y no certifica nada.

No uses GuardPay con fondos reales.

## Versiones con soporte

No hay versiones publicadas. Solo se mantiene la rama `main`.

## Cómo reportar una vulnerabilidad

**Todavía no existe un canal privado de reporte.** El repositorio no declara un correo de seguridad, y el reporte privado de vulnerabilidades de GitHub está deshabilitado (consulta a la API de GitHub del 12 oct 2026: `enabled: false`).

El equipo debe establecer un canal y escribirlo aquí. Una opción es habilitar el reporte privado de GitHub en la configuración del repositorio. Hasta entonces:

- No publiques en un issue público los detalles de una vulnerabilidad que permita mover fondos, saltarse una retención o impersonar al guardián.
- Si necesitas avisar, abre un issue público **sin detalles explotables**, pidiendo un canal privado de contacto.

No hay compromiso de tiempo de respuesta: el proyecto no lo ha definido.

## Qué entra en el alcance

- Los tres contratos: `contracts/account`, `contracts/guardian_hold`, `contracts/hold_registry`.
- La firma y el envío de transacciones del cliente: `app/shared/.../stellar/` y `app/android/.../signing/`.
- La separación entre la IA y la firma (`app/shared/.../ai/`).
- La vista del guardián (`app/shared/.../ui/guardian/`) y el servicio de aviso (`app/android/.../notifications/`).
- Los scripts de `scripts/`, en lo que toca al manejo de claves.

Especialmente relevante:

- cualquier forma de mover fondos sin la firma de la dueña;
- cualquier forma de enviar a un destino nuevo sin una retención madurada;
- que el guardián pueda mover fondos o volverse firmante;
- que una retención detenida se pueda enviar;
- que la app firme algo distinto de la llamada que construyó.

## Fuera del alcance

- Mainnet: no hay despliegue.
- El token de prueba: su administrador es la dueña y `mint`/`clawback` no pasan por la política. Está documentado ([`README.md`](README.md#limitaciones-conocidas)).
- El código vendorizado de OpenZeppelin en `vendor/`: repórtalo a su proyecto.
- Ataques que requieren el teléfono desbloqueado con acceso `adb` al build de depuración. Están documentados como limitaciones en [`AUDIT_REPORT.md`](AUDIT_REPORT.md) (F-08, F-09).

## Riesgos conocidos

Los hallazgos abiertos están en [`AUDIT_REPORT.md`](AUDIT_REPORT.md#3-hallazgos), y las limitaciones aceptadas en [`README.md`](README.md#limitaciones-conocidas). Entre ellos:

- F-01: una retención a un contacto de confianza no se liberaba por el registro. Está corregido en el código y la política nueva está en testnet desde el 12 oct, pero las cuentas creadas antes (la publicada y la de ataques) mantienen la política anterior. El teléfono y el emulador se reprovisionaron con la política nueva el 13 oct, y desde ellos una retención a un contacto se envió al madurar sin gastar tope.
- F-02: el registro desplegado recorre todos sus registros en cada operación, y pasa el límite de 400 entradas hacia los 390 registros. El código ya usa un índice, pero ese registro no está desplegado.
- F-04: el guardián no se puede cambiar y no hay recuperación de cuenta.
- F-16: el diálogo de firma no muestra destino ni monto.

## Manejo de secretos en el repositorio

- Las semillas de los scripts de despliegue viven en `scripts/.testnet/`, ignorado por git.
- La app guarda sus claves en el Android Keystore; la clave privada no sale del teléfono.
- Las pruebas contra testnet generan claves en memoria y no las imprimen.
- El escaneo de esta auditoría no encontró semillas, claves privadas ni tokens versionados ([`AUDIT_REPORT.md`](AUDIT_REPORT.md#5-secretos-e-integridad-del-repositorio)). Eso no garantiza que no existan con otro formato.

Si encuentras un secreto en el repositorio o en su historial, no lo copies en un issue: avisa por el canal privado, cuando exista.
