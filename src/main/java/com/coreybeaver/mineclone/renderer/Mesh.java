package com.coreybeaver.mineclone.renderer;

import org.joml.Matrix4f;

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
        shader.Bind();
        if (texture != null) texture.Bind(0);
        shader.UniformTexture2D("u_Texture", 0);
        if (mvp != null) {
            shader.UniformMat4("u_MVP", mvp);
        }
        Renderer.DrawIndexed(vao);
    }
}