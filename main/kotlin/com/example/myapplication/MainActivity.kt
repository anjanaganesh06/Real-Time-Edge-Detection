// app/src/main/java/com/example/myapplication/MainActivity.kt
package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Size
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import android.widget.FrameLayout
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var glView: MyGLSurfaceView
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var outBuffer: ByteBuffer? = null
    private var lastW = 0
    private var lastH = 0
    private var useCanny = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Simple FrameLayout container
        val container = FrameLayout(this)
        setContentView(container)
        glView = MyGLSurfaceView(this)
        container.addView(glView)

        // Permission
        val requestPermission = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) startCamera() else finish()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermission.launch(Manifest.permission.CAMERA)
        } else {
            startCamera()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .setTargetResolution(Size(640, 480))
                .build()

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                processImageProxy(imageProxy)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImageProxy(image: ImageProxy) {
        val w = image.width
        val h = image.height

        // allocate output direct buffer if needed
        if (outBuffer == null || lastW != w || lastH != h) {
            outBuffer = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder())
            lastW = w
            lastH = h
        }

        // convert YUV_420_888 to NV21 robustly:
        val nv21 = yuv420ToNv21(image)

        // call native
        MyNativeLib.processFrameNV21(nv21, w, h, outBuffer!!, useCanny)

        // give buffer to GL
        runOnUiThread {
            glView.setFrameBuffer(w, h, outBuffer!!)
        }

        image.close()
    }

    private fun yuv420ToNv21(image: ImageProxy): ByteArray {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)

        // Copy Y
        yBuffer.get(nv21, 0, ySize)

        // U and V may have pixelStride != 1 and rowStride != width/2. Build VU interleaved.
        val pixelStrideU = uPlane.pixelStride
        val rowStrideU = uPlane.rowStride
        val pixelStrideV = vPlane.pixelStride
        val rowStrideV = vPlane.rowStride

        // Interleave VU into nv21 starting at ySize
        var pos = ySize
        val chromaHeight = image.height / 2
        val chromaWidth = image.width / 2



        val rowU = ByteArray(rowStrideU)
        val rowV = ByteArray(rowStrideV)

        for (r in 0 until chromaHeight) {
            uBuffer.position(r * rowStrideU)
            vBuffer.position(r * rowStrideV)
            uBuffer.get(rowU, 0, rowStrideU)
            vBuffer.get(rowV, 0, rowStrideV)

            var col = 0
            while (col < chromaWidth) {
                // NV21 wants V then U
                val v = rowV[col * pixelStrideV]
                val u = rowU[col * pixelStrideU]
                nv21[pos++] = v
                nv21[pos++] = u
                col++
            }
        }

        return nv21
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
