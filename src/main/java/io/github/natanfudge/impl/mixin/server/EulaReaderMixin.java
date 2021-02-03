package io.github.natanfudge.impl.mixin.server;

import net.minecraft.server.dedicated.EulaReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EulaReader.class)
public class EulaReaderMixin {
    // Having to accept the eula is not very friendly to automated testing
    @Redirect(method = "isEulaAgreedTo", at = @At(value = "FIELD", target = "Lnet/minecraft/server/dedicated/EulaReader;eulaAgreedTo:Z"))
    private boolean fuckYourEula(EulaReader owner) {
        return true;
    }
}
