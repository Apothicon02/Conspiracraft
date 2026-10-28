package org.conspiracraft.graphics;

import org.lwjgl.util.shaderc.Shaderc;

import java.io.IOException;

import static org.conspiracraft.graphics.Pipelines.pipelines;

public class Pipeline {
    public String vertName;
    public String fragName;
    public long vert;
    public long frag;
    public int colorAttachments;
    public long vkPipeline = -1;
    public boolean depthEnabled = true;
    public boolean cullEnabled = true;
    public Pipeline(String vert, String frag, int colorAttachments, boolean depthEnabled, boolean cullEnabled) {
        vertName = vert;
        fragName = frag;
        this.colorAttachments = colorAttachments;
        this.depthEnabled = depthEnabled;
        this.cullEnabled = cullEnabled;
        pipelines.add(this);
    }
    public Pipeline(String vert, String frag, int colorAttachments, boolean depthEnabled) {
        this(vert, frag, colorAttachments, depthEnabled, true);
    }
    public void compile() throws IOException {
        this.vert = ShaderHelper.createShaderModule(ShaderHelper.compileGLSLString(vertName, Shaderc.shaderc_glsl_vertex_shader));
        this.frag = ShaderHelper.createShaderModule(ShaderHelper.compileGLSLString(fragName, Shaderc.shaderc_glsl_fragment_shader));
    }
}
