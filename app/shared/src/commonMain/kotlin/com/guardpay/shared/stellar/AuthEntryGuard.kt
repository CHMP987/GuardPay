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
 * with no sub-invocations, for an address the app expects.
 */
internal object AuthEntryGuard {
    enum class Role { SmartAccount, ClassicAccount }

    fun matchesCall(invocation: SorobanAuthorizedInvocationXdr, expected: InvokeContractArgsXdr): Boolean {
        val fn = invocation.function as? SorobanAuthorizedFunctionXdr.ContractFn ?: return false
        return invocation.subInvocations.isEmpty() && xdr(fn.value).contentEquals(xdr(expected))
    }

    /**
     * Who must sign for [address]: the smart account itself (owner key through
     * `__check_auth`), or a classic G account whose key is the signer's (the
     * guardian cancelling). Anyone else: null, and the entry is refused.
     */
    fun role(address: String, account: StellarAddress, signerAccountId: String): Role? = when {
        account.isContract && address == account.value -> Role.SmartAccount
        address == signerAccountId -> Role.ClassicAccount
        else -> null
    }

    private fun xdr(args: InvokeContractArgsXdr): ByteArray = XdrWriter().also { args.encode(it) }.toByteArray()
}
