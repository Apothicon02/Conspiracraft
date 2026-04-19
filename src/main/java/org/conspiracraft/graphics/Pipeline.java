package org.conspiracraft.graphics;

import org.lwjgl.util.shaderc.Shaderc;

public class Pipeline {
    public String vertName;
    public String fragName;
    public long vert;
    public long frag;
    public int colorAttachments;
    public long vkPipeline = -1;
    public Pipeline(String vert, String frag, int colorAttachments) {
        vertName = vert;
        fragName = frag;
        this.colorAttachments = colorAttachments;
    }
    public void compile() {
        this.vert = ShaderHelper.createShaderModule(ShaderHelper.compileGLSLString(vertName, Shaderc.shaderc_glsl_vertex_shader));
        this.frag = ShaderHelper.createShaderModule(ShaderHelper.compileGLSLString(fragName, Shaderc.shaderc_glsl_fragment_shader));
    }
}
