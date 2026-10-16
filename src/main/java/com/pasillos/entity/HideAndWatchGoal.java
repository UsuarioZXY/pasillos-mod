package com.pasillos.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Comportamiento unico del Asechador:
 * - Siempre gira la cabeza hacia el jugador mas cercano.
 * - Busca un sitio desde el que el jugador no lo vea (detras de un arbol, de una colina...) a 12-26 bloques.
 * - Si el jugador le mira directamente se queda quieto; si lo sigue mirando, huye a otro escondite
 *   y, si aun asi lo mantiene a la vista, se desvanece.
 * - Nunca ataca.
 */
public class HideAndWatchGoal extends Goal {

    private static final double WATCH_RANGE = 48.0D;
    private static final double MIN_DIST = 12.0D;
    private static final double MAX_DIST = 26.0D;
    private static final int FREEZE_TICKS = 30;
    private static final int VANISH_TICKS = 90;

    private final Asechador mob;
    private Player player;
    private int seenTicks;
    private int repathCooldown;

    public HideAndWatchGoal(Asechador mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        this.player = this.mob.level().getNearestPlayer(this.mob, WATCH_RANGE);
        return this.player != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.player != null && this.player.isAlive() && !this.player.isSpectator()
                && this.mob.distanceTo(this.player) <= WATCH_RANGE + 8.0D;
    }

    @Override
    public void start() {
        this.seenTicks = 0;
        this.repathCooldown = 0;
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
        this.player = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.player == null) {
            return;
        }
        this.mob.getLookControl().setLookAt(this.player, 40.0F, 40.0F);

        double dist = this.mob.distanceTo(this.player);
        boolean seen = isLookedAtBy(this.player);

        if (seen) {
            this.seenTicks++;
            if (dist < 6.0D || this.seenTicks > VANISH_TICKS) {
                this.mob.vanish();
                return;
            }
            if (this.seenTicks <= FREEZE_TICKS) {
                this.mob.getNavigation().stop();
                return;
            }
            flee();
            return;
        }

        if (this.seenTicks > 0) {
            this.seenTicks--;
        }

        boolean hidden = !this.player.hasLineOfSight(this.mob);
        if (hidden && dist >= MIN_DIST && dist <= MAX_DIST) {
            this.mob.getNavigation().stop();
            return;
        }

        if (--this.repathCooldown <= 0) {
            this.repathCooldown = 15;
            Vec3 spot = findHiddenSpot();
            if (spot != null) {
                this.mob.getNavigation().moveTo(spot.x, spot.y, spot.z, 1.0D);
            }
        }
    }

    private void flee() {
        if (--this.repathCooldown > 0) {
            return;
        }
        this.repathCooldown = 10;
        Vec3 spot = findHiddenSpot();
        if (spot != null) {
            this.mob.getNavigation().moveTo(spot.x, spot.y, spot.z, 1.5D);
        }
    }

    private boolean isLookedAtBy(Player p) {
        Vec3 eye = p.getEyePosition();
        Vec3 toMob = this.mob.getEyePosition().subtract(eye);
        double len = toMob.length();
        if (len < 0.001D) {
            return true;
        }
        double dot = p.getViewVector(1.0F).normalize().dot(toMob.scale(1.0D / len));
        return dot > 0.97D && p.hasLineOfSight(this.mob);
    }

    /** Un punto cercano al suelo desde el que el jugador no tiene linea de vision, a 12-26 bloques de el. */
    private Vec3 findHiddenSpot() {
        RandomSource random = this.mob.getRandom();
        Vec3 eye = this.player.getEyePosition();

        for (int i = 0; i < 24; i++) {
            int x = (int) Math.floor(this.mob.getX() + (random.nextDouble() - 0.5D) * 20.0D);
            int z = (int) Math.floor(this.mob.getZ() + (random.nextDouble() - 0.5D) * 20.0D);
            int y = this.mob.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos feet = new BlockPos(x, y, z);

            if (!this.mob.level().getFluidState(feet.below()).isEmpty()) {
                continue;
            }
            double d = Math.sqrt(feet.distToCenterSqr(this.player.getX(), this.player.getY(), this.player.getZ()));
            if (d < MIN_DIST || d > MAX_DIST) {
                continue;
            }
            Vec3 head = new Vec3(x + 0.5D, y + 1.7D, z + 0.5D);
            HitResult hit = this.mob.level().clip(new ClipContext(eye, head,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.player));
            if (hit.getType() == HitResult.Type.MISS) {
                continue; // desde ahi si lo verian
            }
            return Vec3.atBottomCenterOf(feet);
        }
        return null;
    }
}
