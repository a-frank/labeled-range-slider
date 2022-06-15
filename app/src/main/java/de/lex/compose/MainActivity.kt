package de.lex.compose

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.gnarly.compose.ui.slider.LabeledRangeSlider

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContent {
			Box(modifier = Modifier.padding(16.dp)) {
				val steps = (0..100).step(10)
				LabeledRangeSlider(
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