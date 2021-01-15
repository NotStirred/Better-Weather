package corgitaco.betterweather.mixin.client;

import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FogRenderer.class)
public class MixinFogRenderer {

    @Redirect(method = "updateFogColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;getRainStrength(F)F"))
    private static float doNotDarkenFogWithRainStrength(ClientWorld world, float delta) {
        return 0.0F;
    }
}
