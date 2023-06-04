package chapter9.program9_3.callbacks;

import utilities.buffers.ShadowFrameBuffer;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import utilities.Camera;

import static org.lwjgl.opengl.GL11.glViewport;

public class P9_3FrameBufferResizeCB extends GLFWFramebufferSizeCallback {
    private final Camera CAMERA;
    private final ShadowFrameBuffer SHADOW_FRAME_BUFFER;

    public P9_3FrameBufferResizeCB(Camera CAMERA, ShadowFrameBuffer SHADOW_FRAME_BUFFER) {
        this.CAMERA = CAMERA;
        this.SHADOW_FRAME_BUFFER = SHADOW_FRAME_BUFFER;
    }
    @Override
    public void invoke(long window, int width, int height) {
        System.out.println("GLFW Window Resized to: " + width + "*" + height);
        glViewport(0, 0, width, height);
        CAMERA.setProjMat(width, height);
        SHADOW_FRAME_BUFFER.resetTex(width, height);
    }
}
