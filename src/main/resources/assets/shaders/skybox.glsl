#type vertex
#version 330 core
layout(location = 0) in vec3 a_Position;
uniform mat4 u_MVP;
out vec3 v_WorldDir;

void main()
{
    v_WorldDir = a_Position;
    vec3 pos = a_Position * 100.0; // scale cube large
    gl_Position = u_MVP * vec4(pos, 1.0);
}

#type fragment
#version 330 core

in vec3 v_WorldDir;

out vec4 FragColor;

uniform vec3 u_SunDirection; // Optional sun direction
uniform float u_Time;         // Optional for animated sky

vec3 getSkyColor(vec3 dir) {
    // Normalize the direction
    vec3 nDir = normalize(dir);

    // Simple gradient: horizon = light blue, zenith = dark blue
    float t = nDir.y * 0.5 + 0.5; // from 0 at bottom to 1 at top
    vec3 horizon = vec3(0.6, 0.8, 1.0);
    vec3 zenith  = vec3(0.05, 0.1, 0.3);

    vec3 sky = mix(horizon, zenith, t);

    // Optional sun glow
    if (u_SunDirection != vec3(0)) {
        float sunIntensity = max(dot(nDir, normalize(u_SunDirection)), 0.0);
        sky += vec3(1.0, 0.9, 0.6) * pow(sunIntensity, 128.0);
    }

    return sky;
}

void main()
{
    FragColor = vec4(getSkyColor(v_WorldDir), 1.0);
}