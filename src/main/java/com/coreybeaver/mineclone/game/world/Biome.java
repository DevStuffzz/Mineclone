package com.coreybeaver.mineclone.game.world;

import com.coreybeaver.mineclone.util.Noise;

/**
 * A simple biome system driven by noise.  Sea level is assumed to be 64 blocks.
 * Ocean biomes exist below sea level; land biomes are above it with increasing
 * base heights.
 */
public enum Biome {
    DEEP_OCEAN(16, 4),      // deep and flat
    SHALLOW_OCEAN(40, 8),    // just below sea level (64)
    PLAINS(72, 12),          // low land above water
    HILLS(88, 20),           // rolling hills
    MOUNTAINS(104, 32);      // high peaks

    private final int baseHeight;
    private final int variation;

    Biome(int baseHeight, int variation) {
        this.baseHeight = baseHeight;
        this.variation = variation;
    }

    /**
     * Pick a biome based on low-frequency elevation noise.  Negative values
     * produce ocean biomes, positives produce land biomes.
     */
    public static Biome getBiome(int worldX, int worldZ) {
        double elevation = Noise.fbm(worldX * 0.002, worldZ * 0.002, 3, 2.0, 0.5);
        if (elevation < -0.6) return DEEP_OCEAN;
        if (elevation < -0.2) return SHALLOW_OCEAN;
        if (elevation < 0.0)  return PLAINS;
        if (elevation < 0.4)  return HILLS;
        return MOUNTAINS;
    }

    /**
     * Returns two adjacent biomes plus a blend factor t in [0,1].  When t=0
     * the result is exactly biomeA, when t=1 it's exactly biomeB; values in
     * between indicate a transition.
     */
    public static BlendResult getBiomeBlend(int worldX, int worldZ) {
        double elevation = Noise.fbm(worldX * 0.002, worldZ * 0.002, 3, 2.0, 0.5);
        // thresholds must be one more than the number of biomes so that every
        // interval has a start and end value.  order[i] blends toward order[i+1].
        double[] thresh = {-1.0, -0.6, -0.2, 0.0, 0.4, 1.0};
        Biome[] order = {DEEP_OCEAN, SHALLOW_OCEAN, PLAINS, HILLS, MOUNTAINS};

        // iterate only up to the penultimate biome, because we reference i+1
        for (int i = 0; i < order.length - 1; i++) {
            if (elevation < thresh[i+1]) {
                double t = (elevation - thresh[i]) / (thresh[i+1] - thresh[i]);
                return new BlendResult(order[i], order[i+1], t);
            }
        }
        // if we made it past the loop, elevation is in the last range or above it
        return new BlendResult(MOUNTAINS, MOUNTAINS, 0);
    }

    /**
     * Compute surface height using higher-frequency noise around baseHeight.
     */
    public int surfaceHeight(int worldX, int worldZ) {
        BlendResult br = getBiomeBlend(worldX, worldZ);
        // interpolate base height & variation between the two biomes
        double base = lerp(br.biomeA.baseHeight, br.biomeB.baseHeight, br.t);
        double var  = lerp(br.biomeA.variation, br.biomeB.variation, br.t);

        double h = Noise.fbm(worldX * 0.02, worldZ * 0.02, 4, 2.0, 0.5);
        int height = (int) (base + h * var);
        if (height < 0) height = 0;
        if (height >= Chunk.HEIGHT) height = Chunk.HEIGHT - 1;
        return height;
    }

    private static double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    public static class BlendResult {
        public final Biome biomeA, biomeB;
        public final double t;      // 0->1 motion from A to B

        public BlendResult(Biome a, Biome b, double t) {
            this.biomeA = a;
            this.biomeB = b;
            this.t = t;
        }
    }
}