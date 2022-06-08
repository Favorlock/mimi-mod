package io.github.tofodroid.mods.mimi.common.tile;

import java.util.UUID;

import io.github.tofodroid.mods.mimi.common.container.ContainerReceiver;
import io.github.tofodroid.mods.mimi.common.item.ItemMidiSwitchboard;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class TileReceiver extends ANoteResponsiveTile {
    public TileReceiver(BlockPos pos, BlockState state) {
        super(ModTiles.RECEIVER, pos, state, 1);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory) {
        return new ContainerReceiver(id, playerInventory, this.getBlockPos());
    }

    @Override
    public Component getDefaultName() {
		return Component.translatable(this.getBlockState().getBlock().asItem().getDescriptionId());
    }

    public Boolean shouldHandleMessage(UUID sender, Byte channel, Byte note, Boolean publicTransmit) {
        ItemStack switchStack = getSwitchboardStack();
        if(!switchStack.isEmpty()) {
            return ItemMidiSwitchboard.isChannelEnabled(switchStack, channel) && shouldAcceptNote(note) &&
                ( 
                    (publicTransmit && ItemMidiSwitchboard.PUBLIC_SOURCE_ID.equals(ItemMidiSwitchboard.getMidiSource(switchStack))) 
                    || (sender != null && sender.equals(ItemMidiSwitchboard.getMidiSource(switchStack)))
                );
        }
        return false;
    }
    
    protected Boolean shouldAcceptNote(Byte note) {
        ItemStack switchStack = getSwitchboardStack();
        return !switchStack.isEmpty() ? ItemMidiSwitchboard.isNoteFiltered(switchStack, note) : false;
    }
    
    @Override
    protected Boolean shouldHaveEntity() {
        return !this.getSwitchboardStack().isEmpty();
    }
}
