package com.coreybeaver.mineclone.game.world;

import org.joml.Vector3f;

import com.coreybeaver.mineclone.renderer.Mesh;
import com.coreybeaver.mineclone.renderer.Texture;

import java.util.*;

public class World {
    private static final int DEFAULT_VIEW_DISTANCE = 16; // in chunks horizontally

    /**
     * loaded chunk columns keyed by their X/Z coordinates; each column covers
     * the full vertical height (256 blocks) rather than being stacked.
     */
    private final Map<ColumnPos, Chunk> columns = new HashMap<>();
    // queue of chunk positions that still need generation
    private final Queue<ColumnPos> pending = new ArrayDeque<>();
    private final Set<ColumnPos> pendingSet = new HashSet<>();

    private int viewDistance;

    public World() {
        this(DEFAULT_VIEW_DISTANCE);
    }

    public World(int viewDistance) {
        this.viewDistance = viewDistance;
    }

    /**
     * Get or generate the column at the specified chunk-space X/Z indices.
     */
    /**
     * Synchronous getter; still available but will generate immediately if
     * the column is missing.  This is rarely needed; normally the update
     * queue handles chunk creation.
     */
    public Chunk getColumn(int cx, int cz) {
        ColumnPos pos = new ColumnPos(cx, cz);
        return columns.computeIfAbsent(pos, p -> ChunkGenerator.GenChunkOverworld(this, cx, cz));
    }

    /**
     * Add the given chunk coordinates to the generation queue if the column
     * isn't already loaded or queued.
     */
    private void enqueueChunk(int cx, int cz) {
        ColumnPos pos = new ColumnPos(cx, cz);
        if (columns.containsKey(pos) || pendingSet.contains(pos)) return;
        pending.add(pos);
        pendingSet.add(pos);
    }

    /**
     * Update the set of loaded columns based on camera location.
     */
    public void update(Vector3f cameraPos) {
        int camCx = worldToChunk(cameraPos.x, Chunk.WIDTH);
        int camCz = worldToChunk(cameraPos.z, Chunk.DEPTH);

        // collect all candidate chunk positions in view distance
        List<ColumnPos> candidates = new ArrayList<>();
        for (int dx = -viewDistance; dx <= viewDistance; dx++) {
            for (int dz = -viewDistance; dz <= viewDistance; dz++) {
                candidates.add(new ColumnPos(camCx + dx, camCz + dz));
            }
        }
        // sort them by squared distance to camera – nearest first
        candidates.sort(Comparator.comparingInt(p -> {
            int ddx = p.x - camCx;
            int ddz = p.z - camCz;
            return ddx*ddx + ddz*ddz;
        }));
        // enqueue any missing chunks in that order
        for (ColumnPos pos : candidates) {
            if (!columns.containsKey(pos) && !pendingSet.contains(pos)) {
                pending.add(pos);
                pendingSet.add(pos);
            }
        }

        // generate at most one chunk per frame (the oldest in the queue, which
        // due to the ordering above will be close to the camera)
        if (!pending.isEmpty()) {
            ColumnPos next = pending.poll();
            pendingSet.remove(next);
            Chunk chunk = ChunkGenerator.GenChunkOverworld(this, next.x, next.z);
            columns.put(next, chunk);

            // Propagate light from sources in this new chunk
            propagateLightForChunk(next.x, next.z);

            // Propagate sky light for this chunk
            propagateSkyLightForChunk(next.x, next.z);

            // Spread light from neighboring chunks into the new chunk
            spreadLightFromNeighbors(next.x, next.z);
            spreadSkyLightFromNeighbors(next.x, next.z);
        }

        Iterator<ColumnPos> it = columns.keySet().iterator();
        while (it.hasNext()) {
            ColumnPos pos = it.next();
            if (Math.abs(pos.x - camCx) > viewDistance ||
                Math.abs(pos.z - camCz) > viewDistance) {
                it.remove();
            }
        }
    }

    private void propagateLightForChunk(int chunkX, int chunkZ) {
        ColumnPos pos = new ColumnPos(chunkX, chunkZ);
        Chunk chunk = columns.get(pos);
        if (chunk == null) return;

        // Find all light sources in this chunk
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int y = 0; y < Chunk.HEIGHT; y++) {
                for (int z = 0; z < Chunk.DEPTH; z++) {
                    int blockId = chunk.getBlock(x, y, z);
                    Block block = BlockManager.Get().GetBlock(blockId);
                    if (block != null && block.light) {
                        int worldX = chunkX * Chunk.WIDTH + x;
                        int worldZ = chunkZ * Chunk.DEPTH + z;
                        propagateBlockLight(worldX, y, worldZ, 15);
                    }
                }
            }
        }
    }

    private void spreadLightFromNeighbors(int chunkX, int chunkZ) {
        ColumnPos pos = new ColumnPos(chunkX, chunkZ);
        Chunk chunk = columns.get(pos);
        if (chunk == null) return;

        // Check all edge blocks of the new chunk and spread light from neighbors
        for (int y = 0; y < Chunk.HEIGHT; y++) {
            // Check all 4 edges of the chunk
            for (int x = 0; x < Chunk.WIDTH; x++) {
                // North edge (z=0)
                checkAndSpreadFromNeighbor(chunkX, chunkZ, x, y, 0, 0, 0, -1);
                // South edge (z=15)
                checkAndSpreadFromNeighbor(chunkX, chunkZ, x, y, Chunk.DEPTH - 1, 0, 0, 1);
            }
            for (int z = 0; z < Chunk.DEPTH; z++) {
                // West edge (x=0)
                checkAndSpreadFromNeighbor(chunkX, chunkZ, 0, y, z, -1, 0, 0);
                // East edge (x=15)
                checkAndSpreadFromNeighbor(chunkX, chunkZ, Chunk.WIDTH - 1, y, z, 1, 0, 0);
            }
        }
    }

    private void checkAndSpreadFromNeighbor(int chunkX, int chunkZ, int localX, int y, int localZ, int dx, int dy, int dz) {
        int worldX = chunkX * Chunk.WIDTH + localX;
        int worldZ = chunkZ * Chunk.DEPTH + localZ;

        // Get light from the neighboring block (in adjacent chunk)
        int neighborWorldX = worldX + dx;
        int neighborWorldY = y + dy;
        int neighborWorldZ = worldZ + dz;

        byte neighborLight = getBlockLightAt(neighborWorldX, neighborWorldY, neighborWorldZ);
        if (neighborLight > 1) {
            // Calculate what the light should be in the new chunk's edge block
            int newLight = (neighborLight & 0xFF) - 1;
            if (newLight <= 0) return;

            // Get the chunk and set the light directly
            ColumnPos pos = new ColumnPos(chunkX, chunkZ);
            Chunk chunk = columns.get(pos);
            if (chunk == null) return;

            int currentLight = chunk.getBlockLight(localX, y, localZ) & 0xFF;
            if (currentLight >= newLight) return; // Already lit better

            chunk.setBlockLight(localX, y, localZ, (byte) newLight);

            // Now propagate from this newly lit block into the chunk
            spreadLightIntoChunk(worldX, y, worldZ, newLight);
        }
    }

    private void spreadLightIntoChunk(int worldX, int y, int worldZ, int lightLevel) {
        Queue<int[]> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        Set<ColumnPos> affectedChunks = new HashSet<>();

        queue.add(new int[]{worldX, y, worldZ, lightLevel});
        visited.add(((long)worldX << 32) | ((long)y << 16) | (long)worldZ);

        int[][] directions = {
            {1, 0, 0}, {-1, 0, 0},
            {0, 1, 0}, {0, -1, 0},
            {0, 0, 1}, {0, 0, -1}
        };

        while (!queue.isEmpty()) {
            int[] node = queue.poll();
            int x = node[0], ny = node[1], z = node[2], level = node[3];

            int nextLevel = level - 1;
            if (nextLevel <= 0) continue;

            for (int[] dir : directions) {
                int nx = x + dir[0];
                int ny2 = ny + dir[1];
                int nz = z + dir[2];

                if (ny2 < 0 || ny2 >= Chunk.HEIGHT) continue;

                long key = ((long)nx << 32) | ((long)ny2 << 16) | (long)nz;
                if (visited.contains(key)) continue;

                int ncx = Math.floorDiv(nx, Chunk.WIDTH);
                int ncz = Math.floorDiv(nz, Chunk.DEPTH);
                ColumnPos npos = new ColumnPos(ncx, ncz);
                Chunk nchunk = columns.get(npos);
                if (nchunk == null) continue;

                int localNX = nx - ncx * Chunk.WIDTH;
                int localNZ = nz - ncz * Chunk.DEPTH;

                // Check if solid
                int blockId = nchunk.getBlock(localNX, ny2, localNZ);
                Block block = BlockManager.Get().GetBlock(blockId);
                if (block != null && block.type == BlockType.SOLID) continue;

                int currentLight = nchunk.getBlockLight(localNX, ny2, localNZ) & 0xFF;
                if (currentLight >= nextLevel) continue;

                nchunk.setBlockLight(localNX, ny2, localNZ, (byte) nextLevel);
                affectedChunks.add(npos);
                visited.add(key);
                queue.add(new int[]{nx, ny2, nz, nextLevel});
            }
        }

        // Invalidate meshes for affected chunks
        for (ColumnPos pos : affectedChunks) {
            Chunk chunk = columns.get(pos);
            if (chunk != null) {
                chunk.invalidateMeshes();
            }
        }
    }

    public void propagateBlockLight(int worldX, int y, int worldZ, int initialLight) {
        if (initialLight <= 0) return;

        Set<ColumnPos> affectedChunks = new HashSet<>();

        // Simple BFS queue
        class LightNode {
            final int x, y, z;
            final int light;
            LightNode(int x, int y, int z, int light) {
                this.x = x; this.y = y; this.z = z; this.light = light;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof LightNode)) return false;
                LightNode n = (LightNode) o;
                return x == n.x && y == n.y && z == n.z;
            }

            @Override
            public int hashCode() {
                return Objects.hash(x, y, z);
            }
        }

        Queue<LightNode> queue = new ArrayDeque<>();
        Set<LightNode> visited = new HashSet<>();

        LightNode start = new LightNode(worldX, y, worldZ, initialLight);
        queue.add(start);
        visited.add(start);

        // Directions: +X, -X, +Y, -Y, +Z, -Z
        int[][] directions = {
                {1, 0, 0}, {-1, 0, 0},
                {0, 1, 0}, {0, -1, 0},
                {0, 0, 1}, {0, 0, -1}
        };

        while (!queue.isEmpty()) {
            LightNode node = queue.poll();
            int lx = node.x, ly = node.y, lz = node.z;
            int level = node.light;

            if (ly < 0 || ly >= Chunk.HEIGHT) continue;

            int cx = Math.floorDiv(lx, Chunk.WIDTH);
            int cz = Math.floorDiv(lz, Chunk.DEPTH);
            int localX = lx - cx * Chunk.WIDTH;
            int localZ = lz - cz * Chunk.DEPTH;

            ColumnPos colPos = new ColumnPos(cx, cz);
            Chunk chunk = columns.get(colPos);
            if (chunk == null) continue;

            int currentLight = chunk.getBlockLight(localX, ly, localZ) & 0xFF;
            if (currentLight >= level) continue;

            chunk.setBlockLight(localX, ly, localZ, (byte) level);
            affectedChunks.add(colPos);

            // Spread to neighbors
            int nextLevel = level - 1;
            if (nextLevel <= 0) continue;

            for (int[] dir : directions) {
                int nx = lx + dir[0];
                int ny = ly + dir[1];
                int nz = lz + dir[2];

                int ncx = Math.floorDiv(nx, Chunk.WIDTH);
                int ncz = Math.floorDiv(nz, Chunk.DEPTH);
                Chunk neighborChunk = columns.get(new ColumnPos(ncx, ncz));
                if (neighborChunk == null) continue;

                int localNX = nx - ncx * Chunk.WIDTH;
                int localNZ = nz - ncz * Chunk.DEPTH;

                int neighborBlockId = neighborChunk.getBlock(localNX, ny, localNZ);
                Block neighborBlock = BlockManager.Get().GetBlock(neighborBlockId);
                if (neighborBlock != null && neighborBlock.type == BlockType.SOLID) continue;

                LightNode nextNode = new LightNode(nx, ny, nz, nextLevel);
                if (!visited.contains(nextNode)) {
                    queue.add(nextNode);
                    visited.add(nextNode);
                }
            }
        }

        // Rebuild meshes for all affected chunks immediately
        for (ColumnPos pos : affectedChunks) {
            Chunk chunk = columns.get(pos);
            if (chunk != null) {
                chunk.invalidateMeshes();
            }
        }
    }

    // Sky light propagation - treat surface blocks (blocks with only air above) as light sources
    private void propagateSkyLightForChunk(int chunkX, int chunkZ) {
        ColumnPos pos = new ColumnPos(chunkX, chunkZ);
        Chunk chunk = columns.get(pos);
        if (chunk == null) return;

        // Find all surface blocks (any non-air block with only air above them)
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                // Find the highest non-air block in this column
                for (int y = Chunk.HEIGHT - 1; y >= 0; y--) {
                    int blockId = chunk.getBlock(x, y, z);

                    // Skip air blocks
                    if (blockId == 0) {
                        continue;
                    }

                    Block block = BlockManager.Get().GetBlock(blockId);
                    if (block == null) {
                        continue;
                    }

                    // Found any non-air block - check if there's only air above
                    boolean isSurface = true;
                    for (int checkY = y + 1; checkY < Chunk.HEIGHT; checkY++) {
                        int aboveId = chunk.getBlock(x, checkY, z);
                        if (aboveId != 0) {
                            // There's a non-air block above, not a surface
                            isSurface = false;
                            break;
                        }
                    }

                    // If this is a surface block, propagate skylight from it
                    if (isSurface) {
                        int worldX = chunkX * Chunk.WIDTH + x;
                        int worldZ = chunkZ * Chunk.DEPTH + z;
                        propagateSkyLight(worldX, y, worldZ, 15);
                    }

                    // Only check the highest non-air block in each column
                    break;
                }
            }
        }
    }

    public void propagateSkyLight(int worldX, int y, int worldZ, int initialLight) {
        if (initialLight <= 0) return;

        Set<ColumnPos> affectedChunks = new HashSet<>();

        class LightNode {
            final int x, y, z;
            final int light;
            LightNode(int x, int y, int z, int light) {
                this.x = x; this.y = y; this.z = z; this.light = light;
            }
            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof LightNode)) return false;
                LightNode that = (LightNode) o;
                return x == that.x && y == that.y && z == that.z;
            }
            @Override
            public int hashCode() {
                return ((x * 31 + y) * 31 + z);
            }
        }

        Queue<LightNode> queue = new ArrayDeque<>();
        Set<LightNode> visited = new HashSet<>();

        queue.add(new LightNode(worldX, y, worldZ, initialLight));
        visited.add(new LightNode(worldX, y, worldZ, 0));

        int[][] directions = {
            {1, 0, 0}, {-1, 0, 0},
            {0, 1, 0}, {0, -1, 0},
            {0, 0, 1}, {0, 0, -1}
        };

        while (!queue.isEmpty()) {
            LightNode node = queue.poll();
            int lx = node.x, ly = node.y, lz = node.z, level = node.light;

            if (ly < 0 || ly >= Chunk.HEIGHT) continue;

            int cx = Math.floorDiv(lx, Chunk.WIDTH);
            int cz = Math.floorDiv(lz, Chunk.DEPTH);
            int localX = lx - cx * Chunk.WIDTH;
            int localZ = lz - cz * Chunk.DEPTH;

            ColumnPos colPos = new ColumnPos(cx, cz);
            Chunk chunk = columns.get(colPos);
            if (chunk == null) continue;

            int currentLight = chunk.getSkyLight(localX, ly, localZ) & 0xFF;
            if (currentLight >= level) continue;

            chunk.setSkyLight(localX, ly, localZ, (byte) level);
            affectedChunks.add(colPos);

            // Spread to neighbors
            for (int[] dir : directions) {
                int nx = lx + dir[0];
                int ny = ly + dir[1];
                int nz = lz + dir[2];

                int ncx = Math.floorDiv(nx, Chunk.WIDTH);
                int ncz = Math.floorDiv(nz, Chunk.DEPTH);
                Chunk neighborChunk = columns.get(new ColumnPos(ncx, ncz));
                if (neighborChunk == null) continue;

                int localNX = nx - ncx * Chunk.WIDTH;
                int localNZ = nz - ncz * Chunk.DEPTH;

                int neighborBlockId = neighborChunk.getBlock(localNX, ny, localNZ);
                Block neighborBlock = BlockManager.Get().GetBlock(neighborBlockId);
                if (neighborBlock != null && neighborBlock.type == BlockType.SOLID) continue;

                // Calculate light decay based on block type
                // Water blocks cause extra decay (2 instead of 1)
                int decay = 1;
                if (neighborBlock != null && neighborBlock.type == BlockType.LIQUID) {
                    decay = 2;
                }

                int nextLevel = level - decay;
                if (nextLevel <= 0) continue;

                LightNode nextNode = new LightNode(nx, ny, nz, nextLevel);
                if (!visited.contains(nextNode)) {
                    queue.add(nextNode);
                    visited.add(nextNode);
                }
            }
        }

        // Rebuild meshes for all affected chunks
        for (ColumnPos pos : affectedChunks) {
            Chunk chunk = columns.get(pos);
            if (chunk != null) {
                chunk.invalidateMeshes();
            }
        }
    }

    private void spreadSkyLightFromNeighbors(int chunkX, int chunkZ) {
        ColumnPos pos = new ColumnPos(chunkX, chunkZ);
        Chunk chunk = columns.get(pos);
        if (chunk == null) return;

        // Check all edge blocks of the new chunk and spread sky light from neighbors
        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int x = 0; x < Chunk.WIDTH; x++) {
                checkAndSpreadSkyLightFromNeighbor(chunkX, chunkZ, x, y, 0, 0, 0, -1);
                checkAndSpreadSkyLightFromNeighbor(chunkX, chunkZ, x, y, Chunk.DEPTH - 1, 0, 0, 1);
            }
            for (int z = 0; z < Chunk.DEPTH; z++) {
                checkAndSpreadSkyLightFromNeighbor(chunkX, chunkZ, 0, y, z, -1, 0, 0);
                checkAndSpreadSkyLightFromNeighbor(chunkX, chunkZ, Chunk.WIDTH - 1, y, z, 1, 0, 0);
            }
        }
    }

    private void checkAndSpreadSkyLightFromNeighbor(int chunkX, int chunkZ, int localX, int y, int localZ, int dx, int dy, int dz) {
        int worldX = chunkX * Chunk.WIDTH + localX;
        int worldZ = chunkZ * Chunk.DEPTH + localZ;

        int neighborWorldX = worldX + dx;
        int neighborWorldY = y + dy;
        int neighborWorldZ = worldZ + dz;

        byte neighborLight = getSkyLightAt(neighborWorldX, neighborWorldY, neighborWorldZ);
        if (neighborLight > 1) {
            int newLight = (neighborLight & 0xFF) - 1;
            if (newLight <= 0) return;

            ColumnPos pos = new ColumnPos(chunkX, chunkZ);
            Chunk chunk = columns.get(pos);
            if (chunk == null) return;

            int currentLight = chunk.getSkyLight(localX, y, localZ) & 0xFF;
            if (currentLight >= newLight) return;

            chunk.setSkyLight(localX, y, localZ, (byte) newLight);
            spreadSkyLightIntoChunk(worldX, y, worldZ, newLight);
        }
    }

    private void spreadSkyLightIntoChunk(int worldX, int y, int worldZ, int lightLevel) {
        Queue<int[]> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        Set<ColumnPos> affectedChunks = new HashSet<>();

        queue.add(new int[]{worldX, y, worldZ, lightLevel});
        visited.add(((long)worldX << 32) | ((long)y << 16) | (long)worldZ);

        int[][] directions = {
            {1, 0, 0}, {-1, 0, 0},
            {0, 1, 0}, {0, -1, 0},
            {0, 0, 1}, {0, 0, -1}
        };

        while (!queue.isEmpty()) {
            int[] node = queue.poll();
            int x = node[0], ny = node[1], z = node[2], level = node[3];

            int nextLevel = level - 1;
            if (nextLevel <= 0) continue;

            for (int[] dir : directions) {
                int nx = x + dir[0];
                int ny2 = ny + dir[1];
                int nz = z + dir[2];

                if (ny2 < 0 || ny2 >= Chunk.HEIGHT) continue;

                long key = ((long)nx << 32) | ((long)ny2 << 16) | (long)nz;
                if (visited.contains(key)) continue;

                int ncx = Math.floorDiv(nx, Chunk.WIDTH);
                int ncz = Math.floorDiv(nz, Chunk.DEPTH);
                ColumnPos npos = new ColumnPos(ncx, ncz);
                Chunk nchunk = columns.get(npos);
                if (nchunk == null) continue;

                int localNX = nx - ncx * Chunk.WIDTH;
                int localNZ = nz - ncz * Chunk.DEPTH;

                int blockId = nchunk.getBlock(localNX, ny2, localNZ);
                Block block = BlockManager.Get().GetBlock(blockId);
                if (block != null && block.type == BlockType.SOLID) continue;

                int currentLight = nchunk.getSkyLight(localNX, ny2, localNZ) & 0xFF;
                if (currentLight >= nextLevel) continue;

                nchunk.setSkyLight(localNX, ny2, localNZ, (byte) nextLevel);
                affectedChunks.add(npos);
                visited.add(key);
                queue.add(new int[]{nx, ny2, nz, nextLevel});
            }
        }

        for (ColumnPos pos : affectedChunks) {
            Chunk chunk = columns.get(pos);
            if (chunk != null) {
                chunk.invalidateMeshes();
            }
        }
    }


    public byte getSkyLightAt(int worldX, int y, int worldZ) {
        int cx = (int)Math.floor((double)worldX / Chunk.WIDTH);
        int cz = (int)Math.floor((double)worldZ / Chunk.DEPTH);

        ColumnPos pos = new ColumnPos(cx, cz);
        Chunk c = columns.get(pos);

        if(c == null) return 0;

        int lx = worldX - cx * Chunk.WIDTH;
        int lz = worldZ - cz * Chunk.DEPTH;

        return c.getSkyLight(lx, y, lz);
    }

    public byte getBlockLightAt(int worldX, int y, int worldZ) {
        int cx = (int)Math.floor((double)worldX / Chunk.WIDTH);
        int cz = (int)Math.floor((double)worldZ / Chunk.DEPTH);

        ColumnPos pos = new ColumnPos(cx, cz);
        Chunk c = columns.get(pos);

        if(c == null) return 0;

        int lx = worldX - cx * Chunk.WIDTH;
        int lz = worldZ - cz * Chunk.DEPTH;

        return c.getBlockLight(lx, y, lz);
    }

    public MeshLists getMeshes(Texture atlas) {
        MeshLists lists = new MeshLists();

        // SOLID BLOCKS
        for (Map.Entry<ColumnPos, Chunk> entry : columns.entrySet()) {
            ColumnPos pos = entry.getKey();
            Chunk column = entry.getValue();

            if (column.getSolidMesh() == null) {
                column.generateSolidMesh(atlas, this, pos.x, pos.z);
            }

            lists.solids.add(column.getSolidMesh());
        }

        // WATER (render after solids for blending)
        for (Map.Entry<ColumnPos, Chunk> entry : columns.entrySet()) {
            ColumnPos pos = entry.getKey();
            Chunk column = entry.getValue();

            if (column.getWaterMesh() == null) {
                column.generateWaterMesh(atlas, this, pos.x, pos.z);
            }

            Mesh water = column.getWaterMesh();
            if (water != null) {
                lists.waters.add(water);
            }
        }

        // BILLBOARDS (plants, render after water ideally with blending enabled)
        for (Map.Entry<ColumnPos, Chunk> entry : columns.entrySet()) {
            ColumnPos pos = entry.getKey();
            Chunk column = entry.getValue();

            if (column.getBillboardMesh() == null) {
                column.generateBillboardMesh(atlas, this, pos.x, pos.z);
            }

            Mesh billboard = column.getBillboardMesh();
            if (billboard != null) {
                lists.billboards.add(billboard);
            }
        }

        return lists;
    }

    private int worldToChunk(float coord, int chunkSize) {
        return (int) Math.floor(coord / chunkSize);
    }

    /**
     * Return the block ID at the given world coordinates.  If the containing
     * chunk is not loaded the method returns 0 (treated as air).  This is used
     * during mesh generation to decide whether faces between adjacent chunks
     * should be culled.
     */
    public int getBlockAt(int worldX, int y, int worldZ) {
        int cx = (int) Math.floor((double) worldX / Chunk.WIDTH);
        int cz = (int) Math.floor((double) worldZ / Chunk.DEPTH);
        ColumnPos pos = new ColumnPos(cx, cz);
        Chunk c = columns.get(pos);
        if (c == null) return 0;
        int lx = worldX - cx * Chunk.WIDTH;
        int lz = worldZ - cz * Chunk.DEPTH;
        if (lx < 0 || lx >= Chunk.WIDTH || y < 0 || y >= Chunk.HEIGHT || lz < 0 || lz >= Chunk.DEPTH) {
            return 0;
        }
        return c.getBlock(lx, y, lz);
    }

    public void setBlockAt(int worldX, int y, int worldZ, int blockId) {
        int cx = (int) Math.floor((double) worldX / Chunk.WIDTH);
        int cz = (int) Math.floor((double) worldZ / Chunk.DEPTH);
        ColumnPos pos = new ColumnPos(cx, cz);
        Chunk c = columns.get(pos);
        if (c == null) return;
        int lx = worldX - cx * Chunk.WIDTH;
        int lz = worldZ - cz * Chunk.DEPTH;
        if (lx < 0 || lx >= Chunk.WIDTH || y < 0 || y >= Chunk.HEIGHT || lz < 0 || lz >= Chunk.DEPTH) {
            return;
        }
        c.setBlock(lx, y, lz, blockId);
        c.invalidateMeshes();

        // If it's a light source, propagate light
        Block block = BlockManager.Get().GetBlock(blockId);
        if (block != null && block.light) {
            propagateBlockLight(worldX, y, worldZ, 15);
        }
    }

    public static class MeshLists {
        public final List<Mesh> solids = new ArrayList<>();
        public final List<Mesh> waters = new ArrayList<>();
        public final List<Mesh> billboards = new ArrayList<>();
    }

    private static class ColumnPos {
        final int x, z;
        ColumnPos(int x, int z) { this.x = x; this.z = z; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ColumnPos)) return false;
            ColumnPos c = (ColumnPos) o;
            return x == c.x && z == c.z;
        }
        @Override public int hashCode() {
            return Objects.hash(x, z);
        }
    }
}
