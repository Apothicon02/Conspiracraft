int size = 4208;
int height = 432;
int chunkSize = 16;
int sizeChunks = size>> 4;
int heightChunks = height>> 4;

uniform mat4 cam;
uniform int renderDistance;
uniform float timeOfDay;
uniform double time;
uniform ivec3 selected;
uniform bool ui;
uniform bool shadowsEnabled;
uniform bool reflectionShadows;
uniform vec3 sun;
uniform ivec2 res;

vec3 uvDir = vec3(0);
vec3 camPos = vec3(cam[3]);

layout(std430, binding = 0) buffer atlasSSBO
{
    int[] atlasData;
};
layout(std430, binding = 5) buffer chunkLightsSSBO
{
    ivec4[] chunkLightsData;
};
layout(std430, binding = 6) buffer lightsSSBO
{
    int[] lightsData;
};
layout(std430, binding = 8) buffer playerSSBO
{
    int[] playerData;
};
layout(binding = 4) uniform sampler2D white_noise;
layout(binding = 6, rgba32f) uniform image3D scene_unscaled_image;

in vec4 gl_FragCoord;

out vec4 fragColor;

float whiteNoise(vec2 coords) {
    return (texture(white_noise, coords/1024).r)-0.5f;
}

int getLightData(int x, int y, int z) {
    ivec3 chunkPos = ivec3(x, y, z) >> 4;
    ivec3 localPos = ivec3(x, y, z) & ivec3(15);
    int condensedLocalPos = ((((localPos.x*chunkSize)+localPos.z)*chunkSize)+localPos.y);
    ivec3 prevLightChunkPos = chunkPos;
    int condensedChunkPos = (((chunkPos.x*sizeChunks)+chunkPos.z)*heightChunks)+chunkPos.y;
    ivec4 lightPaletteInfo = ivec4(chunkLightsData[condensedChunkPos]);
    int lightValuesPerInt = 32/lightPaletteInfo.z;

    int intIndex  = condensedLocalPos/lightValuesPerInt;
    int bitIndex = (condensedLocalPos - intIndex * lightValuesPerInt) * lightPaletteInfo.z;
    int key = (lightsData[lightPaletteInfo.x+lightPaletteInfo.y+intIndex] >> bitIndex) & lightPaletteInfo.w;
    return lightsData[lightPaletteInfo.x+key];
}

vec4 getLighting(float x, float y, float z) {
    return fromLinear(intToColor(getLightData(int(x), int(y), int(z))));
}

vec3 stepMask(vec3 sideDist) {
    bvec3 mask;
    bvec3 b1 = lessThan(sideDist.xyz, sideDist.yzx);
    bvec3 b2 = lessThanEqual(sideDist.xyz, sideDist.zxy);
    mask.z = b1.z && b2.z;
    mask.x = b1.x && b2.x;
    mask.y = b1.y && b2.y;
    if(!any(mask)) {
        mask.z = true;
    }

    return vec3(mask);
}

vec4 getVoxel(float x, float y, float z, float bX, float bY, float bZ) {
    //bool rightHand = bX >= 1 && bX < 2 && bY >= -1.5f && bY < -0.5f && bZ >= 0.5f && bZ < 1.5f;
    bool stack = bX >= -10 && bX < (-10+30) && bY >= -15.5f && bY < -14.5 && bZ >= 15.5f && bZ < 16.5f;
    bool leftHand = bX >= -2 && bX < -1 && bY >= -1.5f && bY < -0.5f && bZ >= 0.5f && bZ < 1.5f;
    if (leftHand || (stack && ((int(bX+10) % 2) == 0))) {
        ivec2 block = ivec2(0);
        if (leftHand) {
            block = ivec2(playerData[0], playerData[1]);
        } else {
            int slot = int(bX+12);
            block = ivec2(playerData[slot], playerData[slot+1]);
        }
        y += 4; //when the bounds contain a decimal, that decimal should be multiplied by 8 and added to its axis
        z += 4;
        int localX = int(x-(floor(x/8)*8));
        int localY = int(y-(floor(y/8)*8));
        int localZ = int(z-(floor(z/8)*8));
        return fromLinear(intToColor(atlasData[(1024*((block.x*8)+localX)) + (block.y*64) + ((abs(localY-8)-1)*8) + localZ])/255);
    }
    return vec4(0);
}

vec4 maxVoxelColor = vec4(0);
vec3 prevTintColor = vec3(0);
vec3 tint = vec3(0);

vec4 raytrace(vec3 rayPos, vec3 rayDir) {
    vec3 mapPos = vec3(0);
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = (mapPos + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = stepMask(sideDist);
    vec3 prevMapPos = mapPos+(stepMask(sideDist+(mask*(-raySign)*deltaDist))*(-raySign));

    for (int i = 0; i < 512; i++) {
        vec4 voxelColor = getVoxel(mapPos.x, mapPos.y, mapPos.z, (mapPos.x/8)+rayPos.x, (mapPos.y/8)+rayPos.y, (mapPos.z/8)+rayPos.z);
        if (voxelColor.a > 0.f) {
            if (voxelColor.a < fromLinear(vec4(1)).a) {
                float samp = whiteNoise(((vec2(mapPos.x, mapPos.z)*128)+(16)+mapPos.y)+(vec2(16)));
                if (samp > -0.004 && samp < -0.002 || samp > 0 && samp < 0.002) {
                    voxelColor = vec4(fromLinear(vec3(1)), 1);
                }
            }
            vec3 normal = ivec3(mapPos - prevMapPos);
            if (normal.y >0) { //down
                voxelColor.rgb *= 0.7f;
            } else if (normal.y <0) { //up
                voxelColor.rgb *= 1.f;
            } else if (normal.z >0) { //south
                voxelColor.rgb *= 0.85f;
            } else if (normal.z <0) { //north
                voxelColor.rgb *= 0.85f;
            } else if (normal.x >0) { //west
                voxelColor.rgb *= 0.75f;
            } else if (normal.x <0) { //east
                voxelColor.rgb *= 0.95f;
            }
            vec4 lighting = getLighting(camPos.x, camPos.y, camPos.z)*0.95f;
            vec4 lightFog = lighting/fromLinear(vec4(10));
            lighting/=fromLinear(vec4(20));
            float adjustedTimeCam = clamp((abs(1-clamp((distance(camPos, sun-vec3(0, sun.y, 0))/1.5)/size, 0, 1))*1.2)-abs(0.25f-min(0.25f, distance(camPos, vec3(0))/size)), 0.05f, 0.9f);
            float timeBonus = gradient(camPos.y, 64.f, 372.f, 0.1f, 0.f);
            float mixedTime =  max(0.2f, adjustedTimeCam+timeBonus);
            float sunLight = lighting.a*max(0.4f, mixedTime-timeBonus);
            float sunLightNoBonus = lighting.a*mixedTime;
            vec3 brightness = max(lighting.rgb*0.66, vec3(sunLight));
            vec3 desaturation = clamp(-1.5f*(1-max(lighting.rgb, sunLightNoBonus)), vec3(0.f), vec3(0.8f));
            voxelColor.rgb*=brightness;
            voxelColor.rgb+=lighting.rgb*0.34f;
            voxelColor.rgb = hsv2rgb(max(vec3(0), rgb2hsv(voxelColor.rgb)-vec3(0, max(desaturation.r, max(desaturation.g, desaturation.b)), 0)));
            float sunLightFog = (lightFog.a)*mixedTime;
            vec3 sunColor = mix(mix(vec3(0.0f, 0.0f, 4.5f), vec3(2.125f, -0.4f, 0.125f), mixedTime*4), vec3(0.1f, 0.95f, 1.5f), mixedTime) * 0.15f;
            vec3 finalLightFog = sunColor*(sunLightFog);
            vec3 fog = 1 + (vec3(finalLightFog) * 1.5f);
            voxelColor.rgb*=fog;
            voxelColor = toLinear(voxelColor);
            if (voxelColor.a >= 1) {
                vec3 finalTint = (vec3(1) + tint);
                vec4 superFinalTint = fromLinear(vec4(vec3(finalTint) / (max(finalTint.r, max(finalTint.g, finalTint.b))), 1));
                voxelColor *= superFinalTint;
                return voxelColor;
            } else {
                vec4 tintColor = voxelColor * voxelColor.a;
                if (prevTintColor != tintColor.rgb) {
                    vec3 finalTintColor = vec3(toLinear(tintColor*2));
                    tint += finalTintColor;
                }
                prevTintColor = tintColor.rgb;
                if (voxelColor.a > maxVoxelColor.a) {
                    maxVoxelColor = voxelColor;
                }
            }
        }

        mask = stepMask(sideDist);
        prevMapPos = mapPos;
        mapPos += mask * raySign;
        sideDist += mask * raySign * deltaDist;
    }

    return maxVoxelColor;
}

void main() {
    ivec2 pos = ivec2(gl_FragCoord.xy);
    vec2 uv = (pos*2. - res.xy) / res.y;
    uvDir = normalize(vec3(uv, 1));

    if (ui) {
        fragColor = raytrace(vec3(0, 0, 0), uvDir);
    }
    if (fragColor.a < 1) {
        bool checkerOn = checker(pos);
        bool firstHalf = bool(pos.x < res.x/2);
        if (!((firstHalf && !checkerOn) || (!firstHalf && checkerOn))) {
            pos.y++;
        }
        vec4 worldColor = toLinear(vec4(imageLoad(scene_unscaled_image, ivec3(pos.xy, 0))));
        fragColor = mix(worldColor, fragColor, fragColor.a);
    }
}