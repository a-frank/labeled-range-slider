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
	sliderConfig: SliderConfiguration = SliderConfiguration()
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

	val height = remember(key1 = sliderConfig) { sliderConfig.touchCircleRadius * 2 + sliderConfig.textSize.value.dp + sliderConfig.textOffset }
	val barYCenter = sizeAndDensity.derive { composableSize.height - sliderConfig.touchCircleRadiusPx }
	val barXStart = sizeAndDensity.derive { sliderConfig.touchCircleRadiusPx - sliderConfig.stepMarkerRadiusPx }
	val barYStart = sizeAndDensity.derive { barYCenter - sliderConfig.barHeightPx / 2 }
	val barWidth = sizeAndDensity.derive { composableSize.width - 2 * barXStart }
	val barCornerRadius = sizeAndDensity.derive { CornerRadius(sliderConfig.barCornerRadiusPx, sliderConfig.barCornerRadiusPx) }

	val (stepXCoordinates, stepSpacing) = sizeAndDensity.derive(steps) {
		calculateStepCoordinatesAndSpacing(
			numberOfSteps = steps.size,
			barXStart = barXStart,
			barWidth = barWidth,
			stepMarkerRadius = sliderConfig.stepMarkerRadiusPx
		)
	}

	var leftCirclePosition by remember(key1 = composableSize) {
		val lowerBoundIdx = steps.indexOf(selectedLowerBound)
		mutableStateOf(Offset(stepXCoordinates[lowerBoundIdx], barYCenter))
	}
	var rightCirclePosition by remember(key1 = composableSize) {
		val upperBoundIdx = steps.indexOf(selectedUpperBound)
		mutableStateOf(Offset(stepXCoordinates[upperBoundIdx], barYCenter))
	}

	Canvas(
		modifier = modifier
			.height(height)
			.onSizeChanged {
				composableSize = it
			}
			.touchInteraction(remember { MutableInteractionSource() }) {
				touchInteractionState = it
			}
	) {
		drawRoundRect(
			color = sliderConfig.barColor,
			topLeft = Offset(barXStart, barYStart),
			size = Size(barWidth, sliderConfig.barHeightPx),
			cornerRadius = barCornerRadius
		)

		drawRect(
			color = sliderConfig.barColorInRange,
			topLeft = Offset(leftCirclePosition.x, barYStart),
			size = Size(rightCirclePosition.x - leftCirclePosition.x, sliderConfig.barHeightPx)
		)

		drawStepMarkersAndLabels(
			steps = steps,
			stepXCoordinates = stepXCoordinates,
			leftCirclePosition = leftCirclePosition,
			rightCirclePosition = rightCirclePosition,
			barYCenter = barYCenter,
			sliderConfig = sliderConfig
		)

		drawCircleWithShadow(
			leftCirclePosition,
			false,
			sliderConfig
		)

		drawCircleWithShadow(
			rightCirclePosition,
			false,
			sliderConfig
		)
	}

	handleTouch(
		leftCirclePosition = leftCirclePosition,
		rightCirclePosition = rightCirclePosition,
		moveLeft = moveLeft,
		moveRight = moveRight,
		stepXCoordinates = stepXCoordinates,
		stepSpacing = stepSpacing,
		touchInteraction = touchInteractionState,
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

private fun calculateNewLeftCirclePosition(
	touchPositionX: Float,
	leftCirclePosition: Offset,
	rightCirclePosition: Offset,
	stepSpacing: Float,
	firstStepXPosition: Float
): Offset = when {
	touchPositionX < firstStepXPosition                    -> leftCirclePosition.copy(x = firstStepXPosition)
	touchPositionX > (rightCirclePosition.x - stepSpacing) -> leftCirclePosition
	else                                                   -> leftCirclePosition.copy(x = touchPositionX)
}

private fun calculateNewRightCirclePosition(
	touchPositionX: Float,
	leftCirclePosition: Offset,
	rightCirclePosition: Offset,
	stepSpacing: Float,
	lastStepXPosition: Float
): Offset = when {
	touchPositionX > lastStepXPosition                    -> rightCirclePosition.copy(x = lastStepXPosition)
	touchPositionX < (leftCirclePosition.x + stepSpacing) -> rightCirclePosition
	else                                                  -> rightCirclePosition.copy(x = touchPositionX)
}

private fun DrawScope.drawCircleWithShadow(
	position: Offset,
	touched: Boolean,
	sliderConfig: SliderConfiguration
) {
	val touchAddition = if (touched) {
		sliderConfig.touchCircleShadowTouchedSizeAdditionPx
	} else {
		0f
	}

	drawIntoCanvas {
		val paint = androidx.compose.ui.graphics.Paint()
		val frameworkPaint = paint.asFrameworkPaint()
		frameworkPaint.color = sliderConfig.touchCircleColor.toArgb()
		frameworkPaint.setShadowLayer(
			sliderConfig.touchCircleShadowSizePx + touchAddition,
			0f,
			0f,
			Color.DarkGray.toArgb()
		)
		it.drawCircle(
			position,
			sliderConfig.touchCircleRadiusPx,
			paint
		)
	}
}

private fun <T> DrawScope.drawStepMarkersAndLabels(
	steps: List<T>,
	stepXCoordinates: FloatArray,
	leftCirclePosition: Offset,
	rightCirclePosition: Offset,
	barYCenter: Float,
	sliderConfig: SliderConfiguration
) {
	require(steps.size == stepXCoordinates.size) { "Step value size and step coordinate size do not match. Value size: ${steps.size}, Coordinate size: ${stepXCoordinates.size}" }

	steps.forEachIndexed { index, step ->
		val stepMarkerCenter = Offset(stepXCoordinates[index], barYCenter)

		val isCurrentlySelectedByLeftCircle =
			(leftCirclePosition.x > (stepMarkerCenter.x - sliderConfig.stepMarkerRadiusPx / 2)) &&
					(leftCirclePosition.x < (stepMarkerCenter.x + sliderConfig.stepMarkerRadiusPx / 2))
		val isCurrentlySelectedByRightCircle =
			(rightCirclePosition.x > (stepMarkerCenter.x - sliderConfig.stepMarkerRadiusPx / 2)) &&
					(rightCirclePosition.x < (stepMarkerCenter.x + sliderConfig.stepMarkerRadiusPx / 2))

		val paint = when {
			isCurrentlySelectedByLeftCircle || isCurrentlySelectedByRightCircle                     -> sliderConfig.textSelectedPaint
			stepMarkerCenter.x < leftCirclePosition.x || stepMarkerCenter.x > rightCirclePosition.x -> sliderConfig.textOutOfRangePaint
			else                                                                                    -> sliderConfig.textInRangePaint
		}

		drawCircle(
			color = sliderConfig.stepMarkerColor,
			radius = sliderConfig.stepMarkerRadiusPx,
			alpha = .1f,
			center = stepMarkerCenter
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
				stepMarkerCenter.x - (stepText.length * sliderConfig.textSizePx) / 3,
				sliderConfig.textSizePx,
				paint
			)
		}
	}
}

private fun calculateStepCoordinatesAndSpacing(
	numberOfSteps: Int,
	barXStart: Float,
	barWidth: Float,
	stepMarkerRadius: Float,
): Pair<FloatArray, Float> {
	val stepOffset = barXStart + stepMarkerRadius
	val stepSpacing = (barWidth - 2 * stepMarkerRadius) / (numberOfSteps - 1)

	val stepXCoordinates = generateSequence(stepOffset) { it + stepSpacing }
		.take(numberOfSteps)
		.toList()

	return stepXCoordinates.toFloatArray() to stepSpacing
}

private fun handleTouch(
	leftCirclePosition: Offset,
	rightCirclePosition: Offset,
	moveLeft: Boolean,
	moveRight: Boolean,
	stepXCoordinates: FloatArray,
	stepSpacing: Float,
	touchInteraction: TouchInteraction,
	updateLeft: (Offset, Boolean) -> Unit,
	updateRight: (Offset, Boolean) -> Unit,
	onTouchInteractionChanged: (TouchInteraction) -> Unit,
	onRangeIdxChanged: (Int, Int) -> Unit
) {
	when (touchInteraction) {
		is TouchInteraction.Move -> {
			val touchPositionX = touchInteraction.position.x
			if (abs(touchPositionX - leftCirclePosition.x) < abs(touchPositionX - rightCirclePosition.x)) {
				val leftPosition = calculateNewLeftCirclePosition(touchPositionX, leftCirclePosition, rightCirclePosition, stepSpacing, stepXCoordinates.first())
				updateLeft(leftPosition, true)

				if (moveRight) {
					moveToClosestStep(rightCirclePosition, stepXCoordinates) { position, move -> updateRight(position, move) }
				}
			} else {
				val rightPosition = calculateNewRightCirclePosition(touchPositionX, leftCirclePosition, rightCirclePosition, stepSpacing, stepXCoordinates.last())
				updateRight(rightPosition, true)

				if (moveLeft) {
					moveToClosestStep(leftCirclePosition, stepXCoordinates) { position, move -> updateLeft(position, move) }
				}
			}
		}
		is TouchInteraction.Up   -> {
			val (closestLeftValue, closestLeftIndex) = stepXCoordinates.getClosestNumber(leftCirclePosition.x)
			val (closestRightValue, closestRightIndex) = stepXCoordinates.getClosestNumber(rightCirclePosition.x)
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

private fun moveToClosestStep(circlePosition: Offset, stepXCoordinates: FloatArray, update: (Offset, Boolean) -> Unit) {
	val (closestRightValue, _) = stepXCoordinates.getClosestNumber(circlePosition.x)
	val updatedPosition = circlePosition.copy(x = closestRightValue)
	update(updatedPosition, false)
}

@Composable
private fun <T> Pair<IntSize, Density>.derive(additionalKey: Any? = null, block: Density.() -> T): T =
	remember(key1 = first, key2 = additionalKey) {
		second.block()
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

@Preview
@Composable
fun CustomSliderPreview() {
	LabeledRangeSlider(0, 100, (0..100).step(10).toList(), { _, _ -> })
}