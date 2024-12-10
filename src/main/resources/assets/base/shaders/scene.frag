#version 460

uniform float timeOfDay;
uniform int renderDistance;
uniform vec2 res;
uniform mat4 cam;
layout(binding = 0) uniform sampler2D coherent_noise;
layout(binding = 1) uniform sampler2D white_noise;
layout(std430, binding = 0) buffer atlas
{
    int[] atlasData;
};
layout(std430, binding = 1) buffer region1
{
    int[] region1BlockData;
};
layout(std430, binding = 2) buffer region1Lighting
{
    int[] region1LightingData;
};
in vec4 gl_FragCoord;
in vec4 pos;

out vec4 fragColor;

int size = 1024;
int height = 320;
vec3 rayMapPos = vec3(0);
vec3 lightPos = vec3(0);
vec4 lighting = vec4(0);
vec4 lightFog = vec4(0);
vec3 hitPos = vec3(256);
vec4 tint = vec4(1, 1, 1, 0);
float cloudiness = 0;

vec4 intToColor(int color) {
    return vec4(0xFF & color >> 16, 0xFF & color >> 8, 0xFF & color, 0xFF & color >> 24);
}

vec4 getVoxel(int x, int y, int z, int blockType, int blockSubtype) {
    return intToColor(atlasData[(9984*((blockType*8)+x)) + (blockSubtype*64) + ((abs(y-8)-1)*8) + z])/255;
}
vec4 getVoxel(float x, float y, float z, int blockType, int blockSubtype) {
    return getVoxel(int(x), int(y), int(z), blockType, blockSubtype);
}

vec4 getLighting(int x, int y, int z) {
    return intToColor(region1LightingData[(((x*size)+z)*height)+y]);
}
vec4 getLighting(float x, float y, float z) {
    return getLighting(int(x), int(y), int(z));
}

int getBlockData(int x, int y, int z) {
    return region1BlockData[(((x*size)+z)*height)+y];
}
ivec2 getBlock(int x, int y, int z) {
    int blockData = getBlockData(x, y, z);
    return ivec2((blockData >> 16) & 0xFFFF, blockData & 0xFFFF);
}
ivec2 getBlock(float x, float y, float z) {
    return getBlock(int(x), int(y), int(z));
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
        vec4 voxelColor = getVoxel(mapPos.x, mapPos.y, mapPos.z, blockType, blockSubtype);
        if (voxelColor.a > 0.f && voxelColor.a < 1.f) {
            if (hitPos == vec3(256)) {
                hitPos = rayMapPos;
            }
            //bubbles start
            if (blockType == 1) {
                vec3 idk = rayMapPos+(mapPos);
                float samp = whiteNoise(((vec2(mapPos.x, mapPos.z)*128)+((rayMapPos.y*8)+mapPos.y+(timeOfDay*10000)))+(vec2(rayMapPos.x, rayMapPos.z)*8));
                if (samp > 0 && samp < 0.002) {
                    voxelColor = vec4(1, 1, 1, 1);
                }
            } else {
                vec3 idk = rayMapPos+(mapPos);
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
            //                int aboveBlocKInfo = region1BlockData[(((int(rayMapPos.x)*size)+int(rayMapPos.z))*height)+int(rayMapPos.y+1)];
            //                aboveBlockType = (aboveBlocKInfo >> 16) & 0xFFFF;
            //                aboveBlockSubtype = aboveBlocKInfo & 0xFFFF;
            //            }
            //            int aboveColorData = atlasData[(9984*((aboveBlockType*8)+int(mapPos.x))) + (aboveBlockSubtype*64) + ((abs(aboveY-8)-1)*8) + int(mapPos.z)];
            //            if ((0xFF & aboveColorData >> 24) <= 0 || aboveBlockType == 4 || aboveBlockType == 5) {
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
        float offset = noise(vec2(rayMapPos.x, rayMapPos.z)/2)*50;
        vec3 cloudPos = vec3(rayMapPos.x+(timeOfDay*2400), rayMapPos.y+offset-(timeOfDay*34), rayMapPos.z+(timeOfDay*4700));

        float potentialCloudiness = 0.f;
        if (cloudPos.y < 100+(offset*2)) {
            potentialCloudiness += (noise(vec2(cloudPos.x, cloudPos.z))+0.5f)*min(gradient(cloudPos.y, 50+(offset*2), 100+(offset*2), 0, 0.5), gradient(cloudPos.y, 0+(offset*2), 50+(offset*2), 0.5, 0));
        } else if (cloudPos.y > 192+offset && cloudPos.y < 300+offset) {
            potentialCloudiness += noise(vec2(cloudPos.x, cloudPos.z)/4)*min(gradient(cloudPos.y, 246+offset, 300+offset, 0, 0.5), gradient(cloudPos.y, 192+offset, 246+offset, 0.5, 0));
        }
        if (cloudPos.y > 246+offset && cloudPos.y < 400+offset) {
            potentialCloudiness += (noise(vec2(cloudPos.x, cloudPos.z)/2)+0.2f)*min(gradient(cloudPos.y, 323+offset, 400+offset, 0, 0.5), gradient(cloudPos.y, 246+offset, 323+offset, 0.5, 0));
        }
        cloudiness = max(cloudiness, potentialCloudiness);
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
            lightFog = vec4(max(lightFog.r, lighting.r/2), max(lightFog.g, lighting.g/2), max(lightFog.b, lighting.b/2), max(lightFog.a, lighting.a/2));
            //lighting end
        } else {
            lighting = vec4(0, 0, 0, 20);
            lightFog = vec4(max(lightFog.r, lighting.r/2), max(lightFog.g, lighting.g/2), max(lightFog.b, lighting.b/2), max(lightFog.a, lighting.a/2));
        }

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
    if (uv.x >= -0.004 && uv.x <= 0.004 && uv.y >= -0.004385 && uv.y <= 0.004385) {
        fragColor = vec4(0.9, 0.9, 1, 1);
    } else if (uv.x >= -1.87 && uv.x <= 1.87 && uv.y >= -1 && uv.y <= 1) {
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
            hitPos = camPos+(dir*256);
        }
        if (fragColor.a != 1) {
            fragColor = vec4(unmixedFogColor, 1);
            sunLight = 20*0.05f;
        } else {
            fogNoise += max(0, noise(vec2(hitPos.x, hitPos.z)));
            blockLightBrightness = vec3(lighting.r, lighting.g, lighting.b)*0.045f;
        }
        fragColor = vec4(mix(vec3(fragColor), vec3(tint)*0.5f, min(0.9f, tint.a)), 1); //transparency
        float distanceFogginess = clamp(((((distance(camPos, hitPos)*fogNoise)/renderDistance)*0.75)+gradient(hitPos.y, 0, 16, 0, 1.25))*1.25, 0, 1);
        fragColor = vec4(mix(mix(vec3(fragColor), unmixedFogColor, distanceFogginess), vec3(0.8), cloudiness), 1); //distant fog, void fog, clouds
        float adjustedTime = clamp(timeOfDay*2.8, 0, 1);
        float sunBrightness = sunLight*adjustedTime;
        vec3 finalLightFog = mix(vec3(lightFog)/20, mix(vec3(0.06, 0, 0.1), vec3(0.33, 0.3, 0.25), adjustedTime), lightFog.a/11.67f)*atmosphere;
        fragColor = vec4((vec3(fragColor)*max(vec3(0.18), max(blockLightBrightness, vec3(sunBrightness))))+finalLightFog, 1); //brightness, blocklight fog
    } else {
        fragColor = vec4(0, 0, 0, 1);
    }
}