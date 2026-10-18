package com.pasillos.entity;

import com.pasillos.event.NoiseSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Comportamiento unico del Asechador. Nunca ataca.
 *
 * Se mantiene entre 22 y 42 bloques del jugador, siempre con la cabeza girada hacia el, y cambia de
 * escondite cada 10-25 segundos usando uno de tres estilos:
 *  - COVER: detras de un arbol o pilar; cuando el jugador mira a otro lado se asoma por el lateral.
 *  - CROUCH: agazapado, mucho mas bajo, asi cabe detras de cosas mas pequenas.
 *  - STATUE: inmovil a campo abierto y lejos, como si fuera parte del paisaje.
 *
 * Si el jugador puede verlo (lo mira de frente, con linea de vision y dentro de su alcance de vision:
 * mucho de dia, unos 30 bloques de noche por la niebla), se queda helado y a los 2 segundos se desvanece.
 * Si el jugador se acerca a menos de 12 bloques tambien se desvanece, y por debajo de 22 se aleja.
 */
public class HideAndWatchGoal extends Goal {

    private static final double WATCH_RANGE = 64.0D;
    private static final double MIN_DIST = 22.0D;
    private static final double MAX_DIST = 42.0D;
    private static final double SPOOK_DIST = 12.0D;
    private static final int VANISH_TICKS = 40;
    private static final double STAND_EYE = 3.2D;
    private static final double CROUCH_EYE = 1.5D;

    private enum Style { COVER, CROUCH, STATUE }

    private final Asechador mob;
    private Player player;
    private Style style = Style.COVER;
    private int seenTicks;
    private int repathCooldown;
    private int relocateTimer;
    /** Ticks que lleva "alerta" por haber oido ruido: se acerca mas (a 14-30 bloques). */
    private int alertTicks;

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
        this.relocateTimer = 200 + this.mob.getRandom().nextInt(300);
        pickStyle();
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
        this.mob.setState(Asechador.STATE_WATCH);
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

        // --- oye el ruido del jugador: se pone alerta y se acerca un poco ---
        float noise = NoiseSystem.get(this.player);
        if (noise > 4.0F && dist < noise * 0.9D) {
            if (this.alertTicks <= 0) {
                this.relocateTimer = 0; // se reposiciona enseguida
            }
            this.alertTicks = 120;
        } else if (this.alertTicks > 0) {
            this.alertTicks--;
        }

        // --- el jugador lo ve: se congela y desaparece ---
        if (isLookedAtBy(this.player, dist)) {
            this.seenTicks++;
            if (dist < SPOOK_DIST || this.seenTicks > VANISH_TICKS) {
                this.mob.vanish();
                return;
            }
            this.mob.getNavigation().stop();
            this.mob.setState(Asechador.STATE_STARE);
            return;
        }
        if (this.seenTicks > 0) {
            this.seenTicks--;
        }

        // --- esta bien colocado: se queda quieto ---
        boolean hidden = !this.player.hasLineOfSight(this.mob);
        boolean inRange = dist >= minDist() && dist <= maxDist();
        boolean placed = inRange && (this.style == Style.STATUE || hidden);
        this.relocateTimer--;

        if (placed && this.relocateTimer > 0) {
            this.mob.getNavigation().stop();
            this.mob.setState(stillState(hidden));
            return;
        }

        // --- hay que moverse: cambio de escondite o el jugador se acerco ---
        if (this.relocateTimer <= 0) {
            this.relocateTimer = 200 + this.mob.getRandom().nextInt(300);
            pickStyle();
        }
        if (--this.repathCooldown <= 0) {
            this.repathCooldown = 15;
            double speed = dist < SPOOK_DIST + 8.0D ? 1.6D : 1.0D;
            Vec3 spot = findSpot();
            if (spot != null) {
                this.mob.getNavigation().moveTo(spot.x, spot.y, spot.z, speed);
            } else if (dist < MIN_DIST) {
                retreat(speed);
            }
        }
        this.mob.setState(this.style == Style.CROUCH ? Asechador.STATE_CROUCH : Asechador.STATE_WATCH);
    }

    // ------------------------------------------------------------------------------------------

    private double minDist() {
        return this.alertTicks > 0 ? 14.0D : MIN_DIST;
    }

    private double maxDist() {
        return this.alertTicks > 0 ? 30.0D : MAX_DIST;
    }

    private void pickStyle() {
        int roll = this.mob.getRandom().nextInt(100);
        this.style = roll < 45 ? Style.COVER : (roll < 75 ? Style.CROUCH : Style.STATUE);
    }

    private int stillState(boolean hidden) {
        switch (this.style) {
            case CROUCH:
                return Asechador.STATE_CROUCH;
            case COVER:
                // se asoma solo si esta tapado y el jugador no esta mirando hacia donde se esconde
                return hidden && facing(this.player) < 0.55D ? Asechador.STATE_PEEK : Asechador.STATE_WATCH;
            default:
                return Asechador.STATE_WATCH;
        }
    }

    /** Hasta donde ve el jugador: mucho de dia; de noche la niebla lo limita a unos 30 bloques. */
    private double visibleRange() {
        return Ground.isDark(this.mob.level()) ? 30.0D : 64.0D;
    }

    /** Coseno del angulo entre la mirada del jugador y la direccion hacia el Asechador. */
    private double facing(Player p) {
        Vec3 toMob = this.mob.getEyePosition().subtract(p.getEyePosition());
        double len = toMob.length();
        if (len < 0.001D) {
            return 1.0D;
        }
        return p.getViewVector(1.0F).normalize().dot(toMob.scale(1.0D / len));
    }

    private boolean isLookedAtBy(Player p, double dist) {
        return dist <= visibleRange() && facing(p) > 0.97D && p.hasLineOfSight(this.mob);
    }

    /** Un punto cercano desde el que el jugador no lo veria (o, en estilo estatua, un punto lejano a la vista). */
    private Vec3 findSpot() {
        RandomSource random = this.mob.getRandom();
        Vec3 eye = this.player.getEyePosition();
        double eyeHeight = this.style == Style.CROUCH ? CROUCH_EYE : STAND_EYE;
        double minDist = this.style == Style.STATUE ? maxDist() - 10.0D : minDist();

        for (int i = 0; i < 28; i++) {
            int x = Mth.floor(this.mob.getX() + (random.nextDouble() - 0.5D) * 30.0D);
            int z = Mth.floor(this.mob.getZ() + (random.nextDouble() - 0.5D) * 30.0D);
            if (!this.mob.level().hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            int y = Ground.standY(this.mob.level(), x, z, this.mob.getBlockY());
            if (y == Ground.NONE) {
                continue;
            }
            BlockPos feet = new BlockPos(x, y, z);

            if (!this.mob.level().getFluidState(feet.below()).isEmpty()) {
                continue;
            }
            double d = Math.sqrt(feet.distToCenterSqr(this.player.getX(), this.player.getY(), this.player.getZ()));
            if (d < minDist || d > maxDist()) {
                continue;
            }
            if (this.style != Style.STATUE) {
                Vec3 head = new Vec3(x + 0.5D, y + eyeHeight, z + 0.5D);
                HitResult hit = this.mob.level().clip(new ClipContext(eye, head,
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.player));
                if (hit.getType() == HitResult.Type.MISS) {
                    continue; // desde ahi si lo verian
                }
            }
            return Vec3.atBottomCenterOf(feet);
        }
        return null;
    }

    /** Sin escondite a mano, al menos se aleja del jugador. */
    private void retreat(double speed) {
        Vec3 away = this.mob.position().subtract(this.player.position()).multiply(1.0D, 0.0D, 1.0D);
        if (away.lengthSqr() < 0.0001D) {
            return;
        }
        Vec3 target = this.mob.position().add(away.normalize().scale(14.0D));
        this.mob.getNavigation().moveTo(target.x, target.y, target.z, speed);
    }
}
