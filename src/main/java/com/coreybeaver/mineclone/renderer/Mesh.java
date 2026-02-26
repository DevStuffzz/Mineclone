package com.coreybeaver.mineclone.renderer;

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
        shader.Bind();
        if (texture != null) texture.Bind(0);
        shader.UniformTexture2D("u_Texture", 0);
        Renderer.DrawIndexed(vao);
    }
}