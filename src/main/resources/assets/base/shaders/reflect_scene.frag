layout(binding = 6, rgba32f) uniform image3D scene_unscaled_image;
in vec4 gl_FragCoord;

out vec4 fragColor;

void main() {
    vec2 pos = gl_FragCoord.xy;
    checkerOn = checker(ivec2(pos));
    pos += (checkerOn ? ivec2(res.x/2, 0) : ivec2(0));
    fragColor = vec4(imageLoad(scene_unscaled_image, ivec3(pos.xy, 0)));
    vec4 ogFragColor = fragColor;
    vec4 reflectData = vec4(imageLoad(scene_unscaled_image, ivec3(pos, 1)));
    reflectivity = reflectData.w;
    if (reflectivity > 0.f) {
        vec3 reflectDir = reflectData.xyz;
        vec2 uv = (pos * 2. - res.xy) / res.y;
        uvDir = normalize(vec3(uv, 1));
        vec3 ogDir = vec3(cam * vec4(uvDir, 0));
        prevReflectPos = camPos + (ogDir * (fragColor.a * renderDistance));
        float oldReflectivity = reflectivity;
        reflectivity = 0.f;
        vec4 reflectColor = raytrace(prevReflectPos, reflectDir, reflectionShadows && oldReflectivity > 0.25f, renderDistance);
        if (reflectivity > 0.f) {
            reflectDir = reflect(reflectDir, normalize(reflectPos - prevReflectPos));
            reflectColor = mix(reflectColor, raytrace(prevReflectPos, reflectDir, reflectionShadows && reflectivity > 0.25f, renderDistance), reflectivity);
        }
        fragColor = mix(fragColor, reflectColor, oldReflectivity);
    }
    imageStore(scene_unscaled_image, ivec3(pos.xy, 0), vec4(fragColor.rgb, ogFragColor.a));
    fragColor = vec4(0);
}