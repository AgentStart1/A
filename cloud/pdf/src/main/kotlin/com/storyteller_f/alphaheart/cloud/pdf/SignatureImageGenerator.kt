/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.pdf

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

object SignatureImageGenerator {
    fun generate(signee: String, timestamp: String, hint: String): ByteArray {
        val width = 600
        val rowHeight = 30
        val height = rowHeight * 3
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        // Background
        g2d.color = Color.WHITE
        g2d.fillRect(0, 0, width, height)

        // Outer Border
        g2d.color = Color.BLACK
        g2d.stroke = BasicStroke(1f)
        g2d.drawRect(0, 0, width - 1, height - 1)

        val iconWidth = 80

        // Vertical line for Icon
        g2d.drawLine(iconWidth, 0, iconWidth, height)

        // Horizontal lines
        g2d.drawLine(iconWidth, rowHeight, width, rowHeight)
        g2d.drawLine(iconWidth, rowHeight * 2, width, rowHeight * 2)

        // Draw Icon
        drawBadge(g2d, iconWidth / 2, height / 2, 25)

        // Draw Text
        // Define columns
        val labelX = iconWidth + 10
        val labelWidth = 80

        // Vertical line between label and value?
        // The example shows a vertical line between generic label (Signee) and value?
        // Looking closely at the image provided by user...
        // Yes, there is a vertical line separating "Signee" and "commonName=..."

        val valueDividerX = labelX + labelWidth
        g2d.drawLine(valueDividerX, 0, valueDividerX, height)

        val font = Font("SansSerif", Font.PLAIN, 12)
        val boldFont = font.deriveFont(Font.BOLD)

        drawRow(g2d, "Signee", signee, iconWidth, 0, rowHeight, valueDividerX, font, boldFont)
        drawRow(g2d, "Timestamp", timestamp, iconWidth, rowHeight, rowHeight, valueDividerX, font, boldFont)
        drawRow(g2d, "Hint", hint, iconWidth, rowHeight * 2, rowHeight, valueDividerX, font, boldFont)

        g2d.dispose()

        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        return baos.toByteArray()
    }

    @Suppress("LongParameterList")
    private fun drawRow(
        g2d: Graphics2D,
        label: String,
        value: String,
        @Suppress("UNUSED_PARAMETER") x: Int,
        y: Int,
        height: Int,
        dividerX: Int,
        font: Font,
        boldFont: Font,
    ) {
        val centerY = y + height / 2 + 5 // Baseline approximation

        // Label (Right aligned in its box?)
        // Example: "Signee "
        g2d.font = boldFont
        val labelMetrics = g2d.fontMetrics
        val labelStrWidth = labelMetrics.stringWidth(label)
        g2d.drawString(label, dividerX - labelStrWidth - 5, centerY)

        // Value (Left aligned)
        g2d.font = font
        g2d.drawString(value, dividerX + 5, centerY)
    }

    private fun drawBadge(g2d: Graphics2D, cx: Int, cy: Int, radius: Int) {
        g2d.color = Color.BLACK
        val path = Path2D.Double()
        val numPoints = 16
        for (i in 0 until numPoints * 2) {
            val angle = Math.PI * i / numPoints
            val r = if (i % 2 == 0) radius.toDouble() else radius * 0.85
            val x = cx + r * Math.cos(angle)
            val y = cy + r * Math.sin(angle)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.closePath()
        g2d.stroke = BasicStroke(2f)
        g2d.draw(path)

        // Checkmark
        val checkPath = Path2D.Double()
        // Simple checkmark
        checkPath.moveTo((cx - 10).toDouble(), cy.toDouble())
        checkPath.lineTo((cx - 3).toDouble(), (cy + 10).toDouble())
        checkPath.lineTo((cx + 12).toDouble(), (cy - 10).toDouble())
        g2d.draw(checkPath)
    }
}
