package dev.youtiao.aemobile;

import appeng.core.AppEng;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.registry.FMLControlledNamespacedRegistry;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameRegistry;
import dev.youtiao.aemobile.blocks.BlockAEMonitor;
import dev.youtiao.aemobile.blocks.TileAEMonitor;
import dev.youtiao.aemobile.web.WebApplication;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;

import static net.minecraft.block.Block.soundTypePiston;

public class CommonProxy {

    // preInit "Run before anything else. Read your config, create blocks, items,
    // etc, and register them with the GameRegistry."
    private static Block aemonitor;
    public void preInit(FMLPreInitializationEvent event) 	{
        Config.syncronizeConfiguration(event.getSuggestedConfigurationFile());

        AEMobile.info(Config.greeting);
        AEMobile.info("I am " + Tags.MODNAME + " at version " + Tags.VERSION + " and group name " + Tags.GROUPNAME);
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes."
    public void init(FMLInitializationEvent event) {
        FMLControlledNamespacedRegistry<Block> blockRegistry = GameData.getBlockRegistry();
        aemonitor = new BlockAEMonitor(Material.anvil).
                setHardness(3.5F).
                setStepSound(soundTypePiston).
                setBlockName("block.aemonitor").
                setCreativeTab(CreativeTabs.tabDecorations);
        GameRegistry.registerBlock(aemonitor,  "aemonitor");
        GameRegistry.registerTileEntity(TileAEMonitor.class, "aemonitor");
    }

    // postInit "Handle interaction with other mods, complete your setup based on this."
    public void postInit(FMLPostInitializationEvent event) {
        CraftingManager.getInstance().addRecipe(
                new ItemStack(Item.getItemFromBlock(aemonitor), 1), new Object[] {"X#X", "#O#", "X#X", 'X',
                        Item.itemRegistry.getObject("appliedenergistics2:tile.BlockController"),'O',
                        Item.itemRegistry.getObject("appliedenergistics2:item.ItemBasicStorageCell.16k")
                        ,'#',Item.itemRegistry.getObject("appliedenergistics2:tile.BlockFluix")}
        );
    }

    public void serverAboutToStart(FMLServerAboutToStartEvent event) {

    }

    // register server commands in this event handler
    public void serverStarting(FMLServerStartingEvent event) {

    }

    public void serverStarted(FMLServerStartedEvent event) {
        if(event.getSide().isServer()) {
            WebApplication webApplication = new WebApplication();
            webApplication.setDaemon(false);
            webApplication.start();
        }
    }

    public void serverStopping(FMLServerStoppingEvent event) {

    }

    public void serverStopped(FMLServerStoppedEvent event) {

    }
}
