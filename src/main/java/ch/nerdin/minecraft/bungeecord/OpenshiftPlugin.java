package ch.nerdin.minecraft.bungeecord;

import net.md_5.bungee.api.plugin.Plugin;

public class OpenshiftPlugin extends Plugin {

    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerCommand(this, new CreatePodCommand());
    }
}
