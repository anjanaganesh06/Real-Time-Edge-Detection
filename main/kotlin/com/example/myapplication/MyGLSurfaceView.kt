package com.example.myapplication

import android.content.Context
import android.opengl.GLSurfaceView

class MyGLSurfaceView(context: Context) : GLSurfaceView(context) {
    private val renderer = MyGLRenderer(context)

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun setFrameBuffer(width: Int, height: Int, buffer: java.nio.ByteBuffer) {
        renderer.setFrame(width, height, buffer)
        requestRender()
    }

    fun setUseCanny(use: Boolean) {
        renderer.setUseCanny(use)
    }
}
