package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;

/** Behaviour is supplied by the camera mixin. */
final class NoHurtCamModule extends ClientModule {
    NoHurtCamModule() {
        super("NoHurtCam", "Disable the camera shake caused by damage.", "Render");
    }

    @Override
    public void tick(Minecraft mc) {}
}
