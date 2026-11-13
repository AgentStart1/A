/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import org.openqa.selenium.Point
import org.openqa.selenium.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopClickPointTest {
    @Test
    fun `leaf text retains its center`() {
        assertEquals(Point(60, 40), desktopTextClickPoint(Rectangle(10, 20, 40, 100), emptyList()))
    }

    @Test
    fun `text container avoids embedded preview`() {
        val bounds = Rectangle(156, 243, 186, 682)
        val children = listOf(bounds, bounds, Rectangle(164, 273, 112, 666), Rectangle(164, 393, 28, 48))
        assertEquals(Point(497, 258), desktopTextClickPoint(bounds, children))
    }

    @Test
    fun `uncovered center is retained`() {
        assertEquals(
            Point(50, 50),
            desktopTextClickPoint(Rectangle(0, 0, 100, 100), listOf(Rectangle(0, 0, 20, 100))),
        )
    }

    @Test
    fun `overlapping descendants leave largest label area`() {
        assertEquals(
            Point(75, 10),
            desktopTextClickPoint(
                Rectangle(0, 0, 100, 100),
                listOf(Rectangle(0, 20, 80, 100), Rectangle(0, 0, 100, 50)),
            ),
        )
    }

    @Test
    fun `fully covered container retains native center`() {
        assertEquals(
            Point(-50, 50),
            desktopTextClickPoint(Rectangle(-100, 0, 100, 100), listOf(Rectangle(-200, -20, 200, 200))),
        )
    }
}
