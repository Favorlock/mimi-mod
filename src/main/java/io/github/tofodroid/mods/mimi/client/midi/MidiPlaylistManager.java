package io.github.tofodroid.mods.mimi.client.midi;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequencer;

import com.sun.media.sound.MidiUtils;

import io.github.tofodroid.mods.mimi.common.MIMIMod;
import io.github.tofodroid.mods.mimi.common.config.ModConfigs;

public class MidiPlaylistManager extends MidiInputSourceManager {
    // Playlist
    private String playlistFolderPath;
    private List<MidiFileInfo> songList;
    private List<MidiFileInfo> originalList;
    private Long pausedTickPosition;
    private Long pausedMicrosecond;
    private Integer selectedSongIndex;
    private LoopMode currentLoopMode = LoopMode.NONE;
    private Boolean shuffled = false;
        
    // MIDI Sequencer
    private Sequencer activeSequencer;

    public MidiPlaylistManager() {
        // Create Sequencer
        createSequencer();
        
        // Load from Config
        loadFromFolder(ModConfigs.CLIENT.playlistFolderPath.get());
    }

    // Utils
    protected void resetPlaylist() {
        stop();

        this.playlistFolderPath = "";
        this.songList = new ArrayList<>();
        this.pausedTickPosition = null;
        this.pausedMicrosecond = null;
        this.selectedSongIndex = null;
        this.shuffled = false;
    }

    protected Boolean createSequencer() {
        try {
            this.activeSequencer = MidiSystem.getSequencer(false);
            this.activeSequencer.open();
            this.activeSequencer.addMetaEventListener(new MetaEventListener(){
                @Override
                public void meta(MetaMessage meta) {
                    if(MidiUtils.isMetaEndOfTrack(meta) && !activeSequencer.isRunning()) {
                        switch(currentLoopMode) {
                            case ALL:
                                stop();
                                shiftSong(true);
                                playFromBeginning();
                                break;
                            case SINGLE:
                                stop();
                                playFromBeginning();
                                break;
                            case NONE:
                            default:
                                stop();
                                break;
                        }
                    }
                }
                
            });
            return true;
        } catch(Exception e) {
            this.activeSequencer = null;
            MIMIMod.LOGGER.error("Failed to create MIDI Sequencer: ", e);
            return false;
        }
    }
    
    protected Boolean loadSelectedSong() {
        if(isOpen() && isSongSelected()) {
            try {
                Path activePath = Paths.get(this.playlistFolderPath, this.songList.get(this.selectedSongIndex).fileName);
                activeSequencer.setSequence(MidiSystem.getSequence(activePath.toFile()));
                activeSequencer.setTempoInBPM(this.songList.get(this.selectedSongIndex).tempo);
                return true;
            } catch(Exception e) {
                MIMIMod.LOGGER.error("Failed to open MIDI file " + Paths.get(this.playlistFolderPath, this.songList.get(this.selectedSongIndex).fileName).toString(), e);
            }
        }

        return false;
    }

    // Getters
    public Boolean isOpen() {
        return this.activeSequencer != null && this.activeSequencer.isOpen();
    }
    
    public Boolean isSongSelected() {
        return this.selectedSongIndex != null;
    }
    
    public Boolean isSongLoaded() {
        return this.isOpen() && isSongSelected() && this.activeSequencer.getSequence() != null;
    }
        
    public Integer getSongLengthSeconds() {
        return isSongLoaded() ? new Long(this.activeSequencer.getSequence().getMicrosecondLength() / 1000000).intValue() : null;
    }
        
    public Integer getCurrentSongPosSeconds() {
        Long micoPos;

        if(isPlaying()) {
            micoPos = this.activeSequencer.getMicrosecondPosition();
        } else if(this.pausedMicrosecond != null) {
            micoPos = this.pausedMicrosecond;
        } else if(isSongLoaded()) {
            micoPos = 0l;
        } else {
            return null;
        }

        return new Long(micoPos / 1000000).intValue();
    }

    public MidiFileInfo getSelectedSongInfo() {
        return isSongSelected() ? this.songList.get(this.selectedSongIndex) : null;
    }

    public Integer getSelectedSongIndex() {
        return selectedSongIndex;
    }

    public String getPlaylistFolderPath() {
        return this.playlistFolderPath;
    }

    public List<MidiFileInfo> getLoadedPlaylist() {
        return songList;
    }

    // Playlist Controls
    public Boolean loadFromFolder(String folderPath) {
        // Clear existing playlist
        resetPlaylist();

        // Validate path
        if(folderPath != null && !folderPath.trim().isEmpty() && Files.isDirectory(Paths.get(folderPath.trim()), LinkOption.NOFOLLOW_LINKS)) {
            this.playlistFolderPath = folderPath.trim();

            // Load songs
            for(File file : new File(this.playlistFolderPath).listFiles()) {
                if(file.isFile() && file.getAbsolutePath().endsWith("midi") || file.getAbsolutePath().endsWith("mid")) {
                    MidiFileInfo info = MidiFileInfo.fromFile(file);
                    if(info != null) songList.add(info);
                }
            }

            // Select first song
            if(!this.songList.isEmpty()) {
                selectSong(0);
            }

            return true;
        }

        return false;
    }

    public void playFromBeginning() {
        if(isSongLoaded()) {
            stop();
            this.activeSequencer.setTempoInBPM(this.songList.get(this.selectedSongIndex).tempo);
            this.activeSequencer.start();
        }
    }

    public void playFromLastTickPosition() {
        if(isPlaying()) {
            return;
        }

        if(isSongLoaded() && this.pausedTickPosition != null) {
            this.activeSequencer.stop();
            this.activeSequencer.setTickPosition(this.pausedTickPosition);
            this.activeSequencer.setTempoInBPM(this.songList.get(this.selectedSongIndex).tempo);
            this.pausedTickPosition = null;
            this.pausedMicrosecond = null;
            this.activeSequencer.start();
        } else if(isSongLoaded()) {
            playFromBeginning();
        }
    }

    public void pause() {
        if(isSongLoaded() && isPlaying()) {
            this.pausedTickPosition = this.activeSequencer.getTickPosition();
            this.pausedMicrosecond = this.activeSequencer.getMicrosecondPosition();
            this.activeSequencer.stop();
        }
    }

    public void stop() {
        this.pausedTickPosition = null;
        this.pausedMicrosecond = null;

        if(isSongLoaded()) {
            this.activeSequencer.stop();
            this.activeSequencer.setTickPosition(0);
        }
    }

    public void selectSong(Integer songNum) {
        Boolean play = false;

        if(isPlaying()) {
            play = true;
        }

        stop();
        this.selectedSongIndex = null;

        if(songNum != null && songNum >= 0 && songNum < this.songList.size()) {
            this.selectedSongIndex = songNum;
            loadSelectedSong();

            if(play) {
                this.playFromBeginning();
            }
        }
    }

    public void shiftSong(Boolean up) {
        if(!up) {
            if(this.selectedSongIndex > 0) {
                selectSong(this.selectedSongIndex-1);
            } else {
                selectSong(this.songList.size() - 1);
            }
        } else {
            if(this.selectedSongIndex < this.songList.size() - 1) {
                selectSong(this.selectedSongIndex+1);
            } else {
                selectSong(0);
            }
        }
    }
    
    public void shiftLoopMode() {
        if(currentLoopMode == LoopMode.ALL) {
            currentLoopMode = LoopMode.SINGLE;
        } else if(currentLoopMode == LoopMode.SINGLE) {
            currentLoopMode = LoopMode.NONE;
        } else {
            currentLoopMode = LoopMode.ALL;
        }
    }

    public Integer getLoopMode() {
        return currentLoopMode.ordinal();
    }

    public Integer getShuffleMode() {
        return shuffled ? 1 : 0;
    }
    
    public Boolean toggleShuffle() {
        this.shuffled = !this.shuffled;

        if(this.getSongCount() > 0 && this.shuffled) {
            this.originalList = new ArrayList<>(this.songList);
            MidiFileInfo selectedSong = getSelectedSongInfo();
            Collections.shuffle(this.songList);
            this.selectedSongIndex = this.songList.indexOf(selectedSong);
        } else if(this.getSongCount() > 0) {
            MidiFileInfo selectedSong = getSelectedSongInfo();
            this.songList = this.originalList;
            this.originalList = new ArrayList<>();
            this.selectedSongIndex = this.songList.indexOf(selectedSong);
        }

        return shuffled;
    }

    public Integer getSongCount() {
        return songList.size();
    }

    public Boolean isPlaying() {
        return isSongLoaded() && this.activeSequencer.isRunning();
    }

    // Internal Functions
    @Override
    protected void openTransmitter() {
        if(isOpen()) {
            try {
                this.activeTransmitter = this.activeSequencer.getTransmitter();
                this.activeTransmitter.setReceiver(new MidiInputReceiver());
            } catch(Exception e) {
                MIMIMod.LOGGER.error("Midi Device Error: ", e);
                close();
            }
        }
    }

    @Override
    public void open() {
        if(!isOpen()) {
            try {
                this.activeSequencer = MidiSystem.getSequencer(false);
                this.activeSequencer.open();
            } catch(Exception e) {
                // TODO
            }
        }
        
        if(this.activeTransmitter == null) {
            this.openTransmitter();
        }
    }

    @Override
    public void close() {
        stop();
        super.close();

        if(this.activeSequencer != null) {
            this.activeSequencer.close();
            this.activeSequencer = null;
        }
    }

    public enum LoopMode {
        ALL,
        SINGLE,
        NONE;
    }
}
