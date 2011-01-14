/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 * 
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.unidata.idv.control;


import ucar.unidata.data.DataChoice;
import ucar.unidata.ui.AudioPlayer;
import ucar.unidata.util.FileManager;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;


import ucar.unidata.util.Misc;
import ucar.unidata.util.PatternFileFilter;


import visad.VisADException;

import java.awt.*;
import java.awt.event.*;

import java.io.*;

import java.rmi.RemoteException;

import javax.sound.sampled.*;

import javax.swing.*;


/**
 * Class AudioControl Plays and records audio
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.10 $
 */
public class AudioControl extends DisplayControlImpl {

    /** Save the bytes in the bundle */
    private boolean saveAudio = false;

    /** The player */
    private AudioPlayer audioPlayer;

    /** The unbundled bytes */
    private byte[] audioBytes;

    /** File to record to */
    private String recordFilename = "";

    /** File to play */
    private String playFilename = "";

    /** The format */
    private AudioFormat audioFormat;

    /** Record stuff */
    private TargetDataLine targetDataLine;

    /** Record button */
    private JButton recordBtn;

    /** Currently recording */
    private boolean amRecording = false;

    /** Play file field */
    private JTextField playFileField;

    /** Record file field */
    private JTextField recordFileField;


    /**
     * Dummy ctor
     */
    public AudioControl() {}



    /**
     * Called to make this kind of Display Control;
     * This method is called from inside DisplayControlImpl init(several args).
     *
     * @param dataChoice the DataChoice of the moment.
     *
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
        super.init(dataChoice);
        audioPlayer = new AudioPlayer();

        if (audioBytes != null) {
            try {
                String uid = "audio_" + Misc.getUniqueId();
                String file =
                    getControlContext().getObjectStore().getTmpFile(uid);
                IOUtil.writeBytes(new File(file), audioBytes);
                audioPlayer.setFile(file);
            } catch (Exception exc) {
                logException("Saving audio bytes", exc);
            }
        } else {
            if ((dataChoice != null) && (playFilename.length() == 0)) {
                playFilename = dataChoice.getStringId();
                audioPlayer.setFile(playFilename);
            }
        }
        return true;
    }

    /**
     *  Initialization  is done
     */
    public void initDone() {
        if (playFilename.trim().length() > 0) {
            audioPlayer.startPlaying();
        }
        super.initDone();
    }


    /**
     * Remove the control
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void doRemove() throws RemoteException, VisADException {
        super.doRemove();
        if (audioPlayer != null) {
            audioPlayer.close();
            audioPlayer = null;
        }
    }



    /**
     *  Overwrite the base class method to return the recordFilename or url.
     *
     *  @return The recordFilename or url as the title.
     */
    protected String getTitle() {
        if (playFilename != null) {
            return IOUtil.getFileTail(playFilename);
        }
        return super.getTitle();
    }


    /**
     * Make the gui
     *
     * @return The gui
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {
        JTabbedPane tabbedPane = new JTabbedPane();

        playFileField = new JTextField(playFilename, 30);
        JPanel filePanel = GuiUtils.label("File: ",
                                          GuiUtils.centerRight(playFileField,
                                              GuiUtils.makeButton("Select",
                                                  this, "selectPlayFile")));




        tabbedPane.add("Play",
                       GuiUtils.inset(GuiUtils.topCenter(filePanel,
                           audioPlayer), 5));
        tabbedPane.add("Record", GuiUtils.inset(doMakeRecordContents(), 5));
        return tabbedPane;
    }

    /**
     * Make the record tab contents
     *
     * @return record gui
     */
    private JComponent doMakeRecordContents() {
        recordFileField = new JTextField(recordFilename, 30);
        JPanel filePanel =
            GuiUtils.label("File: ",
                           GuiUtils.centerRight(recordFileField,
                               GuiUtils.makeButton("Select", this,
                                   "selectRecordFile")));

        recordBtn = GuiUtils.makeButton("Start Recording", this,
                                        "handleRecordBtn");

        JPanel buttons =
            GuiUtils.hbox(recordBtn,
                          GuiUtils.makeCheckbox("Save file in bundle", this,
                              "saveAudio"));
        return GuiUtils.top(GuiUtils.left(GuiUtils.vbox(filePanel, buttons)));

    }

    /**
     * Record button pressed
     */
    public void handleRecordBtn() {
        if (amRecording) {
            stopRecording();
        } else {
            audioPlayer.stopPlaying();
            Misc.run(this, "startRecording");
        }
    }

    /**
     * Stop recording
     */
    public void stopRecording() {
        if ( !amRecording) {
            return;
        }
        amRecording = false;
        recordBtn.setText("Start Recording");
        if (targetDataLine != null) {
            targetDataLine.stop();
            targetDataLine.close();
        }
    }



    /**
     * Create the audio format to use
     *
     * @return audio format
     */
    private AudioFormat getAudioFormat() {
        float sampleRate = 8000.0F;
        //8000,11025,16000,22050,44100
        int     sampleSizeInBits = 8;  //8,16
        int     channels         = 1;  //1,2
        boolean signed           = true;
        boolean bigEndian        = false;
        return new AudioFormat(sampleRate, sampleSizeInBits, channels,
                               signed, bigEndian);
    }


    /**
     * Select a file to record to
     */
    public void selectRecordFile() {
        PatternFileFilter filter =
            new PatternFileFilter(".+\\.au,.+\\.wav,.+\\.aiff",
                                  "Audio files");
        String filename = FileManager.getWriteFile(filter, ".au");
        if (filename == null) {
            return;
        }
        recordFileField.setText(filename);
        playFileField.setText(filename);
        audioPlayer.setFile(getPlayFilename());
    }


    /**
     * Select a file to play
     */
    public void selectPlayFile() {
        PatternFileFilter filter =
            new PatternFileFilter(".+\\.au,.+\\.wav,.+\\.aiff",
                                  "Audio files");
        String filename = FileManager.getReadFile(filter);
        if (filename == null) {
            return;
        }
        playFileField.setText(filename);
        audioPlayer.setFile(getPlayFilename());
    }

    /**
     * Start recording
     */
    public void startRecording() {
        try {
            if (getRecordFilename().length() == 0) {
                selectRecordFile();
            }
            if (getRecordFilename().length() == 0) {
                return;
            }
            amRecording = true;
            recordBtn.setText("Stop Recording");
            audioPlayer.setFile(getRecordFilename());
            updateLegendLabel();

            audioFormat = getAudioFormat();
            DataLine.Info dataLineInfo =
                new DataLine.Info(TargetDataLine.class, audioFormat);
            targetDataLine =
                (TargetDataLine) AudioSystem.getLine(dataLineInfo);


            String ext =
                IOUtil.getFileExtension(getRecordFilename()).toLowerCase();
            AudioFileFormat.Type fileType = null;
            if (ext.equals(".aifc")) {
                fileType = AudioFileFormat.Type.AIFC;
            } else if (ext.equals(".aiff")) {
                fileType = AudioFileFormat.Type.AIFF;
            } else if (ext.equals(".snd")) {
                fileType = AudioFileFormat.Type.SND;
            } else if (ext.equals(".wav")) {
                fileType = AudioFileFormat.Type.WAVE;
            } else {
                fileType = AudioFileFormat.Type.AU;
            }

            File audioFile = new File(getRecordFilename());
            targetDataLine.open(audioFormat);
            targetDataLine.start();
            AudioSystem.write(new AudioInputStream(targetDataLine), fileType,
                              audioFile);
        } catch (Exception e) {
            logException("Error capturing audio", e);
        }

    }



    /**
     *  Set the Filename property.
     *
     *  @param value The new value for Filename
     */
    public void setRecordFilename(String value) {
        recordFilename = value;
    }

    /**
     *  Get the Filename property.
     *
     *  @return The Filename
     */
    public String getRecordFilename() {
        if (recordFileField != null) {
            recordFilename = recordFileField.getText().trim();
        }
        return recordFilename;
    }



    /**
     *  Set the Filename property.
     *
     *  @param value The new value for Filename
     */
    public void setPlayFilename(String value) {
        playFilename = value;
    }

    /**
     *  Get the Filename property.
     *
     *  @return The Filename
     */
    public String getPlayFilename() {
        if (playFileField != null) {
            playFilename = playFileField.getText().trim();
        }
        return playFilename;
    }





    /**
     *  Set the SaveAudio property.
     *
     *  @param value The new value for SaveAudio
     */
    public void setSaveAudio(boolean value) {
        saveAudio = value;
    }

    /**
     *  Get the SaveAudio property.
     *
     *  @return The SaveAudio
     */
    public boolean getSaveAudio() {
        return saveAudio;
    }

    /**
     *  Set the AudioBytes property.
     *
     *  @param value The new value for AudioBytes
     */
    public void setAudioBytes(byte[] value) {
        audioBytes = value;
    }


    /**
     *  Get the AudioBytes property.
     *
     *  @return The AudioBytes
     *
     * @throws IOException On badness
     */
    public byte[] getAudioBytes() throws IOException {
        if ((recordFilename == null) || (recordFilename.length() == 0)
                || !saveAudio) {
            return null;
        }
        File f = new File(recordFilename);
        if ( !f.exists()) {
            return null;
        }
        return IOUtil.readBytes(new FileInputStream(f));
    }

}
