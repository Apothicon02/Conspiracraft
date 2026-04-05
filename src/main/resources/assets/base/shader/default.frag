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

vec2 getUV(vec3 p, vec3 n) {
    vec2 uv;
    if (n.x > n.y && n.x > n.z) {uv = p.yz;}
    else if (n.y > n.z) { uv = p.xz;}
    else {uv = p.xy;}
    return uv;
}

void main() {
    if (inColor.r > 1 || inColor.g > 1 || inColor.b > 1) {
        outColor = inColor;
    } else {
        vec3 normal = -normalize(cross(dFdx(pos), dFdy(pos)));
        //vec2 uv = getUV(fract(pos), abs(normal));
        vec4 albedo = inColor;//*texture(uv);

        vec4 skylight = ubo.skylight;
        vec3 lighting = vec3((dot(normal, normalize(skylight.xyz))*0.38f)+(0.68f*skylight.a));
        outColor.rgb = mix(albedo.rgb*lighting, vec3(1), clamp(((length(worldPos.xyz-vec3(768, 0, 768))/768))-0.75f, 0, 1)*4);

        outColor = pow(outColor, vec4(2.2)); //gamma
        if (ubo.hdr == 1) {
            outColor = (outColor*400)/80;//exposure
        }
    }
}