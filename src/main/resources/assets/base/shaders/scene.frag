#version 460

uniform vec2 res;
uniform mat4 cam;
layout(binding = 0) uniform sampler2D atlas;
layout(std430, binding = 0) buffer regionVoxels
{
    float[] regionVoxelsData;
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
        vec3 camPos = vec3(cam[3]);
        vec3 dir = normalize(vec3(uv, 1));
        vec4 color = vec4(0, 0, 0, 0);
        vec3 prevVoxelPos = vec3(-100000, -100000, -100000);
        vec4 tint = vec4(0, 0, 0, 0);
        int size = 811;
        for (float traveled = 0f; traveled < size;) {
            int coordinateScale = 1;
            vec3 rayPos = vec3(cam * vec4((dir * traveled)+camPos, 1));
            if (rayPos.y < 1) {
                color = vec4(max(color.r, 0.73), max(color.g, 0.75), max(color.b, 0.8), 1);
                break;
            } else {
                int gridX = int(rayPos.x);
                int gridY = int(rayPos.y);
                int gridZ = int(rayPos.z);
                if (gridX > 0 && gridX <= size && gridY > 0 && gridY < 32 && gridZ > 0 && gridZ <= size && traveled < size-2) {
                    float voxelInfo = regionVoxelsData[gridX + gridY * size + gridZ * size*size];
                    if (voxelInfo != 0f) {
                        coordinateScale = 8;
                        int voxelType = int(voxelInfo);
                        int voxelSubtype = int((voxelInfo-voxelType)*1000);
                        color = texture(atlas, vec2((((rayPos.x-gridX)+voxelType)/1248f), ((int(((rayPos.y-gridY)-1)*-8)+(rayPos.z-gridZ)+(voxelSubtype*8))/1248f)));
                        if (color.a < 1f && tint.a < 1f) {
                            tint = vec4(max(tint.r, color.r), max(tint.g, color.g), max(tint.b, color.b), min(1f, (tint.a+color.a)/2));
                        }
                    }
                } else {
                    color = vec4(max(color.r, 0.63), max(color.g, 0.75), max(color.b, 1), 1);
                    break;
                }
                if (color.a >= 1) {
                    color = vec4(vec3(mix(color, tint, tint.a)), 1);
                    break;
                } else { //march forward until entering another scaled coordinate.
                    prevVoxelPos = vec3(int(rayPos.x*coordinateScale), int(rayPos.y*coordinateScale), int(rayPos.z*coordinateScale));
                    for (float march = 0.003f; march <= 15; march += 0.003f) {
                        traveled += march;
                        rayPos = vec3(cam * vec4((dir * traveled)+camPos, 1));
                        if (prevVoxelPos != vec3(int(rayPos.x*coordinateScale), int(rayPos.y*coordinateScale), int(rayPos.z*coordinateScale))) {
                            break;
                        }
                    }
                }
            }
        }

        fragColor = color;
    }
}