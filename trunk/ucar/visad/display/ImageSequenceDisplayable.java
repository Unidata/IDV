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

import visad.bom.ImageRendererJ3D;

import visad.java2d.DefaultRendererJ2D;

import visad.java2d.DisplayRendererJ2D;

import visad.meteorology.*;

import visad.util.DataUtility;



import java.rmi.RemoteException;

import java.util.*;


/**
 * Provides support for displaying ImageSequences
 * @author Don Murray
 * @version $Revision: 1.27 $
 */
public class ImageSequenceDisplayable extends Grid2DDisplayable {

    /** sequence (data) */
    private ImageSequence sequence;

    /** renderer */
    private ImageRendererJ3D myRenderer = null;

    /**
     * Create a new ImageSequenceDisplayable with the given name and
     * using the given type
     *
     * @param name        name for this displayable
     * @param imageType   RealType for image data
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public ImageSequenceDisplayable(String name, RealType imageType)
            throws VisADException, RemoteException {
        this(name, imageType, null);
    }

    /**
     * Create an ImageSequenceDisplayable
     *
     * @param name        name for this displayable
     * @param imageType   RealType for image data
     * @param initialColorPalette  initial color palette
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public ImageSequenceDisplayable(String name, RealType imageType,
                                    float[][] initialColorPalette)
            throws VisADException, RemoteException {
        super(name, imageType, initialColorPalette,
              ((initialColorPalette != null)
               ? initialColorPalette.length == 4
               : true));
    }

    /**
     * Construct an ImageSequenceDisplayable using the data an
     * properties of another
     *
     * @param that  the other ImageSequenceDisplayable
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    protected ImageSequenceDisplayable(ImageSequenceDisplayable that)
            throws VisADException, RemoteException {
        super(that);
        setImageSequence(that.sequence);
    }

    /**
     * Set the data into this Displayable
     *
     * @param sequence  sequence of images
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setImageSequence(ImageSequence sequence)
            throws VisADException, RemoteException {
        setImageSequence((FieldImpl) sequence);
    }

    /**
     * Set the alpha. Unused.
     *
     * @param alpha alpha
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setAlpha(float alpha) throws RemoteException, VisADException {
        addConstantMaps(new ConstantMap[] {
            new ConstantMap(alpha, Display.Alpha) });
    }



    /**
     * Set the data into this Displayable.  Data does not have to be
     * an ImageSequence, but does have to be a sequence of images
     *
     * @param sequence  sequence of images
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setImageSequence(FieldImpl sequence)
            throws VisADException, RemoteException {

        if (sequence == null) {
            throw new VisADException("Sequence can't be null");
        }
        loadData(sequence);

        /*
        RealType sequenceType =
            (RealType) ((FunctionType) DataUtility.getRangeType(
                sequence)).getFlatRange().getComponent(0);

        //boolean  reuse        = (sequence.getImageCount() == 1)
        //                ? false
        //                : true;
        // for now, let's always make it false since if true, then
        // animation set is not correct

        boolean reuse = false;

        if ((getRGBRealType() == null)
                || !sequenceType.equals(getRGBRealType())) {
            setRGBRealType(sequenceType);

            reuse = false;
        }

        if (renderer != null) {
            renderer.setReUseFrames(reuse);
        }

        setData(sequence);
        */
    }

    /**
     * Get the DataRenderer used for this Displayable.
     *
     * @return  the DataRenderer
     */
    protected DataRenderer getDataRenderer() {

        if (getDisplay().getDisplayRenderer() instanceof DisplayRendererJ2D) {
            return new DefaultRendererJ2D();
        } else {
            if (myRenderer == null) {
                myRenderer = new ImageRendererJ3D();
                myRenderer.setReUseFrames(false);
                //renderer.setSetSetOnReUseFrames(false);
            }
            return myRenderer;
        }
    }

    /**
     * Set the color palette for the display
     *
     * @param colorPalette  colorPalette to use
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setColorPalette(float[][] colorPalette)
            throws RemoteException, VisADException {

        boolean reUseTrue = (myRenderer != null)
                            ? myRenderer.getReUseFrames()
                            : false;

        /// Warning = Major hack
        if (reUseTrue) {  // turn off reuse so color table will change
            myRenderer.setReUseFrames(false);
            super.setColorPalette(colorPalette);
            try {
                Thread.sleep(500);
            } catch (Exception excp) {}

            myRenderer.setReUseFrames(true);
        } else {
            super.setColorPalette(colorPalette);
        }
    }

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     *
     * @return                  A semi-deep clone of this instance.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Displayable cloneForDisplay()
            throws RemoteException, VisADException {
        return new ImageSequenceDisplayable(this);
    }

    /**
     * Override superclass method to avoid adding a ConstantMap.
     *
     * @param grid  grid to use to set the properties.  Can be null.
     *
     * @throws RemoteException   problem with remote data
     * @throws VisADException    problem with local data
     */
    public void setTextureProperties(FieldImpl grid)
            throws VisADException, RemoteException {}

    /**
     * Get whether this is textured or smoothed
     * @return true
     */
    public boolean getTextureEnable() {
        return true;
    }

    /**
     * Set reuse frames property
     * @param on  true to reuse the scene graphs
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setReUseScenes(boolean on)
            throws RemoteException, VisADException {
        if ((myRenderer != null)
                && (myRenderer instanceof ImageRendererJ3D)) {
            myRenderer.setReUseFrames(on);
        }
    }
}
