package com.coreybeaver.mineclone.assetmanager;

import com.coreybeaver.mineclone.renderer.Shader;
import com.coreybeaver.mineclone.renderer.Texture;

import java.util.HashMap;
import java.util.Map;

public class AssetManager {

    private static final Map<String, Shader> shaders = new HashMap<>();
    private static final Map<String, Texture> textures = new HashMap<>();

    // -----------------------------
    // SHADERS
    // -----------------------------
    public static Shader GetShader(String name) {
        if (shaders.containsKey(name)) {
            return shaders.get(name);
        }

        Shader shader = new Shader(name); // assumes Shader constructor loads from file
        shaders.put(name, shader);
        return shader;
    }

    public static void ClearShaders() {
        shaders.clear(); // optionally call delete on each shader if you add a dispose method
    }

    // -----------------------------
    // TEXTURES
    // -----------------------------
    public static Texture GetTexture(String name) {
        if (textures.containsKey(name)) {
            return textures.get(name);
        }

        Texture texture = new Texture(name); // loads PNG or JPG
        textures.put(name, texture);
        return texture;
    }

    public static void ClearTextures() {
        for (Texture t : textures.values()) {
            t.Delete(); // free GPU memory
        }
        textures.clear();
    }

    // -----------------------------
    // CLEAR ALL
    // -----------------------------
    public static void ClearAll() {
        ClearShaders();
        ClearTextures();
    }
}