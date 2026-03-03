package com.coreybeaver.mineclone.game.world;

public class ChunkGenerator {
    // world constants
    public static final int SEA_LEVEL = 64;

    public static Chunk GenChunkOverworld(int chunkX, int chunkZ) {
        Chunk chunk = new Chunk();
        // store position (in blocks) for proper mesh translation later
        chunk.setPosition(chunkX * Chunk.WIDTH, 0, chunkZ * Chunk.DEPTH);

        // iterate over every horizontal position; compute world coords to
        // feed into noise/biome functions.
        int chunkWaterCount = 0;
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
                                chunk.setBlock(x, y, z, 1);           // grass
                            } else if (y < surfaceY && y >= surfaceY - 3) {
                                chunk.setBlock(x, y, z, 4);           // dirt
                            } else if (y < surfaceY - 3) {
                                chunk.setBlock(x, y, z, 5);           // stone
                            }
                        }
                    }
                    // above surface remains air
                }
                // after filling column, place water in any empty block below
                // the global sea level so lowlands become lakes/oceans.
                int waterLevel = SEA_LEVEL; // use constant defined above
                for (int y = 0; y <= waterLevel && y < Chunk.HEIGHT; y++) {
                    if (chunk.getBlock(x, y, z) == 0) {
                        chunk.setBlock(x, y, z, 11);
                        chunkWaterCount++;
                    }
                }
            }
        }


        return chunk;
    }
}
