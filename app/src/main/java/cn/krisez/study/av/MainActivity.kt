package cn.krisez.study.av

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import cn.krisez.study.av.audio.AudioRecordActivity
import cn.krisez.study.av.camera.CameraApiActivity
import cn.krisez.study.av.databinding.ActivityMainBinding
import cn.krisez.study.av.mp4.Mp4Activity
import cn.krisez.study.av.mp4.ParseMp4Activity
import cn.krisez.study.av.pic.ShowPicActivity
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class MainActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityMainBinding
    private val list = arrayListOf(
        Entry("图片展示入口", ShowPicActivity::class.java),
        Entry("音频录制", AudioRecordActivity::class.java),
        Entry("CameraApi录制", CameraApiActivity::class.java),
        Entry("MP4-Video", Mp4Activity::class.java),
    )

    private val mAdapter = MyAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        mBinding.recyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        mBinding.recyclerView.adapter = mAdapter
        mAdapter.setNewInstance(list.toMutableList())
        mAdapter.setOnItemClickListener { _, _, position ->
            startActivity(Intent(this, list[position].clazz))
        }
    }
}

private class MyAdapter(layoutRes: Int = R.layout.item_main) :
    BaseQuickAdapter<Entry, BaseViewHolder>(layoutRes) {
    override fun convert(holder: BaseViewHolder, item: Entry) {
        holder.setText(R.id.text_view, item.name)
    }

}

data class Entry(
    var name: String,
    var clazz: Class<out Activity>
)