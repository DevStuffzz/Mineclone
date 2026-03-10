package com.coreybeaver.mineclone.io;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;

public class Input {

    private static boolean[] keys = new boolean[GLFW.GLFW_KEY_LAST];
    private static boolean[] prevKeys = new boolean[GLFW.GLFW_KEY_LAST];

    private static boolean[] buttons = new boolean[GLFW.GLFW_MOUSE_BUTTON_LAST];
    private static boolean[] prevButtons = new boolean[GLFW.GLFW_MOUSE_BUTTON_LAST];

    private static double mouseX;
    private static double mouseY;

    public static void init(long window) {

        // Keyboard input
        GLFW.glfwSetKeyCallback(window, new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key >= 0 && key < keys.length) {
                    keys[key] = action != GLFW.GLFW_RELEASE;
                }
            }
        });

        // Mouse buttons
        GLFW.glfwSetMouseButtonCallback(window, new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                if (button >= 0 && button < buttons.length) {
                    buttons[button] = action != GLFW.GLFW_RELEASE;
                }
            }
        });

        // Mouse movement
        GLFW.glfwSetCursorPosCallback(window, new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                mouseX = xpos;
                mouseY = ypos;
            }
        });
    }

    /** Call this at the start of each frame to update previous states */
    public static void update() {
        System.arraycopy(keys, 0, prevKeys, 0, keys.length);
        System.arraycopy(buttons, 0, prevButtons, 0, buttons.length);
    }

    // -------- KEYBOARD --------
    /** Returns true while key is held down */
    public static boolean getKey(int key) {
        return keys[key];
    }

    /** Returns true only on the first frame the key is pressed */
    public static boolean getKeyDown(int key) {
        return keys[key] && !prevKeys[key];
    }

    // -------- MOUSE --------
    /** Returns true while button is held down */
    public static boolean getButton(int button) {
        return buttons[button];
    }

    /** Returns true only on the first frame the button is pressed */
    public static boolean getButtonDown(int button) {
        return buttons[button] && !prevButtons[button];
    }

    public static double getMouseX() {
        return mouseX;
    }

    public static double getMouseY() {
        return mouseY;
    }
}