package com.github.sg4yk.audioplayer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.activity_album_detail.*
import kotlinx.android.synthetic.main.activity_album_detail.fab
import kotlinx.android.synthetic.main.activity_playlist_detail.*
import kotlinx.android.synthetic.main.activity_playlist_detail.appBarLayout
import kotlinx.android.synthetic.main.activity_playlist_detail.toolbar

class PlaylistDetailActivity : AppCompatActivity() {
    companion object {
        private var threshold: Int? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_detail)

        if (threshold == null) {
            threshold = -(resources.displayMetrics.widthPixels shr 1)
        }

        toolbar.post {
            toolbar.title = "Playlist"
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
            setUpAppBar()
        }

        setUpAppBar()
    }

    private fun setUpAppBar() {
        appBarLayout.addOnOffsetChangedListener(object : AppBarLayout.OnOffsetChangedListener {
            private var cachedOffset = 0
            override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
                if (threshold!! in verticalOffset until cachedOffset) {
                    fab.hide()
                } else if (threshold!! in (cachedOffset + 1)..verticalOffset) {
                    fab.show()
                }
                val opacity = (verticalOffset - threshold!!) / 200f
                name.alpha = opacity
                cachedOffset = verticalOffset
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }
}
