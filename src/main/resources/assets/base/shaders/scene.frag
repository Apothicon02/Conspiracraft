#version 460

uniform vec2 res;
uniform bool ui;
in vec4 gl_FragCoord;

out vec4 fragColor;

layout(binding = 0) uniform sampler2D scene_image;

void main()
{
    vec2 uv = (vec2(gl_FragCoord)*2. - res) / res.y;
    if (ui && uv.x >= -0.004f && uv.x <= 0.004f && uv.y >= -0.004385f && uv.y <= 0.004385f) {
        fragColor = vec4(0.9f, 0.9f, 1, 1);
    } else {
        fragColor = texture(scene_image, vec2(gl_FragCoord.xy/res));
    }
}