package com.selbie.xkcdclock2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels

class MainActivity : AppCompatActivity() {
    private val viewModel: RotationViewModel by viewModels()
    private val view : ClockView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = ClockView(this, viewModel)
        setContentView(view)
    }

    override fun onDestroy() {
        super.onDestroy()
        view?.onDestroy()
    }
}