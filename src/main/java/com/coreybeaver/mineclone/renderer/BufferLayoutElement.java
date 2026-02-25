package com.coreybeaver.mineclone.renderer;

public class BufferLayoutElement {

    public int type;        // GL_FLOAT, GL_INT, etc
    public int count;       // number of components (3 for vec3)
    public boolean normalized;
    public int size;        // size in bytes of this element
    public int offset;      // offset in bytes (set automatically)

    public BufferLayoutElement(int type, int count, boolean normalized) {
        this.type = type;
        this.count = count;
        this.normalized = normalized;
        this.size = count * getSizeOfType(type);
    }

    private int getSizeOfType(int type) {
        switch (type) {
            case org.lwjgl.opengl.GL11.GL_FLOAT, org.lwjgl.opengl.GL11.GL_UNSIGNED_INT, org.lwjgl.opengl.GL11.GL_INT: return 4;
            case org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE: return 1;
        }
        throw new IllegalArgumentException("Unknown GL type: " + type);
    }
}