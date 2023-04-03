package com.selbie.xkcdclock2

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.os.CountDownTimer
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.min

@SuppressLint("ViewConstructor")
class ClockView(ctx : Context, vm : RotationViewModel ) : View(ctx) {

    val TAG = ClockView::class.simpleName

    private val paint : Paint = Paint()
    private var context = ctx
    private var viewModel = vm
    private var activeRotation = 0.0
    private var firstFrame = true

    private var canvasCenterX : Int = 0
    private var canvasCenterY : Int = 0

    private var bmpInner = BitmapFactory.decodeResource(ctx.resources, R.drawable.inner)
    private var bmpOuter = BitmapFactory.decodeResource(ctx.resources, R.drawable.outer)

    private var refX = 0f
    private var refY = 0f

    private var timer : CountDownTimer? = null

    init {
        // three different ways to set a callback
        //val callback : (View, MotionEvent) -> Boolean = {v:View, me:MotionEvent ->
        //    onTouchEvent(v,me)
        //}
        //this.setOnTouchListener(View.OnTouchListener(callback))

        // OR
        //this.setOnTouchListener(View.OnTouchListener({v:View, me:MotionEvent ->
        //    onTouchEvent(v,me)
        //}))

        // Or even simpler
        this.setOnTouchListener { v, me -> onTouchEvent(v,me)}

        paint.color = 0xffffffff.toInt()
    }

    fun onDestroy() {
        timer?.cancel()
        timer = null
    }

    private fun startTimer() {
        timer?.cancel()
        Log.d(TAG, "starting timer")
        // every 4 minutes of elapased time is 1 degree rotation around the world
        // so redrawing once a minute seems reasonable
        timer = object : CountDownTimer(Long.MAX_VALUE, 60000) {
            override fun onTick(millisUntilFinished: Long) {
                Log.d(TAG, "ClockView.onTick")
                invalidate()
            }
            override fun onFinish() {}

        }.start()
    }

    private fun computeAngle(x: Float, y: Float): Double {
        val radsToDegrees = 360 / (Math.PI * 2)
        var result = atan2(y.toDouble(), x.toDouble()) * radsToDegrees
        if (result < 0) {
            result += 360
        }
        return result
    }

    private fun computeTimeRotationOuter(): Float {
        val currentTime = android.icu.util.Calendar.getInstance()
        val millis = currentTime.timeInMillis % 86400000
        val seconds = millis / 1000
        val minutes = seconds / 60
        val degrees = minutes / 4
        return 180 - degrees.toFloat()
    }

    private fun onTouchEvent( @Suppress("UNUSED_PARAMETER") v : View,
                              event: MotionEvent) : Boolean {

        //val action = event.action
        val actionmasked = event.actionMasked

        if (firstFrame) {
            return false
        }

        if (actionmasked == MotionEvent.ACTION_DOWN) {
            //Log.d(TAG, "event=ACTION_DOWN.  x=" + event.x + "  y=" +  event.y)

            refX = event.x
            refY = event.y
            return true
        }

        if (actionmasked == MotionEvent.ACTION_MOVE) {
            var x = event.x - canvasCenterX
            var y = canvasCenterY - event.y

            //Log.d(TAG, "event=ACTION_MOVE.  x=" + event.x + "  y=" +  event.y)

            if (x != 0.0f && y != 0.0f) {
                val angleB = computeAngle(x,y)
                x = refX - canvasCenterX
                y = canvasCenterY - refY
                val angleA = computeAngle(x, y)

                activeRotation = angleA - angleB

                //Log.d(TAG, "rotation=" + _activeRotation)

                invalidate()
            }
        }

        if ((actionmasked == MotionEvent.ACTION_UP) || (actionmasked == MotionEvent.ACTION_CANCEL)) {
            viewModel.persistentRotation += activeRotation
            while (viewModel.persistentRotation > 360.0) {
                viewModel.persistentRotation -= 360.0
            }
            while (viewModel.persistentRotation < 0.0) {
                viewModel.persistentRotation += 360.0
            }
            activeRotation = 0.0
        }

        return true
    }

    private fun drawBitmapInCenter(canvas : Canvas, bmp : Bitmap, scaleFactor : Float, rotation : Float) {
        canvas.save()
        canvas.translate(canvas.width/2F, canvas.height/2F)
        canvas.scale(scaleFactor, scaleFactor)
        canvas.rotate(rotation)
        canvas.translate(-bmp.width/2F, -bmp.height/2F)
        canvas.drawBitmap(bmp, 0F, 0F, paint)
        canvas.restore()
    }

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null) {
            return
        }

        if (firstFrame) {
            canvasCenterX = width / 2
            canvasCenterY = height / 2
            startTimer()
            firstFrame = false
        }

        val target = min(width, height) - 20
        val scaleFactor = target.toFloat() / bmpOuter.width

        drawBitmapInCenter(canvas, bmpOuter, scaleFactor, viewModel.persistentRotation.toFloat() + activeRotation.toFloat() + computeTimeRotationOuter()  )
        drawBitmapInCenter(canvas, bmpInner, scaleFactor, viewModel.persistentRotation.toFloat() + activeRotation.toFloat())
    }
}
