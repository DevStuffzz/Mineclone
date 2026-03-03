package com.coreybeaver.mineclone.game.world;

import java.util.HashMap;
import java.util.Map;

public class BlockManager {
    private Map<Integer, Block> blocks;
    private static BlockManager instance = null;
    private int numBlocks = 0;

    private BlockManager() {
        blocks = new HashMap<>();
    }

    public void AddBlock(BlockType type, int tex_top, int tex_sides, int tex_bottom, String name) {
        blocks.put(numBlocks, new Block(numBlocks, type, tex_top, tex_sides, tex_bottom, name));

        numBlocks ++;
    }

    public static BlockManager Get() {
        if(instance == null) {
            instance = new BlockManager();
        }

        return instance;
    }

    public Block GetBlock(int id) {
        return blocks.get(id);
    }

    /**
     * Find the numeric ID assigned to a block with the given name.  Returns
     * -1 if no such block exists.  This is handy for generators that need to
     * refer to a block by its semantic name rather than hard‑coding an index.
     */
    public int getIdByName(String name) {
        for (var entry : blocks.entrySet()) {
            Block b = entry.getValue();
            if (b != null && b.name != null && b.name.equals(name)) {
                return entry.getKey();
            }
        }
        return -1;
    }
}
