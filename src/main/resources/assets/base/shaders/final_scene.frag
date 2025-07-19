#version 460

uniform ivec2 res;
in vec4 gl_FragCoord;

out vec4 fragColor;

layout(binding = 0) uniform sampler2D scene_image;

void main()
{
    fragColor = texture(scene_image, vec2(gl_FragCoord.xy/res));
}