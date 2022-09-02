package cn.krisez.study.av.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceHolder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import cn.krisez.study.av.databinding.ActivityCamera2Binding

class Camera2Activity : AppCompatActivity() {
    private lateinit var binding: ActivityCamera2Binding
    private var flashSupport = false
    private var mCameraId = ""
    private var mCameraDevice: CameraDevice? = null
    private var mSession: CameraCaptureSession? = null
    private val mHandler = Handler(Looper.getMainLooper()) {
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCamera2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        //选择一个目标surface
        // 1. SurfaceView, 直接面向用户
        // 2. ImageReader, 需要对帧数据进行逐帧分析/处理
        // 3. OpenGL Texture or TextureView, 因为扩展性不建议使用
        // 4. RenderScript.Allocation, 并行处理（没懂）
        val sv = binding.surfaceView
        sv.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                if (ActivityCompat.checkSelfPermission(
                        this@Camera2Activity,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val manager = getSystemService(CAMERA_SERVICE) as CameraManager
                    val characteristics = manager.getCameraCharacteristics(mCameraId)
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        ?.let { map ->
                            //获取size数据
                            val size = map.getOutputSizes(SurfaceHolder::class.java)
                            size.forEach {
                                Log.d("Krisez", "size for: width ${it.width} height ${it.height}")
                            }
                            val h = sv.height
                            val w = sv.width
                            val s = size.firstOrNull {
                                it.width < h && it.height < w
                            }
                            Log.d("Krisez", "size for: width ${s?.width} height ${s?.height}")
                            Log.d("Krisez", "size for: width $w $h")
                            val lp = sv.layoutParams
                            lp.height = s?.width ?: h
                            lp.width = s?.height ?: w
                            sv.layoutParams = lp
                        }
                    manager.openCamera(mCameraId, object : CameraDevice.StateCallback() {
                        override fun onOpened(camera: CameraDevice) {
                            mCameraDevice = camera
                            createPreviewSession()
                        }

                        override fun onDisconnected(camera: CameraDevice) {
                            camera.close()
                            mCameraDevice = null
                        }

                        override fun onError(camera: CameraDevice, error: Int) {
                            camera.close()
                            mCameraDevice = null
                        }

                    }, null)
                }

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
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            manager.cameraIdList.forEach { id ->
                Log.d("Krisez", "onCreate: cameraId -> $id")
                val characteristics = manager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                val level = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                Log.d("Krisez", "onCreate: face,level -> $facing---$level")
                facing?.let {
                    if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                        val available =
                            characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                        flashSupport = available ?: false
                        mCameraId = id
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        binding.imageCaptureButton.setOnClickListener {
            mCameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)?.let {
                if(imageReader?.surface != null){
                    it.addTarget(imageReader?.surface!!)
                    mSession?.stopRepeating()
                    mSession?.capture(it.build(), object : CameraCaptureSession.CaptureCallback() {
                        override fun onCaptureCompleted(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            result: TotalCaptureResult
                        ) {
                            Log.d("Krisez", "onCaptureCompleted: ")
                            createPreviewSession()
                        }
                    }, mHandler)
                }
            }
        }
        binding.videoCaptureButton.setOnClickListener {

        }
    }

    private var imageReader: ImageReader? = null
    private fun createPreviewSession() {
        val previewSurface = binding.surfaceView.holder.surface
        imageReader =
            ImageReader.newInstance(
                binding.surfaceView.width,
                binding.surfaceView.height,
                ImageFormat.YUV_420_888,
                2
            )
        imageReader?.setOnImageAvailableListener({
            //解码、拍照用，需要注意处理
            val img = it.acquireLatestImage()
            Log.d("Krisez", "createPreviewSession: $img")
            img.close()
        }, mHandler)
        val imReaderSurface = imageReader?.surface
        val builder = mCameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        builder?.addTarget(previewSurface)
        imReaderSurface?.let {
            builder?.addTarget(it)
        }
        val stateCallback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                mCameraDevice?.let {
                    mSession = session
                    builder?.set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                    )
                    builder?.build()?.let {
                        Log.d("Krisez", "onConfigured: ")
                        mSession?.setRepeatingRequest(it, null, mHandler)
                    }
                }
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Log.d("Krisez", "onConfigureFailed: error")
            }

        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            //该方法过时
            mCameraDevice?.createCaptureSession(
                listOf(previewSurface, imReaderSurface),
                stateCallback,
                mHandler
            )
        } else {
            imReaderSurface?.let {
                mCameraDevice?.createCaptureSession(
                    SessionConfiguration(
                        SessionConfiguration.SESSION_REGULAR,
                        listOf(
                            OutputConfiguration(previewSurface),
                            OutputConfiguration(imReaderSurface)
                        ),
                        { c -> c.run() },
                        stateCallback
                    )
                )
            }
        }
    }
}
