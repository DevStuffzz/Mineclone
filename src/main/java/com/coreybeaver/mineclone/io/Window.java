package com.coreybeaver.mineclone.io;

import com.coreybeaver.mineclone.Game;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {
    private String title;
    private int width, height;

    private long glfwWindow;

    private static Window instance = null;

    private Window() {
        this.width = 1920;
        this.height = 1080;
        this.title = "Minecraft Clone";

        try {
            Init();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static Window get() {
        if(Window.instance == null) Window.instance = new Window();
        return Window.instance;
    }

    public void Init() throws IllegalAccessException {
        if(!glfwInit()) throw new IllegalAccessException("GLFW Failed to Initialize");

        glfwWindow = glfwCreateWindow(width, height, title, NULL, NULL);

        if(glfwWindow == NULL) throw new IllegalAccessException("GLFW Window failed to create");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_MAXIMIZED, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);


        glfwMakeContextCurrent(glfwWindow);
        GL.createCapabilities();
        // enable depth testing (was mistakenly using the clear mask)
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        // Capture and hide cursor for mouse look
        glfwSetInputMode(glfwWindow, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        Input.init(glfwWindow);

        glfwSetWindowSizeCallback(glfwWindow, (win, width, height) -> {
            GL11.glViewport(0, 0, width, height);
            Game.GetMainFramebuffer().resize(width, height);
        });

        glfwShowWindow(glfwWindow);
    }

    public void Update() {
        glfwPollEvents();
        glfwSwapBuffers(glfwWindow);
    }

    public void ToggleFullscreen() {
        long monitor = glfwGetWindowMonitor(glfwWindow);

        if (monitor == NULL) {
            // Switch to borderless fullscreen
            long primaryMonitor = glfwGetPrimaryMonitor();
            GLFWVidMode vidMode = glfwGetVideoMode(primaryMonitor);
            if (vidMode != null) {
                glfwSetWindowAttrib(glfwWindow, GLFW_DECORATED, GLFW_FALSE);
                glfwSetWindowMonitor(glfwWindow, primaryMonitor, 0, 0,
                        vidMode.width(), vidMode.height(), vidMode.refreshRate());
            }
        } else {
            // Switch back to windowed mode (restore previous size)
            glfwSetWindowMonitor(glfwWindow, NULL, 100, 100, width, height, 0);
            glfwSetWindowAttrib(glfwWindow, GLFW_DECORATED, GLFW_TRUE);
        }
    }

    public void Destroy() {
        glfwDestroyWindow(glfwWindow);
    }

    public boolean open() {
        return !glfwWindowShouldClose(glfwWindow);
    }
}
