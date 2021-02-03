package io.github.natanfudge.impl.mixin;

import io.github.natanfudge.impl.mixinhandlers.ServerMixinHandler;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    // Used for stopping the server
    @Inject(method = "startServer", at = @At(value = "RETURN"))
    private static void getMinecraftDedis(CallbackInfoReturnable<MinecraftServer> ci){
       ServerMixinHandler.INSTANCE.setServer(ci.getReturnValue());
    }
}
