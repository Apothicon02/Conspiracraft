#version 460

uniform vec2 res;
uniform vec3 camPos;
layout(binding = 0) uniform sampler2D atlas;
layout(std430, binding = 0) buffer regionVoxels
{
    float regionVoxelsData[];
};
in vec2 gl_FragCoord;
in vec4 pos;

out vec4 fragColor;

void main()
{
    vec2 uv = (gl_FragCoord*2. - res.xy) / res.y;
    if (uv.x >= -0.004 && uv.x <= 0.004 && uv.y >= -0.004385 && uv.y <= 0.004385) {
        fragColor = vec4(0.9, 0.9, 1, 1);
    } else {
        vec3 dir = normalize(vec3(uv, 1));
        vec3 color = vec3(0, 0, 0);
        float alpha = 0;
        float traveled = 0;
        for (int i = 0; i < 1000; i++) {
            vec3 rayPos = camPos + (dir * traveled);
            for (int v = 0; v <= regionVoxelsData.length(); v += 4) {
                if (int(rayPos.x) == regionVoxelsData[v] && int(rayPos.y) == regionVoxelsData[v+1] && int(rayPos.z) == regionVoxelsData[v+2]) {
                    float voxelInfo = regionVoxelsData[v+3];
                    int voxelType = int(voxelInfo);
                    int voxelSubtype = int(voxelInfo-voxelType)*10000;
                    vec4 voxelColor = texture(atlas, vec2(((voxelType*8)+1)/32000f, ((voxelSubtype*64)+1)/16000f)); //offset based on ray decimal
                    color = vec3(voxelColor);
                    alpha += voxelColor.a;
                    if (alpha >= 1) {
                        i = 1000;
                        break;
                    }
                }
            }

            traveled += 0.1;
        }

        fragColor = vec4(color, 1);
        //fragColor = texture(atlas, vec2(0, 48f/16000f));
    }
}