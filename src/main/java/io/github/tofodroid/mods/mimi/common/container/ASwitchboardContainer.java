package io.github.tofodroid.mods.mimi.common.container;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.items.ItemStackHandler;

import java.util.UUID;
import io.github.tofodroid.mods.mimi.common.item.ItemMidiSwitchboard;
import io.github.tofodroid.mods.mimi.common.item.ModItems;

public abstract class ASwitchboardContainer extends APlayerInventoryContainer {
	private static final int INVENTORY_PLAYER_START_X = 165;
	private static final int INVENTORY_PLAYER_START_Y = 182;
	private static final int SWITCHBOARD_SLOT_POS_X = 127;
	private static final int SWITCHBOARD_SLOT_POS_Y = 193;

	protected ItemStackHandler targetInventory;

	public ASwitchboardContainer(ContainerType<?> type, int id, PlayerInventory playerInventory) {
        super(type, id, playerInventory);
    }

    protected Slot buildSwitchboardSlot() {
        return new SlotItemHandler(targetInventory, 0, getSwitchboardSlotX(), getSwitchboardSlotY()) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return ModItems.SWITCHBOARD.equals(stack.getItem());
            }
        };
    }

    @Override
	protected Slot buildPlayerSlot(PlayerInventory playerInventory, int slot, int xPos, int yPos) {
		return new Slot(playerInventory, slot, xPos, yPos);
	}
	
	@Override
	protected Integer getPlayerInventoryX() {
		return INVENTORY_PLAYER_START_X;
	}

	@Override
	protected Integer getPlayerInventoryY() {
		return INVENTORY_PLAYER_START_Y;
	}

	protected Integer getSwitchboardSlotX() {
		return SWITCHBOARD_SLOT_POS_X;
	}

	protected Integer getSwitchboardSlotY() {
		return SWITCHBOARD_SLOT_POS_Y;
	}

	public Boolean updateSelectedSwitchboard(ServerPlayerEntity player, UUID newSourceId, String newSourceName, Byte newFilterOct, Byte newFilterNote, Boolean newInvertNoteOct, String newChannelString, Byte newInstrumentId, Boolean newInvertInstrument, Boolean newSysInput, Boolean newPublicBroadcast, Byte newBroadcastNote, Byte newVolume) {
		ItemStack selectedStack = this.getSlot(ContainerInstrument.TARGET_CONTAINER_MIN_SLOT_ID).getStack();

		if(ModItems.SWITCHBOARD.equals(selectedStack.getItem())) {
			ItemMidiSwitchboard.setMidiSource(selectedStack, newSourceId, newSourceName);
			ItemMidiSwitchboard.setFilterOct(selectedStack, newFilterOct);
			ItemMidiSwitchboard.setFilterNote(selectedStack, newFilterNote);
			ItemMidiSwitchboard.setInvertNoteOct(selectedStack, newInvertNoteOct);
			ItemMidiSwitchboard.setEnabledChannelsString(selectedStack, newChannelString);
			ItemMidiSwitchboard.setInstrument(selectedStack, newInstrumentId);
			ItemMidiSwitchboard.setInvertInstrument(selectedStack, newInvertInstrument);
			ItemMidiSwitchboard.setSysInput(selectedStack, newSysInput);
			ItemMidiSwitchboard.setPublicBroadcast(selectedStack, newPublicBroadcast);
			ItemMidiSwitchboard.setBroadcastNote(selectedStack, newBroadcastNote);
			ItemMidiSwitchboard.setInstrumentVolume(selectedStack, newVolume);
			this.detectAndSendChanges();
            return true;
		}

        return false;
	}

	public ItemStack getSelectedSwitchboard() {
		return this.getSlot(APlayerInventoryContainer.TARGET_CONTAINER_MIN_SLOT_ID).getStack();
	}
	
	@Override
	public ItemStack transferStackInSlot(PlayerEntity playerIn, int index) {
		// Below code taken from Vanilla Chest Container
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = this.inventorySlots.get(index);
		if (slot != null && slot.getHasStack()) {
			ItemStack itemstack1 = slot.getStack();
			itemstack = itemstack1.copy();

			// Return Empty Stack if Cannot Merge
			if (index >= TARGET_CONTAINER_MIN_SLOT_ID) {
				// Target --> Player
				if (!this.mergeItemStack(itemstack1, 0, TARGET_CONTAINER_MIN_SLOT_ID-1, false)) {
					return ItemStack.EMPTY;
				}
			} else {
				// Player --> Target
				if (!this.mergeItemStack(itemstack1, TARGET_CONTAINER_MIN_SLOT_ID, TARGET_CONTAINER_MIN_SLOT_ID+targetInventory.getSlots(), false)) {
					return ItemStack.EMPTY;
				}
			}

			if (itemstack1.isEmpty()) {
				slot.putStack(ItemStack.EMPTY);
			} else {
				slot.onSlotChanged();
			}
		}

      return itemstack;
	}
	
	public ItemStackHandler getTargetInventory() {
		return targetInventory;
	}
}