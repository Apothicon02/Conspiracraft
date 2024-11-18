#version 460

uniform int renderDistance;
uniform vec2 res;
uniform mat4 cam;
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

const mat2 m = mat2( 1.6,  1.2, -1.2,  1.6 );

vec2 hash( vec2 p ) {
    p = vec2(dot(p,vec2(127.1,311.7)), dot(p,vec2(269.5,183.3)));
    return -1.0 + 2.0*fract(sin(p)*43758.5453123);
}

float noise( in vec2 p ) {
    const float K1 = 0.366025404; // (sqrt(3)-1)/2;
    const float K2 = 0.211324865; // (3-sqrt(3))/6;
    vec2 i = floor(p + (p.x+p.y)*K1);
    vec2 a = p - i + (i.x+i.y)*K2;
    vec2 o = (a.x>a.y) ? vec2(1.0,0.0) : vec2(0.0,1.0); //vec2 of = 0.5 + 0.5*vec2(sign(a.x-a.y), sign(a.y-a.x));
    vec2 b = a - o + K2;
    vec2 c = a - 1.0 + 2.0*K2;
    vec3 h = max(0.5-vec3(dot(a,a), dot(b,b), dot(c,c) ), 0.0 );
    vec3 n = h*h*h*h*vec3( dot(a,hash(i+0.0)), dot(b,hash(i+o)), dot(c,hash(i+1.0)));
    return dot(n, vec3(70.0));
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
            hitPos = rayMapPos;
            return voxelColor;
        } else {
            vec4 oldTint = tint;
            tint = vec4(max(voxelColor.r, tint.r), max(voxelColor.g, tint.g), max(voxelColor.b, tint.b), max(voxelColor.a, tint.a));
            if (oldTint != tint) {
                hitPos = rayMapPos;
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
        int blockPos = int(rayMapPos.x) + int(rayMapPos.y) * size + int(rayMapPos.z) * size * size;
        int blockInfo = region1BlockData[blockPos];
        int blockType = (blockInfo >> 16) & 0xFFFF;
        int lightingData = region1LightingData[blockPos];
        prevLighting = lighting;
        lighting = vec4(0xFF & lightingData >> 16, 0xFF & lightingData >> 8, 0xFF & lightingData, 0xFF & lightingData >> 24);
        lightFog = vec3(max(lightFog.r, lighting.r), max(lightFog.g, lighting.g), max(lightFog.b, lighting.b));

        if (cloudiness < 1 && rayMapPos.y > -60 && rayMapPos.y < 572) {
            float offset = noise(vec2(rayMapPos.x, rayMapPos.z)/200)*50;
            float offsetY = rayMapPos.y+offset;

            if (offsetY < 100+offset) {
                cloudiness = max(cloudiness, (noise(vec2(rayMapPos.x, rayMapPos.z)/100)+0.5f)*min(gradient(offsetY, 30+offset, 100+offset, 0, 0.5), gradient(offsetY, 0+offset, 30+offset, 0.5, 0)));
            } else if (offsetY > 256+offset && offsetY < 400+offset) {
                cloudiness = max(cloudiness, noise(vec2(rayMapPos.x, rayMapPos.z)/400)*min(gradient(offsetY, 378+offset, 400+offset, 0, 0.5), gradient(offsetY, 256+offset, 378+offset, 0.5, 0)));
            } else if (offsetY > 400+offset && offsetY < 512+offset) {
                cloudiness = max(cloudiness, (noise(vec2(rayMapPos.x, rayMapPos.z)/200)+0.2f)*min(gradient(offsetY, 456+offset, 512+offset, 0, 0.5), gradient(offsetY, 400+offset, 456+offset, 0.5, 0)));
            }
        }

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
        float whiteness = clamp((abs(rayMapPos.y-size)/size)-0.7f, 0f, 0.5f);
        vec3 unmixedFogColor = vec3(0.63+(0.37*whiteness), 0.75+(0.25*whiteness), 1);
        vec3 fogColor = mix(vec3(tint), unmixedFogColor, abs(tint.a-1));
        if (fragColor.a != 1) {
            fragColor = vec4(fogColor, 1);
        } else {
            fragColor = vec4(mix(vec3(fragColor), fogColor*1.2, distance(camPos, hitPos)/renderDistance), 1);
        }
        float voidFogginess = abs(min(hitPos.y, size/50)-10)/10;
        fragColor = vec4(mix(vec3(fragColor), unmixedFogColor, voidFogginess), 1); //voidFog
        fragColor = vec4(mix(vec3(fragColor), vec3(1), cloudiness), 1); //clouds
        vec4 maxLighting = vec4(max(prevLighting.r, lighting.r), max(prevLighting.g, lighting.g), max(prevLighting.b, lighting.b), max(prevLighting.a, lighting.a));
        float blockLightBrightness = max(maxLighting.r, max(maxLighting.g, maxLighting.b))*0.02f;
        float sunBrightness = max(0.1, 7*0.142f); //maxLighting.a 0-7
        vec3 blockLightFog = vec3(lightFog)*0.02f;
        fragColor = vec4(mix((vec3(fragColor.r, fragColor.g, fragColor.b)*max(blockLightBrightness, sunBrightness)), blockLightFog, (blockLightBrightness*(abs(sunBrightness-1)))), 1);
    } else {
        fragColor = vec4(0, 0, 0, 1);
    }
}