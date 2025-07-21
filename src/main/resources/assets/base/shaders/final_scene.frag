#version 460

uniform ivec2 res;

layout(binding = 6) uniform sampler2D scene_image;
in vec4 gl_FragCoord;

out vec4 fragColor;

void main() {
    fragColor = vec4(texture(scene_image, vec2(gl_FragCoord.xy/res)));
}