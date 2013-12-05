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
 * EditPropertiesFrame.java
 *
 * Created on Jan 31, 2010, 12:51:40 PM
 */

package yayman;


import javax.xml.parsers.*;
import org.jdesktop.application.Action;
import org.w3c.dom.*;
import javax.swing.tree.*;
import javax.xml.xpath.*;
import java.util.*;
import java.io.*;
import java.io.FileReader;
import java.io.File;
import javax.swing.event.*;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import java.net.URI;
import java.util.logging.*;

import static com.moviejukebox.tools.PropertiesUtil.getProperty;

public class EditPropertiesFrame extends javax.swing.JDialog {

    private Hashtable<String,String> userProps;
    private Hashtable<String,String> defaultProps;
    private ArrayList<String> xmlProps;
    private ArrayList<String> disabledProps;
    private boolean changingSelection;
    private YAYManPrefs prefsWindow;
    private String propsPath;
    private String moreInfoRoot;
    private DefaultTreeModel allProps;
    private String currentMode;

    public static String PropertiesMode = "Properties";
    public static String SkinPropertiesMode = "SkinProperties";

    public Logger logger = Logger.getLogger("yayman");

    /** Creates new form EditPropertiesFrame */
    /*public EditPropertiesFrame(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
    }*/

    public EditPropertiesFrame(YAYManPrefs parent, boolean modal) {
        this(parent,modal,PropertiesMode);
    }
    
    public EditPropertiesFrame(YAYManPrefs parent, boolean modal, String mode) {
        super(parent.getMainFrame().getFrame(),modal);
        initComponents();
        prefsWindow = parent;
        currentMode = mode;

        userProps = new Hashtable();
        defaultProps = new Hashtable();
        xmlProps = new ArrayList();
        disabledProps = new ArrayList();

        SavedJukebox jb = parent.getSelectedJukebox();

        initialize(jb);
    }
    
    public EditPropertiesFrame(YAYManPrefs parent, SavedJukebox jb, boolean modal, String mode) {
        super(parent.getMainFrame().getFrame(),modal);
        initComponents();
        prefsWindow = parent;
        currentMode = mode;

        userProps = new Hashtable();
        defaultProps = new Hashtable();
        xmlProps = new ArrayList();
        disabledProps = new ArrayList();

        initialize(jb);
    }

    private void initialize(SavedJukebox jb) {
        String propsDefPath, xmlPath = null;

        moreInfoRoot = "http://code.google.com/p/moviejukebox/wiki/PropertiesConfiguration#";
        if (currentMode.equals(PropertiesMode)) {
            propsPath = jb.getPropertiesFile();//getProperty("yayman.propertiesFile");
            propsDefPath = "properties/moviejukebox-default.properties";
            xmlPath = "/yayman/resources/propertiesinfo.xml";
        } else {
            String skinPath = getProperty("mjb.skin.dir", "./skins/default");
            propsPath = skinPath+File.separator + "skin-user.properties";
            propsDefPath = skinPath+File.separator + "skin.properties";
            xmlPath = "/yayman/resources/skinproperties.xml";
            moreInfoRoot = "http://code.google.com/p/moviejukebox/wiki/SkinConfiguration#";
        }

        try {
            File propsFile = new File(propsPath);
            this.setTitle("Editing "+propsFile.getName());
            if (propsFile.exists()) {
                BufferedReader in = new BufferedReader(new FileReader(propsFile));
                String line;
                while ((line = in.readLine()) != null) {
                    int valIndex = line.indexOf("=");
                    if (valIndex != -1) {
                        String key = line.substring(0, valIndex);
                        String val = line.substring(valIndex+1);
                        userProps.put(key, val);
                    }
                }
                in.close();
            }

            File propsDefFile = new File(propsDefPath);
            if (propsDefFile.exists()) {
                BufferedReader in = new BufferedReader(new FileReader(propsDefFile));
                String line;
                while ((line = in.readLine()) != null) {
                    int valIndex = line.indexOf("=");
                    if (valIndex != -1 && !line.startsWith("#")) {
                        String key = line.substring(0, valIndex);
                        String val = line.substring(valIndex+1);
                        defaultProps.put(key, val);
                    }
                }
                in.close();
            }

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db;
            Document doc;

            DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Properties");

            XPath xpath = XPathFactory.newInstance().newXPath();
            db = dbf.newDocumentBuilder();
            doc = db.parse(EditPropertiesFrame.class.getResourceAsStream(xmlPath));

            Element docEle = doc.getDocumentElement();
            NodeList catNL = docEle.getElementsByTagName("Category");
            for (int i=0; i<catNL.getLength(); i++) {
                Element catEle = (Element)catNL.item(i);
                DefaultMutableTreeNode catNode = new DefaultMutableTreeNode(catEle.getAttribute("name"));
                NodeList propNL = catEle.getElementsByTagName("Property");
                for (int j=0; j<propNL.getLength(); j++) {
                    Element propEle = (Element)propNL.item(j);
                    Element descEle = (Element)xpath.evaluate("Desc", propEle, XPathConstants.NODE);
                    String key = propEle.getAttribute("key");
                    String desc = descEle.getTextContent();
                    String def = getDefaultValue(key);

                    YAMJProp prop = new YAMJProp(key, desc, def);

                    if (propEle.hasAttribute("wiki")) {
                        prop.setWikiInfo(Boolean.parseBoolean(propEle.getAttribute("wiki")));
                    }
                    if (propEle.hasAttribute("type")) prop.setType(propEle.getAttribute("type"));
                    if (propEle.hasAttribute("disabled")) prop.setDisabled(Boolean.parseBoolean(propEle.getAttribute("disabled")));
                    if (prop.isDisabled()) disabledProps.add(key);

                    prop.setValue(getUserValue(key));
                    DefaultMutableTreeNode node = new DefaultMutableTreeNode(prop);
                    if (prop.getType().equals("listssubs")) {
                        NodeList subNodes = propEle.getElementsByTagName("SubDescBase");
                        for (int k = 0; k < subNodes.getLength(); k++) {
                            Element subEle = (Element)subNodes.item(k);
                            YAMJSubProp subProp = new YAMJSubProp(subEle.getTextContent());
                            if (subEle.hasAttribute("type")) {
                                subProp.setType(subEle.getAttribute("type"));
                            }
                            if (subEle.hasAttribute("prefix")) {
                                subProp.setPrefix(subEle.getAttribute("prefix"));
                            }
                            prop.addSubProperty(subProp);
                        }
                        addSubProperties(node,prop);
                    } else if (prop.getType().equals("limited")) {
                        if (propEle.hasAttribute("options")) prop.setValidValues(propEle.getAttribute("options"));
                        if (propEle.hasAttribute("otherkey")) prop.setKeyForValues(propEle.getAttribute("otherkey"));
                        if (propEle.hasAttribute("delimiter")) prop.setValueDelimiter(propEle.getAttribute("delimiter"));
                    }

                    xmlProps.add(key);

                    catNode.add(node);
                    if (prop.getKey().equals("mjb.scanner.hashpathdepth")) {
                        String value = prop.getValue();
                        if (value != null) {
                            prefsWindow.setPrevHashdepth(Integer.parseInt(value));
                        } else {
                            prefsWindow.setPrevHashdepth(Integer.parseInt(prop.getDefaultValue()));
                        }
                    }
                }
                rootNode.add(catNode);
            }

            Hashtable<String,String> unknownProps = new Hashtable();
            Enumeration<String> defaultKeys = defaultProps.keys();
            while (defaultKeys.hasMoreElements()) {
                String key = defaultKeys.nextElement();
                if (!xmlProps.contains(key)) {
                    unknownProps.put(key, defaultProps.get(key));
                }
            }

            if (!unknownProps.isEmpty()) {
                String otherPropsCategoryName = "Unrecognized";
                if (currentMode.equals(SkinPropertiesMode)) otherPropsCategoryName = "Skin-Specific";
                DefaultMutableTreeNode catNode = new DefaultMutableTreeNode(otherPropsCategoryName);
                Enumeration<String> unknownKeys = unknownProps.keys();
                while (unknownKeys.hasMoreElements()) {
                    String key = unknownKeys.nextElement();
                    YAMJProp prop = new YAMJProp(key);
                    prop.setDefaultValue(unknownProps.get(key));
                    DefaultMutableTreeNode node = new DefaultMutableTreeNode(prop);

                    catNode.add(node);
                }
                rootNode.add(catNode);
            }

            allProps = new DefaultTreeModel(rootNode);
            propsTree.setModel(allProps);
            disabledLabel.setVisible(false);

            valueTxt.getDocument().addDocumentListener(new DocumentListener() {
                public void changedUpdate(DocumentEvent e) {
                    updateProperty();
                }
                public void removeUpdate(DocumentEvent e) {
                    updateProperty();
                }
                public void insertUpdate(DocumentEvent e) {
                    updateProperty();
                }
            });

            txtFilter.getDocument().addDocumentListener(new DocumentListener() {
                public void changedUpdate(DocumentEvent e) {
                    applyPropFilter();
                }
                public void removeUpdate(DocumentEvent e) {
                    applyPropFilter();
                }
                public void insertUpdate(DocumentEvent e) {
                    applyPropFilter();
                }
            });

            changingSelection = false;

            moreInfoLabel.setVisible(false);

            clearFilterMenuItem.setEnabled(false);

        } catch (Exception ex) {
            YAYManView.logger.severe("Error setting up properties window: "+ex);
            ex.printStackTrace();
            //System.exit(0);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        fileMenu = new javax.swing.JPopupMenu();
        chooseMenu = new javax.swing.JMenuItem();
        fileChooser = new javax.swing.JFileChooser();
        booleanMenu = new javax.swing.JPopupMenu();
        trueMenu = new javax.swing.JMenuItem();
        falseMenu = new javax.swing.JMenuItem();
        skinMenu = new javax.swing.JPopupMenu();
        ltdMenu = new javax.swing.JPopupMenu();
        filterMenu = new javax.swing.JPopupMenu();
        clearFilterMenuItem = new javax.swing.JMenuItem();
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        defaultCheck = new javax.swing.JCheckBox();
        valueTxt = new javax.swing.JTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        descTxt = new javax.swing.JTextArea();
        disabledLabel = new javax.swing.JLabel();
        saveBtn = new javax.swing.JButton();
        cancelBtn = new javax.swing.JButton();
        moreInfoLabel = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        propsTree = new javax.swing.JTree();
        txtFilter = new javax.swing.JTextField();

        fileMenu.setName("fileMenu"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(yayman.YAYManApp.class).getContext().getActionMap(EditPropertiesFrame.class, this);
        chooseMenu.setAction(actionMap.get("chooseFile")); // NOI18N
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(yayman.YAYManApp.class).getContext().getResourceMap(EditPropertiesFrame.class);
        chooseMenu.setIcon(resourceMap.getIcon("chooseMenu.icon")); // NOI18N
        chooseMenu.setText(resourceMap.getString("chooseMenu.text")); // NOI18N
        chooseMenu.setName("chooseMenu"); // NOI18N
        fileMenu.add(chooseMenu);

        fileChooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setName("fileChooser"); // NOI18N

        booleanMenu.setName("booleanMenu"); // NOI18N

        trueMenu.setAction(actionMap.get("setTrue")); // NOI18N
        trueMenu.setIcon(resourceMap.getIcon("trueMenu.icon")); // NOI18N
        trueMenu.setText(resourceMap.getString("trueMenu.text")); // NOI18N
        trueMenu.setName("trueMenu"); // NOI18N
        booleanMenu.add(trueMenu);

        falseMenu.setAction(actionMap.get("setFalse")); // NOI18N
        falseMenu.setIcon(resourceMap.getIcon("falseMenu.icon")); // NOI18N
        falseMenu.setText(resourceMap.getString("falseMenu.text")); // NOI18N
        falseMenu.setName("falseMenu"); // NOI18N
        booleanMenu.add(falseMenu);

        skinMenu.setName("skinMenu"); // NOI18N

        ltdMenu.setName("ltdMenu"); // NOI18N

        filterMenu.setName("filterMenu"); // NOI18N

        clearFilterMenuItem.setAction(actionMap.get("clearFilter")); // NOI18N
        clearFilterMenuItem.setIcon(resourceMap.getIcon("clearFilterMenuItem.icon")); // NOI18N
        clearFilterMenuItem.setText(resourceMap.getString("clearFilterMenuItem.text")); // NOI18N
        clearFilterMenuItem.setName("clearFilterMenuItem"); // NOI18N
        filterMenu.add(clearFilterMenuItem);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setModalityType(java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        setName("Form"); // NOI18N

        jSplitPane1.setDividerLocation(200);
        jSplitPane1.setName("jSplitPane1"); // NOI18N

        jPanel1.setName("jPanel1"); // NOI18N

        defaultCheck.setAction(actionMap.get("toggleDefault")); // NOI18N
        defaultCheck.setText(resourceMap.getString("defaultCheck.text")); // NOI18N
        defaultCheck.setName("defaultCheck"); // NOI18N

        valueTxt.setEditable(false);
        valueTxt.setText(resourceMap.getString("valueTxt.text")); // NOI18N
        valueTxt.setName("valueTxt"); // NOI18N
        valueTxt.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                valueTxtMouseClicked(evt);
            }
        });

        jScrollPane2.setBorder(null);
        jScrollPane2.setEnabled(false);
        jScrollPane2.setName("jScrollPane2"); // NOI18N

        descTxt.setBackground(resourceMap.getColor("descTxt.background")); // NOI18N
        descTxt.setColumns(20);
        descTxt.setEditable(false);
        descTxt.setFont(resourceMap.getFont("descTxt.font")); // NOI18N
        descTxt.setLineWrap(true);
        descTxt.setRows(5);
        descTxt.setWrapStyleWord(true);
        descTxt.setBorder(null);
        descTxt.setName("descTxt"); // NOI18N
        jScrollPane2.setViewportView(descTxt);

        disabledLabel.setForeground(resourceMap.getColor("disabledLabel.foreground")); // NOI18N
        disabledLabel.setText(resourceMap.getString("disabledLabel.text")); // NOI18N
        disabledLabel.setName("disabledLabel"); // NOI18N

        saveBtn.setAction(actionMap.get("closeSave")); // NOI18N
        saveBtn.setText(resourceMap.getString("saveBtn.text")); // NOI18N
        saveBtn.setName("saveBtn"); // NOI18N

        cancelBtn.setAction(actionMap.get("closeNoSave")); // NOI18N
        cancelBtn.setText(resourceMap.getString("cancelBtn.text")); // NOI18N
        cancelBtn.setName("cancelBtn"); // NOI18N

        moreInfoLabel.setForeground(resourceMap.getColor("moreInfoLabel.foreground")); // NOI18N
        moreInfoLabel.setText(resourceMap.getString("moreInfoLabel.text")); // NOI18N
        moreInfoLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        moreInfoLabel.setName("moreInfoLabel"); // NOI18N
        moreInfoLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                moreInfoLabelMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(defaultCheck)
                                .addGap(18, 18, 18)
                                .addComponent(disabledLabel))
                            .addComponent(valueTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 299, Short.MAX_VALUE)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 299, Short.MAX_VALUE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(69, 69, 69)
                        .addComponent(saveBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(cancelBtn))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(moreInfoLabel)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(defaultCheck)
                    .addComponent(disabledLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(valueTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(moreInfoLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 99, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(saveBtn)
                    .addComponent(cancelBtn))
                .addContainerGap())
        );

        jSplitPane1.setRightComponent(jPanel1);

        jPanel2.setName("jPanel2"); // NOI18N

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("Uh-Oh");
        javax.swing.tree.DefaultMutableTreeNode treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("It looks like");
        javax.swing.tree.DefaultMutableTreeNode treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("It appears as though");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("It would seem that");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Apparently");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("For some reason");
        treeNode2.add(treeNode3);
        treeNode1.add(treeNode2);
        treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("We have a");
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("There is a");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Something caused a");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Careless programming resulted in a");
        treeNode2.add(treeNode3);
        treeNode1.add(treeNode2);
        treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Problem here");
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Malfunction");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Error");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Mistake");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Horrific tragedy");
        treeNode2.add(treeNode3);
        treeNode1.add(treeNode2);
        propsTree.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        propsTree.setName("propsTree"); // NOI18N
        propsTree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                propsTreeValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(propsTree);

        txtFilter.setText(resourceMap.getString("txtFilter.text")); // NOI18N
        txtFilter.setToolTipText(resourceMap.getString("txtFilter.toolTipText")); // NOI18N
        txtFilter.setComponentPopupMenu(filterMenu);
        txtFilter.setName("txtFilter"); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(txtFilter, javax.swing.GroupLayout.DEFAULT_SIZE, 199, Short.MAX_VALUE)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 199, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(txtFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 272, Short.MAX_VALUE))
        );

        jSplitPane1.setLeftComponent(jPanel2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 525, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void propsTreeValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_propsTreeValueChanged
        changingSelection = true;
        defaultCheck.setEnabled(false);
        descTxt.setText("");
        valueTxt.setText("");
        disabledLabel.setVisible(false);
        YAMJProp yp = getSelectedProperty();
        moreInfoLabel.setVisible(false);
        if (yp != null) {
            descTxt.setText(yp.getDescription());
            defaultCheck.setSelected(yp.isDefaultValue());
            defaultCheck.setEnabled(true);
            valueTxt.setText(yp.getUsedValue());
            valueTxt.setEditable(!yp.isDefaultValue());
            disabledLabel.setVisible(yp.isDisabled());
            moreInfoLabel.setVisible(yp.hasWikiInfo());
        }
        setMenusEnabled();
        changingSelection = false;
    }//GEN-LAST:event_propsTreeValueChanged

    private void valueTxtMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_valueTxtMouseClicked
        javax.swing.JPopupMenu menu = valueTxt.getComponentPopupMenu();
        if (menu != null) {
            menu.show(valueTxt, evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_valueTxtMouseClicked

    private void moreInfoLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_moreInfoLabelMouseClicked
        try {
            YAMJProp prop = getSelectedProperty();
            if (prop != null && moreInfoLabel.isEnabled()) {
                java.awt.Desktop.getDesktop().browse(new URI(moreInfoRoot+prop.getKey()));
            }
        } catch (Exception ex) {
            YAYManView.logger.severe("Error opening properties info page: "+ex);
        }
    }//GEN-LAST:event_moreInfoLabelMouseClicked

    /**
    * @param args the command line arguments
    */
    /*public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                EditPropertiesFrame dialog = new EditPropertiesFrame(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }*/

    @Action
    public void toggleDefault() {
        if (!changingSelection) {
            valueTxt.setEditable(!defaultCheck.isSelected());
            YAMJProp yp = getSelectedProperty();
            setMenusEnabled();
            if (!defaultCheck.isSelected() && yp != null) {
                userProps.put(yp.getKey(), valueTxt.getText());
                yp.setValue(valueTxt.getText());
            } else if (yp != null) {
                yp.setValue(null);
                userProps.remove(yp.getKey());
                valueTxt.setText(yp.getDefaultValue());
                if (yp.getType().equals("listssubs")) {
                    updateSubProperties(yp);
                }
            }
        }
    }

    private void setMenusEnabled() {
        chooseMenu.setEnabled(!defaultCheck.isSelected());
        trueMenu.setEnabled(!defaultCheck.isSelected());
        falseMenu.setEnabled(!defaultCheck.isSelected());
        valueTxt.setComponentPopupMenu(null);
        valueTxt.setToolTipText(null);
        valueTxt.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        YAMJProp yp = getSelectedProperty();
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(yayman.YAYManApp.class).getContext().getResourceMap(EditPropertiesFrame.class);
        if (yp != null && !yp.getType().equals(YAMJProp.DefaultType)) {
            valueTxt.setToolTipText("Click to select value");
            valueTxt.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            if (yp.getType().equals("folder")) {
                valueTxt.setComponentPopupMenu(fileMenu);
                valueTxt.setEditable(false);
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setDialogTitle("Select jukebox root folder");
            } else if (yp.getType().equals("boolean")) {
                valueTxt.setComponentPopupMenu(booleanMenu);
                valueTxt.setEditable(false);
            } else if (yp.getType().equals("file")) {
                valueTxt.setComponentPopupMenu(fileMenu);
                valueTxt.setEditable(false);
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fileChooser.setDialogTitle("Select file");
            } else if (yp.getType().equals("skin")) {
                valueTxt.setComponentPopupMenu(skinMenu);
                valueTxt.setEditable(false);
                skinMenu.removeAll();
                File skinDir = new File("skins");
                if (skinDir.exists() && skinDir.isDirectory()) {
                    for (int i=0; i < skinDir.listFiles().length; i++) {
                        File f = skinDir.listFiles()[i];
                        if (f.isDirectory()) {
                            JMenuItem menu = new JMenuItem();
                            menu.setText("./skins/"+f.getName());
                            menu.addActionListener(new java.awt.event.ActionListener() {
                                public void actionPerformed(java.awt.event.ActionEvent evt) {
                                    valueTxt.setText(((JMenuItem)evt.getSource()).getText());
                                    forceUpdateProperty();
                                }
                            });
                            menu.setIcon(resourceMap.getIcon("chooseMenu.icon"));
                            skinMenu.add(menu);
                        }
                    }
                }
            } else if (yp.getType().equals("limited")) {
                setLimitedMenu(yp);
            } else {
                valueTxt.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        }
        for (int i=0; i< skinMenu.getSubElements().length; i++) {
            ((JMenuItem)skinMenu.getSubElements()[i]).setEnabled(!defaultCheck.isSelected());
        }
        for (int i=0; i< ltdMenu.getSubElements().length; i++) {
            ((JMenuItem)ltdMenu.getSubElements()[i]).setEnabled(!defaultCheck.isSelected());
        }
    }

    private void setLimitedMenu(YAMJProp yp) {
        valueTxt.setComponentPopupMenu(ltdMenu);
        valueTxt.setEditable(false);
        ltdMenu.removeAll();
        String[] opts = {};
        /*if (yp.getType().equals("limited")) {
            opts = yp.getValidValues().split(yp.getValueDelimiter());
        } else if (yp.getType().equals("listfromother")) {
            if (getUserValue(yp.getKeyForValues()) != null) {
                opts = getUserValue(yp.getKeyForValues()).split(yp.getValueDelimiter());
            } else {
                opts = getDefaultValue(yp.getKeyForValues()).split(yp.getValueDelimiter());
            }
        }*/
        if (yp.getValidValues() != null) {
            opts = yp.getValidValues().split(yp.getValueDelimiter());
        } else if (yp.getKeyForValues() != null) {
            if (getUserValue(yp.getKeyForValues()) != null) {
                opts = getUserValue(yp.getKeyForValues()).split(yp.getValueDelimiter());
            } else {
                opts = getDefaultValue(yp.getKeyForValues()).split(yp.getValueDelimiter());
            }
        }
        for (int i=0; i < opts.length; i++) {
            JMenuItem menu = new JMenuItem();
            menu.setText(opts[i]);
            menu.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    valueTxt.setText(((JMenuItem)evt.getSource()).getText());
                    forceUpdateProperty();
                }
            });
            ltdMenu.add(menu);
        }
    }

    private YAMJProp getSelectedProperty() {
        YAMJProp yp = null;
        DefaultMutableTreeNode selected = getSelectedPropertyNode();
        if (selected != null) {
            yp = (YAMJProp)selected.getUserObject();
        }
        return yp;
    }

    private DefaultMutableTreeNode getSelectedPropertyNode() {
        if (propsTree.getSelectionCount() > 0) {
            Object[] path = propsTree.getSelectionPath().getPath();
            //if (path.length > 2) {
                DefaultMutableTreeNode selected = (DefaultMutableTreeNode)path[path.length-1];
                if (selected.getUserObject().getClass().toString().endsWith("YAMJProp")) {
                    return selected;
                }
            //}
        }

        return null;
    }

    private void updateProperty() {
        if (!changingSelection) {
            YAMJProp yp = getSelectedProperty();
            if (!defaultCheck.isSelected() && valueTxt.isEditable() && yp != null) {
                yp.setValue(valueTxt.getText());
                userProps.put(yp.getKey(), valueTxt.getText());
                if (yp.getType().equals("listssubs")) {
                    updateSubProperties(yp);
                }
            }
        }
    }

    private void forceUpdateProperty() {
        boolean editable = valueTxt.isEditable();
        valueTxt.setEditable(true);
        updateProperty();
        valueTxt.setEditable(editable);
    }

    @Action
    public void closeNoSave() {
        this.setVisible(false);
    }

    @Action
    public void closeSave() {
        try {
            Object[] keys = userProps.keySet().toArray();
            File propsFile = new File(propsPath);
            BufferedWriter out = new BufferedWriter(new FileWriter(propsFile));
            for (int i = 0; i < keys.length; i++) {
                String key = keys[i].toString();
                String val = userProps.get(key);
                if (!val.equals(getDefaultValue(key)) || disabledProps.contains(key)) {
                    out.write(key+"="+val);
                    out.newLine();
                }
            }
            out.flush();
            out.close();
        } catch (Exception ex) {
            YAYManView.logger.severe("Error saving properties: "+ex);
        }
        prefsWindow.resetProperties();
        this.setVisible(false);
    }

    @Action
    public void setTrue() {
        valueTxt.setText("true");
        forceUpdateProperty();
    }

    @Action
    public void setFalse() {
        valueTxt.setText("false");
        forceUpdateProperty();
    }

    @Action
    public void chooseFile() {
        if (fileChooser.showOpenDialog(chooseMenu) == JFileChooser.APPROVE_OPTION) {
            valueTxt.setText(fileChooser.getSelectedFile().getPath().replace("\\", "/"));
            forceUpdateProperty();
        }
    }

    private String getDefaultValue(String key) {
        if (defaultProps.containsKey(key)) {
            return defaultProps.get(key);
        }
        return null;
    }

    private String getUserValue(String key) {
        if (userProps.containsKey(key)) {
            return userProps.get(key);
        }
        return null;
    }

    private void addSubProperties(DefaultMutableTreeNode node, YAMJProp prop) {
        String val = prop.getValue();
        if (val == null) {
            val = prop.getDefaultValue();
            if (val == null) {
                val = "";
            }
        }
        String[] subs = val.split(",");

        String subDescBase = "";
        String keyBase = "";
        String type = "subprop";
        for (int s = 0; s < prop.getSubProperties().size(); s++) {
            YAMJSubProp sp = prop.getSubProperty(s);
            subDescBase = sp.getDesc();
            keyBase = prop.getKey();
            if (sp.getPrefix() != null && !sp.getPrefix().equals("")) {
                keyBase += "."+sp.getPrefix();
            }

            for (int i = 0; i < subs.length; i++) {
                String sub = subs[i];
                String subKey = keyBase+"."+sub;
                String subDesc = subDescBase+sub;
                String subDef = getDefaultValue(subKey);

                YAMJProp subProp = new YAMJProp(subKey, subDesc, subDef, false, type, null);
                subProp.setParentProperty(prop);
                subProp.setValue(getUserValue(subKey));
                subProp.setWikiInfo(false);
                DefaultMutableTreeNode subNode = new DefaultMutableTreeNode(subProp);
                node.add(subNode);
                xmlProps.add(subKey);
            }
        }
    }
    
    public void updateSubProperties(YAMJProp yp) {
        updateSubProperties(getSelectedPropertyNode(), yp);
    }

    public void updateSubProperties(DefaultMutableTreeNode node, YAMJProp yp) {
        node.removeAllChildren();
        addSubProperties(node, yp);
        DefaultTreeModel model = (DefaultTreeModel)propsTree.getModel();
        model.nodeStructureChanged(node);
    }

    public void setProperty(String key, String value) {
        userProps.put(key, value);
    }

    public String getPropertyValue(String key) {
        if (userProps.containsKey(key)) return userProps.get(key);
        return defaultProps.get(key);
    }

    public void mergeProperties(String path) {
        File propsFile = new File(propsPath);
        logger.fine("Merging properties from "+path);
        try {
            if (propsFile.exists()) {
                BufferedReader in = new BufferedReader(new FileReader(propsFile));
                String line;
                while ((line = in.readLine()) != null) {
                    int valIndex = line.indexOf("=");
                    if (valIndex != -1) {
                        String key = line.substring(0, valIndex);
                        String val = line.substring(valIndex+1);
                        userProps.put(key, val);
                    }
                }
            }
        } catch (Exception ex) {
            logger.severe("Error merging properties from "+path+": "+ex);
        }
    }

    public void applyPropFilter() {
        if (txtFilter.getText().length() == 0 || txtFilter.getText().isEmpty()) {
            propsTree.setModel(allProps);
            clearFilterMenuItem.setEnabled(false);
        } else {
            clearFilterMenuItem.setEnabled(true);
            DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Properties");
            DefaultMutableTreeNode allRoot = (DefaultMutableTreeNode)allProps.getRoot();
            for (int i = 0; i < allRoot.getChildCount(); i++) {
                DefaultMutableTreeNode catNode = (DefaultMutableTreeNode)allRoot.getChildAt(i);
                //DefaultMutableTreeNode filtCatNode = new DefaultMutableTreeNode(catNode.getUserObject());
                for (int ii = 0; ii < catNode.getChildCount(); ii++) {
                    DefaultMutableTreeNode propNode = (DefaultMutableTreeNode)catNode.getChildAt(ii);
                    YAMJProp prop = (YAMJProp)propNode.getUserObject();

                    if (prop.getKey().toLowerCase().contains(txtFilter.getText().toLowerCase())) {
                        //filtCatNode.add(new DefaultMutableTreeNode(prop));
                        rootNode.add(new DefaultMutableTreeNode(prop));
                    }
                }
                //if (filtCatNode.getChildCount() > 0) rootNode.add(filtCatNode);
            }
            propsTree.setModel(new DefaultTreeModel(rootNode));
        }
    }

    @Action
    public void clearFilter() {
        txtFilter.setText("");
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPopupMenu booleanMenu;
    private javax.swing.JButton cancelBtn;
    private javax.swing.JMenuItem chooseMenu;
    private javax.swing.JMenuItem clearFilterMenuItem;
    private javax.swing.JCheckBox defaultCheck;
    private javax.swing.JTextArea descTxt;
    private javax.swing.JLabel disabledLabel;
    private javax.swing.JMenuItem falseMenu;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JPopupMenu fileMenu;
    private javax.swing.JPopupMenu filterMenu;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JPopupMenu ltdMenu;
    private javax.swing.JLabel moreInfoLabel;
    private javax.swing.JTree propsTree;
    private javax.swing.JButton saveBtn;
    private javax.swing.JPopupMenu skinMenu;
    private javax.swing.JMenuItem trueMenu;
    private javax.swing.JTextField txtFilter;
    private javax.swing.JTextField valueTxt;
    // End of variables declaration//GEN-END:variables

}
