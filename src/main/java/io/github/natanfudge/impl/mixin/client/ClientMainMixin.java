package io.github.natanfudge.impl.mixin.client;

import io.github.natanfudge.impl.mixinhandlers.MainMixinHandler;
import net.minecraft.client.main.Main;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public class ClientMainMixin {
    // This just prevents it from printing the exception twice.
    @Redirect(method = "main", at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;error(Ljava/lang/String;Ljava/lang/Throwable;)V"))
    private static void silenceFatalErrors(Logger logger, String message, Throwable t){
    }

    @Inject(method = "main", at = @At(value = "HEAD"))
    private static void asSoonAsPossible(String[] args, CallbackInfo ci) {
        MainMixinHandler.INSTANCE.asSoonAsPossible();
    }
}
