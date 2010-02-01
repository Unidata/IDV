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

package ucar.unidata.data.imagery;



import edu.wisc.ssec.mcidas.AreaDirectory;
import edu.wisc.ssec.mcidas.AreaDirectoryList;

import org.w3c.dom.Element;

import ucar.unidata.data.DataSource;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.WrapperException;

import ucar.unidata.xml.XmlDelegate;
import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlPersistable;

import visad.DateTime;
import visad.VisADException;

import java.io.File;

import java.util.ArrayList;

import java.util.regex.*;


/**
 * A class to hold an image directory + the location
 *
 * @author IDV Development Team
 * @version $Revision: 1.27 $
 */
public class AddeImageDescriptor implements Comparable, XmlPersistable,
                                            XmlDelegate {

    /** AreaDirectory for this image */
    AreaDirectory myDirectory;

    /** AreaDirectory for this image */
    AddeImageInfo myInfo = null;

    /** ADDE URL */
    String mySource;

    /** time of the image */
    DateTime myTime = null;

    /** relative time index */
    int relativeIndex = -1;

    /**
     *  Does this descriptor represent relative or absolute times.
     */
    boolean isRelative = false;

    /**
     * Default constructor for unpersistence; does nothing
     *
     */
    public AddeImageDescriptor() {}

    /**
     * Create a descriptor from another, but change the relative index
     *
     * @param relativeIndex    new relative index
     * @param that             other image descriptor
     *
     */
    public AddeImageDescriptor(int relativeIndex, AddeImageDescriptor that) {
        this.myDirectory   = that.myDirectory;
        this.mySource      = that.mySource;
        this.relativeIndex = relativeIndex;
        this.myInfo        = that.myInfo;
        this.isRelative    = true;
    }


    /**
     * Create a descriptor from another.
     *
     * @param that   other image descriptor
     *
     */
    public AddeImageDescriptor(AddeImageDescriptor that) {
        this.myDirectory   = that.myDirectory;
        this.mySource      = that.mySource;
        this.isRelative    = that.isRelative;
        this.relativeIndex = that.relativeIndex;
        if (that.myTime != null) {
            try {
                this.myTime = new DateTime(that.myTime);
            } catch (Exception exc) {
                this.myTime = that.myTime;
            }
        }
        this.myInfo = that.myInfo;
    }

    /**
     * Create an image descriptor from the source
     *
     * @param imageSource   ADDE URL
     */
    public AddeImageDescriptor(String imageSource) {
        mySource = imageSource;
        Pattern pattern = Pattern.compile("pos=(-?[0-9]+)");
        Matcher matcher = pattern.matcher(imageSource.toLowerCase());
        if (matcher.find()) {
            String pos = matcher.group(1);
            this.relativeIndex = Math.abs(new Integer(pos).intValue());
            isRelative         = true;
        }

        processSource();
    }

    /**
     * Create an image descriptor from the source and AreaDirectory
     *
     * @param directory          image metadata
     * @param imageSource        ADDE URL of image
     *
     */
    public AddeImageDescriptor(AreaDirectory directory, String imageSource) {
        this(directory, imageSource, null);
    }

    /**
     * Create an image descriptor from the source and AreaDirectory
     *
     * @param directory          image metadata
     * @param imageSource        ADDE URL of image
     * @param info               Image info
     *
     */
    public AddeImageDescriptor(AreaDirectory directory, String imageSource,
                               AddeImageInfo info) {
        myDirectory = directory;
        mySource    = imageSource;
        myInfo      = info;
        setTimeFromDirectory();
    }

    /**
     * Process the source to create the image directory
     */
    private void processSource() {
        //Try it as a file first or as a URL
        if (IOUtil.isHttpProtocol(mySource) || isFromFile()) {
            myDirectory = processSourceAsFile(mySource);
        } else {
            myDirectory = processSourceAsAddeUrl(mySource);
        }
        setTimeFromDirectory();
    }


    /**
     * Does this represent a file
     *
     * @return Is it a file
     */
    protected boolean isFromFile() {
        return new File(mySource).exists();
    }

    /**
     * Set the time from the image directory.
     */
    private void setTimeFromDirectory() {
        if (myDirectory != null) {
            try {
                myTime = new DateTime(myDirectory.getNominalTime());
            } catch (VisADException ve) {
                myTime = null;
            }
        }
    }

    /**
     * Create an {@link AreaDirectory} from the ADDE URL.   If the URL
     * is a data request (imagedata) then munge it to make an imagedir
     * request.
     *
     * @param imageSource   ADDE URL
     * @return  corresponding image metadata
     */
    private AreaDirectory processSourceAsAddeUrl(String imageSource) {
        int imageDataIndex = imageSource.indexOf("imagedata?");
        int imageDirIndex  = imageSource.indexOf("imagedir");
        if ((imageDataIndex <= 0) && (imageDirIndex <= 0)) {
            throw new IllegalArgumentException("invalid ADDE image URL:"
                    + imageSource);
        }
        int questionMark = imageSource.indexOf("?");
        try {
            StringBuffer buf = new StringBuffer();
            if (imageDataIndex > 0) {
                buf.append(imageSource.substring(0, imageDataIndex));
                buf.append("imagedir");
                buf.append(imageSource.substring(questionMark));
                mySource = imageSource;
                return (AreaDirectory) new AreaDirectoryList(
                    buf.toString()).getDirs().get(0);
            } else {
                buf.append(imageSource.substring(0, imageDirIndex));
                buf.append("imagedata");
                buf.append(imageSource.substring(questionMark));
                mySource = buf.toString();
                return (AreaDirectory) new AreaDirectoryList(
                    imageSource).getDirs().get(0);
            }
        } catch (Exception me) {
            throw new WrapperException("Invalid ADDE URL:" + mySource, me);
        }
    }


    /**
     * Process this as a local file
     *
     * @param imageSource  local file name
     * @return  image metadata
     */
    private AreaDirectory processSourceAsFile(String imageSource) {
        try {
            AreaDirectoryList adl = new AreaDirectoryList(imageSource);
            return adl.getSortedDirs()[0][0];
        } catch (Exception me) {
            throw new WrapperException("Invalid AREA file:" + imageSource,
                                       me);
        }

    }


    /**
     * We just have this here so we can define a XmlDelegate for
     * the AreaDirectory class
     *
     * @param encoder  encoder to use
     * @return  encoded version of this
     */
    public Element createElement(XmlEncoder encoder) {
        encoder.addDelegateForClass(AreaDirectory.class, this);
        return encoder.createElementDontCheckPersistable(this);
    }

    /**
     * Create an image descriptor from the XML element
     *
     * @param encoder    encoder to use
     * @param element    XML description of this object
     * @return  image description based on the element
     */
    public Object createObject(XmlEncoder encoder, Element element) {
        //The false implies don't check the delegate
        return encoder.createObjectDontCheckDelegate(element);
    }



    /**
     *  DO nothing. Just here for the XmlPersistable interface.
     *
     * @param encoder   encoder for XML
     * @param element   XML element
     * @return  true
     */
    public boolean initFromXml(XmlEncoder encoder, Element element) {
        return true;
    }

    /**
     * This is the delegate method for AreaDirectory
     *
     * @param encoder    XML encoder for this object
     * @param object     an AddeImageDescriptor
     * @return   XML representation
     */
    public Element createElement(XmlEncoder encoder, Object object) {
        AreaDirectory directory = (AreaDirectory) object;
        ArrayList     arguments = new ArrayList();
        arguments.add(directory.getDirectoryBlock());
        Element result      = encoder.createObjectElement(object.getClass());
        Element ctorElement = encoder.createConstructorElement(arguments);
        result.appendChild(ctorElement);
        return result;
    }


    /**
     * Get the AreaDirectory (used by XML persistence)
     *
     * @return  this objects image metadata
     */
    public AreaDirectory getDirectory() {
        return myDirectory;
    }

    /**
     * Set the AreaDirectory (used by XML persistence)
     *
     * @param d   image metadata
     */
    public void setDirectory(AreaDirectory d) {
        myDirectory = d;
        setTimeFromDirectory();
    }

    /**
     * Get the source of this image  (used by persistence)
     *
     * @return  ADDE URL
     */
    public String getSource() {
        return mySource;
    }

    /**
     * Set the source of this image  (used by persistence)
     *
     * @param s  the ADDE URL for this image
     */
    public void setSource(String s) {
        mySource = s;
    }

    /**
     * Get the image info of this image  (used by persistence)
     *
     * @return  AddeImageInfo
     */
    public AddeImageInfo getImageInfo() {
        return myInfo;
    }

    /**
     * Set the image info of this image  (used by persistence)
     *
     * @param s  the AddeImageInfo for this image
     */
    public void setImageInfo(AddeImageInfo s) {
        myInfo = s;
    }

    /**
     *  Set the IsRelative property.
     *
     *  @param value The new value for IsRelative
     */
    public void setIsRelative(boolean value) {
        isRelative = value;
    }

    /**
     *  Get the IsRelative property.
     *
     *  @return The IsRelative property
     */
    public boolean getIsRelative() {
        return isRelative;
    }

    /**
     *  Set the RelativeIndex property.
     *
     *  @param value The new value for RelativeIndex
     */
    public void setRelativeIndex(int value) {
        relativeIndex = value;
    }

    /**
     *  Get the RelativeIndex property.
     *
     *  @return The RelativeIndex
     */
    public int getRelativeIndex() {
        return relativeIndex;
    }




    /**
     * Get the time of the image
     * @return  nominal time of the image (may be null)
     */
    public DateTime getImageTime() {
        return (isRelative
                ? null
                : myTime);
    }


    /**
     * Get a text representation of this object
     * @return  representation as a String
     */
    public String toString() {
        if (isRelative) {
            if (relativeIndex == 0) {
                return "Most recent image";
            }
            if ((relativeIndex >= 0)
                    && (relativeIndex < DataSource.ordinalNames.length)) {
                return DataSource.ordinalNames[relativeIndex]
                       + " most recent";
            }
            return (relativeIndex + 1) + "th most recent";
        }
        return (myTime != null)
               ? myTime.toString()
               : myDirectory.getNominalTime().toString();
    }

    /**
     * Implementation for Comparable.
     *
     * @param o   object in question
     * @return  comparison
     * @see java.lang.Comparable
     */
    public int compareTo(Object o) {
        AddeImageDescriptor aid = (AddeImageDescriptor) o;
        AreaDirectory       ad  = aid.getDirectory();
        return myDirectory.getNominalTime().compareTo(ad.getNominalTime());
    }

    /**
     * See if this AddeImageDescriptor is equal to the object in
     * question
     *
     * @param o   object in question
     * @return  true if <code>o</code> is an AddeImageDescriptor and
     *          they area equivalent
     */
    public boolean equals(Object o) {
        if ( !(o instanceof AddeImageDescriptor)) {
            return false;
        }
        AddeImageDescriptor that = (AddeImageDescriptor) o;
        return Misc.equals(that.myDirectory, myDirectory)
               && Misc.equalsIgnoreCase(mySource, that.mySource);
    }
}
