package com.guardpay.shared.stellar

import com.guardpay.shared.domain.StellarAddress
import com.ionspin.kotlin.bignum.integer.BigInteger
import com.soneso.stellar.sdk.Address
import com.soneso.stellar.sdk.KeyPair
import com.soneso.stellar.sdk.scval.Scv
import com.soneso.stellar.sdk.xdr.SCValXdr

/** Real (checksummed) strkeys derived from fixed bytes. Not funded accounts. */
fun gKey(n: Int) = StellarAddress(KeyPair.fromPublicKey(ByteArray(32) { n.toByte() }).getAccountId())
fun cKey(n: Int) = StellarAddress(Address.fromContract(ByteArray(32) { n.toByte() }).getEncodedAddress())

fun sv(a: StellarAddress): SCValXdr = Address(a.value).toSCVal()
fun i128(n: BigInteger): SCValXdr = Scv.toInt128(n)
fun i128(n: Long): SCValXdr = i128(BigInteger.fromLong(n))
fun u64(n: Long): SCValXdr = Scv.toUint64(n.toULong())
fun sym(s: String): SCValXdr = Scv.toSymbol(s)
fun none(): SCValXdr = Scv.toVoid()
fun variant(name: String): SCValXdr = Scv.toVec(listOf(sym(name)))
fun vec(vararg items: SCValXdr): SCValXdr = Scv.toVec(items.toList())

/** A `#[contracttype]` struct as the host encodes it. */
fun struct(vararg fields: Pair<String, SCValXdr>): SCValXdr =
    Scv.toMap(LinkedHashMap(fields.associate { (k, v) -> sym(k) to v }))
