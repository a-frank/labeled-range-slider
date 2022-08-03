package de.gnarly.compose.ui.slider

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun <T : Number> LabeledRangeSlider(
	selectedLowerBound: T,
	selectedUpperBound: T,
	steps: List<T>,
	onRangeChanged: (lower: T, upper: T) -> Unit,
	modifier: Modifier = Modifier,
	sliderConfiguration: SliderConfiguration = SliderConfiguration()
) {
	require(steps.size > 2) { "List of steps has to be at least of size 2" }
	require(steps.contains(selectedLowerBound)) { "selectedLowerBound has to be part of the provided steps" }
	require(steps.contains(selectedUpperBound)) { "selectedUpperBound has to be part of the provided steps" }

	var touchInteractionState by remember { mutableStateOf<TouchInteraction>(TouchInteraction.NoInteraction) }
	var moveLeft by remember { mutableStateOf(false) }
	var moveRight by remember { mutableStateOf(false) }

	var composableSize by remember { mutableStateOf(IntSize(0, 0)) }
	val currentDensity = LocalDensity.current
	val sizeAndDensity = composableSize to currentDensity

	val height = remember(key1 = composableSize) { sliderConfiguration.touchCircleRadius * 2 + sliderConfiguration.textSize.value.dp + sliderConfiguration.textOffset }
	val barCornerRadius = sizeAndDensity.derive { CornerRadius(sliderConfiguration.barCornerRadiusPx, sliderConfiguration.barCornerRadiusPx) }
	val barYCenter = sizeAndDensity.derive { composableSize.height - sliderConfiguration.touchCircleRadiusPx }
	val barXStart = sizeAndDensity.derive { sliderConfiguration.touchCircleRadiusPx - sliderConfiguration.tickCircleRadiusPx }
	val barWidth = sizeAndDensity.derive { composableSize.width - 2 * barXStart }
	val barHeight = sizeAndDensity.derive { barYCenter - sliderConfiguration.barHeightPx / 2 }

	val (tickXCoordinates, tickSpacing) = sizeAndDensity.derive(steps) {
		calculateTickCoordinatesAndSpacing(
			numberOfTicks = steps.size,
			barXStart = barXStart,
			barWidth = barWidth,
			tickCircleRadius = sliderConfiguration.tickCircleRadiusPx
		)
	}

	var leftCirclePosition by remember(key1 = composableSize) {
		val lowerBoundIdx = steps.indexOf(selectedLowerBound)
		mutableStateOf(Offset(tickXCoordinates[lowerBoundIdx], barYCenter))
	}
	var rightCirclePosition by remember(key1 = composableSize) {
		val upperBoundIdx = steps.indexOf(selectedUpperBound)
		mutableStateOf(Offset(tickXCoordinates[upperBoundIdx], barYCenter))
	}

	Canvas(
		modifier = modifier
			.height(height)
			.touchInteraction(remember { MutableInteractionSource() }) {
				touchInteractionState = it
			}
			.onSizeChanged {
				composableSize = it
			},
	) {
		drawRoundRect(
			color = sliderConfiguration.textColorOutOfRange,
			topLeft = Offset(barXStart, barHeight),
			size = Size(barWidth, sliderConfiguration.barHeightPx),
			cornerRadius = barCornerRadius
		)

		drawRect(
			color = sliderConfiguration.barColor,
			topLeft = Offset(leftCirclePosition.x, barHeight),
			size = Size(rightCirclePosition.x - leftCirclePosition.x, sliderConfiguration.barHeightPx)
		)

		drawTicksAndLabels(
			tickValues = steps,
			tickXCoordinates = tickXCoordinates,
			sliderConfiguration = sliderConfiguration,
			leftCirclePosition = leftCirclePosition,
			rightCirclePosition = rightCirclePosition,
			barYCenter = barYCenter
		)

		drawCircleWithShadow(
			position = leftCirclePosition,
			touched = moveLeft,
			sliderConfiguration = sliderConfiguration
		)

		drawCircleWithShadow(
			position = rightCirclePosition,
			touched = moveRight,
			sliderConfiguration = sliderConfiguration
		)

		handleTouch(
			leftCirclePosition = leftCirclePosition,
			rightCirclePosition = rightCirclePosition,
			moveLeft = moveLeft,
			moveRight = moveRight,
			tickXCoordinates = tickXCoordinates,
			tickSpacing = tickSpacing,
			touchInteraction = touchInteractionState,
			sliderConfiguration = sliderConfiguration,
			updateLeft = { position, move ->
				leftCirclePosition = position
				moveLeft = move
			},
			updateRight = { position, move ->
				rightCirclePosition = position
				moveRight = move
			},
			onTouchInteractionChanged = { touchInteractionState = it },
			onRangeIdxChanged = { lowerBoundIdx, upperBoundIdx -> onRangeChanged(steps[lowerBoundIdx], steps[upperBoundIdx]) }
		)
	}
}

private fun <T> DrawScope.drawTicksAndLabels(
	tickValues: List<T>,
	tickXCoordinates: FloatArray,
	sliderConfiguration: SliderConfiguration,
	leftCirclePosition: Offset,
	rightCirclePosition: Offset,
	barYCenter: Float
) {
	assert(tickValues.size == tickXCoordinates.size) { "Step value size and step coordinate size do not match. Value size: ${tickValues.size}, Coordinate size: ${tickXCoordinates.size}" }

	tickValues.forEachIndexed { index, step ->
		val tickCenter = Offset(tickXCoordinates[index], barYCenter)

		val isCurrentlySelectedByLeftCircle =
			(leftCirclePosition.x > (tickCenter.x - sliderConfiguration.tickCircleRadiusPx / 2)) &&
					(leftCirclePosition.x < (tickCenter.x + sliderConfiguration.tickCircleRadiusPx / 2))
		val isCurrentlySelectedByRightCircle =
			(rightCirclePosition.x > (tickCenter.x - sliderConfiguration.tickCircleRadiusPx / 2)) &&
					(rightCirclePosition.x < (tickCenter.x + sliderConfiguration.tickCircleRadiusPx / 2))

		val paint = when {
			isCurrentlySelectedByLeftCircle || isCurrentlySelectedByRightCircle -> sliderConfiguration.textSelectedPaint
			tickCenter.x < leftCirclePosition.x || tickCenter.x > rightCirclePosition.x -> sliderConfiguration.textOutOfRangePaint
			else -> sliderConfiguration.textInRangePaint
		}

		drawCircle(
			color = sliderConfiguration.tickColor,
			radius = sliderConfiguration.tickCircleRadiusPx,
			alpha = .1f,
			center = tickCenter
		)

		drawIntoCanvas {
			val stepText = step.toString().let { text ->
				if (text.length > 3) {
					text.substring(0, 2)
				} else {
					text
				}
			}
			it.nativeCanvas.drawText(
				stepText,
				tickCenter.x - (stepText.length * sliderConfiguration.textSizePx) / 3,
				sliderConfiguration.textSizePx,
				paint
			)
		}
	}
}

private fun DrawScope.drawCircleWithShadow(position: Offset, touched: Boolean, sliderConfiguration: SliderConfiguration) {
	val transparentColor = Color.Transparent.value.toInt()

	val touchAddition = if (touched) {
		sliderConfiguration.touchCircleShadowTouchedSizeAdditionPx
	} else {
		0f
	}

	drawIntoCanvas {
		val paint = androidx.compose.ui.graphics.Paint()
		val frameworkPaint = paint.asFrameworkPaint()
		frameworkPaint.color = transparentColor
		frameworkPaint.setShadowLayer(
			sliderConfiguration.touchCircleShadowSizePx + touchAddition,
			0f,
			0f,
			Color.DarkGray.toArgb()
		)
		it.drawCircle(
			position,
			sliderConfiguration.touchCircleRadiusPx,
			paint
		)
	}

	drawCircle(
		color = sliderConfiguration.touchCircleColor,
		radius = sliderConfiguration.touchCircleRadiusPx,
		center = position
	)
}

private fun DrawScope.handleTouch(
	leftCirclePosition: Offset,
	rightCirclePosition: Offset,
	moveLeft: Boolean,
	moveRight: Boolean,
	tickXCoordinates: FloatArray,
	tickSpacing: Float,
	touchInteraction: TouchInteraction,
	sliderConfiguration: SliderConfiguration,
	updateLeft: (Offset, Boolean) -> Unit,
	updateRight: (Offset, Boolean) -> Unit,
	onTouchInteractionChanged: (TouchInteraction) -> Unit,
	onRangeIdxChanged: (Int, Int) -> Unit
) {
	when (touchInteraction) {
		is TouchInteraction.Move -> {
			val touchPositionX = touchInteraction.position.x
			if (abs(touchPositionX - leftCirclePosition.x) < abs(touchPositionX - rightCirclePosition.x)) {
				val leftPosition = calculateNewLeftCirclePosition(touchPositionX, leftCirclePosition, rightCirclePosition, tickSpacing, sliderConfiguration)
				updateLeft(leftPosition, true)

				if (moveRight) {
					moveToClosestTick(rightCirclePosition, tickXCoordinates) { position, move -> updateRight(position, move) }
				}
			} else {
				val rightPosition = calculateNewRightCirclePosition(touchPositionX, leftCirclePosition, rightCirclePosition, tickSpacing, sliderConfiguration)
				updateRight(rightPosition, true)

				if (moveLeft) {
					moveToClosestTick(leftCirclePosition, tickXCoordinates) { position, move -> updateLeft(position, move) }
				}
			}
		}
		is TouchInteraction.Up   -> {
			val (closestLeftValue, closestLeftIndex) = tickXCoordinates.getClosestNumber(leftCirclePosition.x)
			val (closestRightValue, closestRightIndex) = tickXCoordinates.getClosestNumber(rightCirclePosition.x)
			if (moveLeft) {
				val leftPosition = leftCirclePosition.copy(x = closestLeftValue)
				updateLeft(leftPosition, false)
				onRangeIdxChanged(closestLeftIndex, closestRightIndex)
			} else if (moveRight) {
				val rightPosition = rightCirclePosition.copy(x = closestRightValue)
				updateRight(rightPosition, false)
				onRangeIdxChanged(closestLeftIndex, closestRightIndex)
			}

			onTouchInteractionChanged(TouchInteraction.NoInteraction)
		}
		else                     -> {
			// nothing to do
		}
	}
}

private fun moveToClosestTick(circlePosition: Offset, tickXCoordinates: FloatArray, update: (Offset, Boolean) -> Unit) {
	val (closestRightValue, _) = tickXCoordinates.getClosestNumber(circlePosition.x)
	val updatedPosition = circlePosition.copy(x = closestRightValue)
	update(updatedPosition, false)
}

private fun Density.calculateNewLeftCirclePosition(
	touchPositionX: Float,
	leftCirclePosition: Offset,
	rightCirclePosition: Offset,
	tickSpacing: Float,
	sliderConfiguration: SliderConfiguration
): Offset = when {
	touchPositionX < (sliderConfiguration.touchCircleRadiusPx / 2) -> leftCirclePosition.copy(x = sliderConfiguration.touchCircleRadiusPx)
	touchPositionX > (rightCirclePosition.x - tickSpacing)         -> leftCirclePosition
	else                                                           -> leftCirclePosition.copy(x = touchPositionX)

}

private fun DrawScope.calculateNewRightCirclePosition(
	touchPositionX: Float,
	leftCirclePosition: Offset,
	rightCirclePosition: Offset,
	tickSpacing: Float,
	sliderConfiguration: SliderConfiguration
): Offset = when {
	touchPositionX > size.width - sliderConfiguration.touchCircleRadiusPx -> rightCirclePosition.copy(x = size.width - sliderConfiguration.touchCircleRadiusPx)
	touchPositionX < (leftCirclePosition.x + tickSpacing)                 -> rightCirclePosition
	else                                                                  -> rightCirclePosition.copy(x = touchPositionX)
}

private fun calculateTickCoordinatesAndSpacing(
	numberOfTicks: Int,
	barXStart: Float,
	barWidth: Float,
	tickCircleRadius: Float,
): Pair<FloatArray, Float> {
	val tickOffset = barXStart + tickCircleRadius
	val tickSpacing = (barWidth - 2 * tickCircleRadius) / (numberOfTicks - 1)

	val tickXCoordinates = generateSequence(tickOffset) { it + tickSpacing }
		.take(numberOfTicks)
		.toList()

	return tickXCoordinates.toFloatArray() to tickSpacing
}

private fun FloatArray.getClosestNumber(input: Float): Pair<Float, Int> {
	var minElem = this[0]
	var minValue = abs(minElem - input)
	var minIdx = 0
	for (i in 1..lastIndex) {
		val e = this[i]
		val v = abs(e - input)
		if (minValue > v) {
			minElem = e
			minValue = v
			minIdx = i
		}
	}
	return minElem to minIdx
}

@Composable
private fun <T> Pair<IntSize, Density>.derive(additionalKey: Any? = null, block: Density.() -> T): T =
	remember(key1 = first, key2 = additionalKey) {
		second.block()
	}

@Preview
@Composable
fun CustomSliderPreview() {
	LabeledRangeSlider(0, 100, (0..100).step(10).toList(), { _, _ -> })
}