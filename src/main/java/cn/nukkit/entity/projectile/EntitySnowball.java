package cn.nukkit.entity.projectile;

import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import cn.nukkit.item.ItemSnowball;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.ItemBreakParticle;
import cn.nukkit.nbt.tag.CompoundTag;

/**
 * @author MagicDroidX
 * Nukkit Project
 */
public class EntitySnowball extends EntityProjectile {

    public static final int NETWORK_ID = 81;

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    @Override
    public float getWidth() {
        return 0.25f;
    }

    @Override
    public float getLength() {
        return 0.25f;
    }

    @Override
    public float getHeight() {
        return 0.25f;
    }

    @Override
    protected float getGravity() {
        Object value = Server.getInstance().getProperty("snowball-gravity", 0.03f);
        if (value == null || value.equals("")) return 0.03f;

        try {
            return Float.parseFloat(value.toString());
        } catch (Exception e) {
            return 0.03f;
        }
//        return 0.03f;
    }

    @Override
    protected float getDrag() {
        Object value = Server.getInstance().getProperty("snowball-drag", 0.01f);
        if (value == null || value.equals("")) return 0.01f;

        try {
            return Float.parseFloat(value.toString());
        } catch (Exception e) {
            return 0.01f;
        }
//        return 0.01f;
    }

    public EntitySnowball(FullChunk chunk, CompoundTag nbt) {
        this(chunk, nbt, null);
    }

    public EntitySnowball(FullChunk chunk, CompoundTag nbt, Entity shootingEntity) {
        super(chunk, nbt, shootingEntity);
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        boolean hasUpdate = super.onUpdate(currentTick);

        if (this.age > 1200 || this.isCollided || this.hadCollision) {
            this.kill();
            hasUpdate = true;
        }

        return hasUpdate;
    }

    @Override
    public void onHit() {
        ItemSnowball snowball = new ItemSnowball();
        level.addParticle(new ItemBreakParticle(this, snowball), null, 5);
    }
}
