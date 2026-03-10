package com.coreybeaver.mineclone.game.world;

import com.coreybeaver.mineclone.renderer.*;
import com.coreybeaver.mineclone.util.AtlasUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static org.lwjgl.opengl.GL11.GL_FLOAT;

public class Chunk {

    public static final int WIDTH = 16;
    public static final int HEIGHT = 128;
    public static final int DEPTH = 16;

    private int[][][] blocks;
    private byte[][][] blockLight;  // Light from light-emitting blocks
    private byte[][][] skyLight;    // Light from sky
    private Mesh solidMesh;
    private Mesh waterMesh;
    private Mesh billboardMesh;

    private World world = null;

    private float posX = 0, posY = 0, posZ = 0;

    public Chunk() {
        blocks = new int[WIDTH][HEIGHT][DEPTH];
        blockLight = new byte[WIDTH][HEIGHT][DEPTH];
        skyLight = new byte[WIDTH][HEIGHT][DEPTH];

        // Initialize all light to 0
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < DEPTH; z++) {
                    blocks[x][y][z] = 0;
                    blockLight[x][y][z] = 0;
                    skyLight[x][y][z] = 0;
                }
            }
        }
    }

    public void SetWorld(World world) {
        this.world = world;
    }

    public void setPosition(float x, float y, float z) {
        posX = x;
        posY = y;
        posZ = z;
    }

    public float getPosX() { return posX; }
    public float getPosY() { return posY; }
    public float getPosZ() { return posZ; }

    public int getBlock(int x, int y, int z) {
        if (inBounds(x, y, z)) return blocks[x][y][z];
        return 0;
    }

    public void setBlock(int x, int y, int z, int blockId) {
        if (!inBounds(x, y, z)) return;
        blocks[x][y][z] = blockId;
    }

    // Function ran right after chunk generation
    public void PostGeneration(int chunkX, int chunkZ) {
        // Initialize all light to 0
        for(int x = 0; x < WIDTH; x++) {
            for(int y = 0; y < HEIGHT; y++) {
                for(int z = 0; z < DEPTH; z++) {
                    blockLight[x][y][z] = 0;
                }
            }
        }
    }

    // Propagate light within this chunk only using BFS
    private void propagateLightInChunk(int startX, int startY, int startZ, int startLight) {
        class LightNode {
            int x, y, z, light;
            LightNode(int x, int y, int z, int light) {
                this.x = x; this.y = y; this.z = z; this.light = light;
            }
        }

        Queue<LightNode> queue = new java.util.ArrayDeque<>();
        boolean[][][] visited = new boolean[WIDTH][HEIGHT][DEPTH];

        queue.add(new LightNode(startX, startY, startZ, startLight));
        visited[startX][startY][startZ] = true;

        int[][] directions = {
            {1, 0, 0}, {-1, 0, 0},
            {0, 1, 0}, {0, -1, 0},
            {0, 0, 1}, {0, 0, -1}
        };

        int propagated = 0;
        while (!queue.isEmpty()) {
            LightNode node = queue.poll();

            // Spread to neighbors
            int nextLight = node.light - 1;
            if (nextLight <= 0) continue;

            for (int[] dir : directions) {
                int nx = node.x + dir[0];
                int ny = node.y + dir[1];
                int nz = node.z + dir[2];

                // Check bounds (stay within chunk)
                if (!inBounds(nx, ny, nz)) continue;

                // Check if already visited
                if (visited[nx][ny][nz]) continue;

                // Check if neighbor is solid (blocks light)
                int neighborId = blocks[nx][ny][nz];
                Block neighborBlock = BlockManager.Get().GetBlock(neighborId);
                if (neighborBlock != null && neighborBlock.type == BlockType.SOLID) continue;

                // Check if current light is better
                int currentLight = blockLight[nx][ny][nz] & 0xFF;
                if (currentLight >= nextLight) continue;

                // Set light and add to queue
                blockLight[nx][ny][nz] = (byte) nextLight;
                visited[nx][ny][nz] = true;
                queue.add(new LightNode(nx, ny, nz, nextLight));
                propagated++;
            }
        }

        System.out.println("  Propagated light to " + propagated + " blocks from " + startX + "," + startY + "," + startZ);
    }


    public boolean inBounds(int x, int y, int z) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT && z >= 0 && z < DEPTH;
    }

    public Mesh getSolidMesh() { return solidMesh; }
    public Mesh getWaterMesh() { return waterMesh; }
    public Mesh getBillboardMesh() { return billboardMesh; }

    public void invalidateMeshes() {
        solidMesh = null;
        waterMesh = null;
        billboardMesh = null;
    }


    // Calculate lighting for a face by sampling the neighbor's light OR current block
    private float calculateFaceLighting(int x, int y, int z, int[] offset) {
        int nx = x + offset[0];
        int ny = y + offset[1];
        int nz = z + offset[2];

        int neighborLight = 0;
        int currentLight = blockLight[x][y][z] & 0xFF; // Sample current block's light

        // If neighbor is in bounds, sample directly from chunk
        if (inBounds(nx, ny, nz)) {
            neighborLight = blockLight[nx][ny][nz] & 0xFF;
        } else {
            // Neighbor is out of chunk bounds, query world
            int worldX = (int)posX + x + offset[0];
            int worldY = y + offset[1];
            int worldZ = (int)posZ + z + offset[2];

            if (world != null) {
                neighborLight = world.getBlockLightAt(worldX, worldY, worldZ) & 0xFF;
            }
        }

        // Take the max of current block and neighbor
        int lightLevel = Math.max(currentLight, neighborLight);

        // Normalize to 0.0-1.0, no ambient
        return lightLevel / 15.0f;
    }

    public byte getSkyLight(int x, int y, int z) {
        if (!inBounds(x, y, z)) return 0;
        return skyLight[x][y][z];
    }

    public void setSkyLight(int x, int y, int z, byte level) {
        if (!inBounds(x, y, z)) return;
        skyLight[x][y][z] = level;
    }

    public byte getBlockLight(int x, int y, int z) {
        if (!inBounds(x, y, z)) return 0;
        return blockLight[x][y][z];
    }

    public void setBlockLight(int x, int y, int z, byte level) {
        if (!inBounds(x, y, z)) return;
        blockLight[x][y][z] = level;
    }

    // -------------------------------------------------
    // SOLID MESH
    // -------------------------------------------------
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
                    if (block == null) continue;

                    if (block.type != BlockType.SOLID) continue;

                    float[][][] faces = new float[][][]{
                            {{x,y,z+1},{x+1,y,z+1},{x+1,y+1,z+1},{x,y+1,z+1}},
                            {{x+1,y,z},{x,y,z},{x,y+1,z},{x+1,y+1,z}},
                            {{x,y+1,z+1},{x+1,y+1,z+1},{x+1,y+1,z},{x,y+1,z}},
                            {{x,y,z},{x+1,y,z},{x+1,y,z+1},{x,y,z+1}},
                            {{x+1,y,z+1},{x+1,y,z},{x+1,y+1,z},{x+1,y+1,z+1}},
                            {{x,y,z},{x,y,z+1},{x,y+1,z+1},{x,y+1,z}}
                    };

                    int[] texIndices = {
                            block.tex_sides,
                            block.tex_sides,
                            block.tex_top,
                            block.tex_bottom,
                            block.tex_sides,
                            block.tex_sides
                    };

                    int[][] neighborOffsets = {
                            {0,0,1},{0,0,-1},{0,1,0},{0,-1,0},{1,0,0},{-1,0,0}
                    };

                    int globalX = chunkX * WIDTH + x;
                    int globalZ = chunkZ * DEPTH + z;

                    for (int f = 0; f < 6; f++) {

                        int ngx = globalX + neighborOffsets[f][0];
                        int ngy = y + neighborOffsets[f][1];
                        int ngz = globalZ + neighborOffsets[f][2];

                        int neighborId = world.getBlockAt(ngx, ngy, ngz);

                        boolean neighborEmpty = true;

                        if (neighborId != 0) {
                            Block n = BlockManager.Get().GetBlock(neighborId);
                            if (n != null && n.type == BlockType.SOLID)
                                neighborEmpty = false;
                        }

                        if (neighborEmpty) {
                            addFace(verts, idx, faces[f], texIndices[f], atlasSize, texSize, vertCount, x, y, z, neighborOffsets[f]);
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
                new BufferLayoutElement(GL_FLOAT,2,false),
                new BufferLayoutElement(GL_FLOAT,1,false)
        };

        VertexArray vao = new VertexArray(array, idxArray, layout);
        vao.getPosition().set(posX,posY,posZ);

        solidMesh = new Mesh(vao, atlasTexture);
    }

    private void addFace(List<Float> vertices, List<Integer> indices,
                         float[][] positions, int texIndex,
                         int atlasSize, float texSize,
                         int vertexOffset, int x, int y, int z, int[] faceOffset) {

        float[] uvs = AtlasUtils.getUVs(texIndex, atlasSize, texSize);

        // Sample light from the neighboring block (where the face points)
        float light = calculateFaceLighting(x, y, z, faceOffset);

        for (int i = 0; i < 4; i++) {

            vertices.add(positions[i][0]);
            vertices.add(positions[i][1]);
            vertices.add(positions[i][2]);

            vertices.add(uvs[i*2]);
            vertices.add(uvs[i*2+1]);

            vertices.add(light);
        }

        indices.add(vertexOffset);
        indices.add(vertexOffset+1);
        indices.add(vertexOffset+2);

        indices.add(vertexOffset);
        indices.add(vertexOffset+2);
        indices.add(vertexOffset+3);
    }

    // -------------------------------------------------
    // WATER MESH
    // -------------------------------------------------
    public void generateWaterMesh(Texture atlasTexture, World world, int chunkX, int chunkZ) {

        List<Float> verts = new ArrayList<>();
        List<Integer> idx = new ArrayList<>();
        int vertCount=0;

        int atlasSize = AtlasUtils.atlasSize(atlasTexture);
        float texSize = 1f/atlasSize;

        for(int x=0;x<WIDTH;x++)
            for(int z=0;z<DEPTH;z++)
                for(int y=0;y<HEIGHT;y++){

                    int id = blocks[x][y][z];
                    Block b = BlockManager.Get().GetBlock(id);

                    if(b==null || b.type!=BlockType.LIQUID) continue;

                    int globalX = chunkX*WIDTH + x;
                    int globalZ = chunkZ*DEPTH + z;

                    int aboveId = world.getBlockAt(globalX,y+1,globalZ);

                    if(aboveId!=0) continue;

                    float[][] face = {
                            {x,y+1,z+1},
                            {x+1,y+1,z+1},
                            {x+1,y+1,z},
                            {x,y+1,z}
                    };

                    addFace(verts,idx,face,b.tex_top,atlasSize,texSize,vertCount,x,y,z,new int[]{0,1,0});

                    vertCount+=4;
                }

        float[] array = new float[verts.size()];
        for(int i=0;i<verts.size();i++) array[i]=verts.get(i);

        int[] idxArray = new int[idx.size()];
        for(int i=0;i<idx.size();i++) idxArray[i]=idx.get(i);

        BufferLayoutElement[] layout = {
                new BufferLayoutElement(GL_FLOAT,3,false),
                new BufferLayoutElement(GL_FLOAT,2,false),
                new BufferLayoutElement(GL_FLOAT,1,false)
        };

        VertexArray vao = new VertexArray(array,idxArray,layout);
        vao.getPosition().set(posX,posY,posZ);

        waterMesh = new Mesh(vao,atlasTexture);
    }

    // -------------------------------------------------
    // BILLBOARD MESH
    // -------------------------------------------------
    public void generateBillboardMesh(Texture atlasTexture, World world, int chunkX, int chunkZ){

        List<Float> verts = new ArrayList<>();
        List<Integer> idx = new ArrayList<>();
        int vertCount=0;

        int atlasSize = AtlasUtils.atlasSize(atlasTexture);
        float texSize = 1f/atlasSize;

        for(int x=0;x<WIDTH;x++)
            for(int z=0;z<DEPTH;z++)
                for(int y=0;y<HEIGHT;y++){

                    int id = blocks[x][y][z];
                    Block b = BlockManager.Get().GetBlock(id);

                    if(b==null || b.type!=BlockType.PLANT) continue;

                    float[][] quad1 = {
                            {x,y,z},
                            {x+1,y,z+1},
                            {x+1,y+1,z+1},
                            {x,y+1,z}
                    };

                    float[][] quad2 = {
                            {x+1,y,z},
                            {x,y,z+1},
                            {x,y+1,z+1},
                            {x+1,y+1,z}
                    };

                    addFace(verts,idx,quad1,b.tex_top,atlasSize,texSize,vertCount,x,y,z,new int[]{0,0,0});
                    vertCount+=4;

                    addFace(verts,idx,quad2,b.tex_top,atlasSize,texSize,vertCount,x,y,z,new int[]{0,0,0});
                    vertCount+=4;
                }

        float[] array = new float[verts.size()];
        for(int i=0;i<verts.size();i++) array[i]=verts.get(i);

        int[] idxArray = new int[idx.size()];
        for(int i=0;i<idx.size();i++) idxArray[i]=idx.get(i);

        BufferLayoutElement[] layout = {
                new BufferLayoutElement(GL_FLOAT,3,false),
                new BufferLayoutElement(GL_FLOAT,2,false),
                new BufferLayoutElement(GL_FLOAT,1,false)
        };

        VertexArray vao = new VertexArray(array,idxArray,layout);
        vao.getPosition().set(posX,posY,posZ);

        billboardMesh = new Mesh(vao,atlasTexture);
    }

}