package com.example.flagquiz.ui.screen

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import kotlin.math.abs
import kotlin.math.max

internal data class CountryGeoPoint(val lon: Double, val lat: Double)
internal data class CountryGeoShape(val rings: List<List<CountryGeoPoint>>)
internal data class CountryShapeBounds(
    val minLon: Double,
    val maxLon: Double,
    val minLat: Double,
    val maxLat: Double
)
internal data class CountryFeatureData(
    val nameKey: String,
    val shapes: List<CountryGeoShape>,
    val bounds: CountryShapeBounds,
    val primaryBounds: CountryShapeBounds,
    val areaScore: Double
)
internal data class CountryOutlineData(
    val targetShapes: List<CountryGeoShape>,
    val nearbyShapes: List<CountryGeoShape>,
    val viewportBounds: CountryShapeBounds
)

object CountryOutlineCache {
    private val cache = mutableMapOf<String, CountryOutlineData?>()
    private var countryFeatureMap: Map<String, CountryFeatureData>? = null

    fun preloadAll(context: Context) {
        loadCountryFeatureMap(context)
    }

    fun hasGeometry(context: Context, countryName: String): Boolean {
        val featureMap = loadCountryFeatureMap(context)
        return featureMap.containsKey(resolveCountryKey(countryName))
    }

    fun areaScore(context: Context, countryName: String): Double {
        val featureMap = loadCountryFeatureMap(context)
        return featureMap[resolveCountryKey(countryName)]?.areaScore ?: 0.0
    }

    internal fun getOrLoad(context: Context, countryName: String): CountryOutlineData? = synchronized(cache) {
        val cacheKey = resolveCountryKey(countryName)
        cache.getOrPut(cacheKey) {
            val featureMap = loadCountryFeatureMap(context)
            val targetFeature = featureMap[cacheKey] ?: return@getOrPut null
            val viewportBounds = calculateViewportBounds(targetFeature.primaryBounds)
            val nearbyFeatures = findNearbyFeatures(featureMap, targetFeature, viewportBounds)

            CountryOutlineData(
                targetShapes = targetFeature.shapes,
                nearbyShapes = nearbyFeatures.flatMap { it.shapes },
                viewportBounds = viewportBounds
            )
        }
    }

    private fun loadCountryFeatureMap(context: Context): Map<String, CountryFeatureData> {
        countryFeatureMap?.let { return it }

        val json = context.assets.open("regions/countries.geojson").bufferedReader().use { it.readText() }
        val featureCollection = JSONObject(json)
        val features = featureCollection.getJSONArray("features")
        val map = mutableMapOf<String, CountryFeatureData>()

        for (index in 0 until features.length()) {
            val feature = features.getJSONObject(index)
            val countryName = feature.getJSONObject("properties").optString("name")
            if (countryName.isBlank()) continue
            val geometry = feature.optJSONObject("geometry") ?: continue
            val shapes = parseCountryShapes(geometry)
                .map { it.simplified(0.16) }
                .filter { shape -> shape.rings.any { ring -> ring.size >= 4 } }
            if (shapes.isEmpty()) continue

            val bounds = calculateCountryBounds(shapes) ?: continue
            val primaryBounds = calculatePrimaryBounds(shapes) ?: bounds
            val key = normalizeCountryName(countryName)
            map[key] = CountryFeatureData(
                nameKey = key,
                shapes = shapes,
                bounds = bounds,
                primaryBounds = primaryBounds,
                areaScore = calculateAreaScore(shapes)
            )
        }

        return map.also { countryFeatureMap = it }
    }

    private fun parseCountryShapes(geometry: JSONObject): List<CountryGeoShape> {
        return when (geometry.optString("type")) {
            "Polygon" -> listOf(CountryGeoShape(parseCountryPolygonCoordinates(geometry.getJSONArray("coordinates"))))
            "MultiPolygon" -> {
                val multiPolygon = geometry.getJSONArray("coordinates")
                buildList {
                    for (multiIndex in 0 until multiPolygon.length()) {
                        add(CountryGeoShape(parseCountryPolygonCoordinates(multiPolygon.getJSONArray(multiIndex))))
                    }
                }
            }
            else -> emptyList()
        }
    }

    private fun findNearbyFeatures(
        featureMap: Map<String, CountryFeatureData>,
        targetFeature: CountryFeatureData,
        viewportBounds: CountryShapeBounds
    ): List<CountryFeatureData> {
        return featureMap.values
            .asSequence()
            .filter { it.nameKey != targetFeature.nameKey }
            .filter { feature -> feature.bounds.intersects(viewportBounds) }
            .filter { feature -> feature.bounds.area >= 0.005 || feature.bounds.distanceTo(targetFeature.primaryBounds) < 24.0 }
            .sortedBy { feature -> feature.bounds.distanceTo(targetFeature.primaryBounds) }
            .take(140)
            .toList()
    }

    private fun calculateViewportBounds(focusBounds: CountryShapeBounds): CountryShapeBounds {
        val paddedTarget = focusBounds.expandedBy(
            lonPadding = dynamicPadding(focusBounds.width, 10.0, 34.0),
            latPadding = dynamicPadding(focusBounds.height, 8.0, 24.0)
        )
        return paddedTarget.centeredScaled(
            widthMultiplier = 3.45,
            heightMultiplier = 3.1,
            minLonSpan = 52.0,
            minLatSpan = 36.0
        )
    }

    private fun dynamicPadding(span: Double, minPadding: Double, maxPadding: Double): Double {
        if (span <= 0.0) return minPadding
        return (span * 0.28).coerceIn(minPadding, maxPadding)
    }

    private fun resolveCountryKey(countryName: String): String {
        val normalized = normalizeCountryName(countryName)
        return countryAliases[normalized] ?: normalized
    }
}

@Composable
fun CountryOutlineCard(
    countryName: String,
    zoomFactor: Float = 1f
) {
    val context = LocalContext.current
    val outlineData = remember(countryName) { CountryOutlineCache.getOrLoad(context, countryName) }

    Box(
        modifier = Modifier
            .size(width = 340.dp, height = 250.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(28.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (outlineData == null) {
            Text(
                text = countryName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCountryContext(outlineData, zoomFactor)
            }
        }
    }
}

private fun parseCountryPolygonCoordinates(ringsArray: JSONArray): List<List<CountryGeoPoint>> {
    val rings = mutableListOf<List<CountryGeoPoint>>()
    for (ringIndex in 0 until ringsArray.length()) {
        val ringArray = ringsArray.getJSONArray(ringIndex)
        val points = mutableListOf<CountryGeoPoint>()
        for (pointIndex in 0 until ringArray.length()) {
            val point = ringArray.getJSONArray(pointIndex)
            points += CountryGeoPoint(lon = point.getDouble(0), lat = point.getDouble(1))
        }
        rings += points
    }
    return rings
}

private fun CountryGeoShape.simplified(tolerance: Double): CountryGeoShape {
    val simplifiedRings = rings.mapNotNull { ring ->
        simplifyCountryRing(ring, tolerance)?.takeIf { it.size >= 4 }
    }
    return CountryGeoShape(simplifiedRings)
}

private fun simplifyCountryRing(points: List<CountryGeoPoint>, tolerance: Double): List<CountryGeoPoint>? {
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

private fun DrawScope.drawCountryContext(
    outlineData: CountryOutlineData,
    zoomFactor: Float
) {
    val drawBounds = outlineData.viewportBounds.zoomed(zoomFactor)
    drawCountryShapes(
        shapes = outlineData.nearbyShapes,
        bounds = drawBounds,
        fillColor = Color(0xFFCAD4DE),
        strokeColor = Color(0xFF6C7A89),
        strokeWidthFactor = 0.0045f
    )
    drawCountryShapes(
        shapes = outlineData.targetShapes,
        bounds = drawBounds,
        fillColor = Color(0xFF0A66D9),
        strokeColor = Color(0xFF062C5A),
        strokeWidthFactor = 0.0065f
    )
}

private fun DrawScope.drawCountryShapes(
    shapes: List<CountryGeoShape>,
    bounds: CountryShapeBounds,
    fillColor: Color,
    strokeColor: Color,
    strokeWidthFactor: Float
) {
    val padding = size.minDimension * 0.08f
    val lonSpan = bounds.width.coerceAtLeast(1.0)
    val latSpan = bounds.height.coerceAtLeast(1.0)
    val scaleX = (size.width - padding * 2f) / lonSpan.toFloat()
    val scaleY = (size.height - padding * 2f) / latSpan.toFloat()
    val scale = minOf(scaleX, scaleY)
    val contentSize = Size(
        width = lonSpan.toFloat() * scale,
        height = latSpan.toFloat() * scale
    )
    val offsetX = (size.width - contentSize.width) / 2f
    val offsetY = (size.height - contentSize.height) / 2f
    val strokeWidth = size.minDimension * strokeWidthFactor

    shapes.forEach { shape ->
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
        drawPath(path = path, color = fillColor)
        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = strokeWidth, join = StrokeJoin.Round, cap = StrokeCap.Round)
        )
    }
}

private fun calculateCountryBounds(shapes: List<CountryGeoShape>): CountryShapeBounds? {
    val points = shapes.flatMap { it.rings.flatten() }
    if (points.isEmpty()) return null
    return CountryShapeBounds(
        minLon = points.minOf { it.lon },
        maxLon = points.maxOf { it.lon },
        minLat = points.minOf { it.lat },
        maxLat = points.maxOf { it.lat }
    )
}

private fun calculatePrimaryBounds(shapes: List<CountryGeoShape>): CountryShapeBounds? {
    return shapes
        .mapNotNull { shape -> calculateCountryBounds(listOf(shape)) }
        .maxByOrNull { it.area }
}

private fun calculateAreaScore(shapes: List<CountryGeoShape>): Double {
    return shapes.sumOf { shape ->
        shape.rings.foldIndexed(0.0) { index, total, ring ->
            val ringArea = calculateRingArea(ring)
            if (index == 0) total + ringArea else total - ringArea
        }
    }.coerceAtLeast(0.0)
}

private fun calculateRingArea(points: List<CountryGeoPoint>): Double {
    if (points.size < 3) return 0.0
    var area = 0.0
    for (index in points.indices) {
        val current = points[index]
        val next = points[(index + 1) % points.size]
        area += (current.lon * next.lat) - (next.lon * current.lat)
    }
    return abs(area) / 2.0
}

private fun CountryShapeBounds.expandedBy(lonPadding: Double, latPadding: Double): CountryShapeBounds = CountryShapeBounds(
    minLon = minLon - lonPadding,
    maxLon = maxLon + lonPadding,
    minLat = minLat - latPadding,
    maxLat = maxLat + latPadding
)

private fun CountryShapeBounds.centeredScaled(
    widthMultiplier: Double,
    heightMultiplier: Double,
    minLonSpan: Double,
    minLatSpan: Double
): CountryShapeBounds {
    val centerLon = (minLon + maxLon) / 2.0
    val centerLat = (minLat + maxLat) / 2.0
    val halfWidth = max(width * widthMultiplier / 2.0, minLonSpan / 2.0)
    val halfHeight = max(height * heightMultiplier / 2.0, minLatSpan / 2.0)
    return CountryShapeBounds(
        minLon = centerLon - halfWidth,
        maxLon = centerLon + halfWidth,
        minLat = centerLat - halfHeight,
        maxLat = centerLat + halfHeight
    )
}

private fun CountryShapeBounds.zoomed(zoomFactor: Float): CountryShapeBounds {
    val safeZoom = zoomFactor.coerceIn(0.55f, 3.0f)
    val centerLon = (minLon + maxLon) / 2.0
    val centerLat = (minLat + maxLat) / 2.0
    val halfWidth = width / safeZoom / 2.0
    val halfHeight = height / safeZoom / 2.0
    return CountryShapeBounds(
        minLon = centerLon - halfWidth,
        maxLon = centerLon + halfWidth,
        minLat = centerLat - halfHeight,
        maxLat = centerLat + halfHeight
    )
}

private fun CountryShapeBounds.intersects(other: CountryShapeBounds): Boolean {
    return minLon <= other.maxLon && maxLon >= other.minLon && minLat <= other.maxLat && maxLat >= other.minLat
}

private fun CountryShapeBounds.distanceTo(other: CountryShapeBounds): Double {
    val horizontalGap = when {
        maxLon < other.minLon -> other.minLon - maxLon
        other.maxLon < minLon -> minLon - other.maxLon
        else -> 0.0
    }
    val verticalGap = when {
        maxLat < other.minLat -> other.minLat - maxLat
        other.maxLat < minLat -> minLat - other.maxLat
        else -> 0.0
    }
    return abs(horizontalGap) + abs(verticalGap)
}

private val CountryShapeBounds.width: Double
    get() = maxLon - minLon

private val CountryShapeBounds.height: Double
    get() = maxLat - minLat

private val CountryShapeBounds.area: Double
    get() = width * height

private fun normalizeCountryName(name: String): String {
    val normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
        .replace("[^\\p{ASCII}]".toRegex(), "")
        .lowercase()
    return normalized
        .replace("&", "and")
        .replace("[^a-z0-9]+".toRegex(), " ")
        .trim()
}

private val countryAliases = mapOf(
    normalizeCountryName("Congo - Kinshasa") to normalizeCountryName("Democratic Republic of the Congo"),
    normalizeCountryName("Congo - Brazzaville") to normalizeCountryName("Republic of the Congo"),
    normalizeCountryName("Cote d'Ivoire") to normalizeCountryName("Ivory Coast"),
    normalizeCountryName("Eswatini") to normalizeCountryName("eSwatini"),
    normalizeCountryName("Timor-Leste") to normalizeCountryName("East Timor"),
    normalizeCountryName("Micronesia") to normalizeCountryName("Federated States of Micronesia"),
    normalizeCountryName("United States") to normalizeCountryName("United States of America"),
    normalizeCountryName("Vatican City") to normalizeCountryName("Vatican"),
    normalizeCountryName("Sao Tome and Principe") to normalizeCountryName("Sao Tome and Principe")
)
