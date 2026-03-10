package com.coreybeaver.mineclone.util;

/**
 * Simple 2D Perlin noise implementation adapted from Ken Perlin's improved
 * noise algorithm. Returns values in the range [-1,1].
 */
public class Noise {
    private static final int[] perm = new int[512];

    static {
        int[] p = {
            151,160,137,91,90,15,
            131,13,201,95,96,53,194,233,7,225,140,36,103,30,69,142,8,99,37,240,
            21,10,23,190, 6,148,247,120,234,75,0,26,197,62,94,252,219,203,117,
            35,11,32,57,177,33,88,237,149,56,87,174,20,125,136,171,168, 68,175,
            74,165,71,134,139,48,27,166,77,146,158,231,83,111,229,122,60,211,
            133,230,220,105,92,41,55,46,245,40,244,102,143,54, 65,25,63,161,1,
            216,80,73,209,76,132,187,208, 89,18,169,200,196,135,130,116,188,159,
            86,164,100,109,198,173,186, 3,64,52,217,226,250,124,123,5,202,38,
            147,118,126,255,82,85,212,207,206,59,227,47,16,58,17,182,189,28,42,
            223,183,170,213,119,248,152, 2,44,154,163,70,221,153,101,155,167,
            43,172,9,129,22,39,253,19,98,108,110,79,113,224,232,178,185,112,104,
            218,246,97,228,251,34,242,193,238,210,144,12,191,179,162,241,81,51,
            145,235,249,14,239,107,49,192,214,31,181,199,106,157,184,84,204,176,
            115,121,50,45,127, 4,150,254,138,236,205,93,222,114,67,29,24,72,243,
            141,128,195,78,66,215,61,156,180
        };
        for (int i = 0; i < 512; i++) {
            perm[i] = p[i & 255];
        }
    }

    public static double noise(double x, double y) {
        int X = fastfloor(x) & 255;
        int Y = fastfloor(y) & 255;
        x -= fastfloor(x);
        y -= fastfloor(y);
        double u = fade(x);
        double v = fade(y);

        int A = perm[X] + Y;
        int AA = perm[A];
        int AB = perm[A + 1];
        int B = perm[X + 1] + Y;
        int BA = perm[B];
        int BB = perm[B + 1];

        double res = lerp(v, lerp(u, grad(perm[AA], x, y), grad(perm[BA], x - 1, y)),
                             lerp(u, grad(perm[AB], x, y - 1), grad(perm[BB], x - 1, y - 1)));
        return res;
    }

    public static double noise3D(double x, double y, double z) {
        int X = fastfloor(x) & 255;
        int Y = fastfloor(y) & 255;
        int Z = fastfloor(z) & 255;

        x -= fastfloor(x);
        y -= fastfloor(y);
        z -= fastfloor(z);

        double u = fade(x);
        double v = fade(y);
        double w = fade(z);

        int A = perm[X] + Y;
        int AA = perm[A] + Z;
        int AB = perm[A + 1] + Z;
        int B = perm[X + 1] + Y;
        int BA = perm[B] + Z;
        int BB = perm[B + 1] + Z;

        return lerp(w,
            lerp(v,
                lerp(u, grad3D(perm[AA], x, y, z), grad3D(perm[BA], x - 1, y, z)),
                lerp(u, grad3D(perm[AB], x, y - 1, z), grad3D(perm[BB], x - 1, y - 1, z))
            ),
            lerp(v,
                lerp(u, grad3D(perm[AA + 1], x, y, z - 1), grad3D(perm[BA + 1], x - 1, y, z - 1)),
                lerp(u, grad3D(perm[AB + 1], x, y - 1, z - 1), grad3D(perm[BB + 1], x - 1, y - 1, z - 1))
            )
        );
    }

    /**
     * Simple fractional Brownian motion over multiple octaves, returning
     * values in [-1,1].  Increasing octaves adds detail (roughness).
     */
    public static double fbm(double x, double y, int octaves, double lacunarity, double gain) {
        double total = 0;
        double frequency = 1;
        double amplitude = 1;
        double max = 0;

        for (int i = 0; i < octaves; i++) {
            total += noise(x * frequency, y * frequency) * amplitude;
            max += amplitude;
            amplitude *= gain;
            frequency *= lacunarity;
        }
        return total / max;
    }

    public static double fbm3D(double x, double y, double z, int octaves, double lacunarity, double gain) {
        double total = 0;
        double frequency = 1;
        double amplitude = 1;
        double max = 0;

        for (int i = 0; i < octaves; i++) {
            total += noise3D(x * frequency, y * frequency, z * frequency) * amplitude;
            max += amplitude;
            amplitude *= gain;
            frequency *= lacunarity;
        }
        return total / max;
    }
    private static int fastfloor(double x) {
        return x > 0 ? (int) x : (int) x - 1;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double grad(int hash, double x, double y) {
        int h = hash & 7;      // Convert low 3 bits of hash code
        double u = h < 4 ? x : y;
        double v = h < 4 ? y : x;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    private static double grad3D(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : h == 12 || h == 14 ? x : z;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
}