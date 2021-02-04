package io.github.natanfudge.impl.mixin.server;

import io.github.natanfudge.impl.mixinhandlers.MainMixinHandler;
import io.github.natanfudge.impl.mixinhandlers.ServerMixinHandler;
import net.minecraft.server.Main;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Main.class)
public class ServerMainMixin {
    // After this the server process just ends, so throwing has the same overall result, except we can actually handle it ourselves.
    @Redirect(method = "main", at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;fatal(Ljava/lang/String;Ljava/lang/Throwable;)V"))
    private static void propagateErrorToCaller(Logger logger, String message, Throwable t) throws Throwable {
        throw t;
    }

    @Inject(method = "main", at = @At(value = "HEAD"))
    private static void asSoonAsPossible(String[] args, CallbackInfo ci) {
        MainMixinHandler.INSTANCE.asSoonAsPossible();
    }


}
