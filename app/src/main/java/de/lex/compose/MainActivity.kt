package de.lex.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.gnarly.compose.ui.slider.LabeledRangeSlider

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContent {
			Box(modifier = Modifier.padding(16.dp)) {
				val steps = (0..100).step(10)
				var lowerBound by rememberSaveable { mutableStateOf(10) }
				var upperBound by rememberSaveable { mutableStateOf(90) }

				LabeledRangeSlider(
					selectedLowerBound = lowerBound,
					selectedUpperBound = upperBound,
					steps = steps.toList(),
					onRangeChanged = { lower, upper ->
						lowerBound = lower
						upperBound = upper
					},
					modifier = Modifier.fillMaxWidth()
				)
			}
		}
	}
}