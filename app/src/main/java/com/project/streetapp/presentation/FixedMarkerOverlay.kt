package com.project.streetapp.presentation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

class FixedMarkerOverlay(
    val geoPoint: GeoPoint,
    private val drawable: Drawable,
    private val context: Context,
    val name: String
) : Overlay() {
    override fun draw(canvas: Canvas?, mapView: MapView?, shadow: Boolean) {
        if (canvas == null || mapView == null) return

        val projection = mapView.projection
        val point = Point()
        projection.toPixels(geoPoint, point)

        val scaleFactor = 0.2f
        val iconWidth = (drawable.intrinsicWidth * scaleFactor).toInt()
        val iconHeight = (drawable.intrinsicHeight * scaleFactor).toInt()

        val left = point.x - iconWidth / 2
        val top = point.y - iconHeight / 2

        drawable.setBounds(left, top, left + iconWidth, top + iconHeight)
        drawable.draw(canvas)
    }

    override fun onSingleTapConfirmed(e: MotionEvent?, mapView: MapView?): Boolean {
        if (mapView != null && e != null) {
            val projection = mapView.projection
            val geoPoint = projection.fromPixels(e.x.toInt(), e.y.toInt())
            if (contains(geoPoint.latitude, geoPoint.longitude)) {
                showBottomSheet(name, "GPS", SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()))
                return true
            }
        }
        return false
    }


    private fun contains(latitude: Double, longitude: Double): Boolean {
        return (latitude - geoPoint.latitude).absoluteValue < 0.001 &&
                (longitude - geoPoint.longitude).absoluteValue < 0.001
    }

    private fun showBottomSheet(name: String, gps: String, format: String) {
        if (context is MainActivity) {
            context.showBottomSheet(name, gps, format)
        }
    }
}
