layout(set = 0, binding = 0) readonly uniform UniformBufferObject {
    mat4 view;
    mat4 proj;
    vec4 skylight;
    int hdr;
} ubo;

layout(location = 0) in vec3 pos;
layout(location = 1) in vec4 inColor;
layout(location = 2) in vec4 worldPos;

layout(location = 0) out vec4 outColor;

void main() {
    if (inColor.r > 1 || inColor.g > 1 || inColor.b > 1) {
        outColor = inColor;
    } else {
        vec3 normal = -normalize(cross(dFdx(pos), dFdy(pos)));
        vec4 skylight = ubo.skylight;
        vec3 lighting = vec3((dot(normal, normalize(skylight.xyz))*0.38f)+(0.68f*skylight.a));
        outColor.rgb = mix(inColor.rgb*lighting, vec3(1), clamp(sqrt(sqrt(length(worldPos.xyz-inverse(ubo.view)[3].xyz)/1000))-0.2f, 0, 1));
        outColor = vec4(gl_FragCoord.x/2560, 0, gl_FragCoord.y/1440, 1.f);
        outColor = pow(outColor, vec4(2.2)); //gamma
        if (ubo.hdr == 1) {
            outColor = (outColor*400)/80;//exposure
        }
    }
}