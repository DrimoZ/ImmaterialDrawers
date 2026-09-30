package dev.drimoz.immaterialdrawers.block.tile;

import com.buuz135.functionalstorage.client.model.FramedDrawerModelData;

/**
 * A tile of ours that carries a framed design.
 *
 * <p>Functional Storage 1.21 has this interface ({@code FramedTile}) and its framing code tests it, so
 * our framed drawers are framed for free there. 1.20.1 has none, and tests its own classes; this is
 * the same idea on our side, so the tint handler and the framing recipe serve every framed drawer
 * of ours through one type.
 */
public interface IDFramedTile {

    FramedDrawerModelData getFramedDrawerModelData();

    void setFramedDrawerModelData(FramedDrawerModelData design);
}
