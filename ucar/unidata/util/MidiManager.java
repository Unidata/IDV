/*
 * $Id: MidiManager.java,v 1.6 2006/05/05 19:19:35 jeffmc Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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





package ucar.unidata.util;


import javax.sound.midi.*;




/**
 * Class MidiManager provides a wrapper around the java sound midi api.
 * This was originally taken from the javasound demo
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.6 $
 */
public class MidiManager {

    /** Keep this around so client code can list the instruments without opening their own midimanager */
    private static Instrument[] instrumentList;

    /** the synthesizer */
    private Synthesizer synthesizer;

    /** the sequencer */
    private Sequencer sequencer;

    /** the sequence */
    private Sequence sequence;

    /** my list of instruments */
    private Instrument[] instruments;

    /** wrapper around the channels */
    private ChannelData[] channels;

    /** default properties */
    private MidiProperties myProps = new MidiProperties();

    /**
     * Create a new MidiManager with defaults
     */
    public MidiManager() {
        this(new MidiProperties());
    }

    /**
     * Construct with the specified properties and open it.
     *
     * @param props properties
     */
    public MidiManager(MidiProperties props) {
        myProps = props;
        open();
        setInstrument(props.getInstrumentName());
    }

    /**
     * todo
     */
    public void close() {}

    /**
     * Open up the midi system
     */
    public void open() {
        try {
            if (synthesizer == null) {
                if ((synthesizer = MidiSystem.getSynthesizer()) == null) {
                    System.out.println("getSynthesizer() failed!");
                    return;
                }
            }
            synthesizer.open();
            sequencer = MidiSystem.getSequencer();
            sequence  = new Sequence(Sequence.PPQ, 10);
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        Soundbank sb = synthesizer.getDefaultSoundbank();
        if (sb != null) {
            instruments = synthesizer.getDefaultSoundbank().getInstruments();
            synthesizer.loadInstrument(instruments[0]);
            for (int i = 0; i < instruments.length; i++) {
                //System.err.println("inst:" + instruments[i]);
            }
        }
        MidiChannel midiChannels[] = synthesizer.getChannels();
        channels = new ChannelData[midiChannels.length];
        for (int i = 0; i < channels.length; i++) {
            channels[i] = new ChannelData(midiChannels[i], i);
        }

    }


    /**
     * Set the current instrument
     *
     * @param index index into the instruments
     */
    public void setInstrument(int index) {
        if ((index < 0) || (index >= instruments.length)) {
            return;
        }
        setInstrument(instruments[index]);
        channels[0].channel.programChange(index);
    }

    /**
     * Set the current instrument
     *
     * @param instrumentName name of the instrument
     */
    public void setInstrument(String instrumentName) {
        setInstrument(getInstrumentIndex(instrumentName));
    }


    /**
     * Set the instrument to use
     *
     * @param instrument the instrument
     */
    public void setInstrument(Instrument instrument) {
        synthesizer.loadInstrument(instrument);
    }


    /**
     * Get the list of instruments
     *
     * @return an array of instruments
     */
    public static Instrument[] getInstrumentList() {
        if (instrumentList == null) {
            MidiManager midiManager = new MidiManager();
            instrumentList = midiManager.instruments;
            midiManager.close();
        }
        return instrumentList;
    }

    /**
     * Get the index into the list of instruments.
     * @param instrumentName name of the instrument
     * @return index into the array of instruments or -1 if not found
     */
    private int getInstrumentIndex(String instrumentName) {
        if (instrumentName == null) {
            return -1;
        }
        Instrument[] instruments = getInstrumentList();
        for (int i = 0; i < instruments.length; i++) {
            if ((instruments[i] != null)
                    && Misc.equals(instrumentName,
                                   instruments[i].getName())) {
                return i;
            }
        }
        return -1;
    }

    /** the default playTimeStamp */
    int playTimeStamp = 0;

    /** current channel */
    MidiChannel currentChannel;

    /** current key */
    int currentKey = -1;

    /**
     * Play.
     *
     * @param key   key to play in
     * @param milliseconds length of time
     */
    public synchronized void play(int key, long milliseconds) {
        playTimeStamp++;
        if (currentChannel != null) {
            currentChannel.allSoundOff();
            currentChannel.allNotesOff();
            //            currentChannel.noteOff(currentKey, 0);
        }
        currentChannel = channels[0].channel;
        currentKey     = key;
        currentChannel.noteOn(key, channels[0].velocity);
        turnOffInABit(key, currentChannel, playTimeStamp, milliseconds);
    }

    /**
     * Turn off in a thread
     *
     * @param key    key to play in
     * @param channel  channel to play
     * @param timeStamp  timestamp
     * @param milliseconds length of time
     */
    public void turnOffInABit(final int key, final MidiChannel channel,
                              final int timeStamp, final long milliseconds) {
        Misc.runInABit(milliseconds, new Runnable() {
            public void run() {
                if (timeStamp != playTimeStamp) {
                    return;
                }
                //                    channel.noteOff(key, 128);
                currentChannel.allNotesOff();
                channel.allSoundOff();
            }
        });
    }



    /**
     * Stores MidiChannel information.
     */
    class ChannelData {

        /** channel */
        MidiChannel channel;

        /** some flags */
        boolean solo, mono, mute, sustain;

        /** some types */
        int velocity, pressure, bend, reverb;

        /** some stuff */
        int row, col, num;

        /**
         * Create a new ChannelData
         *
         * @param channel  channel to use
         * @param num numer
         */
        public ChannelData(MidiChannel channel, int num) {
            this.channel = channel;
            this.num     = num;
            velocity     = pressure = bend = reverb = 64;
            reverb       = 0;
        }

    }

}

