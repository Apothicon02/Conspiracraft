uniform layout(binding = 0) sampler2D source;

layout (location = 0) out vec4 finalColor;

void main() {
    finalColor = texelFetch(source, ivec2(gl_FragCoord.xy), 0);
}