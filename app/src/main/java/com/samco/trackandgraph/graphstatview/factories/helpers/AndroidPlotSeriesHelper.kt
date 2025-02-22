package com.samco.trackandgraph.graphstatview.factories.helpers

import com.androidplot.Region
import com.androidplot.util.SeriesUtils
import com.androidplot.xy.FastXYSeries
import com.androidplot.xy.RectRegion
import javax.inject.Inject
import kotlin.math.abs

class AndroidPlotSeriesHelper @Inject constructor() {
    internal fun getFastXYSeries(name: String, xValues: List<Number>, yValues: List<Number>): FastXYSeries {

        var yRegion = SeriesUtils.minMax(yValues)
        if (abs(yRegion.min.toDouble() - yRegion.max.toDouble()) < 0.1)
            yRegion = Region(yRegion.min, yRegion.min.toDouble() + 0.1)

        val xRegion = SeriesUtils.minMax(xValues)
        val rectRegion = RectRegion(xRegion.min, xRegion.max, yRegion.min, yRegion.max)

        return object : FastXYSeries {
            override fun minMax() = rectRegion
            override fun getX(index: Int): Number = xValues[index]
            override fun getY(index: Int): Number = yValues[index]
            override fun getTitle() = name
            override fun size() = xValues.size
        }
    }
}