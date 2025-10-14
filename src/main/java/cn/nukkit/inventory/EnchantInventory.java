package cn.nukkit.inventory;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.item.enchantment.EnchantingHelper;
import cn.nukkit.item.enchantment.EnchantingOption;
import cn.nukkit.level.Position;
import cn.nukkit.network.protocol.PlayerEnchantOptionsPacket;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author MagicDroidX
 * Nukkit Project
 */
public class EnchantInventory extends FakeBlockUIComponent {

    public static int ENCHANTING_TABLE_OPTION_ID = 0;

    public static final int ENCHANT_INPUT_ITEM_UI_SLOT = 14;
    public static final int ENCHANT_REAGENT_UI_SLOT = 15;

    private @NonNull EnchantingOption[] options = new EnchantingOption[3];

    public EnchantInventory(PlayerUIInventory playerUI, Position position) {
        super(playerUI, InventoryType.ENCHANT_TABLE, 14, position);
    }

    @Override
    public void onOpen(Player who) {
        super.onOpen(who);
        who.craftingType = Player.ENCHANT_WINDOW_ID;
    }

    @Override
    public void onClose(Player who) {
        super.onClose(who);
        if (this.getViewers().isEmpty()) {
            for (int i = 0; i < 2; ++i) {
                who.getInventory().addItem(this.getItem(i));
                this.clear(i);
            }
        }
        who.craftingType = Player.CRAFTING_SMALL;
        who.resetCraftingGridType();
    }

    @Override
    public void onSlotChange(int index, Item before, boolean send) {
        if (index != 0) return;

        Player mainViewer = this.viewers.stream().findFirst().orElse(null);
        if (mainViewer == null) return;

        EnchantingOption[] options = EnchantingHelper.generateOptions(this.getHolder(), this.getInputSlot(), mainViewer.getEnchantingSeed());

        for (Player player : this.getViewers()) {
            syncEnchantingTableOptions(player, options);
        }

        mainViewer.setEnchantingSeed(EnchantingHelper.generateSeed());

        System.out.println("Sending enchanting table!!!");

        super.onSlotChange(index, before, send);
    }

    public Item getInputSlot() {
        return this.getItem(0);
    }

    public Item getOutputSlot() {
        return this.getItem(0);
    }

    public Item getReagentSlot() {
        return this.getItem(1);
    }

    public static void syncEnchantingTableOptions(@NonNull Player player, @NonNull EnchantingOption[] enchantingOptions) {
        List<PlayerEnchantOptionsPacket.EnchantOptionData> protocolOptions = new ArrayList<>();

        for (EnchantingOption enchantingOption : enchantingOptions) {
            int optionId = ENCHANTING_TABLE_OPTION_ID++;

            List<PlayerEnchantOptionsPacket.EnchantData> protocolEnchantments = new ArrayList<>();
            for (int j = 0; j < enchantingOption.enchantments().length; j++) {
                protocolEnchantments.add(new PlayerEnchantOptionsPacket.EnchantData(
                        enchantingOption.enchantments()[j].getId(),
                        enchantingOption.enchantments()[j].getLevel()
                ));
            }

            protocolOptions.add(new PlayerEnchantOptionsPacket.EnchantOptionData(
                    enchantingOption.requiredXpLevel(),
                    0,
                    protocolEnchantments,
                    new ArrayList<>(),
                    new ArrayList<>(),
                    enchantingOption.displayName(),
                    optionId
            ));
        }

        player.dataPacket(new PlayerEnchantOptionsPacket() {{
            this.options.addAll(protocolOptions);
        }});
    }
}
