#type vertex
#version 330 core
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec2 aTexCoord;
layout(location = 2) in float aBlockLight;  // Block light value
layout(location = 3) in float aSkyLight;    // Sky light value

out vec2 v_TexCoord;
out float v_BlockLight;
out float v_SkyLight;

uniform mat4 u_MVP;

void main()
{
    gl_Position = u_MVP * vec4(aPos, 1.0);
    v_TexCoord = aTexCoord;
    v_BlockLight = aBlockLight;
    v_SkyLight = aSkyLight;
}

#type fragment
#version 330 core

in vec2 v_TexCoord;
in float v_BlockLight;
in float v_SkyLight;
out vec4 FragColor;

uniform sampler2D u_Texture;

void main()
{
    vec4 color = texture(u_Texture, v_TexCoord);

    // Alpha cutout (Minecraft-style transparency)
    if (color.a < 0.5)
        discard;

    // Mix block light and sky light - take the maximum
    float combinedLight = max(v_BlockLight, v_SkyLight);

    // Apply minimum ambient to prevent completely black areas
    float minAmbient = 0.1;
    float lighting = max(combinedLight, minAmbient);

    FragColor = vec4(color.rgb * lighting, color.a);
}