package dev.youtiao.aemobile.web.mixins;

import dev.youtiao.aemobile.web.WebApplication;
import dev.youtiao.aemobile.web.service.MSPTRealImpl;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.springframework.context.ApplicationContext;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {
    long currentTime = System.nanoTime();
    private MSPTRealImpl msptService;
    @Inject(method = "Lnet/minecraft/server/MinecraftServer;tick()V", at=@At("HEAD"))
    public void preTick(CallbackInfo ci) {
        currentTime = System.nanoTime();
    }

    @Inject(method = "Lnet/minecraft/server/MinecraftServer;tick()V", at=@At("TAIL"))
    public void postTick(CallbackInfo ci) {
        long t = System.nanoTime();
        double mspt = (t - currentTime) * 10E-6;
        currentTime = t;
        if(msptService != null) {
            msptService.addTickms(mspt);
        } else {
            ApplicationContext applicationContext = WebApplication.getApplicationContext();
            if(applicationContext != null) {
                msptService = (MSPTRealImpl) applicationContext.getBean("MSPTReal");
            }
        }
    }
}
