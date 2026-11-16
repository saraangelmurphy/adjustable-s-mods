package adjustablesmods.ui;

import adjustablesmods.SModUpgrades;
import adjustablesmods.ui.plugins.UpgradeButtonPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import lunalib.lunaRefit.BaseRefitButton;

/** A small native-widget panel; S-Mod removal/replacement belongs to vanilla. */
public class SModRefitButton extends BaseRefitButton {
    public static final float WIDTH = 400f, HEIGHT = 340f;
    private ShipVariantAPI activeVariant;
    private FleetMemberAPI activeMember;
    private ButtonAPI upgradeButton;
    private LabelAPI pointsLabel;
    private long panelSession;
    private int displayedBonus;

    @Override public String getButtonName(FleetMemberAPI member, ShipVariantAPI variant) { return "Upgrade S-Mod Limit"; }
    @Override public String getIconName(FleetMemberAPI member, ShipVariantAPI variant) { return "graphics/icons/skills/technology.png"; }
    @Override public int getOrder(FleetMemberAPI member, ShipVariantAPI variant) { return 1; }
    @Override public float getPanelWidth(FleetMemberAPI member, ShipVariantAPI variant) { return WIDTH; }
    @Override public float getPanelHeight(FleetMemberAPI member, ShipVariantAPI variant) { return HEIGHT; }
    @Override public boolean hasTooltip(FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) { return true; }
    @Override public boolean isClickable(FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) { return variant != null; }
    @Override public boolean hasPanel(FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) { return variant != null; }

    @Override
    public void addTooltip(TooltipMakerAPI tooltip, FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) {
        tooltip.addPara("Purchase one additional S-Mod slot for this ship. The story-point price doubles with each upgrade.", 0f);
        tooltip.addPara("Use the vanilla refit controls to change existing S-Mods at a player-owned colony with Heavy Industry.", 10f);
    }

    @Override
    public void initPanel(CustomPanelAPI background, FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) {
        activeVariant = variant;
        activeMember = member;
        panelSession++;
        displayedBonus = SModUpgrades.getSModLimitBonus(variant);
        float width = WIDTH - 20f;
        TooltipMakerAPI content = background.createUIElement(width, HEIGHT - 65f, false);
        background.addUIElement(content).inTL(10f, 10f);
        content.addSectionHeading("Upgrade S-Mod Limit", Alignment.MID, 0f);
        content.addPara("Additional S-Mod slots purchased: %s", 12f, Misc.getHighlightColor(),
                Integer.toString(SModUpgrades.getSModLimitBonus(variant)));
        if (SModUpgrades.isUpgradeLimitReached(variant)) {
            content.addPara("The maximum supported upgrade level has been reached.", 10f);
        } else {
            content.addPara("Purchase one additional slot for %s.", 10f, Misc.getStoryOptionColor(),
                    SModUpgrades.getStoryPointCost(variant) + " story points");
        }
        content.addPara("Installing an S-Mod in the extra slot still costs the usual story point. This upgrade grants no bonus experience.", 10f);
        content.addPara("For removing or replacing S-Mods, use the vanilla colony refit controls.", 10f);
        pointsLabel = content.addPara("Story points available: " + Global.getSector().getPlayerStats().getStoryPoints(), 10f);
        CustomPanelAPI footer = background.createCustomPanel(width, 30f, new UpgradeButtonPlugin(this, variant));
        TooltipMakerAPI actions = footer.createUIElement(width, 30f, false);
        actions.setButtonFontOrbitron20();
        upgradeButton = actions.addButton("Increase Limit", UpgradeButtonPlugin.ACTION_ID,
                Misc.getStoryOptionColor(), Misc.getStoryDarkColor(), Alignment.MID, CutStyle.ALL, width, 25f, 0f);
        upgradeButton.setEnabled(SModUpgrades.canPurchaseUpgrade(variant));
        footer.addUIElement(actions);
        background.addComponent(footer).inBL(10f, 10f);
    }

    public long getPanelSession() { return panelSession; }

    public boolean isPanelActiveFor(ShipVariantAPI variant) {
        return activeVariant == variant && variant != null && displayedBonus == SModUpgrades.getSModLimitBonus(variant);
    }

    public boolean isPanelActiveFor(ShipVariantAPI variant, long session) {
        return panelSession == session && isPanelActiveFor(variant);
    }

    public void finishPurchase() {
        invalidatePanel(); // Reject queued duplicate clicks before LunaLib removes the panel next frame.
        refreshVariant();
        closePanel();
    }

    private void invalidatePanel() {
        panelSession++;
        activeVariant = null;
        activeMember = null;
        upgradeButton = null;
        pointsLabel = null;
    }

    @Override
    public void advance(FleetMemberAPI member, ShipVariantAPI variant, Float amount, MarketAPI market) {
        if (activeVariant == null) return;
        if (!isPanelActiveFor(variant) || activeMember != member) {
            invalidatePanel();
            closePanel();
            return;
        }
        if (upgradeButton != null) upgradeButton.setEnabled(SModUpgrades.canPurchaseUpgrade(variant));
        if (pointsLabel != null) pointsLabel.setText("Story points available: " + Global.getSector().getPlayerStats().getStoryPoints());
    }

    @Override
    public void onPanelClose(FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) { invalidatePanel(); }
}
