package com.secret.blackholeglow;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.opengl.GLES20;

public class ShaderUtils {

    public static int createBuffer(float[] data) {
        // Crear un FloatBuffer
        FloatBuffer buffer = ByteBuffer.allocateDirect(data.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buffer.put(data).position(0);

        // Crear un VBO (Vertex Buffer Object) en OpenGL
        int[] bufferIds = new int[1];
        GLES20.glGenBuffers(1, bufferIds, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferIds[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, buffer.capacity() * 4, buffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        return bufferIds[0];
    }
}