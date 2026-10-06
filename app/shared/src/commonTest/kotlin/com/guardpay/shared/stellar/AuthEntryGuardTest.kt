package com.guardpay.shared.stellar

import com.guardpay.shared.domain.StellarAddress
import com.guardpay.shared.stellar.AuthEntryGuard.Role
import com.soneso.stellar.sdk.InvokeHostFunctionOperation
import com.soneso.stellar.sdk.xdr.HostFunctionXdr
import com.soneso.stellar.sdk.xdr.InvokeContractArgsXdr
import com.soneso.stellar.sdk.xdr.SorobanAuthorizedFunctionXdr
import com.soneso.stellar.sdk.xdr.SorobanAuthorizedInvocationXdr
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A hostile RPC must not get the owner or guardian to sign a call the app did not build. */
class AuthEntryGuardTest {
    private val token = cKey(2)
    private val account = cKey(1)
    private val dest = gKey(4)
    private val rpcOperator = gKey(6)

    private fun call(contract: StellarAddress, fn: String, to: StellarAddress, amount: Long): InvokeContractArgsXdr =
        (InvokeHostFunctionOperation.invokeContractFunction(contract.value, fn, listOf(sv(account), sv(to), i128(amount)))
            .hostFunction as HostFunctionXdr.InvokeContract).value

    private fun invocation(args: InvokeContractArgsXdr, subs: List<SorobanAuthorizedInvocationXdr> = emptyList()) =
        SorobanAuthorizedInvocationXdr(SorobanAuthorizedFunctionXdr.ContractFn(args), subs)

    private val built = call(token, "transfer", dest, 100)

    @Test
    fun exactCallIsSigned() {
        assertTrue(AuthEntryGuard.matchesCall(invocation(call(token, "transfer", dest, 100)), built))
    }

    @Test
    fun anyDifferenceIsRefused() {
        assertFalse(AuthEntryGuard.matchesCall(invocation(call(token, "transfer", rpcOperator, 100)), built))
        assertFalse(AuthEntryGuard.matchesCall(invocation(call(token, "transfer", dest, 101)), built))
        assertFalse(AuthEntryGuard.matchesCall(invocation(call(token, "approve", dest, 100)), built))
        assertFalse(AuthEntryGuard.matchesCall(invocation(call(cKey(7), "transfer", dest, 100)), built))
    }

    @Test
    fun hiddenSubInvocationIsRefused() {
        val smuggled = invocation(built, listOf(invocation(call(token, "transfer", rpcOperator, 5))))
        assertFalse(AuthEntryGuard.matchesCall(smuggled, built))
    }

    @Test
    fun signOnlyForTheAccountOrTheSignersOwnKey() {
        val signerId = gKey(3).value
        assertEquals(Role.SmartAccount, AuthEntryGuard.role(account.value, account, signerId))
        assertEquals(Role.ClassicAccount, AuthEntryGuard.role(signerId, account, signerId))
        assertNull(AuthEntryGuard.role(rpcOperator.value, account, signerId))
        assertNull(AuthEntryGuard.role(cKey(8).value, account, signerId))
    }

    @Test
    fun networkConfigIsTestnetShaped() {
        val ok = GuardPayNetwork("https://soroban-testnet.stellar.org", cKey(1), cKey(2), cKey(3), cKey(4))
        assertEquals(listOf(0u), ok.contextRuleIds)
        assertFailsWith<IllegalArgumentException> { ok.copy(rpcUrl = "http://soroban-testnet.stellar.org") }
        assertFailsWith<IllegalArgumentException> { ok.copy(usdc = gKey(1)) }
        assertFailsWith<IllegalArgumentException> { ok.copy(contextRuleIds = emptyList()) }
    }
}
