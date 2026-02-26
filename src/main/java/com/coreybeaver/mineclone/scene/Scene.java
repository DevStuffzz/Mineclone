package com.coreybeaver.mineclone.scene;

import com.coreybeaver.mineclone.assetmanager.AssetManager;
import com.coreybeaver.mineclone.renderer.*;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class Scene {

    private Shader defaultShader;

    public Camera camera;

    private List<Mesh> meshes;

    public Scene() {
        meshes = new ArrayList<>();
        camera = new Camera(1920, 1080);

        defaultShader = AssetManager.GetShader("assets/shaders/defualt.glsl");

        // =========================
        // QUAD DATA
        // =========================

        float[] vertices = {
                // positions        // uvs
                -0.5f, -0.5f, 0.0f,  0.0f, 0.0f, // bottom-left
                0.5f, -0.5f, 0.0f,  1.0f, 0.0f, // bottom-right
                0.5f,  0.5f, 0.0f,  1.0f, 1.0f, // top-right
                -0.5f,  0.5f, 0.0f,  0.0f, 1.0f  // top-left
        };

        int[] indices = {
                0, 1, 2,
                2, 3, 0
        };

        BufferLayoutElement[] layout = new BufferLayoutElement[] {
                new BufferLayoutElement(GL_FLOAT, 3, false), // position
                new BufferLayoutElement(GL_FLOAT, 2, false)  // uv
        };

        Texture grass = AssetManager.GetTexture("assets/images/test.jpg");
        VertexArray quad = new VertexArray(vertices, indices, layout);

        Mesh block = new Mesh(quad, grass);

        meshes.add(block);

    }

    public void Update(float deltaTime) {
        Renderer.Clear();

        // Update camera movement
        camera.Update(deltaTime);

        for (Mesh mesh : meshes) {
            VertexArray vao = mesh.getVao();
            Texture tex = mesh.getTexture();

            Matrix4f mvp = new Matrix4f()
                    .set(camera.getProjection())
                    .mul(camera.getView())
                    .mul(vao.getModelMatrix());

            defaultShader.Bind();

            if (tex != null) {
                tex.Bind(0);
                defaultShader.UniformTexture2D("u_Texture", 0);
            }

            defaultShader.UniformMat4("u_MVP", mvp);

            Renderer.DrawIndexed(vao);
        }
    }
}