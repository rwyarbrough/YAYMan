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
 * YAMJManagerPrefs.java
 *
 * Created on Jan 13, 2010, 9:49:16 AM
 */

package yayman;

import java.awt.Color;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import org.jdesktop.application.Action;
import javax.swing.JOptionPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.DefaultListModel;
import javax.swing.event.*;
import java.util.logging.*;
import static com.moviejukebox.tools.PropertiesUtil.setPropertiesStreamName;
import static com.moviejukebox.tools.PropertiesUtil.getProperty;
import static com.moviejukebox.tools.PropertiesUtil.setProperty;
import java.util.*;
import java.net.URI;

import java.io.*;
import javax.swing.JSpinner;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;

public class YAYManPrefs extends javax.swing.JDialog {
    YAYManView mainFrame;
    //String defaultPropsPath;
    String settingsName;
    int prevSelIndex;
    public static Logger logger = Logger.getLogger("yayman");
    DefaultComboBoxModel nmtListModel;
    private WebAPI webAPI;
    DefaultListModel jbListModel;
    boolean doScheduleUpdate;
    Timer jukeboxTimer;
    Timer updateCheckTimer;

    /** Creates new form YAMJManagerPrefs */
    /*public YAYManPrefs(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
    }*/
    public YAYManPrefs(YAYManView mainf, boolean modal) {
        super(mainf.getFrame(),modal);
        initComponents();

        PopAPI.searchNmts();
        nmtListModel = new DefaultComboBoxModel();//PopAPI.getNmtListModel();
        //nmtListModel.addElement(new NetworkedMediaTank("None"));
        PopAPI.syncNmtsWith(nmtListModel);
        nmtList.setModel(nmtListModel);
        nmtListModel.addListDataListener(new ListDataListener() {
            public void contentsChanged(ListDataEvent e) {}

            public void intervalRemoved(ListDataEvent e) {}

            public void intervalAdded(ListDataEvent e) {
                if (getSelectedJukebox() != null && nmtListModel.getSize() > 0) {
                    NetworkedMediaTank nmt = (NetworkedMediaTank)nmtListModel.getElementAt(nmtListModel.getSize()-1);
                    if (getSelectedJukebox().getNmt().getName().equals(nmt.getName())) {
                        nmtList.setSelectedIndex(nmtListModel.getSize()-1);
                    }
                }
            }
        });

        webAPI = null;

        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                saveBtnClicked();
            }
        });

        jukeboxNameTxt.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                getSelectedJukebox().setName(jukeboxNameTxt.getText());
                lstJukebox.repaint();
            }
            public void removeUpdate(DocumentEvent e) {
                getSelectedJukebox().setName(jukeboxNameTxt.getText());
                lstJukebox.repaint();
            }
            public void insertUpdate(DocumentEvent e) {
                getSelectedJukebox().setName(jukeboxNameTxt.getText());
                lstJukebox.repaint();
            }
        });
        
        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(spinAutoDaily, "HH:mm");
        spinAutoDaily.setEditor(timeEditor);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        spinAutoDaily.setValue(calendar.getTime());
        
        doScheduleUpdate = true;
        
        jukeboxTimer = new Timer();
        updateCheckTimer = new Timer();

        prevSelIndex = 0;
        mainFrame = mainf;
        settingsName = "./yaymansettings.xml";
        boolean settingsExist = (new File(settingsName)).exists();
        if (!settingsExist) {
            settingsName = "./yamjmansettings.xml";
            settingsExist = (new File(settingsName)).exists();
        }
        Document dom;
        DocumentBuilder db;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        //defaultPropsPath = "properties"+File.separator+"moviejukebox-default.properties";
        try {
            db = dbf.newDocumentBuilder();
            dom = db.newDocument();

            setDefaultProperties();

            //DefaultListModel model = new DefaultListModel();
            jbListModel = new DefaultListModel();
            if (settingsExist) {
		try {
                    //parse using builder to get DOM representation of the XML file
                    dom = db.parse(settingsName);

                    Element docEle = dom.getDocumentElement();
                    
                    NodeList jukeboxes = docEle.getElementsByTagName("YAMJ");
                    int selectedIndex = 0;
                    if (docEle.hasAttribute("selected")) {
                        selectedIndex = Integer.parseInt(docEle.getAttribute("selected"));
                        if (selectedIndex == -1) selectedIndex = 0;
                    }
                    for (int i = 0; i < jukeboxes.getLength(); i++) {
                        Element ele = (Element)jukeboxes.item(i);
                        SavedJukebox jukebox = new SavedJukebox(ele);
                        jbListModel.addElement(jukebox);
                    }
                    
                    if (docEle.hasAttribute("checkVersionStartup")) {
                        newVerChk.setSelected(Boolean.parseBoolean(docEle.getAttribute("checkVersionStartup")));
                    } else {
                        newVerChk.setSelected(true);
                    }
                    
                    if (docEle.hasAttribute("checkVersionDaily")) {
                        newVerDailyChk.setSelected(Boolean.parseBoolean(docEle.getAttribute("checkVersionDaily")));
                    } else {
                        newVerDailyChk.setSelected(true);
                    }

                    if (docEle.hasAttribute("enableHttpRequests")) {
                        httpChk.setSelected(Boolean.parseBoolean(docEle.getAttribute("enableHttpRequests")));
                    } else {
                        httpChk.setSelected(false);
                    }
                    
                    if (docEle.hasAttribute("httpPort")) {
                        portTxt.setText(docEle.getAttribute("httpPort"));
                    } else {
                        portTxt.setText("1338");
                    }
                    
                    if (docEle.hasAttribute("mkvtoolnixFolder")) {
                        setMTNFolder(docEle.getAttribute("mkvtoolnixFolder"));
                    } else {
                        setMTNFolder("mkvToolnix");
                    }
                    
                    if (docEle.hasAttribute("scheduleType")) {
                        String schedFreq = docEle.getAttribute("scheduleType");
                        if (schedFreq.equals("Never")) {
                            //already the default
                            doScheduleUpdate = false;
                        } else if (schedFreq.equals("Daily")) {
                            radAutoDaily.setSelected(true);
                        } else if (schedFreq.equals("Custom")) {
                            radAutoCustom.setSelected(true);
                        }
                    }
                    
                    if (docEle.hasAttribute("scheduleDailyTime")) {
                        Date date = new Date(Long.parseLong(docEle.getAttribute("scheduleDailyTime")));
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(date);
                        cal.set(Calendar.MILLISECOND, 0);
                        cal.set(Calendar.SECOND, 0);
                        spinAutoDaily.setValue(cal.getTime());
                        
                    }
                    
                    if (docEle.hasAttribute("scheduleCustomValue")) {
                        spinAutoCustom.setValue(Integer.parseInt(docEle.getAttribute("scheduleCustomValue")));
                    }
                    
                    if (docEle.hasAttribute("scheduleCustomUnit")) {
                        int index = Integer.parseInt(docEle.getAttribute("scheduleCustomUnit"));
                        cmbAutoCustom.setSelectedIndex(index);
                    }

                    if (jbListModel.isEmpty()) {
                        SavedJukebox jukebox = new SavedJukebox();
                        jbListModel.addElement(jukebox);
                    }
                    
                    lstJukebox.setModel(jbListModel);
                    lstJukebox.setSelectedIndex(selectedIndex);
                    prevSelIndex = selectedIndex;

                    if (doScheduleUpdate) scheduleUpdate();
                } catch(Exception se) {
                    YAYManView.logger.severe("Error reading from properties file: "+se);
		}
		/*} catch(SAXException se) {
                    YAYManView.logger.severe("Error reading from properties file: "+se);
		} /catch(IOException ioe) {
                    YAYManView.logger.severe("Error reading from properties file: "+ioe);
		}*/
            } else {
                settingsName = "./yaymansettings.xml";
                if (JOptionPane.showConfirmDialog(mainFrame.getComponent(), "Would you like to run the setup wizard?", "Run setup wizard?", JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                    Object[] options = {"Basic", "Advanced"};
                    if (JOptionPane.showOptionDialog(mainFrame.getComponent(), "Do you want to run the basic or advanced wizard?", "Basic or advanced?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]) == JOptionPane.YES_OPTION) {
                        SetupWizardBasicFrame wizard = new SetupWizardBasicFrame(this);
                        wizard.setVisible(true);
                    } else {
                        SetupWizardFrame wizard = new SetupWizardFrame(this);
                        wizard.setVisible(true);
                    }
                }

                if (jbListModel.isEmpty()) {
                    SavedJukebox jukebox = new SavedJukebox();
                    jbListModel.addElement(jukebox);
                }
                newVerChk.setSelected(true);
                newVerDailyChk.setSelected(true);
                httpChk.setSelected(false);
                portTxt.setText("1338");
                setMTNFolder("mkvToolnix");
                lstJukebox.setModel(jbListModel);
                lstJukebox.setSelectedIndex(0);
            }
            savePropertiesToDisk();
        } catch(Exception pce) {
            YAYManView.logger.severe("Error while trying to instantiate DocumentBuilder "+pce);
            System.exit(1);
        }
        if (lstJukebox.getModel().getSize() > 1) jukeboxRemoveMenu.setEnabled(true);
        setControlsFromSelectedJukebox();
        setUserProperties(getSelectedJukebox().getPropertiesFile());
        setSkinProperties();
        setApiKeys();
        setProperty("yayman.libraryFile",libTxt.getText());
        setProperty("yayman.propertiesFile",propsTxt.getText());
        //setUndo();
        mainFrame.setJukeboxOptions((DefaultListModel)lstJukebox.getModel(), lstJukebox.getSelectedIndex());

        if (newVerChk.isSelected()) mainFrame.updateYAYMan(false);
        scheduleVersionCheck();

        if (httpChk.isSelected()) webAPI = new WebAPI(mainFrame, portTxt.getText());
        toggleHttpRequests();

        autoButtonChanged();
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
        libMenu = new javax.swing.JPopupMenu();
        chooseLibMenuItem = new javax.swing.JMenuItem();
        editLibMenuItem = new javax.swing.JMenuItem();
        propsMenu = new javax.swing.JPopupMenu();
        choosePropsMenuItem = new javax.swing.JMenuItem();
        editPropsMenuItem = new javax.swing.JMenuItem();
        jukeboxMenu = new javax.swing.JPopupMenu();
        jukeboxAddMenu = new javax.swing.JMenuItem();
        jukeboxAddWizMenu = new javax.swing.JMenuItem();
        jukeboxRemoveMenu = new javax.swing.JMenuItem();
        bgAutoType = new javax.swing.ButtonGroup();
        saveBtn = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        libLbl = new javax.swing.JLabel();
        libTxt = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        propsTxt = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        lstJukebox = new javax.swing.JList();
        jLabel6 = new javax.swing.JLabel();
        jukeboxNameTxt = new javax.swing.JTextField();
        schedToggle = new javax.swing.JCheckBox();
        jLabel7 = new javax.swing.JLabel();
        nmtList = new javax.swing.JComboBox();
        jPanel7 = new javax.swing.JPanel();
        radAutoNever = new javax.swing.JRadioButton();
        jLabel8 = new javax.swing.JLabel();
        radAutoDaily = new javax.swing.JRadioButton();
        radAutoCustom = new javax.swing.JRadioButton();
        spinAutoDaily = new javax.swing.JSpinner();
        spinAutoCustom = new javax.swing.JSpinner();
        cmbAutoCustom = new javax.swing.JComboBox();
        jPanel8 = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        lblProcessTime = new javax.swing.JLabel();
        btnAutoRestart = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        chkJukePreserve = new javax.swing.JCheckBox();
        jPanel3 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        btnConvert = new javax.swing.JButton();
        spinHashdepth = new javax.swing.JSpinner();
        jLabel5 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        backupXml = new javax.swing.JCheckBox();
        backupPosters = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();
        txtBackupPath = new javax.swing.JTextField();
        btnBackupPath = new javax.swing.JButton();
        btnBackup = new javax.swing.JButton();
        backupFanart = new javax.swing.JCheckBox();
        backupBanners = new javax.swing.JCheckBox();
        backupVideoImages = new javax.swing.JCheckBox();
        jPanel2 = new javax.swing.JPanel();
        newVerChk = new javax.swing.JCheckBox();
        httpChk = new javax.swing.JCheckBox();
        portTxt = new javax.swing.JTextField();
        newVerDailyChk = new javax.swing.JCheckBox();
        mkvtoolnixLabel = new javax.swing.JLabel();
        mkvtoolnixTxt = new javax.swing.JTextField();
        mkvtoolnixBtn = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        btnConvertNMJ = new javax.swing.JButton();
        chkAutoNMJ = new javax.swing.JCheckBox();

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(yayman.YAYManApp.class).getContext().getResourceMap(YAYManPrefs.class);
        fileChooser.setBackground(resourceMap.getColor("fileChooser.background")); // NOI18N
        fileChooser.setCurrentDirectory(null);
        fileChooser.setName("fileChooser"); // NOI18N

        libMenu.setName("libMenu"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(yayman.YAYManApp.class).getContext().getActionMap(YAYManPrefs.class, this);
        chooseLibMenuItem.setAction(actionMap.get("chooseLibraryClicked")); // NOI18N
        chooseLibMenuItem.setIcon(resourceMap.getIcon("chooseLibMenuItem.icon")); // NOI18N
        chooseLibMenuItem.setText(resourceMap.getString("chooseLibMenuItem.text")); // NOI18N
        chooseLibMenuItem.setToolTipText(resourceMap.getString("chooseLibMenuItem.toolTipText")); // NOI18N
        chooseLibMenuItem.setName("chooseLibMenuItem"); // NOI18N
        libMenu.add(chooseLibMenuItem);

        editLibMenuItem.setAction(actionMap.get("editLibraryClicked")); // NOI18N
        editLibMenuItem.setIcon(resourceMap.getIcon("editLibMenuItem.icon")); // NOI18N
        editLibMenuItem.setText(resourceMap.getString("editLibMenuItem.text")); // NOI18N
        editLibMenuItem.setToolTipText(resourceMap.getString("editLibMenuItem.toolTipText")); // NOI18N
        editLibMenuItem.setName("editLibMenuItem"); // NOI18N
        libMenu.add(editLibMenuItem);

        propsMenu.setName("propsMenu"); // NOI18N

        choosePropsMenuItem.setAction(actionMap.get("choosePropsClicked")); // NOI18N
        choosePropsMenuItem.setIcon(resourceMap.getIcon("choosePropsMenuItem.icon")); // NOI18N
        choosePropsMenuItem.setText(resourceMap.getString("choosePropsMenuItem.text")); // NOI18N
        choosePropsMenuItem.setToolTipText(resourceMap.getString("choosePropsMenuItem.toolTipText")); // NOI18N
        choosePropsMenuItem.setName("choosePropsMenuItem"); // NOI18N
        propsMenu.add(choosePropsMenuItem);

        editPropsMenuItem.setAction(actionMap.get("editPropertiesClicked")); // NOI18N
        editPropsMenuItem.setIcon(resourceMap.getIcon("editPropsMenuItem.icon")); // NOI18N
        editPropsMenuItem.setText(resourceMap.getString("editPropsMenuItem.text")); // NOI18N
        editPropsMenuItem.setToolTipText(resourceMap.getString("editPropsMenuItem.toolTipText")); // NOI18N
        editPropsMenuItem.setName("editPropsMenuItem"); // NOI18N
        propsMenu.add(editPropsMenuItem);

        jukeboxMenu.setName("jukeboxMenu"); // NOI18N

        jukeboxAddMenu.setAction(actionMap.get("addNewJukebox")); // NOI18N
        jukeboxAddMenu.setIcon(resourceMap.getIcon("jukeboxAddMenu.icon")); // NOI18N
        jukeboxAddMenu.setText(resourceMap.getString("jukeboxAddMenu.text")); // NOI18N
        jukeboxAddMenu.setToolTipText(resourceMap.getString("jukeboxAddMenu.toolTipText")); // NOI18N
        jukeboxAddMenu.setName("jukeboxAddMenu"); // NOI18N
        jukeboxMenu.add(jukeboxAddMenu);

        jukeboxAddWizMenu.setAction(actionMap.get("AddNewWizJukebox")); // NOI18N
        jukeboxAddWizMenu.setIcon(resourceMap.getIcon("jukeboxAddWizMenu.icon")); // NOI18N
        jukeboxAddWizMenu.setText(resourceMap.getString("jukeboxAddWizMenu.text")); // NOI18N
        jukeboxAddWizMenu.setToolTipText(resourceMap.getString("jukeboxAddWizMenu.toolTipText")); // NOI18N
        jukeboxAddWizMenu.setName("jukeboxAddWizMenu"); // NOI18N
        jukeboxMenu.add(jukeboxAddWizMenu);

        jukeboxRemoveMenu.setAction(actionMap.get("removeSelectedJukebox")); // NOI18N
        jukeboxRemoveMenu.setIcon(resourceMap.getIcon("jukeboxRemoveMenu.icon")); // NOI18N
        jukeboxRemoveMenu.setText(resourceMap.getString("jukeboxRemoveMenu.text")); // NOI18N
        jukeboxRemoveMenu.setToolTipText(resourceMap.getString("jukeboxRemoveMenu.toolTipText")); // NOI18N
        jukeboxRemoveMenu.setName("jukeboxRemoveMenu"); // NOI18N
        jukeboxMenu.add(jukeboxRemoveMenu);

        setTitle(resourceMap.getString("prefsWindow.title")); // NOI18N
        setModalityType(java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        setName("prefsWindow"); // NOI18N
        setResizable(false);

        saveBtn.setAction(actionMap.get("saveBtnClicked")); // NOI18N
        saveBtn.setText(resourceMap.getString("saveBtn.text")); // NOI18N
        saveBtn.setName("saveBtn"); // NOI18N

        jTabbedPane1.setName("jTabbedPane1"); // NOI18N

        jPanel1.setName("jPanel1"); // NOI18N

        libLbl.setText(resourceMap.getString("libLbl.text")); // NOI18N
        libLbl.setName("libLbl"); // NOI18N

        libTxt.setEditable(false);
        libTxt.setText(resourceMap.getString("libTxt.text")); // NOI18N
        libTxt.setToolTipText(resourceMap.getString("libTxt.toolTipText")); // NOI18N
        libTxt.setComponentPopupMenu(libMenu);
        libTxt.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        libTxt.setName("libTxt"); // NOI18N
        libTxt.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                libTxtMouseClicked(evt);
            }
        });

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        propsTxt.setEditable(false);
        propsTxt.setText(resourceMap.getString("propsTxt.text")); // NOI18N
        propsTxt.setToolTipText(resourceMap.getString("propsTxt.toolTipText")); // NOI18N
        propsTxt.setComponentPopupMenu(propsMenu);
        propsTxt.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        propsTxt.setName("propsTxt"); // NOI18N
        propsTxt.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                propsTxtMouseClicked(evt);
            }
        });

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        lstJukebox.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lstJukebox.setComponentPopupMenu(jukeboxMenu);
        lstJukebox.setName("lstJukebox"); // NOI18N
        lstJukebox.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                lstJukeboxValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(lstJukebox);

        jLabel6.setText(resourceMap.getString("jLabel6.text")); // NOI18N
        jLabel6.setName("jLabel6"); // NOI18N

        jukeboxNameTxt.setText(resourceMap.getString("jukeboxNameTxt.text")); // NOI18N
        jukeboxNameTxt.setName("jukeboxNameTxt"); // NOI18N

        schedToggle.setText(resourceMap.getString("schedToggle.text")); // NOI18N
        schedToggle.setToolTipText(resourceMap.getString("schedToggle.toolTipText")); // NOI18N
        schedToggle.setName("schedToggle"); // NOI18N

        jLabel7.setText(resourceMap.getString("jLabel7.text")); // NOI18N
        jLabel7.setName("jLabel7"); // NOI18N

        nmtList.setAction(actionMap.get("changeSelectedNMT")); // NOI18N
        nmtList.setName("nmtList"); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(libLbl)
                            .addComponent(jLabel6)
                            .addComponent(jLabel1)
                            .addComponent(jLabel7))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jukeboxNameTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 228, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(libTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                    .addComponent(nmtList, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addGap(150, 150, 150))
                                .addComponent(propsTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addComponent(schedToggle))
                .addGap(55, 55, 55))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel6)
                            .addComponent(jukeboxNameTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(libLbl)
                            .addComponent(libTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(propsTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel7)
                            .addComponent(nmtList, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(schedToggle)))
                .addContainerGap(14, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(resourceMap.getString("jPanel1.TabConstraints.tabTitle"), jPanel1); // NOI18N

        jPanel7.setName("jPanel7"); // NOI18N

        bgAutoType.add(radAutoNever);
        radAutoNever.setSelected(true);
        radAutoNever.setText(resourceMap.getString("radAutoNever.text")); // NOI18N
        radAutoNever.setName("radAutoNever"); // NOI18N
        radAutoNever.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radAutoNeverActionPerformed(evt);
            }
        });

        jLabel8.setText(resourceMap.getString("jLabel8.text")); // NOI18N
        jLabel8.setName("jLabel8"); // NOI18N

        bgAutoType.add(radAutoDaily);
        radAutoDaily.setText(resourceMap.getString("radAutoDaily.text")); // NOI18N
        radAutoDaily.setName("radAutoDaily"); // NOI18N
        radAutoDaily.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radAutoDailyActionPerformed(evt);
            }
        });

        bgAutoType.add(radAutoCustom);
        radAutoCustom.setText(resourceMap.getString("radAutoCustom.text")); // NOI18N
        radAutoCustom.setName("radAutoCustom"); // NOI18N
        radAutoCustom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radAutoCustomActionPerformed(evt);
            }
        });

        spinAutoDaily.setModel(new javax.swing.SpinnerDateModel(new java.util.Date(), null, null, java.util.Calendar.MINUTE));
        spinAutoDaily.setName("spinAutoDaily"); // NOI18N

        spinAutoCustom.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(1), Integer.valueOf(1), null, Integer.valueOf(1)));
        spinAutoCustom.setName("spinAutoCustom"); // NOI18N

        cmbAutoCustom.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Minutes", "Hours" }));
        cmbAutoCustom.setName("cmbAutoCustom"); // NOI18N

        jPanel8.setName("jPanel8"); // NOI18N

        jLabel9.setText(resourceMap.getString("jLabel9.text")); // NOI18N
        jLabel9.setName("jLabel9"); // NOI18N

        lblProcessTime.setText(resourceMap.getString("lblProcessTime.text")); // NOI18N
        lblProcessTime.setName("lblProcessTime"); // NOI18N

        btnAutoRestart.setAction(actionMap.get("scheduleUpdate")); // NOI18N
        btnAutoRestart.setText(resourceMap.getString("btnAutoRestart.text")); // NOI18N
        btnAutoRestart.setName("btnAutoRestart"); // NOI18N

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel8Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel9)
                            .addComponent(lblProcessTime)))
                    .addGroup(jPanel8Layout.createSequentialGroup()
                        .addGap(49, 49, 49)
                        .addComponent(btnAutoRestart)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblProcessTime)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnAutoRestart)
                .addContainerGap(21, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel8)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel7Layout.createSequentialGroup()
                                .addComponent(radAutoCustom)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinAutoCustom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cmbAutoCustom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(radAutoNever)
                            .addGroup(jPanel7Layout.createSequentialGroup()
                                .addComponent(radAutoDaily)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(spinAutoDaily, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(18, 18, 18)
                        .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(150, Short.MAX_VALUE))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(radAutoNever)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(radAutoDaily)
                            .addComponent(spinAutoDaily, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(radAutoCustom)
                            .addComponent(spinAutoCustom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(cmbAutoCustom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(37, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(resourceMap.getString("jPanel7.TabConstraints.tabTitle"), jPanel7); // NOI18N

        jPanel5.setName("jPanel5"); // NOI18N

        chkJukePreserve.setAction(actionMap.get("togglePreserveJukebox")); // NOI18N
        chkJukePreserve.setText(resourceMap.getString("chkJukePreserve.text")); // NOI18N
        chkJukePreserve.setToolTipText(resourceMap.getString("chkJukePreserve.toolTipText")); // NOI18N
        chkJukePreserve.setName("chkJukePreserve"); // NOI18N

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(chkJukePreserve)
                .addContainerGap(424, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(chkJukePreserve)
                .addContainerGap(138, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(resourceMap.getString("jPanel5.TabConstraints.tabTitle"), jPanel5); // NOI18N

        jPanel3.setName("jPanel3"); // NOI18N

        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N

        btnConvert.setAction(actionMap.get("startConvert")); // NOI18N
        btnConvert.setText(resourceMap.getString("btnConvert.text")); // NOI18N
        btnConvert.setName("btnConvert"); // NOI18N

        spinHashdepth.setModel(new javax.swing.SpinnerNumberModel());
        spinHashdepth.setName("spinHashdepth"); // NOI18N

        jLabel5.setText(resourceMap.getString("jLabel5.text")); // NOI18N
        jLabel5.setName("jLabel5"); // NOI18N

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addGap(18, 18, 18)
                        .addComponent(spinHashdepth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnConvert)))
                .addContainerGap(85, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(spinHashdepth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnConvert))
                .addContainerGap(114, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(resourceMap.getString("jPanel3.TabConstraints.tabTitle"), jPanel3); // NOI18N

        jPanel4.setName("jPanel4"); // NOI18N

        backupXml.setText(resourceMap.getString("backupXml.text")); // NOI18N
        backupXml.setName("backupXml"); // NOI18N
        backupXml.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backupXmlActionPerformed(evt);
            }
        });

        backupPosters.setText(resourceMap.getString("backupPosters.text")); // NOI18N
        backupPosters.setName("backupPosters"); // NOI18N
        backupPosters.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backupPostersActionPerformed(evt);
            }
        });

        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N

        txtBackupPath.setEditable(false);
        txtBackupPath.setText(resourceMap.getString("txtBackupPath.text")); // NOI18N
        txtBackupPath.setName("txtBackupPath"); // NOI18N

        btnBackupPath.setAction(actionMap.get("chooseBackupClicked")); // NOI18N
        btnBackupPath.setText(resourceMap.getString("btnBackupPath.text")); // NOI18N
        btnBackupPath.setToolTipText(resourceMap.getString("btnBackupPath.toolTipText")); // NOI18N
        btnBackupPath.setName("btnBackupPath"); // NOI18N

        btnBackup.setAction(actionMap.get("startBackup")); // NOI18N
        btnBackup.setText(resourceMap.getString("btnBackup.text")); // NOI18N
        btnBackup.setToolTipText(resourceMap.getString("btnBackup.toolTipText")); // NOI18N
        btnBackup.setName("btnBackup"); // NOI18N

        backupFanart.setText(resourceMap.getString("backupFanart.text")); // NOI18N
        backupFanart.setName("backupFanart"); // NOI18N
        backupFanart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backupFanartActionPerformed(evt);
            }
        });

        backupBanners.setText(resourceMap.getString("backupBanners.text")); // NOI18N
        backupBanners.setName("backupBanners"); // NOI18N
        backupBanners.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backupBannersActionPerformed(evt);
            }
        });

        backupVideoImages.setText(resourceMap.getString("backupVideoImages.text")); // NOI18N
        backupVideoImages.setName("backupVideoImages"); // NOI18N

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(backupXml)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(backupPosters)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(backupFanart)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(backupBanners)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(backupVideoImages))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtBackupPath, javax.swing.GroupLayout.PREFERRED_SIZE, 272, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnBackupPath)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnBackup)))
                .addContainerGap(30, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(backupXml)
                    .addComponent(backupPosters)
                    .addComponent(backupFanart)
                    .addComponent(backupBanners)
                    .addComponent(backupVideoImages))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(txtBackupPath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnBackupPath)
                    .addComponent(btnBackup))
                .addContainerGap(108, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(resourceMap.getString("jPanel4.TabConstraints.tabTitle"), jPanel4); // NOI18N

        jPanel2.setName("jPanel2"); // NOI18N

        newVerChk.setText(resourceMap.getString("newVerChk.text")); // NOI18N
        newVerChk.setToolTipText(resourceMap.getString("newVerChk.toolTipText")); // NOI18N
        newVerChk.setName("newVerChk"); // NOI18N

        httpChk.setAction(actionMap.get("toggleHttpRequests")); // NOI18N
        httpChk.setText(resourceMap.getString("httpChk.text")); // NOI18N
        httpChk.setToolTipText(resourceMap.getString("httpChk.toolTipText")); // NOI18N
        httpChk.setName("httpChk"); // NOI18N

        portTxt.setText(resourceMap.getString("portTxt.text")); // NOI18N
        portTxt.setName("portTxt"); // NOI18N

        newVerDailyChk.setAction(actionMap.get("scheduleVersionCheck")); // NOI18N
        newVerDailyChk.setText(resourceMap.getString("newVerDailyChk.text")); // NOI18N
        newVerDailyChk.setToolTipText(resourceMap.getString("newVerDailyChk.toolTipText")); // NOI18N
        newVerDailyChk.setName("newVerDailyChk"); // NOI18N

        mkvtoolnixLabel.setForeground(resourceMap.getColor("mkvtoolnixLabel.foreground")); // NOI18N
        mkvtoolnixLabel.setText(resourceMap.getString("mkvtoolnixLabel.text")); // NOI18N
        mkvtoolnixLabel.setToolTipText(resourceMap.getString("mkvtoolnixLabel.toolTipText")); // NOI18N
        mkvtoolnixLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        mkvtoolnixLabel.setName("mkvtoolnixLabel"); // NOI18N
        mkvtoolnixLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                mkvtoolnixLabelMouseClicked(evt);
            }
        });

        mkvtoolnixTxt.setEditable(false);
        mkvtoolnixTxt.setText(resourceMap.getString("mkvtoolnixTxt.text")); // NOI18N
        mkvtoolnixTxt.setName("mkvtoolnixTxt"); // NOI18N

        mkvtoolnixBtn.setAction(actionMap.get("chooseMkvtoolnixFolder")); // NOI18N
        mkvtoolnixBtn.setText(resourceMap.getString("mkvtoolnixBtn.text")); // NOI18N
        mkvtoolnixBtn.setName("mkvtoolnixBtn"); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(newVerChk)
                    .addComponent(newVerDailyChk)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(httpChk)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(portTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(mkvtoolnixLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(mkvtoolnixTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 285, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(mkvtoolnixBtn)))
                .addContainerGap(89, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(newVerChk)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(newVerDailyChk)
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(httpChk)
                    .addComponent(portTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(mkvtoolnixLabel)
                    .addComponent(mkvtoolnixTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(mkvtoolnixBtn))
                .addContainerGap(30, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(resourceMap.getString("jPanel2.TabConstraints.tabTitle"), jPanel2); // NOI18N

        jPanel6.setName("jPanel6"); // NOI18N

        btnConvertNMJ.setAction(actionMap.get("btnConvertNMJClicked")); // NOI18N
        btnConvertNMJ.setText(resourceMap.getString("btnConvertNMJ.text")); // NOI18N
        btnConvertNMJ.setName("btnConvertNMJ"); // NOI18N

        chkAutoNMJ.setAction(actionMap.get("toggleAutoNMJ")); // NOI18N
        chkAutoNMJ.setText(resourceMap.getString("chkAutoNMJ.text")); // NOI18N
        chkAutoNMJ.setToolTipText(resourceMap.getString("chkAutoNMJ.toolTipText")); // NOI18N
        chkAutoNMJ.setName("chkAutoNMJ"); // NOI18N

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(chkAutoNMJ)
                    .addComponent(btnConvertNMJ))
                .addContainerGap(422, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnConvertNMJ)
                .addGap(18, 18, 18)
                .addComponent(chkAutoNMJ)
                .addContainerGap(93, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(resourceMap.getString("jPanel6.TabConstraints.tabTitle"), jPanel6); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(276, 276, 276)
                .addComponent(saveBtn))
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 566, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 196, Short.MAX_VALUE)
                .addGap(11, 11, 11)
                .addComponent(saveBtn)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void backupBannersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backupBannersActionPerformed
        checkBackupAllowed();
}//GEN-LAST:event_backupBannersActionPerformed

    private void backupFanartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backupFanartActionPerformed
        checkBackupAllowed();
}//GEN-LAST:event_backupFanartActionPerformed

    private void backupPostersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backupPostersActionPerformed
        checkBackupAllowed();
}//GEN-LAST:event_backupPostersActionPerformed

    private void backupXmlActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backupXmlActionPerformed
        checkBackupAllowed();
}//GEN-LAST:event_backupXmlActionPerformed

    private void lstJukeboxValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lstJukeboxValueChanged
        if (getSelectedJukebox() != null && lstJukebox.getSelectedIndex() != prevSelIndex) {
            setControlsFromSelectedJukebox();
            prevSelIndex = lstJukebox.getSelectedIndex();
            resetProperties();
        } else if (getSelectedJukebox() == null) {
            lstJukebox.setSelectedIndex(prevSelIndex);
        }
        //toggleSchedule();
}//GEN-LAST:event_lstJukeboxValueChanged

    private void propsTxtMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_propsTxtMouseClicked
        javax.swing.JPopupMenu menu = propsTxt.getComponentPopupMenu();
        if (menu != null) {
            menu.show(propsTxt, evt.getX(), evt.getY());
        }
}//GEN-LAST:event_propsTxtMouseClicked

    private void libTxtMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_libTxtMouseClicked
        javax.swing.JPopupMenu menu = libTxt.getComponentPopupMenu();
        if (menu != null) {
            menu.show(libTxt, evt.getX(), evt.getY());
        }
}//GEN-LAST:event_libTxtMouseClicked

    private void radAutoNeverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radAutoNeverActionPerformed
        autoButtonChanged();
    }//GEN-LAST:event_radAutoNeverActionPerformed

    private void radAutoDailyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radAutoDailyActionPerformed
        autoButtonChanged();
    }//GEN-LAST:event_radAutoDailyActionPerformed

    private void radAutoCustomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radAutoCustomActionPerformed
        autoButtonChanged();
    }//GEN-LAST:event_radAutoCustomActionPerformed

    private void mkvtoolnixLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mkvtoolnixLabelMouseClicked
        try {
            java.awt.Desktop.getDesktop().browse(new URI("http://www.bunkus.org/videotools/mkvtoolnix/"));
        } catch (Exception ex) {
            YAYManView.logger.severe("Error launching mkvtoolnix website: "+ex);
        }
    }//GEN-LAST:event_mkvtoolnixLabelMouseClicked

    private void checkBackupAllowed() {
        btnBackup.setEnabled(!txtBackupPath.getText().equals("") && (backupXml.isSelected() || backupPosters.isSelected() || backupFanart.isSelected() || backupBanners.isSelected()));
    }
    /**
    * @param args the command line arguments
    */
    /*public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                YAYManPrefs dialog = new YAYManPrefs(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });

                dialog.setVisible(true);
            }
        });
    }*/

    @Action
    public void saveBtnClicked() {
        if (webAPI != null) webAPI.stopRunning();
        savePropertiesToDisk();
        resetProperties();
        mainFrame.setJukeboxOptions((DefaultListModel)lstJukebox.getModel(), lstJukebox.getSelectedIndex());
        this.setVisible(false);
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        mainFrame.displayInit();
        if (httpChk.isSelected()) webAPI = new WebAPI(mainFrame, portTxt.getText());
        if (doScheduleUpdate) scheduleUpdate();
    }

    private void savePropertiesToDisk() {
        new MySwingWorker() {
            @Override
            public Void doInBackground() {
                Document dom;
                DocumentBuilder db;
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                try {
                    db = dbf.newDocumentBuilder();
                    dom = db.newDocument();

                    Element rootEle = dom.createElement("Settings");
                    rootEle.setAttribute("selected", ""+lstJukebox.getSelectedIndex());
                    rootEle.setAttribute("checkVersionStartup", ""+newVerChk.isSelected());
                    rootEle.setAttribute("checkVersionDaily", ""+newVerDailyChk.isSelected());
                    rootEle.setAttribute("enableHttpRequests", ""+httpChk.isSelected());
                    rootEle.setAttribute("httpPort", portTxt.getText());
                    rootEle.setAttribute("mkvtoolnixFolder", mkvtoolnixTxt.getText());
                    
                    String schedType = "Never";
                    if (radAutoDaily.isSelected()) {
                        schedType = "Daily";
                    } else if (radAutoCustom.isSelected()) {
                        schedType = "Custom";
                    }
                    rootEle.setAttribute("scheduleType", schedType);
                    
                    Calendar cal = Calendar.getInstance();
                    cal.setTime((Date)spinAutoDaily.getValue());
                    cal.set(Calendar.MILLISECOND, 0);
                    cal.set(Calendar.SECOND, 0);
                    rootEle.setAttribute("scheduleDailyTime", ""+cal.getTime().getTime());
                    
                    rootEle.setAttribute("scheduleCustomValue", spinAutoCustom.getValue().toString());
                    
                    rootEle.setAttribute("scheduleCustomUnit", ""+cmbAutoCustom.getSelectedIndex());
                            
                    dom.appendChild(rootEle);

                    for (int i = 0; i < lstJukebox.getModel().getSize(); i++) {
                        SavedJukebox jb = (SavedJukebox)lstJukebox.getModel().getElementAt(i);
                        Element yamjEle = dom.createElement("YAMJ");
                        yamjEle.setAttribute("name", jb.getName());
                        yamjEle.setAttribute("nmt", jb.getNmt().getName());
                        yamjEle.setAttribute("preserveJukebox", ""+chkJukePreserve.isSelected());
                        yamjEle.setAttribute("autoNMJ", ""+chkAutoNMJ.isSelected());

                        //if (i == lstJukebox.getSelectedIndex()) yamjEle.setAttribute("selected", "true");

                        Element yamjLibEle = dom.createElement("Library");
                        Text yamjLibText = dom.createTextNode(jb.getLibraryFile());
                        yamjLibEle.appendChild(yamjLibText);
                        yamjEle.appendChild(yamjLibEle);

                        Element yamjPropsEle = dom.createElement("Properties");
                        Text yamjPropsText = dom.createTextNode(jb.getPropertiesFile());
                        yamjPropsEle.appendChild(yamjPropsText);
                        yamjEle.appendChild(yamjPropsEle);

                        Element schedEle = dom.createElement("Schedule");
                        schedEle.setAttribute("enabled", ""+jb.isScheduled());
                        //schedEle.setAttribute("hour", ""+jb.getScheduleHour());
                        //schedEle.setAttribute("minute", ""+jb.getScheduleMinute());

                        yamjEle.appendChild(schedEle);

                        rootEle.appendChild(yamjEle);
                    }

                    saveSettings(dom);
                    
                    scheduleUpdate();

                } catch(Exception pce) {
                    YAYManView.logger.severe("Error saving properties file: "+pce);
                    System.exit(1);
                }
                return null;
            }
        }.execute();
    }

    private void saveSettings(Document doc) throws TransformerConfigurationException, TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(doc);
        StreamResult result =  new StreamResult(settingsName);
        transformer.transform(source, result);
        //mainFrame.displayInit();
    }

    public void resetProperties() {
        setProperty("yayman.libraryFile",libTxt.getText());
        setProperty("yayman.propertiesFile",propsTxt.getText());
        setDefaultProperties();
        setUserProperties(propsTxt.getText());
        setSkinProperties();
        setApiKeys();
        if (Boolean.parseBoolean(getProperty("mjb.skipHtmlGeneration","false"))) {
            //Find XML Files
            JukeboxInterface.findLibXMLFiles();
        }
    }

    private void setDefaultProperties() {
        if (!setPropertiesStreamName("./properties/moviejukebox-default.properties")) {
            JOptionPane.showMessageDialog(this,
                "Your YAMJ is hosed, son.",
                "Could not load default properties",
                JOptionPane.ERROR_MESSAGE);
            YAYManView.logger.severe("Could not find default properties file. Exiting.");
            dispose();
            mainFrame.exit();
        }
    }

    private boolean setUserProperties(String propsPath) {
         if (!setPropertiesStreamName(propsPath)) {
             YAYManView.logger.warning("Warning: Could not find user properties file "+propsTxt.getText());
             checkNullJukeboxRoot();
             return false;
         }
         checkNullJukeboxRoot();
         return true;
    }

    private void setSkinProperties() {
        String skinHome = getProperty("mjb.skin.dir", "./skins/default");
        if (!setPropertiesStreamName(skinHome + File.separator + "skin.properties")) {
            JOptionPane.showMessageDialog(this,
                "Could not load properties for the skin specified in "+propsTxt.getText()+".",
                "Skin not found",
                JOptionPane.ERROR_MESSAGE);
            YAYManView.logger.warning("Warning: Could not load properties for the skin specified in "+propsTxt.getText()+".");
        }
        setPropertiesStreamName(skinHome + "/skin-user.properties");
    }

    private void setApiKeys() {
        if (!setPropertiesStreamName("./properties/apikeys.properties")) {
            JOptionPane.showMessageDialog(this,
                "Could not load ./properties/apikeys.properties.",
                "Could not load API keys",
                JOptionPane.ERROR_MESSAGE);
            YAYManView.logger.severe("Could not load ./properties/apikeys.properties. Exiting.");
            dispose();
            System.exit(1);
        }
    }

    private void checkNullJukeboxRoot() {
        String root = getProperty("mjb.jukeboxRoot");
        if (root == null) {
            String cmdPath = getPathFromCmdFile();
            if (cmdPath != null) setProperty("mjb.jukeboxRoot",cmdPath);
        }
    }

    public String getPathFromCmdFile() {
        File cmdFile = new File("My_YAMJ.cmd");
        String path = null;
        if (cmdFile.exists()) {
            try {
                BufferedReader input =  new BufferedReader(new FileReader(cmdFile));
                try {
                    String line = null;
                    while (( line = input.readLine()) != null){
                        if (line.startsWith("CALL ")) {
                            String[] parts = line.split(" ");
                            for (int i = 0; i < parts.length; i++) {
                                String part = parts[i];
                                if (part.equals("-o")) {
                                    path = parts[i+1];
                                    if (path.contains("\"") && !path.endsWith("\"")) {
                                        for (int j = i+2; j < parts.length; j++) {
                                            path += " "+parts[j];
                                            if (parts[j].contains("\"")) {
                                                path = path.replaceAll("\"", "");
                                                path = path.replace("\\", "/");
                                                break;
                                            }
                                        }
                                    } else if (path.contains("\"")) path = path.replaceAll("\"", "");
                                    break;
                                }
                            }
                        }
                    }
                }
                finally {
                    input.close();
                }
            }
            catch (IOException ex){
                ex.printStackTrace();
            }
        }
        return path;
    }

    @Action
    public void chooseLibraryClicked() {
        libraryChoose();
    }

    public void libraryChoose() {
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("Select YAMJ Library File");
        File fileTest = new File(libTxt.getText());
        if (fileTest.exists()) {
            fileChooser.setCurrentDirectory(fileTest.getParentFile());
            fileChooser.setSelectedFile(fileTest);
        }
        if (fileChooser.showOpenDialog(chooseLibMenuItem) == JFileChooser.APPROVE_OPTION) {
            libTxt.setText(fileChooser.getSelectedFile().getAbsolutePath());
            setProperty("yayman.libraryFile",libTxt.getText());
            getSelectedJukebox().setLibraryFile(libTxt.getText());
        }
    }

    @Action
    public void choosePropsClicked() {
        propsChoose();
    }

    private void propsChoose() {
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("Select properties file");
        File fileTest = new File(propsTxt.getText());
        if (fileTest.exists()) {
            fileChooser.setSelectedFile(fileTest);
        }
        if (fileChooser.showOpenDialog(choosePropsMenuItem) == JFileChooser.APPROVE_OPTION) {
            propsTxt.setText(fileChooser.getSelectedFile().getPath());
            getSelectedJukebox().setPropertiesFile(propsTxt.getText());
            resetProperties();
        }
    }

    @Action
    public void editPropertiesClicked() {
        EditPropertiesFrame editProps = new EditPropertiesFrame(this, true);
        editProps.setLocationRelativeTo(this);
        editProps.setVisible(true);
    }

    @Action
    public void editLibraryClicked() {
        EditLibraryFrame editLib = new EditLibraryFrame(mainFrame.getFrame(), true, getSelectedJukebox());
        editLib.setLocationRelativeTo(this);
        editLib.setVisible(true);
    }

    public String getDefaultPropsPath(String path) {
        String fullPath = path;
        if (!fullPath.endsWith(File.separator)) {
            fullPath += File.separator;
        }
        fullPath += "properties"+File.separator+"moviejukebox-default.properties";
        return fullPath;
    }

    public YAYManView getMainFrame() {
        return mainFrame;
    }
    
    public SavedJukebox getSelectedJukebox() {
        if (lstJukebox.getSelectedIndex() == -1) {
            return null;
        }
        return (SavedJukebox)lstJukebox.getSelectedValue();
    }
    
    private void setControlsFromSelectedJukebox() {
        setControlsFromJukebox(getSelectedJukebox());
    }
    
    private void setControlsFromJukebox(SavedJukebox jb) {
        if (jb == null) return;
        jukeboxNameTxt.setText(jb.getName());
        propsTxt.setText(jb.getPropertiesFile());
        libTxt.setText(jb.getLibraryFile());
        schedToggle.setSelected(jb.isScheduled());
        chkJukePreserve.setSelected(jb.getPreserveJukebox());
        chkAutoNMJ.setSelected(jb.doesNMJAutoConvert());
        toggleAutoNMJ();
        if (jb.getNmt() != null) {
            for (int i = 0; i < nmtListModel.getSize(); i++) {
                NetworkedMediaTank nmt = (NetworkedMediaTank)nmtListModel.getElementAt(i);
                if (jb.getNmt().getName().equals(nmt.getName())) {
                    nmtList.setSelectedIndex(i);
                    break;
                }
            }
        }

        EditPropertiesFrame editProps = new EditPropertiesFrame(this, true);
        boolean propsDirty = false;
        if (jb.getCleanJukebox()) {
            editProps.setProperty("mjb.jukeboxClean", "true");
            propsDirty = true;
        }

        if (jb.getSkipIndexGeneration()) {
            editProps.setProperty("mjb.skipIndexGeneration", "true");
            propsDirty = true;
        }

        if (propsDirty) editProps.closeSave();
        editProps.dispose();
    }

    @Action
    public void startConvert() {
        if (JOptionPane.showConfirmDialog(this, "Are you sure you want to conduct a hash conversion?", "Confirm hash conversion", JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
            SpinnerNumberModel model = (SpinnerNumberModel)spinHashdepth.getModel();
            mainFrame.convertHashdepth(model.getNumber().intValue());
            this.setVisible(false);
        }
    }

    public void setPrevHashdepth(int depth) {
        spinHashdepth.setValue(depth);
    }

    @Action
    public void chooseBackupClicked() {
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Select backup folder");
        File fileTest = new File(txtBackupPath.getText());
        if (fileTest.exists()) {
            //fileChooser.setSelectedFile(fileTest);
            fileChooser.setCurrentDirectory(fileTest);
        } else {
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        }
        if (fileChooser.showOpenDialog(btnBackupPath) == JFileChooser.APPROVE_OPTION) {
            txtBackupPath.setText(fileChooser.getSelectedFile().getPath());
        }
        checkBackupAllowed();
    }

    @Action
    public void startBackup() {
        mainFrame.backupJukeboxFiles(backupXml.isSelected(), backupPosters.isSelected(), backupFanart.isSelected(), backupBanners.isSelected(), backupVideoImages.isSelected(), txtBackupPath.getText());
        this.setVisible(false);
    }

    @Action
    public void removeSelectedJukebox() {
        logger.fine("Removed "+getSelectedJukebox().getName()+" jukebox");
        DefaultListModel model = (DefaultListModel)lstJukebox.getModel();//new DefaultListModel();
        model.removeElementAt(lstJukebox.getSelectedIndex());
        lstJukebox.setSelectedIndex(0);
        //setScheduledTimers();
    }

    @Action
    public void addNewJukebox() {
        SavedJukebox jukebox = new SavedJukebox();
        DefaultListModel model = (DefaultListModel)lstJukebox.getModel();
        model.addElement(jukebox);
        lstJukebox.setSelectedIndex(model.getSize()-1);
        jukeboxRemoveMenu.setEnabled(true);
        logger.fine("Added new jukebox");
    }

    public void setSelectedJukebox(int i) {
        lstJukebox.setSelectedIndex(i);
        mainFrame.displayInit();
        savePropertiesToDisk();
        logger.fine("Switched to "+getSelectedJukebox().getName()+" jukebox");
    }

    @Action
    public void AddNewWizJukebox() {
        DefaultListModel model = (DefaultListModel)lstJukebox.getModel();
        SetupWizardFrame wizard = new SetupWizardFrame(this);
        wizard.setVisible(true);
    }

    @Action
    public void togglePreserveJukebox() {
        setProperty("yayman.preserveJukebox", ""+chkJukePreserve.isSelected());
    }

    @Action
    public void btnConvertNMJClicked() {
        try {
            new MySwingWorker(mainFrame.getProgressBar()) {
                @Override
                protected Void doInBackground() {
                    List<NMTLibrary> libraries = JukeboxInterface.getLibraries(libTxt.getText());
                    Map<String, String> propMap = new HashMap();
                    propMap.put("yamj.jukeboxPath", JukeboxInterface.getFullDetailsPath());
                    propMap.put("tonmjdb.source", "yamj");

                    //Log log = LogFactory.getLog(VideoToNmjDb.class);

                    for (int i = 0; i < libraries.size(); i++) {
                        NMTLibrary nmtLib = libraries.get(i);
                        propMap.put("yamj.nmjPath", nmtLib.getPlayerPath());
                        propMap.put("tonmjdb.nmjDbPath", nmtLib.getPath());

                        showProcessing(true);
                        mainFrame.enableControls(false);
                        mainFrame.setProcessing(true);
                        try {
                            new com.syabas.tonmjdb.VideoToNmjDb(propMap) {
                                    @Override
                                    protected String getDbExistAction() throws Throwable
                                    {
                                            int action = JOptionPane.showOptionDialog(null,
                                                    "NMJ DB exists. Action?", "NMJ DB Exists",
                                                    JOptionPane.OK_CANCEL_OPTION,
                                                    JOptionPane.QUESTION_MESSAGE, null,
                                                    new String[] { "Quit", "Merge", "Overwrite" },
                                                    "Quit");

                                            switch (action) {
                                                case 1:
                                                    return com.syabas.tonmjdb.ToNmjDb.DB_EXIST_MERGE;
                                                case 2:
                                                    return com.syabas.tonmjdb.ToNmjDb.DB_EXIST_OVERWRITE;
                                            }

                                            return com.syabas.tonmjdb.ToNmjDb.DB_EXIST_QUIT;
                                    }
                            };

                        } catch (Throwable ex) {
                            YAYManView.logger.severe("Error converting to NMJ: "+ex);
                        }
                        showProcessing(false);
                        mainFrame.enableControls(true);
                        mainFrame.setProcessing(false);
                    }
                    return null;
                    }
                }.execute();
        } catch (Exception ex) {
            YAYManView.logger.severe("Error converting to NMJ: "+ex);
        }
    }

    @Action
    public void toggleAutoNMJ() {
        setProperty("yayman.autoNMJ", ""+chkAutoNMJ.isSelected());
    }

    @Action
    public void changeSelectedNMT() {
        if (nmtListModel.getSize() > 0) {
            NetworkedMediaTank selnmt = (NetworkedMediaTank)nmtListModel.getSelectedItem();
            getSelectedJukebox().setNmt(new NetworkedMediaTank(selnmt.getName(), selnmt.getAddress()));
        }
    }

    /*public DefaultComboBoxModel getNmtModel() {
        return nmtListModel;
    }*/

    @Action
    public void toggleHttpRequests() {
        portTxt.setEnabled(httpChk.isSelected());
    }

    public void addJukebox(SavedJukebox jb) {
        jbListModel.addElement(jb);
        if (lstJukebox.getSelectedIndex() == -1) lstJukebox.setSelectedIndex(0);
    }
    
    public void autoButtonChanged() {
        spinAutoDaily.setEnabled(false);
        spinAutoCustom.setEnabled(false);
        cmbAutoCustom.setEnabled(false);
        if (radAutoNever.isSelected()) {
            lblProcessTime.setText("Never");
        } else if (radAutoDaily.isSelected()) {
            spinAutoDaily.setEnabled(true);
            doScheduleUpdate = true;
        } else if (radAutoCustom.isSelected()) {
            spinAutoCustom.setEnabled(true);
            cmbAutoCustom.setEnabled(true);
            doScheduleUpdate = true;
        }
    }
    
    @Action
    public void scheduleUpdate() {
        doScheduleUpdate = false;
        jukeboxTimer.cancel();
        Calendar scheduledTime = Calendar.getInstance();
        scheduledTime.set(Calendar.MILLISECOND, 0);
        scheduledTime.set(Calendar.SECOND, 0);
        if (radAutoNever.isSelected()) {
            lblProcessTime.setText("Never");
            return;
        } else if (radAutoDaily.isSelected()) {
            Date date = (Date)spinAutoDaily.getValue();
            //Date now = new Date();
            int year = scheduledTime.get(Calendar.YEAR);
            int month = scheduledTime.get(Calendar.MONTH);
            int day = scheduledTime.get(Calendar.DATE);
            
            scheduledTime.setTime(date);
            scheduledTime.set(year, month, day);
            scheduledTime.set(Calendar.MILLISECOND, 0);
            scheduledTime.set(Calendar.SECOND, 0);
            if (!scheduledTime.after(Calendar.getInstance())) {
                scheduledTime.add(Calendar.DATE, 1);
            }
        } else if (radAutoCustom.isSelected()) {
            int interval = Integer.parseInt(spinAutoCustom.getValue().toString());
            int units = Calendar.MINUTE;
            if (cmbAutoCustom.getSelectedIndex() == 0) {
                units = Calendar.MINUTE;
            } else if (cmbAutoCustom.getSelectedIndex() == 1) {
                units = Calendar.HOUR;
            }
            scheduledTime.add(units, interval);
        }
        lblProcessTime.setText(scheduledTime.getTime().toString());
        jukeboxTimer = new Timer();
        jukeboxTimer.schedule(new ScheduleTask(), scheduledTime.getTime());
    }

    private class ScheduleTask extends TimerTask {
        public ScheduleTask() {
            super();
        }
        public void run() {
            int selectedJukeboxIndex = lstJukebox.getSelectedIndex();
            for (int i = 0; i < lstJukebox.getModel().getSize(); i++) {
                SavedJukebox jb = (SavedJukebox)lstJukebox.getModel().getElementAt(i);
                if (jb.isScheduled()) {
                    if (i != lstJukebox.getSelectedIndex()) lstJukebox.setSelectedIndex(i);
                    YAYManView.logger.fine("Automatically processing "+getSelectedJukebox().getName()+" jukebox.");
                    mainFrame.processAllVideos();
                }
            }
            if (lstJukebox.getSelectedIndex() != selectedJukeboxIndex) lstJukebox.setSelectedIndex(selectedJukeboxIndex);
            scheduleUpdate();
        }
    }
    
    @Action
    public void scheduleVersionCheck() {
        updateCheckTimer.cancel();
        if (newVerDailyChk.isSelected()) {
            Calendar scheduledTime = Calendar.getInstance();
            scheduledTime.add(Calendar.DATE, 1);
            updateCheckTimer = new Timer();
            updateCheckTimer.schedule(new ScheduleVersionCheckTask(), scheduledTime.getTime());
        } else {
            //nothing
        }
    }

    private class ScheduleVersionCheckTask extends TimerTask {
        public ScheduleVersionCheckTask() {
            super();
        }
        public void run() {
            mainFrame.updateYAYMan(false);
            scheduleVersionCheck();
        }
    }

    @Action
    public void chooseMkvtoolnixFolder() {
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Select mkvtoolnix folder");
        File fileTest = new File(mkvtoolnixTxt.getText());
        if (fileTest.exists()) {
            fileChooser.setSelectedFile(fileTest);
            fileChooser.setCurrentDirectory(fileTest);
        }
        if (fileChooser.showOpenDialog(mkvtoolnixBtn) == JFileChooser.APPROVE_OPTION) {
            setMTNFolder(fileChooser.getSelectedFile().getPath());
        }
    }
    
    private void setMTNFolder(String f) {
        mkvtoolnixTxt.setText(f);
        //getSelectedJukebox().setMkvtoolnixFolder(f);
        setProperty("yayman.mkvtoolnixFolder", f);
        mkvtoolnixTxt.setForeground(Color.red);
        
        File folder = new File(f);
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            boolean mkvinfo = false;
            boolean mkvextract = false;
            for (int i=0; i < files.length; i++) {
                File file = files[i];
                if (file.getName().compareTo("mkvinfo.exe") == 0) mkvinfo = true;
                if (file.getName().compareTo("mkvextract.exe") == 0) mkvextract = true;
                if (mkvinfo && mkvextract)  {
                    mkvtoolnixTxt.setForeground(Color.green);
                    break;
                }
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox backupBanners;
    private javax.swing.JCheckBox backupFanart;
    private javax.swing.JCheckBox backupPosters;
    private javax.swing.JCheckBox backupVideoImages;
    private javax.swing.JCheckBox backupXml;
    private javax.swing.ButtonGroup bgAutoType;
    private javax.swing.JButton btnAutoRestart;
    private javax.swing.JButton btnBackup;
    private javax.swing.JButton btnBackupPath;
    private javax.swing.JButton btnConvert;
    private javax.swing.JButton btnConvertNMJ;
    private javax.swing.JCheckBox chkAutoNMJ;
    private javax.swing.JCheckBox chkJukePreserve;
    private javax.swing.JMenuItem chooseLibMenuItem;
    private javax.swing.JMenuItem choosePropsMenuItem;
    private javax.swing.JComboBox cmbAutoCustom;
    private javax.swing.JMenuItem editLibMenuItem;
    private javax.swing.JMenuItem editPropsMenuItem;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JCheckBox httpChk;
    private javax.swing.JLabel jLabel1;
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
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JMenuItem jukeboxAddMenu;
    private javax.swing.JMenuItem jukeboxAddWizMenu;
    private javax.swing.JPopupMenu jukeboxMenu;
    private javax.swing.JTextField jukeboxNameTxt;
    private javax.swing.JMenuItem jukeboxRemoveMenu;
    private javax.swing.JLabel lblProcessTime;
    private javax.swing.JLabel libLbl;
    private javax.swing.JPopupMenu libMenu;
    private javax.swing.JTextField libTxt;
    private javax.swing.JList lstJukebox;
    private javax.swing.JButton mkvtoolnixBtn;
    private javax.swing.JLabel mkvtoolnixLabel;
    private javax.swing.JTextField mkvtoolnixTxt;
    private javax.swing.JCheckBox newVerChk;
    private javax.swing.JCheckBox newVerDailyChk;
    private javax.swing.JComboBox nmtList;
    private javax.swing.JTextField portTxt;
    private javax.swing.JPopupMenu propsMenu;
    private javax.swing.JTextField propsTxt;
    private javax.swing.JRadioButton radAutoCustom;
    private javax.swing.JRadioButton radAutoDaily;
    private javax.swing.JRadioButton radAutoNever;
    private javax.swing.JButton saveBtn;
    private javax.swing.JCheckBox schedToggle;
    private javax.swing.JSpinner spinAutoCustom;
    private javax.swing.JSpinner spinAutoDaily;
    private javax.swing.JSpinner spinHashdepth;
    private javax.swing.JTextField txtBackupPath;
    // End of variables declaration//GEN-END:variables

}
