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

    // Mouse look variables
    private double lastMouseX = 0;
    private double lastMouseY = 0;
    private boolean firstMouse = true;
    private float mouseSensitivity = 0.002f;

    public Camera(float width, float height) {
        this.aspect = width / height;
        projection = new Matrix4f().perspective(fov, aspect, near, far);
        updateView();
    }

    // Sets the camera's world position
    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
    }

    public Matrix4f getViewProjection() {
        updateView(); // ensure the view matrix is up-to-date
        return new Matrix4f(projection).mul(view);
    }

    public Matrix4f getViewProjectionNoTranslation() {
        updateView();

        // Copy the view matrix
        Matrix4f viewNoTranslation = new Matrix4f(view);

        // Zero out translation
        viewNoTranslation.m30(0f).m31(0f).m32(0f);

        // Multiply by projection
        return new Matrix4f(projection).mul(viewNoTranslation);
    }

    // Points the camera at a target in world space
    public void lookAt(float targetX, float targetY, float targetZ) {
        Vector3f dir = new Vector3f(targetX, targetY, targetZ).sub(position).normalize();
        // Compute rotation angles from the direction vector
        rotation.x = (float) Math.asin(dir.y); // pitch
        rotation.y = (float) Math.atan2(dir.x, dir.z); // yaw
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

        float moveSpeed = 10.0f * deltaTime;

        // =========================
        // MOUSE LOOK
        // =========================

        double mouseX = -Input.getMouseX();
        double mouseY = Input.getMouseY();

        if (firstMouse) {
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            firstMouse = false;
        }

        double deltaX = mouseX - lastMouseX;
        double deltaY = mouseY - lastMouseY;

        lastMouseX = mouseX;
        lastMouseY = mouseY;

        // Apply mouse movement to rotation
        rotation.y += (float) deltaX * mouseSensitivity;  // yaw
        rotation.x -= (float) deltaY * mouseSensitivity;  // pitch (inverted)

        // Clamp pitch to avoid flipping
        float maxPitch = (float) Math.toRadians(89.0f);
        if (rotation.x > maxPitch) rotation.x = maxPitch;
        if (rotation.x < -maxPitch) rotation.x = -maxPitch;

        // =========================
        // DIRECTION VECTORS
        // =========================

        // Forward on the horizontal plane (ignoring pitch for movement)
        Vector3f forward = new Vector3f(
                (float) Math.sin(rotation.y),
                0,
                (float) Math.cos(rotation.y)
        ).normalize();

        Vector3f right = new Vector3f(
                (float) Math.sin(rotation.y - Math.PI / 2.0),
                0,
                (float) Math.cos(rotation.y - Math.PI / 2.0)
        ).normalize();

        // =========================
        // MOVEMENT (WASD + Space/Shift)
        // =========================

        if (Input.getKey(GLFW_KEY_W))
            position.add(new Vector3f(forward).mul(moveSpeed));

        if (Input.getKey(GLFW_KEY_S))
            position.sub(new Vector3f(forward).mul(moveSpeed));

        if (Input.getKey(GLFW_KEY_D))
            position.add(new Vector3f(right).mul(moveSpeed));

        if (Input.getKey(GLFW_KEY_A))
            position.sub(new Vector3f(right).mul(moveSpeed));

        // Space to move up
        if (Input.getKey(GLFW_KEY_SPACE))
            position.y += moveSpeed;

        // Shift to move down
        if (Input.getKey(GLFW_KEY_LEFT_SHIFT) || Input.getKey(GLFW_KEY_RIGHT_SHIFT))
            position.y -= moveSpeed;
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

    public Vector3f getForward() {
        return new Vector3f(
                (float) Math.cos(rotation.x) * (float) Math.sin(rotation.y),
                (float) Math.sin(rotation.x),
                (float) Math.cos(rotation.x) * (float) Math.cos(rotation.y)
        ).normalize();
    }
}