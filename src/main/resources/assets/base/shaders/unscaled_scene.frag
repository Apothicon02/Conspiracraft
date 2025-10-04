layout(binding = 4, rgba32f) uniform image3D scene_unscaled_image;

in vec4 gl_FragCoord;

out vec4 fragColor;

void main() {
    //uint64_t startTime = clockARB();

    checkerOn = checker(ivec2(gl_FragCoord.xy));
    vec2 pos = gl_FragCoord.xy + (checkerOn ? ivec2(res.x/2, 0) : ivec2(0));
    vec2 uv = (pos*2. - res.xy) / res.y;
    uvDir = normalize(vec3(uv, 1));
    mat4 modifiedCam = (cam * mat4(vec4(uvDir, 0), vec4(uvDir, 0), vec4(uvDir, 0), vec4(uvDir, 0)));
    vec3 ogDir = vec3(modifiedCam[0][0], modifiedCam[0][1], modifiedCam[0][2]);
    //    vec3 ogDir = vec3(cam * vec4(uvDir, 0));
    if (ui && uv.x >= -0.004f && uv.x <= 0.004f && uv.y >= -0.004385f && uv.y <= 0.004385f) {
        fragColor = vec4(0.9, 0.9, 1, 1);
    } else {
        fragColor = raytrace(camPos, ogDir, true, renderDistance);
    }
    //fragColor = vec4(float(clockARB() - startTime) * 0.0000005);
    //fragColor = vec4(noise(pos)+0.5f);
    imageStore(scene_unscaled_image, ivec3(pos.xy, 0), vec4(fragColor.rgb, depth));
    vec3 reflectDir = reflect(ogDir, normalize(reflectPos - prevReflectPos));
    imageStore(scene_unscaled_image, ivec3(pos.xy, 1), vec4(reflectDir.xyz, reflectivity * dot(abs(normalize(ogDir)), abs(normalize(reflectDir)))));
    fragColor = vec4(0);
}