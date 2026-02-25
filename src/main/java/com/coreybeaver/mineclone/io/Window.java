package com.coreybeaver.mineclone.io;

import org.lwjgl.opengl.GL;

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

        Input.init(glfwWindow);

        glfwShowWindow(glfwWindow);
    }

    public void Update() {
        glfwPollEvents();
        glfwSwapBuffers(glfwWindow);
    }

    public void Destroy() {
        glfwDestroyWindow(glfwWindow);
    }

    public boolean open() {
        return !glfwWindowShouldClose(glfwWindow);
    }
}
