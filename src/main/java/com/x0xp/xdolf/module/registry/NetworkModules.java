package com.x0xp.xdolf;

/** Temporary command lookup bridge; module implementations live in their category folders. */
final class NetworkModules {
    private NetworkModules() {}

    static SpammerModule spammer() {
        return (SpammerModule) ClientRuntime.find("Spammer");
    }
}
