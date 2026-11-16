package dev.marston.randomloot.fabric;

/**
 * Every LivingEntity, by way of LivingEntityMixin: somewhere to keep what the hit in
 * progress is about to take.
 *
 * <p>The DAMAGE_POST hook wants that amount at the end of hurtServer, and no Fabric API
 * event has it (AFTER_DAMAGE reports the hit before armor). The shim's DAMAGE_PRE
 * stand-in already holds the number, so it leaves it here for the DAMAGE_POST one. It
 * is an interface because Player has its own actuallyHurt: PlayerMixin sets the value
 * through it.
 */
public interface DamageTakenCarrier {

    void randomloot$setDamageTaken(float damage);
}
