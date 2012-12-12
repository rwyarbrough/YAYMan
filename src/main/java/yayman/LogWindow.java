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
 * LogWindow.java
 *
 * Created on Aug 14, 2010, 12:42:56 PM
 */

package yayman;

import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

public class LogWindow extends javax.swing.JFrame {

    private YAYManView mainFrame;
    private AdjustmentListener scrollListener;

    /** Creates new form LogWindow */
    public LogWindow(YAYManView yview) {
        initComponents();
        mainFrame = yview;

        scrollListener = new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                e.getAdjustable().setValue(e.getAdjustable().getMaximum());
            }
        };
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        txtLog = new javax.swing.JTextArea();

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(yayman.YAYManApp.class).getContext().getResourceMap(LogWindow.class);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setName("Form"); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        txtLog.setColumns(20);
        txtLog.setEditable(false);
        txtLog.setRows(5);
        txtLog.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));
        txtLog.setName("txtLog"); // NOI18N
        jScrollPane1.setViewportView(txtLog);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        mainFrame.showLog(false);
    }//GEN-LAST:event_formWindowClosing

    public void addLogMessage(String message) {
        scrollToBottom(true);
        if (txtLog.getText() != null && !txtLog.getText().isEmpty())
            txtLog.setText(txtLog.getText()+"\r\n");
        txtLog.setText(txtLog.getText()+message);
        scrollToBottom(false);
    }

    public void clearLog() {
        txtLog.setText("");
    }

    public void scrollToBottom(boolean scroll) {
        if (scroll) {
            jScrollPane1.getVerticalScrollBar().addAdjustmentListener(scrollListener);
        } else {
            jScrollPane1.getVerticalScrollBar().removeAdjustmentListener(scrollListener);
        }
        //jScrollPane1.getVerticalScrollBar().addAdjustmentListener(scrollListener);
        //jScrollPane1.getVerticalScrollBar().removeAdjustmentListener(scrollListener);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea txtLog;
    // End of variables declaration//GEN-END:variables

}