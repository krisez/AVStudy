package cn.krisez.study.av.camera

import android.graphics.ImageFormat
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.Bundle
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.internal.compat.CameraManagerCompat
import cn.krisez.study.av.databinding.ActivityCamera2Binding

class Camera2Activity : AppCompatActivity() {
    private lateinit var binding: ActivityCamera2Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCamera2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        val sv = binding.surfaceView2
        sv.post {
            val imageReader = ImageReader.newInstance(sv.width, sv.height, ImageFormat.NV21, 10)
        }
    }
}
