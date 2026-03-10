package com.coreybeaver.mineclone.scene;

import com.coreybeaver.mineclone.assetmanager.AssetManager;
import com.coreybeaver.mineclone.game.world.BlockManager;
import com.coreybeaver.mineclone.game.world.World;
import com.coreybeaver.mineclone.io.Input;
import com.coreybeaver.mineclone.renderer.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;
import static org.lwjgl.opengl.GL11.*;

public class Scene {

    private Shader defaultShader;
    private Shader waterShader;

    public Camera camera;

    private Skybox skybox;

    private World world;

    private Texture atlas;
    // we no longer keep a single mesh list; meshes are provided per frame
    // by the world as two separate lists

    public Scene() {
        camera = new Camera(1920, 1080);
        // start above the flat surface (which generator places at ~64)
        camera.setPosition(8f, 80f, 30f);
        camera.lookAt(8f, 64f, 8f);

        skybox = new Skybox();

        // the shader loader currently ignores the passed path and always loads
        // "assets/shaders/default.glsl", but use the correct spelling anyway.
        defaultShader = AssetManager.GetShader("assets/shaders/default.glsl");


        atlas = AssetManager.GetTexture("assets/images/terrain.png");

        // load both shaders
        defaultShader = AssetManager.GetShader("assets/shaders/default.glsl");
        waterShader = AssetManager.GetShader("assets/shaders/water.glsl");

        // create world and let it load chunks around the starting camera
        world = new World();
        world.update(camera.getPosition());
    }

    public void Update(float deltaTime) {
        // Clear screen each frame
        Renderer.Clear();

        // Update camera movement
        camera.Update(deltaTime);

        // Handle right-click to place glowstone
        if (Input.getButtonDown(GLFW_MOUSE_BUTTON_RIGHT)) {
            placeGlowstone();
        }

        // Update world/chunk loading and rebuild mesh lists
        world.update(camera.getPosition());
        World.MeshLists meshLists = world.getMeshes(atlas);

        // --- Render Skybox ---
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glDepthMask(false);
        glDisable(GL_CULL_FACE);

        // MVP without translation
        Matrix4f viewNoTranslation = new Matrix4f(camera.getView());
        viewNoTranslation.m30(0f).m31(0f).m32(0f);
        Matrix4f skyboxMVP = new Matrix4f(camera.getProjection()).mul(viewNoTranslation);

        // Draw
        skybox.Draw(skyboxMVP, new Vector3f(-1f, 1f, 0f), deltaTime);

        // Restore depth and culling
        glDepthMask(true);
        glDepthFunc(GL_LESS);

        // -------------------------------
        // Render solid meshes
        // -------------------------------
        for (Mesh mesh : meshLists.solids) {
            VertexArray vao = mesh.getVao();
            Matrix4f mvp = new Matrix4f(camera.getProjection()).mul(camera.getView()).mul(vao.getModelMatrix());
            mesh.Draw(defaultShader, mvp);
        }

        // -------------------------------
        // Render water meshes (transparent)
        // -------------------------------
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        for (Mesh mesh : meshLists.waters) {
            VertexArray vao = mesh.getVao();
            vao.position.min(new Vector3f(0.0f, -0.3f, 0.0f));
            Matrix4f mvp = new Matrix4f(camera.getProjection()).mul(camera.getView()).mul(vao.getModelMatrix());
            mesh.Draw(waterShader, mvp);
        }

        glDisable(GL_BLEND);

        // -------------------------------
        // Render billboards (plants, etc.)
        // -------------------------------
        for (Mesh mesh : meshLists.billboards) {
            VertexArray vao = mesh.getVao();
            Matrix4f mvp = new Matrix4f(camera.getProjection()).mul(camera.getView()).mul(vao.getModelMatrix());
            mesh.Draw(defaultShader, mvp);
        }
    }

    private void placeGlowstone() {
        Vector3f rayOrigin = camera.getPosition();
        Vector3f rayDir = camera.getForward();


        // Raycast up to 5 blocks away
        float maxDistance = 5.0f;
        float step = 0.1f;

        Vector3f lastAirPos = null;

        for (float t = 0; t < maxDistance; t += step) {
            Vector3f pos = new Vector3f(rayOrigin).add(new Vector3f(rayDir).mul(t));
            int blockX = (int) Math.floor(pos.x);
            int blockY = (int) Math.floor(pos.y);
            int blockZ = (int) Math.floor(pos.z);

            int blockId = world.getBlockAt(blockX, blockY, blockZ);

            if (blockId == 0) {
                // Air - remember this position
                lastAirPos = new Vector3f(blockX, blockY, blockZ);
            } else {
                // Hit a solid block - place glowstone in the last air position
                if (lastAirPos != null) {
                    int placeX = (int) lastAirPos.x;
                    int placeY = (int) lastAirPos.y;
                    int placeZ = (int) lastAirPos.z;


                    // Get glowstone block ID
                    int glowstoneId = BlockManager.Get().getIdByName("glowstone");
                    if (glowstoneId >= 0) {
                        world.setBlockAt(placeX, placeY, placeZ, glowstoneId);
                    } else {
                        System.out.println("ERROR: Could not find glowstone block!");
                    }
                } else {
                    System.out.println("ERROR: No air position found before hitting block!");
                }
                break;
            }
        }
    }
}