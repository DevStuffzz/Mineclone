package com.coreybeaver.mineclone.renderer;

import org.joml.Vector3f;


import static org.lwjgl.opengl.GL11.*;

public class Renderer {
    private static VertexArray screenQuad;

    public static void Init() {
        float[] vertices = {
                // positions   // texcoords
                -1f, -1f, 0f, 0f,
                1f, -1f, 1f, 0f,
                1f,  1f, 1f, 1f,
                -1f,  1f, 0f, 1f
        };

        int[] indices = {
                0,1,2,
                2,3,0
        };

        BufferLayoutElement[] ble = {
                new BufferLayoutElement(GL_FLOAT, 2, false), // position x,y
                new BufferLayoutElement(GL_FLOAT, 2, false)  // texcoords u,v
        };

        screenQuad = new VertexArray(vertices, indices, ble);
    }

    public static void Clear() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glClearColor(0.9f, 0.5f, 0.9f, 1.0f);
    }

    public static void Clear(Vector3f color) {
        float r = color.x > 1 ? color.x / 255f : color.x;
        float g = color.y > 1 ? color.y / 255f : color.y;
        float b = color.z > 1 ? color.z / 255f : color.z;

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glClearColor(r, g, b, 1.0f);
    }

    public static void DrawIndexed(VertexArray va) {
        va.Bind();
        glDrawElements(GL_TRIANGLES, va.GetIndexCount(), GL_UNSIGNED_INT, 0);
    }

    public static void DrawFrameBuffer(Framebuffer fb) {

        glDisable(GL_DEPTH_TEST);

        glBindTexture(GL_TEXTURE_2D, fb.getTexture());

        screenQuad.Bind();
        glDrawElements(GL_TRIANGLES, screenQuad.GetIndexCount(), GL_UNSIGNED_INT, 0);

        glEnable(GL_DEPTH_TEST);
    }

}
