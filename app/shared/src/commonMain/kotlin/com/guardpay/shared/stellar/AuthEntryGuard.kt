package com.guardpay.shared.stellar

import com.guardpay.shared.domain.StellarAddress
import com.soneso.stellar.sdk.xdr.InvokeContractArgsXdr
import com.soneso.stellar.sdk.xdr.SorobanAuthorizedFunctionXdr
import com.soneso.stellar.sdk.xdr.SorobanAuthorizedInvocationXdr
import com.soneso.stellar.sdk.xdr.XdrWriter

/** Simulation asked for an authorization the app did not expect. Nothing was signed or sent. */
class UnexpectedAuthException(message: String) : Exception(message)

/**
 * The RPC is not trusted. Simulation returns the auth entries to sign, and a
 * signed entry can be replayed in any transaction until it expires, so a hostile
 * or broken RPC could slip in an entry for another call (a transfer to itself,
 * say). An entry is signed only if it authorizes exactly the call the app built,
 * with no sub-invocations, for the account the app expects.
 */
internal object AuthEntryGuard {
    fun matchesCall(invocation: SorobanAuthorizedInvocationXdr, expected: InvokeContractArgsXdr): Boolean {
        val fn = invocation.function as? SorobanAuthorizedFunctionXdr.ContractFn ?: return false
        return invocation.subInvocations.isEmpty() && xdr(fn.value).contentEquals(xdr(expected))
    }

    /**
     * Only the smart account's own entry gets the delegated-signer payload. The
     * signer's G account authorizes as the transaction source (credentials `Void`),
     * so no other address entry is ever expected.
     */
    fun isAccount(address: String, account: StellarAddress): Boolean = account.isContract && address == account.value

    private fun xdr(args: InvokeContractArgsXdr): ByteArray = XdrWriter().also { args.encode(it) }.toByteArray()
}
