package com.coreybeaver.mineclone.scene;

import com.coreybeaver.mineclone.renderer.*;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.*;

public class Scene {

    private Shader shader;
    private VertexArray quad;

    public Camera camera;

    public Scene() {

        camera = new Camera(1920, 1080);

        // =========================
        // SHADER
        // =========================

        String vertexSrc = """
                #version 330 core
                
                layout(location = 0) in vec3 aPos;
                
                uniform mat4 u_MVP;
                
                void main()
                {
                    gl_Position = u_MVP * vec4(aPos, 1.0);
                }
                """;

        String fragmentSrc = """
                #version 330 core
                out vec4 FragColor;
                void main()
                {
                    FragColor = vec4(0.2, 0.7, 0.3, 1.0);
                }
                """;

        shader = new Shader(vertexSrc, fragmentSrc);

        // =========================
        // QUAD DATA
        // =========================

        float[] vertices = {
                // positions
                -0.5f, -0.5f, 0.0f,
                0.5f, -0.5f, 0.0f,
                0.5f,  0.5f, 0.0f,
                -0.5f,  0.5f, 0.0f
        };

        int[] indices = {
                0, 1, 2,
                2, 3, 0
        };

        BufferLayoutElement[] layout = new BufferLayoutElement[] {
                new BufferLayoutElement(GL_FLOAT, 3, false)
        };

        quad = new VertexArray(vertices, indices, layout);
        quad.getPosition().z = -2.0f;

    }

    public void Update(float deltaTime) {

        Renderer.Clear();

        // Update camera movement
        camera.Update(deltaTime);

        // Compute MVP for object
        Matrix4f mvp = new Matrix4f()
                .set(camera.getProjection())
                .mul(camera.getView())
                .mul(quad.getModelMatrix());

        shader.Bind();
        shader.UniformMat4("u_MVP", mvp);

        Renderer.DrawIndexed(quad);
    }
}