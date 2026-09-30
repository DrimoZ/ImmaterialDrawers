package dev.drimoz.immaterialdrawers.block.tile.source;

import com.buuz135.functionalstorage.client.model.FramedDrawerModelData;
import com.hrznstudio.titanium.annotation.Save;
import com.hrznstudio.titanium.block.BasicTileBlock;
import dev.drimoz.immaterialdrawers.block.tile.IDFramedTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * A Source Drawer wearing whatever blocks the player framed it with. <b>Ars Nouveau only.</b>
 * Same design handling as {@code FramedEnergyDrawerTile}. Adapted from Functional Storage 1.20.1's
 * {@code FramedDrawerTile}. Copyright (c) 2021 Buuz135, Rid - MIT. See NOTICE.
 */
public class FramedSourceDrawerTile extends SourceDrawerTile implements IDFramedTile {

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

    @NotNull
    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(FramedDrawerModelData.FRAMED_PROPERTY, framedDrawerModelData).build();
    }
}
