package de.lex.compose

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
	val touchCircleRadiusDp: Dp = 16.dp,
	private val touchToleranceDp: Dp = 24.dp,
	private val tickCircleRadiusDp: Dp = 4.dp,
	val tickColor: Color = Color.DarkGray,
	private val barHeightDp: Dp = 16.dp,
	val textOffsetDp: Dp = 24.dp,
	val textSizeSp: TextUnit = 16.sp,
	val barColor: Color = Color.Cyan,
	val textColorInRange: Color = Color.Black,
	val textColorOutOfRange: Color = Color.LightGray,
	val touchCircleColor: Color = Color.White,
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