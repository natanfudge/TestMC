package io.github.natanfudge.impl.mixin.client;

import io.github.natanfudge.impl.utils.Events;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    private boolean renderedBefore = false;

    // render() is only called once Minecraft has fully finished loading.
    // Can't find any better way of figuring out when that happens other than checking the first time render() is called.
    @Inject(at = @At("HEAD"), method = "render")
    private void onRender(CallbackInfo info) {
        if (!renderedBefore) {
            Events.INSTANCE.getOnTitleScreenLoaded().invoker().invoke();
            renderedBefore = true;
        }
    }
}
