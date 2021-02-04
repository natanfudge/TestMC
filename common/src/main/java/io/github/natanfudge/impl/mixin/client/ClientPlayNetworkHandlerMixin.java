package io.github.natanfudge.impl.mixin.client;

import io.github.natanfudge.impl.utils.Events;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    // It's fairly safe to close the game at this point
    @Inject(method = "onGameJoin", at = @At("TAIL"))
    private  void afterGameJoin(GameJoinS2CPacket packet, CallbackInfo ci){
        Events.INSTANCE.getOnJoinClientWorld().invoker().invoke();
    }
}
