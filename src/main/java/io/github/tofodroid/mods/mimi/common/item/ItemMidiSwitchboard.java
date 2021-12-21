package io.github.tofodroid.mods.mimi.common.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SortedArraySet;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import io.github.tofodroid.mods.mimi.common.midi.MidiChannelNumber;
import io.github.tofodroid.mods.mimi.common.network.SwitchboardStackUpdatePacket;

public class ItemMidiSwitchboard extends Item {
    public static final String FILTER_NOTE_TAG = "filter_note";
    public static final String FILTER_OCT_TAG = "filter_oct";
    public static final String INVERT_NOTE_OCT_TAG = "invert_note_oct";
    public static final Byte FILTER_NOTE_OCT_ALL = -1;
    public static final String BROADCAST_NOTE_TAG = "broadcast_note";
    public static final String BROADCAST_PUBLIC_TAG = "broadcast_public";
    public static final String SOURCE_TAG = "source_uuid";
    public static final String SOURCE_NAME_TAG = "source_name";
    public static final String SYS_INPUT_TAG = "sys_input";
    public static final String ENABLED_CHANNELS_TAG = "enabled_channels";
    public static final String INSTRUMENT_TAG = "filter_instrument";
    public static final String INVERT_INSTRUMENT_TAG = "invert_instrument";
    public static final String VOLUME_TAG = "instrument_volume";
    public static final Byte INSTRUMENT_ALL = -1;
    public static final Byte MAX_INSTRUMENT_VOLUME = 5;
    public static final Byte DEFAULT_INSTRUMENT_VOLUME = 3;
    public static final Byte MIN_INSTRUMENT_VOLUME = 0;

    public static final UUID NONE_SOURCE_ID = new UUID(0,0);
    public static final UUID PUBLIC_SOURCE_ID = new UUID(0,2);
    public static final String ALL_CHANNELS_STRING = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16";

    private Map<Byte,String> INSTRUMENT_NAME_MAP = null;

    public ItemMidiSwitchboard() {
        super(new Properties().group(ModItems.ITEM_GROUP).maxStackSize(1));
        this.setRegistryName("switchboard");
    }

    @Override
    public ActionResultType itemInteractionForEntity(ItemStack stack, PlayerEntity playerIn, LivingEntity target, Hand hand) {
        // Server-side only
        if(!playerIn.getEntityWorld().isRemote()) {  
            if(target instanceof PlayerEntity) {
                ItemMidiSwitchboard.setMidiSource(stack, target.getUniqueID(), target.getName().getString());
                playerIn.setHeldItem(hand, stack);
                playerIn.sendStatusMessage(new StringTextComponent("Set MIDI Source to: " +  target.getName().getString()), true);
                return ActionResultType.CONSUME;
            }
        } else {
            // Cancel click event client side
            if(target instanceof PlayerEntity) {
                return ActionResultType.SUCCESS;
            }
        }
        
        return ActionResultType.PASS;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);

        // Client-side only
        if(worldIn != null && worldIn.isRemote) {
            
            tooltip.add(new StringTextComponent("----------------"));

            // MIDI Source Filter
            tooltip.add(new StringTextComponent("Transmitter: " + ItemMidiSwitchboard.getMidiSourceName(stack)));

            if(ItemMidiSwitchboard.getSysInput(stack)) {
                tooltip.add(new StringTextComponent("System MIDI Device: Enabled"));
            } else {
                tooltip.add(new StringTextComponent("System MIDI Device: Disabled"));
            }

            // MIDI Channels Filter
            SortedArraySet<Byte> acceptedChannels = ItemMidiSwitchboard.getEnabledChannelsSet(stack);
            if(acceptedChannels != null && !acceptedChannels.isEmpty()) {
                if(acceptedChannels.size() == MidiChannelNumber.values().length) {
                    tooltip.add(new StringTextComponent("Channels: All"));
                } else {
                    tooltip.add(new StringTextComponent("Channels: " + acceptedChannels.stream().map(c -> new Integer(c.intValue()+1).toString()).collect(Collectors.joining(", "))));
                }
            } else {
                tooltip.add(new StringTextComponent("Channels: None"));
            }

            // MIDI Note Filter
            tooltip.add(new StringTextComponent("Note Filter: " + (ItemMidiSwitchboard.getInvertNoteOct(stack) ? "All except " : "") + ItemMidiSwitchboard.getFilteredNotesAsString(stack)));

            // Instrument Filter
            tooltip.add(new StringTextComponent("Instrument Filter: " + (ItemMidiSwitchboard.getInvertInstrument(stack) ? "All except " : "") + getInstrumentName(stack)));

            // Instrument Volume
            tooltip.add(new StringTextComponent("Instrument Volume: " + ItemMidiSwitchboard.getInstrumentVolumePercent(stack)));
        }
    }

    // DATA FUNCTIONS
    public static void setMidiSource(ItemStack stack, UUID sourceId, String sourceName) {
        if (sourceId != null && sourceName != null) {
            stack.getOrCreateTag().putUniqueId(SOURCE_TAG, sourceId);
            stack.getOrCreateTag().putString(SOURCE_NAME_TAG, sourceName);
        } else if (stack.hasTag()) {
            stack.getTag().remove(SOURCE_TAG);
            stack.getTag().remove(SOURCE_NAME_TAG);
        }
    }

    public static UUID getMidiSource(ItemStack stack) {
        if (stackTagContainsKey(stack, SOURCE_TAG)) {
            return stack.getTag().getUniqueId(SOURCE_TAG);
        }

        return null;
    }

    public static String getMidiSourceName(ItemStack stack) {
        if(stackTagContainsKey(stack, SOURCE_NAME_TAG)) {
            return stack.getTag().getString(SOURCE_NAME_TAG);
        }else if(getMidiSource(stack) != null) {
            return "Player";
        }

        return "None";
    }

    public static void setEnabledChannelsString(ItemStack stack, String acceptedChannelsString) {
        if (acceptedChannelsString != null && !acceptedChannelsString.trim().isEmpty()) {
            stack.getOrCreateTag().putString(ENABLED_CHANNELS_TAG, acceptedChannelsString);
        } else if (stack.hasTag()) {
            stack.getTag().remove(ENABLED_CHANNELS_TAG);
        }
    }

    public static String getEnabledChannelsString(ItemStack stack) {
        if (stackTagContainsKey(stack, ENABLED_CHANNELS_TAG)) {
            return stack.getTag().getString(ENABLED_CHANNELS_TAG);
        }

        return null;
    }

    public static void toggleChannel(ItemStack switchStack, Byte channelId) {
        if(channelId != null && channelId < 16 && channelId >= 0) {
            SortedArraySet<Byte> acceptedChannels = getEnabledChannelsSet(switchStack);

            if(acceptedChannels == null) {
                acceptedChannels = SortedArraySet.newSet(16);
            }

            if(isChannelEnabled(switchStack, channelId)) {
                acceptedChannels.remove(channelId);
            } else {
                acceptedChannels.add(channelId);
            }

            String acceptedChannelsString = acceptedChannels.stream().map(b -> new Integer(b + 1).toString()).collect(Collectors.joining(","));
            setEnabledChannelsString(switchStack, acceptedChannelsString);
        }
    }
    
    public static void clearEnabledChannels(ItemStack switchStack) {
        setEnabledChannelsString(switchStack, null);
    }
    
    public static void setEnableAllChannels(ItemStack switchStack) {
        setEnabledChannelsString(switchStack, ALL_CHANNELS_STRING);
    }

    public static Boolean isChannelEnabled(ItemStack switchStack, Byte channelId) {
        return getEnabledChannelsSet(switchStack).contains(channelId);
    }
    
    public static SortedArraySet<Byte> getEnabledChannelsSet(ItemStack switchStack) {
        String acceptedChannelString = getEnabledChannelsString(switchStack);

        if(acceptedChannelString != null && !acceptedChannelString.isEmpty()) {
            SortedArraySet<Byte> result = SortedArraySet.newSet(16);
            result.addAll(Arrays.asList(acceptedChannelString.split(",", -1)).stream().map(b -> new Integer(Byte.valueOf(b) - 1).byteValue()).collect(Collectors.toSet()));
            return result;
        }

        return SortedArraySet.newSet(0);
    }

    public static void setFilterOct(ItemStack stack, Byte oct) {
        if (oct >= 0) {
            stack.getOrCreateTag().putByte(FILTER_OCT_TAG, oct);
        } else if (stack.hasTag()) {
            stack.getTag().remove(FILTER_OCT_TAG);
        }
    }
    
    public static void setFilterNote(ItemStack stack, Byte note) {
        if (note >= 0) {
            stack.getOrCreateTag().putByte(FILTER_NOTE_TAG, note);
        } else if (stack.hasTag()) {
            stack.getTag().remove(FILTER_NOTE_TAG);
        }
    }

    public static Byte getFilterOct(ItemStack stack) {
        if (stackTagContainsKey(stack, FILTER_OCT_TAG)) {
            return stack.getTag().getByte(FILTER_OCT_TAG);
        }

        return FILTER_NOTE_OCT_ALL;
    }

    public static Byte getFilterNote(ItemStack stack) {
        if (stackTagContainsKey(stack, FILTER_NOTE_TAG)) {
            return stack.getTag().getByte(FILTER_NOTE_TAG);
        }

        return FILTER_NOTE_OCT_ALL;
    }

    public static void setBroadcastNote(ItemStack stack, Byte note) {
        if (note >= 0) {
            stack.getOrCreateTag().putByte(BROADCAST_NOTE_TAG, note);
        } else if (stack.hasTag()) {
            stack.getTag().remove(BROADCAST_NOTE_TAG);
        }
    }

    public static Byte getBroadcastNote(ItemStack stack) {
        if (stackTagContainsKey(stack, BROADCAST_NOTE_TAG)) {
            return stack.getTag().getByte(BROADCAST_NOTE_TAG);
        }

        return 0;
    }
    
    public static void setPublicBroadcast(ItemStack stack, Boolean pub) {
        if (pub) {
            stack.getOrCreateTag().putBoolean(BROADCAST_PUBLIC_TAG, pub);
        } else if (stack.hasTag()) {
            stack.getTag().remove(BROADCAST_PUBLIC_TAG);
        }
    }

    public static Boolean getPublicBroadcast(ItemStack stack) {
        if (stackTagContainsKey(stack, BROADCAST_PUBLIC_TAG)) {
            return stack.getTag().getBoolean(BROADCAST_PUBLIC_TAG);
        }

        return false;
    }

    public static void setInstrument(ItemStack stack, Byte instrumentId) {
        if (instrumentId >= 0) {
            stack.getOrCreateTag().putByte(INSTRUMENT_TAG, instrumentId);
        } else if (stack.hasTag()) {
            stack.getTag().remove(INSTRUMENT_TAG);
        }
    }

    public static Byte getInstrument(ItemStack stack) {
        if (stackTagContainsKey(stack, INSTRUMENT_TAG)) {
            return stack.getTag().getByte(INSTRUMENT_TAG);
        }

        return INSTRUMENT_ALL;
    }
    
    public static void setSysInput(ItemStack stack, Boolean sysInput) {
        if (sysInput) {
            stack.getOrCreateTag().putBoolean(SYS_INPUT_TAG, sysInput);
        } else if (stack.hasTag()) {
            stack.getTag().remove(SYS_INPUT_TAG);
        }
    }

    public static Boolean getSysInput(ItemStack stack) {
        if (stackTagContainsKey(stack, SYS_INPUT_TAG)) {
            return stack.getTag().getBoolean(SYS_INPUT_TAG);
        }

        return false;
    }
    
    public static void setInvertNoteOct(ItemStack stack, Boolean invert) {
        if (invert) {
            stack.getOrCreateTag().putBoolean(INVERT_NOTE_OCT_TAG, invert);
        } else if (stack.hasTag()) {
            stack.getTag().remove(INVERT_NOTE_OCT_TAG);
        }
    }

    public static Boolean getInvertNoteOct(ItemStack stack) {
        if (stackTagContainsKey(stack, INVERT_NOTE_OCT_TAG)) {
            return stack.getTag().getBoolean(INVERT_NOTE_OCT_TAG);
        }

        return false;
    }

    public static void setInvertInstrument(ItemStack stack, Boolean invert) {
        if (invert) {
            stack.getOrCreateTag().putBoolean(INVERT_INSTRUMENT_TAG, invert);
        } else if (stack.hasTag()) {
            stack.getTag().remove(INVERT_INSTRUMENT_TAG);
        }
    }

    public static Boolean getInvertInstrument(ItemStack stack) {
        if (stackTagContainsKey(stack, INVERT_INSTRUMENT_TAG)) {
            return stack.getTag().getBoolean(INVERT_INSTRUMENT_TAG);
        }

        return false;
    }

    public static void setInstrumentVolume(ItemStack stack, Byte volume) {
        if(volume > MAX_INSTRUMENT_VOLUME) {
            volume = MAX_INSTRUMENT_VOLUME;
        } else if(volume < MIN_INSTRUMENT_VOLUME) {
            volume = MIN_INSTRUMENT_VOLUME;
        }

        stack.getOrCreateTag().putByte(VOLUME_TAG, volume);
    }

    public static Byte getInstrumentVolume(ItemStack stack) {
        if (stackTagContainsKey(stack, VOLUME_TAG)) {
            return stack.getTag().getByte(VOLUME_TAG);
        }

        return DEFAULT_INSTRUMENT_VOLUME;
    }

    public static String getInstrumentVolumePercent(ItemStack stack) {
        Integer value = new Integer(new Double((new Double(getInstrumentVolume(stack)) / new Double(MAX_INSTRUMENT_VOLUME)) * 10).intValue());
        return value == 10 ? value.toString() : "0" + value.toString();
    }

    public static Byte applyVolume(ItemStack stack, Byte sourceVelocity) {
        Byte result = sourceVelocity;

        if(stack != null && stack.getItem().equals(ModItems.SWITCHBOARD)) {
            result = new Integer(new Double((new Double(getInstrumentVolume(stack)) / new Double(MAX_INSTRUMENT_VOLUME)) * sourceVelocity).intValue()).byteValue();
        }

        return  result;
    }

    public String getInstrumentName(ItemStack stack) {
        return INSTRUMENT_NAME_MAP().get(new Integer(ItemMidiSwitchboard.getInstrument(stack)).byteValue());
    }

    public static List<Byte> getFilterNotes(ItemStack stack) {
        List<Byte> result = new ArrayList<>();
        Byte oct = getFilterOct(stack);
        Byte note = getFilterNote(stack);

        if(oct != FILTER_NOTE_OCT_ALL && note != FILTER_NOTE_OCT_ALL && new Integer(oct*12+note) <= Byte.MAX_VALUE) {
            result.add(new Integer(oct*12+note).byteValue());
        } else if(oct != FILTER_NOTE_OCT_ALL) {
            for(int i = 0; i < 12; i++) {
                if(new Integer(oct*12+i) <= Byte.MAX_VALUE) {
                    result.add(new Integer(oct*12+i).byteValue());
                }
            }
        } else if(note != FILTER_NOTE_OCT_ALL) {
            for(int i = 0; i < 10; i++) {
                if(new Integer(i*12+note) <= Byte.MAX_VALUE) {
                    result.add(new Integer(i*12+note).byteValue());
                }
            }
        }

        return result;
    }
    
    public static String getFilteredNotesAsString(ItemStack stack) {
        Byte filterNoteLetter = getFilterNote(stack);
        Byte filterNoteOctave = getFilterOct(stack);
        String filterNoteString = noteLetterFromNum(filterNoteLetter) + (filterNoteOctave != FILTER_NOTE_OCT_ALL ? filterNoteOctave : "*");
        return "**".equals(filterNoteString) ? "All" : filterNoteString;
    }

    public static String getBroadcastNoteAsString(ItemStack stack) {
        String result = "None";

        if(getBroadcastNote(stack) != null) {
            Byte filterNoteLetter = new Integer(getBroadcastNote(stack) % 12).byteValue();
            Byte filterNoteOctave = new Integer(getBroadcastNote(stack) / 12).byteValue();
            result = noteLetterFromNum(filterNoteLetter) + filterNoteOctave;
        }

        return result;
    }

    public static String noteLetterFromNum(Byte octaveNoteNum) {
        switch(octaveNoteNum) {
            case -1:
                return "*";
            case 0:
                return "C";
            case 1:
                return "C#";
            case 2:
                return "D";
            case 3:
                return "D#";
            case 4:
                return "E";
            case 5:
                return "F";
            case 6:
                return "F#";
            case 7:
                return "G";
            case 8:
                return "G#";
            case 9:
                return "A";
            case 10:
                return "A#";
            case 11:
                return "B";
        }

        return "";
    }

    public static Boolean isNoteFiltered(ItemStack stack, Byte note) {
        List<Byte> filteredNotes = getFilterNotes(stack);
        return filteredNotes.isEmpty() ? !getInvertNoteOct(stack) : getInvertNoteOct(stack) ? !filteredNotes.contains(note) : filteredNotes.contains(note);
    }

    public static Boolean isInstrumentFiltered(ItemStack stack, Byte instrument) {
        Byte instrumentId = getInstrument(stack);
        return instrumentId.equals(INSTRUMENT_ALL) ? !getInvertInstrument(stack) : getInvertInstrument(stack) ? instrumentId != instrument : instrumentId == instrument;
    }

    protected static Boolean stackTagContainsKey(ItemStack stack, String tag) {
        return stack != null && stack.getTag() != null && stack.getTag().contains(tag);
    }

    public static SwitchboardStackUpdatePacket getSyncPacket(ItemStack stack) {
        return new SwitchboardStackUpdatePacket(
            ItemMidiSwitchboard.getMidiSource(stack),
            ItemMidiSwitchboard.getMidiSourceName(stack),
            ItemMidiSwitchboard.getFilterOct(stack),
            ItemMidiSwitchboard.getFilterNote(stack),
            ItemMidiSwitchboard.getInvertNoteOct(stack),
            ItemMidiSwitchboard.getEnabledChannelsString(stack),
            ItemMidiSwitchboard.getInstrument(stack),
            ItemMidiSwitchboard.getInvertInstrument(stack),
            ItemMidiSwitchboard.getSysInput(stack),
            ItemMidiSwitchboard.getPublicBroadcast(stack),
            ItemMidiSwitchboard.getBroadcastNote(stack),
            ItemMidiSwitchboard.getInstrumentVolume(stack)
        );
    }

    public Map<Byte,String> INSTRUMENT_NAME_MAP() {
        if(this.INSTRUMENT_NAME_MAP == null) {
            this.INSTRUMENT_NAME_MAP = loadInstrumentNames();
        }

        return this.INSTRUMENT_NAME_MAP;
    }
    
    protected Map<Byte,String> loadInstrumentNames() {
        Map<Byte,String> result = new HashMap<>();
        result.put(new Integer(-1).byteValue(), "All");
        ModItems.INSTRUMENT_ITEMS.forEach(item -> {
            result.put(item.getInstrumentId(), item.getName().getString());
        });
        ModItems.BLOCK_INSTRUMENT_ITEMS.forEach(item -> {
            result.put(item.getInstrumentId(), item.getBlock().getTranslatedName().getString());
        });
        return result;
    }
}