/**
 *
 * Copyright 1997-2005 Unidata Program Center/University Corporation for
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

package ucar.unidata.repository.output;


import org.w3c.dom.*;


import ucar.unidata.repository.*;
import ucar.unidata.repository.auth.*;
import ucar.unidata.repository.type.*;

import ucar.unidata.sql.SqlUtil;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.awt.Image;
import java.awt.image.*;


import java.io.*;

import java.io.File;
import java.io.InputStream;



import java.net.*;


import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



import java.util.regex.*;

import java.util.zip.*;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class ImageOutputHandler extends OutputHandler {

    /** _more_ */
    public static final String ARG_IMAGE_EDIT = "image.edit";

    /** _more_ */
    public static final String ARG_IMAGE_UNDO = "image.undo";

    /** _more_ */
    public static final String ARG_IMAGE_EDIT_RESIZE = "image.edit.resize";

    /** _more_ */
    public static final String ARG_IMAGE_EDIT_WIDTH = "image.edit.width";

    /** _more_ */
    public static final String ARG_IMAGE_EDIT_CROP = "image.edit.crop";

    /** _more_ */
    public static final String ARG_IMAGE_EDIT_REDEYE = "image.edit.redeye";

    /** _more_ */
    public static final String ARG_IMAGE_CROPX1 = "image.edit.cropx1";

    /** _more_ */
    public static final String ARG_IMAGE_CROPY1 = "image.edit.cropy1";

    /** _more_ */
    public static final String ARG_IMAGE_CROPX2 = "image.edit.cropx2";

    /** _more_ */
    public static final String ARG_IMAGE_CROPY2 = "image.edit.cropy2";

    /** _more_ */
    public static final String ARG_IMAGE_EDIT_ROTATE_LEFT =
        "image.edit.rotate.left";

    /** _more_ */
    public static final String ARG_IMAGE_EDIT_ROTATE_RIGHT =
        "image.edit.rotate.right";

    /** _more_ */
    public static final OutputType OUTPUT_GALLERY =
        new OutputType("Gallery", "image.gallery",
                       OutputType.TYPE_HTML | OutputType.TYPE_FORSEARCH, "",
                       ICON_IMAGES);

    /** _more_ */
    public static final OutputType OUTPUT_PLAYER =
        new OutputType("Image Player", "image.player",
                       OutputType.TYPE_HTML | OutputType.TYPE_FORSEARCH, "",
                       ICON_IMAGES);

    /** _more_ */
    public static final OutputType OUTPUT_SLIDESHOW =
        new OutputType("Slideshow", "image.slideshow",
                       OutputType.TYPE_HTML | OutputType.TYPE_FORSEARCH, "",
                       ICON_IMAGES);


    /** _more_ */
    public static final OutputType OUTPUT_EDIT = new OutputType("Edit Image",
                                                     "image.edit",
                                                     OutputType.TYPE_HTML,
                                                     "", ICON_IMAGES);



    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public ImageOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_GALLERY);
        addType(OUTPUT_PLAYER);
        //        addType(OUTPUT_SLIDESHOW);
        addType(OUTPUT_EDIT);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param state _more_
     * @param links _more_
     *
     *
     * @throws Exception _more_
     */

    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {

        //If its a single entry then punt

        if (state.entry != null) {
            if (getAccessManager().canDoAction(request, state.entry,
                    Permission.ACTION_EDIT)) {
                File f = state.entry.getFile();
                if ((f != null) && f.canWrite()) {
                    Link link = makeLink(request, state.getEntry(),
                                         OUTPUT_EDIT);
                    link.setLinkType(OutputType.TYPE_EDIT);
                    links.add(link);
                }
            }
            return;
        }


        List<Entry> entries = state.getAllEntries();
        if (entries.size() == 0) {
            return;
        }

        if (entries.size() > 0) {
            boolean ok = false;
            for (Entry entry : entries) {
                if (entry.getResource().isImage()) {
                    ok = true;
                    break;
                }
            }
            if ( !ok) {
                return;
            }
        }

        if (state.getEntry() != null) {
            //            links.add(makeLink(request, state.getEntry(), OUTPUT_SLIDESHOW));
            links.add(makeLink(request, state.getEntry(), OUTPUT_GALLERY));
            links.add(makeLink(request, state.getEntry(), OUTPUT_PLAYER));
        }
    }




    /** _more_ */
    private Hashtable<String, Image> imageCache = new Hashtable<String,
                                                      Image>();

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    private Image getImage(Entry entry) {
        Image image = imageCache.get(entry.getId());
        if (image == null) {
            image = ImageUtils.readImage(entry.getResource().getPath(),
                                         false);
            //Keep the cache size low
            if (imageCache.size() > 5) {
                imageCache = new Hashtable<String, Image>();
            }
            imageCache.put(entry.getId(), image);
        }
        return image;
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param image _more_
     */
    private void putImage(Entry entry, Image image) {
        imageCache.put(entry.getId(), image);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntry(Request request, Entry entry) throws Exception {

        if ( !getAccessManager().canDoAction(request, entry,
                                             Permission.ACTION_EDIT)) {
            throw new AccessException("Cannot edit image", null);
        }

        StringBuffer sb          = new StringBuffer();


        String       url         = getImageUrl(request, entry, true);
        Image        image       = getImage(entry);
        int          imageWidth  = image.getWidth(null);
        int          imageHeight = image.getHeight(null);
        Image        newImage    = null;
        if (request.exists(ARG_IMAGE_UNDO)) {
            File f = entry.getFile();
            if ((f != null) && f.canWrite()) {
                File entryDir =
                    getStorageManager().getEntryDir(entry.getId(), true);
                File original = new File(entryDir + "/" + "originalimage");
                if (original.exists()) {
                    imageCache.remove(entry.getId());
                    IOUtil.copyFile(original, f);
                    request.remove(ARG_IMAGE_UNDO);
                    return new Result(request.getUrl());
                }
            }
        } else if (request.exists(ARG_IMAGE_EDIT_RESIZE)) {
            newImage = ImageUtils.resize(image,
                                         request.get(ARG_IMAGE_EDIT_WIDTH,
                                             imageWidth), -1);
            request.remove(ARG_IMAGE_EDIT_RESIZE);
        } else if (request.exists(ARG_IMAGE_EDIT_REDEYE)) {
            int x1 = request.get(ARG_IMAGE_CROPX1, 0);
            int y1 = request.get(ARG_IMAGE_CROPY1, 0);
            int x2 = request.get(ARG_IMAGE_CROPX2, 0);
            int y2 = request.get(ARG_IMAGE_CROPY2, 0);
            if ((x1 < x2) && (y1 < y2)) {
                newImage = ImageUtils.removeRedeye(image, x1, y1, x2, y2);
            }
            request.remove(ARG_IMAGE_EDIT_REDEYE);
        } else if (request.exists(ARG_IMAGE_EDIT_CROP)) {
            int x1 = request.get(ARG_IMAGE_CROPX1, 0);
            int y1 = request.get(ARG_IMAGE_CROPY1, 0);
            int x2 = request.get(ARG_IMAGE_CROPX2, 0);
            int y2 = request.get(ARG_IMAGE_CROPY2, 0);
            if ((x1 < x2) && (y1 < y2)) {
                newImage = ImageUtils.clip(ImageUtils.toBufferedImage(image),
                                           new int[] { x1,
                        y1 }, new int[] { x2, y2 });
            }
            request.remove(ARG_IMAGE_EDIT_CROP);
        } else if (request.exists(ARG_IMAGE_EDIT_ROTATE_LEFT)) {
            newImage = ImageUtils.rotate90(ImageUtils.toBufferedImage(image),
                                           true);
            request.remove(ARG_IMAGE_EDIT_ROTATE_LEFT);
        } else if (request.exists(ARG_IMAGE_EDIT_ROTATE_RIGHT)) {
            newImage = ImageUtils.rotate90(ImageUtils.toBufferedImage(image),
                                           false);
            request.remove(ARG_IMAGE_EDIT_ROTATE_RIGHT);
        }
        if (newImage != null) {
            ImageUtils.waitOnImage(newImage);
            putImage(entry, newImage);
            File f = entry.getFile();
            getStorageManager().checkFile(f);
            if ((f != null) && f.canWrite()) {
                File entryDir =
                    getStorageManager().getEntryDir(entry.getId(), true);
                File original = new File(entryDir + "/" + "originalimage");
                if ( !original.exists()) {
                    IOUtil.copyFile(f, original);
                }
                ImageUtils.writeImageToFile(newImage, f);
            }
            return new Result(request.getUrl());
        }
        sb.append(request.formPost(getRepository().URL_ENTRY_SHOW));
        sb.append(HtmlUtil.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HtmlUtil.hidden(ARG_OUTPUT, OUTPUT_EDIT));
        sb.append(HtmlUtil.submit(msg("Change width:"),
                                  ARG_IMAGE_EDIT_RESIZE));
        sb.append(HtmlUtil.input(ARG_IMAGE_EDIT_WIDTH, "" + imageWidth,
                                 HtmlUtil.SIZE_5));
        sb.append(HtmlUtil.space(2));

        sb.append(HtmlUtil.submit(msg("Crop"), ARG_IMAGE_EDIT_CROP));
        sb.append(HtmlUtil.submit(msg("Remove Redeye"),
                                  ARG_IMAGE_EDIT_REDEYE));
        sb.append(HtmlUtil.hidden(ARG_IMAGE_CROPX1, "",
                                  HtmlUtil.SIZE_3
                                  + HtmlUtil.id(ARG_IMAGE_CROPX1)));
        sb.append(HtmlUtil.hidden(ARG_IMAGE_CROPY1, "",
                                  HtmlUtil.SIZE_3
                                  + HtmlUtil.id(ARG_IMAGE_CROPY1)));
        sb.append(HtmlUtil.hidden(ARG_IMAGE_CROPX2, "",
                                  HtmlUtil.SIZE_3
                                  + HtmlUtil.id(ARG_IMAGE_CROPX2)));
        sb.append(HtmlUtil.hidden(ARG_IMAGE_CROPY2, "",
                                  HtmlUtil.SIZE_3
                                  + HtmlUtil.id(ARG_IMAGE_CROPY2)));
        sb.append(HtmlUtil.div("",
                               HtmlUtil.cssClass("image_edit_box")
                               + HtmlUtil.id("image_edit_box")));

        sb.append(HtmlUtil.space(2));
        sb.append(HtmlUtil.submitImage(iconUrl(ICON_ANTIROTATE),
                                       ARG_IMAGE_EDIT_ROTATE_LEFT));
        sb.append(HtmlUtil.space(2));
        sb.append(HtmlUtil.submitImage(iconUrl(ICON_ROTATE),
                                       ARG_IMAGE_EDIT_ROTATE_RIGHT));
        File entryDir = getStorageManager().getEntryDir(entry.getId(), false);
        File original = new File(entryDir + "/" + "originalimage");
        if (original.exists()) {
            sb.append(HtmlUtil.space(2));
            sb.append(HtmlUtil.submit(msg("Undo all edits"), ARG_IMAGE_UNDO));
        }
        sb.append(HtmlUtil.formClose());


        String clickParams =
            "event,'imgid',"
            + HtmlUtil.comma(HtmlUtil.squote(ARG_IMAGE_CROPX1),
                             HtmlUtil.squote(ARG_IMAGE_CROPY1),
                             HtmlUtil.squote(ARG_IMAGE_CROPX2),
                             HtmlUtil.squote(ARG_IMAGE_CROPY2));

        String call = HtmlUtil.onMouseClick(HtmlUtil.call("editImageClick",
                          clickParams));
        sb.append(HtmlUtil.img(url, "", HtmlUtil.id("imgid") + call));
        return new Result("Image Edit", sb);

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputGroup(Request request, Group group,
                              List<Group> subGroups, List<Entry> entries)
            throws Exception {
        Result result = makeResult(request, group, entries);
        addLinks(request, result, new State(group, subGroups, entries));
        return result;
    }

    /**
     * _more_
     *
     * @param output _more_
     *
     * @return _more_
     */
    public String getMimeType(OutputType output) {
        if (output.equals(OUTPUT_GALLERY) || output.equals(OUTPUT_PLAYER)
                || output.equals(OUTPUT_SLIDESHOW)) {
            return repository.getMimeTypeFromSuffix(".html");
        }
        return super.getMimeType(output);
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result makeResult(Request request, Group group,
                              List<Entry> entries)
            throws Exception {

        StringBuffer sb     = new StringBuffer();
        OutputType   output = request.getOutput();
        if (entries.size() == 0) {
            sb.append("<b>Nothing Found</b><p>");
            return new Result("Query Results", sb, getMimeType(output));
        }

        if (output.equals(OUTPUT_GALLERY)) {
            sb.append("<table>");
        } else if (output.equals(OUTPUT_PLAYER)) {
            if ( !request.exists(ARG_ASCENDING)) {
                entries = getEntryManager().sortEntriesOnDate(entries, true);
            }
        }

        int    col        = 0;
        String firstImage = "";
        if (output.equals(OUTPUT_PLAYER)) {
            int cnt = 0;
            for (int i = entries.size() - 1; i >= 0; i--) {
                Entry  entry = entries.get(i);
                String url   = getImageUrl(request, entry);
                if (url == null) {
                    continue;
                }
                if (cnt == 0) {
                    firstImage = url;
                }
                String entryUrl = getEntryLink(request, entry);
                String title =
                    "<table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\">";
                title += "<tr><td><b>Image:</b> " + entryUrl
                         + "</td><td align=right>"
                         + new Date(entry.getStartDate());
                title += "</table>";
                title = title.replace("\"", "\\\"");
                sb.append("addImage(" + HtmlUtil.quote(url) + ","
                          + HtmlUtil.quote(title) + ");\n");
                cnt++;
            }
        } else if (output.equals(OUTPUT_SLIDESHOW)) {
            for (int i = entries.size() - 1; i >= 0; i--) {
                Entry entry = entries.get(i);
                if ( !entry.getResource().isImage()) {
                    continue;
                }
                String url = HtmlUtil.url(
                                 request.url(repository.URL_ENTRY_GET) + "/"
                                 + getStorageManager().getFileTail(
                                     entry), ARG_ENTRYID, entry.getId());
                String thumburl = HtmlUtil.url(
                                      request.url(repository.URL_ENTRY_GET)
                                      + "/"
                                      + getStorageManager().getFileTail(
                                          entry), ARG_ENTRYID, entry.getId(),
                                              ARG_IMAGEWIDTH, "" + 100);
                String entryUrl = getEntryLink(request, entry);
                request.put(ARG_OUTPUT, OutputHandler.OUTPUT_HTML);
                String title = entry.getTypeHandler().getEntryContent(entry,
                                   request, true, false).toString();
                request.put(ARG_OUTPUT, output);
                title = title.replace("\"", "\\\"");
                title = title.replace("\n", " ");
                sb.append("addImage(" + HtmlUtil.quote(url) + ","
                          + HtmlUtil.quote(thumburl) + ","
                          + HtmlUtil.quote(title) + ");\n");

            }
        } else {
            int cnt = 0;
            for (Entry entry : entries) {
                String url = getImageUrl(request, entry);
                if (url == null) {
                    continue;
                }
                /*
                if(cnt==0) {
                    sb.append(HtmlUtil.href(url,"View Gallery","  rel=\"shadowbox[gallery]\" "));
                } else {
                    sb.append(HtmlUtil.href(url,entry.getName(),"  rel=\"shadowbox[gallery]\" class=\"hidden\" "));
                }
                cnt++;
                sb.append(HtmlUtil.br());
                if(true)
                    continue;
                */
                if (col >= 2) {
                    sb.append("</tr>");
                    col = 0;
                }
                if (col == 0) {
                    sb.append("<tr valign=\"bottom\">");
                }
                col++;

                sb.append("<td>");
                String imgExtra = "";
                //                String imgExtra = XmlUtil.attr(ARG_WIDTH, "400");
                String imageUrl = url + "&imagewidth="
                                  + request.get(ARG_IMAGEWIDTH, 400);
                sb.append(HtmlUtil.href(url,
                                        (HtmlUtil.img(imageUrl, "",
                                            imgExtra))));
                sb.append("<br>\n");
                sb.append(getEntryLink(request, entry));
                sb.append(" " + new Date(entry.getStartDate()));
                sb.append("<p></td>");
            }
        }


        if (output.equals(OUTPUT_GALLERY)) {
            sb.append("</table>\n");
        } else if (output.equals(OUTPUT_PLAYER)) {
            String playerTemplate =
                repository.getResource(PROP_HTML_IMAGEPLAYER);
            String widthAttr = "";
            int    width     = request.get(ARG_WIDTH, 600);
            if (width > 0) {
                widthAttr = HtmlUtil.attr(HtmlUtil.ATTR_WIDTH, "" + width);
            }
            String imageHtml = "<IMG NAME=\"animation\" BORDER=\"0\" "
                               + widthAttr + HtmlUtil.attr("SRC", firstImage)
                               + " ALT=\"image\">";

            String tmp = playerTemplate.replace("${imagelist}",
                             sb.toString());
            tmp = tmp.replace("${imagehtml}", imageHtml);
            tmp = StringUtil.replace(tmp, "${root}", repository.getUrlBase());
            String fullUrl = "";
            if (width > 0) {
                request.put(ARG_WIDTH, "0");
                fullUrl = HtmlUtil.href(request.getUrl(),
                                        msg("Use image width"));
            } else {
                request.put(ARG_WIDTH, "600");
                fullUrl = HtmlUtil.href(request.getUrl(),
                                        msg("Use fixed width"));
            }

            sb = new StringBuffer(HtmlUtil.leftRight(getSortLinks(request),
                    fullUrl));
            sb.append(tmp);
        } else if (output.equals(OUTPUT_SLIDESHOW)) {
            String template = repository.getResource(PROP_HTML_SLIDESHOW);
            template = template.replace("${imagelist}", sb.toString());
            template = StringUtil.replace(template, "${root}",
                                          repository.getUrlBase());
            sb = new StringBuffer(template);
        }
        StringBuffer finalSB = new StringBuffer();
        showNext(request, new ArrayList<Group>(), entries, finalSB);

        finalSB.append(HtmlUtil.p());
        finalSB.append(sb);
        return new Result(group.getName(), finalSB, getMimeType(output));
    }




}

