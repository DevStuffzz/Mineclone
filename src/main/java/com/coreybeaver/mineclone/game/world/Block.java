package com.coreybeaver.mineclone.game.world;

public class Block {
    int id;
    BlockType type;
    int tex_top, tex_sides, tex_bottom;

    boolean light = false;

    String name;

    public Block(int id, BlockType type, int texTop, int texSides, int texBottom, String name) {
        this.id = id;
        this.type = type;
        this.tex_top = texTop;
        this.tex_sides = texSides;
        this.tex_bottom = texBottom;
        this.name = name;
    }

    public Block(int id, BlockType type, int texTop, int texSides, int texBottom, String name, boolean light) {
        this.id = id;
        this.type = type;
        this.tex_top = texTop;
        this.tex_sides = texSides;
        this.tex_bottom = texBottom;
        this.name = name;
        this.light = light;
    }

}
