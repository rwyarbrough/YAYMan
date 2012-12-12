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

import com.moviejukebox.plugin.MovieDatabasePlugin;
import java.lang.reflect.*;
import java.util.logging.*;

public class MovieSearchPluginItem {
    private Class pluginClass;
    private String id;
    private static Logger logger = Logger.getLogger("yayman");

    public MovieSearchPluginItem(String ID, Class cl) {
        pluginClass = cl;
        id = ID;
    }

    public MovieSearchPluginItem(Class cl) {
        pluginClass = cl;
    }

    public MovieSearchPluginItem(String className) throws ClassNotFoundException {
        this(Class.forName(className));
    }

    public String getClassName() {
        return pluginClass.getName();
    }

    public Class getPluginClass() {
        return pluginClass;
    }

    @Override
    public String toString() {
        return id;//getPluginID();
    }

    public MovieDatabasePlugin getPlugin() throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        return (MovieDatabasePlugin)pluginClass.getConstructor(new Class[]{}).newInstance(new Object[]{});
    }

    public String getPluginID() {
        String pluginID = null;
        logger.fine("Getting pluginid for "+pluginClass.getSimpleName());
        try {
            Field[] fields = pluginClass.getFields();
            for (int i=0; i < fields.length; i++) {
                Field field = fields[i];
                if (field.getName().endsWith("_PLUGIN_ID") && field.getName().startsWith(pluginClass.getSimpleName().replaceAll("Plugin", "").toUpperCase())) {
                    pluginID = field.get(null).toString();
                    break;
                } else {
                    logger.fine(field.getName()+" doesn't match");
                }
            }
            if (pluginClass.getSimpleName().equals("FilmaffinityPlugin")) {
                pluginID = com.moviejukebox.plugin.FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID;
            }
        } catch (Exception ex) {
            logger.severe("Error getting plugin id for "+pluginClass.getName()+" :"+ex);
        }
        return pluginID;
    }
}
