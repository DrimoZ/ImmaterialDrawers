package dev.drimoz.immaterialdrawers;

import com.hrznstudio.titanium.annotation.config.ConfigFile;
import com.hrznstudio.titanium.annotation.config.ConfigVal;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Every number in this mod a pack author might want to move.
 *
 * <p>Same mechanism as Functional Storage's own {@code FunctionalStorageConfig}: a Titanium
 * {@code @ConfigFile} class, discovered by {@code ModuleController} and injected on config load.
 *
 * <p><b>COMMON on 1.20.1.</b> The 1.21.1 branch needs STARTUP because the values are baked into data
 * components on Functional Storage's upgrades during mod loading. Forge 1.20.1 has no STARTUP type
 * and no components: the divisor is read live, by {@code getStorageDiv()}, whenever a drawer
 * recomputes its multiplier.
 *
 * <p>Deliberately wider than what Functional Storage exposes. Their fluid divisor and base sizes are
 * fixed in code; every equivalent here is editable, including the things that only exist in this
 * mod, because whoever is tuning a pack has better reasons than we do.
 */
@ConfigFile(value = "immaterialdrawers-common", type = ModConfig.Type.COMMON)
public class IDConfig {

    @ConfigVal(comment = "FE held by an unupgraded energy drawer, before the storage upgrades are "
            + "applied. Expressed as ENERGY_BASE_UNITS x FE_PER_UNIT, mirroring how a fluid drawer "
            + "is a slot count times 1000 mB.")
    @ConfigVal.InRangeInt(min = 1)
    public static int ENERGY_BASE_UNITS = 500;

    @ConfigVal(comment = "FE per base unit. Change this to move the whole curve without touching "
            + "the upgrade maths.")
    @ConfigVal.InRangeInt(min = 1)
    public static int FE_PER_UNIT = 1000;

    @ConfigVal(comment = "How much every storage upgrade's multiplier is divided by for energy, the "
            + "same way Functional Storage's FLUID_DIVISOR works for fluids. Higher means a flatter "
            + "curve. With the default of 4 and a Netherite multiplier of 32, four upgrades give "
            + "8^4 = 4096, so a fully upgraded drawer holds about 2.05 billion FE.")
    @ConfigVal.InRangeInt(min = 1)
    public static int ENERGY_DIVISOR = 4;

    @ConfigVal(comment = "Whether an energy drawer hands energy to the blocks around it on its own. "
            + "Turn this off and the drawer only gives energy up to something that asks for it - "
            + "which, unlike items and fluids, almost nothing in the Forge Energy ecosystem does.")
    public static boolean PUSH_TO_NEIGHBOURS = true;

    @ConfigVal(comment = "Throughput, as a divisor of the drawer's own capacity: 200 means it can "
            + "hand out half a percent of what it holds per push. Bigger drawers therefore move "
            + "more, which keeps a fully upgraded one from being a bottleneck and a base one from "
            + "being a free superhighway.")
    @ConfigVal.InRangeInt(min = 1)
    public static int ENERGY_TRANSFER_DIVISOR = 200;

    @ConfigVal(comment = "How far the Wireless Charger reaches, in blocks. Measured from the drawer, "
            + "so a wall of them covers a room without stacking range on top of range.")
    @ConfigVal.InRangeInt(min = 1)
    public static int WIRELESS_CHARGER_RANGE = 8;

    @ConfigVal(comment = "FE the Wireless Charger hands out per sweep, shared across everyone in "
            + "range. It never gives out more than the drawer holds.")
    @ConfigVal.InRangeInt(min = 1)
    public static int WIRELESS_CHARGER_FE_PER_OPERATION = 1000;

    @ConfigVal(comment = "Ticks between Wireless Charger sweeps. Each sweep looks for players in "
            + "range, so lowering this costs an entity query more often.")
    @ConfigVal.InRangeInt(min = 1)
    public static int WIRELESS_CHARGER_INTERVAL_TICKS = 20;

    @ConfigVal(comment = "Ticks between pushes. Four matches the cadence Functional Storage gives "
            + "its upgrades. Lowering it makes drawers more responsive and makes a large wall cost "
            + "proportionally more to tick.")
    @ConfigVal.InRangeInt(min = 1)
    public static int ENERGY_PUSH_INTERVAL_TICKS = 4;

    @ConfigVal(comment = "mB of chemical per unit of a chemical drawer's base size, the same role 1000 "
            + "mB plays for Functional Storage's fluid drawers - which is also the default, so an "
            + "unupgraded 1x1 chemical drawer holds 32,000 mB like a fluid drawer. Storage upgrades scale "
            + "it with the fluid multipliers. Only read when Mekanism is installed.")
    @ConfigVal.InRangeInt(min = 1)
    public static int CHEMICAL_MB_PER_UNIT = 1000;

    @ConfigVal(comment = "Source per unit of the Source Drawer's base size, the role 1000 mB plays for a "
            + "fluid drawer. The drawer is 32 units and scales with the fluid multipliers, so the default "
            + "holds 32,000 Source (3.2 Source Jars) unupgraded and 2,097,152,000 with four Netherite "
            + "upgrades - just under the int ceiling of Ars Nouveau's Source API. Raising this makes the "
            + "top of the curve saturate there. Only read when Ars Nouveau is installed.")
    @ConfigVal.InRangeInt(min = 1)
    public static int SOURCE_PER_UNIT = 1000;

    // ---- Switching content off -------------------------------------------------------------------
    //
    // "Off" means unobtainable: no recipe, and gone from the creative tab. The blocks stay
    // registered, and the ones already placed keep working. Unregistering them would delete every
    // placed drawer and its contents from existing worlds, and would make a server and a client with
    // different configs refuse to connect - registries must match on both sides. See CLAUDE.md §17.

    @ConfigVal(comment = "Whether the Energy Drawer and its framed variant can be crafted and appear in "
            + "the creative tab. Turning this off never removes drawers already placed; they keep "
            + "working and keep their energy.")
    public static boolean ENERGY_DRAWER_ENABLED = true;

    @ConfigVal(comment = "Whether the Chemical Drawers (all three layouts, framed or not) can be crafted "
            + "and appear in the creative tab. They only exist with Mekanism installed; this switch "
            + "cannot bring them back without it. Drawers already placed keep working.")
    public static boolean CHEMICAL_DRAWERS_ENABLED = true;

    @ConfigVal(comment = "Whether the Source Drawer and its framed variant can be crafted and appear in "
            + "the creative tab. They only exist with Ars Nouveau installed. Drawers already placed "
            + "keep working.")
    public static boolean SOURCE_DRAWER_ENABLED = true;

    @ConfigVal(comment = "Whether the Wireless Charger augment can be crafted and appears in the "
            + "creative tab. Chargers already in drawers keep working.")
    public static boolean WIRELESS_CHARGER_ENABLED = true;
}
