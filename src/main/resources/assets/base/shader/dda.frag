layout(set = 0, binding = 0) readonly uniform GlobalUBO {
    mat4 view;
    mat4 proj;
    vec4 skylight;
    vec3 sun;
    int hdr;
    float time;
} globalUbo;
struct ChunkStruct {
    int pointer;
    int paletteSize;
    int bitsPerValue;
    int valueMask;
};
layout(std430, set = 0, binding = 1) readonly buffer ChunksBuffer {
    ChunkStruct[] chunks;
} chunkData;
layout(std430, set = 0, binding = 2) readonly buffer VoxelBuffer {
    int[] voxels;
} voxelData;
#extension GL_EXT_shader_explicit_arithmetic_types_int64 : require
layout(std430, set = 0, binding = 3) readonly buffer LODBuffer {
    int64_t[] lods;
} lodData;
const int size = 2048;
const int height = 320;
const vec3 worldSize = vec3(size, height, size);
const int chunkSize = 16;
const int sizeChunks = size / chunkSize;
const int heightChunks = height / chunkSize;
const int lodSize = 4;
const int sizeLods = size / lodSize;
const int heightLods = height / lodSize;
int packLodPos(int x, int y, int z) {
    return x+y*sizeLods+z*sizeLods*heightLods;
}
int packLodPos(ivec3 pos) {
    return packLodPos(pos.x, pos.y, pos.z);
}
int64_t getLod(ivec3 lodPos) {
    return int64_t(lodData.lods[packLodPos(lodPos)]);
}
int packPos(vec3 pos) {
    return int(pos.x)+int(pos.y)*size+int(pos.z)*(size*height);
}
ivec3 prevBlockChunkPos = ivec3(-1);
ChunkStruct chunk = ChunkStruct(0, 0, 0, 0);
int blockValuesPerInt = -1;
void updateChunkData(ivec3 chunkPos) {
    prevBlockChunkPos = chunkPos;
    int condensedChunkPos = (((chunkPos.x*sizeChunks)+chunkPos.z)*heightChunks)+chunkPos.y;
    chunk = chunkData.chunks[condensedChunkPos];
    blockValuesPerInt = 32/chunk.bitsPerValue;
}
void updateChunkData(int x, int y, int z) {
    updateChunkData(ivec3(x, y, z) >> 4);
}
int getBlockData(int x, int y, int z) {
    updateChunkData(x, y, z);
    ivec3 localPos = ivec3(x, y, z) & ivec3(15);
    int condensedLocalPos = ((((localPos.x*chunkSize)+localPos.z)*chunkSize)+localPos.y);
    int intIndex  = condensedLocalPos/blockValuesPerInt;
    int bitIndex = (condensedLocalPos - intIndex * blockValuesPerInt) * chunk.bitsPerValue;

    int index = chunk.pointer+chunk.paletteSize+intIndex;
    if (index < 0 || index >= 250000000) return 0;

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
layout(set = 0, binding = 4) uniform sampler3D atlas;
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
layout(set = 0, binding = 5) uniform sampler2D noises;
float noise(vec2 coords) {
    return (texture(noises, vec2(coords/1024)).r*2)-0.5f;
}
float getCaustic(bool animated, vec2 checkPos) {
    float time = animated ? globalUbo.time/1000 : 0.f;
    return noise((checkPos+time)*32);
}
layout(set = 0, binding = 9) uniform sampler2D rasterColors;
layout(set = 0, binding = 10) uniform sampler2D rasterDepth;
layout(set = 0, binding = 11) uniform sampler2D rasterNormals;

layout(location = 0) in vec2 uv;

layout(location = 0) out vec4 outColor;
layout(location = 1) out vec4 outNormal;

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

const int renderDistance = min(size, 2048);
vec3 ogPos = vec3(0);
vec3 ogDir = vec3(0);
vec3 ogRayPos = vec3(0);
vec3 ogRayDir = vec3(0);
vec3 sunColor = vec3(0);
bool hasAtmosphere = true;
vec3 sunsetColor = vec3(1, 0.65f, 0.25f); //vec3(0.65, 0.65f, 1)
vec3 skyColor = vec3(0.36f, 0.54f, 1.2f); //vec3(0.56f, 0.56f, 0.7f)
vec3 nightSkyColor = vec3(0.05f, 0.f, 0.225f); //vec3(0.56f, 0.56f, 0.7f)
float skyWhiteline = 0.9f; //0.8
float sunsetHeight = 1.5f; //1
float skyDensity = 1.f; //0.66
float sunBrightnessMul = 1.1f; //1
vec4 getLightingColor(vec3 lightPos, vec4 lighting, bool isSky, float fogginess, bool negateSun) {
    if (!hasAtmosphere) {
        fogginess = 0.f;
    }
    float ogY = ogPos.y;
    vec3 relativeSun = vec3(ogPos.x, 0, ogPos.z)+(globalUbo.sun*(1000/worldSize));
    float sunHeight = relativeSun.y/size;
    float scattering = gradient(lightPos.y, ogY-63, ogY+437, 1.5f, -0.5f);
    float sunDist = (distance(lightPos.xz, relativeSun.xz)/1500);
    float adjustedTime = clamp((sunDist*abs(1-clamp(sunHeight, 0.05f, 0.5f)))+scattering, 0.f, 1.f);
    float thickness = gradient(lightPos.y, 128, 1500-max(0, sunHeight*1000), 0.33+(sunHeight/2), 1);
    float sunSetness = min(1.f, max(abs(sunHeight*sunsetHeight), adjustedTime));
    float whiteY = max(ogY, 200)-135.f;
    float skyWhiteness = mix(max(0.33f, gradient(lightPos.y, (whiteY/4)+47, (whiteY/2)+436, 0, skyWhiteline)), 0.9f, clamp(abs(1-sunSetness), 0, 1.f));
    float sunBrightness = clamp(sunHeight+0.5, mix(-0.07f, 0.33f, skyWhiteness), 1);
    if (negateSun) {
        lighting.a = 0;
    }
    float whiteness = (isSky ? skyWhiteness : mix(skyWhiteline, skyWhiteness, max(0, fogginess-0.8f)*5.f))*clamp(sunHeight+0.8f-(min(sunSetness, 0.2f)*4), 0.33f, 1);
    sunColor = mix(mix(sunsetColor*(1+((10*clamp(sunHeight, 0.f, 0.1f))*(15*min(0.5f, abs(1-sunBrightness))))), mix(nightSkyColor, skyColor, clamp(sunHeight+0.5f, 0, 1))*sunBrightness, sunSetness), vec3(sunBrightness), whiteness);
    sunColor = min(mix(vec3(1), vec3(1, 0.95f, 0.85f), sunSetness/4), lighting.a*sunColor);
    if (!isSky && globalUbo.skylight.w >= 1.f) {
        sunColor*=min(sunBrightnessMul, sunBrightnessMul <= 1.f ? 1.f : max(1.f, sunBrightnessMul-fogginess));
    }
    vec4 color = vec4(max(lighting.rgb, sunColor), thickness);
    return isSky ? color*gradient(lightPos.y, 72, 320, skyDensity, 1) : color;
}

vec3 roundVec(vec3 dir) {
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

vec3 exactPos = vec3(0);
ivec3 blockPos = ivec3(0);
vec3 hitPos = vec3(0);
vec3 flatNormal = vec3(0);
const float bevel = 0.25f;
const float bevelMax = 1-bevel;
const float bevelOffset = 0.5f+(bevel/2);
vec3 bevelNormal(vec3 normal) {
    if (blockPos.x > 0 && blockPos.x < size-1 && blockPos.z > 0 && blockPos.z < size-1 && blockPos.y > 0 && blockPos.y < height-1) { //dont blend with voxels outside of world.
        vec3 localPos = roundVec(fract(hitPos-flatNormal));
        vec3 bevelPos = blockPos+0.5f;
        vec3 absFlatNorm = abs(normal);
        if (absFlatNorm.x < max(absFlatNorm.y, absFlatNorm.z)) {
            if (localPos.x > bevelMax) {
                if (getBlock(bevelPos+vec3(bevelOffset, 0, 0)).x == 0) { normal.x = 1; }
            } else if (localPos.x < bevel) {
                if (getBlock(bevelPos-vec3(bevelOffset, 0, 0)).x == 0) { normal.x = -1; }
            }
        }
        if (absFlatNorm.z < max(absFlatNorm.x, absFlatNorm.y)) {
            if (localPos.z > bevelMax) {
                if (getBlock(bevelPos+vec3(0, 0, bevelOffset)).x == 0) { normal.z = 1; }
            } else if (localPos.z < bevel) {
                if (getBlock(bevelPos-vec3(0, 0, bevelOffset)).x == 0) { normal.z = -1; }
            }
        }
        if (absFlatNorm.y < max(absFlatNorm.x, absFlatNorm.z)) {
            if (localPos.y > bevelMax) {
                if (getBlock(bevelPos+vec3(0, bevelOffset, 0)).x == 0) { normal.y = 1; }
            } else if (localPos.y < bevel) {
                if (getBlock(bevelPos-vec3(0, bevelOffset, 0)).x == 0) { normal.y = -1; }
            }
        }
    }
    return normal;
}

vec3 stepMask(vec3 blockSideDist) {
    bvec3 b1 = lessThan(blockSideDist.xyz, blockSideDist.yzx);
    bvec3 b2 = lessThanEqual(blockSideDist.xyz, blockSideDist.zxy);
    bvec3 blockMask = bvec3(
    b1.x && b2.x,
    b1.y && b2.y,
    b1.z && b2.z
    );
    if(!any(blockMask)) {
        blockMask.z = true;
    }

    return vec3(blockMask);
}

vec3 getDir(vec2 pos) {
    vec2 modifiedUV = (uv * 2.0) - 1.0;
    vec4 clipSpace = vec4((inverse(globalUbo.proj) * vec4(modifiedUV, 1.f, 1.f)).xyz, 0);
    return roundVec(normalize((inverse(globalUbo.view)*clipSpace).xyz));
}
vec3 rayPos = vec3(0);
vec3 rayDir = vec3(0);
vec3 uv3d(ivec3 worldPos, float stageSize, int nextStageSize) {
    vec3 minPos = (worldPos - rayPos) * (1.0 / rayDir);
    vec3 maxPos = (worldPos + stageSize - rayPos) * (1.0 / rayDir);
    vec3 entranceDist = min(minPos, maxPos);
    float entrancePos = max(0.f, max(entranceDist.x, max(entranceDist.y, entranceDist.z)));
    vec3 intersect = rayPos + rayDir * entrancePos;
    return clamp((intersect - worldPos)/(stageSize/nextStageSize), vec3(0.0001f), vec3(nextStageSize-0.0001f));
}
vec3 uv3d(vec3 worldPos, float stageSize, int nextStageSize) { //same thing as above but not ivec3
    vec3 minPos = (worldPos - rayPos) * (1.0 / rayDir);
    vec3 maxPos = (worldPos + stageSize - rayPos) * (1.0 / rayDir);
    vec3 entranceDist = min(minPos, maxPos);
    float entrancePos = max(0.f, max(entranceDist.x, max(entranceDist.y, entranceDist.z)));
    vec3 intersect = rayPos + rayDir * entrancePos;
    return clamp((intersect - worldPos)/(stageSize/nextStageSize), vec3(0.0001f), vec3(nextStageSize-0.0001f));
}

vec3 raySign = vec3(0);
vec3 blockDist = vec3(0);
vec3 normal = vec3(0);
ivec2 block = ivec2(0);
ivec3 voxelRayPos = ivec3(0);
vec4 dda(bool textured) {
    rayDir = roundVec(rayDir);
    ogRayPos = rayPos;
    ogRayDir = rayDir;
    raySign = sign(rayDir);
    int stage = 3;

    vec3 chunkStartPos = rayPos/chunkSize;
    vec3 chunkPos = floor(chunkStartPos);
    vec3 chunkDist = 1.0/rayDir;
    vec3 chunkSideDist = ((chunkPos - chunkStartPos) + 0.5 + raySign * 0.5) * chunkDist;
    vec3 chunkMask = stepMask(chunkSideDist);
    ivec3 chunkWorldPos = ivec3(0);

    vec3 lodStartPos = vec3(0);
    ivec3 lodRayPos = ivec3(0);
    vec3 lodPos = vec3(0);
    vec3 lodDist = vec3(0);
    vec3 lodSideDist = vec3(0);
    vec3 lodMask = vec3(0);
    int64_t lod = 0;
    ivec3 lodWorldPos = ivec3(0);

    vec3 blockStartPos = vec3(0);
    ivec3 blockRayPos = ivec3(0);
    vec3 blockSideDist = vec3(0);
    vec3 blockMask = vec3(0);

    vec3 voxelStartPos = vec3(0);
    voxelRayPos = ivec3(0);
    vec3 voxelPos = vec3(0);
    vec3 voxelDist = vec3(0);
    vec3 voxelSideDist = vec3(0);
    vec3 voxelMask = vec3(0);

    exactPos = ogRayPos;
    while (distance(exactPos, ogRayPos) < renderDistance) {
        bool stepAnything = true;
        if (stage == 3) {
            if (chunkPos.y >= heightChunks && rayDir.y < 0) {
                //no need to do any checks
            } else {
                if (chunkPos.x < 0 || chunkPos.x >= sizeChunks || chunkPos.y < 0 || chunkPos.y >= heightChunks || chunkPos.z < 0 || chunkPos.z >= sizeChunks) { break; }
                updateChunkData(ivec3(chunkPos));
                if (chunk.paletteSize > 1) {
                    stage = 2;
                    stepAnything = false;
                    chunkWorldPos = ivec3(chunkPos*chunkSize);
                    lodStartPos = uv3d(chunkWorldPos, float(chunkSize), lodSize);
                    lodRayPos = ivec3(lodStartPos);
                    lodDist = 1.0/rayDir;
                    lodSideDist = ((lodRayPos - lodStartPos) + 0.5 + raySign * 0.5) * lodDist;
                    lodMask = chunkMask;
                }
            }
        } else if (stage == 2) {
            if (lodRayPos.x < 0 || lodRayPos.x >= lodSize || lodRayPos.y < 0 || lodRayPos.y >= lodSize || lodRayPos.z < 0 || lodRayPos.z >= lodSize) { stage = 3; } else {
                lodPos = (chunkWorldPos/lodSize)+lodRayPos;
                lod = getLod(ivec3(lodPos));
                if (lod != 0) {
                    stage = 1;
                    stepAnything = false;
                    lodWorldPos = ivec3(lodPos*lodSize);
                    blockStartPos = uv3d(lodWorldPos, float(lodSize), lodSize);
                    blockRayPos = ivec3(blockStartPos);
                    blockDist = 1.0/rayDir;
                    blockSideDist = ((blockRayPos - blockStartPos) + 0.5 + raySign * 0.5) * blockDist;
                    blockMask = lodMask;
                }
            }
        } else if (stage == 1) {
            if (blockRayPos.x < 0 || blockRayPos.x >= lodSize || blockRayPos.y < 0 || blockRayPos.y >= lodSize || blockRayPos.z < 0 || blockRayPos.z >= lodSize) { stage = 2; } else {
                int bitIdx = (blockRayPos.x % lodSize) + (blockRayPos.y % lodSize) * lodSize + (blockRayPos.z % lodSize) * lodSize * lodSize;
                int64_t mask = int64_t(1) << bitIdx;
                if ((lod & mask) != 0) {
                    blockPos = lodWorldPos+blockRayPos;
                    block = getBlock(blockPos);
                    if (block.x > 0) {
                        voxelStartPos = uv3d(blockPos, 1.f, blockSize);
                        vec4 voxelColor = block.x == 4 ? vec4(0) : sampleAtlas(int(voxelStartPos.x), int(voxelStartPos.y), int(voxelStartPos.z), block.x, block.y);
                        if (voxelColor.a > 0.f) {
                            flatNormal = -blockMask*raySign;
                            voxelRayPos = ivec3(voxelStartPos);
                            hitPos = blockPos+(voxelStartPos/blockSize)+(flatNormal*0.001f);
                            normal = bevelNormal(flatNormal);
                            return vec4(voxelColor.rgb, 1.f);
                        } else {
                            stage = 0;
                            stepAnything = false;
                            voxelRayPos = ivec3(voxelStartPos);
                            voxelDist = 1.0/rayDir;
                            voxelSideDist = ((voxelRayPos - voxelStartPos) + 0.5 + raySign * 0.5) * voxelDist;
                            voxelMask = blockMask;
                        }
                    }
                }
            }
        } else if (stage == 0) {
            if (voxelRayPos.x < 0 || voxelRayPos.x >= blockSize || voxelRayPos.y < 0 || voxelRayPos.y >= blockSize || voxelRayPos.z < 0 || voxelRayPos.z >= blockSize) { stage = 1; } else {
                ivec3 offsetVoxelPos = voxelRayPos;
                if (block.x == 4 && offsetVoxelPos.y > 2) {
                    bool windDir = true;//timeOfDay > 0.f;
                    float time = globalUbo.time/400000;
                    float windStr = noise(((vec2(blockPos.x, blockPos.z)/12) + (time * 100)) * (16+(time/(time/32))))+0.5f;
                    if (windStr > 0.8) {
                        offsetVoxelPos.x = offsetVoxelPos.x+((offsetVoxelPos.y > 5 ? 3 : (offsetVoxelPos.y > 4 ? 2 : 1)) * (windDir ? -1 : 1));
                        if (block.y < 2) {
                            offsetVoxelPos.z = offsetVoxelPos.z+(offsetVoxelPos.y > 4 ? 2 : 1);
                        }
                    } else if (windStr > 0.4) {
                        offsetVoxelPos.x = offsetVoxelPos.x+((offsetVoxelPos.y > 5 ? 3 : (offsetVoxelPos.y > 4 ? 2 : 1)) * (windDir ? -1 : 1));
                        if (block.y < 2) {
                            offsetVoxelPos.z = offsetVoxelPos.z+(offsetVoxelPos.y > 4 ? 1 : 0);
                        }
                    } else if (windStr > -0.2) {
                        offsetVoxelPos.x = offsetVoxelPos.x+((offsetVoxelPos.y > 4 ? 2 : 1) * (windDir ? -1 : 1));
                        if (block.y < 2) {
                            offsetVoxelPos.z = offsetVoxelPos.z+(offsetVoxelPos.y > 4 ? 1 : 0);
                        }
                    } else if (windStr > -0.8) {
                        offsetVoxelPos.x = offsetVoxelPos.x+((offsetVoxelPos.y > 4 ? 1 : 0) * (windDir ? -1 : 1));
                    }
                    offsetVoxelPos.xz = clamp(offsetVoxelPos.xz, 0, 7);
                }
                vec4 voxelColor = sampleAtlas(offsetVoxelPos.x, offsetVoxelPos.y, offsetVoxelPos.z, block.x, block.y);
                if (voxelColor.a > 0.f) {
                    flatNormal = -voxelMask*raySign;
                    normal = flatNormal;
                    voxelPos = blockPos+(voxelRayPos*voxelSize);
                    vec3 subvoxelPos = (uv3d(voxelPos, voxelSize, blockSize)/blockSize)*voxelSize;
                    hitPos = voxelPos+(subvoxelPos/(blockSize*blockSize))+(flatNormal*0.001f);
                    return vec4(voxelColor.rgb, 1.f);
                }
            }
        }
        if (stepAnything) { //dont step if it just went to a finer detail
            if (stage == 3) {
                chunkMask = stepMask(chunkSideDist);
                chunkPos += chunkMask * raySign;
                chunkSideDist += chunkMask * raySign * chunkDist;
                exactPos = chunkPos*chunkSize;
            } if (stage == 2) {
                lodMask = stepMask(lodSideDist);
                lodRayPos += ivec3(lodMask * raySign);
                lodSideDist += lodMask * raySign * lodDist;
                exactPos = chunkWorldPos+(lodRayPos*lodSize);
            } else if (stage == 1) {
                blockMask = stepMask(blockSideDist);
                blockRayPos += ivec3(blockMask * raySign);
                blockSideDist += blockMask * raySign * blockDist;
            } else if (stage == 0) {
                voxelMask = stepMask(voxelSideDist);
                voxelRayPos += ivec3(voxelMask * raySign);
                voxelSideDist += voxelMask * raySign * voxelDist;
            }
        }
    }
    return vec4(0);
}

vec3 frensel(float ang, vec3 reflectivity) {
    return reflectivity+(vec3(1)-reflectivity)*pow(1-ang, 5);
}
vec3 scatterVec(vec3 vec) {
    vec = fract(vec * 0.1031);
    vec += dot(vec, vec + 33.33);
    return fract((vec.xxy + vec.yzz) * vec.zyx);
}
vec3 mipmap(vec3 color) {
    int inc = 3;
    vec3 subbed = vec3(dot(-flatNormal.x, rayDir.x), dot(-flatNormal.y, rayDir.y), dot(-flatNormal.z, rayDir.z));
    bool xHighest = subbed.x > subbed.y && subbed.x > subbed.z;
    bool yHighest = subbed.y > subbed.x && subbed.y > subbed.z;
    bool zHighest = subbed.z > subbed.x && subbed.z > subbed.y;
    vec3 avgNColor = vec3(0);
    float maxBrightness = 0;
    float neighborsSolid = 0.f;
    for (int x = xHighest ? voxelRayPos.x : voxelRayPos.x-inc; x <= voxelRayPos.x+inc; x+= xHighest ? 100 : inc) {
        for (int y = yHighest ? voxelRayPos.y : voxelRayPos.y-inc; y <= voxelRayPos.y+inc; y+= yHighest ? 100 : inc) {
            for (int z = zHighest ? voxelRayPos.z : voxelRayPos.z-inc; z <= voxelRayPos.z+inc; z+= zHighest ? 100 : inc) {
                vec4 neighbor = sampleAtlasTiled(x, y, z, block.x, block.y);
                if (neighbor.a >= 1.f) {
                    float brightness = max(neighbor.r, max(neighbor.g, neighbor.b));
                    maxBrightness = max(maxBrightness, brightness);
                    avgNColor += (neighbor.rgb)*brightness;
                    neighborsSolid+=1*brightness;
                }
            }
        }
    }
    if (neighborsSolid > 0) {
        avgNColor /= neighborsSolid;
        float dist = min(1, distance(ogPos, blockPos+(voxelRayPos/float(blockSize)))/100);
        color = mix(color, avgNColor, dist);//clamp(mix(color, avgNColor, clamp(dist-max(0, 4*(max(color.r, max(color.g, color.b))-0.5f)), 0, 1)), 0, 1);
    }
    return color;
}
const float Z_NEAR = 0.01f;
void main() {
    vec3 camPos = inverse(globalUbo.view)[3].xyz;
    ogPos = camPos;
    ogDir = getDir(uv);
    rayPos = ogPos;
    rayDir = ogDir;
    vec4 color = dda(true);
    vec3 primaryNormal = normal;
    vec3 primaryLightPos = hitPos+(flatNormal*voxelSize);
    bool isSky = color.a < 1;
    float depth = 0.f;
    if (isSky) {
        primaryLightPos = ogPos + ogDir * renderDistance;
    } else {
        vec4 clipPos = globalUbo.proj * (globalUbo.view * vec4(primaryLightPos, 1.0));
        depth = clipPos.z/clipPos.w;
    }
    bool celestial = false;
    float rasterDepth = texture(rasterDepth, uv).r;
    float reflectivity = block.x == 1 ? 1.f : 0.f;
    float roughness = block.x == 1 ? 0.1f : 0.f;
    if (rasterDepth > depth) {
        isSky = false;
        depth = rasterDepth;
        color = texture(rasterColors, uv);
        primaryNormal = texture(rasterNormals, uv).xyz;
        flatNormal = primaryNormal;
        vec4 clip = vec4(uv*2.0f-1.0f, depth, 1.0f);
        vec4 view = inverse(globalUbo.proj)*clip;
        view/=view.w;
        primaryLightPos = (inverse(globalUbo.view)*view).xyz;
        blockPos = ivec3(primaryLightPos);
        block = ivec2(0);
//        reflectivity = 0.5f;
//        roughness = 0.25f;
        voxelRayPos = ivec3((ivec3(primaryLightPos)-primaryLightPos)*blockSize);
        if (primaryLightPos.x < 0 || primaryLightPos.y < 0 || primaryLightPos.z < 0 || primaryLightPos.x >= size || primaryLightPos.y >= height  || primaryLightPos.z >= size) {
            celestial = true;
        }
    } else {
        color.rgb = mipmap(color.rgb);
    }
    if (!celestial) {
        vec3 primaryFlatNormal = flatNormal;
        ivec3 primaryBlockPos = blockPos;
        ivec3 primaryVoxelRayPos = voxelRayPos;
        ivec2 primaryBlock = block;
        vec3 absNorm = abs(primaryFlatNormal);
        vec3 causticPos = block.x == 1 ? vec3(primaryBlockPos) : primaryLightPos;
        vec3 causticVoxelPos = primaryVoxelRayPos;
        if (block.x != 1) {
            causticVoxelPos = vec3(0);
        }
        float causticness = absNorm.y > max(absNorm.x, absNorm.z) ? getCaustic(block.x == 1, vec2(causticPos.xz)+(causticVoxelPos.xz/8.f)) :
        (absNorm.z > max(absNorm.x, absNorm.y) ? getCaustic(block.x == 1, vec2(causticPos.xy)+(causticVoxelPos.xy/8.f)) :
        getCaustic(block.x == 1, vec2(causticPos.yz)+(causticVoxelPos.yz/8.f)));
        if (primaryBlock.x == 1 && abs(causticness) < 0.033f) {
            color = vec4(1);
        }
        if (causticness > 0) {
            causticness *= -2;
        } else {
            causticness *= 5;
        }
        vec3 tiltedNormal = primaryNormal*causticness;
        vec3 bentNormal = mix(primaryNormal, tiltedNormal, roughness*2);

        vec4 skylight = globalUbo.skylight;
        float normDot = dot(bentNormal, normalize(skylight.xyz));
        vec3 lighting = (vec3(normDot*0.3f/min(1, skylight.a*2))+(0.1f+(0.6f*skylight.a)))*(0.05f+(skylight.a*0.95f));
        vec3 sunDir = vec3(normalize(max(vec3(size*-10, 1000, size*-10), skylight.xyz) - (worldSize/2)));
        rayPos = primaryLightPos;
        rayDir = sunDir;
        vec4 shadowColor = vec4(0);
        if (normDot < 0.f) {
            shadowColor.a = 1.f;
        } else {
            shadowColor = dda(false);
        }
        if (shadowColor.a > 0.0f) {
            lighting *= 0.66f;
        }
        if (reflectivity > 0.f) {
            vec3 idealReflectDir = reflect(ogDir, primaryNormal);
            vec3 reflectDir = roundVec(mix(idealReflectDir, (idealReflectDir/2)+(reflect(ogDir, tiltedNormal)/2), roughness));
            vec3 viewDir = normalize(ogDir);
            vec3 halfVec = normalize(viewDir-reflectDir);
            float ang = max(dot(viewDir, halfVec), 0.f);
            vec3 frensel = frensel(ang, vec3(0.02f, 0.019f, 0.018f));
            rayPos = primaryLightPos;
            rayDir = reflectDir;
            vec4 reflectColor = dda(false);
            if (reflectColor.a < 1.f) {
                reflectColor = getLightingColor(primaryLightPos + reflectDir * renderDistance, vec4(0, 0, 0, 1.f), true, 1, false);
            } else {
                //reflectColor.rgb = mipmap(reflectColor.rgb); //can be disabled with minimal quality degradation.
                vec3 lightPos = hitPos+(flatNormal*voxelSize);
                float fogginess = clamp(sqrt(distance(camPos, lightPos)/(renderDistance*0.66f))-0.15f, 0.f, 1.f);
                reflectColor.rgb = mix(reflectColor.rgb, getLightingColor(lightPos, vec4(0, 0, 0, 1.f), false, fogginess, false).rgb, fogginess);
            }
            color.rgb = mix(color.rgb, reflectColor.rgb, ((frensel*0.75f)+0.25f)*reflectivity);
        }
        color.rgb *= lighting;
        float fogginess = isSky ? 1.f : clamp(sqrt(distance(camPos, primaryLightPos)/(renderDistance*0.66f))-0.15f, 0.f, 1.f);
        color.rgb = mix(color.rgb, getLightingColor(primaryLightPos, vec4(0, 0, 0, 1.f), isSky, fogginess, false).rgb, fogginess);
        outNormal = vec4(primaryNormal, fogginess);
    } else {
        outNormal = vec4(primaryNormal, 1);
    }
    outColor = vec4(color.rgb, 1);
    //outColor = vec4(primaryLightPos.rgb/worldSize, 1);
    gl_FragDepth = depth;
}