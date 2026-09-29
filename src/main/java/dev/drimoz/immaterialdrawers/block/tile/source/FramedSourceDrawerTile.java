package dev.drimoz.immaterialdrawers.block.tile.source;

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
 * A source drawer wearing whatever the player framed it with.
 *
 * <p><b>Ars Nouveau only - see {@code compat.Mods}.</b> Adapted from Functional Storage's
 * {@code FramedFluidDrawerTile}, Copyright (c) 2021 Buuz135, Rid — MIT, see NOTICE; the framing itself
 * is all theirs, keyed on {@link FramedTile} - see {@code FramedEnergyDrawerTile}.
 */
public class FramedSourceDrawerTile extends SourceDrawerTile implements FramedTile {

    @Save
    private FramedDrawerModelData framedDrawerModelData;

    public FramedSourceDrawerTile(BasicTileBlock<SourceDrawerTile> base, BlockEntityType<SourceDrawerTile> entityType,
                                  BlockPos pos, BlockState state) {
        super(base, entityType, pos, state);
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
