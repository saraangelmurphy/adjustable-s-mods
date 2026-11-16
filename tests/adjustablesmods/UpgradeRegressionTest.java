package adjustablesmods;

import adjustablesmods.hullmods.ExtraSModSlots;
import adjustablesmods.ui.SModRefitButton;
import adjustablesmods.ui.plugins.UpgradeButtonPlugin;
import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import java.awt.Color;
import java.lang.reflect.*;
import java.util.*;
import lunalib.lunaRefit.BaseRefitButton;
import lunalib.lunaRefit.LunaRefitManager;

public class UpgradeRegressionTest {
    interface Call { Object invoke(Method m, Object[] a) throws Throwable; }
    static int checks, points, spent, payments;
    static UpgradeButtonPlugin panelAction;
    static final List<Boolean> enabledButtons = new ArrayList<>();

    @SuppressWarnings("unchecked")
    static <T> T proxy(Class<T> type, Call call) {
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->{
            if(m.getName().equals("toString"))return type.getSimpleName();
            if(m.getName().equals("hashCode"))return System.identityHashCode(p);
            if(m.getName().equals("equals"))return p==a[0];
            Object result=call.invoke(m,a==null?new Object[0]:a);
            if(result!=null || !m.getReturnType().isPrimitive())return result;
            if(m.getReturnType()==boolean.class)return false;
            if(m.getReturnType()==int.class)return 0;
            if(m.getReturnType()==float.class)return 0f;
            if(m.getReturnType()==long.class)return 0L;
            return null;
        });
    }
    static void check(boolean value,String message) { if(!value)throw new AssertionError(message); checks++; }
    static class Variant {
        final Set<String> tags=new LinkedHashSet<>(),perma=new LinkedHashSet<>(),smods=new LinkedHashSet<>(),builtIns=new LinkedHashSet<>();
        int trackerAdds;
        final ShipVariantAPI api=proxy(ShipVariantAPI.class,(m,a)->switch(m.getName()) {
            case "getTags"->tags;
            case "addTag"->{tags.add((String)a[0]);yield null;}
            case "removeTag"->{tags.remove(a[0]);yield null;}
            case "getSMods"->smods;
            case "getSModdedBuiltIns"->builtIns;
            case "getPermaMods"->perma;
            case "hasHullMod"->perma.contains(a[0]);
            case "addPermaMod"->{perma.add((String)a[0]);if(a.length>1&&(boolean)a[1])smods.add((String)a[0]);trackerAdds++;yield null;}
            default->null;
        });
    }
    static <T> T ui(Class<T> type) {
        return proxy(type,(m,a)->{
            if(m.getName().equals("createCustomPanel") && a[2] instanceof UpgradeButtonPlugin action)panelAction=action;
            if(m.getName().equals("setEnabled"))enabledButtons.add((boolean)a[0]);
            if(m.getReturnType().isInterface())return ui(m.getReturnType());
            return null;
        });
    }
    static void setup() {
        Global.setSettings(proxy(SettingsAPI.class,(m,a)->m.getReturnType()==Color.class?Color.WHITE:null));
        MutableCharacterStatsAPI stats=proxy(MutableCharacterStatsAPI.class,(m,a)->switch(m.getName()) {
            case "getStoryPoints"->points;
            case "spendStoryPoints"->{
                int cost=(int)a[0]; check(cost>0&&points>=cost,"No unaffordable or negative spend");
                check(a.length==6 && (float)a[4]==0f,"Upgrades grant no bonus experience");
                points-=cost;spent+=cost;payments++;yield null;
            }
            default->null;
        });
        Global.setSector(proxy(SectorAPI.class,(m,a)->m.getName().equals("getPlayerStats")?stats:null));
    }
    static void testTagsAndCost() {
        Variant v=new Variant();
        check(SModUpgrades.getSModLimitBonus(v.api)==0 && SModUpgrades.getStoryPointCost(v.api)==2,"Initial upgrade price is 2 SP");
        check(v.tags.isEmpty(),"Reading cost must not mutate save tags");
        v.tags.add("other_adjustablesmods:extra_slots:9");v.tags.add("adjustablesmods:extra_slots:bad");v.tags.add("adjustablesmods:extra_slots:-2");
        check(SModUpgrades.getSModLimitBonus(v.api)==0,"Ignore malformed, negative and unrelated tags");
        v.tags.add("adjustablesmods:extra_slots:2");v.tags.add("adjustablesmods:extra_slots:1");
        check(SModUpgrades.getSModLimitBonus(v.api)==2&&SModUpgrades.getStoryPointCost(v.api)==8,"Deterministic maximum valid save tag");
        SModUpgrades.increaseSModLimit(v.api);
        check(SModUpgrades.getSModLimitBonus(v.api)==3&&SModUpgrades.getStoryPointCost(v.api)==16,"Price doubles after each purchase");
        check(v.tags.contains("other_adjustablesmods:extra_slots:9")&&v.tags.size()==2,"Normalize only owned tags");
        v.tags.clear();v.tags.add("adjustablesmods:extra_slots:2147483648");
        check(SModUpgrades.getSModLimitBonus(v.api)==0,"Oversized numeric text cannot crash ship creation");
        v.tags.clear();v.tags.add("adjustablesmods:extra_slots:30");points=Integer.MAX_VALUE;
        check(SModUpgrades.getStoryPointCost(v.api)==Integer.MAX_VALUE&&!SModUpgrades.canPurchaseUpgrade(v.api),"Stop before signed integer price overflow");
        check(!SModUpgrades.purchaseUpgrade(v.api),"Overflow-level upgrade cannot charge points");
        check(SModUpgrades.getSModLimitBonus(null)==0&&!SModUpgrades.canPurchaseUpgrade(null),"No selected ship is safe");
        check(!SModUpgrades.canAffordStoryPointsCost(0)&&!SModUpgrades.canAffordStoryPointsCost(-1),"Invalid prices cannot be spent");
    }
    static void testPurchases() {
        Variant v=new Variant();v.smods.add("existing_smod");v.builtIns.add("existing_builtin_smod");points=1;spent=0;payments=0;
        check(!SModUpgrades.purchaseUpgrade(v.api)&&v.tags.isEmpty()&&v.perma.isEmpty()&&points==1,"Insufficient SP leaves ship and wallet unchanged");
        points=10;
        check(SModUpgrades.purchaseUpgrade(v.api)&&points==8&&SModUpgrades.getSModLimitBonus(v.api)==1,"Purchase adds one slot for two SP");
        check(v.perma.contains("asm_extra_smod_slots")&&!v.smods.contains("asm_extra_smod_slots"),"Hidden tracker does not consume an S-Mod slot");
        check(SModUpgrades.purchaseUpgrade(v.api)&&points==4&&SModUpgrades.getSModLimitBonus(v.api)==2,"Second purchase costs four SP");
        check(v.trackerAdds==1,"Tracker is not added repeatedly");
        check(v.smods.equals(Set.of("existing_smod"))&&v.builtIns.equals(Set.of("existing_builtin_smod")),"Existing S-Mods and built-in enhancements stay intact");
        check(!SModUpgrades.purchaseUpgrade(v.api)&&points==4&&payments==2,"Recheck affordability at every purchase");
        Variant module=new Variant();module.tags.addAll(v.tags);module.perma.addAll(v.perma);
        check(SModUpgrades.getSModLimitBonus(module.api)==2&&SModUpgrades.getStoryPointCost(module.api)==8,"Saved/copied variant tags retain purchased slots and cost");
    }
    static void testStats() {
        Variant parent=new Variant(),module=new Variant();parent.tags.add("adjustablesmods:extra_slots:1");module.tags.add("adjustablesmods:extra_slots:3");
        FleetMemberAPI member=proxy(FleetMemberAPI.class,(m,a)->m.getName().equals("getVariant")?parent.api:null);
        StatBonus bonus=new StatBonus();bonus.modifyFlat("other_mod",1);
        DynamicStatsAPI dynamic=proxy(DynamicStatsAPI.class,(m,a)->{
            check(m.getName().equals("getMod")&&a[0].equals(Stats.MAX_PERMANENT_HULLMODS_MOD),"Feature Creep only modifies max S-Mod stat");
            return bonus;
        });
        MutableShipStatsAPI moduleStats=proxy(MutableShipStatsAPI.class,(m,a)->switch(m.getName()) {
            case "getVariant"->module.api;case "getFleetMember"->member;case "getDynamic"->dynamic;default->null;
        });
        ExtraSModSlots effect=new ExtraSModSlots();
        effect.applyEffectsBeforeShipCreation(ShipAPI.HullSize.CRUISER,moduleStats,"asm_extra_smod_slots");
        check(bonus.computeEffective(2f)==6f,"Module uses its own variant bonus and stacks with other stats");
        effect.applyEffectsBeforeShipCreation(ShipAPI.HullSize.CRUISER,moduleStats,"asm_extra_smod_slots");
        check(bonus.computeEffective(2f)==6f,"Reapplying stats does not accumulate bonus");
        MutableShipStatsAPI detached=proxy(MutableShipStatsAPI.class,(m,a)->switch(m.getName()) {
            case "getVariant"->module.api;case "getDynamic"->dynamic;default->null;
        });
        effect.applyEffectsBeforeShipCreation(ShipAPI.HullSize.CRUISER,detached,"asm_extra_smod_slots");
        check(bonus.computeEffective(2f)==6f,"Refit stats work without a fleet member");
        MutableShipStatsAPI fallback=proxy(MutableShipStatsAPI.class,(m,a)->switch(m.getName()) {
            case "getFleetMember"->member;case "getDynamic"->dynamic;default->null;
        });
        effect.applyEffectsBeforeShipCreation(ShipAPI.HullSize.CRUISER,fallback,"asm_extra_smod_slots");
        check(bonus.computeEffective(2f)==4f,"Fleet member is a fallback when stats variant is absent");
        effect.addPostDescriptionSection(ui(TooltipMakerAPI.class),ShipAPI.HullSize.CRUISER,null,400f,true);
        check(true,"Generic hullmod tooltip tolerates no ship");
    }
    static void testPanel() {
        Variant v=new Variant(),other=new Variant();points=10;spent=0;payments=0;
        SModRefitButton button=new SModRefitButton();
        CustomPanelAPI background=ui(CustomPanelAPI.class);
        button.initPanel(background,null,v.api,null);
        check(button.isPanelActiveFor(v.api),"Native UI opens for selected variant");
        UpgradeButtonPlugin action=panelAction;
        action.buttonPressed("remove_smod");action.buttonPressed(null);
        check(payments==0,"Removed legacy actions and unknown events do nothing");
        points=1;button.advance(null,v.api,0.1f,null);action.buttonPressed(new String("increase_limit"));
        check(payments==0&&!enabledButtons.get(enabledButtons.size()-1),"Live button and action reject stale wallet balance");
        points=10;button.advance(null,v.api,0.1f,null);action.buttonPressed(new String("increase_limit"));
        check(payments==1&&points==8&&!button.isPanelActiveFor(v.api),"String value action buys and invalidates panel");
        action.buttonPressed("increase_limit");check(payments==1,"Queued duplicate click cannot buy twice");
        button.initPanel(background,null,v.api,null);action=panelAction;
        button.advance(null,other.api,0.1f,null);action.buttonPressed("increase_limit");
        check(payments==1,"Switching ships invalidates old panel actions");
        button.initPanel(background,null,v.api,null);action=panelAction;
        button.onPanelClose(null,v.api,null);action.buttonPressed("increase_limit");
        check(payments==1,"Closed panel actions cannot spend points");
        UpgradeButtonPlugin closedAction=action;
        button.initPanel(background,null,v.api,null);closedAction.buttonPressed("increase_limit");
        check(payments==1,"Reopening same ship cannot revive an old panel's actions");
        button.initPanel(background,null,v.api,null);action=panelAction;
        SModUpgrades.increaseSModLimit(v.api);points=100;action.buttonPressed("increase_limit");
        check(payments==1&&points==100,"External variant changes cannot charge an undisplayed price");
        button.advance(null,v.api,0.1f,null);
        check(!button.isPanelActiveFor(v.api),"External variant changes invalidate displayed quote");
    }
    static void testRegistration() {
        var buttons=LunaRefitManager.INSTANCE.getButtons();
        var original=new ArrayList<>(buttons);
        try {
            buttons.clear();
            BaseRefitButton other=new BaseRefitButton() {};
            buttons.add(other);
            new ModPlugin().onApplicationLoad();new ModPlugin().onApplicationLoad();
            check(buttons.stream().filter(b->b instanceof SModRefitButton).count()==1,"Registration is idempotent");
            check(buttons.contains(other),"Registration preserves other mods' buttons");
        } finally { buttons.clear();buttons.addAll(original); }
    }
    public static void main(String[] args) {
        try { setup();testTagsAndCost();testPurchases();testStats();testPanel();testRegistration();
            System.out.println("PASS: "+checks+" S-Mod upgrade regression checks");
        } finally { Global.setSector(null);Global.setSettings(null);Global.setSoundPlayer(null); }
    }
}
