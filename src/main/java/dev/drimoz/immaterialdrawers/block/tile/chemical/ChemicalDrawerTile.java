package dev.drimoz.immaterialdrawers.block.tile.chemical;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.config.FunctionalStorageConfig;
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
import mekanism.api.chemical.ChemicalType;
import mekanism.api.chemical.IChemicalHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * A drawer for Mekanism's chemicals, 1, 2 or 4 tanks. <b>Mekanism only</b> - see {@code compat.Mods}.
 *
 * <p>Calqued on Functional Storage's fluid drawer: same layouts, same base size per tank, same fluid
 * divisor for the storage upgrades. On 1.20.1 one tank holds any of Mekanism 10.4's four types; the
 * drawer answers the four capabilities, each with its typed view (see {@code ChemicalTanks}).
 */
public class ChemicalDrawerTile extends ImmaterialDrawerTile<ChemicalDrawerTile> {

    /** Bounded: Mekanism's item tanks move a slice per operation; a click keeps going until nothing moves. */
    private static final int MAX_ROUNDS = 1024;

    @Save
    public BigChemicalHandler chemicalHandler;

    private final FunctionalStorage.DrawerType type;
    private final Map<ChemicalType, LazyOptional<IChemicalHandler<?, ?>>> optionals = new EnumMap<>(ChemicalType.class);

    public ChemicalDrawerTile(BasicTileBlock<ChemicalDrawerTile> base, BlockEntityType<ChemicalDrawerTile> entityType,
                              BlockPos pos, BlockState state, FunctionalStorage.DrawerType type) {
        super(base, entityType, pos, state);
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
        createOptionals();
    }

    private void createOptionals() {
        for (ChemicalType chemicalType : ChemicalType.values()) {
            optionals.put(chemicalType, LazyOptional.of(() -> chemicalHandler.view(chemicalType)));
        }
    }

    /** Per tank: 1.20.1's {@code getSlotAmount()} counts items (stacks of 64); the fluid drawer divides by 64 too. */
    private long capacityFor(int storageMultiplier) {
        return (long) (type.getSlotAmount() / 64) * storageMultiplier * IDConfig.CHEMICAL_MB_PER_UNIT;
    }

    @Override
    public double getStorageDiv() {
        return FunctionalStorageConfig.FLUID_DIVISOR;
    }

    @NotNull
    @Override
    public <U> LazyOptional<U> getCapability(@NotNull Capability<U> cap, @Nullable Direction side) {
        ChemicalType chemicalType = ChemicalCapabilities.typeOf(cap);
        if (chemicalType != null) {
            return optionals.get(chemicalType).cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        optionals.values().forEach(LazyOptional::invalidate);
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        createOptionals();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void initClient() {
        super.initClient();
        String suffix = type.getSlots() == 1 ? "" : "_" + type.getSlots();
        addGuiAddonFactory(() -> new ChemicalDrawerInfoGuiAddon(64, 16,
                new ResourceLocation(ImmaterialDrawers.MOD_ID, "textures/block/chemical_drawer_front" + suffix + ".png"),
                type, this::getChemicalHandler));
    }

    /** A chemical tank item in hand fills the slot it is used on, whichever of the four types it holds. */
    @Override
    public InteractionResult onSlotActivated(Player player, InteractionHand hand, Direction facing,
                                             double hitX, double hitY, double hitZ, int slot) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(FunctionalStorage.CONFIGURATION_TOOL.get()) || stack.is(FunctionalStorage.LINKING_TOOL.get())) {
            return InteractionResult.PASS;
        }
        if (slot >= 0 && slot < chemicalHandler.tanks() && !stack.isEmpty()) {
            Action action = isServer() ? Action.EXECUTE : Action.SIMULATE;
            for (ChemicalType chemicalType : ChemicalType.values()) {
                IChemicalHandler<?, ?> item = stack.getCapability(ChemicalCapabilities.of(chemicalType)).orElse(null);
                if (item == null) {
                    continue;
                }
                if (isLocked() && chemicalHandler.getFilter(slot).isEmpty()
                        && chemicalHandler.getStoredRaw(slot).isEmpty() && action.execute()) {
                    ChemicalStack<?> offered = firstChemicalIn(item);
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

    /** A left click with a tank item empties the slot into it. */
    @Override
    public void onClicked(Player player, int slot) {
        if (!isServer() || slot < 0 || slot >= chemicalHandler.tanks()) {
            return;
        }
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        ChemicalStack<?> held = chemicalHandler.getStoredRaw(slot);
        if (stack.isEmpty() || held.isEmpty()) {
            return;
        }
        IChemicalHandler<?, ?> item = stack.getCapability(ChemicalCapabilities.of(ChemicalType.getTypeFor(held))).orElse(null);
        if (item != null) {
            moveIntoItem(item, slot);
        }
    }

    private long moveFromItem(IChemicalHandler<?, ?> item, int slot, Action action) {
        long moved = 0;
        for (int round = 0; round < MAX_ROUNDS; round++) {
            long thisRound = 0;
            for (int tank = 0; tank < item.getTanks(); tank++) {
                ChemicalStack<?> inItem = item.getChemicalInTank(tank);
                if (inItem.isEmpty()) {
                    continue;
                }
                long fits = inItem.getAmount() - chemicalHandler.insert(slot, inItem, Action.SIMULATE).getAmount();
                if (fits <= 0) {
                    continue;
                }
                ChemicalStack<?> taken = item.extractChemical(tank, fits, action);
                chemicalHandler.insert(slot, taken, action);
                thisRound += taken.getAmount();
            }
            moved += thisRound;
            if (thisRound == 0 || action.simulate()) {
                break;
            }
        }
        return moved;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void moveIntoItem(IChemicalHandler item, int slot) {
        for (int round = 0; round < MAX_ROUNDS; round++) {
            ChemicalStack<?> available = chemicalHandler.extract(slot, Long.MAX_VALUE, Action.SIMULATE);
            if (available.isEmpty()) {
                return;
            }
            long fits = available.getAmount() - ((ChemicalStack<?>) item.insertChemical(available, Action.SIMULATE)).getAmount();
            if (fits <= 0) {
                return;
            }
            item.insertChemical(chemicalHandler.extract(slot, fits, Action.EXECUTE), Action.EXECUTE);
        }
    }

    private static ChemicalStack<?> firstChemicalIn(IChemicalHandler<?, ?> item) {
        for (int tank = 0; tank < item.getTanks(); tank++) {
            if (!item.getChemicalInTank(tank).isEmpty()) {
                return item.getChemicalInTank(tank);
            }
        }
        return item.getEmptyStack();
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

    @Override
    protected void onStorageMultiplierChanged() {
        this.chemicalHandler.setCapacity(capacityFor(getStorageMultiplier()));
        syncObject(this.chemicalHandler);
    }

    @Override
    protected boolean canChangeMultiplier(int newStorageMultiplier) {
        long newCapacity = capacityFor(newStorageMultiplier);
        for (int tank = 0; tank < chemicalHandler.tanks(); tank++) {
            if (chemicalHandler.getStoredRaw(tank).getAmount() > newCapacity) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean hasContents() {
        for (int tank = 0; tank < chemicalHandler.tanks(); tank++) {
            if (!chemicalHandler.getStoredRaw(tank).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** A locked drawer keeps each tank on what it holds now - Functional Storage's fluid drawer does the same. */
    @Override
    public void setLocked(boolean locked) {
        super.setLocked(locked);
        this.chemicalHandler.lockHandler();
        syncObject(this.chemicalHandler);
    }

    /** A locked, emptied drawer still has its assignments to keep, so it drops with its NBT. */
    @Override
    public boolean isEverythingEmpty() {
        if (isLocked()) {
            for (int tank = 0; tank < chemicalHandler.tanks(); tank++) {
                if (!chemicalHandler.getFilter(tank).isEmpty()) {
                    return false;
                }
            }
        }
        return super.isEverythingEmpty();
    }
}
