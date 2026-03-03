package com.coreybeaver.mineclone;

import com.coreybeaver.mineclone.game.world.BlockManager;
import com.coreybeaver.mineclone.game.world.BlockType;
import com.coreybeaver.mineclone.io.Window;
import com.coreybeaver.mineclone.scene.Scene;

import static org.lwjgl.glfw.GLFW.glfwGetTime;

public class Game {
    private Scene scene;
    private Window window;

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
        manager.AddBlock(BlockType.SOLID, 49, 49, 49, "oak_leaves"); // 7
        manager.AddBlock(BlockType.SOLID, 4, 4, 4, "oak_plank"); // 8
        manager.AddBlock(BlockType.SOLID, 176, 176, 176, "sand"); // 9
        manager.AddBlock(BlockType.SOLID, 192, 192, 192, "sandstone"); // 10
        manager.AddBlock(BlockType.LIQUID, 207, 206, 206, "water"); // 11
    }

    private void Update() {

        float lastTime = (float) glfwGetTime();  // initial timestamp

        while (window.open()) {

            float currentTime = (float) glfwGetTime();
            float deltaTime = currentTime - lastTime;
            lastTime = currentTime;

            scene.Update(deltaTime);

            // Swap buffers and poll events
            window.Update();
        }
    }
}
