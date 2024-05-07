package cn.krisez.study.av.pic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.View
import cn.krisez.study.av.R

class MyImgView : View {
    constructor(context: Context?) : this(context, null) {}
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0) {}
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private lateinit var paint: Paint
    private lateinit var bitmap: Bitmap
    private var rect: Rect? = null
    private fun init() {
        paint = Paint()
        bitmap = BitmapFactory.decodeResource(resources, R.mipmap.not_found)
        rect = Rect(0, 0, bitmap.width, bitmap.height)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(width, bitmap.height)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        paint.isAntiAlias = true
        if (!bitmap.isRecycled) {
            rect?.let {
                canvas?.drawBitmap(
                    bitmap, it, it, paint
                )
            }
            bitmap.recycle()
        } else {
            init()
            invalidate()
            Log.d("Krisez", "onDraw-48: bitmap has recycled,re-create")
        }
    }
}