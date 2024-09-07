#version 330

in vec2 outTextCoord;

out vec4 fragColor;

struct Material
{
    vec4 diffuse;
};

uniform sampler2D txtSampler;
uniform Material material;
uniform mat4 sunMatrix;
uniform mat4 modelMatrix;

void main()
{
    fragColor = texture(txtSampler, outTextCoord) + material.diffuse;
    vec2 sunPos = sunMatrix[3].xz;
    vec2 modelPos = modelMatrix[3].xz;
    vec2 distVec = modelPos-sunPos;
    float dist = dot(distVec, distVec)/250000;
    fragColor.rgb -= min(0.6, dist);
}