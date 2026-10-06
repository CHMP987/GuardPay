package com.guardpay.shared.signing

/**
 * Owner's signer. Each platform provides its own implementation: ed25519 in
 * Android Keystore (MUST), passkey (SHOULD). Lands in P6.
 */
interface Signer
