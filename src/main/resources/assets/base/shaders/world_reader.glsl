int size = 4208; //6976
int height = 432;
int chunkSize = 16;
int subChunkSize = chunkSize/2;
int quarterChunkSize = chunkSize/4;
int sizeChunks = size>> 4;
int heightChunks = height>> 4;

uniform mat4 cam;
uniform int renderDistance;
uniform float timeOfDay;
uniform double time;
uniform ivec3 selected;
uniform bool ui;
uniform bool shadowsEnabled;
uniform bool reflectionShadows;
uniform vec3 sun;
uniform bool cloudsEnabled;
uniform ivec3 hand;
uniform ivec2 res;

vec3 normal = vec3(0);
float distanceFogginess = 0.f;
bool firstVoxel = true;
vec3 ogRayPos = vec3(0);
bool addFakeCaustics = false;
float normalBrightness = 1.f;
vec3 uvDir = vec3(0);
vec3 camPos = vec3(cam[3]);
vec3 prevRayMapPos = vec3(0);
vec3 rayMapPos = vec3(0);
vec4 lighting = vec4(0);
vec4 lightFog = vec4(0);
vec3 prevPos = vec3(256);
vec3 prevHitPos = vec3(256);
vec3 hitPos = vec3(256);
vec3 tint = vec3(0);
float reflectivity = 0.f;
bool renderingHand = false;
bool hitSun = false;
float cloudSpeed = 1000.f;
float depth = 0.f;

layout(binding = 3) uniform sampler2D coherent_noise;
layout(binding = 4) uniform sampler2D white_noise;
layout(binding = 5) uniform sampler2D cloud_noise;

float noise(vec2 coords) {
    return (texture(coherent_noise, coords/1024).r)-0.5f;
}
float whiteNoise(vec2 coords) {
    return (texture(white_noise, coords/1024).r)-0.5f;
}
float cloudNoise(vec2 coords) {
    return (texture(cloud_noise, coords/1024).r)-0.5f;
}

layout(std430, binding = 0) buffer atlasSSBO
{
    int[] atlasData;
};
layout(std430, binding = 1) buffer chunkBlocksSSBO
{
    int[] chunkBlocksData;
};
layout(std430, binding = 2) buffer blocksSSBO
{
    int[] blocksData;
};
layout(std430, binding = 3) buffer chunkCornersSSBO
{
    ivec4[] chunkCornersData;
};
layout(std430, binding = 4) buffer cornersSSBO
{
    int[] cornersData;
};
layout(std430, binding = 5) buffer chunkLightsSSBO
{
    ivec4[] chunkLightsData;
};
layout(std430, binding = 6) buffer lightsSSBO
{
    int[] lightsData;
};
layout(std430, binding = 7) buffer chunkEmptySSBO
{
    int[] chunkEmptyData;
};

ivec3 prevCornerChunkPos = ivec3(-1);
ivec4 cornerPaletteInfo = ivec4(-1);
int cornerValuesPerInt = -1;
float logOf2 = log(2);
int getCornerData(int x, int y, int z) {
    ivec3 chunkPos = ivec3(x, y, z) >> 4;
    ivec3 localPos  = ivec3(x, y, z) & ivec3(15);
    int condensedLocalPos = ((((localPos.x*chunkSize)+localPos.z)*chunkSize)+localPos.y);
    if (chunkPos != prevCornerChunkPos) {
        ivec3 prevCornerChunkPos = chunkPos;
        int condensedChunkPos = (((chunkPos.x*sizeChunks)+chunkPos.z)*heightChunks)+chunkPos.y;
        ivec4 cornerPaletteInfo = ivec4(chunkCornersData[condensedChunkPos]);
        int cornerValuesPerInt = 32/cornerPaletteInfo.z;

        int intIndex  = condensedLocalPos/cornerValuesPerInt;
        int bitIndex = (condensedLocalPos - intIndex * cornerValuesPerInt) * cornerPaletteInfo.z;
        int key = (cornersData[cornerPaletteInfo.x+cornerPaletteInfo.y+intIndex] >> bitIndex) & cornerPaletteInfo.w;
        return cornersData[cornerPaletteInfo.x+key];
    } else {
        int intIndex  = condensedLocalPos/cornerValuesPerInt;
        int bitIndex = (condensedLocalPos - intIndex * cornerValuesPerInt) * cornerPaletteInfo.z;
        int key = (cornersData[cornerPaletteInfo.x+cornerPaletteInfo.y+intIndex] >> bitIndex) & cornerPaletteInfo.w;
        return cornersData[cornerPaletteInfo.x+key];
    }
}

bool[8] getCorners(int x, int y, int z) {
    int cornerData = getCornerData(x, y, z);
    bool[8] data = bool[8](false, false, false, false, false, false, false, false);
    for (int bit = 0; bit < 8; bit++) {
        data[bit] = bool(((cornerData & (1 << (bit - 1))) >> (bit - 1)) == 0);
    }
    return data;
}
int shift = 0;
vec4 getVoxel(int x, int y, int z, int bX, int bY, int bZ, int blockType, int blockSubtype, float fire) {
    if (shift == 1) {
        x -= 4;
        if (x < 0) {
            return vec4(0);
        }
    } else if (shift == 2) {
        x += 4;
        if (x >= 8) {
            return vec4(0);
        }
    }
    bool[8] corners = getCorners(bX, bY, bZ);
    int cornerIndex = (y < 4 ? 0 : 4) + (z < 4 ? 0 : 2) + (x < 4 ? 0 : 1);
    if (corners[cornerIndex]) {
        return fromLinear(intToColor(atlasData[(1024*((blockType*8)+x)) + (blockSubtype*64) + ((abs(y-8)-1)*8) + z])/255) + (fire > 0 ? (vec4(vec3(1, 0.3, 0.05)*(abs(max(0, noise((vec2(x+bX, y+bZ)*64)+(float(time)*10000))+noise((vec2(y+bX, z+bZ)*8)+(float(time)*10000))+noise((vec2(z+bZ+x+bX, x+bY)*64)+(float(time)*10000)))*6.66)*fire), 1)) : vec4(0));
    } else {
        return vec4(0, 0, 0, 0);
    }
}
vec4 getVoxel(float x, float y, float z, float bX, float bY, float bZ, int blockType, int blockSubtype, float fire) {
    return getVoxel(int(x), int(y), int(z), int(bX), int(bY), int(bZ), blockType, blockSubtype, fire);
}

ivec3 prevBlockChunkPos = ivec3(-1);
ivec4 blockPaletteInfo = ivec4(-1);
int blockValuesPerInt = -1;
int getBlockData(int x, int y, int z) {
    ivec3 chunkPos = ivec3(x, y, z) >> 4;
    ivec3 localPos = ivec3(x, y, z) & ivec3(15);
    int condensedLocalPos = ((((localPos.x*chunkSize)+localPos.z)*chunkSize)+localPos.y);
    if (chunkPos != prevBlockChunkPos) {
        ivec3 prevBlockChunkPos = chunkPos;
        int condensedChunkPos = (((chunkPos.x*sizeChunks)+chunkPos.z)*heightChunks)+chunkPos.y;
        ivec4 blockPaletteInfo = ivec4(chunkBlocksData[condensedChunkPos*5], chunkBlocksData[(condensedChunkPos*5)+1], chunkBlocksData[(condensedChunkPos*5)+2], chunkBlocksData[(condensedChunkPos*5)+3]);
        int blockValuesPerInt = 32/blockPaletteInfo.z;

        int intIndex  = condensedLocalPos/blockValuesPerInt;
        int bitIndex = (condensedLocalPos - intIndex * blockValuesPerInt) * blockPaletteInfo.z;
        int key = (blocksData[blockPaletteInfo.x+blockPaletteInfo.y+intIndex] >> bitIndex) & blockPaletteInfo.w;
        return blocksData[blockPaletteInfo.x+key];
    } else {
        int intIndex  = condensedLocalPos/blockValuesPerInt;
        int bitIndex = (condensedLocalPos - intIndex * blockValuesPerInt) * blockPaletteInfo.z;
        int key = (blocksData[blockPaletteInfo.x+blockPaletteInfo.y+intIndex] >> bitIndex) & blockPaletteInfo.w;
        return blocksData[blockPaletteInfo.x+key];
    }
}
ivec2 getBlock(int x, int y, int z) {
    int blockData = getBlockData(x, y, z);
    int type = (blockData >> 16) & 0xFFFF;

    return ivec2(type, min(16, blockData & 0xFFFF));
}
ivec2 getBlock(float x, float y, float z) {
    return getBlock(int(x), int(y), int(z));
}
ivec2 getBlock(vec3 pos) {
    return getBlock(int(pos.x), int(pos.y), int(pos.z));
}

bool castsShadow(int x) {
    return true;//(x != 4 && x != 5 && x != 18);
}
bool isBlockSolid(ivec2 block) {
    return (block.x != 0 && block.x != 1 && block.x != 4 && block.x != 5 && block.x != 6 && block.x != 7 && block.x != 11 && block.x != 12 && block.x != 13 && block.x != 14 && block.x != 17 && block.x != 18 && block.x != 21 && block.x != 22);
}
bool hasAO(ivec2 block) {
    return (isBlockSolid(block) ? true : ((block.x == 17 || block.x == 21) && block.y == 0));
}
bool isBlockLight(ivec2 block) {
    return (block.x == 6 || block.x == 7 || block.x == 14 || block.x == 19);
}

ivec3 prevLightChunkPos = ivec3(-1);
ivec4 lightPaletteInfo = ivec4(-1);
int lightValuesPerInt = -1;
int getLightData(int x, int y, int z) {
    ivec3 chunkPos = ivec3(x, y, z) >> 4;
    ivec3 localPos = ivec3(x, y, z) & ivec3(15);
    int condensedLocalPos = ((((localPos.x*chunkSize)+localPos.z)*chunkSize)+localPos.y);
    if (chunkPos != prevLightChunkPos) {
        ivec3 prevLightChunkPos = chunkPos;
        int condensedChunkPos = (((chunkPos.x*sizeChunks)+chunkPos.z)*heightChunks)+chunkPos.y;
        ivec4 lightPaletteInfo = ivec4(chunkLightsData[condensedChunkPos]);
        int lightValuesPerInt = 32/lightPaletteInfo.z;

        int intIndex  = condensedLocalPos/lightValuesPerInt;
        int bitIndex = (condensedLocalPos - intIndex * lightValuesPerInt) * lightPaletteInfo.z;
        int key = (lightsData[lightPaletteInfo.x+lightPaletteInfo.y+intIndex] >> bitIndex) & lightPaletteInfo.w;
        return lightsData[lightPaletteInfo.x+key];
    } else {
        int intIndex  = condensedLocalPos/lightValuesPerInt;
        int bitIndex = (condensedLocalPos - intIndex * lightValuesPerInt) * lightPaletteInfo.z;
        int key = (lightsData[lightPaletteInfo.x+lightPaletteInfo.y+intIndex] >> bitIndex) & lightPaletteInfo.w;
        return lightsData[lightPaletteInfo.x+key];
    }
}

vec4 getLighting(float x, float y, float z) {
    return intToColor(getLightData(int(x), int(y), int(z)));
}

bool isChunkAir(int x, int y, int z) {
    if (x >= 0 && x < sizeChunks && y >= 0 && y < heightChunks && z >= 0 && z < sizeChunks) {
        int condensedChunkPos = (((x*sizeChunks)+z)*heightChunks)+y;
        int chunkDataIndex = condensedChunkPos/32;
        int chunkData = chunkEmptyData[chunkDataIndex];
        if (chunkData != 0) {
            int bit = condensedChunkPos-(chunkDataIndex*32);
            return ((chunkData & (1 << (bit - 1))) >> (bit - 1)) == 0;
        } else {
            return true;
        }
    } else {
        return true;
    }
}

vec3 stepMask(vec3 sideDist) {
    bvec3 mask;
    bvec3 b1 = lessThan(sideDist.xyz, sideDist.yzx);
    bvec3 b2 = lessThanEqual(sideDist.xyz, sideDist.zxy);
    mask.z = b1.z && b2.z;
    mask.x = b1.x && b2.x;
    mask.y = b1.y && b2.y;
    if(!any(mask))
    mask.z = true;

    return vec3(mask);
}

bool isCaustic(vec2 checkPos) {
    float samp = noise((checkPos + (float(time) * 100)) * 64);
    if (samp > -0.1 && samp < 0.1) {
        return true;
    }
    return false;
}

vec4 traceBlock(bool isShadow, float chunkDist, float subChunkDist, float blockDist, vec3 intersect, vec3 rayPos, vec3 rayDir, vec3 iMask, int blockType, int blockSubtype, float sunLight, vec3 unmixedFogColor, float mixedTime) {
    vec3 mapPos = floor(clamp(rayPos, vec3(0.0001), vec3(7.9999)));
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((mapPos - rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = iMask;
    vec3 prevMapPos = mapPos+(stepMask(sideDist+(mask*(-raySign)*deltaDist))*(-raySign));

    vec3 mini = ((mapPos - rayPos) + 0.5 - 0.5 * vec3(raySign)) * deltaDist;
    vec3 hitNormal = -mask * raySign;
    float rayLength = 0.f;
    float voxelDist = max(mini.x, max (mini.y, mini.z));
    if (voxelDist > 0.0f) {
        rayLength += voxelDist/8;
    }
    if (blockDist > 0.0f) {
        rayLength += blockDist;
    }
    if (subChunkDist > 0.0f) {
        rayLength += (subChunkDist*8);
    }
    if (chunkDist > 0.0f) {
        rayLength += (chunkDist*16);
    }
    vec3 realPos = (ogRayPos + rayDir * rayLength);
    prevPos = realPos + (hitNormal * 0.001f);
    ivec2 prevBlock = getBlock(prevPos.x, prevPos.y, prevPos.z);
    float fire = blockType == 19 ? 1.f : (blockType == 9 ? 0.05f : 0.f);
    vec4 prevVoxelColor = firstVoxel ? vec4(0) : getVoxel((prevPos.x-int(prevPos.x))*8, (prevPos.y-int(prevPos.y))*8, (prevPos.z-int(prevPos.z))*8, prevPos.x, prevPos.y, prevPos.z, prevBlock.x, prevBlock.y, fire);
    firstVoxel = false;
    for (int i = 0; mapPos.x < 8.0 && mapPos.x >= 0.0 && mapPos.y < 8.0 && mapPos.y >= 0.0 && mapPos.z < 8.0 && mapPos.z >= 0.0; i++) {
        vec4 voxelColor = getVoxel(mapPos.x, mapPos.y, mapPos.z, rayMapPos.x, rayMapPos.y, rayMapPos.z, blockType, blockSubtype, fire);
        if (voxelColor.a > 0.f) {
            bool canHit = bool(prevVoxelColor.a < voxelColor.a);
            float shouldReflect = 0.f;
            if (reflectivity == 0.f && canHit) {
                if (max(voxelColor.r, max(voxelColor.g, voxelColor.b)) > 0.8f) {
                    if (blockType == 1 && blockSubtype > 0 && prevBlock.x == 0) { //water
                        shouldReflect = 0.6f;
                    } else if (blockType == 7 || (blockType >= 11 && blockType <= 13)) { //glass & kyanite
                        reflectivity = 0.5f;
                    } else if ((blockType == 1 && blockSubtype == 0) || blockType == 22) { //steel
                        reflectivity = 0.16f;
                    } else if (blockType == 15) { //planks
                        reflectivity = 0.12f;
                    }
                }
            }
            normal = ivec3(mapPos - prevMapPos);
            if (voxelColor.a < 1.f) {
                vec4 tintColor = voxelColor*voxelColor.a;
                tint += vec3(toLinear(tintColor)) * (tint == vec3(0) ? 2 : 1);
                //bubbles start
                if (isShadow) { //add check to not return early for semi-transparent blocks that contain solid voxels.
                    return vec4(0);
                }
                bool underwater = false;
                if (blockType == 1 && !renderingHand) {
                    if (getBlock(realPos.x, realPos.y+0.15f, realPos.z).x != 1) { //change to allow non-full water blocks to have caustics.
                      if (isCaustic(vec2(rayMapPos.x, rayMapPos.z)+(mapPos.xz/8))) {
                          voxelColor = fromLinear(vec4(1, 1, 1, 1));
                          shouldReflect = -0.01f;
                      }
                    } else {
                        underwater = true;
                    }
                } else {
                    float samp = whiteNoise(((vec2(mapPos.x, mapPos.z)*128)+((renderingHand ? 3 : rayMapPos.y)*8)+mapPos.y)+(vec2((renderingHand ? 3 : rayMapPos.x), (renderingHand ? 3 : rayMapPos.z))*8));
                    if (samp > -0.004 && samp < -0.002 || samp > 0 && samp < 0.002) {
                        voxelColor = fromLinear(vec4(1, 1, 1, 1));
                    }
                }
                //bubbles end
                if (reflectivity == 0.f) {
                    reflectivity = shouldReflect;
                }

                if (!underwater) {
                    if (hitPos == vec3(256) && canHit) {
                        prevHitPos = renderingHand ? (camPos+(prevMapPos/8)) : prevPos;
                        hitPos = renderingHand ? (camPos+(mapPos/8)) : realPos;
                    }
                }

                if (underwater) {
                    return vec4(0);
                } else if (blockType == 1 && prevBlock.x != 1) { //if ray hit water, but was previously not in water, water will be treated as a solid.
                    voxelColor.a = 1;
                }
            }
            if (voxelColor.a >= 1 && (!isShadow || castsShadow(blockType))) {
                if (hitPos == vec3(256)) {
                    prevHitPos = renderingHand ? (camPos+(prevMapPos/8)) : prevPos;
                    hitPos = renderingHand ? (camPos+(mapPos/8)) : realPos;
                }
                //face-based brightness start
                if (normal.y >0) { //down
                    normalBrightness = 0.7f;
                } else if (normal.y <0) { //up
                    normalBrightness = 1.f;
                } else if (normal.z >0) { //south
                    normalBrightness = 0.85f;
                } else if (normal.z <0) { //north
                    normalBrightness = 0.85f;
                } else if (normal.x >0) { //west
                    normalBrightness = 0.75f;
                } else if (normal.x <0) { //east
                    normalBrightness = 0.95f;
                }
                //face-based brightness end

//                if (!renderingHand) {
//                    float occlusion = 1.5f;
//                    if (ceil(prevPos.x)-prevPos.x < 0.5f) {
//                        if (hasAO(getBlock(ceil(prevPos.x), prevPos.y, prevPos.z))) {
//                            occlusion -= abs(0.5f-min(0.5f, (ceil(prevPos.x) - prevPos.x)));
//                        }
//                    } else if (hasAO(getBlock(floor(prevPos.x) - 0.1f, prevPos.y, prevPos.z))) {
//                        occlusion -= abs(0.5f-min(0.5f, (prevPos.x - floor(prevPos.x))));
//                    }
//                    if (ceil(prevPos.y)-prevPos.y < 0.5f) {
//                        if (hasAO(getBlock(prevPos.x, ceil(prevPos.y), prevPos.z))) {
//                            occlusion -= abs(0.5f-min(0.5f, (ceil(prevPos.y) - prevPos.y)));
//                        }
//                    } else if (hasAO(getBlock(prevPos.x, floor(prevPos.y) - 0.1f, prevPos.z))) {
//                        occlusion -= abs(0.5f-min(0.5f, (prevPos.y - floor(prevPos.y))));
//                    }
//                    if (ceil(prevPos.z)-prevPos.z < 0.5f) {
//                        if (hasAO(getBlock(prevPos.x, prevPos.y, ceil(prevPos.z)))) {
//                            occlusion -= abs(0.5f-min(0.5f, (ceil(prevPos.z) - prevPos.z)));
//                        }
//                    } else if (hasAO(getBlock(prevPos.x, prevPos.y, floor(prevPos.z) - 0.1f))) {
//                        occlusion -= abs(0.5f-min(0.5f, (prevPos.z - floor(prevPos.z))));
//                    }
//                    normalBrightness *= clamp(occlusion, 0.25f, 1.f);
//                }

                if (prevBlock.x == 1 && prevBlock.y > 0) {
                    if (isCaustic(vec2(rayMapPos.x, rayMapPos.z) + (mapPos.xz / 8))) {
                        addFakeCaustics = true;
                    }
                }

                return vec4(vec3(voxelColor), 1);
            }
        }

        mask = stepMask(sideDist);
        prevMapPos = mapPos;
        mapPos += mask * raySign;
        mini = ((mapPos - rayPos) + 0.5 - 0.5 * vec3(raySign)) * deltaDist;
        hitNormal = -mask * raySign;
        rayLength = 0.f;
        voxelDist = max(mini.x, max (mini.y, mini.z));
        if (voxelDist > 0.0f) {
            rayLength += voxelDist/8;
        }
        if (blockDist > 0.0f) {
            rayLength += blockDist;
        }
        if (subChunkDist > 0.0f) {
            rayLength += (subChunkDist*8);
        }
        if (chunkDist > 0.0f) {
            rayLength += (chunkDist*16);
        }
        realPos = (ogRayPos + rayDir * rayLength);
        prevPos = realPos + (hitNormal * 0.001f);
        prevVoxelColor = voxelColor;
        sideDist += mask * raySign * deltaDist;
    }

    return vec4(0.0);
}

vec4 dda(bool isShadow, float chunkDist, float subChunkDist, int condensedChunkPos, vec3 offset, vec3 rayPos, vec3 rayDir, vec3 iMask, bool inBounds, float maxDistance) {
    vec3 mapPos = floor(clamp(rayPos, vec3(0.0001), vec3(7.9999)));
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((mapPos-rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = iMask;
    rayMapPos = offset+mapPos;
    prevRayMapPos = rayMapPos;

    if (distance(rayMapPos, ogRayPos) > maxDistance) {
        return vec4(-1);
    }

    for (int i = 0; mapPos.x < 8.0 && mapPos.x >= 0.0 && mapPos.y < 8.0 && mapPos.y >= 0.0 && mapPos.z < 8.0 && mapPos.z >= 0.0; i++) {
        float adjustedTime = clamp((abs(1-clamp((distance(rayMapPos, sun-vec3(0, sun.y, 0))/1.5)/size, 0, 1))*1.2)-abs(0.25f-min(0.25f, distance(rayMapPos, vec3(0))/size)), 0.05f, 1.f);
        float adjustedTimeCam = clamp((abs(1-clamp((distance(camPos, sun-vec3(0, sun.y, 0))/1.5)/size, 0, 1))*1.2)-abs(0.25f-min(0.25f, distance(camPos, vec3(0))/size)), 0.05f, 0.9f);
        float timeBonus = gradient(rayMapPos.y, 64.f, 372.f, 0.1f, 0.f);
        float mixedTime =  max(0.2f, (adjustedTime/2)+(adjustedTimeCam/2)+timeBonus);
        if (renderingHand) {
            vec3 handPos = vec3(-1, -2, 1);
            if (rayMapPos == handPos || rayMapPos == handPos+vec3(1, 0, 0)) {
                ivec2 blockInfo = ivec2(hand);
                vec3 mini = ((mapPos-rayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
                float d = max (mini.x, max (mini.y, mini.z));
                vec3 intersect = rayPos + rayDir*d;
                vec3 uv3d = intersect - mapPos;
                float blockDist = max(mini.x, max(mini.y, mini.z));

                if (mapPos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
                   uv3d = rayPos - mapPos;
                }

                if (blockInfo.x != 0.f) {
                    if (rayMapPos.x < 0) {
                        shift = 1;
                    } else {
                        shift = 2;
                    }

                    float camDist = 0;
                    float whiteness = gradient(camPos.y, 64, 372, 0, 0.8);
                    vec3 unmixedFogColor = mix(vec3(0.416, 0.495, 0.75), vec3(1), whiteness);
                    float fogNoise = max(0, cloudNoise((vec2(hitPos.x, hitPos.z))+(floor(hitPos.y/16)+(float(time)*7500))))*gradient(hitPos.y, 63, 96, 0, 0.77);
                    if (blockInfo.x != 1.f) {
                        fogNoise+=gradient(hitPos.y, 63.f, 96.f, 0.f, 1.f);
                    }
                    float linearDistFog = camDist+(fogNoise/2)+((fogNoise/2)*camDist);
                    distanceFogginess = max(distanceFogginess, clamp((exp2(linearDistFog-0.75f)+min(0.f, linearDistFog-0.25f))-0.1f, 0.f, 1.f));
                    float sunLight = (lighting.a/20)*(mixedTime-timeBonus);
                    vec4 color = traceBlock(isShadow, chunkDist, subChunkDist, blockDist, intersect, uv3d * 8.0, rayDir, mask, blockInfo.x, blockInfo.y, sunLight, unmixedFogColor, mixedTime);
                    if ((blockInfo.x == 17 || blockInfo.x == 21) && color.a >= 1) {
                        color = vec4(hsv2rgb(rgb2hsv(vec3(color))-vec3(0, cloudNoise(vec2(camPos.x, camPos.z)*10), 0)), 1);
                    }
                    //lighting start
                    lighting = fromLinear(getLighting(camPos.x, camPos.y, camPos.z));
                    lightFog = max(lightFog, lighting*vec4(0.5, 0.5, 0.5, 1));
                    if (color.a >= 1.f) {
                        return color;
                    }
                }
            }
        } else {
            //block start
            ivec2 blockInfo = ivec2(0);
            vec3 uv3d = vec3(0);
            vec3 intersect = vec3(0);
            vec3 mini = ((mapPos-rayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
            float blocKDist = max(mini.x, max(mini.y, mini.z));
            if (inBounds) {
                blockInfo = getBlock(rayMapPos);

                intersect = rayPos + rayDir*blocKDist;
                uv3d = intersect - mapPos;

                if (mapPos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
                    uv3d = rayPos - mapPos;
                }
            }
            //block end

            vec4 color = vec4(0);
            float camDist = distance(camPos, rayMapPos)/renderDistance;
            float whiteness = gradient(rayMapPos.y, 64, 372, 0, 0.8);
            vec3 unmixedFogColor = mix(vec3(0.416, 0.495, 0.75), vec3(1), whiteness);
            if (blockInfo.x != 0.f) {
                float fogNoise = max(0, cloudNoise((vec2(hitPos.x, hitPos.z))+(floor(hitPos.y/16)+(float(time)*7500))))*gradient(hitPos.y, 63, 96, 0, 0.77);
                if (blockInfo.x != 1.f) {
                    fogNoise+=gradient(hitPos.y, 63.f, 96.f, 0.f, 1.f);
                }
                float linearDistFog = camDist+(fogNoise/2)+((fogNoise/2)*camDist);
                distanceFogginess = max(distanceFogginess, clamp((exp2(linearDistFog-0.75f)+min(0.f, linearDistFog-0.25f))-0.1f, 0.f, 1.f));
                float sunLight = (lighting.a/20)*(mixedTime-timeBonus);
                color = traceBlock(isShadow, chunkDist, subChunkDist, blocKDist, intersect, uv3d * 8.0, rayDir, mask, blockInfo.x, blockInfo.y, sunLight, unmixedFogColor, mixedTime);
                if ((blockInfo.x == 17 || blockInfo.x == 21) && color.a >= 1) {
                    color = vec4(hsv2rgb(rgb2hsv(vec3(color))-vec3(0, cloudNoise(vec2(rayMapPos.x, rayMapPos.z)*10), 0)), 1);
                }
                //lighting start
                float lightNoise = max(0, cloudNoise((vec2(prevPos.x, prevPos.y)*64)+(float(time)*10000))+cloudNoise((vec2(prevPos.y, prevPos.z)*64)+(float(time)*10000))+cloudNoise((vec2(prevPos.z, prevPos.x)*64)+(float(time)*10000)));

                lighting = fromLinear(getLighting(prevPos.x, prevPos.y, prevPos.z));
                lightFog = max(lightFog, lighting*(1-(vec4(0.5, 0.5, 0.5, 0)*vec4(lightNoise))));
                lighting *= 1+(vec4(0.5, 0.5, 0.5, -0.25f)*vec4(lightNoise, lightNoise, lightNoise, 1));
            }

            if (color.a >= 1) {
                return color;
            } else if (color.a <= -1) {
                return vec4(-1);
            }
        }

        mask = stepMask(sideDist);
        mapPos += mask * raySign;
        prevRayMapPos = rayMapPos;
        rayMapPos = offset+mapPos;
        sideDist += mask * raySign * deltaDist;
    }

    return vec4(0);
}

vec4 subChunkDDA(bool isShadow, float chunkDist, int condensedChunkPos, vec3 offset, vec3 rayPos, vec3 rayDir, vec3 iMask, bool inBounds, float maxDistance) {
    vec3 mapPos = floor(clamp(rayPos, vec3(0.0001), vec3(1.9999)));
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((mapPos-rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = iMask;
    int subChunks = chunkBlocksData[(condensedChunkPos*5)+4];
    rayMapPos = offset+(mapPos*8);
    prevRayMapPos = rayMapPos;

    if (distance(rayMapPos, ogRayPos) > maxDistance) {
        return vec4(-1);
    }

    for (int i = 0; mapPos.x < 2.0 && mapPos.x >= 0.0 && mapPos.y < 2.0 && mapPos.y >= 0.0 && mapPos.z < 2.0 && mapPos.z >= 0.0; i++) {
        if (renderingHand) {
            vec3 mini = ((mapPos-rayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
            float d = max (mini.x, max (mini.y, mini.z));
            vec3 intersect = rayPos + rayDir*d;
            vec3 uv3d = intersect - mapPos;
            float subChunkDist = max(mini.x, max(mini.y, mini.z));

            if (mapPos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
                uv3d = rayPos - mapPos;
            }

            vec4 color = dda(isShadow, chunkDist, subChunkDist, condensedChunkPos, rayMapPos, uv3d * 8.0, rayDir, mask, true, maxDistance);

            if (color.a >= 1) {
                return color;
            }
        } else {
            ivec3 localPos = ivec3(rayMapPos.x, rayMapPos.y, rayMapPos.z) & ivec3(15);
            int subChunkPos = ((((localPos.x >= subChunkSize  ? 1 : 0)*2)+(localPos.z >= subChunkSize  ? 1 : 0))*2)+(localPos.y >= subChunkSize  ? 1 : 0);
            bool checkBlocks = bool(((subChunks >> (subChunkPos % 32)) & 1) > 0);
            if (checkBlocks) {
                vec3 mini = ((mapPos-rayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
                float subChunkDist = max(mini.x, max(mini.y, mini.z));
                vec3 intersect = rayPos + rayDir*subChunkDist;
                vec3 uv3d = intersect - mapPos;

                if (mapPos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
                    uv3d = rayPos - mapPos;
                }

                vec4 color = dda(isShadow, chunkDist, subChunkDist, condensedChunkPos, rayMapPos, uv3d * 8.0, rayDir, mask, inBounds, maxDistance);

                if (color.a >= 1) {
                    return color;
                } else if (color.a <= -1) {
                    return vec4(-1);
                }
            }
        }

        mask = stepMask(sideDist);
        mapPos += mask * raySign;
        rayMapPos = offset+(mapPos*8);
        sideDist += mask * raySign * deltaDist;
    }

    return vec4(0);
}

vec4 traceWorld(bool isShadow, vec3 ogPos, vec3 rayDir, float maxDistance) {
    if (rayDir.x == 0) {
        rayDir.x = 0.00001f;
    }
    if (rayDir.y == 0) {
        rayDir.y = 0.00001f;
    }
    if (rayDir.z  == 0) {
        rayDir.z = 0.00001f;
    }
    ogRayPos = ogPos;
    vec3 rayPos = ogRayPos/chunkSize;
    vec3 rayMapChunkPos = floor(rayPos);
    vec3 prevRayMapPos = rayMapChunkPos;
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((rayMapChunkPos-rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = stepMask(sideDist);
    rayMapPos = ogRayPos;
    prevRayMapPos = rayMapPos;

    for (int i = 0; distance(rayMapChunkPos, rayPos) < maxDistance/chunkSize; i++) {
        if (renderingHand) {
            vec3 mini = ((rayMapChunkPos-rayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
            float d = max (mini.x, max (mini.y, mini.z));
            vec3 intersect = rayPos + rayDir*d;
            vec3 uv3d = intersect - rayMapChunkPos;
            float chunkDist = max(mini.x, max(mini.y, mini.z));

            if (rayMapChunkPos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
                uv3d = rayPos - rayMapChunkPos;
            }
            ivec3 chunkPos = ivec3(rayMapChunkPos);
            vec4 color = subChunkDDA(isShadow, chunkDist, (((chunkPos.x*sizeChunks)+chunkPos.z)*heightChunks)+chunkPos.y, chunkPos*16, uv3d * 2.0, rayDir, mask, true, maxDistance);
            if (color.a >= 1) {
                return color;
            }
        } else {
            bool inHorizontalBounds = bool(rayMapChunkPos.x >= 0 && rayMapChunkPos.x < sizeChunks && rayMapChunkPos.z >= 0 && rayMapChunkPos.z < sizeChunks);
            if (!inHorizontalBounds) {
                break;
            }
            bool inBounds = bool(inHorizontalBounds && rayMapChunkPos.y >= 0 && rayMapChunkPos.y < heightChunks);
            bool checkSubChunks = inBounds ? !isChunkAir(int(rayMapChunkPos.x), int(rayMapChunkPos.y), int(rayMapChunkPos.z)) : false;
            if (checkSubChunks) {
                vec3 mini = ((rayMapChunkPos - rayPos) + 0.5 - 0.5 * vec3(raySign)) * deltaDist;
                float chunkDist = max(mini.x, max(mini.y, mini.z));
                vec3 intersect = rayPos + rayDir * chunkDist;
                vec3 uv3d = intersect - rayMapChunkPos;

                if (rayMapChunkPos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
                    uv3d = rayPos - rayMapChunkPos;
                }
                ivec3 chunkPos = ivec3(rayMapChunkPos);
                vec4 color = subChunkDDA(isShadow, chunkDist, (((chunkPos.x * sizeChunks) + chunkPos.z) * heightChunks) + chunkPos.y, chunkPos * chunkSize, uv3d * 2.0, rayDir, mask, inBounds, maxDistance);
                if (color.a >= 1) {
                    return color;
                } else if (color.a <= -1) {
                    prevHitPos = rayMapChunkPos * chunkSize;
                    hitPos = rayMapChunkPos * chunkSize;
                    break;
                }
            }
        }

        mask = stepMask(sideDist);
        prevRayMapPos = rayMapChunkPos;
        rayMapChunkPos += mask * raySign;
        rayMapPos = prevRayMapPos*chunkSize;
        prevRayMapPos = rayMapPos;
        sideDist += mask * raySign * deltaDist;
    }

    vec3 mini = ((rayMapChunkPos-rayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
    float chunkDist = max(mini.x, max(mini.y, mini.z));
    vec3 intersect = rayPos + rayDir*chunkDist;
    vec3 uv3d = intersect - rayMapChunkPos;

    if (rayMapChunkPos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
        uv3d = rayPos - rayMapChunkPos;
    }
    prevHitPos = (rayMapChunkPos+uv3d)*16;
    hitPos = prevHitPos;
    vec3 nearestPos = clamp(prevHitPos, vec3(0), vec3(size-1, height-1, size-1));
    lighting = fromLinear(getLighting(nearestPos.x, nearestPos.y, nearestPos.z));
    lightFog = max(lightFog, lighting);
    reflectivity = 0;
    return vec4(0);
}

bool isSky = false;

vec3 prevReflectPos = vec3(0);
vec3 reflectPos = vec3(0);

void clearVars(bool clearHit, bool clearTint) {
    normalBrightness = 1.f;
    firstVoxel = true;
    if (clearHit) {
        prevHitPos = vec3(256);
        hitPos = vec3(256);
    }
    prevRayMapPos = vec3(0);
    rayMapPos = vec3(0);
    lighting = vec4(0);
    lightFog = vec4(0);
    if (clearTint) {
        tint = vec3(0);
    }
    normal = ivec3(0);
}

vec4 prevFog = vec4(1);
vec4 prevSuperFinalTint = vec4(1);

vec4 raytrace(vec3 ogRayPos, vec3 dir, bool checkShadow, float maxDistance) {
    clearVars(true, false);
    vec4 color = traceWorld(false, ogRayPos, dir, maxDistance);
    //return fromLinear(vec4(lighting.a/20)); figure out why theres sunlight everywhere theres blocklight. Maybe its something to do with setting the light value for light blocks?
    depth = distance(camPos, hitPos)/renderDistance;
    isSky = renderingHand ? false : (color.a < 1.f && lighting.a > 0);
    if (renderingHand) {
        prevPos = camPos;
        hitPos = camPos-vec3(0, 0.05f, 0);
        prevHitPos = camPos;
    } else if (isSky) {
        float sunDir = dot(normalize(sun - camPos), dir);
        if (sunDir > 0.95f && sunDir < 1.05f) {
            return vec4(1, 1, 0, 1);
        }
        vec3 lunaPos = (((sun-vec3(size/2, 0, size/2))*vec3(-1, 1, -1))+vec3(size/2, 0, size/2));
        float lunaDir = dot(normalize(lunaPos - camPos), dir);
        if (lunaDir > 0.95f && lunaDir < 1.05f) {
            return vec4(0.95f, 0.95f, 1, 1);
        }
    }
    prevReflectPos = prevHitPos;
    reflectPos = hitPos;
    float borders = max(gradient(hitPos.x, 0, 128, 0, 1), max(gradient(hitPos.z, 0, 128, 0, 1), max(gradient(hitPos.x, size, size-128, 0, 1), gradient(hitPos.z, size, size-128, 0, 1))));
    float adjustedTime = clamp((abs(1-clamp((distance(hitPos, sun-vec3(0, sun.y, 0))/1.5f)/size, 0, 1))*1.2)-abs(0.25f-min(0.25f, distance(rayMapPos, vec3(0))/size)), 0.05f, 1.f);
    float adjustedTimeCam = clamp((abs(1-clamp((distance(camPos, sun-vec3(0, sun.y, 0))/1.5f)/size, 0, 1))*1.2)-abs(0.25f-min(0.25f, distance(rayMapPos, vec3(0))/size)), 0.05f, 0.9f);
    float timeBonus = gradient(hitPos.y, 64.f, 372.f, 0.1f, 0.f);
    float mixedTime = max(0.2f, (adjustedTime/2)+(adjustedTimeCam/2)+timeBonus);
    float fogNoise = 0.25f*((max(0, cloudNoise((vec2(hitPos.x, hitPos.z))+(floor(hitPos.y/16)+(float(time)*7500))))*gradient(hitPos.y, 63, 96, 0, 0.77))+gradient(hitPos.y, 63, 96, 0, 0.33f));
    distanceFogginess = clamp(max(borders, distanceFogginess+fogNoise), 0.f, 1.f);
    if (isSky) {
        distanceFogginess = 1;
    }
    lighting = pow(lighting/20, vec4(2.f))*vec4(200, 200, 200, 35);
    lightFog = pow(lightFog/20, vec4(2.f))*vec4(200, 200, 200, 35);
    float sunLight = (lighting.a/20)*(mixedTime-timeBonus);
    float whiteness = gradient(hitPos.y, 64, 372, 0, 0.8);
    vec3 sunColor = (mix(mix(vec3(0.0f, 0.0f, 4.5f), vec3(2.125f, -0.4f, 0.125f), mixedTime*4), vec3(0.1f, 0.95f, 1.5f), mixedTime) * 0.15f) + mix(vec3(0), vec3(0, 0, 1), abs(0.5f-min(0.5f, mixedTime)));

    vec3 finalRayMapPos = rayMapPos;
    vec4 finalLighting = lighting;
    float finalNormalBrightness = normalBrightness;
    float sunLightFog = (lightFog.a/12)*mixedTime;
    vec3 finalLightFog = max(vec3(lightFog)*((0.7-min(0.7, (lightFog.a/20)*mixedTime))/4), max(vec3(sunColor.b), (sunColor*mix(sunColor.r*6, 0.2, max(sunColor.r, max(sunColor.g, sunColor.b))))*sunLightFog));
    vec3 finalTint = max(vec3(1), tint);

    vec3 cloudPos = (vec3(cam * vec4(uvDir * 500.f, 1.f))-camPos);
    if ((isSky || borders > 0.f) && cloudsEnabled) {
        whiteness = whiteness+((sqrt(max(0, cloudNoise((vec2(cloudPos.x, cloudPos.y*1.5f))+(float(time)*cloudSpeed))+cloudNoise((vec2(cloudPos.y*1.5f, cloudPos.z))+(float(time)*cloudSpeed))+cloudNoise((vec2(cloudPos.z, cloudPos.x))+(float(time)*cloudSpeed)))*gradient(hitPos.y, 0, 372, 1.5f, 0))*gradient(hitPos.y, 0, 372, 1, 0)) * (isSky ? 1.f : pow(borders, 32)));
    }
    vec3 unmixedFogColor = mix(vec3(0.416, 0.495, 0.75), vec3(1), whiteness)*min(1, sunLightFog);

    if (addFakeCaustics) {
        float factor = max(1, 2.5-distanceFogginess)*(1+(max(0, abs(1-mixedTime)-0.9)*10));
        color += (sunLight*(factor/10));
    }
    if (!isSky && checkShadow && shadowsEnabled && !hitSun && adjustedTimeCam > 0.23f) {
        if (color.a >= 1.f && sunLight > 0.f && finalLighting.a > 0) {
            vec3 shadowPos = prevPos;
            vec3 sunDir = normalize(sun - shadowPos);
            clearVars(true, true);
            bool wasRenderingHand = renderingHand;
            if (wasRenderingHand) {
                shift = 0;
                renderingHand = false;
            }
            float oldReflectivity = reflectivity;
            reflectivity = 0.f;
            float factor = max(1, 2.5-distanceFogginess)*(1+(max(0, abs(1-mixedTime)-0.9)*10));;
            if (traceWorld(true, shadowPos, sunDir, maxDistance).a >= 1.f) {
                sunLight /= factor;
                finalLighting.a /= factor;
            } else {
                tint = max(vec3(1), tint*2);
                float tintFactor = max(tint.r, max(tint.g, tint.b));
                color = mix(color, fromLinear(vec4((vec3(tint)/tintFactor)-1, 1)), ((tintFactor-1)/20)*(mixedTime-timeBonus));//sun tint
            }
            reflectivity = oldReflectivity;
            renderingHand = wasRenderingHand;
        }
    }
    vec3 finalLight = (vec3(finalLighting*((0.7-min(0.7, (finalLighting.a/20)*mixedTime))/4))+((finalLighting.a/20)*max(0.23f, sunLight)))*finalNormalBrightness;
    color = vec4(vec3(color)*finalLight, color.a);//light
    //fog start
    if (getBlock(camPos).x == 1) {
        unmixedFogColor += (vec3(-0.5f, -0.5f, 0.5f)*min(1, distanceFogginess*10));
    }
    vec4 fog = 1+vec4(vec3(finalLightFog)*1.5f, 1);
    color *= fog;
    color *= prevFog;
    prevFog = fog;
    color = vec4(mix(vec3(color), unmixedFogColor, distanceFogginess), color.a);

    //fog end
    //transparency start
    vec4 superFinalTint = fromLinear(vec4(vec3(finalTint)/(max(finalTint.r, max(finalTint.g, finalTint.b))), 1));
    color *= superFinalTint;
    color *= prevSuperFinalTint;
    prevSuperFinalTint = superFinalTint;
    //transparency end

    //selection start
    if (ui && !renderingHand && selected == ivec3(finalRayMapPos)) {
        color = vec4(mix(vec3(color), vec3(0.7, 0.7, 1), 0.5f), color.a);
    }
    //selection end
    return color;
}