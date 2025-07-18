#version 460

uniform vec2 res;
uniform bool ui;
in vec4 gl_FragCoord;

out vec4 fragColor;

layout(std430, binding = 7) buffer imageSSBO
{
    vec3[] imageData;
};

void main()
{
    vec2 uv = (vec2(gl_FragCoord)*2. - res) / res.y;
    if (ui && uv.x >= -0.004f && uv.x <= 0.004f && uv.y >= -0.004385f && uv.y <= 0.004385f) {
        fragColor = vec4(0.9f, 0.9f, 1, 1);
    } else {
        ivec2 pixelPos = ivec2(gl_FragCoord.xy*res.xy);
        fragColor = vec4(imageData[int(pixelPos.x*res.y)+pixelPos.y], 1);
    }
}