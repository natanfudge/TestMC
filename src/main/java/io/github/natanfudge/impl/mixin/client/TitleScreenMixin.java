package io.github.natanfudge.impl.mixin.client;

import io.github.natanfudge.impl.events.TitleScreenLoadedEvent;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    private boolean renderedBefore = false;

    @Inject(at = @At("HEAD"), method = "render")
    private void onRender(CallbackInfo info) {
        if (!renderedBefore) {
            TitleScreenLoadedEvent.EVENT.invoker().onTitleScreenLoaded();
            renderedBefore = true;
        }
    }
}
