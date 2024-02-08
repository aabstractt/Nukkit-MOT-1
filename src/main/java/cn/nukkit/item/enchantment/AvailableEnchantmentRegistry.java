package cn.nukkit.item.enchantment;

import cn.nukkit.item.Item;
import lombok.Getter;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AvailableEnchantmentRegistry {

    @Getter private static @NonNull AvailableEnchantmentRegistry instance = new AvailableEnchantmentRegistry();

    private @NonNull List<Integer> enchantments = new ArrayList<>();
    private @NonNull Map<Integer, EnchantmentType[]> primaryItemTypes = new HashMap<>();
    private @NonNull Map<Integer, EnchantmentType[]> secondaryItemTypes = new HashMap<>();

    public void init() {
        this.register(
                Enchantment.ID_PROTECTION_ALL,
                new EnchantmentType[]{EnchantmentType.ARMOR}
        );
        this.register(
                Enchantment.ID_PROTECTION_FIRE,
                new EnchantmentType[]{EnchantmentType.ARMOR}
        );
        this.register(
                Enchantment.ID_PROTECTION_FALL,
                new EnchantmentType[]{EnchantmentType.ARMOR}
        );
        this.register(
                Enchantment.ID_PROTECTION_EXPLOSION,
                new EnchantmentType[]{EnchantmentType.ARMOR}
        );
        this.register(
                Enchantment.ID_PROTECTION_PROJECTILE,
                new EnchantmentType[]{EnchantmentType.ARMOR}
        );
        this.register(
                Enchantment.ID_THORNS,
                new EnchantmentType[]{EnchantmentType.ARMOR_TORSO},
                new EnchantmentType[]{EnchantmentType.ARMOR_HEAD, EnchantmentType.ARMOR_LEGS, EnchantmentType.ARMOR_FEET}
        );
        this.register(
                Enchantment.ID_WATER_BREATHING,
                new EnchantmentType[]{EnchantmentType.ARMOR_HEAD}
        );
        this.register(
                Enchantment.ID_DAMAGE_ALL,
                new EnchantmentType[]{EnchantmentType.SWORD, EnchantmentType.AXE}
        );
        this.register(
                Enchantment.ID_KNOCKBACK,
                new EnchantmentType[]{EnchantmentType.SWORD}
        );

        this.register(
                Enchantment.ID_FIRE_ASPECT,
                new EnchantmentType[]{EnchantmentType.SWORD}
        );
        this.register(
                Enchantment.ID_EFFICIENCY,
                new EnchantmentType[]{EnchantmentType.DIGGER},
                new EnchantmentType[]{EnchantmentType.SHEARS}
        );
        this.register(
                Enchantment.ID_FORTUNE_DIGGING,
                new EnchantmentType[]{EnchantmentType.DIGGER}
        );
        this.register(
                Enchantment.ID_SILK_TOUCH,
                new EnchantmentType[]{EnchantmentType.DIGGER},
                new EnchantmentType[]{EnchantmentType.SHEARS}
        );

        this.register(
                Enchantment.ID_DURABILITY,
                new EnchantmentType[]{EnchantmentType.BREAKABLE}
        );
        this.register(
                Enchantment.ID_BOW_POWER,
                new EnchantmentType[]{EnchantmentType.BOW}
        );
        this.register(
                Enchantment.ID_BOW_KNOCKBACK,
                new EnchantmentType[]{EnchantmentType.BOW}
        );
        this.register(
                Enchantment.ID_BOW_FLAME,
                new EnchantmentType[]{EnchantmentType.BOW}
        );
        this.register(
                Enchantment.ID_BOW_INFINITY,
                new EnchantmentType[]{EnchantmentType.BOW}
        );
    }

    public void register(@NonNull Integer enchantmentId, @NonNull EnchantmentType[] primaryItemTypes) {
        this.register(enchantmentId, primaryItemTypes, new EnchantmentType[0]);
    }

    public void register(@NonNull Integer enchantmentId, @NonNull EnchantmentType[] primaryItemTypes, @NonNull EnchantmentType[] secondaryItemTypes) {
        this.enchantments.add(enchantmentId);

        this.setPrimaryItemTypes(enchantmentId, primaryItemTypes);
        this.setSecondaryItemTypes(enchantmentId, secondaryItemTypes);
    }

    private void setPrimaryItemTypes(@NonNull Integer enchantmentId, @NonNull EnchantmentType[] primaryItemTypes) {
        this.primaryItemTypes.put(enchantmentId, primaryItemTypes);
    }

    private void setSecondaryItemTypes(@NonNull Integer enchantmentId, @NonNull EnchantmentType[] secondaryItemTypes) {
        this.secondaryItemTypes.put(enchantmentId, secondaryItemTypes);
    }

    public @NonNull Enchantment[] getPrimaryEnchantmentsForItem(@NonNull Item item) {
        if (item.hasEnchantments()) return Enchantment.EMPTY_ARRAY;

        List<Enchantment> enchantments = new ArrayList<>();

        for (Integer enchantmentId : this.enchantments) {
            EnchantmentType[] itemTypes = this.primaryItemTypes.get(enchantmentId);

            for (EnchantmentType itemType : itemTypes) {
                if (!itemType.canEnchantItem(item)) continue;

                enchantments.add(Enchantment.get(enchantmentId));
            }
        }

        return enchantments.toArray(Enchantment.EMPTY_ARRAY);
    }
}