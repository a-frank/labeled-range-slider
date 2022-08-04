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
	val barHeight: Dp = 12.dp,
	val barColor: Color = Color.LightGray,
	val barColorInRange: Color = Color.Cyan,
	val barCornerRadius: Dp = 6.dp,
	val touchCircleRadius: Dp = 16.dp,
	val touchCircleShadowSize: Dp = 4.dp,
	val touchCircleShadowTouchedSizeAddition: Dp = 2.dp,
	val touchCircleColor: Color = Color.White,
	val textColorInRange: Color = Color.Black,
	val textColorOutOfRange: Color = Color.LightGray,
	val textSize: TextUnit = 16.sp,
	val textOffset: Dp = 8.dp,
	val stepMarkerColor: Color = Color.DarkGray,
) {
	context(Density) internal val barHeightPx: Float
		get() = barHeight.toPx()

	context(Density) internal val barCornerRadiusPx
		get() = barCornerRadius.toPx()

	context(Density) internal val touchCircleRadiusPx
		get() = touchCircleRadius.toPx()

	context(Density) internal val touchCircleShadowSizePx
		get() = touchCircleShadowSize.toPx()

	context(Density) internal val touchCircleShadowTouchedSizeAdditionPx
		get() = touchCircleShadowTouchedSizeAddition.toPx()

	context(Density) internal val stepMarkerRadiusPx: Float
		get() = (barHeight / 4).toPx()

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