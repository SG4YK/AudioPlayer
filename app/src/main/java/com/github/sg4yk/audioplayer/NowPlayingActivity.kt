package com.github.sg4yk.audioplayer

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock.sleep
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import com.google.android.material.appbar.MaterialToolbar
import kotlin.math.hypot

class NowPlayingActivity : AppCompatActivity() {

    private var fabX: Int = 0
    private var fabY: Int = 0
    private var fabD: Int = 0
    private var radius: Float = 0f
    private lateinit var rootLayout: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_now_playing)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.post {
            setSupportActionBar(toolbar)
            getSupportActionBar()?.setDisplayHomeAsUpEnabled(true);
            getSupportActionBar()?.setDisplayShowHomeEnabled(true);
        }

        fabX = intent.getIntExtra("fabX", 0)
        fabY = intent.getIntExtra("fabY", 0)
        fabD = intent.getIntExtra("fabD", 0)

        radius = hypot(fabX.toDouble(), fabY.toDouble()).toFloat()

        rootLayout = findViewById(R.id.rootLayout)
        rootLayout.visibility = View.INVISIBLE
        rootLayout.post {
            startRevealAnim(fabX, fabY)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    private fun startRevealAnim(centerX: Int, centerY: Int) {
        val startRadius = 0.0f
        val endRadius = Math.hypot(centerX.toDouble(), centerY.toDouble()).toFloat()
        val anim = ViewAnimationUtils.createCircularReveal(rootLayout, centerX, centerY, fabD.toFloat() / 2, endRadius)
        anim.duration = 1000
        anim.interpolator = DecelerateInterpolator(3.0f)
        rootLayout.visibility = View.VISIBLE
        anim.start()
    }

    override fun onBackPressed() {
        val endRadius = 100.0f
        val anim = ViewAnimationUtils.createCircularReveal(rootLayout, fabX, fabY, radius, 0f)
        anim.duration = 500
        anim.interpolator = AccelerateInterpolator()
        anim.doOnEnd {
            rootLayout.visibility = View.INVISIBLE
            finishAfterTransition()
        }
        anim.start()
    }
}
