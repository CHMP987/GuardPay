package com.guardpay.shared.stellar

/**
 * The only door to the chain. Everything that reads or submits goes through
 * here, so the Stellar SDK can be swapped without touching domain or UI.
 * Operations land in P4, once docs/INTERFACES.md is frozen (Sync 1).
 */
interface StellarGateway
