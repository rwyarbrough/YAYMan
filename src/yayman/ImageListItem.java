/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package yayman;

import javax.swing.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.awt.*;
import java.net.*;
import java.io.*;
import javax.imageio.*;
import java.awt.image.*;

import com.omertron.themoviedbapi.model.Artwork;
import com.omertron.thetvdbapi.model.Banner;



/**
 *
 * @author Nordin
 */
public class ImageListItem extends ImageIcon {
    private String original;
    private String display;
    private String thumb;
    private JList thumbList;
    private Image waitImage;
    private int mode;
    
    public static int URLMODE = 1;
    public static int LOCALMODE = 2;

    public ImageListItem(String orig, String disp, final String thum, JList list) throws MalformedURLException, IOException {
        super();
        thumbList = list;
        mode = ImageListItem.URLMODE;
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(yayman.YAYManApp.class).getContext().getResourceMap(ImageManager.class);
        waitImage = resourceMap.getImageIcon("thumbnailWaitIcon").getImage();
        setImage(thum);
        original = orig;
        display = disp;
        thumb = thum;
    }

    public ImageListItem(Artwork orig, Artwork disp, Artwork thum, JList list) throws MalformedURLException, IOException {
        this(orig.getFilePath(), disp.getFilePath(), thum.getFilePath(), list);
    }

    public ImageListItem(Artwork orig, Artwork disp, JList list) throws MalformedURLException, IOException {
        this(orig, disp, disp, list);
    }

    public ImageListItem(final Artwork art, JList list) throws MalformedURLException, IOException {
        this(art, art, list);
    }

    public ImageListItem(Banner banner, JList list) throws MalformedURLException, IOException {
        this(banner.getUrl(), banner.getUrl(), banner.getUrl().replace("/banners/", "/banners/_cache/"), list);
    }
    
    public ImageListItem(File file, JList list) throws MalformedURLException, IOException {
        super();
        mode = ImageListItem.LOCALMODE;
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(yayman.YAYManApp.class).getContext().getResourceMap(ImageManager.class);
        waitImage = resourceMap.getImageIcon("thumbnailWaitIcon").getImage();
        setImage(file.getPath());
        original = file.getPath();
        display = file.getPath();
        thumb = file.getPath();
    }

    @Override
    public String toString() {
        String url = original;
        return url.substring(url.lastIndexOf("/")+1);
    }

    public String getOriginal() {
        return original;
    }

    public String getDisplay() {
        return display;
    }

    public String getThumb() {
        return thumb;
    }

    private void setImage(final String path) throws MalformedURLException, IOException {
        ExecutorService threadPool = Executors.newFixedThreadPool(1);
        threadPool.submit(new SwingWorker() {
            @Override
            protected Void doInBackground() throws MalformedURLException, IOException {
                ImageListItem.super.setImage(waitImage);
                ImageListItem.super.setImage(getScaledImage(path));
                if (thumbList != null) thumbList.repaint();
                return null;
            }
        });
        threadPool.shutdown();
    }

    private Image getScaledImage(String path) throws MalformedURLException, IOException {
       // try {
            Image img;
            if (this.mode == URLMODE) {
                //img = ImageIO.read(new URL(path));
                img = new ImageIcon(new URL(path)).getImage();
            } else {
                //img = ImageIO.read(new File(path));
                img = new ImageIcon(path).getImage();
            }
            //System.out.println("getting scaled image for: "+path);
            //System.out.println("X: "+img.getWidth(null)+", Y: "+img.getHeight(null));
            int w = thumbList.getFixedCellWidth();
            int h = thumbList.getFixedCellHeight();
            int iw = img.getWidth(null);
            int ih = img.getHeight(null);
            double xScale = (double)w/iw;
            double yScale = (double)h/ih;
            double scale = Math.min(xScale, yScale);
            int width = (int)(scale*iw);
            int height = (int)(scale*ih);
            Image scaledImage = img.getScaledInstance(width, height, Image.SCALE_FAST);
            img.flush();
            img = null;
            return scaledImage;
        /*} catch (Exception ex) {
            logger.severe("Error scaling thumbnail image: "+ex);
            logger.severe("Image path: "+path);
            return null;
        }*/
    }
}
