package dev.drimoz.immaterialdrawers.block.source;

import com.buuz135.functionalstorage.block.FramedBlock;
import dev.drimoz.immaterialdrawers.block.tile.source.FramedSourceDrawerTile;
import dev.drimoz.immaterialdrawers.block.tile.source.SourceDrawerTile;
import dev.drimoz.immaterialdrawers.registry.IDSourceContent;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The framed source drawer. <b>Ars Nouveau only - see {@code compat.Mods}.</b> Modelled on Functional
 * Storage's {@code FramedFluidDrawerBlock}, Copyright (c) 2021 Buuz135, Rid — MIT, see NOTICE; framable
 * by their recipe through the {@link FramedBlock} marker - see {@code FramedEnergyDrawerBlock}.
 */
public class FramedSourceDrawerBlock extends SourceDrawerBlock implements FramedBlock {

    public FramedSourceDrawerBlock(Properties properties) {
        super(IDSourceContent.FRAMED_SOURCE_DRAWER_NAME, properties);
    }

    @Override
    protected Ingredient shell() {
        return Ingredient.of(Items.IRON_NUGGET);
    }

    /** Its own block entity type, so its own capability registration - see {@code FramedEnergyDrawerBlock}. */
    @SuppressWarnings("unchecked")
    @Override
    public BlockEntityType.BlockEntitySupplier<SourceDrawerTile> getTileEntityFactory() {
        return (pos, state) -> new FramedSourceDrawerTile(this,
                (BlockEntityType<SourceDrawerTile>) IDSourceContent.FRAMED_SOURCE_DRAWER.type().get(), pos, state);
    }
}
