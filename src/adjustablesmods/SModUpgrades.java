package adjustablesmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipVariantAPI;

public final class SModUpgrades {
    private static final String LIMIT_TAG_PREFIX = "adjustablesmods:extra_slots:";

    private SModUpgrades() {}

    public static int getSModLimitBonus(ShipVariantAPI variant) {
        if (variant == null) return 0;
        int bonus = 0;
        for (String tag : variant.getTags()) {
            if (!tag.startsWith(LIMIT_TAG_PREFIX)) continue;
            try {
                bonus = Math.max(bonus, Integer.parseInt(tag.substring(LIMIT_TAG_PREFIX.length())));
            } catch (NumberFormatException ignored) {
                // Ignore malformed third-party tags rather than breaking refit/ship creation.
            }
        }
        return bonus;
    }

    public static int getStoryPointCost(ShipVariantAPI variant) {
        int bonus = getSModLimitBonus(variant);
        // Preserve the upstream 2, 4, 8... prices without overflowing the exponent.
        return bonus >= 30 ? Integer.MAX_VALUE : 1 << (bonus + 1);
    }

    public static void increaseSModLimit(ShipVariantAPI variant) {
        int bonus = getSModLimitBonus(variant);
        if (bonus == Integer.MAX_VALUE) return;
        for (String tag : new java.util.ArrayList<>(variant.getTags())) {
            if (tag.startsWith(LIMIT_TAG_PREFIX)) variant.removeTag(tag);
        }
        variant.addTag(LIMIT_TAG_PREFIX + (bonus + 1));
    }

    public static boolean isUpgradeLimitReached(ShipVariantAPI variant) {
        // Beyond 30 purchases the next power-of-two price exceeds the game's signed integer currency API.
        return getSModLimitBonus(variant) >= 30;
    }

    public static boolean canPurchaseUpgrade(ShipVariantAPI variant) {
        return variant != null && !isUpgradeLimitReached(variant) && canAffordStoryPointsCost(getStoryPointCost(variant));
    }

    public static boolean purchaseUpgrade(ShipVariantAPI variant) {
        if (!canPurchaseUpgrade(variant)) return false;
        int cost = getStoryPointCost(variant);
        Global.getSector().getPlayerStats().spendStoryPoints(cost, true, null, true, 0f, null);
        increaseSModLimit(variant);
        installSModTracker(variant);
        return true;
    }

    public static void installSModTracker(ShipVariantAPI variant) {
        if (!variant.getPermaMods().contains("asm_extra_smod_slots")) {
            variant.addPermaMod("asm_extra_smod_slots", false);
        }
    }

    public static boolean canAffordStoryPointsCost(int cost) {
        return cost > 0 && Global.getSector() != null && Global.getSector().getPlayerStats().getStoryPoints() >= cost;
    }
}
