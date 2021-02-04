package io.github.natanfudge.impl.mixin.client;

import io.github.natanfudge.impl.utils.Events;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.crash.CrashReport;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    private static Throwable error = null;

    // Just to get an instance of the original exception
    @Inject(method = "printCrashReport", at = @At(value = "HEAD"))
    private static void saveError(CrashReport report, CallbackInfo ci) {
        error = report.getCause();
    }

    @Redirect(method = {"printCrashReport"}, at = @At(value = "INVOKE", target = "Ljava/lang/System;exit(I)V"))
    private static void redirectSystemExitInPrintCrashReport(int status) throws Throwable {
        // Normally Minecraft does System.exit(-1) when initialization errors, but this doesn't get accepted well in test systems, so we throw instead.
        throw error;
    }

    @Redirect(method = {"stop"}, at = @At(value = "INVOKE", target = "Ljava/lang/System;exit(I)V"))
    private void redirectSystemExitInStop(int status) throws Throwable {
        // Normally Minecraft does System.exit(-1) when initialization errors, but this doesn't get accepted well in test systems, so we throw instead.
        throw error;
    }

    // The exception itself will be thrown, so there is no need to print it manually. (Without this the stack trace will print twice)
    @Redirect(method = "printCrashReport", at = @At(value = "INVOKE", target = "Lnet/minecraft/Bootstrap;println(Ljava/lang/String;)V"))
    private static void dontPrintException(String str) {
    }

    @Redirect(method = "run", at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;fatal(Ljava/lang/String;Ljava/lang/Throwable;)V"))
    private void silenceExceptions(Logger logger, String message, Throwable t) {

    }

    // shut the fuck up
    @Redirect(method = "method_31382", at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;error(Ljava/lang/String;Ljava/lang/Throwable;)V"))
    private void shutTheFuckUp(Logger logger, String message, Throwable t) {
    }


}
