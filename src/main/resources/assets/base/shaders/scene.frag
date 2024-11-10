#version 460

uniform vec2 res;
uniform mat4 cam;
layout(binding = 0) uniform sampler2D atlas;
layout(std430, binding = 0) buffer region1
{
    uint[] region1BlockData;
};
layout(std430, binding = 1) buffer lod1
{
    uint[] lod1Data;
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
        vec4 tint = vec4(0, 0, 0, 0);
        int size = 808;

        ivec3 mapPos = ivec3(floor((camPos) + 0.));
        vec3 deltaDist = abs(vec3(length(dir)) / dir);
        ivec3 rayStep = ivec3(sign(dir));
        vec3 sideDist = (sign(dir) * (vec3(mapPos) - (camPos)) + (sign(dir) * 0.5) + 0.5) * deltaDist;
        bvec3 mask;

        for (float traveled = 0; traveled <= (size*(size/100));) {
            float coordinateScale = 1;
            int voxelX = mapPos.x/8;
            int voxelY = mapPos.y/8;
            int voxelZ = mapPos.z/8;
            if (voxelX <= 0 || voxelX > size || voxelY <= 0 || voxelY > size || voxelZ <= 0 || voxelZ > size || traveled >= (size*5)-1) {
                color = vec4(max(color.r, 0.63), max(color.g, 0.75), max(color.b, 1), 1);
                break;
            } else {
                uint voxelInfo = region1BlockData[voxelX + voxelY * size + voxelZ * size * size];
                if (voxelInfo != 0f) {
                    color = texture(atlas, vec2(((voxelInfo)/1248f)+(((mapPos.x-(int(mapPos.x/8)*8))+1)/9984f), ((((mapPos.y-(int(mapPos.y/8)*8))-8)*-8) + (mapPos.z-(int((mapPos.z/8)+1)*8)))/9984f));
                    if (color.a < 1f && tint.a < 1f) {
                        tint = vec4(max(tint.r, color.r), max(tint.g, color.g), max(tint.b, color.b), min(1f, (tint.a+color.a)/2));
                    }
                };
                if (color.a >= 1) {
                    color = vec4(vec3(mix(color, tint, tint.a)), 1);
                    break;
                }
                mask = lessThanEqual(sideDist.xyz, min(sideDist.yzx, sideDist.zxy));
                sideDist += vec3(mask) * deltaDist;
                mapPos += ivec3(vec3(mask)) * rayStep;
                traveled = distance(vec2(camPos.x, camPos.z), vec2(mapPos.x, mapPos.z));
            }
        }

        fragColor = color;
    }
}