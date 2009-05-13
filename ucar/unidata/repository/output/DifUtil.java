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


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class DifUtil {
    public static final String HEADER_ARGS = "xmlns=\"\"\nxmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\nxsi:schemaLocation=\"http://gcmd.gsfc.nasa.gov/Aboutus/xml/dif/ http://gcmd.nasa.gov/Aboutus/xml/dif/dif_v9.7.1.xsd\">\n";


    public static final String TAG_DIF  = "DIF";
    public static final String TAG_Entry_ID = "Entry_ID";
    public static final String TAG_Entry_Title = "Entry_Title";
    public static final String TAG_ISO_Topic_Category = "ISO_Topic_Category";

    public static final String TAG_Data_Set_Citation = "Data_Set_Citation";
    public static final String TAG_Dataset_Creator = "Dataset_Creator";
    public static final String TAG_Dataset_Title = "Dataset_Title";
    public static final String TAG_Dataset_Series_Name = "Dataset_Series_Name";
    public static final String TAG_Dataset_Release_Date = "Dataset_Release_Date";
    public static final String TAG_Dataset_Release_Place = "Dataset_Release_Place";

    public static final String TAG_Dataset_Publisher = "Dataset_Publisher";
    public static final String TAG_Version = "Version";
    public static final String TAG_Issue_Identification = "Issue_Identification";
    public static final String TAG_Data_Presentation_Form = "Data_Presentation_Form";
    public static final String TAG_Other_Citation_Details = "Other_Citation_Details";
    public static final String TAG_Online_Resource = "Online_Resource";
    
    public static final String TAG_Personnel = "Personnel";
    public static final String TAG_Role = "Role";

    public static final String TAG_First_Name = "First_Name";
    public static final String TAG_Middle_Name = "Middle_Name";
    public static final String TAG_Last_Name = "Last_Name";
    public static final String TAG_Email = "Email";
    public static final String TAG_Phone = "Phone";
    public static final String TAG_Fax = "Fax";
    public static final String TAG_Contact_Address = "Contact_Address";
    public static final String TAG_Address = "Address";
    public static final String TAG_City = "City";
    public static final String TAG_Province_or_State = "Province_or_State";
    public static final String TAG_Postal_Code = "Postal_Code";
    public static final String TAG_Country = "Country";
        
    
    public static final String TAG_Discipline = "Discipline";
    public static final String TAG_Discipline_Name = "Discipline_Name";
    public static final String TAG_Subdiscipline = "Subdiscipline";
    public static final String TAG_Detailed_Subdiscipline = "Detailed_Subdiscipline";

    
    public static final String TAG_Parameters = "Parameters";
    public static final String TAG_Category = "Category";
    public static final String TAG_Topic = "Topic";
    public static final String TAG_Term = "Term";
    public static final String TAG_Variable_Level_1 = "Variable_Level_1";
    public static final String TAG_Variable_Level_2 = "Variable_Level_2";
    public static final String TAG_Variable_Level_3 = "Variable_Level_3";
    public static final String TAG_Detailed_Variable = "Detailed_Variable";

    

    public static final String TAG_Keyword = "Keyword";
    public static final String TAG_Sensor_Name = "Sensor_Name";
    public static final String TAG_Short_Name = "Short_Name";
    public static final String TAG_Long_Name = "Long_Name";
    
    public static final String TAG_Source_Name = "Source_Name";

    
    public static final String TAG_Temporal_Coverage = "Temporal_Coverage";
    public static final String TAG_Start_Date = "Start_Date";
    public static final String TAG_Stop_Date = "Stop_Date";
    
    public static final String TAG_Paleo_Temporal_Coverage = "Paleo_Temporal_Coverage";
    public static final String TAG_Paleo_Start_Date = "Paleo_Start_Date";
    public static final String TAG_Paleo_Stop_Date = "Paleo_Stop_Date";

    public static final String TAG_Chronostratigraphic_Unit = "Chronostratigraphic_Unit";
    public static final String TAG_Eon = "Eon";
    public static final String TAG_Era = "Era";
    public static final String TAG_Period = "Period";
    public static final String TAG_Epoch = "Epoch";
    public static final String TAG_Stage = "Stage";
    public static final String TAG_Detailed_Classification = "Detailed_Classification";
        
    

    public static final String TAG_Data_Set_Progress = "Data_Set_Progress";
    public static final String TAG_Spatial_Coverage = "Spatial_Coverage";
    public static final String TAG_Southernmost_Latitude = "Southernmost_Latitude";
    public static final String TAG_Northernmost_Latitude = "Northernmost_Latitude";
    public static final String TAG_Westernmost_Longitude = "Westernmost_Longitude";
    public static final String TAG_Easternmost_Longitude = "Easternmost_Longitude";
    public static final String TAG_Minimum_Altitude = "Minimum_Altitude";
    public static final String TAG_Maximum_Altitude = "Maximum_Altitude";
    public static final String TAG_Minimum_Depth = "Minimum_Depth";

    public static final String TAG_Maximum_Depth = "Maximum_Depth";
    
    public static final String TAG_Location = "Location";
    public static final String TAG_Location_Category = "Location_Category";
    public static final String TAG_Location_Type = "Location_Type";
    public static final String TAG_Location_Subregion1 = "Location_Subregion1";
    public static final String TAG_Location_Subregion2 = "Location_Subregion2";
    public static final String TAG_Location_Subregion3 = "Location_Subregion3";
    public static final String TAG_Detailed_Location = "Detailed_Location";

    
    public static final String TAG_Data_Resolution = "Data_Resolution";
    public static final String TAG_Latitude_Resolution = "Latitude_Resolution";
    public static final String TAG_Longitude_Resolution = "Longitude_Resolution";
    public static final String TAG_Horizontal_Resolution_Range = "Horizontal_Resolution_Range";
    public static final String TAG_Vertical_Resolution = "Vertical_Resolution";
    public static final String TAG_Vertical_Resolution_Range = "Vertical_Resolution_Range";
    public static final String TAG_Temporal_Resolution = "Temporal_Resolution";
    public static final String TAG_Temporal_Resolution_Range = "Temporal_Resolution_Range";

    
    public static final String TAG_Project = "Project";

    
    public static final String TAG_Quality = "Quality";
    public static final String TAG_Access_Constraints = "Access_Constraints";
    public static final String TAG_Use_Constraints = "Use_Constraints";
    public static final String TAG_Data_Set_Language = "Data_Set_Language";

    public static final String TAG_Originating_Center = "Originating_Center";
    public static final String TAG_Data_Center = "Data_Center";
    public static final String TAG_Data_Center_Name = "Data_Center_Name";

        
    public static final String TAG_Data_Center_URL = "Data_Center_URL";
    public static final String TAG_Data_Set_ID = "Data_Set_ID";
        
    
    public static final String TAG_Distribution = "Distribution";
    public static final String TAG_Distribution_Media = "Distribution_Media";

    public static final String TAG_Distribution_Size = "Distribution_Size";
    public static final String TAG_Distribution_Format = "Distribution_Format";
    public static final String TAG_Fees = "Fees";
    
    public static final String TAG_Multimedia_Sample = "Multimedia_Sample";
    public static final String TAG_File = "File";
    public static final String TAG_Format = "Format";
    public static final String TAG_Caption = "Caption";

    public static final String TAG_Description = "Description";
    
    public static final String TAG_Reference = "Reference";
    public static final String TAG_Summary = "Summary";
    public static final String TAG_Related_URL = "Related_URL";
    public static final String TAG_URL_Content_Type = "URL_Content_Type";
    public static final String TAG_Type = "Type";
    public static final String TAG_Subtype = "Subtype";


        

    public static final String TAG_URL = "URL";

    
    public static final String TAG_Parent_DIF = "Parent_DIF";
    public static final String TAG_IDN_Node = "IDN_Node";

    
    public static final String TAG_Originating_Metadata_Node = "Originating_Metadata_Node";

    public static final String TAG_Metadata_Name = "Metadata_Name";
    public static final String TAG_Metadata_Version = "Metadata_Version";
    public static final String TAG_DIF_Creation_Date = "DIF_Creation_Date";
    public static final String TAG_Last_DIF_Revision_Date = "Last_DIF_Revision_Date";
    public static final String TAG_DIF_Revision_History = "DIF_Revision_History";
    public static final String TAG_Future_DIF_Review_Date = "Future_DIF_Review_Date";
    public static final String TAG_Private = "Private";

}
