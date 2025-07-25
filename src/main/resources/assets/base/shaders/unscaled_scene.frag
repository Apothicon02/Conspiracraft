layout(binding = 0) uniform sampler2D scene_image;
layout(binding = 2) uniform sampler2D scene_lighting_blurred;
layout(binding = 6, rgba32f) uniform image2D scene_unscaled_image;

in vec4 gl_FragCoord;
vec4 lowResColor = vec4(0);
vec4 lowResLighting = vec4(0);

out vec4 fragColor;

vec4 traceBlock(bool isShadow, float chunkDist, float subChunkDist, float blockDist, vec3 intersect, vec3 rayPos, vec3 rayDir, vec3 iMask, int blockType, int blockSubtype, float sunLight, vec3 unmixedFogColor, float mixedTime) {
    vec3 mapPos = floor(clamp(rayPos, vec3(0.0001), vec3(7.9999)));
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((mapPos - rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = iMask;
    vec3 prevMapPos = mapPos+(stepMask(sideDist+(mask*(-raySign)*deltaDist))*(-raySign));

    vec3 mini = ((mapPos - rayPos) + 0.5 - 0.5 * vec3(raySign)) * deltaDist;
    vec3 hitNormal = -mask * raySign;
    float rayLength = 0.f;
    float voxelDist = max(mini.x, max (mini.y, mini.z));
    if (voxelDist > 0.0f) {
        rayLength += voxelDist/8;
    }
    if (blockDist > 0.0f) {
        rayLength += blockDist;
    }
    if (subChunkDist > 0.0f) {
        rayLength += (subChunkDist*8);
    }
    if (chunkDist > 0.0f) {
        rayLength += (chunkDist*16);
    }
    vec3 realPos = (ogRayPos + rayDir * rayLength);
    prevPos = realPos + (hitNormal * 0.001f);
    ivec2 prevBlock = getBlock(prevPos.x, prevPos.y, prevPos.z);
    float fire = blockType == 19 ? 1.f : (blockType == 9 ? 0.05f : 0.f);
    vec4 prevVoxelColor = firstVoxel ? vec4(0) : getVoxel((prevPos.x-int(prevPos.x))*8, (prevPos.y-int(prevPos.y))*8, (prevPos.z-int(prevPos.z))*8, prevPos.x, prevPos.y, prevPos.z, prevBlock.x, prevBlock.y, fire);
    firstVoxel = false;
    while (mapPos.x < 8.0 && mapPos.x >= 0.0 && mapPos.y < 8.0 && mapPos.y >= 0.0 && mapPos.z < 8.0 && mapPos.z >= 0.0) {
        vec4 voxelColor = getVoxel(mapPos.x, mapPos.y, mapPos.z, rayMapPos.x, rayMapPos.y, rayMapPos.z, blockType, blockSubtype, fire);
        if (voxelColor.a > 0.f) {
            bool canHit = bool(prevVoxelColor.a < voxelColor.a);
            float shouldReflect = 0.f;
            if (reflectivity == 0.f && canHit) {
                if (max(voxelColor.r, max(voxelColor.g, voxelColor.b)) > 0.8f) {
                    if (blockType == 1 && blockSubtype > 0 && prevBlock.x == 0) { //water
                        shouldReflect = 0.6f;
                    } else if (blockType == 7) { //kyanite
                        reflectivity = 0.66f;
                    } else if (blockType >= 11 && blockType <= 13) { //glass
                        reflectivity = 0.5f;
                    } else if (blockType == 15 || (blockType == 1 && blockSubtype == 0) || blockType == 22) { //planks & steel
                        reflectivity = 0.16f;
                    }
                }
            }
            normal = ivec3(mapPos - prevMapPos);
            if (voxelColor.a < 1.f) {
                tint += vec3(toLinear(voxelColor)) * (tint == vec3(0) ? 2 : 1);
                //bubbles start
                if (isShadow) { //add check to not return early for semi-transparent blocks that contain solid voxels.
                    return vec4(0);
                }
                bool underwater = false;
                if (blockType == 1 && !renderingHand) {
                    if (getBlock(realPos.x, realPos.y+0.15f, realPos.z).x != 1) { //change to allow non-full water blocks to have caustics.
                      if (isCaustic(vec2(rayMapPos.x, rayMapPos.z)+(mapPos.xz/8))) {
                          if (checkerOn) {
                              voxelColor = fromLinear(vec4(1, 1, 1, 1));
                          }
                          shouldReflect = -0.01f;
                      }
                    } else {
                        underwater = true;
                    }
                } else {
                    float samp = whiteNoise(((vec2(mapPos.x, mapPos.z)*128)+((renderingHand ? 3 : rayMapPos.y)*8)+mapPos.y)+(vec2((renderingHand ? 3 : rayMapPos.x), (renderingHand ? 3 : rayMapPos.z))*8));
                    if (samp > -0.004 && samp < -0.002 || samp > 0 && samp < 0.002) {
                        voxelColor = fromLinear(vec4(1, 1, 1, 1));
                    }
                }
                //bubbles end
                if (reflectivity == 0.f) {
                    reflectivity = shouldReflect;
                }

                if (!underwater) {
                    if (hitPos == vec3(256) && canHit) {
                        prevHitPos = renderingHand ? (camPos+(prevMapPos/8)) : prevPos;
                        hitPos = renderingHand ? (camPos+(mapPos/8)) : realPos;
                    }
                }

                if (underwater) {
                    if (getBlock(realPos.x, int(realPos.y) + 1, realPos.z).x != 0) {
                        return vec4(0);
                    }
                }
            }
            if (voxelColor.a >= 1 && (!isShadow || castsShadow(blockType))) {
                if (hitPos == vec3(256)) {
                    prevHitPos = renderingHand ? (camPos+(prevMapPos/8)) : prevPos;;
                    hitPos = renderingHand ? (camPos+(mapPos/8)) : realPos;
                }
                //face-based brightness start
                if (normal.y >0) { //down
                    normalBrightness = 0.7f;
                } else if (normal.y <0) { //up
                    normalBrightness = 1.f;
                } else if (normal.z >0) { //south
                    normalBrightness = 0.85f;
                } else if (normal.z <0) { //north
                    normalBrightness = 0.85f;
                } else if (normal.x >0) { //west
                    normalBrightness = 0.75f;
                } else if (normal.x <0) { //east
                    normalBrightness = 0.95f;
                }
                //face-based brightness end

                if (prevBlock.x == 1 && prevBlock.y > 0) {
                    if (isCaustic(vec2(rayMapPos.x, rayMapPos.z) + (mapPos.xz / 8))) {
                        addFakeCaustics = true;
                    }
                }

                return vec4(vec3(voxelColor), 1);
            }
        }

        mask = stepMask(sideDist);
        prevMapPos = mapPos;
        mapPos += mask * raySign;
        mini = ((mapPos - rayPos) + 0.5 - 0.5 * vec3(raySign)) * deltaDist;
        hitNormal = -mask * raySign;
        rayLength = 0.f;
        voxelDist = max(mini.x, max (mini.y, mini.z));
        if (voxelDist > 0.0f) {
            rayLength += voxelDist/8;
        }
        if (blockDist > 0.0f) {
            rayLength += blockDist;
        }
        if (subChunkDist > 0.0f) {
            rayLength += (subChunkDist*8);
        }
        if (chunkDist > 0.0f) {
            rayLength += (chunkDist*16);
        }
        realPos = (ogRayPos + rayDir * rayLength);
        prevPos = realPos + (hitNormal * 0.001f);
        prevVoxelColor = voxelColor;
        sideDist += mask * raySign * deltaDist;
    }

    return vec4(0.0);
}

vec4 dda(bool isShadow, float chunkDist, float subChunkDist, int condensedChunkPos, vec3 offset, vec3 rayPos, vec3 rayDir, vec3 iMask, bool inBounds, bool checkBlocks) {
    vec3 mapPos = floor(clamp(rayPos, vec3(0.0001), vec3(7.9999)));
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((mapPos-rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = iMask;
    rayMapPos = offset+mapPos;
    prevRayMapPos = rayMapPos;

    if (distance(rayMapPos, ogRayPos) > (isShadow ? 1000 : renderDistance)) {
        return vec4(-1);
    }

    while (mapPos.x < 8.0 && mapPos.x >= 0.0 && mapPos.y < 8.0 && mapPos.y >= 0.0 && mapPos.z < 8.0 && mapPos.z >= 0.0) {
        float adjustedTime = clamp(abs(1-clamp((distance(rayMapPos, sun-vec3(0, sun.y, 0))/1.33)/(size/1.5), 0, 1))*2, 0.05f, 1.f);
        float adjustedTimeCam = clamp(abs(1-clamp((distance(camPos, sun-vec3(0, sun.y, 0))/1.33)/(size/1.5), 0, 1))*2, 0.05f, 0.9f);
        float timeBonus = gradient(rayMapPos.y, 64.f, 372.f, 0.1f, 0.f);
        float mixedTime = (adjustedTime/2)+(adjustedTimeCam/2)+timeBonus;
        if (renderingHand) {
            vec3 handPos = vec3(-1, -2, 1);
            if (rayMapPos == handPos || rayMapPos == handPos+vec3(1, 0, 0)) {
                ivec2 blockInfo = ivec2(hand);
                vec3 mini = ((mapPos-rayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
                float d = max (mini.x, max (mini.y, mini.z));
                vec3 intersect = rayPos + rayDir*d;
                vec3 uv3d = intersect - mapPos;
                float blockDist = max(mini.x, max(mini.y, mini.z));

                if (mapPos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
                    uv3d = rayPos - mapPos;
                }

                if (blockInfo.x != 0.f) {
                    if (rayMapPos.x < 0) {
                        shift = 1;
                    } else {
                        shift = 2;
                    }
                    vec4 color = traceBlock(isShadow, chunkDist, subChunkDist, blockDist, intersect, uv3d * 8.0, rayDir, mask, blockInfo.x, blockInfo.y, 1, vec3(0), mixedTime);
                    if (color.a >= 1.f) {
                        return color;
                    }
                }
            }
        } else if (checkBlocks) {
            //block start
            ivec2 blockInfo = ivec2(0);
            vec3 uv3d = vec3(0);
            vec3 intersect = vec3(0);
            vec3 mini = ((mapPos-rayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
            float blocKDist = max(mini.x, max(mini.y, mini.z));
            if (inBounds) {
                blockInfo = getBlock(rayMapPos);

                intersect = rayPos + rayDir*blocKDist;
                uv3d = intersect - mapPos;

                if (mapPos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
                    uv3d = rayPos - mapPos;
                }
            }
            //block end

            vec4 color = vec4(0);
            float camDist = distance(camPos, rayMapPos)/renderDistance;
            float whiteness = gradient(rayMapPos.y, 64, 372, 0, 0.8);
            vec3 unmixedFogColor = mix(vec3(0.416, 0.495, 0.75), vec3(1), whiteness);
            if (blockInfo.x != 0.f) {
                float fogNoise = max(0, cloudNoise((vec2(hitPos.x, hitPos.z))+(floor(hitPos.y/16)+(float(time)*7500))))*gradient(hitPos.y, 63, 96, 0, 0.77);
                if (blockInfo.x != 1.f) {
                    fogNoise+=gradient(hitPos.y, 63.f, 96.f, 0.f, 1.f);
                }
                float linearDistFog = camDist+(fogNoise/2)+((fogNoise/2)*camDist);
                distanceFogginess = max(distanceFogginess, clamp((exp2(linearDistFog-0.75f)+min(0.f, linearDistFog-0.25f))-0.1f, 0.f, 1.f));
                float sunLight = (lighting.a/16)*(mixedTime-timeBonus);
                color = traceBlock(isShadow, chunkDist, subChunkDist, blocKDist, intersect, uv3d * 8.0, rayDir, mask, blockInfo.x, blockInfo.y, sunLight, unmixedFogColor, mixedTime);
                if ((blockInfo.x == 17 || blockInfo.x == 21) && color.a >= 1) {
                    color = vec4(hsv2rgb(rgb2hsv(vec3(color))-vec3(0, cloudNoise(vec2(rayMapPos.x, rayMapPos.z)*10), 0)), 1);
                }
                //lighting start
                float lightNoise = max(0, cloudNoise((vec2(prevPos.x, prevPos.y)*64)+(float(time)*10000))+cloudNoise((vec2(prevPos.y, prevPos.z)*64)+(float(time)*10000))+cloudNoise((vec2(prevPos.z, prevPos.x)*64)+(float(time)*10000)));

                vec3 relativePos = prevPos-rayMapPos; //smooth position here maybe
                lighting = getLighting(prevPos.x, prevPos.y, prevPos.z);
                lighting = fromLinear(lighting);
                lightFog = max(lightFog, lighting*(1-(vec4(0.5, 0.5, 0.5, 0)*vec4(lightNoise))));
                lighting *= 1+(vec4(0.5, 0.5, 0.5, -0.25f)*vec4(lightNoise, lightNoise, lightNoise, 1));
                //lighting end
            }

            if (color.a >= 1) {
                return color;
            } else if (color.a <= -1) {
                return vec4(-1);
            }
        }

        mask = stepMask(sideDist);
        mapPos += mask * raySign;
        prevRayMapPos = rayMapPos;
        rayMapPos = offset+mapPos;
        sideDist += mask * raySign * deltaDist;
    }

    return vec4(0);
}

vec4 subChunkDDA(bool isShadow, float chunkDist, int condensedChunkPos, vec3 offset, vec3 rayPos, vec3 rayDir, vec3 iMask, bool inBounds, bool checkSubChunks) {
    vec3 mapPos = floor(clamp(rayPos, vec3(0.0001), vec3(1.9999)));
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((mapPos-rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = iMask;
    vec3 realPos = offset+(mapPos*8);
    int subChunks = chunkBlocksData[(condensedChunkPos*5)+4];

    if (distance(realPos, ogRayPos) > (isShadow ? 1000 : renderDistance)) {
        return vec4(-1);
    }

    while (mapPos.x < 2.0 && mapPos.x >= 0.0 && mapPos.y < 2.0 && mapPos.y >= 0.0 && mapPos.z < 2.0 && mapPos.z >= 0.0) {
        if (renderingHand) {
            vec3 mini = ((mapPos-rayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
            float d = max (mini.x, max (mini.y, mini.z));
            vec3 intersect = rayPos + rayDir*d;
            vec3 uv3d = intersect - mapPos;
            float subChunkDist = max(mini.x, max(mini.y, mini.z));

            if (mapPos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
                uv3d = rayPos - mapPos;
            }

            vec4 color = dda(isShadow, chunkDist, subChunkDist, condensedChunkPos, realPos, uv3d * 8.0, rayDir, mask, true, true);

            if (color.a >= 1) {
                return color;
            }
        } else {
            bool checkBlocks = false;
            if (checkSubChunks) {
                ivec3 localPos = ivec3(realPos.x, realPos.y, realPos.z) & ivec3(15);
                int subChunkPos = ((((localPos.x >= subChunkSize  ? 1 : 0)*2)+(localPos.z >= subChunkSize  ? 1 : 0))*2)+(localPos.y >= subChunkSize  ? 1 : 0);
                checkBlocks = (((subChunks >> (subChunkPos % 32)) & 1) > 0);
            }
            if (checkBlocks) {
                vec3 mini = ((mapPos-rayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
                float subChunkDist = max(mini.x, max(mini.y, mini.z));
                vec3 intersect = rayPos + rayDir*subChunkDist;
                vec3 uv3d = intersect - mapPos;

                if (mapPos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
                                               uv3d = rayPos - mapPos;
                }

                vec4 color = dda(isShadow, chunkDist, subChunkDist, condensedChunkPos, realPos, uv3d * 8.0, rayDir, mask, inBounds, true);

                if (color.a >= 1) {
                    return color;
                } else if (color.a <= -1) {
                    return vec4(-1);
                }
            }
        }

        mask = stepMask(sideDist);
        mapPos += mask * raySign;
        realPos = offset+(mapPos*8);
        sideDist += mask * raySign * deltaDist;
    }

    return vec4(0);
}

vec4 traceWorld(bool isShadow, vec3 ogPos, vec3 rayDir) {
    ogRayPos = ogPos;
    vec3 rayPos = ogRayPos/16;
    vec3 rayMapChunkPos = floor(rayPos);
    vec3 prevRayMapPos = rayMapChunkPos;
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((rayMapChunkPos-rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = stepMask(sideDist);

    while (distance(rayMapChunkPos, rayPos) < (renderingHand ? 16 : ((isShadow ? 1000 : renderDistance)/chunkSize))) {
        if (renderingHand) {
            vec3 mini = ((rayMapChunkPos-rayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
            float d = max (mini.x, max (mini.y, mini.z));
            vec3 intersect = rayPos + rayDir*d;
            vec3 uv3d = intersect - rayMapChunkPos;
            float chunkDist = max(mini.x, max(mini.y, mini.z));

            if (rayMapChunkPos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
                uv3d = rayPos - rayMapChunkPos;
            }
            ivec3 chunkPos = ivec3(rayMapChunkPos);
            vec4 color = subChunkDDA(isShadow, chunkDist, (((chunkPos.x*sizeChunks)+chunkPos.z)*heightChunks)+chunkPos.y, chunkPos*16, uv3d * 2.0, rayDir, mask, true, true);
            if (color.a >= 1) {
                return color;
            }
        } else {
            bool inHorizontalBounds = bool(rayMapChunkPos.x >= 0 && rayMapChunkPos.x < sizeChunks && rayMapChunkPos.z >= 0 && rayMapChunkPos.z < sizeChunks);
            if (!inHorizontalBounds) {
                break;
            }
            bool inBounds = bool(inHorizontalBounds && rayMapChunkPos.y >= 0 && rayMapChunkPos.y < heightChunks);
            bool checkSubChunks = inBounds ? !isChunkAir(int(rayMapChunkPos.x), int(rayMapChunkPos.y), int(rayMapChunkPos.z)) : false;
            if (checkSubChunks) {
                vec3 mini = ((rayMapChunkPos - rayPos) + 0.5 - 0.5 * vec3(raySign)) * deltaDist;
                float chunkDist = max(mini.x, max(mini.y, mini.z));
                vec3 intersect = rayPos + rayDir * chunkDist;
                vec3 uv3d = intersect - rayMapChunkPos;

                if (rayMapChunkPos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
                                                       uv3d = rayPos - rayMapChunkPos;
                }
                ivec3 chunkPos = ivec3(rayMapChunkPos);
                vec4 color = subChunkDDA(isShadow, chunkDist, (((chunkPos.x * sizeChunks) + chunkPos.z) * heightChunks) + chunkPos.y, chunkPos * 16, uv3d * 2.0, rayDir, mask, inBounds, checkSubChunks);
                if (color.a >= 1) {
                    return color;
                } else if (color.a <= -1) {
                    prevHitPos = rayMapChunkPos * 16;
                    hitPos = rayMapChunkPos * 16;
                    return vec4(1, 0, 0, 0);
                }
            }
        }

        mask = stepMask(sideDist);
        prevRayMapPos = rayMapChunkPos;
        rayMapChunkPos += mask * raySign;
        sideDist += mask * raySign * deltaDist;
    }

    vec3 mini = ((rayMapChunkPos-rayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
    float chunkDist = max(mini.x, max(mini.y, mini.z));
    vec3 intersect = rayPos + rayDir*chunkDist;
    vec3 uv3d = intersect - rayMapChunkPos;

    if (rayMapChunkPos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
                                           uv3d = rayPos - rayMapChunkPos;
    }
    prevHitPos = (rayMapChunkPos+uv3d)*16;
    hitPos = prevHitPos;
    if (!renderingHand && !isShadow) {
        float sunDir = dot(normalize(sun - camPos), rayDir);
        if (sunDir > 0.85f && sunDir < 1.15f) {
            lighting = fromLinear(vec4(0, 0, 0, 20));
            lightFog = max(lightFog, lighting);
            fragColor = vec4(1, 1, 0, 1);
        }
        vec3 lunaPos = (((sun-vec3(size/2, 0, size/2))*vec3(-1, 1, -1))+vec3(size/2, 0, size/2));
        float lunaDir = dot(normalize(lunaPos - camPos), rayDir);
        if (lunaDir > 0.85f && lunaDir < 1.15) {
            lighting = fromLinear(vec4(5, 5, 5, 0));
            lightFog = max(lightFog, lighting);
            fragColor = vec4(0.95f, 0.95f, 1, 1);
        }
    }
    return vec4(0);
}

bool isSky = false;

vec3 prevReflectPos = vec3(0);
vec3 reflectPos = vec3(0);

void clearVars(bool clearHit, bool clearTint) {
    normalBrightness = 1.f;
    firstVoxel = true;
    if (clearHit) {
        prevHitPos = vec3(256);
        hitPos = vec3(256);
    }
    prevRayMapPos = vec3(0);
    rayMapPos = vec3(0);
    lighting = lowResLighting;
    lightFog = lowResColor;
    if (clearTint) {
        tint = vec3(0);
    }
    normal = ivec3(0);
}

vec4 prevFog = vec4(1);
vec4 prevSuperFinalTint = vec4(1);

vec4 raytrace(vec3 ogRayPos, vec3 dir, bool checkShadow) {
    clearVars(true, false);
    vec4 color = traceWorld(false, ogRayPos, dir);
    if (renderingHand) {
        prevPos = prevHitPos;
    }
    prevReflectPos = prevHitPos;
    reflectPos = hitPos;
    isSky = renderingHand ? false : color.a < 1.f;
    float borders = max(gradient(hitPos.x, 0, 128, 0, 1), max(gradient(hitPos.z, 0, 128, 0, 1), max(gradient(hitPos.x, size, size-128, 0, 1), gradient(hitPos.z, size, size-128, 0, 1))));
    if (isSky) {
        lighting = fromLinear(borders > 0.f ? mix(vec4(0, 0, 0, 20), getLighting(hitPos.x, hitPos.y, hitPos.z), borders) : vec4(0, 0, 0, 20));
        lightFog = lighting;
    }
    float adjustedTime = clamp(abs(1-clamp((distance(hitPos, sun-vec3(0, sun.y, 0))/1.33)/(size/1.5), 0, 1))*2, 0.05f, 1.f);
    float adjustedTimeCam = clamp(abs(1-clamp((distance(camPos, sun-vec3(0, sun.y, 0))/1.33)/(size/1.5), 0, 1))*2, 0.05f, 0.9f);
    float timeBonus = gradient(hitPos.y, 64.f, 372.f, 0.1f, 0.f);
    float mixedTime = (adjustedTime/2)+(adjustedTimeCam/2)+timeBonus;
    float fogNoise = (max(0, cloudNoise((vec2(hitPos.x, hitPos.z))+(floor(hitPos.y/16)+(float(time)*7500))))*gradient(hitPos.y, 63, 96, 0, 0.77))+gradient(hitPos.y, 63, 96, 0, 0.33f);
    float fogDist = distance(camPos, prevPos)/renderDistance;
    float linearDistFog = fogDist+(fogNoise/2)+((fogNoise/2)*fogDist);
    //distanceFogginess = max(distanceFogginess, exp2(linearDistFog-0.75f)+min(0, linearDistFog-0.25f));
    if (isSky) {
        distanceFogginess = 1;
    }
    distanceFogginess = clamp(distanceFogginess, 0.f, 1.f);
    if (renderingHand) {
        lighting = fromLinear(getLighting(camPos.x, camPos.y, camPos.z));
        lightFog = max(lightFog, lighting*(1-vec4(0.5, 0.5, 0.5, 0)));
    } else {
        distanceFogginess = max(distanceFogginess, borders);
        float factor = pow(distanceFogginess, 4);
        lighting = mix(lighting, vec4(0, 0, 0, 20), factor);
        lightFog = mix(lightFog, vec4(0, 0, 0, 20), factor);
    }
    lighting = pow(lighting/20, vec4(2.f))*vec4(200, 200, 200, 32.5f);
    lightFog = pow(lightFog/20, vec4(2.f))*vec4(200, 200, 200, 32.5f);
    float sunLight = (lighting.a/20)*(mixedTime-timeBonus);
    float whiteness = gradient(hitPos.y, 64, 372, 0, 0.8);
    vec3 sunColor = mix(mix(vec3(0.0f, 0.0f, 4.5f), vec3(2.125f, 0.875f, 0.125f), mixedTime*4), vec3(0.1f, 0.95f, 1.5f), mixedTime) * 0.15f;

    vec3 finalRayMapPos = rayMapPos;
    vec4 finalLighting = lighting;
    float finalNormalBrightness = normalBrightness;
    float sunBrightness = (lightFog.a/12)*mixedTime;
    vec3 finalLightFog = max(vec3(lightFog)*((0.7-min(0.7, (lightFog.a/20)*mixedTime))/4), ((sunColor*mix(sunColor.r*6, 0.2, max(sunColor.r, max(sunColor.g, sunColor.b))))*sunBrightness));
    vec3 finalTint = max(vec3(1), tint);

    vec3 cloudPos = (vec3(cam * vec4(uvDir * 500.f, 1.f))-camPos);
    if ((isSky || borders > 0.f) && cloudsEnabled) {
        whiteness = whiteness+((sqrt(max(0, cloudNoise((vec2(cloudPos.x, cloudPos.y*1.5f))+(float(time)*cloudSpeed))+cloudNoise((vec2(cloudPos.y*1.5f, cloudPos.z))+(float(time)*cloudSpeed))+cloudNoise((vec2(cloudPos.z, cloudPos.x))+(float(time)*cloudSpeed)))*gradient(hitPos.y, 0, 372, 1.5f, 0))*gradient(hitPos.y, 0, 372, 1, 0)) * (isSky ? 1.f : pow(borders, 32)));
    }
    vec3 unmixedFogColor = mix(vec3(0.416, 0.495, 0.75), vec3(1), whiteness)*min(1, 0.05f+sunLight);

    if (addFakeCaustics) {
        float factor = max(1, 2.5-distanceFogginess)*(1+(max(0, abs(1-mixedTime)-0.9)*10));
        color += (sunLight*(factor/10));
    }
    if (!isSky && checkShadow && shadowsEnabled && !hitSun) {
        if (color.a >= 1.f && sunLight > 0.f) {
            vec3 shadowPos = prevPos;
            vec3 sunDir = normalize(sun - shadowPos);
            if (sunDir.x == 0) {
                sunDir.x = 0.00001f;
            }
            if (sunDir.y == 0) {
                sunDir.y = 0.00001f;
            }
            if (sunDir.z  == 0) {
                sunDir.z = 0.00001f;
            }
            clearVars(true, true);
            float oldReflectivity = reflectivity;
            reflectivity = 0.f;
            float factor = max(1, 2.5-distanceFogginess)*(1+(max(0, abs(1-mixedTime)-0.9)*10));;
            if (traceWorld(true, shadowPos, sunDir).a >= 1.f) {
                sunLight /= factor;
            }
            reflectivity = oldReflectivity;
            tint = max(vec3(1), tint*2);
            float tintFactor = max(tint.r, max(tint.g, tint.b));
            color = mix(color, fromLinear(vec4((vec3(tint)/tintFactor)-1, 1)), (tintFactor-1)/20);//sun tint
        }
    }
    vec3 finalLight = (vec3(finalLighting*((0.7-min(0.7, (finalLighting.a/20)*mixedTime))/4))+sunLight)*finalNormalBrightness;
    color = vec4(vec3(color)*min(vec3(1.15f), finalLight), color.a);//light
    //fog start
    if (getBlock(camPos).x == 1) {
        unmixedFogColor += (vec3(-0.5f, -0.5f, 0.5f)*min(1, distanceFogginess*10));
    }
    color = vec4(mix(vec3(color), unmixedFogColor, distanceFogginess), color.a);
    vec4 fog = 1+vec4(vec3(finalLightFog), 1);
    color *= fog;
    color *= prevFog;
    prevFog = fog;

    //fog end
    //transparency start
    vec4 superFinalTint = fromLinear(vec4(vec3(finalTint)/(max(finalTint.r, max(finalTint.g, finalTint.b))), 1));
    color *= superFinalTint;
    color *= prevSuperFinalTint;
    prevSuperFinalTint = superFinalTint;
    //transparency end

    if (renderingHand) {
        color = vec4(mix(vec3(color), unmixedFogColor/4.f, reflectivity), color.a);
    }

    //selection start
    if (ui && !renderingHand && selected == ivec3(finalRayMapPos)) {
        color = vec4(mix(vec3(color), vec3(0.7, 0.7, 1), 0.5f), color.a);
    }
    //selection end
    return color;
}

//#extension GL_ARB_gpu_shader_int64 : enable
//#extension GL_ARB_shader_clock : require

void main() {
    //uint64_t startTime = clockARB();

    checkerOn = checker(ivec2(gl_FragCoord.xy));
    vec2 pos = gl_FragCoord.xy + (checkerOn ? ivec2(res.x/2, 0) : ivec2(0));
    lowResColor = vec4(texture(scene_image, vec2(pos.xy/res)));
    //fragColor = lowResColor;
    lowResLighting = vec4(texture(scene_lighting_blurred, vec2(pos.xy/res)));
    //fragColor = vec4((vec3(lowResLighting.a/20)*(0.8f+lowResLighting.rgb/10)), 1);

    vec2 uv = (pos*2. - res.xy) / res.y;
    if (ui && uv.x >= -0.004f && uv.x <= 0.004f && uv.y >= -0.004385f && uv.y <= 0.004385f) {
        fragColor = vec4(0.9, 0.9, 1, 1);
    } else {
        vec2 uv = (pos*2. - res.xy) / res.y;
        uvDir = normalize(vec3(uv, 1));
        ivec2 pixel = ivec2(pos.x, pos.y * res.y);
        vec3 ogDir = vec3(cam * vec4(uvDir, 0));
        vec4 handColor = raytrace(vec3(0, 0, 0), uvDir, true);
        shift = 0;
        renderingHand = false;
        if (handColor.a < 1) {
            prevFog = vec4(1);
            prevSuperFinalTint = vec4(1);
            distanceFogginess = 0;
            vec3 skipPos = camPos + (ogDir * (lowResColor.a * renderDistance));
            fragColor = raytrace(camPos, ogDir, true);
            //reflections start
            if (!isSky && reflectivity > 0.f) {
                vec3 reflectDir = reflect(ogDir, normalize(reflectPos - prevReflectPos));
                fragColor = mix(fragColor, raytrace(reflectPos, reflectDir, false), reflectivity * dot(normalize(ogDir), normalize(reflectDir)));
            }
            //reflections end
        } else {
            fragColor = handColor;
        }

        fragColor = toLinear(fragColor);
    }
    imageStore(scene_unscaled_image, ivec2(pos.xy), vec4(fragColor.rgb, 1));
    fragColor = vec4(0);
    //fragColor = mix(fragColor, vec4(float(clockARB() - startTime) * 0.0000005, 0.0, 1.0, 1.0), 0.95f);
}