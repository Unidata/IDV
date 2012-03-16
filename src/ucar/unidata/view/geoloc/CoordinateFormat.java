package ucar.unidata.view.geoloc;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class CoordinateFormat {
	public enum Cardinality {
	    NORTH("N"),
	    SOUTH("S"),
	    EAST("E"),
	    WEST("W"),
	    NONE("");

	    private final String cardinality;

	    private Cardinality(final String cardinality) {
	        this.cardinality = cardinality;
	    }

	    @Override
	    public String toString() {
	        return cardinality;
	    }
	}	

	public enum DegMinSec {
	    DEGREE("d"),//Degree Symbol broken in VisAD
	    MINUTE("''"),
	    SECOND("\"");

	    private final String dms;

	    private DegMinSec(final String dms) {
	        this.dms = dms;
	    }

	    @Override
	    public String toString() {
	        return dms;
	    }
	}	
	
	public static String accuracy(final int accuracy) {
		if (accuracy == 0) {
			return "#";
		} else {
			final StringBuffer sb = new StringBuffer();
			for (int i = 0; i < accuracy; i++) {
				sb.append("#");
			}
			return "#." + sb.toString();
		}
	}
	
	public interface Format{
		public String format(double number);
	}
	
	public static class CoordFormat implements Format {
		private final int accuracy;		
		private final DegMinSec degminsec;
		public CoordFormat (final int accuracy, final DegMinSec degminsec) {
			this.accuracy = accuracy;
			this.degminsec = degminsec;
		}
		public CoordFormat (final DegMinSec degminsec) {
			this.accuracy = 0;
			this.degminsec = degminsec;
		}
		
		public String format(double number){
			NumberFormat formatter = new DecimalFormat(accuracy(accuracy) + degminsec);
			return formatter.format(number);
		}
	}
	
	public static class EmptyFormat implements Format {

		@Override
		public String format(double number) {
			return "";
		}
		
	}
	
	public static String convert(double coord, Format degF, Format minF, Format secF, Cardinality card){
		double minutes = ((coord - (int)coord) * 60);
		double seconds = ((minutes - (int)minutes) * 60);
		return degF.format(coord) + minF.format(minutes) + secF.format(seconds) + card;
	}
	
}
