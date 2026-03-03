package com.coreybeaver.mineclone.game.world;

import com.coreybeaver.mineclone.renderer.*;
import com.coreybeaver.mineclone.util.AtlasUtils;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_FLOAT;

public class Chunk {

    public static final int WIDTH = 16;
    // vertical size of a single chunk – enlarged so terrain can rise above
    // the sea level of 64 blocks. 128 gives room for hills and mountains.
    public static final int HEIGHT = 128;
    public static final int DEPTH = 16;

    private int[][][] blocks;
    private Mesh solidMesh;
    private Mesh waterMesh;

    // world-space position of this chunk (in block units)
    private float posX = 0, posY = 0, posZ = 0;

    public void setPosition(float x, float y, float z) {
        posX = x;
        posY = y;
        posZ = z;
    }

    public float getPosX() { return posX; }
    public float getPosY() { return posY; }
    public float getPosZ() { return posZ; }

    // helper to expose block contents for post-generation modifications
    public int getBlock(int x, int y, int z) {
        if (inBounds(x, y, z)) return blocks[x][y][z];
        return 0;
    }

    public Chunk() {
        blocks = new int[WIDTH][HEIGHT][DEPTH];
        // initialize all blocks to air (id 0)
        for (int x = 0; x < WIDTH; x++)
            for (int y = 0; y < HEIGHT; y++)
                for (int z = 0; z < DEPTH; z++)
                    blocks[x][y][z] = 0;
    }

    public void setBlock(int x, int y, int z, int blockId) {
        if (inBounds(x, y, z)) blocks[x][y][z] = blockId;
    }

    private boolean inBounds(int x, int y, int z) {
        return x >= 0 && x < WIDTH &&
                y >= 0 && y < HEIGHT &&
                z >= 0 && z < DEPTH;
    }

    public Mesh getSolidMesh() {
        return solidMesh;
    }

    public Mesh getWaterMesh() {
        return waterMesh;
    }

    /**
     * Generate only the solid (non-liquid) mesh.
     */
    /**
     * Generate the solid mesh for this chunk, culling faces against neighbours
     * both inside this chunk and in neighbouring chunks.  The world and the
     * chunk's X/Z coordinates in chunk-space are required in order to query
     * adjacent blocks across chunk boundaries.
     */
    public void generateSolidMesh(Texture atlasTexture, World world, int chunkX, int chunkZ) {
        List<Float> verts = new ArrayList<>();
        List<Integer> idx = new ArrayList<>();
        int vertCount = 0;

        int atlasSize = AtlasUtils.atlasSize(atlasTexture);
        float texSize = 1.0f / atlasSize;

        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < DEPTH; z++) {
                    int blockId = blocks[x][y][z];
                    Block block = BlockManager.Get().GetBlock(blockId);
                    boolean isSolid = block != null && block.type == BlockType.SOLID;
                    if (!isSolid) continue;

                    float[][][] faces = new float[][][]{
                            {{x, y, z+1}, {x+1, y, z+1}, {x+1, y+1, z+1}, {x, y+1, z+1}},
                            {{x+1, y, z}, {x, y, z}, {x, y+1, z}, {x+1, y+1, z}},
                            {{x, y+1, z+1}, {x+1, y+1, z+1}, {x+1, y+1, z}, {x, y+1, z}},
                            {{x, y, z}, {x+1, y, z}, {x+1, y, z+1}, {x, y, z+1}},
                            {{x+1, y, z+1}, {x+1, y, z}, {x+1, y+1, z}, {x+1, y+1, z+1}},
                            {{x, y, z}, {x, y, z+1}, {x, y+1, z+1}, {x, y+1, z}}
                    };

                    int[] texIndices = {
                            block.tex_sides, block.tex_sides, block.tex_top, block.tex_bottom, block.tex_sides, block.tex_sides
                    };

                    int[][] neighborOffsets = {
                            {0,0,1},{0,0,-1},{0,1,0},{0,-1,0},{1,0,0},{-1,0,0}
                    };

                    // compute global coordinates of current block
                    int globalX = chunkX * WIDTH + x;
                    int globalZ = chunkZ * DEPTH + z;

                    for (int f = 0; f < 6; f++) {
                        int ngx = globalX + neighborOffsets[f][0];
                        int ngy = y + neighborOffsets[f][1];
                        int ngz = globalZ + neighborOffsets[f][2];

                        int neighborId = world.getBlockAt(ngx, ngy, ngz);
                        boolean neighborEmpty = neighborId == 0;
                        if (!neighborEmpty) {
                            Block n = BlockManager.Get().GetBlock(neighborId);
                            if (n != null && n.type == BlockType.LIQUID) {
                                neighborEmpty = true;
                            }
                        }

                        if (neighborEmpty) {
                            addFace(verts, idx, faces[f], texIndices[f], atlasSize, texSize, vertCount);
                            vertCount += 4;
                        }
                    }
                }
            }
        }

        float[] array = new float[verts.size()];
        for (int i = 0; i < verts.size(); i++) array[i] = verts.get(i);
        int[] idxArray = new int[idx.size()];
        for (int i = 0; i < idx.size(); i++) idxArray[i] = idx.get(i);

        BufferLayoutElement[] layout = new BufferLayoutElement[]{
                new BufferLayoutElement(GL_FLOAT,3,false),
                new BufferLayoutElement(GL_FLOAT,2,false)
        };

        VertexArray vao = new VertexArray(array, idxArray, layout);
        vao.getPosition().set(posX, posY, posZ);
        solidMesh = new Mesh(vao, atlasTexture);
    }

    private void addFace(List<Float> vertices, List<Integer> indices, float[][] positions, int texIndex, int atlasSize, float texSize, int vertexOffset) {
        // sanity check – texture index should be valid, otherwise we'll sample the wrong cell
        if (texIndex < 0 || texIndex >= atlasSize * atlasSize) {
            System.err.println("Warning: texture index " + texIndex + " out of bounds (atlasSize=" + atlasSize + ")");
            texIndex = 0;
        }

        // compute UV coordinates using the shared utility so the calculation is in one place
        float[] uvs = AtlasUtils.getUVs(texIndex, atlasSize, texSize);

        // each face contributes 4 vertices: position + uv
        for (int i = 0; i < 4; i++) {
            vertices.add(positions[i][0]);
            vertices.add(positions[i][1]);
            vertices.add(positions[i][2]);
            vertices.add(uvs[i * 2]);      // u
            vertices.add(uvs[i * 2 + 1]);  // v
        }

        // Two triangles
        indices.add(vertexOffset);
        indices.add(vertexOffset+1);
        indices.add(vertexOffset+2);

        indices.add(vertexOffset);
        indices.add(vertexOffset+2);
        indices.add(vertexOffset+3);
    }

    /**
     * Generate mesh for water blocks. Only the top face of each liquid block
     * should be rendered (prevents cube look), so we only add a face when the
     * block is liquid and the block above is empty.
     */
    public void generateWaterMesh(Texture atlasTexture, World world, int chunkX, int chunkZ) {
        List<Float> verts = new ArrayList<>();
        List<Integer> idx = new ArrayList<>();
        int vertCount = 0;

        int atlasSize = AtlasUtils.atlasSize(atlasTexture);
        float texSize = 1.0f / atlasSize;

        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                for (int y = 0; y < HEIGHT; y++) {
                    int id = blocks[x][y][z];
                    Block b = BlockManager.Get().GetBlock(id);
                    if (b == null || b.type != BlockType.LIQUID) continue;
                    // use world coordinates to check cell above, which may lie in
                    // an adjacent chunk
                    int globalX = chunkX * WIDTH + x;
                    int globalZ = chunkZ * DEPTH + z;
                    int aboveId = world.getBlockAt(globalX, y+1, globalZ);
                    if (aboveId != 0) continue; // not air

                    float[][] face = new float[][]{{x, y+1, z+1}, {x+1, y+1, z+1}, {x+1, y+1, z}, {x, y+1, z}};
                    int tex = b.tex_top;
                    addFace(verts, idx, face, tex, atlasSize, texSize, vertCount);
                    vertCount += 4;
                }
            }
        }

        float[] array = new float[verts.size()];
        for (int i = 0; i < verts.size(); i++) array[i] = verts.get(i);
        int[] idxArray = new int[idx.size()];
        for (int i = 0; i < idx.size(); i++) idxArray[i] = idx.get(i);

        BufferLayoutElement[] layout = new BufferLayoutElement[]{
                new BufferLayoutElement(GL_FLOAT,3,false),
                new BufferLayoutElement(GL_FLOAT,2,false)
        };
        VertexArray vao = new VertexArray(array, idxArray, layout);
        vao.getPosition().set(posX, posY, posZ);
        waterMesh = new Mesh(vao, atlasTexture);
    }
}