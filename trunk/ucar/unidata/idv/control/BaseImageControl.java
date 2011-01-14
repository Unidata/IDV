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


import ucar.unidata.data.CompositeDataChoice;

import ucar.unidata.data.DataCancelException;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.DataUtil;
import ucar.unidata.data.DerivedDataChoice;
import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.IdvConstants;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.util.ColorTable;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;


import ucar.unidata.util.Resource;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.Util;
import ucar.visad.display.ColorScale;
import ucar.visad.display.ImageSequenceDisplayable;

import visad.*;
import visad.FunctionType;
import visad.RealTupleType;
import visad.RealType;

import visad.georef.MapProjection;

import visad.meteorology.ImageSequence;

import visad.meteorology.ImageSequenceManager;
import visad.meteorology.SingleBandedImageImpl;

import visad.util.BaseRGBMap;
import visad.util.ColorPreview;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.net.URL;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


import javax.swing.*;
import javax.swing.event.*;


/**
 * A DisplayControl for handling image sequences
 *
 * @author Don Murray
 * @version $Revision: 1.10 $ $Date: 2007/08/09 17:21:31 $
 */
public abstract class BaseImageControl extends GridDisplayControl {


    /**
     * empty RGB image
     * @deprecated  use public EMPTY_RGB_IMAGE
     */
    protected static FieldImpl emptyRGBImage = null;

    /**
     * empty sequence
     * @deprecated  use public EMPTY_IMAGE
     */
    protected static FieldImpl emptyImage = null;

    /** empty RGB image */
    public static FieldImpl EMPTY_RGB_IMAGE = null;

    /** empty sequence */
    public static FieldImpl EMPTY_IMAGE = null;

    static {
        try {
            RealType      line    = RealType.YAxis;
            RealType      element = RealType.XAxis;
            RealType      band    = RealType.getRealType("band");
            RealType      red     = RealType.getRealType("r");
            RealType      green   = RealType.getRealType("g");
            RealType      blue    = RealType.getRealType("b");
            RealTupleType rgb     = new RealTupleType(red, green, blue);
            RealTupleType domain  = new RealTupleType(element, line);
            domain.setDefaultSet(new SingletonSet(new RealTuple(domain,
                    new Real[] { new Real(element),
                                 new Real(line) }, null)));

            FlatField ff = new FlatField(new FunctionType(domain, band));
            emptyImage  = ff;
            EMPTY_IMAGE = ff;
            FlatField ffRGB = new FlatField(new FunctionType(domain, rgb));
            emptyRGBImage   = ffRGB;
            EMPTY_RGB_IMAGE = ffRGB;
        } catch (Exception ex) {
            System.out.println("Couldn't create empty set: " + ex);
        }
    }


    /** Image transparency */
    private float alpha = 1.0f;


    /**
     * Default ctor; sets the attribute flags
     */
    public BaseImageControl() {
        //setAttributeFlags(FLAG_ZPOSITION);
    }


    /**
     * What label to use for the data projection
     *
     * @return label
     */
    protected String getDataProjectionLabel() {
        return "Use Native Image Projection";
    }



    /**
     * Applies skip value if necessary
     *
     * @param image  original image
     *
     * @return blown down image if we have a skip value
     *
     * @throws VisADException On badness
     */
    protected FieldImpl getWorkingImage(FieldImpl image)
            throws VisADException {
        if (getSkipValue() <= 0) {
            return image;
        }
        return GridUtil.subset(image, getSkipValue() + 1);
    }


    /**
     * Return the label that is to be used for the skip widget
     * This allows derived classes to override this and provide their
     * own name,
     *
     * @return Label used for the line width widget
     */
    public String getSkipWidgetLabel() {
        return "Pixel Sampling";
    }

    /**
     * Make the alpha slider
     *
     * @return alpha slider component
     */
    protected JComponent doMakeAlphaSlider() {
        JSlider alphaSlider = GuiUtils.makeSlider(0, 100,
                                  100 - (int) (getAlpha() * 100), this,
                                  "setInverseAlphaFromSlider");
        JPanel transLabel = GuiUtils.leftRight(GuiUtils.lLabel("0%"),
                                GuiUtils.rLabel("100%"));
        return GuiUtils.vbox(alphaSlider, transLabel);
    }



    /**
     * Set the alpha
     *
     * @param f Alpha
     */
    public void setAlpha(float f) {
        alpha = f;
    }


    /**
     * Get the alpha
     *
     * @return Get the alpha
     */
    public float getAlpha() {
        return alpha;
    }


    /**
     * Set the alpha
     *
     * @param newAlpha new value
     */
    protected void setAlphaFromSlider(float newAlpha) {
        try {
            alpha = newAlpha;
            //            if (imageDisplay != null) {
            //                imageDisplay.setAlpha(alpha);
            //            }
        } catch (Exception e) {
            logException("Setting alpha value", e);
        }
    }


    /**
     * Called on slider action
     *
     * @param sliderValue slider value
     */
    public void setInverseAlphaFromSlider(int sliderValue) {
        sliderValue = 100 - sliderValue;
        setAlphaFromSlider((float) (((double) sliderValue) / 100.0));
    }



    /**
     * Flag for showing the SkipFactorSlider. Subclasses can override
     * if they don't want to show it.
     *
     * @return true
     */
    protected boolean showSkipFactorSlider() {
        return checkFlag(FLAG_SKIPFACTOR);
    }

    /**
     * Is this a raster display?
     *
     * @return  true if raster
     */
    public boolean getIsRaster() {
        return true;
    }

}
