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
            for (int v = 0; v < regionVoxelsData.length(); v += 4) {
                float x = rayPos.x-regionVoxelsData[v];
                float y = rayPos.y-regionVoxelsData[v+1];
                float z = rayPos.z-regionVoxelsData[v+2];
                if (int(x) == 0 && x >= 0 && int(y) == 0 && y >= 0 && int(z) == 0 && z >= 0) {
                    float voxelInfo = regionVoxelsData[v+3];
                    int voxelType = int(voxelInfo);
                    int voxelSubtype = int((voxelInfo-voxelType)*10000);
                    vec4 voxelColor = texture(atlas, vec2(((x+voxelType)/1248f), ((int((y-1)*-8)+z+(voxelSubtype*8))/1248f)));
                    color = vec3(voxelColor);
                    alpha += voxelColor.a;
                    if (alpha >= 1) {
                        i = 1000;
                        break;
                    }
                }
            }

            traveled += 0.02;
        }

        fragColor = vec4(color, 1);
        //fragColor = texture(atlas, vec2(0, 48f/16000f));
    }
}