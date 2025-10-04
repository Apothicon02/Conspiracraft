uniform vec2 dir;
uniform ivec2 res;

layout(binding = 4, rgba32f) uniform image3D scene_unscaled_image;

in vec4 gl_FragCoord;

const int SAMPLE_COUNT = 33;

const float OFFSETS[33] = float[33](
-31.492310800313383,
-29.49279890952202,
-27.493287034480588,
-25.493775176962394,
-23.49426333888111,
-21.494751522030196,
-19.495239727686936,
-17.495727956033413,
-15.496216205331075,
-13.496704470766609,
-11.497192742860985,
-9.49768100530086,
-7.498169232008111,
-5.498657383208034,
-3.499145400183396,
-1.499633198307682,
0.49987724953141943,
2.499389333137032,
4.498901413060153,
6.498413320200259,
8.49792512523179,
10.497436876648257,
12.496948606810747,
14.4964603364925,
16.4959720782759,
18.495483839075906,
20.49499562199976,
22.494507427704175,
24.49401925537336,
26.493531103412273,
28.49304296992732,
30.49255485304933,
32
);

const float WEIGHTS[33] = float[33](
0.02225373363001186,
0.023619221161248555,
0.024970798479528662,
0.026296832788844086,
0.02758535622332793,
0.02882423924740564,
0.03000137621113065,
0.03110487928420197,
0.03212327660106226,
0.0330457101760925,
0.03386212900694727,
0.03456347278561197,
0.03514184178549173,
0.03559064878769939,
0.03590474934395084,
0.03608054723443864,
0.036115882529016624,
0.036010098075613936,
0.03576479947631989,
0.03538279724301312,
0.03486847323660866,
0.03422770808950289,
0.033467762992046415,
0.032597138167405365,
0.031625411471758104,
0.030563061056320517,
0.029421276393422846,
0.02821176219117745,
0.02694653979614904,
0.025637750612811594,
0.02429746585980491,
0.02293750664868255,
0.010955753413352
);

void main() {
    vec2 pos = gl_FragCoord.xy;
    checkerOn = checker(ivec2(pos));
    pos += (checkerOn ? ivec2(res.x/2, 0) : ivec2(0));
    float reflectivity = imageLoad(scene_unscaled_image, ivec3(pos, 1)).w;
    bool firstPass = dir.x > 0;
    if (reflectivity > 0) {
        vec4 result = vec4(0.0);
        float nulls = 0;
        for (int i = 0; i < SAMPLE_COUNT; ++i) {
            vec2 offset = dir * OFFSETS[i];
            float weight = WEIGHTS[i];
            vec4 newResult = imageLoad(scene_unscaled_image, ivec3(min(res-1, max(vec2(0), pos + offset)), firstPass ? 2 : 3));
            if (newResult.r+newResult.g+newResult.b > 0) {
                result += newResult * (weight+nulls);
                nulls = 0;
            } else {
                newResult = imageLoad(scene_unscaled_image, ivec3(min(res-1, max(vec2(0), pos + offset))+vec2(0, 1), firstPass ? 2 : 3));
                if (newResult.r+newResult.g+newResult.b > 0) {
                    result += newResult * (weight+nulls);
                    nulls = 0;
                } else {
                    newResult = imageLoad(scene_unscaled_image, ivec3(min(res-1, max(vec2(0), pos + offset))+vec2(1, 0), firstPass ? 2 : 3));
                    if (newResult.r+newResult.g+newResult.b > 0) {
                        result += newResult * (weight+nulls);
                        nulls = 0;
                    } else {
                        nulls+=weight;
                    }
                }
            }
        }
        result += nulls;

        if (firstPass) {
            imageStore(scene_unscaled_image, ivec3(pos, 3), result);
        } else {
            vec4 ogColor = imageLoad(scene_unscaled_image, ivec3(pos, 0));
            vec4 ogReflectionColor = imageLoad(scene_unscaled_image, ivec3(pos, 2));
            float roughness = reflectivity; //replace with actual roughness.
            imageStore(scene_unscaled_image, ivec3(pos, 0), vec4(mix(ogColor.rgb, mix(result.rgb, ogReflectionColor.rgb, roughness), reflectivity), 1));
        }
    }
}