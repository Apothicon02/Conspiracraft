#version 460

uniform float timeOfDay;
uniform int renderDistance;
uniform vec2 res;
uniform mat4 cam;
layout(binding = 0) uniform sampler2D coherent_noise;
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
in vec2 gl_FragCoord;
in vec4 pos;

out vec4 fragColor;

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
    return (texture(coherent_noise, coords/2048).r)-0.5;
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

int size = 512;
vec3 rayMapPos = vec3(0);
vec4 prevLighting = vec4(0);
vec4 lighting = vec4(0);
vec3 lightFog = vec3(0);
vec4 tint = vec4(0);
vec3 hitPos = vec3(256);
float cloudiness = 0;

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
            if (hitPos == vec3(256)) {
                hitPos = rayMapPos;
            }
            return voxelColor;
        } else {
            vec4 oldTint = tint;
            tint = vec4(max(voxelColor.r, tint.r), max(voxelColor.g, tint.g), max(voxelColor.b, tint.b), max(voxelColor.a, tint.a));
            if (oldTint != tint) {
                if (hitPos == vec3(256)) {
                    hitPos = rayMapPos;
                }
            }
        }

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

    for (int i = 0; i < renderDistance; i++) {
        if (rayMapPos.y < -60 || rayMapPos.y > 512) {
            break;
        }
        //block start
        int blockPos = int(rayMapPos.x) + int(rayMapPos.y) * size + int(rayMapPos.z) * size * size;
        int blockInfo = region1BlockData[blockPos];
        int blockType = (blockInfo >> 16) & 0xFFFF;
        //block end

        //lighting start
        int lightingData = region1LightingData[blockPos];
        prevLighting = lighting;
        lighting = vec4(0xFF & lightingData >> 16, 0xFF & lightingData >> 8, 0xFF & lightingData, 0xFF & lightingData >> 24);
        lightFog = vec3(max(lightFog.r, lighting.r/2), max(lightFog.g, lighting.g/2), max(lightFog.b, lighting.b/2));
        //lighting end

        //cloud start
        float offset = noise(vec2(rayMapPos.x, rayMapPos.z)/2)*50;
        float offsetY = rayMapPos.y+offset;

        if (offsetY < 100+offset) {
            cloudiness = max(cloudiness, (noise(vec2(rayMapPos.x, rayMapPos.z))+0.5f)*min(gradient(offsetY, 30+offset, 100+offset, 0, 0.5), gradient(offsetY, 0+offset, 30+offset, 0.5, 0)));
        } else if (offsetY > 256+offset && offsetY < 400+offset) {
            cloudiness = max(cloudiness, noise(vec2(rayMapPos.x, rayMapPos.z)/4)*min(gradient(offsetY, 378+offset, 400+offset, 0, 0.5), gradient(offsetY, 256+offset, 378+offset, 0.5, 0)));
        } else if (offsetY > 400+offset && offsetY < 512+offset) {
            cloudiness = max(cloudiness, (noise(vec2(rayMapPos.x, rayMapPos.z)/2)+0.2f)*min(gradient(offsetY, 456+offset, 512+offset, 0, 0.5), gradient(offsetY, 400+offset, 456+offset, 0.5, 0)));
        }
        if (cloudiness >= 1) {
            break;
        }
        //cloud end

        //voxel start
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
        //voxel end

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
        float whiteness = clamp((abs(rayMapPos.y-size)/size)-0.33f, 0f, 0.33f);
        vec3 unmixedFogColor = vec3(0.63+(0.37*whiteness), 0.75+(0.25*whiteness), 1);
        vec3 fogColor = mix(vec3(tint)/(abs(tint.a-1)), unmixedFogColor, abs(tint.a-1));
        float blockLightBrightness = 0;
        if (fragColor.a != 1) {
            fragColor = vec4(fogColor, 1);
        } else {
            max(max(prevLighting.r, lighting.r), max(max(prevLighting.g, lighting.g), max(prevLighting.b, lighting.b)))*0.02f;
        }
        float distanceFogginess = clamp(((distance(camPos, hitPos)/renderDistance)*1.25)+gradient(hitPos.y, 0, 16, 0, 1.25), 0, 1);
        fragColor = vec4(mix(mix(vec3(fragColor), unmixedFogColor, min(distanceFogginess*1.25, 1)), vec3(1), cloudiness), 1); //distant fog, void fog, clouds
        float sunBrightness = max(0.1, (7*0.142f)*timeOfDay); //(max(prevLighting.a, lighting.a)*0.142f)*timeOfDay
        vec3 blockLightFog = lightFog*0.02f;
        fragColor = vec4((vec3(fragColor)*max(blockLightBrightness, sunBrightness))+blockLightFog, 1); //brightness, blocklight fog
    } else {
        fragColor = vec4(0, 0, 0, 1);
    }
}