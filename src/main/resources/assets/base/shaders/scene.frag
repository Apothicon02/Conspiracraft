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
vec3 lightPos = vec3(0);
vec4 lighting = vec4(0);
vec4 lightFog = vec4(0);
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

    vec3 prevMapPos = mapPos+(stepMask(sideDist+(mask*(-raySign)*deltaDist))*(-raySign));

    while (mapPos.x <= 7.0 && mapPos.x >= 0.0 && mapPos.y <= 7.0 && mapPos.y >= 0.0 && mapPos.z <= 7.0 && mapPos.z >= 0.0) {
        int colorData = atlasData[(9984*((blockType*8)+int(mapPos.x))) + (blockSubtype*64) + ((abs(int(mapPos.y)-8)-1)*8) + int(mapPos.z)];
        vec4 voxelColor = vec4(0xFF & colorData >> 16, 0xFF & colorData >> 8, 0xFF & colorData, 0xFF & colorData >> 24)/255;
        if (voxelColor.a >= 1) {
            if (hitPos == vec3(256)) {
                hitPos = rayMapPos;
            }
            //face-based brightness start
            //up should always be 1, down should always be 0.7, facing the sun should be 0.95, perpendicular to the sun should be 0.85, facing away from the sun should be 0.75
            float brightness = 1f;
            ivec3 normal = ivec3(mapPos - prevMapPos);
            if (normal == ivec3(0, 0, 0)) {
                brightness = 0f;
            }
            if (normal.y == 1) { //down
                brightness = 0.7f;
            } else if (normal.y == -1) { //up
                brightness = 1f;
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

            lightPos = (prevMapPos/8f)+rayMapPos;
            return vec4(vec3(voxelColor)*brightness, 1);
        } else {
            vec4 oldTint = tint;
            tint = vec4(max(voxelColor.r, tint.r), max(voxelColor.g, tint.g), max(voxelColor.b, tint.b), max(voxelColor.a, tint.a));
            if (oldTint != tint) {
                if (hitPos == vec3(256)) {
                    hitPos = rayMapPos;
                }
            }
        }

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
        bool inBounds = rayMapPos.y >= 0 && rayMapPos.y < size && rayMapPos.x >= 0 && rayMapPos.x < size && rayMapPos.z >= 0 && rayMapPos.z < size;
        //block start
        int blockPos = int(rayMapPos.x) + int(rayMapPos.y) * size + int(rayMapPos.z) * size * size;
        int blockInfo = region1BlockData[blockPos];
        int blockType = (blockInfo >> 16) & 0xFFFF;

        if (!inBounds) {
            blockInfo = 0;
            blockType = 0;
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

        float potentialCloudiness = 0f;
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
            lightPos = rayMapPos;
            if (blockType != 0f) {
                color = traceBlock(uv3d * 8.0, rayDir, mask, blockType, blockInfo & 0xFFFF);
            }

            //lighting start
            int lightingData = region1LightingData[int(lightPos.x) + int(lightPos.y) * size + int(lightPos.z) * size * size];
            lighting = vec4(0xFF & lightingData >> 16, 0xFF & lightingData >> 8, 0xFF & lightingData, 0xFF & lightingData >> 24);
            lightFog = vec4(max(lightFog.r, lighting.r/2), max(lightFog.g, lighting.g/2), max(lightFog.b, lighting.b/2), max(lightFog.a, lighting.a/2));
            //lighting end
        } else {
            lighting = vec4(0, 0, 0, 12);
            lightFog = vec4(max(lightFog.r, lighting.r/2), max(lightFog.g, lighting.g/2), max(lightFog.b, lighting.b/2), max(lightFog.a, lighting.a/2));
        }

        if (color.a >= 1) {
            return vec4(vec3(mix(color, tint, tint.a)), 1);
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
        vec3 dir = vec3(cam*vec4(normalize(vec3(uv, 1)), 0));
        fragColor = traceWorld(camPos, dir);
        float whiteness = gradient(rayMapPos.y, 0, 96, 0f, -0.5f)+gradient(rayMapPos.y, 96, 152, 0f, 0.3f)+gradient(rayMapPos.y, 152, 372, -2.1f, 0f)+gradient(rayMapPos.y, 372, 555, -0.2f, 0f);
        vec3 unmixedFogColor = max(vec3(0), vec3(0.416+(0.3*whiteness), 0.495+(0.2*whiteness), 0.75+(min(0, whiteness+4.5))));
        float atmosphere = unmixedFogColor.b*1.334;
        vec3 fogColor = mix(vec3(tint)/(abs(tint.a-1)), unmixedFogColor, abs(tint.a-1));
        vec3 blockLightBrightness = vec3(0);
        float sunLight = lighting.a*0.0834f;
        float fogNoise = 1f;
        if (hitPos == vec3(256)) {
            hitPos = camPos+(dir*256);
        }
        if (fragColor.a != 1) {
            fragColor = vec4(fogColor, 1);
            sunLight = 12*0.0834f;
        } else {
            fogNoise += max(0, noise(vec2(hitPos.x, hitPos.z)));
            blockLightBrightness = vec3(lighting.r, lighting.g, lighting.b)*0.045f;
        }
        float distanceFogginess = clamp(((((distance(camPos, hitPos)*fogNoise)/renderDistance)*0.75)+gradient(hitPos.y, 0, 16, 0, 1.25))*1.25, 0, 1);
        fragColor = vec4(mix(mix(vec3(fragColor), unmixedFogColor*1.2, distanceFogginess), vec3(0.8), cloudiness), 1); //distant fog, void fog, clouds
        float adjustedTime = clamp(timeOfDay*1.8, 0, 1);
        float sunBrightness = sunLight*adjustedTime;
        vec3 finalLightFog = mix(vec3(lightFog)/20, mix(vec3(0.06, 0, 0.1), vec3(0.33, 0.3, 0.25), adjustedTime), lightFog.a/7f)*atmosphere;
        fragColor = vec4((vec3(fragColor)*max(vec3(0.18), max(blockLightBrightness, vec3(sunBrightness))))+finalLightFog, 1); //brightness, blocklight fog
    } else {
        fragColor = vec4(0, 0, 0, 1);
    }
}