package com.coreybeaver.mineclone.scene;

import com.coreybeaver.mineclone.assetmanager.AssetManager;
import com.coreybeaver.mineclone.game.world.World;
import com.coreybeaver.mineclone.renderer.*;
import org.joml.Matrix4f;


import static org.lwjgl.opengl.GL11.*;

public class Scene {

    private Shader defaultShader;
    private Shader waterShader;

    public Camera camera;

    private World world;

    private Texture atlas;
    // we no longer keep a single mesh list; meshes are provided per frame
    // by the world as two separate lists

    public Scene() {
        camera = new Camera(1920, 1080);
        // start above the flat surface (which generator places at ~64)
        camera.setPosition(8f, 80f, 30f);
        camera.lookAt(8f, 64f, 8f);

        // the shader loader currently ignores the passed path and always loads
        // "assets/shaders/default.glsl", but use the correct spelling anyway.
        defaultShader = AssetManager.GetShader("assets/shaders/default.glsl");


        atlas = AssetManager.GetTexture("assets/images/terrain.png");

        // load both shaders
        defaultShader = AssetManager.GetShader("assets/shaders/default.glsl");
        waterShader   = AssetManager.GetShader("assets/shaders/water.glsl");

        // create world and let it load chunks around the starting camera
        world = new World();
        world.update(camera.getPosition());
    }

    public void Update(float deltaTime) {
        Renderer.Clear();

        // Update camera movement
        camera.Update(deltaTime);

        // update world/chunk loading and rebuild mesh lists each frame
        world.update(camera.getPosition());
        World.MeshLists meshLists = world.getMeshes(atlas);

        // enable blending for transparent textures (water)
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // draw solids with the default shader
        for (Mesh mesh : meshLists.solids) {
            VertexArray vao = mesh.getVao();
            Matrix4f mvp = new Matrix4f()
                    .set(camera.getProjection())
                    .mul(camera.getView())
                    .mul(vao.getModelMatrix());
            mesh.Draw(defaultShader, mvp);
        }

        // draw water using the specialized water shader
        for (Mesh mesh : meshLists.waters) {
            VertexArray vao = mesh.getVao();
            Matrix4f mvp = new Matrix4f()
                    .set(camera.getProjection())
                    .mul(camera.getView())
                    .mul(vao.getModelMatrix());
            mesh.Draw(waterShader, mvp);
        }
    }
}