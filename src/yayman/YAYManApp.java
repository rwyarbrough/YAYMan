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

import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;
import java.io.*;
import java.util.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.*;
import javax.swing.SwingWorker;
import java.util.zip.*;

/**
 * The main class of the application.
 */
public class YAYManApp extends SingleFrameApplication {

    //private static Logger logger = Logger.getLogger("yayman");

    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        show(new YAYManView(this));
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of YAYManApp
     */
    public static YAYManApp getApplication() {
        return Application.getInstance(YAYManApp.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        if (System.getProperty("mrj.version") != null) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "YAYMan");
        }

        //cleanupFiles();
        //loadNewClasses();
        try {
            //if this croaks, then we probably need to download YAMJ
            Class mjbClass = Class.forName("com.moviejukebox.MovieJukebox");

            //mediaInfoCheck();
            //boolean launchProgram = true;

            if (yamjRevisionCheck()) launch(YAYManApp.class, args);
        } catch (Exception ex) {
            if (doRenameDeleteCheck()) {
                restartApplication(new WizardError());
            } else if (javax.swing.JOptionPane.showConfirmDialog(null, "Do you want to download YAMJ?", "YAMJ Not Detected", javax.swing.JOptionPane.YES_NO_OPTION,javax.swing.JOptionPane.WARNING_MESSAGE) == javax.swing.JOptionPane.YES_OPTION) {
                //cleanOldJars();
                //YAMJUpdateDiag yamjUpdate = new YAMJUpdateDiag();
                //yamjUpdate.setVisible(true);
                YAYManUpdateDiag yaymanUpdate = new YAYManUpdateDiag();
            }
        }
    }

    //needed for Mac Quit menu item
    public boolean quit() {
        this.quit(null);
        return true;
    }

    public static boolean  restartApplication( Object classInJarFile ) {
        String javaBin = System.getProperty("java.home") + "/bin/java";
        File jarFile;
        try{
            jarFile = new File(classInJarFile.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch(Exception e) {
            return false;
        }

        /* is it a jar file? */
        if ( !jarFile.getName().endsWith(".jar") )
        return false;   //no, it's a .class probably

        String  toExec[] = new String[] { javaBin, "-jar", jarFile.getPath(), "-Xms256m", "-Xmx1024m" };
        if (System.getProperty("mrj.version") != null) {
            toExec = new String[] { javaBin, "-jar", jarFile.getPath(), "-Xms256m", "-Xmx1024m", "-d32" };
        }
        try{
            Process p = Runtime.getRuntime().exec( toExec );
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }

        System.exit(0);

        return true;
    }
    
    public static ArrayList<String> getSaveFiles() {
        ArrayList<String> saveFiles = new ArrayList();

        //YAYMan  libs
        saveFiles.add("appframework-1.0.3.jar");
        saveFiles.add("cling-core-1.0.jar");
        saveFiles.add("swing-worker-1.1.jar");
        saveFiles.add("teleal-common-1.0.11.jar");

        //NMJ libs
        saveFiles.add("com.syabas.tonmjdb-1.8.0-20101208.jar");
        saveFiles.add("metadata-extractor-2.4.0-beta-1.jar");
        
        for (String jar : getJars()) {
            saveFiles.add(jar+".jar");
        }
        
        return saveFiles;
    }
    
    private static void cleanOldJars() {
        ArrayList<String> saveFiles = getSaveFiles();
        File libDir = new File("lib");
        if (libDir.exists() && libDir.isDirectory()) {
            File[] jars = libDir.listFiles();
            for (int i=0; i < jars.length; i++) {
                File jar = jars[i];
                if (!saveFiles.contains(jar.getName())) jar.delete();
            }
        }
    }
    
    private static void loadNewClasses() {
        File updateFile = new File("yamjnewlibs.txt");
        if (updateFile.exists()) {
            URL urls [] = {};
            JarFileLoader cl = new JarFileLoader (urls);
            try {
                BufferedReader br = new BufferedReader(new FileReader("yamjnewlibs.txt"));
                String entry = br.readLine();
                
                while (entry != null) {
                    if (!entry.isEmpty()) cl.addFile("lib/"+entry);
                    entry = br.readLine();
                }
                br.close();
            } catch (Exception ex) {
                System.out.println("Error reading update file: "+ex);
            }
        }
    }

    public static void mediaInfoCheck() {
        new SwingWorker<Void,String>() {
            public Void doInBackground() {
            try {
            File miDir = new File("mediaInfo");
            //com.moviejukebox.scanner.MediaInfoScanner scanner = new com.moviejukebox.scanner.MediaInfoScanner();

            //if (!miDir.exists() || !miDir.isDirectory() || miDir.listFiles().length == 1) {
            if (!JukeboxInterface.isMediaInfoScannerActivated()) {
                System.out.println("MediaInfo not detected...");
                String os = System.getProperty("os.name").toLowerCase();
                String downloadPage = null;
                String matchPattern = null;
                if (os.indexOf("win") > -1) {
                    downloadPage = "http://mediainfo.sourceforge.net/en/Download/Windows";
                    matchPattern = "http://sourceforge.net/projects/mediainfo/files/binary/mediainfo/"+"(.+?)"+"CLI"+"(.+?)"+"/download";
                } else if (os.indexOf("mac") > -1) {
                    //downloadPage = "http://code.google.com/p/yayman/downloads/list";
                    //matchPattern = "http://yayman.googlecode.com/files/"+"(.*?)"+"MediaInfo"+"(.*?)"+"Mac"+"(.*?)"+"CLI"+"(.*?)"+".zip";
                    if (javax.swing.JOptionPane.showConfirmDialog(null, "You must download MediaInfo manually", "Download MediaInfo?", javax.swing.JOptionPane.OK_OPTION,javax.swing.JOptionPane.WARNING_MESSAGE) == javax.swing.JOptionPane.YES_OPTION) {
                        java.awt.Desktop.getDesktop().browse(new URI("http://mediainfo.sourceforge.net/en/Download/Mac_OS"));
                        java.awt.Desktop.getDesktop().browse(new URI("http://www.networkedmediatank.com/wiki/index.php/YAMJ_for_Mac#Installation"));
                        Process proc = Runtime.getRuntime().exec("which mediainfo");
                        OutputStream out = proc.getOutputStream();
                        InputStream in = proc.getInputStream();
                        Writer writer = new StringWriter();

                        char[] buffer = new char[1024];
                        try {
                            Reader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                            int n;
                            while ((n = reader.read(buffer)) != -1) {
                                writer.write(buffer, 0, n);
                            }
                        } finally {
                            in.close();
                        }
                        System.out.println(writer.toString());
                    }
                    return null;
                //} else if (os.indexOf("linux") > -1) {
                } else {
                    javax.swing.JOptionPane.showConfirmDialog(null, "You must download MediaInfo manually", "Download MediaInfo", javax.swing.JOptionPane.OK_OPTION,javax.swing.JOptionPane.WARNING_MESSAGE);
                    java.awt.Desktop.getDesktop().browse(new URI("http://mediainfo.sourceforge.net/en/Download"));
                    return null;
                }

                URL url = new URL(downloadPage);
                String html = "";
                BufferedReader read = new BufferedReader(new InputStreamReader(url.openStream()));
                String line = read.readLine();
                while (line != null) {
                    html += line;
                    line = read.readLine();
                }

                String downloadUrl = "";
                //Pattern p = Pattern.compile("CLI"+"(.+?)"+"http://"+"(.+?)"+"/download");
                Pattern p = Pattern.compile(matchPattern);
                Matcher m = p.matcher(html);
                while (m.find()) {
                    downloadUrl = m.group();
                    /*for (int i = 0; i < m.groupCount(); i++) {
                        System.out.println(i+": "+m.group(i));
                    }*/
                    break;
                }

                url = new URL(downloadUrl);

                URLConnection conn = url.openConnection();
                int size = conn.getContentLength();
                BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
                FileOutputStream fos = new FileOutputStream("mediainfo.zip");
                BufferedOutputStream bout = new BufferedOutputStream(fos,1024);
                byte data[] = new byte[1024];
                int written = 0;
                int x = 0;
                while((x = in.read(data,0,1024)) >= 0) {
                    written += x;
                    //bout.write(data);
                    bout.write(data,0,x);
                    if (written > size) written = size;
                }
                bout.flush();
                bout.close();
                in.close();

                File miFile = new File("mediainfo.zip");
                if (!miDir.exists()) miDir.mkdir();
                if (miFile.exists()) {
                    int BUFFER = 2048;
                    BufferedOutputStream dest = null;
                    BufferedInputStream is = null;
                    ZipEntry entry;
                    ZipFile zipfile = new ZipFile(miFile);
                    Enumeration e = zipfile.entries();
                    while(e.hasMoreElements()) {
                        entry = (ZipEntry) e.nextElement();
                        if (entry.isDirectory()) {
                            File dir = new File("mediaInfo/"+entry.getName());
                            if (!dir.exists()) {
                                dir.mkdir();
                            }
                            continue;
                        }
                        is = new BufferedInputStream(zipfile.getInputStream(entry));
                        int count;
                        data = new byte[BUFFER];
                        fos = new FileOutputStream("mediaInfo/"+entry.getName());
                        dest = new BufferedOutputStream(fos, BUFFER);
                        while ((count = is.read(data, 0, BUFFER))
                          != -1) {
                           dest.write(data, 0, count);
                        }
                        dest.flush();
                        dest.close();
                        is.close();
                     }
                     zipfile.close();
                     miFile.delete();
                }

                /*if (os.indexOf("mac") > -1) {
                    Runtime.getRuntime().exec("chmod u+x mediaInfo/mediainfo");
                }*/
            }
            }catch (Exception ex) {
                System.out.println("Error getting MediaInfo: "+ex);
            }
            return null;
            }
        }.execute();
    }

    public static boolean yamjRevisionCheck() {
        try {
            org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(yayman.YAYManApp.class).getContext().getResourceMap(YAYManAboutBox.class);
            int requiredRevision = resourceMap.getInteger("Application.minRevision").intValue();
            int localRevision = Integer.parseInt(com.moviejukebox.MovieJukebox.class.getPackage().getImplementationVersion());
            if (localRevision < requiredRevision) {
                YAYManView.logger.fine("Local YAMJ revision not compatible. "+localRevision+" < "+requiredRevision);
                if (javax.swing.JOptionPane.showConfirmDialog(null, "This version of YAYMan requires YAMJ r"+requiredRevision+" or newer to function properly. \r\nYou have r"+localRevision+". Would you like to download and install the latest YAMJ?", "YAMJ Revision Incompatible", javax.swing.JOptionPane.YES_NO_OPTION,javax.swing.JOptionPane.WARNING_MESSAGE) == javax.swing.JOptionPane.YES_OPTION) {
                    YAMJUpdateDiag yamjUpdate = new YAMJUpdateDiag();
                    yamjUpdate.setVisible(true);
                    return false;
                }
            }
        } catch (Exception ex) {
            YAYManView.logger.severe(ex+"could not check revision");
        }
        return true;
    }
    
    private static boolean doRenameDeleteCheck() {
        boolean renamed = false;
        
        ArrayList<String> finalNames = getJars();
        ArrayList<String> yaymanJars = getYaymanJars();
        File libDir = new File("lib");
        if (libDir.exists() && libDir.isDirectory()) {
            File[] jars = libDir.listFiles();
            if (jars.length <= 4) return false;
            
            for (int i=0; i < jars.length; i++) {
                boolean deleteFile = false;
                File file = jars[i];
                if (yaymanJars.contains(file.getName())) continue;
                
                for (int j=0; j<finalNames.size(); j++) {
                    String trunk = finalNames.get(j);
                    if (file.getName().compareTo(trunk+".jar") == 0) {
                        break;
                    } else if (file.getName().startsWith(trunk)) {
                        File newFile = new File(libDir, trunk+".jar");
                        if (newFile.exists()) {
                            deleteFile = true;
                            break;
                        }
                        renamed = true;
                        file.renameTo(newFile);
                        break;
                    } else if (j == finalNames.size()-1) {
                        //deleteFile = true;
                    }
                }
                if (deleteFile) file.delete();
            }
        }
        
        return renamed;
    }
    
    public static ArrayList<String> getYaymanJars() {
        ArrayList<String> jarNames = new ArrayList();
        //YAYMan  libs
        jarNames.add("appframework-1.0.3.jar");
        jarNames.add("cling-core-1.0.jar");
        jarNames.add("swing-worker-1.1.jar");
        jarNames.add("teleal-common-1.0.11.jar");

        //NMJ libs
        jarNames.add("com.syabas.tonmjdb-1.8.0-20101208.jar");
        jarNames.add("metadata-extractor-2.4.0-beta-1.jar");
        return jarNames;
    }
    
    public static ArrayList<String> getJars() {
        ArrayList<String> jarNames = new ArrayList();
        jarNames.add("allocine-api");
        jarNames.add("anidb");
        jarNames.add("antlr");
        //jarNames.add("api-imdb");
        //jarNames.add("commons-beanutils-core");
        //jarNames.add("commons-beanutils");
        jarNames.add("commons-codec");
        //commons-collections is old
        //jarNames.add("commons-collections");
        jarNames.add("commons-configuration");
        //jarNames.add("commons-digester");
        jarNames.add("commons-io");
        jarNames.add("commons-lang3");
        jarNames.add("commons-lang");
        jarNames.add("commons-logging");
        jarNames.add("dom4j");
        jarNames.add("fanarttvapi");
        jarNames.add("filters");
        jarNames.add("hibernate-commons-annotations");
        //jarNames.add("hibernate-core-lgpl");
        jarNames.add("hibernate-core");
        jarNames.add("hibernate-jpa");
        jarNames.add("jackson-annotations");
        jarNames.add("jackson-core");
        jarNames.add("jackson-databind");
        jarNames.add("javassist");
        jarNames.add("javolution");
        //jaxb-api is new
        jarNames.add("jaxb-api");
        jarNames.add("jaxb-impl");
        jarNames.add("jboss-logging");
        jarNames.add("jboss-transaction-api");
        //jarNames.add("jpa-api");
        //jarNames.add("json");
        jarNames.add("log4j");
        jarNames.add("mjbsqldb");
        jarNames.add("mucommander");
        jarNames.add("ormlite-core");
        jarNames.add("ormlite-jdbc");
        jarNames.add("pojava");
        jarNames.add("rottentomatoesapi");
        jarNames.add("sanselan");
        jarNames.add("saxonhe");
        jarNames.add("simmetrics");
        jarNames.add("slf4j-api");
        jarNames.add("slf4j-log4j12");
        jarNames.add("sqlite-jdbc");
        jarNames.add("subbabaapi");
        jarNames.add("themoviedbapi");
        jarNames.add("thetvdbapi");
        jarNames.add("traileraddictapi");
        jarNames.add("tvrageapi");
        jarNames.add("ws-commons-util");
        jarNames.add("xml-apis");
        jarNames.add("xmlrpc-client");
        jarNames.add("xmlrpc-common");
        jarNames.add("yamj");
        return jarNames;
    }
}
