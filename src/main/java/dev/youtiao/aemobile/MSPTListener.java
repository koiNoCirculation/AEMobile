package dev.youtiao.aemobile;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import dev.youtiao.aemobile.web.WebApplication;
import dev.youtiao.aemobile.web.service.MSPTRealImpl;
import org.springframework.context.ApplicationContext;

public class MSPTListener {
    Long currentTime = System.nanoTime();
    private MSPTRealImpl msptService;
    @SubscribeEvent
    public void tickEvent(TickEvent.ServerTickEvent event) {
        if(event.phase == TickEvent.Phase.START) {
            currentTime = System.nanoTime();
        } else if(event.phase == TickEvent.Phase.END) {
            if(currentTime == null) return;
            long t = System.nanoTime();
            double mspt = (t - currentTime) / 1000000.0;
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
}
