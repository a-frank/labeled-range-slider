package de.lex.compose

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContent {
			Box(modifier = Modifier.padding(16.dp)) {
				val steps = (0..100).step(10)
				CustomSlider(
					steps.toList(),
					onRangeChanged = { lower, upper ->
						Log.i("XXX", "Range changed to $lower - $upper")
					},
					Modifier.fillMaxWidth()
				)
			}
		}
	}
}

@Composable
fun <T> CustomSlider(
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

	Canvas(modifier = modifier
		.height(70.dp)
		.background(Color.Cyan)
		.touchInteraction(remember { MutableInteractionSource() }) {
			touchInteractionState = it
		}
	) {
		val yCenter = size.height / 2

		if (rightXPosition.value == 0f) {
			rightXPosition = size.width.toDp() - sliderConfiguration.touchCircleRadiusDp
		}

		val leftCirclePosition = Offset(leftXPosition.toPx(), yCenter)
		val rightCirclePosition = Offset(rightXPosition.toPx(), yCenter)

		drawRoundRect(
			color = sliderConfiguration.barColor,
			topLeft = Offset(sliderConfiguration.touchCircleRadius / 2, yCenter - sliderConfiguration.barHeight / 2),
			size = Size(size.width - sliderConfiguration.touchCircleRadius / 2, sliderConfiguration.barHeight),
			cornerRadius = CornerRadius(20f, 20f)
		)

		val (tickXCoordinates, tickSpacing) = drawTicks(
			tickValues = steps,
			sliderConfiguration = sliderConfiguration,
			leftCirclePosition = leftCirclePosition,
			rightCirclePosition = rightCirclePosition,
			yCenter
		)

		drawCircle(
			color = sliderConfiguration.touchCircleColor,
			radius = sliderConfiguration.touchCircleRadius,
			center = leftCirclePosition,
		)

		drawCircle(
			color = sliderConfiguration.touchCircleColor,
			radius = sliderConfiguration.touchCircleRadius,
			center = rightCirclePosition
		)

		val currentState = touchInteractionState
		when (currentState) {
			is TouchInteraction.Down          -> {
				val touchPosition = currentState.position
				if (isCircleTouched(touchPosition.x, leftCirclePosition.x, sliderConfiguration.touchCircleRadius, sliderConfiguration.touchTolerance)) {
					moveLeft = true
				}

				if (isCircleTouched(touchPosition.x, rightCirclePosition.x, sliderConfiguration.touchCircleRadius, sliderConfiguration.touchTolerance)) {
					moveRight = true
				}
			}
			is TouchInteraction.Move          -> {
				if (moveLeft) {
					leftXPosition = if (currentState.position.x < (sliderConfiguration.touchCircleRadius / 2)) {
						sliderConfiguration.touchCircleRadiusDp
					} else if (currentState.position.x > (rightCirclePosition.x - tickSpacing)) {
						leftXPosition
					} else {
						currentState.position.x.toDp()
					}
				}
				if (moveRight) {
					rightXPosition = if (currentState.position.x > (size.width.toDp() - sliderConfiguration.touchCircleRadiusDp).toPx()) {
						size.width.toDp() - sliderConfiguration.touchCircleRadiusDp
					} else if (currentState.position.x < (leftCirclePosition.x + tickSpacing)) {
						rightXPosition
					} else {
						currentState.position.x.toDp()
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
			else                              -> {}
		}
	}
}

@Preview
@Composable
fun CustomSliderPreview() {
	CustomSlider((0..100).step(10).toList(), { _, _ -> })
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

fun isCircleTouched(touchX: Float, circlePositionX: Float, circleRadius: Float, touchTolerance: Float): Boolean =
	touchX > (circlePositionX - circleRadius - touchTolerance) && touchX < (circlePositionX + circleRadius + touchTolerance)

data class SliderConfiguration(
	val touchCircleRadiusDp: Dp = 15.dp,
	private val touchToleranceDp: Dp = 16.dp,
	private val tickCircleRadiusDp: Dp = 4.dp,
	private val barHeightDp: Dp = 10.dp,
	private val textOffsetDp: Dp = 22.dp,
	private val textSizeSp: TextUnit = 16.sp,
	val barColor: Color = Color(255, 150, 0),
	val textColorInRange: Color = Color.Black,
	val textColorOutOfRange: Color = Color.LightGray,
	val touchCircleColor: Color = Color.Gray,
) {
	context(DrawScope)
	val touchCircleRadius: Float
		get() = touchCircleRadiusDp.toPx()

	context(DrawScope)
	val touchTolerance: Float
		get() = touchToleranceDp.toPx()

	context(DrawScope)
	val tickCircleRadius: Float
		get() = tickCircleRadiusDp.toPx()

	context(DrawScope)
	val barHeight: Float
		get() = barHeightDp.toPx()

	context(DrawScope)
	val textOffset: Float
		get() = textOffsetDp.toPx()

	context(DrawScope)
	val textSizeValue: Float
		get() = textSizeSp.toPx()

	context(DrawScope)
	val textInRangePaint: Paint
		get() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
			color = textColorInRange.toArgb()
			this.textSize = textSizeValue
		}

	context(DrawScope)
	val textSelectedPaint: Paint
		get() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
			color = textColorInRange.toArgb()
			this.textSize = textSizeValue
			this.typeface = Typeface.DEFAULT_BOLD
		}

	context(DrawScope)
	val textOutOfRangePaint: Paint
		get() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
			color = textColorOutOfRange.toArgb()
			this.textSize = textSizeValue
		}
}

sealed class TouchInteraction {
	object NoInteraction : TouchInteraction()
	data class Down(val position: Offset) : TouchInteraction()
	data class Move(val position: Offset) : TouchInteraction()
	data class Up(val position: Offset) : TouchInteraction()
}

fun Modifier.touchInteraction(key: Any, block: (TouchInteraction) -> Unit): Modifier =
	pointerInput(key) {
		forEachGesture {
			awaitPointerEventScope {

				val down: PointerInputChange = awaitFirstDown()
				block(TouchInteraction.Down(down.position))

				do {
					val event: PointerEvent = awaitPointerEvent()

					event.changes
						.forEach { pointerInputChange: PointerInputChange ->
							if (pointerInputChange.positionChange() != Offset.Zero) pointerInputChange.consume()
						}

					block(TouchInteraction.Move(event.changes.first().position))
				} while (event.changes.any { it.pressed })

				block(TouchInteraction.NoInteraction)
			}
		}
	}

private fun <T> DrawScope.drawTicks(
	tickValues: List<T>,
	sliderConfiguration: SliderConfiguration,
	leftCirclePosition: Offset,
	rightCirclePosition: Offset,
	barYCenter: Float
): Pair<FloatArray, Float> {
	var tickOffset = sliderConfiguration.touchCircleRadius
	val tickSpacing = (size.width - 2 * sliderConfiguration.touchCircleRadius) / (tickValues.size - 1)

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
			color = Color.Black,
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
