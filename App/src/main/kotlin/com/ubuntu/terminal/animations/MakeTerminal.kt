package com.ubuntu.terminal.animations

import android.animation.*
import android.view.View
import android.view.animation.*
import android.widget.TextView
import android.graphics.Color
import android.os.Handler
import android.os.Looper

object MakeAnimation {

    private const val DEFAULT_DURATION = 350L

    // FADE IN
    fun fadeIn(
        view: View,
        duration: Long = DEFAULT_DURATION
    ) {
        view.alpha = 0f
        view.visibility = View.VISIBLE

        view.animate()
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    // FADE OUT
    fun fadeOut(
        view: View,
        duration: Long = DEFAULT_DURATION
    ) {
        view.animate()
            .alpha(0f)
            .setDuration(duration)
            .withEndAction {
                view.visibility = View.GONE
            }
            .start()
    }

    // SLIDE LEFT
    fun slideLeft(
        view: View,
        duration: Long = DEFAULT_DURATION
    ) {
        view.translationX = view.width.toFloat()

        view.visibility = View.VISIBLE

        view.animate()
            .translationX(0f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    // SLIDE RIGHT
    fun slideRight(
        view: View,
        duration: Long = DEFAULT_DURATION
    ) {
        view.translationX = -view.width.toFloat()

        view.visibility = View.VISIBLE

        view.animate()
            .translationX(0f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    // SLIDE TOP
    fun slideTop(
        view: View,
        duration: Long = DEFAULT_DURATION
    ) {
        view.translationY = view.height.toFloat()

        view.visibility = View.VISIBLE

        view.animate()
            .translationY(0f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    // SLIDE BOTTOM
    fun slideBottom(
        view: View,
        duration: Long = DEFAULT_DURATION
    ) {
        view.translationY = -view.height.toFloat()

        view.visibility = View.VISIBLE

        view.animate()
            .translationY(0f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    // ZOOM IN
    fun zoomIn(
        view: View,
        duration: Long = DEFAULT_DURATION
    ) {
        view.scaleX = 0.5f
        view.scaleY = 0.5f
        view.alpha = 0f
        view.visibility = View.VISIBLE

        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(OvershootInterpolator())
            .start()
    }

    // BUTTON CLICK
    fun buttonClick(view: View) {
        view.animate()
            .scaleX(0.92f)
            .scaleY(0.92f)
            .setDuration(90)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(130)
                    .start()
            }
            .start()
    }

    // BLINKING CURSOR
    fun blinkCursor(
        textView: TextView,
        interval: Long = 550L
    ) {
        val handler = Handler(Looper.getMainLooper())

        val blink = object : Runnable {
            private var visible = true

            override fun run() {
                visible = !visible

                textView.alpha = if (visible) 1f else 0f

                handler.postDelayed(this, interval)
            }
        }

        handler.post(blink)
    }

    // LOADING PULSE
    fun pulse(
        view: View,
        duration: Long = 800L
    ) {
        view.animate()
            .alpha(0.35f)
            .scaleX(0.94f)
            .scaleY(0.94f)
            .setDuration(duration)
            .withEndAction {
                view.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(duration)
                    .withEndAction {
                        pulse(view, duration)
                    }
                    .start()
            }
            .start()
    }

    // ROTATION
    fun rotate(
        view: View,
        degrees: Float = 360f,
        duration: Long = 700L
    ) {
        view.animate()
            .rotationBy(degrees)
            .setDuration(duration)
            .setInterpolator(LinearInterpolator())
            .start()
    }

    // ANIMATED TEXT
    fun typeText(
        textView: TextView,
        text: String,
        speed: Long = 35L
    ) {
        val handler = Handler(Looper.getMainLooper())

        textView.text = ""

        var index = 0

        val runnable = object : Runnable {
            override fun run() {
                if (index < text.length) {
                    textView.append(text[index].toString())
                    index++
                    handler.postDelayed(this, speed)
                }
            }
        }

        handler.post(runnable)
    }

    // SHAKE
    fun shake(view: View) {
        val animator = ObjectAnimator.ofFloat(
            view,
            "translationX",
            0f, -12f, 12f, -8f, 8f, 0f
        )

        animator.duration = 350
        animator.start()
    }

    // RESET VIEW
    fun reset(view: View) {
        view.animate().cancel()

        view.alpha = 1f
        view.scaleX = 1f
        view.scaleY = 1f
        view.translationX = 0f
        view.translationY = 0f
        view.rotation = 0f
        view.visibility = View.VISIBLE
    }
}
