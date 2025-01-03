#version 460
#extension GL_NV_gpu_shader5 : enable

uniform float timeOfDay;
uniform double time;
uniform int renderDistance;
uniform vec2 res;
uniform mat4 cam;
uniform ivec3 selected;
uniform bool ui;
uniform vec3 sun;
uniform bool raytrace;
layout(binding = 0) uniform sampler2D coherent_noise;
layout(binding = 1) uniform sampler2D white_noise;
layout(std430, binding = 0) buffer atlas
{
    int[] atlasData;
};
layout(std430, binding = 1) buffer chunks
{
    int[] chunksData;
};
layout(std430, binding = 2) buffer region1
{
    int[] region1BlockData;
};
layout(std430, binding = 3) buffer chunksLight
{
    int[] chunksLightData;
};
layout(std430, binding = 4) buffer region1Lighting
{
    int[] region1LightingData;
};
layout(std430, binding = 5) buffer region1Corners
{
    int8_t[] region1CornersData;
};
in vec4 gl_FragCoord;

out vec4 fragColor;

int size = 1024;
int height = 320;
int chunkSize = 16;
int sizeChunks = size/chunkSize;
int heightChunks = height/chunkSize;
vec3 rayMapPos = vec3(0);
vec3 sunBrightness = vec3(0);
vec3 sunColor = vec3(0);
float fogginess = 0.f;
vec3 lightPos = vec3(0);
vec4 lighting = vec4(0);
vec4 lightFog = vec4(0);
vec3 hitPos = vec3(256);
vec4 tint = vec4(1, 1, 1, 0);
float cloudiness = 0;

vec4 intToColor(int color) {
    return vec4(0xFF & color >> 16, 0xFF & color >> 8, 0xFF & color, 0xFF & color >> 24);
}

bool[8] getCorners(int x, int y, int z) {
    int8_t data = region1CornersData[(((x*size)+z)*height)+y];
    //return bool[8]((data & (int8_t(1) << 7)) != 0, (data & (int8_t(1) << 6)) != 0, (data & (int8_t(1) << 5)) != 0, (data & (int8_t(1) << 4)) != 0, (data & (int8_t(1) << 3)) != 0, (data & (int8_t(1) << 2)) != 0, (data & (int8_t(1) << 1)) != 0, (data & (int8_t(1) << 0)) != 0);
    return bool[8](data == 0, data == 0, data == 0, data == 0, data == 0, data == 0, data == 0, data == 0);
}

vec4 getVoxel(int x, int y, int z, int bX, int bY, int bZ, int blockType, int blockSubtype) {
    bool[8] corners = getCorners(bX, bY, bZ);
    if (x < 4) {
        if (z < 4) {
            if (y < 4) {
                if (!corners[0]) {
                    return vec4(0);
                }
            } else {
                if (!corners[1]) {
                    return vec4(0);
                }
            }
        } else {
            if (y < 4) {
                if (!corners[2]) {
                    return vec4(0);
                }
            } else {
                if (!corners[3]) {
                    return vec4(0);
                }
            }
        }
    } else {
        if (z < 4) {
            if (y < 4) {
                if (!corners[4]) {
                    return vec4(0);
                }
            } else {
                if (!corners[5]) {
                    return vec4(0);
                }
            }
        } else {
            if (y < 4) {
                if (!corners[6]) {
                    return vec4(0);
                }
            } else {
                if (!corners[7]) {
                    return vec4(0);
                }
            }
        }
    }
    return intToColor(atlasData[(9984*((blockType*8)+x)) + (blockSubtype*64) + ((abs(y-8)-1)*8) + z])/255;
}
vec4 getVoxel(float x, float y, float z, float bX, float bY, float bZ, int blockType, int blockSubtype) {
    return getVoxel(int(x), int(y), int(z), int(bX), int(bY), int(bZ), blockType, blockSubtype);
}

int getBlockData(int x, int y, int z) {
    ivec3 chunkPos = ivec3(x/chunkSize, y/chunkSize, z/chunkSize);
    ivec3 localPos = ivec3(x-(chunkPos.x*chunkSize), y-(chunkPos.y*chunkSize), z-(chunkPos.z*chunkSize));
    return region1BlockData[
        chunksData[(((chunkPos.x*sizeChunks)+chunkPos.z)*heightChunks)+chunkPos.y]+
        ((((localPos.x*chunkSize)+localPos.z)*chunkSize)+localPos.y)
    ];
}
ivec2 getBlock(int x, int y, int z) {
    int blockData = getBlockData(x, y, z);
    return ivec2((blockData >> 16) & 0xFFFF, blockData & 0xFFFF);
}
ivec2 getBlock(float x, float y, float z) {
    return getBlock(int(x), int(y), int(z));
}

vec4 getLighting(int x, int y, int z) {
    ivec2 block = getBlock(x, y, z);
    if (block.x != 0 && block.x != 1 && block.x != 4 && block.x != 5 && block.x != 6 && block.x != 7 && block.x != 8 && block.x != 9 && block.x != 11 && block.x != 12 && block.x != 13 && block.x != 14 && block.x != 17) { //return pure darkness if block isnt transparent.
        return vec4(0, 0, 0, 0);
    }
    ivec3 chunkPos = ivec3(x/chunkSize, y/chunkSize, z/chunkSize);
    ivec3 localPos = ivec3(x-(chunkPos.x*chunkSize), y-(chunkPos.y*chunkSize), z-(chunkPos.z*chunkSize));
    return intToColor(region1LightingData[
        chunksLightData[(((chunkPos.x*sizeChunks)+chunkPos.z)*heightChunks)+chunkPos.y]+
        ((((localPos.x*chunkSize)+localPos.z)*chunkSize)+localPos.y)
    ]);
}
vec4 getLighting(float x, float y, float z) {
    return getLighting(int(x), int(y), int(z));
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

float noise(vec2 coords) {
    return (texture(coherent_noise, coords/1024).r)-0.5;
}
float whiteNoise(vec2 coords) {
    return (texture(white_noise, coords/1024).r)-0.5;
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
        if (voxelColor.a > 0.f && voxelColor.a < 1.f) {
            if (hitPos == vec3(256)) {
                hitPos = rayMapPos;
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
                hitPos = rayMapPos;
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
//                ivec2 aboveBlocKInfo = getBlock(rayMapPos.x, rayMapPos.y+1, rayMapPos.z);
//                aboveBlockType = aboveBlocKInfo.x;
//                aboveBlockSubtype = aboveBlocKInfo.y;
//            }
//            vec4 aboveColorData = getVoxel(mapPos.x, aboveY, mapPos.z, aboveBlockType, aboveBlockSubtype);
//            if (aboveColorData.a <= 0 || aboveBlockType == 4 || aboveBlockType == 5) {
//                voxelColor = mix(voxelColor, vec4(1-(abs(noise(vec2(int(mapPos.x)*64, int(mapPos.z)*64)))*0.66f))-vec4(0.14, 0.15, -0.05, 0), 0.9f);
//                brightness = 1f;
//            }
            //snow end
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

vec4 traceWorld(vec3 rayPos, vec3 rayDir) {

    rayMapPos = floor(rayPos);
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((rayMapPos-rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = stepMask(sideDist);

    while (distance(rayMapPos, rayPos) < renderDistance) {
        bool inBounds = rayMapPos.y >= 0 && rayMapPos.y < height && rayMapPos.x >= 0 && rayMapPos.x < size && rayMapPos.z >= 0 && rayMapPos.z < size;

        vec3 flattenedPos = rayMapPos;
        if (flattenedPos.y < height/2) {
            if (flattenedPos.y > 1) {
                flattenedPos.y = height/2;
            } else {
                flattenedPos.y = -100000;
            }
        }
        float thisFogginess = min(1, abs(clamp((distance(flattenedPos, vec3(size/2, height/2, size/2))+distance(flattenedPos.y, height/2))/(size/2.013), 0, 1)-1)*50);
        fogginess = max(fogginess, thisFogginess);
        float adjustedTime = min(1, abs(1-clamp((distance(flattenedPos, sun)+distance(flattenedPos.y, sun.y-(height/2)))/(size/2.013), 0, 1))*2)*fogginess;
        vec3 potentialSunBrightness = mix(vec3(1, 0.66, 0.05), vec3(1, 1, 0.98), adjustedTime)*adjustedTime;
        if (max(sunBrightness.r, max(sunBrightness.g, sunBrightness.b)) < max(potentialSunBrightness.r, max(potentialSunBrightness.g, potentialSunBrightness.b))) {
            sunBrightness = potentialSunBrightness;
        }

        float blueness = min(0.1, max(0, adjustedTime-0.85))*10;
        vec3 potentialSunColor = (mix(vec3(0.66, 0.4, -0.25), vec3(1, mix(0.05, 0.2, blueness), mix(-0.9, 0.2, blueness)), adjustedTime)*adjustedTime)*1.66f;
        if (max(sunColor.r, max(sunColor.g, sunColor.b)) < max(potentialSunColor.r, max(potentialSunColor.g, potentialSunColor.b))) {
            sunColor = potentialSunColor;
        }

        //block start
        ivec2 blockInfo = getBlock(rayMapPos.x, rayMapPos.y, rayMapPos.z);

        if (!inBounds) {
            blockInfo = ivec2(0);
        }

        vec3 mini = ((rayMapPos-rayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
        float d = max (mini.x, max (mini.y, mini.z));
        vec3 intersect = rayPos + rayDir*d;
        vec3 uv3d = intersect - rayMapPos;

        if (rayMapPos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
            uv3d = rayPos - rayMapPos;
        }
        //block end


        //cloud start
        float offset = distance(vec2(rayMapPos.x, rayMapPos.z), vec2(size/2))*0.2f;
        vec3 cloudPos = vec3(rayMapPos.x+(timeOfDay*2400), rayMapPos.y+offset-(timeOfDay*34), rayMapPos.z+(timeOfDay*4700));

        float potentialCloudiness = 0.f;
        if (cloudPos.y > 192 && cloudPos.y < 300) {
            potentialCloudiness += noise(vec2(cloudPos.x, cloudPos.z)/4)*min(gradient(cloudPos.y, 256, 300, 0, 2), gradient(cloudPos.y, 202, 256, 2, 0));
        }
        if (cloudPos.y > 246 && cloudPos.y < 400) {
            potentialCloudiness += (noise(vec2(cloudPos.x, cloudPos.z)/2)+0.2f)*min(gradient(cloudPos.y, 333, 400, 0, 2), gradient(cloudPos.y, 256, 333, 2, 0));
        }
        cloudiness = max(cloudiness, min(0.99f, (potentialCloudiness*potentialCloudiness)*thisFogginess));
        if (cloudiness >= 1) {
            break;
        }
        //cloud end

        vec4 color = vec4(1, 1, 1, 0);
        if (inBounds) {
            if (blockInfo.x != 0.f) {
                color = traceBlock(uv3d * 8.0, rayDir, mask, blockInfo.x, blockInfo.y);
            }

            //lighting start
            vec3 relativePos = lightPos-rayMapPos;
            vec3 centerPos = lightPos.x == 0 && lightPos.y == 0 && lightPos.z == 0 ? rayMapPos : lightPos;
            vec4 centerLighting = getLighting(centerPos.x, centerPos.y, centerPos.z);
            if (!(lightPos.x == 0 && lightPos.y == 0 && lightPos.z == 0)) {
                //smooth lighting start
                vec4 verticalLighting = getLighting(lightPos.x, lightPos.y+(relativePos.y >= 0.5f ? 0.5f : -0.5f), lightPos.z);
                verticalLighting = mix(relativePos.y >= 0.5f ? centerLighting : verticalLighting, relativePos.y >= 0.5f ? verticalLighting : centerLighting, relativePos.y);
                vec4 northSouthLighting = getLighting(lightPos.x, lightPos.y, lightPos.z+(relativePos.z >= 0.5f ? 0.5f : -0.5f));
                northSouthLighting = mix(relativePos.z >= 0.5f ? centerLighting : northSouthLighting, relativePos.z >= 0.5f ? northSouthLighting : centerLighting, relativePos.z);
                vec4 eastWestLighting = getLighting(lightPos.x+(relativePos.x >= 0.5f ? 0.5f : -0.5f), lightPos.y, lightPos.z);
                eastWestLighting = mix(relativePos.x >= 0.5f ? centerLighting : eastWestLighting, relativePos.x >= 0.5f ? eastWestLighting : centerLighting, relativePos.x);
                lighting = mix(mix(mix(eastWestLighting, verticalLighting, 0.25), mix(northSouthLighting, verticalLighting, 0.25), 0.5), centerLighting, distance(rayPos, lightPos)/renderDistance);
                //smooth lighting end
            } else {
                lighting = centerLighting;
            }
            lightFog = vec4(max(lightFog.r, lighting.r/2), max(lightFog.g, lighting.g/2), max(lightFog.b, lighting.b/2), max(lightFog.a, lighting.a == 20 ? lighting.a/2 : lighting.a/2.2));
            //lighting end
        } else {
            lighting = vec4(0, 0, 0, 20);
            lightFog = vec4(max(lightFog.r, lighting.r/2), max(lightFog.g, lighting.g/2), max(lightFog.b, lighting.b/2), max(lightFog.a, lighting.a == 20 ? lighting.a/2 : lighting.a/2.2));
        }

        //snow start
//        if (blockInfo.x == 0 && lighting.a == 20) {
//            float samp = whiteNoise((vec2(rayMapPos.x, rayMapPos.z)*64)+(rayMapPos.y+(float(time)*7500)));
//            float samp2 = noise(vec2(rayMapPos.x, rayMapPos.z)*8);
//            if (samp > 0 && samp < 0.002 && samp2 > 0.0f && samp2 < 0.05f) {
//                color = vec4(1, 1, 1, 1);
//            }
//        }
        //snow end

        if (color.a >= 1) {
            return color;
        }

        mask = stepMask(sideDist);
        rayMapPos += mask * raySign;
        sideDist += mask * raySign * deltaDist;
    }

    lighting = vec4(0, 0, 0, 20);
    lightFog = vec4(max(lightFog.r, lighting.r/2), max(lightFog.g, lighting.g/2), max(lightFog.b, lighting.b/2), max(lightFog.a, lighting.a/2));
    return vec4(0.0);
}


void main()
{
    vec2 uv = (vec2(gl_FragCoord)*2. - res.xy) / res.y;
    if (ui && uv.x >= -0.004 && uv.x <= 0.004 && uv.y >= -0.004385 && uv.y <= 0.004385) {
        fragColor = vec4(0.9, 0.9, 1, 1);
    } else if (raytrace) {
        vec3 camPos = vec3(cam[3]);
        vec3 dir = vec3(cam*vec4(normalize(vec3(uv, 1)), 0));
        fragColor = traceWorld(camPos, dir);
        float whiteness = gradient(rayMapPos.y, 0, 96, 0.f, -0.5f)+gradient(rayMapPos.y, 96, 152, 0.f, 0.3f)+gradient(rayMapPos.y, 152, 372, -2.1f, 0.f)+gradient(rayMapPos.y, 372, 555, -0.2f, 0.f);
        vec3 unmixedFogColor = max(vec3(0), vec3(0.416+(0.3*whiteness), 0.495+(0.2*whiteness), 0.75+(min(0, whiteness+4.5))))*1.2;
        float atmosphere = unmixedFogColor.b*1.334;
        vec3 blockLightBrightness = vec3(0);
        float sunLight = lighting.a*0.05f;
        float fogNoise = 1.f;
        if (hitPos == vec3(256)) {
            hitPos = camPos+(dir*distance(camPos, vec3(size/2, height/2, size/2)));
        }
        if (fragColor.a != 1) {
            fragColor = vec4(unmixedFogColor, 1);
            sunLight = 20*0.05f;
        } else {
            //selection start
            if (ui && selected == ivec3(rayMapPos)) {
                fragColor = vec4(mix(vec3(fragColor), vec3(0.7, 0.7, 1), 0.5f), 1);
            }
            //selection end
            fogNoise += max(0, noise(vec2(hitPos.x, hitPos.z)));
            blockLightBrightness = vec3(min(10, lighting.r), min(10, lighting.g), min(10, lighting.b))*0.1f;
        }
        fragColor = vec4(mix(vec3(fragColor), vec3(tint)*0.5f, min(0.9f, tint.a)), 1);//transparency
        float distanceFogginess = clamp(((distance(camPos, hitPos)*fogNoise)/renderDistance)*0.9375, 0, 1)*fogginess;
        fragColor = vec4(mix(mix(vec3(fragColor), unmixedFogColor, distanceFogginess), vec3(0.8), cloudiness*fogginess), 1);//distant fog, clouds
        vec3 finalLightFog = (mix(vec3(lightFog)/20, vec3(0.06, 0, 0.1)+min(vec3(0.33, 0.33, 0.3), sunColor), lightFog.a/11.67f)*atmosphere)*fogginess; //fog blending + sun distance fog
        float adjustedTime = min(1, abs(1-clamp((distance(rayMapPos, sun-vec3(0, height/2, 0))+distance(rayMapPos.y, sun.y-(height/2)))/(size/2.013), 0, 1))*2);
        fragColor = vec4((vec3(fragColor)*max(vec3(0.18)*fogginess, max(blockLightBrightness, (sunBrightness*sunBrightness)*sunLight)))+finalLightFog, 1); //brightness, blocklight fog, sun
    } else {
        fragColor = vec4(1.0, 1.0, 0.0, 1.0);
    }
}