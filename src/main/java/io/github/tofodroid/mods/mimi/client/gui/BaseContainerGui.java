package io.github.tofodroid.mods.mimi.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.mojang.blaze3d.platform.InputConstants;

import io.github.tofodroid.mods.mimi.common.MIMIMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public abstract class BaseContainerGui<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    protected static final Integer STANDARD_BUTTON_SIZE = 15;

    protected ResourceLocation guiTexture;
    protected Integer TEXTURE_SIZE;
    protected Integer GUI_WIDTH;
    protected Integer GUI_HEIGHT;
    protected Integer START_X;
    protected Integer START_Y;

    public BaseContainerGui(T container, Inventory inv, Integer gWidth, Integer gHeight, Integer textureSize, String textureResource, Component textComponent) {
        super(container, inv, textComponent);
        this.guiTexture = new ResourceLocation(MIMIMod.MODID, textureResource);
        this.TEXTURE_SIZE = textureSize;
        this.passEvents = false;
        this.GUI_HEIGHT = gHeight;
        this.GUI_WIDTH = gWidth;
        this.imageWidth = textureSize;
        this.imageHeight = textureSize;
    }

    @Override
    public void init() {
        START_X = (this.width - GUI_WIDTH) / 2;
        START_Y = Math.round((this.height - GUI_HEIGHT) / 1.25f);
        this.leftPos = START_X;
        this.topPos = START_Y;
    }

    @Override
    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        matrixStack = renderGraphics(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        matrixStack = renderText(matrixStack, mouseX, mouseY);
    }

	@Override
	public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if(shouldRenderBackground()) {
            this.renderBackground(matrixStack);
        }
		super.render(matrixStack, mouseX, mouseY, partialTicks);
		this.renderTooltip(matrixStack, mouseX, mouseY);
	}

    protected Boolean shouldRenderBackground() {
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        InputConstants.Key invKey = InputConstants.getKey(keyCode, scanCode);

        if (!this.minecraft.options.keyInventory.isActiveAndMatches(invKey) && super.keyPressed(keyCode, scanCode, modifiers)) {
           return true;
        }

        return false;
    }

    protected void setAlpha(float alpha) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
    }

    protected abstract PoseStack renderGraphics(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks);
    protected abstract PoseStack renderText(PoseStack matrixStack, int mouseX, int mouseY);

    protected Boolean clickedBox(Integer mouseX, Integer mouseY, Vector3f buttonPos) {
        Integer buttonMinX = START_X + Float.valueOf(buttonPos.x()).intValue();
        Integer buttonMaxX = buttonMinX + STANDARD_BUTTON_SIZE;
        Integer buttonMinY = START_Y + Float.valueOf(buttonPos.y()).intValue();
        Integer buttonMaxY = buttonMinY + STANDARD_BUTTON_SIZE;

        Boolean result = mouseX >= buttonMinX && mouseX <= buttonMaxX && mouseY >= buttonMinY && mouseY <= buttonMaxY;

        if(result) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }

        return result;
    }
    
    protected Boolean clickedBox(Integer mouseX, Integer mouseY, Vector3f buttonPos, Vector3f buttonSize) {
        Integer buttonMinX = START_X + Float.valueOf(buttonPos.x()).intValue();
        Integer buttonMaxX = buttonMinX + Float.valueOf(buttonSize.x()).intValue();
        Integer buttonMinY = START_Y + Float.valueOf(buttonPos.y()).intValue();
        Integer buttonMaxY = buttonMinY + Float.valueOf(buttonSize.y()).intValue();

        Boolean result = mouseX >= buttonMinX && mouseX <= buttonMaxX && mouseY >= buttonMinY && mouseY <= buttonMaxY;

        if(result) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }

        return result;
    }
}
