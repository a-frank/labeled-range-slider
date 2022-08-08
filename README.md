# Labeled Range Slider

The Labeled Range Slider allows a user to select an upper and lower bound of a range, while showing the available values as labels aligned correctly with the steps on a slider

![Labeled Range Slider in action](https://user-images.githubusercontent.com/2872794/183030179-ea3d0043-ba20-4ea4-8283-450000f055e0.gif)

## Usage

```Kotlin
val steps = (0..100).step(10).toList()
var lowerBound by rememberSaveable { mutableStateOf(steps[1]) }
var upperBound by rememberSaveable { mutableStateOf(steps[steps.size - 2]) }

LabeledRangeSlider(
	selectedLowerBound = lowerBound,
	selectedUpperBound = upperBound,
	steps = steps,
	onRangeChanged = { lower, upper ->
		lowerBound = lower
		upperBound = upper
		Log.i(LOG_TAG, "Updated selected range ${lowerBound..upperBound}")
	},
	modifier = Modifier.fillMaxWidth()
)
```

Required parameters:
- The selected lower value
- The selected upper value
- All available values
- A callback to get notified of value changes

Also there is an optinal parameter **sliderConfig**, which allows the customization of the Labeled Range Slider, like
- The height of the slider bar
- The color of the slider bar, in range and out of range
- The size of the touch handles
- The color of the touch handles

and more. You can see the full configuration in the [SliderConfiguration class](labeled-range-slider/src/main/java/de/gnarly/compose/ui/slider/SliderConfiguration.kt)

## How it was implemented

If you are interessed in the implementation details and some thoughts bind it, check out the following two blog posts
- [Drawing the Labeled Range Slider](https://dev.to/lex_fury/draw-the-labeled-range-slider-1771)
- [Make the Labeled Range Slider interactive](https://dev.to/lex_fury/make-the-labeled-range-slider-interactive-5gf)
