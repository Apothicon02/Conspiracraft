#version 460

layout(binding = 0) uniform sampler2D atlas;
layout(std430, binding = 0) buffer subChunkBuffer
{
    int subChunkBufferData[];
};
in vec4 pos;

out vec4 fragColor;

void main()
{
    fragColor = texture(atlas, vec2(0, 48f/16000f));
}