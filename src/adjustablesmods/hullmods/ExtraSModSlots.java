package adjustablesmods.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import adjustablesmods.SModUpgrades;

public class ExtraSModSlots extends BaseHullMod {
    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        // Refit/module stats can contain a different variant from the parent fleet member.
        var variant = stats.getVariant();
        if (variant == null && stats.getFleetMember() != null) variant = stats.getFleetMember().getVariant();
        stats.getDynamic().getMod(Stats.MAX_PERMANENT_HULLMODS_MOD).modifyFlat(id, SModUpgrades.getSModLimitBonus(variant));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float pad = 3f;
        float oPad = 10f;
        Color good = Misc.getPositiveHighlightColor();
        Color b = Misc.getHighlightColor();

        tooltip.setBulletedListMode(" - ");
        tooltip.addPara("Increases the ship's maximum s-mod limit by %s", oPad, good,
                SModUpgrades.getSModLimitBonus(ship == null ? null : ship.getVariant()) + "");
        if (ship != null) tooltip.addPara("A maximum of %s hullmods can be s-modded", pad, b, Misc.getMaxPermanentMods(ship) + "");
        tooltip.setBulletedListMode(null);
    }

}
