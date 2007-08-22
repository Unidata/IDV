/*
 * $Id: TextHistoryPane.java,v 1.11 2007/07/06 20:45:33 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.ui;


import ucar.unidata.util.FontUtil;



import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.print.*;

import java.io.*;

import java.util.StringTokenizer;

import javax.swing.*;


/**
 * TextHistoryPane
 * Keeps a user-settable number of lines in a JTextArea.
 * Lines are always appended to bottom, and top lines are scrolled off.
 * A popup menu allows the user to change the number of lines to keep.
 * @author John Caron
 * @version $Id: TextHistoryPane.java,v 1.11 2007/07/06 20:45:33 jeffmc Exp $
 */
public class TextHistoryPane extends JPanel {

    /** _more_ */
    private JTextArea ta;

    /** _more_ */
    private int nlines, removeIncr,
                count = 0;

    /** _more_ */
    private int ptSize = 12;

    /**
     * constructor
     *  @param nlines  number of lines of text to keep history of
     *  @param removeIncr  remove this number of lines at a time
     *  @param popupOK enable popup menu
     */
    public TextHistoryPane(int nlines, int removeIncr, boolean popupOK) {
        super(new BorderLayout());
        this.nlines = nlines - 1;
        this.removeIncr = Math.min(nlines - 1, removeIncr - 1);  // cant be bigger than nlines

        ta = new JTextArea();
        ta.setLineWrap(true);
        ta.setEditable(false);
        ta.setFont(new Font("Monospaced", Font.PLAIN, ptSize));

        if (popupOK) {
            ta.addMouseListener(new MyPopupMenu());
        }

        JScrollPane sp = new JScrollPane(ta);
        add(sp, BorderLayout.CENTER);
    }

    /**
     * Append this line to the bottom of the JTextArea.
     * A newline is added and JTextArea is scrolled to bottom;
     * remove lines at top if needed.
     * @param line append this line. Ok to have multiple lines (ie embedded newlines)
     *   but not too many.
     */
    public void appendLine(String line) {

        if (count >= nlines) {
            try {
                int remove = Math.max(removeIncr, count - nlines);  // nlines may have changed
                int offset = ta.getLineEndOffset(remove);
                ta.replaceRange("", 0, offset);
            } catch (Exception e) {
                System.out.println("BUG in TextHistoryPane");  // shouldnt happen
            }
            count = nlines - removeIncr;
        }
        ta.append(line);
        ta.append("\n");
        count++;

        // scroll to end
        ta.setCaretPosition(ta.getText().length());
    }

    /**
     * _more_
     */
    public void clear() {
        ta.setText(null);
    }

    /**
     * _more_
     */
    public void gotoTop() {
        ta.setCaretPosition(0);
    }

    /**
     * Class MyPopupMenu
     *
     *
     * @author Unidata development team
     * @version %I%, %G%
     */
    private class MyPopupMenu extends ucar.unidata.ui.event
        .PopupTriggerListener implements Printable {

        /** _more_ */
        private JPopupMenu popup = new JPopupMenu("Options");

        /** _more_ */
        private JTextField nlinesFld = new JTextField();

        /** _more_ */
        private JTextField ptSizeFld = new JTextField();

        /** _more_ */
        private StringTokenizer token;

        /** _more_ */
        private Font newFont;

        /** _more_ */
        private int incrY;

        /**
         * _more_
         *
         */
        MyPopupMenu() {
            super();

            // the popup menu
            JPanel nlPan = new JPanel();
            nlPan.add(new JLabel("Number of lines to keep:"));
            nlPan.add(nlinesFld);
            popup.add(nlPan);

            JPanel psPan = new JPanel();
            psPan.add(new JLabel("Text Point Size:"));
            psPan.add(ptSizeFld);
            popup.add(psPan);

            JMenuItem printButt = new JMenuItem("Print");
            popup.add(printButt);

            JMenuItem writeButt = new JMenuItem("Write to File");
            popup.add(writeButt);

            JMenuItem dissButt = new JMenuItem("Dismiss");
            popup.add(dissButt);

            // listen to changes to the JTextField
            nlinesFld.addActionListener(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    int numLines = Integer.parseInt(nlinesFld.getText());
                    // System.out.println( "numLines = "+numLines);
                    TextHistoryPane.this.nlines = Math.max(numLines - 1,
                            removeIncr);
                    popup.setVisible(false);
                }
            });

            ptSizeFld.addActionListener(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    ptSize = Integer.parseInt(ptSizeFld.getText());
                    ta.setFont(new Font("Monospaced", Font.PLAIN, ptSize));
                    popup.setVisible(false);
                }
            });

            // print
            printButt.addActionListener(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    PrinterJob printJob = PrinterJob.getPrinterJob();
                    PageFormat pf       = printJob.defaultPage();

                    newFont = FontUtil.getMonoFont(10).getFont();
                    FontMetrics fontMetrics =
                        Toolkit.getDefaultToolkit().getFontMetrics(newFont);
                    incrY = fontMetrics.getAscent()
                            + fontMetrics.getDescent();

                    printJob.setPrintable(MyPopupMenu.this, pf);
                    if (printJob.printDialog()) {
                        try {
                            //if (Debug.isSet("print.job")) System.out.println("call printJob.print");
                            printJob.print();
                            //if (Debug.isSet("print.job")) System.out.println(" printJob done");
                        } catch (Exception PrintException) {
                            PrintException.printStackTrace();
                        } finally {
                            popup.setVisible(false);
                        }
                    }
                }
            });

            writeButt.addActionListener(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    String filename = JOptionPane.showInputDialog(null,
                                          "Enter filename: ");
                    if (filename == null) {
                        return;
                    }
                    try {
                        OutputStream os = new BufferedOutputStream(
                                              new FileOutputStream(filename));
                        StringTokenizer token =
                            new StringTokenizer(ta.getText(), "\r\n");
                        while (token.hasMoreTokens()) {
                            os.write(token.nextToken().getBytes());
                            os.write('\n');
                        }
                        os.close();
                    } catch (IOException ioe) {
                        System.out.println(" write TextArea to file = "
                                           + filename + " " + ioe);
                    }
                    popup.setVisible(false);
                }
            });

            dissButt.addActionListener(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    popup.setVisible(false);
                }
            });
        }

        /**
         * _more_
         *
         * @param e
         */
        public void showPopup(MouseEvent e) {
            nlinesFld.setText("" + (TextHistoryPane.this.nlines + 1));
            ptSizeFld.setText("" + ptSize);
            popup.show(ta, e.getX(), e.getY());
        }

        /**
         * _more_
         *
         * @param g
         * @param pf
         * @param pi
         * @return _more_
         *
         * @throws PrinterException
         */
        public int print(Graphics g, PageFormat pf, int pi)
                throws PrinterException {
            if (pi == 0) {
                token = new StringTokenizer(ta.getText(), "\r\n");
            }

            if ( !token.hasMoreTokens()) {
                return Printable.NO_SUCH_PAGE;
            }

            Graphics2D g2d = (Graphics2D) g;
            g2d.setPaint(Color.black);
            g2d.setFont(newFont);

            double xbeg   = pf.getImageableX();
            double ywidth = pf.getImageableHeight() + pf.getImageableY();
            double y      = pf.getImageableY() + incrY;
            while (token.hasMoreTokens() && (y < ywidth)) {
                String toke = token.nextToken();
                g2d.drawString(toke, (int) xbeg, (int) y);
                y += incrY;
            }
            return Printable.PAGE_EXISTS;
        }
    }

}

/*
 *  Change History:
 *  $Log: TextHistoryPane.java,v $
 *  Revision 1.11  2007/07/06 20:45:33  jeffmc
 *  A big J&J
 *
 *  Revision 1.10  2005/05/13 18:31:52  jeffmc
 *  Clean up the odd copyright symbols
 *
 *  Revision 1.9  2004/09/07 18:36:27  jeffmc
 *  Jindent and javadocs
 *
 *  Revision 1.8  2004/02/27 21:19:21  jeffmc
 *  Lots of javadoc warning fixes
 *
 *  Revision 1.7  2004/01/29 17:37:13  jeffmc
 *  A big sweeping checkin after a big sweeping reformatting
 *  using the new jindent.
 *
 *  jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
 *  templates there is a '_more_' to remind us to fill these in.
 *
 *  Revision 1.6  2003/05/19 19:18:18  jeffmc
 *  import the new FontUtil
 *
 *  Revision 1.5  2001/02/21 21:30:25  caron
 *  add printing
 *
 *  Revision 1.4  2001/02/06 23:11:11  caron
 *  add print and write to file
 *
 *  Revision 1.3  2000/08/18 04:15:59  russ
 *  Licensed under GNU LGPL.
 *
 *  Revision 1.2  2000/02/17 20:29:32  caron
 *  use PopupTriggerListener
 *
 *  Revision 1.1  1999/12/16 22:58:19  caron
 *  gridded data viewer checkin
 *
 */






