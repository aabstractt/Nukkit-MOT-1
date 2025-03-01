package cn.nukkit.event.entity;

import cn.nukkit.entity.Entity;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.potion.Effect;

import java.util.Map;

/**
 * @author MagicDroidX
 * Nukkit Project
 */
public class EntityDamageByEntityEvent extends EntityDamageEvent {

    private final Entity damager;

    private float knockBack;

    private Enchantment[] enchantments;

    public EntityDamageByEntityEvent(Entity damager, Entity entity, DamageCause cause, float damage) {
        this(damager, entity, cause, damage, 0.3f);
    }

    public EntityDamageByEntityEvent(Entity damager, Entity entity, DamageCause cause, Map<DamageModifier, Float> modifiers) {
        this(damager, entity, cause, modifiers, 0.3f);
    }

    public EntityDamageByEntityEvent(Entity damager, Entity entity, DamageCause cause, float damage, float knockBack) {
        super(entity, cause, damage);
        this.damager = damager;
        this.knockBack = knockBack;
        this.addAttackerModifiers(damager);
    }

    public EntityDamageByEntityEvent(Entity damager, Entity entity, DamageCause cause, Map<DamageModifier, Float> modifiers, float knockBack) {
        this(damager, entity, cause, modifiers, knockBack, Enchantment.EMPTY_ARRAY);
    }

    public EntityDamageByEntityEvent(Entity damager, Entity entity, DamageCause cause, Map<DamageModifier, Float> modifiers, float knockBack, Enchantment[] enchantments) {
        super(entity, cause, modifiers);
        this.damager = damager;
        this.knockBack = knockBack;
        this.enchantments = enchantments;
        this.addAttackerModifiers(damager);
    }

    protected void addAttackerModifiers(Entity damager) {
        Effect strength = damager.getEffect(Effect.STRENGTH);
        if (strength != null) {
            this.setDamage((float) (this.getDamage(DamageModifier.BASE) * 0.3 * (strength.getAmplifier() + 1)), DamageModifier.STRENGTH);
        }

        Effect weakness = damager.getEffect(Effect.WEAKNESS);
        if (weakness != null) {
            this.setDamage(-(float) (this.getDamage(DamageModifier.BASE) * 0.2 * (weakness.getAmplifier() + 1)), DamageModifier.WEAKNESS);
        }
    }

    public Entity getDamager() {
        return damager;
    }

    public float getKnockBack() {
        return knockBack;
    }

    public void setKnockBack(float knockBack) {
        this.knockBack = knockBack;
    }

    public Enchantment[] getWeaponEnchantments() {
        return enchantments;
    }
}
