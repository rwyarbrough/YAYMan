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

import com.moviejukebox.model.Movie;

public class MovieWrapper implements Comparable {
    Movie movie;

    public MovieWrapper() {
        movie = null;
    }

    public MovieWrapper(Movie mov) {
        movie = mov;
    }

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie mov) {
        movie = mov;
    }
    
    @Override
    public String toString() {
        if (movie != null) {
            //return movie.getBaseName();
            if (movie.getBaseFilename().length() > 25) return movie.getBaseFilename().substring(0, 22)+"...";
            return movie.getBaseFilename();
        }
        return "NullMovie";
    }

    public int compareTo(Object obj) {
        return this.toString().compareTo(obj.toString());
    }
}
