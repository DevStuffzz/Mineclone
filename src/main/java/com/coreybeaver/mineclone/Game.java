package com.coreybeaver.mineclone;

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

        window = Window.get();
        scene = new Scene();
        Update();
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
