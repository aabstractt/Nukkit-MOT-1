package cn.nukkit.entity.item;

import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.data.IntEntityData;
import cn.nukkit.entity.projectile.EntityProjectile;
import cn.nukkit.event.potion.PotionCollideEvent;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.Particle;
import cn.nukkit.level.particle.SpellParticle;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.network.protocol.LevelSoundEventPacket;
import cn.nukkit.potion.Effect;
import cn.nukkit.potion.Potion;

/**
 * @author xtypr
 */
public class EntityPotion extends EntityProjectile {

    public static final int NETWORK_ID = 86;

    public int potionId;

    public EntityPotion(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    public EntityPotion(FullChunk chunk, CompoundTag nbt, Entity shootingEntity) {
        super(chunk, nbt, shootingEntity);
    }

    @Override
    protected void initEntity() {
        super.initEntity();

        potionId = this.namedTag.getShort("PotionId");

        this.dataProperties.putShort(DATA_POTION_AUX_VALUE, this.potionId);

        Effect effect = Potion.getEffect(potionId, true);

        if (effect != null) {
            int count = 0;
            int[] c = effect.getColor();
            count += effect.getAmplifier() + 1;

            int r = ((c[0] * (effect.getAmplifier() + 1)) / count) & 0xff;
            int g = ((c[1] * (effect.getAmplifier() + 1)) / count) & 0xff;
            int b = ((c[2] * (effect.getAmplifier() + 1)) / count) & 0xff;

            this.setDataProperty(new IntEntityData(Entity.DATA_POTION_COLOR, (r << 16) + (g << 8) + b));
        }
    }

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
        Object value = Server.getInstance().getProperty("potion-gravity", 0.05f);
        if (value == null || value.equals("")) return 0.05f;

        try {
            return Float.parseFloat(value.toString());
        } catch (Exception e) {
            return 0.05f;
        }
//        return 0.05f;
    }

    @Override
    protected float getDrag() {
        Object value = Server.getInstance().getProperty("potion-drag", 0.01f);
        if (value == null || value.equals("")) return 0.01f;

        try {
            return Float.parseFloat(value.toString());
        } catch (Exception e) {
            return 0.01f;
        }

//        return 0.01f;
    }

    protected void splash(Entity collidedWith) {
        Potion potion = Potion.getPotion(this.potionId);
        PotionCollideEvent event = new PotionCollideEvent(potion, this);
        this.server.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return;
        }

        this.close();

        potion = event.getPotion();
        if (potion == null) {
            return;
        }

        potion.setSplash(true);

        Particle particle;
        int r;
        int g;
        int b;

        Effect effect = Potion.getEffect(potion.getId(), true);

        if (effect == null) {
            r = 40;
            g = 40;
            b = 255;
        } else {
            int[] colors = effect.getColor();
            r = colors[0];
            g = colors[1];
            b = colors[2];
        }

        particle = new SpellParticle(this, r, g, b);

        this.getLevel().addParticle(particle);
        this.getLevel().addLevelSoundEvent(this, LevelSoundEventPacket.SOUND_GLASS);

        if (this.isLinger()) {
            CompoundTag nbt = Entity.getDefaultNBT(this);
            nbt.putShort("PotionId", this.potionId);
            if (effect != null) {
                nbt.putList("mobEffects", new ListTag<>().add(effect.save()));
            }
            Entity entity = Entity.createEntity("AreaEffectCloud", this.chunk, nbt);
            if (entity instanceof EntityAreaEffectCloud entityAreaEffectCloud) {
                entityAreaEffectCloud.setOwner(this.shootingEntity);
            }
            entity.spawnToAll();
            return;
        }

        Entity[] entities = this.getLevel().getNearbyEntities(this.getBoundingBox().grow(4.125, 2.125, 4.125));
        for (Entity anEntity : entities) {
            double distance = anEntity.distanceSquared(this);
            if (distance < 16) {
                double d = anEntity.equals(collidedWith) ? 3 : 1 - Math.sqrt(distance) / 4;

                potion.applyPotion(anEntity, d);
            }
        }
    }

    private float getGrow() {
        Object value = Server.getInstance().getProperty("potion-grow", 1.5f);
        if (value == null || value.equals("")) return 1.5f;

        try {
            return Float.parseFloat(value.toString());
        } catch (Exception e) {
            return 1.5f;
        }
    }

    @Override
    public void onCollideWithEntity(Entity entity) {
        this.splash(entity);
        this.close();
    }

    @Override
    protected void onHitGround(Vector3 vector3) {
        super.onHitGround(vector3);

        this.splash(null);
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) return false;

        if (this.age > 1200 || this.isCollided) {
            this.close();
            return false;
        }

        return super.onUpdate(currentTick);
    }

    public boolean isLinger() {
        return false;
    }
}
