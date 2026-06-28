layout(set = 0, binding = 0) readonly uniform GlobalUBO {
    mat4 view;
    mat4 proj;
    mat4 viewPrev;
    mat4 projPrev;
    ivec4 renderToggles;
    vec4 skylight;
    vec3 sun;
    int hdr;
    float time;
    ivec2 res;
    vec4 atmosphere;
    vec4 nightAtmosphere;
    vec4 sunsetAtmosphere;
    vec4 deepSunsetAtmosphere;
    float fogginess;
    vec3 skylightMul;
} globalUbo;
layout(set = 0, binding = 9) uniform sampler2D ddaColors;
layout(set = 0, binding = 10) uniform sampler2D ddaDepth;
layout(set = 0, binding = 11) uniform sampler2D ddaNormals;
layout(set = 0, binding = 20) uniform sampler2D blueNoise;
layout(location = 0) in vec2 uv;

layout(location = 0) out vec4 outColor;

vec3 hash(vec3 vec) {
    vec3 p = vec;
    p = vec3(dot(p, vec3(127.1,311.7, 74.7)), dot(p, vec3(269.5,183.3,246.1)), dot(p, vec3(113.5,271.9,124.6)));
    return fract(sin(p)*43758.5453123);
}
vec3 noise(ivec2 coords) {
    return texelFetch(blueNoise, ivec2(coords.x&63, coords.y&63), 0).rgb;
}
const float PI = 3.141592653589793f;
vec3 randomVec(ivec2 coords) {
    vec3 noise = noise(coords);
    vec3 randDir = vec3(normalize(noise.xy * 2.0 - 1.0), noise.z);
    return randDir;
}
const float AO_RADIUS = 1.f;
const float AO_STRENGTH = 2.f;
vec3 SSAO_KERNEL[32] = vec3[](vec3( 0.011,  0.015,  0.021), vec3(-0.032,  0.021,  0.039),vec3( 0.025, -0.041,  0.048), vec3(-0.014, -0.062,  0.071),vec3( 0.073,  0.033,  0.082), vec3(-0.089,  0.051,  0.113),vec3( 0.042, -0.112,  0.129), vec3(-0.061, -0.124,  0.142),vec3( 0.141,  0.082,  0.163), vec3(-0.162,  0.094,  0.185),vec3( 0.091, -0.192,  0.211), vec3(-0.115, -0.211,  0.242),vec3( 0.231,  0.141,  0.268), vec3(-0.252,  0.163,  0.291),vec3( 0.152, -0.311,  0.334), vec3(-0.191, -0.332,  0.371),vec3( 0.341,  0.212,  0.412), vec3(-0.372,  0.231,  0.448),vec3( 0.221, -0.442,  0.491), vec3(-0.271, -0.471,  0.532),vec3( 0.462,  0.301,  0.581), vec3(-0.502,  0.322,  0.623), vec3( 0.311, -0.612,  0.672), vec3(-0.382, -0.641,  0.714), vec3( 0.602,  0.411,  0.763), vec3(-0.642,  0.432,  0.812), vec3( 0.412, -0.802,  0.864), vec3(-0.512, -0.831,  0.911), vec3( 0.732,  0.531,  0.942), vec3(-0.782,  0.562,  0.968), vec3( 0.522, -0.951,  0.985), vec3(-0.621, -0.972,  0.998));
const int KERNEL_SIZE = 32;
//const float Z_NEAR = 0.01f;
//const float ASPECT = 1.7977529f;
//const float FOCAL_LENGTH = 1.3514224f;
vec3 reconstructViewPos(vec2 uvPos, float depth) {
    vec4 clip = vec4((uvPos*2)-1, depth, 1.f);
    vec4 view = inverse(globalUbo.proj)*clip;
    return view.xyz/view.w;
}
float getAO(float depth, vec3 normal) {
    vec3 normalVS = normalize((globalUbo.view * vec4(normal.xyz, 0.f)).xyz);
    vec3 posVS = reconstructViewPos(uv, depth)+(normalize(normalVS)*0.1f);
    vec3 randVec = randomVec(ivec2(gl_FragCoord.xy));
    vec3 tangent = normalize(randVec - normalVS * dot(randVec, normalVS));
    vec3 bitangent = cross(normalVS, tangent);
    mat3 TBN = mat3(tangent, bitangent, normalVS);
    float occlusion = 0.f;
    for (int i = 0; i < KERNEL_SIZE; i++) {
        vec3 sampleVec = TBN*SSAO_KERNEL[i];//randomVec(ivec2(gl_FragCoord.x+(i*2), gl_FragCoord.y+i));
        sampleVec = posVS + sampleVec * AO_RADIUS;
        vec4 offset = globalUbo.proj * vec4(sampleVec, 1.0);
        offset.xyz /= offset.w;
        vec2 sampleUV = (offset.xy*0.5)+0.5;
        if (!(sampleUV.x < 0 || sampleUV.x > 1 || sampleUV.y < 0 || sampleUV.y > 1)) {
            vec3 sampleVS = reconstructViewPos(sampleUV, texture(ddaDepth, sampleUV).r);
            float rangeCheck = smoothstep(0, 1, AO_RADIUS/(length(posVS-sampleVS)+0.001f));
            if (sampleVS.z+0.01f < sampleVec.z) {
                occlusion += AO_STRENGTH*rangeCheck;
            }
        }
    }
    occlusion = 1.0 - (occlusion / KERNEL_SIZE);
    return clamp(occlusion, 0.f, 1);
}

struct ChunkStruct {
    int pointer;
    int paletteSize;
    int bitsPerValue;
    int valueMask;
};
struct LongStruct {
    int first;
    int second;
};
layout(std430, set = 0, binding = 1) readonly buffer RegionBuffer {
    LongStruct[] regions;
} regionData;
layout(std430, set = 0, binding = 2) readonly buffer ChunkBuffer {
    ChunkStruct[] chunks;
} chunkData;
layout(std430, set = 0, binding = 3) readonly buffer VoxelBuffer {
    int[] voxels;
} voxelData;
layout(std430, set = 0, binding = 4) readonly buffer LODBuffer {
    LongStruct[] lods;
} lodData;
layout(std430, set = 0, binding = 5) readonly buffer LightChunkBuffer {
    ChunkStruct[] chunks;
} lightChunkData;
layout(std430, set = 0, binding = 6) readonly buffer LightBuffer {
    int[] lights;
} lightData;
const int size = 4096;
const int height = 640;
const vec3 worldSize = vec3(size, height, size);
const int chunkSize = 16;
const int sizeChunks = size / chunkSize;
const int heightChunks = height / chunkSize;
const int regionSize = 4;
const int sizeRegions = sizeChunks / regionSize;
const int heightRegions = heightChunks / regionSize;
const int lodSize = 4;
const int sizeLods = size / lodSize;
const int heightLods = height / lodSize;
int packLodPos(int x, int y, int z) {
    return x+y*sizeLods+z*sizeLods*heightLods;
}
int packLodPos(ivec3 pos) {
    return packLodPos(pos.x, pos.y, pos.z);
}
LongStruct getLod(ivec3 lodPos) {
    return lodData.lods[packLodPos(lodPos)];
}
int packRegionPos(int x, int y, int z) {
    return x+y*sizeRegions+z*sizeRegions*heightRegions;
}
int packRegionPos(ivec3 pos) {
    return packRegionPos(pos.x, pos.y, pos.z);
}
LongStruct getRegion(ivec3 regionPos) {
    return regionData.regions[packRegionPos(regionPos)];
}
int packPos(vec3 pos) {
    return int(pos.x)+int(pos.y)*size+int(pos.z)*(size*height);
}
ChunkStruct lightChunk = ChunkStruct(0, 0, 0, 0);
int lightValuesPerInt = -1;
void updateLightChunkData(ivec3 chunkPos) {
    int condensedChunkPos = (((chunkPos.x*sizeChunks)+chunkPos.z)*heightChunks)+chunkPos.y;
    lightChunk = lightChunkData.chunks[condensedChunkPos];
    lightValuesPerInt = lightChunk.bitsPerValue <= 0 ? 0 : 32/lightChunk.bitsPerValue;
}
void updateLightChunkData(int x, int y, int z) {
    updateLightChunkData(ivec3(x, y, z) >> 4);
}
const int maxSunlightLevel = 31;
const int fullSunlight = 520093696;
int getLightData(int x, int y, int z) {
    updateLightChunkData(x, y, z);
    if (lightValuesPerInt <= 0) {return fullSunlight;}
    ivec3 localPos = ivec3(x, y, z) & ivec3(15);
    int condensedLocalPos = ((((localPos.x*chunkSize)+localPos.z)*chunkSize)+localPos.y);
    int intIndex = condensedLocalPos/lightValuesPerInt;
    int bitIndex = (condensedLocalPos - intIndex * lightValuesPerInt) * lightChunk.bitsPerValue;

    int index = lightChunk.pointer+lightChunk.paletteSize+intIndex;
    if (index < 0 || index >= 500000000) return fullSunlight;
    int key = (lightData.lights[index] >> bitIndex) & lightChunk.valueMask;
    return lightData.lights[lightChunk.pointer+key];
}
vec4 getLight(int x, int y, int z) {
    int lightData = getLightData(x, y, z);
    return vec4(0xFF & lightData >> 16, 0xFF & lightData >> 8, 0xFF & lightData, 0xFF & lightData >> 24);
}
vec4 getLight(float x, float y, float z) {
    return getLight(int(x), int(y), int(z));
}
vec4 getLight(vec3 pos) {
    return getLight(int(pos.x), int(pos.y), int(pos.z));
}

ChunkStruct chunk = ChunkStruct(0, 0, 0, 0);
int blockValuesPerInt = 0;
void updateChunkData(ivec3 chunkPos) {
    int condensedChunkPos = (((chunkPos.x*sizeChunks)+chunkPos.z)*heightChunks)+chunkPos.y;
    chunk = chunkData.chunks[condensedChunkPos];
    blockValuesPerInt = chunk.bitsPerValue <= 0 ? 0 : 32/chunk.bitsPerValue;
}
void updateChunkData(int x, int y, int z) {
    updateChunkData(ivec3(x, y, z) >> 4);
}
int getBlockData(int x, int y, int z) {
    updateChunkData(x, y, z);
    //if (blockValuesPerInt <= 0) {return 0;}
    ivec3 localPos = ivec3(x, y, z) & ivec3(15);
    int condensedLocalPos = ((((localPos.x*chunkSize)+localPos.z)*chunkSize)+localPos.y);
    int intIndex = condensedLocalPos/blockValuesPerInt;
    int bitIndex = (condensedLocalPos - intIndex * blockValuesPerInt) * chunk.bitsPerValue;

    int index = chunk.pointer+chunk.paletteSize+intIndex;
    if (index < 0 || index >= 500000000) return 0;

    int key = (voxelData.voxels[index] >> bitIndex) & chunk.valueMask;
    return voxelData.voxels[chunk.pointer+key];
}
ivec2 getBlock(int x, int y, int z) {
    int blockData = getBlockData(x, y, z);
    int type = (blockData >> 16) & 0xFFFF;
    return ivec2(type, min(16, blockData & 0xFFFF));
}
ivec2 getBlock(vec3 pos) {
    return getBlock(int(pos.x), int(pos.y), int(pos.z));
}
layout(set = 0, binding = 7) uniform sampler3D atlas;
const float alphaMax = 0.95f;
vec4 fromLinear(vec4 linearRGB){
    bvec4 cutoff = lessThan(linearRGB, vec4(0.0031308));
    vec4 higher = vec4(1.055)*pow(linearRGB, vec4(1.0/2.4)) - vec4(0.055);
    vec4 lower = linearRGB * vec4(12.92);
    return mix(higher, lower, cutoff);
}
vec4 toLinear(vec4 sRGB){
    bvec4 cutoff = lessThan(sRGB, vec4(0.04045));
    vec4 higher = pow((sRGB + vec4(0.055))/vec4(1.055), vec4(2.4));
    vec4 lower = sRGB/vec4(12.92);
    return mix(higher, lower, cutoff);
}
vec3 fromLinear(vec3 linearRGB){
    bvec3 cutoff = lessThan(linearRGB, vec3(0.0031308));
    vec3 higher = vec3(1.055)*pow(linearRGB, vec3(1.0/2.4)) - vec3(0.055);
    vec3 lower = linearRGB * vec3(12.92);
    return vec3(mix(higher, lower, cutoff));
}
vec3 toLinear(vec3 sRGB){
    bvec3 cutoff = lessThan(sRGB, vec3(0.04045));
    vec3 higher = pow((sRGB + vec3(0.055))/vec3(1.055), vec3(2.4));
    vec3 lower = sRGB/vec3(12.92);
    return vec3(mix(higher, lower, cutoff));
}
const int blockSize = 8;
const int blockTexSize = blockSize;
const float voxelSize = 1.f/blockSize;
vec4 sampleAtlas(int x, int y, int z, int blockType, int blockSubtype) {
    //    if ((bX & 1) != 0) { x += blockSize; }
    //    if ((bY & 1) != 0) { y += blockSize; }
    //    if ((bZ & 1) != 0) { z += blockSize; }
    vec4 color = texelFetch(atlas, ivec3(x+(blockType*8), ((abs(y-blockTexSize)-1)*blockTexSize)+z, blockSubtype), 0);
    return vec4(fromLinear(color.rgb), color.a);
}
vec4 sampleAtlasTiled(int x, int y, int z, int blockType, int blockSubtype) {
    ivec3 pos = ivec3(x, y, z);
    if (pos.x < 0) {pos.x+=blockSize;} else if (pos.x >= blockSize) {pos.x-=blockSize;}
    if (pos.y < 0) {pos.y+=blockSize;} else if (pos.y >= blockSize) {pos.y-=blockSize;}
    if (pos.z < 0) {pos.z+=blockSize;} else if (pos.z >= blockSize) {pos.z-=blockSize;}
    return sampleAtlas(pos.x, pos.y, pos.z, blockType, blockSubtype);
}
vec4 getBlockAndVoxel(float x, float y, float z) {
    if (x < 0 || x >= size || z < 0 || z >= size || y < 0 || y >= height) {return vec4(0);}
    int blockData = getBlockData(int(x), int(y), int(z));
    int type = (blockData >> 16) & 0xFFFF;
    ivec2 block = ivec2(type, min(16, blockData & 0xFFFF));
    ivec3 voxelPos = ivec3(fract(vec3(x, y, z))*8);
    return sampleAtlas(voxelPos.x, voxelPos.y, voxelPos.z, block.x, block.y);
}
vec4 getBlockAndVoxel(vec3 pos) {
    return getBlockAndVoxel(pos.x, pos.y, pos.z);
}
layout(set = 0, binding = 8) uniform sampler2D noises;
float noise(vec2 coords) {
    return (texture(noises, vec2(coords/1024)).r*2)-0.5f;
}
float getCaustic(bool animated, vec2 checkPos) {
    float time = animated ? globalUbo.time/1000 : 0.f;
    return noise((checkPos+time)*32);
}

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

const int renderDistance = int(min(size, 2048)*0.75f);
const float maxChunkDist = (renderDistance/chunkSize)*(renderDistance/chunkSize);
vec3 ogPos = vec3(0);
vec3 ogDir = vec3(0);
vec3 ogRayPos = vec3(0);
vec3 ogRayDir = vec3(0);
vec3 sunColor = vec3(0);
float skyWhiteline = 0.9f;
float skyDensity = 1.f;
float sunBrightnessMul = 1.1f;
vec4 getLightingColor(bool celestialSource, vec3 lightPos, vec4 lighting, bool isSky, float fogginess, bool negateSun) {
    float sunHeight = globalUbo.fogginess < 0 ? 1 : abs(dot(normalize(globalUbo.sun - lightPos), vec3(0, 1, 0)));
    float scattering = gradient(lightPos.y, 0, 1500, 1.5f, -0.5f);
    float adjustedTime = clamp((abs(1-clamp(sunHeight, 0.05f, 0.5f)))+scattering, 0.f, 1.f);
    float sunSetness = min(1.f, max(abs(sunHeight), adjustedTime));
    float skyWhiteness = mix(max(0.33f, gradient(lightPos.y, 63, 437, 0, skyWhiteline)), 0.9f, clamp(abs(1-sunSetness), 0, 1.f));
    if (negateSun) { lighting.a = 0; } else {lighting.a = mix(lighting.a, 1.f, fogginess);}
    float whiteness = (isSky ? skyWhiteness : mix(skyWhiteline, skyWhiteness, max(0, fogginess-0.8f)*5.f))*clamp(sunHeight+0.8f-(min(sunSetness, 0.2f)*4), 0.33f, 1);
    float lowSunHeight = 10*clamp(sunHeight, 0.f, 0.1f);
    sunColor = mix(mix(mix(globalUbo.deepSunsetAtmosphere.rgb, globalUbo.sunsetAtmosphere.rgb, min(1, lowSunHeight*2)), mix(globalUbo.nightAtmosphere.rgb, globalUbo.atmosphere.rgb, clamp(sunHeight+0.1f, 0, 1)), sunSetness), vec3(1), whiteness);
    sunColor = mix(vec3(1), vec3(1, 0.95f, 0.85f), sunSetness/4)*(lighting.a*sunColor);
    if (!isSky && !celestialSource) {
        sunColor = mix(globalUbo.skylightMul*lighting.a, sunColor, fogginess);
    }
    //    if (!isSky && globalUbo.skylight.w >= 1.f) {
    //        sunColor*=min(sunBrightnessMul, sunBrightnessMul <= 1.f ? 1.f : max(1.f, sunBrightnessMul-fogginess));
    //    }
    float thickness = 1;//sunHeight < 0 ? gradient(lightPos.y, 128, height-max(0, sunHeight*height), 1+(sunHeight/2), 1)-mix(0.33f, 0, clamp(sunHeight, 0, 1)) : 1;
    vec4 color = vec4(max(lighting.rgb, sunColor*thickness*globalUbo.skylight.a), thickness);
    return isSky ? color*gradient(lightPos.y, 72, 320, skyDensity, 1) : color;
}

vec3 unzeroVec(vec3 dir) {
    if (dir.x == 0.0f) {
        dir.x = 0.001f;
    }
    if (dir.y == 0.0f) {
        dir.y = 0.001f;
    }
    if (dir.z == 0.0f) {
        dir.z = 0.001f;
    }
    return dir;
}

ivec3 blockPos = ivec3(0);
vec3 hitPos = vec3(0);
vec3 shadowPos = vec3(0);
vec3 flatNormal = vec3(0);

vec3 ddaMask = vec3(0);
void stepMask(vec3 sideDist) {
    ddaMask = step(sideDist, vec3(min(min(sideDist.x, sideDist.y), sideDist.z) + 0.000000001));
}

vec3 getDir(vec2 pos) {
    vec4 clipSpace = vec4((inverse(globalUbo.proj) * vec4(pos, 1.f, 1.f)).xyz, 0);
    return unzeroVec(normalize((inverse(globalUbo.view)*clipSpace).xyz));
}
vec3 rayPos = vec3(0);
vec3 rayDir = vec3(0);
vec3 rayDirDivved = vec3(0);
vec3 uv3d(vec3 worldPos, float stageSize, int nextStageSize) {
    vec3 minPos = (worldPos - rayPos) * rayDirDivved;
    vec3 maxPos = minPos + stageSize * rayDirDivved;
    vec3 entranceMin = min(minPos, maxPos);
    float entrancePos = max(0.f, max(entranceMin.x, max(entranceMin.y, entranceMin.z)));
    vec3 intersect = rayPos + rayDir * entrancePos;
    return min((intersect - worldPos)/(stageSize/nextStageSize), vec3(nextStageSize-0.0001f));
}
vec3 uv3d(ivec3 worldPos, float stageSize, int nextStageSize) {
    return uv3d(vec3(worldPos), stageSize, nextStageSize);
}

vec3 firstTintAddition = vec3(0);
vec4 prevTintAddition = vec4(0);
vec4 tint = vec4(1);
vec3 tintNormal = vec3(0);
void addTint(vec4 voxelColor, vec3 normal, bool shadow) {
    if (tintNormal == vec3(0)) {
        tintNormal = normal;
    }
    if (prevTintAddition != voxelColor) {
        float brightness = dot((tint.a < 1 ? -1 : 1) * normal, globalUbo.skylight.xyz)*-0.0001f;
        float tintMul = clamp(0.875f+brightness, 0.75f, 1.f);
        prevTintAddition = voxelColor;
        if (firstTintAddition == vec3(0)) {
            firstTintAddition = voxelColor.rgb*tintMul;
        }
        vec3 shadeTintFactor = (shadow ? vec3(1-voxelColor.r, 1-voxelColor.g, 1-voxelColor.b)*(tint.a == 1 ? 1.5f : 1.f) : vec3(1));
        tint.rgb -= shadeTintFactor * abs(1-voxelColor.rgb)*tintMul*tint.rgb;
        tint.a -= max(shadeTintFactor.r, max(shadeTintFactor.g, shadeTintFactor.b)) * voxelColor.a*tint.a;
    }
}

bool isGlass(int block) {return block == 11 || block == 12 || block == 13 || block == 66 || block == 67;}

int steps = 0;
ivec3 firstVoxelRayPos = ivec3(0);
ivec3 firstBlockPos = ivec3(0);
vec3 firstHitPos = vec3(0);
ivec2 firstBlock = ivec2(0);
bool reverseNormShading = false;
vec3 ogChunkPos = vec3(0);
vec3 raySign = vec3(0);
vec3 normal = vec3(0);
ivec2 block = ivec2(0);
LongStruct lod = LongStruct(0, 0);
ivec3 chunkWorldPos = ivec3(0);
vec4 dda(bool shadow) {
    rayDir = unzeroVec(rayDir);
    rayDirDivved = 1.f/rayDir;
    ogRayPos = rayPos;
    ogRayDir = rayDir;
    raySign = sign(rayDir);
    int stage = 3;

    LongStruct region = LongStruct(0, 0);

    vec3 chunkStartPos = rayPos/chunkSize;
    ivec3 chunkPos = ivec3(chunkStartPos);
    ivec3 ddaPos = chunkPos;
    vec3 dist = 1.0/rayDir;
    vec3 sideDist = ((ddaPos - chunkStartPos) + 0.5 + raySign * 0.5) * dist;
    vec3 chunkSideDist = sideDist;
    stepMask(sideDist);
    chunkWorldPos = ivec3(0);

    vec3 lodStartPos = vec3(0);
    ivec3 lodRayPos = ivec3(0);
    vec3 lodPos = vec3(0);
    vec3 lodSideDist = vec3(0);
    ivec3 lodWorldPos = ivec3(0);

    vec3 blockStartPos = vec3(0);
    ivec3 blockRayPos = ivec3(0);
    vec3 blockSideDist = vec3(0);

    vec3 voxelStartPos = vec3(0);
    vec3 voxelPos = vec3(0);
    while (true) {
        steps++;
        bool stepAnything = true;
        if (stage == 3) {
            if (abs(dot(ddaPos-ogChunkPos, ogChunkPos-ddaPos)) >= maxChunkDist || ddaPos.x < 0 || ddaPos.x >= sizeChunks || ddaPos.z < 0 || ddaPos.z >= sizeChunks) { break; }
            if (ddaPos.y >= heightChunks && rayDir.y < 0) {
                //no need to do any checks
            } else {
                if (ddaPos.y < 0 || ddaPos.y >= heightChunks) { break; }
                region = getRegion(ddaPos/regionSize);
                ivec3 chunkRayPos = ddaPos % regionSize;
                int bitIdx = (chunkRayPos.x & (regionSize-1)) + (chunkRayPos.y & (regionSize-1)) * regionSize + (chunkRayPos.z & (regionSize-1)) * regionSize * regionSize;
                int regionToUse = region.first;
                if (bitIdx >= 32) {
                    bitIdx-=32;
                    regionToUse = region.second;
                }
                int mask = 1 << bitIdx;
                if ((regionToUse & mask) != 0) {
                    updateChunkData(ddaPos);
                    stage = 2;
                    stepAnything = false;
                    chunkPos = ddaPos;
                    chunkSideDist = sideDist;

                    chunkWorldPos = ddaPos*chunkSize;
                    lodStartPos = uv3d(chunkWorldPos, float(chunkSize), lodSize);
                    ddaPos = ivec3(lodStartPos);
                    sideDist = ((ddaPos - lodStartPos) + 0.5 + raySign * 0.5) * dist;
                }
            }
        } else if (stage == 2) {
            if (ddaPos.x < 0 || ddaPos.x >= lodSize || ddaPos.y < 0 || ddaPos.y >= lodSize || ddaPos.z < 0 || ddaPos.z >= lodSize) {
                stage = 3;
                ddaPos = chunkPos;
                sideDist = chunkSideDist;
            } else {
                lodPos = (chunkWorldPos/lodSize)+ddaPos;
                lod = getLod(ivec3(lodPos));
                if (lod != LongStruct(0, 0)) {
                    stage = 1;
                    stepAnything = false;
                    lodRayPos = ddaPos;
                    lodSideDist = sideDist;

                    lodWorldPos = ivec3(lodPos*lodSize);
                    blockStartPos = uv3d(lodWorldPos, float(lodSize), lodSize);
                    ddaPos = ivec3(blockStartPos);
                    sideDist = ((ddaPos - blockStartPos) + 0.5 + raySign * 0.5) * dist;
                }
            }
        } else if (stage == 1) {
            if (ddaPos.x < 0 || ddaPos.x >= lodSize || ddaPos.y < 0 || ddaPos.y >= lodSize || ddaPos.z < 0 || ddaPos.z >= lodSize) {
                stage = 2;
                ddaPos = lodRayPos;
                sideDist = lodSideDist;
            } else {
                int bitIdx = (ddaPos.x & (lodSize-1)) + (ddaPos.y & (lodSize-1)) * lodSize + (ddaPos.z & (lodSize-1)) * lodSize * lodSize;
                int lodToUse = lod.first;
                if (bitIdx >= 32) {
                    bitIdx-=32;
                    lodToUse = lod.second;
                }
                int mask = 1 << bitIdx;
                if ((lodToUse & mask) != 0) {
                    blockPos = lodWorldPos+ddaPos;
                    block = getBlock(blockPos);
                    voxelStartPos = uv3d(blockPos, 1.f, blockSize);
                    vec4 voxelColor = sampleAtlas(int(voxelStartPos.x), int(voxelStartPos.y), int(voxelStartPos.z), block.x, block.y);
                    if (voxelColor.a > 0) {
                        flatNormal = -ddaMask*raySign;
                        normal = flatNormal;
                        hitPos = blockPos+(voxelStartPos*voxelSize)+(flatNormal*0.001f);
                        voxelPos = hitPos;
                        if (firstBlock.x == 0) {
                            firstBlockPos = blockPos;
                            firstVoxelRayPos = ivec3(voxelStartPos);
                            firstBlock = block;
                            firstHitPos = hitPos;
                        }
                        if (voxelColor.a > alphaMax) {
                            shadowPos = hitPos+vec3(0, voxelSize, 0);
                            return voxelColor;
                        } else {
                            addTint(voxelColor, normal, shadow);
                        }
                    } else {
                        stage = 0;
                        stepAnything = false;
                        blockRayPos = ddaPos;
                        blockSideDist = sideDist;

                        ddaPos = ivec3(voxelStartPos);
                        sideDist = ((ddaPos - voxelStartPos) + 0.5 + raySign * 0.5) * dist;
                    }
                }
            }
        } else if (stage == 0) {
            if (ddaPos.x < 0 || ddaPos.x >= blockSize || ddaPos.y < 0 || ddaPos.y >= blockSize || ddaPos.z < 0 || ddaPos.z >= blockSize) {
                stage = 1;
                ddaPos = blockRayPos;
                sideDist = blockSideDist;
            } else {
                vec4 voxelColor = sampleAtlas(ddaPos.x, ddaPos.y, ddaPos.z, block.x, block.y);
                if (voxelColor.a > 0) {
                    flatNormal = -ddaMask*raySign;
                    normal = flatNormal;
                    voxelPos = blockPos+(ddaPos*voxelSize)+(flatNormal*0.001f);
                    vec3 subvoxelPos = (uv3d(voxelPos, voxelSize, blockSize)/blockSize)*voxelSize;
                    hitPos = voxelPos+(subvoxelPos);
                    if (firstBlock.x == 0) {
                        firstVoxelRayPos = ivec3(voxelPos*blockSize);
                        firstBlock = block;
                        firstHitPos = hitPos;
                    }
                    if (voxelColor.a > alphaMax) {
                        shadowPos = hitPos;
                        return voxelColor;
                    } else {
                        addTint(voxelColor, normal, shadow);
                        stage = 1;
                        ddaPos = blockRayPos;
                        sideDist = blockSideDist;
                    }
                }
            }
        }
        if (stepAnything) { //dont step if it just went to a finer detail
            stepMask(sideDist);
            sideDist += ddaMask * raySign * dist;
            ddaPos += ivec3(ddaMask * raySign);
        }
    }
    return vec4(0);
}

void main() {
    vec4 color = texture(ddaColors, uv);
    float depth = texture(ddaDepth, uv).r;
    vec4 normal = texture(ddaNormals, uv);
    vec3 camPos = inverse(globalUbo.view)[3].xyz;
    vec3 ogPos = camPos;
    ogChunkPos = ogPos/chunkSize;
    vec2 uvNdc = (uv * 2.0) - 1.0;
    vec3 ogDir = getDir(uvNdc);
    vec3 pos = vec3(0);
    vec3 lightPos = vec3(0);
    bool isSky = color.a < alphaMax;
    float fogginessMul = 1.f;
    bool isGlowing = color.r >= 1 || color.g > 1 || color.b > 1;
    if (isSky) {
        pos = ogPos + ogDir * renderDistance;
        lightPos = pos;
    } else {
        vec4 ndc = vec4(uvNdc, depth, 1.0);
        vec4 viewPos = inverse(globalUbo.proj) * ndc;
        viewPos /= viewPos.w;
        vec4 worldPos = inverse(globalUbo.view) * viewPos;
        worldPos /= worldPos.w;
        pos = worldPos.xyz;
        lightPos = pos+(normal.xyz*0.01f);
    }
    bool inBounds = !(lightPos.x < 0 || lightPos.y < 0 || lightPos.z < 0 || lightPos.x >= size || lightPos.y >= height  || lightPos.z >= size);
    if (!inBounds && !isGlowing) {fogginessMul = 0.f;}
    float fogDist = pow(globalUbo.fogginess, 4);
    float maxFogginess = globalUbo.fogginess > 0 ? 1.f-max(0, globalUbo.fogginess-1) : 0;
    float fogginess = globalUbo.fogginess <= 0 ? 0 : (isSky ? maxFogginess : clamp((sqrt(distance(camPos, lightPos)/(renderDistance*0.66f*fogDist))-0.25f)*gradient(lightPos.y, 63, 80, 1, 1+abs(noise(lightPos.xz)*0.67f))*fogginessMul, 0.f, maxFogginess));
    bool celestialSource = globalUbo.skylight.x < 0 || globalUbo.skylight.x >= size || globalUbo.skylight.y < 0 || globalUbo.skylight.y >= height || globalUbo.skylight.z < 0 || globalUbo.skylight.z >= size;
    vec4 blockLighting = inBounds ? min(vec4(1), getLight(lightPos)/vec4(15, 15, 15, maxSunlightLevel)) : vec4(0, 0, 0, 1);
    float shadowFactor = 1.f;
    if (!isSky) {
        vec4 shadowColor = vec4(0);
        vec3 source = globalUbo.fogginess <= 0 ? globalUbo.skylight.xyz : vec3(globalUbo.skylight.x, globalUbo.skylight.y > 0 ? max(globalUbo.skylight.y, lightPos.y+9) : globalUbo.skylight.y, globalUbo.skylight.z);
        if (globalUbo.skylight.a <= 0 || dot(normal.xyz, normalize(source)) <= 0.f) {
            shadowColor.a = 1.f;
        } else if (globalUbo.renderToggles.x > 0) {
            vec3 sunDir = normalize(source - lightPos);
            rayPos = lightPos;
            rayDir = sunDir;
            shadowColor = dda(true);
        }
        if (shadowColor.a > 0.0f) {
            shadowFactor = !celestialSource ? 0.02f : 0.5f;//gradient(hitPos.y, 63, 256, 0.85f, 0.367f);//mix(0.66f, 0.15f, min(1.f, distance(primaryLightPos.xz, ogPos.xz)/150.f)));
            blockLighting.a *= shadowFactor;
        }
    }
    vec3 lighting = getLightingColor(celestialSource, lightPos, blockLighting, isSky, fogginess, false).rgb;
    color.rgb *= lighting;
    color.rgb = mix(color.rgb, lighting, fogginess);
    //color.rgb = pos/worldSize;
    outColor = vec4(color.rgb, mix(getAO(depth, normal.xyz), 1, clamp((fogginess*2)+max(0, abs(1-shadowFactor)-0.34f), 0, 1)));
}