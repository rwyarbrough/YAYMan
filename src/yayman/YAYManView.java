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
import com.moviejukebox.tools.*;
import static java.lang.Boolean.parseBoolean;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.plugin.TheTvDBPlugin;
import com.moviejukebox.*;
import com.moviejukebox.scanner.*;

import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.text.JTextComponent;
import javax.swing.JProgressBar;
import javax.swing.ImageIcon;
import javax.swing.DefaultListModel;
import javax.swing.event.*;
import javax.swing.JCheckBoxMenuItem;
import java.util.concurrent.Callable;
import javax.swing.DefaultComboBoxModel;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.PopupMenu;
import java.awt.MenuItem;
import javax.swing.tree.*;
import java.awt.Image;
import java.awt.event.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JOptionPane;

import org.w3c.dom.*;
import java.io.*;
import java.util.*;
import java.net.*;

import java.net.URI;
import java.util.logging.*;

import java.lang.reflect.*;

import static yayman.MovieXmlTools.*;

/**
 * The application's main frame.
 */
public class YAYManView extends FrameView {
    private YAYManPrefs prefsWindow;
    private LogDiag logWindow;
    private MovieListModel vidList;
    private MovieListModel filteredList;
    private boolean vidXml = false;
    private Hashtable undoChanges;
    private ArrayList defaultFields;
    public static Logger logger = Logger.getLogger("yayman");
    private boolean isBusy;
    private HashMap pluginMap;
    final TrayIcon trayIcon;
    private FileHandler fh;
    private MySwingWorker processLibraryWorker;
    private int currentVideoIndex = -1;
    private ImageCycler imageCycler;
    private ImageCycler setCycler;
    //private ImageManager imageManager;
    private NMTManager nmtManager;
    private YAMJUpdateDiag yamjUpdate;
    private YAYManUpdateDiag yaymanUpdate;

    //private DefaultListModel defaultGenresModel;

    public YAYManView(SingleFrameApplication app) {
        super(app);

        initComponents();

        nmtManager = null;
        yamjUpdate = null;
        yaymanUpdate = null;

        ResourceMap resourceMap = getResourceMap();
        
        //Mac localizations
        if (System.getProperty("mrj.version") != null) {
            for (int i = 0; i < menuBar.getMenuCount(); i++) {
                javax.swing.JMenu menu = menuBar.getMenu(i);
                if (menu.getText().equals("File")) {
                    menuBar.remove(menu);
                    break;
                }
            }

           //Find the Help menu
            javax.swing.JMenu helpMenu = null;
            for (int i = 0; i < menuBar.getMenuCount(); i++) {
                javax.swing.JMenu menu = menuBar.getMenu(i);
                if (menu.getText().equals("Help")) {
                    helpMenu = menu;
                    break;
                }
            }

            //Remove the "About" menu item
            if (helpMenu != null) {
                for (int i = 0; i < helpMenu.getItemCount(); i++) {
                    javax.swing.JMenuItem menuItem = helpMenu.getItem(i);
                    if (menuItem.getName().equals("aboutMenuItem"))  {
                        helpMenu.remove(menuItem);
                        break;
                    }
                }
            }

            try {
                OSXAdapter.setAboutHandler(this, getClass().getDeclaredMethod("showAboutBox", (Class[])null));
                OSXAdapter.setQuitHandler(app, app.getClass().getDeclaredMethod("quit", (Class[])null));
                OSXAdapter.setDockIconImage(resourceMap.getImageIcon("Application.largeIcon").getImage());
            } catch (Exception ex) {
                logger.severe("Error setting About handler: "+ex);
            }
        }

        logWindow = new LogDiag(this);

        String logFilename = "yayman.log";
        //LogFormatter mjbFormatter = new LogFormatter();

        try {
            fh = new FileHandler(logFilename);
            fh.setFormatter(new java.util.logging.SimpleFormatter());
            fh.setLevel(Level.ALL);

            ConsoleHandler ch = new ConsoleHandler() {
                @Override
                public void publish(LogRecord record) {
                    super.publish(record);
                    if (isLoggable(record)) {
                        statusMessageLabel.setText(record.getMessage());
                        logWindow.addLogMessage(record.getMessage());
                    }
                }
            };
            ch.setFormatter(new java.util.logging.SimpleFormatter());
            ch.setLevel(Level.FINE);

            logger.setUseParentHandlers(false);
            logger.addHandler(fh);
            logger.addHandler(ch);
            logger.setLevel(Level.ALL);

            try {
                //if this croaks, then we probably need to download YAMJ
                org.apache.log4j.Logger tonmjlog = org.apache.log4j.Logger.getLogger("com.syabas.tonmjdb.ToNmjDb");
                org.apache.log4j.ConsoleAppender ca = new org.apache.log4j.ConsoleAppender(new com.moviejukebox.tools.FilteringLayout()) {
                    @Override
                    public void append(org.apache.log4j.spi.LoggingEvent event) {
                        logger.fine(event.getRenderedMessage());
                    }
                };
                //ca.setThreshold(org.apache.log4j.Priority.INFO);

                tonmjlog.addAppender(ca);
            } catch (Exception ex) {

            }

        } catch (Exception ex) {
            System.out.println("Error initializing logger: "+ex);
        }

        this.getFrame().addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exit();
            }
        });

        // status bar initialization - message timeout, idle icon and busy animation, etc
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                logger.fine("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String)(evt.getNewValue());
                    logger.fine((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer)(evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });

        prefsWindow = new YAYManPrefs(this, true);
        prefsWindow.setLocationRelativeTo(YAYManApp.getApplication().getMainFrame());

        JukeboxInterface.mediaInfoCheck(prefsWindow);

        javax.swing.ImageIcon frameIcon = resourceMap.getImageIcon("mainFrameIcon");
        this.getFrame().setIconImage(frameIcon.getImage());

        defaultFields = new ArrayList();
        defaultFields.add(title_Txt);
        defaultFields.add(titleSort_Txt);
        defaultFields.add(videoSource_Txt);
        defaultFields.add(runtime_Txt);
        defaultFields.add(subtitles_Txt);
        defaultFields.add(plot_Txt);
        defaultFields.add(outline_Txt);
        defaultFields.add(container_Txt);
        defaultFields.add(videoCodec_Txt);
        defaultFields.add(audioCodec_Txt);
        defaultFields.add(audioChannels_Txt);
        defaultFields.add(resolution_Txt);
        defaultFields.add(fps_Txt);
        //defaultFields.add(id_Txt);
        defaultFields.add(certification_Txt);
        defaultFields.add(year_Txt);
        defaultFields.add(top250_Txt);

        for (int i = 0; i < defaultFields.size(); i++) {
            JTextComponent comp = (JTextComponent)defaultFields.get(i);
            comp.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    makeEditable(evt);
                }
            });
            comp.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent evt) {
                    maybeSaveChanges(evt);
                }
            });
            comp.setEditable(false);
            comp.setText("");
        }

        id_Txt.setEditable(false);
        id_Txt.setText("");
        id_Txt.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                makeEditable(evt);
            }
        });
        id_Txt.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent evt) {
                    if (id_Txt.isEditable() && evt.getKeyCode() == 10 && !isBusy) {
                        updateMovieId(getSelectedMovie(false),cmbMovieID.getSelectedItem().toString(), id_Txt.getText());
                        id_Txt.setEditable(false);
                        id_Txt.getCaret().setVisible(false);
                    } else if (id_Txt.isEditable() && evt.getKeyCode() == 27) {
                        id_Txt.setEditable(false);
                        id_Txt.setText(undoChanges.get(id_Txt.getName()).toString());
                        id_Txt.getCaret().setVisible(false);
                    } else {
                        //System.out.println(evt.getKeyCode());
                    }
                }
            });

        txtFilter.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                applyVideoFilter();
            }
            public void removeUpdate(DocumentEvent e) {
                applyVideoFilter();
            }
            public void insertUpdate(DocumentEvent e) {
                applyVideoFilter();
            }
        });

        // System tray
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = resourceMap.getImageIcon("mainFrameIcon").getImage();

            MouseListener mouseListener = new MouseListener() {
                public void mouseClicked(MouseEvent e) {
                    //System.out.println("Tray Icon - Mouse clicked!");
                }

                public void mouseEntered(MouseEvent e) {
                    //System.out.println("Tray Icon - Mouse entered!");
                }

                public void mouseExited(MouseEvent e) {
                    //System.out.println("Tray Icon - Mouse exited!");
                }

                public void mousePressed(MouseEvent e) {
                    //System.out.println("Tray Icon - Mouse pressed!");
                }

                public void mouseReleased(MouseEvent e) {
                    //System.out.println("Tray Icon - Mouse released!");
                }
            };


            ActionListener exitListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    //System.out.println("Exiting...");
                    //System.exit(0);
                    exit();
                }
            };

            ActionListener minimizeListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JFrame frame = YAYManView.this.getFrame();
                    frame.setVisible(!frame.isVisible());

                    MenuItem menuItem = (MenuItem)e.getSource();
                    if (frame.isVisible()) {
                        frame.setExtendedState(JFrame.NORMAL);
                        frame.toFront();
                        menuItem.setLabel("Hide");
                    } else {
                        menuItem.setLabel("Restore");
                    }
                }
            };

            ActionListener processListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    YAYManView.this.processAllVideos();
                }
            };

            PopupMenu popup = new PopupMenu();
            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(exitListener);
            exitItem.setName("exitMenuItem");

            MenuItem minItem = new MenuItem("Hide");
            minItem.addActionListener(minimizeListener);
            minItem.setName("minMenuItem");

            MenuItem processItem = new MenuItem("Process library now");
            processItem.addActionListener(processListener);
            processItem.setName("processMenuItem");

            popup.add(processItem);
            popup.add(minItem);
            popup.add(exitItem);


            trayIcon = new TrayIcon(image, "YAYMan", popup);

            ActionListener actionListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    //trayIcon.displayMessage("Action Event", "An Action Event Has Been Performed!", TrayIcon.MessageType.INFO);
                    JFrame frame = YAYManView.this.getFrame();
                    frame.setVisible(!frame.isVisible());

                    MenuItem menuItem = null;
                    PopupMenu menu = ((TrayIcon)e.getSource()).getPopupMenu();
                    for (int i = 0; i < menu.getItemCount(); i ++) {
                        MenuItem mi = menu.getItem(i);
                        if (mi.getName().equals("minMenuItem")) {
                            menuItem = mi;
                            break;
                        }
                    }
                    if (frame.isVisible()) {
                        frame.setExtendedState(JFrame.NORMAL);
                        frame.toFront();
                        menuItem.setLabel("Hide");
                    } else {
                        menuItem.setLabel("Restore");
                    }
                }
            };

            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(actionListener);
            trayIcon.addMouseListener(mouseListener);

            try {
                tray.add(trayIcon);
                WindowListener windowListener = new WindowListener() {
                    public void windowActivated(WindowEvent e) {

                    }
                    public void windowClosed(WindowEvent e) {

                    }
                    public void windowClosing(WindowEvent e) {

                    }
                    public void windowDeactivated(WindowEvent e) {

                    }
                    public void windowDeiconified(WindowEvent e) {
                        YAYManView.this.getFrame().setVisible(true);
                    }
                    public void windowIconified(WindowEvent e) {
                        YAYManView.this.getFrame().setVisible(false);
                    }
                    public void windowOpened(WindowEvent e) {

                    }
                };
                //this.getFrame().addWindowListener(windowListener);
            } catch (Exception e) {
                System.err.println("TrayIcon could not be added.");
            }
        } else {
            trayIcon = null;
        }

        displayInit();
        isBusy = false;
        undoChanges = new Hashtable();
        logWindow.setIconImage(resourceMap.getImageIcon("logMenuItem.icon").getImage());
        cancelProcessingMenu.setVisible(false);
        movieTabbedPane.setEnabledAt(2, false);
        movieTabbedPane.setEnabledAt(3, false);
        logger.fine("Using YAMJ "+MovieJukebox.class.getPackage().getSpecificationVersion()+", r"+MovieJukebox.class.getPackage().getImplementationVersion());
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = YAYManApp.getApplication().getMainFrame();
            aboutBox = new YAYManAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        YAYManApp.getApplication().show(aboutBox);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        jSplitPane1 = new javax.swing.JSplitPane();
        leftPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        videoList = new javax.swing.JList();
        txtFilter = new javax.swing.JTextField();
        rightPanel = new javax.swing.JPanel();
        title_Txt = new javax.swing.JTextField();
        movieTabbedPane = new javax.swing.JTabbedPane();
        basicPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        titleSort_Txt = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        runtime_Txt = new javax.swing.JTextField();
        idLbl = new javax.swing.JLabel();
        id_Txt = new javax.swing.JTextField();
        thumbLbl = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        certification_Txt = new javax.swing.JTextField();
        cmbMovieID = new javax.swing.JComboBox();
        jLabel15 = new javax.swing.JLabel();
        year_Txt = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        top250_Txt = new javax.swing.JTextField();
        plotTabbedPane = new javax.swing.JTabbedPane();
        jPanel8 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        plot_Txt = new javax.swing.JTextArea();
        jPanel9 = new javax.swing.JPanel();
        jScrollPane7 = new javax.swing.JScrollPane();
        outline_Txt = new javax.swing.JTextArea();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        videoSource_Txt = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        subtitles_Txt = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        container_Txt = new javax.swing.JTextField();
        jPanel3 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        videoCodec_Txt = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        resolution_Txt = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        fps_Txt = new javax.swing.JTextField();
        jPanel4 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        audioCodec_Txt = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        audioChannels_Txt = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        btnRefreshMediaInfo = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        videoGenresList = new javax.swing.JList();
        btnGenreAdd = new javax.swing.JButton();
        btnGenreRemove = new javax.swing.JButton();
        jScrollPane5 = new javax.swing.JScrollPane();
        genresList = new javax.swing.JList();
        jLabel17 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        fileTree = new javax.swing.JTree();
        jPanel6 = new javax.swing.JPanel();
        setPosterLbl = new javax.swing.JLabel();
        jScrollPane6 = new javax.swing.JScrollPane();
        lstSets = new javax.swing.JList();
        jLabel18 = new javax.swing.JLabel();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        restartMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        prefsMenuItem = new javax.swing.JMenuItem();
        jukeboxMenu = new javax.swing.JMenu();
        yamjPropsMenuItem = new javax.swing.JMenuItem();
        skinPropsMenuItem = new javax.swing.JMenuItem();
        libraryFileMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        processMenuItem = new javax.swing.JMenuItem();
        viewMenu = new javax.swing.JMenu();
        refreshLibMenuItem = new javax.swing.JMenuItem();
        jukeboxDirMenuItem = new javax.swing.JMenuItem();
        yamjDirMenuItem = new javax.swing.JMenuItem();
        movieMenu = new javax.swing.JMenu();
        htmlView = new javax.swing.JMenuItem();
        libraryBrowse = new javax.swing.JMenuItem();
        toolsMenu = new javax.swing.JMenu();
        logMenuItem = new javax.swing.JCheckBoxMenuItem();
        nmtMenuItem = new javax.swing.JMenuItem();
        yamjUpMenuItem = new javax.swing.JMenuItem();
        newVersionMenu = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        supportMenu = new javax.swing.JMenuItem();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        cancelProcessingMenu = new javax.swing.JMenu();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        idMenu = new javax.swing.JPopupMenu();
        idVisit = new javax.swing.JMenuItem();
        idRegen = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        idAdd = new javax.swing.JMenuItem();
        idRemove = new javax.swing.JMenuItem();
        posterMenu = new javax.swing.JPopupMenu();
        posterEdit = new javax.swing.JMenuItem();
        fanartEdit = new javax.swing.JMenuItem();
        bannerEdit = new javax.swing.JMenuItem();
        filterMenu = new javax.swing.JPopupMenu();
        clearFilterMenuItem = new javax.swing.JMenuItem();
        fileTitleMenu = new javax.swing.JPopupMenu();
        fileTitleRenameMenu = new javax.swing.JMenuItem();
        vimageEdit = new javax.swing.JMenuItem();
        setMenu = new javax.swing.JPopupMenu();
        setEdit = new javax.swing.JMenuItem();
        setAdd = new javax.swing.JMenuItem();
        setRemove = new javax.swing.JMenuItem();
        setImageMenu = new javax.swing.JPopupMenu();
        setPosterEdit = new javax.swing.JMenuItem();
        setFanartEdit = new javax.swing.JMenuItem();
        setBannerEdit = new javax.swing.JMenuItem();
        plotMenu = new javax.swing.JPopupMenu();
        plotSyncMenu = new javax.swing.JMenuItem();

        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.addHierarchyBoundsListener(new java.awt.event.HierarchyBoundsListener() {
            public void ancestorMoved(java.awt.event.HierarchyEvent evt) {
            }
            public void ancestorResized(java.awt.event.HierarchyEvent evt) {
                mainPanelAncestorResized(evt);
            }
        });
        mainPanel.addAncestorListener(new javax.swing.event.AncestorListener() {
            public void ancestorMoved(javax.swing.event.AncestorEvent evt) {
                mainPanelAncestorMoved(evt);
            }
            public void ancestorAdded(javax.swing.event.AncestorEvent evt) {
            }
            public void ancestorRemoved(javax.swing.event.AncestorEvent evt) {
            }
        });

        jSplitPane1.setDividerLocation(150);
        jSplitPane1.setName("jSplitPane1"); // NOI18N

        leftPanel.setName("leftPanel"); // NOI18N

        jScrollPane1.setMinimumSize(new java.awt.Dimension(150, 300));
        jScrollPane1.setName("jScrollPane1"); // NOI18N
        jScrollPane1.setPreferredSize(new java.awt.Dimension(150, 130));

        videoList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Your movies are not", "loading properly.", "Check the settings to", "make sure your library", "xml file is properly set." };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        videoList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        videoList.setName("videoList"); // NOI18N
        videoList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                videoListValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(videoList);

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(yayman.YAYManApp.class).getContext().getResourceMap(YAYManView.class);
        txtFilter.setText(resourceMap.getString("txtFilter.text")); // NOI18N
        txtFilter.setToolTipText(resourceMap.getString("txtFilter.toolTipText")); // NOI18N
        txtFilter.setComponentPopupMenu(filterMenu);
        txtFilter.setName("txtFilter"); // NOI18N

        javax.swing.GroupLayout leftPanelLayout = new javax.swing.GroupLayout(leftPanel);
        leftPanel.setLayout(leftPanelLayout);
        leftPanelLayout.setHorizontalGroup(
            leftPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(txtFilter, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
        );
        leftPanelLayout.setVerticalGroup(
            leftPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(leftPanelLayout.createSequentialGroup()
                .addComponent(txtFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 353, Short.MAX_VALUE))
        );

        jSplitPane1.setLeftComponent(leftPanel);

        rightPanel.setName("rightPanel"); // NOI18N

        title_Txt.setEditable(false);
        title_Txt.setFont(resourceMap.getFont("title_Txt.font")); // NOI18N
        title_Txt.setText(resourceMap.getString("title_Txt.text")); // NOI18N
        title_Txt.setName("title_Txt"); // NOI18N

        movieTabbedPane.setName("movieTabbedPane"); // NOI18N
        movieTabbedPane.setPreferredSize(new java.awt.Dimension(481, 500));

        basicPanel.setName("basicPanel"); // NOI18N

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setToolTipText(resourceMap.getString("jLabel1.toolTipText")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        titleSort_Txt.setEditable(false);
        titleSort_Txt.setName("titleSort_Txt"); // NOI18N

        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N

        runtime_Txt.setEditable(false);
        runtime_Txt.setText(resourceMap.getString("runtime_Txt.text")); // NOI18N
        runtime_Txt.setName("runtime_Txt"); // NOI18N

        idLbl.setText(resourceMap.getString("idLbl.text")); // NOI18N
        idLbl.setComponentPopupMenu(idMenu);
        idLbl.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        idLbl.setName("idLbl"); // NOI18N
        idLbl.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                idLblMouseClicked(evt);
            }
        });

        id_Txt.setEditable(false);
        id_Txt.setText(resourceMap.getString("id_Txt.text")); // NOI18N
        id_Txt.setComponentPopupMenu(idMenu);
        id_Txt.setName("id_Txt"); // NOI18N

        thumbLbl.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        thumbLbl.setText(resourceMap.getString("thumbLbl.text")); // NOI18N
        thumbLbl.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        thumbLbl.setComponentPopupMenu(posterMenu);
        thumbLbl.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        thumbLbl.setMaximumSize(new java.awt.Dimension(180, 273));
        thumbLbl.setMinimumSize(new java.awt.Dimension(180, 273));
        thumbLbl.setName("thumbLbl"); // NOI18N
        thumbLbl.setPreferredSize(new java.awt.Dimension(180, 273));
        thumbLbl.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        thumbLbl.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                thumbLblMouseWheelMoved(evt);
            }
        });
        thumbLbl.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                thumbLblMouseClicked(evt);
            }
        });

        jLabel14.setText(resourceMap.getString("jLabel14.text")); // NOI18N
        jLabel14.setName("jLabel14"); // NOI18N

        certification_Txt.setEditable(false);
        certification_Txt.setText(resourceMap.getString("certification_Txt.text")); // NOI18N
        certification_Txt.setName("certification_Txt"); // NOI18N

        cmbMovieID.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "imdb" }));
        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(yayman.YAYManApp.class).getContext().getActionMap(YAYManView.class, this);
        cmbMovieID.setAction(actionMap.get("displayMovieID")); // NOI18N
        cmbMovieID.setComponentPopupMenu(idMenu);
        cmbMovieID.setName("cmbMovieID"); // NOI18N

        jLabel15.setText(resourceMap.getString("jLabel15.text")); // NOI18N
        jLabel15.setName("jLabel15"); // NOI18N

        year_Txt.setEditable(false);
        year_Txt.setText(resourceMap.getString("year_Txt.text")); // NOI18N
        year_Txt.setName("year_Txt"); // NOI18N

        jLabel16.setText(resourceMap.getString("jLabel16.text")); // NOI18N
        jLabel16.setName("jLabel16"); // NOI18N

        top250_Txt.setEditable(false);
        top250_Txt.setText(resourceMap.getString("top250_Txt.text")); // NOI18N
        top250_Txt.setName("top250_Txt"); // NOI18N

        plotTabbedPane.setTabPlacement(javax.swing.JTabbedPane.LEFT);
        plotTabbedPane.setComponentPopupMenu(plotMenu);
        plotTabbedPane.setName("plotTabbedPane"); // NOI18N

        jPanel8.setName("jPanel8"); // NOI18N

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        plot_Txt.setColumns(20);
        plot_Txt.setEditable(false);
        plot_Txt.setFont(resourceMap.getFont("plot_Txt.font")); // NOI18N
        plot_Txt.setLineWrap(true);
        plot_Txt.setRows(5);
        plot_Txt.setWrapStyleWord(true);
        plot_Txt.setName("plot_Txt"); // NOI18N
        jScrollPane2.setViewportView(plot_Txt);

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 328, Short.MAX_VALUE)
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 104, Short.MAX_VALUE)
        );

        plotTabbedPane.addTab(resourceMap.getString("jPanel8.TabConstraints.tabTitle"), jPanel8); // NOI18N

        jPanel9.setName("jPanel9"); // NOI18N

        jScrollPane7.setName("jScrollPane7"); // NOI18N

        outline_Txt.setColumns(20);
        outline_Txt.setFont(resourceMap.getFont("outline_Txt.font")); // NOI18N
        outline_Txt.setLineWrap(true);
        outline_Txt.setRows(5);
        outline_Txt.setWrapStyleWord(true);
        outline_Txt.setName("outline_Txt"); // NOI18N
        jScrollPane7.setViewportView(outline_Txt);

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 328, Short.MAX_VALUE)
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 104, Short.MAX_VALUE)
        );

        plotTabbedPane.addTab(resourceMap.getString("jPanel9.TabConstraints.tabTitle"), jPanel9); // NOI18N

        javax.swing.GroupLayout basicPanelLayout = new javax.swing.GroupLayout(basicPanel);
        basicPanel.setLayout(basicPanelLayout);
        basicPanelLayout.setHorizontalGroup(
            basicPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(basicPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(basicPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(basicPanelLayout.createSequentialGroup()
                        .addGroup(basicPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(idLbl)
                            .addComponent(jLabel3)
                            .addComponent(jLabel15))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(basicPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(basicPanelLayout.createSequentialGroup()
                                .addComponent(cmbMovieID, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(id_Txt, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(titleSort_Txt, javax.swing.GroupLayout.DEFAULT_SIZE, 326, Short.MAX_VALUE)
                            .addGroup(basicPanelLayout.createSequentialGroup()
                                .addGroup(basicPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(runtime_Txt, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(year_Txt, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(basicPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel14)
                                    .addComponent(jLabel16))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(basicPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(top250_Txt)
                                    .addComponent(certification_Txt, javax.swing.GroupLayout.DEFAULT_SIZE, 64, Short.MAX_VALUE)))))
                    .addComponent(plotTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 383, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(thumbLbl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        basicPanelLayout.setVerticalGroup(
            basicPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(basicPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(basicPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(thumbLbl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(basicPanelLayout.createSequentialGroup()
                        .addGroup(basicPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(titleSort_Txt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(plotTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(basicPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(idLbl)
                            .addComponent(cmbMovieID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(id_Txt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(basicPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(certification_Txt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel14)
                            .addComponent(runtime_Txt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(basicPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel15)
                            .addComponent(year_Txt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel16)
                            .addComponent(top250_Txt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(408, 408, 408))
        );

        movieTabbedPane.addTab(resourceMap.getString("basicPanel.TabConstraints.tabTitle"), basicPanel); // NOI18N

        jPanel2.setName("jPanel2"); // NOI18N

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        videoSource_Txt.setEditable(false);
        videoSource_Txt.setText(resourceMap.getString("videoSource_Txt.text")); // NOI18N
        videoSource_Txt.setName("videoSource_Txt"); // NOI18N

        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N

        subtitles_Txt.setEditable(false);
        subtitles_Txt.setText(resourceMap.getString("subtitles_Txt.text")); // NOI18N
        subtitles_Txt.setName("subtitles_Txt"); // NOI18N

        jLabel5.setText(resourceMap.getString("jLabel5.text")); // NOI18N
        jLabel5.setName("jLabel5"); // NOI18N

        container_Txt.setEditable(false);
        container_Txt.setText(resourceMap.getString("container_Txt.text")); // NOI18N
        container_Txt.setName("container_Txt"); // NOI18N

        jPanel3.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanel3.setName("jPanel3"); // NOI18N

        jLabel6.setText(resourceMap.getString("jLabel6.text")); // NOI18N
        jLabel6.setName("jLabel6"); // NOI18N

        videoCodec_Txt.setEditable(false);
        videoCodec_Txt.setText(resourceMap.getString("videoCodec_Txt.text")); // NOI18N
        videoCodec_Txt.setName("videoCodec_Txt"); // NOI18N

        jLabel9.setText(resourceMap.getString("jLabel9.text")); // NOI18N
        jLabel9.setName("jLabel9"); // NOI18N

        resolution_Txt.setEditable(false);
        resolution_Txt.setText(resourceMap.getString("resolution_Txt.text")); // NOI18N
        resolution_Txt.setName("resolution_Txt"); // NOI18N

        jLabel10.setText(resourceMap.getString("jLabel10.text")); // NOI18N
        jLabel10.setName("jLabel10"); // NOI18N

        fps_Txt.setEditable(false);
        fps_Txt.setText(resourceMap.getString("fps_Txt.text")); // NOI18N
        fps_Txt.setName("fps_Txt"); // NOI18N

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(videoCodec_Txt, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(resolution_Txt, javax.swing.GroupLayout.DEFAULT_SIZE, 105, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(fps_Txt, javax.swing.GroupLayout.DEFAULT_SIZE, 79, Short.MAX_VALUE)
                        .addGap(44, 44, 44))))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(videoCodec_Txt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(resolution_Txt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(fps_Txt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanel4.setName("jPanel4"); // NOI18N

        jLabel7.setText(resourceMap.getString("jLabel7.text")); // NOI18N
        jLabel7.setName("jLabel7"); // NOI18N

        audioCodec_Txt.setEditable(false);
        audioCodec_Txt.setText(resourceMap.getString("audioCodec_Txt.text")); // NOI18N
        audioCodec_Txt.setName("audioCodec_Txt"); // NOI18N

        jLabel8.setText(resourceMap.getString("jLabel8.text")); // NOI18N
        jLabel8.setName("jLabel8"); // NOI18N

        audioChannels_Txt.setEditable(false);
        audioChannels_Txt.setText(resourceMap.getString("audioChannels_Txt.text")); // NOI18N
        audioChannels_Txt.setName("audioChannels_Txt"); // NOI18N

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(audioCodec_Txt, javax.swing.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(audioChannels_Txt, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(audioCodec_Txt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(audioChannels_Txt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel12.setText(resourceMap.getString("jLabel12.text")); // NOI18N
        jLabel12.setName("jLabel12"); // NOI18N

        jLabel13.setText(resourceMap.getString("jLabel13.text")); // NOI18N
        jLabel13.setName("jLabel13"); // NOI18N

        btnRefreshMediaInfo.setAction(actionMap.get("refreshMediaInfo")); // NOI18N
        btnRefreshMediaInfo.setIcon(resourceMap.getIcon("btnRefreshMediaInfo.icon")); // NOI18N
        btnRefreshMediaInfo.setText(resourceMap.getString("btnRefreshMediaInfo.text")); // NOI18N
        btnRefreshMediaInfo.setEnabled(false);
        btnRefreshMediaInfo.setName("btnRefreshMediaInfo"); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(videoSource_Txt, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(container_Txt, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(subtitles_Txt, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel13))
                        .addGap(23, 23, 23)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel12)))
                    .addComponent(btnRefreshMediaInfo))
                .addContainerGap(171, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(videoSource_Txt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(container_Txt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(subtitles_Txt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(11, 11, 11)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(jLabel12))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnRefreshMediaInfo)
                .addGap(58, 58, 58))
        );

        movieTabbedPane.addTab(resourceMap.getString("jPanel2.TabConstraints.tabTitle"), jPanel2); // NOI18N

        jPanel1.setName("jPanel1"); // NOI18N

        jScrollPane4.setName("jScrollPane4"); // NOI18N

        videoGenresList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "If you", "can read", "this, then", "the genres", "did not load", "properly." };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        videoGenresList.setName("videoGenresList"); // NOI18N
        jScrollPane4.setViewportView(videoGenresList);

        btnGenreAdd.setIcon(resourceMap.getIcon("btnGenreAdd.icon")); // NOI18N
        btnGenreAdd.setText(resourceMap.getString("btnGenreAdd.text")); // NOI18N
        btnGenreAdd.setName("btnGenreAdd"); // NOI18N

        btnGenreRemove.setIcon(resourceMap.getIcon("btnGenreRemove.icon")); // NOI18N
        btnGenreRemove.setText(resourceMap.getString("btnGenreRemove.text")); // NOI18N
        btnGenreRemove.setName("btnGenreRemove"); // NOI18N

        jScrollPane5.setName("jScrollPane5"); // NOI18N

        genresList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "If you", "can read", "this, then", "the genres", "did not load", "properly." };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        genresList.setName("genresList"); // NOI18N
        jScrollPane5.setViewportView(genresList);

        jLabel17.setText(resourceMap.getString("jLabel17.text")); // NOI18N
        jLabel17.setName("jLabel17"); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel17)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnGenreAdd)
                            .addComponent(btnGenreRemove))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(273, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel17)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 251, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(btnGenreAdd)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnGenreRemove))
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 251, Short.MAX_VALUE))
                .addContainerGap())
        );

        movieTabbedPane.addTab(resourceMap.getString("jPanel1.TabConstraints.tabTitle"), jPanel1); // NOI18N

        jPanel5.setName("jPanel5"); // NOI18N

        jScrollPane3.setName("jScrollPane3"); // NOI18N

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("Files");
        fileTree.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        fileTree.setComponentPopupMenu(fileTitleMenu);
        fileTree.setEnabled(false);
        fileTree.setName("fileTree"); // NOI18N
        fileTree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                fileTreeValueChanged(evt);
            }
        });
        jScrollPane3.setViewportView(fileTree);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 567, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 271, Short.MAX_VALUE)
                .addContainerGap())
        );

        movieTabbedPane.addTab(resourceMap.getString("jPanel5.TabConstraints.tabTitle"), jPanel5); // NOI18N

        jPanel6.setName("jPanel6"); // NOI18N

        setPosterLbl.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        setPosterLbl.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        setPosterLbl.setComponentPopupMenu(setImageMenu);
        setPosterLbl.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        setPosterLbl.setMaximumSize(new java.awt.Dimension(180, 273));
        setPosterLbl.setMinimumSize(new java.awt.Dimension(180, 273));
        setPosterLbl.setName("setPosterLbl"); // NOI18N
        setPosterLbl.setPreferredSize(new java.awt.Dimension(180, 273));
        setPosterLbl.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        setPosterLbl.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                setPosterLblMouseWheelMoved(evt);
            }
        });
        setPosterLbl.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                setPosterLblMouseClicked(evt);
            }
        });

        jScrollPane6.setName("jScrollPane6"); // NOI18N

        lstSets.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        lstSets.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lstSets.setName("lstSets"); // NOI18N
        lstSets.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                lstSetsValueChanged(evt);
            }
        });
        jScrollPane6.setViewportView(lstSets);

        jLabel18.setText(resourceMap.getString("jLabel18.text")); // NOI18N
        jLabel18.setName("jLabel18"); // NOI18N

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel18)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 142, Short.MAX_VALUE)
                .addComponent(setPosterLbl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel18)
                    .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jScrollPane6, javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(setPosterLbl, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        movieTabbedPane.addTab(resourceMap.getString("jPanel6.TabConstraints.tabTitle"), jPanel6); // NOI18N

        javax.swing.GroupLayout rightPanelLayout = new javax.swing.GroupLayout(rightPanel);
        rightPanel.setLayout(rightPanelLayout);
        rightPanelLayout.setHorizontalGroup(
            rightPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(rightPanelLayout.createSequentialGroup()
                .addGroup(rightPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(title_Txt, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 592, Short.MAX_VALUE)
                    .addComponent(movieTabbedPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 592, Short.MAX_VALUE))
                .addGap(10, 10, 10))
        );
        rightPanelLayout.setVerticalGroup(
            rightPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(rightPanelLayout.createSequentialGroup()
                .addComponent(title_Txt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(movieTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 321, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(29, Short.MAX_VALUE))
        );

        movieTabbedPane.getAccessibleContext().setAccessibleName(resourceMap.getString("jTabbedPane1.AccessibleContext.accessibleName")); // NOI18N

        jSplitPane1.setRightComponent(rightPanel);

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 663, Short.MAX_VALUE)
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 381, Short.MAX_VALUE)
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        restartMenuItem.setAction(actionMap.get("forceRestart")); // NOI18N
        restartMenuItem.setIcon(resourceMap.getIcon("restartMenuItem.icon")); // NOI18N
        restartMenuItem.setText(resourceMap.getString("restartMenuItem.text")); // NOI18N
        restartMenuItem.setName("restartMenuItem"); // NOI18N
        fileMenu.add(restartMenuItem);

        exitMenuItem.setAction(actionMap.get("exit")); // NOI18N
        exitMenuItem.setIcon(resourceMap.getIcon("exitMenuItem.icon")); // NOI18N
        exitMenuItem.setText(resourceMap.getString("exitMenuItem.text")); // NOI18N
        exitMenuItem.setToolTipText(resourceMap.getString("exitMenuItem.toolTipText")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        editMenu.setText(resourceMap.getString("editMenu.text")); // NOI18N
        editMenu.setName("editMenu"); // NOI18N

        prefsMenuItem.setAction(actionMap.get("showPrefs")); // NOI18N
        prefsMenuItem.setIcon(resourceMap.getIcon("prefsMenuItem.icon")); // NOI18N
        prefsMenuItem.setText(resourceMap.getString("prefsMenuItem.text")); // NOI18N
        prefsMenuItem.setName("prefsMenuItem"); // NOI18N
        editMenu.add(prefsMenuItem);

        jukeboxMenu.setIcon(resourceMap.getIcon("jukeboxMenu.icon")); // NOI18N
        jukeboxMenu.setText(resourceMap.getString("jukeboxMenu.text")); // NOI18N
        jukeboxMenu.setName("jukeboxMenu"); // NOI18N
        editMenu.add(jukeboxMenu);

        yamjPropsMenuItem.setAction(actionMap.get("showYAMJProps")); // NOI18N
        yamjPropsMenuItem.setIcon(resourceMap.getIcon("yamjPropsMenuItem.icon")); // NOI18N
        yamjPropsMenuItem.setText(resourceMap.getString("yamjPropsMenuItem.text")); // NOI18N
        yamjPropsMenuItem.setName("yamjPropsMenuItem"); // NOI18N
        editMenu.add(yamjPropsMenuItem);

        skinPropsMenuItem.setAction(actionMap.get("showSkinProps")); // NOI18N
        skinPropsMenuItem.setIcon(resourceMap.getIcon("skinPropsMenuItem.icon")); // NOI18N
        skinPropsMenuItem.setText(resourceMap.getString("skinPropsMenuItem.text")); // NOI18N
        skinPropsMenuItem.setName("skinPropsMenuItem"); // NOI18N
        editMenu.add(skinPropsMenuItem);

        libraryFileMenuItem.setAction(actionMap.get("showLibraryEdit")); // NOI18N
        libraryFileMenuItem.setIcon(resourceMap.getIcon("libraryFileMenuItem.icon")); // NOI18N
        libraryFileMenuItem.setText(resourceMap.getString("libraryFileMenuItem.text")); // NOI18N
        libraryFileMenuItem.setName("libraryFileMenuItem"); // NOI18N
        editMenu.add(libraryFileMenuItem);

        jSeparator1.setName("jSeparator1"); // NOI18N
        editMenu.add(jSeparator1);

        processMenuItem.setAction(actionMap.get("processAllVideos")); // NOI18N
        processMenuItem.setIcon(resourceMap.getIcon("processMenuItem.icon")); // NOI18N
        processMenuItem.setText(resourceMap.getString("processMenuItem.text")); // NOI18N
        processMenuItem.setToolTipText(resourceMap.getString("processMenuItem.toolTipText")); // NOI18N
        processMenuItem.setName("processMenuItem"); // NOI18N
        editMenu.add(processMenuItem);

        menuBar.add(editMenu);

        viewMenu.setText(resourceMap.getString("viewMenu.text")); // NOI18N
        viewMenu.setName("viewMenu"); // NOI18N

        refreshLibMenuItem.setAction(actionMap.get("refreshLibrary")); // NOI18N
        refreshLibMenuItem.setIcon(resourceMap.getIcon("refreshLibMenuItem.icon")); // NOI18N
        refreshLibMenuItem.setText(resourceMap.getString("refreshLibMenuItem.text")); // NOI18N
        refreshLibMenuItem.setName("refreshLibMenuItem"); // NOI18N
        viewMenu.add(refreshLibMenuItem);

        jukeboxDirMenuItem.setAction(actionMap.get("openJukeboxDir")); // NOI18N
        jukeboxDirMenuItem.setIcon(resourceMap.getIcon("jukeboxDirMenuItem.icon")); // NOI18N
        jukeboxDirMenuItem.setText(resourceMap.getString("jukeboxDirMenuItem.text")); // NOI18N
        jukeboxDirMenuItem.setName("jukeboxDirMenuItem"); // NOI18N
        viewMenu.add(jukeboxDirMenuItem);

        yamjDirMenuItem.setAction(actionMap.get("openYAMJDir")); // NOI18N
        yamjDirMenuItem.setIcon(resourceMap.getIcon("yamjDirMenuItem.icon")); // NOI18N
        yamjDirMenuItem.setText(resourceMap.getString("yamjDirMenuItem.text")); // NOI18N
        yamjDirMenuItem.setName("yamjDirMenuItem"); // NOI18N
        viewMenu.add(yamjDirMenuItem);

        movieMenu.setIcon(resourceMap.getIcon("movieMenu.icon")); // NOI18N
        movieMenu.setText(resourceMap.getString("movieMenu.text")); // NOI18N
        movieMenu.setEnabled(false);
        movieMenu.setName("movieMenu"); // NOI18N

        htmlView.setAction(actionMap.get("openHTMLDetailsPage")); // NOI18N
        htmlView.setIcon(resourceMap.getIcon("htmlView.icon")); // NOI18N
        htmlView.setText(resourceMap.getString("htmlView.text")); // NOI18N
        htmlView.setName("htmlView"); // NOI18N
        movieMenu.add(htmlView);

        libraryBrowse.setAction(actionMap.get("browseLibrary")); // NOI18N
        libraryBrowse.setIcon(resourceMap.getIcon("libraryBrowse.icon")); // NOI18N
        libraryBrowse.setText(resourceMap.getString("libraryBrowse.text")); // NOI18N
        libraryBrowse.setToolTipText(resourceMap.getString("libraryBrowse.toolTipText")); // NOI18N
        libraryBrowse.setActionCommand(resourceMap.getString("libraryBrowse.actionCommand")); // NOI18N
        libraryBrowse.setName("libraryBrowse"); // NOI18N
        movieMenu.add(libraryBrowse);

        viewMenu.add(movieMenu);

        menuBar.add(viewMenu);

        toolsMenu.setText(resourceMap.getString("toolsMenu.text")); // NOI18N
        toolsMenu.setName("toolsMenu"); // NOI18N

        logMenuItem.setAction(actionMap.get("logMenuToggle")); // NOI18N
        logMenuItem.setText(resourceMap.getString("logMenuItem.text")); // NOI18N
        logMenuItem.setToolTipText(resourceMap.getString("logMenuItem.toolTipText")); // NOI18N
        logMenuItem.setIcon(resourceMap.getIcon("logMenuItem.icon")); // NOI18N
        logMenuItem.setName("logMenuItem"); // NOI18N
        toolsMenu.add(logMenuItem);

        nmtMenuItem.setAction(actionMap.get("showNmts")); // NOI18N
        nmtMenuItem.setIcon(resourceMap.getIcon("NMTs....icon")); // NOI18N
        nmtMenuItem.setText(resourceMap.getString("NMTs....text")); // NOI18N
        nmtMenuItem.setName("NMTs..."); // NOI18N
        toolsMenu.add(nmtMenuItem);

        yamjUpMenuItem.setAction(actionMap.get("updateYAMJ")); // NOI18N
        yamjUpMenuItem.setIcon(resourceMap.getIcon("yamjUpMenuItem.icon")); // NOI18N
        yamjUpMenuItem.setText(resourceMap.getString("yamjUpMenuItem.text")); // NOI18N
        yamjUpMenuItem.setName("yamjUpMenuItem"); // NOI18N
        toolsMenu.add(yamjUpMenuItem);

        newVersionMenu.setAction(actionMap.get("updateYAYMan")); // NOI18N
        newVersionMenu.setIcon(resourceMap.getIcon("newVersionMenu.icon")); // NOI18N
        newVersionMenu.setText(resourceMap.getString("newVersionMenu.text")); // NOI18N
        newVersionMenu.setName("newVersionMenu"); // NOI18N
        toolsMenu.add(newVersionMenu);

        menuBar.add(toolsMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        supportMenu.setAction(actionMap.get("openSupportForum")); // NOI18N
        supportMenu.setIcon(resourceMap.getIcon("supportMenu.icon")); // NOI18N
        supportMenu.setText(resourceMap.getString("supportMenu.text")); // NOI18N
        supportMenu.setName("supportMenu"); // NOI18N
        helpMenu.add(supportMenu);

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setIcon(resourceMap.getIcon("aboutMenuItem.icon")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        cancelProcessingMenu.setAction(actionMap.get("cancelJukeboxProcessing")); // NOI18N
        cancelProcessingMenu.setIcon(resourceMap.getIcon("cancelProcessingMenu.icon")); // NOI18N
        cancelProcessingMenu.setText(resourceMap.getString("cancelProcessingMenu.text")); // NOI18N
        cancelProcessingMenu.setName("cancelProcessingMenu"); // NOI18N
        menuBar.add(cancelProcessingMenu);

        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 663, Short.MAX_VALUE)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 493, Short.MAX_VALUE)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusAnimationLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusMessageLabel)
                    .addComponent(statusAnimationLabel)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(3, 3, 3))
        );

        idMenu.setName("idMenu"); // NOI18N

        idVisit.setAction(actionMap.get("visitMoviePage")); // NOI18N
        idVisit.setIcon(resourceMap.getIcon("idVisit.icon")); // NOI18N
        idVisit.setText(resourceMap.getString("idVisit.text")); // NOI18N
        idVisit.setToolTipText(resourceMap.getString("idVisit.toolTipText")); // NOI18N
        idVisit.setName("idVisit"); // NOI18N
        idMenu.add(idVisit);

        idRegen.setAction(actionMap.get("regenMovieData")); // NOI18N
        idRegen.setIcon(resourceMap.getIcon("idRegen.icon")); // NOI18N
        idRegen.setText(resourceMap.getString("idRegen.text")); // NOI18N
        idRegen.setToolTipText(resourceMap.getString("idRegen.toolTipText")); // NOI18N
        idRegen.setName("idRegen"); // NOI18N
        idMenu.add(idRegen);

        jSeparator2.setName("jSeparator2"); // NOI18N
        idMenu.add(jSeparator2);

        idAdd.setAction(actionMap.get("showAddMovieIdDiag")); // NOI18N
        idAdd.setIcon(resourceMap.getIcon("idAdd.icon")); // NOI18N
        idAdd.setText(resourceMap.getString("idAdd.text")); // NOI18N
        idAdd.setName("idAdd"); // NOI18N
        idMenu.add(idAdd);

        idRemove.setAction(actionMap.get("removeId")); // NOI18N
        idRemove.setIcon(resourceMap.getIcon("idRemove.icon")); // NOI18N
        idRemove.setText(resourceMap.getString("idRemove.text")); // NOI18N
        idRemove.setName("idRemove"); // NOI18N
        idMenu.add(idRemove);

        posterMenu.setName("posterMenu"); // NOI18N

        posterEdit.setAction(actionMap.get("showSrcImg")); // NOI18N
        posterEdit.setIcon(resourceMap.getIcon("imageManager.icon")); // NOI18N
        posterEdit.setText(resourceMap.getString("posterEdit.text")); // NOI18N
        posterEdit.setName("posterEdit"); // NOI18N
        posterMenu.add(posterEdit);

        fanartEdit.setAction(actionMap.get("showFanart")); // NOI18N
        fanartEdit.setIcon(resourceMap.getIcon("fanartEdit.icon")); // NOI18N
        fanartEdit.setText(resourceMap.getString("fanartEdit.text")); // NOI18N
        fanartEdit.setName("fanartEdit"); // NOI18N
        posterMenu.add(fanartEdit);

        bannerEdit.setAction(actionMap.get("showBanner")); // NOI18N
        bannerEdit.setIcon(resourceMap.getIcon("bannerEdit.icon")); // NOI18N
        bannerEdit.setText(resourceMap.getString("bannerEdit.text")); // NOI18N
        bannerEdit.setName("bannerEdit"); // NOI18N
        posterMenu.add(bannerEdit);

        filterMenu.setName("filterMenu"); // NOI18N

        clearFilterMenuItem.setAction(actionMap.get("clearFilter")); // NOI18N
        clearFilterMenuItem.setIcon(resourceMap.getIcon("clearFilterMenuItem.icon")); // NOI18N
        clearFilterMenuItem.setText(resourceMap.getString("clearFilterMenuItem.text")); // NOI18N
        clearFilterMenuItem.setName("clearFilterMenuItem"); // NOI18N
        filterMenu.add(clearFilterMenuItem);

        fileTitleMenu.setName("fileTitleMenu"); // NOI18N

        fileTitleRenameMenu.setAction(actionMap.get("fileTitleRename")); // NOI18N
        fileTitleRenameMenu.setIcon(resourceMap.getIcon("edit.icon")); // NOI18N
        fileTitleRenameMenu.setText(resourceMap.getString("fileTitleRenameMenu.text")); // NOI18N
        fileTitleRenameMenu.setName("fileTitleRenameMenu"); // NOI18N
        fileTitleMenu.add(fileTitleRenameMenu);

        vimageEdit.setAction(actionMap.get("showVideoImage")); // NOI18N
        vimageEdit.setIcon(resourceMap.getIcon("vimageEdit.icon")); // NOI18N
        vimageEdit.setText(resourceMap.getString("vimageEdit.text")); // NOI18N
        vimageEdit.setName("vimageEdit"); // NOI18N
        fileTitleMenu.add(vimageEdit);

        setMenu.setName("setMenu"); // NOI18N

        setEdit.setAction(actionMap.get("editSet")); // NOI18N
        setEdit.setIcon(resourceMap.getIcon("setEdit.icon")); // NOI18N
        setEdit.setText(resourceMap.getString("setEdit.text")); // NOI18N
        setEdit.setName("setEdit"); // NOI18N
        setMenu.add(setEdit);

        setAdd.setIcon(resourceMap.getIcon("setAdd.icon")); // NOI18N
        setAdd.setText(resourceMap.getString("setAdd.text")); // NOI18N
        setAdd.setName("setAdd"); // NOI18N
        setMenu.add(setAdd);

        setRemove.setIcon(resourceMap.getIcon("setRemove.icon")); // NOI18N
        setRemove.setText(resourceMap.getString("setRemove.text")); // NOI18N
        setRemove.setEnabled(false);
        setRemove.setName("setRemove"); // NOI18N
        setMenu.add(setRemove);

        setImageMenu.setName("setImageMenu"); // NOI18N

        setPosterEdit.setAction(actionMap.get("editSetPoster")); // NOI18N
        setPosterEdit.setIcon(resourceMap.getIcon("setPosterEdit.icon")); // NOI18N
        setPosterEdit.setText(resourceMap.getString("setPosterEdit.text")); // NOI18N
        setPosterEdit.setName("setPosterEdit"); // NOI18N
        setImageMenu.add(setPosterEdit);

        setFanartEdit.setAction(actionMap.get("editSetFanart")); // NOI18N
        setFanartEdit.setIcon(resourceMap.getIcon("setFanartEdit.icon")); // NOI18N
        setFanartEdit.setText(resourceMap.getString("setFanartEdit.text")); // NOI18N
        setFanartEdit.setName("setFanartEdit"); // NOI18N
        setImageMenu.add(setFanartEdit);

        setBannerEdit.setAction(actionMap.get("editSetBanner")); // NOI18N
        setBannerEdit.setIcon(resourceMap.getIcon("setBannerEdit.icon")); // NOI18N
        setBannerEdit.setText(resourceMap.getString("setBannerEdit.text")); // NOI18N
        setBannerEdit.setName("setBannerEdit"); // NOI18N
        setImageMenu.add(setBannerEdit);

        plotMenu.setName("plotMenu"); // NOI18N

        plotSyncMenu.setAction(actionMap.get("syncPlotOutline")); // NOI18N
        plotSyncMenu.setIcon(resourceMap.getIcon("plotSyncMenu.icon")); // NOI18N
        plotSyncMenu.setText(resourceMap.getString("plotSyncMenu.text")); // NOI18N
        plotSyncMenu.setToolTipText(resourceMap.getString("plotSyncMenu.toolTipText")); // NOI18N
        plotSyncMenu.setName("plotSyncMenu"); // NOI18N
        plotMenu.add(plotSyncMenu);

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents



    public void displayInit() {
        if (getProperty("yayman.libraryFile") == null) return;
        try {
            vidList = new MovieListModel();
            Collection<MediaLibraryPath> movieLibraryPaths;
            movieLibraryPaths = JukeboxInterface.parseMovieLibraryRootFile(new File(prefsWindow.getSelectedJukebox().getLibraryFile()));

            int threadsMaxDirScan = movieLibraryPaths.size();
            if (threadsMaxDirScan < 1)
                threadsMaxDirScan = 1;

            ThreadExecutor<Void> tasks = JukeboxInterface.getThreadExecutor();
            final Library library = new Library();
            for (final MediaLibraryPath mediaLibraryPath : movieLibraryPaths) {
                // Multi-thread parallel processing
                tasks.submit(new Callable<Void>() {
                    public Void call() {
                        MovieDirectoryScanner mds = new MovieDirectoryScanner();
                        // scan uses synchronized method Library.addMovie
                        mds.scan(mediaLibraryPath, library);
                        return null;
                    };
                });
            }
            tasks.waitFor();

            if (library.size() > 0) {
                vidList.setLibrary(library);
            }

            Map genres = library.getIndexes();
            Object[] keys = genres.keySet().toArray();
            for (int i = 0; i < keys.length; i++) {
                System.out.println(keys[i]+", "+genres.get(keys[i]));
            }


        } catch (Exception ex) {
            logger.severe("Error reading from library file: "+ex);
        } catch (Throwable ex) {
            logger.severe("Error reading from library file: "+ex);
        }
        applyVideoFilter();
        vimageEdit.setVisible(false);

        pluginMap = JukeboxInterface.getSearchPluginsMap();
    }

    private File getSelectedXmlFile() {
        return MovieXmlTools.getXmlFile(getSelectedMovie(false));
    }

    public void refreshCurrentMovie() {
        currentVideoIndex = -1;
        videoListValueChanged(null);
    }

    private void videoListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_videoListValueChanged
        if (videoList.getSelectedIndex() == currentVideoIndex) return;
        currentVideoIndex = videoList.getSelectedIndex();
        new MySwingWorker(progressBar) {
            @Override
            public Void doInBackground() {
                showProcessing(true);
                posterEdit.setEnabled(false);
                fanartEdit.setEnabled(false);
                movieMenu.setEnabled(!videoList.isSelectionEmpty());
                if (videoList.isSelectionEmpty()) {
                    libraryBrowse.setEnabled(false);
                    htmlView.setEnabled(false);
                    videoList.setToolTipText(null);
                    return null;
                }
                libraryBrowse.setEnabled(true);
                htmlView.setEnabled(true);

                File xmlFile = getSelectedXmlFile();
                for (int i = 0; i < defaultFields.size(); i++) {
                    JTextComponent comp = (JTextComponent)defaultFields.get(i);
                    comp.setEditable(false);
                }

                //cmbMovieID.setModel(moviePluginModel);
                bannerEdit.setVisible(false);

                if (imageCycler != null) imageCycler.dispose();
                imageCycler = new ImageCycler(thumbLbl);
                if (thumbLbl.getIcon() != null) ((ImageIcon)thumbLbl.getIcon()).getImage().flush();

                if (xmlFile.exists()) {
                    vidXml = true;
                    posterEdit.setEnabled(true);
                    fanartEdit.setEnabled(true);
                    loadVideoXml();
                    displayMovieID();
                } else {
                    logger.warning("XML file not found: "+xmlFile.getPath());
                    vidXml = false;
                    for (int i = 0; i < defaultFields.size(); i++) {
                        JTextComponent comp = (JTextComponent)defaultFields.get(i);
                        comp.setText("");
                    }
                    id_Txt.setText("");
                    thumbLbl.setText("No thumbnail found.");
                    thumbLbl.setIcon(null);
                }
                videoList.setToolTipText(getSelectedMovie(false).getBaseFilename());
                statusMessageLabel.setText(getSelectedMovie(false).getFile().getAbsolutePath());
                return null;
            }

            @Override
            protected void done() {
                showProcessing(false);
            }
        }.execute();
    }//GEN-LAST:event_videoListValueChanged

    private void thumbLblMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_thumbLblMouseClicked
        if (evt.getButton() == 2) {
            /*popupFrame = new ImagePopupFrame(imageCycler.getUnscaledImage(),thumbLbl);
            popupFrame.setVisible(true);*/
            new ImagePopupFrame(imageCycler.getUnscaledImage(), thumbLbl).setVisible(true);
        } else {
            thumbLbl.getComponentPopupMenu().show(thumbLbl, evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_thumbLblMouseClicked

    private void idLblMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_idLblMouseClicked
        idLbl.getComponentPopupMenu().show(idLbl, evt.getX(), evt.getY());
    }//GEN-LAST:event_idLblMouseClicked

    private void fileTreeValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_fileTreeValueChanged
        fileTitleRenameMenu.setEnabled(false);
        fileTree.setEditable(false);
        vimageEdit.setVisible(false);
        if (fileTree.getSelectionCount() > 0 && fileTree.getSelectionPath() != null) {
            Object[] path = fileTree.getSelectionPath().getPath();
            if (path.length > 1) {
                fileTitleRenameMenu.setEnabled(true);
                fileTree.setEditable(true);
                if (path.length > 2 && parseBoolean(getProperty("mjb.includeVideoImages", "false"))) {
                //if (path.length > 2) {
                    vimageEdit.setVisible(true);
                }
            }
        }
    }//GEN-LAST:event_fileTreeValueChanged

    private void thumbLblMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_thumbLblMouseWheelMoved
        if (evt.isControlDown() || evt.isShiftDown() || evt.isAltDown()) {
            java.awt.event.MouseEvent event = new java.awt.event.MouseEvent(thumbLbl, evt.getID(), evt.getWhen(), evt.getModifiers(), evt.getX(), evt.getY(), 1, false, evt.BUTTON2);
            thumbLblMouseClicked(event);
        } else {
            if (imageCycler.size() > 0) {
                ImageIcon image;
                if (evt.getWheelRotation() > 0)  {
                    image = imageCycler.nextImage();
                } else {
                    image = imageCycler.previousImage();
                }
                thumbLbl.setIcon(image);
                thumbLbl.setToolTipText(imageCycler.getImageType());
            }
        }
    }//GEN-LAST:event_thumbLblMouseWheelMoved

    private void mainPanelAncestorMoved(javax.swing.event.AncestorEvent evt) {//GEN-FIRST:event_mainPanelAncestorMoved
        if (logWindow != null) {
            logWindow.setLocation(this.getFrame().getLocation().x+this.getFrame().getWidth(), this.getFrame().getLocation().y);
            logWindow.setSize(logWindow.getWidth(), this.getFrame().getHeight());
        }
    }//GEN-LAST:event_mainPanelAncestorMoved

    private void mainPanelAncestorResized(java.awt.event.HierarchyEvent evt) {//GEN-FIRST:event_mainPanelAncestorResized
        if (logWindow != null) {
            logWindow.setLocation(this.getFrame().getLocation().x+this.getFrame().getWidth(), this.getFrame().getLocation().y);
            logWindow.setSize(logWindow.getWidth(), this.getFrame().getHeight());
        }
    }//GEN-LAST:event_mainPanelAncestorResized

    private void setPosterLblMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_setPosterLblMouseWheelMoved
        if (evt.isControlDown() || evt.isShiftDown() || evt.isAltDown()) {
            java.awt.event.MouseEvent event = new java.awt.event.MouseEvent(setPosterLbl, evt.getID(), evt.getWhen(), evt.getModifiers(), evt.getX(), evt.getY(), 1, false, evt.BUTTON2);
            setPosterLblMouseClicked(event);
        } else {
            if (setCycler.size() > 0) {
                ImageIcon image;
                if (evt.getWheelRotation() > 0)  {
                    image = setCycler.nextImage();
                } else {
                    image = setCycler.previousImage();
                }
                setPosterLbl.setIcon(image);
                setPosterLbl.setToolTipText(setCycler.getImageType());
            }
        }
    }//GEN-LAST:event_setPosterLblMouseWheelMoved

    private void setPosterLblMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_setPosterLblMouseClicked
        if (evt.getButton() == 2) {
            new ImagePopupFrame(setCycler.getUnscaledImage(), setPosterLbl).setVisible(true);
        } else {
            setPosterLbl.getComponentPopupMenu().show(setPosterLbl, evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_setPosterLblMouseClicked

    private void lstSetsValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lstSetsValueChanged
        setPosterEdit.setEnabled(false);
        setFanartEdit.setEnabled(false);
        setBannerEdit.setEnabled(false);
        if (lstSets.getSelectedIndex() == -1) {
            setPosterLbl.setIcon(null);
            return;
        }

        setPosterEdit.setEnabled(true);
        setFanartEdit.setEnabled(true);
        setBannerEdit.setEnabled(true);
        if (setCycler != null) setCycler.dispose();
        setCycler = new ImageCycler(setPosterLbl);
        String setName = lstSets.getSelectedValue().toString();
        String setPoster = "Set_"+setName+"_1"+getProperty("mjb.scanner.thumbnailToken","_small")+".png";
        String setBanner = "Set_"+setName+"_1"+getProperty("mjb.scanner.bannerToken",".banner")+".jpg";
        String setFanart = "Set_"+setName+"_1"+getProperty("mjb.scanner.fanartToken",".fanart")+".jpg";
        if (new File(JukeboxInterface.getFullDetailsPath()+"/"+setPoster).exists()) {
            setCycler.add(new ImageIcon(JukeboxInterface.getFullDetailsPath()+"/"+setPoster), "Set Poster");
        }

        if (new File(JukeboxInterface.getFullDetailsPath()+"/"+setFanart).exists()) {
            setCycler.add(new ImageIcon(JukeboxInterface.getFullDetailsPath()+"/"+setFanart), "Set Fanart");
        }

        if (new File(JukeboxInterface.getFullDetailsPath()+"/"+setBanner).exists()) {
            setCycler.add(new ImageIcon(JukeboxInterface.getFullDetailsPath()+"/"+setBanner), "Set Banner");
        }

        if (setCycler.size() > 0) {
            setPosterLbl.setIcon(setCycler.get(0));
            setPosterLbl.setText(null);
        } else {
            logger.warning("No images found for "+setName);
            setPosterLbl.setText("No image found.");
            setPosterLbl.setIcon(null);
        }
    }//GEN-LAST:event_lstSetsValueChanged

    public void loadVideoXml() {
        MovieXmlTools.makeCurrentXml(getSelectedMovie(false));
        Movie movie = getSelectedMovie();
        try {
            movieTabbedPane.setEnabledAt(3, false);
            if (movie == null) {
                logger.severe("Error: Selected movie returned null.");
                return;
            }
            if (movie.isTVShow()) {
                //cmbMovieID.setModel(tvPluginModel);
                //bannerEdit.setVisible(parseBoolean(getProperty("mjb.includeWideBanners","false")));
                bannerEdit.setVisible(true);
                movieTabbedPane.setEnabledAt(3, true);
            } else if (movieTabbedPane.getSelectedIndex() == 3) {
                movieTabbedPane.setSelectedIndex(0);
            }

            for (int i = 0; i < defaultFields.size(); i++) {
                JTextComponent comp = (JTextComponent)defaultFields.get(i);
                comp.setText(getSelectedVideoXmlValue(comp.getName().replaceAll("_Txt", "")));
            }

            DefaultComboBoxModel model = new DefaultComboBoxModel();
            ArrayList ids = MovieXmlTools.getMovieIDs(movie);
            if (ids != null) {
                for (int i = 0; i < ids.size(); i ++) {
                    if (pluginMap.get(ids.get(i)) != null) model.addElement(pluginMap.get(ids.get(i)));
                }
            }
            cmbMovieID.setModel(model);

            displayMovieID();

            loadFileTitleXml();

            loadVideoGenres();

            loadVideoSets();

        } catch (Exception ex) {
            logger.severe("Error loading movie data: "+ex);
        }

        //images code
        try {
            String dfPath = getFullDetailsPath();
            String thumbFile = dfPath+File.separator+URLDecoder.decode(getSelectedVideoXmlValue("posterFile"),"UTF-8");
            if (new File(thumbFile).exists()) {
                ImageIcon thumb = new ImageIcon(thumbFile);
                //thumb.getImage().flush();
                imageCycler.add(thumb,"Poster");
                //thumbLbl.setIcon(thumb);
            } 

            String fanartFile = dfPath+File.separator+URLDecoder.decode(getSelectedVideoXmlValue("fanartFile"),"UTF-8");
            if (new File(fanartFile).exists() && !fanartFile.equals(Movie.UNKNOWN)) {
                imageCycler.add(new ImageIcon(fanartFile),"Fanart");
            }

            String bannerFile = dfPath+File.separator+URLDecoder.decode(getSelectedVideoXmlValue("bannerFile"),"UTF-8");
            if (new File(bannerFile).exists() && !bannerFile.equals(Movie.UNKNOWN)) {
                imageCycler.add(new ImageIcon(bannerFile),"Banner");
            }

            if (imageCycler.size() > 0) {
                thumbLbl.setIcon(imageCycler.get(0));
                thumbLbl.setText(null);
            } else {
                logger.warning("No images found for "+movie.getBaseFilename());
                thumbLbl.setText("No thumbnail found.");
                thumbLbl.setIcon(null);
            }
        } catch (Exception ex) {
            logger.severe("Error loading poster thumbnail: "+ex);
        }
        
        movie = null;
    }

    private void loadFileTitleXml() {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Files");
        NodeList files = ((Element)getSelectedVideoXmlNode("files")).getElementsByTagName("file");
        for (int i = 0; i < files.getLength(); i++) {
            Element ele = (Element)files.item(i);
            XmlTreeNode fileNode = new XmlTreeNode(ele,"title") {
                @Override
                public void setUserObject(Object obj) {
                    super.setUserObject(obj);
                    if (obj.getClass().getName().equals(String.class.getName())) {
                        saveXmlDocument(getXmlFile(getSelectedMovie()),(Node)getUserObject());
                        if (this.getChildCount() == 1) {
                            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode)this.children().nextElement();
                            ((Element)childNode.getUserObject()).setTextContent(obj.toString());
                            saveXmlDocument(getXmlFile(getSelectedMovie()),(Node)childNode.getUserObject());
                        }
                        //MovieXmlTools.updateOtherFileTitleXml((Node)getUserObject(), (String)obj, this.getChildCount() == 1);
                        updateMovieXmlIndices(getSelectedMovie());
                        createHTML();
                    }
                }
            };
            NodeList fileTitles = ele.getElementsByTagName("fileTitle");
            NodeList filePlots = ele.getElementsByTagName("filePlot");
            for (int j = 0; j < fileTitles.getLength(); j ++) {
                XmlTreeNode fileTitleNode = new XmlTreeNode(fileTitles.item(j)) {
                    @Override
                    public void setUserObject(Object obj) {
                        super.setUserObject(obj);
                        if (obj.getClass().getName().equals(String.class.getName())) {
                            saveXmlDocument(getXmlFile(getSelectedMovie()),(Node)getUserObject());
                            updateMovieXmlIndices(getSelectedMovie());
                            createHTML();
                        }
                    }
                };
                if (filePlots.getLength() > 0) {
                    XmlTreeNode filePlotNode = new XmlTreeNode(filePlots.item(j)) {
                        @Override
                        public void setUserObject(Object obj) {
                            super.setUserObject(obj);
                            if (obj.getClass().getName().equals(String.class.getName())) {
                                saveXmlDocument(getXmlFile(getSelectedMovie()),(Node)getUserObject());
                                updateMovieXmlIndices(getSelectedMovie());
                                createHTML();
                            }
                        }
                    };
                    fileTitleNode.add(filePlotNode);
                }
                fileNode.add(fileTitleNode);
            }
            rootNode.add(fileNode);
        }
        fileTree.setModel(new DefaultTreeModel(rootNode));

        fileTree.setEnabled(true);
    }

    private void loadVideoGenres() {

    }

    private void loadVideoSets() {
        DefaultListModel sets = new DefaultListModel();
        setPosterLbl.setIcon(null);
        NodeList nl = ((Element)getSelectedVideoXmlNode("indexes")).getElementsByTagName("index");
        for (int i = 0; i < nl.getLength(); i++) {
            Element index = (Element)nl.item(i);
            if (index.getAttribute("type").equalsIgnoreCase("Set")) {
                sets.addElement(nl.item(i).getTextContent());
            }
        }
        lstSets.setModel(sets);

        boolean enableSetsTab = false;

        if (sets.size() > 0) {
            lstSets.setSelectedIndex(0);
            enableSetsTab = true;
        } else {
            lstSetsValueChanged(null);
        }
        
        int tabIndex = movieTabbedPane.getSelectedIndex();
        int setsIndex = 4;
        for (int i = 0; i < movieTabbedPane.getTabCount(); i++) {
            if (movieTabbedPane.getTitleAt(i).equalsIgnoreCase("Sets")) {
                setsIndex = i;
                break;
            }
        }

        movieTabbedPane.setEnabledAt(setsIndex, enableSetsTab);
        if (!enableSetsTab && tabIndex == setsIndex) movieTabbedPane.setSelectedIndex(0);
    }
    
    @Action
    public void showPrefs() {
        prefsWindow.setVisible(true);
    }

    public void updateSelectedMovieXml(String attribute, String newValue) {
        MovieXmlTools.updateMovieXml(getSelectedMovie(false), attribute, newValue);
    }

    public String getSelectedVideoXmlValue(String attribute) {
        return getSelectedVideoXmlNode(attribute).getTextContent();
    }

    private Node getSelectedVideoXmlNode(String attribute) {
        return MovieXmlTools.getVideoXmlNode(getSelectedMovie(false), attribute);
    }

    public void updateSelectedMovieFileXml(String tagName, String newValue) {
        MovieXmlTools.updateMovieFileXml(getSelectedMovie(false), getSelectedEpisodeNumber(), tagName, newValue);
    }

    public void applyVideoFilter() {
        int vidSelection = videoList.getSelectedIndex();
        int startNumber = videoList.getModel().getSize();
        if (txtFilter.getText().length() == 0) {
            videoList.setModel(vidList);
            clearFilterMenuItem.setEnabled(false);
        } else {
            //MovieListModel oldFilter = filteredList;
            filteredList = new MovieListModel();
            filteredList.setBackgroundLibrary(vidList.getLibrary());
            for (int i = 0; i < vidList.getSize(); i++) {
                if (vidList.get(i).toString().toLowerCase().contains(txtFilter.getText().toLowerCase())) {
                    filteredList.addElement(vidList.get(i));
                }
            }
            if (!filteredList.isEmpty()) videoList.setModel(filteredList);//filteredList = oldFilter;
            clearFilterMenuItem.setEnabled(true);
        }

        if (vidSelection != -1 && startNumber == videoList.getModel().getSize()) {
            videoList.setSelectedIndex(vidSelection);
        } else if (videoList.getModel().getSize() > 0) videoList.setSelectedIndex(0);
    }

    @Action
    public void regenMovieData() {
        new MySwingWorker(progressBar) {
            @Override
            public Void doInBackground() {
                showProcessing(true);
                enableControls(false);
                if (!id_Txt.getText().isEmpty()) {
                    JukeboxInterface.RegenerateMovieData(getSelectedMovie(false), (MovieSearchPluginItem)cmbMovieID.getSelectedItem(), id_Txt.getText());
                } else {
                    logger.warning("Cannot regenerate movie information without ID.");
                }
                return null;
            }

            @Override
            public void done() {
                enableControls(true);
                currentVideoIndex = -1;
                videoListValueChanged(null);
                showProcessing(false);
            }
        }.execute();
    }

    private void makeEditable(java.awt.event.MouseEvent evt) {
        JTextComponent field = (JTextComponent)evt.getComponent();
        if (!field.isEditable() && evt.getClickCount() == 2 && vidXml && !isBusy) {
            field.setSelectionStart(0);
            field.setSelectionEnd(0);
            field.setEditable(true);
            txtFilter.requestFocus();
            field.requestFocus();
            field.setCaretPosition(field.getText().length());
            undoChanges.put(field.getName(), field.getText());
        }
    }

    private void maybeSaveChanges(java.awt.event.KeyEvent evt) {
        JTextComponent field = (JTextComponent)evt.getComponent();
        if (field.isEditable() && evt.getKeyCode() == 10 && !isBusy) {
            updateSelectedMovieXml(field.getName().replace("_Txt", ""), field.getText());
            createHTML();
            field.setEditable(false);
            field.getCaret().setVisible(false);
        } else if (field.isEditable() && evt.getKeyCode() == 27) {
            field.setEditable(false);
            field.getCaret().setVisible(false);
            field.setText(undoChanges.get(field.getName()).toString());
        } else {
            //System.out.println(evt.getKeyCode());
        }
    }

    public void createHTML() {
        if (Boolean.parseBoolean(getProperty("mjb.skipHtmlGeneration","false"))) return;
        final MovieJukeboxHTMLWriter htmlWriter = new MovieJukeboxHTMLWriter();

        Movie movie = getSelectedMovie(false);
        movie.setDirty(DirtyFlag.INFO, true);
        htmlWriter.generateMovieDetailsHTML(JukeboxInterface.getJukebox(), movie);
        logger.fine("HTML details page written for "+movie.getBaseFilename());
    }

    @Action
    public void showSrcImg() {
        try {
            ExecutorService threadPool = Executors.newFixedThreadPool(40);
            threadPool.submit(new ImageManagerWorker(ImageManager.PosterMode));
            threadPool.shutdown();
        } catch (Exception ex) {
            logger.severe(ex+": launching poster manager for "+getSelectedMovie(false).getBaseFilename());
        }
    }

    @Action
    public void showFanart() {
        try {
            ExecutorService threadPool = Executors.newFixedThreadPool(40);
            threadPool.submit(new ImageManagerWorker(ImageManager.FanartMode));
            threadPool.shutdown();
        } catch (Exception ex) {
            logger.severe(ex+": launching fanart manager for "+getSelectedMovie(false).getBaseFilename());
        }
    }

    public class ImageManagerWorker extends MySwingWorker {
        private String managerMode;

        public ImageManagerWorker(String mod) {
            super(YAYManView.this.progressBar);
            managerMode = mod;
        }

        @Override
        protected Void doInBackground() {
            try {
                showProcessing(true);
                ImageManager imgView = new ImageManager(YAYManView.this, true, getSelectedMovie(true), managerMode);
                showProcessing(false);
                imgView.setLocationRelativeTo(YAYManApp.getApplication().getMainFrame());
                imgView.setVisible(true);
            } catch (Exception ex) {
                YAYManView.logger.severe(ex+": launching image manager for "+getSelectedMovie(false).getBaseFilename());
            } catch (Throwable th) {
                YAYManView.logger.severe(th+": launching image manager for "+getSelectedMovie(false).getBaseFilename());
            }
            return null;
        }
    }

    public String getFullDetailsPath() {
        return JukeboxInterface.getFullDetailsPath();
    }

    @Action
    public void processAllVideos() {
        processLibraryWorker = new MySwingWorker(progressBar) {
            @Override
            protected Void doInBackground() {
                try {
                    if (SystemTray.isSupported()) {
                        if (!YAYManView.this.getFrame().isVisible()) {
                            trayIcon.displayMessage("Processing", "YAMJ is processing your library...", TrayIcon.MessageType.INFO);
                        }
                    }
                    showProcessing(true);
                    enableControls(false);
                    setProcessing(true);
                    JukeboxInterface.generateLibrary();

                } catch (InvocationTargetException ex) {
                    ex.printStackTrace();
                } catch (Exception ex) {
                    YAYManView.logger.severe(ex+": processing movies");  
                } catch (Throwable th) {
                    YAYManView.logger.severe(th+": processing movies");
                } finally {
                    showProcessing(false);
                    enableControls(true);
                    setProcessing(false);
                    if (SystemTray.isSupported()) {
                        if (!YAYManView.this.getFrame().isVisible()) {
                            trayIcon.displayMessage("Processing complete", "YAMJ has finished processing your library.", TrayIcon.MessageType.INFO);
                        }
                    }
                    displayInit();
                }
                return null;
            }
        };
        processLibraryWorker.execute();
    }

    public Movie getSelectedMovie(boolean loadXML) {
        Movie movie = null;
        try {
            movie = (Movie)((MovieWrapper)videoList.getSelectedValue()).getMovie();
            
            File xmlFile = MovieXmlTools.getXmlFile(movie);
            
            if (xmlFile.exists() && loadXML) {
                Movie xmlMovie = JukeboxInterface.cloneMovie(movie);
                final MovieJukeboxXMLReader xmlReader = new MovieJukeboxXMLReader();
                xmlReader.parseMovieXML(xmlFile, xmlMovie);
                movie = xmlMovie;
            }
        } catch (Exception ex) {
            YAYManView.logger.severe(ex+": couldn't get selected movie");
        }
        return movie;
    }

    public Movie getSelectedMovie() {
        return getSelectedMovie(true);
    }

    public MovieFile getSelectedMovieFile() {
        Movie movie = getSelectedMovie(false);
        MovieFile movieFile= null;
        for (MovieFile mf : movie.getMovieFiles()) {
            if (mf.getFirstPart() <= getSelectedEpisodeNumber() && mf.getLastPart() >= getSelectedEpisodeNumber()) {
                movieFile = mf;
                break;
            }
        }
        return movieFile;
    }

    public void enableControls(boolean enabled) {
        videoList.setEnabled(enabled);
        processMenuItem.setEnabled(enabled);
        idRegen.setEnabled(enabled);
        posterEdit.setEnabled(enabled);
        fanartEdit.setEnabled(enabled);
        bannerEdit.setEnabled(enabled);
        txtFilter.setEnabled(enabled);
        cmbMovieID.setEnabled(enabled);
        isBusy = !enabled;
        fileTree.setEnabled(enabled);
        editMenu.setEnabled(enabled);
        viewMenu.setEnabled(enabled);
        idAdd.setEnabled(enabled);
        idRemove.setEnabled(enabled);
    }

    public void setProcessing(boolean processing) {
        //cancelProcessingMenu.setVisible(processing);
        if (SystemTray.isSupported()) {
            ResourceMap resourceMap = getResourceMap();
            javax.swing.ImageIcon frameIcon;
            if (processing) {
                frameIcon = resourceMap.getImageIcon("mainFrameAnimIcon");
            } else {
                frameIcon = resourceMap.getImageIcon("mainFrameIcon");
            }
            trayIcon.setImage(frameIcon.getImage());
        }
    }

    @Action
    public void displayMovieID() {
        id_Txt.setText("");
        if (videoList.getSelectedIndex() != -1 && cmbMovieID.getSelectedIndex() != -1) {
            id_Txt.setText(getVideoId(getSelectedMovie(false),cmbMovieID.getSelectedItem().toString()));
            id_Txt.setEditable(false);
        }
    }

    @Action
    public void visitMoviePage() {
        String site = cmbMovieID.getSelectedItem().toString();
        String key = id_Txt.getText();
        String prefix = "";
        String suffix = "";
        if (site.equals(ImdbPlugin.IMDB_PLUGIN_ID)) {
            prefix = "http://www.imdb.com/title/";
        } else if (site.equals(TheMovieDbPlugin.TMDB_PLUGIN_ID)) {
            prefix = "http://www.themoviedb.org/movie/";
        } else if (site.equals(TheTvDBPlugin.THETVDB_PLUGIN_ID)) {
            prefix = "http://www.thetvdb.com/?tab=series&id=";
        } else if (site.equals(AllocinePlugin.ALLOCINE_PLUGIN_ID)) {
            prefix = "http://www.allocine.fr/film/fichefilm_gen_cfilm=";
            suffix = ".html";
        } else if (site.equals(FilmUpITPlugin.FILMUPIT_PLUGIN_ID)) {
            prefix = "http://filmup.leonardo.it/sc_";
            suffix = ".htm";
        } else if (site.equals(FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID)) {
            prefix = "http://www.filmaffinity.com/es/film";
        } else if (site.equals(KinopoiskPlugin.IMDB_PLUGIN_ID)) {
            prefix = "http://www.kinopoisk.ru/level/1/film/";
        } else if (site.equals(MovieMeterPlugin.MOVIEMETER_PLUGIN_ID)) {
            prefix = "http://www.moviemeter.nl/film/";
        } else if (site.equals(ScopeDkPlugin.SCOPEDK_PLUGIN_ID)) {
            prefix = "http://www.scope.dk/film/";
        } else if (site.equals(SratimPlugin.SRATIM_PLUGIN_ID)) {
            prefix = "http://www.sratim.co.il/movies/view.aspx?id=";
        } else {
            logger.warning("Visiting this site is not currently supported.");
            return;
        }
        try {
            java.awt.Desktop.getDesktop().browse(new URI(prefix+key+suffix));
        } catch (Exception ex) {
            logger.severe("Error launching plugin "+site+": "+ex);
            logger.severe("Attempted address: "+prefix+key+suffix);
        }
    }

    private void addSearchPlugin(DefaultComboBoxModel model, String pluginName) {
        try {
            Class pluginClass = Class.forName(pluginName);
            model.addElement(new MovieSearchPluginItem(pluginClass));
            if (!pluginClass.getSuperclass().getName().equals(Object.class.getName())) {
                addSearchPlugin(model, pluginClass.getSuperclass().getName());
            }
        } catch (Exception ex) {
            logger.severe(ex+": couldn't get ID for plugin "+pluginName);
        }
    }

    @Action
    public void openJukeboxDir() {
        String path = getFullDetailsPath().replace("\\", "/");
        try {
            java.awt.Desktop.getDesktop().open(new File(path).getCanonicalFile());
        } catch (Exception ex) {
            logger.severe(ex+": could not open jukebox folder '"+path+"'");
        }
    }

    @Action
    public void browseLibrary() {
        File lib = getSelectedMovie(false).getFile().getParentFile();
        try {
            java.awt.Desktop.getDesktop().open(lib.getCanonicalFile());
        } catch (Exception ex) {
            logger.severe(ex+": could not open library folder '"+lib.getPath()+"'");
        }
    }

    @Action
    public void clearFilter() {
        txtFilter.setText("");
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    @Action
    public void refreshLibrary() {
        displayInit();
    }

    @Action
    public void openHTMLDetailsPage() {
        if (Boolean.parseBoolean(getProperty("mjb.skipHtmlGeneration","false"))) {
            logger.fine("YAMJ is not set to generate detail pages.");
            return;
        }
        String path = getFullDetailsPath()+"/"+getSelectedVideoXmlValue("details");
        try {
            path = getFullDetailsPath()+"/"+URLDecoder.decode(getSelectedVideoXmlValue("details"), "UTF-8");
            java.awt.Desktop.getDesktop().open(new File(path).getCanonicalFile());
        } catch (UnsupportedEncodingException ex) {
            logger.severe("Encoding 'UTF-8' not supported.");
        } catch (Exception ex) {
            logger.severe(ex+": could not open details page '"+path+"'");
        }
    }

    public void convertHashdepth(final int oldDepth) {
        new MySwingWorker(progressBar) {
            @Override
            public Void doInBackground() {
                try {
                    showProcessing(true);
                    enableControls(false);
                    displayInit();
                    Movie[] movies = (vidList.getLibrary()).values().toArray(new Movie[1]);
                    int progress = 0;
                    String hashstr = "";
                    for (int i = 0; i < movies.length; i++) {
                        Movie movie = movies[i];
                        logger.fine("Converting "+movie.getBaseFilename()+"...");

                        File file = movie.getFile();
                        String mediaLibraryRoot = file.getParentFile().getAbsolutePath();
                        int mediaLibraryRootPathIndex = FileTools.getDirPathWithSeparator(mediaLibraryRoot).length();
                        String relativeFilename = file.getAbsolutePath().substring(mediaLibraryRootPathIndex);
                        if(oldDepth > 0){
                            int d, pos=relativeFilename.length();
                            for(d = oldDepth+1; d > 0 && pos > 0; d-- )
                                pos=relativeFilename.lastIndexOf("/", pos-1);
                            hashstr = relativeFilename.substring(pos+1);
                            hashstr = Integer.toHexString(hashstr.hashCode());
                        }
                        String oldBase = FileTools.makeSafeFilename(movie.getBaseFilename()) + hashstr;

                        renameJukeboxFiles(movie, oldBase, movie.getBaseName());

                        progress = i / movies.length;
                        progressBar.setValue(progress);
                        logger.fine("Finished converting "+movie.getBaseFilename());
                    }

                } catch (Exception ex) {
                    YAYManView.logger.severe(ex+" while converting files");
                } catch (Throwable th) {
                    YAYManView.logger.severe(th+" while converting files");
                }
                return null;
            }

            @Override
            public void done() {
                showProcessing(false);
                enableControls(true);
                logger.fine("Conversion complete.");
            }
        }.execute();
    }

    public void renameJukeboxFiles(Movie movie, String oldBase, String newBase) {
        File xmlFile = new File(getFullDetailsPath() + File.separator + oldBase + ".xml");
        if (xmlFile.exists()) {
            xmlFile.renameTo(new File(getFullDetailsPath() + File.separator + newBase + ".xml"));

            renameAttribute(movie,"details",newBase);
            renameAttribute(movie,"posterFile",newBase);
            renameAttribute(movie,"fanartFile",newBase);
            renameAttribute(movie,"detailPosterFile",newBase);
            renameAttribute(movie,"thumbnail",newBase);
            renameAttribute(movie,"bannerFile",newBase);
        }

        File playlistFile = new File(getFullDetailsPath() + File.separator + oldBase + ".playlist.jsp");
        if (playlistFile.exists()) {
            playlistFile.renameTo(new File(getFullDetailsPath() + File.separator + newBase + ".playlist.jsp"));
        }
    }
    
    private String getExtension(String file) {
        return file.substring(file.lastIndexOf("."));
    }

    private String getExtension(File file) {
        return getExtension(file.getName());
    }

    private String getNewFileName(Movie movie, String attr, String newBase) {
        String newName = Movie.UNKNOWN;
        String attVal = HTMLTools.decodeUrl(MovieXmlTools.getVideoXmlValue(movie,attr));
        if (!attVal.equals(Movie.UNKNOWN)) {
            String ext = getExtension(attVal);
            if (attr.equals("fanartFile")) {
                newName = newBase+getProperty("mjb.scanner.fanartToken", ".fanart")+ext;
            } else if (attr.equals("detailPosterFile")) {
                newName = newBase+getProperty("mjb.scanner.posterToken", "_large")+ext;
            } else if (attr.equals("thumbnail")) {
                newName = newBase+getProperty("mjb.scanner.thumbnailToken", "_small")+ext;
            } else if (attr.equals("bannerFile")) {
                newName = newBase+getProperty("mjb.scanner.bannerToken", ".banner")+ext;
            } else {
                newName = newBase+ext;
            }
        }
        return newName;
    }

    private void renameAttribute(Movie movie, String attr, String newBase) {
        if (!MovieXmlTools.getVideoXmlValue(movie,attr).equals(Movie.UNKNOWN)) {
            String newName = getNewFileName(movie,attr,newBase);
            File file = new File(getFullDetailsPath() + File.separator + HTMLTools.decodeUrl(MovieXmlTools.getVideoXmlValue(movie,attr)));
            if (file.exists()) {
                file.renameTo(new File (getFullDetailsPath() + File.separator + newName));
            }
            MovieXmlTools.updateMovieXml(movie, attr, HTMLTools.encodeUrl(newName));
        }
    }

    public void backupJukeboxFiles(final boolean backupXml, final boolean backupPosters, final boolean backupFanart, final boolean backupBanners, final boolean backupVideoImages, final String backupPath) {
        new MySwingWorker(progressBar) {
            @Override
            public Void doInBackground() {
                try {
                    showProcessing(true);
                    progressBar.setIndeterminate(false);
                    enableControls(false);
                    Movie[] movies = (vidList.getLibrary()).values().toArray(new Movie[1]);
                    progressBar.setMaximum(movies.length);
                    for (int i = 0; i < movies.length; i++) {
                        progressBar.setValue(i);
                        Movie movie = movies[i];
                        logger.fine("Backing up "+movie.getBaseFilename());

                        File xmlFile = new File(getFullDetailsPath() + File.separator + movie.getBaseName() + ".xml");
                        if (xmlFile.exists()) {
                            if (backupXml) {
                                FileTools.copyFile(xmlFile, new File(backupPath, xmlFile.getName()));
                            }
                            
                            if (backupPosters) {
                                backupFileFromXml(movie, "posterFile", backupPath);
                            }

                            if (backupFanart) {
                                backupFileFromXml(movie, "fanartFile", backupPath);
                            }

                            if (backupBanners) {
                                backupFileFromXml(movie, "bannerFile", backupPath);
                            }

                            if (backupVideoImages) {
                                backupFilesFromXml(movie, "fileImageFile", backupPath);
                            }
                        }
                    }
                } catch (Exception ex) {
                    YAYManView.logger.severe(ex+" while backing up files");
                } catch (Throwable th) {
                    YAYManView.logger.severe(th+" while backing up files");
                }
                return null;
            }

            @Override
            public void done() {
                showProcessing(false);
                enableControls(true);
                logger.fine("Backup complete.");
            }
        }.execute();
    }

    private void backupFileFromXml(Movie movie, String attr, String backupPath) {
        String attrValue = MovieXmlTools.getVideoXmlValue(movie,attr);
        if (!attrValue.equals(Movie.UNKNOWN)) {
            File file = new File(getFullDetailsPath() + File.separator + HTMLTools.decodeUrl(attrValue));
            if (file.exists()) {
                FileTools.copyFile(file, new File(backupPath, file.getName()));
            }
        }
    }

    private void backupFilesFromXml(Movie movie, String attr, String backupPath) {
        NodeList nodes = MovieXmlTools.getVideoXmlNodes(movie, attr);
        if (nodes != null && nodes.getLength() > 0) {
            for (int i = 0; i < nodes.getLength(); i++) {
                String attrValue = nodes.item(i).getTextContent();
                if (!attrValue.equals(Movie.UNKNOWN)) {
                    File file = new File(getFullDetailsPath() + File.separator + HTMLTools.decodeUrl(attrValue));
                    if (file.exists()) {
                        FileTools.copyFile(file, new File(backupPath, file.getName()));
                    }
                }
            }
        }
    }

    @Action
    public void openSupportForum() {
        try {
            java.awt.Desktop.getDesktop().browse(new URI("http://www.networkedmediatank.com/showthread.php?tid=35678"));
        } catch (Exception ex) {
            YAYManView.logger.severe(ex+": could not open support thread");
        }
    }

    @Action
    public void fileTitleRename() {
        TreePath path = fileTree.getSelectionPath();
        if (path != null && path.getPath().length > 1) {
            fileTree.startEditingAtPath(fileTree.getSelectionPath());
        }
    }

    @Action
    public void showSkinProps() {
        EditPropertiesFrame editProps = new EditPropertiesFrame(prefsWindow, true, EditPropertiesFrame.SkinPropertiesMode);
        editProps.setLocationRelativeTo(this.getFrame());
        editProps.setVisible(true);
    }

    public void setJukeboxOptions(DefaultListModel model, int index) {
        jukeboxMenu.removeAll();
        for (int i = 0; i < model.getSize(); i++) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(model.get(i).toString());
            if (i == index) item.setSelected(true);
            item.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    JCheckBoxMenuItem clickedItem =(JCheckBoxMenuItem)evt.getSource();
                    for (int j = 0; j < jukeboxMenu.getItemCount(); j++) {
                        JCheckBoxMenuItem currentItem = (JCheckBoxMenuItem)jukeboxMenu.getItem(j);
                        currentItem.setSelected(false);
                        if (currentItem == clickedItem) prefsWindow.setSelectedJukebox(j);
                    }
                    clickedItem.setSelected(true);
                }
            });
            jukeboxMenu.add(item);
        }
    }

    @Action
    public void showYAMJProps() {
        EditPropertiesFrame editProps = new EditPropertiesFrame(prefsWindow, true, EditPropertiesFrame.PropertiesMode);
        editProps.setLocationRelativeTo(this.getFrame());
        editProps.setVisible(true);
    }

    @Action
    public void showBanner() {
        try {
            ExecutorService threadPool = Executors.newFixedThreadPool(40);
            threadPool.submit(new ImageManagerWorker(ImageManager.BannerMode));
            threadPool.shutdown();
        } catch (Exception ex) {
            logger.severe(ex+": launching banner manager for "+getSelectedMovie(false).getBaseFilename());
        }
    }

    @Action
    public void showLibraryEdit() {
        SavedJukebox jukebox = prefsWindow.getSelectedJukebox();
        EditLibraryFrame editLib = new EditLibraryFrame(this.getFrame(), true, jukebox);
        editLib.setLocationRelativeTo(this.getFrame());
        editLib.setVisible(true);
    }

    @Action
    public void showVideoImage() {
        try {
            ExecutorService threadPool = Executors.newFixedThreadPool(40);
            threadPool.submit(new ImageManagerWorker(ImageManager.VideoImageMode));
            threadPool.shutdown();
        } catch (Exception ex) {
            logger.severe(ex+": launching video image manager for "+getSelectedMovie(false).getBaseFilename());
        }
    }
    
    public int getSelectedEpisodeNumber() {
        if (fileTree.isSelectionEmpty() || fileTree.getSelectionPath().getPath().length < 3) return -1;
        XmlTreeNode treeNode = getSelectedEpisodeNode();//(XmlTreeNode)fileTree.getSelectionPath().getPath()[2];
        Element element = treeNode.getElement();
        return Integer.parseInt(element.getAttribute("part"));
    }

    public XmlTreeNode getSelectedEpisodeNode() {
        if (fileTree.isSelectionEmpty() || fileTree.getSelectionPath().getPath().length < 3) return null;
        return (XmlTreeNode)fileTree.getSelectionPath().getPath()[2];
    }

    @Action
    public void logMenuToggle() {
        logWindow.setVisible(logMenuItem.isSelected());
    }

    public void showLog(boolean show) {
        logWindow.setVisible(show);
        logMenuItem.setSelected(show);
    }

    @Action
    public void showAddMovieIdDiag() {
        AddMovieIdDialog diag = new AddMovieIdDialog(this);
        diag.setLocationRelativeTo(this.getFrame());
        diag.setVisible(true);
    }

    public void addSelectedMovieId(MovieSearchPluginItem plugin, String key) {
        addMovieId(getSelectedMovie(false), plugin.toString(), key);
        DefaultComboBoxModel model = (DefaultComboBoxModel)cmbMovieID.getModel();
        model.addElement(plugin);
        setAccessIdRemove();
        logger.fine("Added video ID for "+plugin);
    }

    public HashMap<String,MovieSearchPluginItem> getMovieIds() {
        HashMap<String,MovieSearchPluginItem> ids = new HashMap();
        DefaultComboBoxModel model = (DefaultComboBoxModel)cmbMovieID.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            MovieSearchPluginItem plugin = (MovieSearchPluginItem)model.getElementAt(i);
            ids.put(plugin.toString(), plugin);
        }
        return ids;
    }

    public void setAccessIdRemove() {
        idRemove.setEnabled(cmbMovieID.getModel().getSize() > 1);
    }

    @Action
    public void removeId() {
        if (JOptionPane.showConfirmDialog(YAYManView.this.getComponent(), "Are you sure you want to delete this movie ID?", "Confirm Delete", JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
            removeMovieId(getSelectedMovie(false), cmbMovieID.getSelectedItem().toString());
            DefaultComboBoxModel model = (DefaultComboBoxModel)cmbMovieID.getModel();
            model.removeElementAt(cmbMovieID.getSelectedIndex());
        }
    }

    @Action
    public void cancelJukeboxProcessing() {
        if (processLibraryWorker != null) {
            processLibraryWorker.cancel(true);
            processLibraryWorker.showProcessing(false);
        }
        enableControls(true);
        setProcessing(false);
    }

    public boolean busy() {
        return isBusy;
    }

    public void windowClosing(WindowEvent e) {
        boolean doClose = true;
        Object source = e.getSource();

        if (doClose) {
            this.exit();
        } else {
            // code to hide window
        }
    }

    @Action
    public void exit() {
        YAYManView.this.getFrame().dispose();
        fh.close();

        System.exit(0); //calling the method is a must
    }

    @Action
    public void showNmts() {
        if (nmtManager == null) {
            nmtManager = new NMTManager(prefsWindow.getSelectedJukebox().getNmt().getName());
            nmtManager.setLocationRelativeTo(this.getFrame());
            nmtManager.setVisible(true);
        } else {
            nmtManager.setVisible(true);
            nmtManager.requestFocus();
        }
    }

    @Action
    public void updateYAMJ() {
        if (yamjUpdate != null) {
            yamjUpdate.closeForm();
            yamjUpdate = null;
        }
        yamjUpdate = new YAMJUpdateDiag(this);
        yamjUpdate.setVisible(true);
    }

    @Action
    public void updateYAYMan() {
        updateYAYMan(true);
    }

    public void updateYAYMan(boolean noisy) {
        if (yaymanUpdate != null) {
            yaymanUpdate.closeForm();
            yaymanUpdate = null;
        }
        yaymanUpdate = new YAYManUpdateDiag(this, noisy);
    }

    @Action
    public void forceRestart() {
        if (!busy()) YAYManApp.restartApplication(this);
    }

    @Action
    public void openYAMJDir() {
        try {
            java.awt.Desktop.getDesktop().open(new File(System.getProperty("user.dir")).getCanonicalFile());
        } catch (Exception ex) {
            logger.severe(ex+": could not open jukebox folder '"+System.getProperty("user.dir")+"'");
        }
    }

    @Action
    public void editSetPoster() {
        try {
            ExecutorService threadPool = Executors.newFixedThreadPool(40);
            threadPool.submit(new ImageManagerWorker(ImageManager.SetPosterMode));
            threadPool.shutdown();
        } catch (Exception ex) {
            logger.severe(ex+": launching set poster manager for "+getSelectedMovie(false).getBaseFilename());
        }
    }

    @Action
    public void editSetFanart() {
        try {
            ExecutorService threadPool = Executors.newFixedThreadPool(40);
            threadPool.submit(new ImageManagerWorker(ImageManager.SetFanartMode));
            threadPool.shutdown();
        } catch (Exception ex) {
            logger.severe(ex+": launching set fanart manager for "+getSelectedMovie(false).getBaseFilename());
        }
    }

    @Action
    public void editSetBanner() {
        try {
            ExecutorService threadPool = Executors.newFixedThreadPool(40);
            threadPool.submit(new ImageManagerWorker(ImageManager.SetBannerMode));
            threadPool.shutdown();
        } catch (Exception ex) {
            logger.severe(ex+": launching set banner manager for "+getSelectedMovie(false).getBaseFilename());
        }
    }

    public String getSelectedSet() {
        if (lstSets.getSelectedIndex() == -1) return null;
        return lstSets.getSelectedValue().toString();
    }

    @Action
    public void refreshMediaInfo() {
        Movie movie = getSelectedMovie();
        if (movie != null) {
            //System.out.println("\r\n"+movie.getResolution());
            /*movie.setAudioChannels(null);
            movie.setCodecs(null);
            movie.setContainer(null);
            movie.setFileDate(null);*/
            movie.setRuntime(Movie.UNKNOWN,"yayman");
            //movie.setVideoCodec(Movie.UNKNOWN);
            movie.setResolution(Movie.UNKNOWN,"yayman");
            movie.setAspectRatio(Movie.UNKNOWN,"yayman");
            movie.setVideoOutput(Movie.UNKNOWN,"yayman");
            //movie.setAudioCodec(Movie.UNKNOWN);
            MediaInfoScanner miScanner = new MediaInfoScanner();
            miScanner.scan(movie);
            //System.out.println("\r\n"+movie.getResolution());
        }
    }

    @Action
    public void syncPlotOutline() {
        javax.swing.JTextArea source;
        javax.swing.JTextArea dest;
        if (plotTabbedPane.getSelectedIndex() == 0) {
            source = plot_Txt;
            dest = outline_Txt;
        } else {
            source = outline_Txt;
            dest = plot_Txt;
        }
        
        dest.setText(source.getText());
        
        java.awt.event.KeyEvent evt = new java.awt.event.KeyEvent(dest, java.awt.event.KeyEvent.KEY_PRESSED, Calendar.getInstance().getTime().getTime(), java.awt.event.KeyEvent.META_MASK, 10, java.awt.event.KeyEvent.CHAR_UNDEFINED);
        dest.setEditable(true);
        maybeSaveChanges(evt);
        dest.setEditable(false);
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField audioChannels_Txt;
    private javax.swing.JTextField audioCodec_Txt;
    private javax.swing.JMenuItem bannerEdit;
    private javax.swing.JPanel basicPanel;
    private javax.swing.JButton btnGenreAdd;
    private javax.swing.JButton btnGenreRemove;
    private javax.swing.JButton btnRefreshMediaInfo;
    private javax.swing.JMenu cancelProcessingMenu;
    private javax.swing.JTextField certification_Txt;
    private javax.swing.JMenuItem clearFilterMenuItem;
    private javax.swing.JComboBox cmbMovieID;
    private javax.swing.JTextField container_Txt;
    private javax.swing.JMenu editMenu;
    private javax.swing.JMenuItem fanartEdit;
    private javax.swing.JPopupMenu fileTitleMenu;
    private javax.swing.JMenuItem fileTitleRenameMenu;
    private javax.swing.JTree fileTree;
    private javax.swing.JPopupMenu filterMenu;
    private javax.swing.JTextField fps_Txt;
    private javax.swing.JList genresList;
    private javax.swing.JMenuItem htmlView;
    private javax.swing.JMenuItem idAdd;
    private javax.swing.JLabel idLbl;
    private javax.swing.JPopupMenu idMenu;
    private javax.swing.JMenuItem idRegen;
    private javax.swing.JMenuItem idRemove;
    private javax.swing.JMenuItem idVisit;
    private javax.swing.JTextField id_Txt;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JMenuItem jukeboxDirMenuItem;
    private javax.swing.JMenu jukeboxMenu;
    private javax.swing.JPanel leftPanel;
    private javax.swing.JMenuItem libraryBrowse;
    private javax.swing.JMenuItem libraryFileMenuItem;
    private javax.swing.JCheckBoxMenuItem logMenuItem;
    private javax.swing.JList lstSets;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenu movieMenu;
    private javax.swing.JTabbedPane movieTabbedPane;
    private javax.swing.JMenuItem newVersionMenu;
    private javax.swing.JMenuItem nmtMenuItem;
    private javax.swing.JTextArea outline_Txt;
    private javax.swing.JPopupMenu plotMenu;
    private javax.swing.JMenuItem plotSyncMenu;
    private javax.swing.JTabbedPane plotTabbedPane;
    private javax.swing.JTextArea plot_Txt;
    private javax.swing.JMenuItem posterEdit;
    private javax.swing.JPopupMenu posterMenu;
    private javax.swing.JMenuItem prefsMenuItem;
    private javax.swing.JMenuItem processMenuItem;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JMenuItem refreshLibMenuItem;
    private javax.swing.JTextField resolution_Txt;
    private javax.swing.JMenuItem restartMenuItem;
    private javax.swing.JPanel rightPanel;
    private javax.swing.JTextField runtime_Txt;
    private javax.swing.JMenuItem setAdd;
    private javax.swing.JMenuItem setBannerEdit;
    private javax.swing.JMenuItem setEdit;
    private javax.swing.JMenuItem setFanartEdit;
    private javax.swing.JPopupMenu setImageMenu;
    private javax.swing.JPopupMenu setMenu;
    private javax.swing.JMenuItem setPosterEdit;
    private javax.swing.JLabel setPosterLbl;
    private javax.swing.JMenuItem setRemove;
    private javax.swing.JMenuItem skinPropsMenuItem;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JTextField subtitles_Txt;
    private javax.swing.JMenuItem supportMenu;
    private javax.swing.JLabel thumbLbl;
    private javax.swing.JTextField titleSort_Txt;
    private javax.swing.JTextField title_Txt;
    private javax.swing.JMenu toolsMenu;
    private javax.swing.JTextField top250_Txt;
    private javax.swing.JTextField txtFilter;
    private javax.swing.JTextField videoCodec_Txt;
    private javax.swing.JList videoGenresList;
    private javax.swing.JList videoList;
    private javax.swing.JTextField videoSource_Txt;
    private javax.swing.JMenu viewMenu;
    private javax.swing.JMenuItem vimageEdit;
    private javax.swing.JMenuItem yamjDirMenuItem;
    private javax.swing.JMenuItem yamjPropsMenuItem;
    private javax.swing.JMenuItem yamjUpMenuItem;
    private javax.swing.JTextField year_Txt;
    // End of variables declaration//GEN-END:variables

    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;

    private JDialog aboutBox;
}
