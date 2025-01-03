package cn.nukkit.item.enchantment;

import cn.nukkit.item.*;

/**
 * @author MagicDroidX
 * Nukkit Project
 */
public enum EnchantmentType {
    ALL,
    ARMOR,
    ARMOR_HEAD,
    ARMOR_TORSO,
    ARMOR_LEGS,
    ARMOR_FEET,
    SWORD,
    AXE,
    PICKAXE,
    SHEARS,
    DIGGER,
    FISHING_ROD,
    BREAKABLE,
    BOW,
    WEARABLE,
    TRIDENT,

    CROSSBOW,

    MACE;

    public boolean canEnchantItem(Item item) {
        if (this == ALL) {
            return true;

        } else if (this == BREAKABLE && item.getMaxDurability() >= 0) {
            return true;

        } else if (item instanceof ItemArmor) {
            if (this == WEARABLE || (this == ARMOR && item.isArmor())) {
                return true;
            }

            switch (this) {
                case ARMOR_HEAD:
                    return item.isHelmet();
                case ARMOR_TORSO:
                    return item.isChestplate();
                case ARMOR_LEGS:
                    return item.isLeggings();
                case ARMOR_FEET:
                    return item.isBoots();
                default:
                    return false;
            }
        } else {
            switch (this) {
                case SWORD:
                    return item.isSword();
                    case AXE:
                    return item.isAxe();
                case PICKAXE:
                    return item.isPickaxe();
                case SHEARS:
                    return item instanceof ItemShears;
                case DIGGER:
                    return item.isPickaxe() || item.isShovel() || item.isAxe() || item.isHoe();
                case BOW:
                    return item instanceof ItemBow;
                case FISHING_ROD:
                    return item instanceof ItemFishingRod;
                case WEARABLE:
                    return item instanceof ItemSkull;
                case TRIDENT:
                    return item instanceof ItemTrident;
                case CROSSBOW:
                    return item instanceof ItemCrossbow;
                default:
                    return false;
            }
        }
    }
}
