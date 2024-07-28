package dev.youtiao.aemobile.blocks;

import appeng.me.helpers.IGridProxyable;
import com.mojang.authlib.GameProfile;
import dev.youtiao.aemobile.Tags;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.HashSet;

public class BlockAEMonitor extends Block implements ITileEntityProvider {
    public BlockAEMonitor(Material p_i45394_1_) {
        super(p_i45394_1_);
    }

    /*
     * 相邻AE网络方块被挖掉时，清除记录
     */
    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block block) {
        TileAEMonitor tileEntity = (TileAEMonitor) world.getTileEntity(x, y, z);
        if(!world.isRemote) {
            tileEntity.setDirectionAETile(null);
            for (ForgeDirection value : ForgeDirection.values()) {
                TileEntity te = world.getTileEntity(x + value.offsetX, y + value.offsetY, z + value.offsetZ);
                if (te instanceof IGridProxyable) {
                    tileEntity.setDirectionAETile(value);
                    break;
                }
            }
        }
    }

    /*
     * 玩家放下方块时，记录玩家uuid
     * 同时检测边上的ae网络方块位置并记录在TE里面
     */
    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entityPlayerLike, ItemStack itemStack) {
        TileAEMonitor tileEntity = (TileAEMonitor) world.getTileEntity(x, y, z);
        if(!world.isRemote) {
            tileEntity.setPosTuple(new TileAEMonitor.PosTuple(world.provider.dimensionId, x, y, z));
            if (entityPlayerLike instanceof EntityPlayer) {
                GameProfile gameProfile = ((EntityPlayer) entityPlayerLike).getGameProfile();
                String password = gameProfile.getId().toString().split("-")[0];
                tileEntity.setOwnerUUID(gameProfile.getId());
                TileAEMonitor.tilesInTheWorld.computeIfAbsent(password.toLowerCase(), (e) -> new HashSet<>());
                TileAEMonitor.tilesInTheWorld.get(password.toLowerCase()).add(tileEntity.getPosTuple());
                ((EntityPlayer) entityPlayerLike).addChatMessage(new ChatComponentText("password:" + password));
            }
            if (tileEntity.getDirectionAETile() == null) {
                for (ForgeDirection value : ForgeDirection.values()) {
                    TileEntity te = world.getTileEntity(x + value.offsetX, y + value.offsetY, z + value.offsetZ);
                    if (te instanceof IGridProxyable) {
                        tileEntity.setDirectionAETile(value);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public TileEntity createNewTileEntity(World world, int i) {
        return new TileAEMonitor();
    }

    @Override
    public void registerBlockIcons(IIconRegister p_149651_1_) {
        blockIcon = p_149651_1_.registerIcon(Tags.MODID+":"+getUnlocalizedName());
    }

    @Override
    public IIcon getIcon(IBlockAccess p_149673_1_, int p_149673_2_, int p_149673_3_, int p_149673_4_, int p_149673_5_) {
        return blockIcon;
    }
}
