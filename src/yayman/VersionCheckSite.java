/*
 *      Copyright (c) 2009-2010 nord
 *
 *      Web: http://mediaplayersite.com/YAYMan
 *
 *      This software is licensed under a Creative Commons License
 *      See this page: http://creativecommons.org/licenses/by-nc/3.0/
 *
 *      For any reuse or distribution, you must make clear to others the
 *      license terms of this work.
 */

package yayman;

import java.net.*;
import java.io.*;
import java.util.regex.*;
import javax.swing.SwingWorker;

public class VersionCheckSite implements Comparable<VersionCheckSite> {
    private String siteURL;
    private String element;
    private String downloadURL;
    private String version;
    private boolean connectionMade;
    private boolean currentVersion;
    private String fileURL;
    private String baseFileURL;
    private boolean finishedCheck;

    public VersionCheckSite() {
        siteURL = null;
        element = null;
        downloadURL = null;
        version = "0";
        connectionMade = false;
        baseFileURL = null;
        fileURL = null;
        finishedCheck = false;
    }

    public VersionCheckSite(String url, String ele) {
        this();
        siteURL = url;
        element = ele;
        downloadURL = url;
    }

    public String getSiteURL() {
        return siteURL;
    }

    public String getElement() {
        return element;
    }

    public String getDownloadURL() {
        return downloadURL;
    }

    public String getFileURL() {
        return fileURL;
    }

    public void setSiteURL(String url) {
        siteURL = url;
    }

    public void setElement(String ele) {
        element = ele;
    }

    public void setBaseFileURL(String url) {
        baseFileURL = url;
    }

    public void setDownloadURL(String durl) {
        downloadURL = durl;
    }

    public boolean getConnectSuccess() {
        return connectionMade;
    }

    public boolean isCurrentVersion() {
        return currentVersion;
    }
    
    public String getVersion() {
        return version;
    }

    public int compareTo(VersionCheckSite otherSite) {
        return compareTo(otherSite.getVersion());
    }
    
    public boolean isCheckFinished() {
        return finishedCheck;
    }

    public int compareTo(String str) {
        String oVer = str;
        String[] v = version.split("\\.");
        String[] ov = oVer.split("\\.");
        int limit = v.length;
        if (ov.length < v.length) limit = ov.length;
        for (int i = 0; i < limit; i++) {
            int iov = Integer.parseInt(ov[i]);
            int iv = Integer.parseInt(v[i]);
            if (iv > iov) {
                return 1;
            } else if (iv < iov) {
                return -1;
            } else if (i+1 == limit && ov.length < v.length) return 1;
        }
        return 0;
    }
    
    public void checkSiteWorker() {
        new SwingWorker<Void,String>() {
            public Void doInBackground() {
                checkSite();
                return null;
            }
        }.execute();
    }

    public void checkSite() {
        try {
            URL url = new URL(this.getSiteURL());
            String html = "";
            BufferedReader read = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = read.readLine();
            while (line != null) {
                html += line;
                line = read.readLine();
            }

            html = html.replaceAll("\\s+", " ");
            Pattern p = Pattern.compile("<"+this.getElement()+">(.*?)</"+this.getElement()+">");
            Matcher m = p.matcher(html);
            if (m.find() == true) {
                version = m.group(1);
            }
            String[] verParts = version.split(" ");
            for (int i = 0; i < verParts.length; i++) {
                if (verParts[i].matches("v\\d+(\\.\\d+)*+")) {
                    version = verParts[i].replaceAll("v", "");
                    break;
                } else if (verParts[i].matches("\\d+(\\.\\d+)*+")) {
                    version = verParts[i];
                    break;
                } else {
                    version = "0";
                }
            }
            connectionMade = true;
            
            if (this.getDownloadURL() != null) {
                url = new URL(this.getDownloadURL());
                html = "";
                read = new BufferedReader(new InputStreamReader(url.openStream()));
                line = read.readLine();
                while (line != null) {
                    html += line;
                    line = read.readLine();
                }

                html = html.replaceAll("\\s+", " ");
            } 

            p = Pattern.compile(baseFileURL+"(.)+?\\.zip");
            m = p.matcher(html);
            while (m.find()) {
                fileURL = m.group(0);
                if (!fileURL.startsWith("http://")) fileURL = "http://"+fileURL;
                break;
            }
        } catch (Throwable th) {
            YAYManView.logger.severe(th+" could not check for new version");
        }
        finishedCheck = true;
    }
}
