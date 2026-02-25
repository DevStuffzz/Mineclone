package com.coreybeaver.mineclone.scene;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import com.coreybeaver.mineclone.io.Input;
import static org.lwjgl.glfw.GLFW.*;

public class Camera {

    private Matrix4f projection;
    private Matrix4f view;

    private Vector3f position = new Vector3f(0, 0, 3);
    private Vector3f rotation = new Vector3f(0, 0, 0);

    private float fov = (float) Math.toRadians(70.0f);
    private float aspect;
    private float near = 0.1f;
    private float far = 1000f;

    public Camera(float width, float height) {
        this.aspect = width / height;
        projection = new Matrix4f().perspective(fov, aspect, near, far);
        updateView();
    }

    private void updateView() {

        Vector3f direction = new Vector3f(
                (float) Math.cos(rotation.x) * (float) Math.sin(rotation.y),
                (float) Math.sin(rotation.x),
                (float) Math.cos(rotation.x) * (float) Math.cos(rotation.y)
        );

        Vector3f target = new Vector3f(position).add(direction);

        view = new Matrix4f().lookAt(
                position,
                target,
                new Vector3f(0, 1, 0)
        );
    }

    public void Update(float deltaTime) {

        float moveSpeed = 5.0f * deltaTime;
        float lookSpeed = -2.5f * deltaTime;

        // =========================
        // ROTATION (Arrow Keys)
        // =========================

        if (Input.isKeyDown(GLFW_KEY_UP))
            rotation.x -= lookSpeed;

        if (Input.isKeyDown(GLFW_KEY_DOWN))
            rotation.x += lookSpeed;

        if (Input.isKeyDown(GLFW_KEY_LEFT))
            rotation.y -= lookSpeed;

        if (Input.isKeyDown(GLFW_KEY_RIGHT))
            rotation.y += lookSpeed;

        // Clamp pitch to avoid flipping
        float maxPitch = (float) Math.toRadians(89.0f);
        if (rotation.x > maxPitch) rotation.x = maxPitch;
        if (rotation.x < -maxPitch) rotation.x = -maxPitch;

        // =========================
        // DIRECTION VECTORS
        // =========================

        Vector3f forward = new Vector3f(
                (float) Math.cos(rotation.x) * (float) Math.sin(rotation.y),
                (float) Math.sin(rotation.x),
                (float) Math.cos(rotation.x) * (float) Math.cos(rotation.y)
        ).normalize();

        Vector3f right = new Vector3f(
                (float) Math.sin(rotation.y - Math.PI / 2.0),
                0,
                (float) Math.cos(rotation.y - Math.PI / 2.0)
        ).normalize();

        // =========================
        // MOVEMENT (WASD)
        // =========================

        if (Input.isKeyDown(GLFW_KEY_W))
            position.add(new Vector3f(forward).mul(moveSpeed));

        if (Input.isKeyDown(GLFW_KEY_S))
            position.sub(new Vector3f(forward).mul(moveSpeed));

        if (Input.isKeyDown(GLFW_KEY_D))
            position.add(new Vector3f(right).mul(moveSpeed));

        if (Input.isKeyDown(GLFW_KEY_A))
            position.sub(new Vector3f(right).mul(moveSpeed));
    }

    public Matrix4f getProjection() {
        return projection;
    }

    public Matrix4f getView() {
        updateView();
        return view;
    }

    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getRotation() {
        return rotation;
    }
}