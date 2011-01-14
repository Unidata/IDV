/*
 * $Id: AudioPlayer.java,v 1.7 2007/07/06 20:45:29 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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
 * n * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.unidata.ui;






import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.Resource;
import ucar.unidata.util.StringUtil;


import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.geom.*;

import java.io.BufferedInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import java.net.URL;

import javax.sound.midi.*;
import javax.sound.sampled.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;




/**
 * Components of the playback originally from Sun's Java Sound Jukebox demo
 */
public class AudioPlayer extends JPanel implements LineListener,
        MetaEventListener {

    /** start icon */
    private static Icon startIcon;

    /** stop icon */
    private static Icon pauseIcon;




    /** _more_ */
    private int timestamp = 0;

    /** _more_ */
    private JLabel timeLabel;

    /** _more_ */
    private JProgressBar progressBar;

    /** _more_ */
    private boolean running = false;

    /** _more_ */
    private Sequencer sequencer;

    /** _more_ */
    private boolean midiEOM;

    /** _more_ */
    private boolean audioEOM;

    /** _more_ */
    private Synthesizer synthesizer;

    /** _more_ */
    private MidiChannel channels[];

    /** _more_ */
    private Object currentSound;

    /** _more_ */
    private String currentName;

    /** _more_ */
    private double duration;

    /** _more_ */
    private JButton startBtn;

    /** _more_ */
    private JButton rewindBtn;

    /** _more_ */
    private JSlider volumeSlider;

    /** _more_ */
    private JSlider seekSlider;

    /** _more_ */
    private String soundFile;




    /**
     * _more_
     */
    public AudioPlayer() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(5, 5, 5, 5));
        add(doMakeControls());
        init();
    }

    /**
     * _more_
     *
     * @param file _more_
     */
    public void setFile(String file) {
        stopPlaying();
        currentSound = null;
        soundFile    = file;
    }

    /**
     * _more_
     */
    public void startPlaying() {
        if ((soundFile == null) || running) {
            return;
        }
        Misc.run(new Runnable() {
            public void run() {
                try {
                    timestamp++;
                    if (currentSound == null) {
                        if ( !loadSound(soundFile)) {
                            return;
                        }
                    }
                    if (currentSound != null) {
                        running = true;
                        playSound(timestamp);
                    }
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        });
    }


    /**
     * _more_
     */
    public void stopPlaying() {
        timestamp++;
        startBtn.setIcon(startIcon);
        running = false;
        if (usingClip()) {
            ((Clip) currentSound).stop();
        } else if (usingSequencer()) {
            sequencer.stop();
        }
    }


    /**
     * _more_
     */
    public void handleRewindButton() {
        boolean wasRunning = running;
        if (usingClip()) {
            ((Clip) currentSound).setFramePosition(0);
        } else {
            stopPlaying();
            if (wasRunning) {
                startPlaying();
            }
        }
        updateControls();
    }

    /**
     * _more_
     */
    public void handleStartButton() {
        if ( !running) {
            startPlaying();
            //      setComponentsEnabled(true);
        } else {
            stopPlaying();
            //      setComponentsEnabled(false);
        }
    }



    /**
     * _more_
     */
    public void init() {
        try {
            sequencer = MidiSystem.getSequencer();
            if (sequencer instanceof Synthesizer) {
                synthesizer = (Synthesizer) sequencer;
                channels    = synthesizer.getChannels();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        sequencer.addMetaEventListener(this);
    }




    /**
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     */
    public boolean loadSound(String file) {
        currentName = IOUtil.getFileTail(file);
        duration    = 0.0;
        updateControls();
        try {
            File f = new File(file);
            if (f.exists()) {
                try {
                    currentSound = AudioSystem.getAudioInputStream(f);
                } catch (Exception e1) {
                    FileInputStream is = new FileInputStream(f);
                    currentSound = new BufferedInputStream(is, 1024);
                }
            } else {
                InputStream is = IOUtil.getInputStream(file);
                try {
                    currentSound = AudioSystem.getAudioInputStream(is);
                } catch (Exception e) {
                    currentSound = new BufferedInputStream(is, 1024);
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            currentSound = null;
            return false;
        }


        if (sequencer == null) {
            currentSound = null;
            return false;
        }

        if (currentSound instanceof AudioInputStream) {
            try {
                AudioInputStream stream = (AudioInputStream) currentSound;
                AudioFormat      format = stream.getFormat();

                /**
                 * we can't yet open the device for ALAW/ULAW playback,
                 * convert ALAW/ULAW to PCM
                 */
                if ((format.getEncoding() == AudioFormat.Encoding.ULAW)
                        || (format.getEncoding()
                            == AudioFormat.Encoding.ALAW)) {
                    AudioFormat tmp =
                        new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                        format.getSampleRate(),
                                        format.getSampleSizeInBits() * 2,
                                        format.getChannels(),
                                        format.getFrameSize() * 2,
                                        format.getFrameRate(), true);
                    stream = AudioSystem.getAudioInputStream(tmp, stream);
                    format = tmp;
                }
                DataLine.Info info = new DataLine.Info(Clip.class,
                                         stream.getFormat(),
                                         ((int) stream.getFrameLength()
                                          * format.getFrameSize()));

                Clip clip = (Clip) AudioSystem.getLine(info);
                clip.addLineListener(this);
                clip.open(stream);
                currentSound = clip;
                seekSlider.setMaximum((int) stream.getFrameLength());
            } catch (Exception ex) {
                ex.printStackTrace();
                currentSound = null;
                return false;
            }
        } else if ((currentSound instanceof Sequence)
                   || (currentSound instanceof BufferedInputStream)) {
            try {
                sequencer.open();
                if (currentSound instanceof Sequence) {
                    sequencer.setSequence((Sequence) currentSound);
                } else {
                    sequencer.setSequence((BufferedInputStream) currentSound);
                }
                seekSlider.setMaximum((int) (sequencer.getMicrosecondLength()
                                             / 1000));

            } catch (InvalidMidiDataException imde) {
                LogUtil.userErrorMessage("Unsupported audio file:" + file);
                currentSound = null;
                return false;
            } catch (Exception ex) {
                ex.printStackTrace();
                currentSound = null;
                return false;
            }
        }

        seekSlider.setValue(0);
        seekSlider.setEnabled(true);
        volumeSlider.setEnabled(true);

        duration = getDuration();

        return true;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    private boolean usingSequencer() {
        if (currentSound == null) {
            return false;
        }
        return ((currentSound instanceof Sequence)
                || (currentSound instanceof BufferedInputStream));
    }

    /**
     * _more_
     *
     * @return _more_
     */
    private boolean usingClip() {
        if (currentSound == null) {
            return false;
        }
        return (currentSound instanceof Clip);
    }

    /**
     * _more_
     *
     * @param myTimestamp _more_
     */
    private void playSound(int myTimestamp) {
        running = true;
        setVolume();
        midiEOM = audioEOM = false;
        if (usingSequencer()) {
            sequencer.start();
            while ( !midiEOM && running && (myTimestamp == timestamp)) {
                Misc.sleep(99);
                updateControls();
            }
        } else if (usingClip()) {
            Clip clip = (Clip) currentSound;
            clip.start();
            Misc.sleep(99);
            while (clip.isActive() && running && (myTimestamp == timestamp)) {
                Misc.sleep(99);
                updateControls();
            }
            if ( !clip.isActive() && running && (myTimestamp == timestamp)) {
                clip.stop();
                clip.setFramePosition(0);
                updateControls();
                running = false;
            }
        }
    }


    /**
     * _more_
     */
    public void close() {
        stopPlaying();
        if (sequencer != null) {
            sequencer.close();
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public double getDuration() {
        double duration = 0.0;
        if (currentSound instanceof Sequence) {
            duration = ((Sequence) currentSound).getMicrosecondLength()
                       / 1000000.0;
        } else if (currentSound instanceof BufferedInputStream) {
            duration = sequencer.getMicrosecondLength() / 1000000.0;
        } else if (usingClip()) {
            Clip clip = (Clip) currentSound;
            duration = clip.getBufferSize()
                       / (clip.getFormat().getFrameSize()
                          * clip.getFormat().getFrameRate());
        }
        return duration;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public double getSeconds() {
        double seconds = 0.0;
        if (usingClip()) {
            Clip clip = (Clip) currentSound;
            seconds = clip.getFramePosition()
                      / clip.getFormat().getFrameRate();
        } else if (usingSequencer()) {
            try {
                seconds = sequencer.getMicrosecondPosition() / 1000000.0;
            } catch (IllegalStateException e) {
                LogUtil.logException(
                    " IllegalStateException "
                    + "on sequencer.getMicrosecondPosition(): ", e);
            }
        }
        return seconds;
    }


    /**
     * _more_
     *
     * @param event _more_
     */
    public void update(LineEvent event) {
        if (event.getType() == LineEvent.Type.STOP) {
            startBtn.setIcon(startIcon);
            audioEOM = true;
        } else if (event.getType() == LineEvent.Type.START) {
            startBtn.setIcon(pauseIcon);
        }
    }


    /**
     * _more_
     *
     * @param message _more_
     */
    public void meta(MetaMessage message) {
        if (message.getType() == 47) {  // 47 is end of track
            midiEOM = true;
        }
    }


    /**
     * _more_
     */
    public void setVolume() {
        double value = volumeSlider.getValue() / 100.0;
        if (usingClip()) {
            try {
                Clip clip = (Clip) currentSound;
                FloatControl gainControl = (FloatControl) clip.getControl(
                                               FloatControl.Type.MASTER_GAIN);
                float dB = (float) (Math.log((value == 0.0)
                                             ? 0.0001
                                             : value) / Math.log(10.0)
                                                 * 20.0);
                gainControl.setValue(dB);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else if (usingSequencer()) {
            for (int i = 0; i < channels.length; i++) {
                channels[i].controlChange(7, (int) (value * 127.0));

            }
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    private JPanel doMakeControls() {
        if (startIcon == null) {
            pauseIcon = Resource.getIcon("/auxdata/ui/icons/Pause16.gif",
                                         true);
            startIcon = Resource.getIcon("/auxdata/ui/icons/Play16.gif",
                                         true);
        }

        Font monoFont = Font.decode("monospaced");
        timeLabel = new JLabel(StringUtil.padRight(" ", 12));
        timeLabel.setFont(monoFont);
        progressBar = new JProgressBar(0, 100);
        startBtn = GuiUtils.makeImageButton("/auxdata/ui/icons/Play16.gif",
                                            this, "handleStartButton");
        rewindBtn =
            GuiUtils.makeImageButton("/auxdata/ui/icons/Rewind16.gif", this,
                                     "handleRewindButton");
        JPanel buttons = GuiUtils.left(GuiUtils.hbox(rewindBtn, startBtn));

        seekSlider = GuiUtils.makeSlider(0, 100, 0, this,
                                         "seekSliderChanged");
        //      seekSlider.setEnabled(false);
        volumeSlider = GuiUtils.makeSlider(0, 100, 80, this,
                                           "volumeSliderChanged");

        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
        JPanel sliderPanel = GuiUtils.doLayout(new Component[] {
                                 GuiUtils.rLabel("Seek:"),
                                 seekSlider, GuiUtils.rLabel("Volume:"),
                                 volumeSlider }, 2, GuiUtils.WT_NY,
                                     GuiUtils.WT_N);
        return GuiUtils.top(GuiUtils.vbox(GuiUtils.inset(buttons, 3),
                                          GuiUtils.leftCenterRight(buttons,
                                              progressBar,
                                                  GuiUtils.inset(timeLabel,
                                                      new Insets(0, 5, 0,
                                                          5))), sliderPanel));

    }


    /**
     * _more_
     *
     * @param value _more_
     */
    public void volumeSliderChanged(int value) {
        setVolume();
    }

    /**
     * _more_
     *
     * @param value _more_
     */
    public void seekSliderChanged(int value) {
        if (usingClip()) {
            ((Clip) currentSound).setFramePosition(value);
        } else if (currentSound instanceof Sequence) {
            long dur = ((Sequence) currentSound).getMicrosecondLength();
            sequencer.setMicrosecondPosition(value * 1000);
        } else if (currentSound instanceof BufferedInputStream) {
            long dur = sequencer.getMicrosecondLength();
            sequencer.setMicrosecondPosition(value * 1000);
        }
        updateControls();
    }


    /**
     * _more_
     *
     * @param state _more_
     */
    public void setComponentsEnabled(boolean state) {
        //      seekSlider.setEnabled(state);
    }


    /**
     * _more_
     */
    public void updateControls() {
        timeLabel.setText(StringUtil.padRight(" ", 12));
        double seconds = getSeconds();
        if (midiEOM) {
            seconds = duration;
        }

        if ((currentSound == null) || (duration <= 0.0) || (seconds <= 0.0)) {
            progressBar.setValue(0);
            return;
        }

        String s = String.valueOf(seconds);
        s = s.substring(0, s.indexOf('.') + 2);
        s = s + "/" + ((int) duration);
        timeLabel.setText(StringUtil.padRight(s, 12));
        int progress = (int) (seconds / duration * 100);
        progressBar.setValue(progress);
    }


}

