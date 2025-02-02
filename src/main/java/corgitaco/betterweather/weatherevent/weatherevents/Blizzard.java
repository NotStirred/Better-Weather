package corgitaco.betterweather.weatherevent.weatherevents;

import com.mojang.blaze3d.systems.RenderSystem;
import corgitaco.betterweather.BetterWeather;
import corgitaco.betterweather.BetterWeatherClientUtil;
import corgitaco.betterweather.BetterWeatherUtil;
import corgitaco.betterweather.SoundRegistry;
import corgitaco.betterweather.api.weatherevent.BetterWeatherID;
import corgitaco.betterweather.api.weatherevent.WeatherEvent;
import corgitaco.betterweather.audio.MovingWeatherSound;
import corgitaco.betterweather.config.BetterWeatherConfig;
import corgitaco.betterweather.config.BetterWeatherConfigClient;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.profiler.IProfiler;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.client.event.EntityViewRenderEvent;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.Random;

public class Blizzard extends WeatherEvent {

    public static final Color SKY_COLOR = new Color(155, 155, 155);

    static int idx2 = 0;
    private final float[] rainSizeX = new float[1024];
    private final float[] rainSizeZ = new float[1024];

    public Blizzard() {
        super(new BetterWeatherID(BetterWeather.MOD_ID, "BLIZZARD"), 0.3);
        for (int i = 0; i < 32; ++i) {
            for (int j = 0; j < 32; ++j) {
                float f = (float) (j - 16);
                float f1 = (float) (i - 16);
                float f2 = MathHelper.sqrt(f * f + f1 * f1);
                this.rainSizeX[i << 5 | j] = -f1 / f2;
                this.rainSizeZ[i << 5 | j] = f / f2;
            }
        }
    }

    @Override
    public Color modifySkyColor(Color biomeColor, Color returnColor, @Nullable Color seasonTargetColor, float rainStrength) {
        return SKY_COLOR;
    }

    @Override
    public Color modifyFogColor(Color biomeColor, Color returnColor, @Nullable Color seasonTargetColor, float rainStrength) {
        return SKY_COLOR;
    }

    @Override
    public Color modifyCloudColor(Color returnColor, float rainStrength) {
        return SKY_COLOR;
    }

    @Override
    public float dayLightDarkness() {
        return 2.0F;
    }

    public static boolean doBlizzardsAffectDeserts(Biome biome) {
        if (!BetterWeatherConfig.doBlizzardsOccurInDeserts.get())
            return biome.getCategory() != Biome.Category.DESERT;
        else
            return true;
    }

    public static boolean doBlizzardsDestroyPlants(Material material) {
        if (BetterWeatherConfig.doBlizzardsDestroyPlants.get())
            return material == Material.PLANTS && material == Material.TALL_PLANTS && material == Material.OCEAN_PLANT;
        else
            return false;
    }

    public static void addSnowAndIce(Chunk chunk, World world, long worldTime) {
        ChunkPos chunkpos = chunk.getPos();
        int chunkXStart = chunkpos.getXStart();
        int chunkZStart = chunkpos.getZStart();
        IProfiler iprofiler = world.getProfiler();
        iprofiler.startSection("blizzard");
        BlockPos blockpos = world.getHeight(Heightmap.Type.MOTION_BLOCKING, world.getBlockRandomPos(chunkXStart, 0, chunkZStart, 15));
        Biome biome = world.getBiome(blockpos);
        if (world.isAreaLoaded(blockpos, 1)) {
            if (world.getWorldInfo().isRaining() && worldTime % BetterWeatherConfig.tickSnowAndIcePlaceSpeed.get() == 0 && biome.getCategory() != Biome.Category.NETHER && biome.getCategory() != Biome.Category.THEEND && biome.getCategory() != Biome.Category.NONE && doBlizzardsAffectDeserts(biome) && BetterWeatherConfig.spawnSnowAndIce.get()) {
                BlockState blockStateDown = world.getBlockState(blockpos.down());
                if (blockStateDown.getBlock() == Blocks.WATER && blockStateDown.getFluidState().getLevel() == 8) {
                    world.setBlockState(blockpos.down(), Blocks.ICE.getDefaultState());
                    return;
                }
                BlockState blockState = world.getBlockState(blockpos);
                if (doesSnowGenerate(world, blockpos) || doBlizzardsDestroyPlants(blockState.getMaterial())) {
                    world.setBlockState(blockpos, Blocks.SNOW.getDefaultState());
                    return;
                }
                    Block block = blockState.getBlock();

                    if (block == Blocks.SNOW && blockState.hasProperty(BlockStateProperties.LAYERS_1_8)) {
                        int snowLayerHeight = blockState.get(BlockStateProperties.LAYERS_1_8);

                        if (snowLayerHeight < 7) //Never layer to 8 and turn into a snow block
                            world.setBlockState(blockpos, block.getDefaultState().with(BlockStateProperties.LAYERS_1_8, snowLayerHeight + 1));
                    }
                }
            }
        iprofiler.endSection();
    }

    public static boolean doesSnowGenerate(IWorldReader worldIn, BlockPos pos) {
        if (worldIn.getLightFor(LightType.BLOCK, pos) < 10) {
            BlockState blockstate = worldIn.getBlockState(pos);
            return blockstate.isAir(worldIn, pos) && Blocks.SNOW.getDefaultState().isValidPosition(worldIn, pos);
        } else
            return false;
    }

    @Override
    public void worldTick(ServerWorld world, int tickSpeed, long worldTime) {
    }

    @Override
    public void clientTick(ClientWorld world, int tickSpeed, long worldTime, Minecraft mc) {
        BlizzardClient.blizzardClient(world, worldTime, mc, forcedRenderDistance());
    }


    @Override
    public boolean renderWeather(Minecraft mc, ClientWorld world, LightTexture lightTexture, int ticks, float partialTicks, double x, double y, double z) {
        float rainStrength = world.getRainStrength(partialTicks);
        lightTexture.enableLightmap();
        int floorX = MathHelper.floor(x);
        int floorY = MathHelper.floor(y);
        int floorZ = MathHelper.floor(z);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        RenderSystem.enableAlphaTest();
        RenderSystem.disableCull();
        RenderSystem.normal3f(0.0F, 1.0F, 0.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.defaultAlphaFunc();
        RenderSystem.enableDepthTest();
        int graphicsQuality = 5;
        if (Minecraft.isFancyGraphicsEnabled()) {
            graphicsQuality = 10;
        }

        RenderSystem.depthMask(Minecraft.isFabulousGraphicsEnabled());
        int i1 = -1;
        float ticksAndPartialTicks = (float) ticks + partialTicks;
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        BlockPos.Mutable blockPos = new BlockPos.Mutable();

        for (int graphicQualityZ = floorZ - graphicsQuality; graphicQualityZ <= floorZ + graphicsQuality; ++graphicQualityZ) {
            for (int graphicQualityX = floorX - graphicsQuality; graphicQualityX <= floorX + graphicsQuality; ++graphicQualityX) {
                int rainSizeIdx = (graphicQualityZ - floorZ + 16) * 32 + graphicQualityX - floorX + 16;
                //These 2 doubles control the size of rain particles.
                double rainSizeX = (double) this.rainSizeX[rainSizeIdx] * 0.5D;
                double rainSizeZ = (double) this.rainSizeZ[rainSizeIdx] * 0.5D;
                blockPos.setPos(graphicQualityX, 0, graphicQualityZ);
                Biome biome = world.getBiome(blockPos);
                int topPosY = mc.world.getHeight(Heightmap.Type.MOTION_BLOCKING, blockPos.getX(), blockPos.getZ());
                int floorYMinusGraphicsQuality = floorY - graphicsQuality;
                int floorYPlusGraphicsQuality = floorY + graphicsQuality;
                if (floorYMinusGraphicsQuality < topPosY) {
                    floorYMinusGraphicsQuality = topPosY;
                }

                if (floorYPlusGraphicsQuality < topPosY) {
                    floorYPlusGraphicsQuality = topPosY;
                }

                int posY2 = topPosY;
                if (topPosY < floorY) {
                    posY2 = floorY;
                }

                if (floorYMinusGraphicsQuality != floorYPlusGraphicsQuality) {
                    Random random = new Random(graphicQualityX * graphicQualityX * 3121 + graphicQualityX * 45238971 ^ graphicQualityZ * graphicQualityZ * 418711 + graphicQualityZ * 13761);
                    blockPos.setPos(graphicQualityX, floorYMinusGraphicsQuality, graphicQualityZ);

                    //This is rain rendering.
                    if (i1 != 1) {
                        if (i1 >= 0) {
                            tessellator.draw();
                        }

                        i1 = 1;
                        mc.getTextureManager().bindTexture(WorldRenderer.SNOW_TEXTURES);
                        bufferbuilder.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
                    }

                    float f7 = (float) (random.nextDouble() + (double) (ticksAndPartialTicks * (float) random.nextGaussian()) * 0.03D);
                    float fallSpeed = (float) (random.nextDouble() + (double) (ticksAndPartialTicks * (float) random.nextGaussian()) * 0.03D);
                    double d3 = (double) ((float) graphicQualityX + 0.5F) - x;
                    double d5 = (double) ((float) graphicQualityZ + 0.5F) - z;
                    float f9 = MathHelper.sqrt(d3 * d3 + d5 * d5) / (float) graphicsQuality;
                    float ticksAndPartialTicks0 = ((1.0F - f9 * f9) * 0.3F + 0.5F) * rainStrength;
                    blockPos.setPos(graphicQualityX, posY2, graphicQualityZ);
                    int k3 = WorldRenderer.getCombinedLight(world, blockPos);
                    int l3 = k3 >> 16 & '\uffff';
                    int i4 = (k3 & '\uffff') * 3;
                    int j4 = (l3 * 3 + 240) / 4;
                    int k4 = (i4 * 3 + 240) / 4;
                    if (Blizzard.doBlizzardsAffectDeserts(biome)) {
                        bufferbuilder.pos((double) graphicQualityX - x - rainSizeX + 0.5D + random.nextGaussian() * 2, (double) floorYPlusGraphicsQuality - y, (double) graphicQualityZ - z - rainSizeZ + 0.5D + random.nextGaussian()).tex(0.0F + f7, (float) floorYMinusGraphicsQuality * 0.25F - Math.abs(fallSpeed)).color(1.0F, 1.0F, 1.0F, ticksAndPartialTicks0).lightmap(k4, j4).endVertex();
                        bufferbuilder.pos((double) graphicQualityX - x + rainSizeX + 0.5D + random.nextGaussian() * 2, (double) floorYPlusGraphicsQuality - y, (double) graphicQualityZ - z + rainSizeZ + 0.5D + random.nextGaussian()).tex(1.0F + f7, (float) floorYMinusGraphicsQuality * 0.25F - Math.abs(fallSpeed)).color(1.0F, 1.0F, 1.0F, ticksAndPartialTicks0).lightmap(k4, j4).endVertex();
                        bufferbuilder.pos((double) graphicQualityX - x + rainSizeX + 0.5D + random.nextGaussian() * 2, (double) floorYMinusGraphicsQuality - y, (double) graphicQualityZ - z + rainSizeZ + 0.5D + random.nextGaussian()).tex(1.0F + f7, (float) floorYPlusGraphicsQuality * 0.25F - Math.abs(fallSpeed)).color(1.0F, 1.0F, 1.0F, ticksAndPartialTicks0).lightmap(k4, j4).endVertex();
                        bufferbuilder.pos((double) graphicQualityX - x - rainSizeX + 0.5D + random.nextGaussian() * 2, (double) floorYMinusGraphicsQuality - y, (double) graphicQualityZ - z - rainSizeZ + 0.5D + random.nextGaussian()).tex(0.0F + f7, (float) floorYPlusGraphicsQuality * 0.25F - Math.abs(fallSpeed)).color(1.0F, 1.0F, 1.0F, ticksAndPartialTicks0).lightmap(k4, j4).endVertex();
                    }
                }
            }
        }

        if (i1 >= 0) {
            tessellator.draw();
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultAlphaFunc();
        RenderSystem.disableAlphaTest();
        lightTexture.disableLightmap();

        return true;
    }

    @Override
    public void onCommandWeatherChange() {
        BlizzardClient.stopPlaying();
    }

    @Override
    public void handleFogDensity(EntityViewRenderEvent.FogDensity event, Minecraft mc) {
        if (BetterWeatherConfigClient.blizzardFog.get()) {
            if (mc.world != null && mc.player != null) {
                if (BetterWeatherUtil.isOverworld(mc.world.getDimensionKey())) {
                    BlockPos playerPos = new BlockPos(mc.player.getPositionVec());
                    if (Blizzard.doBlizzardsAffectDeserts(mc.world.getBiome(playerPos))) {
                        float partialTicks = mc.isGamePaused() ? mc.renderPartialTicksPaused : mc.timer.renderPartialTicks;
                        float fade = mc.world.getRainStrength(partialTicks);
                        event.setDensity(fade * 0.1F);
                        event.setCanceled(true);
                        if (idx2 != 0)
                            idx2 = 0;
                    } else {
                        if (idx2 == 0) {
                            event.setCanceled(false);
                            idx2++;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void livingEntityUpdate(Entity entity) {
        if (BetterWeatherUtil.isOverworld(entity.world.getDimensionKey())) {
            if (entity instanceof LivingEntity) {
                if (BetterWeatherConfig.doBlizzardsSlowPlayers.get() && entity.getPosition().getY() < entity.world.getHeight(Heightmap.Type.MOTION_BLOCKING, entity.getPosition().getX(), entity.getPosition().getZ())) //TODO: Get a good algorithm to determine whether or not a player/entity is indoors.
                    ((LivingEntity) entity).addPotionEffect(new EffectInstance(Effects.SLOWNESS, 5, BetterWeatherConfig.blizzardSlownessAmplifier.get(), true, false));
            }
        }
    }

    @Override
    public boolean disableSkyColor() {
        return true;
    }

    @Override
    public float modifyTemperature(float biomeTemp, float modifiedBiomeTemp, double seasonModifier) {
        return Math.max(-0.5F, modifiedBiomeTemp - 0.5F);
    }

    @Override
    public int forcedRenderDistance() {
        return BetterWeatherConfigClient.forcedRenderDistanceDuringBlizzards.get();
    }

    @Override
    public void tickLiveChunks(Chunk chunk, ServerWorld world) {
        Blizzard.addSnowAndIce(chunk, world, world.getWorldInfo().getGameTime());
    }

    @Override
    public boolean refreshPlayerRenderer() {
        return BetterWeather.usingOptifine;
    }

    @Override
    public boolean preventChunkRendererRefreshingWhenOptifineIsPresent() {
        return true;
    }

    public enum BlizzardLoopSoundTrack {
        LOOP1(SoundRegistry.BLIZZARD_LOOP1, 2400),
        LOOP2(SoundRegistry.BLIZZARD_LOOP2, 2400),
        LOOP3(SoundRegistry.BLIZZARD_LOOP3, 2400),
        LOOP4(SoundRegistry.BLIZZARD_LOOP4, 2400),
        LOOP5(SoundRegistry.BLIZZARD_LOOP5, 2400),
        LOOP6(SoundRegistry.BLIZZARD_LOOP6, 2400),
        LOOP7(SoundRegistry.BLIZZARD_LOOP7, 1200);

        private final SoundEvent soundEvent;
        private final int replayRate;

        BlizzardLoopSoundTrack(SoundEvent soundEvent, int tickReplayRate) {
            this.soundEvent = soundEvent;
            this.replayRate = tickReplayRate;
        }

        public SoundEvent getSoundEvent() {
            return this.soundEvent;
        }

        public int getReplayRate() {
            return this.replayRate;
        }
    }


    public static class BlizzardClient {
        public static MovingWeatherSound BLIZZARD_SOUND = new MovingWeatherSound(SoundRegistry.BLIZZARD_LOOP1, BetterWeatherConfigClient.blizzardLoopEnumValue.get().getReplayRate(), SoundCategory.WEATHER, Minecraft.getInstance().gameRenderer.getActiveRenderInfo().getBlockPos(), BetterWeatherConfigClient.blizzardVolume.get().floatValue(), BetterWeatherConfigClient.blizzardPitch.get().floatValue());


        public static void blizzardClient(ClientWorld world, long worldTime, Minecraft mc, int forcedRenderDistance) {
            SoundHandler soundHandler = mc.getSoundHandler();
            if (!soundHandler.isPlaying(BLIZZARD_SOUND) && doBlizzardsAffectDeserts(mc.world.getBiome(mc.gameRenderer.getActiveRenderInfo().getBlockPos()))) {
                MovingWeatherSound blizzardSound = new MovingWeatherSound(BetterWeatherConfigClient.blizzardLoopEnumValue.get().getSoundEvent(), BetterWeatherConfigClient.blizzardLoopEnumValue.get().getReplayRate(), SoundCategory.WEATHER, Minecraft.getInstance().gameRenderer.getActiveRenderInfo().getBlockPos(), BetterWeatherConfigClient.blizzardVolume.get().floatValue(), BetterWeatherConfigClient.blizzardPitch.get().floatValue());
                soundHandler.play(blizzardSound);
                BLIZZARD_SOUND = blizzardSound;
            }

            if (!BLIZZARD_SOUND.isDonePlaying() && !doBlizzardsAffectDeserts(mc.world.getBiome(mc.gameRenderer.getActiveRenderInfo().getBlockPos()))) {
                BLIZZARD_SOUND.finishPlaying();
            }

            if (BetterWeather.usingOptifine)
                mc.worldRenderer.renderDistanceChunks = forcedRenderDistance;
            else {
                if (worldTime % 20 == 0) {
                    if (doBlizzardsAffectDeserts(world.getBiome(mc.player.getPosition())))
                        BetterWeatherClientUtil.refreshViewFrustum(mc, forcedRenderDistance);
                    else
                        BetterWeatherClientUtil.refreshViewFrustum(mc, mc.gameSettings.renderDistanceChunks);
                }
            }
        }

        public static void stopPlaying() {
            BLIZZARD_SOUND.finishPlaying();
        }
    }
}
