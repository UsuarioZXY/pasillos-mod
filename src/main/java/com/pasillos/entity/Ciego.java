package com.pasillos.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * El Ciego: una criatura delgada y sin ojos (los tiene cosidos) que anda a cuatro patas, algo parecida a
 * un perro pero que no lo es. No ve nada: te caza por el ruido que haces (ver NoiseSystem). Si te quedas
 * en silencio, o agachado, pierde tu rastro. Si te alcanza, muerde fuerte.
 */
public class Ciego extends Monster {

    private static final EntityDataAccessor<Boolean> HUNTING =
            SynchedEntityData.defineId(Ciego.class, EntityDataSerializers.BOOLEAN);

    // Solo cliente: cuanto de "cazando" lleva (0 a 1) para que la pose cambie suave.
    private float huntAmount;
    private float huntAmountO;

    public Ciego(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ListenAndHuntGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.55D));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(HUNTING, false);
    }

    public boolean isHunting() {
        return this.entityData.get(HUNTING);
    }

    public void setHunting(boolean hunting) {
        this.entityData.set(HUNTING, hunting);
    }

    public float getHuntAmount(float partialTick) {
        return Mth.lerp(partialTick, this.huntAmountO, this.huntAmount);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.huntAmountO = this.huntAmount;
            this.huntAmount += ((this.isHunting() ? 1.0F : 0.0F) - this.huntAmount) * 0.2F;
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return distanceToClosestPlayer > 80.0D;
    }

    @Override
    public float getVoicePitch() {
        return 0.55F + this.random.nextFloat() * 0.15F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isHunting() ? SoundEvents.WOLF_GROWL : SoundEvents.WARDEN_SNIFF;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOLF_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOLF_DEATH;
    }
}
