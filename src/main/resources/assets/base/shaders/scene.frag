#version 460

uniform vec2 res;
uniform mat4 cam;
uniform int renderDistance;
layout(std430, binding = 0) buffer atlasSSBO
{
    int[] atlasData;
};
layout(std430, binding = 1) buffer blockChunksSSBO
{
    int[] blockChunksData;
};
layout(std430, binding = 2) buffer blocksSSBO
{
    int[] blocksData;
};
in vec4 gl_FragCoord;

out vec4 fragColor;

int size = 6144;
int height = 320;
int chunkSize = 16;
int sizeChunks = size/chunkSize;
int heightChunks = height/chunkSize;

vec3 rayMapPos = vec3(0);

vec4 intToColor(int color) {
    return vec4(0xFF & color >> 16, 0xFF & color >> 8, 0xFF & color, 0xFF & color >> 24);
}

vec4 getVoxel(int x, int y, int z, int bX, int bY, int bZ, int blockType, int blockSubtype) {
    return intToColor(atlasData[(9984*((blockType*8)+x)) + (blockSubtype*64) + ((abs(y-8)-1)*8) + z])/255;
}
vec4 getVoxel(float x, float y, float z, float bX, float bY, float bZ, int blockType, int blockSubtype) {
    return getVoxel(int(x), int(y), int(z), int(bX), int(bY), int(bZ), blockType, blockSubtype);
}

int getBlockData(int x, int y, int z) {
    ivec3 chunkPos = ivec3(x/chunkSize, y/chunkSize, z/chunkSize);
    ivec3 localPos = ivec3(x-(chunkPos.x*chunkSize), y-(chunkPos.y*chunkSize), z-(chunkPos.z*chunkSize));
    int condensedChunkPos = (((chunkPos.x*sizeChunks)+chunkPos.z)*heightChunks)+chunkPos.y;
    int pointer = blockChunksData[condensedChunkPos*2];
    int paletteSize = blockChunksData[(condensedChunkPos*2)+1];
    int condensedLocalPos = ((((localPos.x*chunkSize)+localPos.z)*chunkSize)+localPos.y);
    int blocksPerInt = 2;

    blocksPerInt = 32;
    int specificInt = (condensedLocalPos/blocksPerInt);
    int values = blocksData[pointer+paletteSize+specificInt];
    int whereInInt = condensedLocalPos-(specificInt*blocksPerInt);
    return blocksData[pointer+((values >> whereInInt) & 1)];
}
ivec2 getBlock(int x, int y, int z) {
    int blockData = getBlockData(x, y, z);
    return ivec2((blockData >> 16) & 0xFFFF, blockData & 0xFFFF);
}
ivec2 getBlock(vec3 pos) {
    return getBlock(int(pos.x), int(pos.y), int(pos.z));
}
bool isChunkAir(int x, int y, int z) {
    if (x >= 0 && x < sizeChunks && y >= 0 && y < heightChunks && z >= 0 && z < sizeChunks) {
        int condensedChunkPos = (((x*sizeChunks)+z)*heightChunks)+y;
        int pointer = blockChunksData[condensedChunkPos*2];
        int paletteSize = blockChunksData[(condensedChunkPos*2)+1];
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
        if (voxelColor.a >= 1) {
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

            return vec4(vec3(voxelColor)*brightness, 1);
        }

        prevVoxelColor = voxelColor;
        prevMapPos = mapPos;
        mask = stepMask(sideDist);
        mapPos += mask * raySign;
        sideDist += mask * raySign * deltaDist;
    }

    return vec4(0.0);
}

vec4 dda(vec3 rayPos, vec3 rayDir) {
    rayMapPos = floor(rayPos);
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((rayMapPos-rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = stepMask(sideDist);

    while (distance(rayMapPos, rayPos) < 24) {
        if (rayMapPos.x >= 0 && rayMapPos.x < size && rayMapPos.y >= 0 && rayMapPos.y < height && rayMapPos.z >= 0 && rayMapPos.z < size) {
            //block start
            ivec2 blockInfo = getBlock(vec3(rayMapPos.x, rayMapPos.y, rayMapPos.z));

            vec3 mini = ((rayMapPos-rayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
            float d = max (mini.x, max (mini.y, mini.z));
            vec3 intersect = rayPos + rayDir*d;
            vec3 uv3d = intersect - rayMapPos;

            if (rayMapPos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
                uv3d = rayPos - rayMapPos;
            }
            //block end

            vec4 color = vec4(1, 1, 1, 0);
            if (blockInfo.x != 0.f) {
                color = traceBlock(uv3d * 8.0, rayDir, mask, blockInfo.x, blockInfo.y);
            }
            if (color.a >= 1) {
                return color;
            }

            mask = stepMask(sideDist);
            rayMapPos += mask * raySign;
            sideDist += mask * raySign * deltaDist;
        } else {
            return vec4(0, 0, 1, 1);
        }
    }

    return vec4(0.0);
}


vec4 traceWorld(vec3 rayOg, vec3 dir) {
    ivec3 bDir = ivec3(dir.x < 0 ? -1 : (dir.x > 0 ? 1 : 0), dir.y < 0 ? -1 : (dir.y > 0 ? 1 : 0), dir.z < 0 ? -1 : (dir.z > 0 ? 1 : 0));
    float traveled = 0f;
    vec3 rayPos = rayOg;
    while (distance(rayOg, rayPos) < renderDistance) {
        if (rayPos.x >= 0 && rayPos.x < size && rayPos.y >= 0 && rayPos.y < height && rayPos.z >= 0 && rayPos.z < size) {
            rayPos = vec3(cam * vec4((dir * traveled), 1));
            bool nearVisibleBlock = false;
            for (int x = min(0, bDir.x); x <= max(0, bDir.x) && !nearVisibleBlock; x++) {
                for (int z = min(0, bDir.y); z <= max(0, bDir.y) && !nearVisibleBlock; z++) {
                    for (int y = min(0, bDir.z); y <= max(0, bDir.z); y++) {
                        if (!isChunkAir(int(rayPos.x/16)+x, int(rayPos.y/16)+y, int(rayPos.z/16)+z)) {
                            nearVisibleBlock = true;
                            break;
                        }
                    }
                }
            }
            if (nearVisibleBlock) {
                mat4 offsetCam = cam;
                vec3 prevRayPos = vec3(cam * vec4((dir * max(0, traveled-16)), 1));
                offsetCam[3] = vec4(prevRayPos, 0);
                vec4 color = dda(prevRayPos, vec3(offsetCam*vec4(dir, 0)));
                if (color.a > 0) {
                    return color;
                }
            }
            traveled+=16;
        } else {
            return vec4(0, 0, 1, 1);
        }
    }
    return vec4(0);

    //return dda(rayOg, vec3(cam*vec4(dir, 0)));
}

void main()
{
    vec2 uv = (vec2(gl_FragCoord)*2. - res.xy) / res.y;
    if (uv.x >= -0.004 && uv.x <= 0.004 && uv.y >= -0.004385 && uv.y <= 0.004385) {
        fragColor = vec4(0.9, 0.9, 1, 1);
    } else {
        vec3 camPos = vec3(cam[3]);
        vec3 dir = normalize(vec3(uv, 1));
        fragColor = traceWorld(camPos/16, dir);
    }
}