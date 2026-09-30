package dev.drimoz.immaterialdrawers.storage.chemical;

import mekanism.api.chemical.ChemicalType;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.api.chemical.infuse.IInfusionHandler;
import mekanism.api.chemical.pigment.IPigmentHandler;
import mekanism.api.chemical.slurry.ISlurryHandler;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/**
 * Mekanism 10.4's four chemical capabilities. <b>Mekanism only.</b>
 *
 * <p>Asked of Forge by type rather than read from {@code mekanism.common.capabilities.Capabilities}
 * (its implementation, not its API): Forge keys capabilities by interface, so this is the same
 * instance Mekanism registered.
 */
public final class ChemicalCapabilities {

    public static final Capability<IGasHandler> GAS = CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<IInfusionHandler> INFUSION = CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<IPigmentHandler> PIGMENT = CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<ISlurryHandler> SLURRY = CapabilityManager.get(new CapabilityToken<>() {});

    private ChemicalCapabilities() {
    }

    public static Capability<? extends IChemicalHandler<?, ?>> of(ChemicalType type) {
        return switch (type) {
            case GAS -> GAS;
            case INFUSION -> INFUSION;
            case PIGMENT -> PIGMENT;
            case SLURRY -> SLURRY;
        };
    }

    /** Which chemical type a capability is for, or null if it is none of the four. */
    public static ChemicalType typeOf(Capability<?> capability) {
        for (ChemicalType type : ChemicalType.values()) {
            if (of(type) == capability) {
                return type;
            }
        }
        return null;
    }
}
