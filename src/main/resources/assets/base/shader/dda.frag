layout(set = 0, binding = 0) readonly uniform GlobalUBO {
    mat4 view;
    mat4 proj;
    vec4 skylight;
    vec3 sun;
    int hdr;
} globalUbo;

layout(std430, set = 0, binding = 1) readonly buffer VoxelBuffer {
    int[] voxels;
} voxelData;
const int size = 1024;
const int height = 320;
const vec3 worldSize = vec3(size, height, size);
int packPos(vec3 pos) {
    return int(pos.x)+int(pos.y)*size+int(pos.z)*(size*height);
}

layout(set = 0, binding = 2) uniform sampler3D atlas;
const int blockSize = 8;
const int blockTexSize = blockSize;
vec4 sampleAtlas(int x, int y, int z, int bX, int bY, int bZ, int blockType, int blockSubtype) {
//    if ((bX & 1) != 0) { x += blockSize; }
//    if ((bY & 1) != 0) { y += blockSize; }
//    if ((bZ & 1) != 0) { z += blockSize; }
    return texelFetch(atlas, ivec3(x+(blockType*8), ((abs(y-blockTexSize)-1)*blockTexSize)+z, blockSubtype), 0);
}

layout(location = 0) in vec2 uv;

layout(location = 0) out vec4 outColor;

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

vec3 ogPos = vec3(0);
vec3 ogDir = vec3(0);
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
    float sunHeight = globalUbo.sun.y/size;
    float scattering = gradient(lightPos.y, ogY-63, ogY+437, 1.5f, -0.5f);
    float sunDist = (distance(lightPos.xz, globalUbo.sun.xz)/(size*1.5f));
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

vec3 stepMask(vec3 sideDist) {
    bvec3 b1 = lessThan(sideDist.xyz, sideDist.yzx);
    bvec3 b2 = lessThanEqual(sideDist.xyz, sideDist.zxy);
    bvec3 mask = bvec3(
    b1.x && b2.x,
    b1.y && b2.y,
    b1.z && b2.z
    );
    if(!any(mask)) {
        mask.z = true;
    }

    return vec3(mask);
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
vec3 getDir(vec2 pos) {
    vec2 modifiedUV = (uv * 2.0) - 1.0;
    vec4 clipSpace = vec4((inverse(globalUbo.proj) * vec4(modifiedUV, 1.f, 1.f)).xyz, 0);
    return roundVec(normalize((inverse(globalUbo.view)*clipSpace).xyz));
}

const float bevel = 0.125f;
const float bevelMax = 1-bevel;
const float bevelOffset = 0.5f+(bevel/2);

void main() {
    vec4 color = vec4(0);
    vec3 camPos = inverse(globalUbo.view)[3].xyz;
    vec3 rayDir = getDir(uv);
    vec3 rayPos = camPos;
    ogPos = rayPos;
    ogDir = rayDir;
    vec3 mapPos = floor(rayPos);
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((mapPos - rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = stepMask(sideDist);
    vec3 normal = vec3(0);
    while (mapPos.x >= 0 && mapPos.x < size && mapPos.y >= 0 && mapPos.y < height && mapPos.z >= 0 && mapPos.z < size) {
        int voxel = voxelData.voxels[packPos(mapPos)];
        if (voxel > 0) {
            normal = -mask*raySign; //flat normal
            vec3 mini = ((mapPos-ogPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
            float dist = max(mini.x, max(mini.y, mini.z));
            vec3 exactPos = ogPos + ogDir * dist;
            vec3 voxelPos = clamp(fract(exactPos-0.01f)*8, 0.01f, 7.99f);

            if (mapPos.x > 0 && mapPos.x < size-1 && mapPos.z > 0 && mapPos.z < size-1 && mapPos.y > 0 && mapPos.y < height-1) { //dont blend with voxels outside of world.
                vec3 localPos = roundVec(fract(exactPos));
                vec3 bevelPos = mapPos+0.5f;
                vec3 absFlatNorm = abs(normal);
                if (absFlatNorm.x < max(absFlatNorm.y, absFlatNorm.z)) {
                    if (localPos.x > bevelMax) {
                        if (voxelData.voxels[packPos(bevelPos+vec3(bevelOffset, 0, 0))] == 0) { normal.x = 1; }
                    } else if (localPos.x < bevel) {
                        if (voxelData.voxels[packPos(bevelPos-vec3(bevelOffset, 0, 0))] == 0) { normal.x = -1; }
                    }
                }
                if (absFlatNorm.z < max(absFlatNorm.x, absFlatNorm.y)) {
                    if (localPos.z > bevelMax) {
                        if (voxelData.voxels[packPos(bevelPos+vec3(0, 0, bevelOffset))] == 0) { normal.z = 1; }
                    } else if (localPos.z < bevel) {
                        if (voxelData.voxels[packPos(bevelPos-vec3(0, 0, bevelOffset))] == 0) { normal.z = -1; }
                    }
                }
                if (absFlatNorm.y < max(absFlatNorm.x, absFlatNorm.z)) {
                    if (localPos.y > bevelMax) {
                        if (voxelData.voxels[packPos(bevelPos+vec3(0, bevelOffset, 0))] == 0) { normal.y = 1; }
                    } else if (localPos.y < bevel) {
                        if (voxelData.voxels[packPos(bevelPos-vec3(0, bevelOffset, 0))] == 0) { normal.y = -1; }
                    }
                }
            }

            color = vec4(sampleAtlas(int(voxelPos.x), int(voxelPos.y), int(voxelPos.z), int(mapPos.x), int(mapPos.y), int(mapPos.z), voxel, 0).rgb, 1);
            if (voxel == 1) {
                color = vec4(0.3, 0.35, 1.f, 1.f);
            }
            break;
        }
        mask = stepMask(sideDist);
        mapPos += mask * raySign;
        sideDist += mask * raySign * deltaDist;
    }
    bool isSky = color.a < 1;
    vec3 lightPos = mapPos;
    if (isSky) {
        lightPos = ogPos + ogDir * size;
    } else {
        vec4 skylight = globalUbo.skylight;
        vec3 lighting = (vec3(dot(normal, normalize(skylight.xyz))*0.38f/min(1, skylight.a*2))+(0.03f+(0.59f*skylight.a)))*(0.05f+(skylight.a*0.95f));
        color.rgb *= lighting;
    }
    float fogginess = isSky ? 1.f : clamp(sqrt(distance(camPos, mapPos)/(size*0.66f))-0.15f, 0.f, 1.f);
    color.rgb = mix(color.rgb, getLightingColor(lightPos, vec4(0, 0, 0, 0.9f), isSky, fogginess, false).rgb, fogginess);

    color.rgb = pow(color.rgb, vec3(2.2)); //gamma
    if (globalUbo.hdr == 1) {
        color.rgb = (color.rgb*400)/80;//exposure
    }
    outColor = vec4(color.rgb, 1);
}