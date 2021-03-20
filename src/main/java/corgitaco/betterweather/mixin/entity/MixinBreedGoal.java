package corgitaco.betterweather.mixin.entity;

import corgitaco.betterweather.BetterWeather;
import corgitaco.betterweather.api.BetterWeatherWorldData;
import net.minecraft.entity.ai.goal.BreedGoal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BreedGoal.class)
public abstract class MixinBreedGoal {


    @Shadow
    @Final
    protected AnimalEntity animal;

    @Shadow
    @Final
    protected World world;

    @Inject(method = "shouldExecute", at = @At("HEAD"), cancellable = true)
    private void seasonBreeding(CallbackInfoReturnable<Boolean> cir) {
        if (((BetterWeatherWorldData) world).getSeasonContext() != null) {
            if (((BetterWeatherWorldData) world).getSeasonContext().getCurrentSubSeasonSettings().getEntityTypeBreedingBlacklist().contains(this.animal.getType()))
                cir.setReturnValue(false);
        }
    }
}
