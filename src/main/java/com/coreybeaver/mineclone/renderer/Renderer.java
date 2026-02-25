package com.coreybeaver.mineclone.renderer;

import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;

public class Renderer {
    public static void Clear() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glClearColor(0.3f, 0.5f, 0.9f, 1.0f);
    }

    public static void Clear(Vector3f color) {
        color = color.normalize();
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glClearColor(color.x, color.y, color.z, 1.0f);
    }
}
