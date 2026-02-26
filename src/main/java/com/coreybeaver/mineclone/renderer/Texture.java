package com.coreybeaver.mineclone.renderer;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import java.io.InputStream;

public class Texture {

    private int rendererID;
    private int width, height;


    public Texture(String resourcePath) {
        // Load image from resources
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) throw new RuntimeException("Texture file not found: " + resourcePath);

            byte[] bytes = in.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes);
            buffer.flip();

            IntBuffer w = MemoryUtil.memAllocInt(1);
            IntBuffer h = MemoryUtil.memAllocInt(1);
            IntBuffer channels = MemoryUtil.memAllocInt(1);

            STBImage.stbi_set_flip_vertically_on_load(true);
            ByteBuffer image = STBImage.stbi_load_from_memory(buffer, w, h, channels, 4);
            if (image == null) {
                throw new RuntimeException("Failed to load texture: " + resourcePath + "\n" + STBImage.stbi_failure_reason());
            }

            width = w.get();
            height = h.get();

            rendererID = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, rendererID);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, image);

            STBImage.stbi_image_free(image);
            MemoryUtil.memFree(buffer);
            MemoryUtil.memFree(w);
            MemoryUtil.memFree(h);
            MemoryUtil.memFree(channels);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void Bind() {
        glBindTexture(GL_TEXTURE_2D, rendererID);
    }

    public void Bind(int slot) {
        glActiveTexture(GL_TEXTURE0 + slot);
        glBindTexture(GL_TEXTURE_2D, rendererID);
    }

    public void Unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public int getID() {
        return rendererID;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void Delete() {
        glDeleteTextures(rendererID);
    }
}