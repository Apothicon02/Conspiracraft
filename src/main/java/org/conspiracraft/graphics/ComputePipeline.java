package org.conspiracraft.graphics;

import org.lwjgl.util.shaderc.Shaderc;

import java.io.IOException;

public class ComputePipeline {
    public String name;
    public long comp;
    public long vkPipeline = -1;
    public ComputePipeline(String name) {
        this.name = name;
    }
    public void compile() throws IOException {
        this.comp = ShaderHelper.createShaderModule(ShaderHelper.compileGLSLString(name, Shaderc.shaderc_glsl_compute_shader));
    }
}
