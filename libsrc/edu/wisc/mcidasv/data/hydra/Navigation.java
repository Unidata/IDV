package edu.wisc.ssec.mcidasv.data.hydra;

import visad.CoordinateSystem;
import visad.Linear2DSet;


public interface Navigation {

    public CoordinateSystem getVisADCoordinateSystem(Object subset) throws Exception;

    public double[] getEarthLocOfDataCoord(int[] coord) throws Exception;

}