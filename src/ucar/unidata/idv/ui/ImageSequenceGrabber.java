/*
 * Copyright 1997-2025 Unidata Program Center/University Corporation for
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


import ij.ImagePlus;

import org.jcodec.api.SequenceEncoder;
import org.jcodec.codecs.mjpeg.JpegDecoder;
import org.jcodec.codecs.png.PNGDecoder;
import org.jcodec.common.Preconditions;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.Transform;
import org.w3c.dom.Element;




import ucar.unidata.data.GeoLocationInfo;
import ucar.unidata.data.gis.KmlDataSource;
import ucar.unidata.idv.IdvObjectStore;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.flythrough.Flythrough;
import ucar.unidata.ui.AnimatedGifEncoder;
import ucar.unidata.ui.ImagePanel;
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

import visad.DateTime;
import visad.Real;

import java.awt.Graphics;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import java.nio.ByteBuffer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.text.DecimalFormat;

import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;




/**
 * Class ImageSequenceGrabber. Manages the movie capture dialog,
 * capturing a series of jpegs from a ViewManager, writing them out
 * to disk, previewing them and generating quicktime movies.
 *
 *
 * @author IDV Development Team
 */
public class ImageSequenceGrabber implements Runnable, ActionListener {

    /** xml tag or attr name */
    public static final String ATTR_ANIS_HEIGHT = "anis_height";

    /** xml tag or attr name */
    public static final String ATTR_ANIS_POSTHTML = "anis_posthtml";

    /** xml tag or attr name */
    public static final String ATTR_ANIS_PREHTML = "anis_prehtml";

    /** xml tag or attr name */
    public static final String ATTR_ANIS_TYPE = "anis_type";

    /** xml tag or attr name */
    public static final String ATTR_ANIS_WIDTH = "anis_width";

    /** igml xml attributes */
    public static final String ATTR_APPENDTIME = "appendtime";

    /** igml xml attributes */
    public static final String ATTR_FILENAME = "filename";

    /** igml xml attributes */
    public static final String ATTR_IMAGEDIR = "imagedir";

    /** igml xml attributes */
    public static final String ATTR_IMAGEPREFIX = "imageprefix";

    /** igml xml attributes */
    public static final String ATTR_IMAGESUFFIX = "imagesuffix";

    /** xml tag or attr name */
    public static final String ATTR_IMAGETEMPLATE = "imagetemplate";

    /** xml tag or attr name */
    public static final String ATTR_KML_DESC = "kml_desc";

    /** xml tag or attr name */
    public static final String ATTR_KML_NAME = "kml_name";

    /** xml tag or attr name */
    public static final String ATTR_KML_OPEN = "kml_open";

    /** xml tag or attr name */
    public static final String ATTR_KML_VISIBILITY = "kml_visibility";

    /** igml xml attributes */
    public static final String ATTR_STEPS = "steps";

    /** the viewpoint file */
    public static final String ATTR_VIEWPOINTFILE = "viewpointfile";

    /** Action commands for gui buttons */
    public static final String CMD_CLEAR = "clear";

    /** Action commands for gui buttons */
    public static final String CMD_GRAB = "grab";

    /** Action commands for gui buttons */
    public static final String CMD_GRAB_ANIMATION = "grab.animation";

    /** Action commands for gui buttons */
    public static final String CMD_PREVIEW_CLOSE = "preview.close";

    /** Action commands for gui buttons */
    public static final String CMD_PREVIEW_DELETE = "preview.delete";

    /** Action commands for gui buttons */
    public static final String CMD_PREVIEW_NEXT = "preview.next";

    /** Action commands for gui buttons */
    public static final String CMD_PREVIEW_PLAY = "preview.play";

    /** Action commands for gui buttons */
    public static final String CMD_PREVIEW_PREV = "preview.prev";

    /** Action commands for gui buttons */
    public static final String CMD_PREVIEW_SHOW = "preview.show";

    /** Action commands for gui buttons */
    public static final String CMD_PUBLISH = "publish";

    /** Property for the image file template */
    public static final String PROP_IMAGEALTDIR =
        "imagesequencegrabber.altdir";

    /** Property for the image file template */
    public static final String PROP_IMAGETEMPLATE =
        "imagesequencegrabber.template";

    /** {@literal "Global"} GIF palette preference ID. */
    public static final String PREF_USE_GLOBAL_PALETTE =
        "idv.capture.gif.useGlobalTable";

    /** Tooltip text for global palette checkbox. */
    public static final String GLOBAL_PALETTE_TOOLTIP =
        "<html>Turn off to correct colors for varying color animations.<br/>" +
        "<br/></br/>This option controls whether or not a resulting GIF " +
        "image will use a color palette<br/>taken from all the frames of the" +
        " animation.<br/><br/>If the option is turned off, the animation's " +
        "frames will use their own color palettes.<br/>This can result in " +
        "the same color value looking different between different frames" +
        "<br/>in the resulting animated GIF.</html>";

    /** How much we pause between animation captures */
    private static int SLEEP_TIME = 500;

    /** Filter for HTML files */
    public static final PatternFileFilter FILTER_ANIS =
        new PatternFileFilter(".+\\.html|.+\\.htm",
                              "AniS or FlAniS HTML File (*.html)", ".html");

    /** xml tag or attr name */
    public static final String TAG_DESCRIPTION = "description";

    /** xml tag or attr name */
    public static final String TAG_VISIBILITY = "visibility";

    /** widget for saving html */
    private static JCheckBox copyCbx;

    /** default */
    private static String dfltAltDir;

    /** The default image file template */
    private static String dfltTemplate;

    /** widget for saving html */
    private static JTextField heightFld;

    /** widget for saving html */
    private static JTextArea postFld;

    /** widget for saving html */
    private static JTextArea preFld;

    /** widget for switching between AniS and FlAniS */
    private static JCheckBox typeCbx;

    /** widget for saving html */
    private static JTextField widthFld;

    /** Used for synchronization_ */
    private Object MUTEX = new Object();

    /**
     *  A virtual timestamp to keep us form having two animation captures running at the same time.
     */
    private int captureTimeStamp = 0;

    /**
     *  A flag that tells us when we are automatically capturing the animation timesteps
     */
    private boolean capturingAnim = false;

    /**
     *  A flag that tells us when we are doing the automatic capture.
     */
    private boolean capturingAuto = false;

    /** annoying beep checkbox */
    private JCheckBox beepCbx = new JCheckBox("Beep", false);

    /** How many images have been captured */
    int imageCnt = 0;

    /** Index in the list of images we are currently showing as a preview */
    int previewIndex = 0;

    /** image quality */
    private float quality = 1.0f;

    /** Has the user paused the previews */
    boolean paused = false;

    /** Flag to see when we are playing a preview */
    private boolean isPlaying = false;

    /** List of earth locations corresponding to each image */
    List<ImageWrapper> images = new ArrayList<ImageWrapper>();

    /** Used for managing the current preview thread */
    private int timestamp = 0;

    /** Have we told the user about constraints of writing transparent images */
    boolean notifiedForTransparent = false;

    /** flag for just capturing the images */
    private boolean justCaptureAnimation = false;

    /** capture flythrough */
    private JCheckBox grabFlythroughCbx = new JCheckBox("Capture Flythrough",
                                              false);

    /** widget */
    JCheckBox animationResetCbx = new JCheckBox("Reset to start time", true);

    /** Components to enable/disable */
    private List alternateComps = new ArrayList();

    /** write positions */
    private boolean writePositions = false;

    /** Capture all views */
    JRadioButton allViewsBtn;

    /** If non null then we capture from this */
    private JComponent alternateComponent;

    /** Should use alternate dir */
    private JCheckBox alternateDirCbx;

    /** Holds the directory */
    private JTextField alternateDirFld;

    /** Is the background of the image transparent_ */
    JCheckBox backgroundTransparentBtn;

    /** Specifies the capture rate */
    JTextField captureRateFld;

    /** Clear all captured frames */
    JButton clearButton;

    /** Close the window */
    JButton closeButton;

    /** Capture the contents */
    JRadioButton contentsBtn;

    /** Write out the movie */
    JButton createButton;

    /** Button  to delete a frame */
    JButton deleteFrameButton;

    /** The directory we write to */
    String directory;

    /** Specifies the display rate in the generated movie */
    JTextField displayRateFld;

    /** Specifies the display rate in the generated movie */
    JTextField endPauseFld;

    /** Holds the file prefix */
    private JTextField fileTemplateFld;

    /** Shows how many  frames have been captured */
    JLabel frameLbl;

    /** fullscreen mode */
    JRadioButton fullScreenBtn;

    /** Capture full window */
    JRadioButton fullWindowBtn;

    /** Turns on animation based  capture */
    JButton grabAnimationBtn;

    /** Turns on automatic capture */
    JButton grabAutoBtn;

    /** Captures one frame */
    JButton grabBtn;

    /** radio button for high image quality */
    private JRadioButton hiBtn;

    /** The IDV */
    private IntegratedDataViewer idv;

    /** The igml */
    private ImageGenerator imageGenerator;

    /** imagesize */
    Dimension imageSize;

    /** File path of the last previewed image */
    String lastPreview;

    /** radio button for low image quality */
    private JRadioButton lowBtn;

    /** The window for the main gui */
    JDialog mainDialog;

    /** Capture the main display */
    JRadioButton mainDisplayBtn;

    /** radio button for medium image quality */
    private JRadioButton medBtn;

    /** If non-null then we use this and don't ask the user. */
    private String movieFileName;

    /** Button  to step forward one frame */
    JButton nextButton;

    /** overwrite */
    private JCheckBox overwriteCbx;

    /** Button  to play preview */
    JButton playButton;

    /** Image icon for playing */
    private ImageIcon playIcon;

    /** Button  to step back one frame */
    JButton prevButton;

    /** Button to show the preview window */
    JButton previewButton;

    /** The window for the previews */
    JDialog previewDialog;

    /** The label for showing previews */
    JLabel previewLbl;

    /** Where we show the preview */

    // JLabel previewImage;
    ImagePanel previewPanel;

    /** preview rate field */
    JTextField previewRateFld;

    /** publish checkbox */
    private JComboBox publishCbx;

    /** The igml movie node. May be null */
    private Element scriptingNode;

    /** Image icon for stopping */
    private ImageIcon stopIcon;

    /**
     *  The {@ref ViewManager} we are capturing.
     */
    private ViewManager viewManager;

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
        this(viewManager, alternateComponent, false);
    }

    /**
     * Create a new ImageSequenceGrabber
     *
     * @param viewManager   associated ViewManager
     * @param alternateComponent   alternate component
     * @param justCaptureAnimation true to just capture the animation
     */
    public ImageSequenceGrabber(ViewManager viewManager,
                                JComponent alternateComponent,
                                boolean justCaptureAnimation) {
        this.alternateComponent   = alternateComponent;
        this.viewManager          = viewManager;
        this.idv                  = viewManager.getIdv();
        this.justCaptureAnimation = justCaptureAnimation;
        init();

        if (this.justCaptureAnimation) {
            startAnimationCapture();
        }
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
                                Element scriptingNode,
                                List<ImageWrapper> imageFiles,
                                Dimension size, double displayRate) {
        this(filename, idv, imageGenerator, scriptingNode, imageFiles, size,
             displayRate, -1);
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
     * @param endPause  end pause (seconds)
     */
    public ImageSequenceGrabber(String filename, IntegratedDataViewer idv,
                                ImageGenerator imageGenerator,
                                Element scriptingNode,
                                List<ImageWrapper> imageFiles,
                                Dimension size, double displayRate,
                                double endPause) {
        this.idv            = idv;
        this.imageGenerator = imageGenerator;
        this.scriptingNode  = scriptingNode;
        this.images         = ImageWrapper.makeImageWrappers(imageFiles);
        this.idv            = idv;
        movieFileName       = filename;
        createMovie(movieFileName, images, size, displayRate, scriptingNode,
                    endPause);
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
    private JComponent addAltComp(JComponent comp) {
        alternateComps.add(comp);
        GuiUtils.enableTree(comp, false);

        return comp;
    }

    /**
     * Get the animation widget
     *
     * @return the animation widget
     */
    private AnimationWidget getAnimationWidget() {
        if ((grabFlythroughCbx != null) && grabFlythroughCbx.isSelected()
                && (viewManager instanceof MapViewManager)) {
            Flythrough flythrough =
                ((MapViewManager) viewManager).getFlythrough();

            if (flythrough != null) {
                return flythrough.getAnimationWidget();
            }
        }

        AnimationWidget animationWidget = viewManager.getAnimationWidget();

        if (animationWidget == null) {
            animationWidget = viewManager.getExternalAnimationWidget();
        }

        return animationWidget;
    }

    /**
     * Get the Animation
     *
     * @return the Animation
     */
    private Animation getAnimation() {
        if ((grabFlythroughCbx != null) && grabFlythroughCbx.isSelected()
                && (viewManager instanceof MapViewManager)) {
            Flythrough flythrough =
                ((MapViewManager) viewManager).getFlythrough();

            if (flythrough != null) {
                return flythrough.getAnimation();
            }
        }

        Animation anime = viewManager.getAnimation();

        if (anime == null) {
            anime = viewManager.getExternalAnimation();
        }

        return anime;
    }

    /**
     * Initialize me. Create the windows, etc.
     */
    private void init() {

        // Store the images in a unique (by current time) subdir of the user's tmp  dir
        directory =
            IOUtil.joinDir(viewManager.getStore().getUserTmpDirectory(),
                           "images_" + System.currentTimeMillis());

        // Make sure the dir exists
        IOUtil.makeDir(directory);
        mainDialog     = GuiUtils.createDialog("Movie Capture", false);
        frameLbl       = GuiUtils.cLabel("No frames");
        displayRateFld = new JTextField("2", 3);
        endPauseFld    = new JTextField("2", 3);
        endPauseFld.setToolTipText(
            "Number of seconds to pause on last frame of animated GIF");
        captureRateFld = new JTextField("2", 3);

        String imgp = "/auxdata/ui/icons/";

        mainDisplayBtn = new JRadioButton("Current View", true);
        allViewsBtn    = new JRadioButton("All Views", false);
        contentsBtn    = new JRadioButton("Current View & Legend", false);
        fullWindowBtn  = new JRadioButton("Full Window", false);
        fullScreenBtn  = new JRadioButton("Full Screen", false);

        ButtonGroup bg = GuiUtils.buttonGroup(mainDisplayBtn, fullWindowBtn);

        bg.add(allViewsBtn);
        bg.add(contentsBtn);
        bg.add(fullScreenBtn);
        beepCbx.setToolTipText("Beep when an image is captured");

        List btns = Misc.newList(new Object[] {
            new JLabel("What to capture:"), mainDisplayBtn, allViewsBtn,
            contentsBtn, fullWindowBtn, fullScreenBtn
        });

        btns.add(beepCbx);

        JComponent whatPanel = GuiUtils.vbox(btns);

        if (dfltAltDir == null) {
            dfltAltDir = idv.getStore().get(PROP_IMAGEALTDIR, "");
        }

        alternateDirFld = new JTextField(dfltAltDir, 30);

        JButton alternateDirBtn = new JButton("Select");

        GuiUtils.setupDirectoryChooser(alternateDirBtn, alternateDirFld);

        if (dfltTemplate == null) {
            dfltTemplate = idv.getStore().get(PROP_IMAGETEMPLATE,
                    "image_%count%_%time%");
        }

        fileTemplateFld = new JTextField(dfltTemplate, 30);
        fileTemplateFld.setToolTipText(
            "<html>Enter the file name template to use.<br>"
            + "<b>%count%</b> is the image counter<br>"
            + "<b>%count:decimal format%</b> allows you to format the count. Google 'java decimalformat' for more information.<br>"
            + "<b>%time%</b> is the  animation time in the default format<br>"
            + "<b>%time:some time format string%</b> a macro that begins with &quot;time:&quot;,contains a time format string using the:<br>"
            + "java SimpleDateFormat formatting (see google)." + "</html>");
        overwriteCbx    = new JCheckBox("Overwrite", false);
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
        grabBtn           = makeButton("One Image", CMD_GRAB);
        grabAutoBtn       = makeButton("Automatically", GuiUtils.CMD_START);
        grabAnimationBtn  = makeButton("Time Animation", CMD_GRAB_ANIMATION);
        grabFlythroughCbx = new JCheckBox("Flythrough", false);

        boolean hasFlythrough = false;

        if ((viewManager instanceof MapViewManager)
                && ((MapViewManager) viewManager).getFlythrough() != null) {
            hasFlythrough =
                ((MapViewManager) viewManager).getFlythrough().hasPoints();
        }

        List frameButtons = new ArrayList();

        frameButtons.add(previewButton = makeButton("Preview",
                CMD_PREVIEW_SHOW));
        frameButtons.add(clearButton = makeButton("Delete All", CMD_CLEAR));
        frameButtons.add(createButton = makeButton("Save Movie",
                GuiUtils.CMD_OK));

        JComponent publishButton;

        if (idv.getPublishManager().isPublishingEnabled()) {

            // frameButtons.add(publishButton = makeButton("Publish Movie",
            // CMD_PUBLISH));
        } else {

            // publishButton = new JPanel();
        }

        closeButton = makeButton("Close", GuiUtils.CMD_CLOSE);

        JLabel titleLbl =
            GuiUtils.cLabel(
                "Note: Make sure the view window is not obscured");
        JPanel titlePanel = GuiUtils.inset(titleLbl, 8);
        JPanel runPanel   =
            GuiUtils.hflow(Misc.newList(GuiUtils.rLabel(" Rate: "),
                                        captureRateFld,
                                        new JLabel(" seconds")));
        int maxBtnWidth =
            Math.max(grabAnimationBtn.getPreferredSize().width,
                     Math.max(grabBtn.getPreferredSize().width,
                              grabAutoBtn.getPreferredSize().width));

        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);

        JPanel capturePanel = GuiUtils.doLayout(new Component[] {
            grabBtn, GuiUtils.filler(), GuiUtils.top(grabAnimationBtn),
            GuiUtils.vbox(animationResetCbx, (hasFlythrough
                    ? (JComponent) grabFlythroughCbx
                    : GuiUtils.filler())), grabAutoBtn, runPanel,
            GuiUtils.filler(maxBtnWidth + 10, 1), GuiUtils.filler(),
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
        capturePanel = GuiUtils.hbox(
            GuiUtils.top(capturePanel),
            GuiUtils.top(
                GuiUtils.inset(
                    GuiUtils.vbox(whatPanel, backgroundTransparentBtn),
                    new Insets(0, 10, 0, 0))));
        capturePanel = GuiUtils.inset(GuiUtils.left(capturePanel), 5);
        capturePanel.setBorder(BorderFactory.createTitledBorder("Capture"));
        GuiUtils.setHFill();

        JPanel filePanel = GuiUtils.doLayout(null, new Component[] {
            GuiUtils.rLabel("Image Quality:"),
            GuiUtils.left(GuiUtils.hbox(hiBtn, medBtn, lowBtn)),
            GuiUtils.filler(), GuiUtils.right(alternateDirCbx),
            // GuiUtils.filler(),
            // GuiUtils.filler(),
            // addAltComp(GuiUtils.rLabel("Directory:")),
            addAltComp(alternateDirFld), addAltComp(alternateDirBtn),
            addAltComp(GuiUtils.rLabel("Filename Template:")),
            addAltComp(fileTemplateFld), addAltComp(overwriteCbx)
        }, 3, GuiUtils.WT_NYN, GuiUtils.WT_N, null, null,
           new Insets(0, 5, 0, 0));

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

        // Only show the window if init filename is null
        if ( !justCaptureAnimation && (movieFileName == null)
                && (scriptingNode == null)) {
            GuiUtils.packDialog(mainDialog,
                                GuiUtils.centerBottom(contents,
                                    GuiUtils.wrap(GuiUtils.inset(closeButton,
                                        4))));
            mainDialog.setVisible(true);
        }
    }

    /**
     * Return the value of the {@literal "idv.capture.gif.useGlobalTable"}
     * preference.
     *
     * <p>Note: if the preference is not set, the value will default to
     * {@code true}.</p>
     *
     * @return Value of {@code idv.capture.gif.useGlobalTable}.
     */
    private boolean getGlobalPaletteValue() {
        IdvObjectStore store = idv.getStore();
        return store.get(PREF_USE_GLOBAL_PALETTE, true);
    }

    /**
     * Load in the current preview image into the gui
     */
    private void setPreviewImage() {

        // if (previewImage == null) {
        if (previewPanel == null) {
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
                String current = images.get(previewIndex).getPath();

                if ( !Misc.equals(current, lastPreview)) {
                    previewPanel.loadFile(current);

                    /*
                     *                     Image image =
                     *   Toolkit.getDefaultToolkit().createImage(current);
                     *   previewPanel.setImage(image);
                     */

                    /*
                     * ImageIcon icon = new ImageIcon(image);
                     * previewImage.setIcon(icon);
                     * previewImage.setText(null);
                     */
                    lastPreview = current;
                }

                previewLbl.setText("  Frame: " + (previewIndex + 1) + "/"
                                   + images.size());
            } else {
                previewLbl.setText("   No images   ");
                previewPanel.setImage(null);

                /*
                 * previewLbl.setText("  Frame:        ");
                 * previewImage.setText("   No images   ");
                 * previewImage.setIcon(null);
                 */
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

            playIcon       = GuiUtils.getImageIcon(imgp + "Play16.gif");
            stopIcon       = GuiUtils.getImageIcon(imgp + "Stop16.gif");
            previewRateFld = new JTextField("1", 3);

            ChangeListener rateListener = new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    JSlider slide = (JSlider) e.getSource();

                    if (slide.getValueIsAdjusting()) {

                        // return;
                    }

                    double value = slide.getValue() / 4.0;

                    previewRateFld.setText("" + value);
                }
            };
            JComponent[] comps = GuiUtils.makeSliderPopup(1, 20, 4,
                                     rateListener);
            JComponent sliderBtn = comps[0];

            playButton = makeButton(playIcon, CMD_PREVIEW_PLAY, "Play/Stop");
            nextButton = makeButton(GuiUtils.getImageIcon(imgp
                    + "StepForward16.gif"), CMD_PREVIEW_NEXT,
                                            "Go to next frame");
            prevButton = makeButton(GuiUtils.getImageIcon(imgp
                    + "StepBack16.gif"), CMD_PREVIEW_PREV,
                                         "Go to previous frame");
            deleteFrameButton = makeButton("Delete this frame",
                                           CMD_PREVIEW_DELETE);

            List buttonList = Misc.newList(prevButton, playButton,
                                           nextButton);

            buttonList.add(GuiUtils.filler(20, 5));
            buttonList.add(new JLabel(" Delay: "));
            buttonList.add(previewRateFld);
            buttonList.add(new JLabel("(s)  "));
            buttonList.add(sliderBtn);

            JPanel buttons = GuiUtils.hflow(buttonList);

            buttons    = GuiUtils.inset(buttons, 5);
            previewLbl = new JLabel("  ");

            // previewImage = new JLabel();
            previewPanel = new ImagePanel();
            previewPanel.setPreferredSize(new Dimension(640, 480));
            lastPreview  = null;
            previewIndex = 0;
            setPreviewImage();

            // previewImage.setBorder(BorderFactory.createEtchedBorder());
            previewPanel.setBorder(BorderFactory.createEtchedBorder());

            JComponent topComp = GuiUtils.leftRight(buttons,
                                     GuiUtils.hbox(deleteFrameButton,
                                         previewLbl));
            JPanel contents = GuiUtils.topCenterBottom(topComp, previewPanel,
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

        // if (capturingAuto || capturingAnim) {
        // grabAutoBtn.setIcon (stopIcon);
        // }
        // else {
        // grabAutoBtn.setIcon (startIcon);
        // }
        grabAnimationBtn.setEnabled( !capturingAuto);
        grabBtn.setEnabled( !capturingAuto && !capturingAnim);
    }

    /**
     * Turn on automatic  capture
     */
    private void startCapturingAuto() {
        grabAutoBtn.setText("Stop");

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
        grabAutoBtn.setText("Automatically");

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
        if (capturingAnim || (getAnimation() == null)) {
            return;
        }

        grabAnimationBtn.setText("Stop animation");
        capturingAnim = true;
        checkEnabled();

        // Run the imageGrab in another thread because visad errors when the getImage
        // is called from the awt event thread
        // runAnimationCapture(++captureTimeStamp);
        Misc.run(new Runnable() {
            public void run() {
                Misc.sleep(2000);
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
            getAnimation().setAnimating(false);

            if ((animationResetCbx != null)
                    && animationResetCbx.isSelected()) {
                getAnimationWidget().gotoBeginning();
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
                    if (steps[i] > getAnimation().getNumSteps()) {
                        break;
                    }

                    getAnimation().setCurrent(steps[i]);

                    // Sleep for a bit  to allow for the display to redraw itself
                    try {
                        Misc.sleep(sleepTime);
                    } catch (Exception exc) {}

                    // Has the user pressed Stop?
                    if ( !keepRunning(timestamp)) {
                        break;
                    }

                    // Now grab the image in block mode
                    grabImageAndBlock();
                }
            } else if ((scriptingNode != null) && (viewManager != null)
                       && XmlUtil.hasAttribute(scriptingNode,
                           ATTR_VIEWPOINTFILE)) {
                String viewpointFile =
                    imageGenerator.applyMacros(scriptingNode,
                        ATTR_VIEWPOINTFILE);
                List viewpoints = (List) idv.decodeObject(
                                      IOUtil.readContents(
                                          viewpointFile, getClass()));

                for (int i = 0; i < viewpoints.size(); i++) {
                    double[] matrix = (double[]) viewpoints.get(i);

                    viewManager.setDisplayMatrix(matrix);

                    // Sleep for a bit  to allow for the display to redraw itself
                    try {
                        Misc.sleep(sleepTime);
                    } catch (Exception exc) {}

                    // Has the user pressed Stop?
                    if ( !keepRunning(timestamp)) {
                        break;
                    }

                    // Now grab the image in block mode
                    grabImageAndBlock();
                }
            } else {
                ///if(animationResetCbx.isSelected())
                // getAnimationWidget().gotoBeginning();

                int start = getAnimation().getCurrent();

                while (true) {

                    // Sleep for a bit  to allow for the display to redraw itself
                    try {
                        Misc.sleep(sleepTime);
                    } catch (Exception exc) {}

                    // Has the user pressed Stop?
                    if ((getAnimation() == null) || !keepRunning(timestamp)) {
                        break;
                    }

                    // Now grab the image in block mode
                    grabImageAndBlock();

                    if (getAnimation() == null) {
                        break;
                    }

                    getAnimationWidget().stepForward();

                    int current = getAnimation().getCurrent();

                    if (current <= start) {
                        break;
                    }
                }
            }

            if ( !keepRunning(timestamp)) {
                return;
            }

            stopAnimationCapture(true);
        } catch (Exception exc) {
            LogUtil.logException("Creating movie", exc);
        }

    }

    /**
     * Turn off animation based  capture
     *
     * @param andWrite write movie
     */
    private void stopAnimationCapture(boolean andWrite) {
        if (viewManager != null) {
            viewManager.useImages(ImageWrapper.makeFileList(images),
                                  justCaptureAnimation);

            if (justCaptureAnimation) {
                return;
            }
        }

        capturingAnim = false;

        if (andWrite) {
            writeMovie();
        }

        // This implies we write the animation and then are done
        if (imageGenerator != null) {

            // close();
            imageGenerator.doneCapturingMovie();

            // return;
        }

        grabAnimationBtn.setText("Time Animation");
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
                double sleepTime = 1;

                try {
                    sleepTime = Double.parseDouble(
                        previewRateFld.getText().trim());
                } catch (Exception noop) {}

                Misc.sleep((long) (sleepTime * 1000));
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
                String filename = images.get(previewIndex).getPath();

                images.remove(previewIndex);
                previewIndex--;
                imagesChanged();
                new File(filename).delete();
            }
        } else if (cmd.equals(CMD_GRAB_ANIMATION)) {
            synchronized (MUTEX) {
                if (capturingAnim) {
                    stopAnimationCapture(false);
                } else {
                    startAnimationCapture();
                }
            }
        } else if (cmd.equals(CMD_GRAB)) {

            // Run the imageGrab in another thread because visad errors when the getImage
            // is called from the awt event thread
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

    public static Picture convertColorSpace(Picture pic, ColorSpace tgtColor) {
        Transform tr = ColorUtil.getTransform(pic.getColor(), tgtColor);
        Picture res = Picture.create(pic.getWidth(), pic.getHeight(), tgtColor);
        tr.transform(pic, res);
        return res;
    }

    /**
     * Used by the pubishing facility to publish movides to some external site
     */
    private void publishMovie() {
        stopCapturingAuto();

        String uid  = Misc.getUniqueId();
        String tail = uid + FileManager.SUFFIX_MOV;
        String file = idv.getStore().getTmpFile(tail);

        createMovie(file);
        idv.getPublishManager().doPublish("Publish Quicktime file", file);
    }

    /**
     * Close the window
     */
    private void close() {
        captureTimeStamp++;
        capturingAuto = false;

        if (previewDialog != null) {
            previewDialog.dispose();
            previewDialog = null;
        }

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
            images.get(i).deleteFile();
        }

        images = new ArrayList<ImageWrapper>();

        if (viewManager != null) {

            // TODO:
            viewManager.useImages(ImageWrapper.makeFileList(images),
                                  justCaptureAnimation);
        }
    }

    /**
     * Run in a thread.
     */
    public void run() {
        capturingAuto = true;

        while (capturingAuto) {
            grabImageAndBlock();

            if ( !capturingAuto) {
                return;
            }

            double captureRate = 2.0;

            try {
                captureRate = (Double.parseDouble(
                    captureRateFld.getText().trim()));
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
     * Write out the movie
     */
    private void writeMovie() {
        stopCapturingAuto();

        String filename = null;

        if (isInteractive() && (movieFileName == null)) {
            JCheckBox writePositionsCbx = new JCheckBox("Save viewpoints",
                                              writePositions);

            writePositionsCbx.setToolTipText(
                "Also save the viewpoint matrices as an 'xidv' file");

            JCheckBox otherGlobalPaletteBox =
                new JCheckBox("Use constant color palette for GIF",
                    getGlobalPaletteValue());
            otherGlobalPaletteBox.setToolTipText(GLOBAL_PALETTE_TOOLTIP);
            otherGlobalPaletteBox.addActionListener(e -> {
                boolean value = otherGlobalPaletteBox.isSelected();
                IdvObjectStore store = idv.getStore();
                store.put(PREF_USE_GLOBAL_PALETTE, value);
                store.save();
            });
            List<JComponent> accessoryComps = new ArrayList<>(10);

            accessoryComps.add(
                GuiUtils.leftRight(
                    GuiUtils.rLabel(" Frames per second: "), displayRateFld));
            accessoryComps.add(
                GuiUtils.leftRight(
                    GuiUtils.rLabel(" End Frame Pause: "), endPauseFld));
            accessoryComps.add(writePositionsCbx);
            accessoryComps.add(otherGlobalPaletteBox);

            if (publishCbx == null) {
                publishCbx = idv.getPublishManager().makeSelector();
            }

            if (publishCbx != null) {
                accessoryComps.add(publishCbx);
            }

            JComponent extra =
                GuiUtils.topCenter(GuiUtils.vbox(accessoryComps),
                                   GuiUtils.filler());

            filename =
                FileManager.getWriteFile(Misc.newList(
                        FileManager.FILTER_MP4, FileManager.FILTER_MOV,
                        FileManager.FILTER_AVI, FileManager.FILTER_ANIMATEDGIF,
                        FileManager.FILTER_ZIP, FileManager.FILTER_KMZ,
                        FILTER_ANIS), FileManager.SUFFIX_MP4, extra);
            writePositions = writePositionsCbx.isSelected();
        } else {
            filename = movieFileName;
        }

        if (filename != null) {

            // if ( !filename.toLowerCase().endsWith(".mov")) {
            // filename = filename + ".mov";
            // }
            createMovie(filename);
        }
    }

    /**
     * Get the file prefix to use
     *
     *
     * @param cnt   image count
     * @return File prefix
     */
    private String getFilePrefix(int cnt) {
        String  filename     = "";
        String  template     = "image_%count%_%time%";
        boolean usingDefault = true;

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
                    template     = template + "_%time%";
                    usingDefault = false;
                }
            }
        } else {
            if (alternateDirCbx.isSelected()) {
                template = fileTemplateFld.getText().trim();

                if ( !template.equals(dfltTemplate)) {
                    dfltTemplate = template;
                    idv.getStore().put(PROP_IMAGETEMPLATE, dfltTemplate);
                    idv.getStore().save();
                    usingDefault = false;
                }
            }
        }

        while (true) {
            String formatString = StringUtil.findFormatString("count", "%",
                                      template);

            if (formatString == null) {
                break;
            }

            System.err.println(formatString);

            DecimalFormat format         = new DecimalFormat(formatString);
            String        formattedValue = format.format(cnt);
            String        tmp            = StringUtil.replace(template,
                                            "%count:" + formatString + "%",
                                            formattedValue);

            if (tmp.equals(template)) {
                throw new IllegalStateException("Bad formatting:" + tmp);
            }

            template = tmp;
        }

        template = StringUtil.replace(template, "%count%", "" + cnt);

        try {
            Real r = getAnimation().getAniValue();

            if (r != null) {
                DateTime dttm       = new DateTime(r);
                String   timeString = "" + dttm;

                timeString = StringUtil.replace(timeString, ":", "_");
                timeString = StringUtil.replace(timeString, "-", "_");
                timeString = StringUtil.replace(timeString, " ", "_");
                template = StringUtil.replace(template, "%time%", timeString);
                template   = ucar.visad.UtcDate.applyTimeMacro(template, dttm);
            } else {
                String stub = usingDefault
                              ? "_%time%"
                              : "%time%";

                template = StringUtil.replace(template, stub, "");
            }
        } catch (Exception exc) {}

        template = StringUtil.replace(template, "/", "_");
        template = StringUtil.replace(template, "\\", "_");

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

                if ( !Misc.equals(filename, dfltAltDir)) {
                    dfltAltDir = filename;
                    idv.getStore().put(PROP_IMAGEALTDIR, dfltAltDir);
                    idv.getStore().save();
                }

                if (filename.length() == 0) {
                    filename = directory;
                    alternateDirFld.setText(directory);
                } else {

                    // IOUtil.makeDir(filename);
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

        if ((beepCbx != null) && beepCbx.isSelected()) {
            Toolkit.getDefaultToolkit().beep();
        }

        try {
            Hashtable imageProperties = new Hashtable();

            synchronized (MUTEX) {
                if (viewManager != null) {
                    if (viewManager.useDisplay()) {

                        // Sleep a bit to let the display get updated
                        // TODO???  Misc.sleep(500);
                    }
                }

                // String filename = getFilePrefix(imageCnt++);
                String filename = getFilePrefix(images.size());
                String tmp      = filename.toLowerCase();

                if ( !(tmp.endsWith(".gif") || tmp.endsWith(".png")
                        || tmp.endsWith(".jpg") || tmp.endsWith(".jpeg"))) {
                    filename = filename + getFileSuffix();
                }

                String path = IOUtil.joinDir(getFileDirectory(), filename);

                if (isInteractive() && !overwriteCbx.isSelected()
                        && alternateDirCbx.isSelected()
                        && new File(path).exists()) {
                    if (JOptionPane
                            .showConfirmDialog(null, "File:" + path
                                + " exists. Do you want to overwrite?", "File exists", JOptionPane
                                    .YES_NO_OPTION) == 1) {
                        stopCapturingAuto();
                        stopAnimationCapture(false);

                        return;
                    }

                    overwriteCbx.setSelected(true);
                }

                // System.err.println ("ImageSequenceGrabber file dir: " +getFileDirectory() +" path: " +  path);
                DateTime time = null;

                time = (((getAnimation() != null)
                         && (getAnimation().getAniValue() != null))
                        ? new DateTime(getAnimation().getAniValue())
                        : null);

                GeoLocationInfo bounds = null;

                if (viewManager != null) {
                    bounds = viewManager.getVisibleGeoBounds();
                }

                //System.err.println("image sequence");
                if (alternateComponent != null) {
                    //System.err.println("image sequence - alt");
                    GuiUtils.toFront(GuiUtils.getFrame(alternateComponent));
                    Misc.sleep(50);
                    ImageUtils.writeImageToFile(alternateComponent, path);
                } else {
                    if ( !idv.getArgsManager().getIsOffScreen()) {
                        viewManager.toFront();
                        Misc.sleep(100);
                    }

                    if (imageGenerator != null) {  //I guess we are in scripting mode.
                        BufferedImage image;
                        boolean combine = XmlUtil.getAttribute(scriptingNode,
                                              ImageGenerator.ATTR_COMBINE,
                                              false);
                        if (combine) {
                            List<Image> images = new LinkedList<Image>();

                            for (Object o :
                                    viewManager.getVMManager()
                                        .getViewManagers()) {
                                ViewManager vm = (ViewManager) o;
                                vm.getAnimation().setAniValue(time);
                                images.add(vm.captureIslImage(scriptingNode));
                            }
                            int cols = 2;
                            cols = XmlUtil.getAttribute(scriptingNode,
                                    imageGenerator.ATTR_COLUMNS, cols);
                            image = (BufferedImage) ImageUtils.gridImages2(
                                images, 0, Color.BLACK, cols);

                        } else {
                            image = viewManager.captureIslImage(scriptingNode);
                        }
                        Hashtable props = new Hashtable();

                        props.put(ImageGenerator.PROP_IMAGEPATH, path);
                        props.put(ImageGenerator.PROP_IMAGEFILE,
                                  IOUtil.getFileTail(path));
                        imageGenerator.putIndex(props,
                                ImageGenerator.PROP_IMAGEINDEX,
                                images.size());
                        imageGenerator.processImage(image, path,
                                scriptingNode, props, viewManager,
                                imageProperties);
                        subsetBounds(bounds, imageProperties);
                    } else {
                        List<Component> components =
                            new LinkedList<Component>();

                        if (fullWindowBtn.isSelected()) {  // Full Window
                            components
                                .add(viewManager.getDisplayWindow()
                                    .getComponent());
                        } else if (mainDisplayBtn.isSelected()) {  // View
                            components.add(
                                viewManager.getMaster().getComponent());
                        } else if (fullScreenBtn.isSelected()) {  // Full Screen
                            components.add(
                                viewManager.getMaster().getComponent());
                        } else if (allViewsBtn.isSelected()) {  // All Views
                            for (Object o :
                                    viewManager.getDisplayWindow()
                                        .getViewManagers()) {
                                components.add(
                                    ((MapViewManager) o).getComponent());
                            }
                        } else {  // View & Legend
                            components.add(viewManager.getContents());
                        }

                        Image image = captureImages(
                                          components,
                                          ImageUtils.getColumnCountFromComps(
                                              components));

                        if (allViewsBtn.isSelected()) {

                            // Otherwise, this gets set in the captureImage method.
                            imageSize = new Dimension(image.getWidth(null),
                                    image.getHeight(null));
                        }

                        if (backgroundTransparentBtn.isSelected()) {
                            image = ImageUtils.makeColorTransparent(image,
                                    viewManager.getBackground());
                        }

                        ImageUtils.writeImageToFile(image, path,
                                getImageQuality());
                    }
                }

                ImageWrapper imageWrapper;

                if (viewManager != null) {
                    imageWrapper = new ImageWrapper(path, time, bounds,
                            viewManager.getDisplayMatrix());
                } else {
                    imageWrapper = new ImageWrapper(path, time);
                }

                imageWrapper.setProperties(imageProperties);
                images.add(imageWrapper);
                imagesChanged();
            }
        } catch (Throwable exc) {
            stopAnimationCapture(false);
            LogUtil.logException("Error capturing image", exc);
        }

    }

    /**
     * Capture images.
     *
     * @param components the components
     * @param cols number of columns
     * @return the image
     * @throws AWTException the aWT exception
     */
    public Image captureImages(List<? extends Component> components, int cols)
            throws AWTException {
        List<Image> images = new LinkedList<Image>();

            for (Component c : components) {
                images.add(captureImage(c));
            }

        return ImageUtils.gridImages2(images, 0, Color.GRAY, cols);
    }

    /**
     * Capture image.
     *
     * @param comp the comp
     * @return the image
     * @throws AWTException the aWT exception
     */
    public Image captureImage(Component comp) throws AWTException {
        Dimension dim;
        Point     loc;

        if (fullScreenBtn.isSelected()) {  // Full Screen
            dim = Toolkit.getDefaultToolkit().getScreenSize();
            loc = new Point(0, 0);
        } else {
            dim = comp.getSize();
            loc = comp.getLocationOnScreen();
        }

        GraphicsConfiguration gc    = comp.getGraphicsConfiguration();
        Robot                 robot = new Robot(gc.getDevice());

        if ((gc.getBounds().x > 0) || (gc.getBounds().y > 0)) {
            System.err.println("Offsetting location:" + loc
                               + " by gc bounds: " + gc.getBounds().x + " "
                               + gc.getBounds().y);
            loc.x -= gc.getBounds().x;
            loc.y -= gc.getBounds().y;
            System.err.println("new location:" + loc);
        }

        imageSize = new Dimension(dim.width, dim.height);

        return robot.createScreenCapture(new Rectangle(loc.x, loc.y,
                dim.width, dim.height));
    }

    /**
     * Subset bounds
     *
     * @param bounds the bounds
     * @param returnProps  the return properties
     */
    public static void subsetBounds(GeoLocationInfo bounds,
                                    Hashtable returnProps) {
        if (bounds == null) {
            return;
        }

        Double d;

        if ((d = (Double) returnProps.get(ImageGenerator.ATTR_NORTH))
                != null) {
            bounds.setMaxLat(d.doubleValue());
        }

        if ((d = (Double) returnProps.get(ImageGenerator.ATTR_SOUTH))
                != null) {
            bounds.setMinLat(d.doubleValue());
        }

        if ((d = (Double) returnProps.get(ImageGenerator.ATTR_WEST))
                != null) {
            bounds.setMinLon(d.doubleValue());
        }

        if ((d = (Double) returnProps.get(ImageGenerator.ATTR_EAST))
                != null) {
            bounds.setMaxLon(d.doubleValue());
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
        Dimension size = imageSize;

        if ((size == null) && (viewManager != null)) {
            Component comp = viewManager.getMaster().getDisplayComponent();

            // Get the size of the display
            size = comp.getSize();
        }

        if (size == null) {
            size = new Dimension(600, 400);
        }

        double displayRate =
            (Double.parseDouble(displayRateFld.getText()));
        double endPause = (Double.parseDouble(endPauseFld.getText()));

        if (scriptingNode != null) {
            displayRate = imageGenerator.applyMacros(scriptingNode,
                    imageGenerator.ATTR_FRAMERATE, displayRate);
            endPause = imageGenerator.applyMacros(scriptingNode,
                    imageGenerator.ATTR_ENDFRAMEPAUSE, -1);
        }

        createMovie(movieFile, images, size, displayRate, scriptingNode,
                    endPause);
    }

    /**
     * Create an image panel
     *
     * @param file   file to write to
     * @param images list of images
     * @param scriptingNode scripting node
     */
    private void createPanel(String file, List<ImageWrapper> images,
                             Element scriptingNode) {
        try {
            imageGenerator.debug("Making image panel:" + file);

            int width = imageGenerator.applyMacros(scriptingNode,
                            imageGenerator.ATTR_WIDTH, 100);
            int columns = imageGenerator.applyMacros(scriptingNode,
                              imageGenerator.ATTR_COLUMNS, 1);
            int space = imageGenerator.applyMacros(scriptingNode,
                            imageGenerator.ATTR_SPACE, 0);
            Color background = imageGenerator.applyMacros(scriptingNode,
                                   imageGenerator.ATTR_BACKGROUND,
                                   (Color) null);
            List sizedImages = new ArrayList();

            for (ImageWrapper imageWrapper : images) {
                String        imageFile     = imageWrapper.getPath();
                Image         image         = ImageUtils.readImage(imageFile);
                BufferedImage bufferedImage =
                    ImageUtils.toBufferedImage(image);

                image = imageGenerator.resize(bufferedImage, scriptingNode);
                sizedImages.add(image);
            }

            Image image = ImageUtils.gridImages(sizedImages, space,
                              background, columns);

            ImageUtils.writeImageToFile(image, file);
        } catch (Exception exc) {
            LogUtil.logException("Writing panel", exc);
        }
    }

    /**
     * actually create the movie
     *
     *
     * @param commaSeparatedFiles This can be a list of comma separated files. eg: .mov, .kmz, etc
     * @param images List of images to make a movie from
     * @param size size
     * @param displayRate display rate
     * @param scriptingNode isl node. May be null
     */
    private void createMovie(String commaSeparatedFiles,
                             List<ImageWrapper> images, Dimension size,
                             double displayRate, Element scriptingNode) {
        createMovie(commaSeparatedFiles, images, size, displayRate,
                    scriptingNode, -1);
    }

    /**
     * What file suffix should we use for the images. For now better by jpg
     *
     * @return File suffix
     */
    protected String getFileSuffix() {
        if (justCaptureAnimation) {
            return FileManager.SUFFIX_PNG;
        }

        String defSuffix = FileManager.SUFFIX_PNG;

        if ((scriptingNode != null) && (movieFileName != null)) {
            final String suffix = IOUtil.getFileExtension(movieFileName);

            if (suffix != null) {
                if (suffix.equalsIgnoreCase(FileManager.SUFFIX_KMZ)
                        || suffix.equalsIgnoreCase(FileManager.SUFFIX_ZIP)
                        || suffix.equalsIgnoreCase(FileManager.SUFFIX_KML)) {
                    defSuffix = FileManager.SUFFIX_PNG;
                } else if (suffix.equalsIgnoreCase(FileManager.SUFFIX_MOV)) {
                    defSuffix = FileManager.SUFFIX_JPG;
                }  else if (suffix.equalsIgnoreCase(FileManager.SUFFIX_GIF)) {
                    defSuffix = FileManager.SUFFIX_PNG;
                }  else if (suffix.equalsIgnoreCase(FileManager.SUFFIX_MP4)) {
                    defSuffix = FileManager.SUFFIX_PNG;
                }// TODO: AVI?
            }

            defSuffix = imageGenerator.applyMacros(scriptingNode,
                    ATTR_IMAGESUFFIX, defSuffix);

            if ( !defSuffix.startsWith(".")) {
                defSuffix = "." + defSuffix;
            }
        }

        if ((backgroundTransparentBtn != null)
                && backgroundTransparentBtn.isSelected()) {
            defSuffix = FileManager.SUFFIX_PNG;
        }

        return defSuffix;
    }

    /**
     * actually create the movie
     *
     *
     * @param commaSeparatedFiles This can be a list of comma separated files. eg: .mov, .kmz, etc
     * @param images List of images to make a movie from
     * @param size size
     * @param displayRate display rate
     * @param scriptingNode isl node. May be null
     * @param endPause end pause value
     */
    private void createMovie(String commaSeparatedFiles,
                             List<ImageWrapper> images, Dimension size,
                             double displayRate, Element scriptingNode,
                             double endPause) {

        List fileToks = StringUtil.split(commaSeparatedFiles, ",", true,
                                         true);
        boolean doingPanel = false;

        if ((scriptingNode != null)
                && scriptingNode.getTagName().equals("panel")) {
            doingPanel = true;
        }

        if (writePositions && (fileToks.size() > 0)) {
            try {
                String positionFilename =
                    IOUtil.stripExtension((String) fileToks.get(0)) + ".xidv";
                List<double[]> positions = new ArrayList<double[]>();

                for (ImageWrapper imageWrapper : images) {
                    positions.add(imageWrapper.getPosition());
                }

                IOUtil.writeFile(positionFilename,
                                 idv.encodeObject(positions, true, true));
            } catch (IOException ioe) {
                LogUtil.userErrorMessage("Error writing positions:" + ioe);

                return;
            }
        }

        // System.err.println("doingPanel:" + doingPanel + " " + scriptingNode);
        for (int i = 0; i < fileToks.size(); i++) {
            String movieFile = (String) fileToks.get(i);

            try {
                if (doingPanel) {
                    createPanel(movieFile, images, scriptingNode);

                    continue;
                }
                // IDV in non-interactive mode does not properly get the size
                // for n-panel views because ATTR_HEIGHT, ATTR_WIDTH are set
                // for one panel in ImageGenerator.captureMovie.

                if (( !isInteractive()) && (images.size() > 0)) {

                    BufferedImage image = ImageUtils.toBufferedImage(
                                              ImageUtils.readImage(
                                                  images.get(0).getPath()));

                    ImageUtils.waitOnImage(image);

                    size = new Dimension(image.getWidth(null),
                                         image.getHeight(null));
                }

                // System.err.println("createMovie:" + movieFile);
                if (movieFile.toLowerCase().endsWith(
                        FileManager.SUFFIX_GIF)) {
                    double  rate   = 1.0 / displayRate;
                    boolean useGCT = getGlobalPaletteValue();
                    AnimatedGifEncoder.createGif(movieFile,
                            ImageWrapper.makeFileList(images),
                            AnimatedGifEncoder.REPEAT_FOREVER,
                            (int) (rate * 1000), (int) ((endPause == -1)
                            ? -1
                            : endPause * 1000), useGCT);
                } else if (movieFile.toLowerCase().endsWith(".htm")
                           || movieFile.toLowerCase().endsWith(".html")) {
                    createAnisHtml(movieFile, images, size, displayRate,
                            scriptingNode);
                } else if (movieFile.toLowerCase()
                        .endsWith(FileManager.SUFFIX_KMZ)) {
                    createKmz(movieFile, images, scriptingNode);
                } else if (movieFile.toLowerCase().endsWith(FileManager.SUFFIX_ZIP)) {
                    createZip(movieFile, images, scriptingNode);
                } else if (movieFile.toLowerCase().endsWith(
                        FileManager.SUFFIX_AVI)) {
                    ImageUtils.writeAvi(ImageWrapper.makeFileList(images),
                                        displayRate, new File(movieFile));
                } else if (movieFile.toLowerCase().endsWith(FileManager.SUFFIX_MP4)) {
                    File output = new File(movieFile);
                    // TODO(jon): jcodec has a strange way of specifying FPS...30 FPS would be "30/1", 29.97 FPS would be "30000/1001".
                    //SequenceEncoder enc = SequenceEncoder.createWithFps(NIOUtils.writableChannel(output), new Rational(displayRate, 1));
                    //for (ImageWrapper img : images) {
                    //    logger.trace("IMG WRAPPER: path: {}", img.getPath());
                    //}

                    SequenceEncoder enc = SequenceEncoder.createSequenceEncoder(output, (int)displayRate);
                    List<String> imageList = ImageWrapper.makeFileList(images);
                    for (String image : imageList) {
                        if (image.endsWith(".jpg")) {
                            enc.encodeNativeFrame(decodeJPG(new File(image), ColorSpace.RGB));
                        } else if (image.endsWith(".png")) {
                            enc.encodeNativeFrame(decodePNG(new File(image), ColorSpace.RGB));
                        }
                    }
                    enc.finish();
                } else {

                    // System.err.println("mov:" + movieFile);
                    //SecurityManager backup = System.getSecurityManager();

                    //System.setSecurityManager(null);

                    if (size == null) {
                        size = new Dimension(600, 400);
                    }

                    JpegImagesToMovie.createMovie(movieFile, size.width,
                            size.height, (int) displayRate,
                            new Vector(ImageWrapper.makeFileList(images)));
                    //System.setSecurityManager(backup);
                }
            } catch (NumberFormatException nfe) {
                LogUtil.userErrorMessage("Bad number format");

                return;
            } catch (IOException ioe) {
                LogUtil.userErrorMessage("Error writing movie:" + ioe);

                return;
            }

            idv.getPublishManager().publishContent(movieFile, viewManager,
                    publishCbx);
        }

    }

    public Picture decodeJPG(File f, ColorSpace tgtColor) throws IOException {
        Picture picture = decodeJPG0(f);
        return convertColorSpace(picture, tgtColor);
    }

    public Picture decodeJPG0(File f) throws IOException {
        padImage(f, "jpg");
        JpegDecoder jpgDec = new JpegDecoder();
        ByteBuffer buf = NIOUtils.fetchFromFile(f);
        VideoCodecMeta codecMeta = jpgDec.getCodecMeta(buf);
        Picture pic = Picture.create(codecMeta.getSize().getWidth(), codecMeta.getSize().getHeight(),
                ColorSpace.RGB);
        return jpgDec.decodeFrame(buf, pic.getData());
    }

    public Picture decodePNG(File f, ColorSpace tgtColor) throws IOException {
        Picture picture = decodePNG0(f);
        Preconditions.checkNotNull(picture, "cant decode " + f.getPath());
        return convertColorSpace(picture, tgtColor);
    }

    public Picture decodePNG0(File f) throws IOException {
        padImage(f, "png");
        PNGDecoder pngDec = new PNGDecoder();
        ByteBuffer buf = NIOUtils.fetchFromFile(f);
        VideoCodecMeta codecMeta = pngDec.getCodecMeta(buf);
        Picture pic = Picture.create(codecMeta.getSize().getWidth(), codecMeta.getSize().getHeight(),
                ColorSpace.RGB);
        return pngDec.decodeFrame(buf, pic.getData());
    }

    /**
     * Given an existing image file, check to see if either the width or height
     * of the image is not divisible by two, and simply rewrite the given image
     * with both a width and height that are divisible by two.
     *
     * <p>This oddity is due to an apparent limitation with YUV420.</p>
     *
     * @param f Image file. Cannot be {@code null}.
     * @param type For now one of {@literal "png"} or {@code "jpg"}.
     *
     * @throws IOException if there was a problem reading or writing the image.
     */
    private void padImage(File f, String type) throws IOException {
        BufferedImage orig = ImageIO.read(f);
        int newWidth = orig.getWidth();
        int newHeight = orig.getHeight();
        if ((orig.getWidth() % 2) != 0) {
            newWidth++;
        }
        if ((orig.getHeight() % 2) != 0) {
            newHeight++;
        }
        if ((newWidth != orig.getWidth()) || (newHeight != orig.getHeight())) {
            BufferedImage newImg = new BufferedImage(newWidth, newHeight, orig.getType());
            Graphics g = newImg.getGraphics();
            Color bgColor = Color.BLACK;
            if (viewManager != null) {
                bgColor = viewManager.getBackground();
            }
            g.setColor(bgColor);
            g.fillRect(0, 0, newWidth, newHeight);
            g.drawImage(orig, 0, 0, null);
            g.dispose();
            ImageIO.write(newImg, type, f);
        }
    }
    /**
     * create the kmz
     *
     * @param movieFile file name
     * @param images list of images
     * @param scriptingNode isl node
     */
    public void createKmz(String movieFile, List<ImageWrapper> images,
                          Element scriptingNode) {

        try {
            ZipOutputStream zos = null;

            if (movieFile.toLowerCase().endsWith(FileManager.SUFFIX_KMZ)) {
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
                    String  file  = XmlUtil.getAttribute(child, ATTR_FILENAME);

                    if (zos != null) {
                        zos.putNextEntry(
                            new ZipEntry(IOUtil.getFileTail(file)));

                        byte[] bytes =
                            IOUtil.readBytes(IOUtil.getInputStream(file));

                        zos.write(bytes, 0, bytes.length);
                    }
                }
            }

            // <name>Buienradar.nl</name>
            TimeZone tz          = TimeZone.getTimeZone("GMT");
            boolean  didKmlFiles = false;

            for (ImageWrapper imageWrapper : images) {
                List kmlFiles = (List) imageWrapper.getProperty("kmlfiles");

                if ( !didKmlFiles && (kmlFiles != null)) {
                    didKmlFiles = true;

                    for (String kmlFile : (List<String>) kmlFiles) {
                        String tail = IOUtil.getFileTail(kmlFile);

                        if (zos != null) {
                            zos.putNextEntry(new ZipEntry(tail));

                            byte[] imageBytes = IOUtil.readBytes(
                                                    new FileInputStream(
                                                        kmlFile));

                            zos.write(imageBytes, 0, imageBytes.length);
                        }
                    }
                }

                String extraKml = (String) imageWrapper.getProperty("kml");
                String image    = imageWrapper.getPath();
                String tail     = IOUtil.getFileTail(image);

                // System.err.println("tail:" + tail);
                if (zos != null) {
                    zos.putNextEntry(new ZipEntry(tail));

                    byte[] imageBytes =
                        IOUtil.readBytes(new FileInputStream(image));

                    zos.write(imageBytes, 0, imageBytes.length);
                }

                DateTime        dttm   = imageWrapper.getDttm();
                GeoLocationInfo bounds = imageWrapper.getBounds();

                if (extraKml != null) {
                    sb.append(extraKml);
                }

                sb.append("<GroundOverlay>\n");
                sb.append("<name>" + ((dttm == null)
                                      ? tail
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
                        + FileManager.SUFFIX_KML));

                byte[] kmlBytes = sb.toString().getBytes();

                // System.out.println("sb:" + sb);
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
     * create the kmz
     *
     * @param movieFile file name
     * @param images list of images
     * @param scriptingNode isl node
     */
    public void createZip(String movieFile, List<ImageWrapper> images,
                          Element scriptingNode) {

        try {
            ZipOutputStream zos = null;

            if (movieFile.toLowerCase().endsWith(FileManager.SUFFIX_ZIP)) {
                zos = new ZipOutputStream(new FileOutputStream(movieFile));
            }

            StringBuffer sb =
                    new StringBuffer(
                            "<entries>\n");
            DateTime time = null;

            time = (((getAnimation() != null)
                    && (getAnimation().getAniValue() != null))
                    ? new DateTime(getAnimation().getAniValue())
                    : null);

            String pName = IOUtil.stripExtension(IOUtil.getFileTail(movieFile));
            sb.append("<entry fromdate=\"" + time.toString() + "\" id=\"imageloopParent\" name=\"" + pName + "\"  type=\"media_imageloop\">\n");
            sb.append("</entry>\n");

            int idx = 0;
            for (ImageWrapper imageWrapper : images) {

                zos.putNextEntry(
                        new ZipEntry(IOUtil.getFileTail(imageWrapper.getPath())));
                byte[] imageBytes = IOUtil.readBytes(
                        new FileInputStream(
                                imageWrapper.getPath()));

                zos.write(imageBytes, 0, imageBytes.length);

                String image    = imageWrapper.getPath();
                String tail     = IOUtil.getFileTail(image);


                DateTime        dttm   = imageWrapper.getDttm();
                GeoLocationInfo bounds = imageWrapper.getBounds();
                String         nameStr = "image_" + idx;

                sb.append("<entry fromdate=\"" + dttm.toString() + "\" file=\""+ tail + "\" name=\"" + nameStr +
                                               "\" parent=\"imageloopParent\" type=\"file\">\n");
                sb.append("</entry>\n");
                idx++;
            }

            sb.append("</entries>\n");
            if (zos != null) {
                zos.putNextEntry(
                        new ZipEntry("entries" + FileManager.SUFFIX_XML));

                byte[] kmlBytes = sb.toString().getBytes();

                // System.out.println("sb:" + sb);
                zos.write(kmlBytes, 0, kmlBytes.length);
                zos.close();
            } else {
                IOUtil.writeFile(movieFile, sb.toString());
            }
        } catch (Exception exc) {
            LogUtil.logException("Saving zip file", exc);
        }

    }
    /**
     * create the anis html
     *
     * @param movieFile file name
     * @param images list of images
     * @param size the size
     * @param displayRate rate
     * @param scriptingNode isl node
     */
    private void createAnisHtml(String movieFile, List<ImageWrapper> images,
                                Dimension size, double displayRate,
                                Element scriptingNode) {

        try {
            boolean copyFiles = false;
            boolean doFlanis  = false;
            String  type      = "anis";
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
                    typeCbx   = new JCheckBox("FlAniS? (default: AniS)");
                    typeCbx.setToolTipText(
                        "Check this box to create HTML to use the Flash animator (FlAniS); otherwise, the java applet animator (AniS) will be used.");
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
                    GuiUtils.rLabel(""), typeCbx, GuiUtils.rLabel(""), copyCbx
                }, 2, GuiUtils.WT_NY, GuiUtils.WT_N);

                if ( !GuiUtils.showOkCancelDialog(null,
                        "ANIS Applet Information", contents, null)) {
                    return;
                }

                copyFiles = copyCbx.isSelected();
                doFlanis  = typeCbx.isSelected();
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
                type = XmlUtil.getAttribute(scriptingNode, ATTR_ANIS_TYPE,
                                            type);
                doFlanis = type.toLowerCase().equals("anis")
                           ? false
                           : true;
            }

            StringBuffer sb    = new StringBuffer();
            String       files = "";

            for (int i = 0; i < images.size(); i++) {
                ImageWrapper imageWrapper = images.get(i);
                String       file         = imageWrapper.getPath();

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

            if (doFlanis) {
                sb.append(
                    "<OBJECT type=\"application/x-shockwave-flash\" data=\"./flanis.swf\" width=\""
                    + width + "\" height=\"" + height
                    + "\" id=\"FlAniS\"> \n");
                sb.append("<PARAM NAME=\"movie\" VALUE=\"./flanis.swf\"> \n");
                sb.append("<PARAM NAME=\"quality\" VALUE=\"high\"> \n");
                sb.append("<PARAM NAME=\"menu\" value=\"false\"> \n");
                sb.append(
                    "<PARAM NAME=\"FlashVars\" value=\"controls=startstop,step,speed,toggle,zoom&filenames="
                    + files + "\"> \n");
                sb.append("</OBJECT>\n");
            } else {
                sb.append("<APPLET code=\"AniS.class\" width=" + width
                          + " height=" + height + ">\n");
                sb.append(
                    "<PARAM name=\"controls\" value=\"startstop,step, speed, toggle, zoom\">\n");
                sb.append("<PARAM name=\"filenames\" value=\"" + files
                          + "\">\n");
                sb.append("</APPLET>\n");
            }

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
