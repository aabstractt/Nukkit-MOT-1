package cn.nukkit.item.enchantment;

import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemID;
import cn.nukkit.level.Position;
import lombok.NonNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public final class EnchantingHelper {

    private static int MAX_BOOKSHELF_COUNT = 15;

    public static long generateSeed() {
        return ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public static @NonNull Item enchantItem(@NonNull Item inputItem, @NonNull Enchantment[] enchantments) {
        Item resultItem = inputItem.getId() == ItemID.BOOK ? Item.get(ItemID.ENCHANTED_BOOK) : inputItem.clone();
        resultItem.addEnchantment(enchantments);

        return resultItem;
    }

    public static @NonNull EnchantingOption[] generateOptions(@NonNull Position position, @NonNull Item input, long seed) {
        if (input.isNull() || input.hasEnchantments()) return new EnchantingOption[0];

        Random random = new Random(seed);

        int bookshelfCount = countBookshelves(position);
        int baseRequiredLevel = random.nextInt(1, 8) + (bookshelfCount >> 1) + random.nextInt(bookshelfCount + 1);
        int topRequiredLevel = (int) (double) Math.max(baseRequiredLevel / 3, 1);
        int midRequiredLevel = (int) (double) Math.max(baseRequiredLevel * 2 / 3 + 1, 1);
        int bottomRequiredLevel = Math.max(baseRequiredLevel, bookshelfCount * 2);

        return new EnchantingOption[]{
                createOption(random, input, topRequiredLevel),
                createOption(random, input, midRequiredLevel),
                createOption(random, input, bottomRequiredLevel)
        };
    }

    private static int countBookshelves(@NonNull Position position) {
        int bookshelfCount = 0;
        for (int x = -2; x <= 2; ++x) {
            for (int z = -2; z <= 2; ++z) {
                if (Math.abs(x) != 2 && Math.abs(z) != 2) continue;

                boolean checking = true;
                for (int y = 0; y <= 1; y++) {
                    if (!checking) continue;

                    int spaceX = Math.max(Math.min(x, 1), -1);
                    int spaceZ = Math.max(Math.min(z, 1), -1);

                    Block spaceBlock = position.getLevel().getBlock(position.add(spaceX, y, spaceZ));
                    if (spaceBlock.getId() == BlockID.AIR) continue;

                    checking = false;
                }

                if (!checking) continue;

                for (int y = 0; y <= 1; y++) {
                    Block block = position.getLevel().getBlock(position.add(x, y, z));
                    if (block.getId() != BlockID.BOOKSHELF) continue;

                    bookshelfCount++;
                    if (bookshelfCount >= MAX_BOOKSHELF_COUNT) return bookshelfCount;
                }
            }
        }

        return bookshelfCount;
    }

    private static EnchantingOption createOption(@NonNull Random random, @NonNull Item inputItem, int requiredXpLevel) {
        int enchantingPower = requiredXpLevel;

        int enchantAbility = inputItem.getEnchantAbility();
        enchantingPower = enchantingPower + random.nextInt(0, enchantAbility >> 2) + random.nextInt(0, enchantAbility >> 2) + 1;

        double bonus = 1 + (random.nextFloat() + random.nextFloat() - 1) * 0.15;
        enchantingPower = (int) Math.round(enchantingPower * bonus);

        List<Enchantment> resultEnchantments = new ArrayList<>();
        List<Enchantment> availableEnchantments = getAvailableEnchantments(enchantingPower, inputItem);

        Enchantment lastEnchantment = getRandomWeightedEnchantment(random, availableEnchantments);
        if (lastEnchantment == null) {
            return new EnchantingOption(
                    requiredXpLevel,
                    getRandomOptionName(random),
                    Enchantment.EMPTY_ARRAY
            );
        }

        resultEnchantments.add(lastEnchantment);

        while (random.nextInt() <= (enchantingPower + 1) / 50) {
            Enchantment finalLastEnchantment = lastEnchantment;
            availableEnchantments = availableEnchantments.stream()
                    .filter(enchantment -> enchantment.getId() != finalLastEnchantment.getId())
                    .filter(enchantment -> enchantment.isCompatibleWith(finalLastEnchantment))
                    .collect(Collectors.toList());

            lastEnchantment = getRandomWeightedEnchantment(random, availableEnchantments);
            if (lastEnchantment == null) break;

            resultEnchantments.add(lastEnchantment);
            enchantingPower >>= 1;
        }

        return new EnchantingOption(
                requiredXpLevel,
                getRandomOptionName(random),
                resultEnchantments.toArray(Enchantment.EMPTY_ARRAY)
        );
    }

    private static @NonNull List<Enchantment> getAvailableEnchantments(int enchantingPower, @NonNull Item item) {
        List<Enchantment> enchantments = new ArrayList<>();

        for (Enchantment enchantment : AvailableEnchantmentRegistry.getInstance().getPrimaryEnchantmentsForItem(item)) {
            for (int level = enchantment.getMaxLevel(); level > 0; level--) {
                if (enchantingPower >= enchantment.getMinEnchantAbility(level) && enchantingPower <= enchantment.getMaxEnchantAbility(level)) {
                    enchantments.add(enchantment);
                    break;
                }
            }
        }

        return enchantments;
    }

    private static @Nullable Enchantment getRandomWeightedEnchantment(@NonNull Random random, @NonNull List<Enchantment> enchantments) {
        if (enchantments.isEmpty()) return null;

        int totalWeight = 0;
        for (Enchantment enchantment : enchantments) {
            totalWeight += enchantment.getWeight();
        }

        int randomWeight = random.nextInt(totalWeight);
        for (Enchantment enchantment : enchantments) {
            randomWeight -= enchantment.getWeight();
            if (randomWeight >= 0) continue;

            return enchantment;
        }

        return null;
    }

    private static @NonNull String getRandomOptionName(@NonNull Random random) {
        StringBuilder name = new StringBuilder();

        int length = random.nextInt(5, 15);
        for (int i = 0; i < length; i++) {
            name.append(i);
        }

        return name.toString();
    }
}