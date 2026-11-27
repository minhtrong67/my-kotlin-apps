package com.gnort67.calendarpro.ui.shapes

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/** Hình khối Material 3 Expressive (cookie, scallop, flower...) dựng từ polygon của bản web. */
class PolygonShape(private val pts: FloatArray) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val p = Path()
        p.moveTo(pts[0] * size.width, pts[1] * size.height)
        var i = 2
        while (i < pts.size) {
            p.lineTo(pts[i] * size.width, pts[i + 1] * size.height)
            i += 2
        }
        p.close()
        return Outline.Generic(p)
    }
}

object ExpressiveShapes {
    val Circle = PolygonShape(POLY_CIRCLE)
    val Scallop = PolygonShape(POLY_SCALLOP)
    val Cookie = PolygonShape(POLY_COOKIE)
    val Sunny = PolygonShape(POLY_SUNNY)
    val Flower = PolygonShape(POLY_FLOWER)
    val Clover = PolygonShape(POLY_CLOVER)
    val Burst = PolygonShape(POLY_BURST)
}
