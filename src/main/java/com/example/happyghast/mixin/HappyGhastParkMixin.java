package com.example.happyghast.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.example.happyghast.HappyGhastParkMod;
import com.example.happyghast.HappyGhastSprintable;

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
public abstract class HappyGhastParkMixin implements HappyGhastSprintable {

    @Unique private boolean hgParked = false;    // estado "sentado"
    @Unique private boolean hgSprint = false;    // sprint del jinete (latched desde input) - deprecated
    @Unique private boolean hgCustomSprint = false;  // custom sprint flag for keybinding
    @Unique private int     hgDbgTick = 0;       // contador de logs
    @Unique private Double  hgBaseFly = null;    // cache del FLYING_SPEED base

    @Unique
    private net.minecraft.particle.ParticleEffect bhg$themeParticle() {
        String theme = (HappyGhastParkMod.CONFIG != null ? HappyGhastParkMod.CONFIG.particleTheme : "villager");
        if ("soul".equalsIgnoreCase(theme)) {
            return ParticleTypes.SOUL_FIRE_FLAME;
        } else if ("endrod".equalsIgnoreCase(theme)) {
            return ParticleTypes.END_ROD;
        } else {
            // default
            return ParticleTypes.HAPPY_VILLAGER;
        }
    }

    // Implementation of HappyGhastSprintable interface
    @Unique
    public boolean isCustomSprinting() {
        return this.hgCustomSprint;
    }

    @Unique
    public void setCustomSprinting(boolean sprinting) {
        this.hgCustomSprint = sprinting;
    }

    @Unique
    private void bhg$spawnGroundMark(ServerWorld sw, HappyGhastEntity self, int points, double radius) {
        // Posición base: justo por debajo del hitbox
        var box = self.getBoundingBox();
        double centerX = self.getX();
        double centerZ = self.getZ();
        double baseY   = box.minY - 0.05;

        // Intentar “apoyar” la marca en el primer bloque sólido bajo el ghast (1–3 bloques)
        double y = baseY;
        var pos = net.minecraft.util.math.BlockPos.ofFloored(centerX, box.minY - 0.1, centerZ);
        for (int i = 0; i < 3; i++) {
            var below = pos.down(i + 1);
            var bs = sw.getBlockState(below);
            if (!bs.isAir()) {
                y = below.getY() + 0.02; // apenas por encima para evitar z-fighting
                break;
            }
        }

        var type = bhg$themeParticle();
        // Dibujar anillo
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 / points) * i + (self.age % 20) * 0.15;
            double x = centerX + Math.cos(angle) * radius;
            double z = centerZ + Math.sin(angle) * radius;
            // Spread mínimo para “chisporroteo” suave
            sw.spawnParticles(type, x, y, z, 1, 0.02, 0.01, 0.02, 0.0);
        }
    }


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
        HappyGhastEntity self = (HappyGhastEntity)(Object)this;
            String vm = (HappyGhastParkMod.CONFIG != null ? HappyGhastParkMod.CONFIG.visualMode : "particles");
            if (!"glow".equalsIgnoreCase(vm) && self.isGlowing()) {
                self.setGlowing(false);
        }

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

                String vm = (HappyGhastParkMod.CONFIG != null ? HappyGhastParkMod.CONFIG.visualMode : "particles");
                if ("glow".equalsIgnoreCase(vm)) {
                    self.setGlowing(true);
                } else {
                    if (self.isGlowing()) self.setGlowing(false);
                }

                self.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1.0f, 1.0f);

                if ("particles".equalsIgnoreCase(vm) && self.getEntityWorld() instanceof ServerWorld sw) {
                    String style = (HappyGhastParkMod.CONFIG != null ? HappyGhastParkMod.CONFIG.particleStyle : "ground_mark");
                    if ("ground_mark".equalsIgnoreCase(style)) {
                        // Marca en el suelo: burst inicial más denso
                        double radius = Math.max(self.getWidth() * 0.55, 0.9);
                        for (int r = 0; r < 2; r++) {
                            bhg$spawnGroundMark(sw, self, 20, radius + r * 0.15);
                        }
                    } else {
                        // “ring” clásico (alrededor del cuerpo, debajo)
                        var box = self.getBoundingBox();
                        double y = box.minY + 0.2;
                        double radius = Math.max(self.getWidth() * 0.55, 0.9);
                        var type = bhg$themeParticle();
                        for (int i = 0; i < 24; i++) {
                            double angle = (Math.PI * 2.0 / 24.0) * i;
                            double x = self.getX() + Math.cos(angle) * radius;
                            double z = self.getZ() + Math.sin(angle) * radius;
                            sw.spawnParticles(type, x, y, z, 1, 0.0, 0.01, 0.0, 0.0);
                        }
                    }
                }
            } else {
                player.sendMessage(Text.literal("Happy Ghast released."), true);
                String vm = (HappyGhastParkMod.CONFIG != null ? HappyGhastParkMod.CONFIG.visualMode : "particles");
                if (!"glow".equalsIgnoreCase(vm) && self.isGlowing()) self.setGlowing(false);
                if ("glow".equalsIgnoreCase(vm)) self.setGlowing(false);
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

        // Visual según estado y config
        String vm = (HappyGhastParkMod.CONFIG != null ? HappyGhastParkMod.CONFIG.visualMode : "particles");
        if (!"glow".equalsIgnoreCase(vm) && self.isGlowing()) {
            self.setGlowing(false);
        }

        if ("glow".equalsIgnoreCase(vm)) {
            if (this.hgParked) {
                if (!self.isGlowing()) self.setGlowing(true);
            } else if (self.isGlowing()) {
                self.setGlowing(false);
            }
        } else if ("particles".equalsIgnoreCase(vm)) {
            if (this.hgParked && self.getEntityWorld() instanceof ServerWorld sw && (self.age % 10) == 0) {
                String style = (HappyGhastParkMod.CONFIG != null ? HappyGhastParkMod.CONFIG.particleStyle : "ground_mark");
                double radius = Math.max(self.getWidth() * 0.55, 0.9);

                if ("ground_mark".equalsIgnoreCase(style)) {
                    // marca en el suelo “viva”
                    bhg$spawnGroundMark(sw, self, 12, radius);
                } else {
                    // anillo bajo el cuerpo
                    var box = self.getBoundingBox();
                    double y = box.minY + 0.2;
                    var type = bhg$themeParticle();
                    int points = 8;
                    for (int i = 0; i < points; i++) {
                        double angle = (Math.PI * 2.0 / points) * i + (self.age % 20) * 0.1;
                        double x = self.getX() + Math.cos(angle) * radius;
                        double z = self.getZ() + Math.sin(angle) * radius;
                        sw.spawnParticles(type, x, y, z, 1, 0.02, 0.01, 0.02, 0.0);
                    }
                }
            }
        } else {
            if (self.isGlowing()) self.setGlowing(false);
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
            // multiplicador total - use custom sprint flag instead of the old player.isSprinting() based flag
            double parkedMul = (HappyGhastParkMod.CONFIG != null ? HappyGhastParkMod.CONFIG.parkedSpeedMultiplier : 2.0);
            double sprintMul = (HappyGhastParkMod.CONFIG != null ? HappyGhastParkMod.CONFIG.sprintMultiplier : 1.25);
            double totalMul  = parkedMul * (this.hgCustomSprint ? sprintMul : 1.0);

            if (fly != null && this.hgBaseFly != null) {
                double before = fly.getBaseValue();
                double target = this.hgBaseFly * totalMul;
                if (before != target) fly.setBaseValue(target);

                if (HappyGhastParkMod.CONFIG != null && HappyGhastParkMod.CONFIG.debugLogs) {
                    System.out.println(String.format(
                        "[HappyGhastPark] attr: parked=%s sprint=%s | baseFly=%.4f -> target=%.4f (mul=%.2f=%.2f%s%.2f)",
                        this.hgParked, this.hgCustomSprint, this.hgBaseFly, target, totalMul, parkedMul,
                        (this.hgCustomSprint ? "x" : ""), (this.hgCustomSprint ? sprintMul : 1.0)
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
                        this.hgParked, this.hgCustomSprint, vv.length(), max, clamped,
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
            this.hgCustomSprint = false;
        }
    }
}
