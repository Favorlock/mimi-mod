package io.github.tofodroid.mods.mimi.client.midi;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.github.tofodroid.mods.mimi.common.block.BlockInstrument;
import io.github.tofodroid.mods.mimi.common.instruments.EntityInstrumentDataUtil;
import io.github.tofodroid.mods.mimi.common.instruments.InstrumentDataUtil;
import io.github.tofodroid.mods.mimi.common.instruments.ItemInstrumentDataUtil;
import io.github.tofodroid.mods.mimi.common.item.ItemInstrument;
import io.github.tofodroid.mods.mimi.common.item.ModItems;
import io.github.tofodroid.mods.mimi.common.tile.TileInstrument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

public class MidiInputManager {
    public final MidiInputDeviceManager inputDeviceManager;
    public final MidiPlaylistManager playlistManager;

    private Boolean hasEnabledTransmitter = false;
    private List<Object> localInstrumentToPlay = new ArrayList<>();

    public MidiInputManager() {
        this.inputDeviceManager = new MidiInputDeviceManager();
        this.playlistManager = new MidiPlaylistManager();
        this.playlistManager.open();
        this.inputDeviceManager.open();
    }

    public Boolean hasEnabledTransmitter() {
        return hasEnabledTransmitter;
    }
    
    public List<Byte> getLocalInstrumentsToPlay(Byte channel) {
        return localInstrumentToPlay.stream().filter(data -> {
            if(data instanceof ItemStack) {
                return ItemInstrumentDataUtil.INSTANCE.doesAcceptChannel((ItemStack)data, channel);
            } else if(data instanceof TileInstrument) {
                return EntityInstrumentDataUtil.INSTANCE.doesAcceptChannel((TileInstrument)data, channel);
            } else {
                return false;
            }
        }).map(data -> {
            if(data instanceof ItemStack) {
                return ItemInstrumentDataUtil.INSTANCE.getInstrumentIdFromData((ItemStack)data);
            } else {
                return EntityInstrumentDataUtil.INSTANCE.getInstrumentIdFromData((TileInstrument)data);
            }
        }).collect(Collectors.toList());
    }
    
    @SubscribeEvent
    public void handleTick(PlayerTickEvent event) {
        if(event.phase != Phase.END || event.side != LogicalSide.CLIENT || !event.player.isUser()) {
            return;
        }

        this.hasEnabledTransmitter = hasEnabledTransmitter(event.player);
        this.localInstrumentToPlay = localInstrumentsToPlay(event.player);
    }
    
    protected Boolean hasEnabledTransmitter(PlayerEntity player) {
        if(player.inventory != null) {
            ItemStack transmitterStack = null;

            // If an active transmitter is on cursor then don't update
            if(player.inventory.getItemStack() != null && ModItems.TRANSMITTER.equals(player.inventory.getItemStack().getItem()) && ModItems.TRANSMITTER.isEnabled(player.inventory.getItemStack())) {
                return hasEnabledTransmitter;
            }

            // Off-hand isn't part of hotbar, so check it explicitly
            if(ModItems.TRANSMITTER.equals(player.getHeldItemOffhand().getItem()) && ModItems.TRANSMITTER.isEnabled(player.getHeldItemOffhand())) {
                transmitterStack = player.getHeldItemOffhand();
            }

            // check hotbar
            for(int i = 0; i < 9; i++) {
                ItemStack invStack = player.inventory.getStackInSlot(i);
                if(transmitterStack == null && ModItems.TRANSMITTER.equals(invStack.getItem()) && ModItems.TRANSMITTER.isEnabled(invStack)) {
                    transmitterStack = player.inventory.getStackInSlot(i);
                }
            }

            return transmitterStack != null;
        }

        return false;
    }

    protected List<Object> localInstrumentsToPlay(PlayerEntity player) {
        List<Object> result = new ArrayList<>();

        // Check for seated instrument
        TileInstrument instrumentEntity = BlockInstrument.getTileInstrumentForEntity(player);
        if(instrumentEntity != null && InstrumentDataUtil.MIDI_MAESTRO_ID.equals(EntityInstrumentDataUtil.INSTANCE.getLinkedMaestro(instrumentEntity))) {
            result.add(instrumentEntity);
        }

        // Check for held instruments
        ItemStack mainHand = ItemInstrument.getEntityHeldInstrumentStack(player, Hand.MAIN_HAND);
        if(mainHand != null && InstrumentDataUtil.MIDI_MAESTRO_ID.equals(ItemInstrumentDataUtil.INSTANCE.getLinkedMaestro(mainHand))) {
            result.add(mainHand);
        }

        ItemStack offHand = ItemInstrument.getEntityHeldInstrumentStack(player, Hand.OFF_HAND);
        if(offHand != null && InstrumentDataUtil.MIDI_MAESTRO_ID.equals(ItemInstrumentDataUtil.INSTANCE.getLinkedMaestro(mainHand))) {
            result.add(offHand);
        }

        return result;
    }
}
