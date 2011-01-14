package ucar.unidata.util;

//~--- non-JDK imports --------------------------------------------------------

import visad.DateTime;
import visad.VisADException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parse a string based on a date based template. The template uses
 * date elements only (numerically only!):
 * <li>y, Y : Year
 * <li>m, M : Month
 * <li>d, D     : Day
 * <li>h, H : Hour
 * <li>n, N : Minute (m is already in use for MONTH)
 * <li>s, S : Seconds
 * Internally a regular expression is generated to parse a string to find the
 * dates and/or times somewhere in this string. Any character not in the list
 * above is simply copied to the regular expression.
 *
 * <p>The values for the year, month and day can be queried with the get() function
 *
 * <p>Examples:
 * <br>1. The pattern 'yyyymmdd' will generate this regular expression:
 * <pre>        '\D*(\d{4})(\d{2})(\d{2}).*'</pre>
 * <br>2. The pattern 'yyyy-mm-dd' will generate:
 * <pre>        '\D*(\d{4})-(\d{2})-(\d{2}).*'</pre>
 *
 * <p>when applied the regular expression will give the following results:
 * <p>'GLR20070315.txt' gives: year=2007, month=03, day=15
 * <p>and
 * <p>  'GLR2007031520080414.txt' gives: year=2007, month=03, day=15
 *
 * <p>The code below will print the year (2006)
 * <pre>        String fname = "GLR20060214.txt";
 *      String template = "yyyymmdd";
 *      DatePattern p = new DatePattern(template);
 *      p.match(fname);
 *      System.out.println(fname);
 *      System.out.println(p.get(YEAR));
 * </pre>
 */

/**
 * handle user defined simple date/time patterns (such as yyyymm hh:nn)
 *
 * @author  Willem Nieuwenhuis
 * @version $Revision: 1.54 $
 */
public class DatePattern {
    final static char        DAY         = 'd';
    final static char        HOUR        = 'h';
    final static char        MINUTE      = 'n';    // 'm' is already used for MONTH
    final static char        MONTH       = 'm';
    final static char        SECOND      = 's';
    final static char        YEAR        = 'y';
    final static String      valid_chars = "ymdhns";
    private List<DmyPattern> matchList   = null;
    private Pattern          pat         = null;
    private List<DmyPattern> dmts;
    private String           regPattern;

    /**
     * Constructor: creates a regular expression to extract a date based on a template. The template uses
     * date/time elements only (numerically only!):
     * <li>y, Y : Year
     * <li>m, M : Month
     * <li>d, D     : Day
     * <li>h, H : Hour
     * <li>n, N : Minute (m is already in use for MONTH)
     * <li>s, S : Seconds
     * @param dmtpat the user defined date/time pattern
     */
    public DatePattern(String dmtpat) {
        dmts = new ArrayList<DmyPattern>();
        compileDatetimePattern(dmtpat);    // create regexp from template
        pat = Pattern.compile(getRegex());    // compile the regexp
    }

    /**
     * Return the fields for which the template matched the search string.
     * For example the template 'yyyymm' applied to the search string 'GLR200704'
     * will return [YEAR, MONTH].
     * @return The list with found fields
     */
    public List<Character> getValidFields() {
        ArrayList<Character> ac = new ArrayList<Character>();

        for (int i = 0; i < matchList.size(); i++) {
            ac.add(matchList.get(i).dt_type);
        }

        return ac;
    }

    /**
     * Retrieve the value of the field. If no value was parsed the value will be -1.
     * Recognized field values are:
     * <li>YEAR
     * <li>MONTH
     * <li>DAY
     * <li>HOUR
     * <li>MINUTE
     * <li>SECOND
     * @param field the field to retrieve the value for
     * @return the value of the field
     */
    public int get(char field) {
        if (pat == null) {
            return -1;
        }

        if (matchList == null) {
            return -1;
        }

        for (int i = 0; i < matchList.size(); i++) {
            if (matchList.get(i).dt_type == field) {
                return Integer.parseInt(matchList.get(i).reg);
            }
        }

        return -1;
    }

    /**
     * Match a search string against the compile pattern.
     * @param search the search string
     * @return true if there is a match
     */
    public boolean match(String search) {
        Matcher match = pat.matcher(search);

        if (matchList != null) {
            matchList = null;
        }

        matchList = new ArrayList<DmyPattern>();

        if (match.matches() && (match.groupCount() == dmts.size())) {
            for (int g = 1; g <= match.groupCount(); g++) {
                matchList.add(new DmyPattern(dmts.get(g - 1).getDt_type(), match.group(g)));
            }
        }

        return match.matches();
    }

    /**
     * Get the regular expression that is generated from the template
     * @return The regular expression
     */
    public String getRegex() {
        return regPattern;
    }

    /**
     * Create a regular expression to extract a date based on a template. The template uses
     * date/time elements only (numerically only!):
     * <li>y, Y : Year
     * <li>m, M : Month
     * <li>d, D     : Day
     * <li>h, H : Hour
     * <li>n, N : Minute (m is already in use for MONTH)
     * <li>s, S : Seconds
     * Any character not in the list is simply copied to the regular expression.
     * The regular expression generated can be used to parse string to find
     * dates and/or times; the expression is generated to find the first date/time string
     * somewhere in the string
     *
     * <p>Examples:
     * <br>1. The pattern 'yyyymmdd' will generate this regular expression:
     * <pre>        '\D*(\d{4})(\d{2})(\d{2}).*'</pre>
     * <br>2. The pattern 'yyyy-mm-dd' will generate:
     * <pre>        '\D*(\d{4})-(\d{2})-(\d{2}).*'</pre>
     *
     * <p>when applied the regular expression will give the following results:
     * <p>'GLR20070315.txt' gives: year=2007, month=03, day=15
     * <p>and
     * <p>  'GLR2007031520080414.txt' gives: year=2007, month=03, day=15
     *
     * @param pattern       the template
     *
     */
    private void compileDatetimePattern(String pattern) {
        regPattern = "\\D*";

        int    cc      = 0;
        char   old_c   = 'x';
        char[] patloc  = pattern.toCharArray();
        String patpart = "";

        for (char c : patloc) {
            char k = Character.toLowerCase(c);

            if (old_c == k) {
                cc++;
            } else {
                if (cc > 0) {
                    patpart = "(\\d{" + Integer.toString(cc) + "})";
                    dmts.add(new DmyPattern(old_c, patpart));
                    regPattern += patpart;
                }

                // if no pattern character simply copy char to pattern
                if (valid_chars.indexOf(k) == -1) {
                    regPattern += c;
                    cc         = 0;
                } else {
                    cc = 1;
                }

                old_c = k;
            }
        }

        // handle last part as well
        if (cc > 0) {
            patpart = "(\\d{" + Integer.toString(cc) + "})";
            dmts.add(new DmyPattern(old_c, patpart));
            regPattern += patpart;
        }

        regPattern += ".*";
    }

    /**
     * @return the date / time of the latest match() call
     */
    public DateTime getDateTime() {
        int             year    = 0;
        int             month   = 1;
        int             day     = 1;
        int             hour    = 0;
        int             minute  = 0;
        int             seconds = 0;
        List<Character> dpc     = getValidFields();

        for (Character c : dpc) {
            switch (c) {
            case DatePattern.YEAR :
                year = get(c);

                break;

            case DatePattern.MONTH :
                month = get(c);

                break;

            case DatePattern.DAY :
                day = get(c);

                break;

            case DatePattern.HOUR :
                hour = get(c);

                break;

            case DatePattern.MINUTE :
                minute = get(c);

                break;

            case DatePattern.SECOND :
                seconds = get(c);

                break;
            }
        }

        DateTime dt = null;

        try {
            Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));

            cal.set(year, month - 1, day, hour, minute, seconds);

            int doy = cal.get(Calendar.DAY_OF_YEAR);

            seconds += hour * 3600 + minute * 60;
            dt      = new DateTime(year, doy, seconds);
        } catch (VisADException e) {

            // Nothing to do; dt already set
        }

        return dt;
    }

    public static void main(String[] args) {
        String      fname   = "GLR2008081820090919.txt";
        String      fname2  = "GLR20070315.txt";
        String      fname3  = "20060214";
        String      mypat   = "yyyymmdd";
        String      fname4  = "200704";
        String      mypat2  = "yyyymm";
        String      fname11 = "GLR2007-04-14.txt";
        String      mypat11 = "yyyy-mm-dd";
        String      fname12 = "GLR2007-04-14 23:16:17.txt";
        String      mypat12 = "GLRyyyy-mm-dd hh:nn:ss";
        DatePattern p       = new DatePattern(mypat);

        printDetails(fname, p);
        System.out.println(p.match(fname2));
        printDetails(fname2, p);
        System.out.println(p.match(fname3));
        printDetails(fname3, p);
        p = new DatePattern(mypat2);
        printDetails(fname4, p);
        p = new DatePattern(mypat11);
        printDetails(fname11, p);
        p = new DatePattern(mypat12);
        printDetails(fname12, p);
    }

    private static void printDetails(String fname4, DatePattern p) {
        System.out.println(p.match(fname4));
        System.out.println(p.getValidFields());
        System.out.println(fname4);
        System.out.println(p.getRegex());
        System.out.println(p.get(YEAR));
        System.out.println(p.get(MONTH));
        System.out.println(p.get(DAY));
        System.out.println(p.get(HOUR));
        System.out.println(p.get(MINUTE));
        System.out.println(p.get(SECOND));
        System.out.println(p.getDateTime());
    }

    public class DmyPattern {
        private char   dt_type;
        private String reg;

        public DmyPattern(char dtType, String reg) {
            super();
            dt_type  = dtType;
            this.reg = reg;
        }

        public char getDt_type() {
            return dt_type;
        }

        public String getReg() {
            return reg;
        }
    }
}
