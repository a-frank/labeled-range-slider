package de.gnarly.compose.ui.slider

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SliderConfiguration(
	val touchCircleRadius: Dp = 16.dp,
	val tickColor: Color = Color.DarkGray,
	val barHeight: Dp = 12.dp,
	val textOffset: Dp = 4.dp,
	val textSize: TextUnit = 16.sp,
	val barColor: Color = Color.Cyan,
	val textColorInRange: Color = Color.Black,
	val textColorOutOfRange: Color = Color.LightGray,
	val touchCircleColor: Color = Color.White,
) {
	context(DrawScope)
			internal val touchCircleRadiusPx: Float
		get() = touchCircleRadius.toPx()

	context(DrawScope)
			internal val tickCircleRadiusPx: Float
		get() = (barHeight / 4).toPx()

	context(DrawScope)
			internal val barHeightPx: Float
		get() = barHeight.toPx()

	context(DrawScope)
			internal val textSizePx: Float
		get() = textSize.toPx()

	context(DrawScope)
			internal val textInRangePaint: Paint
		get() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
			color = textColorInRange.toArgb()
			this.textSize = textSizePx
		}

	context(DrawScope)
			internal val textSelectedPaint: Paint
		get() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
			color = textColorInRange.toArgb()
			this.textSize = textSizePx
			this.typeface = Typeface.DEFAULT_BOLD
		}

	context(DrawScope)
			internal val textOutOfRangePaint: Paint
		get() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
			color = textColorOutOfRange.toArgb()
			this.textSize = textSizePx
		}
}