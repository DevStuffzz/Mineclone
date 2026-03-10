#type vertex
#version 330 core

layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec2 a_TexCoord;
layout(location = 2) in float aBlockLight;
layout(location = 3) in float aSkyLight;

out vec2 v_TexCoord;
out float v_BlockLight;
out float v_SkyLight;

uniform mat4 u_MVP;
uniform float u_Time;

void main()
{
    float speed = 2.0;        // how fast it switches
    float tileSize = 0.0625;  // one tile in a 16x16 atlas

    int frame = int(floor(u_Time * speed)) % 2;

    float offsetX = frame * tileSize;

    v_TexCoord = a_TexCoord + vec2(offsetX, 0.0);
    v_BlockLight = aBlockLight;
    v_SkyLight = aSkyLight;

    gl_Position = u_MVP * vec4(a_Position, 1.0);
}

#type fragment
#version 330 core
in vec2 v_TexCoord;
in float v_BlockLight;
in float v_SkyLight;
out vec4 FragColor;

uniform sampler2D u_Texture;

void main() {
    vec4 col = texture(u_Texture, v_TexCoord);

    // Mix block light and sky light - take the maximum
    float combinedLight = max(v_BlockLight, v_SkyLight);
    float minAmbient = 0.1;
    float lighting = max(combinedLight, minAmbient);

    // Apply lighting
    FragColor = vec4(col.rgb * lighting, col.a);
}