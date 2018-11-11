package com.hmomeni.trimviewsample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.hmomeni.trimview.TrimView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        trimView.max = 100
        trimView.trim = 50
        trimView.minTrim = 20
        trimView.maxTrim = 80

        trimView.onTrimChangeListener = object : TrimView.TrimChangeListener() {
            override fun onLeftEdgeChanged(trimStart: Int, trim: Int) {
                Log.d(javaClass.simpleName, String.format("onLeftEdge --> trimStart = %d, trim = %d", trimStart, trim))
            }

            override fun onRightEdgeChanged(trimStart: Int, trim: Int) {
                Log.d(javaClass.simpleName, String.format("onRightEdge --> trimStart = %d, trim = %d", trimStart, trim))
            }

            override fun onRangeChanged(trimStart: Int, trim: Int) {
                Log.d(javaClass.simpleName, String.format("onRange --> trimStart = %d, trim = %d", trimStart, trim))
            }
        }
    }
}
