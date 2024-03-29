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


/*
 * imageViewer.java
 *
 * Created on Jan 14, 2010, 11:26:56 AM
 */

package yayman;

import javax.swing.*;
import java.awt.*;
import java.net.*;
import java.util.*;
import java.awt.image.BufferedImage;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.awt.dnd.*;
import javax.swing.border.*;
import java.awt.datatransfer.*;
import javax.swing.TransferHandler;

import org.w3c.dom.*;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
//import com.moviejukebox.model.Artwork.*;
//import com.moviejukebox.model.Artwork.Artwork;
import com.omertron.themoviedbapi.TheMovieDbApi;
import com.omertron.themoviedbapi.model.MovieDb;
import com.omertron.themoviedbapi.model.Artwork;
import com.omertron.themoviedbapi.model.ArtworkType;
//import com.moviejukebox.model.Artwork.ArtworkSize;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.plugin.TheTvDBPlugin;
import com.moviejukebox.plugin.TheMovieDbPlugin;
import com.moviejukebox.tools.*;
import com.moviejukebox.plugin.DefaultBackgroundPlugin;

import com.omertron.thetvdbapi.TheTVDBApi;
import com.omertron.thetvdbapi.model.Banner;
import com.omertron.thetvdbapi.model.Banners;
import com.omertron.thetvdbapi.model.Series;
import com.omertron.thetvdbapi.model.Episode;


import java.io.*;
import static com.moviejukebox.tools.PropertiesUtil.getProperty;
import java.util.logging.*;

import java.awt.Toolkit;
import com.moviejukebox.tools.GraphicTools;

public class ImageManager extends javax.swing.JDialog {
    private YAYManView mainProgram;
    private int currentIndex;
    private int attachIndex;
    private Movie currentMovie;
    //public static final String PosterMode = Artwork.ARTWORK_SIZE_POSTER;
    //public static final ArtworkType PosterMode = ArtworkType.POSTER;
    //public static final String FanartMode = Artwork.ARTWORK_TYPE_BACKDROP;
    //public static final ArtworkType FanartMode = ArtworkType.BACKDROP;
    public static final String PosterMode = "Poster";
    public static final String FanartMode = "Fanart";
    public static final String BannerMode = "Banner";
    public static final String VideoImageMode = "VideoImage";
    public static final String SetPosterMode = "SetPoster";
    public static final String SetFanartMode = "SetFanart";
    public static final String SetBannerMode = "SetBanner";
    private boolean isSetImage;
    private String setImageName;
    private String currentMode;
    private ArtworkType movieMode;
    private String tvMode;
    private ImageIcon originalImage;
    private Image newImage;
    private Image attachImage;
    private Image otherImage;
    private Image waitImage;
    private ThreadLocal<JukeboxInterface.ToolSet> threadTools;
    private Logger logger;
    private Border origBorder;
    
    public ImageManager(YAYManView parent, boolean modal, Movie movie, String mode) {
        super(parent.getFrame(), modal);
        initComponents();

        logger = Logger.getLogger("yayman");

        this.addComponentListener(new ComponentListener() {
            public void componentHidden(ComponentEvent e) {

            }

            public void componentMoved(ComponentEvent e) {

            }

            public void componentResized(ComponentEvent e) {
                fitImagesToWindow();
            }

            public void componentShown(ComponentEvent e) {

            }
        });

        internetTxt.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                displayImageFromUrl();
            }
            public void removeUpdate(DocumentEvent e) {
                displayImageFromUrl();
            }
            public void insertUpdate(DocumentEvent e) {
                displayImageFromUrl();
            }
        });

        mainProgram = parent;
        currentMode = mode;
        currentMovie = movie;
        threadTools = JukeboxInterface.getToolThread();
        originalImage = null;

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(yayman.YAYManApp.class).getContext().getResourceMap(ImageManager.class);
        waitImage = resourceMap.getImageIcon("thumbnailWaitIcon").getImage();

        isSetImage = false;
        setImageName = null;
        String displayImage = Movie.UNKNOWN;
        if (currentMode.equals(PosterMode)) {
            displayImage = movie.getPosterFilename();
            movieMode = ArtworkType.POSTER;
            tvMode = PosterMode;
        } else if (currentMode.equals(FanartMode)) {
            displayImage = movie.getFanartFilename();
            thumbList.setFixedCellHeight(75);
            //wizardTabs.setTitleAt(0, "Fanart");
            movieMode = ArtworkType.BACKDROP;
            tvMode = FanartMode;
        } else if (currentMode.equals(BannerMode)) {
            displayImage = movie.getBannerFilename();
            thumbList.setFixedCellHeight(48);
            //wizardTabs.setTitleAt(0, "Banner");
            tvMode = BannerMode;
        } else if (currentMode.equals(VideoImageMode)) {
            XmlTreeNode treeNode = mainProgram.getSelectedEpisodeNode();
            String episode = treeNode.getElement().getAttribute("part");
            XmlTreeNode parentNode = (XmlTreeNode)treeNode.getParent();
            NodeList imageFileNodes = parentNode.getElement().getElementsByTagName("fileImageFile");
            for (int i = 0; i < imageFileNodes.getLength(); i++) {
                Element imageFileNode = (Element)imageFileNodes.item(i);
                if (imageFileNode.getAttribute("part").equals(episode)) {
                    displayImage = HTMLTools.decodeUrl(imageFileNode.getTextContent());
                }
            }
            thumbList.setFixedCellHeight(75);
            //wizardTabs.setTitleAt(0, "Video Image");
            tvMode = currentMode;
        } else if (currentMode.equals(SetPosterMode)) {
            isSetImage = true;
            currentMode = PosterMode;
            displayImage = "Set_"+mainProgram.getSelectedSet()+"_1"+getProperty("mjb.scanner.thumbnailToken","_small")+".png";
            setImageName = displayImage;
            movieMode = ArtworkType.BACKDROP;
            tvMode = PosterMode;
        } else if (currentMode.equals(SetFanartMode)) {
            isSetImage = true;
            currentMode = FanartMode;
            displayImage = "Set_"+mainProgram.getSelectedSet()+"_1"+getProperty("mjb.scanner.fanartToken",".fanart")+".jpg";
            thumbList.setFixedCellHeight(75);
            setImageName = displayImage;
            movieMode = ArtworkType.BACKDROP;
            tvMode = FanartMode;
        } else if (currentMode.equals(SetBannerMode)) {
            isSetImage = true;
            currentMode = BannerMode;
            displayImage = "Set_"+mainProgram.getSelectedSet()+"_1"+getProperty("mjb.scanner.bannerToken",".banner")+".jpg";
            thumbList.setFixedCellHeight(48);
            setImageName = displayImage;
            tvMode = BannerMode;
        }

        wizardTabs.setTitleAt(0, "Current "+currentMode);

        if (!displayImage.equals(Movie.UNKNOWN) && new File(JukeboxInterface.getFullDetailsPath()+File.separator+displayImage).exists()) {
            originalImage = new ImageIcon(JukeboxInterface.getFullDetailsPath()+File.separator+displayImage);
            originalImage.getImage().flush();
            fitImagesToWindow();

            this.setTitle(displayImage);
        } else {
            YAYManView.logger.warning("Image not found: "+displayImage);
            this.setTitle("Image Manager");
        }

        currentIndex = -1;
        attachIndex = -1;
        findOtherImages();
        
        DropTargetListener dtl = new DropTargetListener() {
            public void dragEnter(DropTargetDragEvent dtde) {
                
            }
            
            public void dragExit(DropTargetEvent dtde) {
                useLocalTxt.setBorder(origBorder);
            }
            
            public void dragOver(DropTargetDragEvent dtde) {
                origBorder = useLocalTxt.getBorder();
                useLocalTxt.setBorder(new CompoundBorder());
            }
            
            public void drop(DropTargetDropEvent dtde) {
                
            }
            
            public void dropActionChanged(DropTargetDragEvent dtde) {
                
            }
        };

        useLocalTxt.setTransferHandler(new TransferHandler(){
            public boolean importData(JComponent comp, Transferable t) {
                // Make sure we have the right starting points
                /*if (!(comp instanceof FSTree)) {
                    return false;
                }*/
                if (!t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    return false;
                }

                try {
                    java.util.List data = (java.util.List)t.getTransferData(DataFlavor.javaFileListFlavor);
                    Iterator i = data.iterator();
                    if (i.hasNext()) {
                        File f = (File)i.next();
                        useLocalTxt.setText(f.getName());
                    }
                    return true;
                } catch (UnsupportedFlavorException ufe) {
                    System.err.println("Ack! we should not be here.\nBad Flavor.");
                } catch (IOException ioe) {
                    System.out.println("Something failed during import:\n" + ioe);
                }
                return false;
            }

            public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
                //if (comp instanceof FSTree) {
                    //for (int i = 0; i < transferFlavors.length; i++) {
                        if (!transferFlavors[0].equals(DataFlavor.javaFileListFlavor)) {
                            return false;
                        }
                    //}
                    return true;
                //}
                //return false;
            }
        });

        DropTarget dt = new DropTarget(useLocalTxt,dtl);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        fileChooser = new javax.swing.JFileChooser();
        wizardTabs = new javax.swing.JTabbedPane();
        imgPanel = new javax.swing.JPanel();
        lblImage = new javax.swing.JLabel();
        moviedbPanel = new javax.swing.JPanel();
        lblMidImage = new javax.swing.JLabel();
        useImageBtn = new javax.swing.JButton();
        progressBar = new javax.swing.JProgressBar();
        jScrollPane1 = new javax.swing.JScrollPane();
        thumbList = new javax.swing.JList();
        attachmentsPanel = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        attImageList = new javax.swing.JList();
        useAttachBtn = new javax.swing.JButton();
        lblAttachImage = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        internetTxt = new javax.swing.JTextField();
        useInternetBtn = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        localTxt = new javax.swing.JTextField();
        useLocalTxt = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        lblOtherImage = new javax.swing.JLabel();

        fileChooser.setName("fileChooser"); // NOI18N

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(yayman.YAYManApp.class).getContext().getResourceMap(ImageManager.class);
        setTitle(resourceMap.getString("imageWindow.title")); // NOI18N
        setIconImage(null);
        setIconImages(null);
        setModalityType(java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        setName("imageWindow"); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        wizardTabs.setName("wizardTabs"); // NOI18N

        imgPanel.setName("imgPanel"); // NOI18N

        lblImage.setText(resourceMap.getString("lblImage.text")); // NOI18N
        lblImage.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        lblImage.setName("lblImage"); // NOI18N

        javax.swing.GroupLayout imgPanelLayout = new javax.swing.GroupLayout(imgPanel);
        imgPanel.setLayout(imgPanelLayout);
        imgPanelLayout.setHorizontalGroup(
            imgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblImage, javax.swing.GroupLayout.DEFAULT_SIZE, 520, Short.MAX_VALUE)
        );
        imgPanelLayout.setVerticalGroup(
            imgPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblImage, javax.swing.GroupLayout.DEFAULT_SIZE, 421, Short.MAX_VALUE)
        );

        wizardTabs.addTab(resourceMap.getString("imgPanel.TabConstraints.tabTitle"), imgPanel); // NOI18N

        moviedbPanel.setName("moviedbPanel"); // NOI18N

        lblMidImage.setText(resourceMap.getString("lblMidImage.text")); // NOI18N
        lblMidImage.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        lblMidImage.setName("lblMidImage"); // NOI18N
        lblMidImage.setPreferredSize(new java.awt.Dimension(500, 14));

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(yayman.YAYManApp.class).getContext().getActionMap(ImageManager.class, this);
        useImageBtn.setAction(actionMap.get("useMovieDBImage")); // NOI18N
        useImageBtn.setText(resourceMap.getString("useImageBtn.text")); // NOI18N
        useImageBtn.setToolTipText(resourceMap.getString("useImageBtn.toolTipText")); // NOI18N
        useImageBtn.setName("useImageBtn"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        thumbList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        thumbList.setFixedCellHeight(131);
        thumbList.setFixedCellWidth(92);
        thumbList.setName("thumbList"); // NOI18N
        thumbList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                thumbListValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(thumbList);

        javax.swing.GroupLayout moviedbPanelLayout = new javax.swing.GroupLayout(moviedbPanel);
        moviedbPanel.setLayout(moviedbPanelLayout);
        moviedbPanelLayout.setHorizontalGroup(
            moviedbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(moviedbPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(moviedbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(moviedbPanelLayout.createSequentialGroup()
                        .addComponent(useImageBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(progressBar, 0, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblMidImage, javax.swing.GroupLayout.DEFAULT_SIZE, 382, Short.MAX_VALUE))
        );
        moviedbPanelLayout.setVerticalGroup(
            moviedbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(moviedbPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(moviedbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(moviedbPanelLayout.createSequentialGroup()
                        .addGroup(moviedbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(useImageBtn)
                            .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 365, Short.MAX_VALUE)
                        .addContainerGap())
                    .addComponent(lblMidImage, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 410, Short.MAX_VALUE)))
        );

        wizardTabs.addTab(resourceMap.getString("moviedbPanel.TabConstraints.tabTitle"), moviedbPanel); // NOI18N

        attachmentsPanel.setName("attachmentsPanel"); // NOI18N

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        attImageList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        attImageList.setFixedCellHeight(131);
        attImageList.setFixedCellWidth(92);
        attImageList.setName("attImageList"); // NOI18N
        attImageList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                attImageListValueChanged(evt);
            }
        });
        jScrollPane2.setViewportView(attImageList);

        useAttachBtn.setAction(actionMap.get("useAttachImage")); // NOI18N
        useAttachBtn.setText(resourceMap.getString("useAttachBtn.text")); // NOI18N
        useAttachBtn.setName("useAttachBtn"); // NOI18N

        lblAttachImage.setText(resourceMap.getString("lblAttachImage.text")); // NOI18N
        lblAttachImage.setName("lblAttachImage"); // NOI18N

        javax.swing.GroupLayout attachmentsPanelLayout = new javax.swing.GroupLayout(attachmentsPanel);
        attachmentsPanel.setLayout(attachmentsPanelLayout);
        attachmentsPanelLayout.setHorizontalGroup(
            attachmentsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(attachmentsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(attachmentsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(useAttachBtn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblAttachImage, javax.swing.GroupLayout.DEFAULT_SIZE, 382, Short.MAX_VALUE))
        );
        attachmentsPanelLayout.setVerticalGroup(
            attachmentsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(attachmentsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(attachmentsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblAttachImage, javax.swing.GroupLayout.DEFAULT_SIZE, 410, Short.MAX_VALUE)
                    .addGroup(attachmentsPanelLayout.createSequentialGroup()
                        .addComponent(useAttachBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 365, Short.MAX_VALUE)
                        .addContainerGap())))
        );

        wizardTabs.addTab(resourceMap.getString("attachmentsPanel.TabConstraints.tabTitle"), attachmentsPanel); // NOI18N

        jPanel3.setName("jPanel3"); // NOI18N

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        internetTxt.setText(resourceMap.getString("internetTxt.text")); // NOI18N
        internetTxt.setName("internetTxt"); // NOI18N

        useInternetBtn.setAction(actionMap.get("useInternetImage")); // NOI18N
        useInternetBtn.setText(resourceMap.getString("useInternetBtn.text")); // NOI18N
        useInternetBtn.setName("useInternetBtn"); // NOI18N

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        localTxt.setEditable(false);
        localTxt.setText(resourceMap.getString("localTxt.text")); // NOI18N
        localTxt.setName("localTxt"); // NOI18N

        useLocalTxt.setAction(actionMap.get("useLocalImage")); // NOI18N
        useLocalTxt.setText(resourceMap.getString("useLocalTxt.text")); // NOI18N
        useLocalTxt.setName("useLocalTxt"); // NOI18N

        jButton1.setAction(actionMap.get("chooseLocalImage")); // NOI18N
        jButton1.setText(resourceMap.getString("jButton1.text")); // NOI18N
        jButton1.setName("jButton1"); // NOI18N

        lblOtherImage.setText(resourceMap.getString("lblOtherImage.text")); // NOI18N
        lblOtherImage.setName("lblOtherImage"); // NOI18N

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblOtherImage, javax.swing.GroupLayout.DEFAULT_SIZE, 500, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addComponent(internetTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 439, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(useInternetBtn))
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addComponent(localTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 352, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(useLocalTxt)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(internetTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(useInternetBtn))
                .addGap(18, 18, 18)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(localTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(useLocalTxt)
                    .addComponent(jButton1))
                .addGap(18, 18, 18)
                .addComponent(lblOtherImage, javax.swing.GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE)
                .addContainerGap())
        );

        wizardTabs.addTab(resourceMap.getString("jPanel3.TabConstraints.tabTitle"), jPanel3); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(wizardTabs, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 525, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(wizardTabs, javax.swing.GroupLayout.DEFAULT_SIZE, 449, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void thumbListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_thumbListValueChanged
        displaySelectedImage();
    }//GEN-LAST:event_thumbListValueChanged

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        thumbList.setModel(new DefaultListModel());
        for (int i = 0; i < thumbList.getModel().getSize(); i ++) {
            ((ImageListItem)thumbList.getModel().getElementAt(i)).getImage().flush();
            thumbList.remove(i);
        }
        if (lblImage.getIcon() != null) ((ImageIcon)lblImage.getIcon()).getImage().flush();
        if (lblMidImage.getIcon() != null) ((ImageIcon)lblMidImage.getIcon()).getImage().flush();
        if (originalImage != null) originalImage.getImage().flush();
        if (newImage != null) newImage.flush();
        if (attachImage != null) attachImage.flush();
        if (otherImage != null) otherImage.flush();
        waitImage.flush();
        threadTools = null;
        ImageManager.this.dispose();
    }//GEN-LAST:event_formWindowClosing

    private void attImageListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_attImageListValueChanged
        displaySelectedAttachment();
    }//GEN-LAST:event_attImageListValueChanged

    /**
    * @param args the command line arguments
    */
    /*public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                ImageManager dialog = new ImageManager(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }*/

    private void findOtherImages() {
        Vector dbImages = new Vector();
        
        if (currentMovie.isTVShow()) {
            loadTvDbPosters(dbImages);
            wizardTabs.setTitleAt(1, "thetvdb");
        } else {
            loadMovieDbPosters(dbImages);
        }

        thumbList.setListData(dbImages);
        if (!dbImages.isEmpty()) {
            thumbList.setSelectedIndex(0);
        } else {
            thumbList.setEnabled(false);
        }
        
        loadAttachmentImages();
    }

    /*class ImageListItem extends ImageIcon {
        private String original;
        private String display;
        private String thumb;
        
        public ImageListItem(String orig, String disp, final String thum) {
            super();
            setImage(thum);
            original = orig;
            display = disp;
            thumb = thum;
        }

        public ImageListItem(Artwork orig, Artwork disp, Artwork thum) {
            this(orig.getFilePath(), disp.getFilePath(), thum.getFilePath());
        }

        public ImageListItem(Artwork orig, Artwork disp) {
            this(orig, disp, disp);
        }

        public ImageListItem(final Artwork art) {
            this(art, art);
        }

        public ImageListItem(Banner banner) {
            this(banner.getUrl(), banner.getUrl(), banner.getUrl().replace("/banners/", "/banners/_cache/"));
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

        private void setImage(final String path) {
            ExecutorService threadPool = Executors.newFixedThreadPool(1);
            threadPool.submit(new SwingWorker() {
                @Override
                protected Void doInBackground() {
                    ImageListItem.super.setImage(waitImage);
                    Image scaledImage = getScaledImage(path);
                    ImageListItem.super.setImage(scaledImage);
                    thumbList.repaint();
                    return null;
                }
            });
            threadPool.shutdown();
        }

        private Image getScaledImage(String path) {
            try {
                Image img = new ImageIcon(new URL(path)).getImage();;
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
            } catch (Exception ex) {
                logger.severe("Error scaling thumbnail image: "+ex);
                logger.severe("Image path: "+path);
                return null;
            }
        }
    }*/

    @org.jdesktop.application.Action
    public void displaySelectedImage() {
        try {
            if (thumbList.getModel().getSize() > 0 && currentIndex != thumbList.getSelectedIndex() && thumbList.getSelectedIndex() != -1) {
                lblMidImage.setText("Getting image to display...");
                if (lblMidImage.getIcon() != null) ((ImageIcon)lblMidImage.getIcon()).getImage().flush();
                lblMidImage.setIcon(null);
                new SwingWorker<Void,String>() {
                    public Void doInBackground() {
                        try {
                            progressBar.setIndeterminate(true);
                            lblMidImage.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                            
                            ImageListItem pli = (ImageListItem)thumbList.getSelectedValue();
                            
                            newImage = new ImageIcon(new URL(pli.getDisplay())).getImage();
                            fitImagesToWindow();
                            currentIndex = thumbList.getSelectedIndex();
                        } catch (Exception ex) {
                            YAYManView.logger.severe("Error processing movies: "+ex);
                        } catch (Throwable th) {
                            YAYManView.logger.severe("Error processing movies: "+th);
                        }
                        return null;
                    }

                    @Override
                    public void done() {
                        /*progressBar.setIndeterminate(false);
                        progressBar.setToolTipText(null);
                        lblMidImage.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));*/
                    }
                }.execute();
            } else {
                newImage = null;
            }
        } catch (Exception ex) {
            YAYManView.logger.severe("Error displaying image: "+ex);
        }
        currentIndex = thumbList.getSelectedIndex();
    }

    @org.jdesktop.application.Action
    public void useMovieDBImage() {
        try {
            lblImage.setIcon(null);
            ImageListItem pli = (ImageListItem)thumbList.getSelectedValue();
            if (currentMode.equals(PosterMode)) {
                updatePosterFromURL(pli.getOriginal());
            } else if (currentMode.equals(FanartMode)) {
                updateFanartFromURL(pli.getOriginal());
            } else if (currentMode.equals(BannerMode)) {
                updateBannerFromURL(pli.getOriginal());
            } else if (currentMode.equals(VideoImageMode)) {
                updateVideoImageFromURL(pli.getOriginal());
            }
            //ImageManager.this.dispose();
            formWindowClosing(null);
        } catch (Exception ex) {
            YAYManView.logger.severe("Error using MovieDB image: "+ex);
        }
    }

    @org.jdesktop.application.Action
    public void useInternetImage() {
        try {
            if (internetTxt.getText().length() > 0) {
                //URL imageURL = new URL(internetTxt.getText());
                if (new ImageIcon(new URL(internetTxt.getText())).getIconWidth() == -1) {
                    logger.severe("No image found at: "+internetTxt.getText());
                    return;
                }

                this.setVisible(false);
                if (currentMode.equals(PosterMode)) {
                    updatePosterFromURL(internetTxt.getText());
                } else if (currentMode.equals(FanartMode)) {
                    updateFanartFromURL(internetTxt.getText());
                } else if (currentMode.equals(BannerMode)) {
                    updateBannerFromURL(internetTxt.getText());
                } else if (currentMode.equals(VideoImageMode)) {
                    updateVideoImageFromURL(internetTxt.getText());
                }
                ImageManager.this.dispose();
                //formWindowClosing(null);
            }
        } catch (java.net.MalformedURLException ex) {
            logger.severe("Invalid URL for image: "+internetTxt.getText());
            internetTxt.setText("");
        } catch (Exception ex) {
            YAYManView.logger.severe("Error using internet image: "+ex);
        }
    }

    @org.jdesktop.application.Action
    public void chooseLocalImage() {
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            localTxt.setText(fileChooser.getSelectedFile().getPath());

            if (!localTxt.getText().isEmpty()) {
                if (otherImage != null) otherImage.flush();
                otherImage = new ImageIcon(localTxt.getText()).getImage();
                fitOtherToWindow();
            }
        }
    }

    @org.jdesktop.application.Action
    public void useLocalImage() {
        try {
            if (localTxt.getText().length() > 0) {
                if (currentMode.equals(PosterMode)) {
                    updatePosterFromFile(localTxt.getText());
                } else if (currentMode.equals(FanartMode)) {
                    updateFanartFromFile(localTxt.getText());
                } else if (currentMode.equals(BannerMode)) {
                    updateBannerFromFile(localTxt.getText());
                } else if (currentMode.equals(VideoImageMode)) {
                    updateVideoImageFromFile(localTxt.getText());
                }
                //ImageManager.this.dispose();
                //this.dispose();
                formWindowClosing(null);
            }
        } catch (Exception ex) {
            YAYManView.logger.severe("Error using local image: "+ex);
        }
    }

    private void loadMovieDbPosters(Vector dbImages) {
        try {
            logger.fine("Getting images from theMovieDB...");
            String API_KEY = PropertiesUtil.getProperty("API_KEY_TheMovieDB");
            String language = PropertiesUtil.getProperty("themoviedb.language", "en-US");

            TheMovieDbApi TMDb;
            MovieDb mdb = null;
            //java.util.List<Artwork> movieArt;

            TMDb = new TheMovieDbApi(API_KEY);
            TMDb.setProxy(WebBrowser.getMjbProxyHost(), WebBrowser.getMjbProxyPort(), WebBrowser.getMjbProxyUsername(), WebBrowser.getMjbProxyPassword());
            TMDb.setTimeout(WebBrowser.getMjbTimeoutConnect(), WebBrowser.getMjbTimeoutRead());

            String imdbId = currentMovie.getId(ImdbPlugin.IMDB_PLUGIN_ID);
            if (imdbId == null || imdbId.equals("") || imdbId.isEmpty() || imdbId.equals(Movie.UNKNOWN)) {
                imdbId = currentMovie.getId(TheMovieDbPlugin.TMDB_PLUGIN_ID);
                mdb = TMDb.getMovieInfo(Integer.parseInt(imdbId), language);
            } else {
                mdb = TMDb.getMovieInfoImdb(imdbId, language);
            }

            if (mdb == null) {
                YAYManView.logger.warning("No theMovieDB information found for "+imdbId);
                return;
            }
            
            //movieArt = TMDb.getMovieImages(mdb.getId(), language);
            
            //ArrayList<Artwork> artworks = getArtworkOfType(TMDb.getMovieImages(mdb.getId(), language));
            ArrayList<Artwork> artworks = getArtworkOfType(TMDb.getMovieImages(mdb.getId(), ""));
            
            if (artworks.isEmpty() || artworks.size() < 1) {
                YAYManView.logger.warning("No images found for movie.");
                return;
                /*artworks = getArtworkOfType(TMDb.getMovieImages(mdb.getId(),""));
                if (artworks.isEmpty()) {
                    YAYManView.logger.warning("Still no images found. Sorry.");
                    return;
                }*/
            }
            logger.fine("Found "+artworks.size()+" images.");
            
            for (Artwork artwork : artworks) {
                String orig = TMDb.createImageUrl(artwork.getFilePath(), "original").toString();
                String disp = null;
                String thumb = null;
                //check valid image sizes with http://api.themoviedb.org/3/configuration?api_key=API_KEY
                if (movieMode == ArtworkType.POSTER) {
                    //original, w500, w342, w185, w154, w92
                    disp = TMDb.createImageUrl(artwork.getFilePath(), "w500").toString();
                    thumb = TMDb.createImageUrl(artwork.getFilePath(), "w92").toString();
                } else {
                    //original, w1280, w780, w300
                    disp = TMDb.createImageUrl(artwork.getFilePath(), "w780").toString();
                    thumb = TMDb.createImageUrl(artwork.getFilePath(), "w300").toString();
                }
                dbImages.add(new ImageListItem(orig,disp,thumb,thumbList));
            }
            
        } catch (Exception ex) {
            logger.severe("Error initializing themoviedb: "+ex);
        }
    }

    private void loadTvDbPosters(Vector dbImages) {
        logger.fine("Getting images from theTvDB...");
        String TV_API_KEY = PropertiesUtil.getProperty("API_KEY_TheTVDb");
        String language = PropertiesUtil.getProperty("thetvdb.language", "en");
        TheTVDBApi tvDB = new TheTVDBApi(TV_API_KEY);
        String id = currentMovie.getId(TheTvDBPlugin.THETVDB_PLUGIN_ID);
        //String matchedMode = "season";
        String bannerType = "season";
        if (tvMode.equals(BannerMode)) bannerType = "seasonwide";
        
        try {

            boolean foundPoster = false;
            if (!Movie.UNKNOWN.equals(id)) {
                Banners banners = tvDB.getBanners(id);

                if (!banners.getSeasonList().isEmpty()) {
                    if (tvMode.equals(FanartMode)) {
                        for (Banner banner : banners.getFanartList()) {
                            dbImages.add(new ImageListItem(banner, thumbList));
                        }
                    } else if (tvMode.equals(VideoImageMode)) {
                        Episode ep = tvDB.getEpisode(id, currentMovie.getSeason(), mainProgram.getSelectedEpisodeNumber(), language);
                        String imageUrl = "http://www.thetvdb.com/banners/episodes/"+id+"/"+ep.getId()+".jpg";
                        dbImages.add(new ImageListItem(imageUrl, imageUrl, imageUrl, thumbList));
                        foundPoster = true;
                    } else if (tvMode.equals(PosterMode)) {
                        for (Banner banner : banners.getSeasonList()) {
                            if (banner.getSeason() == currentMovie.getSeason() || isSetImage) { // only check for the correct season
                                if (banner.getBannerType2().toString().equalsIgnoreCase(bannerType)) {
                                    dbImages.add(new ImageListItem(banner, thumbList));
                                    foundPoster = true;
                                }
                            }
                        }
                    } else {
                        for (Banner banner : banners.getSeasonList()) {
                            if (banner.getSeason() == currentMovie.getSeason()) { // only check for the correct season
                                if (banner.getBannerType2().toString().equalsIgnoreCase(bannerType)) {
                                    dbImages.add(new ImageListItem(banner, thumbList));
                                    foundPoster = true;
                                }
                            }
                        }

                        if (isSetImage) {
                            for (Banner banner : banners.getSeriesList()) {
                                if (banner.getBannerType2().toString().equalsIgnoreCase(bannerType) || banner.getBannerType2().toString().equalsIgnoreCase("Graphical") || banner.getBannerType2().toString().equalsIgnoreCase("Blank") || banner.getBannerType2().toString().equalsIgnoreCase("Text")) {
                                    dbImages.add(new ImageListItem(banner, thumbList));
                                    foundPoster = true;
                                }
                            }
                        }
                    }

                    if (!foundPoster && !banners.getPosterList().isEmpty()) {
                        for (Banner banner : banners.getPosterList()) {
                            if (banner.getBannerType2().toString().equalsIgnoreCase(bannerType)) {
                                dbImages.add(new ImageListItem(banner, thumbList));
                                foundPoster = true;
                            }
                        }
                    }
                    if (!foundPoster && !banners.getSeriesList().isEmpty() && tvMode.equals(BannerMode)) {
                        for (Banner banner : banners.getSeriesList()) {
                            //if (banner.getBannerType2().equalsIgnoreCase(bannerType)) {
                                dbImages.add(new ImageListItem(banner, thumbList));
                                foundPoster = true;
                            //}
                        }
                    }
                    if (!foundPoster) {
                        Series series = tvDB.getSeries(id, language);
                        if (series.getPoster() != null && !series.getPoster().isEmpty()) {
                            Banner banner = new Banner();
                            banner.setUrl(series.getPoster());
                            dbImages.add(new ImageListItem(banner, thumbList));
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.severe("Error loading TVDB posters: "+ex);
        }
    }

    public void updatePosterFromURL(final String posterURL) {
        new MySwingWorker(mainProgram.getProgressBar()) {
            @Override
            protected Void doInBackground() {
                showProcessing(true);
                mainProgram.enableControls(false);
                currentMovie = null;
                Movie movie = mainProgram.getSelectedMovie();
                String downloadPath = "";
                if (!isSetImage) {
                    logger.fine("Updating XML...");
                    mainProgram.updateSelectedMovieXml("posterURL", posterURL);
                    downloadPath = JukeboxInterface.getFullDetailsPath()+"/"+movie.getPosterFilename();
                } else {
                    String bigSetPosterName = "Set_"+mainProgram.getSelectedSet()+"_1"+".jpg";
                    downloadPath = JukeboxInterface.getFullDetailsPath()+"/"+bigSetPosterName;
                }

                try {
                    //movie.setDirtyPoster(true);
                    movie.setDirty(com.moviejukebox.model.DirtyFlag.POSTER, true);
                    logger.fine("Downloading new poster...");
                    FileTools.downloadImage(new File(downloadPath), posterURL);

                    if (!isSetImage) {
                        //JukeboxInterface.updateMoviePoster(movie);
                        logger.fine("Creating details poster...");
                        //MovieJukebox.createPoster(tools.imagePlugin, JukeboxInterface.getJukebox(jukeboxDetailsRoot, jukeboxDetailsRoot), skinHome, movie, true);
                        JukeboxInterface.createPoster(movie);
                        logger.fine("Creating thumbnail...");
                        JukeboxInterface.createThumbnail(movie);
                    } else {
                        logger.fine("Creating thumbnail...");
                        JukeboxInterface.createSetThumbnail(movie, mainProgram.getSelectedSet());
                    }
                    logger.fine("Poster and thumbnail creation complete");
                } catch (Exception ex) {
                    logger.severe("Error updating movie poster: "+ex);
                }
                mainProgram.refreshCurrentMovie();
                showProcessing(false);
                mainProgram.enableControls(true);
                return null;
            }
        }.execute();
    }

    public void updateFanartFromURL(final String fanartURL) {
        new MySwingWorker(mainProgram.getProgressBar()) {
            @Override
            protected Void doInBackground() {
                showProcessing(true);
                mainProgram.enableControls(false);
                String fanartToken = getProperty("mjb.scanner.fanartToken", ".fanart");
                String ext = fanartURL.substring(fanartURL.lastIndexOf("."));
                Movie movie = null;
                String downloadPath = "";
                if (!isSetImage) {
                    movie = mainProgram.getSelectedMovie(false);
                    logger.fine("Updating XML...");
                    currentMovie = null;
                    mainProgram.updateSelectedMovieXml("fanartURL",fanartURL);
                    mainProgram.updateSelectedMovieXml("fanartFile",movie.getBaseName() + fanartToken + ext);
                    movie = mainProgram.getSelectedMovie();
                } else {
                    downloadPath = JukeboxInterface.getFullDetailsPath()+"/"+setImageName;
                }

                try {
                    logger.fine("Downloading fanart...");
                    if (!isSetImage) {
                        JukeboxInterface.downloadFanart(movie);
                    } else {
                        FileTools.downloadImage(new File(downloadPath), fanartURL);
                    }

                    logger.fine("Fanart retrieved");
                } catch (Exception ex) {
                    logger.severe("Error updating fanart: "+ex);
                }
                if (!isSetImage) mainProgram.createHTML();
                mainProgram.refreshCurrentMovie();
                showProcessing(false);
                mainProgram.enableControls(true);
                return null;
            }
        }.execute();
    }

    public void updatePosterFromFile(final String localPosterFile) {
        new MySwingWorker(mainProgram.getProgressBar()) {
            @Override
            protected Void doInBackground() {
                showProcessing(true);
                mainProgram.enableControls(false);
                logger.fine("Copying poster...");
                Movie movie = mainProgram.getSelectedMovie();
                int extInd = localPosterFile.lastIndexOf(".");
                String ext = ".jpg";
                if (extInd != -1) {
                    ext = localPosterFile.substring(extInd);
                }
                final String jukeboxDetailsRoot = JukeboxInterface.getFullDetailsPath();
                String destFile = "";

                if (!isSetImage) {
                    movie.setPosterFilename(movie.getBaseName() + ext);
                    destFile = jukeboxDetailsRoot+File.separator+movie.getPosterFilename();
                } else {
                    destFile = jukeboxDetailsRoot+"/"+"Set_"+mainProgram.getSelectedSet()+"_1"+".jpg";
                }

                FileTools.copyFile(localPosterFile, destFile);

                if (!isSetImage) {
                    logger.fine("Updating XML...");
                    currentMovie = null;
                    mainProgram.updateSelectedMovieXml("posterFile", movie.getPosterFilename());

                    logger.fine("Creating details poster...");
                    JukeboxInterface.createPoster(movie);
                    logger.fine("Creating thumbnail...");
                    JukeboxInterface.createThumbnail(movie);
                } else {
                    logger.fine("Creating thumbnail...");
                    JukeboxInterface.createSetThumbnail(movie, mainProgram.getSelectedSet());
                }
                logger.fine("Poster and thumbnail creation complete");
                mainProgram.refreshCurrentMovie();
                showProcessing(false);
                mainProgram.enableControls(true);
                deleteTempFolder();
                return null;
            }
        }.execute();
    }

    public void updateFanartFromFile(final String fanartPath) {
        new MySwingWorker(mainProgram.getProgressBar()) {
            @Override
            protected Void doInBackground() {
                showProcessing(true);
                mainProgram.enableControls(false);
                logger.fine("Setting fanart...");
                Movie movie = null;
                String fanartToken = getProperty("mjb.scanner.fanartToken", ".fanart");
                File fullFanartFile = new File(fanartPath);
                String destFileName = "";
                movie = mainProgram.getSelectedMovie();
                if (!isSetImage) {
                    //movie.setFanartFilename(movie.getBaseName() + fanartToken + fanartPath.substring(fanartPath.lastIndexOf(".")));
                    String fanartFilename = FileTools.makeSafeFilename(movie.getFanartFilename());
                    destFileName = JukeboxInterface.getFullDetailsPath() + File.separator + fanartFilename;
                } else {
                    destFileName = setImageName;
                }

                DefaultBackgroundPlugin backgroundPlugin = new DefaultBackgroundPlugin();

                try {
                    BufferedImage fanartImage = GraphicTools.loadJPEGImage(fullFanartFile);
                    if (fanartImage != null) {
                        fanartImage = backgroundPlugin.generate(movie, fanartImage, "fanart", null);
                        if (Boolean.parseBoolean(PropertiesUtil.getProperty("fanart.perspective", "false"))) {
                            destFileName = destFileName.subSequence(0, destFileName.lastIndexOf(".") + 1) + "png";
                            //if (!isSetImage) movie.setFanartFilename(destFileName);
                        }
                        GraphicTools.saveImageToDisk(fanartImage, JukeboxInterface.getFullDetailsPath()+"/"+destFileName);
                        logger.fine("Fanart set");
                    } else {
                        System.out.println("fanartImage is null!");
                    }
                } catch (Exception ex) {
                    logger.severe("Error setting fanart from file: "+ex);
                }
                if (!isSetImage) {
                    mainProgram.updateSelectedMovieXml("fanartFile",movie.getFanartFilename());
                    mainProgram.updateSelectedMovieXml("fanartURL",Movie.UNKNOWN);
                    mainProgram.createHTML();
                }
                mainProgram.refreshCurrentMovie();
                showProcessing(false);
                mainProgram.enableControls(true);
                deleteTempFolder();
                return null;
            }
        }.execute();
    }

    public void updateBannerFromFile(final String bannerPath) {
        new MySwingWorker(mainProgram.getProgressBar()) {
            @Override
            protected Void doInBackground() {
                showProcessing(true);
                mainProgram.enableControls(false);
                logger.fine("Setting banner...");
                Movie movie = null;
                File fullBannerFile = new File(bannerPath);
                String bannerFilename = "";
                String destFileName = "";
                if (!isSetImage) {
                    movie = mainProgram.getSelectedMovie();
                    String bannerToken = getProperty("mjb.scanner.bannertToken", ".banner");
                    //movie.setFanartFilename(movie.getBaseName() + bannerToken + bannerPath.substring(bannerPath.lastIndexOf(".")));
                    bannerFilename = FileTools.makeSafeFilename(movie.getBannerFilename());
                    destFileName = JukeboxInterface.getFullDetailsPath() + File.separator + bannerFilename;
                } else {
                    destFileName = JukeboxInterface.getFullDetailsPath()+"/"+setImageName;
                }

                try {
                    BufferedImage bannerImage = GraphicTools.loadJPEGImage(fullBannerFile);
                    if (bannerImage != null) {
                        FileTools.copyFile(fullBannerFile, new File(destFileName));
                        logger.fine("Banner set");
                    }
                } catch (Exception ex) {
                    logger.severe("Error setting banner from file: "+ex);
                }

                if (!isSetImage) {
                    mainProgram.updateSelectedMovieXml("bannerFile",movie.getBannerFilename());
                    mainProgram.updateSelectedMovieXml("bannerURL",Movie.UNKNOWN);
                    mainProgram.createHTML();
                }
                mainProgram.refreshCurrentMovie();
                showProcessing(false);
                mainProgram.enableControls(true);
                deleteTempFolder();
                return null;
            }
        }.execute();
    }

    public void updateBannerFromURL(final String bannerURL) {
        new MySwingWorker(mainProgram.getProgressBar()) {
            @Override
            protected Void doInBackground() {
                showProcessing(true);
                mainProgram.enableControls(false);
                String bannerToken = getProperty("mjb.scanner.bannerToken", ".banner");
                String ext = bannerURL.substring(bannerURL.lastIndexOf("."));
                Movie movie =  null;
                String downloadPath = "";
                if (!isSetImage) {
                    movie = mainProgram.getSelectedMovie(false);
                    logger.fine("Updating movie XML...");
                    currentMovie = null;
                    mainProgram.updateSelectedMovieXml("bannerURL",bannerURL);
                    mainProgram.updateSelectedMovieXml("bannerFile",movie.getBaseName() + bannerToken + ext);
                    movie = mainProgram.getSelectedMovie();
                    downloadPath = JukeboxInterface.getFullDetailsPath()+"/"+movie.getBannerFilename();
                } else {
                    downloadPath = JukeboxInterface.getFullDetailsPath()+"/"+setImageName;
                }

                try {
                    logger.fine("Downloading banner..."+bannerURL+" -> "+downloadPath);
                    FileTools.downloadImage(new File(downloadPath), bannerURL);

                    logger.fine("Banner retrieved");
                } catch (Exception ex) {
                    logger.severe("Error updating banner: "+ex);
                }
                if (!isSetImage) mainProgram.createHTML();
                mainProgram.refreshCurrentMovie();
                showProcessing(false);
                mainProgram.enableControls(true);
                return null;
            }
        }.execute();
    }

    public void updateVideoImageFromFile(final String imagePath) {
        new MySwingWorker(mainProgram.getProgressBar()) {
            @Override
            protected Void doInBackground() {
                showProcessing(true);
                mainProgram.enableControls(false);
                logger.fine("Setting video image...");
                Movie movie = mainProgram.getSelectedMovie();
                String videoimageToken = getProperty("mjb.scanner.videoimageToken", ".videoimage");
                String ext = imagePath.substring(imagePath.lastIndexOf("."));
                //String part = "_" + mainProgram.getSelectedEpisodeNumber();
                String part = "";
                String imageFilename = FileTools.makeSafeFilename(movie.getBaseName() + videoimageToken + part + ext);
                String destFileName = JukeboxInterface.getFullDetailsPath() + File.separator + imageFilename;

                File vidImageFile = new File(imagePath);
                try {
                    BufferedImage vidImage = GraphicTools.loadJPEGImage(vidImageFile);
                    if (vidImage != null) {
                        FileTools.copyFile(vidImageFile, new File(destFileName));
                        logger.fine("Banner set");
                    }
                } catch (Exception ex) {
                    logger.severe("Error setting video image from file: "+ex);
                }

                mainProgram.updateSelectedMovieFileXml("fileImageURL",Movie.UNKNOWN);
                mainProgram.updateSelectedMovieFileXml("fileImageFile",imageFilename);
                mainProgram.createHTML();
                mainProgram.refreshCurrentMovie();
                showProcessing(false);
                mainProgram.enableControls(true);
                deleteTempFolder();
                return null;
            }
        }.execute();
    }

    public void updateVideoImageFromURL(final String imageURL) {
        new MySwingWorker(mainProgram.getProgressBar()) {
            @Override
            protected Void doInBackground() {
                showProcessing(true);
                mainProgram.enableControls(false);
                String videoimageToken = getProperty("mjb.scanner.videoimageToken", ".videoimage");
                String ext = imageURL.substring(imageURL.lastIndexOf("."));
                //String part = "_" + mainProgram.getSelectedEpisodeNumber();
                String part = "";
                Movie movie = mainProgram.getSelectedMovie(false);
                logger.fine("Updating movie XML...");
                currentMovie = null;
                mainProgram.updateSelectedMovieFileXml("fileImageURL",imageURL);
                mainProgram.updateSelectedMovieFileXml("fileImageFile",movie.getBaseName() + videoimageToken + part + ext);
                movie = mainProgram.getSelectedMovie();
                MovieFile movieFile = mainProgram.getSelectedMovieFile();

                try {
                    logger.fine("Downloading video image...");
                    FileTools.downloadImage(new File(JukeboxInterface.getFullDetailsPath()+"/"+movieFile.getVideoImageFilename(mainProgram.getSelectedEpisodeNumber())), imageURL);

                    logger.fine("Video image retrieved");
                } catch (Exception ex) {
                    logger.severe("Error updating video image: "+ex);
                }
                mainProgram.createHTML();
                mainProgram.refreshCurrentMovie();
                showProcessing(false);
                mainProgram.enableControls(true);
                return null;
            }
        }.execute();
    }

    public void fitImagesToWindow() {
        fitOriginalToWindow();
        fitNewToWindow();
        fitOtherToWindow();
        fitAttachToWindow();
    }

    public void fitOriginalToWindow() {
        ExecutorService threadPool = Executors.newFixedThreadPool(1);
        threadPool.submit(new MySwingWorker() {
            @Override
            protected Void doInBackground() {
                if (originalImage != null) {
                    Image img = originalImage.getImage();
                    int w = ImageManager.this.getWidth()-35;
                    int h = ImageManager.this.getHeight()-70;
                    int iw = img.getWidth(null);
                    int ih = img.getHeight(null);
                    double xScale = (double)w/iw;
                    double yScale = (double)h/ih;
                    double scale = Math.min(xScale, yScale);
                    int width = (int)(scale*iw);
                    int height = (int)(scale*ih);
                    ImageIcon scaledImage = new ImageIcon(img.getScaledInstance(width, height, Image.SCALE_FAST));
                    img.flush();
                    if (lblImage.getIcon() != null) ((ImageIcon)lblImage.getIcon()).getImage().flush();
                    lblImage.setIcon(scaledImage);
                }
                return null;
            }
        });
        threadPool.shutdown();
    }

    public void fitNewToWindow() {
        ExecutorService threadPool = Executors.newFixedThreadPool(1);
        threadPool.submit(new MySwingWorker(progressBar) {
            @Override
            protected Void doInBackground() {
                if (lblMidImage.getIcon() != null) ((ImageIcon)lblMidImage.getIcon()).getImage().flush();
                if (newImage != null) {
                    int w = ImageManager.this.getWidth()-155;
                    int h = ImageManager.this.getHeight()-75;
                    int iw = newImage.getWidth(null);
                    int ih = newImage.getHeight(null);
                    double xScale = (double)w/iw;
                    double yScale = (double)h/ih;
                    double scale = Math.min(xScale, yScale);
                    int width = (int)(scale*iw);
                    int height = (int)(scale*ih);
                    ImageIcon scaledImage = new ImageIcon(newImage.getScaledInstance(width, height, Image.SCALE_FAST));
                    lblMidImage.setIcon(scaledImage);
                    lblMidImage.setText("");
                } else {
                    lblMidImage.setIcon(null);
                    lblMidImage.setText("No image to display.");
                }
                progressBar.setIndeterminate(false);
                progressBar.setToolTipText(null);
                lblMidImage.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                return null;
            }
        });
        threadPool.shutdown();
    }
    
    public void fitAttachToWindow() {
        ExecutorService threadPool = Executors.newFixedThreadPool(1);
        threadPool.submit(new MySwingWorker() {
            @Override
            protected Void doInBackground() {
                if (lblAttachImage.getIcon() != null) ((ImageIcon)lblAttachImage.getIcon()).getImage().flush();
                if (attachImage != null) {
                    int w = ImageManager.this.getWidth()-155;
                    int h = ImageManager.this.getHeight()-75;
                    int iw = attachImage.getWidth(null);
                    int ih = attachImage.getHeight(null);
                    double xScale = (double)w/iw;
                    double yScale = (double)h/ih;
                    double scale = Math.min(xScale, yScale);
                    int width = (int)(scale*iw);
                    int height = (int)(scale*ih);
                    ImageIcon scaledImage = new ImageIcon(attachImage.getScaledInstance(width, height, Image.SCALE_FAST));
                    lblAttachImage.setIcon(scaledImage);
                    lblAttachImage.setText("");
                } else {
                    lblAttachImage.setIcon(null);
                    lblAttachImage.setText("No image to display.");
                }
                lblAttachImage.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                return null;
            }
        });
        threadPool.shutdown();
    }

    public void fitOtherToWindow() {
        ExecutorService threadPool = Executors.newFixedThreadPool(1);
        threadPool.submit(new MySwingWorker() {
            @Override
            protected Void doInBackground() {
                if (lblOtherImage.getIcon() != null) ((ImageIcon)lblOtherImage.getIcon()).getImage().flush();
                if (otherImage != null) {
                    //Image img = newImage;
                    int w = ImageManager.this.getWidth()-40;
                    int h = ImageManager.this.getHeight()-190;
                    int iw = otherImage.getWidth(null);
                    int ih = otherImage.getHeight(null);
                    double xScale = (double)w/iw;
                    double yScale = (double)h/ih;
                    double scale = Math.min(xScale, yScale);
                    int width = (int)(scale*iw);
                    int height = (int)(scale*ih);
                    ImageIcon scaledImage = new ImageIcon(otherImage.getScaledInstance(width, height, Image.SCALE_FAST));

                    lblOtherImage.setIcon(scaledImage);
                } else {
                    lblOtherImage.setIcon(null);
                }
            return null;
            }
        });
        threadPool.shutdown();
    }

    public void displayImageFromUrl() {
        if (!internetTxt.getText().endsWith(".png") && !internetTxt.getText().endsWith(".bmp") && !internetTxt.getText().endsWith(".jpg")) return;
        try {
            if (otherImage != null) otherImage.flush();
            URL url = new URL(internetTxt.getText());
            URLConnection conn = url.openConnection();
            if (conn.getContentLength() > -1 )otherImage = new ImageIcon(new URL(internetTxt.getText())).getImage();
        } catch (Exception ex) {
            logger.severe("Invalid URL to display image: "+internetTxt.getText());
            otherImage = null;
        }
        if (otherImage != null && otherImage.getWidth(null) == -1) otherImage = null;
        fitOtherToWindow();
    }
    
    private ArrayList<Artwork> getArtworkOfType(java.util.List<Artwork> movieArt) {
        String language = PropertiesUtil.getProperty("themoviedb.language", "en");
        ArrayList<Artwork> artworks = new ArrayList();
        for (Artwork artwork: movieArt) {
            if (artwork.getArtworkType() == movieMode && ((artwork.getLanguage() == null) || (artwork.getLanguage().compareTo(language) == 0))) {
                artworks.add(artwork);
            }
        }
        return artworks;
    }
    
    private void loadAttachmentImages() {
        Vector attachImageIcons = new Vector();
        File tmpDir = new File("mkvtoolnixtemp");
        if (tmpDir.isDirectory() && tmpDir.exists()) { 
            File[] files = tmpDir.listFiles();
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                f.delete();
            }
            tmpDir.delete();
        }
        Mkvtoolnix mtn = new Mkvtoolnix(getProperty("yayman.mkvtoolnixFolder"));
        ArrayList<Mkvtoolnix.Attachment> allAttach = new ArrayList();
        if (mtn.isValid()) {
            try {
                MovieFile[] files = currentMovie.getFiles().toArray(new MovieFile[0]);
                //System.out.println("MovieFiles: "+files.length);
                for (int i=0; i < files.length; i++) {
                    MovieFile mFile = files[i];
                    ArrayList<Mkvtoolnix.Attachment> attachments = mtn.getImageAttachments(mFile.getFile().getPath());
                    for (Mkvtoolnix.Attachment attach : attachments) {
                        //System.out.println("Created attachment: "+attach);
                        allAttach.add(attach);
                    }
                }
                //System.out.println("All Attachments: "+allAttach.size());
                ArrayList<File> attachImageFiles = mtn.getImageAttachmentFiles(allAttach, tmpDir.getName());
                //System.out.println("attachImageFiles: "+attachImageFiles.size());
                for (File f : attachImageFiles) {
                    attachImageIcons.add(new ImageListItem(f, attImageList));
                }
                //System.out.println("attachImageIcons: "+attachImageIcons.size());
            } catch (Exception ex) {
                logger.severe("Error reading attachments: "+ex);
            }
        }
        attImageList.setListData(attachImageIcons);
        if (!attachImageIcons.isEmpty()) {
            attImageList.setSelectedIndex(0);
            useAttachBtn.setEnabled(true);
        } else {
            attImageList.setEnabled(false);
            useAttachBtn.setEnabled(false);
        }
    }
    
    @org.jdesktop.application.Action
    public void displaySelectedAttachment() {
        try {
            if (attImageList.getModel().getSize() > 0 && attachIndex != attImageList.getSelectedIndex() && attImageList.getSelectedIndex() != -1) {
                lblAttachImage.setText("Getting image to display...");
                if (lblAttachImage.getIcon() != null) ((ImageIcon)lblAttachImage.getIcon()).getImage().flush();
                lblAttachImage.setIcon(null);
                new SwingWorker<Void,String>() {
                    public Void doInBackground() {
                        try {
                            lblAttachImage.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                            
                            ImageListItem pli = (ImageListItem)attImageList.getSelectedValue();
                            
                            attachImage = new ImageIcon(pli.getDisplay()).getImage();
                            fitImagesToWindow();
                            attachIndex = attImageList.getSelectedIndex();
                        } catch (Exception ex) {
                            YAYManView.logger.severe("Error processing movies: "+ex);
                        } catch (Throwable th) {
                            YAYManView.logger.severe("Error processing movies: "+th);
                        }
                        return null;
                    }

                    @Override
                    public void done() {
                        /*progressBar.setIndeterminate(false);
                        progressBar.setToolTipText(null);
                        lblMidImage.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));*/
                    }
                }.execute();
            } else {
                attachImage = null;
            }
        } catch (Exception ex) {
            YAYManView.logger.severe("Error displaying image: "+ex);
        }
        attachIndex = attImageList.getSelectedIndex();
    }

    @org.jdesktop.application.Action
    public void useAttachImage() {
        try {
            lblImage.setIcon(null);
            ImageListItem ili = (ImageListItem)attImageList.getSelectedValue();
            if (currentMode.equals(PosterMode)) {
                updatePosterFromFile(ili.getOriginal());
            } else if (currentMode.equals(FanartMode)) {
                updateFanartFromFile(ili.getOriginal());
            } else if (currentMode.equals(BannerMode)) {
                updateBannerFromFile(ili.getOriginal());
            } else if (currentMode.equals(VideoImageMode)) {
                updateVideoImageFromFile(ili.getOriginal());
            }
            formWindowClosing(null);
        } catch (Exception ex) {
            YAYManView.logger.severe("Error using attached image: "+ex);
        }
    }
    
    private void deleteTempFolder() {
        File tmpDir = new File("mkvtoolnixtemp");
        if (tmpDir.isDirectory() && tmpDir.exists()) { 
            File[] files = tmpDir.listFiles();
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                f.delete();
            }
            tmpDir.delete();
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JList attImageList;
    private javax.swing.JPanel attachmentsPanel;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JPanel imgPanel;
    private javax.swing.JTextField internetTxt;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lblAttachImage;
    private javax.swing.JLabel lblImage;
    private javax.swing.JLabel lblMidImage;
    private javax.swing.JLabel lblOtherImage;
    private javax.swing.JTextField localTxt;
    private javax.swing.JPanel moviedbPanel;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JList thumbList;
    private javax.swing.JButton useAttachBtn;
    private javax.swing.JButton useImageBtn;
    private javax.swing.JButton useInternetBtn;
    private javax.swing.JButton useLocalTxt;
    private javax.swing.JTabbedPane wizardTabs;
    // End of variables declaration//GEN-END:variables

}
