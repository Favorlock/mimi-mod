package io.github.tofodroid.mods.mimi.common.tile;

import java.util.Arrays;
import java.util.UUID;

import io.github.tofodroid.mods.mimi.common.container.ContainerMechanicalMaestro;
import io.github.tofodroid.mods.mimi.common.entity.EntityNoteResponsiveTile;
import io.github.tofodroid.mods.mimi.common.inventory.MechanicalMaestroInventoryStackHandler;
import io.github.tofodroid.mods.mimi.common.item.ItemInstrument;
import io.github.tofodroid.mods.mimi.common.item.ItemInstrumentBlock;
import io.github.tofodroid.mods.mimi.common.item.ItemMidiSwitchboard;
import io.github.tofodroid.mods.mimi.common.item.ModItems;
import io.github.tofodroid.mods.mimi.common.network.MidiNotePacket;
import io.github.tofodroid.mods.mimi.common.network.MidiNotePacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;

public class TileMechanicalMaestro extends ANoteResponsiveTile {
    public static final UUID MECH_UUID = new UUID(0,3);

    public Boolean hasEntity;

    public TileMechanicalMaestro(BlockPos pos, BlockState state) {
        super(ModTiles.MECHANICALMAESTRO, pos, state, 2);
    }

    @Override
    public LazyOptional<? extends ItemStackHandler> buildInventory() {
        return LazyOptional.of(() -> new MechanicalMaestroInventoryStackHandler(INVENTORY_SIZE));
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player playerEntity) {
        return new ContainerMechanicalMaestro(id, playerInventory, this.getBlockPos());
    }

    @Override
    public Component getDisplayName() {
		return new TranslatableComponent(this.getBlockState().getBlock().asItem().getDescriptionId());
    }

    public ItemStack getSwitchboardStack() {
        if(this.inventory.isPresent() && ModItems.SWITCHBOARD.equals(this.inventory.orElse(null).getStackInSlot(0).getItem())) {
            return this.inventory.orElse(null).getStackInSlot(0);
        }

        return ItemStack.EMPTY;
    }

    public ItemStack getInstrumentStack() {
        if(this.inventory.isPresent() && this.inventory.orElse(null).getStackInSlot(1).getItem() instanceof ItemInstrument || this.inventory.orElse(null).getStackInSlot(1).getItem() instanceof ItemInstrumentBlock) {
            return this.inventory.orElse(null).getStackInSlot(1);
        }

        return ItemStack.EMPTY;
    }

    public Byte getInstrumentId() {
        ItemStack instrumentStack = this.getInstrumentStack();

        if(!instrumentStack.isEmpty()) {
            if(instrumentStack.getItem() instanceof ItemInstrument) {
                return ((ItemInstrument)instrumentStack.getItem()).getInstrumentId();
            } else if(instrumentStack.getItem() instanceof ItemInstrumentBlock) {
                return ((ItemInstrumentBlock)instrumentStack.getItem()).getInstrumentId();
            }
        }

        return null;
    }

    public UUID getMaestroUUID() {
        String idString = "tile-mech-maestro-" + this.getBlockPos().getX() + "-" + this.getBlockPos().getY() + "-" + this.getBlockPos().getZ();
        return UUID.nameUUIDFromBytes(idString.getBytes());
    }

    public Boolean shouldHandleMessage(UUID sender, Byte channel, Byte note, Boolean publicTransmit) {
        ItemStack switchStack = getSwitchboardStack();
        if(!switchStack.isEmpty() && getInstrumentId() != null) {
            return ItemMidiSwitchboard.isChannelEnabled(switchStack, channel) &&
                ( 
                    (publicTransmit && ItemMidiSwitchboard.PUBLIC_SOURCE_ID.equals(ItemMidiSwitchboard.getMidiSource(switchStack))) 
                    || (sender != null && sender.equals(ItemMidiSwitchboard.getMidiSource(switchStack)))
                );
        }
        return false;
    }
    
    @Override
    public void tick(Level world, BlockPos pos, BlockState state, ANoteResponsiveTile self) {
        if(tickCount >= UPDATE_EVERY_TICKS) {
            tickCount = 0;
            if(this.hasLevel() && !this.level.isClientSide && !this.isRemoved()) {
                if(this.shouldHaveEntity()) {
                    EntityNoteResponsiveTile.create(this.level, this.getBlockPos());
                } else {
                    if(EntityNoteResponsiveTile.remove(this.level, this.getBlockPos())) {
                        this.allNotesOff();
                    }
                }
            }
        } else {
            tickCount ++;
        }        
    }

    @Override
    protected Boolean shouldHaveEntity() {
        return !this.getInstrumentStack().isEmpty() && !this.getSwitchboardStack().isEmpty() && this.level.hasNeighborSignal(this.getBlockPos());
    }
    
    public void allNotesOff() {
		if(this.getInstrumentId() != null && this.getLevel() instanceof ServerLevel) {
			MidiNotePacketHandler.handlePacketsServer(
                Arrays.asList(new MidiNotePacket(MidiNotePacket.ALL_NOTES_OFF, Integer.valueOf(0).byteValue(), this.getInstrumentId(), this.getMaestroUUID(), true, this.getBlockPos())),
                (ServerLevel)this.getLevel(),
                null
            );
		}
    }
}

