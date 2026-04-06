package org.conspiracraft.renderer;

import org.conspiracraft.Settings;
import org.conspiracraft.renderer.assets.Texture;
import org.conspiracraft.renderer.assets.Textures;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL46.*;

public class Framebuffer {
    public int id;
    public boolean hasDepth;
    public int[] attachments;
    public Map<Integer, Texture> textures;
    public Framebuffer() {
        this.id = 0;
        this.hasDepth = true;
        this.attachments = null;
    }
    public Framebuffer(int colorAttachmentAmount, boolean hasDepth) {
        if (colorAttachmentAmount <= 0 || colorAttachmentAmount > 5) {throw new RuntimeException("Invalid attachment amount: " + colorAttachmentAmount);}
        this.id = glGenFramebuffers();
        this.hasDepth = hasDepth;
        glBindFramebuffer(GL_FRAMEBUFFER, id);
        attachments = new int[colorAttachmentAmount];
        attachments[0] = GL_COLOR_ATTACHMENT0;
        if (colorAttachmentAmount > 1) {
            attachments[1] = GL_COLOR_ATTACHMENT1;
        } if (colorAttachmentAmount > 2) {
            attachments[2] = GL_COLOR_ATTACHMENT2;
        } if (colorAttachmentAmount > 3) {
            attachments[3] = GL_COLOR_ATTACHMENT3;
        } if (colorAttachmentAmount > 4) {
            attachments[4] = GL_COLOR_ATTACHMENT4;
        }
        glDrawBuffers(attachments);
        textures = new HashMap<>();
        for (int attachment : attachments) {
            Texture tex = Textures.create(Settings.width, Settings.height);
            textures.put(attachment, tex);
            glBindTexture(GL_TEXTURE_2D, tex.id);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, tex.width, tex.height, 0, GL_RGBA, GL_FLOAT, 0);
            glFramebufferTexture2D(GL_FRAMEBUFFER, attachment, GL_TEXTURE_2D, tex.id, 0);
        }
        if (hasDepth) {
            Texture tex = Textures.create(Settings.width, Settings.height);
            textures.put(GL_DEPTH_ATTACHMENT, tex);
            glBindTexture(GL_TEXTURE_2D, tex.id);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32F, tex.width, tex.height, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, tex.id, 0);
        }
    }
    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, id);
        if (attachments != null) {
            glDrawBuffers(attachments);
        }
        glClearColor(0, 0, 0, 0);
        glClearDepthf(0.f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }
    public void copy(int sourceAttachment, Texture destTexture) { //example: displayFB.copy(GL_COLOR_ATTACHMENT0, rasterFB.textures.get(GL_COLOR_ATTACHMENT0));
        glBindTexture(GL_TEXTURE_2D, destTexture.id);
        glReadBuffer(sourceAttachment);
        glCopyTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, 0, 0, Settings.width, Settings.height, 0);
    }
}
