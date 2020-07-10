package com.hmomeni.trimviewsample

import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import com.hmomeni.trimview.TrimView
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Timber.plant(Timber.DebugTree())

        trimView.max = 100
        trimView.trim = 50
        trimView.minTrim = 20
        trimView.maxTrim = 80

        trimView.onTrimChangeListener = object : TrimView.TrimChangeListener() {
            override fun onLeftEdgeChanged(trimStart: Int, trim: Int) {
                trimView.progress = 0
            }

            override fun onRightEdgeChanged(trimStart: Int, trim: Int) {
                trimView.progress = 0
            }

            override fun onRangeChanged(trimStart: Int, trim: Int) {
                trimView.progress = 0
            }
        }
//        timer()
    }

    private val handler = Handler()
    private fun timer() {
        trimView.progress++
        handler.postDelayed({
            timer()
        }, 500)
    }
}
