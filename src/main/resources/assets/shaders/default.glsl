#type vertex
#version 330 core
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec2 aTexCoord;
layout(location = 2) in float aLight;  // Block light value

out vec2 v_TexCoord;
out float v_Light;

uniform mat4 u_MVP;

void main()
{
    gl_Position = u_MVP * vec4(aPos, 1.0);
    v_TexCoord = aTexCoord;
    v_Light = aLight;
}

#type fragment
#version 330 core

in vec2 v_TexCoord;
in float v_Light;
out vec4 FragColor;

uniform sampler2D u_Texture;

void main()
{
    vec4 color = texture(u_Texture, v_TexCoord);

    // Alpha cutout (Minecraft-style transparency)
    if (color.a < 0.5)
        discard;

    // Apply block lighting (clamped to minimum ambient)
    float minAmbient = 0.1;  // Prevent completely black areas
    float lighting = max(v_Light, minAmbient);

    FragColor = vec4(color.rgb * lighting, color.a);
}