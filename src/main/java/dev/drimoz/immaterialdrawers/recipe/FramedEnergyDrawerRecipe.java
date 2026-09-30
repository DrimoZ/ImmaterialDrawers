package dev.drimoz.immaterialdrawers.recipe;

import com.buuz135.functionalstorage.block.FramedDrawerBlock;
import dev.drimoz.immaterialdrawers.block.energy.FramedEnergyDrawerBlock;
import dev.drimoz.immaterialdrawers.registry.IDContent;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Frames one of our framed drawers in a crafting grid: side block, front block, drawer, optional
 * divider - the same grid as Functional Storage's framing.
 *
 * <p>Adapted from Functional Storage 1.20.1's {@code recipe/FramedDrawerRecipe}.
 * Copyright (c) 2021 Buuz135, Rid - MIT. See NOTICE.
 *
 * <p>Ours because theirs cannot take our blocks on 1.20.1: its {@code matches} tests
 * {@code instanceof FramedDrawerBlock} and their other framed classes, not an interface (on 1.21.1
 * it tests the {@code FramedBlock} marker, and needs nothing from us). The stack it builds is theirs:
 * {@link FramedDrawerBlock#fill} writes the {@code Style} tag their loader and ours both read.
 */
public class FramedEnergyDrawerRecipe extends CustomRecipe {

    public FramedEnergyDrawerRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    public static boolean matches(ItemStack side, ItemStack front, ItemStack drawer) {
        return side.getItem() instanceof BlockItem
                && front.getItem() instanceof BlockItem
                && drawer.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof FramedEnergyDrawerBlock;
    }

    @Override
    public boolean matches(CraftingContainer grid, Level level) {
        return matches(grid.getItem(0), grid.getItem(1), grid.getItem(2));
    }

    @Override
    public ItemStack assemble(CraftingContainer grid, RegistryAccess registryAccess) {
        if (!matches(grid, null)) {
            return ItemStack.EMPTY;
        }
        return FramedDrawerBlock.fill(grid.getItem(0), grid.getItem(1), grid.getItem(2).copy(), grid.getItem(3));
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return IDContent.FRAMED_RECIPE.get();
    }
}
