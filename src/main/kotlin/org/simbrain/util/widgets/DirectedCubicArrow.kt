package org.simbrain.util.widgets

import org.piccolo2d.PNode
import org.piccolo2d.nodes.PArea
import org.piccolo2d.nodes.PPath
import org.simbrain.util.*
import java.awt.BasicStroke
import java.awt.Color
import java.awt.geom.Line2D
import java.awt.geom.Point2D

/**
 * @author Zoë Tosi
 * @author Leo Yulin Li
 */
class DirectedCubicArrow(
        val thickness: Float = 20.0f,
        val color: Color = Color(0, 255, 0, 128),
        var t: Double = 0.5
) : PNode() {

    /**
     * The triangle at the tip of the arrow. This triangle is constructed only once and during [update] the this
     * triangle will be placed onto the correct location
     */
    private val arrowTip = listOf(point(0, 0), point(0.5, -0.866025), point(-0.5, -0.866025))
            .map { it * (thickness * 2.0) }.toPolygon()
            .let { polygon -> PArea(polygon, null) }
            .apply { paint = Color.GREEN }

    /**
     * Update the shape of the arrow base on the outlines of source and target.
     *
     * @return the updated center location of this arrow
     */
    fun update(sourceOutlines: RectangleOutlines, targetOutlines: RectangleOutlines): Point2D {

        // 0. clear old arrow
        removeAllChildren()

        // 1. for each source and target, find a side of the outline to let the arrow connect
        val (sourceSide, targetSide) = (sourceOutlines.toList() cartesianProduct targetOutlines.toList()).filter { (source, target) ->
            // make sure the curve does not bent backward
            val line = target.headOffset - source.tailOffset
            line dot source.normal > 0 && line dot target.normal < 0
        }.minBy { (source, target) -> source.midPoint distanceSqTo target.midPoint } ?: return point(0, 0)

        // 2. compute the vector from source to target
        val deltaVector = targetSide.headOffset - sourceSide.tailOffset

        // 3. put the arrow tip at the right location and orient it at the right angle
        arrowTip.getTransformReference(true).apply {
            setToTranslation(targetSide.tipOffset.x, targetSide.tipOffset.y)
            val theta = targetSide.normalTheta
            rotate(theta)
        }

        // 4. compute the curve
        val curveModel = cubicBezier(
                sourceSide.tailOffset,
                sourceSide.tailOffset + deltaVector.abs * sourceSide.unitNormal * 0.5,
                targetSide.headOffset + deltaVector.abs * targetSide.unitNormal * 0.5,
                targetSide.headOffset
        )

        val curveView = PPath.Double(curveModel, BasicStroke(thickness)).apply {
            paint = null
            strokePaint = color
        }

        // 5. add shape to node
        addChild(arrowTip)
        addChild(curveView)

        // 6. return the new location of this arrow
        return curveModel.midpoint

    }

    /**
     * Given a side of a rectangle bound, find the location of where an arrow tail would go.
     */
    private val Line2D.tailOffset
        get() = p(t) + unitNormal * (thickness * 2.5)

    /**
     * Given a side of a rectangle bound, find the location of where an arrow head would go.
     */
    private val Line2D.headOffset
        get() = p(1 - t) + unitNormal * (thickness * 2.5)

    /**
     * Given a side of a rectangle bound, find the location of where the tip of an arrow would go.
     */
    private val Line2D.tipOffset
        get() = p(1 - t) + unitNormal * (thickness * 1.0)
}