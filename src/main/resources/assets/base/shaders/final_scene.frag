uniform ivec2 res;

layout(binding = 6, rgba32f) uniform image3D scene_unscaled_image;
in vec4 gl_FragCoord;

out vec4 fragColor;

void main() {
    ivec2 pos = ivec2(gl_FragCoord.xy);
    bool checkerOn = checker(pos);
    bool firstHalf = bool(pos.x < res.x/2);
    if (!((firstHalf && !checkerOn) || (!firstHalf && checkerOn))) {
        pos.y++;
    }
    fragColor = toLinear(vec4(imageLoad(scene_unscaled_image, ivec3(pos.xy, 0))));
}