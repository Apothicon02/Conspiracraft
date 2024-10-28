#version 460

layout(std430, binding = 0) buffer subChunkBuffer
{
    int subChunkBufferData[];
};
in vec4 pos;

out vec4 fragColor;

void main()
{
    if (subChunkBufferData[0] == 0) {
        fragColor = vec4(255, 0, 0, 1);
    } else {
        fragColor = vec4(0, 255, 0, 1);
    }
}