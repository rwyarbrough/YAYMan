/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package yayman;

import javax.swing.tree.*;
import org.w3c.dom.*;

public class XmlTreeNode extends DefaultMutableTreeNode {
    //private Element element;
    private String attributeName;

    public XmlTreeNode(Element ele) {
        super(ele);
        attributeName = null;
    }

    public XmlTreeNode (Node node) {
        this((Element)node);
    }

    public XmlTreeNode(Element ele, String attrib) {
        this(ele);
        attributeName = attrib;
    }

    public Element getElement() {
        return (Element)super.getUserObject();
    }

    @Override
    public String toString() {
        if (attributeName != null) return getElement().getAttribute(attributeName);
        return getElement().getTextContent();
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attrib) {
        attributeName = attrib;
    }

    @Override
    public void setUserObject(Object obj) {
        if (obj.getClass().getName().equals(Element.class.getName())) {
            super.setUserObject(obj);
        } else if (obj.getClass().getName().equals(String.class.getName())) {
            String value = obj.toString();
            if (attributeName != null) {
                getElement().setAttribute(attributeName, value);
            } else {
                getElement().setTextContent(value);
            }
            //updateXmlNode(getXmlFile(getSelectedMovie()),(Node)getUserObject());
            //createHTML();
        }
    }
}
