package org.conspiracraft.graphics;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.shaderc.Shaderc;

import java.io.IOException;

import static org.conspiracraft.graphics.Pipelines.computePipelines;

public class ComputePipeline {
    public String name;
    public long comp;
    public long vkPipeline = -1;
    public ComputePipeline(String name) {
        this.name = name;
        computePipelines.add(this);
    }
    public void compile(MemoryStack stack) throws IOException {
        this.comp = ShaderHelper.createShaderModule(ShaderHelper.compileGLSLString(stack, name, Shaderc.shaderc_glsl_compute_shader));
    }
}
