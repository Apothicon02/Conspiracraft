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
    if(!any(mask))
    mask.z = true;

    return vec3(mask);
}

vec4 getVoxel(float x, float y, float z) {
    if (x == 4 && z == 4) {
        if (y == 3) {
            return vec4(0, 1, 0, 1);
        } else if (y == 2) {
            return vec4(1, 0.5, 0, 1);
        } else if (y == 1) {
            return vec4(0.5, 0.5, 0.5, 1);
        } else if (y == 0) {
            return vec4(0.2, 0.2, 0.2, 1);
        }
    }
    return vec4(0.f);
}

vec4 raytrace(vec3 rayPos, vec3 rayDir) {
    vec3 mapPos = floor(clamp(rayPos, vec3(0.0001), vec3(7.9999)));
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((mapPos - rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = stepMask(sideDist);

    vec3 mini = ((mapPos - rayPos) + 0.5 - 0.5 * vec3(raySign)) * deltaDist;
    for (int i = 0; mapPos.x < 8.0 && mapPos.x >= 0.0 && mapPos.y < 8.0 && mapPos.y >= 0.0 && mapPos.z < 8.0 && mapPos.z >= 0.0 && i < 8*3; i++) {
        vec4 voxelColor = getVoxel(mapPos.x, mapPos.y, mapPos.z);
        if (voxelColor.a > 0.f) {
            return voxelColor;
        }

        mask = stepMask(sideDist);
        mapPos += mask * raySign;
        mini = ((mapPos - rayPos) + 0.5 - 0.5 * vec3(raySign)) * deltaDist;
        sideDist += mask * raySign * deltaDist;
    }

    return vec4(0.0);
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
    }
}