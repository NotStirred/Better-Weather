package corgitaco.betterweather.mixin.client;

import corgitaco.betterweather.api.Climate;
import corgitaco.betterweather.api.season.Season;
import corgitaco.betterweather.helpers.BetterWeatherWorldData;
import corgitaco.betterweather.helpers.BiomeModifier;
import corgitaco.betterweather.helpers.BiomeUpdate;
import corgitaco.betterweather.season.SeasonContext;
import corgitaco.betterweather.weatherevent.BWWeatherEventContext;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.BooleanSupplier;

@Mixin(ClientWorld.class)
public abstract class MixinClientWorld implements BetterWeatherWorldData, Climate, BiomeUpdate {

    @Shadow
    public abstract DynamicRegistries func_241828_r();

    @Nullable
    SeasonContext seasonContext;

    @Nullable
    private BWWeatherEventContext weatherContext;

    @Nullable
    @Override
    public SeasonContext getSeasonContext() {
        return this.seasonContext;
    }

    @Nullable
    @Override
    public SeasonContext setSeasonContext(SeasonContext seasonContext) {
        this.seasonContext = seasonContext;
        return this.seasonContext;
    }

    @Nullable
    @Override
    public BWWeatherEventContext getWeatherEventContext() {
        return this.weatherContext;
    }

    @Nullable
    @Override
    public BWWeatherEventContext setWeatherEventContext(BWWeatherEventContext weatherEventContext) {
        this.weatherContext = weatherEventContext;
        return this.weatherContext;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(BooleanSupplier hasTicksLeft, CallbackInfo ci) {
        if (this.seasonContext != null) {
            this.seasonContext.tick((ClientWorld) (Object) this);
        }
    }

    @Override
    public Season getSeason() {
        return this.seasonContext;
    }

    @Override
    public void updateBiomeData() {
        for (Map.Entry<RegistryKey<Biome>, Biome> entry : this.func_241828_r().getRegistry(Registry.BIOME_KEY).getEntries()) {
            Biome biome = entry.getValue();
            RegistryKey<Biome> biomeKey = entry.getKey();
            float seasonHumidityModifier = seasonContext == null ? 0.0F : (float) this.seasonContext.getCurrentSubSeasonSettings().getHumidityModifier(biomeKey);
            float seasonTemperatureModifier = seasonContext == null ? 0.0F : (float) this.seasonContext.getCurrentSubSeasonSettings().getTemperatureModifier(biomeKey);
            float weatherHumidityModifier = weatherContext == null ? 0.0F : (float) this.weatherContext.getCurrentWeatherEventSettings().getHumidityModifierAtPosition(null);
            float weatherTemperatureModifier = weatherContext == null ? 0.0F : (float) this.weatherContext.getCurrentWeatherEventSettings().getTemperatureModifierAtPosition(null);

            ((BiomeModifier) (Object) biome).setSeasonTempModifier(seasonTemperatureModifier);
            ((BiomeModifier) (Object) biome).setSeasonHumidityModifier(seasonHumidityModifier);
            ((BiomeModifier) (Object) biome).setWeatherTempModifier(weatherTemperatureModifier);
            ((BiomeModifier) (Object) biome).setWeatherHumidityModifier(weatherHumidityModifier);
        }
    }
}
