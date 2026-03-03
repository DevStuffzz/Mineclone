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
        return columns.computeIfAbsent(pos, p -> ChunkGenerator.GenChunkOverworld(cx, cz));
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
            Chunk chunk = ChunkGenerator.GenChunkOverworld(next.x, next.z);
            columns.put(next, chunk);
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

    public MeshLists getMeshes(Texture atlas) {
        MeshLists lists = new MeshLists();
        // generate/collect solid meshes first
        for (Map.Entry<ColumnPos, Chunk> entry : columns.entrySet()) {
            ColumnPos pos = entry.getKey();
            Chunk column = entry.getValue();
            if (column.getSolidMesh() == null) {
                column.generateSolidMesh(atlas, this, pos.x, pos.z);
            }
            lists.solids.add(column.getSolidMesh());
        }
        // then generate/append water meshes so they render on top (for blending)
        for (Map.Entry<ColumnPos, Chunk> entry : columns.entrySet()) {
            ColumnPos pos = entry.getKey();
            Chunk column = entry.getValue();
            if (column.getWaterMesh() == null) {
                column.generateWaterMesh(atlas, this, pos.x, pos.z);
            }
            Mesh water = column.getWaterMesh();
            if (water != null) lists.waters.add(water);
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

    public static class MeshLists {
        public final List<Mesh> solids = new ArrayList<>();
        public final List<Mesh> waters = new ArrayList<>();
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
