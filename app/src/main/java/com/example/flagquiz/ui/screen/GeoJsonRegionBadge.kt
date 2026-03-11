package com.example.flagquiz.ui.screen

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.flagquiz.domain.model.GameRegion
import org.json.JSONObject
import kotlin.math.abs

internal data class GeoPoint(val lon: Double, val lat: Double)
internal data class GeoShape(val rings: List<List<GeoPoint>>)
internal data class ShapeBounds(
    val minLon: Double,
    val maxLon: Double,
    val minLat: Double,
    val maxLat: Double
)
internal data class BadgeRegionData(
    val shapes: List<GeoShape>,
    val bounds: ShapeBounds
)

object RegionBadgeCache {
    private val cache = mutableMapOf<GameRegion, BadgeRegionData>()

    fun preloadAll(context: Context) {
        GameRegion.entries.forEach { region ->
            load(context, region)
        }
    }

    internal fun load(context: Context, region: GameRegion): BadgeRegionData = synchronized(cache) {
        cache.getOrPut(region) {
            val shapes = loadRegionShapes(context, region.assetName())
                .map { it.simplified(region.simplifyTolerance()) }
                .filter { shape -> shape.rings.any { ring -> ring.size >= 4 } }
            BadgeRegionData(
                shapes = shapes,
                bounds = region.viewportOverride() ?: calculateVisualBounds(shapes) ?: fallbackBounds(shapes)
            )
        }
    }
}

@Composable
fun GeoJsonRegionBadge(region: GameRegion) {
    val context = LocalContext.current
    val regionData = remember(region) { RegionBadgeCache.load(context, region) }
    val background = when (region) {
        GameRegion.WORLD -> Color(0xFFD7F0FF)
        GameRegion.AFRICA -> Color(0xFFFFE5B4)
        GameRegion.ASIA -> Color(0xFFFFE0CC)
        GameRegion.EUROPE -> Color(0xFFDDE7FF)
        GameRegion.NORTH_AMERICA -> Color(0xFFD7F7E3)
        GameRegion.SOUTH_AMERICA -> Color(0xFFFFD9E2)
        GameRegion.OCEANIA -> Color(0xFFD9F4FF)
    }
    val land = when (region) {
        GameRegion.WORLD -> Color(0xFF3E8E5B)
        GameRegion.AFRICA -> Color(0xFF9B6A2F)
        GameRegion.ASIA -> Color(0xFFB65A3A)
        GameRegion.EUROPE -> Color(0xFF5372C7)
        GameRegion.NORTH_AMERICA -> Color(0xFF2C8C67)
        GameRegion.SOUTH_AMERICA -> Color(0xFFB54D6B)
        GameRegion.OCEANIA -> Color(0xFF2E86A6)
    }

    Box(
        modifier = Modifier
            .size(88.dp)
            .background(background, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(72.dp)) {
            drawGeoShapes(regionData, land)
        }
    }
}

private fun GameRegion.assetName(): String = when (this) {
    GameRegion.WORLD -> "world.geojson"
    GameRegion.AFRICA -> "africa.geojson"
    GameRegion.ASIA -> "asia.geojson"
    GameRegion.EUROPE -> "europe.geojson"
    GameRegion.NORTH_AMERICA -> "north_america.geojson"
    GameRegion.SOUTH_AMERICA -> "south_america.geojson"
    GameRegion.OCEANIA -> "oceania.geojson"
}

private fun GameRegion.viewportOverride(): ShapeBounds? = when (this) {
    GameRegion.EUROPE -> ShapeBounds(minLon = -25.0, maxLon = 45.0, minLat = 34.0, maxLat = 72.0)
    GameRegion.ASIA -> ShapeBounds(minLon = 25.0, maxLon = 180.0, minLat = -12.0, maxLat = 82.0)
    GameRegion.NORTH_AMERICA -> ShapeBounds(minLon = -170.0, maxLon = -50.0, minLat = 5.0, maxLat = 85.0)
    GameRegion.SOUTH_AMERICA -> ShapeBounds(minLon = -92.0, maxLon = -30.0, minLat = -58.0, maxLat = 15.0)
    GameRegion.OCEANIA -> ShapeBounds(minLon = 110.0, maxLon = 180.0, minLat = -52.0, maxLat = 5.0)
    else -> null
}

private fun GameRegion.simplifyTolerance(): Double = when (this) {
    GameRegion.WORLD -> 0.75
    GameRegion.ASIA -> 0.45
    GameRegion.NORTH_AMERICA -> 0.45
    GameRegion.EUROPE -> 0.22
    GameRegion.AFRICA -> 0.28
    GameRegion.SOUTH_AMERICA -> 0.28
    GameRegion.OCEANIA -> 0.30
}

private fun loadRegionShapes(context: Context, fileName: String): List<GeoShape> {
    val json = context.assets.open("regions/$fileName").bufferedReader().use { it.readText() }
    val featureCollection = JSONObject(json)
    val features = featureCollection.getJSONArray("features")
    val shapes = mutableListOf<GeoShape>()

    for (index in 0 until features.length()) {
        val geometry = features.getJSONObject(index).getJSONObject("geometry")
        when (geometry.getString("type")) {
            "Polygon" -> shapes += GeoShape(parsePolygonCoordinates(geometry.getJSONArray("coordinates")))
            "MultiPolygon" -> {
                val multiPolygon = geometry.getJSONArray("coordinates")
                for (multiIndex in 0 until multiPolygon.length()) {
                    shapes += GeoShape(parsePolygonCoordinates(multiPolygon.getJSONArray(multiIndex)))
                }
            }
        }
    }

    return shapes
}

private fun parsePolygonCoordinates(ringsArray: org.json.JSONArray): List<List<GeoPoint>> {
    val rings = mutableListOf<List<GeoPoint>>()
    for (ringIndex in 0 until ringsArray.length()) {
        val ringArray = ringsArray.getJSONArray(ringIndex)
        val points = mutableListOf<GeoPoint>()
        for (pointIndex in 0 until ringArray.length()) {
            val point = ringArray.getJSONArray(pointIndex)
            points += GeoPoint(lon = point.getDouble(0), lat = point.getDouble(1))
        }
        rings += points
    }
    return rings
}

private fun GeoShape.simplified(tolerance: Double): GeoShape {
    val simplifiedRings = rings.mapNotNull { ring ->
        simplifyRing(ring, tolerance)?.takeIf { it.size >= 4 }
    }
    return GeoShape(simplifiedRings)
}

private fun simplifyRing(points: List<GeoPoint>, tolerance: Double): List<GeoPoint>? {
    if (points.size < 4) return null

    val closedPoints = if (points.first() == points.last()) points else points + points.first()
    val simplified = mutableListOf(closedPoints.first())
    var lastKept = closedPoints.first()
    val toleranceSquared = tolerance * tolerance

    for (index in 1 until closedPoints.lastIndex) {
        val point = closedPoints[index]
        val deltaLon = point.lon - lastKept.lon
        val deltaLat = point.lat - lastKept.lat
        if ((deltaLon * deltaLon) + (deltaLat * deltaLat) >= toleranceSquared) {
            simplified += point
            lastKept = point
        }
    }

    val lastPoint = closedPoints.last()
    if (simplified.last() != lastPoint) {
        simplified += lastPoint
    }

    return when {
        simplified.size < 4 -> null
        simplified.first() != simplified.last() -> simplified + simplified.first()
        else -> simplified
    }
}

private fun DrawScope.drawGeoShapes(regionData: BadgeRegionData, color: Color) {
    val bounds = regionData.bounds
    val lonSpan = (bounds.maxLon - bounds.minLon).coerceAtLeast(1.0)
    val latSpan = (bounds.maxLat - bounds.minLat).coerceAtLeast(1.0)
    val padding = size.minDimension * 0.06f
    val scaleX = (size.width - padding * 2f) / lonSpan.toFloat()
    val scaleY = (size.height - padding * 2f) / latSpan.toFloat()
    val scale = minOf(scaleX, scaleY)
    val contentWidth = lonSpan.toFloat() * scale
    val contentHeight = latSpan.toFloat() * scale
    val offsetX = (size.width - contentWidth) / 2f
    val offsetY = (size.height - contentHeight) / 2f

    regionData.shapes.forEach { shape ->
        val path = Path().apply {
            shape.rings.forEach { ring ->
                ring.forEachIndexed { index, point ->
                    val x = ((point.lon - bounds.minLon).toFloat() * scale) + offsetX
                    val y = ((bounds.maxLat - point.lat).toFloat() * scale) + offsetY
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
        }
        drawPath(path = path, color = color)
    }
}

private fun calculateVisualBounds(shapes: List<GeoShape>): ShapeBounds? {
    val boundsByArea = shapes.mapNotNull { shape ->
        val outerRing = shape.rings.firstOrNull().orEmpty()
        if (outerRing.size < 3) return@mapNotNull null

        val area = polygonArea(outerRing)
        if (area <= 0.0) return@mapNotNull null

        ShapeBounds(
            minLon = outerRing.minOf { it.lon },
            maxLon = outerRing.maxOf { it.lon },
            minLat = outerRing.minOf { it.lat },
            maxLat = outerRing.maxOf { it.lat }
        ) to area
    }.sortedByDescending { it.second }

    if (boundsByArea.isEmpty()) return null

    val totalArea = boundsByArea.sumOf { it.second }
    val largestArea = boundsByArea.first().second
    val minimumArea = largestArea * 0.015
    val targetCoverage = totalArea * 0.985
    var coveredArea = 0.0
    val selectedBounds = mutableListOf<ShapeBounds>()

    boundsByArea.forEachIndexed { index, (bounds, area) ->
        val shouldKeep = index == 0 || area >= minimumArea || coveredArea < targetCoverage
        if (shouldKeep) {
            selectedBounds += bounds
            coveredArea += area
        }
    }

    return ShapeBounds(
        minLon = selectedBounds.minOf { it.minLon },
        maxLon = selectedBounds.maxOf { it.maxLon },
        minLat = selectedBounds.minOf { it.minLat },
        maxLat = selectedBounds.maxOf { it.maxLat }
    )
}

private fun fallbackBounds(shapes: List<GeoShape>): ShapeBounds {
    val points = shapes.flatMap { it.rings.flatten() }
    return ShapeBounds(
        minLon = points.minOfOrNull { it.lon } ?: -180.0,
        maxLon = points.maxOfOrNull { it.lon } ?: 180.0,
        minLat = points.minOfOrNull { it.lat } ?: -90.0,
        maxLat = points.maxOfOrNull { it.lat } ?: 90.0
    )
}

private fun polygonArea(points: List<GeoPoint>): Double {
    var area = 0.0
    for (index in 0 until points.lastIndex) {
        val current = points[index]
        val next = points[index + 1]
        area += (current.lon * next.lat) - (next.lon * current.lat)
    }
    return abs(area) / 2.0
}

