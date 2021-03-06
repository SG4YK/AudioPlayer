package com.github.sg4yk.audioplayer.utils

import android.animation.Animator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.DisplayMetrics
import android.view.View
import android.widget.ImageView
import androidx.annotation.WorkerThread
import com.github.sg4yk.audioplayer.media.Audio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


object Generic {

    /**
     * Convert milliseconds to readable string
     * e.g., 01:32
     *
     * @param msec milliseconds
     * @return
     */
    fun msecToStr(msec: Long): String {
        val seconds = msec / 1000
        val minutes = seconds / 60
        return "%02d:%02d".format(minutes, seconds % 60)
    }

    @Deprecated("This method is no longer used")
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

    @Deprecated("This method is no longer used")
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
        val metrics: DisplayMetrics = context.resources.displayMetrics
        return metrics.density
    }


    /**
     * Calculate approximate luminance of color
     *
     * @param color
     * @return Luminance
     */
    fun luminance(color: Int): Int {
        val b = color and 0xff
        val g = (color and 0xff00) shr 8
        val r = (color and 0xff0000) shr 16
        return (r + r + r + b + g + g + g + g) shr 3
    }

    /**
     * Change the opacity of color
     *
     * @param color
     * @param alpha Opacity, from 0 to 255
     * @return
     */
    fun setAlpha(color: Int, alpha: Int): Int {
        val b = color and 0xff
        val g = (color and 0xff00) shr 8
        val r = (color and 0xdff0000) shr 16
        return Color.argb(alpha, r, g, b)
    }

    /**
     * Set light status bar on or off
     *
     * Notice: A light status bar has dark icon
     *
     * @param view The decoration view of window
     * @param on [true] for light status bar, [false] for dark status bar
     */
    fun setLightStatusBar(view: View, on: Boolean) {
        GlobalScope.launch(Dispatchers.Main) {
            if (on) {
                view.systemUiVisibility = view.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                view.systemUiVisibility = view.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
        }
    }

    fun openWebsite(ctx: Context, url: String) {
        val website: Uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, website)
        if (intent.resolveActivity(ctx.packageManager) != null) {
            ctx.startActivity(intent)
        }
    }

    fun openWebsite(activity: Activity, url: String) {
        val website: Uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, website)
        if (intent.resolveActivity(activity.packageManager) != null) {
            activity.startActivity(intent)
        }
    }
}