package dev.youtiao.aemobile.web.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

public class FakePlayerGetCraftFailure extends FakePlayer {
    private String errorMessage = null;

    public FakePlayerGetCraftFailure(WorldServer world, GameProfile name) {
        super(world, name);
    }
    public String getErrorMessage() {
        return errorMessage;
    }
    @Override
    public void addChatMessage(IChatComponent p_145747_1_) {
        errorMessage = p_145747_1_.getFormattedText();
    }
}
