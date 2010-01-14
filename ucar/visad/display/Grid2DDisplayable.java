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


import ucar.unidata.data.grid.GridUtil;

import ucar.visad.display.*;

import visad.*;

import visad.bom.ImageRendererJ3D;

import visad.java2d.*;

import visad.java3d.*;

import visad.meteorology.*;

import visad.util.DataUtility;

import java.rmi.RemoteException;

import java.util.*;



/**
 * A class to support showing 2D gridded data as color on a plane
 * in a DisplayMaster.
 *
 * @author Stuart Wier
 * @version $Revision: 1.33 $
 */
public class Grid2DDisplayable extends RGBDisplayable implements GridDisplayable {

    /** data */
    private Field field;

    /** renderer */
    private DataRenderer myRenderer;

    /** flag for texturing */
    private boolean isTextured = false;

    /** flag for missing values as transparent */
    private boolean isMissingTransparent = true;

    /** use the default scheme for texturing */
    private boolean useDefault = false;

    /** threshold for number of points for texturing */
    private int THRESHOLD = 500000;

    /** threshold for number of points for texturing */
    private int curvedSize = 10;

    /**
     * Constructs an instance with the supplied name.
     *
     * @param name a String identifier
     * @param alphaflag boolean flag whether to use transparency
     * @exception VisADException  from construction of super class
     * @exception RemoteException from construction of super class
     */
    public Grid2DDisplayable(String name, boolean alphaflag)
            throws VisADException, RemoteException {
        this(name, null, null, alphaflag);
    }

    /**
     * Constructs from a name for the Displayable and the type of the
     * RGB parameter.
     *
     * @param name              The name for the displayable.
     * @param rgbRealType       The type of the RGB parameter.  May be
     *                          <code>null</code>.
     * @param colorPalette      The initial colorPalette to use. May be
     *                          <code>null</code> (Vis5D palette used
     *                          as default).
     * @param alphaflag         boolean - use Display.RBGA if true
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Grid2DDisplayable(String name, RealType rgbRealType,
                             float[][] colorPalette, boolean alphaflag)
            throws VisADException, RemoteException {
        super(name, rgbRealType, colorPalette, alphaflag);
    }

    /**
     * Set the data into the Displayable
     *
     * @param field a VisAD FlatField with a 2D nature
     * @exception VisADException  from construction of VisAd objects
     * @exception RemoteException from construction of VisAD objects
     * @deprecated Should use setData now.
     */
    public void setGrid2D(FieldImpl field)
            throws VisADException, RemoteException {
        loadData(field);
    }

    /**
     * Constructs from another instance.  The following attributes are set from
     * the other instance: color palette, the color RealType.
     * @param that              The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected Grid2DDisplayable(Grid2DDisplayable that)
            throws VisADException, RemoteException {

        super(that);
        field                = that.field;  // copy the data
        isTextured           = that.isTextured;
        isMissingTransparent = that.isMissingTransparent;
        useDefault           = that.useDefault;
        setTextureProperties((FieldImpl) field);
    }

    /**
     * Set the data into the Displayable; set RGB Type
     *
     * @param field a VisAD FlatField with a 2D nature
     * @exception VisADException  from construction of VisAd objects
     * @exception RemoteException from construction of VisAD objects
     */
    public void loadData(FieldImpl field)
            throws VisADException, RemoteException {

        // get the RealType of the range from the FlatField
        TupleType tt       = GridUtil.getParamType(field);
        RealType  ffldType = tt.getRealComponents()[0];

        if ((getRGBRealType() == null)
                || !ffldType.equals(getRGBRealType())) {
            setRGBRealType(ffldType);
        }

        this.field = field;
        setTextureProperties(field);

        setData(this.field);
    }

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     * @return                  A semi-deep clone of this instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Displayable cloneForDisplay()  // revise
            throws RemoteException, VisADException {
        return new Grid2DDisplayable(this);
    }

    /**
     * Obtains the DataRenderer for this displayable.
     *
     * @return                    The DataRenderer for this displayable.
     * @throws VisADException     VisAD failure.
     */
    protected DataRenderer getDataRenderer() throws VisADException {

        try {

            if (getDisplay().getDisplayRenderer()
                    instanceof DisplayRendererJ2D) {
                myRenderer = new DefaultRendererJ2D();
            } else {
                if ((getData() != null)
                        && ImageRendererJ3D.isImageType(getData().getType())
                        && getTextureEnable() && !getUseDefaultRenderer()
                        && !getUseRGBTypeForSelect()) {

                    myRenderer = new visad.bom.ImageRendererJ3D();
                    ((visad.bom.ImageRendererJ3D) myRenderer).setReUseFrames(
                        false);
                } else {
                    myRenderer = new DefaultRendererJ3D();
                }
            }
        } catch (RemoteException re) {
            myRenderer = super.getDataRenderer();
        }
        //System.out.println("myRenderer = " + myRenderer.getClass().getName());
        return myRenderer;
    }

    /**
     * Set whether this should be shown as color shading or
     * as a texture map.
     *
     * @param enable  true to enable texturing
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public void setTextureEnable(boolean enable)
            throws VisADException, RemoteException {
        isTextured = enable;
        setTextureProperties(null);
    }

    /**
     * Adds this instance's data references to the associated VisAD display if
     * possible.  This method does not verify that the VisAD display has been
     * set.
     *
     * @param newMaps new maps
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void myAddConstantMaps(ConstantMap[] newMaps)
            throws VisADException, RemoteException {
        if (myRenderer != null) {
            String c = myRenderer.getClass().getName();
            if ((c.equals("visad.bom.ImageRendererJ3D") && !isTextured)
                    || (c.equals("visad.java3d.DefaultRendererJ3D")
                        && !isTextured)) {

                setDisplayInactive();
                removeDataReferences();
                addDataReferences();
                setDisplayActive();
            } else {
                super.myAddConstantMaps(newMaps);
            }
        } else {
            super.myAddConstantMaps(newMaps);
        }
    }


    /**
     * Create a ConstantMap for texture mapping
     *
     * @param enable  true to enable texture mapping
     *
     * @return  ConstantMap for Texture mapping based on enable
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    protected ConstantMap makeTextureMap(boolean enable)
            throws VisADException, RemoteException {
        return new ConstantMap((enable)
                               ? 1.0
                               : 0.0, Display.TextureEnable);
    }

    /**
     *  Get whether this is textured or smoothed
     * @return true is textured
     */
    public boolean getTextureEnable() {
        return isTextured;
    }

    /**
     * Set whether missing values should be shown as transparent or not
     * @param enable  true enable missing transparency
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public void setMissingTransparent(boolean enable)
            throws VisADException, RemoteException {
        isMissingTransparent = enable;
        addConstantMap(new ConstantMap((enable)
                                       ? 1.0
                                       : 0.0, Display.MissingTransparent));
    }

    /**
     *  Get whether this is textured or smoothed
     * @return true is textured
     */
    public boolean getMissingTransparent() {
        return isMissingTransparent;
    }

    /**
     * Set whether to always use default renderer instead of specialized
     * one.
     * @param use   true to always use the default renderer
     */
    public void setUseDefaultRenderer(boolean use) {
        useDefault = use;
    }

    /**
     * Get whether to always use default renderer instead of specialized
     * one.
     * @return true to always use the default renderer
     */
    public boolean getUseDefaultRenderer() {
        return useDefault;
    }

    /**
     * A method that subclasses can call to set the texture properties on
     * a <code>Grid2DDisplayable</code>.  Checks to see if the grid is
     * too big to not do texture mapping.
     * @param grid  grid to use to set the properties.  Can be null.
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public void setTextureProperties(FieldImpl grid)
            throws VisADException, RemoteException {

        FieldImpl fi = (grid == null)
                       ? (FieldImpl) getData()
                       : grid;
        // can't determine anything
        if (fi == null) {
            return;
        }

        int dfltCurvedSize = curvedSize;
        /*
        if (getDisplay() != null) {
            dfltCurvedSize =
                getDisplay().getGraphicsModeControl().getCurvedSize();
        }
        */

        float dataSize = (float) GridUtil.getSpatialDomain(fi).getLength();
        ConstantMapSet maps = new ConstantMapSet();
        if ((dataSize > THRESHOLD) && !isTextured && false) {  // TODO: figure out what breaks when we don't do this!
            int curvedSize = Math.min(dfltCurvedSize,
                                      Math.max((int) ((THRESHOLD / dataSize)
                                          * 10.), 3));
            maps.put(makeCurvedSizeMap(curvedSize));
            maps.put(makeTextureMap(true));

        } else {
            if (isTextured) {
                maps.put(makeCurvedSizeMap(dfltCurvedSize));
            }
            maps.put(makeTextureMap(isTextured));
        }
        addConstantMaps(maps.getConstantMaps());
    }

    /**
     * Set whether this GridDisplayable should have the data colored
     * by another parameter.  This implementation is a no-op.
     *
     * @param yesno true if colored by another
     */
    public void setColoredByAnother(boolean yesno) {}

    /**
     * Set the default texture curve size
     * @param size  size of texture
     */
    public void setCurvedSize(int size) {
        curvedSize = size;
        try {
            setTextureProperties(null);
        } catch (Exception e) {
            System.err.println("couldn't set curved size");
        }
    }

    /**
     * Get the default texture curve size
     * @return size of curved texture
     */
    public int getCurvedSize() {
        return curvedSize;
    }
}
