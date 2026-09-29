package dev.drimoz.immaterialdrawers.block.tile.chemical;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.tile.FramedTile;
import com.buuz135.functionalstorage.client.model.FramedDrawerModelData;
import com.hrznstudio.titanium.annotation.Save;
import com.hrznstudio.titanium.block.BasicTileBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

import javax.annotation.Nonnull;
import java.util.HashMap;

/**
 * A chemical drawer wearing whatever blocks the player framed it with.
 *
 * <p><b>Mekanism only - see {@code compat.Mods}.</b>
 *
 * <p>Adapted from Functional Storage's {@code block/tile/FramedFluidDrawerTile}.
 * Copyright (c) 2021 Buuz135, Rid — MIT. See NOTICE. As with the framed energy drawer, almost none of
 * the framing lives here: {@code Drawer} and {@code FramedDrawerRecipe} branch on {@link FramedTile}
 * and {@code FramedBlock}, never on a concrete class, so implementing the two interfaces buys the
 * whole feature. See {@code FramedEnergyDrawerTile}.
 */
public class FramedChemicalDrawerTile extends ChemicalDrawerTile implements FramedTile {

    @Save
    private FramedDrawerModelData framedDrawerModelData;

    public FramedChemicalDrawerTile(BasicTileBlock<ChemicalDrawerTile> base,
                                    BlockEntityType<ChemicalDrawerTile> entityType,
                                    BlockPos pos, BlockState state, FunctionalStorage.DrawerType type) {
        super(base, entityType, pos, state, type);
        // Never null, so the FramedTile branches in Drawer that null-check it are the safety net
        // rather than the normal path. An empty design renders as the bare frame.
        this.framedDrawerModelData = new FramedDrawerModelData(new HashMap<>());
    }

    @Override
    public FramedDrawerModelData getFramedDrawerModelData() {
        return framedDrawerModelData;
    }

    @Override
    public void setFramedDrawerModelData(FramedDrawerModelData framedDrawerModelData) {
        this.framedDrawerModelData = framedDrawerModelData;
        markForUpdate();
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
        }
    }

    @Nonnull
    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(FramedDrawerModelData.FRAMED_PROPERTY, framedDrawerModelData).build();
    }
}
