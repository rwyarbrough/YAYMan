/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package yayman;

import java.io.*;
import java.util.logging.*;
import org.w3c.dom.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.parsers.DocumentBuilderFactory;

public class SetListItem {
    private static Logger logger = Logger.getLogger("yayman");
    String setName;
    String originalName;
    int setOrder;
    int originalOrder;

    public SetListItem() {
        setName = null;
        originalName = null;
        setOrder = 1;
        originalOrder = 1;
    }

    public SetListItem(String name) {
        this();
        setName = name;
        originalName = name;
    }

    public SetListItem(String name, int order) {
        this(name);
        setOrder = order;
        originalOrder = order;
    }

    @Override
    public String toString() {
        return setName;
    }

    public String getName() {
        return setName;
    }

    public int getOrder() {
        return setOrder;
    }

    public void setName(String name) {
        if (name.length() > 0) setName = name;
    }

    public void setOrder(int order) {
        setOrder = order;
    }

    public void setOrder(String order) {
        setOrder(Integer.parseInt(order));
    }

    public void updateSet(File xmlFile) {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);
            NodeList setsNodes = doc.getDocumentElement().getElementsByTagName("sets");
            Node setsNode = null;
            for (int i = 0; i < setsNodes.getLength(); i++) {
                Node node = setsNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    setsNode = node;
                    break;
                }
            }
            NodeList sets = setsNode.getChildNodes();
            for (int i = 0; i < sets.getLength(); i++) {
                Node node = sets.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element set = (Element)sets.item(i);
                    if (Integer.parseInt(set.getAttribute("order")) == originalOrder && set.getTextContent().equals(originalName)) {
                        set.setAttribute("order", ""+setOrder);
                        set.setTextContent(setName);
                        Transformer xformer = TransformerFactory.newInstance().newTransformer();
                        xformer.transform(new DOMSource(doc), new StreamResult(xmlFile));
                        originalOrder = setOrder;
                        originalName = setName;
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            logger.severe("Error updating set '"+setName+"' from "+xmlFile.getPath());
            logger.severe(ex.toString());
        }
    }

    public boolean removeSet(File xmlFile) {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);
            Node setsNode = doc.getDocumentElement().getElementsByTagName("sets").item(0);
            NodeList sets = setsNode.getChildNodes();
            for (int i = 0; i < sets.getLength(); i++) {
                Element set = (Element)sets.item(i);
                if (Integer.parseInt(set.getAttribute("order")) == setOrder && set.getTextContent().equals(setName)) {
                    setsNode.removeChild(set);
                    Transformer xformer = TransformerFactory.newInstance().newTransformer();
                    xformer.transform(new DOMSource(doc), new StreamResult(xmlFile));
                    return true;
                }
            }
        } catch (Exception ex) {
            logger.severe("Error removing set '"+setName+"' from "+xmlFile.getPath());
            logger.severe(ex.toString());
            return false;
        }
        return false;
    }

    public boolean createSet(File xmlFile) {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);
            Node movieNode = doc.getDocumentElement().getElementsByTagName("movie").item(0);
            Node setsNode;
            if (((Element)movieNode).getElementsByTagName("sets").getLength() > 0) {
                setsNode = ((Element)movieNode).getElementsByTagName("sets").item(0);
            } else {
                setsNode = doc.createElement("sets");
                movieNode.appendChild(setsNode);
            }
            Element setEle = doc.createElement("set");
            setEle.setAttribute("order", ""+setOrder);
            setEle.setTextContent(setName);
            setsNode.appendChild(setEle);

            Transformer xformer = TransformerFactory.newInstance().newTransformer();
            xformer.transform(new DOMSource(doc), new StreamResult(xmlFile));
            return true;
        } catch (Exception ex) {
            logger.severe("Error creating set '"+setName+"' in "+xmlFile.getPath());
            logger.severe(ex.toString());
            return false;
        }
    }
}
