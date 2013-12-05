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
import javax.swing.SwingWorker;
import java.awt.Cursor;
import java.awt.Container;
import javax.swing.JProgressBar;

public class MySwingWorker extends SwingWorker<Void,String> {
    protected JProgressBar progressBar;

    public MySwingWorker() {
        super();
        progressBar = null;
    }

    public MySwingWorker(JProgressBar progress) {
        super();
        progressBar = progress;
    }

    public MySwingWorker(MySwingWorker work) {
        this(work.progressBar);
    }

    @Override
    protected Void doInBackground() {
        try {
            
        } catch (Exception ex) {
            YAYManView.logger.severe("Error: "+ex);
        } catch (Throwable th) {
            YAYManView.logger.severe("Error: "+th);
        }
        return null;
    }

    @Override
    protected void done() {
        //showProcessing(false);
    }

    public void doPublish(String... chunks) {
        this.publish(chunks);
    }

    public void showProcessing(boolean isProcessing) {
        progressBar.setVisible(isProcessing);
        progressBar.setIndeterminate(isProcessing);
        Container topLevel = null;
        if (progressBar != null) {
            topLevel = progressBar.getTopLevelAncestor();
        }
        if (topLevel != null) {
            if (isProcessing) {
                topLevel.setCursor(new Cursor(Cursor.WAIT_CURSOR));
            } else {
                topLevel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        }
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public void setProgressBar(JProgressBar bar) {
        progressBar = bar;
    }
}