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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun <T> LabeledRangeSlider(
	steps: List<T>,
	onRangeChanged: (lower: T, upper: T) -> Unit,
	modifier: Modifier = Modifier,
	sliderConfiguration: SliderConfiguration = SliderConfiguration()
) {
	require(steps.size > 2)

	var touchInteractionState by remember { mutableStateOf<TouchInteraction>(TouchInteraction.NoInteraction) }
	var moveLeft by remember { mutableStateOf(false) }
	var moveRight by remember { mutableStateOf(false) }
	var leftXPosition by remember { mutableStateOf(sliderConfiguration.touchCircleRadiusDp) }
	var rightXPosition by remember { mutableStateOf(0.dp) }

	val height = sliderConfiguration.touchCircleRadiusDp + sliderConfiguration.textOffsetDp + sliderConfiguration.textSizeSp.value.dp

	Canvas(modifier = modifier
		.height(height)
		.touchInteraction(remember { MutableInteractionSource() }) {
			touchInteractionState = it
		}
	) {
		val barYCenter = size.height - sliderConfiguration.touchCircleRadius

		if (rightXPosition.value == 0f) {
			rightXPosition = size.width.toDp() - sliderConfiguration.touchCircleRadiusDp
		}

		val leftCirclePosition = Offset(leftXPosition.toPx(), barYCenter)
		val rightCirclePosition = Offset(rightXPosition.toPx(), barYCenter)

		val barXStart = sliderConfiguration.touchCircleRadius - sliderConfiguration.tickCircleRadius
		val barWidth = size.width - sliderConfiguration.touchCircleRadius - 2 * sliderConfiguration.tickCircleRadius
		val barTopLeft = Offset(barXStart, barYCenter - sliderConfiguration.barHeight / 2)
		val barCornerRadius = CornerRadius(20f, 20f)
		drawRoundRect(
			color = sliderConfiguration.textColorOutOfRange,
			topLeft = barTopLeft,
			size = Size(barWidth, sliderConfiguration.barHeight),
			cornerRadius = barCornerRadius
		)

		drawRect(
			color = sliderConfiguration.barColor,
			topLeft = Offset(leftCirclePosition.x, barYCenter - sliderConfiguration.barHeight / 2),
			size = Size(rightCirclePosition.x - leftCirclePosition.x, sliderConfiguration.barHeight)
		)

		val (tickXCoordinates, tickSpacing) = drawTicks(
			tickValues = steps,
			barXStart = barXStart,
			barWidth = barWidth,
			sliderConfiguration = sliderConfiguration,
			leftCirclePosition = leftCirclePosition,
			rightCirclePosition = rightCirclePosition,
			barYCenter = barYCenter
		)

		drawCircleWithShadow(
			position = leftCirclePosition,
			radius = sliderConfiguration.touchCircleRadius,
			color = sliderConfiguration.touchCircleColor
		)

		drawCircleWithShadow(
			position = rightCirclePosition,
			radius = sliderConfiguration.touchCircleRadius,
			color = sliderConfiguration.touchCircleColor
		)

		when (val currentState = touchInteractionState) {
			is TouchInteraction.Down          -> {
				val touchPositionX = currentState.position.x
				if (abs(touchPositionX - leftXPosition.toPx()) < abs(touchPositionX - rightXPosition.toPx())) {
					leftXPosition = if (touchPositionX < (sliderConfiguration.touchCircleRadius / 2)) {
						sliderConfiguration.touchCircleRadiusDp
					} else if (touchPositionX > (rightCirclePosition.x - tickSpacing)) {
						leftXPosition
					} else {
						touchPositionX.toDp()
					}
					moveLeft = true
				} else {
					rightXPosition = if (touchPositionX > (size.width.toDp() - sliderConfiguration.touchCircleRadiusDp).toPx()) {
						size.width.toDp() - sliderConfiguration.touchCircleRadiusDp
					} else if (touchPositionX < (leftCirclePosition.x + tickSpacing)) {
						rightXPosition
					} else {
						touchPositionX.toDp()
					}
					moveRight = true
				}
			}
			is TouchInteraction.Move          -> {
				val touchPositionX = currentState.position.x
				if (moveLeft) {
					leftXPosition = if (touchPositionX < (sliderConfiguration.touchCircleRadius / 2)) {
						sliderConfiguration.touchCircleRadiusDp
					} else if (touchPositionX > (rightCirclePosition.x - tickSpacing)) {
						leftXPosition
					} else {
						touchPositionX.toDp()
					}
				}
				if (moveRight) {
					rightXPosition = if (touchPositionX > (size.width.toDp() - sliderConfiguration.touchCircleRadiusDp).toPx()) {
						size.width.toDp() - sliderConfiguration.touchCircleRadiusDp
					} else if (touchPositionX < (leftCirclePosition.x + tickSpacing)) {
						rightXPosition
					} else {
						touchPositionX.toDp()
					}
				}
			}
			is TouchInteraction.NoInteraction -> {
				val (closestLeftValue, closestLeftIndex) = tickXCoordinates.getClosestNumber(leftXPosition.toPx())
				val (closestRightValue, closestRightIndex) = tickXCoordinates.getClosestNumber(rightXPosition.toPx())
				if (moveLeft) {
					leftXPosition = closestLeftValue.toDp()
					onRangeChanged(steps[closestLeftIndex], steps[closestRightIndex])
				}

				if (moveRight) {
					rightXPosition = closestRightValue.toDp()
					onRangeChanged(steps[closestLeftIndex], steps[closestRightIndex])
				}

				moveLeft = false
				moveRight = false
			}
		}
	}
}

@Preview
@Composable
fun CustomSliderPreview() {
	LabeledRangeSlider((0..100).step(10).toList(), { _, _ -> })
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

private fun <T> DrawScope.drawTicks(
	tickValues: List<T>,
	barXStart: Float,
	barWidth: Float,
	sliderConfiguration: SliderConfiguration,
	leftCirclePosition: Offset,
	rightCirclePosition: Offset,
	barYCenter: Float
): Pair<FloatArray, Float> {
	var tickOffset = barXStart + sliderConfiguration.tickCircleRadius
	val tickSpacing = (barWidth - 2 * sliderConfiguration.tickCircleRadius) / (tickValues.size - 1)

	val tickXCoordinates = mutableListOf<Float>()
	tickValues.forEach { step ->
		tickXCoordinates += tickOffset
		val tickCenter = Offset(tickOffset, barYCenter)

		val isCurrentlySelectedByLeftCircle =
			(leftCirclePosition.x > (tickCenter.x - sliderConfiguration.tickCircleRadius / 2)) &&
					(leftCirclePosition.x < (tickCenter.x + sliderConfiguration.tickCircleRadius / 2))
		val isCurrentlySelectedByRightCircle =
			(rightCirclePosition.x > (tickCenter.x - sliderConfiguration.tickCircleRadius / 2)) &&
					(rightCirclePosition.x < (tickCenter.x + sliderConfiguration.tickCircleRadius / 2))

		val paint = when {
			isCurrentlySelectedByLeftCircle || isCurrentlySelectedByRightCircle         -> sliderConfiguration.textSelectedPaint
			tickCenter.x < leftCirclePosition.x || tickCenter.x > rightCirclePosition.x -> sliderConfiguration.textOutOfRangePaint
			else                                                                        -> sliderConfiguration.textInRangePaint
		}

		drawCircle(
			color = sliderConfiguration.tickColor,
			radius = sliderConfiguration.tickCircleRadius,
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
				tickCenter.x - (stepText.length * sliderConfiguration.textSizeValue) / 3,
				tickCenter.y - sliderConfiguration.textOffset,
				paint
			)
		}

		tickOffset += tickSpacing
	}
	return tickXCoordinates.toFloatArray() to tickSpacing
}

private fun DrawScope.drawCircleWithShadow(position: Offset, radius: Float, color: Color) {
	val transparentColor = Color.Transparent.value.toInt()

	drawIntoCanvas {
		val paint = androidx.compose.ui.graphics.Paint()
		val frameworkPaint = paint.asFrameworkPaint()
		frameworkPaint.color = transparentColor
		frameworkPaint.setShadowLayer(
			2.dp.toPx(),
			0f,
			0f,
			Color.DarkGray.toArgb()
		)
		it.drawCircle(
			position,
			radius,
			paint
		)
	}

	drawCircle(
		color = color,
		radius = radius,
		center = position
	)
}