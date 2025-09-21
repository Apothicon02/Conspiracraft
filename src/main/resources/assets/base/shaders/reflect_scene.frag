layout(binding = 6, rgba32f) uniform image3D scene_unscaled_image;
in vec4 gl_FragCoord;

out vec4 fragColor;

void main() {
    vec2 pos = gl_FragCoord.xy;
    checkerOn = checker(ivec2(pos));
    pos += (checkerOn ? ivec2(res.x/2, 0) : ivec2(0));
    fragColor = vec4(imageLoad(scene_unscaled_image, ivec3(pos.xy, 0)));
    vec4 reflectData = vec4(imageLoad(scene_unscaled_image, ivec3(pos, 1)));
    reflectivity = reflectData.w;
    float ogReflectivity = reflectivity;
    vec4 reflectColor = vec4(0, 0, 0, ogReflectivity);
    if (reflectivity > 0.f) {
        vec3 reflectDir = reflectData.xyz;
        vec2 uv = (pos * 2. - res.xy) / res.y;
        uvDir = normalize(vec3(uv, 1));
        vec3 ogDir = vec3(cam * vec4(uvDir, 0));
        prevReflectPos = camPos + (ogDir * (fragColor.a * renderDistance));
        reflectivity = 0.f;
        reflectColor.rgb = raytrace(prevReflectPos, reflectDir, reflectionShadows && ogReflectivity > 0.25f, renderDistance).rgb;
        if (reflectivity > 0.f) {
            reflectDir = reflect(reflectDir, normalize(reflectPos - prevReflectPos));
            reflectColor.rgb = mix(reflectColor.rgb, raytrace(prevReflectPos, reflectDir, reflectionShadows && reflectivity > 0.25f, renderDistance).rgb, reflectivity);
        }
        //fragColor = mix(fragColor, reflectColor, ogReflectivity);
    }
    //imageStore(scene_unscaled_image, ivec3(pos.xy, 0), fragColor);
    imageStore(scene_unscaled_image, ivec3(pos.xy, 2), vec4(reflectColor.rgb, ogReflectivity));
}