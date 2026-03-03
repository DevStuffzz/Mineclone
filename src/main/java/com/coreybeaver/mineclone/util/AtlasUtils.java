package com.coreybeaver.mineclone.util;

import com.coreybeaver.mineclone.renderer.Texture;

public class AtlasUtils {
    // size of a single square tile within the atlas (in pixels)
    public static final int TILE_SIZE = 32;

    /**
     * Number of tiles along one edge of the atlas texture.
     */
    public static int atlasSize(Texture atlas) {
        return 16;
    }
    public static float[] getUVs(int index, int atlasSize, float texSize) {
        // atlasSize = number of textures along one axis, e.g., 16 if 16x16 textures in atlas
        atlasSize = 16;
        texSize = 1.0f / atlasSize;
        int x = index % atlasSize;
        int y = index / atlasSize;

        float u0 = x * texSize;
        float v0 = 1.0f - (y + 1) * texSize; // flip Y
        float u1 = (x + 1) * texSize;
        float v1 = 1.0f - y * texSize;

        return new float[] {
                u0, v0, // bottom-left
                u1, v0, // bottom-right
                u1, v1, // top-right
                u0, v1  // top-left
        };
    }
}