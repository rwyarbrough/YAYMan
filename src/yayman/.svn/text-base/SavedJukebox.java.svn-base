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

import org.w3c.dom.*;

public class SavedJukebox {
    private String jukeboxName;
    private String libraryFile;
    private String propertiesFile;
    private boolean scheduleEnabled;
    //private int scheduleHour;
    //private int scheduleMinute;
    //private String nmt;
    private NetworkedMediaTank nmt;
    private boolean cleanJukebox;
    private boolean preserveJukebox;
    private boolean skipIndexGeneration;
    private boolean autoNMJConvert;

    public SavedJukebox() {
        jukeboxName = "Default";
        libraryFile = "./My_Library.xml";
        propertiesFile = "./moviejukebox.properties";
        scheduleEnabled = false;
        //scheduleHour = 0;
        //scheduleMinute = 0;
        nmt = new NetworkedMediaTank();
        cleanJukebox = false;
        preserveJukebox = false;
        skipIndexGeneration = false;
        autoNMJConvert = false;
    }

    public SavedJukebox(Element ele) {
        this();
        if (ele.hasAttribute("name")) {
            jukeboxName = ele.getAttribute("name");
        }

        if (ele.hasAttribute("nmt")) nmt.setName(ele.getAttribute("nmt"));

        Element propsEle = (Element)ele.getElementsByTagName("Properties").item(0);
        if (propsEle != null) {
            propertiesFile = propsEle.getTextContent();
        }

        Element libEle = (Element)ele.getElementsByTagName("Library").item(0);
        if (libEle != null) {
            libraryFile = libEle.getTextContent();
        }

        Element schedEle = (Element)ele.getElementsByTagName("Schedule").item(0);
        if (schedEle != null) {
            scheduleEnabled = Boolean.parseBoolean(schedEle.getAttribute("enabled"));
            //scheduleHour = Integer.parseInt(schedEle.getAttribute("hour"));
            //scheduleMinute = Integer.parseInt(schedEle.getAttribute("minute"));
        }

        if (ele.hasAttribute("cleanJukebox")) cleanJukebox = Boolean.parseBoolean(ele.getAttribute("cleanJukebox"));

        if (ele.hasAttribute("preserveJukebox")) preserveJukebox = Boolean.parseBoolean(ele.getAttribute("preserveJukebox"));

        if (ele.hasAttribute("skipIndexGeneration")) skipIndexGeneration = Boolean.parseBoolean(ele.getAttribute("skipIndexGeneration"));

        if (ele.hasAttribute("autoNMJ")) autoNMJConvert = Boolean.parseBoolean(ele.getAttribute("autoNMJ"));
    }

    public String getName() {
        return jukeboxName;
    }

    public String getPropertiesFile() {
        return propertiesFile;
    }

    public String getLibraryFile() {
        return libraryFile;
    }

    public boolean isScheduled() {
        return scheduleEnabled;
    }

    /*public int getScheduleHour() {
        return scheduleHour;
    }

    public int getScheduleMinute() {
        return scheduleMinute;
    }*/

    public NetworkedMediaTank getNmt() {
        return nmt;
    }

    public boolean getCleanJukebox() {
        return cleanJukebox;
    }

    public boolean getPreserveJukebox() {
        return preserveJukebox;
    }

    public boolean getSkipIndexGeneration() {
        return skipIndexGeneration;
    }

    public boolean doesNMJAutoConvert() {
        return autoNMJConvert;
    }

    public void setName(String n) {
        if (n != null && !n.equals("")) jukeboxName = n;
    }

    public void setPropertiesFile(String p) {
        if (p != null && !p.equals("")) propertiesFile = p;
    }

    public void setLibraryFile(String l) {
        if (l != null && !l.equals("")) libraryFile = l;
    }

    public void setScheduled(boolean b) {
        scheduleEnabled = b;
    }

    public void setScheduleHour(Object o) {
        setScheduleHour(Integer.parseInt(o.toString()));
    }

    /*public void setScheduleHour(int h) {
        if (h > -1 && h < 24) scheduleHour = h;
    }

    public void setScheduleMinute(Object o) {
        setScheduleMinute(Integer.parseInt(o.toString()));
    }

    public void setScheduleMinute(int m) {
        if (m > -1 && m < 60) scheduleMinute = m;
    }*/

    public void setNmt(NetworkedMediaTank n) {
        nmt = n;
    }

    public void setCleanJukebox(boolean b) {
        cleanJukebox = b;
    }

    public void setPreserveJukebox(boolean b) {
        preserveJukebox = b;
    }

    public void setSkipIndexGeneration(boolean b) {
        skipIndexGeneration = b;
    }

    public void setNMJAutoConvert(boolean b) {
        autoNMJConvert = b;
    }

    @Override
    public String toString() {
        return jukeboxName;
    }
}
