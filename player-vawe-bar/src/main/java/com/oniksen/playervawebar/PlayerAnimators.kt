package com.onixen.player_wave_bar

import android.animation.ValueAnimator
import android.util.Log
import android.view.animation.LinearInterpolator

sealed class PlayerAnimators {
    class Offset(private val from: Int, private val to: Int, private val duration: Long) : PlayerAnimators() {
        fun create(listener: (Int) -> Unit): ValueAnimator {
            return ValueAnimator.ofInt(from, to).apply {
                this.duration = this@Offset.duration
                this.interpolator = LinearInterpolator()

                addUpdateListener { listener.invoke(it.animatedValue as Int) }
            }
        }
    }
    class Position(private val from: Int, private val to: Int, private val duration: Long): PlayerAnimators() {
        fun create(listener: (Int) -> Unit): ValueAnimator {
            val animator = ValueAnimator.ofInt(from, to).apply {
                this.duration = this@Position.duration
                this.interpolator = LinearInterpolator()

                addUpdateListener { listener.invoke(it.animatedValue as Int) }
            }
            return animator
        }
    }

    companion object {
        private const val TAG = "player_animators"
    }
}
