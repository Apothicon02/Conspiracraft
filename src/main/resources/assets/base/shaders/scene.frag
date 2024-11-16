#version 460

uniform int renderDistance;
uniform vec2 res;
uniform mat4 cam;
layout(std430, binding = 0) buffer atlas
{
    int[] atlasData;
};
layout(std430, binding = 1) buffer region1
{
    int[] region1BlockData;
};
layout(std430, binding = 2) buffer lod1
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
    } else if (uv.x >= -1.87 && uv.x <= 1.87 && uv.y >= -1 && uv.y <= 1) {
        vec3 camPos = vec3(cam[3]);
        vec3 dir = vec3(cam*vec4(normalize(vec3(uv, 1)), 0));
        vec4 color = vec4(0, 0, 0, 0);
        vec4 tint = vec4(0, 0, 0, 0);
        int size = 808;

        ivec3 mapPos = ivec3(floor((camPos) + 0.));
        vec3 deltaDist = abs(vec3(length(dir)) / dir);
        ivec3 rayStep = ivec3(sign(dir));
        vec3 sideDist = (sign(dir) * (vec3(mapPos) - (camPos)) + (sign(dir) * 0.5) + 0.5) * deltaDist;
        bvec3 mask;

        while (true == true) {
            int blockX = mapPos.x;
            int blockY = mapPos.y;
            int blockZ = mapPos.z;
            if (blockX <= 0 || blockX > size || blockY <= 0 || blockY > size || blockZ <= 0 || blockZ > size || distance(mapPos, camPos) > renderDistance) {
                float whiteness = abs((mapPos.y/(size*8f))-1f)/4f;
                color = vec4(max(color.r, 0.63+(0.37*whiteness)), max(color.g, 0.75+(0.25*whiteness)), max(color.b, 1), 1);
                break;
            } else {
                int blockInfo = region1BlockData[blockX + blockY * size + blockZ * size * size];
                int blockType = (blockInfo >> 16) & 0xFFFF;
                if (blockType != 0f) {
                    int blockSubtype = blockInfo & 0xFFFF;

                    ivec3 origin = ivec3(0, 0, 0); //add the position the ray entered the block to this
                    ivec3 voxelMapPos = ivec3(floor((origin) + 0.));
                    vec3 voxelDeltaDist = abs(vec3(length(dir)) / dir);
                    ivec3 voxelRayStep = ivec3(sign(dir));
                    vec3 voxelSideDist = (sign(dir) * (vec3(voxelMapPos) - (origin)) + (sign(dir) * 0.5) + 0.5) * voxelDeltaDist;
                    bvec3 voxelMask;
                    while (voxelMapPos.x >= 0 && voxelMapPos.x < 8 && voxelMapPos.y >= 0 && voxelMapPos.y < 8 && voxelMapPos.z >= 0 && voxelMapPos.z < 8) {
                        int colorData = atlasData[(9984*((blockType*8)+voxelMapPos.x)) + (blockSubtype*64) + voxelMapPos.y + voxelMapPos.z];
                        color = vec4(0xFF & colorData >> 16, 0xFF & colorData >> 8, 0xFF & colorData, 0xFF & colorData >> 24)/255;
                        if (color.a < 1f && tint.a < 1f) {
                            tint = vec4(max(tint.r, color.r), max(tint.g, color.g), max(tint.b, color.b), min(1f, (tint.a+color.a)/2));
                        } else if (color.a >= 1) {
                            color = vec4(vec3(mix(color, tint, tint.a)), 1);
                            break;
                        }
                        mask = lessThanEqual(voxelSideDist.xyz, min(voxelSideDist.yzx, voxelSideDist.zxy));
                        voxelSideDist += vec3(mask) * voxelDeltaDist;
                        voxelMapPos += ivec3(vec3(mask)) * voxelRayStep;
                    }
                };
                mask = lessThanEqual(sideDist.xyz, min(sideDist.yzx, sideDist.zxy));
                sideDist += vec3(mask) * deltaDist;
                mapPos += ivec3(vec3(mask)) * rayStep;
            }
        }

        fragColor = color;
    } else {
        fragColor = vec4(0, 0, 0, 1);
    }
}