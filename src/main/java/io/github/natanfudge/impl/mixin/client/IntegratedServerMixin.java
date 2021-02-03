package io.github.natanfudge.impl.mixin.client;

import io.github.natanfudge.impl.utils.Events;
import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IntegratedServer.class)
public class IntegratedServerMixin {
    @Inject(method = "setupServer", at = @At("TAIL"))
    private void afterServerInitialized(CallbackInfoReturnable<Boolean> cir){
        Events.INSTANCE.getOnServerWorldLoaded().invoker().invoke();
    }

}
