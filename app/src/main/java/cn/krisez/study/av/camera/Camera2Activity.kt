package cn.krisez.study.av.camera

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import cn.krisez.study.av.databinding.ActivityCamera2Binding
import java.util.concurrent.Executor

class Camera2Activity : AppCompatActivity() {
    private lateinit var binding: ActivityCamera2Binding
    private var flashSupport = false
    private var mCameraId = ""
    private var mCameraDevice: CameraDevice? = null
    private var mSession: CameraCaptureSession? = null
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
                        /*characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                            ?.let {map->
                                val available =
                                    characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)

                            }*/
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
    }

    private fun createPreviewSession() {
        /*val imageReader =
                    ImageReader.newInstance(sv.width, sv.height, ImageFormat.YUV_420_888, 10)
                // 在SurfaceHolder.Callback.surfaceCreated()之后调用
                val previewSurface = sv.holder.surface
                val imReaderSurface = imageReader.surface
                val targets = listOf(previewSurface, imReaderSurface)*/
        val previewSurface = binding.surfaceView.holder.surface
        val builder = mCameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        builder?.addTarget(previewSurface)
        val stateCallback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                mCameraDevice?.let {
                    mSession = session
                    builder?.set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                    )
                    val request = builder?.build()?.let {
                        Log.d("Krisez", "onConfigured: ")
                        mSession?.setRepeatingRequest(it, null, null)
                    }
                }
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Log.d("Krisez", "onConfigureFailed: error")
            }

        }
        //该方法过时
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            mCameraDevice?.createCaptureSession(
                listOf(previewSurface),
                stateCallback,
                null
            )
        } else {
            mCameraDevice?.createCaptureSession(
                SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR,
                    listOf(OutputConfiguration(previewSurface)),
                    { c -> c.run() },
                    stateCallback
                )
            )
        }
    }
}
