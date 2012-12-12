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

public class NMTLibrary {
    String path;
    String playerpath;
    String excludename;
    String description;

    public NMTLibrary() {
        path = playerpath = excludename = description = "";
    }

    public NMTLibrary(String Path, String PlayerPath, String ExcludeName) {
        this();
        path = Path;
        playerpath = PlayerPath;
        excludename = ExcludeName;
    }

    public NMTLibrary(String Path, String PlayerPath, String ExcludeName, String Description) {
        this(Path, PlayerPath, ExcludeName);
        description = Description;
    }

    @Override
    public String toString() {
        if (description == null || description.equals("")) {
            return path;
        }
        return description;
    }

    public String getPath() {
        return path;
    }

    public String getPlayerPath() {
        return playerpath;
    }

    public String getExclude() {
        return excludename;
    }

    public String getDescription() {
        if (description == null) description = "";
        return description;
    }

    public void setPath(String Path) {
        path = Path;
    }

    public void setPlayerPath(String PlayerPath) {
        playerpath = PlayerPath;
    }

    public void setExclude(String ExcludeName) {
        excludename = ExcludeName;
    }

    public void setDescription(String Description) {
        description = Description;
    }
}
