package com.example.myapplication;
import java.nio.ByteBuffer;

public class MyNativeLib {
    static {
        System.loadLibrary("native-lib"); // name from CMakeLists
    }

    // inputNV21: Java byte[] of NV21 (Y+VU)
    // outDirectBuffer: ByteBuffer.allocateDirect(width*height*4) (RGBA)
    public static native void processFrameNV21(
            byte[] inputNV21, int width, int height, ByteBuffer outDirectBuffer,boolean useCanny);
}
