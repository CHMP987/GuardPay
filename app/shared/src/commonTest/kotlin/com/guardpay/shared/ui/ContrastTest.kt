package com.guardpay.shared.ui

import androidx.compose.ui.graphics.Color
import com.guardpay.shared.ui.theme.GpColor
import com.guardpay.shared.ui.theme.contrastRatio
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/** The contrast table of the visual proposal (section 5), recomputed from the tokens. */
class ContrastTest {
    private fun assertRatio(fg: Color, bg: Color, expected: Double, name: String) {
        val r = contrastRatio(fg, bg)
        assertTrue(abs(r - expected) < 0.06, "$name: got $r, table says $expected")
    }

    @Test
    fun matchesTheProposalTable() {
        assertRatio(GpColor.Navy, GpColor.White, 18.7, "Navy/White")
        assertRatio(GpColor.Navy, GpColor.Ice, 17.7, "Navy/Ice")
        assertRatio(GpColor.Teal, GpColor.White, 2.93, "Teal/White")
        assertRatio(GpColor.Navy, GpColor.Teal, 6.39, "Navy/Teal")
        assertRatio(GpColor.Mint, GpColor.Navy, 12.7, "Mint/Navy")
        assertRatio(GpColor.Warning, GpColor.White, 3.19, "Warning/White")
        assertRatio(GpColor.Success, GpColor.White, 3.30, "Success/White")
        assertRatio(GpColor.Danger, GpColor.White, 4.83, "Danger/White")
        assertRatio(GpColor.Danger, GpColor.Ice, 4.56, "Danger/Ice")
        assertRatio(GpColor.TealText, GpColor.White, 4.95, "TealText/White")
        assertRatio(GpColor.TealText, GpColor.Ice, 4.67, "TealText/Ice")
        assertRatio(GpColor.WarningText, GpColor.White, 5.29, "WarningText/White")
        assertRatio(GpColor.WarningText, GpColor.Ice, 4.99, "WarningText/Ice")
        assertRatio(GpColor.SuccessText, GpColor.White, 5.44, "SuccessText/White")
        assertRatio(GpColor.SuccessText, GpColor.Ice, 5.13, "SuccessText/Ice")
    }

    @Test
    fun everyTextColorPassesOnBothSurfaces() {
        val text = listOf(GpColor.Navy, GpColor.Navy64, GpColor.TealText, GpColor.WarningText, GpColor.SuccessText, GpColor.Danger)
        for (fg in text) for (bg in listOf(GpColor.White, GpColor.Ice)) {
            assertTrue(contrastRatio(fg, bg) >= 4.5, "$fg on $bg is under 4.5:1")
        }
        assertTrue(contrastRatio(GpColor.White, GpColor.Navy) >= 4.5, "button text")
        assertTrue(contrastRatio(GpColor.Mint, GpColor.Navy) >= 4.5, "guardian band")
    }

    @Test
    fun inputBordersPassComponentContrast() {
        assertTrue(contrastRatio(GpColor.Navy50, GpColor.White) >= 3.0)
        assertTrue(contrastRatio(GpColor.Navy50, GpColor.Ice) >= 3.0)
    }

    @Test
    fun whiteTextNeverWorksOnTeal() {
        // The reason white text never goes on Teal: it would fail even large-text contrast.
        assertTrue(contrastRatio(GpColor.White, GpColor.Teal) < 3.0)
    }
}
