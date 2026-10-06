package com.guardpay.shared.domain

/** Shape-valid strkeys for tests (not real accounts). */
fun addr(c: Char, prefix: Char = 'G') = StellarAddress(prefix + c.toString().repeat(55))

val ACCOUNT = addr('A', prefix = 'C')
val ANA = TrustedContact("Ana", addr('B'))
val STRANGER = addr('X')

fun usdc(whole: Long) = UsdcAmount.ofWhole(whole)

fun hash(n: Int) = TxHash(n.toString(16).padStart(64, '0'))

fun hold(
    status: HoldStatus = HoldStatus.Held,
    destination: StellarAddress = STRANGER,
    amount: UsdcAmount = usdc(150),
    createdAt: Long = 1_000,
    readyAt: Long = 1_120,
    expiresAt: Long? = 1_720,
    cancelledBy: Party? = null,
) = HeldPayment(
    id = 7,
    account = ACCOUNT,
    destination = destination,
    amount = amount,
    createdAt = LedgerTime(createdAt),
    readyAt = LedgerTime(readyAt),
    expiresAt = expiresAt?.let(::LedgerTime),
    status = status,
    cancelledBy = cancelledBy,
    cancelledAt = cancelledBy?.let { LedgerTime(1_060) },
)
