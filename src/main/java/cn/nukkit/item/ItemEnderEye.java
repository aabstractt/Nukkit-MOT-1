package cn.nukkit.item;

import cn.nukkit.Player;
import cn.nukkit.math.Vector3;

public class ItemEnderEye extends ProjectileItem {
    public ItemEnderEye() {
        this(0, 1);
    }

    public ItemEnderEye(Integer meta) {
        this(meta, 1);
    }

    public ItemEnderEye(Integer meta, int count) {
        super(ENDER_EYE, meta, count, "Ender Eye");
    }

    @Override
    public boolean onClickAir(Player player, Vector3 directionVector) {
        return !player.isSneaking() || super.onClickAir(player, directionVector);
    }

    @Override
    public String getProjectileEntityType() {
        return "EnderEye";
    }

    @Override
    public float getThrowForce() {
        return 1.5f;
    }
}
