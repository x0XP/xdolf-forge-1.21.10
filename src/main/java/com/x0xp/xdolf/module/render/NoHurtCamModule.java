package com.x0xp.xdolf.module.render;

import com.x0xp.xdolf.module.ClientModule;

import com.x0xp.xdolf.*;
import com.x0xp.xdolf.module.support.*;

import net.minecraft.client.Minecraft;

/** Behaviour is supplied by the camera mixin. */
public final class NoHurtCamModule extends ClientModule {
    public NoHurtCamModule() {
        super("NoHurtCam", "Disable the camera shake caused by damage.", "Render");
    }

    @Override
    public void tick(Minecraft mc) {}
}
