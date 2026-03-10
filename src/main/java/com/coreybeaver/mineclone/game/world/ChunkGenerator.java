package com.coreybeaver.mineclone.game.world;

import com.coreybeaver.mineclone.util.Noise;

public class ChunkGenerator {
    // world constants
    public static final int SEA_LEVEL = 64;

    // simple tree generator parameters
    private static final int TRUNK_HEIGHT = 4;      // slightly shorter trunks
    private static final int LEAF_RADIUS = 2;       // radius for canopy (bigger now)
    private static final double FOREST_TREE_CHANCE = 0.6; // per-column
    private static final double PLAINS_TREE_CHANCE = 0.02; // rare plains sapling

    private static final double GRASS_CHANCE = 0.25;   // fairly common
    private static final double ROSE_CHANCE  = 0.03;   // rare

    // Ore and dirt vein parameters: {id, minY, maxY, chancePerChunk, maxVeinSize}
    private static final int[][] ORE_PARAMS = {
        {4, 0, 128, 20, 10},  // dirt: everywhere, common, large veins
        {16, 40, 128, 20, 8}, // coal: high, common, medium-large veins
        {15, 30, 100, 15, 7}, // iron: high/mid, common, medium veins
        {17, 20, 60, 10, 5},  // silver: middle, less common, small-medium veins
        {18, 0, 30, 4, 4},   // diamond: deep, rare, small veins
        {19, 0, 20, 2, 3},   // ruby: deep, very rare, very small veins
        {20, 0, 25, 2, 3},   // lapis: deep, very rare, very small veins
        {21, 0, 15, 1, 3}    // emerald: deepest, rarest, very small veins
    };

    public static Chunk GenChunkOverworld(World world, int chunkX, int chunkZ) {
        Chunk chunk = new Chunk();
        // store position (in blocks) for proper mesh translation later
        chunk.setPosition(chunkX * Chunk.WIDTH, 0, chunkZ * Chunk.DEPTH);
        chunk.SetWorld(world);

        // iterate over every horizontal position; compute world coords to
        // feed into noise/biome functions.
        int chunkWaterCount = 0;
        // keep track of which local columns have trees so we don't place
        // adjacent trunks inside the same chunk
        boolean[][] treePlaced = new boolean[Chunk.WIDTH][Chunk.DEPTH];
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                int worldX = chunkX * Chunk.WIDTH + x;
                int worldZ = chunkZ * Chunk.DEPTH + z;

                // choose biome and get surface height for this column
                Biome biome = Biome.getBiome(worldX, worldZ);
                int surfaceY = biome.surfaceHeight(worldX, worldZ);

                // for ocean biomes we want a sand/sandstone seabed; compute ids
                BlockManager bm = BlockManager.Get();
                int sandId = bm.getIdByName("sand");
                int sandstoneId = bm.getIdByName("sandstone");
                int stoneId = bm.getIdByName("stone");

                int topId = biome.surfaceTopBlock(worldX, worldZ);

                // fill vertical slice based on surface height
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    if (y <= surfaceY) {
                        if (biome == Biome.DEEP_OCEAN || biome == Biome.SHALLOW_OCEAN) {
                            int depth = surfaceY - y;
                            if (depth < 2 && sandId >= 0) {
                                chunk.setBlock(x, y, z, sandId);
                            } else if (depth < 4 && sandstoneId >= 0) {
                                chunk.setBlock(x, y, z, sandstoneId);
                            } else if (stoneId >= 0) {
                                chunk.setBlock(x, y, z, stoneId);
                            }
                        } else {
                            if (y == surfaceY) {
                                chunk.setBlock(x, y, z, topId);
                            } else if (y < surfaceY && y >= surfaceY - 3) {
                                chunk.setBlock(x, y, z, 4);           // dirt remains constant
                            } else if (y < surfaceY - 3) {
                                chunk.setBlock(x, y, z, 5);           // stone
                            }
                        }
                    }
                    // above surface remains air
                }

                // the global sea level so lowlands become lakes/oceans.
                int waterLevel = SEA_LEVEL; // use constant defined above
                for (int y = 0; y <= waterLevel && y < Chunk.HEIGHT; y++) {
                    if (chunk.getBlock(x, y, z) == 0) {
                        chunk.setBlock(x, y, z, 12);
                    }
                }
            }
        }

        // Generate Ores and Dirt veins in stone
        java.util.Random random = new java.util.Random((long)chunkX * 34123123L ^ (long)chunkZ * 567891234L);
        for (int[] ore : ORE_PARAMS) {
            int oreId = ore[0];
            int minY = ore[1];
            int maxY = ore[2];
            int attempts = ore[3];
            int maxVeinSize = ore[4];

            for (int i = 0; i < attempts; i++) {
                int startX = random.nextInt(Chunk.WIDTH);
                int startY = minY + random.nextInt(Math.max(1, maxY - minY));
                int startZ = random.nextInt(Chunk.DEPTH);

                generateVein(chunk, startX, startY, startZ, oreId, maxVeinSize, random);
            }
        }

        // Carve caves AFTER water generation using 3D noise
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                int worldX = chunkX * Chunk.WIDTH + x;
                int worldZ = chunkZ * Chunk.DEPTH + z;

                for (int y = 2; y < Chunk.HEIGHT - 5; y++) {
                    if (chunk.getBlock(x, y, z) != 0 && chunk.getBlock(x, y, z) != 12) {
                        double caveNoise = Noise.fbm3D(worldX * 0.03, y * 0.05, worldZ * 0.03, 4, 2.0, 0.5);

                        // Caves should be bigger the lower they go.
                        // Decrease threshold linearly as y decreases.
                        double depthScale = (double) y / Chunk.HEIGHT; // 0 to 1
                        double threshold = 0.1 + (depthScale * 0.3); // 0.1 at bottom, 0.4 at top

                        if (caveNoise > threshold) {
                            chunk.setBlock(x, y, z, 0);
                        }
                    }
                }
            }
        }

        // Place trees and plants on grass *after* caves are generated
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                int worldX = chunkX * Chunk.WIDTH + x;
                int worldZ = chunkZ * Chunk.DEPTH + z;

                // Find the new surface after cave carving
                int surfaceY = -1;
                for (int y = Chunk.HEIGHT - 1; y >= 0; y--) {
                    int block = chunk.getBlock(x, y, z);
                    if (block != 0 && block != 12) { // Not air or water
                        surfaceY = y;
                        break;
                    }
                }

                if (surfaceY < 0) continue;

                Biome biome = Biome.getBiome(worldX, worldZ);
                int grassId = 1; // Assuming grass block is ID 1

                if (chunk.getBlock(x, surfaceY, z) == grassId) {
                    // Tree placement logic
                    Biome.SubBiome sb = biome.getSubBiome(worldX, worldZ);
                    double treeNoise = Noise.fbm(worldX * 0.1, worldZ * 0.1, 2, 2.0, 0.5);
                    double tThreshold = 1.0;
                    if (sb == Biome.SubBiome.FOREST) {
                        tThreshold = 1.0 - FOREST_TREE_CHANCE;
                    } else if (biome == Biome.PLAINS) {
                        tThreshold = 1.0 - PLAINS_TREE_CHANCE;
                    }

                    if (treeNoise > tThreshold && surfaceY + TRUNK_HEIGHT + 1 < Chunk.HEIGHT) {
                        boolean near = false;
                        for (int dx = -1; dx <= 1 && !near; dx++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                int nx = x + dx;
                                int nz = z + dz;
                                if (nx >= 0 && nx < Chunk.WIDTH && nz >= 0 && nz < Chunk.DEPTH) {
                                    if (treePlaced[nx][nz]) {
                                        near = true;
                                        break;
                                    }
                                }
                            }
                        }
                        if (!near) {
                            placeOakTree(chunk, x, surfaceY + 1, z);
                            treePlaced[x][z] = true;
                        }
                    }

                    // Decoration logic (grass/roses)
                    if (surfaceY >= SEA_LEVEL && surfaceY + 1 < Chunk.HEIGHT && chunk.getBlock(x, surfaceY + 1, z) == 0) {
                        double decoNoise = Noise.fbm(worldX * 0.2, worldZ * 0.2, 2, 2.0, 0.5);
                        decoNoise = (decoNoise + 1.0) * 0.5;
                        if (decoNoise > 1.0 - ROSE_CHANCE) {
                            chunk.setBlock(x, surfaceY + 1, z, 13);
                        } else if (decoNoise > 1.0 - GRASS_CHANCE) {
                            chunk.setBlock(x, surfaceY + 1, z, 11);
                        }
                    }
                }
            }
        }

        chunk.PostGeneration(chunkX, chunkZ);
        return chunk;
    }

    /**
     * Place a minimal oak tree at the given local chunk coordinates.  The
     * caller must ensure there is enough vertical space (checked before call).
     * Leaves are only placed inside the chunk bounds; neighbouring chunks may
     * fill in gaps when they generate.
     */
    private static void placeOakTree(Chunk chunk, int x, int y, int z) {
        int logId = BlockManager.Get().getIdByName("oak_log");
        int leafId = BlockManager.Get().getIdByName("oak_leaves");
        // vertical trunk
        for (int h = 0; h < TRUNK_HEIGHT; h++) {
            if (y + h < Chunk.HEIGHT && chunk.inBounds(x, y + h, z)) {
                chunk.setBlock(x, y + h, z, logId);
            }
        }
        // circular, blocky canopy around the top (several layers thick)
        int top = y + TRUNK_HEIGHT;
        for (int dx = -LEAF_RADIUS; dx <= LEAF_RADIUS; dx++) {
            for (int dz = -LEAF_RADIUS; dz <= LEAF_RADIUS; dz++) {
                if (dx*dx + dz*dz <= LEAF_RADIUS*LEAF_RADIUS) {
                    for (int dy = 0; dy <= 2; dy++) { // a little vertical thickness
                        int lx = x + dx;
                        int ly = top + dy;
                        int lz = z + dz;
                        if (chunk.inBounds(lx, ly, lz) && chunk.getBlock(lx, ly, lz) == 0) {
                            chunk.setBlock(lx, ly, lz, leafId);
                        }
                    }
                }
            }
        }
        // add a thinner cap on very top
        int capY = top + 3;
        int capRadius = Math.max(0, LEAF_RADIUS - 1);
        for (int dx = -capRadius; dx <= capRadius; dx++) {
            for (int dz = -capRadius; dz <= capRadius; dz++) {
                if (dx*dx + dz*dz <= capRadius*capRadius) {
                    int lx = x + dx;
                    int ly = capY;
                    int lz = z + dz;
                    if (chunk.inBounds(lx, ly, lz) && chunk.getBlock(lx, ly, lz) == 0) {
                        chunk.setBlock(lx, ly, lz, leafId);
                    }
                }
            }
        }
    }

    private static void generateVein(Chunk chunk, int x, int y, int z, int blockId, int size, java.util.Random random) {
        int stoneId = 5;
        for (int i = 0; i < size; i++) {
            if (chunk.inBounds(x, y, z)) {
                // all should be in veins, where the rarer they are the smaller the vien
                // make them spawn only in stone.
                if (chunk.getBlock(x, y, z) == stoneId) {
                    chunk.setBlock(x, y, z, blockId);
                }
            }

            // Random walk to next block in vein
            x += random.nextInt(3) - 1;
            y += random.nextInt(3) - 1;
            z += random.nextInt(3) - 1;
        }
    }
}
