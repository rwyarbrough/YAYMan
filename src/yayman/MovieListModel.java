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

import javax.swing.DefaultListModel;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.Library;

public class MovieListModel extends DefaultListModel {
    private Library library;

    public MovieListModel() {
        super();
        setLibrary(null);
    }

    public MovieListModel(Library lib) {
        super();
        setLibrary(lib);
    }

    public Movie getMovie(int index) {
        return ((MovieWrapper)super.get(index)).getMovie();
    }

    public Library getLibrary() {
        return library;
    }

    public void setLibrary(Library lib) {
        library = lib;
        super.removeAllElements();
        if (library != null && library.size() > 0) {
            for (final Movie movie : library.values()) {
                MovieWrapper mw = new MovieWrapper(movie);
                super.addElement(mw);
            }
        }
    }

    public void setBackgroundLibrary(Library lib) {
        library = lib;
    }
}