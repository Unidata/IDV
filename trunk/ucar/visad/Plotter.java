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

package ucar.visad;


import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.ext.awt.image.codec.tiff.TIFFEncodeParam;

import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.transcoder.image.TIFFTranscoder;
import org.apache.fop.render.ps.PSTranscoder;
import org.apache.fop.svg.PDFTranscoder;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.svg.SVGDocument;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;

import javax.swing.JComponent;


/**
 * Plot a Plottable object to file. This is roughly analoguous to java
 * printing which provides a Graphics2D to a Printable object, which the
 * Printable can then render itself to, before being printed
 */
public class Plotter {

    /** Specifies a format of SVG */
    public static final int SVG = 0;

    /** Specifies a format of PNG */
    public static final int PNG = 1;

    /** Specifies a format of JPG */
    public static final int JPG = 2;

    /** Specifies a format of Postscript */
    public static final int PS = 3;

    /** Specifies a format of PDF */
    public static final int PDF = 4;

    /** Specifies a format of PS2 */
    public static final int TIFF = 5;

    /** Specifies a format of DIFAX */
    public static final int DIFAX = 6;

    /** The different file formats supported */
    public static final String[] FORMATS = {
        "svg", "png", "jpg", "ps", "pdf", "tiff", "difax"
    };

    /** The document being created */
    Object document;

    //      private PageFormat currentPage;


    /** The image format */
    private int format;



    /** The filename that is to be plotted to */
    private String filename;



    /** Specify if the SVG file is to be compressed */
    private boolean svgCompress = false;

    /** List of colors */
    Color[] colours = null;



    /** Specify the number of bits of colour for raster images */
    private int colourDepth = 8;

    /**
     * Create a plotter
     *
     * @param format
     *                The format of the distination file SVG, PNG or PS
     * @param filename
     *                The filename to plot to
     */
    public Plotter(int format, String filename) {
        this.format = format;
        setFilename(filename);
    }

    /**
     * ctor
     *
     * @param filename file to write to
     */
    public Plotter(String filename) {
        String  tmp     = filename.toLowerCase();
        boolean foundIt = false;
        for (int i = 0; i < FORMATS.length; i++) {
            if (tmp.endsWith("." + FORMATS[i])) {
                format  = i;
                foundIt = true;
                break;
            }
        }
        if ( !foundIt) {
            throw new IllegalArgumentException("Unknown file format:"
                    + filename);
        }
        setFilename(filename);
    }



    /**
     * Create a plotter
     *
     * @param format
     *                The format of the distination file SVG, PNG or PS
     * @param filename
     *                The filename to plot to
     * @param monochrome
     *                Set to true if all colours are to be plotted as
     *                either pure black or pure white
     */
    public Plotter(int format, String filename, boolean monochrome) {
        this(format, filename);
        if (monochrome) {
            setColourDepth(1);
        }
    }

    /**
     * handle the error. THis just throws the error but can be overwritten
     *
     * @param exc The error.
     *
     * @throws Exception On badness
     */
    public void handleError(Exception exc) throws Exception {
        throw exc;
    }


    /**
     * Set the filename
     *
     * @param filename the filename
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * set monochrome
     *
     * @param monochrome is monochrome
     */
    public void setMonochrome(boolean monochrome) {
        if (monochrome) {
            setColourDepth(1);
        }
    }

    /**
     * Set color depth
     *
     * @param colourDepth the depth
     */
    public void setColourDepth(int colourDepth) {
        this.colourDepth = colourDepth;
    }

    /**
     * Find out which format the plotter is plotting to
     *
     * @return format
     */
    public int getFormat() {
        return format;
    }

    /**
     * Plot a plottable object to the file specified in the constructor
     *
     * @param plottable
     *                The object to be plotted
     *
     * @throws Exception On badness
     * @throws FileNotFoundException On badness
     * @throws IOException On badness
     */
    public void plot(Plottable plottable)
            throws FileNotFoundException, IOException, Exception {
        // If not a monochrome chart, get the colours
        if (colourDepth > 1) {
            colours = plottable.getColours();
        }

        Graphics2D graphics = initialiseDocument(plottable);

        plottable.plot(graphics);
        finaliseDocument(graphics);
        graphics.dispose();
    }

    /**
     * Get the preview of the given plot
     *
     * @param plottable plot to preview
     *
     * @return preview component
     *
     * @throws FileNotFoundException On badness
     * @throws IOException On badness
     */
    public JComponent getPreview(Plottable plottable)
            throws FileNotFoundException, IOException {
        final int MAX_WIDTH  = 800;
        final int MAX_HEIGHT = 600;

        //              currentPage = page;
        // Prepare the document to be the right size
        Graphics2D graphics = initialiseDocument(plottable);
        // Plot the data to the document
        //              plottable.plot(graphics, page);
        plottable.plot(graphics);
        // Populate the document root with the SVG Content
        SVGDocument svgDocument = (SVGDocument) document;

        Element     docRoot     = svgDocument.getDocumentElement();
        Element     svgRoot     = ((SVGGraphics2D) graphics).getRoot(docRoot);

        // Set the dimensions of the SVG Image in pixels
        int[] chartSize  = plottable.getSize();
        int   viewWidth  = chartSize[0];
        int   viewHeight = chartSize[1];
        if (viewWidth > MAX_WIDTH) {
            viewWidth = MAX_WIDTH;
        }
        if (viewHeight > MAX_HEIGHT) {
            viewHeight = MAX_HEIGHT;
        }
        Dimension  preferredSize = new Dimension(viewWidth, viewHeight);

        JSVGCanvas canvas        = null;
        if (document instanceof SVGDocument) {
            canvas = new JSVGCanvas();
            canvas.setPreferredSize(preferredSize);
            canvas.setSVGDocument(svgDocument);
            canvas.setDoubleBufferedRendering(true);
        }

        return canvas;
    }

    /**
     * init
     *
     * @param plottable the plottable
     *
     * @return The graphics to use
     *
     * @throws FileNotFoundException On badness
     * @throws IOException On badness
     */
    private Graphics2D initialiseDocument(Plottable plottable)
            throws FileNotFoundException, IOException {
        Graphics2D graphics = null;
        // Create a Graphics2D object appropriate for each different
        // file format, and scaled for the page size
        // Also instantiates the document class variable, used to store
        // the rendered data before it is written to file
        if (format == Plotter.SVG) {
            graphics = initialiseSVG(plottable);
        } else if (format == Plotter.PNG) {
            graphics = initialisePNG(plottable);
        } else if (format == Plotter.JPG) {
            graphics = initialiseJPG(plottable);
        } else if (format == Plotter.PDF) {
            graphics = initialisePDF(plottable);
        } else if (format == Plotter.PS) {
            graphics = initialisePS2(plottable);
        } else if (format == Plotter.TIFF) {
            graphics = initialiseTIFF(plottable);
        } else if (format == Plotter.DIFAX) {
            graphics = initialiseDIFAX(plottable);
        } else {
            System.err.println("Bad Format: " + format);
        }

        return graphics;
    }

    /**
     * Writes the document to file in the appropriate format
     *
     * @param graphics
     *                The rendered product
     *
     * @throws Exception On badness
     * @throws IOException On badness
     */
    private void finaliseDocument(Graphics2D graphics)
            throws IOException, Exception {
        if (format == Plotter.SVG) {
            finaliseSVG(graphics);
        } else if (format == Plotter.PNG) {
            finalisePNG(graphics);
        } else if (format == Plotter.JPG) {
            finaliseJPG(graphics);
        } else if (format == Plotter.PDF) {
            finalisePDF(graphics);
        } else if (format == Plotter.PS) {
            finalisePS2(graphics);
        } else if (format == Plotter.TIFF) {
            finaliseTIFF(graphics);
        } else if (format == Plotter.DIFAX) {
            finaliseDIFAX(graphics);
        }
    }

    /**
     * Create a Graphics2D used by batik to render the SVG This method also
     * constructs the document used to store the rendered data, in this case
     * a DOM tree
     *
     * @returns an output stream initialised with a postscript header
     *
     * @param plottable The plottable
     *
     * @return The graphics to use
     */
    private Graphics2D initialiseSVG(Plottable plottable) {
        int[] chartSize = plottable.getSize();
        // Set the dimensions of the SVG Image in pixels
        int       width  = chartSize[0];
        int       height = chartSize[1];
        ;
        Dimension size   = new Dimension(width, height);

        // Create a fresh document
        DOMImplementation domImpl =
            SVGDOMImplementation.getDOMImplementation();
        String nameSpace = SVGDOMImplementation.SVG_NAMESPACE_URI;
        // Create a blank SVG document. This MUST contain the SVG
        // NameSpace.
        document = domImpl.createDocument(nameSpace, "svg", null);

        // Create the SVG Generator. Note that the SVG document is
        // used purely as a reference, Nothing is appended to it ... yet
        SVGGraphics2D graphics2d = new SVGGraphics2D((Document) document);

        graphics2d.setSVGCanvasSize(size);

        return graphics2d;
    }

    /**
     * Dump the Graphics 2D into a file, via a DOM document
     *
     * @param graphics The graphics to use
     *
     * @throws Exception On badness
     */
    private void finaliseSVG(Graphics2D graphics) throws Exception {
        Document svgDocument = (Document) document;
        // Get the root of the blank SVG Document
        Element docRoot = svgDocument.getDocumentElement();
        // Append the root from the SVG Generator to the SVG Document
        Element svgRoot = ((SVGGraphics2D) graphics).getRoot(docRoot);

        int                viewWidth  = 800;
        int                viewHeight = 600;
        java.awt.Rectangle size       = graphics.getClipBounds();
        if (size != null) {
            viewWidth  = (int) size.getWidth();
            viewHeight = (int) size.getHeight();
        }
        // Set the dimensions of the SVG Image in pixels
        //              int viewWidth = (int)currentPage.getWidth();
        //              int viewHeight = (int)currentPage.getHeight();

        svgRoot.setAttributeNS(null, "viewBox",
                               "0, 0, " + String.valueOf(viewWidth) + ", "
                               + String.valueOf(viewHeight));
        svgRoot.setAttributeNS(null, "color-rendering", "optimizeSpeed");
        // Dump the document to a file
        try {
            Writer outWriter = null;
            BufferedOutputStream outStream =
                new BufferedOutputStream(new FileOutputStream(filename));
            if (svgCompress) {
                GZIPOutputStream gzipStream = new GZIPOutputStream(outStream);
                outWriter = new OutputStreamWriter(gzipStream, "UTF-8");
            } else {
                outWriter = new OutputStreamWriter(outStream, "UTF-8");
            }

            ((SVGGraphics2D) graphics).stream(docRoot, outWriter);
            // Flush and close everything. Not sure if needs to
            // be this thorough
            outWriter.flush();
            outWriter.close();
            outStream.flush();
            outStream.close();
        } catch (FileNotFoundException e) {
            handleError(e);
            System.err.println("VectorPlotter.generateSVGFile: " + e);
        } catch (IOException e) {
            handleError(e);
            System.err.println("VectorPlotter.generateSVGFile: " + e);
        }

    }

    /**
     * Create a document in which to draw a PNG image This is in fact an SVG
     * document, which will later be transcoded
     *
     * @param plottable The plottable
     *
     * @return The graphics to use
     */
    private Graphics2D initialisePNG(Plottable plottable) {
        // PNGs are produced through SVG/batik so initialise a
        // SVG document
        Graphics2D graphics2d = initialiseSVG(plottable);

        return graphics2d;
    }

    /**
     * Create a document in which to draw a TIFF image This is in fact an
     * SVG document, which will later be transcoded
     *
     * @param plottable The plottable
     *
     * @return The graphics to use
     */
    private Graphics2D initialiseTIFF(Plottable plottable) {
        // PNGs are produced through SVG/batik so initialise a
        // SVG document
        Graphics2D graphics2d = initialiseSVG(plottable);

        return graphics2d;
    }

    /**
     * Create a document in which to draw a PNG image This is in fact an SVG
     * document, which will later be transcoded
     *
     * @param plottable The plottable
     *
     * @return The graphics to use
     */
    private Graphics2D initialiseJPG(Plottable plottable) {
        // JPGs are produced through SVG/batik so initialise a
        // SVG document
        Graphics2D graphics2d = initialiseSVG(plottable);

        return graphics2d;
    }

    /**
     * Create a document in which to draw a PNG image This is in fact an SVG
     * document, which will later be transcoded
     *
     * @param plottable The plottable
     *
     * @return The graphics to use
     */
    private Graphics2D initialisePDF(Plottable plottable) {
        // PDFs are produced through SVG/batik so initialise a
        // SVG document
        Graphics2D graphics2d = initialiseSVG(plottable);

        return graphics2d;
    }

    /**
     * Create a document in which to draw a PNG image This is in fact an SVG
     * document, which will later be transcoded
     *
     * @param plottable The plottable
     *
     * @return The graphics to use
     */
    private Graphics2D initialisePS2(Plottable plottable) {
        // PS2s are produced through SVG/batik so initialise a
        // SVG document
        Graphics2D graphics2d = initialiseSVG(plottable);

        return graphics2d;
    }

    /**
     * Create a document in which to draw a PNG image This is in fact an SVG
     * document, which will later be transcoded
     *
     * @param plottable The plottable
     *
     * @return The graphics to use
     */
    private Graphics2D initialiseDIFAX(Plottable plottable) {

        // PNGs are produced through SVG/batik so initialise a
        // SVG document
        Graphics2D graphics2d = initialiseSVG(plottable);

        return graphics2d;
    }

    /**
     * Dump the Graphics2D into a DOM document, then transcode this into a
     * PNG file using the batik SVG transcoder
     *
     * @param graphics The graphics to use
     *
     * @throws Exception On badness
     */
    private void finalisePNG(Graphics2D graphics) throws Exception {

        Document svgDocument = (Document) document;
        // Get the root of the blank SVG Document
        Element docRoot = svgDocument.getDocumentElement();
        // Append the root from the SVG Generator to the SVG Document
        Element svgRoot = ((SVGGraphics2D) graphics).getRoot(docRoot);

        // Use the transcoder to convert the SVG Document to a PNG file
        try {
            BufferedOutputStream pngStream =
                new BufferedOutputStream(new FileOutputStream(filename));
            TranscoderInput  transcoderIn  = new TranscoderInput(svgDocument);
            TranscoderOutput transcoderOut = new TranscoderOutput(pngStream);

            PNGTranscoder    pngTranscoder = new PNGTranscoder();
            pngTranscoder.addTranscodingHint(ImageTranscoder.KEY_MEDIA,
                                             "screen");

            //                      configureAntiAlias(pngTranscoder, svgRoot, colourDepth);
            pngTranscoder.transcode(transcoderIn, transcoderOut);
            pngStream.flush();
            pngStream.close();

            // If the image is not true colour
            if (colourDepth < 16) {
                // Convert it to an indexed image, with
                // a colour palette
                File file = new File(filename);
                indexImage(colourDepth, file, "png");
            }
        } catch (FileNotFoundException e) {
            handleError(e);
            System.err.println("Plotter.finalisePNG: " + e);
        } catch (IOException e) {
            handleError(e);
            System.err.println("Plotter.finalisePNG: " + e);
        } catch (TranscoderException e) {
            handleError(e);
            System.err.println("Plotter.finalisePNG: " + e);
        }

    }

    /**
     * Dump the Graphics2D into a DOM document, then transcode this into a
     * PNG file using the batik SVG transcoder
     *
     * @param graphics The graphics to use
     *
     * @throws Exception On badness
     */
    private void finaliseTIFF(Graphics2D graphics) throws Exception {
        Document svgDocument = (Document) document;
        // Get the root of the blank SVG Document
        Element docRoot = svgDocument.getDocumentElement();
        // Append the root from the SVG Generator to the SVG Document
        Element svgRoot = ((SVGGraphics2D) graphics).getRoot(docRoot);

        // Use the transcoder to convert the SVG Document to a TIFF file
        try {
            BufferedOutputStream tiffStream =
                new BufferedOutputStream(new FileOutputStream(filename));
            TranscoderInput  transcoderIn   =
                new TranscoderInput(svgDocument);
            TranscoderOutput transcoderOut  =
                new TranscoderOutput(tiffStream);

            TIFFTranscoder   tiffTranscoder = new TIFFTranscoder();
            PNGTranscoder    pngTranscoder  = new PNGTranscoder();

            //                      configureAntiAlias(pngTranscoder, svgRoot, colourDepth);
            pngTranscoder.transcode(transcoderIn, transcoderOut);

            tiffStream.flush();
            tiffStream.close();

        } catch (FileNotFoundException e) {
            handleError(e);
            System.err.println("Plotter.finaliseTIFF: " + e);
        } catch (IOException e) {
            handleError(e);
            System.err.println("Plotter.finaliseTIFF: " + e);
        } catch (TranscoderException e) {
            handleError(e);
            System.err.println("Plotter.finaliseTIFF: " + e);
        }

    }

    /**
     * Dump the Graphics2D into a DOM document, then transcode this into a
     * JPG file using the batik SVG transcoder
     *
     * @param graphics The graphics to use
     *
     * @throws Exception On badness
     */
    private void finaliseJPG(Graphics2D graphics) throws Exception {
        Document svgDocument = (Document) document;
        // Get the root of the blank SVG Document
        Element docRoot = svgDocument.getDocumentElement();
        // Append the root from the SVG Generator to the SVG Document
        Element svgRoot = ((SVGGraphics2D) graphics).getRoot(docRoot);

        // Use the transcoder to convert the SVG Document to a PNG file
        try {
            BufferedOutputStream jpgStream =
                new BufferedOutputStream(new FileOutputStream(filename));
            TranscoderInput  transcoderIn  = new TranscoderInput(svgDocument);
            TranscoderOutput transcoderOut = new TranscoderOutput(jpgStream);

            JPEGTranscoder   jpgTranscoder = new JPEGTranscoder();
            jpgTranscoder.transcode(transcoderIn, transcoderOut);
            jpgStream.flush();
            jpgStream.close();

            // Note jpeg is a 32 bit image format so don't
            // bother indexing it

        } catch (FileNotFoundException e) {
            handleError(e);
            System.err.println("Plotter.finaliseJPG: " + e);
        } catch (IOException e) {
            handleError(e);
            System.err.println("Plotter.finaliseJPG: " + e);
        } catch (TranscoderException e) {
            handleError(e);
            System.err.println("Plotter.finaliseJPG: " + e);
        }

    }

    /**
     * Dump the Graphics2D into a DOM document, then transcode this into a
     * PNG file using the batik SVG transcoder
     *
     * @param graphics The graphics to use
     *
     * @throws Exception On badness
     */
    private void finalisePDF(Graphics2D graphics) throws Exception {
        Document svgDocument = (Document) document;
        // Get the root of the blank SVG Document
        Element docRoot = svgDocument.getDocumentElement();
        // Append the root from the SVG Generator to the SVG Document
        Element svgRoot = ((SVGGraphics2D) graphics).getRoot(docRoot);

        // Use the transcoder to convert the SVG Document to a PNG file
        try {
            BufferedOutputStream pdfStream =
                new BufferedOutputStream(new FileOutputStream(filename));
            TranscoderInput  transcoderIn  = new TranscoderInput(svgDocument);
            TranscoderOutput transcoderOut = new TranscoderOutput(pdfStream);

            PDFTranscoder    pdfTranscoder = new PDFTranscoder();
            pdfTranscoder.transcode(transcoderIn, transcoderOut);
            pdfStream.flush();
            pdfStream.close();
        } catch (FileNotFoundException e) {
            handleError(e);
            System.err.println("Plotter.finalisePDF: " + e);
        } catch (IOException e) {
            handleError(e);
            System.err.println("Plotter.finalisePDF: " + e);
        } catch (TranscoderException e) {
            handleError(e);
            System.err.println("Plotter.finalisePDF: " + e);
        }

    }

    /**
     * Dump the Graphics2D into a DOM document, then transcode this into a
     * PNG file using the batik SVG transcoder
     *
     * @param graphics The graphics to use
     *
     * @throws Exception On badness
     */
    private void finalisePS2(Graphics2D graphics) throws Exception {
        Document svgDocument = (Document) document;
        // Get the root of the blank SVG Document
        Element docRoot = svgDocument.getDocumentElement();
        // Append the root from the SVG Generator to the SVG Document
        Element svgRoot = ((SVGGraphics2D) graphics).getRoot(docRoot);

        java.awt.Rectangle size       = graphics.getClipBounds();
        int                viewWidth  = 800;
        int                viewHeight = 600;
        if (size != null) {
            viewWidth  = (int) size.getWidth();
            viewHeight = (int) size.getHeight();
        }

        //              int viewWidth = (int)currentPage.getWidth();
        //              int viewHeight = (int)currentPage.getHeight();


        svgRoot.setAttributeNS(null, "viewBox",
                               "0, 0, " + String.valueOf(viewWidth) + ", "
                               + String.valueOf(viewHeight));

        // Use the transcoder to convert the SVG Document to a PS file
        try {
            BufferedOutputStream psStream =
                new BufferedOutputStream(new FileOutputStream(filename));
            TranscoderInput  transcoderIn  = new TranscoderInput(svgDocument);
            TranscoderOutput transcoderOut = new TranscoderOutput(psStream);

            PSTranscoder     psTranscoder  = new PSTranscoder();
            psTranscoder.addTranscodingHint(PSTranscoder.KEY_WIDTH,
                                            new Float(viewWidth));
            psTranscoder.addTranscodingHint(PSTranscoder.KEY_HEIGHT,
                                            new Float(viewHeight));

            psTranscoder.transcode(transcoderIn, transcoderOut);
            psStream.flush();
            psStream.close();
        } catch (FileNotFoundException e) {
            handleError(e);
            System.err.println("Plotter.finalisePS2: " + e);
        } catch (IOException e) {
            handleError(e);
            System.err.println("Plotter.finalisePS2: " + e);
        } catch (TranscoderException e) {
            handleError(e);
            System.err.println("Plotter.finalisePS2: " + e);
        }

    }

    /**
     * Dump the Graphics2D into a DOM document, then transcode this into a
     * PNG file using the batik SVG transcoder
     *
     * @param graphics The graphics to use
     *
     * @throws Exception On badness
     */
    private void finaliseDIFAX(Graphics2D graphics) throws Exception {
        Document svgDocument = (Document) document;
        // Get the root of the blank SVG Document
        Element docRoot = svgDocument.getDocumentElement();
        // Append the root from the SVG Generator to the SVG Document
        Element svgRoot = ((SVGGraphics2D) graphics).getRoot(docRoot);

        // Use the transcoder to convert the SVG Document to a PNG file
        try {
            BufferedOutputStream pngStream =
                new BufferedOutputStream(new FileOutputStream(filename));
            TranscoderInput  transcoderIn  = new TranscoderInput(svgDocument);
            TranscoderOutput transcoderOut = new TranscoderOutput(pngStream);

            PNGTranscoder    pngTranscoder = new PNGTranscoder();
            pngTranscoder.addTranscodingHint(ImageTranscoder.KEY_MEDIA,
                                             "screen");

            // Use 1 bit colour (true monochrome)
            pngTranscoder.addTranscodingHint(PNGTranscoder.KEY_INDEXED,
                                             new Integer(1));
            // And switch off antialiasing - this uses shades
            svgRoot.setAttributeNS(null, "shape-rendering", "crispEdges");
            svgRoot.setAttributeNS(null, "text-rendering", "optimizeSpeed");
            pngTranscoder.transcode(transcoderIn, transcoderOut);
            pngStream.flush();
            pngStream.close();

            // Convert it to an indexed image, with
            // a colour palette
            File file = new File(filename);
            indexImage(1, file, "png");
        } catch (FileNotFoundException e) {
            handleError(e);
            System.err.println("Plotter.finaliseDIFAX: " + e);
        } catch (IOException e) {
            handleError(e);
            System.err.println("Plotter.finaliseDIFAX: " + e);
        } catch (TranscoderException e) {
            handleError(e);
            System.err.println("Plotter.finaliseDIFAX: " + e);
        }

    }

    /**
     * Read a true colour image from file and convert it into an
     * indexed image ie. One with a limited number of colours stored
     * in a colour table. This allows filesize to be significantly reduced
     * NOTE. Due to java bug 6345283 the only bit depths that will
     * work correctly are 1 bit and 8 bit. 16 Bits is not supported
     * at all, and bit depths between 2 and 7 inclusive will still
     * be written to an 8 bit file, even if they still use a reduced
     * bit colour palette
     * @param depth The intended colour depth (usually 8 bit or 1 bit)
     * @param file The file containing the true colour image
     * @param format The fileformat ("png", or "tiff")
     *
     * @throws Exception On badness
     */
    private void indexImage(int depth, File file, String format)
            throws Exception {
        try {
            BufferedImage image32 = ImageIO.read(file);
            BufferedImage indexed = indexImage(image32, depth);
            ImageIO.write(indexed, format, file);
        } catch (Exception e) {
            handleError(e);
            e.printStackTrace();
        }
    }

    /**
     * Split a 32 bit integer RGB value into its byte RGB components
     * @param rgb The 32 bit integer RGB value
     * @return byte[3] array containing red, green and blue respectively
     */
    private static byte[] splitRGB(int rgb) {
        byte[] bytes = new byte[3];
        bytes[0] = (byte) ((rgb >> 16) & 0xFF);
        bytes[1] = (byte) ((rgb >> 8) & 0xFF);
        bytes[2] = (byte) ((rgb >> 0) & 0xFF);

        return bytes;
    }

    /**
     *   Take an array list of colours and create a byte RGB colour table
     * for use with IndexColorModel
     * @param colours A list of all the colours to be put in the table
     * @param depth The colour depth. There should be no more than
     * 1 << depth elements in colours, any others will be discarded
     * Any unused table entries will be set to white
     * @return An RGB colour table to be used by IndexColorModel
     */
    private static byte[][] makeIndexTable(ArrayList colours, int depth) {
        int numColours = colours.size();

        // Calculate the table size to match the colour depth
        int      tableSize = 1 << depth;
        byte[][] table     = new byte[3][tableSize];

        for (int i = 0; i < numColours; i++) {
            int    rgbInt = ((Integer) colours.get(i)).intValue();
            byte[] rgb    = splitRGB(rgbInt);
            if (i < tableSize) {
                table[0][i] = rgb[0];
                table[1][i] = rgb[1];
                table[2][i] = rgb[2];
            }
        }

        // Fill any unused table entries with white
        if (tableSize > numColours) {
            for (int i = numColours; i < tableSize; i++) {
                table[0][i] = -1;
                table[1][i] = -1;
                table[2][i] = -1;
            }
        }
        return table;
    }

    /**
     * Take a hash table of colours and weights. Reduce the colours so
     * that only the most heavily weighted remain
     * @param weightTable A Hasjtable of colours. Each Integer RGB forms
     * the key to the Hashtable, the corresponding weight for the colour
     * is stored in the table
     * @param newSize newSize
     * @return new colors
     */
    private ArrayList reduceColours(Hashtable weightTable, int newSize) {
        ArrayList             reduced    = new ArrayList();

        java.util.Enumeration keys       = weightTable.keys();
        int                   numColours = weightTable.size();
        Integer[]             sorted     = new Integer[numColours];

        // If we already meet the new size restriction
        if (numColours <= newSize) {
            // Return all the colours
            while (keys.hasMoreElements()) {
                reduced.add(keys.nextElement());
            }
            return reduced;
        }

        // Separate the RGB keys from the weights
        for (int i = 0; i < numColours; i++) {
            Integer rgb   = (Integer) keys.nextElement();
            Integer count = (Integer) weightTable.get(rgb);
            sorted[i] = count;
        }
        // Sort the weights into ascending order
        Arrays.sort(sorted);
        int sortedLen = sorted.length;

        if (sortedLen == 0) {
            return reduced;
        }

        // Loop over each of the size of the target colour table
        for (int i = 0; i < newSize; i++) {
            // Get the next weight in ascending order of size
            Integer tableWeight = sorted[sortedLen - i - 1];
            keys = weightTable.keys();
            boolean found = false;
            // Find the colour for the next weight
            for (int j = 0; j < weightTable.size(); j++) {
                // Get the colour and it's weight
                Integer rgb    = (Integer) keys.nextElement();
                Integer weight = (Integer) weightTable.get(rgb);
                // Only get the first instance of this weight
                if ( !found) {
                    // If the weight matches
                    if (weight.equals(tableWeight)) {
                        // Use this colour
                        reduced.add(rgb);
                        weightTable.remove(rgb);
                        found = true;
                    }
                }
            }
        }

        return reduced;
    }

    /**
     * Convert an image with a bit depth > 1 to a bit depth of one, by simply
     * setting all none white pixels to black
     *
     * @param imageDepthN  imageDepthN
     */
    private void monochromatise(BufferedImage imageDepthN) {
        int xSize = imageDepthN.getWidth();
        int ySize = imageDepthN.getHeight();

        int WHITE = 0xFFFFFF;
        int GRAY  = 0xBBBBBB;
        int BLACK = 0x000000;
        for (int i = 0; i < xSize; i++) {
            for (int j = 0; j < ySize; j++) {
                int rgb = imageDepthN.getRGB(i, j) & 0xFFFFFF;
                if (rgb < GRAY) {
                    rgb = BLACK;
                }
                //                               else {
                //                                      rgb = WHITE;
                //                              }
                imageDepthN.setRGB(i, j, rgb);
            }
        }
    }

    /**
     * Convert a 32 bit binary image into an indexed image with a
     * specific colour depth
     * IMPORTANT NOTE. The contents of the instance variable - colours,
     * will be placed into the beginning of the colour index table. This
     * is because image processing techniques such as antialiasing and
     * transparency can push the number of colours of an otherwise low
     * colour image into the thousands, most of these colours will be
     * lost upon conversion to low bit color. The colours used in the
     * Chart can be reserved by putting them into the colours variable,
     * ensuring that it is the less important antialiasing shades which
     * get lost, and not the main colours used in the chart
     * ANOTHER IMPORTANT NOTE. Due to java bug 6345283 the only bit depths
     * that will work correctly are 1 bit and 8 bit. 16 Bits is not
     * supported at all, and bit depths between 2 and 7 inclusive will still
     * be written to an 8 bit file, even if they still use a reduced
     * bit colour palette
     *
     *
     * @param image32  image32
     * @param depth Number of bits per pixel. See "ANOTHER IMPORTANT NOTE"
     * @return byte[3] array containing red, green and blue respectively
     *
     * @throws IOException On badness
     */
    private BufferedImage indexImage(BufferedImage image32, int depth)
            throws IOException {
        final int PALETTE_SIZE  = 1 << depth;
        ArrayList colourList    = new ArrayList();
        ArrayList reservedList  = new ArrayList();
        Hashtable colourWeights = new Hashtable();

        // First of all, reserve any colours provided by the chart
        // by putting them into the colour table
        if (colours != null) {
            for (int i = 0; i < colours.length; i++) {
                int     rgbInt = colours[i].getRGB() & 0xFFFFFF;
                Integer rgb    = new Integer(rgbInt);
                colourList.add(rgb);
                reservedList.add(rgb);
            }
        }

        // Scan the 32bit image to find out which colours it uses
        int xSize = image32.getWidth();
        int ySize = image32.getHeight();
        for (int i = 0; i < xSize; i++) {
            for (int j = 0; j < ySize; j++) {
                int rgb = image32.getRGB(i, j) & 0xFFFFFF;

                // Make a list of every colour used
                Integer rgbInteger = new Integer(rgb);
                if ( !colourList.contains(rgbInteger)) {
                    colourList.add(rgbInteger);
                }
                // Weight each colour in proportion to how often
                // it appears
                int weight = 1;
                if (reservedList.contains(rgbInteger)) {
                    // Heavily weight reserved colours
                    weight = 1000;
                }
                Integer count = (Integer) colourWeights.get(rgbInteger);
                if (count == null) {
                    colourWeights.put(rgbInteger, new Integer(weight));
                } else {
                    int countInt = count.intValue() + weight;
                    colourWeights.remove(rgbInteger);
                    colourWeights.put(rgbInteger, new Integer(countInt));
                }
            }
        }

        // If not monochrome. Reduce the colours so that the most
        // heavily weighted are put into the palette
        if (depth > 1) {
            colourList = reduceColours(colourWeights, PALETTE_SIZE);
        }

        // Put these colours into an index table
        int numColours = colourList.size();

        // Limit the number of colours to the colour depth of the
        // target image.
        if (numColours > 1 << depth) {
            numColours = 1 << depth;
        }
        byte[][] table = makeIndexTable(colourList, depth);

        // Define the colour model
        BufferedImage indexImage = null;
        if (depth < 2) {
            // Make sure that the image is true monochrome
            // (dont rely on java to do the colour reduction)
            monochromatise(image32);
            // Only for monochrome, use BYTE_BINARY
            // NOTE. For 4 bit colour, you should be able to
            // use this too. But because of java bug number
            // 6345283 it doesn't work.
            indexImage = new BufferedImage(xSize, ySize,
                                           BufferedImage.TYPE_BYTE_BINARY);
        } else {
            // Otherwise put the colour table into an 
            // IndexColorModel
            IndexColorModel colorModel = new IndexColorModel(depth,
                                             numColours, table[0], table[1],
                                             table[2]);

            indexImage = new BufferedImage(xSize, ySize,
                                           BufferedImage.TYPE_BYTE_INDEXED,
                                           colorModel);
        }

        // Copy the pixel value from the 32 bit source to the new
        // indexed image
        // Note that it is possible to draw the source image into
        // the target image, but it seems to insist on dithering, 
        // even when it doesn't have to, the results look hideous
        for (int i = 0; i < xSize; i++) {
            for (int j = 0; j < ySize; j++) {
                int rgb = image32.getRGB(i, j);
                indexImage.setRGB(i, j, rgb);
            }
        }

        return indexImage;
    }




    /**
     * Interface for classes which create charts capable of being plotted via
     * a third party rendering library to a vector based graphics format
     */
    public interface Plottable {

        /**
         * This will plot the chart to a vector graphics file
         * Don't use this method directly. It is called by the Plotter class
         * @param graphics the graphics
         */
        public void plot(Graphics2D graphics);

        /**
         * Get a list of the colours used in the chart
         * This may be necessary if the chart is being plotted to a medium
         * with limited colours, eg. 8 bit PNG. The antialiasing and gradient
         * fill used in some charts can easily grab all the available colours
         * By taking the ones used specifically by the chart, the important
         * colours can be reserved in the colour table ensuring that any
         * colour loss has only minimal cosmetic effect
         * @return The colours used by this chart
         */
        public Color[] getColours();

        /**
         * Get the size_
         *
         * @return size
         */
        public int[] getSize();
    }



}
