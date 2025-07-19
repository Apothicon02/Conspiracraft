#version 430 core

uniform mat4 cam;
uniform int renderDistance;
uniform float timeOfDay;
uniform double time;
uniform ivec3 selected;
uniform bool ui;
uniform bool shadowsEnabled;
uniform bool raytracedCaustics;
uniform bool snowing;
uniform vec3 sun;
uniform bool cloudsEnabled;
uniform ivec3 hand;

layout(binding = 0, rgba32f) uniform writeonly image2D scene_image;
layout(binding = 1) uniform sampler2D coherent_noise;
layout(binding = 2) uniform sampler2D white_noise;
layout(binding = 3) uniform sampler2D cloud_noise;

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
layout(std430, binding = 7) buffer imageSSBO
{
    int[] imageData;
};
layout(std430, binding = 8) buffer chunkEmptySSBO
{
    int[] chunkEmptyData;
};
in vec4 gl_FragCoord;

out vec4 fragColor;
int size = 2048; //6976
int height = 432;
int chunkSize = 16;
int subChunkSize = chunkSize/2;
int quarterChunkSize = chunkSize/4;
int sizeChunks = size>> 4;
int heightChunks = height>> 4;

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
bool renderingHand = true;
bool hitSun = false;
float cloudSpeed = 1000.f;

float lerp(float invLerpValue, float toValue, float fromValue) {
    return toValue + invLerpValue * (fromValue - toValue);
}

float inverseLerp(float y, float fromY, float toY) {
    return (y - fromY) / (toY - fromY);
}

float clampedLerp(float toValue, float fromValue, float invLerpValue) {
    if (invLerpValue < 0.0) {
        return toValue;
    } else {
        return invLerpValue > 1.0 ? fromValue : lerp(invLerpValue, toValue, fromValue);
    }
}

float gradient(float y, float fromY, float toY, float fromValue, float toValue) {
    return clampedLerp(toValue, fromValue, inverseLerp(y, fromY, toY));
}

// Converts a color from linear light gamma to sRGB gamma
vec4 fromLinear(vec4 linearRGB)
{
    bvec3 cutoff = lessThan(linearRGB.rgb, vec3(0.0031308));
    vec3 higher = vec3(1.055)*pow(linearRGB.rgb, vec3(1.0/2.4)) - vec3(0.055);
    vec3 lower = linearRGB.rgb * vec3(12.92);

    return vec4(mix(higher, lower, cutoff), linearRGB.a);
}

// Converts a color from sRGB gamma to linear light gamma
vec4 toLinear(vec4 sRGB)
{
    bvec3 cutoff = lessThan(sRGB.rgb, vec3(0.04045));
    vec3 higher = pow((sRGB.rgb + vec3(0.055))/vec3(1.055), vec3(2.4));
    vec3 lower = sRGB.rgb/vec3(12.92);

    return vec4(mix(higher, lower, cutoff), sRGB.a);
}

vec3 rgb2hsv(vec3 c)
{
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

vec3 hsv2rgb(vec3 c)
{
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

float noise(vec2 coords) {
    return (texture(coherent_noise, coords/1024).r)-0.5f;
}
float whiteNoise(vec2 coords) {
    return (texture(white_noise, coords/1024).r)-0.5f;
}
float cloudNoise(vec2 coords) {
    return (texture(cloud_noise, coords/2048).r)-0.5f;
}

vec4 intToColor(int color) {
    return vec4(0xFF & color >> 16, 0xFF & color >> 8, 0xFF & color, 0xFF & color >> 24);
}

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
    bool[8] data = bool[8](0);
    for (int bit = 0; bit < 8; bit++) {
        data[bit] = ((cornerData & (1 << (bit - 1))) >> (bit - 1)) == 0;
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

bool isBlockSolid(ivec2 block) {
    return (block.x != 0 && block.x != 1 && block.x != 4 && block.x != 5 && block.x != 6 && block.x != 7 && block.x != 8 && block.x != 9 && block.x != 11 && block.x != 12 && block.x != 13 && block.x != 14 && block.x != 17 && block.x != 18 && block.x != 21 && block.x != 22);
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

vec4 getLighting(float x, float y, float z, bool shiftedX, bool shiftedY, bool shiftedZ) {
    int intX = int(x);
    int intY = int(y);
    int intZ = int(z);
    ivec2 block = getBlock(intX, intY, intZ);
    if (!(shiftedX && shiftedY && shiftedZ) && (block.x == 17 || block.x == 21) && block.y == 0) {
        return vec4(0, 0, 0, 0);
    }
    vec4 light = intToColor(getLightData(intX, intY, intZ));
    if (isBlockSolid(block)) { //return pure darkness if block isnt transparent.
                               bool[8] corners = getCorners(intX, intY, intZ);
                               float localX = (x-intX);
                               float localY = (y-intY);
                               float localZ = (z-intZ);
                               bool anyEmpty = false;
                               if (shiftedY) {
                                   int ySide = (localY < 0.5f ? 0 : 4);
                                   if (!corners[ySide + 0 + 0]) {
                                       anyEmpty = true;
                                   } else if (!corners[ySide + 2 + 0]) {
                                       anyEmpty = true;
                                   } else if (!corners[ySide + 0 + 1]) {
                                       anyEmpty = true;
                                   } else if (!corners[ySide + 2 + 1]) {
                                       anyEmpty = true;
                                   }
                               }
                               if (shiftedZ) {
                                   int zSide = (localZ < 0.5f ? 0 : 2);
                                   if (!corners[0 + zSide + 0]) {
                                       anyEmpty = true;
                                   } else if (!corners[4 + zSide + 0]) {
                                       anyEmpty = true;
                                   } else if (!corners[0 + zSide + 1]) {
                                       anyEmpty = true;
                                   } else if (!corners[4 + zSide + 1]) {
                                       anyEmpty = true;
                                   }
                               }
                               if (shiftedX) {
                                   int xSide = (localX < 0.5f ? 0 : 1);
                                   if (!corners[0 + 0 + xSide]) {
                                       anyEmpty = true;
                                   } else if (!corners[4 + 0 + xSide]) {
                                       anyEmpty = true;
                                   } else if (!corners[0 + 2 + xSide]) {
                                       anyEmpty = true;
                                   } else if (!corners[4 + 2 + xSide]) {
                                       anyEmpty = true;
                                   }
                               }
                               if (!anyEmpty) {
                                   return vec4(0, 0, 0, 0);
                               }
    }
    return light;
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

bool checker(ivec2 pixel)
{
    bool xOdd = bool(pixel.x % 2 == 1);
    bool yOdd = bool(pixel.y % 2 == 1);
    if (xOdd && yOdd) { //both even or both odd
                        return true;
    }
    return false;
}

bool checkerOn = false;
float normalBrightness = 1.f;
bool isSnowFlake = false;
bool wasEverTinted = false;
ivec3 normal = ivec3(0);
float distanceFogginess = 0.f;
bool firstVoxel = true;
vec3 ogRayPos = vec3(0);
bool addFakeCaustics = false;

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

    bool wasTinted = false;
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
    while (mapPos.x < 8.0 && mapPos.x >= 0.0 && mapPos.y < 8.0 && mapPos.y >= 0.0 && mapPos.z < 8.0 && mapPos.z >= 0.0) {
        vec4 voxelColor = getVoxel(mapPos.x, mapPos.y, mapPos.z, rayMapPos.x, rayMapPos.y, rayMapPos.z, blockType, blockSubtype, fire);
        if (voxelColor.a > 0.f) {
            bool canHit = bool(prevVoxelColor.a < voxelColor.a);
            float shouldReflect = 0.f;
            if (reflectivity == 0.f && canHit) {
                if (max(voxelColor.r, max(voxelColor.g, voxelColor.b)) > 0.8f) {
                    if (blockType == 1 && blockSubtype > 0 && prevBlock.x == 0) { //water
                                                                                  shouldReflect = 0.6f;
                    } else if (blockType == 7) { //kyanite
                                                 reflectivity = 0.66f;
                    } else if (blockType >= 11 && blockType <= 13) { //glass
                                                                     reflectivity = 0.5f;
                    } else if (blockType == 15 || (blockType == 1 && blockSubtype == 0) || blockType == 22) { //planks & steel
                                                                                                              reflectivity = 0.16f;
                    }
                }
            }
            normal = ivec3(mapPos - prevMapPos);
            if (voxelColor.a < 1.f) {
                //bubbles start
                if (isShadow && !raytracedCaustics) { //add check to not return early for semi-transparent blocks that contain solid voxels.
                                                      return vec4(0);
                }
                bool underwater = false;
                if (blockType == 1 && !renderingHand) {
                    if (getBlock(realPos.x, realPos.y+0.15f, realPos.z).x == 0) { //change to allow non-full water blocks to have caustics.
                                                                                  if (isCaustic(vec2(rayMapPos.x, rayMapPos.z)+(mapPos.xz/8))) {
                                                                                      if (checkerOn) {
                                                                                          voxelColor = fromLinear(vec4(0.9f, 1, 1, 1));
                                                                                      }
                                                                                      shouldReflect = -0.01f;
                                                                                  }
                    } else {
                        underwater = true;
                    }
                } else {
                    float samp = whiteNoise(((vec2(mapPos.x, mapPos.z)*128)+((renderingHand ? 3 : rayMapPos.y)*8)+mapPos.y)+(vec2((renderingHand ? 3 : rayMapPos.x), (renderingHand ? 3 : rayMapPos.z))*8));
                    if (samp > 0 && samp < 0.002) {
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
                    vec3 finalLight = vec3(lighting * ((0.7 - min(0.7, (lighting.a / 20) * mixedTime)) / 4)) + sunLight;
                    vec4 color = vec4(vec3(voxelColor) * min(vec3(1.15f), finalLight), 1);//light
                    //fog start
                    color = vec4(mix(vec3(color), unmixedFogColor, distanceFogginess), 1);
                    //fog end
                    tint += vec3(toLinear(color));
                }

                if (underwater) {
                    if (getBlock(realPos.x, int(realPos.y) + 1, realPos.z).x != 0) {
                        return vec4(0);
                    }
                }
            }
            if (voxelColor.a >= 1) {
                if (hitPos == vec3(256)) {
                    prevHitPos = renderingHand ? (camPos+(prevMapPos/8)) : prevPos;;
                    hitPos = renderingHand ? (camPos+(mapPos/8)) : realPos;
                }
                //face-based brightness start
                if (normal.y == 1) { //down
                                     normalBrightness = 0.7f;
                } else if (normal.y == -1) { //up
                                             normalBrightness = 1.f;
                } else if (normal.z == 1) { //south
                                            normalBrightness = 0.85f;
                } else if (normal.z == -1) { //north
                                             normalBrightness = 0.85f;
                } else if (normal.x == 1) { //west
                                            normalBrightness = 0.75f;
                } else if (normal.x == -1) { //east
                                             normalBrightness = 0.95f;
                }
                //face-based brightness end

                //snow start
                if (snowing) {
                    if (toLinear(vec4(lighting.a)).a >= 20) {
                        int aboveBlockType = blockType;
                        int aboveBlockSubtype = blockSubtype;
                        int aboveY = int(mapPos.y+1);
                        int aboveRayMapPosY = int(rayMapPos.y);
                        if (aboveY == 8) {
                            aboveY = 0;
                            aboveRayMapPosY++;
                            ivec2 aboveBlocKInfo = getBlock(int(rayMapPos.x), aboveRayMapPosY, int(rayMapPos.z));
                            aboveBlockType = aboveBlocKInfo.x;
                            aboveBlockSubtype = aboveBlocKInfo.y;
                        }
                        vec4 aboveColorData = getVoxel(mapPos.x, float(aboveY), mapPos.z, rayMapPos.x, aboveRayMapPosY, rayMapPos.z, aboveBlockType, aboveBlockSubtype, 0);
                        if (aboveColorData.a <= 0 || aboveBlockType == 4 || aboveBlockType == 5 || aboveBlockType == 18) {
                            voxelColor = mix(voxelColor, vec4(1-(abs(noise(vec2(int(mapPos.x)*64, int(mapPos.z)*64)))*0.66f))-vec4(0.14, 0.15, -0.05, 0)*1.33f, 0.9f);
                            normalBrightness = 1.f;
                            isSnowFlake = true;
                        }
                    }
                }
                //snow end

                if (!shadowsEnabled || !raytracedCaustics) {
                    if (prevBlock.x == 1 && prevBlock.y > 0) {
                        if (isCaustic(vec2(rayMapPos.x, rayMapPos.z) + (mapPos.xz / 8))) {
                            addFakeCaustics = true;
                        }
                    }
                }

                return vec4(vec3(voxelColor), 1);
            }
        }

        mask = stepMask(sideDist);
        mapPos += mask * raySign;
        prevMapPos = mapPos;
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

    return vec4(0);
}

vec4 dda(bool isShadow, float chunkDist, float subChunkDist, int condensedChunkPos, vec3 offset, vec3 rayPos, vec3 rayDir, vec3 iMask, bool inBounds, bool checkBlocks) {
    vec3 mapPos = floor(clamp(rayPos, vec3(0.0001), vec3(7.9999)));
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((mapPos-rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = iMask;
    rayMapPos = offset+mapPos;
    prevRayMapPos = rayMapPos;

    if (distance(rayMapPos, ogRayPos) > (isShadow ? 1000 : renderDistance)) {
        return vec4(-1);
    }

    while (mapPos.x < 8.0 && mapPos.x >= 0.0 && mapPos.y < 8.0 && mapPos.y >= 0.0 && mapPos.z < 8.0 && mapPos.z >= 0.0) {
        float adjustedTime = clamp(abs(1-clamp((distance(rayMapPos, sun-vec3(0, sun.y, 0))/1.33)/(size/1.5), 0, 1))*2, 0.05f, 1.f);
        float adjustedTimeCam = clamp(abs(1-clamp((distance(camPos, sun-vec3(0, sun.y, 0))/1.33)/(size/1.5), 0, 1))*2, 0.05f, 0.9f);
        float timeBonus = gradient(rayMapPos.y, 64.f, 372.f, 0.1f, 0.f);
        float mixedTime = (adjustedTime/2)+(adjustedTimeCam/2)+timeBonus;
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
                    vec4 color = traceBlock(isShadow, chunkDist, subChunkDist, blockDist, intersect, uv3d * 8.0, rayDir, mask, blockInfo.x, blockInfo.y, 1, vec3(0), mixedTime);
                    if (color.a >= 1.f) {
                        return color;
                    }
                }
            }
        } else if (checkBlocks) {
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
            vec3 lightPos = rayMapPos;
            float camDist = distance(camPos, rayMapPos)/renderDistance;
            float whiteness = gradient(rayMapPos.y, 64, 372, 0, 0.8);
            vec3 unmixedFogColor = mix(vec3(0.416, 0.495, 0.75), vec3(1), whiteness);
            if (blockInfo.x != 0.f) {
                float fogNoise = max(0, noise((vec2(hitPos.x, hitPos.z))+(floor(hitPos.y/16)+(float(time)*7500))))*gradient(hitPos.y, 63, 96, 0, 0.77);
                if (blockInfo.x != 1.f) {
                    fogNoise+=gradient(hitPos.y, 63.f, 96.f, 0.f, 1.f);
                }
                float linearDistFog = camDist+(fogNoise/2)+((fogNoise/2)*camDist);
                distanceFogginess = max(distanceFogginess, clamp((exp2(linearDistFog-0.75f)+min(0.f, linearDistFog-0.25f))-0.1f, 0.f, 1.f));
                float sunLight = (lighting.a/16)*(mixedTime-timeBonus);
                color = traceBlock(isShadow, chunkDist, subChunkDist, blocKDist, intersect, uv3d * 8.0, rayDir, mask, blockInfo.x, blockInfo.y, sunLight, unmixedFogColor, mixedTime);
                if ((blockInfo.x == 17 || blockInfo.x == 21) && color.a >= 1) {
                    color = vec4(hsv2rgb(rgb2hsv(vec3(color))-vec3(0, noise(vec2(rayMapPos.x, rayMapPos.z)*10), 0)), 1);
                }
                lightPos = prevPos;
            }

            //lighting start
            if (inBounds && color.a >= 1.f) {
                float lightNoise = max(0, noise((vec2(lightPos.x, lightPos.y)*64)+(float(time)*10000))+noise((vec2(lightPos.y, lightPos.z)*64)+(float(time)*10000))+noise((vec2(lightPos.z, lightPos.x)*64)+(float(time)*10000)));
                float sunlightNoise = max(0, noise((vec2(lightPos.x, lightPos.z)*16)+(float(time)*20000)) * ((blockInfo.x == 17 || blockInfo.x == 21) ? 2 : 1));

                vec3 relativePos = lightPos-rayMapPos; //smooth position here maybe
                vec4 centerLighting = getLighting(lightPos.x, lightPos.y, lightPos.z, true, true, true);
                //                lighting = centerLighting;
                //smooth lighting start
                vec4 verticalLighting = getLighting(lightPos.x, lightPos.y+(relativePos.y >= 0.5f ? 0.5f : -0.5f), lightPos.z, false, true, false);
                verticalLighting = mix(relativePos.y >= 0.5f ? centerLighting : verticalLighting, relativePos.y >= 0.5f ? verticalLighting : centerLighting, relativePos.y);
                vec4 northSouthLighting = getLighting(lightPos.x, lightPos.y, lightPos.z+(relativePos.z >= 0.5f ? 0.5f : -0.5f), false, false, true);
                northSouthLighting = mix(relativePos.z >= 0.5f ? centerLighting : northSouthLighting, relativePos.z >= 0.5f ? northSouthLighting : centerLighting, relativePos.z);
                vec4 eastWestLighting = getLighting(lightPos.x+(relativePos.x >= 0.5f ? 0.5f : -0.5f), lightPos.y, lightPos.z, true, false, false);
                eastWestLighting = mix(relativePos.x >= 0.5f ? centerLighting : eastWestLighting, relativePos.x >= 0.5f ? eastWestLighting : centerLighting, relativePos.x);
                lighting = mix(mix(mix(eastWestLighting, verticalLighting, 0.25), mix(northSouthLighting, verticalLighting, 0.25), 0.5), centerLighting, 0.5);
                //smooth lighting end
                //fake shadows start
                vec4 offsetLighting = getLighting(lightPos.x-normal.x, lightPos.y-normal.y, lightPos.z-normal.z, true, true, true);
                if (offsetLighting.r < centerLighting.r) {
                    lighting.r *= 0.8;
                }
                if (offsetLighting.g < centerLighting.g) {
                    lighting.g *= 0.8;
                }
                if (offsetLighting.b < centerLighting.b) {
                    lighting.b *= 0.8;
                }
                vec3 dir = normalize(lightPos - sun);
                lighting.a *= 1+(((dot(normal, dir)-1)/4)*(min(0.75, abs(1-mixedTime))+0.25));
                //fake shadows end
                lighting = fromLinear(lighting);
                lightFog = max(lightFog, lighting*(1-(vec4(0.5, 0.5, 0.5, 0)*vec4(lightNoise))));
                lighting *= 1+(vec4(0.5, 0.5, 0.5, snowing ? 0.1 : -0.25f)*vec4(lightNoise, lightNoise, lightNoise, sunlightNoise));
            }
            //lighting end

            //snow start
            if (snowing) {
                if (blockInfo.x == 0 && toLinear(vec4(lighting.a)).a >= 20) {
                    float samp = whiteNoise((vec2(rayMapPos.x, rayMapPos.z)*32)+(rayMapPos.y+(float(time)*7500)));
                    float samp2 = noise(vec2(rayMapPos.x, rayMapPos.z)*8);
                    if (samp > 0 && samp < 0.002 && samp2 > 0.0f && samp2 < 0.05f) {
                        color = vec4(1);
                        isSnowFlake = true;
                    }
                }
            }
            //snow end

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

vec4 subChunkDDA(bool isShadow, float chunkDist, int condensedChunkPos, vec3 offset, vec3 rayPos, vec3 rayDir, vec3 iMask, bool inBounds, bool checkSubChunks) {
    vec3 mapPos = floor(clamp(rayPos, vec3(0.0001), vec3(1.9999)));
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((mapPos-rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = iMask;
    vec3 realPos = offset+(mapPos*8);
    int subChunks = chunkBlocksData[(condensedChunkPos*5)+4];

    if (distance(realPos, ogRayPos) > (isShadow ? 1000 : renderDistance)) {
        return vec4(-1);
    }

    while (mapPos.x < 2.0 && mapPos.x >= 0.0 && mapPos.y < 2.0 && mapPos.y >= 0.0 && mapPos.z < 2.0 && mapPos.z >= 0.0) {
        if (renderingHand) {
            vec3 mini = ((mapPos-rayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
            float d = max (mini.x, max (mini.y, mini.z));
            vec3 intersect = rayPos + rayDir*d;
            vec3 uv3d = intersect - mapPos;
            float subChunkDist = max(mini.x, max(mini.y, mini.z));

            if (mapPos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
                                           uv3d = rayPos - mapPos;
            }

            vec4 color = dda(isShadow, chunkDist, subChunkDist, condensedChunkPos, realPos, uv3d * 8.0, rayDir, mask, true, true);

            if (color.a >= 1) {
                return color;
            }
        } else {
            bool checkBlocks = false;
            if (checkSubChunks) {
                ivec3 localPos = ivec3(realPos.x, realPos.y, realPos.z) & ivec3(15);
                int subChunkPos = ((((localPos.x >= subChunkSize  ? 1 : 0)*2)+(localPos.z >= subChunkSize  ? 1 : 0))*2)+(localPos.y >= subChunkSize  ? 1 : 0);
                checkBlocks = (((subChunks >> (subChunkPos % 32)) & 1) > 0);
            }
            if (checkBlocks) {
                vec3 mini = ((mapPos-rayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
                float subChunkDist = max(mini.x, max(mini.y, mini.z));
                vec3 intersect = rayPos + rayDir*subChunkDist;
                vec3 uv3d = intersect - mapPos;

                if (mapPos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
                                               uv3d = rayPos - mapPos;
                }

                vec4 color = dda(isShadow, chunkDist, subChunkDist, condensedChunkPos, realPos, uv3d * 8.0, rayDir, mask, inBounds, true);

                if (color.a >= 1) {
                    return color;
                } else if (color.a <= -1) {
                    return vec4(-1);
                }
            } else {
                //snow start
                if (snowing) {
                    if (toLinear(vec4(lighting.a)).a >= 20) {
                        float samp = whiteNoise((vec2(mapPos.x, mapPos.z)*32)+((mapPos.y)+(float(time)*7500)));
                        float samp2 = noise(vec2(mapPos.x, mapPos.z)*4);
                        if (samp > 0 && samp < 0.002 && samp2 > 0.0f && samp2 < 0.05f) {
                            isSnowFlake = true;
                            return vec4(1);
                        }
                    }
                }
                //snow end
            }
        }

        mask = stepMask(sideDist);
        mapPos += mask * raySign;
        realPos = offset+(mapPos*8);
        sideDist += mask * raySign * deltaDist;
    }

    return vec4(0);
}

vec4 traceWorld(bool isShadow, vec3 ogPos, vec3 rayDir) {
    ogRayPos = ogPos;
    vec3 rayPos = ogRayPos/16;
    vec3 rayMapChunkPos = floor(rayPos);
    vec3 prevRayMapPos = rayMapChunkPos;
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((rayMapChunkPos-rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = stepMask(sideDist);

    while (distance(rayMapChunkPos, rayPos) < (renderingHand ? 16 : ((isShadow ? 1000 : renderDistance)/chunkSize))) {
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
            vec4 color = subChunkDDA(isShadow, chunkDist, (((chunkPos.x*sizeChunks)+chunkPos.z)*heightChunks)+chunkPos.y, chunkPos*16, uv3d * 2.0, rayDir, mask, true, true);
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
                vec3 mini = ((rayMapChunkPos-rayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
                float chunkDist = max(mini.x, max(mini.y, mini.z));
                vec3 intersect = rayPos + rayDir*chunkDist;
                vec3 uv3d = intersect - rayMapChunkPos;

                if (rayMapChunkPos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
                                                       uv3d = rayPos - rayMapChunkPos;
                }
                ivec3 chunkPos = ivec3(rayMapChunkPos);
                vec4 color = subChunkDDA(isShadow, chunkDist, (((chunkPos.x*sizeChunks)+chunkPos.z)*heightChunks)+chunkPos.y, chunkPos*16, uv3d * 2.0, rayDir, mask, inBounds, checkSubChunks);
                if (color.a >= 1) {
                    return color;
                } else if (color.a <= -1) {
                    prevHitPos = rayMapChunkPos*16;
                    hitPos = rayMapChunkPos*16;
                    return vec4(1, 0, 0, 0);
                }
            } else {
                //snow start
                if (snowing) {
                    if (toLinear(vec4(lighting.a)).a >= 20) {
                        float samp = whiteNoise((vec2(rayMapChunkPos.x, rayMapChunkPos.z)*64)+((rayMapChunkPos.y)+(float(time)*7500)));
                        float samp2 = noise(vec2(rayMapChunkPos.x, rayMapChunkPos.z)*8);
                        if (samp > 0 && samp < 0.002 && samp2 > 0.0f && samp2 < 0.05f) {
                            isSnowFlake = true;
                            return vec4(1);
                        }
                    }
                }
                //snow end
            }
        }

        mask = stepMask(sideDist);
        prevRayMapPos = rayMapChunkPos;
        rayMapChunkPos += mask * raySign;
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
    if (!renderingHand && !isShadow) {
        float sunDir = dot(normalize(sun - camPos), rayDir);
        if (sunDir > 0.95f && sunDir < 1) {
            lighting = fromLinear(vec4(0, 0, 0, 20));
            lightFog = max(lightFog, lighting);
            hitSun = true;
            return vec4(1, 1, 0, 1);
        }
        float lunaDir = dot(normalize((((sun-vec3(size/2, 0, size/2))*vec3(-1, 1, -1))+vec3(size/2, 0, size/2)) - camPos), rayDir);
        if (lunaDir > 0.953f && lunaDir < 1) {
            lighting = fromLinear(vec4(5, 5, 5, 0));
            lightFog = max(lightFog, lighting);
            return vec4(0.95f, 0.95f, 1, 1);
        }
    }
    return vec4(0);
}

bool isSky = false;

vec3 prevReflectPos = vec3(0);
vec3 reflectPos = vec3(0);

void clearVars(bool clearHit, bool clearTint) {
    firstVoxel = true;
    wasEverTinted = false;
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
    normalBrightness = 1.f;
}

vec4 prevFog = vec4(1);
vec4 prevSuperFinalTint = vec4(1);

vec4 raytrace(vec3 ogRayPos, vec3 dir, bool checkShadow) {
    clearVars(true, false);
    vec4 color = traceWorld(false, ogRayPos, dir);
    if (renderingHand) {
        prevPos = prevHitPos;
    }
    prevReflectPos = prevHitPos;
    reflectPos = hitPos;
    isSky = renderingHand ? false : color.a < 1.f;
    float borders = max(gradient(hitPos.x, 0, 128, 0, 1), max(gradient(hitPos.z, 0, 128, 0, 1), max(gradient(hitPos.x, size, size-128, 0, 1), gradient(hitPos.z, size, size-128, 0, 1))));
    if (isSky) {
        lighting = fromLinear(borders > 0.f ? mix(vec4(0, 0, 0, 20), getLighting(hitPos.x, hitPos.y, hitPos.z, false, false, false), borders) : vec4(0, 0, 0, 20));
        lightFog = lighting;
    }
    float adjustedTime = clamp(abs(1-clamp((distance(hitPos, sun-vec3(0, sun.y, 0))/1.33)/(size/1.5), 0, 1))*2, 0.05f, 1.f);
    float adjustedTimeCam = clamp(abs(1-clamp((distance(camPos, sun-vec3(0, sun.y, 0))/1.33)/(size/1.5), 0, 1))*2, 0.05f, 0.9f);
    float timeBonus = gradient(hitPos.y, 64.f, 372.f, 0.1f, 0.f);
    float mixedTime = (adjustedTime/2)+(adjustedTimeCam/2)+timeBonus;
    float fogNoise = (max(0, noise((vec2(hitPos.x, hitPos.z))+(floor(hitPos.y/16)+(float(time)*7500))))*gradient(hitPos.y, 63, 96, 0, 0.77))+gradient(hitPos.y, 63, 96, 0, 0.33f);
    float fogDist = distance(camPos, prevPos)/renderDistance;
    float linearDistFog = fogDist+(fogNoise/2)+((fogNoise/2)*fogDist);
    //distanceFogginess = max(distanceFogginess, exp2(linearDistFog-0.75f)+min(0, linearDistFog-0.25f));
    if (isSky) {
        distanceFogginess = 1;
    }
    distanceFogginess = clamp(distanceFogginess, 0.f, 1.f);
    if (renderingHand) {
        lighting = fromLinear(getLighting(camPos.x, camPos.y, camPos.z, true, true, true));
        lightFog = max(lightFog, lighting*(1-vec4(0.5, 0.5, 0.5, 0)));
    } else {
        distanceFogginess = max(distanceFogginess, borders);
        float factor = pow(distanceFogginess, 4);
        lighting = mix(lighting, vec4(0, 0, 0, 20), factor);
        lightFog = mix(lightFog, vec4(0, 0, 0, 20), factor);
    }
    lighting = pow(lighting/20, vec4(2.f))*vec4(200, 200, 200, 18.5f);
    lightFog = pow(lightFog/20, vec4(2.f))*vec4(200, 200, 200, 18.5f);
    float sunLight = (lighting.a/16)*(mixedTime-timeBonus);
    float whiteness = gradient(hitPos.y, 64, 372, 0, 0.8);
    vec3 sunColor = mix(mix(vec3(0.0f, 0.0f, 4.5f), vec3(2.125f, 0.875f, 0.125f), mixedTime*4), vec3(0.1f, 0.95f, 1.5f), mixedTime) * 0.15f;

    vec3 finalRayMapPos = rayMapPos;
    vec4 finalLighting = vec4(lighting.r*normalBrightness, lighting.g*normalBrightness, lighting.b*normalBrightness, lighting.a);
    float sunBrightness = (lightFog.a/12)*mixedTime;
    vec3 finalLightFog = max(vec3(lightFog)*((0.7-min(0.7, (lightFog.a/20)*mixedTime))/4), ((sunColor*mix(sunColor.r*6, 0.2, max(sunColor.r, max(sunColor.g, sunColor.b))))*sunBrightness));
    vec3 finalTint = max(vec3(1), tint);


    vec3 cloudPos = (vec3(cam * vec4(uvDir * 500.f, 1.f))-camPos);
    if (isSnowFlake) {
        whiteness = max(whiteness+0.5f, 1.f);
    } else if ((isSky || borders > 0.f) && cloudsEnabled) {
        whiteness = whiteness+((sqrt(max(0, noise((vec2(cloudPos.x, cloudPos.y*1.5f))+(float(time)*cloudSpeed))+noise((vec2(cloudPos.y*1.5f, cloudPos.z))+(float(time)*cloudSpeed))+noise((vec2(cloudPos.z, cloudPos.x))+(float(time)*cloudSpeed)))*gradient(hitPos.y, 0, 372, 1.5f, 0))*gradient(hitPos.y, 0, 372, 1, 0)) * (isSky ? 1.f : pow(borders, 32)));
    }
    vec3 unmixedFogColor = mix(vec3(0.416, 0.495, 0.75), vec3(1), whiteness)*min(1, 0.05f+sunLight);

    if (addFakeCaustics) {
        float factor = max(1, 2.5-distanceFogginess)*(1+(max(0, abs(1-mixedTime)-0.9)*10));
        color += (sunLight*(factor/25));
    }
    if (!isSky && !isSnowFlake && checkShadow && shadowsEnabled && !hitSun) {
        if (color.a >= 1.f && sunLight > 0.f) {
            vec3 shadowPos = prevPos;
            vec3 sunDir = normalize(sun - shadowPos);
            if (sunDir.x == 0) {
                sunDir.x = 0.00001f;
            }
            if (sunDir.y == 0) {
                sunDir.y = 0.00001f;
            }
            if (sunDir.z  == 0) {
                sunDir.z = 0.00001f;
            }
            clearVars(true, true);
            float oldReflectivity = reflectivity;
            reflectivity = 0.f;
            float factor = max(1, 2.5-distanceFogginess)*(1+(max(0, abs(1-mixedTime)-0.9)*10));;
            if (traceWorld(true, shadowPos, sunDir).a >= 1.f) {
                sunLight /= factor;
            }
            if (raytracedCaustics && reflectivity < 0.f) {
                color += (sunLight*(factor/20));
            }
            reflectivity = oldReflectivity;
            tint = max(vec3(1), tint);
            color = color*fromLinear(vec4(vec3(tint)/max(tint.r, max(tint.g, tint.b)), 1));//sun tint
        }
    }
    vec3 finalLight = vec3(finalLighting*((0.7-min(0.7, (finalLighting.a/20)*mixedTime))/4))+sunLight;
    color = vec4(vec3(color)*min(vec3(1.15f), finalLight), color.a);//light
    //fog start
    if (getBlock(camPos).x == 1) {
        unmixedFogColor += (vec3(-0.5f, -0.5f, 0.5f)*min(1, distanceFogginess*10));
    }
    color = vec4(mix(vec3(color), unmixedFogColor, distanceFogginess), color.a);
    vec4 fog = 1+(isSnowFlake ? vec4(finalLight/5, 0) : vec4(vec3(finalLightFog), 1));
    color *= fog;
    color *= prevFog;
    prevFog = fog;

    //fog end
    //transparency start
    vec4 superFinalTint = fromLinear(vec4(vec3(finalTint)/(max(finalTint.r, max(finalTint.g, finalTint.b))), 1));
    color *= superFinalTint;
    color *= prevSuperFinalTint;
    prevSuperFinalTint = superFinalTint;
    //transparency end

    if (renderingHand) {
        color = vec4(mix(vec3(color), unmixedFogColor/4.f, reflectivity), color.a);
    }

    //selection start
    if (ui && !renderingHand && selected == ivec3(finalRayMapPos)) {
        color = vec4(mix(vec3(color), vec3(0.7, 0.7, 1), 0.5f), color.a);
    }
    //selection end
    return color;
}

#extension GL_ARB_gpu_shader_int64 : enable
#extension GL_ARB_shader_clock : require

void main() {
    uint64_t startTime = clockARB();

    ivec2 res = imageSize(scene_image);
    ivec2 pixel = ivec2(gl_FragCoord.xy);

    checkerOn = checker(pixel);
    vec2 uv = (vec2(pixel)*2. - res.xy) / res.y;
    uvDir = normalize(vec3(uv, 1));
    vec3 ogDir = vec3(cam * vec4(uvDir, 0));
    vec4 handColor = raytrace(vec3(0, 0, 0), uvDir, true);
    shift = 0;
    renderingHand = false;
    if (handColor.a < 1) {
        prevFog = vec4(1);
        prevSuperFinalTint = vec4(1);
        distanceFogginess = 0;
        fragColor = raytrace(camPos, ogDir, true);
    } else {
        fragColor = handColor;
    }

    //reflections start
    if (!isSky && !isSnowFlake && reflectivity > 0.f && handColor.a < 1) {
        vec3 reflectDir = reflect(ogDir, normalize(reflectPos - prevReflectPos));
        fragColor = mix(fragColor, raytrace(reflectPos, reflectDir, false), reflectivity * (max(reflectivity * 0.5f, dot(normalize(ogDir), normalize(reflectDir)))));
    }
    //reflections end

    fragColor = toLinear(fragColor);
    //fragColor = mix(fragColor, vec4(float(clockARB() - startTime) * 0.0000005, 0.0, 1.0, 1.0), 0.95f);
    //imageStore(scene_image, pixel, fragColor);
}