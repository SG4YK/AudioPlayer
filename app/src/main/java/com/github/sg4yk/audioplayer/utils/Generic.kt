package com.github.sg4yk.audioplayer.utils

import android.animation.Animator
import android.view.View
import android.widget.ImageView
import androidx.annotation.WorkerThread

object Generic {

    @WorkerThread
    fun msecToStr(msec: Int): String {
        val seconds = msec / 1000
        val minutes = seconds / 60
        return "%02d:%02d".format(minutes, seconds % 60)
    }

    fun crossFade(v1: ImageView, v2: ImageView, duration: Long) {
        if (v1.visibility == v2.visibility) {
            return
        }
        if (v1.visibility == View.VISIBLE) {
            // from v1 to v2
            crossFader(v1, v2, duration)
        } else if (v2.visibility == View.VISIBLE) {
            // from v2 to v1
            crossFader(v2, v1, duration)

        }
    }

    private fun crossFader(old: ImageView, new: ImageView, duration: Long) {
        new.alpha = 0f
        new.visibility = View.VISIBLE
        old.animate().alpha(0f).setDuration(duration).setListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {}
            override fun onAnimationCancel(animation: Animator?) {}
            override fun onAnimationEnd(animation: Animator?) {
                old.visibility = View.GONE
            }
            override fun onAnimationStart(animationOut: Animator?) {
                if (animationOut != null) {
                    animationOut.pause()
                    new.animate().alpha(1f).setDuration(duration).setListener(
                        object :Animator.AnimatorListener {
                            override fun onAnimationRepeat(animation: Animator?) {}
                            override fun onAnimationEnd(animation: Animator?) {}
                            override fun onAnimationCancel(animation: Animator?) {}
                            override fun onAnimationStart(animation: Animator?) {
                                // to make sure 2 animation start synchronously
                                animationOut.resume()
                            }
                        }
                    )
                }
            }
        })


    }
}