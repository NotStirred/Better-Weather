package corgitaco.betterweather.mixin.entity;

import corgitaco.betterweather.BetterWeather;
import corgitaco.betterweather.BetterWeatherUtil;
import corgitaco.betterweather.datastorage.BetterWeatherSeasonData;
import corgitaco.betterweather.season.SeasonSystem;
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
        if (BetterWeather.useSeasons && BetterWeatherUtil.isOverworld(this.world.getDimensionKey())) {
            if (SeasonSystem.getSubSeasonFromTime(BetterWeatherSeasonData.get(this.world).getSeasonTime(), this.world, BetterWeatherSeasonData.get(this.world).getSeasonCycleLength()).getEntityTypeBreedingBlacklist().contains(this.animal.getType()))
                cir.setReturnValue(false);
        }
    }
}
