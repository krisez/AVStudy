package cn.krisez.study.av.mp4

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
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
import kotlin.math.roundToInt


class ParseMp4Activity : AppCompatActivity() {
    private lateinit var binding: ActivityParseMp4Binding
    private var isLoopAudio = true
    private var isLoopVideo = true
    private var audioCodec: MediaCodec? = null
    var at: AudioTrack? = null
    private var videoCodec: MediaCodec? = null
    private var w: Int = 0
    private var h: Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParseMp4Binding.inflate(layoutInflater)
        setContentView(binding.root)
        val surfaceView = binding.surfaceView

        val holder = surfaceView.holder
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                w = surfaceView.width
                h = surfaceView.height
                decodeVideo()
                decodeAudio()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                Log.d("Krisez", "surfaceChanged: $width--$height")
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                holder.removeCallback(this)
            }
        })

    }

    private fun decodeAudio() {
        CoroutineScope(Dispatchers.IO).launch {
            val me = MediaExtractor()
            me.setDataSource("http://172.20.136.111/media/pop-star-kda.mp4")
//                    me.setDataSource("http://172.20.136.111/media/雅俗共赏.mp4")
            val count = me.trackCount
            var audioTrack = -1
            var frequency = 44100
            var minBuffer = 0
            val aa = AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
            var af: AudioFormat? = null
            for (i in 0 until count) {
                val format = me.getTrackFormat(i)
                val type = format.getString(MediaFormat.KEY_MIME)
                val isAudio = type?.contains("audio") == true
                Log.d("Krisez", "decodeAudio: format -> $format")
                if (isAudio) {
                    frequency = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    minBuffer = AudioTrack.getMinBufferSize(
                        frequency,
                        AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                    af = AudioFormat.Builder()
                        .setSampleRate(frequency)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build()
                    at = AudioTrack(
                        aa,
                        af,
                        minBuffer,
                        AudioTrack.MODE_STREAM,
                        AudioManager.AUDIO_SESSION_ID_GENERATE
                    )
                    audioTrack = i
                    audioCodec = MediaCodec.createDecoderByType(type ?: "")
                    audioCodec?.configure(format, null, null, 0)
                    audioCodec?.start()
                }
            }
            me.selectTrack(audioTrack)
            val bufferInfo = MediaCodec.BufferInfo()
            var sampleSize = -1
            var inputBufferId = -1
            val bf = ByteBuffer.allocate(minBuffer)
            val startMs = System.nanoTime()
            at?.play()
            while (isLoopAudio) {
                inputBufferId = audioCodec?.dequeueInputBuffer(-1) ?: -1
                Log.d("Krisez", "decodeAudio: inputBufferId->$inputBufferId")
                if (inputBufferId >= 0) {
                    sampleSize = me.readSampleData(bf, 0)
                    Log.d("Krisez", "decodeAudio: size-> $sampleSize")
                    val time = me.sampleTime
                    Log.d("Krisez", "decodeAudio: time->$time")
                    if (sampleSize < 0) {
                        audioCodec?.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isLoopAudio = false
                    } else {
                        bufferInfo.presentationTimeUs = me.sampleTime
                        bufferInfo.offset = 0
                        bufferInfo.flags = me.sampleFlags
                        bufferInfo.size = sampleSize
                        audioCodec?.getInputBuffer(inputBufferId)?.put(bf)
                        audioCodec?.queueInputBuffer(inputBufferId, 0, sampleSize, time, 0)
                    }
                }

                val outputId = audioCodec?.dequeueOutputBuffer(bufferInfo, 10000) ?: -1
                Log.d("Krisez", "decodeAudio: outputId->$outputId")
                if (isLoopAudio && outputId > -1) {
                    val bytebuffer = audioCodec?.getOutputBuffer(outputId)
                    // 拿到原始的pcm数据通过AudioTrack播放
                    val bytes = ByteArray(bytebuffer?.remaining() ?: 0)
                    bytebuffer?.get(bytes)
                    at?.write(bytes, 0, bytes.size)
//                    codec?.releaseOutputBuffer(outputId, bufferInfo.presentationTimeUs * 1000L + startMs)
                    audioCodec?.releaseOutputBuffer(outputId, false)
                }
                me.advance()
            }
            audioCodec?.stop()
            audioCodec?.release()
            audioCodec = null
            at?.stop()
            at = null
            me.release()
        }
    }

    private fun decodeVideo() {
        CoroutineScope(Dispatchers.IO).launch {
            val me = MediaExtractor()
            me.setDataSource("http://172.20.136.111/media/pop-star-kda.mp4")
//                    me.setDataSource("http://172.20.136.111/media/雅俗共赏.mp4")
            val count = me.trackCount
            var videoTrack = -1
            var maxSize = -1
            for (i in 0 until count) {
                val format = me.getTrackFormat(i)
                val type = format.getString(MediaFormat.KEY_MIME)
                val isVideo = type?.contains("video") == true
                Log.d("Krisez", "decodeVideo: $format")
                if (isVideo) {
                    val videoW = format.getInteger(MediaFormat.KEY_WIDTH)
                    val videoH = format.getInteger(MediaFormat.KEY_HEIGHT)
                    val rate = videoW / 1f / videoH
                    binding.surfaceView.post {
                        Log.d("Krisez", "decodeVideoss: $w--$h")
                        Log.d("Krisez", "decodeVideoss: $w--${(w / rate)}")
//                        binding.surfaceView.holder.setFixedSize(w, (w / rate).toInt())
                        val lp = binding.surfaceView.layoutParams
                        lp.height = (w / rate).roundToInt()
                        binding.surfaceView.layoutParams = lp
                    }
//                    if (videoH > videoW) {
//                    }
                    maxSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                    videoTrack = i
                    videoCodec = MediaCodec.createDecoderByType(type ?: "")
                    // 给了surface 会直接把解码后的数据给到surface缓冲区，就拿不到原始数据，设置surface为null 就能拿到原始数据
                    videoCodec?.configure(format, binding.surfaceView.holder.surface, null, 0)
//                    codec.configure(format, null, null, 0)
                    videoCodec?.start()
                }
            }
            me.selectTrack(videoTrack)
            val bufferInfo = MediaCodec.BufferInfo()
            val startMs = System.nanoTime()
            var sampleSize = -1
            var inputBufferId = -1
            val bf = ByteBuffer.allocate(maxSize)
            while (isLoopVideo) {
                inputBufferId = videoCodec?.dequeueInputBuffer(-1) ?: -1
                Log.d("Krisez", "decodeVideo: inputBufferId->$inputBufferId")
                if (inputBufferId >= 0) {
                    sampleSize = me.readSampleData(bf, 0)
                    Log.d("Krisez", "decodeVideo: size-> $sampleSize")
                    val time = me.sampleTime
                    Log.d("Krisez", "decodeVideo: time->$time")
                    if (sampleSize < 0) {
                        videoCodec?.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isLoopVideo = false
                    } else {
                        bufferInfo.presentationTimeUs = me.sampleTime
                        bufferInfo.offset = 0
                        bufferInfo.flags = me.sampleFlags
                        bufferInfo.size = sampleSize
                        videoCodec?.getInputBuffer(inputBufferId)?.put(bf)
                        videoCodec?.queueInputBuffer(inputBufferId, 0, sampleSize, time, 0)
                    }
                }

                val outputId = videoCodec?.dequeueOutputBuffer(bufferInfo, 10000) ?: -1
                Log.d("Krisez", "decodeVideo: outputId->$outputId")
                if (isLoopVideo && outputId > -1) {
                    // 获取原始数据
                    //val out = codec?.getOutputBuffer(outputId)
                    /*val out = codec?.getOutputImage(outputId) //获取image数据方便点
                    // 下面的yuv 需要判断下解析的数据类型 color-format
//                    codec?.outputFormat
                    val y = ByteArray(1920 * 1080)
                    val u = ByteArray((1920 / 2) * (1080 / 2))
                    val v = ByteArray(u.size)
                    out?.get(y, 0, y.size)
                    for (i in 0 until u.size * 2 step 2) {
                        out?.get(v, i / 2, 1)
                        out?.get(u, i / 2, 1)
                    }
                    val data = ByteArray(y.size + u.size + v.size)
                    System.arraycopy(y, 0, data, 0, y.size)
                    // yyyy vu vu
                    for (i in 0 until u.size * 2 step 2) {
                        data[y.size + i] = v[i / 2]
                        data[y.size + i + 1] = u[i / 2]
                    }
                    val outputStream = ByteArrayOutputStream()
                    val image = YuvImage(data, ImageFormat.NV21, 1920, 1080, null)
                    val rect = Rect(0, 0, 1920, 1080)
                    image.compressToJpeg(rect, 100, outputStream)
                    val bitmap = BitmapFactory.decodeByteArray(outputStream.toByteArray(), 0, outputStream.size())
                    Log.d("Krisez", "surfaceCreated: ${bitmap?.height},${bitmap?.width}")
                    val canvas = binding.surfaceView.holder.lockCanvas()
                    canvas?.drawBitmap(bitmap, 0f, 0f, Paint())
                    binding.surfaceView.holder.unlockCanvasAndPost(canvas)
                    bitmap?.recycle()
                    //释放资源
                    codec?.releaseOutputBuffer(outputId, true)*/
                    // over原始数据
                    // 控制时间，直接释放在surface上
                    videoCodec?.releaseOutputBuffer(outputId, bufferInfo.presentationTimeUs * 1000L + startMs)
//                            codec?.releaseOutputBuffer(outputId, me.sampleTime* 1000L  + startMs)  //这样一卡一卡的。。
                } else {
                    Log.d("Krisez", "decodeVideo: end decode")
                }
                me.advance()
            }
            videoCodec?.stop()
            videoCodec?.release()
            videoCodec = null
            me.release()
        }
    }

    override fun onPause() {
        isLoopAudio = false
        isLoopVideo = false
        super.onPause()
    }

    override fun onDestroy() {
        audioCodec?.stop()
        audioCodec?.release()
        at?.stop()
        videoCodec?.stop()
        videoCodec?.release()
        super.onDestroy()
    }
}