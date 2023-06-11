package chapter10.program10_2.launcher;

import chapter10.program10_1.launcher.Program10_1;
import utilities.Color;
import utilities.GLFWWindow;
import utilities.Materials;
import utilities.ShaderProgramSetter;
import utilities.callbacks.DefaultCallbacks;
import utilities.models.Sphere;
import utilities.sceneComponents.PositionalLight;
import utilities.sceneComponents.Texture;

import java.nio.file.Path;

import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.opengl.GL43.*;
import static utilities.ValuesContainer.VALS_OF_16;

public class Program10_2 extends Program10_1 {
    private int globalAmbientLoc, mv_matrixLoc, proj_matrixLoc, norm_matrixLoc, materialShininessLoc,
            lightAmbientLoc, lightDiffuseLoc, lightSpecularLoc, lightPosition, materialAmbientLoc,
            materialDiffuseLoc, materialSpecularLoc;
    private PositionalLight positionalLight;
    private Sphere sphere;
    private int defaultProgram;
    private Materials material;

    public Program10_2(String title) {
        super(title);
    }

    @Override
    protected void init(String title) {
        final int WINDOW_INIT_W = 1500, WINDOW_INIT_H = 1000;
        CAMERA.setProjMat(WINDOW_INIT_W, WINDOW_INIT_H);
        GLFWWindow glfwWindow = new GLFWWindow(WINDOW_INIT_W, WINDOW_INIT_H, title);
        windowID = glfwWindow.getWindowHandle();
        glfwWindow.setClearColor(new Color(0f, 0f, 0f, 0f));

        new DefaultCallbacks(windowID, CAMERA).bindToGLFW();

        glEnable(GL_CULL_FACE);
        glFrontFace(GL_CCW);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glActiveTexture(GL_TEXTURE0);

        defaultProgram = new ShaderProgramSetter(Path.of("src/main/resources/shaders/program10_2/vert.glsl")
                , Path.of("src/main/resources/shaders/program10_2/frag.glsl"))
                .getProgram();

        getAllUniformsLoc();
        sphere = new Sphere();
        positionalLight = new PositionalLight();
        material = new Materials("gold");
        Texture normalMapTexture = new Texture(0, "src/main/resources/textures/normalMaps/castleroofNORMAL.jpg");
        normalMapTexture.bind();

        System.out.println("Hint: You can press F1 to look around.");
    }


    @Override
    protected void getAllUniformsLoc() {
        globalAmbientLoc = glGetUniformLocation(defaultProgram, "globalAmbient");
        mv_matrixLoc = glGetUniformLocation(defaultProgram, "mv_matrix");
        proj_matrixLoc = glGetUniformLocation(defaultProgram, "proj_matrix");
        norm_matrixLoc = glGetUniformLocation(defaultProgram, "norm_matrix");
        lightAmbientLoc = glGetUniformLocation(defaultProgram, "light.ambient");
        lightDiffuseLoc = glGetUniformLocation(defaultProgram, "light.diffuse");
        lightSpecularLoc = glGetUniformLocation(defaultProgram, "light.specular");
        lightPosition = glGetUniformLocation(defaultProgram, "light.position");
        materialAmbientLoc = glGetUniformLocation(defaultProgram, "material.ambient");
        materialDiffuseLoc = glGetUniformLocation(defaultProgram, "material.diffuse");
        materialSpecularLoc = glGetUniformLocation(defaultProgram, "material.specular");
        materialShininessLoc = glGetUniformLocation(defaultProgram, "material.shininess");
    }

    @Override
    protected void loop() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        CAMERA.updateVMat();
        drawScene();
        CAMERA.handle();

        glfwSwapBuffers(windowID);
        glfwPollEvents();
    }

    private void drawScene() {
        glUseProgram(defaultProgram);

        //繪製sphere

        // light
        glUniform4fv(globalAmbientLoc, positionalLight.getGLOBAL_AMBIENT());
        glUniform4fv(lightAmbientLoc, positionalLight.getLIGHT_AMBIENT());
        glUniform4fv(lightDiffuseLoc, positionalLight.getLIGHT_DIFFUSE());
        glUniform4fv(lightSpecularLoc, positionalLight.getLIGHT_SPECULAR());
        glUniform3fv(lightPosition, positionalLight.getLIGHT_POSITION());
        positionalLight.flipAll();
        // material
        glUniform4fv(materialAmbientLoc, material.getAMBIENT());
        glUniform4fv(materialDiffuseLoc, material.getDIFFUSE());
        glUniform4fv(materialSpecularLoc, material.getSPECULAR());
        glUniform1fv(materialShininessLoc, material.getSHININESS());
        material.flipAll();


        // matrices
        glUniformMatrix4fv(mv_matrixLoc, false, sphere.getMV_MAT().get(VALS_OF_16));
        glUniformMatrix4fv(norm_matrixLoc, false, sphere.getINV_TR_MAT().get(VALS_OF_16));
        glUniformMatrix4fv(proj_matrixLoc, false, CAMERA.getProjMat().get(VALS_OF_16));

        sphere.updateState(CAMERA);
        sphere.draw(GL_TRIANGLES);
    }

    public static void main(String[] args) {
        new Program10_2("Normal Mapping");
    }
}
