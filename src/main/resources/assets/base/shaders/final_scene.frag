uniform ivec2 res;

layout(binding = 6, rgba32f) uniform image2D scene_unscaled_image;
in vec4 gl_FragCoord;

out vec4 fragColor;

void main() {
    ivec2 pos = ivec2(gl_FragCoord.xy);
    bool checkerOn = checker(pos);
    bool firstHalf = bool(pos.x < res.x/2);
    if ((firstHalf && !checkerOn) || (!firstHalf && checkerOn)) {
        fragColor = vec4(imageLoad(scene_unscaled_image, pos));
    } else {
        fragColor = vec4(imageLoad(scene_unscaled_image, pos+ivec2(0, 1)));
    }
}