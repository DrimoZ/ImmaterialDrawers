package dev.drimoz.immaterialdrawers.block.tile.chemical;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.tile.DrawerProperties;
import com.buuz135.functionalstorage.item.FSAttachments;
import com.buuz135.functionalstorage.item.component.SizeProvider;
import com.hrznstudio.titanium.annotation.Save;
import com.hrznstudio.titanium.block.BasicTileBlock;
import dev.drimoz.immaterialdrawers.IDConfig;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.block.tile.ImmaterialDrawerTile;
import dev.drimoz.immaterialdrawers.client.gui.ChemicalDrawerInfoGuiAddon;
import dev.drimoz.immaterialdrawers.storage.chemical.BigChemicalHandler;
import dev.drimoz.immaterialdrawers.storage.chemical.ChemicalCapabilities;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Supplier;

/**
 * A drawer that holds Mekanism chemicals - gases, infuse types, pigments, slurries - one tank per
 * slot, in the 1x1, 1x2 and 2x2 layouts Functional Storage's fluid drawer comes in.
 *
 * <p><b>Mekanism only - see {@code compat.Mods}.</b>
 *
 * <p>Modelled on Functional Storage's {@code block/tile/FluidDrawerTile}.
 * Copyright (c) 2021 Buuz135, Rid — MIT. See NOTICE. The one structural difference is the parent:
 * theirs extends {@code ControllableDrawerTile}, and its controller recognises it by name. Ours
 * cannot be recognised by name, so it is an item drawer with no item slots, like the energy drawer -
 * {@link ImmaterialDrawerTile} explains why and holds the parts that make it work.
 *
 * <p><b>Scaled exactly like a fluid drawer.</b> Same base size - the drawer type's slot amount, 32,
 * 16 or 8 units - and Functional Storage's own {@code fluid_storage_modifier}, so the same upgrades
 * fit and multiply the same way. Chemicals are measured in mB like fluids, and there is no int
 * ceiling to calibrate against here (see {@link BigChemicalHandler}), so a component of our own would
 * have bought nothing but a second curve to keep in step.
 *
 * <p><b>No push.</b> Unlike the energy drawer, and like the fluid drawer: Mekanism's pressurized
 * tubes pull from a block when their side is set to, so the ecosystem already does what the energy
 * drawer had to do for itself.
 */
public class ChemicalDrawerTile extends ImmaterialDrawerTile<ChemicalDrawerTile> {

    @Save
    public BigChemicalHandler chemicalHandler;

    private final FunctionalStorage.DrawerType type;

    public ChemicalDrawerTile(BasicTileBlock<ChemicalDrawerTile> base, BlockEntityType<ChemicalDrawerTile> entityType,
                              BlockPos pos, BlockState state, FunctionalStorage.DrawerType type) {
        super(base, entityType, pos, state, new DrawerProperties(type.getSlotAmount(), FSAttachments.FLUID_STORAGE_MODIFIER));
        this.type = type;
        this.chemicalHandler = new BigChemicalHandler(type.getSlots(), capacityFor(getStorageMultiplier())) {
            @Override
            public void onChange() {
                syncObject(chemicalHandler);
                updateComparators();
            }

            @Override
            public boolean isDrawerLocked() {
                return isLocked();
            }

            @Override
            public boolean isDrawerVoid() {
                return isVoid();
            }

            @Override
            public boolean isDrawerCreative() {
                return isCreative();
            }
        };
    }

    /**
     * mB per tank for a storage multiplier - {@code FluidDrawerTile.getTankCapacity}, in a long and
     * with the mB per unit in the config. Saturates instead of wrapping for the Max Storage upgrade,
     * whose multiplier is {@link Integer#MAX_VALUE}.
     */
    public static long capacityFor(double storageMultiplier) {
        double capacity = Math.floor(storageMultiplier * IDConfig.CHEMICAL_MB_PER_UNIT);
        return capacity >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) capacity;
    }

    /** Their fluid drawer's screen, with our fronts and chemicals in the windows. */
    @OnlyIn(Dist.CLIENT)
    @Override
    public void initClient() {
        super.initClient();
        String suffix = type.getSlots() == 1 ? "" : "_" + type.getSlots();
        addGuiAddonFactory(() -> new ChemicalDrawerInfoGuiAddon(64, 16,
                ResourceLocation.fromNamespaceAndPath(ImmaterialDrawers.MOD_ID,
                        "textures/block/chemical_drawer_front" + suffix + ".png"),
                type, this::getChemicalHandler));
    }

    /**
     * Right-click a slot with a chemical container - a Mekanism chemical tank, most often - to empty
     * it into that slot. {@code FluidDrawerTile.onSlotActivated}, with Mekanism's item capability in
     * place of the fluid one.
     *
     * <p>A locked slot pinned to nothing takes the container's chemical as its assignment first, as
     * the fluid drawer does with a bucket; that is the only way to give an empty locked drawer a
     * purpose.
     *
     * <p>The client runs the same code as a simulation, so it knows to answer {@code SUCCESS} - and
     * not go on to use the item - without changing anything it would then disagree with the server
     * about.
     */
    @Override
    public InteractionResult onSlotActivated(Player player, InteractionHand hand, Direction facing,
                                             double hitX, double hitY, double hitZ, int slot) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(FunctionalStorage.CONFIGURATION_TOOL.get()) || stack.is(FunctionalStorage.LINKING_TOOL.get())) {
            return InteractionResult.PASS;
        }
        if (slot >= 0 && slot < chemicalHandler.getChemicalTanks() && !stack.isEmpty()) {
            IChemicalHandler item = stack.getCapability(ChemicalCapabilities.ITEM);
            if (item != null) {
                Action action = isServer() ? Action.EXECUTE : Action.SIMULATE;
                if (isLocked() && chemicalHandler.getFilter(slot).isEmpty() && chemicalHandler.getStoredRaw(slot) == 0
                        && action.execute()) {
                    ChemicalStack offered = firstChemicalIn(item);
                    if (!offered.isEmpty()) {
                        chemicalHandler.setFilter(slot, offered);
                        markForUpdate();
                    }
                }
                if (moveFromItem(item, slot, action) > 0) {
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return super.onSlotActivated(player, hand, facing, hitX, hitY, hitZ, slot);
    }

    /**
     * Left-click a slot with a chemical container to fill it from that slot -
     * {@code FluidDrawerTile.onClicked}. Replaces the item drawer's own left-click entirely, which
     * would try to hand the player an item out of a handler that has none.
     */
    @Override
    public void onClicked(Player player, int slot) {
        if (!isServer() || slot < 0 || slot >= chemicalHandler.getChemicalTanks()) {
            return;
        }
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        IChemicalHandler item = stack.isEmpty() ? null : stack.getCapability(ChemicalCapabilities.ITEM);
        if (item != null) {
            moveIntoItem(item, slot);
        }
    }

    /**
     * How many rate-limited rounds one click may take. Mekanism's tank items are throttled per
     * operation - a basic tank moves 1,000 mB at a time, which is right for a tube and wrong for a
     * player, who clicked once and means "all of it". 64,000 mB at 1,000 a round is 64; this leaves
     * room for any tier and still bounds a handler that never says no.
     */
    private static final int MAX_ROUNDS = 1024;

    /**
     * Offered, then taken: only what the drawer actually accepted leaves the item. Repeated until
     * nothing moves, past the item's per-operation rate limit - see {@link #MAX_ROUNDS}. A simulation
     * stops after one round, since nothing it does changes the next.
     */
    private long moveFromItem(IChemicalHandler item, int slot, Action action) {
        long moved = 0;
        for (int round = 0; round < MAX_ROUNDS; round++) {
            long thisRound = 0;
            for (int tank = 0; tank < item.getChemicalTanks(); tank++) {
                ChemicalStack inItem = item.getChemicalInTank(tank);
                if (inItem.isEmpty()) {
                    continue;
                }
                ChemicalStack remainder = chemicalHandler.insertChemical(slot, inItem, Action.SIMULATE);
                long fits = inItem.getAmount() - remainder.getAmount();
                if (fits <= 0) {
                    continue;
                }
                ChemicalStack taken = item.extractChemical(tank, fits, action);
                chemicalHandler.insertChemical(slot, taken, action);
                thisRound += taken.getAmount();
            }
            moved += thisRound;
            if (thisRound == 0 || action.simulate()) {
                break;
            }
        }
        return moved;
    }

    /** The reverse: only what the item accepted leaves the drawer, in as many rounds as it takes. */
    private void moveIntoItem(IChemicalHandler item, int slot) {
        for (int round = 0; round < MAX_ROUNDS; round++) {
            ChemicalStack available = chemicalHandler.extractChemical(slot, Long.MAX_VALUE, Action.SIMULATE);
            if (available.isEmpty()) {
                return;
            }
            long fits = available.getAmount() - item.insertChemical(available, Action.SIMULATE).getAmount();
            if (fits <= 0) {
                return;
            }
            item.insertChemical(chemicalHandler.extractChemical(slot, fits, Action.EXECUTE), Action.EXECUTE);
        }
    }

    private static ChemicalStack firstChemicalIn(IChemicalHandler item) {
        for (int tank = 0; tank < item.getChemicalTanks(); tank++) {
            if (!item.getChemicalInTank(tank).isEmpty()) {
                return item.getChemicalInTank(tank);
            }
        }
        return ChemicalStack.EMPTY;
    }

    @Override
    public ChemicalDrawerTile getSelf() {
        return this;
    }

    public FunctionalStorage.DrawerType getDrawerType() {
        return type;
    }

    public BigChemicalHandler getChemicalHandler() {
        return chemicalHandler;
    }

    /** Functional Storage's fluid component - see the class comment for why not one of ours. */
    @Override
    protected Supplier<DataComponentType<SizeProvider>> storageModifier() {
        return FSAttachments.FLUID_STORAGE_MODIFIER;
    }

    @Override
    protected void onStorageMultiplierChanged() {
        this.chemicalHandler.setCapacity(capacityFor(getStorageMultiplier()));
        syncObject(this.chemicalHandler);
    }

    @Override
    protected boolean canChangeMultiplier(double newSizeMultiplier) {
        long newCapacity = capacityFor(newSizeMultiplier);
        for (int tank = 0; tank < chemicalHandler.getChemicalTanks(); tank++) {
            if (chemicalHandler.getStoredRaw(tank) > newCapacity) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean hasContents() {
        for (int tank = 0; tank < chemicalHandler.getChemicalTanks(); tank++) {
            if (chemicalHandler.getStoredRaw(tank) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Pins every tank to the chemical it holds, as {@code FluidDrawerTile.setLocked} does. Unlike
     * the energy drawer this is real work: chemicals come in hundreds of kinds, and a locked drawer
     * keeps its assignment when emptied.
     */
    @Override
    public void setLocked(boolean locked) {
        super.setLocked(locked);
        this.chemicalHandler.lockHandler();
        syncObject(this.chemicalHandler);
    }

    /**
     * A locked drawer with an assignment is not empty, even with nothing in it: breaking it must keep
     * the assignment, as it does for a fluid drawer.
     */
    @Override
    public boolean isEverythingEmpty() {
        if (isLocked()) {
            for (int tank = 0; tank < chemicalHandler.getChemicalTanks(); tank++) {
                if (!chemicalHandler.getFilter(tank).isEmpty()) {
                    return false;
                }
            }
        }
        return super.isEverythingEmpty();
    }
}
