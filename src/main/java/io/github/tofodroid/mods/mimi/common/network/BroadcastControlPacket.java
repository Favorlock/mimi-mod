package io.github.tofodroid.mods.mimi.common.network;

import io.github.tofodroid.mods.mimi.common.MIMIMod;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;

public class BroadcastControlPacket {
    public enum CONTROL {
        PLAY,
        PAUSE,
        STOP,
        TOGGLE_PUBLIC,
        UNKNOWN;

        public static CONTROL fromByte(byte b) {
            try {
                return CONTROL.values()[b];
            } catch(Exception e) {}
            return CONTROL.UNKNOWN;
        }
    };
    
    public final CONTROL control;

    public BroadcastControlPacket(CONTROL control) {
        this.control = control != null ? control : CONTROL.UNKNOWN;
    }
    
    public static BroadcastControlPacket decodePacket(FriendlyByteBuf buf) {
        try {
            byte control = buf.readByte();

            return new BroadcastControlPacket(CONTROL.fromByte(control));
        } catch(IndexOutOfBoundsException e) {
            MIMIMod.LOGGER.error("BroadcasterControlPacket did not contain enough bytes. Exception: " + e);
            return null;
        } catch(DecoderException e) {
            MIMIMod.LOGGER.error("BroadcasterControlPacket contained invalid bytes. Exception: " + e);
            return null;
        }
    }

    public static void encodePacket(BroadcastControlPacket pkt, FriendlyByteBuf buf) {
        buf.writeByte(Integer.valueOf(pkt.control.ordinal()).byteValue());
    }
}
