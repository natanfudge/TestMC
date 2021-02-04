package io.github.natanfudge.impl.mixin.server;

import net.minecraft.server.dedicated.AbstractPropertiesHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Function;

@Mixin(AbstractPropertiesHandler.class)
public class AbstractPropertyHandlerMixin {

    @Shadow
    protected <V> V get(String key, Function<String, V> parser, V fallback) {
        throw new AssertionError();
    }

    // Minecraft doesn't allow you to join the test server without disabling online mode
    @Redirect(method = "parseBoolean", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/dedicated/AbstractPropertiesHandler;get(Ljava/lang/String;Ljava/util/function/Function;Ljava/lang/Object;)Ljava/lang/Object;"))
    private <V> Object turnOffOnlineMode(AbstractPropertiesHandler abstractPropertiesHandler, String key, Function<String, V> parser, V fallback) {
        if (key.equals("online-mode")) return true;
        else return get(key, Boolean::valueOf, fallback);
    }
}
