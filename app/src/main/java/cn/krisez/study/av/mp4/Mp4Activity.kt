package cn.krisez.study.av.mp4

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cn.krisez.study.av.databinding.ActivityMp4Binding

class Mp4Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMp4Binding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tv1.setOnClickListener {
            startActivity(Intent(this, ParseMp4Activity::class.java))
        }
        binding.tv2.setOnClickListener {
            startActivity(Intent(this, AssembleMp4Activity::class.java))
        }
    }
}
