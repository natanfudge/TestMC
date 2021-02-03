package io.github.natanfudge.impl.mixin.server;

import io.github.natanfudge.impl.utils.Events;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftDedicatedServer.class)
public class MinecraftDedicatedServerMixin {
    @Inject(method = "setupServer", at = @At("TAIL"))
    private void afterServerInitialized(CallbackInfoReturnable<Boolean> cir){
        Events.INSTANCE.getOnServerWorldLoaded().invoker().invoke();
    }

}
