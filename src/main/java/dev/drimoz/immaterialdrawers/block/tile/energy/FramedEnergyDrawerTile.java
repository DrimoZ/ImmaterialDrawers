package dev.drimoz.immaterialdrawers.block.tile.energy;

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
 * An energy drawer wearing whatever blocks the player framed it with.
 *
 * <p>Adapted from Functional Storage's {@code block/tile/FramedFluidDrawerTile}.
 * Copyright (c) 2021 Buuz135, Rid — MIT. See NOTICE.
 *
 * <p>This is the variant that matters for the point of the mod. An unframed energy drawer is a
 * copper block in a wall of oak ones; a framed one takes the same textures as the drawers around
 * it, which is a better answer than shipping a variant per wood type and hoping one of them
 * matches what the player built with.
 *
 * <p><b>Almost none of the framing lives here.</b> {@code Drawer} already copies the style onto the
 * dropped item, reads it back when the block is placed, adds the "use on a block to frame it"
 * tooltip and answers pick-block with the framed stack — all of it branching on {@link FramedTile}
 * and {@code FramedBlock}, never on a concrete class. {@code FramedDrawerRecipe} is generic in the
 * same way: any {@code BlockItem} whose block implements {@code FramedBlock} can be framed in a
 * crafting grid. Implementing two interfaces buys the entire feature.
 *
 * <p>The one exception is the block colour handler, which Functional Storage registers from a list
 * it builds by scanning <em>its own</em> block registry. We are not in it and cannot be, so the
 * tint has to be registered on our side — that is client work and belongs with the rest of the
 * rendering. See CLAUDE.md §11.
 */
public class FramedEnergyDrawerTile extends EnergyDrawerTile implements FramedTile {

    @Save
    private FramedDrawerModelData framedDrawerModelData;

    public FramedEnergyDrawerTile(BasicTileBlock<EnergyDrawerTile> base,
                                  BlockEntityType<EnergyDrawerTile> entityType,
                                  BlockPos pos, BlockState state) {
        super(base, entityType, pos, state);
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
