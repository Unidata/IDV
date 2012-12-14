package ucar.unidata.util;

import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: lesserwhirls
 * Date: 12/14/12
 * Time: 8:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class UnidataTdsUrlRemapper {

    private static HashMap<String, Remapper> map;

    private void initMap() {
        List<UrlRemapperBean> beans = readUrlRemapFile("/ucar/unidata/idv/resources/tdsUrlMaps.xml");
        map = makeMapBeans(beans);
    }

    public List<String> getMappedUrlPaths(String oldUrlPath) {
        List<String> result = getMappedUrlPaths(oldUrlPath,null);
        return result;
    }

    public List<String> getMappedUrlPaths(String oldUrlPath,String urlType) {
        List<String> result = new ArrayList<String>();
        // look in our Url Remapper map
        if (map == null) {
            initMap();
        }

        Remapper mbean = map.get(oldUrlPath);
        if (mbean != null && mbean.newUrls != null)  {
            result.add(mbean.newUrl); // if its unique, then we are done
            return result;
        }

        // not unique - match against urlType (best, latest, or files)
        if (urlType != null) {
            if (mbean != null) {
                for (UrlRemapperBean r : mbean.newUrls) {
                    if (r.getUrlType().equals(urlType)) result.add(r.newUrlPath);
                }
            }
        }
        return result;
    }

    private List<UrlRemapperBean> readUrlRemapFile(String path) {
        java.util.List<UrlRemapperBean> beans = new ArrayList<UrlRemapperBean>(1000);
        System.out.printf("reading table %s%n", path);
        InputStream is = null;

        try {
            is = UnidataTdsUrlRemapper.class.getResourceAsStream(path);
            if (is == null) {
                System.out.println("Cant read file " + path);
                return null;
            }

            SAXBuilder builder = new SAXBuilder();
            org.jdom.Document doc = builder.build(is);
            org.jdom.Element root = doc.getRootElement();
            List<org.jdom.Element> dsElems = root.getChildren("urlMap");
            for (org.jdom.Element dsElem : dsElems) {
                String dsType = dsElem.getAttributeValue("type");
                List<org.jdom.Element> params = dsElem.getChildren("urlPath");
                for (org.jdom.Element elem : params) {
                    String oldUrlPath = elem.getAttributeValue("oldUrlPath");
                    String newUrlPath = elem.getAttributeValue("newUrlPath");
                    beans.add(new UrlRemapperBean(dsType,oldUrlPath,newUrlPath));
                }
            }
            return beans;

        } catch (IOException ioe) {
            ioe.printStackTrace();
            return null;

        } catch (JDOMException e) {
            e.printStackTrace();
            return null;

        } finally {
            if (is != null) try {
                is.close();
            } catch (IOException e) {
            }
        }
    }

    private String getNewName(HashMap<String, Remapper> map, String urlType, String oldUrlPath) {
        Remapper mbean = map.get(oldUrlPath);
        if (mbean == null) return null; // ??
        if (mbean.newUrl != null) return mbean.newUrl; // if its unique, then we are done

        for (UrlRemapperBean r : mbean.newUrls) {
            if (r.getUrlType().equals(urlType)) return r.getNewUrlPath();
        }

        return null;
    }

    public static class UrlRemapperBean implements Comparable<UrlRemapperBean> {
        String urlType, oldUrlPath, newUrlPath;

        // no-arg constructor
        public UrlRemapperBean() {
        }

        public UrlRemapperBean(String dsType, String oldUrlPath, String newUrlPath) {
            this.urlType = dsType;
            this.oldUrlPath = oldUrlPath;
            this.newUrlPath = newUrlPath;
        }

        public String getUrlType() {
            return urlType;
        }

        public String getOldUrlPath() {
            return oldUrlPath;
        }

        public String getNewUrlPath() {
            return newUrlPath;
        }

        public String getStatus() {
            //    if (oldName.equals(newName)) return "*";
            //    if (oldName.equalsIgnoreCase(newName)) return "**";
            return "*";
        }

        @Override
        public int compareTo(UrlRemapperBean o) {
            return newUrlPath.compareTo(o.getNewUrlPath());
        }
    }

    private HashMap<String, Remapper> makeMapBeans(List<UrlRemapperBean> vbeans) {
        HashMap<String, Remapper> map = new HashMap<String, Remapper>(3000);
        for (UrlRemapperBean vbean : vbeans) {

            // construct the old -> new mapping
            Remapper mbean = map.get(vbean.getOldUrlPath());
            if (mbean == null) {
                mbean = new Remapper(vbean.getOldUrlPath());
                map.put(vbean.getOldUrlPath(), mbean);
            }
            mbean.add(vbean);
        }

        for (Remapper rmap : map.values()) {
            rmap.finish();
        }

        return map;
    }

    private class Remapper {
        String oldUrl, newUrl; // newName exists when theres only one
        List<UrlRemapperBean> newUrls = new ArrayList<UrlRemapperBean>();
        HashMap<String, UrlRemapperBean> newUrlMap = new HashMap<String, UrlRemapperBean>();

        // no-arg constructor
        public Remapper() {
        }

        public Remapper(String oldUrl) {
            this.oldUrl = oldUrl;
        }

        void add(UrlRemapperBean vbean) {
            newUrlMap.put(vbean.getNewUrlPath(), vbean);
            newUrls.add(vbean);
        }

        void finish() {
            if (newUrlMap.values().size() == 1) {
                newUrl = newUrls.get(0).getNewUrlPath();
                // newVars = null; // GC
            }
        }

        public int getCount() {
            return newUrls.size();
        }

        public String getOldUrl() {
            return oldUrl;
        }
    }


    public static void main(String[] args) throws IOException {
        UnidataTdsUrlRemapper u = new UnidataTdsUrlRemapper();

        List<String> result = u.getMappedUrlPaths("fmrc/NCEP/GFS/Alaska_191km/files/latest.xml");
        System.out.println(result.toString());

        result = u.getMappedUrlPaths("fmrc/NCEP/GFS/Alaska_191km/files/latest.xml", "latest");
        System.out.println(result.toString());
    }
}
