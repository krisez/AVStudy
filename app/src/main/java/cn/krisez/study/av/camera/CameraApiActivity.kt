package cn.krisez.study.av.camera

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import cn.krisez.study.av.R

/**
 * @author Krisez
 * SDK CameraApi调用
 */
class CameraApiActivity :AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_api)
        findViewById<TextView>(R.id.tv_camera_2).setOnClickListener {
            startActivity(Intent(this,Camera2Activity::class.java))
        }

        findViewById<TextView>(R.id.tv_camera_x).setOnClickListener {
            startActivity(Intent(this,CameraXActivity::class.java))
        }
    }
}