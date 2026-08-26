package dev.drimoz.immaterialdrawers.augment;

import com.buuz135.functionalstorage.block.tile.ControllableDrawerTile;
import com.buuz135.functionalstorage.item.component.FunctionalUpgradeBehavior;
import com.mojang.serialization.MapCodec;
import dev.drimoz.immaterialdrawers.IDConfig;
import dev.drimoz.immaterialdrawers.block.tile.energy.EnergyDrawerTile;
import dev.drimoz.immaterialdrawers.storage.BigEnergyStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.List;

/**
 * The Wireless Charger: a wall of drawers that keeps the tools in your pockets full.
 *
 * <p>This is the first augment, and the reason the mod is not an energy cell in drawer clothing.
 * Functional Storage's {@code FunctionalUpgradeBehavior} is its one officially supported extension
 * point (CLAUDE.md §9) — a synchronised registry dispatched by codec, attached to an item through a
 * data component. No mixin, no {@code instanceof} to dodge: the drawer's own {@code serverTick}
 * calls {@link #work} for whatever sits in its utility slots.
 *
 * <p><b>It only does anything in an energy drawer.</b> The upgrade slots into any drawer, because
 * the utility slots belong to Functional Storage and accept any utility upgrade. Put it in a drawer
 * full of cobblestone and nothing happens, which is the honest behaviour for an augment that needs
 * energy to move.
 *
 * <p><b>The clamp.</b> Every offer goes through {@link BigEnergyStorage#availableToGive()}, and it
 * is recomputed for each item charged. {@code receiveEnergy} is committed when called, so offering
 * more than the drawer holds hands out energy that never existed — the defect the review caught in
 * the push loop. One drawer feeding a dozen tools is exactly the shape that hides it.
 */
public record ChargeNearbyBehavior() implements FunctionalUpgradeBehavior {

    public static final ChargeNearbyBehavior INSTANCE = new ChargeNearbyBehavior();

    /** No state to serialise: the numbers are config, not per-item. */
    public static final MapCodec<ChargeNearbyBehavior> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public void work(Level level, BlockPos pos, ControllableDrawerTile<?> drawer,
                     ItemStack upgradeStack, int upgradeSlot) {
        if (!(drawer instanceof EnergyDrawerTile energyDrawer)) {
            return;
        }
        // work() runs every tick and behaviours gate themselves - Functional Storage's own
        // ExecuteEveryBehavior is a decorator that does nothing but this. Offset by position so a
        // wall of chargers does not all sweep for players on the same tick.
        if (Math.floorMod(level.getGameTime() + pos.asLong(), IDConfig.WIRELESS_CHARGER_INTERVAL_TICKS) != 0) {
            return;
        }

        BigEnergyStorage storage = energyDrawer.getEnergyStorage();
        if (storage.availableToGive() <= 0) {
            return;
        }

        int budget = IDConfig.WIRELESS_CHARGER_FE_PER_OPERATION;
        AABB range = new AABB(pos).inflate(IDConfig.WIRELESS_CHARGER_RANGE);

        for (Player player : level.getEntitiesOfClass(Player.class, range)) {
            budget = charge(player, storage, budget);
            if (budget <= 0) {
                return;
            }
        }
    }

    /**
     * Fills whatever this player is carrying that will take a charge, and returns what is left of
     * the budget.
     *
     * <p>Main inventory, armour and offhand. A charger that skips powered armour is not doing the
     * job people install it for.
     */
    private static int charge(Player player, BigEnergyStorage storage, int budget) {
        for (List<ItemStack> compartment : List.of(player.getInventory().items,
                player.getInventory().armor, player.getInventory().offhand)) {
            for (ItemStack stack : compartment) {
                if (budget <= 0) {
                    return 0;
                }
                budget = charge(stack, storage, budget);
            }
        }
        return budget;
    }

    private static int charge(ItemStack stack, BigEnergyStorage storage, int budget) {
        if (stack.isEmpty()) {
            return budget;
        }
        IEnergyStorage item = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (item == null || !item.canReceive()) {
            return budget;
        }

        // Recomputed per item: the drawer shrinks as it charges, and an offer larger than what is
        // left is how energy gets invented.
        int offer = (int) Math.min(budget, storage.availableToGive());
        if (offer <= 0) {
            return 0;
        }

        int accepted = item.receiveEnergy(offer, false);
        if (accepted > 0) {
            storage.extractEnergy(accepted, false);
            return budget - accepted;
        }
        return budget;
    }

    @Override
    public List<Component> getTooltip() {
        return List.of(
                Component.translatable("augment.immaterialdrawers.wireless_charger.desc",
                        IDConfig.WIRELESS_CHARGER_RANGE).withStyle(ChatFormatting.GRAY),
                Component.translatable("augment.immaterialdrawers.wireless_charger.energy_only")
                        .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public MapCodec<? extends FunctionalUpgradeBehavior> codec() {
        return CODEC;
    }
}
