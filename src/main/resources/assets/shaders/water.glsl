#type vertex
#version 330 core
layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec2 a_TexCoord;
out vec2 v_TexCoord;

uniform mat4 u_MVP;

void main() {
    v_TexCoord = a_TexCoord;
    gl_Position = u_MVP * vec4(a_Position, 1.0);
}

#type fragment
#version 330 core
in vec2 v_TexCoord;
out vec4 FragColor;

uniform sampler2D u_Texture;

void main() {
    vec4 col = texture(u_Texture, v_TexCoord);
    // subtle blue tint and slight transparency for water
    FragColor = col * vec4(0.5, 0.7, 1.0, 0.8);
}