package com.darkcart.xdolf;

import net.minecraftforge.fml.common.Mod;

/** Client-only entry point. Forge skips this mod on dedicated servers. */
@Mod(Xdolf.ID)
public final class Xdolf {
    public static final String ID = "xdolf";

    public Xdolf() {
        ClientRuntime.register();
    }
}
