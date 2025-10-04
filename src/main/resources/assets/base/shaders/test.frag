layout(binding = 4, rgba32f) uniform image3D scene_unscaled_image;

in vec2 gl_FragCoord;
out vec4 fragColor;

void main() {
    checkerOn = checker(ivec2(gl_FragCoord.xy));
    bool firstHalf = bool(gl_FragCoord.x <= res.x/2);
    if (!firstHalf) {
        checkerOn = !checkerOn;
    }
    vec2 pos = gl_FragCoord.xy + (checkerOn ? ivec2(res.x/2, 0) : ivec2(0));
    vec2 uv =  (2.*pos-res.xy)/res.y;
    uvDir = normalize(vec3(uv, 1));
    vec3 ogDir = vec3(cam*vec4(uvDir, 0));
    vec3 color = vec3(0);

    vec3 rayMapPos = floor(camPos);
    vec3 stepDir = vec3(0);
    vec3 sideDist = vec3(9e9);
    vec3 deltaDist = 1./abs(ogDir);
    float side = 0.;
    vec3 S = step(0., ogDir);
    stepDir = 2.*S-1.;
    sideDist = (S-stepDir * fract(camPos)) * deltaDist;
    for(int i = 0; i < renderDistance; i++) {
        vec4 conds = step(sideDist.xxyy, sideDist.yzzx);

        vec3 cases = vec3(0);
        cases.x = conds.x * conds.y;
        cases.y = (1.-cases.x) * conds.z * conds.w;
        cases.z = (1.-cases.x) * (1.-cases.y);

        sideDist += max((2.*cases-1.) * deltaDist, 0.);

        rayMapPos += cases * stepDir;

        if (getBlock(rayMapPos).x > 0.) {
            lighting = fromLinear(getLighting(rayMapPos.x, rayMapPos.y, rayMapPos.z));
            side = cases.y + 2. * cases.z;
            vec3 n = vec3(side==0., side==1., side==2.);
            vec3 p = rayMapPos + .5 - stepDir*.5;

            float t = (dot(n, p - camPos)) / dot(n, ogDir);
            vec3 hit = camPos + ogDir * t;
            vec3 uvw = hit - rayMapPos;

            hitPos = rayMapPos;

            if (side == 0.f) {
                color = fromLinear(vec3(0.7));
            } else if(side == 1.f) {
                color = fromLinear(vec3(0.5));
            } else {
                color = fromLinear(vec3(0.3));
            }
            break;
        }
    }
    
    float adjustedTime = clamp((abs(1-clamp((distance(hitPos, sun-vec3(0, sun.y, 0))/1.5f)/size, 0, 1))*1.2)-abs(0.25f-min(0.25f, distance(rayMapPos, vec3(0))/size)), 0.05f, 1.f);
    float adjustedTimeCam = clamp((abs(1-clamp((distance(camPos, sun-vec3(0, sun.y, 0))/1.5f)/size, 0, 1))*1.2)-abs(0.25f-min(0.25f, distance(rayMapPos, vec3(0))/size)), 0.05f, 0.9f);
    float timeBonus = gradient(hitPos.y, 64.f, 372.f, 0.1f, 0.f);
    float mixedTime = max(0.2f, (adjustedTime/2)+(adjustedTimeCam/2)+timeBonus);
    lighting = pow(lighting/20, vec4(2.f))*vec4(200, 200, 200, 35);
    float sunLight = (lighting.a/20)*(mixedTime-timeBonus);
    vec3 finalLight = (vec3(lighting*((0.7-min(0.7, (lighting.a/20)*mixedTime))/4))+((lighting.a/20)*max(0.23f, sunLight)));
    color = color*(finalLight/2);//light
    
    color = toLinear(color);
    imageStore(scene_unscaled_image, ivec3(pos.xy, 0), vec4(color.rgb, 0));
}