package cn.krisez.study.av.pic

import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import androidx.appcompat.app.AppCompatActivity
import cn.krisez.study.av.R
import cn.krisez.study.av.databinding.ActivityShowPicBinding

/**
 * 显示图片页面
 */
private const val TAG = "IMG_PIC"
class ShowPicActivity : AppCompatActivity() {
    private lateinit var binding: ActivityShowPicBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShowPicBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.imageView.setOnClickListener {
            binding.imageView.setImageResource(R.mipmap.not_found)
        }
        val surfaceView = binding.surfaceView
        val holder = surfaceView.holder
        holder.setFixedSize(1080, 1920)
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                val paint = Paint()
                paint.isAntiAlias = true
                val bitmap = BitmapFactory.decodeResource(resources, R.mipmap.not_found)
                val c = holder.lockCanvas()
                Log.d(TAG, "surfaceCreated: ${bitmap.width},${bitmap.height}")
                Log.d(TAG, "surfaceCreated: ${surfaceView.width},${surfaceView.height}")
                c.drawBitmap(
                    bitmap,
                    Rect(0, 0, bitmap.width, bitmap.height),
                    Rect(0, 0, bitmap.width, bitmap.height),
                    paint
                )
                holder.unlockCanvasAndPost(c)
                bitmap.recycle()
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {

            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {

            }
        })
    }
}