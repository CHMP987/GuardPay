package com.guardpay.shared.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigatorTest {
    @Test
    fun popToDropsEverythingAboveTheRoot() {
        val nav = Navigator()
        nav.go(Screen.Home)
        nav.go(Screen.Pay)
        nav.go(Screen.Detail(PaymentKey.Local(1)))
        nav.popTo(Screen.Home)
        assertEquals(Screen.Home, nav.current)
        assertEquals(2, nav.depth)
        assertTrue(nav.back())
        assertEquals(Screen.Entry, nav.current)
    }

    @Test
    fun popToResetsWhenTheRootIsMissing() {
        val nav = Navigator()
        nav.go(Screen.GuardianList)
        nav.popTo(Screen.Home)
        assertEquals(Screen.Home, nav.current)
        assertEquals(1, nav.depth)
        assertFalse(nav.back())
    }

    @Test
    fun replaceSwapsOnlyTheTop() {
        val nav = Navigator(Screen.Home)
        nav.go(Screen.Pay)
        nav.replace(Screen.Detail(PaymentKey.Hold(3)))
        assertEquals(2, nav.depth)
        assertTrue(nav.back())
        assertEquals(Screen.Home, nav.current)
    }
}
