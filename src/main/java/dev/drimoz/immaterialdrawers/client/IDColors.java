package dev.drimoz.immaterialdrawers.client;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.FramedDrawerBlock;
import com.buuz135.functionalstorage.client.model.FramedDrawerModelData;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.block.tile.energy.FramedEnergyDrawerTile;
import dev.drimoz.immaterialdrawers.registry.IDContent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

/**
 * Tints the framed drawer with the colour of whatever it was framed with - leaves, grass, vines.
 *
 * <p>On 1.21.1 we reuse Functional Storage's {@code FramedColors}, which tests an interface. Their
 * 1.20.1 one tests their own tile classes, so it would answer white for ours. This is the same loop,
 * over our tile: the first block of the design that has a tint gives the colour, skipping
 * Functional Storage's own blocks as theirs does.
 */
@Mod.EventBusSubscriber(modid = ImmaterialDrawers.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class IDColors {

    private static final int NO_TINT = 0xFFFFFF;

    private IDColors() {
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (level == null || pos == null || tintIndex != 0
                    || !(level.getBlockEntity(pos) instanceof FramedEnergyDrawerTile tile)) {
                return NO_TINT;
            }
            FramedDrawerModelData design = tile.getFramedDrawerModelData();
            if (design == null) {
                return NO_TINT;
            }
            for (Map.Entry<String, Item> entry : design.getDesign().entrySet()) {
                if (entry.getValue() instanceof BlockItem blockItem
                        && !ForgeRegistries.ITEMS.getKey(blockItem).getNamespace().equals(FunctionalStorage.MOD_ID)) {
                    int color = Minecraft.getInstance().getBlockColors()
                            .getColor(blockItem.getBlock().defaultBlockState(), level, pos, tintIndex);
                    if (color != -1) {
                        return color;
                    }
                }
            }
            return NO_TINT;
        }, IDContent.FRAMED_ENERGY_DRAWER.getLeft().get());
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            if (tintIndex != 0) {
                return NO_TINT;
            }
            FramedDrawerModelData design = FramedDrawerBlock.getDrawerModelData(stack);
            if (design == null) {
                return NO_TINT;
            }
            for (Item item : design.getDesign().values()) {
                if (item instanceof BlockItem blockItem) {
                    int color = event.getItemColors().getColor(new ItemStack(blockItem), tintIndex);
                    if (color != -1) {
                        return color;
                    }
                }
            }
            return NO_TINT;
        }, IDContent.FRAMED_ENERGY_DRAWER.getLeft().get());
    }
}
