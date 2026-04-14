package org.conspiracraft.graphics;

public class Pipeline {
    public String vert;
    public String frag;
    public int colorAttachments;
    public long vkPipeline = -1;
    public Pipeline(String vert, String frag, int colorAttachments) {
        this.vert = vert;
        this.frag = frag;
        this.colorAttachments = colorAttachments;
    }
}
