package adjustablesmods.ui.plugins;

import adjustablesmods.SModUpgrades;
import adjustablesmods.ui.SModRefitButton;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import org.lwjgl.util.vector.Vector2f;

public class UpgradeButtonPlugin extends BaseCustomUIPanelPlugin {
    public static final String ACTION_ID = "increase_limit";
    private final SModRefitButton refitButton;
    private final ShipVariantAPI variant;
    private boolean purchased;
    private final long session;

    public UpgradeButtonPlugin(SModRefitButton refitButton, ShipVariantAPI variant) {
        this.refitButton = refitButton;
        this.variant = variant;
        this.session = refitButton.getPanelSession();
    }

    @Override
    public void buttonPressed(Object buttonId) {
        if (purchased || !ACTION_ID.equals(buttonId) || !refitButton.isPanelActiveFor(variant, session)) return;
        if (!SModUpgrades.purchaseUpgrade(variant)) return;
        purchased = true;
        refitButton.finishPurchase();
        if (Global.getSoundPlayer() != null) {
            Global.getSoundPlayer().playSound("ui_char_spent_story_point", 1f, 1f,
                    Global.getSoundPlayer().getListenerPos(), new Vector2f());
        }
    }
}
