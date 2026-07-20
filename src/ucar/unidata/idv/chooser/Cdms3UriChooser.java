package ucar.unidata.idv.chooser;

import org.w3c.dom.Element;
import ucar.unidata.data.DataManager;
import ucar.unidata.io.s3.CdmS3Uri;
import ucar.unidata.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

//import static ucar.unidata.data.hydra.MODIS_L1B_Utility.k;


/**
 * Allows the user to select a url as a data source
 *
 * @author IDV development team
 * @version $Revision: 1.40 $Date: 2007/07/27 13:53:08 $
 */


public class Cdms3UriChooser extends IdvChooser implements ActionListener {

    /** Property name to get the list or urls */
    public static final String PREF_URLLIST = "idv.urllist";
    public static final String PREF_KEYLIST = "idv.keylist";
    public static final String PREF_BUCKETLIST = "idv.bucketlist";
    public static final String SCHEME_CDM_S3 = "cdms3";
    private static final String schemeCdmS3 = "cdms3";
    /** Manages the pull down list of urls */
    private PreferenceList prefList;
    private PreferenceList keyList;
    private PreferenceList bucketList;


    /** The list of urls */
    private JComboBox box;
    private JComboBox boxk;
    private JComboBox boxb;


    /** The text area for multi-line urls */

    private JTextField  profileField;
    private JTextField  endpointField;
    private JTextField  endpointSegField;
    /** text scroller */
    private JScrollPane textScroller;


    /** Holds the combo box */
    private JComponent urlPanel;
    private JComponent bucketPanel;
    private JComponent keyPanel;
    private JComponent profilePanel;
    private JComponent endpointPanel;
    /** Holds the text area */
    protected JComboBox hostComboBox;

    /** Are we showing the combo box */
    private boolean showBox = true;

    /** panel */
    private GuiUtils.CardLayoutPanel cardLayoutPanel;
    private GuiUtils.CardLayoutPanel cardLayoutPanel1;
    private GuiUtils.CardLayoutPanel cardLayoutPanel2;
    /** _more_          */
    private JLabel urlLabel;


    /**
     * Create the <code>UrlChooser</code>
     *
     * @param mgr The <code>IdvChooserManager</code>
     * @param root  The xml root that defines this chooser
     *
     */
    public Cdms3UriChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean canDoUpdate() {
        return false;
    }


    /**
     * Get the tooltip for the load button
     *
     * @return The tooltip for the load button
     */
    protected String getLoadToolTip() {
        return "Load the URL";
    }



    /**
     * Create the GUI
     *
     * @return The GUI
     */
    protected JComponent doMakeContents() {
        JComponent dsComp = getDataSourcesComponent();

        prefList = getPreferenceList(PREF_URLLIST);
        keyList = getPreferenceList(PREF_KEYLIST);
        bucketList = getPreferenceList(PREF_BUCKETLIST);

        JComponent urlButtons = getDefaultButtons();

        box = prefList.createComboBox(CMD_LOAD, this);
        boxk = keyList.createComboBox(CMD_LOAD, this);
        boxb = bucketList.createComboBox(CMD_LOAD, this);


        JPanel boxWrapper = GuiUtils.doLayout(new Component[] { box }, 1,
                GuiUtils.WT_Y, GuiUtils.WT_N);
        JPanel boxkWrapper = GuiUtils.doLayout(new Component[] { boxk }, 1,
                GuiUtils.WT_Y, GuiUtils.WT_N);
        JPanel boxbWrapper = GuiUtils.doLayout(new Component[] { boxb }, 1,
                GuiUtils.WT_Y, GuiUtils.WT_N);

        boxWrapper.setPreferredSize(new Dimension(200, 40));
        boxkWrapper.setPreferredSize(new Dimension(200, 40));
        boxbWrapper.setPreferredSize(new Dimension(200, 40));

        urlLabel  = GuiUtils.rLabel("URL:");
        urlPanel = GuiUtils.top(boxWrapper);
        urlPanel.setPreferredSize(new Dimension(350, 10));
        bucketPanel = GuiUtils.top(boxbWrapper);
        bucketPanel.setPreferredSize(new Dimension(150, 10));
        keyPanel = GuiUtils.top(boxkWrapper);
        keyPanel.setPreferredSize(new Dimension(350, 40));

        //textPanel = textScroller;

        profileField = new JTextField(30);
        profileField.setToolTipText("example:user_name");
        endpointField = new JTextField(30);
        endpointField.setToolTipText("example:my.endpoint.edu");
        endpointSegField = new JTextField(30);
        endpointSegField.setToolTipText("example:endpoint/path/");
        cardLayoutPanel = new GuiUtils.CardLayoutPanel();
        cardLayoutPanel.addCard(urlPanel);
        cardLayoutPanel1 = new GuiUtils.CardLayoutPanel();
        cardLayoutPanel1.setToolTipText("bucket-name");
        cardLayoutPanel1.addCard(bucketPanel);
        cardLayoutPanel2 = new GuiUtils.CardLayoutPanel();
        cardLayoutPanel2.setToolTipText("super/long/key");
        cardLayoutPanel2.addCard(keyPanel);



        //cardLayoutPanel.addCard(textPanel);
        hostComboBox = new JComboBox();
        GuiUtils.setListData(
                hostComboBox,
                Misc.newList(
                        new TwoFacedObject("Endpoint", "Endpoint"),
                        new TwoFacedObject("Amazon", "AWS"),
                        new TwoFacedObject("Microsoft", "Azure"),
                        new TwoFacedObject("Google", "GCS")));

        hostComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateEndpointFields();
            }
        });

        JComponent mainContents = GuiUtils.formLayout(new Component[] {
                GuiUtils.rLabel("Data Source Type:"),
                GuiUtils.left(dsComp),

                GuiUtils.rLabel("Host:"),
                GuiUtils.left(hostComboBox),
                GuiUtils.rLabel("Profile:"),
                GuiUtils.left(profileField),
                GuiUtils.rLabel("Endpoint Host:"),
                GuiUtils.left(endpointField),
                GuiUtils.rLabel("Endpoint Segments:"),
                GuiUtils.left(endpointSegField),

                //GuiUtils.top(GuiUtils.inset(urlLabel,
                //        new Insets(10, 0, 0, 0))),
                //cardLayoutPanel,
                GuiUtils.top(GuiUtils.inset(GuiUtils.rLabel("Bucket:"),
                        new Insets(10, 0, 0, 0))),
                bucketPanel,
                GuiUtils.top(GuiUtils.inset(GuiUtils.rLabel("Key:"),
                        new Insets(10, 0, 0, 0))),
                cardLayoutPanel2,

                 }, GRID_INSETS);

        mainContents = GuiUtils.vbox(mainContents, urlButtons);
        setHaveData(true);
        setStatus("Press \"" + CMD_LOAD + "\" to load the URL", "buttons");

        cardLayoutPanel.revalidate();
        cardLayoutPanel.repaint();

        return GuiUtils.top(mainContents);
    }

    private void updateEndpointFields() {
        Object selected = hostComboBox.getSelectedItem();
        boolean isEndpoint = false;

        if (selected instanceof TwoFacedObject) {
            TwoFacedObject tfo = (TwoFacedObject) selected;
            isEndpoint = "Endpoint".equals(tfo.toString())
                    || "Endpoint".equals(String.valueOf(tfo.getId()));
        } else if (selected != null) {
            isEndpoint = "Endpoint".equals(selected.toString());
        }

        endpointField.setEnabled(isEndpoint);
        endpointSegField.setEnabled(isEndpoint);
    }


    /**
     * _more_
     *
     * @param msg _more_
     * @param what _more_
     */
    public void setStatus(String msg, String what) {
        super.setStatus("Press \"" + CMD_LOAD + "\" to load the URL",
                "buttons");
    }



    /**
     * Wrapper around {@see #loadURLInner()}, showing the wait cursor
     */
    private void loadURL() {
        showWaitCursor();
        loadURLInner();
        showNormalCursor();
    }

    /**
     * Load the url
     *  * Example cdms3 URIs:
     *
     *   cdms3://profile_name@my.endpoint.edu/endpoint/path/bucket-name?super/long/key</li>
     *   cdms3://profile_name@my.endpoint.edu/bucket-name?super/long/key</li>
     *   cdms3://my.endpoint.edu/endpoint/path/bucket-name?super/long/key</li>
     *   cdms3://my.endpoint.edu/bucket-name?super/long/key</li>
     *
     */
    private void loadURLInner() {
        String host =(String)((TwoFacedObject) hostComboBox.getSelectedItem()).getId();

        String url          = "";
        String dataSourceId = getDataSourceId();
        if (showBox) {
            if(host.equals("Endpoint")){
                String profile = profileField.getText().trim();
                profile = profile.isEmpty() ? null : profile;
                String endpoint = endpointField.getText().trim();
                endpoint = endpoint.isEmpty() ? null : endpoint;
                String endpointseg = endpointSegField.getText().trim();
                endpointseg = endpointseg.isEmpty() ? null : endpointseg;
                Object keyselectedItem = boxk.getSelectedItem();
                Object bucketselectedItem = boxb.getSelectedItem();
                String key = null;
                String bucket = null;

                if(endpoint == null || endpoint.length() == 0){
                    userMessage("Please specify endpoint and path");
                    return;
                }
                if(keyselectedItem != null){
                    key = keyselectedItem.toString().trim();
                    key = key.startsWith("/") ? key.substring(1) : key;
                }
                if(bucketselectedItem != null){
                    bucket = bucketselectedItem.toString().trim();
                    bucket = bucket.startsWith("/") ? bucket.substring(1) : bucket;
                }

                if(key == null || bucket == null){
                    userMessage("Please specify a key and bucket");
                    return;
                }
                CdmS3Uri cdmS3Uri = null;
                try {
                    if (profile != null && endpointseg != null) {
                        cdmS3Uri = new CdmS3Uri(schemeCdmS3 + "://" + profile + "@" + endpoint + "/" + endpointseg +  bucket + "?" + key);
                    } else if ( endpointseg != null) {
                        cdmS3Uri = new CdmS3Uri(schemeCdmS3 + "://" + endpoint + "/" + endpointseg +  bucket + "?" + key);
                    } else   {
                        cdmS3Uri = new CdmS3Uri(schemeCdmS3 + "://" + endpoint +  "/" + bucket +  "?" + key);
                    }

                } catch (Exception e) {}
                if(cdmS3Uri == null){
                    userMessage("Please specify a valid url");
                    return;
                }
                url = cdmS3Uri.toString();

            } else if(host.equals("AWS")){
                String profile = profileField.getText().trim();
                profile = profile.isEmpty() ? null : profile;
                String endpoint = endpointField.getText().trim();
                endpoint = endpoint.isEmpty() ? null : endpoint;
                Object keyselectedItem = boxk.getSelectedItem();
                Object bucketselectedItem = boxb.getSelectedItem();
                String key = null;
                String bucket = null;

                if(keyselectedItem != null){
                    key = keyselectedItem.toString().trim();
                    key = key.startsWith("/") ? key.substring(1) : key;
                }
                if(bucketselectedItem != null){
                    bucket = bucketselectedItem.toString().trim();
                    bucket = bucket.startsWith("/") ? bucket.substring(1) : bucket;
                }

                if(key == null || bucket == null){
                    userMessage("Please specify a key and bucket");
                    return;
                }
                CdmS3Uri cdmS3Uri = null;
                try {
                    if (profile != null) {
                        cdmS3Uri = new CdmS3Uri(schemeCdmS3 + "://" + profile + "@aws" + "/" + bucket +  "?" + key);
                    } else {
                        cdmS3Uri = new CdmS3Uri(schemeCdmS3 + ":"  +  bucket + "?" + key);
                    }
                } catch (Exception e) {}
                if(cdmS3Uri == null){
                    userMessage("Please specify a valid url");
                    return;
                }
                url = cdmS3Uri.toString();

            } else if(host.equals("GCS")){
                String profile = profileField.getText().trim();
                profile = profile.isEmpty() ? null : profile;
                String endpoint = endpointField.getText().trim();  //storage.googleapis.com
                endpoint = endpoint.isEmpty() ? null : endpoint;
                Object keyselectedItem = boxk.getSelectedItem();
                Object bucketselectedItem = boxb.getSelectedItem(); //gcp-public-data-goes-16
                String key = null;
                String bucket = null;

                if(keyselectedItem != null){
                    key = keyselectedItem.toString().trim();
                    key = key.startsWith("/") ? key.substring(1) : key;
                }
                if(bucketselectedItem != null){
                    bucket = bucketselectedItem.toString().trim();
                    bucket = bucket.startsWith("/") ? bucket.substring(1) : bucket;
                }

                if(key == null || bucket == null){
                    userMessage("Please specify a key and bucket");
                    return;
                }
                CdmS3Uri cdmS3Uri = null;
                try {
                    if (profile != null) {
                        cdmS3Uri = new CdmS3Uri(schemeCdmS3 + "://" + profile + "@" + "storage.googleapis.com" + "/" + bucket +  "?" + key);
                    } else {
                        cdmS3Uri = new CdmS3Uri(schemeCdmS3 + "://" + "storage.googleapis.com" + "/" + bucket + "?" + key);
                    }
                } catch (Exception e) {}
                if(cdmS3Uri == null){
                    userMessage("Please specify a valid url");
                    return;
                }
                url = cdmS3Uri.toString();

            } else {

                if ((url.length() == 0) && (dataSourceId == null)) {
                    userMessage("Please specify a url");
                    return;
                }
            }
        }

        Hashtable properties = new Hashtable();
        if (dataSourceId != null) {
            properties.put(DataManager.DATATYPE_ID, dataSourceId);
        }

        if (showBox) {
            if (idv.handleAction(url, properties)) {
                closeChooser();
                prefList.saveState(box);
                keyList.saveState(boxk);
                bucketList.saveState(boxb);
            }
        }
    }

    /**
     * Handle the action event from the GUI
     */
    public void doLoadInThread() {
        loadURL();
    }



}
