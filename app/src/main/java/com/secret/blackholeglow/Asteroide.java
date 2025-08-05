// Asteroide.java
package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.util.ObjLoader;
import com.secret.blackholeglow.util.ObjLoader.Mesh;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.List;
import java.util.Random;

/**
 * ====================================================================
 * Asteroide
 * ====================================================================
 *  • Aparece cada [SPAWN_MIN..SPAWN_MAX]s
 *  • Viaja en Z de Z_START→Z_END a Z_SPEED m/s
 *  • Escala easing de SCALE_MIN→SCALE_MAX
 *  • Sale al lado izquierdo o derecho garantizado
 *  • Nunca desaparece hasta el próximo spawn
 */
public class Asteroide extends BaseShaderProgram implements SceneObject {
    private static final String TAG = "Asteroide";

    // --- CONFIGURABLES ---
    private static final float Z_START     = -50f;   // Z inicial (m)
    private static final float Z_END       =  50f;   // Z final (m)
    private static final float Z_SPEED     =   12f;   // velocidad Z (m/s)

    private static final float SCALE_MIN   = 0.01f;  // escala mínima (m)
    private static final float SCALE_MAX   = 1.5f;   // escala máxima (m)

    private static final float SPAWN_MIN   =  6f;    // intervalo spawn mínimo (s)
    private static final float SPAWN_MAX   =  8f;    // intervalo spawn máximo (s)

    private static final float DRIFT_AMP   = 0.2f;   // deriva lateral adimensional
    private static final float EXIT_FACTOR = 1.5f;   // factor para garantizar salida lateral

    // --- LÓGICA INTERNA ---
    private final float instanceScale;
    private final boolean useSolidColor;
    private final float[] solidColor;
    private final float alpha, uvScale;

    private final int textureId;
    private final FloatBuffer vertexBuffer, texCoordBuffer;
    private final ShortBuffer indexBuffer;
    private final int indexCount;
    private final int aPosLoc, aTexLoc;
    private final int uMvpLoc, uTexLoc, uUseSolidColorLoc,
            uSolidColorLoc, uAlphaLoc, uUvScaleLoc;

    private final Random rand = new Random();
    private float timer     = 0f;    // s desde último spawn
    private float nextSpawn = 0f;    // s para próximo spawn
    private boolean active  = false; // si está volando
    private float zPos      = Z_START;
    private float baseX, baseY;      // punto objetivo lateral (±1)

    // Matrices de trabajo
    private final float[] model = new float[16],
            view  = new float[16],
            proj  = new float[16],
            mv    = new float[16],
            mvp   = new float[16];

    public Asteroide(Context ctx,
                     TextureManager texMgr,
                     String vertAsset,
                     String fragAsset,
                     int textureResId,
                     float instanceScale,
                     boolean useSolidColor,
                     float[] solidColor,
                     float alpha,
                     float uvScale) {
        super(ctx, vertAsset, fragAsset);
        this.instanceScale = instanceScale;
        this.useSolidColor = useSolidColor;
        this.solidColor    = solidColor!=null?solidColor:new float[]{1f,1f,1f,1f};
        this.alpha         = alpha;
        this.uvScale       = uvScale;

        // textura
        textureId = texMgr.getTexture(textureResId);

        // malla
        Mesh mesh;
        try {
            mesh = ObjLoader.loadObj(ctx,"asteroide.obj");
        } catch(IOException e){
            throw new RuntimeException("Error cargando asteroide.obj",e);
        }
        vertexBuffer   = mesh.vertexBuffer;
        texCoordBuffer = mesh.uvBuffer;

        // índices
        List<short[]> faces = mesh.faces;
        int triCount=0;
        for(short[] f:faces) triCount+=f.length-2;
        indexCount = triCount*3;
        ShortBuffer ib = ByteBuffer
                .allocateDirect(indexCount*Short.BYTES)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        for(short[] f:faces){
            short v0=f[0];
            for(int i=1;i<f.length-1;i++){
                ib.put(v0).put(f[i]).put(f[i+1]);
            }
        }
        ib.position(0);
        indexBuffer = ib;

        // locations
        aPosLoc           = GLES20.glGetAttribLocation(programId, "a_Position");
        aTexLoc           = GLES20.glGetAttribLocation(programId, "a_TexCoord");
        uMvpLoc           = GLES20.glGetUniformLocation(programId,"u_MVP");
        uTexLoc           = GLES20.glGetUniformLocation(programId,"u_Texture");
        uUseSolidColorLoc = GLES20.glGetUniformLocation(programId,"u_UseSolidColor");
        uSolidColorLoc    = GLES20.glGetUniformLocation(programId,"u_SolidColor");
        uAlphaLoc         = GLES20.glGetUniformLocation(programId,"u_Alpha");
        uUvScaleLoc       = GLES20.glGetUniformLocation(programId,"u_UvScale");

        scheduleNextSpawn();
    }

    private void scheduleNextSpawn(){
        active    = false;
        timer     = 0f;
        zPos      = Z_START;
        nextSpawn = SPAWN_MIN + rand.nextFloat()*(SPAWN_MAX-SPAWN_MIN);
        // Elige dirección lateral fuera del centro (|baseX|>0.3)
        do {
            baseX = rand.nextFloat()*2f-1f;
            baseY = rand.nextFloat()*2f-1f;
        } while(Math.abs(baseX)<0.3f);
        Log.d(TAG,String.format(
                "Próximo spawn en %.2f s hacia X=%.2f", nextSpawn, baseX));
    }

    @Override
    public void update(float dt){
        timer += dt;
        if(!active){
            if(timer>=nextSpawn){
                active = true;
                timer  = 0f;
                Log.d(TAG,"Asteroide spawned");
            } else return;
        }
        // Avanza Z
        zPos += Z_SPEED*dt;
        // Sólo reinicia con el siguiente spawn
        if(timer>=nextSpawn){
            scheduleNextSpawn();
        }
    }

    @Override
    public void draw(){
        if(!active) return;
        useProgram();

        // progreso p [0..1]
        float p = (zPos-Z_START)/(Z_END-Z_START);
        p = Math.max(0f,Math.min(1f,p));
        // easing
        float e = p<0.5f?4*p*p*p:1f-(float)Math.pow(-2f*p+2f,3)/2f;
        // escala
        float frac = SCALE_MIN + e*(SCALE_MAX-SCALE_MIN);
        float finalScale = Math.min(frac*instanceScale,SCALE_MAX);

        // cámara ortho fija
        float aspect=(float)SceneRenderer.screenWidth/SceneRenderer.screenHeight;
        Matrix.orthoM(proj,0,-aspect,aspect,-1f,1f,0.1f,100f);
        Matrix.setLookAtM(view,0,0f,0f,30f,0f,0f,0f,0f,1f,0f);

        // deriva + salida lateral:
        float dx = DRIFT_AMP*(float)Math.sin(e*2f*Math.PI)*(1f-p);
        float actualX = baseX*(p*EXIT_FACTOR) + dx;
        float actualY = baseY*(1f-p);

        // modelo
        Matrix.setIdentityM(model,0);
        Matrix.translateM(model,0,actualX,actualY,zPos);
        Matrix.scaleM(model,0,finalScale,finalScale,finalScale);
        Matrix.multiplyMM(mv,0,view,0,model,0);
        Matrix.multiplyMM(mvp,0,proj,0,mv,0);
        GLES20.glUniformMatrix4fv(uMvpLoc,1,false,mvp,0);

        // uniforms color/alpha/uvScale
        GLES20.glUniform1i(uUseSolidColorLoc,useSolidColor?1:0);
        GLES20.glUniform4fv(uSolidColorLoc,1,solidColor,0);
        GLES20.glUniform1f(uAlphaLoc,alpha);
        GLES20.glUniform1f(uUvScaleLoc,uvScale);

        // bind + draw
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureId);
        GLES20.glUniform1i(uTexLoc,0);

        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(aPosLoc,3,GLES20.GL_FLOAT,false,0,vertexBuffer);
        texCoordBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aTexLoc);
        GLES20.glVertexAttribPointer(aTexLoc,2,GLES20.GL_FLOAT,false,0,texCoordBuffer);
        indexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES,indexCount,GLES20.GL_UNSIGNED_SHORT,indexBuffer);
        GLES20.glDisableVertexAttribArray(aPosLoc);
        GLES20.glDisableVertexAttribArray(aTexLoc);
    }
}
