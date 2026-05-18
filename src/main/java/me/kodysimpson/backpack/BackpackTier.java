package me.kodysimpson.backpack;

public enum BackpackTier {
    SMALL("small"),
    MEDIUM("medium"),
    LARGE("large"),
    ENDER("ender");

    private final String configKey;

    BackpackTier(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }

    public boolean isEnder() {
        return this == ENDER;
    }

    public static BackpackTier fromString(String name) {
        for (BackpackTier tier : values()) {
            if (tier.configKey.equalsIgnoreCase(name)) return tier;
        }
        return null;
    }
}
