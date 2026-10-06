package com.guardpay.shared.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import com.guardpay.shared.domain.ACCOUNT
import com.guardpay.shared.domain.ANA
import com.guardpay.shared.domain.STRANGER
import com.guardpay.shared.domain.StellarAddress
import com.guardpay.shared.domain.addr
import com.guardpay.shared.domain.usdc
import com.guardpay.shared.signing.Signer
import com.guardpay.shared.stellar.AccountRules
import com.guardpay.shared.stellar.FakeSigner
import com.guardpay.shared.stellar.FakeStellarGateway
import com.guardpay.shared.ui.guardian.GuardianConfig
import com.guardpay.shared.ui.guardian.GuardianSession
import com.guardpay.shared.ui.model.DataSource
import com.guardpay.shared.ui.owner.OwnerConfig
import com.guardpay.shared.ui.owner.OwnerSession
import com.guardpay.shared.ui.wiring.GatewayGuardianChain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.datetime.TimeZone
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Every payment state on a 360 dp wide phone, against the in-memory contract model.
 * Each screen must name its state in words, keep every node inside 360 dp, and never
 * say "seguro", "protegido" or "verificado", on screen or to a screen reader. When
 * the build passes `gp.screens`, each screen is saved as a PNG (SIMULATED evidence).
 */
@OptIn(ExperimentalTestApi::class)
class ScreensAt360Test {
    private val ownerKey = ByteArray(32) { 1 }
    private val guardianKey = ByteArray(32) { 2 }
    private val guardian = addr('D')
    private val banned = Regex("""\b(segur[oa]s?|protegid[oa]s?|verificad[oa]s?)\b""", RegexOption.IGNORE_CASE)
    private val outDir = System.getProperty("gp.screens")?.let(::File)

    private class Rig(
        val gw: FakeStellarGateway,
        val nav: Navigator,
        val owner: OwnerSession,
        val guardian: GuardianSession,
        val scope: CoroutineScope,
    )

    /** Submits run to completion inline: the fake never suspends, so no dispatcher is needed. */
    private suspend fun rig(balance: Long = 1_000, signer: Signer = FakeSigner(ownerKey)): Rig {
        val gw = FakeStellarGateway(
            ACCOUNT, ownerKey, guardianKey,
            AccountRules(guardian, dailyCap = usdc(50), holdDurationSeconds = 120, expiryWindowSeconds = 600),
            listOf(ANA), usdc(balance),
        )
        val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        val names = mapOf(ANA.address to "Ana", guardian to "Diego")
        val nav = Navigator()
        val owner = OwnerSession(
            gw, signer, OwnerConfig(ACCOUNT, "Laura", "Diego", DataSource.Simulation, contactNames = names, tz = TimeZone.UTC),
            { com.guardpay.shared.ai.Analysis.Unavailable }, nav, scope,
        )
        val g = GuardianSession(
            GatewayGuardianChain(gw, ACCOUNT, FakeSigner(guardianKey)),
            GuardianConfig("Laura", "Diego", DataSource.Simulation, contactNames = names, tz = TimeZone.UTC),
            scope,
        )
        owner.refresh()
        g.refresh()
        return Rig(gw, nav, owner, g, scope)
    }

    private fun OwnerSession.fill(to: StellarAddress, amount: String) {
        newPayment()
        if (to == ANA.address) selectContact(to) else {
            openOther()
            setOther(to.value)
        }
        setAmount(amount)
    }

    /** Owner queues 150 USDC to a stranger and lands on its Detalle. */
    private fun Rig.queueHold() {
        nav.go(Screen.Home)
        owner.fill(STRANGER, "150")
        owner.openReview()
        owner.sign()
        check(nav.current == Screen.Detail(PaymentKey.Hold(0))) { "queue did not land on Detalle: ${nav.current}" }
    }

    private fun shoot(rig: Rig, name: String, mustSay: String) = runSkikoComposeUiTest(
        size = Size(720f, 1600f),
        density = Density(2f),
    ) {
        setContent { GuardPayApp(rig.nav, rig.owner, rig.guardian, simulation = true) }
        waitForIdle()
        val texts = allText()
        assertTrue(texts.any { mustSay in it }, "$name: no node says \"$mustSay\". Saw: $texts")
        texts.filter { banned.containsMatchIn(it) }.let { if (it.isNotEmpty()) fail("$name says: $it") }
        assertFitsIn360(name)
        outDir?.let { dir ->
            dir.mkdirs()
            ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(dir, "$name.png"))
        }
        rig.scope.cancel()
    }

    private fun SkikoComposeUiTest.nodes(): List<SemanticsNode> =
        onAllNodes(SemanticsMatcher("any") { true }, useUnmergedTree = true).fetchSemanticsNodes()

    private fun SkikoComposeUiTest.allText(): List<String> = nodes().flatMap { n ->
        n.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } +
            n.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() +
            listOfNotNull(n.config.getOrNull(SemanticsProperties.EditableText)?.text)
    }

    private fun SkikoComposeUiTest.assertFitsIn360(name: String) {
        val widthPx = 720f
        val outside = nodes().filter { it.boundsInRoot.right > widthPx + 1f || it.boundsInRoot.left < -1f }
        assertTrue(outside.isEmpty(), "$name: nodes outside 360 dp: ${outside.map { it.boundsInRoot to it.config }}")
    }

    @Test
    fun entrada() {
        val r = kotlinx.coroutines.runBlocking { rig() }
        shoot(r, "00-entrada", "Simulación")
    }

    @Test
    fun borrador() {
        val r = kotlinx.coroutines.runBlocking { rig() }
        r.nav.go(Screen.Home)
        r.owner.fill(STRANGER, "150")
        shoot(r, "01-borrador", "150")
    }

    @Test
    fun firmando() {
        val waits = object : Signer {
            override val publicKey = ownerKey
            override suspend fun signHash(digest: ByteArray): ByteArray = awaitCancellation()
        }
        val r = kotlinx.coroutines.runBlocking { rig(signer = waits) }
        r.nav.go(Screen.Home)
        r.owner.fill(STRANGER, "150")
        r.owner.openReview()
        r.owner.sign()
        shoot(r, "02-firmando", "Esperando tu passkey")
    }

    @Test
    fun retenido() {
        val r = kotlinx.coroutines.runBlocking { rig() }
        r.queueHold()
        shoot(r, "03-retenido", "Retenido")
    }

    @Test
    fun listoParaEnviar() {
        val r = kotlinx.coroutines.runBlocking { rig() }
        r.queueHold()
        r.gw.advance(120)
        kotlinx.coroutines.runBlocking { r.owner.refresh() }
        shoot(r, "04-listo-para-enviar", "Listo para enviar")
    }

    @Test
    fun enviado() {
        val r = kotlinx.coroutines.runBlocking { rig() }
        r.queueHold()
        r.gw.advance(120)
        kotlinx.coroutines.runBlocking { r.owner.refresh() }
        r.owner.release(0)
        shoot(r, "05-enviado", "Enviado")
    }

    @Test
    fun detenido() {
        val r = kotlinx.coroutines.runBlocking { rig() }
        r.queueHold()
        kotlinx.coroutines.runBlocking {
            r.gw.submitCancel(ACCOUNT, 0, FakeSigner(guardianKey))
            r.owner.refresh()
        }
        shoot(r, "06-detenido", "Detenido por Diego")
    }

    @Test
    fun vencido() {
        val r = kotlinx.coroutines.runBlocking { rig() }
        r.queueHold()
        r.gw.advance(120 + 600 + 1)
        kotlinx.coroutines.runBlocking { r.owner.refresh() }
        shoot(r, "07-vencido", "Vencido")
    }

    @Test
    fun rechazado() {
        val r = kotlinx.coroutines.runBlocking { rig(balance = 10) }
        r.nav.go(Screen.Home)
        r.owner.fill(ANA.address, "20")
        r.owner.openReview()
        r.owner.sign()
        shoot(r, "08-rechazado", "Rechazado por el contrato")
    }

    @Test
    fun errorDeRed() {
        val r = kotlinx.coroutines.runBlocking { rig() }
        r.queueHold()
        r.gw.advance(120)
        kotlinx.coroutines.runBlocking { r.owner.refresh() }
        r.gw.networkDown = true
        r.owner.release(0)
        shoot(r, "09-error-de-red", "Error de red")
    }

    @Test
    fun inicioConUnPagoRetenido() {
        val r = kotlinx.coroutines.runBlocking { rig() }
        r.queueHold()
        r.nav.popTo(Screen.Home)
        shoot(r, "10-inicio", "Retenido")
    }

    @Test
    fun guardianLista() {
        val r = kotlinx.coroutines.runBlocking { rig() }
        r.queueHold()
        kotlinx.coroutines.runBlocking { r.guardian.refresh() }
        r.nav.reset(Screen.Entry)
        r.nav.go(Screen.GuardianList)
        shoot(r, "11-guardian-lista", "Retenido")
    }

    @Test
    fun guardianConfirmaDetener() {
        val r = kotlinx.coroutines.runBlocking { rig() }
        r.queueHold()
        kotlinx.coroutines.runBlocking { r.guardian.refresh() }
        r.nav.reset(Screen.Entry)
        r.nav.go(Screen.GuardianList)
        r.nav.go(Screen.GuardianDetail(0))
        r.guardian.askStop(0)
        shoot(r, "12-guardian-detener", "Detener")
    }
}
