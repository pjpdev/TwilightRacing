package za.madtek.arcaderacer;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {

    private long handle;
    private int width;
    private int height;
    private boolean fullscreen;
    private boolean resizable;
    private String title;
    private boolean resized = false;

    public Window(int width, int height, boolean fullscreen, boolean resizable, String title) {
        this.width = width;
        this.height = height;
        this.fullscreen = fullscreen;
        this.resizable = resizable;
        this.title = title;
    }

    public void init() {

        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit())
            throw new IllegalStateException("Failed to initialize GLFW");

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, resizable ? GLFW_TRUE : GLFW_FALSE);
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);

        handle = glfwCreateWindow(width, height, title,
                fullscreen ? glfwGetPrimaryMonitor() : NULL, NULL);

        if (handle == NULL)
            throw new RuntimeException("Failed to create GLFW Window");

        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            glfwGetWindowSize(handle, pWidth, pHeight);

            GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            glfwSetWindowPos(
                    handle,
                    (vidMode.width() - pWidth.get(0)) / 2,
                    (vidMode.height() - pHeight.get(0)) / 2
            );
        }

        glfwShowWindow(handle);

        glfwSetFramebufferSizeCallback(handle, (window, newWidth, newHeight) -> {
            resized = true;
            width = newWidth;
            height = newHeight;
        });
    }

    public void update() {
        glfwPollEvents();
    }

    public void free() {
        glfwFreeCallbacks(handle);
        glfwDestroyWindow(handle);

        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(handle);
    }

    public long getHandle() {
        return handle;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public boolean isResizable() {
        return resizable;
    }

    public String getTitle() {
        return title;
    }

    public boolean isResized() {
        return resized;
    }

    public void setResized(boolean resized) {
        this.resized = resized;
    }
}
