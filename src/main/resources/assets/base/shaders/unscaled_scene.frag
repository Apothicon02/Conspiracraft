layout(binding = 6, rgba32f) uniform image3D scene_unscaled_image;

in vec4 gl_FragCoord;

out vec4 fragColor;

void main() {
//    uint64_t startTime = clockARB();

    checkerOn = checker(ivec2(gl_FragCoord.xy));
    vec2 pos = gl_FragCoord.xy + (checkerOn ? ivec2(res.x/2, 0) : ivec2(0));
    vec2 uv = (pos*2. - res.xy) / res.y;
    uvDir = normalize(vec3(uv, 1));
    vec3 ogDir = vec3(cam * vec4(uvDir, 0));
    if (ui && uv.x >= -0.004f && uv.x <= 0.004f && uv.y >= -0.004385f && uv.y <= 0.004385f) {
        fragColor = vec4(0.9, 0.9, 1, 1);
    } else {
        renderingHand = true;
        vec2 relativePos = pos/res;
        vec4 handColor = relativePos.x >= 0.35f && relativePos.x <= 0.65f && relativePos.y < 0.25f ? raytrace(vec3(0, 0, 0), uvDir, true, 2) : vec4(0);
        renderingHand = false;
        shift = 0;
        prevFog = vec4(1);
        prevSuperFinalTint = vec4(1);
        distanceFogginess = 0;
        if (handColor.a < 1) {
            fragColor = raytrace(camPos, ogDir, true, renderDistance);
        } else {
            fragColor = handColor;
        }
    }
    //fragColor = vec4(float(clockARB() - startTime) * 0.0000005);
    imageStore(scene_unscaled_image, ivec3(pos.xy, 0), vec4(fragColor.rgb, depth));
    vec3 reflectDir = reflect(ogDir, normalize(reflectPos - prevReflectPos));
    imageStore(scene_unscaled_image, ivec3(pos.xy, 1), vec4(reflectDir.xyz, reflectivity * dot(abs(normalize(ogDir)), abs(normalize(reflectDir)))));
    fragColor = vec4(0);
}