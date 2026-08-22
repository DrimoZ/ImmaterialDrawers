package dev.drimoz.immaterialdrawers.client;

import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.registry.IDComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Adds the energy line to Functional Storage's storage upgrades.
 *
 * <p>Their {@code ClientSetup} builds an upgrade's tooltip by reading the size components it knows
 * about — item storage, fluid storage, controller range — and writes one line per component it
 * finds. It cannot know about a fourth, so an upgrade sitting in an energy drawer's slot described
 * what it does to items and fluids and said nothing about the drawer it was actually in.
 *
 * <p>The sentence is theirs and the noun is ours: {@code SizeProvider.getTooltip} produces
 * "Multiplies the block <thing> by 8" from their translation keys, and we hand it our own
 * {@code storageupgrade.obj.energy_storage}. Writing our own phrasing would put two differently
 * worded lines in the same tooltip.
 */
@EventBusSubscriber(modid = ImmaterialDrawers.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class IDTooltips {

    private IDTooltips() {
    }

    @SubscribeEvent
    public static void addEnergyStorageLine(ItemTooltipEvent event) {
        var energy = event.getItemStack().get(IDComponents.ENERGY_STORAGE_MODIFIER.get());
        if (energy != null) {
            event.getToolTip().add(energy
                    .getTooltip(Component.translatable("storageupgrade.obj.immaterialdrawers.energy_storage"))
                    .copy().withStyle(ChatFormatting.GRAY));
        }
    }
}
