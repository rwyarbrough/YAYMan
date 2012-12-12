/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package yayman;

/**
 *
 * @author Dan
 */
public class WizardError {
    private boolean fatalError;
    private boolean error;
    private String message;

    public WizardError() {
        fatalError = false;
        error = false;
        message = "";
    }

    public boolean hasFatalError() {
        return fatalError;
    }

    public boolean hasError() {
        return (error || fatalError);
    }

    public String getErrorMessage() {
        return "<html>"+message+"</html>";
    }

    public String getMessage() {
        return message;
    }

    public void setFatalError(boolean b) {
        fatalError = b;
        if (fatalError) error = true;
    }

    public void setError(boolean b) {
        error = b;
    }

    public void setErrorMessage(String s) {
        setMessage(s);
        setError(true);
    }

    public void setFatalMessage(String s) {
        setMessage(s);
        setFatalError(true);
    }

    public void setMessage(String s) {
        message = s;
    }
}
