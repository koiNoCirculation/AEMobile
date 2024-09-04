package dev.youtiao.aemobile.web.controller;

import appeng.crafting.v2.CraftingJobV2;
import dev.youtiao.aemobile.blocks.TileAEMonitor;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.DimensionManager;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping(("/AE2"))
public class AEPanelController {
    private ExecutorService executorService = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors());
    @GetMapping("/getNetworks")
    public TileAEMonitor.Response getNetworks(@RequestParam String ownerUUID) {
        System.out.println(TileAEMonitor.tilesInTheWorld);
        System.out.println(ownerUUID);
        return TileAEMonitor.Response.ofSuccess(TileAEMonitor.tilesInTheWorld.getOrDefault(ownerUUID.toLowerCase(), new HashSet<>()));
    }
    @PostMapping("/cancelTask")
    public TileAEMonitor.Response cancelTask(@RequestParam String ownerUUID, @RequestParam int dimid,
                                             @RequestParam int x, @RequestParam int y, @RequestParam int z, @RequestParam int cpuid) throws ExecutionException, InterruptedException {
        Set<TileAEMonitor.PosTuple> tuples = TileAEMonitor.tilesInTheWorld.get(ownerUUID.toLowerCase());
        if (tuples == null) {
            return TileAEMonitor.Response.ofError(String.format("UUID %s has not placed any ae monitor blocks in the world", ownerUUID));
        }
        if (tuples.contains(new TileAEMonitor.PosTuple(dimid, x, y, z))) {
            TileEntity tileEntity = DimensionManager.getWorld(dimid).getTileEntity(x, y, z);
            if (tileEntity != null) {
                ((TileAEMonitor)tileEntity).cancelTask(cpuid);
                return TileAEMonitor.Response.ofSuccess("Success");
            } else {
                return TileAEMonitor.Response.ofError("Internal error");
            }
        } else {
            return TileAEMonitor.Response.ofError(String.format("No ae monitor block placed at Dim %d, x=%d, y=%d, z=%d", dimid, x, y, z));
        }
    }

    @GetMapping("/getCraftingCpuInfo")
    public Flux<ServerSentEvent<TileAEMonitor.Response>> getCraftingCPUInfo(@RequestParam String ownerUUID,
                                                                            @RequestParam int dimid,
                                                                            @RequestParam int x,
                                                                            @RequestParam int y,
                                                                            @RequestParam int z) {
        Set<TileAEMonitor.PosTuple> tuples = TileAEMonitor.tilesInTheWorld.get(ownerUUID.toLowerCase());
        return Flux.interval(Duration.ofSeconds(0), Duration.ofSeconds(5)).map(seq -> {
            ServerSentEvent.Builder<TileAEMonitor.Response> ev = ServerSentEvent.
                    <TileAEMonitor.Response>builder().id(seq.toString()).event("message");
            if (tuples == null) {
                ev.data(TileAEMonitor.Response.ofError(String.format("UUID %s has not placed any ae monitor blocks in the world", ownerUUID)));
            }
            else if (tuples.contains(new TileAEMonitor.PosTuple(dimid, x, y, z))) {
                TileEntity tileEntity = DimensionManager.getWorld(dimid).getTileEntity(x, y, z);
                if (tileEntity != null) {
                    try {
                        ev.data(((TileAEMonitor) tileEntity).getCraftingCPUInfo());
                    } catch (Exception e) {
                        ev.data(TileAEMonitor.Response.ofError("Internal error"));
                    }
                } else {
                    ev.data(TileAEMonitor.Response.ofError("Internal error"));
                }
            } else {
                ev.data(TileAEMonitor.Response.ofError(String.format("No ae monitor block placed at Dim %d, x=%d, y=%d, z=%d", dimid, x, y, z)));
            }
            return ev.build();
        });
    }

    @GetMapping("/getCraftingCpuInfoNoSSE")
    public TileAEMonitor.Response getCraftingCPUInfoNoSSE(@RequestParam String ownerUUID,
                                                                            @RequestParam int dimid,
                                                                            @RequestParam int x,
                                                                            @RequestParam int y,
                                                                            @RequestParam int z) throws ExecutionException, InterruptedException {
        Set<TileAEMonitor.PosTuple> tuples = TileAEMonitor.tilesInTheWorld.get(ownerUUID.toLowerCase());
        if (tuples == null) {
            return TileAEMonitor.Response.ofError(String.format("UUID %s has not placed any ae monitor blocks in the world", ownerUUID));
        }
        else if (tuples.contains(new TileAEMonitor.PosTuple(dimid, x, y, z))) {
            TileEntity tileEntity = DimensionManager.getWorld(dimid).getTileEntity(x, y, z);
            if (tileEntity != null) {
                return ((TileAEMonitor) tileEntity).getCraftingCPUInfo();
            } else {
                return TileAEMonitor.Response.ofError("Internal error");
            }
        } else {
            return TileAEMonitor.Response.ofError(String.format("No ae monitor block placed at Dim %d, x=%d, y=%d, z=%d", dimid, x, y, z));
        }
    }


    @PostMapping("/generateCraftingPlan")
    public SseEmitter generateCraftingPlan(@RequestParam String ownerUUID,
                                           @RequestParam int dimid,
                                           @RequestParam int x,
                                           @RequestParam int y,
                                           @RequestParam int z,
                                           @RequestParam String item,
                                           @RequestParam int meta,
                                           @RequestParam long count,
                                           @RequestParam(required = false) String nbt) {
        Set<TileAEMonitor.PosTuple> tuples = TileAEMonitor.tilesInTheWorld.get(ownerUUID.toLowerCase());
        CraftingJobV2[] craftingJob = new CraftingJobV2[1];
        SseEmitter emitter = new SseEmitter(1800000L);
        executorService.execute(() -> {
            try {
                int i = 0;
                SseEmitter.SseEventBuilder ev = SseEmitter.event().id(String.valueOf(i)).name("message");
                if (tuples == null) {
                    ev.data(TileAEMonitor.Response.ofError(String.format("UUID %s has not placed any ae monitor blocks in the world", ownerUUID)));

                }
                else if (tuples.contains(new TileAEMonitor.PosTuple(dimid, x, y, z))) {
                    TileEntity tileEntity = DimensionManager.getWorld(dimid).getTileEntity(x, y, z);
                    if (tileEntity != null) {
                        if(craftingJob[0] == null) {
                            craftingJob[0] = ((TileAEMonitor)tileEntity).generateCraftingPlan(item, meta, nbt, count);
                        }

                        while (craftingJob[0].simulateFor(1500)) {
                            SseEmitter.SseEventBuilder simulating = SseEmitter.event().id(String.valueOf(i)).name("message");
                            simulating.data(TileAEMonitor.Response.ofSuccess("SIMULATING - resolved: " +  craftingJob[0].getContext().getResolvedTasks().size()));
                            emitter.send(simulating);
                        }
                        SseEmitter.SseEventBuilder result = SseEmitter.event().id(String.valueOf(i)).name("message");
                        result.data(((TileAEMonitor) tileEntity).populatePlan(craftingJob[0]));
                        emitter.send(result);
                        emitter.complete();
                        return;
                    } else {
                        ev.data(TileAEMonitor.Response.ofError("Internal error"));
                    }
                } else {
                    ev.data(TileAEMonitor.Response.ofError(String.format("No ae monitor block placed at Dim %d, x=%d, y=%d, z=%d", dimid, x, y, z)));
                }
                emitter.send(ev);
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
       return  emitter;
    }

    @PostMapping("/startCraftingJob")
    public TileAEMonitor.Response startCraftingJob(@RequestParam String ownerUUID,
                                                       @RequestParam int dimid,
                                                       @RequestParam int x,
                                                       @RequestParam int y,
                                                       @RequestParam int z,
                                                       @RequestParam String item,
                                                       @RequestParam int meta,
                                                       @RequestParam long count,
                                                       @RequestParam int cpuId,
                                                       @RequestParam @Nullable String nbt,
                                                       @RequestParam boolean allowMissing) {
        Set<TileAEMonitor.PosTuple> tuples = TileAEMonitor.tilesInTheWorld.get(ownerUUID.toLowerCase());
        if (tuples == null) {
            return TileAEMonitor.Response.ofError(String.format("UUID %s has not placed any ae monitor blocks in the world", ownerUUID));
        }
        if (tuples.contains(new TileAEMonitor.PosTuple(dimid, x, y, z))) {
            TileEntity tileEntity = DimensionManager.getWorld(dimid).getTileEntity(x, y, z);
            if (tileEntity != null) {
                return ((TileAEMonitor)tileEntity).submitCraftJob(new TileAEMonitor.CraftRequest(item, meta, count, cpuId, nbt, allowMissing));
            } else {
                return TileAEMonitor.Response.ofError("Internal error");
            }
        } else {
            return TileAEMonitor.Response.ofError(String.format("No ae monitor block placed at Dim %d, x=%d, y=%d, z=%d", dimid, x, y, z));
        }
    }




    @GetMapping("/getCraftingDetails")
    public Flux<ServerSentEvent<TileAEMonitor.Response>> getCraftingDetails(@RequestParam String ownerUUID,
                                                                                        @RequestParam int dimid,
                                                                                        @RequestParam int x,
                                                                                        @RequestParam int y,
                                                                                        @RequestParam int z,
                                                                                        @RequestParam int cpuid) {

        Set<TileAEMonitor.PosTuple> tuples = TileAEMonitor.tilesInTheWorld.get(ownerUUID.toLowerCase());
        return Flux.interval(Duration.ofSeconds(0), Duration.ofSeconds(5)).map(seq -> {
                    ServerSentEvent.Builder<TileAEMonitor.Response> ev = ServerSentEvent.
                            <TileAEMonitor.Response>builder().id(seq.toString()).event("message");
                    if (tuples == null) {
                        ev = ev.data(TileAEMonitor.Response.ofError(String.format("UUID %s has not placed any ae monitor blocks in the world", ownerUUID)));
                    }
                    else if (tuples.contains(new TileAEMonitor.PosTuple(dimid, x, y, z))) {
                        TileEntity tileEntity = DimensionManager.getWorld(dimid).getTileEntity(x, y, z);
                        if (tileEntity != null) {
                            ev = ev.data(((TileAEMonitor) tileEntity).getCraftingStatus(cpuid));
                        } else {
                            ev = ev.data(TileAEMonitor.Response.ofError("Internal error"));
                        }
                    } else {
                        ev = ev.data(TileAEMonitor.Response.ofError(String.format("No ae monitor block placed at Dim %d, x=%d, y=%d, z=%d", dimid, x, y, z)));
                    }
                    return ev.build();
                }
        );
    }

    @GetMapping("/getItems")
    public Flux<ServerSentEvent<TileAEMonitor.Response>> getItems(@RequestParam String ownerUUID,
                                                                  @RequestParam int dimid,
                                                                  @RequestParam int x,
                                                                  @RequestParam int y,
                                                                  @RequestParam int z,
                                                                  @RequestParam boolean craftableOnly) {

        Set<TileAEMonitor.PosTuple> tuples = TileAEMonitor.tilesInTheWorld.get(ownerUUID.toLowerCase());
        return Flux.interval(Duration.ofSeconds(0), Duration.ofSeconds(300)).map(seq -> {
            ServerSentEvent.Builder<TileAEMonitor.Response> ev = ServerSentEvent.
                    <TileAEMonitor.Response>builder().id(seq.toString()).event("message");
            if (tuples == null) {
                ev = ev.data(TileAEMonitor.Response.ofError(String.format("UUID %s has not placed any ae monitor blocks in the world", ownerUUID)));
            }
            else if (tuples.contains(new TileAEMonitor.PosTuple(dimid, x, y, z))) {
                TileEntity tileEntity = DimensionManager.getWorld(dimid).getTileEntity(x, y, z);
                if (tileEntity != null) {
                    ev = ev.data(((TileAEMonitor) tileEntity).getAllStoredItems(craftableOnly));
                } else {
                    ev = ev.data(TileAEMonitor.Response.ofError("Internal error"));
                }
            } else {
                ev = ev.data(TileAEMonitor.Response.ofError(String.format("No ae monitor block placed at Dim %d, x=%d, y=%d, z=%d", dimid, x, y, z)));
            }
            return ev.build();
        });
    }
}
