package cn.nukkit.item;

import cn.nukkit.Server;
import cn.nukkit.nbt.tag.CompoundTag;

/**
 * Created on 2015/12/27 by xtypr.
 * Package cn.nukkit.item in project Nukkit .
 */
public class ItemPotionSplash extends ProjectileItem {

    public ItemPotionSplash(Integer meta) {
        this(meta, 1);
    }

    public ItemPotionSplash(Integer meta, int count) {
        super(SPLASH_POTION, meta, count, "Splash Potion");
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public String getProjectileEntityType() {
        return "ThrownPotion";
    }

    @Override
    public float getThrowForce() {
        Object value = Server.getInstance().getProperty("potion-throw-force", 0.50f);
        if (value == null || value.equals("")) return 0.50f;

        try {
            return Float.parseFloat(value.toString());
        } catch (Exception e) {
            return 0.50f;
        }
    }

    @Override
    protected void correctNBT(CompoundTag nbt) {
        nbt.putInt("PotionId", this.meta);
    }
}
