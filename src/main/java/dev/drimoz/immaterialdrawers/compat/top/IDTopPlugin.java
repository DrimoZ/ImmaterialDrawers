package dev.drimoz.immaterialdrawers.compat.top;

import com.hrznstudio.titanium.annotation.plugin.FeaturePlugin;
import com.hrznstudio.titanium.event.handler.EventManager;
import com.hrznstudio.titanium.plugin.FeaturePluginInstance;
import com.hrznstudio.titanium.plugin.PluginPhase;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;

/**
 * Hands our probe provider to The One Probe, if it is installed.
 *
 * <p>Same shape as Functional Storage's {@code compat/top/TOPPlugin}.
 * Copyright (c) 2021 Buuz135, Rid — MIT. See NOTICE.
 *
 * <p>Titanium's {@code @FeaturePlugin} is what makes the {@code compileOnly} dependency safe: the
 * plugin manager only instantiates this class when {@code theoneprobe} is actually loaded, so
 * nothing here is ever classloaded in a pack without it. TOP itself is reached through
 * {@code InterModComms} rather than a direct call, which is its own published way in.
 */
@FeaturePlugin(value = "theoneprobe", type = FeaturePlugin.FeaturePluginType.MOD)
public class IDTopPlugin implements FeaturePluginInstance {

    @Override
    public void execute(PluginPhase phase) {
        if (phase == PluginPhase.CONSTRUCTION) {
            EventManager.mod(InterModEnqueueEvent.class)
                    .process(event -> InterModComms.sendTo(
                            "theoneprobe", "getTheOneProbe", () -> EnergyProbeProvider.REGISTER))
                    .subscribe();
        }
    }
}
