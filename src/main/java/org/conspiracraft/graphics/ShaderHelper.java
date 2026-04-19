package org.conspiracraft.graphics;

import org.conspiracraft.utils.Utils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK13.*;

public class ShaderHelper {
    public static long createShaderModule(ByteBuffer buffer) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                    .pCode(buffer);
            LongBuffer pShaderModule = stack.mallocLong(1);

            int err = vkCreateShaderModule(Device.vkDevice, createInfo, null, pShaderModule);
            if (err != VK_SUCCESS) {throw new RuntimeException("Failed to create shader module: " + err);}
            return pShaderModule.get(0);
        }
    }

    public static ByteBuffer compileGLSLString(String path, int stage) {
        StringBuilder glsl = new StringBuilder("#version 450 \n");
        glsl.append(Utils.readFile("assets/base/shader/" + path));
        long compiler = Shaderc.shaderc_compiler_initialize();
        if (compiler == 0) {System.out.print("Failed to create shaderc compiler");}
        long result = Shaderc.shaderc_compile_into_spv(compiler, glsl, stage, path, "main", 0);
        if (result == 0) {System.out.print(path + " compile returned null.");}
        long status = Shaderc.shaderc_result_get_compilation_status(result);
        if (status != Shaderc.shaderc_compilation_status_success) {System.out.print("Shader compile error:\n" + Shaderc.shaderc_result_get_error_message(result));}

        ByteBuffer spirv = Shaderc.shaderc_result_get_bytes(result);
        ByteBuffer copy = MemoryUtil.memAlloc(spirv.remaining());
        copy.put(spirv.duplicate().rewind());
        copy.flip();
        Shaderc.shaderc_result_release(result);
        Shaderc.shaderc_compiler_release(compiler);
        return copy;
    }
}
