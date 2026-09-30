package dev.drimoz.immaterialdrawers.augment;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.item.UpgradeItem;
import dev.drimoz.immaterialdrawers.IDConfig;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.block.tile.energy.EnergyDrawerTile;
import dev.drimoz.immaterialdrawers.storage.BigEnergyStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Wireless Charger: a wall of drawers that keeps the tools in your pockets full.
 *
 * <p><b>On 1.20.1 it is an item, not a behaviour.</b> The 1.21.1 branch registers a
 * {@code FunctionalUpgradeBehavior} and Functional Storage calls it (CLAUDE.md §9). Functional Storage
 * 1.20.1 has no such registry: its upgrades are compared by identity in its own tick. What it does
 * have is a utility slot that accepts any {@code UpgradeItem} of {@code Type.UTILITY}, and the tile that
 * has to act on the charger is ours - so {@link EnergyDrawerTile#serverTick} finds it in the slots
 * and calls {@link #work}. In any other drawer it sits in the slot and does nothing, as on 1.21.1.
 *
 * <p><b>The clamp.</b> Every offer goes through {@link BigEnergyStorage#availableToGive()},
 * recomputed per item charged: {@code receiveEnergy} is committed when called, so offering more than
 * the drawer holds hands out energy that never existed.
 */
public class WirelessChargerItem extends UpgradeItem {

    public WirelessChargerItem() {
        super(new Properties(), Type.UTILITY);
        // UpgradeItem puts itself in Functional Storage's tab; it belongs in ours.
        FunctionalStorage.TAB.getTabList().remove(this);
        setItemGroup(ImmaterialDrawers.TAB);
    }

    /** Called by an energy drawer's tick for each charger in its utility slots. */
    public static void work(Level level, BlockPos pos, EnergyDrawerTile drawer) {
        // Offset by position so a wall of chargers does not all sweep on the same tick.
        if (Math.floorMod(level.getGameTime() + pos.asLong(), IDConfig.WIRELESS_CHARGER_INTERVAL_TICKS) != 0) {
            return;
        }
        BigEnergyStorage storage = drawer.getEnergyStorage();
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

    /** Main inventory, armour and offhand - a charger that skips powered armour is not doing the job. */
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
        IEnergyStorage item = stack.getCapability(ForgeCapabilities.ENERGY).orElse(null);
        if (item == null || !item.canReceive()) {
            return budget;
        }
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

    /** Titanium's tooltip hook - its {@code appendHoverText} is final. After Functional Storage's "Type" line. */
    @Override
    public void addTooltipDetails(@Nullable Key key, ItemStack stack, List<Component> tooltip, boolean advanced) {
        super.addTooltipDetails(key, stack, tooltip, advanced);
        tooltip.add(Component.translatable("augment.immaterialdrawers.wireless_charger.desc",
                IDConfig.WIRELESS_CHARGER_RANGE).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("augment.immaterialdrawers.wireless_charger.energy_only")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
