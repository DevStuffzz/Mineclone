package com.coreybeaver.mineclone.renderer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class VertexArray {

    private int vaoID;
    private int vboID;
    private int eboID;

    private int indexCount;

    private Vector3f position = new Vector3f(0,0,0);
    private Vector3f rotation = new Vector3f(0,0,0);
    private Vector3f scale = new Vector3f(1,1,1);

    public VertexArray(float[] vertices,
                       int[] indices,
                       BufferLayoutElement[] layout) {

        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        // VBO
        vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        // EBO
        eboID = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
        indexCount = indices.length;

        // Layout
        int stride = 0;
        for (BufferLayoutElement element : layout)
            stride += element.size;

        int offset = 0;
        for (int i = 0; i < layout.length; i++) {
            BufferLayoutElement element = layout[i];

            glEnableVertexAttribArray(i);

            if (element.type == GL_FLOAT) {
                glVertexAttribPointer(i, element.count, element.type,
                        element.normalized, stride, offset);
            } else {
                glVertexAttribIPointer(i, element.count,
                        element.type, stride, offset);
            }

            offset += element.size;
        }

        glBindVertexArray(0);
    }

    public void Bind() {
        glBindVertexArray(vaoID);
    }

    public int GetIndexCount() {
        return indexCount;
    }

    public void Delete() {
        glDeleteBuffers(vboID);
        glDeleteBuffers(eboID);
        glDeleteVertexArrays(vaoID);
    }


    public Matrix4f getModelMatrix() {
        return new Matrix4f()
                .identity()
                .translate(position)
                .rotateX(rotation.x)
                .rotateY(rotation.y)
                .rotateZ(rotation.z)
                .scale(scale);
    }

    public Vector3f getPosition() { return position; }
    public Vector3f getRotation() { return rotation; }
    public Vector3f getScale() { return scale; }

}