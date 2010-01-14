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

package ucar.visad.display;



import visad.*;

import visad.java2d.*;

import visad.java3d.*;

import visad.util.HersheyFont;

import java.awt.Font;

import java.rmi.RemoteException;

import java.text.DecimalFormat;

import java.text.NumberFormat;


/**
 * Super class for displaying text data
 *
 * @author IDV Development Team
 * @version $Revision: 1.17 $
 */
public class TextDisplayable extends LineDrawing {

    /** Control for the ScalarMap */
    private TextControl textControl;

    /** ScalarType for the ScalarMap */
    private ScalarType textType;

    /** ScalarMap for the text */
    private ScalarMap textMap;

    /** text format */
    NumberFormat labelFormat = new DecimalFormat("#########");

    /** text font */
    Object labelFont = null;

    /** The text rotation */
    private double rotation = 0.0;

    /** The character rotation */
    private double characterRotation = 0.0;

    /** text size */
    private float textSize = 1.0f;

    /** text justification */
    private TextControl.Justification justification =
        TextControl.Justification.CENTER;

    /** text justification */
    private TextControl.Justification verticalJustification =
        TextControl.Justification.BOTTOM;

    /** screen locked */
    private boolean screenLocked = false;

    /** screen locked */
    private boolean useSphere = false;


    /**
     * Default constructor
     * @throws VisADException  necessary VisAD object couldn't be created
     * @throws RemoteException  remote error
     */
    public TextDisplayable() throws VisADException, RemoteException {
        this("Display_Text");
    }

    /**
     * Construct with the given name.
     * @param name  name must contain no spaces.
     * @throws VisADException  necessary VisAD object couldn't be created
     * @throws RemoteException  remote error
     */
    public TextDisplayable(String name)
            throws VisADException, RemoteException {
        this(name, null);
    }

    /**
     * Construct with the given name.
     * @param textType  type to map to Display.Text  may not be null
     * @throws VisADException  necessary VisAD object couldn't be created
     * @throws RemoteException  remote error
     */
    public TextDisplayable(ScalarType textType)
            throws VisADException, RemoteException {
        this(textType.getName(), textType);
    }

    /**
     * Construct with the given name.
     * @param name  name to identify this object
     * @param textType  type to map to Display.Text
     * @throws VisADException  necessary VisAD object couldn't be created
     * @throws RemoteException  remote error
     */
    public TextDisplayable(String name, ScalarType textType)
            throws VisADException, RemoteException {
        this(name, textType, false);
    }

    /**
     * Construct with the given name.
     * @param name  name to identify this object
     * @param textType  type to map to Display.Text
     * @param screenLocked true to use a screen locked renderer
     * @throws VisADException  necessary VisAD object couldn't be created
     * @throws RemoteException  remote error
     */
    public TextDisplayable(String name, ScalarType textType,
                           boolean screenLocked)
            throws VisADException, RemoteException {
        super(name);
        this.screenLocked = screenLocked;
        if (textType != null) {
            setTextType(textType);
        }
    }

    /**
     * Constructs from another instance.
     * @param that              The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected TextDisplayable(TextDisplayable that)
            throws RemoteException, VisADException {

        super(that);
        this.textType              = that.textType;
        this.labelFormat           = that.labelFormat;
        this.labelFont             = that.labelFont;
        this.rotation              = that.rotation;
        this.characterRotation     = that.characterRotation;
        this.textSize              = that.textSize;
        this.justification         = that.justification;
        this.verticalJustification = that.verticalJustification;
        this.screenLocked          = that.screenLocked;
        if (textType != null) {
            setTextScalarMap(textType);
        }

    }

    /**
     * Set the text type to use.
     * @param textType  RealType or TextType to map to Display.Text
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setTextType(ScalarType textType)
            throws RemoteException, VisADException {
        this.textType = textType;
        setTextScalarMap(textType);
    }

    /**
     * Return the currently used ScalarType for display.
     * @return  the ScalarType
     */
    public ScalarType getTextType() {
        return textType;
    }

    /**
     * Set the formatting for all labels
     * @param format  Number format
     *
     * @throws RemoteException   Java RMI error
     * @throws VisADException    VisAD error
     */
    public void setNumberFormat(NumberFormat format)
            throws VisADException, RemoteException {
        if ((textControl != null) && (format != null)) {
            textControl.setNumberFormat(format);
        }
        labelFormat = format;
    }

    /**
     * Get the formatting for text object if values.  May be null (if not set)
     * @return format used for labeling
     */
    public NumberFormat getNumberFormat() {
        return labelFormat;
    }

    /**
     * Set the screenlocked attribute
     * @param locked  true to set screen locked
     *
     * @throws RemoteException   Java RMI error
     * @throws VisADException    VisAD error
     */
    public void setScreenLocked(boolean locked)
            throws VisADException, RemoteException {
        screenLocked = locked;
        if ((textControl != null)) {
            textControl.setAutoSize( !screenLocked);
        }
    }

    /**
     * Get whether this is a screen locked display or not
     * @return true if screen locked
     */
    public boolean getScreenLocked() {
        return screenLocked;
    }

    /**
     * Set the size for all labels
     * @param size  size (1.0 default)
     *
     * @throws RemoteException   Java RMI error
     * @throws VisADException    VisAD error
     */
    public void setTextSize(float size)
            throws VisADException, RemoteException {
        if ((textControl != null) && (size != textSize)) {
            textControl.setSize(size);
        }
        textSize = size;
    }

    /**
     * Get the currently used text size
     * @return size of text
     */
    public float getTextSize() {
        return (textControl != null)
               ? (float) textControl.getSize()
               : textSize;
    }



    /**
     * Set the whether we're on a sphere
     * @param sphere  true if sphere
     *
     * @throws RemoteException   Java RMI error
     * @throws VisADException    VisAD error
     */
    public void setSphere(boolean sphere)
            throws VisADException, RemoteException {
        if ((textControl != null) && (useSphere != sphere)) {
            textControl.setSphere(sphere);
        }
        useSphere = sphere;
    }

    /**
     * Get the sphere property
     * @return true if sphere
     */
    public boolean getSphere() {
        return useSphere;
    }



    /**
     * Set the justification for all labels
     * @param justification The justification
     *
     * @throws RemoteException   Java RMI error
     * @throws VisADException    VisAD error
     */
    public void setJustification(TextControl.Justification justification)
            throws VisADException, RemoteException {

        if ((textControl != null) && (this.justification != justification)) {
            textControl.setJustification(justification);
        }
        this.justification = justification;
    }

    /**
     * Get the currently used text justification
     * @return justification
     */
    public TextControl.Justification getJustification() {
        return justification;
    }




    /**
     * Set the vertical justification for all labels
     *
     * @param verticalJustification The justification
     *
     * @throws RemoteException   Java RMI error
     * @throws VisADException    VisAD error
     */
    public void setVerticalJustification(
            TextControl.Justification verticalJustification)
            throws VisADException, RemoteException {

        if ((textControl != null)
                && (this.verticalJustification != verticalJustification)) {
            textControl.setVerticalJustification(verticalJustification);
        }
        this.verticalJustification = verticalJustification;
    }

    /**
     * Get the currently used text vertical justification
     * @return justification
     */
    public TextControl.Justification getverticalJustification() {
        return verticalJustification;
    }



    /**
     * Set the Font for all
     *
     * @param font  font for text (Font or HersheyFont)
     *
     * @throws RemoteException   Java RMI error
     * @throws VisADException    VisAD error or not a valid font object
     */
    public void setFont(Object font) throws VisADException, RemoteException {
        if ( !((font instanceof java.awt.Font)
                || (font instanceof visad.util.HersheyFont)
                || (font == null))) {
            throw new VisADException(
                "Font must be java.awt.Font or HersheyFont");
        }
        if (textControl != null) {
            textControl.setFont(font);
        }
        labelFont = font;
    }

    /**
     * Set the rotation
     *
     * @param rotation  rotation
     *
     * @throws RemoteException   Java RMI error
     * @throws VisADException    VisAD error
     */
    public void setRotation(double rotation)
            throws VisADException, RemoteException {
        if (textControl != null) {
            textControl.setRotation(rotation);
        }
        this.rotation = rotation;
    }


    /**
     * Set the character rotation
     *
     * @param rotation  rotation
     *
     * @throws RemoteException   Java RMI error
     * @throws VisADException    VisAD error
     */
    public void setCharacterRotation(double rotation)
            throws VisADException, RemoteException {
        if (textControl != null) {
            textControl.setCharacterRotation(rotation);
        }
        this.characterRotation = rotation;
    }

    /**
     * Get the font for labels.  May be null (if not set or using HersheyFont)
     * @return font used for labeling
     */
    public Font getFont() {

        if (labelFont instanceof java.awt.Font) {
            return (Font) labelFont;
        } else {
            return null;
        }
    }

    /**
     * Get the font for labels.  May be null (if not set or using HersheyFont)
     * @return font used for labeling
     */
    public HersheyFont getHersheyFont() {

        if (labelFont instanceof visad.util.HersheyFont) {
            return (HersheyFont) labelFont;
        } else {
            return null;
        }
    }

    /**
     * Create the ScalarMap to text for the given ScalarType
     *
     * @param textType  type for the ScalarMap
     *
     * @throws RemoteException   Java RMI error
     * @throws VisADException    VisAD error
     */
    private void setTextScalarMap(ScalarType textType)
            throws VisADException, RemoteException {

        ScalarMap oldTextMap = textMap;

        textMap = new ScalarMap(textType, Display.Text);

        textMap.addScalarMapListener(new ScalarMapListener() {

            public void controlChanged(ScalarMapControlEvent event)
                    throws RemoteException, VisADException {

                int id = event.getId();

                if ((id == event.CONTROL_ADDED)
                        || (id == event.CONTROL_REPLACED)) {
                    textControl = (TextControl) textMap.getControl();

                    if (textControl != null) {  //set parms for textControl
                        textControl.setJustification(justification);
                        textControl.setVerticalJustification(
                            verticalJustification);
                        textControl.setSize(textSize);
                        textControl.setAutoSize( !screenLocked);
                        textControl.setNumberFormat(labelFormat);
                        textControl.setRotation(rotation);
                        textControl.setCharacterRotation(characterRotation);
                        textControl.setFont(labelFont);
                        textControl.setSphere(useSphere);
                    }
                }
            }

            public void mapChanged(ScalarMapEvent event) {}  // ignore
        });
        if (textType instanceof RealType) {
            applyDisplayUnit(textMap, (RealType) textType);
        }
        replaceScalarMap(oldTextMap, textMap);
        fireScalarMapSetChange();
    }

    /**
     * Set the units for the displayed range
     * @param unit Unit for display
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setDisplayUnit(Unit unit)
            throws VisADException, RemoteException {
        //Make sure this unit is ok
        if ( !(textType instanceof RealType)) {
            return;
        }
        checkUnit((RealType) textType, unit);
        super.setDisplayUnit(unit);
        applyDisplayUnit(textMap, (RealType) textType);
    }


    /**
     * Clone this {@link Displayable} so it can go into a different
     * display.
     *
     * @return clone of this object
     *
     * @throws RemoteException   Java RMI error
     * @throws VisADException    VisAD error
     */
    public synchronized Displayable cloneForDisplay()
            throws RemoteException, VisADException {
        return new TextDisplayable(this);
    }

    /**
     * Get the renderer for this TextDisplayable
     * @return  renderer
     *
     * @throws VisADException problem creating renderer
     */
    protected DataRenderer getDataRenderer() throws VisADException {

        DataRenderer renderer = (getDisplay().getDisplayRenderer()
                                 instanceof DisplayRendererJ2D)
                                ? (DataRenderer) new DefaultRendererJ2D()
                                : (screenLocked)
                                  ? (DataRenderer) new visad.bom
                                      .ScreenLockedRendererJ3D()
                                  : (DataRenderer) new DefaultRendererJ3D();
        return renderer;

    }

}
