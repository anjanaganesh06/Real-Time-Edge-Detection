package com.example.myapplication;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.egl.EGLConfig;

public class MyGLRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "MyGLRenderer";
    private int textureId = -1;
    private int program;
    private int positionHandle, texCoordHandle, textureUniform;
    private FloatBuffer vertexBuffer, texBuffer;
    private int viewWidth = 0, viewHeight = 0;
    private int frameWidth = 0, frameHeight = 0;
    private ByteBuffer processedFrameBuffer = null;
    private volatile boolean frameAvailable = false;
    private boolean useCanny = true;
    private final Context context;

    private static final float[] VERTICES = {
            -1f, 1f,
            -1f, -1f,
            1f, 1f,
            1f, -1f
    };

    private static final float[] TEXCOORDS = {
            0f, 0f,
            0f, 1f,
            1f, 0f,
            1f, 1f
    };

    public MyGLRenderer(Context ctx) {
        this.context = ctx;
        vertexBuffer = ByteBuffer.allocateDirect(VERTICES.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.put(VERTICES).position(0);

        texBuffer = ByteBuffer.allocateDirect(TEXCOORDS.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        texBuffer.put(TEXCOORDS).position(0);
    }

    public void setUseCanny(boolean use) {
        this.useCanny = use;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        positionHandle = GLES20.glGetAttribLocation(program, "a_Position");
        texCoordHandle = GLES20.glGetAttribLocation(program, "a_TexCoord");
        textureUniform = GLES20.glGetUniformLocation(program, "u_Texture");

        // generate texture
        int[] tex = new int[1];
        GLES20.glGenTextures(1, tex, 0);
        textureId = tex[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glClearColor(0f, 0f, 0f, 1f);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        viewWidth = width;
        viewHeight = height;
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        synchronized (this) {
            if (frameAvailable && processedFrameBuffer != null) {
                processedFrameBuffer.position(0);
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

                // upload pixels - RGBA unsigned byte
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                        frameWidth, frameHeight, 0, GLES20.GL_RGBA,
                        GLES20.GL_UNSIGNED_BYTE, processedFrameBuffer);

                frameAvailable = false;
            }
        }

        GLES20.glUseProgram(program);

        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        texBuffer.position(0);
        GLES20.glEnableVertexAttribArray(texCoordHandle);
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texBuffer);

        GLES20.glUniform1i(textureUniform, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(texCoordHandle);
    }

    public synchronized void setFrame(int width, int height, ByteBuffer frame) {
        this.frameWidth = width;
        this.frameHeight = height;
        this.processedFrameBuffer = frame;
        this.frameAvailable = true;
        saveFrameToFile(frame, width, height);

    }

    // --- Shader helpers
    private static final String VERTEX_SHADER =
            "attribute vec4 a_Position; attribute vec2 a_TexCoord; varying vec2 v_TexCoord; " +
                    "void main(){ gl_Position = a_Position; v_TexCoord = a_TexCoord; }";

    private static final String FRAGMENT_SHADER =
            "precision mediump float; varying vec2 v_TexCoord; uniform sampler2D u_Texture; " +
                    "void main(){ gl_FragColor = texture2D(u_Texture, v_TexCoord); }";

    private int loadShader(int type, String shaderSrc) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderSrc);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile shader " + type + ":");
            Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }

    private int createProgram(String vtxSrc, String fragSrc) {
        int vert = loadShader(GLES20.GL_VERTEX_SHADER, vtxSrc);
        int frag = loadShader(GLES20.GL_FRAGMENT_SHADER, fragSrc);
        int prog = GLES20.glCreateProgram();
        GLES20.glAttachShader(prog, vert);
        GLES20.glAttachShader(prog, frag);
        GLES20.glLinkProgram(prog);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Could not link program: ");
            Log.e(TAG, GLES20.glGetProgramInfoLog(prog));
            GLES20.glDeleteProgram(prog);
            return 0;
        }
        return prog;
    }
    private void saveFrameToFile(ByteBuffer rgbaBuffer, int width, int height) {
        try {
            byte[] bytes = new byte[width * height * 4];
            rgbaBuffer.position(0);
            rgbaBuffer.get(bytes);

            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bmp.copyPixelsFromBuffer(ByteBuffer.wrap(bytes));

            File file = new File(Environment.getExternalStorageDirectory(), "processed_frame.png");
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            Log.d("SAVE", "Saved frame to: " + file.getAbsolutePath());
        }
        catch (Exception e) {
            Log.e("SAVE", "Error saving frame", e);
        }
    }

}
