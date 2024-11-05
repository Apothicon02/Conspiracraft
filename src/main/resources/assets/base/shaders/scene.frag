#version 460

layout(std430, binding = 0) buffer subChunkBuffer
{
    int subChunkBufferData[];
};
in vec4 pos;

uniform sampler2D tex;

out vec4 fragColor;

void main()
{
    fragColor = texture(tex, vec2(0, 0));
}