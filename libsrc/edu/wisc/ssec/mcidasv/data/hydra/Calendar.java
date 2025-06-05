package edu.wisc.ssec.mcidasv.data.hydra;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.io.PrintStream;

public final class Calendar {
    /**
     * Nombre de millisecondes dans une journée. Cette constante est
     * utilisée pour convertir des intervalles de temps du Java en
     * nombre de jours.
     */
    private static final double MILLIS_IN_DAY = 1000*60*60*24;

    /**
     * Jour julien correspondant à l'époch du Java (1er janvier 1970 à minuit).
     * Cette constante est utilisée pour convertir des dates du Java en jour
     * julien.
     *
     * La valeur {@link #julianDay}   du 1er janvier 2000 00:00 GMT est 2451544.5 jours.
     * La valeur {@link Date#getTime} du 1er janvier 2000 00:00 GMT est 10957 jours.
     */
    private static final double JULIAN_DAY_1970 = 2451544.5-10957;

    /**
     * Do not allow instantiation of this class.
     */
    private Calendar() {
    }

    /**
     * Returns the julian day of the given date. This is the number of days elapsed since
     * January 1st, 4713 before J.C. at noon GMT. This is named after <cite>Julius Scaliger</cite>,
     * not to be confused to the number of days elapsed since the beginning of the year (named after
     * <cite>Julius Caesar</cite>).
     *
     * @param  time The time for which to evaluate the julian day.
     * @return Number of days elapsed since January 1st, 4713 before J.C. at noon GMT.
     */
    public static double julianDay(final Date time) {
        return julianDay(time.getTime());
    }

    /**
     * Computes the {@linkplain #julianDay(Date) julian day}.
     *
     * @param time The date in milliseconds elapsed since January 1st, 1970.
     */
    static double julianDay(final long time) {
        return (time/MILLIS_IN_DAY) + JULIAN_DAY_1970;
    }

    /**
     * Retourne le nombre de siècles écoulés depuis le 1 janvier 2000 à midi.
     * Cette information est utilisée dans les formules de Laskar (1986) pour
     * calculer la longueur d'une année tropicale, ainsi que par Chapront-Touze
     * et Chapront (1988) pour la longueur d'un mois synodique.
     */
    static double julianCentury(final Date time) {
        return ((time.getTime() / MILLIS_IN_DAY) + (JULIAN_DAY_1970 - 2451545.0)) / 36525;
    }

    /**
     * Retourne la longueur de l'année tropicale. L'année tropicale est définie comme l'intervalle
     * moyen entre deux équinoxes vernales (autour du 21 mars dans l'hémisphère nord). Il correspond
     * au cycle des saisons. Cet intervalle de temps est une <strong>moyenne</strong>. Un cycle réel
     * peut s'écarter de plusieurs minutes de cette moyenne. Notez aussi qu'une année tropicale
     * n'est pas identique à une année sidérale, qui est le temps requis par la Terre pour compléter
     * un orbite autour du Soleil. En l'an 2000, l'année tropicale avait une longueur d'environ
     * 365,2422 jours tandis que l'année sidérale avait une longueur de 365,2564 jours.
     *
     * @param  time A date in the year for which to compute the length.
     * @return The tropical length of the given year.
     */
    public static double tropicalYearLength(final Date time) {
        final double T = julianCentury(time);
        return 365.2421896698 + T*(-0.00000615359 + T*(-7.29E-10 + T*(2.64E-10)));
    }

    /**
     * Retourne la longueur du mois synodique. Le mois synodique est l'intervalle de temps moyen
     * entre deux conjonctions de la lune et du soleil. Il correspond au cycle des phases de la
     * lune. Cet intervalle de temps est une <strong>moyenne</strong>. Un cycle réel peut s'écarter
     * de plusieurs heures de cette moyenne.
     *
     * @param  time A date in the month for which to compute the length.
     * @return The synodic length of the given month.
     */
    public static double synodicMonthLength(final Date time) {
        final double T=julianCentury(time);
        return 29.5305888531 + T*(0.00000021621 + T*(-3.64E-10));
    }

    /**
     * Returns a date format to be used by the command line tools.
     */
    static DateFormat getDateFormat() {
        final DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CANADA);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format;
    }

    /**
     * Prints the length of a tropical year and a synodic month for the given date.
     * Cette application peut être lancée avec la syntaxe suivante:
     * This application can be launch from the command line as below:
     *
     * {@preformat shell
     *     java org.geotoolkit.nature.Calendar [date]
     * }
     *
     * where <var>date</var> is an optional argument specifying the date and time in the
     * {@code "yyyy-MM-dd HH:mm:ss"} format, UTC time zone. If this argument is omitted,
     * then the current time is used.
     *
     * @param args The command line argument.
     * @throws ParseException If the date is not properly formatted.
     */
    public static void main(final String[] args) throws ParseException {
        final DateFormat format = getDateFormat();
        final Date time = (args.length != 0) ? format.parse(args[0]) : new Date();
        final PrintStream out = System.out;
        out.print("Date (UTC)   : "); out.println(format.format(time));
        out.print("Tropical year: "); out.println(tropicalYearLength(time));
        out.print("Synodic month: "); out.println(synodicMonthLength(time));
    }
}