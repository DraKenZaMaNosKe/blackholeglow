package com.secret.blackholeglow;

import android.opengl.GLES20;
import android.opengl.Matrix;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

public class RotatingTexturedCubeBackground implements SceneObject {
    private final int program;
    private final int aPosLoc;
    private final int uMVPMatrixLoc, uColorLoc;
    private final FloatBuffer vBuffer;
    private final ShortBuffer lineIdxBuffer;
    private final int lineIndexCount;
    private final float[] proj = new float[16];
    private final float[] view = new float[16];
    private final float[] model = new float[16];
    private final float[] tmp = new float[16];
    private final float[] mvp = new float[16];
    private float angle = 0f;
    private float yawOverride = 0f;

    public RotatingTexturedCubeBackground(TextureManager ignore) {
        String vShader =
                "attribute vec4 a_Position;\n" +
                        "uniform mat4 u_MVP;\n" +
                        "void main(){ gl_Position = u_MVP * a_Position; }";
        String fShader =
                "precision mediump float;\n" +
                        "uniform vec4 u_Color;\n" +
                        "void main(){ gl_FragColor = u_Color; }";
        program = ShaderUtils.createProgram(vShader, fShader);
        aPosLoc = GLES20.glGetAttribLocation(program, "a_Position");
        uMVPMatrixLoc = GLES20.glGetUniformLocation(program, "u_MVP");
        uColorLoc = GLES20.glGetUniformLocation(program, "u_Color");
        // generar malla de esfera lat/long...
        List<Float> verts = new ArrayList<>();
        int latBands=20, longBands=20;
        for(int lat=0;lat<=latBands;lat++){
            float theta = (float)(Math.PI*lat/latBands);
            float sinT = (float)Math.sin(theta), cosT=(float)Math.cos(theta);
            for(int lon=0;lon<=longBands;lon++){
                float phi = (float)(2*Math.PI*lon/longBands);
                verts.add(sinT*(float)Math.cos(phi));
                verts.add(cosT);
                verts.add(sinT*(float)Math.sin(phi));
            }
        }
        float[] vArr=new float[verts.size()];for(int i=0;i<vArr.length;i++)vArr[i]=verts.get(i);
        List<Short> lines=new ArrayList<>();
        int rowLen=longBands+1;
        for(int lat=0;lat<=latBands;lat++){
            int base=lat*rowLen;
            for(int lon=0;lon<longBands;lon++){
                lines.add((short)(base+lon)); lines.add((short)(base+lon+1));
            }
        }
        for(int lon=0;lon<=longBands;lon++){
            for(int lat=0;lat<latBands;lat++){
                lines.add((short)(lat*rowLen+lon));
                lines.add((short)(((lat+1)*rowLen+lon)));
            }
        }
        lineIndexCount=lines.size();
        float[] tmpV=new float[vArr.length];System.arraycopy(vArr,0,tmpV,0,vArr.length);
        short[] tmpI=new short[lineIndexCount];for(int i=0;i<tmpI.length;i++)tmpI[i]=lines.get(i);
        vBuffer=ByteBuffer.allocateDirect(tmpV.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(tmpV);
        vBuffer.position(0);
        lineIdxBuffer=ByteBuffer.allocateDirect(tmpI.length*2).order(ByteOrder.nativeOrder()).asShortBuffer().put(tmpI);
        lineIdxBuffer.position(0);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    public void yawOverride(float deg){ yawOverride+=deg; }

    @Override
    public void update(float dt) {
        angle += 30f*dt;
        angle += yawOverride;
        yawOverride = 0f;
        if(angle>360f) angle-=360f;
    }

    @Override
    public void draw() {
        GLES20.glClearColor(0f,0f,0f,1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(program);
        float aspect=(float)SceneRenderer.screenWidth/SceneRenderer.screenHeight;
        Matrix.perspectiveM(proj,0,45,aspect,1f,20f);
        Matrix.setLookAtM(view,0,0f,0f,-4f,0f,0f,0f,0f,1f,0f);
        Matrix.setIdentityM(model,0);
        Matrix.rotateM(model,0,angle,0f,1f,0f);
        Matrix.multiplyMM(tmp,0,view,0,model,0);
        Matrix.multiplyMM(mvp,0,proj,0,tmp,0);
        GLES20.glUniformMatrix4fv(uMVPMatrixLoc,1,false,mvp,0);
        GLES20.glUniform4f(uColorLoc,0f,1f,0f,1f);
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(aPosLoc,3,GLES20.GL_FLOAT,false,3*4,vBuffer);
        GLES20.glLineWidth(1.5f);
        GLES20.glDrawElements(GLES20.GL_LINES,lineIndexCount,GLES20.GL_UNSIGNED_SHORT,lineIdxBuffer);
        GLES20.glDisableVertexAttribArray(aPosLoc);
    }
}
