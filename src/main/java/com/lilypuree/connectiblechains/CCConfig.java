package com.lilypuree.connectiblechains;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod.EventBusSubscriber(modid = ConnectibleChains.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CCConfig {
    private static final transient boolean IS_DEBUG_ENV = FMLEnvironment.production;

    public ForgeConfigSpec.DoubleValue chainHangAmount;
    public ForgeConfigSpec.IntValue maxChainRange;
    public ForgeConfigSpec.IntValue quality;

    public static ForgeConfigSpec COMMON_CONFIG;

    public CCConfig() {
        ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();
        COMMON_BUILDER.comment("Connectible Chains Config\n\n");
//        chainHangAmount = COMMON_BUILDER.comment("""
//                Chain Hang Amount
//                Effects how much the chain hangs.
//                Collision will update on new chains or world loading.
//                Has no effect in multiplayer.
//                """).defineInRange("chainHangAmount", 9.0f, 0.0f, Double.MAX_VALUE);
//        maxChainRange = COMMON_BUILDER.comment("""
//                Max Chain Distance
//                Warning: Long chains can sometimes become invisible!
//                Has no effect in multiplayer
//                """).defineInRange("maxChainRange", 7, 0, 32);
//        quality = COMMON_BUILDER.comment("""
//                Chain Quality
//                Effects the visual quality the chain.
//                """).defineInRange("quality", 4, 1, 9);

        chainHangAmount = COMMON_BUILDER
                .comment("\r\n Chain Hang Amount\n"
                        + "\r\n Effects how much the chain hangs."
                        + "\r\n Collision will update on new chains or world loading."
                        + "\r\n  Has no effect in multiplayer."
                ).defineInRange("chainHangAmount", 9.0f, 0.0f, Double.MAX_VALUE);
        maxChainRange = COMMON_BUILDER
                .comment("\r\n  Max Chain Distance\n"
                        + "\r\n Warning: Long chains can sometimes become invisible!"
                        + "\r\n Has no effect in multiplayer.")
                .defineInRange("maxChainRange", 7, 0, 32);
        quality = COMMON_BUILDER
                .comment("\r\n  Chain Quality\n"
                        + "\r\n Effects the visual quality the chain.")
                .defineInRange("quality", 4, 1, 9);

        COMMON_CONFIG = COMMON_BUILDER.build();
    }

    public float getChainHangAmount() {
        return chainHangAmount.get().floatValue();
    }

    public int getMaxChainRange() {
        return maxChainRange.get();
    }

    public int getQuality() {
        return quality.get();
    }

    public boolean doDebugDraw() {
        return IS_DEBUG_ENV && Minecraft.getInstance().options.renderDebug;
    }
}
