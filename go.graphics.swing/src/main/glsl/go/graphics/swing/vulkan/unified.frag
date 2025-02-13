#version 450
#extension GL_ARB_separate_shader_objects : enable


layout (location=0) in vec2 frag_texcoord;


layout(set=1, binding=0) uniform sampler2D texHandle;

layout(set=0, binding=1) uniform UnifiedData {
    float shadow_depth;
} unified;

layout(push_constant) uniform UnifiedPerCall {
    int globalTransIndex;
    int padding;
    vec2 localRot;
    vec4 localTrans;
    vec4 color;
    float intensity;
    int mode;
} local;


layout (location=0) out vec4 out_color;

void main() {
    float fragDepth = gl_FragCoord.z;
    vec4 fragColor = local.color;

    bool textured = local.mode!=0;

    if(textured) {
        bool progress_fence = local.mode > 3;

        vec4 tex_color;
        if(progress_fence) {
            tex_color = texture(texHandle, fragColor.rg+(fragColor.ba-fragColor.rg)*frag_texcoord);
        } else {
            tex_color = texture(texHandle, frag_texcoord);
        }

        bool image_fence = local.mode>0;
        bool torso_fence = local.mode>1 && !progress_fence;
        bool shadow_fence = abs(float(local.mode))>2.0 && !progress_fence;

        if(torso_fence && tex_color.a < 0.1 && tex_color.r > 0.1) { // torso pixel
            fragColor.rgb *= tex_color.b;
        } else if(shadow_fence && tex_color.a < 0.1 && tex_color.g > 0.1) { // shadow pixel
            fragColor.rgba = tex_color.aaag;
            fragDepth += unified.shadow_depth;
        } else if(image_fence) { // image pixel
            if(!torso_fence && !shadow_fence && !progress_fence) {
                fragColor *= tex_color;
            } else {
                fragColor = tex_color;
            }
        }
    }

    if(fragColor.a < 0.5) discard;

    fragColor.rgb *= local.intensity;

    out_color = fragColor;
    gl_FragDepth = fragDepth;
}
