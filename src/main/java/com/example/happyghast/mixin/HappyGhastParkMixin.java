package com.example.happyghast.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.example.happyghast.HappyGhastParkMod;

import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.HappyGhastEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.Random;
import net.minecraft.particle.ParticleTypes;

import net.minecraft.storage.WriteView;
import net.minecraft.storage.ReadView;

@Mixin(HappyGhastEntity.class)
public abstract class HappyGhastParkMixin {

    @Unique private boolean hgParked = false;    // estado “sentado”
    @Unique private boolean hgSprint = false;    // sprint del jinete (latched desde input)
    @Unique private int     hgDbgTick = 0;       // contador de logs
    @Unique private Double  hgBaseFly = null;    // cache del FLYING_SPEED base

    // ===== Persistencia =====
    @Inject(method = "writeCustomData", at = @At("TAIL"))
    private void bhg$write(WriteView view, CallbackInfo ci) {
        view.putInt("BHGParked", this.hgParked ? 1 : 0);
        if (this.hgBaseFly != null) view.putDouble("BHGBaseFly", this.hgBaseFly);
    }

    @Inject(method = "readCustomData", at = @At("TAIL"))
    private void bhg$read(ReadView view, CallbackInfo ci) {
        this.hgParked = view.getInt("BHGParked", 0) != 0;
        if (view.contains("BHGBaseFly")) this.hgBaseFly = view.getDouble("BHGBaseFly", 0.05);
    }

    // ===== Toggle con Blaze Rod (requiere harness) =====
    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void bhg$interact(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        HappyGhastEntity self = (HappyGhastEntity)(Object)this;
        ItemStack stack = player.getStackInHand(hand);

        if (stack.isOf(Items.BLAZE_ROD)) {
            if (!self.isWearingBodyArmor()) {
                player.sendMessage(Text.literal("This Happy Ghast needs a harness."), true);
                self.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 0.7f, 0.8f);
                cir.setReturnValue(ActionResult.SUCCESS);
                return; // evita montarte con la Blaze
            }

            this.hgParked = !this.hgParked;

            if (this.hgParked) {
                player.sendMessage(Text.literal("Happy Ghast parked."), true);
                self.setGlowing(true);
                self.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1.0f, 1.0f);

                if (self.getEntityWorld() instanceof ServerWorld sw) {
                    Vec3d p = new Vec3d(self.getX(), self.getY() + self.getHeight()*0.5, self.getZ());
                    Random r = self.getRandom();
                    for (int i = 0; i < 20; i++) {
                        double dx = (r.nextDouble() - 0.5) * 1.2;
                        double dy = (r.nextDouble() - 0.5) * 0.8;
                        double dz = (r.nextDouble() - 0.5) * 1.2;
                        sw.spawnParticles(ParticleTypes.GLOW, p.x, p.y, p.z, 1, dx, dy, dz, 0.02);
                    }
                }
            } else {
                player.sendMessage(Text.literal("Happy Ghast released."), true);
                self.setGlowing(false);
                self.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1.0f, 0.5f);
            }

            if (HappyGhastParkMod.CONFIG != null && HappyGhastParkMod.CONFIG.debugLogs) {
                System.out.println(String.format(
                    "[HappyGhastPark] toggle -> parked=%s | cfg{parked=%.2f, sprint=%.2f, max=%.2f, logs=%s/%dt}",
                    this.hgParked,
                    HappyGhastParkMod.CONFIG.parkedSpeedMultiplier,
                    HappyGhastParkMod.CONFIG.sprintMultiplier,
                    HappyGhastParkMod.CONFIG.maxSpeed,
                    HappyGhastParkMod.CONFIG.debugLogs,
                    HappyGhastParkMod.CONFIG.debugEveryTicks
                ));
            }

            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }
    }

    // ===== Capturar sprint del jinete desde el input controlado =====
    // (Solo guardamos el flag; la aceleración real se aplica en tick con atributo)
    @Inject(method = "getControlledMovementInput", at = @At("HEAD"))
    private void bhg$captureSprint(PlayerEntity controllingPlayer, Vec3d movementInput, CallbackInfoReturnable<Vec3d> cir) {
        HappyGhastEntity self = (HappyGhastEntity)(Object)this;
        if (this.hgParked && self.hasPassengers()) {
            this.hgSprint = controllingPlayer != null && controllingPlayer.isSprinting();
        } else {
            this.hgSprint = false;
        }
    }

    // ===== “Quieto” cuando está sentado sin jinete =====
    @Inject(method = "isStill", at = @At("HEAD"), cancellable = true)
    private void bhg$isStill(CallbackInfoReturnable<Boolean> cir) {
        if (this.hgParked) {
            HappyGhastEntity self = (HappyGhastEntity)(Object)this;
            cir.setReturnValue(!self.hasPassengers());
        }
    }

    // ===== Tick: glowing + boost de atributo + clamp + logs =====
    @Inject(method = "tick", at = @At("TAIL"))
    private void bhg$tick(CallbackInfo ci) {
        HappyGhastEntity self = (HappyGhastEntity)(Object)this;

        // glowing según estado
        if (this.hgParked) {
            if (!self.isGlowing()) self.setGlowing(true);
        } else if (self.isGlowing()) {
            self.setGlowing(false);
        }

        // FLYING_SPEED base
        EntityAttributeInstance fly = self.getAttributeInstance(EntityAttributes.FLYING_SPEED);
        if (fly != null && this.hgBaseFly == null) {
            this.hgBaseFly = fly.getBaseValue();
            if (HappyGhastParkMod.CONFIG != null && HappyGhastParkMod.CONFIG.debugLogs) {
                System.out.println(String.format(
                    "[HappyGhastPark] init base fly=%.4f | cfg{parked=%.2f, sprint=%.2f, max=%.2f}",
                    this.hgBaseFly,
                    HappyGhastParkMod.CONFIG.parkedSpeedMultiplier,
                    HappyGhastParkMod.CONFIG.sprintMultiplier,
                    HappyGhastParkMod.CONFIG.maxSpeed
                ));
            }
        }

        boolean hasPassenger = self.hasPassengers();

        if (this.hgParked && hasPassenger) {
            // multiplicador total
            double parkedMul = (HappyGhastParkMod.CONFIG != null ? HappyGhastParkMod.CONFIG.parkedSpeedMultiplier : 2.0);
            double sprintMul = (HappyGhastParkMod.CONFIG != null ? HappyGhastParkMod.CONFIG.sprintMultiplier : 1.25);
            double totalMul  = parkedMul * (this.hgSprint ? sprintMul : 1.0);

            if (fly != null && this.hgBaseFly != null) {
                double before = fly.getBaseValue();
                double target = this.hgBaseFly * totalMul;
                if (before != target) fly.setBaseValue(target);

                if (HappyGhastParkMod.CONFIG != null && HappyGhastParkMod.CONFIG.debugLogs) {
                    System.out.println(String.format(
                        "[HappyGhastPark] attr: parked=%s sprint=%s | baseFly=%.4f -> target=%.4f (mul=%.2f=%.2f%s%.2f)",
                        this.hgParked, this.hgSprint, this.hgBaseFly, target, totalMul, parkedMul,
                        (this.hgSprint ? "x" : ""), (this.hgSprint ? sprintMul : 1.0)
                    ));
                }
            }

            // Clamp de velocidad real
            double max = (HappyGhastParkMod.CONFIG != null ? HappyGhastParkMod.CONFIG.maxSpeed : 1.6);
            Vec3d v = self.getVelocity();
            double speed = v.length();
            boolean clamped = false;
            if (speed > max && speed > 1.0E-4) {
                self.setVelocity(v.multiply(max / speed));
                clamped = true;
            }

            if (HappyGhastParkMod.CONFIG != null && HappyGhastParkMod.CONFIG.debugLogs) {
                int every = Math.max(1, HappyGhastParkMod.CONFIG.debugEveryTicks);
                if (++hgDbgTick >= every) {
                    hgDbgTick = 0;
                    Vec3d vv = self.getVelocity();
                    System.out.println(String.format(
                        "[HappyGhastPark] tick: parked=%s sprint=%s speed=%.3f max=%.3f clamped=%s | pos=(%.2f, %.2f, %.2f) vel=(%.3f, %.3f, %.3f)",
                        this.hgParked, this.hgSprint, vv.length(), max, clamped,
                        self.getX(), self.getY(), self.getZ(),
                        vv.x, vv.y, vv.z
                    ));
                }
            }
        } else {
            // Restaurar atributo al salir del estado
            if (fly != null && this.hgBaseFly != null && fly.getBaseValue() != this.hgBaseFly) {
                fly.setBaseValue(this.hgBaseFly);
                if (HappyGhastParkMod.CONFIG != null && HappyGhastParkMod.CONFIG.debugLogs) {
                    System.out.println(String.format("[HappyGhastPark] restore: baseFly=%.4f", this.hgBaseFly));
                }
            }
            this.hgSprint = false;
        }
    }
}
