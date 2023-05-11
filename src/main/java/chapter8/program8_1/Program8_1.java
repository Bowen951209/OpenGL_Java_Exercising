package chapter8.program8_1;


import chapter6.Torus;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI;
import utilities.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;

import static org.joml.Math.toRadians;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL43.*;


public class Program8_1 {
    private static long windowHandle;

    private static Matrix4f camProjMat;

    private static final FloatBuffer valsOf16 = BufferUtils.createFloatBuffer(16);// utility buffer for transferring matrices
    private static final FloatBuffer valsOf3 = BufferUtils.createFloatBuffer(3);
    private static final int[] vbo = new int[6];
    private static final GLFWFramebufferSizeCallbackI resizeGlViewportAndResetAspect = (long window, int w, int h) -> {
        System.out.println("GLFW Window Resized to: " + w + "*" + h);
        glViewport(0, 0, w, h);
        setCamProjMat(w, h);
    };


    private static final Vector3f cameraPos = new Vector3f(0f, 0f, 5f);
    private static final Vector3f lightPos = new Vector3f(-3.8f, 2.2f, 1.1f);
    private static final Vector3f torusPos = new Vector3f(1.6f, 0f, -.3f);
    private static final Vector3f pyramidPos = new Vector3f(-1f, .1f, .3f);

    // 白光特性
    private static final float[] globalAmbient = {0.7f, 0.7f, 0.7f, 1.0f};
    private static final float[] lightAmbient = {0.0f, 0.0f, 0.0f, 1.0f};
    private static final float[] lightDiffuse = {1.0f, 1.0f, 1.0f, 1.0f};
    private static final float[] lightSpecular = {1.0f, 1.0f, 1.0f, 1.0f};

    private static Torus torus;
    private static int renderingProgram1, renderingProgram2;


    private static ModelReader pyramid;
    private static int p1shadowMVPLoc, p2mvLoc, p2projLoc, p2nLoc, p2sLoc, p2mshiLoc, p2ambLoc, p2globalAmbLoc, p2diffLoc, p2specLoc, p2posLoc, p2mambLoc, p2mdiffLoc, p2mspecLoc;
    private static final Vector3f ORIGIN = new Vector3f(0.0f, 0.0f, 0.0f);
    private static final Vector3fc UP = new Vector3f(0.0f, 1.0f, 0.0f);
    private static final Matrix4f B = new Matrix4f(
            .5f, 0f, 0f, 0f,
            0f, .5f, 0f, 0f,
            0f, 0f, .5f, 0f,
            .5f, .5f, .5f, 1f
    );
    private static Matrix4f lightPMat, lightVMat;
    private static int shadowFrameBuffer;
    private static int shadowTex;

    public static void main(String[] args) {
        init();
        // 迴圈
        while (!GLFW.glfwWindowShouldClose(windowHandle)) {
            loop();
        }
        // 釋出
        GLFW.glfwTerminate();
        System.out.println("Program exit and freed glfw.");
    }

    private static void init() {
        final int windowInitW = 800, windowInitH = 600;
        setCamProjMat(windowInitW, windowInitH);
        GLFWWindow glfwWindow = new GLFWWindow(windowInitW, windowInitH, "第8章");
        windowHandle = glfwWindow.getWindowHandle();
        glfwWindow.setClearColor(new Color(0f, 0f, 0f, 0f));
        glfwSetFramebufferSizeCallback(windowHandle, resizeGlViewportAndResetAspect);
        glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            switch (key) {
                case GLFW_KEY_W -> cameraPos.add(0f, 0f, -.1f);
                case GLFW_KEY_S -> cameraPos.add(0f, 0f, .1f);
                case GLFW_KEY_A -> cameraPos.add(-.1f, 0f, 0f);
                case GLFW_KEY_D -> cameraPos.add(.1f, 0f, 0f);
            }
        });
        glEnable(GL_CULL_FACE);
        glFrontFace(GL_CCW);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glActiveTexture(GL_TEXTURE0);
        renderingProgram1 = new ShaderProgramSetter(Path.of("src/main/java/chapter8/program8_1/shaders/vert1Shader.glsl")
                , Path.of("src/main/java/chapter8/program8_1/shaders/frag1Shader.glsl"))
                .getProgram();
        renderingProgram2 = new ShaderProgramSetter(Path.of("src/main/java/chapter8/program8_1/shaders/vert2Shader.glsl")
                , Path.of("src/main/java/chapter8/program8_1/shaders/frag2Shader.glsl"))
                .getProgram();

        setupVertices();
        createShadowBuffers(windowHandle);
        configShadowFrameBuffer();
        getAllUniformsLoc();
    }

    private static void configShadowFrameBuffer() {
        // 使用自定義幀緩衝區，將紋理附著到其上
        glBindFramebuffer(GL_FRAMEBUFFER, shadowFrameBuffer);
        glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, shadowTex, 0);
        // 關閉繪製顏色
        glDrawBuffer(GL_NONE);
    }

    private static void createShadowBuffers(long window) {
        IntBuffer frameBufW = BufferUtils.createIntBuffer(1), frameBufH = BufferUtils.createIntBuffer(1);
        glfwGetFramebufferSize(window, frameBufW, frameBufH);

        // 創建自定義frame buffer
        shadowFrameBuffer = glGenFramebuffers();

        // 創建陰影紋理儲存深度訊息
        shadowTex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, shadowTex);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32, frameBufW.get(0), frameBufH.get(0), 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
    }

    private static void loop() {
        // ROUND1 從光源處渲染

        // 使用自定義幀緩衝區
        glBindFramebuffer(GL_FRAMEBUFFER, shadowFrameBuffer);

        Matrix4f torusMMat = new Matrix4f().translate(torusPos).rotateX(toRadians(30f)).rotateY(toRadians(40f));
        Matrix4f pyramidMMat = new Matrix4f().translate(pyramidPos).rotateX(toRadians(25f));
        passOne(torusMMat, pyramidMMat);

        // 使用顯示緩衝區，重新繪製
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        passTwo(torusMMat, pyramidMMat);

        glfwSwapBuffers(windowHandle);
        glfwPollEvents();

    }


    private static void passOne(Matrix4f torusMMat, Matrix4f pyramidMMat) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glUseProgram(renderingProgram1);

        lightVMat = new Matrix4f().lookAt(lightPos, ORIGIN, UP);
        lightPMat = camProjMat; // lightPMat 用的參數跟相機都是一樣的。
        // 繪製torus
        Matrix4f shadowMVP1 = new Matrix4f().mul(lightPMat).mul(lightVMat).mul(torusMMat);

        glUniformMatrix4fv(p1shadowMVPLoc, false, shadowMVP1.get(valsOf16));
        glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glDrawElements(GL_TRIANGLES, torus.getNumIndices(), GL_UNSIGNED_INT, 0);

        // 繪製pyramid
        shadowMVP1 = new Matrix4f().mul(lightPMat).mul(lightVMat).mul(pyramidMMat);

        glUniformMatrix4fv(p1shadowMVPLoc, false, shadowMVP1.get(valsOf16));
        glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glDrawArrays(GL_TRIANGLES, 0, pyramid.getNumOfvertices());
    }

    private static void setupVertices() {
        System.out.println("Loading models...");
        torus = new Torus(.5f, .2f, 48, true);
        pyramid = new ModelReader("src/main/java/chapter8/program8_1/models/pyr.obj");

        int[] vao = new int[1];
        glGenVertexArrays(vao);
        glBindVertexArray(vao[0]);

        glGenBuffers(vbo);

        FloatBuffer pvalues = BufferUtils.createFloatBuffer(torus.getVertices().length * 3);
        FloatBuffer nvalues = BufferUtils.createFloatBuffer(torus.getNormals().length * 3);
        IntBuffer indices = torus.getIndicesInBuffer();
        for (int i = 0; i < torus.getNumVertices(); i++) {
            pvalues.put(torus.getVertices()[i].x());         // vertex position
            pvalues.put(torus.getVertices()[i].y());
            pvalues.put(torus.getVertices()[i].z());

            nvalues.put(torus.getNormals()[i].x());         // normal vector
            nvalues.put(torus.getNormals()[i].y());
            nvalues.put(torus.getNormals()[i].z());
        }
        pvalues.flip(); // 此行非常必要!
        nvalues.flip();
        indices.flip();

        // Torus
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glBindBuffer(GL_ARRAY_BUFFER, vbo[0]); // #0: 頂點
        glBufferData(GL_ARRAY_BUFFER, pvalues, GL_STATIC_DRAW);

        glBindBuffer(GL_ARRAY_BUFFER, vbo[2]); // #2: 法向量
        glBufferData(GL_ARRAY_BUFFER, nvalues, GL_STATIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo[3]); // #3: 索引
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        // Pyramid
        glBindBuffer(GL_ARRAY_BUFFER, vbo[4]); // #4: 頂點
        glBufferData(GL_ARRAY_BUFFER, pyramid.getPvalue(), GL_STATIC_DRAW);

        glBindBuffer(GL_ARRAY_BUFFER, vbo[5]); // #5: 法向量
        glBufferData(GL_ARRAY_BUFFER, pyramid.getPvalue(), GL_STATIC_DRAW);


        System.out.println("Model load done.");
    }

    private static void passTwo(Matrix4f torusMMat, Matrix4f pyramidMMat) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glUseProgram(renderingProgram2);

        Matrix4f camVMat = new Matrix4f().lookAt(cameraPos, ORIGIN, UP);

        // 繪製torus
        setupLights(Materials.goldAmbient(), Materials.goldDiffuse(), Materials.goldSpecular(), Materials.goldShininess());
        Matrix4f mvMat = new Matrix4f(camVMat).mul(torusMMat);
        Matrix4f invTrMat = new Matrix4f(mvMat).invert().transpose();
        Matrix4f shadowMVP2 = new Matrix4f(B).mul(lightPMat).mul(lightVMat).mul(torusMMat);
        glUniformMatrix4fv(p2mvLoc, false, mvMat.get(valsOf16));
        glUniformMatrix4fv(p2projLoc, false, camProjMat.get(valsOf16));
        glUniformMatrix4fv(p2nLoc, false, invTrMat.get(valsOf16));
        glUniformMatrix4fv(p2sLoc, false, shadowMVP2.get(valsOf16));

        glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);

        glDrawElements(GL_TRIANGLES, torus.getNumIndices(), GL_UNSIGNED_INT, 0);


        // 繪製pyramid
        setupLights(Materials.bronzeAmbient(), Materials.bronzeDiffuse(), Materials.bronzeSpecular(), Materials.bronzeShininess());
        mvMat = new Matrix4f(camVMat).mul(pyramidMMat);
        invTrMat = new Matrix4f(mvMat).invert().transpose();
        shadowMVP2 = new Matrix4f(B).mul(lightPMat).mul(lightVMat).mul(torusMMat);
        glUniformMatrix4fv(p2mvLoc, false, mvMat.get(valsOf16));
        glUniformMatrix4fv(p2projLoc, false, camProjMat.get(valsOf16));
        glUniformMatrix4fv(p2nLoc, false, invTrMat.get(valsOf16));
        glUniformMatrix4fv(p2sLoc, false, shadowMVP2.get(valsOf16));

        glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);

        glDrawArrays(GL_TRIANGLES, 0, pyramid.getNumOfvertices());
    }

    private static void setupLights(float[] matAmb, float[] matDif, float[] matSpe, float matShi) {
        glProgramUniform4fv(renderingProgram2, p2globalAmbLoc, globalAmbient);
        glProgramUniform4fv(renderingProgram2, p2ambLoc, lightAmbient);
        glProgramUniform4fv(renderingProgram2, p2diffLoc, lightDiffuse);
        glProgramUniform4fv(renderingProgram2, p2specLoc, lightSpecular);
        glProgramUniform3fv(renderingProgram2, p2posLoc, lightPos.get(valsOf3));
        glProgramUniform4fv(renderingProgram2, p2mambLoc, matAmb);
        glProgramUniform4fv(renderingProgram2, p2mdiffLoc, matDif);
        glProgramUniform4fv(renderingProgram2, p2mspecLoc, matSpe);
        glProgramUniform1f(renderingProgram2, p2mshiLoc, matShi);
    }

    private static void setCamProjMat(int w, int h) {
        float aspect = (float) w / (float) h;
        camProjMat = new Matrix4f().perspective(1.0472f, aspect, .1f, 1000f); // 1.0472 = 60度
        System.out.println("Projection matrix set.");
    }

    private static void getAllUniformsLoc() {
        p1shadowMVPLoc = glGetUniformLocation(renderingProgram1, "shadowMVP");
        p2mvLoc = glGetUniformLocation(renderingProgram2, "mv_matrix");
        p2projLoc = glGetUniformLocation(renderingProgram2, "proj_matrix");
        p2nLoc = glGetUniformLocation(renderingProgram2, "norm_matrix");
        p2sLoc = glGetUniformLocation(renderingProgram2, "shadowMVP");
        p2globalAmbLoc = glGetUniformLocation(renderingProgram2, "globalAmbient");
        p2ambLoc = glGetUniformLocation(renderingProgram2, "light.ambient");
        p2diffLoc = glGetUniformLocation(renderingProgram2, "light.diffuse");
        p2specLoc = glGetUniformLocation(renderingProgram2, "light.specular");
        p2posLoc = glGetUniformLocation(renderingProgram2, "light.position");
        p2mambLoc = glGetUniformLocation(renderingProgram2, "material.ambient");
        p2mdiffLoc = glGetUniformLocation(renderingProgram2, "material.diffuse");
        p2mspecLoc = glGetUniformLocation(renderingProgram2, "material.specular");
        p2mshiLoc = glGetUniformLocation(renderingProgram2, "material.shininess");
    }
}
