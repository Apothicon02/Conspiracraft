layout(set = 0, binding = 0) readonly uniform GlobalUBO {
    mat4 view;
    mat4 proj;
    vec4 skylight;
    int hdr;
} globalUbo;
layout(std430, set = 0, binding = 1) readonly buffer VoxelBuffer {
    int[] voxels;
} voxelData;
layout(location = 0) in vec2 uv;

layout(location = 0) out vec4 outColor;

const int size = 1024;
const int height = 320;
const vec3 worldSize = vec3(size, height, size);

int packPos(vec3 pos) {
    return int(pos.x)+int(pos.y)*size+int(pos.z)*(size*height);
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

vec3 ogPos = vec3(0);
vec3 ogDir = vec3(0);
vec3 sunColor = vec3(0);
vec4 getLightingColor(vec3 lightPos, vec4 lighting, bool isSky, float fogginess, bool negateSun) {
    float ogY = ogPos.y;
    float sunHeight = globalUbo.skylight.y/size;
    float scattering = gradient(lightPos.y, ogY-63, ogY+437, 1.5f, -0.5f);
    float sunDist = (distance(lightPos.xz, globalUbo.skylight.xz)/(size*1.5f));
    float adjustedTime = clamp((sunDist*abs(1-clamp(sunHeight, 0.05f, 0.5f)))+scattering, 0.f, 1.f);
    float thickness = gradient(lightPos.y, 128, 1500-max(0, sunHeight*1000), 0.33+(sunHeight/2), 1);
    float sunSetness = min(1.f, max(abs(sunHeight*1.5f), adjustedTime));
    float whiteY = max(ogY, 200)-135.f;
    float skyWhiteness = mix(max(0.33f, gradient(lightPos.y, (whiteY/4)+47, (whiteY/2)+436, 0, 0.9)), 0.9f, clamp(abs(1-sunSetness), 0, 1.f));
    float sunBrightness = clamp(sunHeight+0.5, mix(0.f, 0.33f, skyWhiteness), 1.f);
    lighting.rgb = max(vec3(0), lighting.rgb-(sunBrightness*lighting.a));
    if (negateSun) {
        lighting.a = 0;
    }
    float whiteness = isSky ? skyWhiteness : mix(0.9f, skyWhiteness, max(0, fogginess-0.8f)*5.f);
    sunColor = mix(mix(vec3(1, 0.65f, 0.25f)*(1+((10*clamp(sunHeight, 0.f, 0.1f))*(15*min(0.5f, abs(1-sunBrightness))))), vec3(0.36f, 0.54f, 1.2f)*sunBrightness, sunSetness), vec3(sunBrightness), whiteness);
    return vec4(max(lighting.rgb, min(mix(vec3(1), vec3(1, 0.95f, 0.85f), sunSetness/4), lighting.a*sunColor)).rgb, thickness);
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

vec3 roundDir(vec3 dir) {
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
    return roundDir(normalize((inverse(globalUbo.view)*clipSpace).xyz));
}

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
            normal = -mask*raySign;
            if (voxel == 1) {
                color = vec4(0.3, 0.35, 1.f, 1.f);
            } else if (voxel == 2) {
                color = vec4(0.95, 0.93, 0.85f, 1.f);
            } else {
                color = vec4(0.5, 0.95, 0.5f, 1.f);
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
        vec3 lighting = vec3(vec3(dot(normal, normalize(skylight.xyz))*0.38f)+(0.68f*skylight.a));
        color.rgb *= lighting;
    }
    float fogginess = isSky ? 1.f : clamp(sqrt(distance(camPos, mapPos)/(size*0.66f)), 0.f, 1.f);
    color.rgb = mix(color.rgb, getLightingColor(lightPos, vec4(0, 0, 0, 0.9f), isSky, fogginess, false).rgb, fogginess);

    color.rgb = pow(color.rgb, vec3(2.2)); //gamma
    if (globalUbo.hdr == 1) {
        color.rgb = (color.rgb*400)/80;//exposure
    }
    outColor = vec4(color.rgb, 1);
}