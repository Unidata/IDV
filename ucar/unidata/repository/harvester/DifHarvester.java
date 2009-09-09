/**
 * $Id: TrackDataSource.java,v 1.90 2007/08/06 17:02:27 jeffmc Exp $
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

package ucar.unidata.repository.harvester;


import org.w3c.dom.*;

import ucar.unidata.repository.*;
import ucar.unidata.repository.data.*;
import ucar.unidata.repository.metadata.*;

import ucar.unidata.sql.SqlUtil;


import ucar.unidata.util.CatalogUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;



import java.io.*;



import java.net.*;


import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;




/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class DifHarvester extends Harvester {

    /*
<?xml version="1.0" encoding="ISO-8859-1" standalone="yes"?>
<DIF>
  <Entry_ID>
  <Entry_Title>
  <Data_Set_Citation>
    <Dataset_Creator>
    <Dataset_Title>
    <Dataset_Release_Date>
    <Dataset_Release_Place>
    <Dataset_Publisher>
    <Online_Resource>

  <Personnel>
    <Role>
    <First_Name>
    <Last_Name>
    <Email>
    <Phone>
    <Fax>
    <Contact_Address>
      <Address>
      <City>
      <Province_or_State>
      <Postal_Code>
      <Country>
    </Contact_Address>
  </Personnel>
  <Personnel>

    <Role>Investigator</Role>
    <First_Name>R.</First_Name>
    <Middle_Name>G.</Middle_Name>
    <Last_Name>Barry</Last_Name>
    <Contact_Address>
      <Address>449 UCB</Address>

      <City>Boulder</City>
      <Province_or_State>CO</Province_or_State>
      <Postal_Code>80309-0449</Postal_Code>
      <Country>USA</Country>
    </Contact_Address>
  </Personnel>
  <Personnel>

    <Role>Technical Contact</Role>
    <First_Name>NSIDC</First_Name>
    <Last_Name>User Services</Last_Name>
    <Email>nsidc@nsidc.org</Email>
    <Phone>(303) 492-6199</Phone>
    <Fax>(303) 492-2468</Fax>

    <Contact_Address>
      <Address>National Snow and Ice Data Center</Address>
      <Address>CIRES, 449 UCB</Address>
      <Address>University of Colorado</Address>
      <City>Boulder</City>
      <Province_or_State> CO</Province_or_State>

      <Postal_Code>80309-0449</Postal_Code>
      <Country>USA</Country>
    </Contact_Address>
  </Personnel>
  <Parameters>
    <Category>EARTH SCIENCE</Category>
    <Topic>Cryosphere</Topic>

    <Term>Frozen Ground</Term>
    <Variable_Level_1>Active Layer</Variable_Level_1>
  </Parameters>
  <Parameters>
    <Category>EARTH SCIENCE</Category>
    <Topic>Land Surface</Topic>
    <Term>Frozen Ground</Term>

    <Variable_Level_1>Active Layer</Variable_Level_1>
    <Detailed_Variable>freeze/thaw depth</Detailed_Variable>
  </Parameters>
  <ISO_Topic_Category>Geoscientific Information</ISO_Topic_Category>
  <Keyword>edaphic factor</Keyword>
  <Keyword>freeze depth</Keyword>

  <Keyword>Freezing Degree Days</Keyword>
  <Keyword>Freezing Index</Keyword>
  <Keyword>freezing index</Keyword>
  <Keyword>Seasonal Thaw Layer</Keyword>
  <Keyword>Stefan solution</Keyword>
  <Keyword>Thaw Depth</Keyword>

  <Keyword>Thaw Depth Measurements</Keyword>
  <Keyword>Thaw Depths</Keyword>
  <Keyword>Thawing Degree Days</Keyword>
  <Keyword>Thawing Degree-day</Keyword>
  <Keyword>Thawing Index</Keyword>
  <Temporal_Coverage>

    <Start_Date>1901-01-01</Start_Date>
    <Stop_Date>2002-12-31</Stop_Date>
  </Temporal_Coverage>
  <Data_Set_Progress>COMPLETE</Data_Set_Progress>
  <Spatial_Coverage>
    <Southernmost_Latitude>50</Southernmost_Latitude>
    <Northernmost_Latitude>90</Northernmost_Latitude>

    <Westernmost_Longitude>-180</Westernmost_Longitude>
    <Easternmost_Longitude>180</Easternmost_Longitude>
  </Spatial_Coverage>
  <Location>
    <Location_Category>Geographic Region</Location_Category>
    <Location_Type>Arctic</Location_Type>
  </Location>

  <>The relationship between freeze/thaw depth and freezing and thawing indices has been demonstrated in several places. For example, Brown et al. (2000) review the relationship between thaw depth and the annual thawing index. Romanovsky and Osterkamp (1995), Zhang et al. (1997), Nelson et al. (1998), Klene et al. (2001), and Hinkel and Nelson (2003) all demonstrate the general validity of the approach. Nelson et al. (1997) used dthe apData_Set_LanguageQualityproach to map to map active layer depth for the Kuparuk River Basin in Alaska. Zhang et al. (2005b) also use the approach to map active layer depth in the Ob, Yenisey, and Lena River Basins of Russia.

The quality of the input freezing and thawing indices is described in Frauenfeld, et al. (submitted) and in the documentation for the data set. The investigators conclude they are adequate for broad scale analysis. The difficulty lies in calculating the edaphic factors. There is some uncertainty in the estimation of the edaphic factors as discussed in Zhang et al. (2005b). Overall this approach is reasonable for broad scale calculations and for estimating the response of freeze and thaw depth to different climatological factors.</Quality>Data_Center
  <>English</Data_Set_Language>
  <>NSIDC_FGDC</Originating_Center>
  <Data_Center>
    <Data_Center_Name>
      <Short_Name>NSIDC_FGDC</Short_Name>
      <Long_Name>NSIDC Frozen Ground Data Center</Long_Name>
    </Data_Center_Name>
    <Data_Center_URL>http://nsidc.org/fgdc/</Data_Center_URL>
    <Data_Set_ID>GGD651</Data_Set_ID>
    <Personnel>Originating_Center
      <Role>Data Center Contact</Role>
      <First_Name>NSIDC FGDC</First_Name>
      <Last_Name>User Services</Last_Name>

      <Email>nsidc@nsidc.org</Email>
      <Phone>+1 (303) 492-6199</Phone>
      <Fax>+1 (303) 492-2468</Fax>
      <Contact_Address>
        <Address>National Snow and Ice Data Center</Address>
        <Address>CIRES, 449 UCB</Address>

        <Address>University of Colorado</Address>
        <City>Boulder</City>
        <Province_or_State>CO</Province_or_State>
        <Postal_Code>80309-0449</Postal_Code>
        <Country>USA</Country>
      </Contact_Address>

    </Personnel>
  </Data_Center>
  <Data_Center>
    <Data_Center_Name>
      <Short_Name>WDC/GLACIOLOGY, BOULDER</Short_Name>
      <Long_Name>World Data Center for Glaciology, Boulder</Long_Name>
    </Data_Center_Name>
    <Data_Center_URL>http://nsidc.org/wdc/</Data_Center_URL>

    <Data_Set_ID>GGD651</Data_Set_ID>
    <Personnel>
      <Role>Data Center Contact</Role>
      <First_Name>NSIDC</First_Name>
      <Last_Name>USER SERVICES</Last_Name>
      <Email>nsidc@nsidc.org</Email>

      <Phone>+1 (303) 492-6199</Phone>
      <Fax>+1 (303) 492-2468</Fax>
      <Contact_Address>
        <Address>National Snow and Ice Data Center</Address>
        <Address>CIRES, 449 UCB</Address>
        <Address>University of Colorado</Address>

        <City>Boulder</City>
        <Province_or_State>CO</Province_or_State>
        <Postal_Code>80309-0449</Postal_Code>
        <Country>USA</Country>
      </Contact_Address>
    </Personnel>
  </Data_Center>

  <Distribution>
    <Distribution_Media>FTP</Distribution_Media>
    <Distribution_Size>479.4 MB</Distribution_Size>
    <Distribution_Format>ASCII text</Distribution_Format>
  </Distribution>
  <Reference>Brown, J., K. M. Hinkel, and F. E. Nelson. 2000. The Circumpolar Active Layer Monitoring (CALM) program: Research designs and initial results. Polar Geogr., 24(3), 163-258.

Frauenfeld, O. W., T. Zhang, and J. L. McCreight. Submitted. Climatology and variability of the 20th century Northern Hemisphere freezing/thawing index. International Journal of Climatology.

Hinkel, K. M., and F. E. Nelson. 2003. Spatial and temporal patterns of active layer thickness at Circumpolar Active Layer Monitoring (CALM) sites in northern Alaska, 1995-2000. J. Geophys. Res., 108(D2), 8168. doi:10.1029/2001JD000927.

Klene, A. E., F. E. Nelson, N. I. Shiklomanov, and K. M. Hinkel. 2001. The N-factor in natural landscapes: Variability of air and soil-surface temperatures, Kuparuk River Basin, Alaska. Arct. Antarct. Alp. Res., 33(2), 140-148.

Knowles, K. 2004. EASE-Grid land cover data resampled from AVHRR Global 1 km land cover, Version 2, March 1992 - April 1993. Boulder CO, USA: National Snow and Ice Data Center. Digital Media.

Mitchell T. D. and P. D. Jones. 2005. An improved method of constructing a database of monthly climate observations and associated high-resolution grids. International Journal of Climatology 25, 693-712.  Nelson, F. E., N. I. Shiklomanov, G. R. Mueller, K. M. Hinkel, D. A. Walker, and J. G. Bockheim. 1997. Estimating active-layer thickness over a large region: Kuparuk River basin, Alaska, U.S.A. Arct. Alp. Res., 29(4), 367-378.

Nelson, F. E., and S. I. Outcalt. 1987. A computational method for prediction and regionalization of permafrost. Arct. Alp. Res., 19, 279-288.

Nelson, F. E., S. I. Outcalt, J. Brown, N. I. Shiklomanov, and K. M. Hinkel. 1998. Spatial and temporal attributes of the active-layer thickness record, Barrow, Alaska, U.S.A., in Lewkowicz, A. and M. Allard (editors). 1998. Proceedings of 7th International Conference on Permafrost pp. 797-802, Ste-Foy, Canada: Cent. d'Etudes Nordique, Univ. Laval.

Romanovsky, V. E., and T. E. Osterkamp. 1995. Interannual variations of the thermal regime of the active layer and near-surface permafrost in Northern Alaska. Permafrost Periglacial Proc., 6, 313-335.

Zhang, T., O. W. Frauenfeld, J. McCreight, and R. G. Barry. 2005a. Northern Hemisphere EASE-Grid annual freezing and thawing indices, 1901 - 2002. Boulder, CO: National Snow and Ice Data Center/World Data Center for Glaciology. Digital media.

Zhang, T., O. W. Frauenfeld, M. C. Serreze, A. Etringer, C. Oelke, J. McCreight, R. G. Barry, D. Gilichinsky, D. Yang, H. Ye, F. Ling, and S. Chudinova. 2005b. Spatial and temporal variability in active layer thickness over the Russian Arctic drainage basin. J Geophysical Research, Vol. 110, D16101. doi:10.1029/2004JD005642.

Zhang, T., T. E. Osterkamp, and K. Stamnes. 1997. Effects of climate on the active layer and permafrost on the north slope of Alaska, U.S.A. Permafrost Periglacial Proc., 8, 45-67.</Reference>

  <Summary>This data set contains mean, median, minimum and maximum freeze and thaw depths for each year from  1901 to 2002 on the 25 km resolution Equal-Area Scalable Earth Grid (EASE-Grid) for areas north of 50  deg. Freeze and thaw depths are estimated using a variant of the Stefan solution using an edaphic factor  and freezing or thawing indices as inputs. The edaphic factor is estimated based on different land surface  types; the freezing and thawing indices are from Northern Hemisphere EASE-Grid annual freezing  and thawing indices, 1901 - 2002 (Zhang, et al. 2005).

Two ASCII files are available for each year for freeze depth and thaw depth, respectively. Each file is  approximately 25.6 MB in size. In addition, there is one 10.5 MB ASCII file defining the latitude and longitude coordinates for each grid point. The data set is available via FTP as three compressed files.</Summary>

  <Related_URL>
    <URL_Content_Type>
      <Type>VIEW RELATED INFORMATION</Type>
    </URL_Content_Type>
    <URL>http://nsidc.org/data/ggd649.html</URL>
  </Related_URL>
  <Related_URL>
    <URL_Content_Type>

      <Type>VIEW RELATED INFORMATION</Type>
    </URL_Content_Type>
    <URL>http://newice.colorado.edu:8000/data/ease/</URL>
  </Related_URL>
  <Metadata_Name>CEOS IDN DIF</Metadata_Name>
  <Metadata_Version>9.7</Metadata_Version>
</DIF>

    */






    /** _more_ */
    Group topGroup;

    /** _more_ */
    String url;

    /**
     * _more_
     *
     * @param repository _more_
     * @param group _more_
     * @param url _more_
     * @param user _more_
     */
    public DifHarvester(Repository repository, Group group, String url,
                        User user) {
        super(repository);
        setName("DIF harvester");
        this.topGroup = group;
        this.url      = url;
        this.user     = user;
    }



    /**
     * _more_
     *
     *
     * @param timestamp _more_
     * @throws Exception _more_
     */
    protected void runInner(int timestamp) throws Exception {}

    /**
     * _more_
     *
     * @param difNode _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Metadata makeMetadata(Element difNode) throws Exception {
        Metadata     metadata = null;
        String       tag      = difNode.getTagName();

        MetadataType type     = null;

        if (type == null) {
            return null;
        }


        return metadata;
    }

    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static List<Metadata> ingestDif(String url) throws Exception {
        List<Metadata> results = new ArrayList<Metadata>();
        Element        root    = XmlUtil.getRoot(url, DifHarvester.class);
        if (root == null) {
            throw new IllegalArgumentException("Could not parse xml:" + url);
        }
        NodeList children = XmlUtil.getElements(root);
        for (int i = 0; i < children.getLength(); i++) {
            Element  node     = (Element) children.item(i);
            Metadata metadata = makeMetadata(node);
            if (metadata == null) {
                continue;
            }
            results.add(metadata);
        }
        return results;
    }

    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        try {
            ingestDif(
                "http://nsidc.org/cgi-bin/get_metadata.pl?id=G02169&format=DIF&style=XML");
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }




}

