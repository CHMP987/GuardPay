# Spike C (P1-C): firma desde Kotlin aceptada por `__check_auth`

**Veredicto: VERDE para C2 (ed25519) contra la cuenta OZ v0.7.0 de la demo del SDK.** Falta repetirlo contra la cuenta del Spike A (pin `b40c5ea`). C3 (passkey, GH-33): **no corrido**.

| | |
| --- | --- |
| Etiqueta | **REAL** (testnet, salida literal de una corrida) |
| Fecha | 2026-10-06, ledgers 5048761–5048763 (06:16 UTC) |
| Red | Stellar **Testnet** (`Test SDF Network ; September 2015`), RPC `https://soroban-testnet.stellar.org` |
| SDK | `com.soneso.stellar:stellar-sdk:1.14.0` (tag v1.14.0, commit `ef89d7d`), JVM, JDK 17 |
| Código | `spikes/kmp-signing/src/main/kotlin/SpikeC.kt` (desechable) |
| Comando | `./gradlew -p spikes/kmp-signing run` desde la raíz del repo |

## Qué se probó

1. Se generan en memoria tres claves de un solo uso: el pagador de comisiones (cuenta G, fondeada con friendbot), la clave de la dueña y la de un atacante. Ningún secreto se imprime ni se escribe.
2. Se despliega una smart account desde el wasm de la demo del SDK (`86b49fe0…6d28`, OZ stellar-contracts **v0.7.0**). Firmante: `External(verificador ed25519 CAW2Z46I…7AJ6, clave de la dueña)`. Sin políticas.
3. Se fondea la cuenta con 20 XLM y se construye `transfer(C → pagador, 1 XLM)` sobre el SAC nativo, que solo la cuenta puede autorizar.
4. Se calcula el digest de dos formas y se comparan: con `SmartAccountAuth.buildAuthDigest` y con un SHA-256 independiente sobre `signature_payload ‖ XDR(Vec[U32(0)])`, con el XDR escrito a mano.
5. Se ejecutan dos controles negativos (solo simulación) y después el caso positivo, que se envía a la red.

## Resultados

| Caso | Esperado | Resultado |
| --- | --- | --- |
| Digest SDK = digest independiente | iguales | **MATCH** |
| N1: la dueña firma `signature_payload` **sin** `context_rule_ids` | rechazo | **RECHAZADO**: `Error(Auth, InvalidAction)`; la cuenta devuelve `Error(Crypto, InvalidInput)` |
| N2: una clave que no es firmante firma el digest correcto | rechazo | **RECHAZADO**: `Error(Auth, InvalidAction)`; la cuenta devuelve `Error(Crypto, InvalidInput)` |
| Positivo: la dueña firma el digest correcto, regla `[0]` | aceptado y ejecutado | **SUCCESS** en el ledger 5048763 |

### Valores de la corrida

```
smart account C        = CCD6OT2PUCU7SVMXRHE6L3OUWGAIBWVJXTXTZY2XIVN5L2RIRO7MJH42
fee payer G            = GD3WRYI3KF7HOPRZ7VZLDZGP2VPMHBXIHO3RPQISZ6SYHU54C32XTPPM
owner ed25519 pubkey   = c03010d82a188d27537c3909557d38c641d56b31821aa0bb6a3f17f18ba52d6e
auth credential arm    = AddressV2
signature_payload      = 6d73938a2093f8bb8d872b7e7c66056e9b39efa0d839df9d94d055d87733891d
xdr(context_rule_ids)  = 0000001000000001000000010000000300000000
digest (SDK)           = eae4cc5605f6db0c582946cee03fab792178f22d69951bd808aaf680a08485f4
digest (independiente) = eae4cc5605f6db0c582946cee03fab792178f22d69951bd808aaf680a08485f4
```

### Transacciones (REAL)

Las dos se volvieron a consultar con `getTransaction` directo al RPC, sin pasar por el SDK, y ambas devolvieron `SUCCESS`.

| Tx | Hash | Ledger |
| --- | --- | --- |
| Fondeo G → C (20 XLM) | `3205e683b80d7e3e1b382946a68f6e65b065583eba95ea1e7021e1be70652c26` | 5048761 |
| **`transfer` C → G (1 XLM) firmado en Kotlin** | `2c7c671fc37dbf919613e3bca699d223582c40afe49d4a176e5168d551ab2d26` | 5048763 |

https://stellar.expert/explorer/testnet/tx/2c7c671fc37dbf919613e3bca699d223582c40afe49d4a176e5168d551ab2d26

## Qué demuestra y qué no

- `[FACT]` kmp-stellar-sdk 1.14.0 produce, desde Kotlin y con ed25519 puro, una firma que el `__check_auth` de una cuenta OZ v0.7.0 acepta en testnet.
- `[FACT]` En esa versión, el digest que se firma es `sha256(signature_payload ‖ xdr(context_rule_ids))`. El cálculo independiente coincide con el del SDK, y el host acepta la firma hecha sobre él.
- `[FACT]` Firmar solo el `signature_payload`, sin los ids de regla, se rechaza (N1). Esto es compatible con que `context_rule_ids` esté atado al digest firmado, que es la propiedad de la que depende el diseño.
- `[FACT]` Una clave que no es firmante se rechaza aunque firme el digest correcto (N2).
- `[INFERENCE]` El id 0 corresponde a la regla `Default` que crea el constructor. No se leyó con `get_context_rule`; solo se sabe que `[0]` fue aceptado.
- `[UNVERIFIED]` **No se probó contra el pin `b40c5ea`.** La cuenta es OZ v0.7.0, no la nuestra, y la fórmula del digest o el formato del `AuthPayload` podrían cambiar entre versiones. Hay que repetirlo contra la cuenta del Spike A de Abraham en cuanto exista.
- `[UNVERIFIED]` La cuenta no tenía política, así que esto no dice nada sobre `GuardianHold.enforce` ni sobre el riesgo R6.
- `[UNVERIFIED]` El `signature_payload` (el preimage `HashIdPreimage`) lo calculó el SDK. Lo que se verificó de forma independiente es el segundo paso, el digest. El primero solo se valida porque el host aceptó la firma.
- `[FACT]` Corrió en JVM, no en el teléfono. El `Signer` con Keystore en Android es P6.

## Pendiente

| Test | Estado |
| --- | --- |
| C2 contra la cuenta del Spike A (`b40c5ea`) | **no corrido** (la cuenta aún no existe) |
| C3 / GH-33: firmante passkey (WebAuthn) | **no corrido** (SHOULD) |
| Firma ed25519 desde Android (Keystore) | **no corrido** (P6) |

Fallback de R5: **no hizo falta**. No se llegó a ningún escalón.
