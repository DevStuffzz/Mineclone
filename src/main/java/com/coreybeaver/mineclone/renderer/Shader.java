package com.coreybeaver.mineclone.renderer;

import org.joml.*;
import org.lwjgl.system.MemoryStack;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.opengl.GL20.*;

public class Shader {

    private int rendererID;
    private String vertexSrc;
    private String fragmentSrc;

    public Shader(String url) {
        String vertexSrc = "";
        String fragmentSrc = "";

        try {
            InputStream in = getClass().getClassLoader().getResourceAsStream("assets/shaders/default.glsl");
            if (in == null) throw new RuntimeException("Shader file not found");
            String source = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            // Split shader source by "#type" marker
            String[] split = source.split("#type");
            for (String s : split) {
                s = s.trim();
                if (s.isEmpty()) continue;

                int eol = s.indexOf("\n");
                if (eol == -1) continue;

                String type = s.substring(0, eol).trim();
                String code = s.substring(eol + 1).trim();

                if (type.equals("vertex")) vertexSrc = code;
                else if (type.equals("fragment")) fragmentSrc = code;
            }

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        // Call the main constructor
        Init(vertexSrc, fragmentSrc);
    }


    public Shader(String vertexSrc, String fragSrc) {
        Init(vertexSrc, fragSrc);
    }

    private void Init(String vertexSrc, String fragSrc) {
        this.vertexSrc = vertexSrc;
        this.fragmentSrc = fragSrc;
        Compile();
    }


    private void Compile() {
        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexSrc);
        glCompileShader(vertexShader);

        if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) == GL_FALSE) {
            System.err.println("Vertex shader failed to compile:");
            System.err.println(glGetShaderInfoLog(vertexShader));
            glDeleteShader(vertexShader);
            return;
        }

        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentSrc);
        glCompileShader(fragmentShader);

        if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) == GL_FALSE) {
            System.err.println("Fragment shader failed to compile:");
            System.err.println(glGetShaderInfoLog(fragmentShader));
            glDeleteShader(fragmentShader);
            return;
        }

        rendererID = glCreateProgram();
        glAttachShader(rendererID, vertexShader);
        glAttachShader(rendererID, fragmentShader);
        glLinkProgram(rendererID);
        glValidateProgram(rendererID);

        if (glGetProgrami(rendererID, GL_LINK_STATUS) == GL_FALSE) {
            System.err.println("Shader program failed to link:");
            System.err.println(glGetProgramInfoLog(rendererID));
        }

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
    }

    public void Bind() {
        glUseProgram(rendererID);
    }

    public void Unbind() {
        glUseProgram(0);
    }

    private int getUniformLocation(String name) {
        return glGetUniformLocation(rendererID, name);
    }

    // ========================
    // FLOATS
    // ========================

    public void UniformFloat(String name, float v) {
        glUniform1f(getUniformLocation(name), v);
    }

    public void UniformFloat2(String name, float x, float y) {
        glUniform2f(getUniformLocation(name), x, y);
    }

    public void UniformFloat3(String name, float x, float y, float z) {
        glUniform3f(getUniformLocation(name), x, y, z);
    }

    public void UniformFloat4(String name, float x, float y, float z, float w) {
        glUniform4f(getUniformLocation(name), x, y, z, w);
    }

    public void UniformFloat2(String name, Vector2f v) {
        glUniform2f(getUniformLocation(name), v.x, v.y);
    }

    public void UniformFloat3(String name, Vector3f v) {
        glUniform3f(getUniformLocation(name), v.x, v.y, v.z);
    }

    public void UniformFloat4(String name, Vector4f v) {
        glUniform4f(getUniformLocation(name), v.x, v.y, v.z, v.w);
    }

    // ========================
    // INTEGERS
    // ========================

    public void UniformInt(String name, int v) {
        glUniform1i(getUniformLocation(name), v);
    }

    public void UniformInt2(String name, int x, int y) {
        glUniform2i(getUniformLocation(name), x, y);
    }

    public void UniformInt3(String name, int x, int y, int z) {
        glUniform3i(getUniformLocation(name), x, y, z);
    }

    public void UniformInt4(String name, int x, int y, int z, int w) {
        glUniform4i(getUniformLocation(name), x, y, z, w);
    }

    public void UniformInt2(String name, Vector2i v) {
        glUniform2i(getUniformLocation(name), v.x, v.y);
    }

    public void UniformInt3(String name, Vector3i v) {
        glUniform3i(getUniformLocation(name), v.x, v.y, v.z);
    }

    public void UniformInt4(String name, Vector4i v) {
        glUniform4i(getUniformLocation(name), v.x, v.y, v.z, v.w);
    }

    // ========================
    // MATRICES
    // ========================

    public void UniformMat3(String name, Matrix3f mat) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(9);
            mat.get(buffer);
            glUniformMatrix3fv(getUniformLocation(name), false, buffer);
        }
    }

    public void UniformMat4(String name, Matrix4f mat) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            mat.get(buffer);
            glUniformMatrix4fv(getUniformLocation(name), false, buffer);
        }
    }

    // ========================
    // MISC
    // ========================

    public void UniformBool(String name, boolean b) {
        glUniform1i(getUniformLocation(name), b ? 1 : 0);
    }

    public void UniformTexture2D(String name, int textureSlot) {
        glUniform1i(getUniformLocation(name), textureSlot);
    }
}