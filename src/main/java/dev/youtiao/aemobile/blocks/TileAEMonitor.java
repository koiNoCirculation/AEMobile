package dev.youtiao.aemobile.blocks;

import appeng.api.config.Actionable;
import appeng.api.config.CraftingMode;
import appeng.api.networking.crafting.CraftingItemList;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.crafting.v2.CraftingJobV2;
import appeng.me.GridAccessException;
import appeng.me.cache.CraftingGridCache;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.helpers.IGridProxyable;
import appeng.util.item.AEItemStack;
import appeng.util.item.ItemList;
import com.google.common.collect.ImmutableList;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import org.apache.commons.lang3.tuple.Pair;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

public class TileAEMonitor extends TileEntity {


    public static Map<String, Set<PosTuple>> tilesInTheWorld = new HashMap<>();

    private PosTuple posTuple;
    private ForgeDirection directionAETile;

    private UUID ownerUUID;

    private ArrayDeque<FutureTask<?>> tasks = new ArrayDeque<>();

    public static class Response<T> {
        private T body;
        private boolean succeed;
        private String message;
        public static <T> Response<T> ofSuccess(T body) {
            Response<T> response = new Response<>();
            response.body = body;
            response.succeed = true;
            return response;
        }
        public static Response ofError(String message) {
            Response response = new Response();
            response.succeed = false;
            response.message = message;
            return response;
        }

        public T getBody() {
            return body;
        }

        public boolean isSucceed() {
            return succeed;
        }

        public String getMessage() {
            return message;
        }
    }

    public class CraftPlan {
        private long bytesUsed;

        public Collection<CraftTaskItem> getPlan() {
            return plan;
        }

        private Collection<CraftTaskItem> plan;

        public long getBytesUsed() {
            return bytesUsed;
        }
    }

    public class ItemStackResponse {
        private String item_name;   //Item.itemRegistry.getNameForObject(stack.getItem())

        private int meta;

        private String nbt;
        private long count;
        private boolean craftable;

        public String getItem_name() {
            return item_name;
        }

        public long getCount() {
            return count;
        }

        public boolean isCraftable() {
            return craftable;
        }

        public int getMeta() {
            return meta;
        }

        public String getNbt() {
            return nbt;
        }
    }

    public static class CraftRequest {
        private String item_name;   //Item.itemRegistry.getNameForObject(stack.getItem())

        private int meta; //

        private long count;

        private int cpuId;

        private String nbt;

        public CraftRequest(String item_name, int meta, long count, int cpuId) {
            this.item_name = item_name;
            this.meta = meta;
            this.count = count;
            this.cpuId = cpuId;
        }

        public String getItem_name() {
            return item_name;
        }

        public long getCount() {
            return count;
        }

        public int getMeta() {
            return meta;
        }

        public String getNbt() {
            return nbt;
        }
    }

    public static class PosTuple {
        private int dimid;
        private int x;
        private int y;
        private int z;

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }

        public int getDimid() {
            return dimid;
        }

        public PosTuple(int dimid, int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimid = dimid;
        }
        @Override
        public boolean equals(Object o) {
            if(o instanceof PosTuple) {
                if(o == this) {
                    return true;
                }
                return dimid == ((PosTuple) o).dimid && x == ((PosTuple) o).x && y == ((PosTuple) o).y && ((PosTuple) o).z == z;
            }
            return false;
        }
        @Override
        public int hashCode() {
            return (x * 100 + y) * 10 + z;
        }
    }

    public class CraftingCPUInfo {
        private int idx;
        private String cpuName;
        private int meta;
        private String item;

        private String nbt;
        private long storage;
        private long parallelism;
        private long remainingCount;

        public String getCpuName() {
            return cpuName;
        }

        public String getItem() {
            return item;
        }

        public long getStorage() {
            return storage;
        }

        public long getParallelism() {
            return parallelism;
        }

        public long getRemainingCount() {
            return remainingCount;
        }

        public int getMeta() {
            return meta;
        }

        public int getIdx() {
            return idx;
        }

        public void setIdx(int idx) {
            this.idx = idx;
        }

        public String getNbt() {
            return nbt;
        }
    }

    public class CraftTaskItem {
        private String name;
        private int meta;

        private String nbt;

        private boolean isCrafting;

        private long numberPresent;
        private long numberSent;
        private long numberRemainingToCraft;

        private long missing;

        public String getName() {
            return name;
        }

        public boolean isCrafting() {
            return isCrafting;
        }

        public long getNumberPresent() {
            return numberPresent;
        }

        public long getNumberSent() {
            return numberSent;
        }

        public long getNumberRemainingToCraft() {
            return numberRemainingToCraft;
        }

        public int getMeta() {
            return meta;
        }
        @Override
        public boolean equals(Object o) {
            if(o instanceof CraftTaskItem) {
                return Objects.equals(meta, ((CraftTaskItem) o).meta) && Objects.equals(name, ((CraftTaskItem) o).name);
            } else {
                return false;
            }
        }
        @Override
        public int hashCode() {
            return 100 * meta + name.hashCode();
        }

        public long getMissing() {
            return missing;
        }

        public void setMissing(long missing) {
            this.missing = missing;
        }

        public String getNbt() {
            return nbt;
        }
    }


    @Override
    public void readFromNBT(NBTTagCompound p_145839_1_) {
        super.readFromNBT(p_145839_1_);
        String uuid = p_145839_1_.getString("ownerUUID");
        if(uuid == null) return;
        String password = uuid.split("-")[0];
        ownerUUID = UUID.fromString(uuid);
        int[] tuple = p_145839_1_.getIntArray("posTuple");
        posTuple = new PosTuple(tuple[0], tuple[1], tuple[2], tuple[3]);
        String direction = p_145839_1_.getString("direction");
        if(direction != null) directionAETile = ForgeDirection.valueOf(p_145839_1_.getString("direction"));
        TileAEMonitor.tilesInTheWorld.computeIfAbsent(password, (e) -> {
            return new HashSet<>();
        });
        TileAEMonitor.tilesInTheWorld.get(password).add(posTuple);
    }

    @Override
    public void writeToNBT(NBTTagCompound p_145841_1_) {
        super.writeToNBT(p_145841_1_);
        if(ownerUUID == null) return;
        p_145841_1_.setString("ownerUUID", ownerUUID.toString());
        p_145841_1_.setIntArray("posTuple", new int[]{posTuple.dimid, posTuple.x, posTuple.y, posTuple.z});
        if(directionAETile != null) {
            p_145841_1_.setString("direction", directionAETile.name());
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if(!worldObj.isRemote) {//Exception
            if(ownerUUID == null) return;
            String password = ownerUUID.toString().toLowerCase().split("-")[0];
            tilesInTheWorld.get(password).remove(posTuple);
        }
    }

    public ForgeDirection getDirectionAETile() {
        return directionAETile;
    }

    public void setDirectionAETile(ForgeDirection directionAETile) {
        this.directionAETile = directionAETile;
    }

    public void setOwnerUUID(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    public PosTuple getPosTuple() {
        return posTuple;
    }

    public void setPosTuple(PosTuple posTuple) {
        this.posTuple = posTuple;
    }

    private String writeNBTAsBase64(NBTTagCompound nbt) {
        if(nbt == null) {
            return null;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try(DataOutputStream dos = new DataOutputStream(bos)) {
            CompressedStreamTools.write(nbt, dos);
            return Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private NBTTagCompound readNBTFromBase64(String s) {
        if(s == null) {
            return null;
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(Base64.getDecoder().decode(s));
        try(DataInputStream dis = new DataInputStream(bis)) {
            return CompressedStreamTools.read(dis);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void updateEntity() {
        while (!tasks.isEmpty())
            tasks.poll().run();
    }


    public Response<List<CraftingCPUInfo>> getCraftingCPUInfo() throws ExecutionException, InterruptedException {
        FutureTask<Response> ft = new FutureTask<>(() -> {
            TileEntity tileEntity = worldObj.getTileEntity(xCoord + directionAETile.offsetX, yCoord + directionAETile.offsetY, zCoord + directionAETile.offsetZ);
            if (tileEntity instanceof IGridProxyable) {
                try {
                    int[] i = {1};
                    List<CraftingCPUInfo> list = ((IGridProxyable) tileEntity).getProxy().getCrafting().getCpus().stream().map(e -> {
                        CraftingCPUInfo craftingCPUInfo = new CraftingCPUInfo();
                        craftingCPUInfo.setIdx(i[0]);
                        craftingCPUInfo.cpuName = e.getName();
                        IAEItemStack finalOutput = e.getFinalOutput();
                        if (finalOutput != null && ((CraftingCPUCluster) e).getRemainingOperations() > 0) {
                            craftingCPUInfo.item = Item.itemRegistry.getNameForObject(finalOutput.getItem());
                            craftingCPUInfo.meta = finalOutput.getItemDamage();
                            craftingCPUInfo.remainingCount = e.getRemainingItemCount();
                            craftingCPUInfo.nbt = writeNBTAsBase64(finalOutput.getItemStack().getTagCompound());
                        }
                        craftingCPUInfo.parallelism = e.getCoProcessors();
                        craftingCPUInfo.storage = e.getAvailableStorage();
                        i[0]++;
                        return craftingCPUInfo;
                    }).collect(Collectors.toList());
                    return Response.ofSuccess(list);
                } catch (GridAccessException e) {
                    return Response.ofError(e.getMessage());
                }
            } else {
                return Response.ofError("No ME interface connected to this block. Cannot attach to AE grid.");
            }
        });
        tasks.add(ft);
        return ft.get();
    }

    public Response<CraftPlan> populatePlan(CraftingJobV2 iCraftingJob) {
        ItemList plan = new ItemList();
        iCraftingJob.populatePlan(plan);
        List<CraftTaskItem> itemList = new ArrayList<>();
        TileEntity tileEntity = worldObj.getTileEntity(xCoord + directionAETile.offsetX, yCoord + directionAETile.offsetY, zCoord + directionAETile.offsetZ);
        IGridProxyable gridProxyable = (IGridProxyable) tileEntity;
        final IStorageGrid sg;
        try {
            sg = gridProxyable.getProxy().getGrid().getCache(IStorageGrid.class);
            final IMEInventory<IAEItemStack> items = sg.getItemInventory();
            for (IAEItemStack iaeItemStack : plan) {
                CraftTaskItem craftTaskItem = new CraftTaskItem();
                craftTaskItem.name = Item.itemRegistry.getNameForObject(iaeItemStack.getItem());
                craftTaskItem.meta = iaeItemStack.getItemDamage();
                craftTaskItem.numberRemainingToCraft = iaeItemStack.getCountRequestable();
                craftTaskItem.nbt = writeNBTAsBase64(iaeItemStack.getItemStack().getTagCompound());
                IAEItemStack missing = iaeItemStack.copy();
                IAEItemStack toExtract = items.extractItems(iaeItemStack, Actionable.SIMULATE, new MachineSource((IActionHost) gridProxyable.getProxy().getMachine()));
                if (toExtract == null) {
                    toExtract = missing.copy();
                    toExtract.setStackSize(0);
                }
                craftTaskItem.numberPresent = toExtract.getStackSize();
                craftTaskItem.setMissing(missing.getStackSize() - toExtract.getStackSize());
                itemList.add(craftTaskItem);
            }
            CraftPlan craftPlan = new CraftPlan();
            craftPlan.bytesUsed = iCraftingJob.getByteTotal();
            craftPlan.plan = itemList;
            return Response.ofSuccess(craftPlan);
        } catch (GridAccessException e) {
            throw new RuntimeException(e);
        }
    }
    public CraftingJobV2 generateCraftingPlan(String item, int meta, String nbt, long count) {
        TileEntity tileEntity = worldObj.getTileEntity(xCoord + directionAETile.offsetX, yCoord + directionAETile.offsetY, zCoord + directionAETile.offsetZ);
        IGridProxyable gridProxyable = (IGridProxyable) tileEntity;
        try {
            ItemStack itemStack = new ItemStack((Item) Item.itemRegistry.getObject(item), 1, meta);
            itemStack.setTagCompound(readNBTFromBase64(nbt));
            AEItemStack aeItemStack = AEItemStack.create(itemStack);
            aeItemStack.setStackSize(count);
            return new CraftingJobV2(worldObj, gridProxyable.getProxy().getGrid(),  new MachineSource((IActionHost) gridProxyable.getProxy().getMachine()), aeItemStack, CraftingMode.STANDARD, null);
        } catch (GridAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public Response<Collection<CraftTaskItem>> getCraftingStatus(int cpuIndex) {
        TileEntity tileEntity = worldObj.getTileEntity(xCoord + directionAETile.offsetX, yCoord + directionAETile.offsetY, zCoord + directionAETile.offsetZ);
        if(tileEntity instanceof IGridProxyable) {
            try {
                Map<Pair<String, Pair<NBTTagCompound,Integer>>, CraftTaskItem> items = new HashMap<>();
                int i = 0;
                ICraftingCPU selected = null;
                for (ICraftingCPU cpu : ((IGridProxyable) tileEntity).getProxy().getCrafting().getCpus()) {
                    if(i == cpuIndex) {
                        selected = cpu;
                        break;
                    }
                    i++;
                }
                if(selected == null) {
                    return Response.ofSuccess(ImmutableList.of());
                }
                CraftingCPUCluster cpu = (CraftingCPUCluster) selected;
                if(cpu.getRemainingItemCount() <= 0) {
                    return Response.ofSuccess(ImmutableList.of());
                }
                ItemList pending = new ItemList();
                ItemList active = new ItemList();
                ItemList storage = new ItemList();
                cpu.getListOfItem(pending, CraftingItemList.PENDING);
                cpu.getListOfItem(active, CraftingItemList.ACTIVE);
                cpu.getListOfItem(storage, CraftingItemList.STORAGE);
                for (IAEItemStack iaeItemStack : pending) {
                    CraftTaskItem craftTaskItem = new CraftTaskItem();
                    craftTaskItem.name = Item.itemRegistry.getNameForObject(iaeItemStack.getItem());
                    craftTaskItem.meta = iaeItemStack.getItemDamage();
                    craftTaskItem.nbt = writeNBTAsBase64(iaeItemStack.getItemStack().getTagCompound());
                    craftTaskItem.isCrafting = false;
                    craftTaskItem.numberRemainingToCraft = iaeItemStack.getStackSize();
                    items.put(Pair.of(craftTaskItem.name, Pair.of(iaeItemStack.getItemStack().getTagCompound(), craftTaskItem.meta)), craftTaskItem);
                }
                for (IAEItemStack iaeItemStack : active) {
                    CraftTaskItem craftTaskItem;
                    String n = Item.itemRegistry.getNameForObject(iaeItemStack.getItem());
                    int d = iaeItemStack.getItemDamage();
                    Pair<String, Pair<NBTTagCompound, Integer>> key = Pair.of(n, Pair.of(iaeItemStack.getItemStack().getTagCompound(), iaeItemStack.getItemDamage()));
                    if(items.containsKey(key)) {
                        craftTaskItem = items.get(key);
                    } else {
                        craftTaskItem = new CraftTaskItem();
                        craftTaskItem.name = n;
                        craftTaskItem.meta = d;
                        craftTaskItem.nbt = writeNBTAsBase64(iaeItemStack.getItemStack().getTagCompound());
                        items.put(Pair.of(craftTaskItem.name, Pair.of(iaeItemStack.getItemStack().getTagCompound(), craftTaskItem.meta)), craftTaskItem);
                    }
                    craftTaskItem.isCrafting = true;
                    craftTaskItem.numberSent += iaeItemStack.getStackSize();
                }
                for (IAEItemStack iaeItemStack : storage) {
                    CraftTaskItem craftTaskItem;
                    String n = Item.itemRegistry.getNameForObject(iaeItemStack.getItem());
                    int d = iaeItemStack.getItemDamage();
                    Pair<String, Pair<NBTTagCompound, Integer>> key = Pair.of(n, Pair.of(iaeItemStack.getItemStack().getTagCompound(), iaeItemStack.getItemDamage()));
                    if(items.containsKey(key)) {
                        craftTaskItem = items.get(key);
                    } else {
                        craftTaskItem = new CraftTaskItem();
                        craftTaskItem.name = n;
                        craftTaskItem.meta = d;
                        craftTaskItem.nbt = writeNBTAsBase64(iaeItemStack.getItemStack().getTagCompound());
                        items.put(Pair.of(craftTaskItem.name, Pair.of(iaeItemStack.getItemStack().getTagCompound(), craftTaskItem.meta)), craftTaskItem);
                    }
                    craftTaskItem.numberPresent += iaeItemStack.getStackSize();
                }
                Iterator<Map.Entry<Pair<String, Pair<NBTTagCompound, Integer>>, CraftTaskItem>> iterator = items.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Pair<String,  Pair<NBTTagCompound, Integer>>, CraftTaskItem> next = iterator.next();
                    if(next.getValue().numberPresent == 0 && next.getValue().numberRemainingToCraft == 0 && next.getValue().numberSent == 0) {
                        iterator.remove();
                    }
                }
                return Response.ofSuccess(items.values());
            } catch (GridAccessException e) {
                e.printStackTrace();
                return Response.ofError(e.getMessage());
            }
        } else {
            return Response.ofError("No ME interface connected to this block. Cannot attach to AE grid.");
        }
    }

    public Response<String> submitCraftJob(CraftRequest request) {
        TileEntity tileEntity = worldObj.getTileEntity(xCoord + directionAETile.offsetX, yCoord + directionAETile.offsetY, zCoord + directionAETile.offsetZ);
        if(tileEntity instanceof IGridProxyable) {
            try {
                IGridProxyable gridProxyable = (IGridProxyable) tileEntity;
                ICraftingGrid cg = null;
                cg = gridProxyable.getProxy().getGrid().getCache(ICraftingGrid.class);
                ItemStack itemStack = new ItemStack((Item) Item.itemRegistry.getObject(request.item_name), 1, request.meta);
                itemStack.setTagCompound(readNBTFromBase64(request.nbt));
                AEItemStack aeItemStack = AEItemStack.create(itemStack);
                aeItemStack.setStackSize(request.count);
                ICraftingGrid finalCg = cg;
                Future<ICraftingJob> jobFuture;
                if (finalCg instanceof CraftingGridCache) {

                    jobFuture = ((CraftingGridCache) finalCg).beginCraftingJob(
                            worldObj,
                            gridProxyable.getProxy().getGrid(),
                            new MachineSource((IActionHost) gridProxyable.getProxy().getMachine()),
                            aeItemStack,
                            CraftingMode.STANDARD,
                            null);
                } else {
                    jobFuture = finalCg.beginCraftingJob(
                            worldObj,
                            gridProxyable.getProxy().getGrid(),
                            new MachineSource((IActionHost) gridProxyable.getProxy().getMachine()),
                            aeItemStack,
                            null);
                }
                int i = 0;
                CraftingCPUCluster selected = null;
                for (ICraftingCPU cpu : ((IGridProxyable) tileEntity).getProxy().getCrafting().getCpus()) {
                    if (i == request.cpuId - 1) {
                        selected = (CraftingCPUCluster) cpu;
                        break;
                    }
                    i++;
                }
                ICraftingJob iCraftingJob = jobFuture.get();

                CraftingCPUCluster finalSelected = selected;
                ICraftingLink g = finalCg.submitJob(
                        iCraftingJob,
                        null,
                        finalSelected,
                        true,
                        new MachineSource((IActionHost) gridProxyable.getProxy().getMachine()));
                if (g != null) {
                    return Response.ofSuccess("Successfully submitted craft job");
                } else {
                    return Response.ofError("Failed submitting craft job");
                }
            } catch (GridAccessException e) {
                return Response.ofError(e.getMessage());
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            return Response.ofError("No ME interface connected to this block. Cannot attach to AE grid.");
        }
    }
    public Response<List<ItemStackResponse>> getAllStoredItems(boolean canCraftOnly) {
        TileEntity tileEntity = worldObj.getTileEntity(xCoord + directionAETile.offsetX, yCoord + directionAETile.offsetY, zCoord + directionAETile.offsetZ);
        if(tileEntity instanceof IGridProxyable) {
            try {
                FutureTask<IMEMonitor<IAEItemStack>> ft = new FutureTask<>(() -> ((IGridProxyable) tileEntity).getProxy().getStorage().getItemInventory());
                tasks.add(ft);
                IMEMonitor<IAEItemStack> itemInventory = ft.get();
                List<ItemStackResponse> list = new ArrayList<>();
                for (IAEItemStack iaeItemStack : itemInventory.getStorageList()) {
                    if(canCraftOnly && !iaeItemStack.isCraftable()) {
                        continue;
                    }
                    ItemStackResponse itemStackResponse = new ItemStackResponse();
                    itemStackResponse.item_name = Item.itemRegistry.getNameForObject(iaeItemStack.getItem());
                    itemStackResponse.meta = iaeItemStack.getItemDamage();
                    itemStackResponse.nbt = writeNBTAsBase64(iaeItemStack.getItemStack().getTagCompound());
                    itemStackResponse.count = iaeItemStack.getStackSize();
                    itemStackResponse.craftable = iaeItemStack.isCraftable();
                    list.add(itemStackResponse);
                }
                return Response.ofSuccess(list);
            }  catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            return Response.ofError("No ME interface connected to this block. Cannot attach to AE grid.");
        }
    }

    public void cancelTask(int cpuId) throws ExecutionException, InterruptedException {
        FutureTask<Void> voidFutureTask = new FutureTask<>(() -> {
            IGridProxyable tileEntity = (IGridProxyable) worldObj.getTileEntity(xCoord + directionAETile.offsetX, yCoord + directionAETile.offsetY, zCoord + directionAETile.offsetZ);
            List<Pair<Integer, ICraftingCPU>> cpulist = new ArrayList<>();
            int i = 0;
            try {
                for (ICraftingCPU cpus : tileEntity.getProxy().getCrafting().getCpus()) {
                    if (i == cpuId) {
                        ((CraftingCPUCluster) cpus).cancel();
                    }
                    i++;
                }
            } catch (GridAccessException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
        tasks.add(voidFutureTask);
        voidFutureTask.get();
        return;

    }
}
