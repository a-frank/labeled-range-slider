package de.gnarly.compose.ui.slider

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange


sealed class TouchInteraction {
	object NoInteraction : TouchInteraction()
	data class Down(val position: Offset) : TouchInteraction()
	data class Move(val position: Offset) : TouchInteraction()
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