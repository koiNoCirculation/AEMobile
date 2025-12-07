package dev.youtiao.aemobile.web.mixins;

import codechicken.nei.config.GuiItemIconDumper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dev.youtiao.aemobile.blocks.TileAEMonitor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Mixin(GuiItemIconDumper.class)
@SideOnly(Side.CLIENT)
public class MixinGuiitemIconDumper {
    @Overwrite(remap = false)
    public void exportImage(File dir, BufferedImage img, ItemStack stack) throws IOException {
        String name = Item.itemRegistry.getNameForObject(stack.getItem()) + "_" + stack.getItemDamage() + "_tag_" + TileAEMonitor.writeNBTAsBase64(stack.getTagCompound());
        name = name.replace("/", "_").replace(":", "_");
        File file = new File(dir, name + ".png");
        ImageIO.write(img, "png", file);
        name = Item.itemRegistry.getNameForObject(stack.getItem()) + "_" + stack.getItemDamage() + "_tag_" + "null";
        name = name.replace("/", "_").replace(":", "_");
        file = new File(dir, name + ".png");
        ImageIO.write(img, "png", file);
    }
}
