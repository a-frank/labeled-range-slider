package de.lex.compose

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Slider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContent {
			Box(modifier = Modifier.padding(8.dp)) {
				val steps = (0..100).step(10)
				CustomSlider(steps.toSet(), Modifier.fillMaxWidth())
			}
		}
	}
}

@Composable
fun <T> CustomSlider(steps: Set<T>, modifier: Modifier = Modifier) {
	require(steps.size > 2)

	val interactionSource = remember { MutableInteractionSource() }

	var touchInteractionState by remember {
		mutableStateOf<TouchInteraction>(TouchInteraction.NoInteraction)
	}

	var leftPositionOffset by remember {
		mutableStateOf(0f)
	}

	var rightPositionOffset by remember {
		mutableStateOf(0f)
	}

	Canvas(modifier = modifier
		.height(50.dp)
		.pointerInput(interactionSource) {
			forEachGesture {
				awaitPointerEventScope {

					// Wait for at least one pointer to press down, and set first contact position
					val down: PointerInputChange = awaitFirstDown()
					touchInteractionState = TouchInteraction.Down(down.position)


					do {
						// This PointerEvent contains details including events, id, position and more
						val event: PointerEvent = awaitPointerEvent()

						event.changes
							.forEachIndexed { index: Int, pointerInputChange: PointerInputChange ->
								// This necessary to prevent other gestures or scrolling
								// when at least one pointer is down on canvas to draw
								if (pointerInputChange.positionChange() != Offset.Zero) pointerInputChange.consume()
							}

						touchInteractionState =
							TouchInteraction.Move(event.changes.first().position)
					} while (event.changes.any { it.pressed })

					touchInteractionState =
						TouchInteraction.Up(currentEvent.changes.first().position)

					touchInteractionState = TouchInteraction.NoInteraction
				}
			}
		}
	) {
		val touchCircleRadius = 15.dp.toPx()
		val tickCircleRadius = 4.dp.toPx()
		val tickSpacing = (size.width - 2 * touchCircleRadius) / (steps.size - 1)
		val yCenter = size.height / 2
		val barHeight = 10.dp.toPx()

		val textYOffset = 50f
		val textSize = 16.sp.toPx()
		val textColorInRange = android.graphics.Color.rgb(0, 0, 0)
		val textPaintInRange = Paint(Paint.ANTI_ALIAS_FLAG).apply {
			color = textColorInRange
			this.textSize = textSize
		}
		val textPaintSelected = Paint(Paint.ANTI_ALIAS_FLAG).apply {
			color = textColorInRange
			this.textSize = textSize
			this.typeface = Typeface.DEFAULT_BOLD
		}
		val textPaintOutOfRange = Paint(Paint.ANTI_ALIAS_FLAG).apply {
			color = android.graphics.Color.rgb(200, 200, 200)
			this.textSize = textSize
		}

		val barColor = Color(255, 150, 0)

		val circleColor = Color.Gray
		val leftCircleStartPosition = Offset(touchCircleRadius, yCenter)
		val rightCircleStartPosition = Offset(size.width - touchCircleRadius, yCenter)
		val leftCircleCurrentPosition =
			leftCircleStartPosition.copy(x = leftCircleStartPosition.x + leftPositionOffset)

		drawLine(
			barColor,
			Offset(touchCircleRadius, yCenter),
			Offset(size.width - touchCircleRadius, yCenter),
			barHeight
		)

		var tickOffset = touchCircleRadius

		steps.forEach { step ->
			val tickCenter = Offset(tickOffset, yCenter)

			val isCurrentlySelectedByLeftCircle =
				(leftCircleStartPosition.x > (tickCenter.x - tickCircleRadius / 2)) &&
						(leftCircleStartPosition.x < (tickCenter.x + tickCircleRadius / 2))
			val isCurrentlySelectedByRightCircle =
				(rightCircleStartPosition.x > (tickCenter.x - tickCircleRadius / 2)) &&
						(rightCircleStartPosition.x < (tickCenter.x + tickCircleRadius / 2))

			val paint = if (isCurrentlySelectedByLeftCircle || isCurrentlySelectedByRightCircle) {
				textPaintSelected
			} else if (tickCenter.x < leftCircleStartPosition.x || tickCenter.x > rightCircleStartPosition.x) {
				textPaintOutOfRange
			} else {
				textPaintInRange
			}

			drawCircle(
				Color.Black,
				radius = tickCircleRadius,
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
					tickCenter.x - (stepText.length * textSize) / 3,
					tickCenter.y - textYOffset,
					paint
				)
			}

			tickOffset += tickSpacing
		}

		drawCircle(
			circleColor,
			radius = touchCircleRadius,
			center = leftCircleCurrentPosition,
		)

		drawCircle(
			circleColor,
			radius = touchCircleRadius,
			center = rightCircleStartPosition
		)
	}
}

@Preview
@Composable
fun CustomSliderPreview() {
	CustomSlider((0..100).step(10).toSet())
}

sealed class TouchInteraction {
	object NoInteraction : TouchInteraction()
	class Down(val position: Offset) : TouchInteraction()
	class Move(val position: Offset) : TouchInteraction()
	class Up(val position: Offset) : TouchInteraction()
}
