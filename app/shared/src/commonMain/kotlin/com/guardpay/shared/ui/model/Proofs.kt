package com.guardpay.shared.ui.model

import com.guardpay.shared.domain.HeldPayment
import com.guardpay.shared.domain.HoldStatus
import com.guardpay.shared.domain.LedgerTime
import com.guardpay.shared.domain.StellarAddress
import com.guardpay.shared.domain.TxHash
import com.guardpay.shared.ui.components.ProofRow
import kotlinx.datetime.TimeZone

/**
 * Where "Ver en Stellar" points. [explorerBase] is e.g.
 * `https://stellar.expert/explorer/testnet`; null means no links. Contract
 * addresses are null until INTERFACES.md and the deploy (Sync 2).
 */
data class ProofLinks(
    val explorerBase: String? = null,
    val codeUrl: String? = null,
    val testsUrl: String? = null,
    val registry: StellarAddress? = null,
    val policy: StellarAddress? = null,
) {
    fun tx(hash: TxHash) = explorerBase?.let { "$it/tx/${hash.hex}" }
    fun contract(a: StellarAddress) = explorerBase?.let { "$it/contract/${a.value}" }
}

/** A transaction this app saw, with who did what. */
data class KnownTx(val what: String, val hash: TxHash)

/** A refusal this app received, with the contract's error code when it gave one. */
data class RejectedAttempt(val hash: TxHash?, val code: Int?, val at: LedgerTime?)

/**
 * The rows of "Ver en Stellar" (section 7). "Clave del pago" is left out: the
 * registry exposes no binding hash yet (INTERFACES.md). Every value comes from a
 * chain read or a transaction this app submitted; nothing is computed here.
 */
fun proofRows(
    hold: HeldPayment?,
    txs: List<KnownTx>,
    rejected: List<RejectedAttempt>,
    links: ProofLinks,
    tz: TimeZone,
): List<ProofRow> = buildList {
    if (hold != null) {
        add(ProofRow("ID del pago", hold.id.toString(), "El registro existe en el contrato"))
        val status = when (hold.status) {
            HoldStatus.Held -> "HELD"
            HoldStatus.Executed -> "EXECUTED"
            HoldStatus.Cancelled -> "CANCELLED"
        }
        add(ProofRow("Estado", status, "Lo dice el contrato, no la app"))
        add(ProofRow("Hora de liberación (ledger)", hold.readyAt.stamp(tz), "La espera la fijó el contrato"))
    }
    listOfNotNull(links.policy?.let { "GuardianHold" to it }, links.registry?.let { "HoldRegistry" to it })
        .forEach { (name, a) ->
            add(ProofRow("Contratos · $name", a.short(), "Qué código aplica la regla", a.value, links.contract(a)))
        }
    txs.forEach { t ->
        add(ProofRow("Transacciones · ${t.what}", t.hash.hex.take(8) + "…" + t.hash.hex.takeLast(4), "Quién hizo qué y cuándo", t.hash.hex, links.tx(t.hash)))
    }
    rejected.forEach { r ->
        val code = r.code?.let { "error $it" } ?: "sin código"
        val value = (r.hash?.let { it.hex.take(8) + "… · " } ?: "") + code
        add(ProofRow("Intentos rechazados", value, "La regla actuó", r.hash?.hex ?: code, r.hash?.let { links.tx(it) }))
    }
}
