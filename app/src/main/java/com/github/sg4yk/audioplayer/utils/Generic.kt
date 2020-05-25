package com.github.sg4yk.audioplayer.utils

import android.animation.Animator
import android.content.Context
import android.graphics.Color
import android.util.DisplayMetrics
import android.view.View
import android.widget.ImageView
import androidx.annotation.WorkerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


object Generic {

    @WorkerThread
    fun msecToStr(msec: Long): String {
        val seconds = msec / 1000
        val minutes = seconds / 60
        return "%02d:%02d".format(minutes, seconds % 60)
    }

    fun crossFade(v1: ImageView, v2: ImageView, duration: Long = 300, delay: Long = 0) {
        if (v1.visibility == v2.visibility) {
            return
        }
        if (v1.visibility == View.VISIBLE) {
            // from v1 to v2
            crossFader(v1, v2, duration, delay)
        } else if (v2.visibility == View.VISIBLE) {
            // from v2 to v1
            crossFader(v2, v1, duration, delay)
        }
    }

    private fun crossFader(old: ImageView, new: ImageView, duration: Long, delay: Long) {
        new.alpha = 0f
        new.visibility = View.VISIBLE
        old.animate().alpha(0f).setDuration(duration).setStartDelay(delay)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationEnd(animation: Animator?) {
                    old.visibility = View.GONE
                }

                override fun onAnimationStart(animationOut: Animator?) {
                    if (animationOut != null) {
                        animationOut.pause()
                        new.animate().alpha(1f).setDuration(duration).setStartDelay(delay).setListener(
                            object : Animator.AnimatorListener {
                                override fun onAnimationRepeat(animation: Animator?) {}
                                override fun onAnimationEnd(animation: Animator?) {}
                                override fun onAnimationCancel(animation: Animator?) {}
                                override fun onAnimationStart(animation: Animator?) {
                                    // to make sure 2 animation start synchronously
                                    animationOut.resume()
                                }
                            }
                        ).start()
                    }
                }
            }).start()
    }

    fun dp2px(dp: Float, context: Context): Float {
        return dp * getDensity(context)
    }

    fun px2dp(px: Float, context: Context): Float {
        return px / getDensity(context)
    }

    fun getDensity(context: Context): Float {
        val metrics: DisplayMetrics = context.getResources().getDisplayMetrics()
        return metrics.density
    }

    fun luminance(color: Int): Int {
        val B = color and 0b11111111;
        val G = (color and 0b1111111100000000) shr 8
        val R = (color and 0b111111110000000000000000) shr 16
        return (R + R + R + B + G + G + G + G) shr 3
    }

    fun setAlpha(color: Int, alpha: Int): Int {
        val B = color and 0b11111111;
        val G = (color and 0b1111111100000000) shr 8
        val R = (color and 0b111111110000000000000000) shr 16
        return Color.argb(alpha, R, G, B)
    }

    fun setLightStatusBar(view: View, on: Boolean) {
        GlobalScope.launch(Dispatchers.Main) {
            if (on) {
                view.systemUiVisibility = view.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                view.systemUiVisibility = view.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
        }
    }
}