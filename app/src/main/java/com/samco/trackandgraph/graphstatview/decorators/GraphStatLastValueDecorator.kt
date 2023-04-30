package com.samco.trackandgraph.graphstatview.decorators

import com.samco.trackandgraph.graphstatview.factories.viewdto.ILastValueData
import org.threeten.bp.OffsetDateTime

class GraphStatLastValueDecorator(listMode: Boolean) :
    GraphStatViewDecorator<ILastValueData>(listMode) {

    override fun decorate(view: IDecoratableGraphStatView, data: ILastValueData) {
        TODO("Not yet implemented")
    }

    override fun setTimeMarker(time: OffsetDateTime) {
        TODO("Not yet implemented")
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }
}