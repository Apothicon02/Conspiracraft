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
layout(std430, binding = 2) buffer region2
{
    int[] region2BlockData;
};
layout(std430, binding = 3) buffer region3
{
    int[] region3BlockData;
};
layout(std430, binding = 4) buffer region4
{
    int[] region4BlockData;
};
layout(std430, binding = 5) buffer lod1
{
    uint[] lod1Data;
};
in vec2 gl_FragCoord;
in vec4 pos;

out vec4 fragColor;

vec3 stepMask(vec3 sideDist) {
    bvec3 mask;
    bvec3 b1 = lessThan(sideDist.xyz, sideDist.yzx);
    bvec3 b2 = lessThanEqual(sideDist.xyz, sideDist.zxy);
    mask.z = b1.z && b2.z;
    mask.x = b1.x && b2.x;
    mask.y = b1.y && b2.y;
    if(!any(mask)) // Thank you Spalmer
    mask.z = true;

    return vec3(mask);
}

vec4 tint = vec4(0);

vec4 traceBlock(vec3 rayPos, vec3 rayDir, vec3 iMask, int blockType, int blockSubtype) {
    rayPos = clamp(rayPos, vec3(0.0001), vec3(7.9999));
    vec3 mapPos = floor(rayPos);
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((mapPos - rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = iMask;

    while (mapPos.x <= 7.0 && mapPos.x >= 0.0 && mapPos.y <= 7.0 && mapPos.y >= 0.0 && mapPos.z <= 7.0 && mapPos.z >= 0.0) {
        int colorData = atlasData[(9984*((blockType*8)+int(mapPos.x))) + (blockSubtype*64) + ((abs(int(mapPos.y)-8)-1)*8) + int(mapPos.z)];
        vec4 voxelColor = vec4(0xFF & colorData >> 16, 0xFF & colorData >> 8, 0xFF & colorData, 0xFF & colorData >> 24)/255;
        if (voxelColor.a >= 1) {
            return voxelColor;
        } else {
            tint = vec4(max(voxelColor.r, tint.r), max(voxelColor.g, tint.g), max(voxelColor.b, tint.b), max(voxelColor.a, tint.a));
        }

        mask = stepMask(sideDist);
        mapPos += mask * raySign;
        sideDist += mask * raySign * deltaDist;
    }

    return vec4(0.0);
}

int size = 808;
vec3 rayMapPos = vec3(0);

vec4 traceWorld(vec3 rayPos, vec3 rayDir) {

    rayMapPos = floor(rayPos);
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((rayMapPos-rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = stepMask(sideDist);

    for (int i = 0; i < renderDistance; i++) {
        if (rayMapPos.x <= 0 || rayMapPos.x > size || rayMapPos.y < 0 || rayMapPos.y > 50 || rayMapPos.z < 0 || rayMapPos.z > size) { //cull rays that go out of bounds
            break;
        }
        int blockInfo = region1BlockData[int(rayMapPos.x) + int(rayMapPos.y) * size + int(rayMapPos.z) * size * size];
        int blockType = (blockInfo >> 16) & 0xFFFF;
        if (blockType != 0f) {
            int blockSubtype = blockInfo & 0xFFFF;

            vec3 mini = ((rayMapPos-rayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
            float d = max (mini.x, max (mini.y, mini.z));
            vec3 intersect = rayPos + rayDir*d;
            vec3 uv3d = intersect - rayMapPos;

            if (rayMapPos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
                uv3d = rayPos - rayMapPos;
            }

            vec4 color = traceBlock(uv3d * 8.0, rayDir, mask, blockType, blockSubtype);
            if (color.a >= 1) {
                return vec4(vec3(mix(color, tint, tint.a)), 1);
            }
        }

        mask = stepMask(sideDist);
        rayMapPos += mask * raySign;
        sideDist += mask * raySign * deltaDist;
    }

    return vec4(0.0);
}


void main()
{
    vec2 uv = (gl_FragCoord*2. - res.xy) / res.y;
    if (uv.x >= -0.004 && uv.x <= 0.004 && uv.y >= -0.004385 && uv.y <= 0.004385) {
        fragColor = vec4(0.9, 0.9, 1, 1);
    } else if (uv.x >= -1.87 && uv.x <= 1.87 && uv.y >= -1 && uv.y <= 1) {
        vec3 camPos = vec3(cam[3]);
        fragColor = traceWorld(camPos, vec3(cam*vec4(normalize(vec3(uv, 1)), 0)));
        float whiteness = clamp((abs(rayMapPos.y-size)/size)-0.7f, 0f, 0.5f);
        vec3 fogColor = vec3(max(tint.r, 0.63+(0.37*whiteness)), max(tint.g, 0.75+(0.25*whiteness)), max(tint.b, 1));
        if (fragColor.a != 1) {
            fragColor = vec4(fogColor, 1);
        } else {
            fragColor = vec4(mix(vec3(fragColor), fogColor*1.2, distance(camPos, rayMapPos)/renderDistance), 1);
        }
    } else {
        fragColor = vec4(0, 0, 0, 1);
    }
}