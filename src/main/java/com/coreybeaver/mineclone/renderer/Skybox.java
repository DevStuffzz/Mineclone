package com.coreybeaver.mineclone.renderer;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Skybox {

    private Mesh mesh;
    private Shader shader;

    public Skybox() {
        // Create cube vertices
        float[] vertices = {
                // positions
                -1f,  1f, -1f,
                -1f, -1f, -1f,
                1f, -1f, -1f,
                1f,  1f, -1f,

                -1f,  1f,  1f,
                -1f, -1f,  1f,
                1f, -1f,  1f,
                1f,  1f,  1f,
        };

        int[] indices = {
                // front
                0, 1, 2, 2, 3, 0,
                // back
                4, 5, 6, 6, 7, 4,
                // left
                4, 5, 1, 1, 0, 4,
                // right
                3, 2, 6, 6, 7, 3,
                // top
                4, 0, 3, 3, 7, 4,
                // bottom
                1, 5, 6, 6, 2, 1
        };

        // Layout: only positions
        BufferLayoutElement[] layout = {
                new BufferLayoutElement(org.lwjgl.opengl.GL11.GL_FLOAT, 3, false)
        };

        VertexArray vao = new VertexArray(vertices, indices, layout);

        // No texture needed for procedural skybox
        mesh = new Mesh(vao, null);

        // Load shader
        shader = new Shader("assets/shaders/skybox.glsl");
    }

    /**
     * Draw the skybox.
     * @param viewProjection The camera view-projection matrix.
     * @param sunDirection Direction of the sun (optional).
     * @param time Time in seconds for procedural animation (optional).
     */
    public void Draw(Matrix4f viewProjection, Vector3f sunDirection, float time) {
        shader.Bind();

        // Use matrix directly, no further modification
        shader.UniformMat4("u_MVP", viewProjection);

        if (sunDirection != null) {
            shader.UniformFloat3("u_SunDirection", sunDirection);
        } else {
            shader.UniformFloat3("u_SunDirection", new Vector3f(0, 0, 0));
        }

        shader.UniformFloat("u_Time", time);

        mesh.Draw(shader);
    }

    public void Delete() {
        mesh.getVao().Delete();
    }
}