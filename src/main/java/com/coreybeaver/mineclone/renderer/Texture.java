package com.coreybeaver.mineclone.renderer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.stb.STBImage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.stb.STBImage.*;

public class Texture {

    private int rendererID;
    private int width, height;

    public Texture(String resourcePath) {
        // Load from classpath
        ByteBuffer imageBuffer;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) throw new RuntimeException("Cannot find texture: " + resourcePath);

            byte[] bytes = in.readAllBytes();
            imageBuffer = BufferUtils.createByteBuffer(bytes.length);
            imageBuffer.put(bytes);
            imageBuffer.flip();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read texture: " + resourcePath, e);
        }

        // Prepare STB buffers
        IntBuffer w = BufferUtils.createIntBuffer(1);
        IntBuffer h = BufferUtils.createIntBuffer(1);
        IntBuffer comp = BufferUtils.createIntBuffer(1);

        stbi_set_flip_vertically_on_load(true);

        ByteBuffer data = stbi_load_from_memory(imageBuffer, w, h, comp, 4);
        if (data == null) throw new RuntimeException("Failed to load texture: " + resourcePath + "\n" + stbi_failure_reason());

        width = w.get();
        height = h.get();

        // Generate OpenGL texture
        rendererID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, rendererID);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);

        stbi_image_free(data);
    }

    public void Bind(int slot) {
        // activate the appropriate texture unit and bind this texture
        glActiveTexture(GL_TEXTURE0 + slot);
        glBindTexture(GL_TEXTURE_2D, rendererID);
    }

    public void Unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public void Delete() {
        glDeleteTextures(rendererID);
    }
}