package dev.drimoz.immaterialdrawers.block.tile.energy;

import com.buuz135.functionalstorage.client.model.FramedDrawerModelData;
import com.hrznstudio.titanium.annotation.Save;
import com.hrznstudio.titanium.block.BasicTileBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * An energy drawer wearing whatever blocks the player framed it with.
 *
 * <p>Adapted from Functional Storage 1.20.1's {@code block/tile/FramedDrawerTile}.
 * Copyright (c) 2021 Buuz135, Rid - MIT. See NOTICE.
 *
 * <p><b>More of the framing lives on our side than on 1.21.1.</b> There, a {@code FramedTile}
 * interface lets their block and recipe code handle any framed tile. Functional Storage 1.20.1 has
 * no such interface: its framing tests its own classes. What carries over is the model data -
 * their {@code framedblock} model loader reads {@link FramedDrawerModelData#FRAMED_PROPERTY} off
 * whatever {@code ModelData} the tile hands it, whoever owns the tile.
 */
public class FramedEnergyDrawerTile extends EnergyDrawerTile {

    @Save
    private FramedDrawerModelData framedDrawerModelData;

    public FramedEnergyDrawerTile(BasicTileBlock<EnergyDrawerTile> base,
                                  BlockEntityType<EnergyDrawerTile> entityType,
                                  BlockPos pos, BlockState state) {
        super(base, entityType, pos, state);
        // Never null: an empty design renders as the bare frame.
        this.framedDrawerModelData = new FramedDrawerModelData(new HashMap<>());
    }

    public FramedDrawerModelData getFramedDrawerModelData() {
        return framedDrawerModelData;
    }

    public void setFramedDrawerModelData(FramedDrawerModelData framedDrawerModelData) {
        this.framedDrawerModelData = framedDrawerModelData;
        markForUpdate();
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
        }
    }

    @NotNull
    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(FramedDrawerModelData.FRAMED_PROPERTY, framedDrawerModelData).build();
    }
}
