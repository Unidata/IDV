package ucar.unidata.view.geoloc;

import ucar.unidata.geoloc.Earth;
import ucar.unidata.geoloc.EarthEllipsoid;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.projection.LatLonProjection;

public class EditableLatLon extends LatLonProjection {

    private double latitude0;

    private double longitude0;

    private double latitude1;

    private double longitude1;

    private Earth earth;

    private double centerLon;

    /**
     * Label getter
     * @return
     */
    public String getProjectionTypeLabel() {
        return "Editable Lat/Lon";
    }

    /**
     * Resize ProjectionRect based on the params
     */
	private boolean suspendResize = false;

    private void resize() {
        if (suspendResize) {
            return;
        }

        double x0 = longitude0;
        double x1 = longitude1;

        // detect dateline crossing
        boolean crossesDateline = Math.abs(x1 - x0) > 180;

        if (crossesDateline) {
            // convert negative side into continuous space
            if (x0 < 0) x0 += 360;
            if (x1 < 0) x1 += 360;
        }

        double minX = Math.min(x0, x1);
        double maxX = Math.max(x0, x1);

        this.defaultMapArea =
            new ProjectionRect(minX, latitude0, maxX, latitude1);
    }

    private double normalizeLon(double lon) {
        return ((lon + 180) % 360 + 360) % 360 - 180;
    }

    /**
     * Getter for latitude0
     * @return latitude0
     */
    public double getUpperLeftLatitude() {
        return this.latitude0;
    }

    /**
     * Getter for latitude1
     * @return
     */
    public double getLowerRightLatitude() {
        return this.latitude1;
    }

    /**
     * Getter for longitude0
     * @return
     */
    public double getUpperLeftLongitude() {
        return this.longitude0;
    }

    /**
     * Getter for longitude1
     * @return
     */
    public double getLowerRightLongitude() {
        return this.longitude1;
    }

    /**
     * Setter for latitude0
     * @param latitude0
     */
    public void setUpperLeftLatitude(double latitude0) {
        this.latitude0 = validateLatitude(latitude0);
        resize();
    }

    /**
     * Setter for latitude1
     * @param latitude1
     */
    public void setLowerRightLatitude(double latitude1) {
        this.latitude1 = validateLatitude(latitude1);
        resize();
    }

    /**
     * Setter for longitude0
     * @param longitude0
     */
    public void setUpperLeftLongitude(double longitude0) {
        this.longitude0 = validateLongitude(longitude0);
        resize();
    }

    /**
     * Setter for longitude1
     * @param longitude1
     */
    public void setLowerRightLongitude(double longitude1) {
        this.longitude1 = validateLongitude(longitude1);
        resize();
    }

    /**
     * toString for params
     * @return params as a string
     */
    public String paramsToString() {
        return "LatLon{latitude0=" + this.latitude0 + ", longitude0=" + this.longitude0 + ", latitude1=" + this.latitude1 + ", longitude1=" + this.longitude1+"}";
    }

    /**
     * toString override
     * @return params as a string
     */
    public String toString() {
        return paramsToString();
    }

    /**
     * Taken from ucar.unidata.geoloc.projection.LatLonProjection
     * @return
     */
    public ProjectionImpl constructCopy() {
        EditableLatLon result = new EditableLatLon(this.getName(), this.getDefaultMapArea());

        result.setName(this.name);
        result.earth = this.earth;

        // copy editable fields so they are retained when editing projection
        result.setUpperLeftLatitude(this.latitude0);
        result.setUpperLeftLongitude(this.longitude0);
        result.setLowerRightLatitude(this.latitude1);
        result.setLowerRightLongitude(this.longitude1);

        result.setCenterLon(this.centerLon);

        result.setDefaultMapArea(this.defaultMapArea);

        return result;
    }

    private double validateLatitude(double lat) {
        if (lat < -90.0 || lat > 90.0) {
            throw new IllegalArgumentException(
                    "Latitude must be between -90 and 90"
            );
        }
        return lat;
    }

    private double validateLongitude(double lon) {
        if (lon < -180.0 || lon > 180.0) {
            throw new IllegalArgumentException(
                    "Longitude must be between -180 and 180"
            );
        }
        return lon;
    }

    /**
     * Constructor
     */
    public EditableLatLon() {
        super("Editable LatLon");
        this.earth = EarthEllipsoid.DEFAULT;
        this.defaultMapArea = new ProjectionRect(-90, -45, 90, 45);
        this.addParameter("latitude0", latitude0);
        this.addParameter("longitude0", longitude0);
        this.addParameter("latitude1", latitude1);
        this.addParameter("longitude1", longitude1);
        // new ProjectionRect(-90, -45, 90, 45)
    }

    /**
     * Constructor
     * @param earth
     */
    public EditableLatLon(Earth earth) {
        super("Editable LatLon");
        this.earth = earth;
        this.defaultMapArea = new ProjectionRect(-90, -45, 90, 45);
        this.addParameter("latitude0", latitude0);
        this.addParameter("longitude0", longitude0);
        this.addParameter("latitude1", latitude1);
        this.addParameter("longitude1", longitude1);

    }

    /**
     * Constructor
     * @param name
     */
    public EditableLatLon(String name) {
        super(name);
        this.name = name;
        this.earth = EarthEllipsoid.DEFAULT;
        this.defaultMapArea = new ProjectionRect(-90, -45, 90, 45);
        this.addParameter("latitude0", latitude0);
        this.addParameter("longitude0", longitude0);
        this.addParameter("latitude1", latitude1);
        this.addParameter("longitude1", longitude1);

    }

    /**
     * Constructor
     * @param name
     * @param defaultMapArea
     */
    public EditableLatLon(String name, ProjectionRect defaultMapArea) {
        super(name, defaultMapArea);
        this.name = name;
        this.earth = EarthEllipsoid.DEFAULT;
        this.defaultMapArea = defaultMapArea;
        this.addParameter("latitude0", latitude0);
        this.addParameter("longitude0", longitude0);
        this.addParameter("latitude1", latitude1);
        this.addParameter("longitude1", longitude1);

    }
}