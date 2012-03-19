package ucar.unidata.view.geoloc;

//~--- JDK imports ------------------------------------------------------------

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * The CoordinateFormat class to format geo coordinates.
 */
public class CoordinateFormat {

    /** Empty format. */
    public static final Format EMPTY_FORMAT = new EmptyFormat();

    /**
     * The Cardinality enum.
     */
    public enum Cardinality {

        /** NORTH. */
        NORTH("N"),

        /** SOUTH. */
        SOUTH("S"),

        /** EAST. */
        EAST("E"),

        /** WEST. */
        WEST("W"),

        /** NONE. */
        NONE("");

        /** The cardinality. */
        private final String cardinality;

        /**
         * Instantiates a new cardinality.
         *
         * @param cardinality the cardinality
         */
        private Cardinality(final String cardinality) {
            this.cardinality = cardinality;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return cardinality;
        }
    }

    /**
     * The Degrees Minutes Seconds enum.
     */
    public enum DegMinSec {

        /** Degrees. */
        DEGREE("¡"),

        /** Minutes. */
        MINUTE("''"),

        /** Seconds. */
        SECOND("\""),

        /** Colon. */
        COLON(":"),

        /** None. */
        NONE("");

        /** The dms. */
        private final String dms;

        /**
         * Instantiates a new deg min sec.
         *
         * @param dms the dms
         */
        private DegMinSec(final String dms) {
            this.dms = dms;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return dms;
        }
    }

    /**
     * Accuracy.
     *
     * @param accuracy the accuracy
     * @return the format string
     */
    private static String accuracy(final int accuracy) {
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

    /**
     * Convert a decimal coordinate.
     *
     * @param coord the decimal coord
     * @param degF the deg format
     * @param minF the min format
     * @param secF the sec format
     * @param card the cardinality
     * @return the formatted coordinate string
     */
    public static String convert(double coord, Format degF, Format minF, Format secF, Cardinality card) {
        double minutes = ((coord - (int) coord) * 60.0d);
        double seconds = ((minutes - (int) minutes) * 60.0d);

        return degF.format(coord) + minF.format(minutes) + secF.format(seconds) + card;
    }

    /**
     * The Format interface.
     */
    public interface Format {

        /**
         * Format the number.
         *
         * @param number to be formatted
         * @return the formatted number as string
         */
        public String format(double number);
    }


    /**
     * The DecimalCoordFormat.
     */
    public static class DecimalCoordFormat implements Format {

        /** The decimal accuracy. */
        private final int accuracy;

        /** The degrees minutes seconds. */
        private final DegMinSec degminsec;

        /**
         * Instantiates a new coord format.
         *
         * @param accuracy the decimal accuracy
         * @param degminsec the degminsec
         */
        public DecimalCoordFormat(final int accuracy, final DegMinSec degminsec) {
            this.accuracy  = accuracy;
            this.degminsec = degminsec;
        }

        /**
         * {@inheritDoc}
         */
        public String format(double number) {
            NumberFormat formatter = new DecimalFormat(accuracy(accuracy) + degminsec);

            return formatter.format(number);
        }
    }


    /**
     * The empty format.
     */
    private static class EmptyFormat implements Format {

        /**
         * {@inheritDoc}
         */
        @Override
        public String format(double number) {
            return "";
        }
    }


    /**
     * The FloorCoordFormat class. Useful when you simply want to lop off anything after the decimal point.
     */
    public static class FloorCoordFormat implements Format {

        /** The degrees minutes seconds. */
        private final DegMinSec degminsec;

        /**
         * Instantiates a new floor coord format.
         *
         * @param degminsec the degminsec
         */
        public FloorCoordFormat(final DegMinSec degminsec) {
            this.degminsec = degminsec;
        }

        /**
         * {@inheritDoc}
         */
        public String format(double number) {
            NumberFormat formatter = new DecimalFormat(accuracy(0) + degminsec);

            return formatter.format((int) number);
        }
    }
}
