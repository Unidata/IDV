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


import ucar.unidata.util.Misc;



import java.util.List;


/**
 * Class to hold information about a list of image datasets
 *
 * @author IDV development team
 * @version $Revision: 1.15 $
 */
public class ImageDataset {

    /** name of this dataset */
    String myName;

    /** list of data descriptors */
    List myDescriptors;

    /**
     *  Empty constructor for XML encoding
     */
    public ImageDataset() {}


    /**
     * Construct a new ImageDatset with the specified name
     * and a list of image descriptors.
     *
     * @param name              what is your name?
     * @param imageDescriptors  a List of decriptors to locate
     *                          the images.  These could be filenames,
     *                          URLS (ADDE or otherwise)
     */
    public ImageDataset(String name, List imageDescriptors) {
        myName        = name;
        myDescriptors = imageDescriptors;
    }

    /**
     * Get the name of this dataset.
     *
     * @return the dataset name
     */
    public String getDatasetName() {
        return myName;
    }


    /**
     * Set the name of this dataset.
     *
     * @param name   name for this dataset
     */
    public void setDatasetName(String name) {
        myName = name;
    }

    /**
     * Get the descriptors (locations) of the images
     * in this dataset.
     *
     * @return list of image descriptors
     */
    public List getImageDescriptors() {
        return myDescriptors;
    }


    /**
     * Set the descriptors (locations) of the images
     * in this dataset.
     *
     * @param d  the list of descriptors
     */
    public void setImageDescriptors(List d) {
        myDescriptors = d;
    }

    /**
     * Get the number of images in the dataset.
     *
     * @return number of descriptors
     */
    public int getNumImages() {
        return myDescriptors.size();
    }

    /**
     * Return a String representation of this object.
     *
     * @return  a String representation of this object
     */
    public String toString() {
        return myName;
    }

    /**
     * Check to see if this ImageDataset is equal to the object
     * in question.
     * @param o  object in question
     * @return true if they are the same or equivalent objects
     */
    public boolean equals(Object o) {
        if ( !(o instanceof ImageDataset)) {
            return false;
        }
        ImageDataset that = (ImageDataset) o;
        return (this == that)
               || (Misc.equals(that.myName, myName)
                   && Misc.equals(that.myDescriptors, myDescriptors));
    }

    /**
     * Get the hashcode for this object
     *
     * @return  the hash code
     */
    public int hashCode() {
        int hashCode = myName.hashCode();
        hashCode ^= myDescriptors.hashCode();
        return hashCode;
    }
}
