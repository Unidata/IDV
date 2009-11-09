
package ucar.visad.data;

import visad.*;
import visad.data.*;

/**
 * CachedGriddedSet extends GriddedSet mainly to use CachedGriddedSets
 * in the create method.
 */
public class CachedGriddedSet extends GriddedSet {

  /** construct a GriddedSet with samples */
  public CachedGriddedSet(MathType type, float[][] samples, int[] lengths)
         throws VisADException {
    this(type, samples, lengths, null, null, null, true);
  }

  /** construct a GriddedSet with samples and non-default CoordinateSystem */
  public CachedGriddedSet(MathType type, float[][] samples, int[] lengths,
                    CoordinateSystem coord_sys, Unit[] units,
                    ErrorEstimate[] errors) throws VisADException {
    this(type, samples, lengths, coord_sys, units, errors, true);
  }

  public CachedGriddedSet(MathType type, float[][] samples, int[] lengths,
             CoordinateSystem coord_sys, Unit[] units,
             ErrorEstimate[] errors, boolean copy)
             throws VisADException {
    super(type, samples, lengths, coord_sys, units, errors, copy);
  }

  /**
   * General Factory method for creating the proper gridded set
   * (Gridded1DSet, Gridded2DSet, etc.).
   *
   * @param type                 MathType for the returned set
   * @param samples              Set samples
   * @param lengths              The dimensionality of the manifold.  <code>
   *                             lengths[i]</code> contains the number of points
   *                             in the manifold for dimension <code>i</code>.
   * @param coord_sys            CoordinateSystem for the GriddedSet
   * @param units                Unit-s of the values in <code>samples</code>
   * @param errors               ErrorEstimate-s for the values
   * @param copy                 make a copy of the samples
   * @param test                 test to make sure samples are valid.  Used
   *                             for creating Gridded*DSets where the 
   *                             manifold dimension is equal to the domain
   *                             dimension
   * @throws VisADException      problem creating the set
   */
    public static GriddedSet create(MathType type, float[][] samples,
                                   int[] lengths, CoordinateSystem coord_sys,
                           Unit[] units, ErrorEstimate[] errors,
                           boolean copy, boolean test)
          throws VisADException {
    int domain_dimension = samples.length;
    int manifold_dimension = lengths.length;
    if (manifold_dimension > domain_dimension) {
      throw new SetException("GriddedSet.create: manifold_dimension " +
                             manifold_dimension + " is greater than" +
                             " domain_dimension " + domain_dimension);
    }

    switch (domain_dimension) {
      case 1:
        return new Gridded1DSet(type, samples,
                                lengths[0],
                                coord_sys, units, errors, copy);
      case 2:
        if (manifold_dimension == 1) {
          return new CachedGridded2DSet(type, samples,
                                  lengths[0],
                                  coord_sys, units, errors, copy);
        }
        else {
          return new CachedGridded2DSet(type, samples,
                                  lengths[0], lengths[1],
                                  coord_sys, units, errors, copy, test);
        }
      case 3:
        if (manifold_dimension == 1) {
          return new CachedGridded3DSet(type, samples,
                                  lengths[0],
                                  coord_sys, units, errors, copy);
        }
        else if (manifold_dimension == 2) {
          return new CachedGridded3DSet(type, samples,
                                  lengths[0], lengths[1],
                                  coord_sys, units, errors, copy);
        }
        else {
          return new CachedGridded3DSet(type, samples,
                                  lengths[0], lengths[1], lengths[2],
                                  coord_sys, units, errors, copy, test);
        }
      default:
        return new GriddedSet(type, samples,
                              lengths,
                              coord_sys, units, errors, copy);
    }
  }

}

