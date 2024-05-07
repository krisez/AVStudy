package cn.krisez.study.av.mp4

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import androidx.appcompat.app.AppCompatActivity
import cn.krisez.study.av.databinding.ActivityParseMp4Binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

class ParseMp4Activity : AppCompatActivity() {
    private lateinit var binding: ActivityParseMp4Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParseMp4Binding.inflate(layoutInflater)
        setContentView(binding.root)
        val surfaceView = binding.surfaceView
        val holder = surfaceView.holder
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                CoroutineScope(Dispatchers.IO).launch {
                    val me = MediaExtractor()
                    me.setDataSource("http://172.20.136.111/media/pop-star-kda.mp4")
//                    me.setDataSource("http://172.20.136.111/media/雅俗共赏.mp4")
                    val count = me.trackCount
                    var videoTrack = -1
                    var codec: MediaCodec? = null
                    var maxSize = -1
                    for (i in 0 until count) {
                        val format = me.getTrackFormat(i)
                        val type = format.getString(MediaFormat.KEY_MIME)
                        val isVideo = type?.contains("video") == true
                        Log.d("Krisez", "onCreate: $format")
                        if (isVideo) {
                            maxSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                            videoTrack = i
                            codec = MediaCodec.createDecoderByType(type ?: "")
                            codec.configure(format, holder.surface, null, 0)
                            codec.start()
                        }
                    }
                    me.selectTrack(videoTrack)
                    val bufferInfo = MediaCodec.BufferInfo()
//                    val buffers = codec?.inputBuffers
                    val startMs = System.nanoTime()
                    var sampleSize = -1
                    var inputBufferId = -1
                    val bf = ByteBuffer.allocate(maxSize)
                    var isLoop = true
                    val bytes = byteArrayOf()
                    while (isLoop) {
                        inputBufferId = codec?.dequeueInputBuffer(-1) ?: -1
                        Log.d("Krisez", "surfaceCreated: inputBufferId->$inputBufferId")
                        if (inputBufferId >= 0) {
                            sampleSize = me.readSampleData(bf, 0)
                            Log.d("Krisez", "surfaceCreated: size-> $sampleSize")
                            val time = me.sampleTime
                            Log.d("Krisez", "surfaceCreated: time->$time")
                            if (sampleSize < 0) {
                                codec?.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                isLoop = false
                            } else {
                                bufferInfo.presentationTimeUs = me.sampleTime
                                bufferInfo.offset = 0
                                bufferInfo.flags = me.sampleFlags
                                bufferInfo.size = sampleSize
                                codec?.getInputBuffer(inputBufferId)?.put(bf)
                                codec?.queueInputBuffer(inputBufferId, 0, sampleSize, time, 0)
                            }
                        }

                        val outputId = codec?.dequeueOutputBuffer(bufferInfo, 10000) ?: -1
                        Log.d("Krisez", "surfaceCreated: outputId->$outputId")
                        if (isLoop && outputId > -1) {
                            val out = codec?.getOutputBuffer(outputId)
//                            out?.
                            Log.d("Krisez", "surfaceCreated: ${out.toString()}")
                            codec?.releaseOutputBuffer(outputId, bufferInfo.presentationTimeUs * 1000L  + startMs)
//                            codec?.releaseOutputBuffer(outputId, me.sampleTime* 1000L  + startMs)  //这样一卡一卡的。。
                        } else {
                            Log.d("Krisez", "surfaceCreated: end decode")
                        }
                        me.advance()
                    }
                    codec?.stop()
                    codec?.release()
                    me.release()
                }
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                holder.removeCallback(this)
            }
        })
    }
}