package cn.nukkit.item;

import cn.nukkit.Server;

/**
 * @author MagicDroidX
 * Nukkit Project
 */
public class ItemSnowball extends ProjectileItem {

    public ItemSnowball() {
        this(0, 1);
    }

    public ItemSnowball(Integer meta) {
        this(meta, 1);
    }

    public ItemSnowball(Integer meta, int count) {
        super(SNOWBALL, 0, count, "Snowball");
    }

    @Override
    public int getMaxStackSize() {
        return 16;
    }

    @Override
    public String getProjectileEntityType() {
        return "Snowball";
    }

    @Override
    public float getThrowForce() {
        Object value = Server.getInstance().getProperty("snowball-throw-force", 1.5f);
        if (value == null || value.equals("")) return 1.5f;

        try {
            return Float.parseFloat(value.toString());
        } catch (Exception e) {
            return 1.5f;
        }
//        return 1.5f;return 1.5f;
    }
}
