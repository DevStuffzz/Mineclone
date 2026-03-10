package com.coreybeaver.mineclone;

import com.coreybeaver.mineclone.game.world.BlockManager;
import com.coreybeaver.mineclone.game.world.BlockType;

import com.coreybeaver.mineclone.io.Input;
import com.coreybeaver.mineclone.io.Window;

import com.coreybeaver.mineclone.renderer.Renderer;
import com.coreybeaver.mineclone.renderer.Shader;
import com.coreybeaver.mineclone.scene.Scene;

import com.coreybeaver.mineclone.renderer.Framebuffer;

import java.awt.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL13.*;

public class Game {
    private Scene scene;
    private Window window;

    private Shader framebufferShader;
    private static Framebuffer MAIN_FB;

    private static boolean  initialized = false;

    public Game() {
        if(!initialized) Init();
    }

    private void Init() {
        initialized = true;

        // register all block types before creating any chunks/meshes
        InitBlocks();

        window = Window.get();
        scene = new Scene();

        framebufferShader = new Shader("assets/shaders/framebuffer.glsl");

        MAIN_FB = new Framebuffer(1920, 1080);

        Update();
    }

    private void InitBlocks() {
        BlockManager manager = BlockManager.Get();
        manager.AddBlock(BlockType.AIR, -1, -1, -1, "air"); // 0
        manager.AddBlock(BlockType.SOLID, 0, 3, 2, "grass_block"); // 1
        manager.AddBlock(BlockType.SOLID, 66, 68, 2, "snow_grass_block"); // 2
        manager.AddBlock(BlockType.SOLID, 66, 66, 66, "snow_block"); // 3
        manager.AddBlock(BlockType.SOLID, 2, 2, 2, "dirt"); // 4
        manager.AddBlock(BlockType.SOLID, 1, 1, 1, "stone"); // 5
        manager.AddBlock(BlockType.SOLID, 21, 20, 21, "oak_log"); // 6
        manager.AddBlock(BlockType.SOLID, 53, 53, 53, "oak_leaves"); // 7
        manager.AddBlock(BlockType.SOLID, 4, 4, 4, "oak_plank"); // 8
        manager.AddBlock(BlockType.SOLID, 176, 176, 176, "sand"); // 9
        manager.AddBlock(BlockType.SOLID, 192, 192, 192, "sandstone"); // 10
        manager.AddBlock(BlockType.PLANT, 39, 39, 39, "tall_grass"); // 11
        manager.AddBlock(BlockType.LIQUID, 206, 206, 206, "water"); // 12

        manager.AddBlock(BlockType.PLANT, 12, 12, 12, "rose"); // 13
        manager.AddBlock(BlockType.SOLID, 105, 105, 105, "glowstone", true); // 14

    }

    private void Update() {

        float lastTime = (float) glfwGetTime();

        Renderer.Init();

        while(window.open()) {

            float currentTime = (float) glfwGetTime();
            float deltaTime = currentTime - lastTime;
            lastTime = currentTime;

            // Update input states for this frame
            // Input.update();

            // --- Render scene to framebuffer ---
            MAIN_FB.bind();
            Renderer.Clear();
            scene.Update(deltaTime);
            MAIN_FB.unbind();

            // --- Render framebuffer to screen ---
            Renderer.Clear();
            framebufferShader.Bind();
            framebufferShader.UniformInt("u_Texture", 0);
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, MAIN_FB.getTexture());
            Renderer.DrawFrameBuffer(MAIN_FB);
            framebufferShader.Unbind();

            window.Update();
        }
    }

    public static Framebuffer GetMainFramebuffer() {
        return MAIN_FB;
    }
}
