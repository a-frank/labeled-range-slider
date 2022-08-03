package de.gnarly.compose.ui.slider

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SliderConfiguration(
	val touchCircleRadius: Dp = 16.dp,
	val touchCircleShadowSize: Dp = 4.dp,
	val touchCircleShadowTouchedSizeAddition: Dp = 2.dp,
	val tickColor: Color = Color.DarkGray,
	val barHeight: Dp = 12.dp,
	val barCornerRadius: Dp = 6.dp,
	val textOffset: Dp = 8.dp,
	val textSize: TextUnit = 16.sp,
	val barColor: Color = Color.Cyan,
	val textColorInRange: Color = Color.Black,
	val textColorOutOfRange: Color = Color.LightGray,
	val touchCircleColor: Color = Color.White,
) {
	context(Density) internal val touchCircleRadiusPx: Float
		get() = touchCircleRadius.toPx()

	context(Density) internal val touchCircleShadowSizePx: Float
		get() = touchCircleShadowSize.toPx()

	context(Density) internal val touchCircleShadowTouchedSizeAdditionPx: Float
		get() = touchCircleShadowTouchedSizeAddition.toPx()

	context(Density) internal val tickCircleRadiusPx: Float
		get() = (barHeight / 4).toPx()

	context(Density) internal val barHeightPx: Float
		get() = barHeight.toPx()

	context(Density) internal val barCornerRadiusPx: Float
		get() = barCornerRadius.toPx()

	context(Density) internal val textSizePx: Float
		get() = textSize.toPx()

	context(Density) internal val textInRangePaint: Paint
		get() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
			color = textColorInRange.toArgb()
			this.textSize = textSizePx
		}

	context(Density) internal val textSelectedPaint: Paint
		get() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
			color = textColorInRange.toArgb()
			this.textSize = textSizePx
			this.typeface = Typeface.DEFAULT_BOLD
		}

	context(Density) internal val textOutOfRangePaint: Paint
		get() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
			color = textColorOutOfRange.toArgb()
			this.textSize = textSizePx
		}
}