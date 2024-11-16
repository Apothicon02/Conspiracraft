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
        int lod1Size = size/4;

        ivec3 lod1MapPos = ivec3(floor((camPos/4) + 0.));
        vec3 lod1DeltaDist = abs(vec3(length(dir)) / dir);
        ivec3 lod1RayStep = ivec3(sign(dir));
        vec3 lod1SideDist = (sign(dir) * (vec3(lod1MapPos) - (camPos/4)) + (sign(dir) * 0.5) + 0.5) * lod1DeltaDist;
        bvec3 lod1Mask;

        int maxAttempts = renderDistance;
        for (int i = 0; i <= maxAttempts; i++) {
            if (i == maxAttempts || lod1MapPos.x < 0 || lod1MapPos.x >= lod1Size || lod1MapPos.y < 0 || lod1MapPos.y >= lod1Size || lod1MapPos.z < 0 || lod1MapPos.z >= lod1Size) {
                float whiteness = abs(((lod1MapPos.y*4)/(lod1Size*8f))-1f)/4f;
                fragColor = vec4(max(tint.r, 0.63+(0.37*whiteness)), max(tint.g, 0.75+(0.25*whiteness)), max(tint.b, 1), 1);
                i = 9999999;
            } else {
                if (lod1Data[lod1MapPos.x + lod1MapPos.y * lod1Size + lod1MapPos.z * lod1Size * lod1Size] == 1) { //only check block grid if lod is full
                    ivec3 origin = lod1MapPos*4;
                    ivec3 mapPos = ivec3(floor((origin) + 0.));
                    vec3 deltaDist = abs(vec3(length(dir)) / dir);
                    ivec3 rayStep = ivec3(sign(dir));
                    vec3 sideDist = (sign(dir) * (vec3(mapPos) - (origin)) + (sign(dir) * 0.5) + 0.5) * deltaDist;
                    bvec3 mask;

                    for (int e = 0; e <= 4; e++) {
                        int blockInfo = region1BlockData[mapPos.x + mapPos.y * size + mapPos.z * size * size];
                        int blockType = (blockInfo >> 16) & 0xFFFF;
                        if (blockType != 0f) {
                            fragColor = vec4(0, 1, 0, 1);
                            i = 9999999;
                            break;
//                            int blockSubtype = blockInfo & 0xFFFF;
//                            int colorData = atlasData[(9984*(blockType*8)) + (blockSubtype*64)];
//                            vec4 blockColor = vec4(0xFF & colorData >> 16, 0xFF & colorData >> 8, 0xFF & colorData, 0xFF & colorData >> 24)/255;
//                            if (blockColor.a < 1f && tint.a < 1f) {
//                                tint = vec4(max(tint.r, blockColor.r), max(tint.g, blockColor.g), max(tint.b, blockColor.b), min(1f, (tint.a+blockColor.a)/2));
//                            } else {
//                                fragColor = vec4(vec3(mix(blockColor, tint, tint.a)), 1);
//                                i = 9999999;
//                                break;
//                            }
                        };
                        mask = lessThanEqual(sideDist.xyz, min(sideDist.yzx, sideDist.zxy));
                        sideDist += vec3(mask) * deltaDist;
                        mapPos += ivec3(vec3(mask)) * rayStep;
                    }

                }
                lod1Mask = lessThanEqual(lod1SideDist.xyz, min(lod1SideDist.yzx, lod1SideDist.zxy));
                lod1SideDist += vec3(lod1Mask) * lod1DeltaDist;
                lod1MapPos += ivec3(vec3(lod1Mask)) * lod1RayStep;
            }
        }
    } else {
        fragColor = vec4(0, 0, 0, 1);
    }
}