package com.onixen.playervawebar

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.math.log
import kotlin.math.sin

/**
 *
 * */
class PlayerWaveBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = 0
): View(context, attrs, defStyleAttr) {
    private var barPositionAnimator: ValueAnimator? = null //Animator for the position of the indicator and the sine wave on the bar
    private var offsetAnimator: ValueAnimator? = null

    private var waveStrokeWidth: Float = resources.getDimension(R.dimen.wave_stroke_width)
    private var amplitude: Float = resources.getDimension(R.dimen.wave_amplitude)
    private var indicatorRadius: Float = resources.getDimension(R.dimen.wave_indicator_radius)
    private var idicatorMultiplier: Float = resources.getString(R.string.wave_indicator_multiplayer).toFloat()
    private var frequency: Int = resources.getInteger(R.integer.wave_frequency)
    private var renderingStep: Int = resources.getInteger(R.integer.wave_rendering_step)
    private var startOffset: Int = resources.getInteger(R.integer.wave_start_offset)

    private var wavePrimaryColor = ContextCompat.getColor(context, R.color.wave_color)
    private var waveColorNotCompleated = ContextCompat.getColor(context, R.color.wave_color_not_compleated)

    private val indicatorPaint = Paint().apply {
        color = wavePrimaryColor
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = waveStrokeWidth
        isAntiAlias = true
    }
    private val wavePaint = Paint().apply {
        color = wavePrimaryColor
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = waveStrokeWidth
        isAntiAlias = true
    }
    private val wavePaintNotCompleated = Paint().apply {
        color = waveColorNotCompleated
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = waveStrokeWidth
        isAntiAlias = true
    }

    private var animatedOffsetValue: Int = 0
    private var filledInPercentage = 0
    private var minViewHeight = 0
    private val minViewWidth = 400
    private var waveCoordinateX = 0f
    private var indicatorFullRadius = 0f
    /** The coefficient of proportionality of the track length relative to the length of the bar. */
    private var propCoef: Float = 0f
    private var trackPosition: Int = 0
    private var audioIsPlaying: Boolean = false
    private var trackDuration: Int = 0

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val trackCurrentTime: MutableStateFlow<Int> by lazy { MutableStateFlow(-1) }
    private val rewindTime: MutableStateFlow<Int> by lazy { MutableStateFlow(-1) }

    init {
        idicatorMultiplier = resources.getString(R.string.wave_indicator_multiplayer).toFloat()
        initByUserXmlValues(attrs)
        setPaints()

        indicatorFullRadius = indicatorRadius + waveStrokeWidth / 2
        minViewHeight = calculateMinHeight()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (layoutParams.width == ViewGroup.LayoutParams.WRAP_CONTENT && layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            setMeasuredDimension(minViewWidth, minViewHeight)
        }
        else if (layoutParams.width == ViewGroup.LayoutParams.WRAP_CONTENT) {
            setMeasuredDimension(minViewWidth, measuredHeight)
        }
        else if (layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            setMeasuredDimension(measuredWidth, minViewHeight)
        }
    }
    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        val centerY = height / 2f // Y-координата центра синусоиды
        val startX = 0f + indicatorFullRadius // X-координата начальной точки проигранного времени
        val endX = width.toFloat() - indicatorFullRadius // X-координата конечной точки не проигранного времени

        val path = Path()
        path.moveTo(startX, centerY)

        var trackPosInCoord = trackPosition / propCoef + indicatorFullRadius
        if (trackPosInCoord.isNaN() && filledInPercentage > 0) {
            trackPosInCoord = calculateStartFrom(startX)
        }

        var xPos = startX
        while (xPos < trackPosInCoord) {
            val y = calculateY(centerY, xPos)

            path.lineTo(xPos, y)
            path.moveTo(xPos, y)

            waveCoordinateX = xPos
            xPos += renderingStep
        }
        if (trackPosInCoord.isNaN()) {
            canvas.drawLine(startX, centerY, endX, centerY, wavePaintNotCompleated)
            canvas.drawCircle(startX, centerY, indicatorRadius, indicatorPaint)
        } else {
            canvas.drawPath(path, wavePaint)
            canvas.drawLine(trackPosInCoord, centerY, endX, centerY, wavePaintNotCompleated)
            canvas.drawCircle(trackPosInCoord, centerY, indicatorRadius, indicatorPaint)
        }
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_MOVE -> performActionMove(event.x)
            MotionEvent.ACTION_UP -> preformActionUp()
            MotionEvent.ACTION_DOWN -> preformActionDown()
            else -> {
                Log.i(TAG, "onTouchEvent: Вызвано не обрабатываемое действие")
                return false
            }
        }
        return true
    }

    private fun initByUserXmlValues(attrs: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.PlayerWaveBar)
        wavePrimaryColor = typedArray.getColor(R.styleable.PlayerWaveBar_waveColor, wavePrimaryColor)
        waveStrokeWidth = typedArray.getDimension(R.styleable.PlayerWaveBar_waveStrokeWidth, waveStrokeWidth)
        renderingStep = typedArray.getInt(R.styleable.PlayerWaveBar_renderingStep, renderingStep)
        amplitude = typedArray.getDimension(R.styleable.PlayerWaveBar_amplitude, amplitude)
        frequency = typedArray.getInt(R.styleable.PlayerWaveBar_frequency, frequency)
        startOffset = typedArray.getInt(R.styleable.PlayerWaveBar_offset, startOffset)
        filledInPercentage = typedArray.getInt(R.styleable.PlayerWaveBar_filledIn, filledInPercentage)
        indicatorRadius = typedArray.getDimension(R.styleable.PlayerWaveBar_indicatorRadius, resources.getDimension(R.dimen.wave_indicator_radius))
        typedArray.recycle()
    }
    private fun calculateStartFrom(startDx: Float): Float {
        val availableWidth = width - indicatorFullRadius * 2
        val fillingPercentage = (availableWidth / 100 * filledInPercentage)
        return startDx + fillingPercentage
    }
    private fun setPaints() {
        indicatorPaint.apply {
            color = wavePrimaryColor
        }
        wavePaint.apply {
            color = wavePrimaryColor
            strokeWidth = waveStrokeWidth
            alpha = 180
        }
        wavePaintNotCompleated.apply {
            strokeWidth = waveStrokeWidth
        }
    }
    private fun calculateMinHeight(): Int {
        val calculatedGraphicHeight = amplitude * 2 + waveStrokeWidth
        val calculatedIndicatorHeight = (indicatorRadius * 2 + waveStrokeWidth) * idicatorMultiplier
        return if (calculatedIndicatorHeight < calculatedGraphicHeight) { calculatedGraphicHeight.toInt() } else { calculatedIndicatorHeight.toInt() }
    }
    private fun calculateY(centerY: Float, currentX: Float): Float {
        return centerY + sin((currentX + (animatedOffsetValue + startOffset)) / frequency) * amplitude
    }
    @Deprecated("The method will be modified and renamed", ReplaceWith("updateAnimators()"))
    private fun initAnimators() {
        /*Log.i(TAG, "initAnimators: ")

        val to = (trackDuration / (frequency * 4)) * 10
        offsetAnimator?.cancel()
        offsetAnimator = PlayerAnimators.Offset(
            from = 0,
            to = to,
            duration = trackDuration.toLong()
        ).create {
            this@PlayerWaveBar.animatedOffsetValue = it + startOffset
            invalidate()
        }

        barPositionAnimator?.cancel()
        barPositionAnimator = PlayerAnimators.Position(
            trackPosition,
            trackDuration,
            trackDuration.toLong()
        ).create {
            this@PlayerWaveBar.trackPosition = it
            coroutineScope.launch { trackCurrentTime.emit(trackPosition) }
            invalidate()
        }*/

        updateAnimators()
    }
    /**
     * Update the animators using new data about the current position of the track.
     * */
    private fun updateAnimators() {
        Log.i(TAG, "updateAnimators: ")
        val timeDx = trackDuration - trackPosition

        barPositionAnimator?.cancel()
        barPositionAnimator = PlayerAnimators.Position(
            trackPosition,
            trackDuration,
            timeDx.toLong()
        ).create {
            this@PlayerWaveBar.trackPosition = it
            coroutineScope.launch { trackCurrentTime.emit(trackPosition) }
            invalidate()
        }

        val to = (timeDx / (frequency * 4)) * 10
        offsetAnimator?.cancel()
        offsetAnimator = PlayerAnimators.Offset(
            0,
            to,
            timeDx.toLong()
        ).create {
            this@PlayerWaveBar.animatedOffsetValue = it + startOffset
            invalidate()
        }
    }
    private fun preformActionDown() {
        barPositionAnimator?.pause()
        indicatorRadius *= idicatorMultiplier
        invalidate()
    }
    private fun preformActionUp() {
        Log.i(TAG, "preformActionUp: ")

        /*val timeDx = trackDuration - trackPosition

        barPositionAnimator?.cancel()
        barPositionAnimator = PlayerAnimators.Position(
            trackPosition,
            trackDuration,
            timeDx.toLong()
        ).create {
            this@PlayerWaveBar.trackPosition = it
            coroutineScope.launch { trackCurrentTime.emit(trackPosition) }
            invalidate()
        }

        val to = (timeDx / (frequency * 4)) * 10
        offsetAnimator?.cancel()
        offsetAnimator = PlayerAnimators.Offset(
            0,
            to,
            timeDx.toLong()
        ).create {
            this@PlayerWaveBar.animatedOffsetValue = it + startOffset
            invalidate()
        }*/

        updateAnimators()

        coroutineScope.launch { rewindTime.emit(trackPosition) }

        if (audioIsPlaying) {
            barPositionAnimator?.start()
            offsetAnimator?.start()
        }

        indicatorRadius /= idicatorMultiplier
        invalidate()
    }
    private fun performActionMove(eventX: Float): Boolean {
        // Правая граница до которой можно передвинуть индикатор (с учётом его размеров)
        val rightBorder = width - indicatorFullRadius
        val leftBorder = 0f + indicatorFullRadius
        // Если положение пальца будет выходить за пределы указанных границ, то не будем регистрировать такое перемещение
        if (eventX < leftBorder || eventX > rightBorder) return false

        // Вычисление на сколько необходимо сдвинуть индикатор
        val dx = eventX - waveCoordinateX

        if (eventX in 0f..rightBorder) {
            val offset = (dx * propCoef).toInt()
            if (trackPosition + offset in 0..trackDuration)
                trackPosition += offset
            waveCoordinateX = eventX

            invalidate()
        }
        return true
    }
    private fun calculatePropCoef(trackDuration: Int, barWidth: Int, indicatorRadius: Float): Float {
        return trackDuration.toFloat() / (barWidth.toFloat() - indicatorRadius * 2)
    }


    /**
     * The Wave Bar preset method for synchronous operation with the track.
     *
     * @param trackDuration The full length of the track in milliseconds.
     * @param currentPosition The number of milliseconds already played.
     * */
    fun prepare(duration: Int, position: Int) {
        // Functions are called in a strictly specified sequence.
        this.trackDuration = duration
        this.trackPosition = position
        this.propCoef = calculatePropCoef(trackDuration, width, indicatorFullRadius)
        updateAnimators()
    }

    fun startAnimation() {
        Log.i(TAG, "startAnimation: ")

        audioIsPlaying = true
        updateAnimators()
        offsetAnimator?.start()
        barPositionAnimator?.start()
    }
    fun stopAnimation() {
        Log.i(TAG, "stopAnimation: ")

        offsetAnimator?.cancel()
        barPositionAnimator?.cancel()
        audioIsPlaying = false

        trackPosition = 0
        updateAnimators()
    }
    fun pauseAnimation(currentPos: Int) {
        Log.i(TAG, "pauseAnimation: ")
        trackPosition = currentPos
        audioIsPlaying = false
        updateAnimators()
        offsetAnimator?.start()
        offsetAnimator?.pause()
        barPositionAnimator?.start()
        barPositionAnimator?.pause()

        Log.d(TAG, "offsetAnimator state = ${offsetAnimator?.isPaused}")
        Log.d(TAG, "barPositionAnimator state = ${barPositionAnimator?.isPaused}")
    }
    fun resumeAnimation() {
        Log.i(TAG, "resumeAnimation: ")

        offsetAnimator?.resume()
        barPositionAnimator?.resume()
        audioIsPlaying = true
    }

    fun getCurrentTimeFlow(): Flow<Int> {
        return trackCurrentTime
    }
    fun getRewindTimeFlow(): Flow<Int> {
        return rewindTime
    }


    @Deprecated(
        message = "The method will be removed in one of the following versions, because the operability of the entire wave Bar depends on the sequence " +
                "of calling this method. When using the new method, you should remove the use of setTrackPosition().",
        replaceWith = ReplaceWith(expression = "this.prepare(duration = 0, position = 0)"),
        level = DeprecationLevel.WARNING
    )
    fun setTrackDuration(value: Int) {
        trackDuration = value
        propCoef = calculatePropCoef(value, width, indicatorFullRadius)
        initAnimators()
    }
    @Deprecated(
        message = "The method will be removed in one of the following versions, because the operability of the entire wave Bar depends on the sequence " +
                "of calling this method. When using the new method, you should remove the use of setTrackDuration().",
        replaceWith = ReplaceWith(expression = "this.prepare(duration = 0, position = 0)"),
        level = DeprecationLevel.WARNING
    )
    fun setTrackPosition(value: Int) {
        trackPosition = value
    }

    companion object {
        private const val TAG = "player_vawe_bar_debug"
    }
}
