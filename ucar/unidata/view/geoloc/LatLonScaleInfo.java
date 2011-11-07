package ucar.unidata.view.geoloc;

/**
 * Struct containing Latitude / Longitude scale information.
 */
public class LatLonScaleInfo {
    
	/** The abscissa label. */
    public String abscissaLabel = "Latitude"; //Default
    
    /** The major tick spacing. */
    public int majorTickSpacing = 4;

    /** The minor tick spacing. */
    public int minorTickSpacing = 8;

    /**
     * Instantiates a new lat lon scale info.
     */
    public LatLonScaleInfo() {}

    /**
     * Instantiates a new lat lon scale info.
     *
     * @param abscissaLabel the abscissa label
     * 
     * @param majorTickSpacing the major tick spacing
     * 
     * @param minorTickSpacing the minor tick spacing
     */
    public LatLonScaleInfo(String abscissaLabel, int majorTickSpacing, int minorTickSpacing) {
		super();
		this.abscissaLabel = abscissaLabel;
		this.majorTickSpacing = majorTickSpacing;
		this.minorTickSpacing = minorTickSpacing;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((abscissaLabel == null) ? 0 : abscissaLabel.hashCode());
		result = prime * result + majorTickSpacing;
		result = prime * result + minorTickSpacing;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LatLonScaleInfo other = (LatLonScaleInfo) obj;
		if (abscissaLabel == null) {
			if (other.abscissaLabel != null)
				return false;
		} else if (!abscissaLabel.equals(other.abscissaLabel))
			return false;
		if (majorTickSpacing != other.majorTickSpacing)
			return false;
		if (minorTickSpacing != other.minorTickSpacing)
			return false;
		return true;
	}    
    
}
