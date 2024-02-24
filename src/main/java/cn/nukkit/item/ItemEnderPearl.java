package cn.nukkit.item;

import cn.nukkit.Server;

public class ItemEnderPearl extends ProjectileItem {

    public ItemEnderPearl() {
        this(0, 1);
    }

    public ItemEnderPearl(Integer meta) {
        this(meta, 1);
    }

    public ItemEnderPearl(Integer meta, int count) {
        super(ENDER_PEARL, 0, count, "Ender Pearl");
    }

    @Override
    public int getMaxStackSize() {
        return 16;
    }

    @Override
    public String getProjectileEntityType() {
        return "EnderPearl";
    }

    @Override
    public float getThrowForce() {
        Object value = Server.getInstance().getProperty("ender-pearl-throw-force", 1.5f);
        if (value == null || value.equals("")) return 1.5f;

        try {
            return Float.parseFloat(value.toString());
        } catch (Exception e) {
            return 1.5f;
        }
//        return 1.5f;
    }
}
