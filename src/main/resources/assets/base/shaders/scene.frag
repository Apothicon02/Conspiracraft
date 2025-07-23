layout(binding = 1, rgba32f) uniform writeonly image2D scene_lighting;

in vec4 gl_FragCoord;

out vec4 fragColor;

bool hitBright = false;

void main() {
    fragColor = vec4(0);
}