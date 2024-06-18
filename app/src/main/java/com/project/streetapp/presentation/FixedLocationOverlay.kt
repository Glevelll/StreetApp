package com.project.streetapp.presentation

import android.graphics.Canvas
import android.graphics.Point
import android.graphics.drawable.Drawable
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

class FixedLocationOverlay(private val geoPoint: GeoPoint, private val drawable: Drawable) : Overlay() {
    override fun draw(canvas: Canvas?, mapView: MapView?, shadow: Boolean) {
        if (canvas == null || mapView == null) return

        val projection = mapView.projection
        val point = Point()
        projection.toPixels(geoPoint, point)

        val scaleFactor = 0.5
        val iconWidth = (drawable.intrinsicWidth * scaleFactor).toInt()
        val iconHeight = (drawable.intrinsicHeight * scaleFactor).toInt()

        val left = point.x - iconWidth / 2
        val top = point.y - iconHeight / 2

        drawable.setBounds(left, top, left + iconWidth, top + iconHeight)
        drawable.draw(canvas)
    }
}
