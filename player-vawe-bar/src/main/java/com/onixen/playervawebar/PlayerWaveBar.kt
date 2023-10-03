package com.onixen.playervawebar

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
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
import kotlin.math.sin

class PlayerWaveBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = 0
): View(context, attrs, defStyleAttr) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val idicatorMultiplier: Float = 1.2f

    private var wavePrimaryColor = ContextCompat.getColor(context, R.color.wave_color)
    private var waveColorNotCompleated = ContextCompat.getColor(context, R.color.wave_color_not_compleated)
    private var waveStrokeWidth = 4.dpToPx()
    /** шаг с которым отрисовывается синусоида */
    private var step: Int = 1
    private var amplitude: Float = 4.dpToPx()
    private var frequency: Int = 14
    /** Стартовое смещение синусоиды */
    private var offset: Int = 0
    private var indicatorRadius = 6.dpToPx()
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
    private lateinit var offsetAnimator: ValueAnimator
    private var animatedOffsetValue: Int = 0

    /** Процент с которого начинается заполнение бара. Используется для отображения синусоиды в layout */
    private var startFrom = 0
    private var minViewHeight = 0
    private val minViewWidth = 400
    /** Значение X координаты для обработки перемещения индикатора жестом */
    private var actionX = 0f
    private var indicatorFullRadius = 0f

    /** Коэффициент пропорциональности длинны трека относительно длинны бара */
    private var propCoef: Float = 0f
    /** Аниматор для положения индикатора и синусоиды на баре */
    private lateinit var barPositionAnimator: ValueAnimator
    private var trackPosition: Int = 0
    private var audioIsPlaying: Boolean = false

    private var trackDuration: Int = 0
    private val trackCurrentTime: MutableStateFlow<Int> by lazy { MutableStateFlow(-1) }
    private val rewindTime: MutableStateFlow<Int> by lazy { MutableStateFlow(-1) }

    init {
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

        val trackPosInCoord = trackPosition / propCoef + indicatorFullRadius

        var xPos = startX
        while (xPos < trackPosInCoord) {
            val y = centerY + sin((xPos + (Math.PI.toFloat() / 4f) * animatedOffsetValue) / frequency) * amplitude

            path.lineTo(xPos, y)
            path.moveTo(xPos, y)

            actionX = xPos
            xPos += step
        }
        if (trackPosInCoord.isNaN()) {
            canvas.drawLine(startX, centerY, endX, centerY, wavePaintNotCompleated)
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
        waveStrokeWidth = typedArray.getDimension(R.styleable.PlayerWaveBar_waveWidth, waveStrokeWidth)
        step = typedArray.getInt(R.styleable.PlayerWaveBar_step, step)
        amplitude = typedArray.getDimension(R.styleable.PlayerWaveBar_amplitude, amplitude)
        frequency = typedArray.getInt(R.styleable.PlayerWaveBar_frequency, frequency)
        offset = typedArray.getInt(R.styleable.PlayerWaveBar_offset, offset)
        startFrom = typedArray.getInt(R.styleable.PlayerWaveBar_startFrom, startFrom)
        indicatorRadius = typedArray.getDimension(R.styleable.PlayerWaveBar_indicatorRadius, indicatorRadius)
        typedArray.recycle()
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
    private fun calculateY(centerY: Float): Float {
        return centerY + sin((x + (Math.PI.toFloat() / 4f) * animatedOffsetValue) / frequency) * amplitude
    }
    private fun initAnimators() {
        val to = (trackDuration / (frequency * 4)) * 10
        offsetAnimator = PlayerAnimators.Offset(0, to, trackDuration.toLong()).create {
            this@PlayerWaveBar.animatedOffsetValue = it
            invalidate()
        }
        barPositionAnimator = PlayerAnimators.Position(
            trackPosition,
            trackDuration,
            trackDuration.toLong()
        ).create {
            this@PlayerWaveBar.trackPosition = it
            coroutineScope.launch { trackCurrentTime.emit(trackPosition) }
            invalidate()
        }
    }
    private fun preformActionDown() {
        barPositionAnimator.pause()
        indicatorRadius *= idicatorMultiplier
        invalidate()
    }
    private fun preformActionUp() {
        val timeDx = trackDuration - trackPosition
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
        offsetAnimator = PlayerAnimators.Offset(trackPosition, to, timeDx.toLong()).create {
            this@PlayerWaveBar.animatedOffsetValue = it
            invalidate()
        }
        coroutineScope.launch { rewindTime.emit(trackPosition) }
        if (audioIsPlaying) barPositionAnimator.start()

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
        val dx = eventX - actionX

        if (eventX in 0f..rightBorder) {
            val offset = (dx * propCoef).toInt()
            if (trackPosition + offset in 0..trackDuration)
                trackPosition += offset
            Log.d(TAG, "performActionMove: track position = $trackPosition")
            actionX = eventX

            invalidate()
        }
        return true
    }
    private fun calculatePropCoef(trackDuration: Int, barWidth: Int, indicatorRadius: Float): Float {
        return trackDuration.toFloat() / (barWidth.toFloat() - indicatorRadius * 2)
    }
    private fun Int.dpToPx(): Float {
        return this * resources.displayMetrics.density
    }

    fun setTrackDuration(value: Int) {
        trackDuration = value
        propCoef = calculatePropCoef(value, width, indicatorFullRadius)
        initAnimators()
    }
    fun setTrackPosition(value: Int) {
        trackPosition = value
    }
    fun startAnimation() {
        offsetAnimator.start()
        barPositionAnimator.start()
        audioIsPlaying = true
    }
    fun stopAnimation() {
        offsetAnimator.cancel()
        barPositionAnimator.cancel()
        audioIsPlaying = false
    }
    fun pauseAnimation() {
        offsetAnimator.pause()
        barPositionAnimator.pause()
        audioIsPlaying = false
    }
    fun resumeAnimation() {
        offsetAnimator.resume()
        barPositionAnimator.resume()
        audioIsPlaying = true
    }
    fun getCurrentTimeFlow(): Flow<Int> {
        return trackCurrentTime
    }
    fun getRewindTimeFlow(): Flow<Int> {
        return rewindTime
    }

    companion object {
        private const val TAG = "player_vawe_bar_debug"
    }
}
