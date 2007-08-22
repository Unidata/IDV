/*
 * $Id: ImageSequenceGrabber.java,v 1.96 2007/08/13 18:38:55 jeffmc Exp $
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

package ucar.unidata.idv.ui;



import ij.*;

import org.w3c.dom.Element;

import ucar.unidata.data.GeoLocationInfo;
import ucar.unidata.data.gis.KmlDataSource;



import ucar.unidata.idv.*;

import ucar.unidata.ui.AnimatedGifEncoder;
import ucar.unidata.ui.ImageUtils;

import ucar.unidata.ui.JpegImagesToMovie;
import ucar.unidata.util.FileManager;

import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;

import ucar.visad.display.Animation;
import ucar.visad.display.AnimationWidget;

import visad.*;

import visad.georef.EarthLocation;


import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import java.beans.*;

import java.io.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;
import java.util.zip.*;


import javax.media.*;
import javax.media.control.*;
import javax.media.datasink.*;
import javax.media.format.VideoFormat;
import javax.media.protocol.*;
import javax.media.protocol.DataSource;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;





/**
 * Class ImageSequenceGrabber. Manages the movie capture dialog,
 * capturing a series of jpegs from a ViewManager, writing them out
 * to disk, previewing them and generating quicktime movies.
 *
 *
 * @author IDV Development Team
 */
public class ImageSequenceGrabber implements Runnable, ActionListener {


    /** How much we pause between animation captures */
    private static int SLEEP_TIME = 500;

    /** The default image file template */
    private static String dfltTemplate;


    /** Filter for HTML files */
    public static final PatternFileFilter FILTER_ANIS =
        new PatternFileFilter(".+\\.html|.+\\.htm",
                              "ANIS Applet HTML File (*.html)", ".html");


    /** xml tag or attr name */
    public static final String TAG_VISIBILITY = "visibility";

    /** xml tag or attr name */
    public static final String TAG_DESCRIPTION = "description";

    /** xml tag or attr name */
    public static final String ATTR_KML_NAME = "kml_name";

    /** xml tag or attr name */
    public static final String ATTR_KML_OPEN = "kml_open";

    /** xml tag or attr name */
    public static final String ATTR_KML_VISIBILITY = "kml_visibility";

    /** xml tag or attr name */
    public static final String ATTR_KML_DESC = "kml_desc";

    /** xml tag or attr name */
    public static final String ATTR_ANIS_WIDTH = "anis_width";

    /** xml tag or attr name */
    public static final String ATTR_ANIS_HEIGHT = "anis_height";

    /** xml tag or attr name */
    public static final String ATTR_ANIS_PREHTML = "anis_prehtml";

    /** xml tag or attr name */
    public static final String ATTR_ANIS_POSTHTML = "anis_posthtml";



    /** igml xml attributes */
    public static final String ATTR_FILENAME = "filename";


    /** igml xml attributes */
    public static final String ATTR_IMAGEDIR = "imagedir";

    /** igml xml attributes */
    public static final String ATTR_IMAGESUFFIX = "imagesuffix";

    /** igml xml attributes */
    public static final String ATTR_IMAGEPREFIX = "imageprefix";

    /** xml tag or attr name */
    public static final String ATTR_IMAGETEMPLATE = "imagetemplate";

    /** Property for the image file template */
    public static final String PROP_IMAGETEMPLATE =
        "imagesequencegrabber.template";


    /** igml xml attributes */
    public static final String ATTR_APPENDTIME = "appendtime";

    /** igml xml attributes */
    public static final String ATTR_STEPS = "steps";


    /** Action commands for gui buttons */
    public static final String CMD_GRAB = "grab";

    /** Action commands for gui buttons */
    public static final String CMD_PUBLISH = "publish";

    /** Action commands for gui buttons */
    public static final String CMD_GRAB_ANIMATION = "grab.animation";

    /** Action commands for gui buttons */
    public static final String CMD_CLEAR = "clear";

    /** Action commands for gui buttons */
    public static final String CMD_PREVIEW_PLAY = "preview.play";

    /** Action commands for gui buttons */
    public static final String CMD_PREVIEW_SHOW = "preview.show";

    /** Action commands for gui buttons */
    public static final String CMD_PREVIEW_NEXT = "preview.next";

    /** Action commands for gui buttons */
    public static final String CMD_PREVIEW_PREV = "preview.prev";

    /** Action commands for gui buttons */
    public static final String CMD_PREVIEW_DELETE = "preview.delete";

    /** Action commands for gui buttons */
    public static final String CMD_PREVIEW_CLOSE = "preview.close";




    /**
     *  The {@ref ViewManager} we are capturing.
     */
    private ViewManager viewManager;

    /** If non null then we capture from this */
    private JComponent alternateComponent;

    /** Used for synchronization_ */
    private Object MUTEX = new Object();


    /** The igml movie node. May be null */
    private Element scriptingNode;

    /** The igml */
    private ImageGenerator imageGenerator;

    /**
     *  A flag that tells us when we are automatically capturing the animation timesteps
     */
    private boolean capturingAnim = false;

    /**
     *  A virtual timestamp to keep us form having two animation captures running at the same time.
     */
    private int captureTimeStamp = 0;

    /**
     *  A flag that tells us when we are doing the automatic capture.
     */
    private boolean capturingAuto = false;


    /** The window for the main gui */
    JDialog mainDialog;

    /** File path of the last previewed image */
    String lastPreview;

    /** Where we show the preview */
    JLabel previewImage;

    /** The label for showing previews */
    JLabel previewLbl;

    /** Index in the list of images we are currently showing as a preview */
    int previewIndex = 0;

    /** The window for the previews */
    JDialog previewDialog;


    /** How many images have been captured */
    int imageCnt = 0;

    /** image quality */
    private float quality = 1.0f;

    /** Holds the filenames of the images we have captured */
    Vector images = new Vector();

    /** List of times corresponding to each image */
    Vector times = new Vector();

    /** List of earth locations corresponding to each image */
    Vector locs = new Vector();

    /** The directory we write to */
    String directory;

    /** Has the user paused the previews */
    boolean paused = false;

    /** Shows how many  frames have been captured */
    JLabel frameLbl;


    /** Image icon for playing */
    private ImageIcon playIcon;

    /** Image icon for stopping */
    private ImageIcon stopIcon;


    /** Specifies the display rate in the generated movie */
    JTextField displayRateFld;

    /** Specifies the capture rate */
    JTextField captureRateFld;

    /** Flag to see when we are playing a preview */
    private boolean isPlaying = false;

    /** Used for managing the current preview thread */
    private int timestamp = 0;

    /** radio button for high image quality */
    private JRadioButton hiBtn;

    /** radio button for medium image quality */
    private JRadioButton medBtn;

    /** radio button for low image quality */
    private JRadioButton lowBtn;

    /** Button  to delete a frame */
    JButton deleteFrameButton;

    /** Button  to play preview */
    JButton playButton;

    /** Button  to step back one frame */
    JButton prevButton;

    /** Button  to step forward one frame */
    JButton nextButton;

    /** Button to show the preview window */
    JButton previewButton;

    /** Turns on automatic capture */
    JButton grabAutoButton;


    /** Turns on animation based  capture */
    JButton grabAnimationButton;

    /** Captures one frame */
    JButton grabButton;

    /** Clear all captured frames */
    JButton clearButton;

    /** Write out the movie */
    JButton createButton;

    /** Close the window */
    JButton closeButton;

    /** Capture the main display */
    JRadioButton mainDisplayBtn;

    /** Capture the contents */
    JRadioButton contentsBtn;

    /** Capture full window */
    JRadioButton fullWindowBtn;


    /** This is the Animation from the ViewManager. */
    private Animation anime;

    /** This is the Animation from the ViewManager. */
    private AnimationWidget animationWidget;

    /** If non-null then we use this and don't ask the user. */
    private String movieFileName;

    /** widget */
    JCheckBox animationResetCbx = new JCheckBox("Reset to start time", true);

    /** Is the background of the image transparent_ */
    JCheckBox backgroundTransparentBtn;

    /** Have we told the user about constraints of writing transparent images */
    boolean notifiedForTransparent = false;

    /** Holds the directory */
    private JTextField alternateDirFld;

    /** Holds the file prefix */
    private JTextField fileTemplateFld;

    /** Should use alternate dir */
    private JCheckBox alternateDirCbx;

    /** Components to enable/disable */
    private List alternateComps = new ArrayList();

    /** The IDV */
    private IntegratedDataViewer idv;

    /**
     * Create me with the given {@link ucar.unidata.idv.ViewManager}
     *
     * @param viewManager The view manager we are capturing images from
     *
     */
    public ImageSequenceGrabber(ViewManager viewManager) {
        this(viewManager, null);
    }



    /**
     * Create me with the given {@link ucar.unidata.idv.ViewManager}
     *
     * @param viewManager The view manager we are capturing images from
     * @param alternateComponent If non null then use this component as
     * the source of the image
     *
     */
    public ImageSequenceGrabber(ViewManager viewManager,
                                JComponent alternateComponent) {
        this.alternateComponent = alternateComponent;
        this.viewManager        = viewManager;
        this.idv                = viewManager.getIdv();
        init();
    }







    /**
     *  This gets called when we automatically create a movie. It will not show the
     *  dialog window and will start up the animation capture
     *
     * @param viewManager The view manager we are capturing images from
     * @param filename The file we are writing to
     * @param idv The IDV
     */
    public ImageSequenceGrabber(ViewManager viewManager, String filename,
                                IntegratedDataViewer idv) {
        this(viewManager, filename, idv, null, null);
    }


    /**
     *  This gets called when we automatically create a movie. It will not show the
     *  dialog window and will start up the animation capture
     *
     * @param viewManager The view manager we are capturing images from
     * @param filename The file we are writing to
     * @param idv The IDV
     * @param scriptingNode The igml node
     * @param imageGenerator  imageGenerator
     */
    public ImageSequenceGrabber(ViewManager viewManager, String filename,
                                IntegratedDataViewer idv,
                                ImageGenerator imageGenerator,
                                Element scriptingNode) {
        this.viewManager    = viewManager;
        this.imageGenerator = imageGenerator;
        this.scriptingNode  = scriptingNode;
        movieFileName       = filename;
        if (scriptingNode != null) {
            movieFileName = imageGenerator.applyMacros(scriptingNode,
                    ATTR_FILENAME, movieFileName);
        }

        this.idv = idv;
        init();
        startAnimationCapture();
    }

    /**
     *  This gets called when we automatically create a movie. It will not show the
     *  dialog window and will start up the animation capture
     *
     * @param filename The file we are writing to
     * @param idv The IDV
     * @param scriptingNode The igml node
     * @param imageGenerator  imageGenerator
     * @param imageFiles List of files to write
     * @param size Size of image
     * @param displayRate Display rate
     */
    public ImageSequenceGrabber(String filename, IntegratedDataViewer idv,
                                ImageGenerator imageGenerator,
                                Element scriptingNode, List imageFiles,
                                Dimension size, double displayRate) {
        this.idv            = idv;
        this.imageGenerator = imageGenerator;
        this.scriptingNode  = scriptingNode;
        this.images         = new Vector(imageFiles);
        this.idv            = idv;
        movieFileName       = filename;
        createMovie(movieFileName, images, null, null, size, displayRate,
                    null);
    }






    /**
     * Show the main window
     */
    public void show() {
        mainDialog.setVisible(true);
    }

    /**
     * Utility to make a button
     *
     * @param icon The button icon
     * @param cmd The action command
     * @param tooltip The tooltip
     * @return The button
     */
    private JButton makeButton(ImageIcon icon, String cmd, String tooltip) {
        JButton b = GuiUtils.getImageButton(icon);
        b.setActionCommand(cmd);
        b.addActionListener(this);
        b.setToolTipText(tooltip);
        return b;
    }

    /**
     * Utility to make a button
     *
     * @param label The button label
     * @param cmd The action command
     * @return The button
     */

    private JButton makeButton(String label, String cmd) {
        JButton b = new JButton(label);
        b.setActionCommand(cmd);
        b.addActionListener(this);
        return b;
    }

    /**
     * Add the component to the list of components for the alternate dir stuff.
     *
     * @param comp The component
     *
     * @return The component
     */
    private JComponent addAlternateComp(JComponent comp) {
        alternateComps.add(comp);
        GuiUtils.enableTree(comp, false);
        return comp;
    }


    /**
     * Initialize me. Create the windows, etc.
     */
    private void init() {

        //Get the animation from the VM
        anime           = viewManager.getAnimation();
        animationWidget = viewManager.getAnimationWidget();

        //Store the images in a unique (by current time) subdir of the user's tmp  dir
        directory =
            IOUtil.joinDir(viewManager.getStore().getUserTmpDirectory(),
                           "images_" + System.currentTimeMillis());

        //Make sure the dir exists
        IOUtil.makeDir(directory);

        mainDialog     = GuiUtils.createDialog("Movie Capture", false);

        frameLbl       = GuiUtils.cLabel("No frames");

        displayRateFld = new JTextField("2", 3);

        captureRateFld = new JTextField("2", 3);

        String imgp = "/auxdata/ui/icons/";

        mainDisplayBtn = new JRadioButton("Main Display", true);
        contentsBtn    = new JRadioButton("Outer Contents", false);
        fullWindowBtn  = new JRadioButton("Full Window", false);
        GuiUtils.buttonGroup(mainDisplayBtn, fullWindowBtn).add(contentsBtn);

        JComponent whatPanel = GuiUtils.vbox(new JLabel("What to capture:"),
                                             mainDisplayBtn, contentsBtn,
                                             fullWindowBtn);



        alternateDirFld = new JTextField("", 30);
        JButton alternateDirBtn = new JButton("Select");
        GuiUtils.setupDirectoryChooser(alternateDirBtn, alternateDirFld);

        if (dfltTemplate == null) {
            dfltTemplate = idv.getStore().get(PROP_IMAGETEMPLATE,
                    "image_%count%_%time%");
        }
        fileTemplateFld = new JTextField(dfltTemplate, 30);
        fileTemplateFld.setToolTipText(
            "<html>Enter the file name template to use.<br><b>%count%</b> is the image counter<br>"
            + "<b>%time%</b> is the  animation time in the default format<br>"
            + "<b>%time:some time format string%</b> a macro that begins with &quot;time:&quot;,contains a time format string using the:<br>"
            + "java SimpleDateFormat formatting (see google)." + "</html>");
        alternateDirCbx = new JCheckBox("Save Files To:", false);
        alternateDirCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                for (int i = 0; i < alternateComps.size(); i++) {
                    GuiUtils.enableTree((JComponent) alternateComps.get(i),
                                        alternateDirCbx.isSelected());
                }
            }
        });


        hiBtn  = new JRadioButton("High", true);
        medBtn = new JRadioButton("Better", false);
        lowBtn = new JRadioButton("Low", false);
        GuiUtils.buttonGroup(hiBtn, medBtn).add(lowBtn);


        grabButton          = makeButton("One Image", CMD_GRAB);
        grabAutoButton      = makeButton("Automatically", GuiUtils.CMD_START);
        grabAnimationButton = makeButton("Time Animation",
                                         CMD_GRAB_ANIMATION);



        List frameButtons = new ArrayList();
        frameButtons.add(previewButton = makeButton("Preview",
                CMD_PREVIEW_SHOW));
        frameButtons.add(clearButton = makeButton("Delete All", CMD_CLEAR));
        frameButtons.add(createButton = makeButton("Save Movie",
                GuiUtils.CMD_OK));
        JComponent publishButton;
        if (idv.getPublishManager().isPublishingEnabled()) {
            frameButtons.add(publishButton = makeButton("Publish Movie",
                    CMD_PUBLISH));
        } else {
            //            publishButton = new JPanel();
        }



        closeButton = makeButton("Close", GuiUtils.CMD_CLOSE);
        JLabel titleLbl =
            GuiUtils.cLabel(
                "Note: Make sure the view window is not obscured");
        JPanel titlePanel = GuiUtils.inset(titleLbl, 8);

        JPanel runPanel =
            GuiUtils.hflow(Misc.newList(GuiUtils.rLabel(" Rate: "),
                                        captureRateFld,
                                        new JLabel(" seconds")));


        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
        JPanel capturePanel = GuiUtils.doLayout(new Component[] {
            grabButton, GuiUtils.filler(), grabAnimationButton,
            animationResetCbx, GuiUtils.top(grabAutoButton), runPanel
        }, 2, GuiUtils.WT_N, GuiUtils.WT_N);


        backgroundTransparentBtn = new JCheckBox("Background Transparent");
        backgroundTransparentBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (backgroundTransparentBtn.isSelected()
                        && !notifiedForTransparent) {
                    LogUtil.userMessage(
                        "Note: Only KMZ files can be saved with background transparency on");
                }
                notifiedForTransparent = true;
            }
        });
        capturePanel = GuiUtils.hbox(capturePanel,
                                     GuiUtils.top(GuiUtils.vbox(whatPanel,
                                         backgroundTransparentBtn)));


        capturePanel = GuiUtils.inset(GuiUtils.left(capturePanel), 5);
        capturePanel.setBorder(BorderFactory.createTitledBorder("Capture"));


        JComponent altDirPanel =
            GuiUtils.centerRight(
                GuiUtils.wrap(addAlternateComp(alternateDirFld)),
                addAlternateComp(alternateDirBtn));

        GuiUtils.tmpInsets = new Insets(0, 5, 0, 5);
        JPanel filePanel = GuiUtils.doLayout(new Component[] {
            GuiUtils.rLabel("Image Quality:"),
            GuiUtils.left(GuiUtils.hbox(hiBtn, medBtn, lowBtn)),
            alternateDirCbx, GuiUtils.filler(),
            addAlternateComp(GuiUtils.rLabel("Directory:")), altDirPanel,
            addAlternateComp(GuiUtils.rLabel("Filename Template:")),
            GuiUtils.left(addAlternateComp(fileTemplateFld))
        }, 2, GuiUtils.WT_NY, GuiUtils.WT_N);

        filePanel = GuiUtils.inset(filePanel, 5);
        filePanel.setBorder(BorderFactory.createTitledBorder("Image Files"));


        JPanel framesPanel = GuiUtils.vbox(GuiUtils.left(frameLbl),
                                           GuiUtils.hflow(frameButtons, 4,
                                               0));
        framesPanel = GuiUtils.inset(framesPanel, 5);
        framesPanel.setBorder(BorderFactory.createTitledBorder("Frames"));

        JPanel contents = GuiUtils.vbox(capturePanel, filePanel, framesPanel);
        contents = GuiUtils.inset(contents, 5);

        mainDialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                close();
            }
        });

        contents = GuiUtils.topCenter(titlePanel, contents);


        imagesChanged();
        checkEnabled();

        //Only show the window if init filename is null
        if ((movieFileName == null) && (scriptingNode == null)) {
            GuiUtils.packDialog(mainDialog,
                                GuiUtils.centerBottom(contents,
                                    GuiUtils.wrap(GuiUtils.inset(closeButton,
                                        4))));
            mainDialog.setVisible(true);
        }
    }


    /**
     * Load in the current preview image into the gui
     */
    private void setPreviewImage() {
        if (previewImage == null) {
            return;
        }

        synchronized (MUTEX) {
            if (previewIndex >= images.size()) {
                previewIndex = images.size() - 1;
            }
            if (previewIndex < 0) {
                previewIndex = 0;
            }
            boolean haveImages = images.size() > 0;
            prevButton.setEnabled(haveImages);
            nextButton.setEnabled(haveImages);
            playButton.setEnabled(haveImages);
            deleteFrameButton.setEnabled(haveImages);
            if (haveImages) {
                String current = images.get(previewIndex).toString();
                if ( !Misc.equals(current, lastPreview)) {
                    Image image =
                        Toolkit.getDefaultToolkit().createImage(current);
                    ImageIcon icon = new ImageIcon(image);
                    previewImage.setIcon(icon);
                    previewImage.setText(null);
                    lastPreview = current;
                }
                previewLbl.setText("  Frame: " + (previewIndex + 1) + "/"
                                   + images.size());
            } else {
                previewLbl.setText("  Frame:        ");
                previewImage.setText("   No images   ");
                previewImage.setIcon(null);
                lastPreview = null;
            }
        }
    }



    /**
     * Show the preview window
     */
    private void showPreview() {
        if (previewDialog == null) {
            previewDialog = new JDialog(mainDialog, "Movie Preview", false);


            String imgp = "/auxdata/ui/icons/";
            playIcon   = GuiUtils.getImageIcon(imgp + "Play16.gif");
            stopIcon   = GuiUtils.getImageIcon(imgp + "Stop16.gif");


            playButton = makeButton(playIcon, CMD_PREVIEW_PLAY, "Play/Stop");


            nextButton = makeButton(GuiUtils.getImageIcon(imgp
                    + "StepForward16.gif"), CMD_PREVIEW_NEXT,
                                            "Go to next frame");
            prevButton = makeButton(GuiUtils.getImageIcon(imgp
                    + "StepBack16.gif"), CMD_PREVIEW_PREV,
                                         "Go to previous frame");

            deleteFrameButton = makeButton("Delete this frame",
                                           CMD_PREVIEW_DELETE);
            JPanel buttons = GuiUtils.hflow(Misc.newList(prevButton,
                                 playButton, nextButton, deleteFrameButton));

            buttons      = GuiUtils.inset(buttons, 5);

            previewLbl   = new JLabel("  ");
            previewImage = new JLabel();
            lastPreview  = null;
            previewIndex = 0;
            setPreviewImage();
            previewImage.setBorder(BorderFactory.createEtchedBorder());
            JPanel contents =
                GuiUtils.topCenterBottom(GuiUtils.hflow(Misc.newList(buttons,
                    previewLbl)), previewImage,
                                  GuiUtils.wrap(makeButton("Close",
                                      CMD_PREVIEW_CLOSE)));
            GuiUtils.packDialog(previewDialog, contents);
        }
        previewDialog.setVisible(true);

    }

    /**
     * Enable/disable buttons as needed
     */
    private void checkEnabled() {
        //      if (capturingAuto || capturingAnim) {
        //          grabAutoButton.setIcon (stopIcon);
        //      }
        //      else {
        //          grabAutoButton.setIcon (startIcon);
        //      }
        grabAnimationButton.setEnabled( !capturingAuto);
        grabButton.setEnabled( !capturingAuto && !capturingAnim);
    }

    /**
     * Turn on automatic  capture
     */
    private void startCapturingAuto() {
        grabAutoButton.setText("Stop");
        if (capturingAuto) {
            return;
        }
        capturingAuto = true;
        Misc.run(this);
        checkEnabled();
    }

    /**
     * Turn off automatic  capture
     */
    private void stopCapturingAuto() {
        grabAutoButton.setText("Automatically");
        if ( !capturingAuto) {
            return;
        }
        capturingAuto = false;
        checkEnabled();
    }



    /**
     * Turn on animation based  capture
     */
    private void startAnimationCapture() {
        if (capturingAnim || (anime == null)) {
            return;
        }
        grabAnimationButton.setText("Stop animation");
        capturingAnim = true;
        checkEnabled();
        //Run the imageGrab in another thread because visad errors when the getImage
        //is called from the awt event thread
        Misc.run(new Runnable() {
            public void run() {
                runAnimationCapture(++captureTimeStamp);
            }
        });
    }

    /**
     * Should we keep running
     *
     * @param timestamp The virtual timestamp we started the run with
     *
     * @return keep running
     */
    private boolean keepRunning(int timestamp) {
        if ( !capturingAnim || (timestamp != captureTimeStamp)) {
            return false;
        }
        return true;
    }

    /**
     *  Actually step through each timestep. We use the timestamp argument to determine
     *  if the user  hit Stop/Start in quick succession. THis prevents us from having two threads
     *  active at once.
     *
     * @param timestamp Used to know which thread should be running
     */
    private void runAnimationCapture(int timestamp) {
        try {
            anime.setAnimating(false);
            if (animationResetCbx.isSelected()) {
                animationWidget.gotoBeginning();
            }

            int sleepTime =
                idv.getStateManager().getProperty("idv.capture.sleep",
                    SLEEP_TIME);
            if ((scriptingNode != null)
                    && XmlUtil.hasAttribute(scriptingNode, ATTR_STEPS)) {
                String stepsString =
                    imageGenerator.applyMacros(scriptingNode, ATTR_STEPS);
                int[] steps = Misc.parseInts(stepsString, ",");
                for (int i = 0; i < steps.length; i++) {
                    if (steps[i] > anime.getNumSteps()) {
                        break;
                    }
                    anime.setCurrent(steps[i]);
                    //Sleep for a bit  to allow for the display to redraw itself
                    try {
                        Misc.sleep(sleepTime);
                    } catch (Exception exc) {}
                    //Has the user pressed Stop?
                    if ( !keepRunning(timestamp)) {
                        break;
                    }
                    //Now grab the image in block mode
                    grabImageAndBlock();
                }

            } else {
                while (true) {
                    //Sleep for a bit  to allow for the display to redraw itself
                    try {
                        Misc.sleep(sleepTime);
                    } catch (Exception exc) {}
                    //Has the user pressed Stop?
                    if ( !keepRunning(timestamp)) {
                        break;
                    }
                    //Now grab the image in block mode
                    grabImageAndBlock();
                    int current = anime.getCurrent();
                    animationWidget.stepForward();
                    if (current == anime.getCurrent()) {
                        break;
                    }
                }
            }



            if ( !keepRunning(timestamp)) {
                return;
            }
            stopAnimationCapture();
        } catch (Exception exc) {
            LogUtil.logException("Creating movie", exc);
        }
    }


    /**
     * Turn off animation based  capture
     */
    private void stopAnimationCapture() {
        capturingAnim = false;
        writeMovie();
        //This implies we write the animation and then are done
        if (imageGenerator != null) {
            //            close();
            imageGenerator.doneCapturingMovie();
            //            return;
        }
        grabAnimationButton.setText("Capture animation");
        checkEnabled();
    }


    /**
     * Get the image quality
     *
     * @return the image quality
     */
    private float getImageQuality() {
        float quality = 1.0f;
        if (medBtn.isSelected()) {
            quality = 0.6f;
        } else if (lowBtn.isSelected()) {
            quality = 0.2f;
        }
        return quality;
    }

    /**
     * Stop playing preview
     */
    private void previewStopPlaying() {
        isPlaying = false;
        playButton.setIcon(playIcon);
    }

    /**
     * Start playing preview. This calls {@see #previewStartPlaying(int)}
     * in a thread.
     */
    private void previewStartPlaying() {
        isPlaying = true;
        playButton.setIcon(stopIcon);
        Misc.run(new Runnable() {
            public void run() {
                previewStartPlaying(++timestamp);
            }
        });
    }

    /**
     * Start playing preview.  Called from a thread
     *
     * @param ts The timestamp for thread management
     */
    private void previewStartPlaying(int ts) {
        while (isPlaying && (ts == timestamp) && (images.size() > 0)) {
            previewNext();
            try {
                Misc.sleep(1000);
            } catch (Exception exc) {}
            if (previewIndex >= images.size() - 1) {
                isPlaying = false;
            }
        }
        if ( !isPlaying) {
            previewStopPlaying();
        }
    }


    /**
     * GO to the next frame in the previews
     */
    private void previewNext() {
        previewIndex++;
        if (previewIndex >= images.size()) {
            previewIndex = 0;
        }
        setPreviewImage();
    }

    /**
     * Handle gui actions
     *
     * @param ae The <code>ActionEvent</code>
     */
    public void actionPerformed(ActionEvent ae) {
        String cmd = ae.getActionCommand();
        if (cmd.equals(GuiUtils.CMD_START)) {
            if ( !capturingAuto) {
                startCapturingAuto();
            } else {
                stopCapturingAuto();
            }
        } else if (cmd.equals(CMD_CLEAR)) {
            capturingAuto = false;
            deleteFiles();
            imagesChanged();
            checkEnabled();
        } else if (cmd.equals(CMD_PREVIEW_PLAY)) {
            if (isPlaying) {
                previewStopPlaying();
            } else {
                previewStartPlaying();
            }
        } else if (cmd.equals(CMD_PREVIEW_SHOW)) {
            showPreview();
        } else if (cmd.equals(CMD_PREVIEW_CLOSE)) {
            if (previewDialog != null) {
                previewDialog.setVisible(false);
            }
        } else if (cmd.equals(CMD_PREVIEW_NEXT)) {
            previewNext();
        } else if (cmd.equals(CMD_PREVIEW_PREV)) {
            previewIndex--;
            if (previewIndex < 0) {
                previewIndex = images.size() - 1;
            }
            setPreviewImage();
        } else if (cmd.equals(CMD_PREVIEW_DELETE)) {
            if ((images.size() > 0) && (previewIndex >= 0)
                    && (previewIndex < images.size())) {
                String filename = images.get(previewIndex).toString();
                images.remove(previewIndex);
                times.remove(previewIndex);
                locs.remove(previewIndex);
                previewIndex--;
                imagesChanged();
                new File(filename).delete();
            }
        } else if (cmd.equals(CMD_GRAB_ANIMATION)) {
            synchronized (MUTEX) {
                if (capturingAnim) {
                    stopAnimationCapture();
                } else {
                    startAnimationCapture();
                }
            }
        } else if (cmd.equals(CMD_GRAB)) {
            //Run the imageGrab in another thread because visad errors when the getImage
            //is called from the awt event thread
            Misc.run(new Runnable() {
                public void run() {
                    grabImageAndBlock();
                }
            });
        } else if (cmd.equals(GuiUtils.CMD_CLOSE)) {
            close();
        } else if (cmd.equals(CMD_PUBLISH)) {
            publishMovie();
        } else if (cmd.equals(GuiUtils.CMD_OK)) {
            writeMovie();
        }
    }


    /**
     * Is this being run interactively
     *
     * @return is interactive
     */
    private boolean isInteractive() {
        return scriptingNode == null;
    }


    /**
     * Write out the movie
     */
    private void writeMovie() {
        stopCapturingAuto();
        String filename = null;
        if (isInteractive() && (movieFileName == null)) {
            JComponent extra =
                GuiUtils.topCenter(
                    GuiUtils.hflow(
                        Misc.newList(
                            GuiUtils.rLabel(" Frames per second:"),
                            displayRateFld)), GuiUtils.filler());
            filename =
                FileManager.getWriteFile(Misc.newList(FileManager.FILTER_MOV,
                                                      FileManager.FILTER_AVI,
                    FileManager.FILTER_ANIMATEDGIF, FileManager.FILTER_KMZ,
                    FILTER_ANIS), FileManager.SUFFIX_MOV, extra);
        } else {
            filename = movieFileName;
        }


        if (filename != null) {
            //            if ( !filename.toLowerCase().endsWith(".mov")) {
            //                filename = filename + ".mov";
            //            }
            createMovie(filename);
        }

    }


    /**
     * Used by the pubishing facility to publish movides to some external site
     */
    private void publishMovie() {
        stopCapturingAuto();
        String uid  = Misc.getUniqueId();
        String tail = uid + ".mov";
        String file = idv.getStore().getTmpFile(tail);
        createMovie(file);
        idv.getPublishManager().doPublish("Publish Quicktime file", file);
    }

    /**
     * Close the window
     */
    private void close() {
        anime = null;

        if (previewDialog != null) {
            previewDialog.dispose();
            previewDialog = null;
        }

        capturingAuto = false;
        viewManager.clearImageGrabber(this);

        mainDialog.dispose();
        try {
            if ( !alternateDirCbx.isSelected()) {
                deleteFiles();
                (new File(directory)).delete();
            }
        } catch (Exception exc) {
            LogUtil.logException("Disposing of tmp directory", exc);
        }
    }

    /**
     *  Get rid of generated files
     */
    private void deleteFiles() {
        for (int i = 0; i < images.size(); i++) {
            (new File((String) images.get(i))).delete();
        }
        images = new Vector();
        times  = new Vector();
        locs   = new Vector();
    }

    /**
     * Run in a thread.
     */
    public void run() {
        capturingAuto = true;
        while (capturingAuto) {
            grabImageAndBlock();
            double captureRate = 2.0;
            try {
                captureRate = (new Double(
                    captureRateFld.getText().trim())).doubleValue();
            } catch (NumberFormatException nfe) {
                stopCapturingAuto();
                LogUtil.userErrorMessage(
                    "Bad number format for capture rate: "
                    + captureRateFld.getText());
                return;
            }
            Misc.sleep((long) (captureRate * 1000));
        }
    }


    /**
     * What file suffix should we use for the images. For now better by jpg
     *
     * @return File suffix
     */
    protected String getFileSuffix() {
        if (scriptingNode != null) {
            return imageGenerator.applyMacros(scriptingNode,
                    ATTR_IMAGESUFFIX, "jpg");
        }
        if ((backgroundTransparentBtn != null)
                && backgroundTransparentBtn.isSelected()) {
            return "png";
        }
        //        if(true) return "png";
        return "jpg";
    }




    /**
     * Get the file prefix to use
     *
     *
     * @param cnt   image count
     * @return File prefix
     */
    private String getFilePrefix(int cnt) {
        String filename = "";
        String template = "image_%count%_%time%";
        if (scriptingNode != null) {
            if ( !XmlUtil.hasAttribute(scriptingNode, ATTR_IMAGEPREFIX)) {
                template = imageGenerator.applyMacros(scriptingNode,
                        ATTR_IMAGETEMPLATE, template);
            } else {
                template = imageGenerator.applyMacros(scriptingNode,
                        ATTR_IMAGEPREFIX, "image");

                template = template + "_%count%";
                if (imageGenerator.applyMacros(scriptingNode,
                        ATTR_APPENDTIME, false)) {
                    template = template + "_%time%";
                }
            }
        } else {
            if (alternateDirCbx.isSelected()) {
                template = fileTemplateFld.getText().trim();
                if ( !template.equals(dfltTemplate)) {
                    dfltTemplate = template;
                    idv.getStore().put(PROP_IMAGETEMPLATE, dfltTemplate);
                    idv.getStore().save();
                }
            }
        }

        template = StringUtil.replace(template, "%count%", "" + cnt);

        try {
            DateTime dttm       = new DateTime(anime.getAniValue());

            String   timeString = "" + dttm;
            timeString = StringUtil.replace(timeString, ":", "_");
            timeString = StringUtil.replace(timeString, "-", "_");
            timeString = StringUtil.replace(timeString, " ", "_");
            template   = StringUtil.replace(template, "%time%", timeString);
            template   = ucar.visad.UtcDate.applyTimeMacro(template, dttm);
        } catch (Exception exc) {}
        template   = StringUtil.replace(template, "/", "_");
        template   = StringUtil.replace(template, "\\", "_");

        return template;
    }

    /**
     * Get the dir to use
     *
     * @return dir
     */
    private String getFileDirectory() {
        String filename = "";
        if (scriptingNode != null) {
            filename = imageGenerator.applyMacros(scriptingNode,
                    ATTR_IMAGEDIR, filename);
        } else {
            if (alternateDirCbx.isSelected()) {
                filename = alternateDirFld.getText().trim();
                if (filename.length() == 0) {
                    filename = directory;
                    alternateDirFld.setText(directory);
                } else {
                    //                    IOUtil.makeDir(filename);
                }
            }
        }

        if (filename.length() == 0) {
            filename = directory;
        }
        IOUtil.makeDir(filename);
        return filename;
    }




    /**
     * Take a screen snapshot in blocking mode
     */
    private void grabImageAndBlock() {
        synchronized (MUTEX) {
            String filename = getFilePrefix(imageCnt++);
            String tmp = filename.toLowerCase();
            if (!(tmp.endsWith(".gif") ||
                  tmp.endsWith(".png") ||
                  tmp.endsWith(".jpg") ||
                  tmp.endsWith(".jpeg"))) {
                filename = filename + "." + getFileSuffix();
            }

            String path = IOUtil.joinDir(getFileDirectory(), filename);
            //            System.err.println ("ImageSequenceGrabber file dir: " +getFileDirectory() +" path: " +  path);
            DateTime time = null;
            try {
                time = (((anime != null) && (anime.getAniValue() != null))
                        ? new DateTime(anime.getAniValue())
                        : null);
            } catch (Exception exc) {
                LogUtil.logException("Getting animation time", exc);
            }
            if (alternateComponent != null) {
                try {
                    GuiUtils.toFront(GuiUtils.getFrame(alternateComponent));
                    Misc.sleep(50);
                    ImageUtils.writeImageToFile(alternateComponent, path);
                } catch (Exception nfe) {
                    LogUtil.userErrorMessage("Error");
                    return;
                }
            } else {
                viewManager.toFront();
                Misc.sleep(100);
                if (imageGenerator != null) {
                    try {
                        //                        System.err.println ("Calling getImage");
                        BufferedImage image =
                            viewManager.getMaster().getImage(false);
                        //                        System.err.println ("After Calling getImage");
                        Hashtable props = new Hashtable();
                        props.put(ImageGenerator.PROP_IMAGEPATH, path);
                        props.put(ImageGenerator.PROP_IMAGEFILE,
                                  IOUtil.getFileTail(path));
                        props.put(ImageGenerator.PROP_IMAGEINDEX,
                                  new Integer(images.size()));
                        imageGenerator.processImage(image, path,
                                scriptingNode, props, viewManager);
                    } catch (Throwable exc) {
                        LogUtil.userErrorMessage("Error processing image:"
                                + exc);
                        exc.printStackTrace();
                        return;
                    }
                } else {
                    try {
                        Component comp;
                        if (fullWindowBtn.isSelected()) {
                            comp = viewManager.getDisplayWindow()
                                .getComponent();
                        } else if (mainDisplayBtn.isSelected()) {
                            comp = viewManager.getMaster().getComponent();
                        } else {
                            comp = viewManager.getContents();
                        }
                        Dimension dim   = comp.getSize();
                        Point     loc   = comp.getLocationOnScreen();
                        Robot     robot = new Robot();
                        BufferedImage image =
                            robot.createScreenCapture(new Rectangle(loc.x,
                                loc.y, dim.width, dim.height));

                        if (backgroundTransparentBtn.isSelected()) {
                            image = ImageUtils.makeColorTransparent(image,
                                    viewManager.getBackground());
                        }
                        ImageUtils.writeImageToFile(image, path,
                                getImageQuality());
                    } catch (Exception exc) {
                        LogUtil.logException("Saving image file", exc);
                    }
                }
            }
            images.add(path);
            times.add(time);
            //TODO
            if (viewManager != null) {
                locs.add(viewManager.getVisibleGeoBounds());
            } else {
                locs.add(null);
            }
            imagesChanged();
        }
    }



    /**
     * The list of images have changed. Update the UI.
     */
    private void imagesChanged() {
        synchronized (MUTEX) {
            if (images.size() == 0) {
                lastPreview = null;
            }
            if (images.size() == 0) {
                frameLbl.setText("No frames");
            } else if (images.size() == 1) {
                frameLbl.setText(images.size() + " frame");
            } else {
                frameLbl.setText(images.size() + " frames");
            }
            createButton.setEnabled(images.size() > 0);
            clearButton.setEnabled(images.size() > 0);
            previewButton.setEnabled(images.size() > 0);
            setPreviewImage();
        }
    }

    /**
     * actually create the movie
     *
     * @param movieFile Where to write the movie
     */
    private void createMovie(String movieFile) {
        Component comp = viewManager.getMaster().getDisplayComponent();
        //Get the size of the display
        Dimension size = comp.getSize();
        double displayRate =
            (new Double(displayRateFld.getText())).doubleValue();

        createMovie(movieFile, images, times, locs, size, displayRate,
                    scriptingNode);
    }

    /** widget for saving html */
    private static JTextArea preFld;

    /** widget for saving html */
    private static JTextArea postFld;

    /** widget for saving html */
    private static JTextField widthFld;

    /** widget for saving html */
    private static JTextField heightFld;

    /** widget for saving html */
    private static JCheckBox copyCbx;

    /**
     * actually create the movie
     *
     *
     * @param commaSeparatedFiles This can be a list of comma separated files. eg: .mov, .kmz, etc
     * @param images List of images to make a movie from
     * @param times List of times
     * @param locs List of bounds
     * @param size size
     * @param displayRate display rate
     * @param scriptingNode isl node. May be null
     */
    private void createMovie(String commaSeparatedFiles, List images,
                             List times, List locs, Dimension size,
                             double displayRate, Element scriptingNode) {

        List fileToks = StringUtil.split(commaSeparatedFiles, ",", true,
                                         true);


        for (int i = 0; i < fileToks.size(); i++) {
            String movieFile = (String) fileToks.get(i);
            try {
                //                System.err.println("createMovie:" + movieFile);
                if (movieFile.toLowerCase().endsWith(".gif")) {
                    double rate = 1.0 / displayRate;
                    //                    System.err.println("images:" + images);
                    AnimatedGifEncoder.createGif(movieFile, images,
                            AnimatedGifEncoder.REPEAT_FOREVER,
                            (int) (rate * 1000));
                } else if (movieFile.toLowerCase().endsWith(".htm")
                           || movieFile.toLowerCase().endsWith(".html")) {
                    createAnisHtml(movieFile, images, times, size,
                                   displayRate, scriptingNode);
                } else if (movieFile.toLowerCase().endsWith(".kmz")
                           || movieFile.toLowerCase().endsWith(".kml")) {
                    createKmz(movieFile, images, times, locs, size,
                              displayRate, scriptingNode);

                } else if (movieFile.toLowerCase().endsWith(".avi")) {
                    ImageUtils.writeAvi(images, displayRate, new File(movieFile));
                } else {
                    //                    System.err.println("mov:" + movieFile);
                    SecurityManager backup = System.getSecurityManager();
                    System.setSecurityManager(null);
                    JpegImagesToMovie.createMovie(movieFile, size.width,
                            size.height, (int) displayRate,
                            new Vector(images));
                    System.setSecurityManager(backup);
                }
            } catch (NumberFormatException nfe) {
                LogUtil.userErrorMessage("Bad number format");
                return;
            } catch (IOException ioe) {
                LogUtil.userErrorMessage("Error writing movie:" + ioe);
                return;
            }

        }


    }



    /**
     * create the kmz
     *
     * @param movieFile file name
     * @param images list of images
     * @param times List of times
     * @param locs List of bounds
     * @param size image size
     * @param displayRate rate
     * @param scriptingNode isl node
     */
    private void createKmz(String movieFile, List images, List times,
                           List locs, Dimension size, double displayRate,
                           Element scriptingNode) {

        try {
            ZipOutputStream zos = null;
            if (movieFile.toLowerCase().endsWith(".kmz")) {
                zos = new ZipOutputStream(new FileOutputStream(movieFile));
            }

            StringBuffer sb =
                new StringBuffer(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            sb.append("<kml xmlns=\"http://earth.google.com/kml/2.0\">\n");

            String open       = "1";
            String visibility = "1";
            if (scriptingNode != null) {
                visibility = imageGenerator.applyMacros(scriptingNode,
                        ATTR_KML_VISIBILITY, visibility);

                open = imageGenerator.applyMacros(scriptingNode,
                        ATTR_KML_OPEN, open);
            }

            sb.append("<Folder>\n");
            sb.append("<open>" + open + "</open>\n");
            sb.append(XmlUtil.tag(TAG_VISIBILITY, "", visibility));
            if (scriptingNode != null) {
                String folderName = imageGenerator.applyMacros(scriptingNode,
                                        ATTR_KML_NAME, (String) null);
                if (folderName != null) {
                    sb.append("<name>" + folderName + "</name>\n");
                }

                String desc = imageGenerator.applyMacros(scriptingNode,
                                  ATTR_KML_DESC, (String) null);
                if (desc == null) {
                    desc = imageGenerator.applyMacros(
                        XmlUtil.getChildText(scriptingNode));
                    if (desc != null) {
                        desc = desc.trim();
                    }
                }

                if ((desc != null) && (desc.length() > 0)) {
                    sb.append(XmlUtil.tag(TAG_DESCRIPTION, "", desc));
                }
            }


            if (scriptingNode != null) {
                Element kmlElement = XmlUtil.findChild(scriptingNode,
                                         ImageGenerator.TAG_KML);
                if (kmlElement != null) {
                    sb.append(
                        imageGenerator.applyMacros(
                            XmlUtil.getChildText(kmlElement)));
                }
                List nodes = XmlUtil.findChildren(scriptingNode,
                                 ImageGenerator.TAG_KMZFILE);
                for (int i = 0; i < nodes.size(); i++) {
                    Element child = (Element) nodes.get(i);
                    String  file  = XmlUtil.getAttribute(child,
                                        ATTR_FILENAME);
                    if (zos != null) {
                        zos.putNextEntry(
                            new ZipEntry(IOUtil.getFileTail(file)));
                        byte[] bytes =
                            IOUtil.readBytes(IOUtil.getInputStream(file));
                        zos.write(bytes, 0, bytes.length);
                    }
                }
            }
            //    <name>Buienradar.nl</name>
            TimeZone tz = TimeZone.getTimeZone("GMT");


            for (int i = 0; i < images.size(); i++) {
                String image = (String) images.get(i);
                String tail  = IOUtil.getFileTail(image);
                //      System.err.println("tail:" + tail);
                if (zos != null) {
                    zos.putNextEntry(new ZipEntry(tail));
                    byte[] imageBytes =
                        IOUtil.readBytes(new FileInputStream(image));
                    zos.write(imageBytes, 0, imageBytes.length);
                }
                DateTime        dttm   = (DateTime) ((times == null)
                        ? null
                        : times.get(i));
                GeoLocationInfo bounds = (GeoLocationInfo) ((locs == null)
                        ? null
                        : locs.get(i));

                sb.append("<GroundOverlay>\n");
                sb.append("<name>" + ((dttm == null)
                                      ? image
                                      : dttm.toString()) + "</name>\n");
                sb.append(XmlUtil.tag(TAG_VISIBILITY, "", visibility));
                sb.append("<Icon><href>" + tail + "</href></Icon>\n");
                if (bounds != null) {
                    KmlDataSource.createLatLonBox(bounds, sb);
                }
                if (dttm != null) {
                    String when = dttm.formattedString("yyyy-MM-dd", tz)
                                  + "T"
                                  + dttm.formattedString("HH:mm:ss", tz)
                                  + "Z";
                    sb.append("<TimeStamp><when>" + when
                              + "</when></TimeStamp>\n");
                }
                sb.append("</GroundOverlay>\n");
            }
            sb.append("</Folder></kml>\n");

            if (zos != null) {
                zos.putNextEntry(
                    new ZipEntry(
                        IOUtil.stripExtension(IOUtil.getFileTail(movieFile))
                        + ".kml"));

                byte[] kmlBytes = sb.toString().getBytes();
                //                System.out.println("sb:" + sb);
                zos.write(kmlBytes, 0, kmlBytes.length);
                zos.close();
            } else {
                IOUtil.writeFile(movieFile, sb.toString());
            }
        } catch (Exception exc) {
            LogUtil.logException("Saving kmz file", exc);
        }

    }


    /**
     * create the anis html
     *
     * @param movieFile file name
     * @param images list of images
     * @param times List of times
     * @param size image size
     * @param displayRate rate
     * @param scriptingNode isl node
     */
    private void createAnisHtml(String movieFile, List images, List times,
                                Dimension size, double displayRate,
                                Element scriptingNode) {

        try {
            boolean copyFiles = false;
            String  dir       = IOUtil.getFileRoot(movieFile);
            String  preText   = "";
            String  postText  = "";
            String  width     = "" + ((size != null)
                                      ? "" + (size.width + 200)
                                      : "600");
            String  height    = "" + ((size != null)
                                      ? "" + (size.height + 200)
                                      : "600");
            if (scriptingNode == null) {
                if (preFld == null) {
                    preFld    = new JTextArea(5, 20);
                    postFld   = new JTextArea(5, 20);
                    copyCbx   = new JCheckBox("Copy image files to: " + dir);
                    widthFld  = new JTextField("600", 5);
                    heightFld = new JTextField("600", 5);
                }
                widthFld.setText(width);
                heightFld.setText(height);
                copyCbx.setText("Copy image files to: " + dir);
                GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
                JPanel contents = GuiUtils.doLayout(new Component[] {
                    GuiUtils.rLabel("Dimension:"),
                    GuiUtils.left(GuiUtils.hbox(widthFld, new JLabel(" X "),
                        heightFld)),
                    GuiUtils.top(GuiUtils.rLabel("Top HTML:")),
                    GuiUtils.makeScrollPane(preFld, 200, 100),
                    GuiUtils.top(GuiUtils.rLabel("Bottom HTML:")),
                    GuiUtils.makeScrollPane(postFld, 200, 100),
                    GuiUtils.rLabel(""), copyCbx
                }, 2, GuiUtils.WT_NY, GuiUtils.WT_N);
                if ( !GuiUtils.showOkCancelDialog(null,
                        "ANIS Applet Information", contents, null)) {
                    return;
                }
                copyFiles = copyCbx.isSelected();
                preText   = preFld.getText();
                postText  = postFld.getText();
                width     = widthFld.getText().trim();
                height    = heightFld.getText().trim();
            } else {
                width = XmlUtil.getAttribute(scriptingNode, ATTR_ANIS_WIDTH,
                                             width);
                height = XmlUtil.getAttribute(scriptingNode,
                        ATTR_ANIS_HEIGHT, height);
                preText = XmlUtil.getAttribute(scriptingNode,
                        ATTR_ANIS_PREHTML, preText);
                postText = XmlUtil.getAttribute(scriptingNode,
                        ATTR_ANIS_POSTHTML, postText);
            }

            StringBuffer sb    = new StringBuffer();
            String       files = "";

            for (int i = 0; i < images.size(); i++) {
                String file = images.get(i).toString();
                if (copyFiles) {
                    IOUtil.copyFile(new File(file), new File(dir));
                }
                if (i > 0) {
                    files = files + ",";
                }
                files = files + IOUtil.getFileTail(file);
            }
            sb.append(preText);
            sb.append("\n");
            sb.append("<APPLET code=\"AniS.class\" width=" + width
                      + " height=" + height + ">\n");
            sb.append(
                "<PARAM name=\"controls\" value=\"startstop,audio, step, speed, refresh, toggle, zoom\">\n");
            sb.append("<PARAM name=\"filenames\" value=\"" + files + "\">\n");
            sb.append("</APPLET>\n");
            sb.append("\n");
            sb.append(postText);
            IOUtil.writeFile(new File(movieFile), sb.toString());
        } catch (Exception exc) {
            LogUtil.logException("Saving html file", exc);
        }
    }




    /**
     * main
     *
     * @param args args
     */
    public static void main(String[] args) {
        AnimatedGifEncoder e = new AnimatedGifEncoder();
        e.start("test.gif");
        for (int i = 0; i < args.length; i++) {
            ImagePlus image = new ImagePlus(args[i]);
            e.addFrame(image);
        }
        e.finish();
    }



}

