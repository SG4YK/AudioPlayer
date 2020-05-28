package com.github.sg4yk.audioplayer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.github.sg4yk.audioplayer.utils.MediaHunter
import jp.wasabeef.recyclerview.adapters.AlphaInAnimationAdapter
import kotlinx.android.synthetic.main.activity_artist_detail.*
import kotlinx.coroutines.*

class ArtistDetailActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_TAG = "ARTIST_DETAIL_EXTRA"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_artist_detail)
        val extras = intent.getStringArrayExtra(EXTRA_TAG)

        toolbar.post {
            toolbar.title = extras[1]
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
        }
        val albumItemAdapter = AlbumItemAdapter()

        recyclerView.apply {
            layoutManager = GridLayoutManager(this@ArtistDetailActivity, 2)
            adapter = AlphaInAnimationAdapter(albumItemAdapter).apply {
                setFirstOnly(true)
                setDuration(200)
            }
            setHasFixedSize(true)
        }

        GlobalScope.launch(Dispatchers.IO) {
            delay(50)
            val albumList = MediaHunter.getAlbumsByArtistId(this@ArtistDetailActivity, extras[0].toLong())
            withContext(Dispatchers.Main) {
                albumItemAdapter.setAlbums(albumList)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }
}
