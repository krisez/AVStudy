package cn.krisez.study.av.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.*
import android.media.MediaRecorder.AudioSource
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import cn.krisez.study.av.databinding.ActivityAudioRecordBinding
import cn.krisez.study.av.util.PcmToWavUtil
import kotlinx.coroutines.*
import java.io.*


/**
 * @author krisez
 * audio录制，[AudioRecord]和[MediaRecorder]
 */
@SuppressLint("SetTextI18n")
class AudioRecordActivity : AppCompatActivity(), OnClickListener {
    private val permission = Manifest.permission.RECORD_AUDIO
    private lateinit var binding: ActivityAudioRecordBinding
    private var isPlay = false
    private var isRecord = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tvMediaRecord.setOnClickListener(this)
        binding.tvMediaPlayer.setOnClickListener(this)
        binding.tvAudioRecord.setOnClickListener(this)
        binding.tvAudioTrack.setOnClickListener(this)
        binding.tvAudioRecordPlay.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        if (ActivityCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(permission),
                100
            )
            return
        }
        when (v) {
            binding.tvMediaRecord -> {
                binding.tvMediaRecord.isEnabled = true
                binding.tvAudioRecord.isEnabled = false
                binding.tvMediaPlayer.isEnabled = false
                binding.tvAudioTrack.isEnabled = false
                binding.tvAudioRecordPlay.isEnabled = false
                clickMediaRecord()
            }

            binding.tvMediaPlayer -> {
                binding.tvMediaRecord.isEnabled = false
                binding.tvAudioRecord.isEnabled = false
                binding.tvMediaPlayer.isEnabled = true
                binding.tvAudioTrack.isEnabled = false
                binding.tvAudioRecordPlay.isEnabled = false
                clickMediaPlayer()
            }

            binding.tvAudioRecord -> {
                binding.tvMediaRecord.isEnabled = false
                binding.tvAudioRecord.isEnabled = true
                binding.tvMediaPlayer.isEnabled = false
                binding.tvAudioTrack.isEnabled = false
                binding.tvAudioRecordPlay.isEnabled = false
                clickAudioRecord()
            }

            binding.tvAudioTrack -> {
                binding.tvMediaRecord.isEnabled = false
                binding.tvAudioRecord.isEnabled = false
                binding.tvMediaPlayer.isEnabled = false
                binding.tvAudioTrack.isEnabled = true
                binding.tvAudioRecordPlay.isEnabled = false
                clickAudioTrack()
            }

            binding.tvAudioRecordPlay -> {
                if (!checkHeadsetEnable()) {
                    Toast.makeText(this, "请插入耳机或者使用蓝牙设备", Toast.LENGTH_SHORT).show()
                    return
                }
                binding.tvMediaRecord.isEnabled = false
                binding.tvAudioRecord.isEnabled = false
                binding.tvMediaPlayer.isEnabled = false
                binding.tvAudioTrack.isEnabled = false
                binding.tvAudioRecordPlay.isEnabled = true
               /* CoroutineScope(Dispatchers.Default).launch {
                    while (true) {
                        Log.d("Krisez", "onClick: 检测是否拔耳机")
                        if (!checkHeadsetEnable()) {
                            clickAudioRecord()
                            break
                        }
                        delay(1000)
                    }
                }*/
                clickRecordAndPlay()
            }
        }
    }

    private fun checkHeadsetEnable(): Boolean {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        var isAudioNotLocale = false
        for (device in audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            binding.tvAudioRecordPlay.text = "${binding.tvAudioRecordPlay.text} + ${device.type}\n"
            when (device.type) {
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                AudioDeviceInfo.TYPE_BLE_SPEAKER,
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                    // 插入耳机
                    isAudioNotLocale = true
                }
            }
        }
        return isAudioNotLocale
    }

    /**
     *
     */

    private fun clickRecordAndPlay() {
        if (isRecord) {
            job?.cancel(null)
            ar?.stop()
            ar?.release()
            ar = null
            at?.stop()
            at?.release()
            at = null
            binding.tvAudioRecordPlay.text = "边录边播，输出不能用手机自带扬声器，需要连接其他设备"
            resetAllBtn()
            isRecord = false
            isPlay = false
            return
        }

        minBuffer = AudioRecord.getMinBufferSize(
            frequency,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            ar = AudioRecord(
                AudioSource.MIC,
                frequency,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuffer
            )
            val data = ByteArray(minBuffer)
            ar?.startRecording()
            isRecord = true
            val outMinBuffer = AudioTrack.getMinBufferSize(
                frequency,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val aa = AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
            val af = AudioFormat.Builder()
                .setSampleRate(frequency)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build()
            at = AudioTrack(
                aa,
                af,
                outMinBuffer,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
            at?.play()
            isPlay = true
            try {
                //开启协程，java代码可开启线程
                job = CoroutineScope(Dispatchers.IO).launch {
                    withContext(Dispatchers.IO) {
                        while (isRecord) {
                            val read = ar?.read(data, 0, minBuffer) ?: -1
                            if (read > 0) {
                                try {
                                    val size = data.size
                                    if (size > 0) {
                                        at?.write(data, 0, data.size)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }
                Log.d("AudioRecordActivity", "clickRecordAndPlay: 开始录音${job?.start()}")
                binding.tvAudioRecordPlay.text = "正在录音"
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            Toast.makeText(this, "未授权", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetAllBtn() {
        binding.tvMediaRecord.isEnabled = true
        binding.tvAudioRecord.isEnabled = true
        binding.tvMediaPlayer.isEnabled = true
        binding.tvAudioTrack.isEnabled = true
        binding.tvAudioRecordPlay.isEnabled = true
    }

    /**
     * 点击后通过[MediaRecorder]进行录音，参数为aac相关
     */
    private var mediaRecorder: MediaRecorder? = null
    private fun clickMediaRecord() {
        if (isRecord) {
            mediaRecorder?.stop()
            mediaRecorder?.reset()
            mediaRecorder?.release()
            mediaRecorder = null
            binding.tvMediaRecord.text = "MediaRecord录音"
            Toast.makeText(this, "录音已保存，可点击MediaPlayer播放", Toast.LENGTH_LONG).show()
            isRecord = false
            resetAllBtn()
            return
        }
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            MediaRecorder()
        }
        //设置录音来源
        mediaRecorder?.setAudioSource(AudioSource.MIC)
        //设置输出格式
        mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
        //设置编码格式
        mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        val path = getExternalFilesDir(Environment.DIRECTORY_RINGTONES)
        val file = File(path, "media_record.aac")
        Log.d("AudioRecordActivity", "clickMediaRecord: 文件地址-->${file.path}")
        if (file.exists()) {
            Log.d("AudioRecordActivity", "删除已有文件: ${file.path}")
            Log.d("AudioRecordActivity", "删除已有文件: ${file.delete()}")
        }
        //设置输出路径
        mediaRecorder?.setOutputFile(file.path)
        try {
            mediaRecorder?.prepare()
            mediaRecorder?.start()
            binding.tvMediaRecord.text = "正在录音"
            isRecord = true
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }

    }

    /**
     * 点击后将[MediaRecorder]录制的音频文件通过[MediaPlayer]播放，播放一次后结束
     */
    private var mp: MediaPlayer? = null
    private fun clickMediaPlayer() {
        if (isPlay) {
            mp?.stop()
            mp?.reset()
            mp?.release()
            mp = null
            binding.tvMediaPlayer.text = "MediaPlayer播放"
            isPlay = false
            resetAllBtn()
            return
        }
        val path = getExternalFilesDir(Environment.DIRECTORY_RINGTONES)
        val file = File(path, "media_record.aac")
        if (file.exists()) {
            mp = MediaPlayer()
            mp?.setDataSource(file.path)
            try {
                mp?.setOnCompletionListener {
                    binding.tvMediaPlayer.performClick()
                }
                mp?.setOnPreparedListener {
                    mp?.isLooping = false
                    mp?.start()
                }
                mp?.prepare()
                isPlay = true
                binding.tvMediaPlayer.text = "正在播放"
            } catch (e: Exception) {
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                mp = null
            }
        } else {
            Toast.makeText(this, "文件不存在，请先录制", Toast.LENGTH_SHORT).show()
            resetAllBtn()
        }
    }

    /**
     * 点击后通过[AudioRecord]进行音频录制，该方法更加贴近底层
     * [参考网址](https://www.cnblogs.com/renhui/p/7457321.html).
     * 该方法未处理PCM数据，可参考PCM转换成WAV格式[PcmToWavUtil]
     */
    private val frequency = 44100
    private var ar: AudioRecord? = null
    private var minBuffer = 0
    private var job: Job? = null
    private var fos: FileOutputStream? = null

    private fun clickAudioRecord() {
        if (isRecord) {
            isRecord = false
            Log.d("AudioRecordActivity", "clickAudioRecord: 取消协程${job?.isActive},停止录音")
            job?.cancel(null)
            ar?.stop()
            ar?.release()
            ar = null
            try {
                fos?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            binding.tvAudioRecord.text = "AudioRecord录音"
            resetAllBtn()
            Toast.makeText(this, "录音完成，请用AudioTrack播放试听", Toast.LENGTH_SHORT).show()

            //pcm文件添加head数据转换成wav文件。
            /*val util = PcmToWavUtil(
                frequency,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            util.pcmToWav(
                File(getExternalFilesDir(Environment.DIRECTORY_RINGTONES), "audio_record.pcm"),
                File(getExternalFilesDir(Environment.DIRECTORY_RINGTONES), "audio_record.wav")
            )*/
            return
        }
        minBuffer = AudioRecord.getMinBufferSize(
            frequency,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            ar = AudioRecord(
                AudioSource.MIC,
                frequency,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuffer
            )
            val data = ByteArray(minBuffer)
            ar?.startRecording()
            isRecord = true
            try {
                val file =
                    File(getExternalFilesDir(Environment.DIRECTORY_RINGTONES), "audio_record.pcm")
                Log.d("AudioRecordActivity", "clickAudioRecord: 文件地址-->${file.path}")
                if (file.exists()) {
                    Log.d("AudioRecordActivity", "删除已有文件: ${file.path}")
                    Log.d("AudioRecordActivity", "删除已有文件: ${file.delete()}")
                }
                fos = FileOutputStream(file)
                //开启协程，java代码可开启线程
                job = CoroutineScope(Dispatchers.IO).launch {
                    withContext(Dispatchers.IO) {
                        while (isRecord) {
                            val read = ar?.read(data, 0, minBuffer) ?: -1
                            if (read > 0) {
                                try {
                                    fos?.write(data)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }
                Log.d("AudioRecordActivity", "clickAudioRecord: 开始录音${job?.start()}")
                binding.tvAudioRecord.text = "正在录音"
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            Toast.makeText(this, "未授权", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 点击后通过[AudioTrack]对[AudioRecord]录制的音频播放，[网址](https://www.cnblogs.com/renhui/p/7463287.html)
     * 只能播放解码后的PCM流
     */
    private var at: AudioTrack? = null
    private var outJob: Job? = null
    private var fis: FileInputStream? = null
    private fun clickAudioTrack() {
        val file = File(getExternalFilesDir(Environment.DIRECTORY_RINGTONES), "audio_record.pcm")
        if (!file.exists()) {
            Toast.makeText(this, "文件不存在，请先录制", Toast.LENGTH_SHORT).show()
            resetAllBtn()
            return
        }
        if (isPlay) {
            isPlay = false
            Log.d("AudioRecordActivity", "clickAudioTrack: 取消协程${outJob?.isActive},停止录音")
            outJob?.cancel(null)
            at?.stop()
            at?.release()
            at = null
            try {
                fis?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            binding.tvAudioTrack.text = "AudioTrack播放"
            resetAllBtn()
            return
        }
        minBuffer = AudioTrack.getMinBufferSize(
            frequency,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val aa = AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()
        val af = AudioFormat.Builder()
            .setSampleRate(frequency)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build()
        outJob = CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                val fileSize = file.length()
                Log.i("AudioRecordActivity", "clickAudioTrack: File size is $fileSize,choose mode.")
                //5mb的分割线
                if (fileSize > 5 * 1024 * 1024) {
                    Log.i("AudioRecordActivity", "clickAudioTrack: mode stream")
                    //MODE_STREAM写法，适用内存不足，文件太大的时候用
                    at = AudioTrack(
                        aa,
                        af,
                        minBuffer,
                        AudioTrack.MODE_STREAM,
                        AudioManager.AUDIO_SESSION_ID_GENERATE
                    )
                    isPlay = true
                    fis = FileInputStream(file)
                    val data = ByteArray(minBuffer)
                    val dataInputStream = DataInputStream(fis)
                    at?.play()
                    while (isPlay) {
                        val len = dataInputStream.read(data)
                        if (len == -1) {
                            break
                        }
                        at?.write(data, 0, len)
                    }
                    try {
                        dataInputStream.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else if (fileSize > 0) {
                    Log.i("AudioRecordActivity", "clickAudioTrack: mode static")
                    //MODE_STATIC写法，适用内存足够，文件够小的时候用
                    fis = FileInputStream(file)
                    val size = fis?.channel?.size()?.toInt() ?: 0
                    if (size > 0) {
                        val baos = ByteArrayOutputStream(size)
                        var value: Int
                        Log.d("AudioRecordActivity", "clickAudioTrack: 读取数据...")
                        while (true) {
                            value = fis?.read() ?: -1
                            if (value != -1) {
                                baos.write(value)
                            } else {
                                break
                            }
                        }
                        val data = baos.toByteArray()
                        try {
                            fis?.close()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        at = AudioTrack(
                            aa,
                            af,
                            data.size,
                            AudioTrack.MODE_STATIC,
                            AudioManager.AUDIO_SESSION_ID_GENERATE
                        )
                        at?.write(data, 0, data.size)
                        Log.d("AudioRecordActivity", "clickAudioTrack: 播放PCM")
                        at?.play()
                        isPlay = true
                    }
                } else {
                    Log.e(
                        "AudioRecordActivity",
                        "clickAudioTrack: file '${file.path}' is not available,please check it."
                    )
                }
            }
        }
        binding.tvAudioTrack.text = "正在播放"
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var isGrant = true
        grantResults.forEach {
            if (it != PackageManager.PERMISSION_GRANTED) {
                isGrant = false
            }
        }
        if (!isGrant) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                AlertDialog.Builder(this)
                    .setTitle("无权限")
                    .setMessage("无法获取录音权限，请至设置->应用->权限管理授予对应权限。")
                    .setPositiveButton("去设置") { d, _ ->
                        d.dismiss()
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.parse("package:${packageName}")
                        startActivity(intent)
                    }
                    .setNegativeButton("取消") { d, _ ->
                        d.dismiss()
                    }
                    .show()
            }
        }
    }


    override fun onDestroy() {
        mediaRecorder?.stop()
        mediaRecorder?.reset()
        mediaRecorder?.release()
        mp?.stop()
        mp?.reset()
        mp?.release()
        job?.cancel(null)
        ar?.stop()
        ar?.release()
        outJob?.cancel(null)
        at?.stop()
        at?.release()
        try {
            fis?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }
}