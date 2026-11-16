package adjustablesmods;

import adjustablesmods.ui.SModRefitButton;
import com.fs.starfarer.api.BaseModPlugin;
import lunalib.lunaRefit.LunaRefitManager;

public class ModPlugin extends BaseModPlugin {
    @Override
    public void onApplicationLoad() {
        if (!LunaRefitManager.hasButtonOfClass(SModRefitButton.class)) {
            LunaRefitManager.addRefitButton(new SModRefitButton());
        }
    }
}
