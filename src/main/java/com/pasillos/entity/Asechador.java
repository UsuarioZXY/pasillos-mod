package com.pasillos.entity;

import com.pasillos.event.AsechadorDirector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;

/**
 * El Asechador: una silueta altisima que nunca ataca. Se esconde de varias maneras, te observa desde
 * lejos y desaparece si lo miras demasiado, si te acercas o si intentas golpearlo. Vuelve a aparecer
 * mas tarde en otro sitio (lo gestiona AsechadorDirector).
 */
public class Asechador extends PathfinderMob {

    /** De pie, vigilando. */
    public static final int STATE_WATCH = 0;
    /** Asomandose por el lateral de su cobertura. */
    public static final int STATE_PEEK = 1;
    /** Agazapado, mucho mas bajo. */
    public static final int STATE_CROUCH = 2;
    /** Te ha pillado mirandolo: se queda rigido, con la boca abierta, justo antes de desvanecerse. */
    public static final int STATE_STARE = 3;

    private static final EntityDataAccessor<Integer> STATE =
            SynchedEntityData.defineId(Asechador.class, EntityDataSerializers.INT);
    private static final EntityDimensions CROUCH_SIZE = EntityDimensions.scalable(0.9F, 1.9F);

    // Solo del lado del cliente: cuanto de cada pose se ha alcanzado (0 a 1), para que el modelo
    // pase de una pose a otra poco a poco en vez de dar un salto.
    private float crouchAmount;
    private float crouchAmountO;
    private float peekAmount;
    private float peekAmountO;
    private float stareAmount;
    private float stareAmountO;

    /** Solo cliente: es una alucinacion (no existe en el servidor) y cuantos ticks le quedan. */
    private boolean hallucination;
    private int hallucinationLife;

    public Asechador(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public boolean isHallucination() {
        return this.hallucination;
    }

    public int getHallucinationLife() {
        return this.hallucinationLife;
    }

    public void setHallucinationLife(int ticks) {
        this.hallucinationLife = ticks;
    }

    /** Lo convierte en una alucinacion: solo la ve el jugador, no hace nada y se esfuma sola. */
    public void makeHallucination(int lifeTicks) {
        this.hallucination = true;
        this.hallucinationLife = lifeTicks;
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new HideAndWatchGoal(this));
    }

    // ------------------------------------------------------------------------------------------
    // Estado sincronizado (lo usan el modelo y la IA)
    // ------------------------------------------------------------------------------------------

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STATE, STATE_WATCH);
    }

    public int getState() {
        return this.entityData.get(STATE);
    }

    public void setState(int state) {
        if (this.getState() != state) {
            this.entityData.set(STATE, state);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (STATE.equals(key)) {
            this.refreshDimensions();
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return this.getState() == STATE_CROUCH ? CROUCH_SIZE : super.getDimensions(pose);
    }

    public float getCrouchAmount(float partialTick) {
        return Mth.lerp(partialTick, this.crouchAmountO, this.crouchAmount);
    }

    public float getPeekAmount(float partialTick) {
        return Mth.lerp(partialTick, this.peekAmountO, this.peekAmount);
    }

    public float getStareAmount(float partialTick) {
        return Mth.lerp(partialTick, this.stareAmountO, this.stareAmount);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            int state = this.getState();
            this.crouchAmountO = this.crouchAmount;
            this.peekAmountO = this.peekAmount;
            this.stareAmountO = this.stareAmount;
            this.crouchAmount += ((state == STATE_CROUCH ? 1.0F : 0.0F) - this.crouchAmount) * 0.18F;
            this.peekAmount += ((state == STATE_PEEK ? 1.0F : 0.0F) - this.peekAmount) * 0.14F;
            this.stareAmount += ((state == STATE_STARE ? 1.0F : 0.0F) - this.stareAmount) * 0.4F;
        }
    }

    // ------------------------------------------------------------------------------------------
    // Vida
    // ------------------------------------------------------------------------------------------

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            this.spawnAmbientParticles();
        } else if (this.tickCount % 20 == 0 && this.level().isDay()) {
            this.vanish(); // con la luz del dia se disuelve
        }
    }

    /** Ceniza que cae de su cuerpo, gotas de tinta negra y almas cuando se asoma. */
    private void spawnAmbientParticles() {
        RandomSource random = this.random;
        double height = this.getBbHeight();
        if (random.nextInt(3) == 0) {
            this.level().addParticle(ParticleTypes.ASH,
                    this.getX() + (random.nextDouble() - 0.5D) * 1.3D,
                    this.getY() + random.nextDouble() * height,
                    this.getZ() + (random.nextDouble() - 0.5D) * 1.3D,
                    0.0D, -0.02D, 0.0D);
        }
        if (random.nextInt(7) == 0) {
            this.level().addParticle(ParticleTypes.SQUID_INK,
                    this.getX() + (random.nextDouble() - 0.5D) * 0.6D,
                    this.getY() + height * 0.9D,
                    this.getZ() + (random.nextDouble() - 0.5D) * 0.6D,
                    0.0D, -0.03D, 0.0D);
        }
        if (this.getState() == STATE_PEEK && random.nextInt(4) == 0) {
            this.level().addParticle(ParticleTypes.SCULK_SOUL,
                    this.getX() + (random.nextDouble() - 0.5D) * 0.8D,
                    this.getY() + height * 0.85D,
                    this.getZ() + (random.nextDouble() - 0.5D) * 0.8D,
                    0.0D, 0.02D, 0.0D);
        }
        if (this.getState() == STATE_STARE) {
            // pillado: suelta humo negro por la boca y la ceniza se agita
            this.level().addParticle(ParticleTypes.LARGE_SMOKE,
                    this.getX() + (random.nextDouble() - 0.5D) * 0.3D,
                    this.getY() + height * 0.84D,
                    this.getZ() + (random.nextDouble() - 0.5D) * 0.3D,
                    0.0D, 0.03D, 0.0D);
        }
    }

    /** Se desvanece en humo y tinta. Mas tarde el director lo hara aparecer en otro lugar. */
    public void vanish() {
        if (this.level() instanceof ServerLevel serverLevel) {
            double mid = this.getY() + this.getBbHeight() * 0.5D;
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), mid, this.getZ(),
                    40, 0.5D, this.getBbHeight() * 0.35D, 0.5D, 0.02D);
            serverLevel.sendParticles(ParticleTypes.SQUID_INK, this.getX(), mid, this.getZ(),
                    25, 0.4D, this.getBbHeight() * 0.3D, 0.4D, 0.05D);
            serverLevel.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), mid, this.getZ(),
                    12, 0.4D, this.getBbHeight() * 0.3D, 0.4D, 0.04D);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.8F, 0.4F);
            AsechadorDirector.scheduleRespawn(serverLevel, this.position());
        }
        this.discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && source.getEntity() instanceof Player) {
            this.vanish();
            return false;
        }
        return super.hurt(source, amount);
    }

    /** Los brazos y la cabeza ladeada sobresalen del hitbox; sin esto desapareceria al borde de la pantalla. */
    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(1.5D, 0.0D, 1.5D);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return distanceToClosestPlayer > 64.0D;
    }

    /** Solo se usa si el bioma lo lista; en esta version lo controla AsechadorDirector. */
    public static boolean canSpawn(EntityType<Asechador> type, ServerLevelAccessor level, MobSpawnType reason,
                                   BlockPos pos, RandomSource random) {
        return Ground.isDark(level.getLevel())
                && level.getEntitiesOfClass(Asechador.class, new AABB(pos).inflate(96.0D)).isEmpty();
    }
}
