#version 460

uniform vec2 res;
uniform mat4 cam;
uniform int renderDistance;
uniform float timeOfDay;
uniform double time;
uniform ivec3 selected;
uniform bool ui;
uniform vec3 sun;

layout(binding = 0) uniform sampler2D coherent_noise;
layout(binding = 1) uniform sampler2D white_noise;

layout(std430, binding = 0) buffer atlasSSBO
{
    int[] atlasData;
};
layout(std430, binding = 1) buffer chunkBlocksSSBO
{
    ivec4[] chunkBlocksData;
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
in vec4 gl_FragCoord;

out vec4 fragColor;

int size = 2048;
int height = 320;
int chunkSize = 16;
int sizeChunks = size/chunkSize;
int heightChunks = height/chunkSize;

vec3 camPos = vec3(cam[3]);
vec3 prevRayMapPos = vec3(0);
vec3 rayMapPos = vec3(0);
vec3 lightPos = vec3(0);
vec4 lighting = vec4(0);
vec4 lightFog = vec4(0);
vec3 hitNorm = vec3(0);
vec3 prevHitPos = vec3(256);
vec3 hitPos = vec3(256);
vec4 sunTint = vec4(1, 1, 1, 0);
vec4 tint = vec4(1, 1, 1, 0);
float reflectivity = 0.f;

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
    return (texture(coherent_noise, coords/1024).r)-0.5;
}
float whiteNoise(vec2 coords) {
    return (texture(white_noise, coords/1024).r)-0.5;
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
    if (chunkPos != prevCornerChunkPos) {
        prevCornerChunkPos = chunkPos;
        int condensedChunkPos = (((chunkPos.x*sizeChunks)+chunkPos.z)*heightChunks)+chunkPos.y;
        cornerPaletteInfo = chunkCornersData[condensedChunkPos];
        cornerValuesPerInt = 32/cornerPaletteInfo.z;
    }
    ivec3 localPos  = ivec3(x, y, z) & ivec3(15);
    int condensedLocalPos = ((((localPos.x*chunkSize)+localPos.z)*chunkSize)+localPos.y);
    int intIndex  = condensedLocalPos/cornerValuesPerInt;
    int bitIndex = (condensedLocalPos - intIndex * cornerValuesPerInt) * cornerPaletteInfo.z;
    int key = (cornersData[cornerPaletteInfo.x+cornerPaletteInfo.y+intIndex] >> bitIndex) & cornerPaletteInfo.w;
    return cornersData[cornerPaletteInfo.x+key];
}

bool[8] getCorners(int x, int y, int z) {
    int cornerData = getCornerData(x, y, z);
    bool[8] data = bool[8](0);
    for (int bit = 0; bit < 8; bit++) {
        data[bit] = ((cornerData & (1 << (bit - 1))) >> (bit - 1)) == 0;
    }
    return data;
}
vec4 getVoxel(int x, int y, int z, int bX, int bY, int bZ, int blockType, int blockSubtype) {
    bool[8] corners = getCorners(bX, bY, bZ);
    int cornerIndex = (y < 4 ? 0 : 4) + (z < 4 ? 0 : 2) + (x < 4 ? 0 : 1);
    if (corners[cornerIndex]) {
        return fromLinear(intToColor(atlasData[(9984*((blockType*8)+x)) + (blockSubtype*64) + ((abs(y-8)-1)*8) + z])/255);
    } else {
        return vec4(0, 0, 0, 0);
    }
}
vec4 getVoxel(float x, float y, float z, float bX, float bY, float bZ, int blockType, int blockSubtype) {
    return getVoxel(int(x), int(y), int(z), int(bX), int(bY), int(bZ), blockType, blockSubtype);
}

ivec3 prevBlockChunkPos = ivec3(-1);
ivec4 blockPaletteInfo = ivec4(-1);
int blockValuesPerInt = -1;
int getBlockData(int x, int y, int z) {
    ivec3 chunkPos = ivec3(x, y, z) >> 4;
    if (chunkPos != prevBlockChunkPos) {
        prevBlockChunkPos = chunkPos;
        int condensedChunkPos = (((chunkPos.x*sizeChunks)+chunkPos.z)*heightChunks)+chunkPos.y;
        blockPaletteInfo = chunkBlocksData[condensedChunkPos];
        blockValuesPerInt = 32/blockPaletteInfo.z;
    }
    ivec3 localPos = ivec3(x, y, z) & ivec3(15);
    int condensedLocalPos = ((((localPos.x*chunkSize)+localPos.z)*chunkSize)+localPos.y);
    int intIndex  = condensedLocalPos/blockValuesPerInt;
    int bitIndex = (condensedLocalPos - intIndex * blockValuesPerInt) * blockPaletteInfo.z;
    int key = (blocksData[blockPaletteInfo.x+blockPaletteInfo.y+intIndex] >> bitIndex) & blockPaletteInfo.w;
    return blocksData[blockPaletteInfo.x+key];
}
ivec2 getBlock(int x, int y, int z) {
    int blockData = getBlockData(x, y, z);
    return ivec2((blockData >> 16) & 0xFFFF, blockData & 0xFFFF);
}
ivec2 getBlock(float x, float y, float z) {
    return getBlock(int(x), int(y), int(z));
}
ivec2 getBlock(vec3 pos) {
    return getBlock(int(pos.x), int(pos.y), int(pos.z));
}

bool isBlockSolid(ivec2 block) {
    return (block.x != 0 && block.x != 1 && block.x != 4 && block.x != 5 && block.x != 6 && block.x != 7 && block.x != 8 && block.x != 9 && block.x != 11 && block.x != 12 && block.x != 13 && block.x != 14 && block.x != 17);
}

ivec3 prevLightChunkPos = ivec3(-1);
ivec4 lightPaletteInfo = ivec4(-1);
int lightValuesPerInt = -1;
int getLightData(int x, int y, int z) {
    ivec3 chunkPos = ivec3(x, y, z) >> 4;
    if (chunkPos != prevLightChunkPos) {
        prevLightChunkPos = chunkPos;
        int condensedChunkPos = (((chunkPos.x*sizeChunks)+chunkPos.z)*heightChunks)+chunkPos.y;
        lightPaletteInfo = chunkLightsData[condensedChunkPos];
        lightValuesPerInt = 32/lightPaletteInfo.z;
    }
    ivec3 localPos = ivec3(x, y, z) & ivec3(15);
    int condensedLocalPos = ((((localPos.x*chunkSize)+localPos.z)*chunkSize)+localPos.y);
    int intIndex  = condensedLocalPos/lightValuesPerInt;
    int bitIndex = (condensedLocalPos - intIndex * lightValuesPerInt) * lightPaletteInfo.z;
    int key = (lightsData[lightPaletteInfo.x+lightPaletteInfo.y+intIndex] >> bitIndex) & lightPaletteInfo.w;
    return lightsData[lightPaletteInfo.x+key];
}

vec4 getLighting(float x, float y, float z, bool shiftedX, bool shiftedY, bool shiftedZ) {
    int intX = int(x);
    int intY = int(y);
    int intZ = int(z);
    ivec2 block = getBlock(intX, intY, intZ);
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
        } else if (shiftedZ) {
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
        } else if (shiftedX) {
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
    vec4 light = intToColor(getLightData(intX, intY, intZ));
    if (block.x == 17) {
        light = light/1.1;
    }
    return fromLinear(max(vec4(0.5, 0.5, 0.5, 0), light));
}

bool isChunkAir(int x, int y, int z) {
    if (x >= 0 && x < sizeChunks && y >= 0 && y < heightChunks && z >= 0 && z < sizeChunks) {
        int condensedChunkPos = (((x*sizeChunks)+z)*heightChunks)+y;
        int paletteSize = chunkBlocksData[condensedChunkPos].y;
        if (paletteSize == 1) {
            return true;
        } else {
            return false;
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

bool traceBlockMinimal(vec3 blockPos, vec3 rayPos, vec3 rayDir, vec3 iMask, int blockType, int blockSubtype) {
    rayPos = clamp(rayPos, vec3(0.0001), vec3(7.9999));
    vec3 mapPos = floor(rayPos);
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((mapPos - rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = iMask;

    vec4 prevVoxelColor = vec4(1, 1, 1, 0);
    while (mapPos.x < 8.0 && mapPos.x >= 0.0 && mapPos.y < 8.0 && mapPos.y >= 0.0 && mapPos.z < 8.0 && mapPos.z >= 0.0) {
        vec4 voxelColor = getVoxel(mapPos.x, mapPos.y, mapPos.z, blockPos.x, blockPos.y, blockPos.z, blockType, blockSubtype);
        if (voxelColor.a >= 1) {
            return false;
        } else if (voxelColor.a > 0) {
            sunTint = vec4(min(vec3(sunTint), vec3(prevVoxelColor)/0.5), max(0.5, max(sunTint.a, prevVoxelColor.a)));
            sunTint = vec4(vec3(sunTint)*vec3(voxelColor), sunTint.a+(voxelColor.a/25));
        }

        prevVoxelColor = voxelColor;
        mask = stepMask(sideDist);
        mapPos += mask * raySign;
        sideDist += mask * raySign * deltaDist;
    }

    return true;
}

bool sunDDA(ivec3 chunkPos, vec3 rayPos, vec3 rayDir, vec3 iMask) {
    rayPos = clamp(rayPos, vec3(0.0001), vec3(15.9999));
    vec3 mapPos = floor(rayPos);
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((mapPos-rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = iMask;
    vec3 realMapPos = (chunkPos*16)+mapPos;

    while (mapPos.x < 16.0 && mapPos.x >= 0.0 && mapPos.y < 16.0 && mapPos.y >= 0.0 && mapPos.z < 16.0 && mapPos.z >= 0.0) {
        mask = stepMask(sideDist);
        mapPos += mask * raySign;
        realMapPos = (chunkPos*16)+mapPos;
        sideDist += mask * raySign * deltaDist;

        //block start
        ivec2 blockInfo = getBlock(realMapPos);

        vec3 mini = ((mapPos-rayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
        float d = max (mini.x, max (mini.y, mini.z));
        vec3 intersect = rayPos + rayDir*d;
        vec3 uv3d = intersect - mapPos;

        if (mapPos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
            uv3d = rayPos - mapPos;
        }
        //block end

        if (blockInfo.x != 0) {
            if (!traceBlockMinimal(realMapPos, uv3d * 8.0, rayDir, mask, blockInfo.x, blockInfo.y)) {
                return false;
            }
        }
    }

    return true;
}

bool traceSun(vec3 ogRayPos, vec3 rayDir) {
    vec3 rayPos = ogRayPos/16;
    vec3 rayMapChunkPos = floor(rayPos);
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((rayMapChunkPos-rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = stepMask(sideDist);

    for (int i = 0; i < heightChunks; i++) {
        if (rayMapChunkPos.x >= 0 && rayMapChunkPos.x < sizeChunks-1 && rayMapChunkPos.y >= 0 && rayMapChunkPos.y < heightChunks-1 && rayMapChunkPos.z >= 0 && rayMapChunkPos.z < sizeChunks-1) {
            if (!isChunkAir(int(rayMapChunkPos.x), int(rayMapChunkPos.y), int(rayMapChunkPos.z))) {
                vec3 mini = ((rayMapChunkPos-rayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
                float d = max (mini.x, max (mini.y, mini.z));
                vec3 intersect = rayPos + rayDir*d;
                vec3 uv3d = intersect - rayMapChunkPos;

                if (rayMapChunkPos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
                    uv3d = rayPos - rayMapChunkPos;
                }
                if (!sunDDA(ivec3(rayMapChunkPos), uv3d * 16.0, rayDir, mask)) {
                    return false;
                }
            }
        } else {
            return true;
        }

        mask = stepMask(sideDist);
        rayMapChunkPos += mask * raySign;
        sideDist += mask * raySign * deltaDist;
    }
    return true;
}

vec4 traceBlock(vec3 rayPos, vec3 rayDir, vec3 iMask, int blockType, int blockSubtype) {
    rayPos = clamp(rayPos, vec3(0.0001), vec3(7.9999));
    vec3 mapPos = floor(rayPos);
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((mapPos - rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = iMask;

    vec3 prevMapPos = mapPos+(stepMask(sideDist+(mask*(-raySign)*deltaDist))*(-raySign));

    vec4 prevVoxelColor = vec4(1, 1, 1, 0);
    while (mapPos.x < 8.0 && mapPos.x >= 0.0 && mapPos.y < 8.0 && mapPos.y >= 0.0 && mapPos.z < 8.0 && mapPos.z >= 0.0) {
        vec4 voxelColor = getVoxel(mapPos.x, mapPos.y, mapPos.z, rayMapPos.x, rayMapPos.y, rayMapPos.z, blockType, blockSubtype);
        if (voxelColor.a > 0.f) {
            if (reflectivity == 0.f) {
                if (blockType == 1) { //water
                    reflectivity = 0.5f;
                } else if (blockType == 7) { //kyanite
                    reflectivity = 0.33f;
                } else if (blockType >= 8 && blockType <= 10) { //stones
                    reflectivity = 0.05f;
                } else if (blockType >= 11 && blockType <= 13) { //glass
                    reflectivity = 0.25f;
                } else if (blockType == 15) { //planks
                    reflectivity = 0.08f;
                }
            }
            if (voxelColor.a < 1.f) {
                if (hitPos == vec3(256)) {
                    prevHitPos = prevRayMapPos+(rayPos/8.f);
                    hitPos = rayMapPos+(rayPos/8.f);
                    hitNorm = vec3(mapPos - prevMapPos);
                }
                //bubbles start
                if (blockType == 1) {
                    float samp = whiteNoise(((vec2(mapPos.x, mapPos.z)*128)+((rayMapPos.y*8)+mapPos.y+(float(time)*10000)))+(vec2(rayMapPos.x, rayMapPos.z)*8));
                    if (samp > 0 && samp < 0.002) {
                        voxelColor = vec4(1, 1, 1, 1);
                    }
                } else {
                    float samp = whiteNoise(((vec2(mapPos.x, mapPos.z)*128)+(rayMapPos.y*8)+mapPos.y)+(vec2(rayMapPos.x, rayMapPos.z)*8));
                    if (samp > 0 && samp < 0.002) {
                        voxelColor = vec4(1, 1, 1, 1);
                    }
                }
                //bubbles end
                tint = vec4(min(vec3(tint), vec3(prevVoxelColor)/0.5), max(0.5, max(tint.a, prevVoxelColor.a)));
                tint = vec4(vec3(tint)*vec3(voxelColor), tint.a+(voxelColor.a/25));
            }
            if (voxelColor.a >= 1) {
                if (hitPos == vec3(256)) {
                    prevHitPos = prevRayMapPos+(rayPos/8.f);
                    hitPos = rayMapPos+(rayPos/8.f);
                    hitNorm = vec3(mapPos - prevMapPos);
                }
                //face-based brightness start
                float brightness = 1.f;
                ivec3 normal = ivec3(mapPos - prevMapPos);
                if (normal.y == 1) { //down
                    brightness = 0.7f;
                } else if (normal.y == -1) { //up
                    brightness = 1.f;
                } else if (normal.z == 1) { //south
                    brightness = 0.85f;
                } else if (normal.z == -1) { //north
                    brightness = 0.85f;
                } else if (normal.x == 1) { //west
                    brightness = 0.75f;
                } else if (normal.x == -1) { //east
                    brightness = 0.95f;
                }
                //face-based brightness end

                lightPos = (prevMapPos/8.f)+rayMapPos;

                //snow start
                //            int aboveBlockType = blockType;
                //            int aboveBlockSubtype = blockSubtype;
                //            int aboveY = int(mapPos.y+1);
                //            if (aboveY == 8) {
                //                aboveY = 0;
                //                ivec2 aboveBlocKInfo = getBlock(int(rayMapPos.x), int(rayMapPos.y+1), int(rayMapPos.z));
                //                aboveBlockType = aboveBlocKInfo.x;
                //                aboveBlockSubtype = aboveBlocKInfo.y;
                //            }
                //            vec4 aboveColorData = getVoxel(mapPos.x, float(aboveY), mapPos.z, rayMapPos.x, rayMapPos.y, rayMapPos.z, aboveBlockType, aboveBlockSubtype);
                //            if (aboveColorData.a <= 0 || aboveBlockType == 4 || aboveBlockType == 5) {
                //                voxelColor = mix(voxelColor, vec4(1-(abs(noise(vec2(int(mapPos.x)*64, int(mapPos.z)*64)))*0.66f))-vec4(0.14, 0.15, -0.05, 0), 0.9f);
                //                brightness = 1f;
                //            }
                //snow end

                return vec4(vec3(voxelColor)*brightness, 1);
            }
        }

        prevVoxelColor = voxelColor;
        prevMapPos = mapPos;
        mask = stepMask(sideDist);
        mapPos += mask * raySign;
        sideDist += mask * raySign * deltaDist;
    }

    return vec4(0.0);
}

vec4 dda(ivec3 chunkPos, vec3 rayPos, vec3 rayDir, vec3 iMask) {
    rayPos = clamp(rayPos, vec3(0.0001), vec3(15.9999));
    vec3 mapPos = floor(rayPos);
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((mapPos-rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = iMask;
    rayMapPos = (chunkPos*16)+mapPos;
    prevRayMapPos = rayMapPos;

    while (mapPos.x < 16.0 && mapPos.x >= 0.0 && mapPos.y < 16.0 && mapPos.y >= 0.0 && mapPos.z < 16.0 && mapPos.z >= 0.0) {
        //block start
        ivec2 blockInfo = getBlock(rayMapPos);

        vec3 mini = ((mapPos-rayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
        float d = max (mini.x, max (mini.y, mini.z));
        vec3 intersect = rayPos + rayDir*d;
        vec3 uv3d = intersect - mapPos;

        if (mapPos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
            uv3d = rayPos - mapPos;
        }
        //block end

        vec4 color = vec4(0);
        if (blockInfo.x != 0.f) {
            color = traceBlock(uv3d * 8.0, rayDir, mask, blockInfo.x, blockInfo.y);
        }

        //lighting start
        vec3 relativePos = lightPos-rayMapPos;
        vec3 centerPos = lightPos.x == 0 && lightPos.y == 0 && lightPos.z == 0 ? rayMapPos : lightPos;
        vec4 centerLighting = getLighting(centerPos.x, centerPos.y, centerPos.z, false, false, false);
        if (!(lightPos.x == 0 && lightPos.y == 0 && lightPos.z == 0)) {
            //smooth lighting start
            vec4 verticalLighting = getLighting(lightPos.x, lightPos.y+(relativePos.y >= 0.5f ? 0.5f : -0.5f), lightPos.z, false, true, false);
            verticalLighting = mix(relativePos.y >= 0.5f ? centerLighting : verticalLighting, relativePos.y >= 0.5f ? verticalLighting : centerLighting, relativePos.y);
            vec4 northSouthLighting = getLighting(lightPos.x, lightPos.y, lightPos.z+(relativePos.z >= 0.5f ? 0.5f : -0.5f), false, false, true);
            northSouthLighting = mix(relativePos.z >= 0.5f ? centerLighting : northSouthLighting, relativePos.z >= 0.5f ? northSouthLighting : centerLighting, relativePos.z);
            vec4 eastWestLighting = getLighting(lightPos.x+(relativePos.x >= 0.5f ? 0.5f : -0.5f), lightPos.y, lightPos.z, true, false, false);
            eastWestLighting = mix(relativePos.x >= 0.5f ? centerLighting : eastWestLighting, relativePos.x >= 0.5f ? eastWestLighting : centerLighting, relativePos.x);
            lighting = mix(mix(mix(eastWestLighting, verticalLighting, 0.25), mix(northSouthLighting, verticalLighting, 0.25), 0.5), centerLighting, distance(rayPos, lightPos)/renderDistance);
            //smooth lighting end
        } else {
            lighting = centerLighting;
        }
        lightFog = vec4(max(lightFog.r, lighting.r/2), max(lightFog.g, lighting.g/2), max(lightFog.b, lighting.b/2), max(lightFog.a, lighting.a/2));
        //lighting end

        //snow start
//        if (blockInfo.x == 0 && lighting.a == 20) {
//            float samp = whiteNoise((vec2(mapPos.x, mapPos.z)*64)+(mapPos.y+(float(time)*7500)));
//            float samp2 = noise(vec2(mapPos.x, mapPos.z)*8);
//            if (samp > 0 && samp < 0.002 && samp2 > 0.0f && samp2 < 0.05f) {
//                color = vec4(1, 1, 1, 1);
//            }
//        }
        //snow end

        if (color.a >= 1) {
            return color;
        }

        mask = stepMask(sideDist);
        mapPos += mask * raySign;
        prevRayMapPos = rayMapPos;
        rayMapPos = (chunkPos*16)+mapPos;
        sideDist += mask * raySign * deltaDist;
    }

    return vec4(0);
}

int halfChunkSize = chunkSize/2;

vec4 traceWorld(vec3 ogRayPos, vec3 rayDir) {
    vec3 rayPos = ogRayPos/16;
    vec3 rayMapChunkPos = floor(rayPos);
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((rayMapChunkPos-rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = stepMask(sideDist);

    while (distance(rayMapChunkPos, rayPos) < renderDistance/16) {
        if (rayMapChunkPos.x >= 0 && rayMapChunkPos.x < sizeChunks-1 && rayMapChunkPos.y >= 0 && rayMapChunkPos.y < heightChunks-1 && rayMapChunkPos.z >= 0 && rayMapChunkPos.z < sizeChunks-1) {
            if (!isChunkAir(int(rayMapChunkPos.x), int(rayMapChunkPos.y), int(rayMapChunkPos.z))) {
                vec3 mini = ((rayMapChunkPos-rayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
                float d = max (mini.x, max (mini.y, mini.z));
                vec3 intersect = rayPos + rayDir*d;
                vec3 uv3d = intersect - rayMapChunkPos;

                if (rayMapChunkPos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
                    uv3d = rayPos - rayMapChunkPos;
                }
                vec4 color = dda(ivec3(rayMapChunkPos), uv3d * 16.0, rayDir, mask);
                if (color.a >= 1) {
                    return color;
                }
            } else {
                lighting = getLighting((rayMapChunkPos.x*chunkSize)+halfChunkSize, (rayMapChunkPos.y*chunkSize)+halfChunkSize, (rayMapChunkPos.z*chunkSize)+halfChunkSize, false, false, false);
                lightFog = vec4(max(lightFog.r, lighting.r/2), max(lightFog.g, lighting.g/2), max(lightFog.b, lighting.b/2), max(lightFog.a, lighting.a/2));
            }
        }

        mask = stepMask(sideDist);
        rayMapChunkPos += mask * raySign;
        sideDist += mask * raySign * deltaDist;
    }
    return vec4(0);
}

void main()
{
    vec2 uv = (vec2(gl_FragCoord)*2. - res.xy) / res.y;
    if (ui && uv.x >= -0.004 && uv.x <= 0.004 && uv.y >= -0.004385 && uv.y <= 0.004385) {
        fragColor = vec4(0.9, 0.9, 1, 1);
    } else {
        vec3 dir = normalize(vec3(uv, 1));
        vec3 ogDir = vec3(cam*vec4(dir, 0));
        fragColor = traceWorld(camPos, ogDir);
        bool isSky = hitPos == vec3(256);
        if (isSky) {
            hitPos = vec3(cam * vec4((dir * renderDistance), 1));
        }
        vec3 finalPrevHitPos = prevHitPos;
        prevHitPos = vec3(256);
        vec3 finalHitPos = hitPos;
        hitPos = vec3(256);
        vec3 finalPrevRayMapPos = prevRayMapPos;
        prevRayMapPos = vec3(0);
        vec3 finalRayMapPos = rayMapPos;
        rayMapPos = vec3(0);
        vec4 finalLighting = lighting;
        lighting = vec4(0);
        vec4 finalLightFog = lightFog;
        lightFog = vec4(0);
        vec4 finalTint = tint;
        tint = vec4(1, 1, 1, 0);
        vec3 finalLightPos = lightPos;
        lightPos = vec3(0);
        vec4 reflectedColor = vec4(0, 0, 0, 0);
        if (!isSky && reflectivity > 0.f) {
            vec3 reflectDir = reflect(ogDir, normalize(hitNorm));
            reflectedColor = traceWorld(finalPrevHitPos, reflectDir);
            bool isReflectingSky = hitPos == vec3(256);
            if (isReflectingSky) {
                hitPos = vec3(finalPrevHitPos + (reflectDir * renderDistance));
            }
            float adjustedTime = clamp(abs(1-clamp((distance(hitPos, sun-vec3(0, sun.y, 0))/1.4)/(size/1.5), 0, 1))*2, 0.05f, 1.f);
            float adjustedTimeCam = clamp(abs(1-clamp((distance(camPos, sun-vec3(0, sun.y, 0))/1.4)/(size/1.5), 0, 1))*2, 0.05f, 0.9f);
            float timeBonus = gradient(hitPos.y, 0, 372, 0.3, 0f);
            float mixedTime = (adjustedTime/2)+(adjustedTimeCam/2)+timeBonus;
            float fogNoise = max(0, noise(vec2(hitPos.x, hitPos.z)));
            float distanceFogginess = clamp(exp2((distance(camPos, hitPos)/renderDistance)*(1+fogNoise))/1.8, 0, 1f);
            float sunLight = (lighting.a/15)*(mixedTime-timeBonus);
            float whiteness = gradient(hitPos.y, 0, 372, -1.95f, 1.33f);
            vec3 sunColor = mix(mix(vec3(0.66, 0.33, 1), vec3(1.33, 0.33, 0.25), mixedTime*4), vec3(0.93, 0.95, 1), mixedTime);
            reflectedColor = vec4(mix(vec3(reflectedColor), vec3(tint)*0.5f, min(0.9f, tint.a)), 1);//transparency
            if (isReflectingSky) {
                fogNoise = 0f;
                distanceFogginess = 1f;
            } else {
                if (reflectedColor.a >= 1.f && sunLight > 0.f) {
                    if (lightPos == vec3(0)) {
                        lightPos = hitPos;
                    }
                    if (!traceSun(lightPos, normalize(sun - lightPos))) {
                        sunLight /= (max(1, 2.5-distanceFogginess)*(1+(max(0, abs(1-mixedTime)-0.9)*10)))*0.72f;
                    } else {
                        reflectedColor = vec4(mix(vec3(reflectedColor), vec3(sunTint), min(0.9f, sunTint.a/4)), 1);//sun tint
                    }
                }
            }

            vec3 unmixedFogColor = max(vec3(0), vec3(0.416+(0.3*whiteness), 0.495+(0.2*whiteness), 0.75+(min(0, whiteness+4.5))));

            reflectedColor = vec4(mix(vec3(reflectedColor), unmixedFogColor, distanceFogginess), 1);//distant fog
            vec3 superFinalLightFog = mix(vec3(lightFog-0.5f)*2, sunColor, sunLight)*0.15f; //fog blending + sun distance fog
            reflectedColor = vec4((vec3(reflectedColor)*max(vec3(lighting/3f), sunLight)) + superFinalLightFog, 1); //brightness, blocklight fog, sun
        }
        float adjustedTime = clamp(abs(1-clamp((distance(finalHitPos, sun-vec3(0, sun.y, 0))/1.4)/(size/1.5), 0, 1))*2, 0.05f, 1.f);
        float adjustedTimeCam = clamp(abs(1-clamp((distance(camPos, sun-vec3(0, sun.y, 0))/1.4)/(size/1.5), 0, 1))*2, 0.05f, 0.9f);
        float timeBonus = gradient(finalHitPos.y, 0, 372, 0.3, 0f);
        float mixedTime = (adjustedTime/2)+(adjustedTimeCam/2)+timeBonus;
        float fogNoise = max(0, noise(vec2(finalHitPos.x, finalHitPos.z)));
        float distanceFogginess = clamp(exp2((distance(camPos, finalHitPos)/renderDistance)*(1+fogNoise))/1.8, 0, 1f);
        float sunLight = (finalLighting.a/15)*(mixedTime-timeBonus);
        float whiteness = gradient(finalHitPos.y, 0, 372, -1.95f, 1.33f);
        vec3 sunColor = mix(mix(vec3(0.66, 0.33, 1), vec3(4.25, 1.75, 0.25), mixedTime*4), vec3(0.93, 0.95, 1), mixedTime);
        fragColor = vec4(mix(vec3(fragColor), vec3(finalTint)*0.5f, min(0.9f, finalTint.a)), 1);//transparency
        if (isSky) {
            fogNoise = 0f;
            distanceFogginess = 1f;
        } else {
            if (fragColor.a >= 1.f && sunLight > 0.f) {
                if (finalLightPos == vec3(0)) {
                    finalLightPos = finalHitPos;
                }
                if (!traceSun(finalLightPos, normalize(sun - finalLightPos))) {
                    sunLight /= (max(1, 2.5-distanceFogginess)*(1+(max(0, abs(1-mixedTime)-0.9)*10)))*0.72f;
                } else {
                    fragColor = vec4(mix(vec3(fragColor), vec3(sunTint), min(0.9f, sunTint.a/4)), 1);//sun tint
                }
            }
        }

        vec3 unmixedFogColor = max(vec3(0), vec3(0.416+(0.3*whiteness), 0.495+(0.2*whiteness), 0.75+(min(0, whiteness+4.5))));

        //selection start
        if (ui && selected == ivec3(finalRayMapPos)) {
            fragColor = vec4(mix(vec3(fragColor), vec3(0.7, 0.7, 1), 0.5f), 1);
        }
        //selection end

        fragColor = vec4(mix(vec3(fragColor), unmixedFogColor, distanceFogginess), 1);//distant fog
        vec3 superFinalLightFog = mix(vec3(finalLightFog-0.5f)*2, sunColor, sunLight)*0.15f;//fog blending + sun distance fog
        fragColor = vec4((vec3(fragColor)*max(vec3(finalLighting/3f), sunLight)) + superFinalLightFog, 1);//brightness, blocklight fog, sun
        fragColor = toLinear(mix(fragColor, reflectedColor, reflectivity));
    }
}