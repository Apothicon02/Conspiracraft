layout(binding = 1, rgba32f) uniform writeonly image2D scene_lighting;

in vec4 gl_FragCoord;

out vec4 fragColor;

bool hitBright = false;

vec4 traceBlock(bool isShadow, float chunkDist, float subChunkDist, float blockDist, vec3 intersect, vec3 rayPos, vec3 rayDir, vec3 iMask, int blockType, int blockSubtype, float sunLight, vec3 unmixedFogColor, float mixedTime) {
    vec3 mapPos = floor(clamp(rayPos, vec3(0.0001), vec3(7.9999)));
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((mapPos - rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = iMask;

    bool wasTinted = false;
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
            normal = normalize(vec3(realPos - prevPos));
            if (voxelColor.a < 1.f) {
                //bubbles start
                if (isShadow && !raytracedCaustics) { //add check to not return early for semi-transparent blocks that contain solid voxels.
                    return vec4(0);
                }
                bool underwater = false;
                if (blockType == 1) {
                    if (getBlock(realPos.x, realPos.y+0.15f, realPos.z).x == 0) { //change to allow non-full water blocks to have caustics.
                      if (isCaustic(vec2(rayMapPos.x, rayMapPos.z)+(mapPos.xz/8))) {
                          voxelColor = fromLinear(vec4(0.9f, 1, 1, 1));
                          shouldReflect = -0.01f;
                      }
                    } else {
                        underwater = true;
                    }
                } else {
                    float samp = whiteNoise(((vec2(mapPos.x, mapPos.z)*128)+(( rayMapPos.y)*8)+mapPos.y)+(vec2((rayMapPos.x), (rayMapPos.z))*8));
                    if (samp > 0 && samp < 0.002) {
                        voxelColor = fromLinear(vec4(1, 1, 1, 1));
                    }
                }
                //bubbles end
                if (reflectivity == 0.f) {
                    reflectivity = shouldReflect;
                }

                if (!underwater) {
                    if (hitPos == vec3(256) && canHit) {
                        prevHitPos = prevPos;
                        hitPos = realPos;
                    }
                    vec3 finalLight = vec3(lighting * ((0.7 - min(0.7, (lighting.a / 20) * mixedTime)) / 4)) + sunLight;
                    vec4 color = vec4(vec3(voxelColor) * min(vec3(1.15f), finalLight), 1);//light
                    //fog start
                    color = vec4(mix(vec3(color), unmixedFogColor, distanceFogginess), 1);
                    //fog end
                    tint += vec3(toLinear(color));
                }

                if (underwater) {
                    if (getBlock(realPos.x, int(realPos.y) + 1, realPos.z).x != 0) {
                        return vec4(0);
                    }
                }
            }
            if (voxelColor.a >= 1) {
                if ((voxelColor.r > 0.95f || voxelColor.g > 0.95f || voxelColor.b > 0.95f) && isBlockLight(ivec2(blockType, blockSubtype))) {
                    hitBright = true;
                }
                if (hitPos == vec3(256)) {
                    prevHitPos = prevPos;;
                    hitPos = realPos;
                }

                //snow start
                if (snowing) {
                    if (toLinear(vec4(lighting.a)).a >= 20) {
                        int aboveBlockType = blockType;
                        int aboveBlockSubtype = blockSubtype;
                        int aboveY = int(mapPos.y+1);
                        int aboveRayMapPosY = int(rayMapPos.y);
                        if (aboveY == 8) {
                            aboveY = 0;
                            aboveRayMapPosY++;
                            ivec2 aboveBlocKInfo = getBlock(int(rayMapPos.x), aboveRayMapPosY, int(rayMapPos.z));
                            aboveBlockType = aboveBlocKInfo.x;
                            aboveBlockSubtype = aboveBlocKInfo.y;
                        }
                        vec4 aboveColorData = getVoxel(mapPos.x, float(aboveY), mapPos.z, rayMapPos.x, aboveRayMapPosY, rayMapPos.z, aboveBlockType, aboveBlockSubtype, 0);
                        if (aboveColorData.a <= 0 || aboveBlockType == 4 || aboveBlockType == 5 || aboveBlockType == 18) {
                            voxelColor = mix(voxelColor, vec4(1-(abs(noise(vec2(int(mapPos.x)*64, int(mapPos.z)*64)))*0.66f))-vec4(0.14, 0.15, -0.05, 0)*1.33f, 0.9f);
                            isSnowFlake = true;
                        }
                    }
                }
                //snow end

                if (!shadowsEnabled || !raytracedCaustics) {
                    if (prevBlock.x == 1 && prevBlock.y > 0) {
                        if (isCaustic(vec2(rayMapPos.x, rayMapPos.z) + (mapPos.xz / 8))) {
                            addFakeCaustics = true;
                        }
                    }
                }

                return vec4(vec3(voxelColor), 1);
            }
        }

        mask = stepMask(sideDist);
        mapPos += mask * raySign;
        prevMapPos = mapPos;
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

    return vec4(0);
}

ivec2 blockInfo = ivec2(0);

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
        if (checkBlocks) {
            //block start
            blockInfo = ivec2(0);
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
            vec3 lightPos = rayMapPos;
            float camDist = distance(camPos, rayMapPos)/renderDistance;
            float whiteness = gradient(rayMapPos.y, 64, 372, 0, 0.8);
            vec3 unmixedFogColor = mix(vec3(0.416, 0.495, 0.75), vec3(1), whiteness);
            if (blockInfo.x != 0.f) {
                float fogNoise = max(0, noise((vec2(hitPos.x, hitPos.z))+(floor(hitPos.y/16)+(float(time)*7500))))*gradient(hitPos.y, 63, 96, 0, 0.77);
                if (blockInfo.x != 1.f) {
                    fogNoise+=gradient(hitPos.y, 63.f, 96.f, 0.f, 1.f);
                }
                float linearDistFog = camDist+(fogNoise/2)+((fogNoise/2)*camDist);
                distanceFogginess = max(distanceFogginess, clamp((exp2(linearDistFog-0.75f)+min(0.f, linearDistFog-0.25f))-0.1f, 0.f, 1.f));
                float sunLight = (lighting.a/18)*(mixedTime-timeBonus);
                color = traceBlock(isShadow, chunkDist, subChunkDist, blocKDist, intersect, uv3d * 8.0, rayDir, mask, blockInfo.x, blockInfo.y, sunLight, unmixedFogColor, mixedTime);
                if ((blockInfo.x == 17 || blockInfo.x == 21) && color.a >= 1) {
                    color = vec4(hsv2rgb(rgb2hsv(vec3(color))-vec3(0, noise(vec2(rayMapPos.x, rayMapPos.z)*10), 0)), 1);
                }
                lightPos = prevPos;
            }

            //lighting start
            if (inBounds && color.a >= 1.f) {
                float lightNoise = max(0, cloudNoise((vec2(lightPos.x, lightPos.y)*64)+(float(time)*15000))+cloudNoise((vec2(lightPos.y, lightPos.z)*64)+(float(time)*15000))+cloudNoise((vec2(lightPos.z, lightPos.x)*64)+(float(time)*15000)));
                vec3 relativePos = lightPos-rayMapPos;
                lighting = getLighting(lightPos.x, lightPos.y, lightPos.z, true, true, true);
                //smooth lighting start
                vec4 verticalLighting = getLighting(lightPos.x, lightPos.y+(relativePos.y >= 0.5f ? 0.5f : -0.5f), lightPos.z, false, true, false);
                verticalLighting = mix(relativePos.y >= 0.5f ? lighting : verticalLighting, relativePos.y >= 0.5f ? verticalLighting : lighting, relativePos.y);
                vec4 northSouthLighting = getLighting(lightPos.x, lightPos.y, lightPos.z+(relativePos.z >= 0.5f ? 0.5f : -0.5f), false, false, true);
                northSouthLighting = mix(relativePos.z >= 0.5f ? lighting : northSouthLighting, relativePos.z >= 0.5f ? northSouthLighting : lighting, relativePos.z);
                vec4 eastWestLighting = getLighting(lightPos.x+(relativePos.x >= 0.5f ? 0.5f : -0.5f), lightPos.y, lightPos.z, true, false, false);
                eastWestLighting = mix(relativePos.x >= 0.5f ? lighting : eastWestLighting, relativePos.x >= 0.5f ? eastWestLighting : lighting, relativePos.x);
                lighting = mix(min(mix(eastWestLighting, verticalLighting, 0.25), mix(northSouthLighting, verticalLighting, 0.25)), lighting, 0.8);
                //smooth lighting end
                lighting = fromLinear(lighting);
                lightFog = max(lightFog, lighting*(1-(vec4(0.5, 0.5, 0.5, 0)*vec4(lightNoise))));
                lighting *= 1+(vec4(0.5, 0.5, 0.5, 0)*vec4(lightNoise, lightNoise, lightNoise, 0));
            }
            //lighting end

            //snow start
            if (snowing) {
                if (blockInfo.x == 0 && toLinear(vec4(lighting.a)).a >= 20) {
                    float samp = whiteNoise((vec2(rayMapPos.x, rayMapPos.z)*32)+(rayMapPos.y+(float(time)*7500)));
                    float samp2 = noise(vec2(rayMapPos.x, rayMapPos.z)*8);
                    if (samp > 0 && samp < 0.002 && samp2 > 0.0f && samp2 < 0.05f) {
                        color = vec4(1);
                        isSnowFlake = true;
                    }
                }
            }
            //snow end

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
        } else {
            //snow start
            if (snowing) {
                if (toLinear(vec4(lighting.a)).a >= 20) {
                    float samp = whiteNoise((vec2(mapPos.x, mapPos.z) * 32) + ((mapPos.y) + (float(time) * 7500)));
                    float samp2 = noise(vec2(mapPos.x, mapPos.z) * 4);
                    if (samp > 0 && samp < 0.002 && samp2 > 0.0f && samp2 < 0.05f) {
                        isSnowFlake = true;
                        return vec4(1);
                    }
                }
            }
            //snow end
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

    while (distance(rayMapChunkPos, rayPos) < ((isShadow ? 1000 : renderDistance)/chunkSize)) {
        bool inHorizontalBounds = bool(rayMapChunkPos.x >= 0 && rayMapChunkPos.x < sizeChunks && rayMapChunkPos.z >= 0 && rayMapChunkPos.z < sizeChunks);
        if (!inHorizontalBounds) {
            break;
        }
        bool inBounds = bool(inHorizontalBounds && rayMapChunkPos.y >= 0 && rayMapChunkPos.y < heightChunks);
        bool checkSubChunks = inBounds ? !isChunkAir(int(rayMapChunkPos.x), int(rayMapChunkPos.y), int(rayMapChunkPos.z)) : false;
        if (checkSubChunks) {
            vec3 mini = ((rayMapChunkPos-rayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
            float chunkDist = max(mini.x, max(mini.y, mini.z));
            vec3 intersect = rayPos + rayDir*chunkDist;
            vec3 uv3d = intersect - rayMapChunkPos;

            if (rayMapChunkPos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
                                                   uv3d = rayPos - rayMapChunkPos;
            }
            ivec3 chunkPos = ivec3(rayMapChunkPos);
            vec4 color = subChunkDDA(isShadow, chunkDist, (((chunkPos.x*sizeChunks)+chunkPos.z)*heightChunks)+chunkPos.y, chunkPos*16, uv3d * 2.0, rayDir, mask, inBounds, checkSubChunks);
            if (color.a >= 1) {
                return color;
            } else if (color.a <= -1) {
                prevHitPos = rayMapChunkPos*16;
                hitPos = rayMapChunkPos*16;
                return vec4(1, 0, 0, 0);
            }
        } else {
            //snow start
            if (snowing) {
                if (toLinear(vec4(lighting.a)).a >= 20) {
                    float samp = whiteNoise((vec2(rayMapChunkPos.x, rayMapChunkPos.z)*64)+((rayMapChunkPos.y)+(float(time)*7500)));
                    float samp2 = noise(vec2(rayMapChunkPos.x, rayMapChunkPos.z)*8);
                    if (samp > 0 && samp < 0.002 && samp2 > 0.0f && samp2 < 0.05f) {
                        isSnowFlake = true;
                        return vec4(1);
                    }
                }
            }
            //snow end
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
    if (!isShadow) {
        float sunDir = dot(normalize(sun - camPos), rayDir);
        if (sunDir > 0.95f && sunDir < 1) {
            lighting = fromLinear(vec4(0, 0, 0, 20));
            lightFog = max(lightFog, lighting);
            hitSun = true;
            return vec4(1, 1, 0, 1);
        }
        float lunaDir = dot(normalize((((sun-vec3(size/2, 0, size/2))*vec3(-1, 1, -1))+vec3(size/2, 0, size/2)) - camPos), rayDir);
        if (lunaDir > 0.953f && lunaDir < 1) {
            lighting = fromLinear(vec4(5, 5, 5, 0));
            lightFog = max(lightFog, lighting);
            return vec4(0.95f, 0.95f, 1, 1);
        }
    }
    return vec4(0);
}

bool isSky = false;

vec3 prevReflectPos = vec3(0);
vec3 reflectPos = vec3(0);

void clearVars(bool clearHit, bool clearTint) {
    hitBright = false;
    blockInfo = ivec2(0);
    firstVoxel = true;
    wasEverTinted = false;
    if (clearHit) {
        prevHitPos = vec3(256);
        hitPos = vec3(256);
    }
    prevRayMapPos = vec3(0);
    rayMapPos = vec3(0);
    lighting = vec4(0);
    lightFog = vec4(0);
    if (clearTint) {
        tint = vec3(0);
    }
    normal = ivec3(0);
}

vec4 prevFog = vec4(1);
vec4 prevSuperFinalTint = vec4(1);
float depth = 0.f;

vec4 raytrace(vec3 ogRayPos, vec3 dir, bool checkShadow) {
    clearVars(true, false);
    vec4 color = traceWorld(false, ogRayPos, dir);
    prevReflectPos = prevHitPos;
    reflectPos = hitPos;
    isSky = color.a < 1.f;
    float borders = max(gradient(hitPos.x, 0, 128, 0, 1), max(gradient(hitPos.z, 0, 128, 0, 1), max(gradient(hitPos.x, size, size-128, 0, 1), gradient(hitPos.z, size, size-128, 0, 1))));
    if (isSky) {
        lighting = fromLinear(borders > 0.f ? mix(vec4(0, 0, 0, 20), getLighting(hitPos.x, hitPos.y, hitPos.z, false, false, false), borders) : vec4(0, 0, 0, 20));
        lightFog = lighting;
    }

    imageStore(scene_lighting, ivec2(gl_FragCoord.xy), lighting * (hitBright ? vec4(3, 3, 3, 1) : vec4(1)));
    depth = distance(camPos, hitPos)/renderDistance;

    if (isSky) {
        depth = 10;
        lighting = fromLinear(borders > 0.f ? mix(vec4(0, 0, 0, 20), lighting, borders) : vec4(0, 0, 0, 20));
        lightFog = lighting;
        float adjustedTime = clamp(abs(1-clamp((distance(hitPos, sun-vec3(0, sun.y, 0))/1.33)/(size/1.5), 0, 1))*2, 0.05f, 1.f);
        float adjustedTimeCam = clamp(abs(1-clamp((distance(camPos, sun-vec3(0, sun.y, 0))/1.33)/(size/1.5), 0, 1))*2, 0.05f, 0.9f);
        float timeBonus = gradient(hitPos.y, 64.f, 372.f, 0.1f, 0.f);
        float mixedTime = (adjustedTime/2)+(adjustedTimeCam/2)+timeBonus;
        float fogNoise = (max(0, noise((vec2(hitPos.x, hitPos.z))+(floor(hitPos.y/16)+(float(time)*7500))))*gradient(hitPos.y, 63, 96, 0, 0.77))+gradient(hitPos.y, 63, 96, 0, 0.33f);
        float fogDist = distance(camPos, prevPos)/renderDistance;
        float linearDistFog = fogDist+(fogNoise/2)+((fogNoise/2)*fogDist);
        distanceFogginess = 1;
        float factor = pow(distanceFogginess, 4);
        lighting = mix(lighting, vec4(0, 0, 0, 20), factor);
        lightFog = mix(lightFog, vec4(0, 0, 0, 20), factor);
        lighting = pow(lighting / 20, vec4(2.f)) * vec4(150, 150, 150, 18.5f);
        lightFog = pow(lightFog / 20, vec4(2.f)) * vec4(150, 150, 150, 18.5f);
        float sunLight = (lighting.a / 18) * (mixedTime - timeBonus);
        float whiteness = gradient(hitPos.y, 64, 372, 0, 0.8);
        vec3 sunColor = mix(mix(vec3(0.0f, 0.0f, 4.5f), vec3(2.125f, 0.875f, 0.125f), mixedTime * 4), vec3(0.1f, 0.95f, 1.5f), mixedTime) * 0.15f;

        vec3 finalRayMapPos = rayMapPos;
        vec4 finalLighting = lighting;
        float sunBrightness = (lightFog.a / 12) * mixedTime;
        vec3 finalLightFog = max(vec3(lightFog) * ((0.7 - min(0.7, (lightFog.a / 20) * mixedTime)) / 4), ((sunColor * mix(sunColor.r * 6, 0.2, max(sunColor.r, max(sunColor.g, sunColor.b)))) * sunBrightness));
        vec3 cloudPos = (vec3(cam * vec4(uvDir * 500.f, 1.f)) - camPos);
        if (cloudsEnabled) {
            whiteness = whiteness + ((sqrt(max(0, cloudNoise((vec2(cloudPos.x, cloudPos.y * 2.5f)) + (float(time) * cloudSpeed)) + cloudNoise((vec2(cloudPos.y * 2.5f, cloudPos.z)) + (float(time) * cloudSpeed)) + cloudNoise((vec2(cloudPos.z, cloudPos.x)) + (float(time) * cloudSpeed))) * gradient(hitPos.y, 0, 372, 1.5f, 0)) * gradient(hitPos.y, 0, 372, 1, 0)) * (isSky ? 1.f : pow(borders, 32)));
        }
        vec3 unmixedFogColor = mix(vec3(0.416, 0.495, 0.75), vec3(1), whiteness) * min(1, 0.05f + sunLight);
        vec3 finalLight = vec3(finalLighting * ((0.7 - min(0.7, (finalLighting.a / 20) * mixedTime)) / 4)) + sunLight;
        color = vec4(vec3(color) * min(vec3(1.15f), finalLight), color.a);//light
        //fog start
        if (getBlock(camPos).x == 1) {
            unmixedFogColor += (vec3(-0.5f, -0.5f, 0.5f) * min(1, distanceFogginess * 10));
        }
        color = vec4(mix(vec3(color), unmixedFogColor, distanceFogginess), color.a);
        vec4 fog = 1 + (isSnowFlake ? vec4(finalLight / 5, 0) : vec4(vec3(finalLightFog), 1));
        color *= fog;
        color *= prevFog;
        prevFog = fog;

        //fog end
    }

    return color;
}

//#extension GL_ARB_gpu_shader_int64 : enable
//#extension GL_ARB_shader_clock : require

void main() {
    //uint64_t startTime = clockARB();
    vec2 uv = (vec2(gl_FragCoord)*2. - res.xy) / res.y;
    uvDir = normalize(vec3(uv, 1));
    vec3 ogDir = vec3(cam * vec4(uvDir, 0));
    fragColor = vec4(vec3(toLinear(raytrace(camPos, ogDir, true))), depth);

    //fragColor = mix(fragColor, vec4(float(clockARB() - startTime) * 0.0000005, 0.0, 1.0, 1.0), 0.95f);
}