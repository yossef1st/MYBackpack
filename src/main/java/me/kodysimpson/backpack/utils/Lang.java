package me.kodysimpson.backpack.utils;

import org.bukkit.ChatColor;

public enum Lang {

    PREFIX("&6[MYBackpack]&r", "&6[MYBackpack]&r"),
    GIVEN("&aتم إعطاء &e{player}&a حقيبة {tier}", "&aGiven &e{player}&a a {tier} backpack"),
    RECEIVED("&6لقد استلمت حقيبة {tier}&6!", "&6You received a {tier} backpack!"),
    RELOADED("&aتم إعادة تحميل الكونفيج", "&aConfiguration reloaded"),
    PLAYER_NOT_FOUND("&cاللاعب &e{player}&c غير موجود", "&cPlayer &e{player}&c not found"),
    INVALID_TIER("&cتير غير صحيح. استخدم: small, medium, large, ender", "&cInvalid tier. Use: small, medium, large, ender"),
    NO_PERMISSION("&cليس لديك الصلاحية", "&cYou don't have permission"),
    USAGE("&eالاستخدام: /backpack give <player> <small|medium|large|ender>", "&eUsage: /backpack give <player> <small|medium|large|ender>"),
    AUTO_COLLECT_ENABLED("&6التجميع التلقائي: &aمفعل", "&6Auto-collect: &aEnabled"),
    AUTO_COLLECT_DISABLED("&6التجميع التلقائي: &cمعطل", "&6Auto-collect: &cDisabled"),
    COLLECTED("&7» &6تم جمع العنصر في الحقيبة!", "&7» &6Item collected into backpack!"),
    PLUGIN_ENABLED("&aMYBackpack v{ver} - تم التفعيل بواسطة YOSSEF_1ST باستخدام opencode", "&aMYBackpack v{ver} - Enabled by YOSSEF_1ST using opencode"),
    PLUGIN_DISABLED("&cMYBackpack - تم التعطيل", "&cMYBackpack - Disabled");

    private final String ar;
    private final String en;

    Lang(String ar, String en) {
        this.ar = ar;
        this.en = en;
    }

    public String get(boolean arabic) {
        return arabic ? ar : en;
    }

    public String format(boolean arabic, Object... replacements) {
        String msg = get(arabic);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                msg = msg.replace("{" + replacements[i] + "}", String.valueOf(replacements[i + 1]));
            }
        }
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
