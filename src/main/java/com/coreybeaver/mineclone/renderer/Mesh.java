package com.coreybeaver.mineclone.renderer;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.glfwGetTime;

public class Mesh {
    private VertexArray vao;
    private Texture texture;

    public Mesh(VertexArray vao, Texture texture) {
        this.vao = vao;
        this.texture = texture;
    }

    public VertexArray getVao() { return vao; }
    public Texture getTexture() { return texture; }

    public void Draw(Shader shader) {
        Draw(shader, null);
    }

    /**
     * Draw using the provided shader and MVP matrix.  If mvp is null the
     * shader is left responsible for setting its own transform.
     */
    public void Draw(Shader shader, Matrix4f mvp) {

        Matrix4f model = vao.getModelMatrix();

        shader.Bind();
        if (texture != null) texture.Bind(0);
        shader.UniformTexture2D("u_Texture", 0);
        // Pointing from top-left-front
        Vector3f lightDir = new Vector3f(-1f, -1f, -1f).normalize();
        shader.UniformFloat3("u_LightDir", lightDir);
        shader.UniformMat4("u_Model", model);
        shader.UniformFloat("u_Time", (float) glfwGetTime());
        if (mvp != null) {
            shader.UniformMat4("u_MVP", mvp);
        }
        Renderer.DrawIndexed(vao);
    }
}