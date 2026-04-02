layout(location = 0) out vec4 outColor;

const int size = 1024;
const int height = 320;
const vec3 worldSize = vec3(size, height, size);
const vec3 sun = vec3(2553, 166, 512);

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
vec3 sunColor = vec3(0);
vec4 getLightingColor(vec3 lightPos, vec4 lighting, bool isSky, float fogginess, bool negateSun) {
    float ogY = ogPos.y;
    float sunHeight = sun.y/size;
    float scattering = gradient(lightPos.y, ogY-63, ogY+437, 1.5f, -0.5f);
    float sunDist = (distance(lightPos.xz, sun.xz)/(size*1.5f));
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

void main() {
    outColor = vec4(0);

    vec2 uv = ((gl_FragCoord.xy/vec2(1920, 1080))*2)-1;
    vec3 camPos = vec3(size/2, height/2, 0);
    vec3 rayDir = normalize(vec3(uv, -1.0))*-1;
    vec3 rayPos = camPos;
    ogPos = rayPos;
    vec3 mapPos = floor(rayPos);
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((mapPos - rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = stepMask(sideDist);

    while (mapPos.x >= 0 && mapPos.x < size && mapPos.y >= 0 && mapPos.y < height && mapPos.z >= 0 && mapPos.z < size) {
        float hillHeight = mix(2, 100, clamp(((mapPos.x+mapPos.z)/size)-0.3f, 0, 1));
        float t = mapPos.x / hillHeight;
        float hill = sqrt(sin(t * 3.14f * 2.0)+1)*20;
        t = mapPos.z / hillHeight;
        hill += sqrt(sin(t * 3.14f * 2.0)+1)*20;
        if (mapPos.y < 80+hill) {
            vec3 normal = -mask*raySign;
            if (mapPos.y < 128) {
                normal = vec3(0, 1, 0);
                outColor = vec4(0.3, 0.35, 1.f, 1.f);
            } else {
                outColor = vec4(0.55, 1, 0.5f, 1.f);
            }
            outColor *= vec4(vec3(dot(-normal, sun)*-0.0001f)+0.6f, 1);
            break;
        }
        mask = stepMask(sideDist);
        mapPos += mask * raySign;
        sideDist += mask * raySign * deltaDist;
    }
    bool isSky = outColor.a < 1;
    float fogginess = isSky ? 1.f : clamp(sqrt(distance(camPos, mapPos)/(size*0.66f)), 0.f, 1.f);
    outColor = mix(outColor, getLightingColor(mapPos, vec4(0, 0, 0, 0.9f), isSky, fogginess, false), fogginess);
}