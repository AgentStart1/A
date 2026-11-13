/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import org.openqa.selenium.Point
import org.openqa.selenium.Rectangle

/** Prefer the label's own area when native text bounds also contain child controls. */
internal fun desktopTextClickPoint(bounds: Rectangle, descendants: List<Rectangle>): Point {
    // Mac2's scoped predicate query can also return its root with a new element handle.
    val children = descendants.filter { it != bounds }
    val center = Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2)
    if (children.none { it.containsPoint(center) }) return center
    val rows =
        (listOf(bounds.y, bounds.y + bounds.height) + children.flatMap { listOf(it.y, it.y + it.height) })
            .map { it.coerceIn(bounds.y, bounds.y + bounds.height) }.distinct().sorted()
    val freeAreas =
        rows.asSequence().zipWithNext().filter { (top, bottom) -> bottom > top }.flatMap { (top, bottom) ->
            val middle = top + (bottom - top) / 2
            val obstacles = children.filter { it.y <= middle && it.y + it.height > middle }
            val columns =
                (listOf(bounds.x, bounds.x + bounds.width) + obstacles.flatMap { listOf(it.x, it.x + it.width) })
                    .map { it.coerceIn(bounds.x, bounds.x + bounds.width) }.distinct().sorted()
            columns.asSequence().zipWithNext()
                .filter { (left, right) -> right > left }
                .map { (left, right) -> Rectangle(left, top, bottom - top, right - left) }
                .filter { area -> obstacles.none { it.containsPoint(area.centerPoint()) } }
        }
    return freeAreas.maxByOrNull { it.width.toLong() * it.height }?.centerPoint() ?: center
}

private fun Rectangle.centerPoint(): Point = Point(x + width / 2, y + height / 2)

private fun Rectangle.containsPoint(point: Point): Boolean =
    point.x >= x && point.x < x + width && point.y >= y && point.y < y + height
