package com.guardpay.shared.ui.wiring

import com.guardpay.shared.domain.HeldPayment
import com.guardpay.shared.domain.LedgerTime
import com.guardpay.shared.domain.StellarAddress
import com.guardpay.shared.domain.TrustedContact
import com.guardpay.shared.signing.Signer
import com.guardpay.shared.stellar.StellarGateway
import com.guardpay.shared.stellar.SubmitResult
import com.guardpay.shared.ui.guardian.GuardianChain

/**
 * Narrows the gateway to what the guardian may do. The guardian's screens only see
 * [GuardianChain]; the gateway and the guardian's key stay here.
 */
class GatewayGuardianChain(
    private val gateway: StellarGateway,
    private val ownerAccount: StellarAddress,
    private val guardian: Signer,
) : GuardianChain {
    override suspend fun latestLedgerTime(): LedgerTime = gateway.latestLedgerTime()
    override suspend fun readHolds(): List<HeldPayment> = gateway.readHolds(ownerAccount)
    override suspend fun readTrustedContacts(): List<TrustedContact> = gateway.readTrustedContacts(ownerAccount)

    override suspend fun submitCancel(holdId: Long, onSigned: () -> Unit): SubmitResult {
        val tracking = object : Signer {
            override val publicKey: ByteArray get() = guardian.publicKey
            override suspend fun signHash(digest: ByteArray): ByteArray =
                guardian.signHash(digest).also { onSigned() }
        }
        return gateway.submitCancel(ownerAccount, holdId, tracking)
    }
}
