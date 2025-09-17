layout(std430, binding = 0) buffer voxelsSSBO
{
    float[] voxelsData;
};

uniform mat4 cam;
uniform ivec3 selected;
uniform bool ui;
uniform ivec2 res;

in vec4 gl_FragCoord;

out vec4 fragColor;

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

bool checker(ivec2 pixel) {
    bool xOdd = bool(pixel.x % 2 == 1);
    bool yOdd = bool(pixel.y % 2 == 1);
    if ((xOdd && yOdd) || (!xOdd && !yOdd)) { //both even or both odd
        return true;
    }
    return false;
}

int condensePos(int x, int y, int z) {
    return ((((x*8)+y)*8)+z)*4;
}
int condensePos(float x, float y, float z) {
    return condensePos(int(x), int(y), int(z));
}

vec4 getVoxel(float x, float y, float z) {
    if (x >= 0 && x < 8 && z >= 0 && z < 8) {
        if (y >= 0 && y < 8) {
            int pos = condensePos(x, y, z);
            return vec4(voxelsData[pos], voxelsData[pos+1], voxelsData[pos+2], voxelsData[pos+3]);
        } else if (ui && y == -1) {
            if (checker(ivec2(x, z))) {
                return vec4(0.2f, 0.6, 0.2f, 1.f);
            }
            return vec4(0.2f, 0.8f, 0.2f, 1.f);
        }
    }
    return vec4(0.f);
}

bool hitSelection = false;

vec4 raytrace(vec3 rayPos, vec3 rayDir) {
    vec3 mapPos = floor(rayPos);
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((mapPos - rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = stepMask(sideDist);
    vec3 prevMapPos = mapPos+(stepMask(sideDist+(mask*(-raySign)*deltaDist))*(-raySign));

    for (int i = 0; i < 128; i++) {
        vec4 voxelColor = getVoxel(mapPos.x, mapPos.y, mapPos.z);
        if (voxelColor.a > 0.f) {
            if (selected == mapPos) {
                hitSelection = true;
            }
            vec3 normal = ivec3(mapPos - prevMapPos);
            if (normal.y >0) { //down
                voxelColor *= 0.7f;
            } else if (normal.y <0) { //up
                voxelColor *= 1.f;
            } else if (normal.z >0) { //south
                voxelColor *= 0.85f;
            } else if (normal.z <0) { //north
                voxelColor *= 0.85f;
            } else if (normal.x >0) { //west
                voxelColor *= 0.75f;
            } else if (normal.x <0) { //east
                voxelColor *= 0.95f;
            }
            return voxelColor;
        }

        mask = stepMask(sideDist);
        prevMapPos = mapPos;
        mapPos += mask * raySign;
        sideDist += mask * raySign * deltaDist;
    }

    return vec4(0.5f, 0.75f, 1.f, 1.f);
}

void main() {
    vec2 pos = gl_FragCoord.xy;
    vec2 uv = (pos*2. - res.xy) / res.y;
    vec3 uvDir = normalize(vec3(uv, 1));
    mat4 modifiedCam = (cam * mat4(vec4(uvDir, 0), vec4(uvDir, 0), vec4(uvDir, 0), vec4(uvDir, 0)));
    vec3 ogDir = vec3(modifiedCam[0][0], modifiedCam[0][1], modifiedCam[0][2]);
    if (ui && uv.x >= -0.004f && uv.x <= 0.004f && uv.y >= -0.004385f && uv.y <= 0.004385f) {
        fragColor = vec4(0.9, 0.9, 1, 1);
    } else {
        fragColor = raytrace(vec3(cam[3]), ogDir);
        if (hitSelection) {
            if (max(fragColor.r, max(fragColor.g, fragColor.b)) > 0.5f) {
                fragColor/=2;
            } else {
                fragColor*=2;
            }
        }
    }
}