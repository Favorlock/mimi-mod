package io.github.tofodroid.mods.mimi.client.midi;

import javax.sound.midi.ShortMessage;

import io.github.tofodroid.mods.mimi.common.MIMIMod;
import io.github.tofodroid.mods.mimi.common.network.MidiNotePacket;
import io.github.tofodroid.mods.mimi.common.network.NetworkManager;
import net.minecraft.entity.player.PlayerEntity;

public class MidiDeviceInputReceiver extends MidiInputReceiver {
    @Override
    protected void handleMessage(ShortMessage message, PlayerEntity player) {
        MIMIMod.proxy.getMidiInput().getLocalInstrumentsForMidiDevice(player, new Integer(message.getChannel()).byteValue()).forEach(instrument -> {
            if(isNoteOnMessage(message)) {
                handleMidiNoteOn(new Integer(message.getChannel()).byteValue(), instrument, message.getMessage()[1], message.getMessage()[2], player);
            } else if(isNoteOffMessage(message)) {
                handleMidiNoteOff(new Integer(message.getChannel()).byteValue(), instrument, message.getMessage()[1], player);
            } else if(isAllNotesOffMessage(message)) {
                handleMidiNoteOff(new Integer(message.getChannel()).byteValue(), instrument, MidiNotePacket.ALL_NOTES_OFF, player);
            }
        });
    }
    
    public void handleMidiNoteOn(Byte channel, Byte instrument, Byte midiNote, Byte velocity, PlayerEntity player) {
        MidiNotePacket packet = new MidiNotePacket(midiNote, velocity, instrument, player.getUniqueID(), false, player.getPosition());
        NetworkManager.NET_CHANNEL.sendToServer(packet);
        MIMIMod.proxy.getMidiSynth().handlePacket(packet);
    }
    
    public void handleMidiNoteOff(Byte channel, Byte instrument, Byte midiNote, PlayerEntity player) {
        MidiNotePacket packet = new MidiNotePacket(midiNote, Integer.valueOf(0).byteValue(), instrument, player.getUniqueID(), false, player.getPosition());
        NetworkManager.NET_CHANNEL.sendToServer(packet);
        MIMIMod.proxy.getMidiSynth().handlePacket(packet);
    }
}
