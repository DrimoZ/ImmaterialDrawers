package dev.drimoz.immaterialdrawers.block.tile.chemical;

import com.buuz135.functionalstorage.FunctionalStorage;
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
 * A chemical drawer wearing whatever blocks the player framed it with. <b>Mekanism only.</b>
 * Adapted from Functional Storage 1.20.1's {@code FramedDrawerTile}. Copyright (c) 2021 Buuz135, Rid
 * - MIT. See NOTICE.
 */
public class FramedChemicalDrawerTile extends ChemicalDrawerTile implements IDFramedTile {

    @Save
    private FramedDrawerModelData framedDrawerModelData;

    public FramedChemicalDrawerTile(BasicTileBlock<ChemicalDrawerTile> base, BlockEntityType<ChemicalDrawerTile> entityType,
                                    BlockPos pos, BlockState state, FunctionalStorage.DrawerType type) {
        super(base, entityType, pos, state, type);
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
