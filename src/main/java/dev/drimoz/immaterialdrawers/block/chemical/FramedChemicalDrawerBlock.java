package dev.drimoz.immaterialdrawers.block.chemical;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.FramedBlock;
import dev.drimoz.immaterialdrawers.block.tile.chemical.ChemicalDrawerTile;
import dev.drimoz.immaterialdrawers.block.tile.chemical.FramedChemicalDrawerTile;
import dev.drimoz.immaterialdrawers.registry.IDChemicalContent;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The framed chemical drawer, in each of the three layouts.
 *
 * <p><b>Mekanism only - see {@code compat.Mods}.</b>
 *
 * <p>Modelled on Functional Storage's {@code block/FramedFluidDrawerBlock}.
 * Copyright (c) 2021 Buuz135, Rid — MIT. See NOTICE. {@code FramedBlock} is an empty marker, and
 * every framed-specific path in Functional Storage tests for it - so, like the framed energy drawer,
 * this is framable by their own recipe with nothing on our side. See {@code FramedEnergyDrawerBlock}.
 */
public class FramedChemicalDrawerBlock extends ChemicalDrawerBlock implements FramedBlock {

    public FramedChemicalDrawerBlock(FunctionalStorage.DrawerType type, Properties properties) {
        super(IDChemicalContent.nameFor(type, true), type, properties);
    }

    /** Iron nuggets, as on every framed drawer of theirs: the frame is the cheap shell you dress. */
    @Override
    protected Ingredient shell() {
        return Ingredient.of(Items.IRON_NUGGET);
    }

    /**
     * Its own block entity type, and therefore its own capability registration - the trap
     * {@code FramedEnergyDrawerBlock#getTileEntityFactory} describes.
     */
    @SuppressWarnings("unchecked")
    @Override
    public BlockEntityType.BlockEntitySupplier<ChemicalDrawerTile> getTileEntityFactory() {
        return (pos, state) -> new FramedChemicalDrawerTile(this,
                (BlockEntityType<ChemicalDrawerTile>) IDChemicalContent.drawer(type, true).type().get(),
                pos, state, type);
    }
}
