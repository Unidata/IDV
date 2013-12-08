package ucar.unidata.data.imagery;

import edu.wisc.ssec.mcidas.adde.AddeImageURL;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: yuanho
 * Date: 12/7/13
 * Time: 10:45 AM
 * To change this template use File | Settings | File Templates.
 */
public class AddeImageURLInfo extends AddeImageURL {
    public double locationLat;
    public double locationLon;
    public int locationLine;
    public int locationElem;

    public AddeImageURLInfo(){

    }

    AddeImageURLInfo(String  sourceURL){
        decodeSourceURL(sourceURL);

       // AddeImageURLInfo(this.host, requestType, group, descriptor, locateKey, locateValue,
       //         placeValue, lines, elements,  lmag,  emag, band, unit, spacing);
    }

    private void decodeSourceURL(String baseURL) {
        try {
            URL url = new URL(baseURL);
            setHost(url.getHost());
            setRequestType(url.getPath().substring(1).toLowerCase());
            String query = url.getQuery();
            setExtraKeys(query);
        } catch (MalformedURLException mue) {}
    }

    public void setLocationLat(double lat){
        String locKey = getLocateKey();


        if(locKey.equals(AddeImageURL.KEY_LATLON)){
            String locationStr = getLocateValue();
            List<String> locList = StringUtil.split(locationStr, " ");
            this.locationLat = lat;
            String locValue = Misc.format(lat) + " " + locList.get(1);
            setLocateValue(locValue);
        }
    }

    public void setLocationLon(double lon){
        String locKey = getLocateKey();

        if(locKey.equals(AddeImageURL.KEY_LATLON)){
            String locationStr = getLocateValue();
            List<String> locList = StringUtil.split(locationStr, " ");
            this.locationLon = lon;
            String locValue = locList.get(0)  + " " + Misc.format(lon);
            setLocateValue(locValue);
        }
    }

    public void setLocationLine(int line){
        String locKey = getLocateKey();
        if(locKey.equals(AddeImageURL.KEY_LINEELE)){
            String locationStr = getLocateValue();
            List<String> locList = StringUtil.split(locationStr, " ");
            this.locationLine = line;
            String locValue = line + " " + locList.get(1);
            setLocateValue(locValue);
        }

    }

    public void setLocationElem(int elem){
        String locKey = getLocateKey();
        if(locKey.equals(AddeImageURL.KEY_LINEELE)){
            String locationStr = getLocateValue();
            List<String> locList = StringUtil.split(locationStr, " ");
            this.locationElem = elem;
            String locValue = locList.get(0) + " " + elem;
            setLocateValue(locValue);
        }

    }

    public double getLocationLat(){
        return locationLat;
    }

    public double getLocationon(){
        return locationLon;
    }

    public int getLocationLine(){
        return locationLine;
    }

    public int getLocationElem(){
        return locationElem;
    }

    public void remove(){

    }
}
