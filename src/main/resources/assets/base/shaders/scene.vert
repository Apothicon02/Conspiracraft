#version 460

layout (location = 0) in vec3 position;

uniform bool raytrace;
uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

void main()
{
    if (raytrace) {
        gl_Position = vec4(position, 1.0);
    } else {
        gl_Position = projection * view * model * vec4(position, 1.0f);
    }
}