package com.coreybeaver.mineclone;

import com.coreybeaver.mineclone.io.Window;
import com.coreybeaver.mineclone.scene.Scene;

public class Game {
    private Scene scene;
    private Window window;

    private static boolean  initialized = false;

    public Game() {
        if(!initialized) Init();
    }

    private void Init() {
        initialized = true;


        scene = new Scene();
        window = Window.get();
        Update();
    }

    private void Update() {
        while(window.open()) {
            scene.Update();
            window.Update();
        }
    }
}
