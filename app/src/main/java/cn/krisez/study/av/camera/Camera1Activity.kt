package cn.krisez.study.av.camera

import android.graphics.ImageFormat
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import androidx.appcompat.app.AppCompatActivity
import cn.krisez.study.av.databinding.ActivityCamera1Binding


class Camera1Activity : AppCompatActivity() {
    private lateinit var binding: ActivityCamera1Binding
    private var mFrontCameraId = -1
    private lateinit var mFrontCameraInfo: CameraInfo
    private var mBackCameraId = -1
    private lateinit var mBackCameraInfo: CameraInfo
    private var mCamera: Camera? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCamera1Binding.inflate(layoutInflater)
        setContentView(binding.root)
        val num = Camera.getNumberOfCameras()
        Log.d("Camera1Activity", "onCreate: camera num is $num")
        for (i in 0 until num) {
            val info = CameraInfo()
            //获取相机信息
            Camera.getCameraInfo(i, info);
            //前置摄像头
            if (CameraInfo.CAMERA_FACING_FRONT == info.facing) {
                mFrontCameraId = i
                mFrontCameraInfo = info
            } else if (CameraInfo.CAMERA_FACING_BACK == info.facing) {
                mBackCameraId = i
                mBackCameraInfo = info
            }
        }
        mCamera = Camera.open(mBackCameraId)
        adjustCameraOrientation(mBackCameraInfo)
        val sv = binding.surfaceView1
        sv.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {

            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                initPreviewParams(width, height)
                //设置预览 SurfaceHolder
                mCamera?.setPreviewDisplay(holder)
                mCamera?.startPreview()
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
            }

        })

    }

    private fun initPreviewParams(shortSize: Int, longSize: Int) {
        if (mCamera != null) {
            mCamera?.parameters?.run {
                //获取手机支持的尺寸
                val bestSize: Camera.Size = supportedPreviewSizes?.let {
                    getBestSize(shortSize, longSize, it)
                } ?: supportedPreviewSizes[0]
                //设置预览大小
                setPreviewSize(bestSize.width, bestSize.height)
                //设置图片大小，拍照
                setPreviewSize(bestSize.width, bestSize.height)
                setPictureSize(bestSize.width, bestSize.height)
                previewFormat = ImageFormat.NV21
                //设置聚焦
                focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                mCamera?.parameters = this
            }
        }
    }

    /**
     * 获取预览最后尺寸
     */
    private fun getBestSize(shortSize: Int, longSize: Int, sizes: List<Camera.Size>): Camera.Size? {
        var bestSize: Camera.Size? = null
        val uiRatio = longSize.toFloat() / shortSize
        var minRatio = uiRatio
        for (previewSize in sizes) {
            val cameraRatio = previewSize.width.toFloat() / previewSize.height

            //如果找不到比例相同的，找一个最近的,防止预览变形
            val offset = Math.abs(cameraRatio - minRatio)
            if (offset < minRatio) {
                minRatio = offset
                bestSize = previewSize
            }
            //比例相同
            if (uiRatio == cameraRatio) {
                bestSize = previewSize
                break
            }
        }
        return bestSize
    }

    private fun adjustCameraOrientation(info: CameraInfo) {
        //判断当前的横竖屏
        val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.rotation
        } else {
            windowManager.defaultDisplay.rotation
        }
        var degress = 0
        when (rotation) {
            Surface.ROTATION_0 -> degress = 0
            Surface.ROTATION_90 -> degress = 90
            Surface.ROTATION_180 -> degress = 180
            Surface.ROTATION_270 -> degress = 270
        }
        var result = 0
        //后置摄像头
        if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
            result = (info.orientation - degress + 360) % 360
        } else if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            //先镜像
            result = (info.orientation + degress) % 360
            result = (360 - result) % 360
        }
        mCamera!!.setDisplayOrientation(result)
    }

    override fun onDestroy() {
        super.onDestroy()
        mCamera?.stopPreview()
        mCamera?.release()
        mCamera = null
    }
}
