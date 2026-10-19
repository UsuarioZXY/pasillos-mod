package com.pasillos.entity;

import com.pasillos.event.NoiseSystem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * El Ciego no ve: oye. Escucha a los jugadores a (ruido x 1,3) bloques (y siempre a menos de 3), va hacia
 * donde sono el ultimo ruido y, si te encuentra pegado a el, te muerde. Cuando pierde el rastro
 * se queda un rato olfateando por la zona y luego se rinde.
 */
public class ListenAndHuntGoal extends Goal {

    private static final double MIN_HEARING = 3.0D;
    private static final int GIVE_UP_TICKS = 140;

    private final Ciego mob;
    private Player player;
    private Vec3 lastHeard;
    private int lostTicks;
    private int attackCooldown;
    private int repath;

    public ListenAndHuntGoal(Ciego mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private Player hearNearest() {
        Player best = null;
        double bestDist = Double.MAX_VALUE;
        for (Player p : this.mob.level().players()) {
            if (p.isSpectator() || p.isCreative() || !p.isAlive()) {
                continue;
            }
            double d = this.mob.distanceTo(p);
            double hearing = Math.max(MIN_HEARING, NoiseSystem.get(p) * 1.3D);
            if (d <= hearing && d < bestDist) {
                best = p;
                bestDist = d;
            }
        }
        return best;
    }

    @Override
    public boolean canUse() {
        if (this.mob.tickCount % 5 != 0) {
            return false;
        }
        this.player = hearNearest();
        return this.player != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.lostTicks < GIVE_UP_TICKS && this.lastHeard != null;
    }

    @Override
    public void start() {
        this.lastHeard = this.player.position();
        this.lostTicks = 0;
        this.repath = 0;
        this.mob.setHunting(true);
    }

    @Override
    public void stop() {
        this.mob.setHunting(false);
        this.mob.getNavigation().stop();
        this.player = null;
        this.lastHeard = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
        Player heard = this.mob.tickCount % 4 == 0 ? hearNearest() : null;
        if (heard != null) {
            this.player = heard;
            this.lastHeard = heard.position();
            this.lostTicks = 0;
        } else if (this.mob.tickCount % 4 == 0) {
            this.lostTicks += 4;
        }

        // mordisco: solo si esta pegado (huele, no ve)
        if (this.player != null && this.player.isAlive() && this.mob.distanceTo(this.player) < 2.2F
                && this.attackCooldown <= 0) {
            this.attackCooldown = 24;
            this.mob.swing(InteractionHand.MAIN_HAND);
            this.mob.doHurtTarget(this.player);
            this.lostTicks = 0;
        }

        double dist = this.mob.position().distanceTo(this.lastHeard);
        if (dist > 1.5D) {
            if (--this.repath <= 0) {
                this.repath = 8;
                double loud = this.player != null ? NoiseSystem.get(this.player) : 0.0F;
                double speed = loud >= 38.0D ? 1.7D : 1.25D;
                this.mob.getNavigation().moveTo(this.lastHeard.x, this.lastHeard.y, this.lastHeard.z, speed);
            }
        } else {
            // llego al sitio y no hay nadie: olfatea
            this.mob.getNavigation().stop();
            this.lostTicks += 1;
        }
    }
}
