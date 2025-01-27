package io.github.tofodroid.mods.mimi.client.gui;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;

import io.github.tofodroid.mods.mimi.client.ClientProxy;
import io.github.tofodroid.mods.mimi.client.midi.MidiInputManager;
import io.github.tofodroid.mods.mimi.common.midi.MidiFileInfo;
import io.github.tofodroid.mods.mimi.common.network.NetworkManager;
import io.github.tofodroid.mods.mimi.common.network.TransmitterNotePacket;
import io.github.tofodroid.mods.mimi.common.network.TransmitterNotePacket.TransmitMode;
import io.github.tofodroid.mods.mimi.common.MIMIMod;
import io.github.tofodroid.mods.mimi.common.config.ModConfigs;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.CommonComponents;

public class GuiMidiFileCaster extends BaseGui {
    // GUI
    private static final Integer DEFAULT_TEXT_FIELD_COLOR = 14737632;
    private EditBox folderPathField;

    // Data
    private String folderPathString;
    
    // Button Boxes
    private static final Vector3f LOAD_FOLDER_BUTTON = new Vector3f(301,37,0);
    private static final Vector3f SAVE_DEFAULT_BUTTON = new Vector3f(320,37,0);
    private static final Vector3f LOAD_DEFAULT_BUTTON = new Vector3f(339,37,0);
    private static final Vector3f PREVIOUS_BUTTON = new Vector3f(14,271,0);
    private static final Vector3f STOP_BUTTON = new Vector3f(33,271,0);
    private static final Vector3f PLAY_PAUSE_BUTTON = new Vector3f(52,271,0);
    private static final Vector3f NEXT_BUTTON = new Vector3f(71,271,0);
    private static final Vector3f LOOP_BUTTON = new Vector3f(90,271,0);
    private static final Vector3f LOOP_SCREEN = new Vector3f(108,272,0);
    private static final Vector3f SHUFFLE_BUTTON = new Vector3f(125,271,0);
    private static final Vector3f SHUFFLE_SCREEN = new Vector3f(143,272,0);
    private static final Vector3f TRANSMIT_BUTTON = new Vector3f(160,271,0);
    private static final Vector3f TRANSMIT_SCREEN = new Vector3f(178,272,0);

    // Time Slider
    private static final Integer SLIDE_Y = 270;
    private static final Integer SLIDE_MIN_X = 205;
    private static final Integer SLIDE_MAX_X = 339;
    private static final Integer SLIDE_WIDTH = SLIDE_MAX_X - SLIDE_MIN_X;

    // MIDI
    private MidiInputManager midiInputManager;
    
    public GuiMidiFileCaster(Player player) {
        super(368, 300, 400, "textures/gui/gui_midi_playlist.png", "item.MIMIMod.gui_midi_playlist");
        this.midiInputManager = ((ClientProxy)MIMIMod.proxy).getMidiInput();
        this.folderPathString = this.midiInputManager.fileCasterManager.getPlaylistFolderPath();
    }

    @Override
    public void init() {
        super.init();

        // Fields
        folderPathField = this.addWidget(new EditBox(this.font, this.START_X + 90, this.START_Y + 40, 207, 10, CommonComponents.EMPTY));
        folderPathField.setValue(folderPathString);
        folderPathField.setMaxLength(256);
        folderPathField.setResponder(this::handlePathChange);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int imouseX = (int)Math.round(mouseX);
        int imouseY = (int)Math.round(mouseY);

        if(this.folderPathString != null && clickedBox(imouseX, imouseY, LOAD_FOLDER_BUTTON)) {
            this.midiInputManager.fileCasterManager.loadFromFolder(this.folderPathString);
        } else if(this.folderPathString != null && clickedBox(imouseX, imouseY, SAVE_DEFAULT_BUTTON)) {
            ModConfigs.CLIENT.playlistFolderPath.set(this.folderPathString);
        } else if(ModConfigs.CLIENT.playlistFolderPath.get() != null && !ModConfigs.CLIENT.playlistFolderPath.get().isEmpty() && clickedBox(imouseX, imouseY, LOAD_DEFAULT_BUTTON)) {
            this.folderPathString = ModConfigs.CLIENT.playlistFolderPath.get();
            this.folderPathField.setValue(this.folderPathString);
            this.midiInputManager.fileCasterManager.loadFromFolder(this.folderPathString);
        } else if(clickedBox(imouseX, imouseY, PREVIOUS_BUTTON)) {
            Double slidePercentage = null;

            if(this.midiInputManager.fileCasterManager.isSongLoaded()) {
                slidePercentage =  Double.valueOf(this.midiInputManager.fileCasterManager.getCurrentSongPosSeconds()) / Double.valueOf(this.midiInputManager.fileCasterManager.getSongLengthSeconds());
            }

            if(slidePercentage != null && slidePercentage >= 0.25) {
                this.midiInputManager.fileCasterManager.playFromBeginning();
            } else {
                this.midiInputManager.fileCasterManager.shiftSong(false);
            }
        } else if(clickedBox(imouseX, imouseY, STOP_BUTTON)) {
            this.midiInputManager.fileCasterManager.stop();
        } else if(clickedBox(imouseX, imouseY, PLAY_PAUSE_BUTTON)) {
            if(this.midiInputManager.fileCasterManager.isPlaying()) { 
                this.midiInputManager.fileCasterManager.pause();
            } else if(this.midiInputManager.fileCasterIsActive()) {
                this.midiInputManager.fileCasterManager.playFromLastTickPosition();
            }
        } else if(clickedBox(imouseX, imouseY, NEXT_BUTTON)) {
            this.midiInputManager.fileCasterManager.shiftSong(true);
        } else if(clickedBox(imouseX, imouseY, LOOP_BUTTON)) {
            this.midiInputManager.fileCasterManager.shiftLoopMode();
        } else if(clickedBox(imouseX, imouseY, SHUFFLE_BUTTON)) {
            this.midiInputManager.fileCasterManager.toggleShuffle();
        } else if(clickedBox(imouseX, imouseY, TRANSMIT_BUTTON)) {
            TransmitMode oldMode = this.midiInputManager.fileCasterManager.getTransmitMode();
            this.midiInputManager.fileCasterManager.shiftTransmitMode();
            NetworkManager.BROADCAST_CHANNEL.sendToServer(TransmitterNotePacket.createAllNotesOffPacket(TransmitterNotePacket.ALL_CHANNELS, oldMode));
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
           this.minecraft.player.closeContainer();
        }
  
        return !this.folderPathField.keyPressed(keyCode, scanCode, modifiers) && !this.folderPathField.canConsumeInput() ? super.keyPressed(keyCode, scanCode, modifiers) : true;
    }

    @Override
    protected  PoseStack renderGraphics(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        // Set Texture
        RenderSystem.setShaderTexture(0, guiTexture);

        // Background
        blit(matrixStack, START_X, START_Y, this.getBlitOffset(), 0, 0, GUI_WIDTH, GUI_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);

        // Selected Song Box
        if(this.midiInputManager.fileCasterManager.isSongSelected()) {
            Integer songOffset;

            if(this.midiInputManager.fileCasterManager.getSongCount() <= 6 || this.midiInputManager.fileCasterManager.getSelectedSongIndex() < 3) {
                songOffset = this.midiInputManager.fileCasterManager.getSelectedSongIndex();
            } else if(this.midiInputManager.fileCasterManager.getSelectedSongIndex() > this.midiInputManager.fileCasterManager.getSongCount() - 3) {
                songOffset = 6 - (this.midiInputManager.fileCasterManager.getSongCount() - this.midiInputManager.fileCasterManager.getSelectedSongIndex());
            } else {
                songOffset = 3;
            }
            
            Integer boxY = 82 + 10 * songOffset;
            blit(matrixStack, START_X + 15, START_Y + boxY, this.getBlitOffset(), 1, 301, 338, 11, TEXTURE_SIZE, TEXTURE_SIZE);
        }

        // Play/Pause Button
        blit(matrixStack, START_X + 53, START_Y + 272, this.getBlitOffset(), 1 + this.midiInputManager.fileCasterManager.isPlaying().compareTo(false) * 13, 355, 13, 13, TEXTURE_SIZE, TEXTURE_SIZE);

        // Loop Screen
        blit(matrixStack, START_X + Float.valueOf(LOOP_SCREEN.x()).intValue(), START_Y + Float.valueOf(LOOP_SCREEN.y()).intValue(), this.getBlitOffset(), 1 + (13 * this.midiInputManager.fileCasterManager.getLoopMode()), 313, 13, 13, TEXTURE_SIZE, TEXTURE_SIZE);

        // Shuffle Screen    
        blit(matrixStack, START_X + Float.valueOf(SHUFFLE_SCREEN.x()).intValue(), START_Y + Float.valueOf(SHUFFLE_SCREEN.y()).intValue(), this.getBlitOffset(), 1 + (13 * this.midiInputManager.fileCasterManager.getShuffleMode()), 327, 13, 13, TEXTURE_SIZE, TEXTURE_SIZE);
        
        // Transmit Screen    
        blit(matrixStack, START_X + Float.valueOf(TRANSMIT_SCREEN.x()).intValue(), START_Y + Float.valueOf(TRANSMIT_SCREEN.y()).intValue(), this.getBlitOffset(), 1 + (13 * this.midiInputManager.fileCasterManager.getTransmitModeInt()), 341, 13, 13, TEXTURE_SIZE, TEXTURE_SIZE);

        // Time Slider
        Integer slideOffset = 0;
        if(this.midiInputManager.fileCasterManager.isSongLoaded()) {
            Integer slideLength = this.midiInputManager.fileCasterManager.getSongLengthSeconds();
            Integer slideProgress = this.midiInputManager.fileCasterManager.getCurrentSongPosSeconds();
            Double slidePercentage =  Double.valueOf(slideProgress) / Double.valueOf(slideLength);
            slideOffset = Double.valueOf(Math.floor(slidePercentage * SLIDE_WIDTH)).intValue();
        }

        blit(matrixStack, START_X + SLIDE_MIN_X + slideOffset, START_Y + SLIDE_Y, this.getBlitOffset(), 43, 313, 7, 17, TEXTURE_SIZE, TEXTURE_SIZE);

        // Text Field
        this.folderPathField.render(matrixStack, mouseX, mouseY, partialTicks);
        
        return matrixStack;
    }

    @Override
    protected PoseStack renderText(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        // Playlist
        if(this.midiInputManager.fileCasterManager.getSongCount() > 0) {
            Integer minSong;
            if(this.midiInputManager.fileCasterManager.getSongCount() <= 6 || this.midiInputManager.fileCasterManager.getSelectedSongIndex() < 3) {
                minSong = 0;
            } else if(this.midiInputManager.fileCasterManager.getSelectedSongIndex() > this.midiInputManager.fileCasterManager.getSongCount() - 3) {
                minSong = this.midiInputManager.fileCasterManager.getSongCount() - 6;
            } else {
                minSong = this.midiInputManager.fileCasterManager.getSelectedSongIndex() - 3;
            }

            for(int i = 0; i < 6; i++) {
                if(this.midiInputManager.fileCasterManager.getSongCount() > (minSong + i)) {
                    MidiFileInfo info = this.midiInputManager.fileCasterManager.getLoadedPlaylist().get(minSong + i);
                    this.font.draw(matrixStack, (minSong + i + 1) + "). " + (info.fileName.length() > 50 ? info.fileName.substring(0,50) + "..." : info.fileName), START_X + 18, START_Y + 84 + i * 10, 0xFF00E600);
                } else {
                    break;
                }            
            }
        }

        // Current Song
        MidiFileInfo info = this.midiInputManager.fileCasterManager.getSelectedSongInfo();
        if(info != null) {
            this.font.draw(matrixStack, "Channel Instruments: ", START_X + 16, START_Y + 174, 0xFF00E600);
            
            Integer index = 0;
            for(Integer i = 0; i < 16; i+=2) {
                String name = info.instrumentMapping.get(i) == null ? "None" : info.instrumentMapping.get(i);
                this.font.draw(matrixStack, (i+1) + ": " + name, START_X + 16, START_Y + 188 + 10*index, 0xFF00E600);
                index++;
            }

            index = 0;
            for(Integer i = 1; i < 16; i+=2) {
                String name = info.instrumentMapping.get(i) == null ? "None" : info.instrumentMapping.get(i);
                this.font.draw(matrixStack, (i+1) + ": " + name, START_X + 184, START_Y + 188 + 10*index, 0xFF00E600);
                index++;
            }
        }
        
        return matrixStack;
    }

    protected void handlePathChange(String folderPath) {
        if(folderPath != null && !folderPath.trim().isEmpty()) {
            try {
                if(Files.isDirectory(Paths.get(folderPath), LinkOption.NOFOLLOW_LINKS)) {
                    this.folderPathString = folderPath.trim();
                    this.folderPathField.setTextColor(DEFAULT_TEXT_FIELD_COLOR);
                } else {
                    throw new RuntimeException("Folder not found: " + folderPath);
                }
            } catch(Exception e) {
                this.folderPathString = null;
                this.folderPathField.setTextColor(13112340);
            }
        } else {
            this.folderPathString = null;
            this.folderPathField.setTextColor(13112340);
        }
    }
}