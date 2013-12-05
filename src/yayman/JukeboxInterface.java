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

import static com.moviejukebox.tools.PropertiesUtil.*;

import com.moviejukebox.model.*;
import com.moviejukebox.writer.*;
import com.moviejukebox.reader.*;
import com.moviejukebox.plugin.*;
import com.moviejukebox.plugin.trailer.*;
import com.moviejukebox.scanner.*;
import static com.moviejukebox.tools.StringTools.*;
import com.moviejukebox.plugin.ImdbPlugin.*;
import com.moviejukebox.MovieJukebox;
//import com.moviejukebox.tools.LogFormatter;
import com.moviejukebox.tools.ThreadExecutor;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.scanner.artwork.*;

import java.io.*;
import javax.xml.stream.XMLStreamException;
import java.lang.reflect.*;
import java.util.*;
import java.util.StringTokenizer;
import static java.lang.Boolean.parseBoolean;

import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.text.SimpleDateFormat;

import org.w3c.dom.*;

import javax.xml.parsers.*;

import javax.swing.DefaultListModel;

//import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.FileAppender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LoggingEvent;

import javax.swing.SwingWorker;
import java.util.zip.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.*;

public class JukeboxInterface {

    static String logFilename = "moviejukebox.log";
    //static LogFormatter mjbFormatter = new LogFormatter();
    //static FileHandler fh;
    static ConsoleHandler ch;
    static FileAppender fa;
    static ConsoleAppender ca;
    static org.apache.log4j.Logger mjbLogger;
    static java.util.logging.Logger logger;
    static String lastMessage = "N/A";
    static boolean yamjRunning = false;
    static ArrayList<File> libXmlFiles;
    static {
        try {
            mjbLogger = org.apache.log4j.Logger.getLogger(MovieJukebox.class);
            //mjbLogger.setLevel(Level.ALL);
            logger = java.util.logging.Logger.getLogger("yayman");
        } catch (Exception ex) {
            System.out.println(ex+": Error initializing loggers");
        }
        PropertyConfigurator.configure("properties/log4j.properties");
    }

    public static MovieJukebox getMovieJukebox() {//throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        /*Class[] argTypes = {String.class, String.class};
        Constructor mjbConst = MovieJukebox.class.getDeclaredConstructor(argTypes);
        mjbConst.setAccessible(true);
        Object[] args = {getProperty("yayman.libraryFile"), FileTools.getCanonicalPath(getProperty("mjb.jukeboxRoot"))};
        return (MovieJukebox)mjbConst.newInstance(args);*/
        try {
            String root = getProperty("mjb.jukeboxRoot");
            String libPath = getProperty("yayman.libraryFile");
            String outPath = null;
            if (root != null && !root.isEmpty()) outPath = FileTools.getCanonicalPath(root);
            if (outPath == null) {
                List<NMTLibrary> libs = getLibraries(new File(libPath));
                if (libs.size() > 0) {
                    outPath = libs.get(0).getPath();
                }
            }
            return new MovieJukebox(libPath, outPath);
        } catch (Exception ex) {
            //logger.severe("Error getting MovieJukebox: "+ex);
            logger.severe("Error getting MovieJukebox: "+ex);
            return null;
        }
    }

    public static Jukebox getJukebox(String jukeboxRoot, String jukeboxTempLocation, String detailsDirName) {
        return new Jukebox(FileTools.getCanonicalPath(jukeboxRoot), FileTools.getCanonicalPath(jukeboxTempLocation), detailsDirName);
    }

    public static Jukebox getJukebox() {
        return getJukebox(getJukeboxRoot());
    }

    public static Jukebox getJukebox(String jukeboxRoot) {
        return getJukebox(jukeboxRoot, jukeboxRoot, getProperty("mjb.detailsDirName"));
    }

    public static Jukebox getJukebox(String jukeboxRoot, String jukeboxTempLocation) {
        return getJukebox(jukeboxRoot, jukeboxTempLocation, getProperty("mjb.detailsDirName"));
    }

    public static class ToolSet {
        public MovieImagePlugin imagePlugin = com.moviejukebox.MovieJukebox.getImagePlugin(getProperty("mjb.image.plugin", "com.moviejukebox.plugin.DefaultImagePlugin"));
        public MovieImagePlugin backgroundPlugin = com.moviejukebox.MovieJukebox.getBackgroundPlugin(getProperty("mjb.background.plugin", "com.moviejukebox.plugin.DefaultBackgroundPlugin"));
        public MediaInfoScanner miScanner = new MediaInfoScanner();
        public AppleTrailersPlugin trailerPlugin = new AppleTrailersPlugin();
        public OpenSubtitlesPlugin subtitlePlugin = new OpenSubtitlesPlugin();
    }

    public static ThreadLocal<ToolSet> getToolThread() {
        return new ThreadLocal<ToolSet>() {
            @Override
            protected ToolSet initialValue() {return new ToolSet(); };
        };
    }

    /*public static ToolSet getToolSet() {
        return new ThreadLocal<ToolSet>() {
            @Override
            protected ToolSet initialValue() {return new ToolSet(); };
        }.get();
    }*/

    /*public static void updateMovieData(MovieJukeboxXMLWriter xmlWriter, MediaInfoScanner miScanner, MovieImagePlugin backgroundPlugin, String jukeboxDetailsRoot,
        String tempJukeboxDetailsRoot, Movie movie) throws FileNotFoundException, XMLStreamException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {*/
    public static void updateMovieData(Movie movie) throws FileNotFoundException, XMLStreamException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        ToolSet tools = getToolThread().get();
        //MovieJukeboxXMLWriter xmlWriter = new MovieJukeboxXMLWriter();
        MovieJukeboxXMLReader xmlReader = new MovieJukeboxXMLReader();
        //getMovieJukebox().updateMovieData(xmlWriter, tools.miScanner, tools.backgroundPlugin, getJukebox(), movie);
        getMovieJukebox().updateMovieData(xmlReader, tools.miScanner, tools.backgroundPlugin, getJukebox(), movie, new Library());
    }

    //public static void updateMoviePoster(String jukeboxDetailsRoot, String tempJukeboxDetailsRoot, Movie movie) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
    public static void updateMoviePoster(Movie movie) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        getMovieJukebox().updateMoviePoster(getJukebox(), movie);
    }

    //public static void updateTvBanner(String jukeboxDetailsRoot, String tempJukeboxDetailsRoot, Movie movie) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
    public static void updateTvBanner(Movie movie) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        ToolSet tools = getToolThread().get();
        getMovieJukebox().updateTvBanner(getJukebox(), movie, tools.imagePlugin);
    }

    public static void createPoster(Movie movie) {
        ToolSet tools = getToolThread().get();
        String skinHome = getProperty("mjb.skin.dir", "./skins/default");
        MovieJukebox.createPoster(tools.imagePlugin, JukeboxInterface.getJukebox(), skinHome, movie, true);
    }

    public static void createThumbnail(Movie movie) {
        ToolSet tools = getToolThread().get();
        String skinHome = getProperty("mjb.skin.dir", "./skins/default");
        MovieJukebox.createThumbnail(tools.imagePlugin, JukeboxInterface.getJukebox(), skinHome, movie, true);
    }

    public static void createSetThumbnail(Movie movie, String setName) {
        ToolSet tools = getToolThread().get();
        String skinHome = getProperty("mjb.skin.dir", "./skins/default");
        String oldPoster = movie.getPosterFilename();
        String oldThumb = movie.getThumbnailFilename();
        //String oldURL = movie.getPosterURL();

        movie.setPosterFilename("Set_"+setName+"_1"+".jpg");
        movie.setThumbnailFilename("Set_"+setName+"_1"+getProperty("mjb.scanner.thumbnailToken","_small")+".png");
        //movie.setPosterURL(url);

        MovieJukebox.createThumbnail(tools.imagePlugin, JukeboxInterface.getJukebox(), skinHome, movie, true);
        File setFullPosterFile = new File(JukeboxInterface.getFullDetailsPath()+"/"+movie.getPosterFilename());
        if (setFullPosterFile.exists()) setFullPosterFile.delete();

        movie.setPosterFilename(oldPoster);
        movie.setThumbnailFilename(oldThumb);
        //movie.setPosterURL(oldURL);
    }

    public static void downloadFanart(Movie movie) throws NoSuchFieldException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        ToolSet tools = getToolThread().get();
        Field fanartOverwrite = FanartScanner.class.getDeclaredField("fanartOverwrite");
        fanartOverwrite.setAccessible(true);
        boolean overwriteDefault = fanartOverwrite.getBoolean(null);
        fanartOverwrite.set(null, true);


        // the first argument is the name of the method, the second is the argument types
        Class[] argTypes = {MovieImagePlugin.class, Jukebox.class, Movie.class};
        Method downloadFanartMethod = FanartScanner.class.getDeclaredMethod("downloadFanart", argTypes);
        downloadFanartMethod.setAccessible(true);
        Object[] args = {tools.backgroundPlugin, JukeboxInterface.getJukebox(), movie};
        // the first argument would be an instance of the object for a non-null method
        movie.setDirty(DirtyFlag.FANART, true);
        downloadFanartMethod.invoke(null, args);

        fanartOverwrite.set(null, overwriteDefault);
    }

    public static Collection<MediaLibraryPath> parseMovieLibraryRootFile(File f) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        //Class[] argTypes = {File.class};
        //Method parseMethod = MovieJukebox.class.getDeclaredMethod("parseMovieLibraryRootFile", argTypes);
        //parseMethod.setAccessible(true);
        //Object[] args = {f};
        //return (Collection<MediaLibraryPath>)parseMethod.invoke(getMovieJukebox(), args);
        return MovieJukeboxLibraryReader.parse(f);
    }

    public static void generateLibrary() throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchFieldException, IOException {
        if (yamjRunning) {
            logger.warning("YAMJ is already processing a jukebox. New request ignored.");
            return;
        }
        yamjRunning = true;
        long timeStart = System.currentTimeMillis();

        resetHandlers();
        //ch.setWorker(worker);

        // These are pulled from the Manifest.MF file that is created by the Ant build script
        String mjbVersion = MovieJukebox.class.getPackage().getSpecificationVersion();
        String mjbRevision = MovieJukebox.class.getPackage().getImplementationVersion();
        String mjbBuildDate = MovieJukebox.class.getPackage().getImplementationTitle();
        // Just create a pretty underline.
        String mjbTitle = "";
        if (mjbVersion == null)
            mjbVersion = "";
        for (int i = 1; i <= mjbVersion.length(); i++) {
            mjbTitle += "~";
        }

        mjbLogger.info("Yet Another Movie Jukebox " + mjbVersion);
        mjbLogger.info("~~~ ~~~~~~~ ~~~~~ ~~~~~~~ " + mjbTitle);
        mjbLogger.info("http://code.google.com/p/moviejukebox/");
        mjbLogger.info("Copyright (c) 2004-2010 YAMJ Members");
        mjbLogger.info("");
        mjbLogger.info("This software is licensed under a Creative Commons License");
        mjbLogger.info("See this page: http://code.google.com/p/moviejukebox/wiki/License");
        mjbLogger.info("");

        // Print the revision information if it was populated by Hudson CI
        if (!((mjbRevision == null) || (mjbRevision.equalsIgnoreCase("${env.SVN_REVISION}")))) {
            mjbLogger.info("  Revision: r" + mjbRevision);
            mjbLogger.info("Build Date: " + mjbBuildDate);
            mjbLogger.info("");
        }
        mjbLogger.info("Processing started at " + new Date());
        mjbLogger.info("");

        StringBuilder sb = new StringBuilder("{");
        for (Map.Entry<Object, Object> propEntry : com.moviejukebox.tools.PropertiesUtil.getEntrySet()) {
            sb.append(propEntry.getKey() + "=" + propEntry.getValue() + ",");
        }
        sb.replace(sb.length() - 1, sb.length(), "}");
        mjbLogger.info("Properties: " + sb.toString());


        MovieFilenameScanner.setSkipKeywords(tokenizeToArray(getProperty("filename.scanner.skip.keywords", ""), ",;| "),
                Boolean.parseBoolean(getProperty("filename.scanner.skip.caseSensitive", "true")));
        MovieFilenameScanner.setExtrasKeywords(tokenizeToArray(getProperty("filename.extras.keywords", "trailer,extra,bonus"), ",;| "));
        MovieFilenameScanner.setMovieVersionKeywords(tokenizeToArray(getProperty("filename.movie.versions.keywords",
                        "remastered,directors cut,extended cut,final cut"), ",;|"));
        MovieFilenameScanner.setLanguageDetection(parseBoolean(getProperty("filename.scanner.language.detection", "true")));
        final KeywordMap languages = getKeywordMap("filename.scanner.language.keywords", null);
        if (languages.size() > 0) {
            MovieFilenameScanner.clearLanguages();
            for (String lang : languages.getKeywords()) {
                String values = languages.get(lang);
                if (values != null) {
                    MovieFilenameScanner.addLanguage(lang, values, values);
                } else {
                    mjbLogger.info("MovieFilenameScanner: No values found for language code " + lang);
                }
            }
        }

        final KeywordMap sourceKeywords = getKeywordMap("filename.scanner.source.keywords",
                            "HDTV,PDTV,DVDRip,DVDSCR,DSRip,CAM,R5,LINE,HD2DVD,DVD,DVD5,DVD9,HRHDTV,MVCD,VCD,TS,VHSRip,BluRay,HDDVD,D-THEATER,SDTV");
        MovieFilenameScanner.setSourceKeywords(sourceKeywords.getKeywords(), sourceKeywords);


        String temp = getProperty("sorting.strip.prefixes");
        if (temp != null) {
            StringTokenizer st = new StringTokenizer(temp, ",");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                if (token.startsWith("\"") && token.endsWith("\"")) {
                    token = token.substring(1, token.length() - 1);
                }
                Movie.addSortIgnorePrefixes(token.toLowerCase());
            }
        }

        MovieJukebox ml = getMovieJukebox();

        MovieJukebox.setJukeboxPreserve(Boolean.parseBoolean(getProperty("yayman.preserveJukebox","false")));

        try {
            Field jukeboxCleanField = MovieJukebox.class.getDeclaredField("jukeboxClean");
            jukeboxCleanField.setAccessible(true);
            jukeboxCleanField.set(null, Boolean.parseBoolean(getProperty("mjb.jukeboxClean","false")));

            Field jukeboxSkipIndexField = MovieJukebox.class.getDeclaredField("skipIndexGeneration");
            jukeboxSkipIndexField.setAccessible(true);
            jukeboxSkipIndexField.set(null, Boolean.parseBoolean(getProperty("mjb.skipIndexGeneration","false")));

            Field jukeboxSkipHtmlField = MovieJukebox.class.getDeclaredField("skipHtmlGeneration");
            jukeboxSkipHtmlField.setAccessible(true);
            jukeboxSkipHtmlField.set(null, Boolean.parseBoolean(getProperty("mjb.skipHtmlGeneration","false")));
        } catch (Exception ex) {
            logger.severe("Error setting jukebox parameter: "+ex);
        }

        //setJukebox();

        Class[] argTypes = null;
        Object[] args = null;
        /*if (Integer.parseInt(MovieJukebox.class.getPackage().getImplementationVersion()) < 1497) {
            argTypes = new Class[]{boolean.class, boolean.class};
            args = new Object[]{false, false};
        } else if (Integer.parseInt(MovieJukebox.class.getPackage().getImplementationVersion()) < 1664) {
            argTypes = new Class[]{boolean.class};
            args = new Object[]{false};
        }*/
        Method generateLibraryMethod = MovieJukebox.class.getDeclaredMethod("generateLibrary", argTypes);
        generateLibraryMethod.setAccessible(true);
        //System.out.println("JB: "+ml.getJukebox().getJukeboxRootLocationDetails());
        generateLibraryMethod.invoke(ml, args);

        //fh.close();
        if (Boolean.parseBoolean(getProperty("mjb.appendDateToLogFile", "false"))) {
            // File (or directory) with old name
            File file = new File(logFilename);

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-kkmmss");
            logFilename = "moviejukebox_" + dateFormat.format(timeStart) + ".log";

            // File with new name
            File file2 = new File(logFilename);

            // Rename file (or directory)
            if (!file.renameTo(file2)) {
                mjbLogger.info("Error renaming log file.");
            }
        }
        resetHandlers();

        if (Boolean.parseBoolean(getProperty("yayman.autoNMJ", "false"))) {
            try {
                Class tonmjClass = Class.forName("com.syabas.tonmjdb.ToNmjDb");
                Class metaClass = Class.forName("com.drew.metadata.Metadata");
            } catch (Exception ex) {
                logger.severe("The ConvertToNMJ libraries are not installed properly!");
                logger.severe("Error: "+ex);
            }
            List<NMTLibrary> libraries = JukeboxInterface.getLibraries(getProperty("yayman.libraryFile"));
            Map<String, String> propMap = new HashMap();
            propMap.put("yamj.jukeboxPath", getFullDetailsPath());
            propMap.put("tonmjdb.source", "yamj");

            //Log log = LogFactory.getLog(VideoToNmjDb.class);

            try {
                for (int i = 0; i < libraries.size(); i++) {
                    NMTLibrary nmtLib = libraries.get(i);
                    propMap.put("yamj.nmjPath", nmtLib.getPlayerPath());
                    propMap.put("tonmjdb.nmjDbPath", nmtLib.getPath());

                    new com.syabas.tonmjdb.VideoToNmjDb(propMap) {
                            @Override
                            protected String getDbExistAction() throws Throwable {
                                return com.syabas.tonmjdb.ToNmjDb.DB_EXIST_MERGE;
                            }
                    };
                }
            } catch (Throwable ex) {
                YAYManView.logger.severe("Error converting to NMJ: "+ex);
            }
        }
        yamjRunning = false;
    }

    /*private static void renameLogFile(String logFilename, Collection<MediaLibraryPath> movieLibraryPaths)  throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (Integer.parseInt(MovieJukebox.class.getPackage().getImplementationVersion()) >= 1759) {
            Class[] argTypes = {String.class, Collection.class};
            Object[] args = {logFilename, movieLibraryPaths};
            Method renameMethod = MovieJukebox.class.getDeclaredMethod("renameLogFile", argTypes);
            renameMethod.setAccessible(true);
            renameMethod.invoke(null, args);
        } else {
            if (Boolean.parseBoolean(getProperty("mjb.appendDateToLogFile", "false"))) {
                // File (or directory) with old name
                File file = new File(logFilename);

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-kkmmss");
                logFilename = "moviejukebox_" + dateFormat.format(timeStart) + ".log";

                // File with new name
                File file2 = new File(logFilename);

                // Rename file (or directory)
                if (!file.renameTo(file2)) {
                    mjbLogger.info("Error renaming log file.");
                }
            }
        }
    }*/

    private static void resetHandlers() throws IOException {
        /*mjbLogger.removeHandler(fh);
        mjbLogger.removeHandler(ch);

        fh = new FileHandler(logFilename);
        fh.setFormatter(mjbFormatter);
        fh.setLevel(Level.ALL);

        //ch = new WorkerConsoleHandler();
        ch = new ConsoleHandler() {
            @Override
            public void publish(LogRecord record) {
                //super.publish(record);
                if (isLoggable(record)) {
                    logger.fine(record.getMessage());
                    lastMessage = record.getMessage();
                }
            }
        };
        ch.setFormatter(mjbFormatter);
        ch.setLevel(Level.FINE);
        //ch.setLevel(Level.ALL);

        mjbLogger.addHandler(fh);
        mjbLogger.addHandler(ch);*/

        mjbLogger.removeAllAppenders();
        fa = new FileAppender(new com.moviejukebox.tools.FilteringLayout(),"moviejukebox.log");
        fa.setThreshold(Priority.INFO);

        ca = new ConsoleAppender(new com.moviejukebox.tools.FilteringLayout()) {
            @Override
            public void append(LoggingEvent event) {
                super.append(event);
                logger.fine(event.getRenderedMessage());
                lastMessage = event.getRenderedMessage();
            }
        };
        ca.setThreshold(Priority.INFO);

        mjbLogger.addAppender(fa);
        mjbLogger.addAppender(ca);
    }

    public static void setConsoleHandlerOnly(MySwingWorker w) throws IOException {
        resetHandlers();
        //mjbLogger.removeHandler(fh);
        mjbLogger.removeAppender(fa);
        ch.setLevel(Level.FINER);
    }

    /*private static class WorkerConsoleHandler extends ConsoleHandler {
        MySwingWorker worker;
        public WorkerConsoleHandler(MySwingWorker w) {
            super();
            worker = w;
        }

        public WorkerConsoleHandler() {
            super();
        }

        @Override
        public void publish(LogRecord record) {
            super.publish(record);
            if (isLoggable(record) && worker != null) {
                worker.doPublish(record.getMessage());
                logger.fine(record.getMessage());
            }
        }
        public void setWorker(MySwingWorker w) {
            worker = w;
        }
    }*/

    public static String getFullDetailsPath() {
        return getProperty("mjb.jukeboxRoot")+"/"+getProperty("mjb.detailsDirName");
    }

    public static String getJukeboxRoot() {
        return getProperty("mjb.jukeboxRoot");
    }

    public static ThreadExecutor getThreadExecutor() {
        int MaxThreadsProcess = Integer.parseInt(getProperty("mjb.MaxThreadsProcess", "0"));
        if(MaxThreadsProcess <= 0) MaxThreadsProcess = Runtime.getRuntime().availableProcessors();
        int MaxThreadsDownload = Integer.parseInt(getProperty("mjb.MaxThreadsDownload", "0"));
        if(MaxThreadsDownload <= 0) MaxThreadsDownload = MaxThreadsProcess;
        return new ThreadExecutor<Void>(MaxThreadsProcess, MaxThreadsDownload);
    }

    private static void setJukebox() throws NoSuchFieldException, IllegalAccessException {
        //Class[] argTypes = {String.class, String.class, String.class};
        Field jukeboxField = MovieJukebox.class.getDeclaredField("jukebox");
        jukeboxField.setAccessible(true);
        //Object[] args = {getProperty("mjb.jukeboxRoot"), getProperty("mjb.jukeboxTempDir", "./temp"), getProperty("mjb.detailsDirName", "Jukebox")};
        Jukebox jukebox = new Jukebox(getProperty("mjb.jukeboxRoot"), getProperty("mjb.jukeboxTempDir", "./temp"), getProperty("mjb.detailsDirName", "Jukebox"));
        jukeboxField.set(null, jukebox);
    }

    public static void RegenerateMovieData(Movie movie, MovieSearchPluginItem selectedPlugin, String id) {
        logger.fine("Regenerating movie data...");
        //String newImdbID = id;
        boolean videoimageDownload = parseBoolean(getProperty("mjb.includeVideoImages", "false"));
        boolean fanartMovieDownload = parseBoolean(getProperty("fanart.movie.download","false"));//FanartScanner.checkDownloadFanart(false);
        boolean fanartTvDownload = parseBoolean(getProperty("fanart.tv.download","false"));//FanartScanner.checkDownloadFanart(true);
        boolean bannerDownload = parseBoolean(getProperty("mjb.includeWideBanners", "false"));
        String skinHome = getProperty("mjb.skin.dir", "./skins/default");

        final MovieJukeboxXMLWriter xmlWriter = new MovieJukeboxXMLWriter();
        final MovieJukeboxHTMLWriter htmlWriter = new MovieJukeboxHTMLWriter();
        final String jukeboxDetailsRoot = getFullDetailsPath();
        final String jukeboxRoot = JukeboxInterface.getJukeboxRoot();

        Library library = new Library();
        //MovieSearchPluginItem selectedPlugin = (MovieSearchPluginItem)cmbMovieID.getSelectedItem();
        String pluginID = selectedPlugin.getPluginID();
        //Movie movie = getSelectedMovie(false);
        movie.setId(pluginID, id);
        //movie.setDirty(true);
        movie.setDirty(DirtyFlag.INFO, true);
        try {
            ToolSet tools = getToolThread().get();

            String baseName = FileTools.makeSafeFilename(movie.getBaseName());
            File finalXmlFile = new File(jukeboxDetailsRoot + File.separator + baseName + ".xml");
            finalXmlFile.delete();

            logger.fine("Updating movie data from "+pluginID+"...");
            //String oldValue = getProperty("mjb.forceXMLOverwrite", "false");
            String oldMoviePlugin = getProperty("mjb.internet.plugin", "com.moviejukebox.plugin.ImdbPlugin").trim();
            String oldTVPlugin = getProperty("mjb.internet.tv.plugin", "com.moviejukebox.plugin.TheTvDBPlugin").trim();

            //setProperty("mjb.forceXMLOverwrite", "true");
            //if (isTV()) {
                setProperty("mjb.internet.plugin", selectedPlugin.getClassName());
            //} else {
                setProperty("mjb.internet.tv.plugin", selectedPlugin.getClassName());
            //}
            //JukeboxWrapper.setConsoleHandlerOnly(this);
            //JukeboxWrapper.updateMovieData(xmlWriter, tools.miScanner, tools.backgroundPlugin, jukeboxRoot, jukeboxRoot, movie);
            for (MovieFile mf : movie.getMovieFiles()) {
                mf.setNewFile(true);
            }
            updateMovieData(movie);
            //setProperty("mjb.forceXMLOverwrite", oldValue);
            setProperty("mjb.internet.plugin", oldMoviePlugin);
            setProperty("mjb.internet.tv.plugin", oldTVPlugin);
            /*movie.setDirty(true);
            movie.setDirtyPoster(true);
            movie.setDirtyFanart(true);
            movie.setDirtyBanner(true);
            movie.setDirtyNFO(true);*/
            movie.setDirty(DirtyFlag.INFO, true);
            movie.setDirty(DirtyFlag.POSTER, true);
            movie.setDirty(DirtyFlag.FANART, true);
            movie.setDirty(DirtyFlag.BANNER, true);
            movie.setDirty(DirtyFlag.NFO, true);

            // Then get this movie's poster
            logger.fine("Getting movie poster...");
            JukeboxInterface.updateMoviePoster(movie);

            // Download episode images if required
            if (videoimageDownload) {
                VideoImageScanner.scan(tools.imagePlugin, JukeboxInterface.getJukebox(jukeboxRoot), movie);
            }

            // Get Fanart only if requested
            // Note that the FanartScanner will check if the file is newer / different
            if ((fanartMovieDownload && !movie.isTVShow()) || (fanartTvDownload && movie.isTVShow())) {
                File fanartFile = new File(JukeboxInterface.getFullDetailsPath()+File.separator+movie.getFanartFilename());
                if (fanartFile.exists() && !fanartFile.getName().equals(Movie.UNKNOWN)) fanartFile.delete();
                FanartScanner.scan(tools.backgroundPlugin, JukeboxInterface.getJukebox(jukeboxRoot), movie);
            }

            // Get Banner if requested and is a TV show
            if (bannerDownload && movie.isTVShow()) {
                if (!BannerScanner.scan(tools.imagePlugin, JukeboxInterface.getJukebox(jukeboxRoot), movie)) {
                    JukeboxInterface.updateTvBanner(movie);
                }
            }

            // Get subtitle
            tools.subtitlePlugin.generate(movie);

            // Get Trailer
            tools.trailerPlugin.generate(movie);

            library.mergeExtras();
            OpenSubtitlesPlugin.logOut();

            logger.fine("Writing XML...");
            xmlWriter.writeMovieXML(JukeboxInterface.getJukebox(jukeboxRoot), movie, library);
            logger.fine("Creating detail poster...");
            MovieJukebox.createPoster(tools.imagePlugin, JukeboxInterface.getJukebox(jukeboxRoot), skinHome, movie, true);
            logger.fine("Creating thumbnail...");
            MovieJukebox.createThumbnail(tools.imagePlugin, JukeboxInterface.getJukebox(jukeboxRoot), skinHome, movie, true);
            if (!Boolean.parseBoolean(getProperty("mjb.skipHtmlGeneration","false"))) {
                logger.fine("Creating HTML page...");
                htmlWriter.generateMovieDetailsHTML(JukeboxInterface.getJukebox(jukeboxRoot), movie);

                logger.fine("Creating playlist file...");
                htmlWriter.generatePlaylist(JukeboxInterface.getJukebox(jukeboxRoot), movie);
            } else {
                logger.fine("Updating XML indices...");
                MovieXmlTools.updateMovieXmlIndices(movie);
            }
            logger.fine("Movie details regeneration complete.");
        } catch (Exception ex) {
            logger.severe(ex+": regenerating "+movie.getBaseFilename()+" information");
        }
    }

    public static HashMap<String,MovieSearchPluginItem> getSearchPluginsMap() {
        HashMap<String,MovieSearchPluginItem> plugins = new HashMap();
        try {
            ArrayList<MovieSearchPluginItem> list = getSearchPluginsArray();
            for (int i = 0; i < list.size(); i++) plugins.put(list.get(i).toString(), list.get(i));
        } catch (Exception ex) {
            logger.severe(ex+": Error getting search plugins map");
        }
        return plugins;
    }

    public static ArrayList<MovieSearchPluginItem> getSearchPluginsArray() {
        ArrayList<MovieSearchPluginItem> list = new ArrayList();
        try {
            list.add(new MovieSearchPluginItem(AllocinePlugin.ALLOCINE_PLUGIN_ID,AllocinePlugin.class));
            list.add(new MovieSearchPluginItem(AniDbPlugin.ANIDB_PLUGIN_ID,AniDbPlugin.class));
            list.add(new MovieSearchPluginItem(AnimatorPlugin.ANIMATOR_PLUGIN_ID,AnimatorPlugin.class));
            list.add(new MovieSearchPluginItem(ComingSoonPlugin.COMINGSOON_PLUGIN_ID,ComingSoonPlugin.class));
            list.add(new MovieSearchPluginItem(FanartTvPlugin.FANARTTV_PLUGIN_ID,FanartTvPlugin.class));
            list.add(new MovieSearchPluginItem(FilmDeltaSEPlugin.FILMDELTA_PLUGIN_ID,FilmDeltaSEPlugin.class));
            list.add(new MovieSearchPluginItem(FilmKatalogusPlugin.FILMKAT_PLUGIN_ID,FilmKatalogusPlugin.class));
            list.add(new MovieSearchPluginItem(FilmUpITPlugin.FILMUPIT_PLUGIN_ID,FilmUpITPlugin.class));
            list.add(new MovieSearchPluginItem(FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID,FilmaffinityPlugin.class));
            list.add(new MovieSearchPluginItem(FilmwebPlugin.FILMWEB_PLUGIN_ID,FilmwebPlugin.class));
            list.add(new MovieSearchPluginItem(ImdbPlugin.IMDB_PLUGIN_ID,ImdbPlugin.class));
            list.add(new MovieSearchPluginItem(KinopoiskPlugin.KINOPOISK_PLUGIN_ID,KinopoiskPlugin.class));
            list.add(new MovieSearchPluginItem(OfdbPlugin.OFDB_PLUGIN_ID,OfdbPlugin.class));
            list.add(new MovieSearchPluginItem(RottenTomatoesPlugin.ROTTENTOMATOES_PLUGIN_ID,RottenTomatoesPlugin.class));
            list.add(new MovieSearchPluginItem(ScopeDkPlugin.SCOPEDK_PLUGIN_ID,ScopeDkPlugin.class));
            list.add(new MovieSearchPluginItem(SratimPlugin.SRATIM_PLUGIN_ID,SratimPlugin.class));
            list.add(new MovieSearchPluginItem(TVRagePlugin.TVRAGE_PLUGIN_ID,TVRagePlugin.class));
            list.add(new MovieSearchPluginItem(TheMovieDbPlugin.TMDB_PLUGIN_ID,TheMovieDbPlugin.class));
            //list.add(new MovieSearchPluginItem("moviedb",TheMovieDbPlugin.class));
            list.add(new MovieSearchPluginItem(TheTvDBPlugin.THETVDB_PLUGIN_ID, TheTvDBPlugin.class));
            list.add(new MovieSearchPluginItem(MovieMeterPlugin.MOVIEMETER_PLUGIN_ID,MovieMeterPlugin.class));
        } catch (Exception ex) {
            logger.severe(ex+": Error getting search plugins array");
        }
        return list;
    }

    public static int getYAMJRevision() {
        return Integer.parseInt(MovieJukebox.class.getPackage().getImplementationVersion());
    }

    public static DefaultListModel getDefaultGenres() {
        DefaultListModel model = new DefaultListModel();
        File genresFile = new File(getProperty("mjb.xmlGenreFile","genres-default.xml"));


        return model;
    }

    public static List<NMTLibrary> getLibraries(File libraryFile) {
        List<NMTLibrary> libraries = new ArrayList();
        if (libraryFile.exists()) {
            try {
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(libraryFile);

                Element libsEle = doc.getDocumentElement();
                NodeList libsNL = doc.getElementsByTagName("library");
                for (int i = 0; i < libsNL.getLength(); i++) {
                    Element libEle = (Element)libsNL.item(i);
                    Element pathEle = (Element)libEle.getElementsByTagName("path").item(0);
                    Element playerPathEle = (Element)libEle.getElementsByTagName("playerpath").item(0);
                    if (playerPathEle == null) {
                        playerPathEle = (Element)libEle.getElementsByTagName("nmtpath").item(0);
                    }
                    Element excludeEle = (Element)libEle.getElementsByTagName("exclude").item(0);
                    Element descEle = null;
                    if(libEle.getElementsByTagName("description") != null ) {
                        descEle = (Element)libEle.getElementsByTagName("description").item(0);
                    }
                    String pathStr, playerPathStr, excludeStr, descStr;
                    excludeStr = descStr = null;
                    pathStr = pathEle.getTextContent();
                    playerPathStr = playerPathEle.getTextContent();
                    if (excludeEle != null) {
                        excludeStr = excludeEle.getAttribute("name");
                    }
                    if (descEle != null && descEle.getTextContent() != null) {
                        descStr = descEle.getTextContent();
                    }
                    NMTLibrary libObj = new NMTLibrary(pathStr, playerPathStr, excludeStr, descStr);
                    libraries.add(libObj);
                    //libs.addElement(libObj);
                }

            } catch (Exception ex) {
                YAYManView.logger.severe("Error loading library file: "+ex);
            }
        } else {
            YAYManView.logger.severe(libraryFile.getPath()+" does not exist");
        }
        return libraries;
    }

    public static List<NMTLibrary> getLibraries(String libraryFile) {
        return getLibraries(new File(libraryFile));
    }

    public static String getLastLogMessage() {
        return lastMessage;
    }

    public static boolean isMediaInfoScannerActivated() {
        boolean activated = false;
        try {
            Field field = com.moviejukebox.scanner.MediaInfoScanner.class.getDeclaredField("isActivated");
            field.setAccessible(true);
            activated = field.getBoolean(null);

        } catch (Exception ex) {
            logger.severe("Error checking status of MediaInfoScanner: "+ex);
        }
        return activated;
    }

    public static void mediaInfoCheck(final YAYManPrefs prefs) {
        new SwingWorker<Void,String>() {
            public Void doInBackground() {
            try {
            File miDir = new File("mediaInfo");
            //com.moviejukebox.scanner.MediaInfoScanner scanner = new com.moviejukebox.scanner.MediaInfoScanner();

            //if (!miDir.exists() || !miDir.isDirectory() || miDir.listFiles().length == 1) {
            if (!JukeboxInterface.isMediaInfoScannerActivated()) {
                logger.warning("MediaInfo not detected...");
                String os = System.getProperty("os.name").toLowerCase();
                String downloadPage = null;
                String matchPattern = null;
                String saveFileName = "mediainfo.zip";
                if (os.indexOf("win") > -1) {
                    downloadPage = "http://mediainfo.sourceforge.net/en/Download/Windows";
                    matchPattern = "http://sourceforge.net/projects/mediainfo/files/binary/mediainfo/"+"(.+?)"+"CLI"+"(.+?)"+"/download";
                } else if (os.indexOf("mac") > -1) {
                    //downloadPage = "http://code.google.com/p/yayman/downloads/list";
                    //matchPattern = "http://yayman.googlecode.com/files/"+"(.*?)"+"MediaInfo"+"(.*?)"+"Mac"+"(.*?)"+"CLI"+"(.*?)"+".zip";
                    File miFile = new File("/usr/local/bin/mediainfo");
                    if (miFile.exists()) {
                        EditPropertiesFrame propsFrame = new EditPropertiesFrame(prefs, prefs.getSelectedJukebox(), false, EditPropertiesFrame.PropertiesMode);
                        propsFrame.setProperty("mediainfo.home", "/usr/local/bin/");
                        propsFrame.closeSave();
                    } else {
                        if (javax.swing.JOptionPane.showConfirmDialog(null, "MediaInfo not found. Install it?", "MediaInfo", javax.swing.JOptionPane.OK_OPTION,javax.swing.JOptionPane.WARNING_MESSAGE) == javax.swing.JOptionPane.YES_OPTION) {
                            downloadPage = "http://mediainfo.sourceforge.net/en/Download/Mac_OS";
                            matchPattern = "http://downloads.sourceforge.net/mediainfo/MediaInfo_CLI_"+"(.+?)"+"_Mac_Universal.dmg";
                            saveFileName = "mediainfo.dmg";
                        } else {
                            return null;
                        }
                        
                    }
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
                    if (os.indexOf("win") > -1) {
                        downloadUrl = m.group();
                    } else if (os.indexOf("mac") > -1) {
                        String ver = m.group(1);
                        downloadUrl = "http://sourceforge.net/projects/mediainfo/files/binary/mediainfo/"+ver+"/MediaInfo_CLI_"+ver+"_Mac_Universal.dmg/download";
                        logger.fine("Downloading mediainfo...");
                    }
                    break;
                }

                url = new URL(downloadUrl);

                URLConnection conn = url.openConnection();
                int size = conn.getContentLength();
                BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
                FileOutputStream fos = new FileOutputStream(saveFileName);
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


                if (os.indexOf("win") > -1) {
                    File miFile = new File(saveFileName);
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
                } else if (os.indexOf("mac") > -1) {
                    logger.fine("Mounting mediainfo disk image...");
                    //Process proc = Runtime.getRuntime().exec("hdiutil detach \"/Volumes/MediaInfo CLI\"");
                    //proc.waitFor();
                    Process proc = Runtime.getRuntime().exec("hdiutil attach "+saveFileName);
                    //proc.waitFor();
                    //proc = Runtime.getRuntime().exec("open \"/Volumes/MediaInfo CLI/MediaInfo CLI.pkg\"");
                    //proc = Runtime.getRuntime().exec("installer -pkg \"/Volumes/MediaInfo CLI/MediaInfo CLI.pkg\" -target /");
                }
            }
            }catch (Exception ex) {
                System.out.println("Error getting MediaInfo: "+ex);
            }
            return null;
            }
        }.execute();
    }
    
    public static Movie cloneMovie(Movie movie) {
        Movie newMovie = new Movie();
        newMovie.setTitle(movie.getTitle(),"yayman");
        newMovie.setContainer(movie.getContainer(),"yayman");
        newMovie.setFile(movie.getFile());
        newMovie.setContainerFile(movie.getContainerFile());
        newMovie.setAspectRatio(movie.getAspectRatio(), "yayman");
        //newMovie.setAudioChannels(movie.getAudioChannels());
        //newMovie.setAudioCodec(movie.getAudioCodec());
        newMovie.setBaseFilename(movie.getBaseFilename());
        newMovie.setBaseName(movie.getBaseName());
        newMovie.setExtra(movie.isExtra());
        newMovie.setFileDate(movie.getFileDate());
        newMovie.setFileSize(movie.getFileSize());
        newMovie.setFormatType(movie.getFormatType());
        newMovie.setFps(movie.getFps(),"yayman");
        newMovie.setResolution(movie.getResolution(),"yayman");
        newMovie.setRuntime(movie.getRuntime(),"yayman");
        movie.setSetMaster(movie.isSetMaster());
        movie.setSetSize(movie.getSetSize());
        movie.setSubtitles(movie.getSubtitles());
        //movie.setVideoCodec(movie.getVideoCodec());
        movie.setVideoOutput(movie.getVideoOutput(),"yayman");
        movie.setVideoSource(movie.getVideoSource(),"yayman");
        movie.setVideoType(movie.getVideoType());
        
        for (MovieFile mf : movie.getMovieFiles()) {
            MovieFile bmf = new MovieFile();
            bmf.setFile(mf.getFile());
            newMovie.addMovieFile(bmf);
        }
        
        for (ExtraFile ef : movie.getExtraFiles()) {
            ExtraFile bef = new ExtraFile();
            bef.setFile(ef.getFile());
            newMovie.addExtraFile(bef);
        }
        
        for (String setName : movie.getSets().keySet()) {
            Map<String,Integer> sets = new HashMap();
            sets.put(setName, movie.getSets().get(setName));
            newMovie.setSets(sets);
        }
        return newMovie;
    }
    
    public static void findLibXMLFiles() {
        libXmlFiles = new ArrayList();
        
        File jukeDir = new File(JukeboxInterface.getFullDetailsPath());
        FilenameFilter xmlFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                //String lowercaseName = name.toLowerCase();
                if (name.matches("Certification_(.)+_(\\d)+\\.xml")) {
                    return true;
                } else if (name.matches("Genres_(.)+_(\\d)+\\.xml")) {
                    return true;
                } else if (name.matches("Other_(All|HD|Movings|Rating|Sets|Top250|TV Shows|Unwatched)_(\\d)+\\.xml")) {
                    return true;
                } else if (name.matches("Set_(.)+_(\\d)+\\.xml")) {
                    return true;
                } else if (name.matches("Title_([A-Z]|09)_(\\d)+\\.xml")) {
                    return true;
                } else if (name.matches("Year_(\\d){4}-(\\d){2}_(\\d)+\\.xml")) {
                    return true;
                } else if (name.matches("Year_Last Year_(\\d)+\\.xml")) {
                    return true;
                } else if (name.matches("Year_This Year_(\\d)+\\.xml")) {
                    return true;
                } else {
                    return false;
                }
            }
        };
        File[] files = jukeDir.listFiles(xmlFilter);
        libXmlFiles.addAll(Arrays.asList(files));
    }
    
    public static ArrayList<File> getLibXMLFiles() {
        return libXmlFiles;
    }
}
