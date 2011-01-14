/*
 * $Id: JpegImagesToMovie.java,v 1.3 2007/07/06 20:45:31 jeffmc Exp $
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
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.unidata.ui;


import ucar.unidata.util.LogUtil;

import java.awt.Dimension;



import java.io.*;

import java.util.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.datasink.*;
import javax.media.format.VideoFormat;
import javax.media.protocol.*;
import javax.media.protocol.DataSource;



/**
 * This program takes a list of JPEG or PNG image files and convert them into
 * a QuickTime movie.
 */
public class JpegImagesToMovie implements ControllerListener,
                                          DataSinkListener {

    /**
     * _more_
     *
     * @param width
     * @param height
     * @param frameRate
     * @param inFiles
     * @param outML
     * @return _more_
     */
    public boolean doIt(int width, int height, int frameRate, Vector inFiles,
                        MediaLocator outML) {
        //        System.err.println ("Writing movie: WxH: "  + width + "/" + height + " rate:" + frameRate);


        ImageDataSource ids = new ImageDataSource(width, height, frameRate,
                                  inFiles);

        Processor processor;

        try {
            //      System.err.println("- create processor for the image datasource ...");
            processor = Manager.createProcessor(ids);
        } catch (Exception e) {
            LogUtil.logException(
                "Yikes!  Cannot create a processor from the data source.", e);
            return false;
        }

        processor.addControllerListener(this);

        // Put the Processor into configured state so we can set
        // some processing options on the processor.
        processor.configure();
        if ( !waitForState(processor, processor.Configured)) {
            LogUtil.userErrorMessage("Failed to configure the processor.");
            return false;
        }

        // Set the output content descriptor to QuickTime. 
        processor.setContentDescriptor(
            new ContentDescriptor(FileTypeDescriptor.QUICKTIME));

        // Query for the processor for supported formats.
        // Then set it on the processor.
        TrackControl       tcs[] = processor.getTrackControls();
        javax.media.Format f[]   = tcs[0].getSupportedFormats();
        if ((f == null) || (f.length <= 0)) {
            LogUtil.userErrorMessage(
                "The mux does not support the input format: "
                + tcs[0].getFormat());
            return false;
        }

        tcs[0].setFormat(f[0]);

        //      System.err.println("Setting the track format to: " + f[0]);

        // We are done with programming the processor.  Let's just
        // realize it.
        processor.realize();
        if ( !waitForState(processor, processor.Realized)) {
            LogUtil.userErrorMessage("Failed to realize the processor.");
            return false;
        }

        // Now, we'll need to create a DataSink.
        DataSink dsink;
        if ((dsink = createDataSink(processor, outML)) == null) {
            return false;
        }

        dsink.addDataSinkListener(this);
        fileDone = false;

        //      System.err.println("start processing...");

        // OK, we can now start the actual transcoding.
        try {
            processor.start();
            dsink.start();
        } catch (IOException e) {
            LogUtil.logException("IO error during processing", e);
            return false;
        }

        // Wait for EndOfStream event.
        waitForFileDone();

        // Cleanup.
        try {
            dsink.close();
        } catch (Exception e) {}
        processor.removeControllerListener(this);

        //      System.err.println("...done processing.");

        return true;
    }


    /**
     * Create the DataSink.
     *
     * @param p
     * @param outML
     * @return The newly created sink
     */
    DataSink createDataSink(Processor p, MediaLocator outML) {

        DataSource ds;

        if ((ds = p.getDataOutput()) == null) {
            LogUtil.userErrorMessage(
                "Something is really wrong: the processor does not have an output DataSource");
            return null;
        }

        DataSink dsink;

        try {
            //      System.err.println("- create DataSink for: " + outML);
            dsink = Manager.createDataSink(ds, outML);
            dsink.open();
        } catch (Exception e) {
            LogUtil.logException("Cannot create the DataSink: ", e);
            return null;
        }

        return dsink;
    }


    /** _more_ */
    Object waitSync = new Object();

    /** _more_ */
    boolean stateTransitionOK = true;

    /**
     * Block until the processor has transitioned to the given state.
     * Return false if the transition failed.
     *
     * @param p
     * @param state
     * @return _more_
     */
    boolean waitForState(Processor p, int state) {
        synchronized (waitSync) {
            try {
                while ((p.getState() < state) && stateTransitionOK) {
                    waitSync.wait();
                }
            } catch (Exception e) {}
        }
        return stateTransitionOK;
    }


    /**
     * Controller Listener.
     *
     * @param evt
     */
    public void controllerUpdate(ControllerEvent evt) {

        if ((evt instanceof ConfigureCompleteEvent)
                || (evt instanceof RealizeCompleteEvent)
                || (evt instanceof PrefetchCompleteEvent)) {
            synchronized (waitSync) {
                stateTransitionOK = true;
                waitSync.notifyAll();
            }
        } else if (evt instanceof ResourceUnavailableEvent) {
            synchronized (waitSync) {
                stateTransitionOK = false;
                waitSync.notifyAll();
            }
        } else if (evt instanceof EndOfMediaEvent) {
            evt.getSourceController().stop();
            evt.getSourceController().close();
        }
    }


    /** _more_ */
    Object waitFileSync = new Object();

    /** _more_ */
    boolean fileDone = false;

    /** _more_ */
    boolean fileSuccess = true;

    /**
     * Block until file writing is done.
     * @return _more_
     */
    boolean waitForFileDone() {
        synchronized (waitFileSync) {
            try {
                while ( !fileDone) {
                    waitFileSync.wait();
                }
            } catch (Exception e) {}
        }
        return fileSuccess;
    }


    /**
     * Event handler for the file writer.
     *
     * @param evt
     */
    public void dataSinkUpdate(DataSinkEvent evt) {

        if (evt instanceof EndOfStreamEvent) {
            synchronized (waitFileSync) {
                fileDone = true;
                waitFileSync.notifyAll();
            }
        } else if (evt instanceof DataSinkErrorEvent) {
            synchronized (waitFileSync) {
                fileDone    = true;
                fileSuccess = false;
                waitFileSync.notifyAll();
            }
        }
    }



    /**
     * _more_
     *
     * @param movieFile
     * @param width
     * @param height
     * @param frameRate
     * @param images
     */
    public static void createMovie(String movieFile, int width, int height,
                                   int frameRate, Vector images) {
        //        System.err.println ("images:" + images);
        MediaLocator oml;
        if ((oml = JpegImagesToMovie.createMediaLocator(movieFile)) == null) {
            LogUtil.userErrorMessage("Cannot build media locator from: "
                                     + movieFile);
            return;
        }
        JpegImagesToMovie imageToMovie = new JpegImagesToMovie();
        imageToMovie.doIt(width, height, frameRate, images, oml);
    }



    /**
     * _more_
     *
     * @param args
     */
    public static void main(String args[]) {

        //        System.err.println ("Running with new JpegImagesToMovie");


        if (args.length == 0) {
            prUsage();
        }

        // Parse the arguments.
        int    i          = 0;
        int    width      = -1,
               height     = -1,
               frameRate  = 1;
        Vector inputFiles = new Vector();
        String outputURL  = null;

        while (i < args.length) {

            if (args[i].equals("-w")) {
                i++;
                if (i >= args.length) {
                    prUsage();
                }
                width = new Integer(args[i]).intValue();
            } else if (args[i].equals("-h")) {
                i++;
                if (i >= args.length) {
                    prUsage();
                }
                height = new Integer(args[i]).intValue();
            } else if (args[i].equals("-f")) {
                i++;
                if (i >= args.length) {
                    prUsage();
                }
                frameRate = new Integer(args[i]).intValue();
            } else if (args[i].equals("-o")) {
                i++;
                if (i >= args.length) {
                    prUsage();
                }
                outputURL = args[i];
            } else {
                inputFiles.addElement(args[i]);
            }
            i++;
        }

        if ((outputURL == null) || (inputFiles.size() == 0)) {
            prUsage();
        }

        // Check for output file extension.
        if ( !outputURL.endsWith(".mov") && !outputURL.endsWith(".MOV")) {
            LogUtil.userErrorMessage(
                "The output file extension should end with a .mov extension");
            prUsage();
        }

        if ((width < 0) || (height < 0)) {
            LogUtil.userErrorMessage(
                "Please specify the correct image size.");
            prUsage();
        }

        // Check the frame rate.
        if (frameRate < 1) {
            frameRate = 1;
        }

        // Generate the output media locators.
        MediaLocator oml;

        if ((oml = createMediaLocator(outputURL)) == null) {
            LogUtil.userErrorMessage("Cannot build media locator from: "
                                     + outputURL);
            System.exit(0);
        }

        JpegImagesToMovie imageToMovie = new JpegImagesToMovie();
        imageToMovie.doIt(width, height, frameRate, inputFiles, oml);
        System.exit(0);
    }

    /**
     * _more_
     */
    static void prUsage() {
        System.err.println(
            "Usage: java JpegImagesToMovie -w <width> -h <height> -f <frame rate> -o <output URL> <input JPEG file 1> <input JPEG file 2> ...");
        System.exit(-1);
    }

    /**
     * Create a media locator from the given string.
     *
     * @param url
     * @return _more_
     */
    static MediaLocator createMediaLocator(String url) {

        MediaLocator ml;
        File         file   = new File(url);
        File         parent = file.getParentFile();

        //Check if this is the specification of a file
        //We can't just do file.exists because the file might not exist
        //So we get the parent directory and make sure it is a directory
        if ((parent != null) && parent.isDirectory()) {
            if ((ml = new MediaLocator("file:" + url)) != null) {
                return ml;
            }
        } else {}


        if ((url.indexOf(":") > 0) && (ml = new MediaLocator(url)) != null) {
            return ml;
        }

        if (url.startsWith(File.separator)) {
            if ((ml = new MediaLocator("file:" + url)) != null) {
                return ml;
            }
        } else {
            String fileName = "file:" + System.getProperty("user.dir")
                              + File.separator + url;
            if ((ml = new MediaLocator(fileName)) != null) {
                return ml;
            }
        }

        return null;
    }


    ///////////////////////////////////////////////
    //
    // Inner classes.
    ///////////////////////////////////////////////


    /**
     * A DataSource to read from a list of JPEG image files and
     * turn that into a stream of JMF buffers.
     * The DataSource is not seekable or positionable.
     */
    class ImageDataSource extends PullBufferDataSource {

        /** _more_ */
        ImageSourceStream streams[];

        /**
         * _more_
         *
         * @param width
         * @param height
         * @param frameRate
         * @param images
         *
         */
        ImageDataSource(int width, int height, int frameRate, Vector images) {
            streams = new ImageSourceStream[1];
            streams[0] = new ImageSourceStream(width, height, frameRate,
                    images);
        }

        /**
         * _more_
         *
         * @param source
         */
        public void setLocator(MediaLocator source) {}

        /**
         * _more_
         * @return _more_
         */
        public MediaLocator getLocator() {
            return null;
        }

        /**
         * Content type is of RAW since we are sending buffers of video
         * frames without a container format.
         * @return _more_
         */
        public String getContentType() {
            return ContentDescriptor.RAW;
        }

        /**
         * _more_
         */
        public void connect() {}

        /**
         * _more_
         */
        public void disconnect() {}

        /**
         * _more_
         */
        public void start() {}

        /**
         * _more_
         */
        public void stop() {}

        /**
         * Return the ImageSourceStreams.
         * @return _more_
         */
        public PullBufferStream[] getStreams() {
            return streams;
        }

        /**
         * We could have derived the duration from the number of
         * frames and frame rate.  But for the purpose of this program,
         * it's not necessary.
         * @return _more_
         */
        public Time getDuration() {
            return DURATION_UNKNOWN;
        }

        /**
         * _more_
         * @return _more_
         */
        public Object[] getControls() {
            return new Object[0];
        }

        /**
         * _more_
         *
         * @param type
         * @return _more_
         */
        public Object getControl(String type) {
            return null;
        }
    }


    /**
     * The source stream to go along with ImageDataSource.
     */
    class ImageSourceStream implements PullBufferStream {

        /** _more_ */
        Vector images;

        /** _more_ */
        int width, height;

        /** _more_ */
        VideoFormat format;

        /** _more_ */
        int nextImage = 0;  // index of the next image to be read.

        /** _more_ */
        boolean ended = false;

        /**
         * _more_
         *
         * @param width
         * @param height
         * @param frameRate
         * @param images
         *
         */
        public ImageSourceStream(int width, int height, int frameRate,
                                 Vector images) {
            this.width  = width;
            this.height = height;
            this.images = images;

            format = new VideoFormat(VideoFormat.JPEG,
                                     new Dimension(width, height),
                                     javax.media.Format.NOT_SPECIFIED,
                                     javax.media.Format.byteArray,
                                     (float) frameRate);
        }

        /**
         * We should never need to block assuming data are read from files.
         * @return _more_
         */
        public boolean willReadBlock() {
            return false;
        }

        /**
         * This is called from the Processor to read a frame worth
         * of video data.
         *
         * @param buf
         *
         * @throws IOException
         */
        public void read(Buffer buf) throws IOException {

            // Check if we've finished all the frames.
            if (nextImage >= images.size()) {
                // We are done.  Set EndOfMedia.
                //              System.err.println("Done reading all images.");
                buf.setEOM(true);
                buf.setOffset(0);
                buf.setLength(0);
                ended = true;
                return;
            }

            String imageFile = (String) images.elementAt(nextImage);
            //            System.err.println ("imageFile: " + imageFile);

            boolean convertedToJpg = false;
            if ( !imageFile.endsWith("jpg") && !imageFile.endsWith("jpeg")) {
                imageFile = ImageUtils.convertImageTo(imageFile, "jpg");
                convertedToJpg = true;
            }

            nextImage++;

            //      System.err.println("  - reading image file: " + imageFile);

            // Open a random access file for the next image. 
            RandomAccessFile raFile;
            raFile = new RandomAccessFile(imageFile, "r");

            byte data[] = null;

            // Check the input buffer type & size.
            //            System.err.println("Reading image:" + imageFile + " size=" +raFile.length());

            if (buf.getData() instanceof byte[]) {
                data = (byte[]) buf.getData();
            }

            // Check to see the given buffer is big enough for the frame.
            if ((data == null) || (data.length < raFile.length())) {
                data = new byte[(int) raFile.length()];
                buf.setData(data);
            }

            // Read the entire JPEG image from the file.
            raFile.readFully(data, 0, (int) raFile.length());

            //      System.err.println("    read " + raFile.length() + " bytes.");

            buf.setOffset(0);
            buf.setLength((int) raFile.length());
            buf.setFormat(format);
            buf.setFlags(buf.getFlags() | buf.FLAG_KEY_FRAME);

            // Close the random access file.
            raFile.close();
            if(convertedToJpg) {
                new File(imageFile).delete();
            }
        }

        /**
         * Return the format of each video frame.  That will be JPEG.
         * @return _more_
         */
        public javax.media.Format getFormat() {
            return format;
        }

        /**
         * _more_
         * @return _more_
         */
        public ContentDescriptor getContentDescriptor() {
            return new ContentDescriptor(ContentDescriptor.RAW);
        }

        /**
         * _more_
         * @return _more_
         */
        public long getContentLength() {
            return 0;
        }

        /**
         * _more_
         * @return _more_
         */
        public boolean endOfStream() {
            return ended;
        }

        /**
         * _more_
         * @return _more_
         */
        public Object[] getControls() {
            return new Object[0];
        }

        /**
         * _more_
         *
         * @param type
         * @return _more_
         */
        public Object getControl(String type) {
            return null;
        }
    }
}

