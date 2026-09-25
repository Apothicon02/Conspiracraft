package org.conspiracraft.items;

import org.conspiracraft.Main;
import org.conspiracraft.audio.AudioController;
import org.conspiracraft.audio.SFX;
import org.conspiracraft.audio.Sounds;
import org.conspiracraft.audio.Source;
import org.conspiracraft.entities.EntityTypes;
import org.conspiracraft.graphics.Renderer;
import org.conspiracraft.graphics.textures.Textures;
import org.conspiracraft.items.types.ItemTypes;
import org.conspiracraft.player.HandManager;
import org.conspiracraft.player.InputHandler;
import org.conspiracraft.player.Player;
import org.conspiracraft.world.World;
import org.joml.*;

import java.lang.Math;
import java.nio.IntBuffer;

import static org.conspiracraft.graphics.Renderer.pushUBO;
import static org.conspiracraft.graphics.Renderer.warpOffset;
import static org.conspiracraft.player.HandManager.lmbDown;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_LCTRL;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_SPACE;

public class GrappleItem extends DurableItem implements Cloneable {
    public static final SFX[] lengthSFXs = new SFX[]{Sounds.GEAR_CLICK1, Sounds.GEAR_CLICK2, Sounds.GEAR_CLICK3};
    public Vector2i grappleBlock = new Vector2i(-1);
    public Vector3d grapplePos = new Vector3d(-1);
    public double maxDistance = 0.f;
    public Source lengthSource = null;
    public int lengthSFXI = 0;
    @Override
    public Item load(IntBuffer data) {
        return ((GrappleItem)new GrappleItem().type(ItemTypes.itemTypeMap.get(data.get())).moveTo(new Vector3f(data.get()/1000f, data.get()/1000f, data.get()/1000f)).rot(data.get()/1000f).hover(data.get()/1000f, data.get()>0).amount(data.get()).timeSpawned(data.get())).durability(data.get());
    }

    @Override
    public void drawHeld(Matrix4f handMatrix) {
        if (grapplePos.y() > -1) {
            pushUBO.updateTex(Textures.entities);
            pushUBO.updateSize(new Vector2i(-8, -8));
            pushUBO.updateAtlasOffset(EntityTypes.ROPE.atlasOffset);
            Vector3f startPos = new Vector3f();
            new Matrix4f(handMatrix).getTranslation(startPos);
            Vector3f endPos = new Vector3f((float) (grapplePos.x()-warpOffset.x()), (float) (grapplePos.y()-warpOffset.y()), (float) (grapplePos.z()-warpOffset.z()));
            Renderer.drawLineFixedWidth(startPos, endPos, 0.0625f, new Vector4f(0.85f, 0.85f, 0.475f, 1.f));//0.03125f
        }
    }

    @Override
    public void tick() {
        baseTick();
        if (lmbDown && grapplePos.y() != -1) {
            double prevMaxDistance = maxDistance;
            if (Main.player.inputHandler.isKeyDown(SDL_SCANCODE_SPACE)) {
                maxDistance-=0.15d;
                maxDistance = Math.max(maxDistance, 2.d);
            } else if (Main.player.inputHandler.isKeyDown(SDL_SCANCODE_LCTRL)) {
                maxDistance+=0.15d;
            }
            if (maxDistance != prevMaxDistance) {
                if (lengthSource == null || !lengthSource.isPlaying()) {
                    lengthSource = new Source(new Vector3f(Main.player.pos), 0.2f + (Player.playerRand.nextFloat()*0.05f), 0.8f + (Player.playerRand.nextFloat()*0.1f), 0.f, 0);
                    lengthSource.play(lengthSFXs[lengthSFXI], false);
                    lengthSFXI+=1+Player.playerRand.nextInt(2);
                    if (lengthSFXI >= lengthSFXs.length) {lengthSFXI = 0;}
                    AudioController.disposableSources.add(lengthSource);
                }
            }
            Vector3d dir = new Vector3d(Main.player.pos).sub(grapplePos);
            double dist = dir.length();
            if (dist > maxDistance) {
                dir.normalize();
                double velocity = Main.player.vel.dot(dir);
                if (velocity > 0) {Main.player.vel.sub(new Vector3d(dir).mul(velocity));}
                Main.player.vel.add(new Vector3d(dir).mul(-0.01f*(dist-maxDistance)));
            } else {
                maxDistance = dist;
            }
            if (Math.random() < 0.5f*Math.max(Math.abs(Main.player.vel.x()), Math.max(Math.abs(Main.player.vel.y()), Math.abs(Main.player.vel.z())))) {
                World.spawnParticle(grapplePos, grappleBlock);
            }
        } else {
            grapplePos.set(-1);
        }
    }
}
