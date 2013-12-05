/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package yayman;

import java.util.ArrayList;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import java.awt.Image;

public class ImageCycler extends ArrayList<ImageIcon> {
    private int index;
    private JComponent displayComponent;
    private ArrayList<ImageIcon> unscaledImages;
    private ArrayList<String> imageTypes;

    public ImageCycler() {
        super();
        index = 0;
        displayComponent = null;
        unscaledImages = new ArrayList();
        imageTypes = new ArrayList();
    }

    public ImageCycler(JComponent comp) {
        this();
        displayComponent = comp;
    }

    public ImageIcon nextImage() {
        index++;
        if (index > this.size()-1) index = 0;
        return this.get(index);
    }

    public ImageIcon previousImage() {
        index--;
        if (index < 0) index = this.size()-1;
        return this.get(index);
    }

    public int getIndex() {
        return index;
    }

    public boolean add(ImageIcon image, String type) {
        if (displayComponent != null) {
            unscaledImages.add(image);
            Image img = image.getImage();
            int w = displayComponent.getWidth();
            int h = displayComponent.getHeight();
            int iw = img.getWidth(null);
            int ih = img.getHeight(null);
            double xScale = (double)w/iw;
            double yScale = (double)h/ih;
            double scale = Math.min(xScale, yScale);
            int width = (int)(scale*iw);
            int height = (int)(scale*ih);
            image = new ImageIcon(img.getScaledInstance(width, height, Image.SCALE_FAST));
        }
        imageTypes.add(type);
        return super.add(image);
    }

    @Override
    public boolean add(ImageIcon image) {
        return add(image,"Image");
    }

    public ImageIcon getUnscaledImage(int i) {
        if (unscaledImages.size() == 0) return get(i);
        return unscaledImages.get(i);
    }

    public ImageIcon getUnscaledImage() {
        return getUnscaledImage(index);
    }

    public String getImageType(int i) {
        return imageTypes.get(i);
    }

    public String getImageType() {
        return imageTypes.get(index);
    }

    public void dispose() {
        index = -1;
        for (int i = 0; i < super.size(); i++) {
            super.get(i).getImage().flush();
            unscaledImages.get(i).getImage().flush();
        }
    }
}
