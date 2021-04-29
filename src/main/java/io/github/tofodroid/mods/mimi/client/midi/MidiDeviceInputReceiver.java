package io.github.tofodroid.mods.mimi.client.midi;

import javax.sound.midi.ShortMessage;

import io.github.tofodroid.mods.mimi.common.MIMIMod;
import net.minecraft.entity.player.PlayerEntity;

public class MidiDeviceInputReceiver extends MidiInputReceiver {

    @Override
    protected void handleMessage(ShortMessage message, PlayerEntity player) {
        if(MIMIMod.proxy.getMidiInput().hasEnabledTransmitter(player)) {
            handleMessageMaestro(message);
        }

        MIMIMod.proxy.getMidiInput().getLocalInstrumentsToPlay(new Integer(message.getChannel()).byteValue()).forEach(instrument -> {
            handleMessageMidi(message, player, instrument);
        });
    } 
}
