uniform ivec2 res;

out vec4 fragColor;

void main() {
    vec2 pos = gl_FragCoord.xy;
    fragColor = vec4(1, 0, 1, 1);
}