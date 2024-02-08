package cn.nukkit.item.enchantment;

import lombok.RequiredArgsConstructor;

public record EnchantingOption(
        int requiredXpLevel,
        String displayName,
        Enchantment[] enchantments
) {}