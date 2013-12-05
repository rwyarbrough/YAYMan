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

import org.w3c.dom.*;
import java.io.*;
import com.moviejukebox.model.*;
import java.util.logging.*;
import java.util.ArrayList;
import com.moviejukebox.tools.*;
import com.moviejukebox.tools.PropertiesUtil.*;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.parsers.*;
import static com.moviejukebox.tools.PropertiesUtil.getProperty;

import javax.xml.xpath.*;
import org.xml.sax.*;

public class MovieXmlTools {

    private static Logger logger = Logger.getLogger("yayman");
    private static XPath xPath=XPathFactory.newInstance().newXPath();
    private static String moviedb = "moviedb";

    public static void updateMovieXml(Movie movie, String attribute, String newValue) {
        File xmlFile = getXmlFile(movie);
        try {
            Node node = getVideoXmlNode(movie, attribute);
            node.setTextContent(newValue);

            saveXmlDocument(xmlFile, node);
            updateMovieXmlIndices(movie);
        } catch (Exception ex) {
            logger.severe(ex+": updating XML data for"+movie.getBaseFilename());
        }
    }
    
    /*public static void updateOtherXml(Node fileNode, String attribute, String newValue) {
        String location = "";
        try {
            location = getFileLocation(fileNode);
            
            ArrayList<File> xmlFiles = JukeboxInterface.getLibXMLFiles();
            for (int i=0; i < xmlFiles.size(); i++) {
                File xml = xmlFiles.get(i);
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml);
                String path = "//library/movies/movie/files/file/fileLocation";
                NodeList locNodes = (NodeList)(xPath.evaluate(path, doc, XPathConstants.NODESET));
                for (int j=0; j < locNodes.getLength(); j++) {
                    Node locNode = locNodes.item(j);
                    if (locNode.getTextContent().equals(location)) {
                        Element movieNode = (Element)(locNode.getParentNode().getParentNode().getParentNode());
                        Node attNode = movieNode.getElementsByTagName(attribute).item(0);
                        attNode.setTextContent(newValue);
                        Transformer xformer = TransformerFactory.newInstance().newTransformer();
                        xformer.transform(new DOMSource(doc), new StreamResult(xml));
                        //logger.fine("Updated in "+xml.getName());
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            logger.severe(ex+": updating other XML data for "+location);
        }
    }*/
    
    private static String getFileLocation(Node node) {
        String loc = "";
        NodeList children = node.getChildNodes();
        for (int i=0; i < children.getLength(); i++) {
            Node currChild = children.item(i);
            if (currChild.getNodeName().matches("fileLocation")) {
                loc = currChild.getTextContent();
                break;
            }
        }
        
        return loc;
    }
    
    public static void updateMovieXmlIndices(Movie movie) {
        if (!Boolean.parseBoolean(getProperty("mjb.skipHtmlGeneration","false"))) return;
        logger.fine("here");
        try {
            File xmlFile = getXmlFile(movie);
            String path = "//details/movie/files/file[@firstPart=\"1\"]";
            Node fileNode = (Node)(xPath.evaluate(path, getDocument(xmlFile), XPathConstants.NODE));
            String location = getFileLocation(fileNode);
            Element movieNode = (Element)fileNode.getParentNode().getParentNode();
            
            ArrayList<File> xmlFiles = JukeboxInterface.getLibXMLFiles();
            for (int i=0; i < xmlFiles.size(); i++) {
                File xml = xmlFiles.get(i);
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml);
                path = "//library/movies/movie/files/file/fileLocation";
                NodeList locNodes = (NodeList)(xPath.evaluate(path, doc, XPathConstants.NODESET));
                for (int j=0; j < locNodes.getLength(); j++) {
                    Node locNode = locNodes.item(j);
                    if (locNode.getTextContent().equals(location)) {
                        //logger.fine("Updating "+xml.getName());
                        Element idxMovieNode = (Element)(locNode.getParentNode().getParentNode().getParentNode());
                        movieNode.getElementsByTagName("first").item(0).setTextContent(idxMovieNode.getElementsByTagName("first").item(0).getTextContent());
                        movieNode.getElementsByTagName("previous").item(0).setTextContent(idxMovieNode.getElementsByTagName("previous").item(0).getTextContent());
                        movieNode.getElementsByTagName("next").item(0).setTextContent(idxMovieNode.getElementsByTagName("next").item(0).getTextContent());
                        movieNode.getElementsByTagName("last").item(0).setTextContent(idxMovieNode.getElementsByTagName("last").item(0).getTextContent());
                        Node parentNode = idxMovieNode.getParentNode();
                        parentNode.replaceChild(doc.adoptNode(movieNode), idxMovieNode);
                        saveXmlDocument(xml, doc);
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            logger.severe(ex+": updating XML indices for "+movie.getBaseFilename());
        }
    }

    public static void updateMovieFileXml(Movie movie, int part, String tagName, String newValue) {
        File xmlFile = getXmlFile(movie);
        try {
            Node node = getMovieFileXmlNode(movie, part, tagName);
            node.setTextContent(newValue);

            saveXmlDocument(xmlFile, node);
        } catch (Exception ex) {
            logger.severe(ex+": updating XML data for "+movie.getBaseFilename());
        }
    }
    
 /*   public static void updateOtherFileTitleXml(Node fileNode, String newValue, boolean renameSub) {
        if (!Boolean.parseBoolean(getProperty("mjb.skipHtmlGeneration","false"))) {
            return;
        }
        //String path = "//details/movie/files/file[@firstPart=\"1\"]";
        //Node fileNode = (Node)(xPath.evaluate(path, getDocument(movie), XPathConstants.NODE));
        String location = "";
        try {
            location = getFileLocation(fileNode);
            
            ArrayList<File> xmlFiles = JukeboxInterface.getLibXMLFiles();
            for (int i=0; i < xmlFiles.size(); i++) {
                File xml = xmlFiles.get(i);
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml);
                String path = "//library/movies/movie/files/file/fileLocation";
                NodeList locNodes = (NodeList)(xPath.evaluate(path, doc, XPathConstants.NODESET));
                for (int j=0; j < locNodes.getLength(); j++) {
                    Node locNode = locNodes.item(j);
                    if (locNode.getTextContent().equals(location)) {
                        Element otherFileNode = (Element)(locNode.getParentNode());
                        otherFileNode.setAttribute("title", newValue);
                        if (renameSub) {
                            otherFileNode.getElementsByTagName("fileTitle").item(0).setTextContent(newValue);
                        }
                        
                        Transformer xformer = TransformerFactory.newInstance().newTransformer();
                        xformer.transform(new DOMSource(doc), new StreamResult(xml));
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            logger.severe(ex+": updating XML file data for "+location);
        }
    }
    
    public static void updateOtherFileNodeXml(Element fileSubNode, String newValue) {
        if (!Boolean.parseBoolean(getProperty("mjb.skipHtmlGeneration","false"))) {
            return;
        }
        String tagName = fileSubNode.getNodeName();
        String pt = "";
        if (fileSubNode.hasAttribute("part")) pt = fileSubNode.getAttribute("part");
        String location = "";
        try {
            location = getFileLocation(fileSubNode.getParentNode());
            ArrayList<File> xmlFiles = JukeboxInterface.getLibXMLFiles();
            for (int i=0; i < xmlFiles.size(); i++) {
                File xml = xmlFiles.get(i);
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml);
                String path = "//library/movies/movie/files/file/fileLocation";
                NodeList locNodes = (NodeList)(xPath.evaluate(path, doc, XPathConstants.NODESET));
                for (int j=0; j < locNodes.getLength(); j++) {
                    Node locNode = locNodes.item(j);
                    if (locNode.getTextContent().equals(location)) {
                        Node otherFileNode = locNode.getParentNode();
                        NodeList fileMatchedNodes = ((Element)otherFileNode).getElementsByTagName(tagName);
                        if (fileMatchedNodes.getLength() > 1) {
                            for (int k=0; k < fileMatchedNodes.getLength(); k++) {
                                Element sub = (Element)fileMatchedNodes.item(k);
                                if (sub.hasAttribute("part") && sub.getAttribute("part").matches(pt)) {
                                    sub.setTextContent(newValue);
                                    Transformer xformer = TransformerFactory.newInstance().newTransformer();
                                    xformer.transform(new DOMSource(doc), new StreamResult(xml));
                                    break;
                                }
                            }
                        } else if (fileMatchedNodes.getLength() != 0) {
                            fileMatchedNodes.item(0).setTextContent(newValue);
                            Transformer xformer = TransformerFactory.newInstance().newTransformer();
                            xformer.transform(new DOMSource(doc), new StreamResult(xml));
                        } else {
                            break;
                        }
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            logger.severe(ex+": updating XML file data for "+location);
        }
    }*/

    public static File getXmlFile(Movie movie) {
        String safeBaseName = FileTools.makeSafeFilename(movie.getBaseName());
        return new File(JukeboxInterface.getFullDetailsPath()+File.separator+safeBaseName+".xml");
    }

    public static InputSource getInputSource(Movie movie) throws java.io.FileNotFoundException {
        return new InputSource(new FileInputStream(getXmlFile(movie)));
    }

    public static Document getDocument(Movie movie) throws javax.xml.parsers.ParserConfigurationException, org.xml.sax.SAXException, java.io.IOException {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(getXmlFile(movie));
    }
    
    public static Document getDocument(File xmlFile) throws javax.xml.parsers.ParserConfigurationException, org.xml.sax.SAXException, java.io.IOException {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);
    }

    public static void saveXmlDocument(File xmlFile, Node node) {
        /*try {
            Document doc = node.getOwnerDocument();
            Transformer xformer = TransformerFactory.newInstance().newTransformer();
            xformer.transform(new DOMSource(doc), new StreamResult(xmlFile));
        } catch (Exception ex) {
            logger.severe(ex+": updating XML node "+node.getNodeName());
        }*/
        saveXmlDocument(xmlFile, node.getOwnerDocument());
    }
    
    public static void saveXmlDocument(File xmlFile, Document doc) {
        try {
            //Document doc = node.getOwnerDocument();
            Transformer xformer = TransformerFactory.newInstance().newTransformer();
            xformer.transform(new DOMSource(doc), new StreamResult(xmlFile));
        } catch (Exception ex) {
            logger.severe(ex+": updating XML node in "+xmlFile.getName());
        }
    }

    public static String getVideoXmlValue(Movie movie, String attribute) {
        return getVideoXmlNode(movie,attribute).getTextContent();
    }

    public static Node getVideoXmlNode(Movie movie, String attribute) {
        //File xmlFile = getXmlFile(movie);
        Node node = null;
        try {
            // Read xml and build a DOM document
            Document doc = getDocument(movie);
            if (attribute.equals("id")) {
                NodeList nl = doc.getElementsByTagName(attribute);
                for (int i = 0; i < nl.getLength(); i++) {
                    Element ele = (Element)nl.item(i);
                    if (ele.hasAttribute(moviedb) && ele.getAttribute(moviedb).equals(getProperty("yayman.selectedIdPlugin.key"))) {
                        node = ele;
                        break;
                    }
                }
                if (node == null) {
                    Element ele = doc.createElement("id");
                    ele.setAttribute(moviedb, getProperty("yayman.selectedIdPlugin.key"));
                    ele.setTextContent("");
                    node = ele;
                    Node detailsNode = doc.getElementsByTagName("movie").item(0);
                    Node dbNode = doc.getElementsByTagName("id").item(0);
                    detailsNode.insertBefore(node, dbNode);
                }
            } else if (attribute.equals("details")) {
                node = doc.getElementsByTagName(attribute).item(1);
            } else {
                node = doc.getElementsByTagName(attribute).item(0);
            }
            /*String path = "//details/movie/"+attribute;
            if (attribute.equals("id")) {
                path += "[@movieDatabase=\""+getProperty("yayman.selectedIdPlugin.key")+"\"]";
            }
            node = (Node)(xPath.evaluate(path, getDocument(movie), XPathConstants.NODE));
            if (node == null && attribute.equals("id") && createMissing) {
                Document doc = node.getOwnerDocument();
                Element ele = doc.createElement("id");
                ele.setAttribute("movieDatabase", getProperty("yayman.selectedIdPlugin.key"));
                ele.setTextContent("");
                node = ele;
                Node movieNode = (Node)(xPath.evaluate("//details/movie", doc, XPathConstants.NODE));
                Node dbNode = (Node)(xPath.evaluate("//details/movie/id[1]", doc, XPathConstants.NODE));
                movieNode.insertBefore(node, dbNode);
            }*/
        } catch (Exception ex) {
            logger.severe(ex+": getting "+movie.getBaseFilename()+" XML node "+attribute);
        }
        return node;
    }

    public static NodeList getVideoXmlNodes(Movie movie, String attribute) {
        try {
            // Read xml and build a DOM document
            Document doc = getDocument(movie);
            return doc.getElementsByTagName(attribute);
        } catch (Exception ex) {
            logger.severe(ex+": getting "+movie.getBaseFilename()+" XML node "+attribute);
        }
        return null;
    }

    public static Node getVideoIdNode(Movie movie, String dbkey) {
        Node node = null;
        try {
            String path = "//details/movie/id[@"+moviedb+"=\""+dbkey+"\"]";
            node = (Node)(xPath.evaluate(path, getDocument(movie), XPathConstants.NODE));
        } catch (Exception ex) {
            logger.severe(ex+": getting "+movie.getBaseFilename()+" XML id node for "+dbkey);
        }
        return node;
    }

    public static void updateMovieId(Movie movie, String dbkey, String newKey) {
        File xmlFile = getXmlFile(movie);
        try {
            Node node = getVideoIdNode(movie, dbkey);
            node.setTextContent(newKey);

            saveXmlDocument(xmlFile, node);
            updateMovieXmlIndices(movie);
        } catch (Exception ex) {
            logger.severe(ex+": updating XML data for "+movie.getBaseFilename());
        }
    }

    public static String getVideoId(Movie movie, String dbkey) {
        return getVideoIdNode(movie,dbkey).getTextContent();
    }

    public static void addMovieId(Movie movie, String dbkey, String newKey) {
        try {
            Document doc = getDocument(movie);
            Element ele = doc.createElement("id");
            ele.setAttribute(moviedb, dbkey);
            ele.setTextContent(newKey);
            Node movieNode = (Node)(xPath.evaluate("//details/movie", doc, XPathConstants.NODE));
            Node dbNode = (Node)(xPath.evaluate("//details/movie/id[1]", doc, XPathConstants.NODE));
            movieNode.insertBefore(ele, dbNode);
            saveXmlDocument(getXmlFile(movie), ele);
            updateMovieXmlIndices(movie);
        } catch (Exception ex) {
            logger.severe(ex+": adding "+movie.getBaseFilename()+" id "+dbkey);
        }
    }

    public static void removeMovieId(Movie movie, String dbkey) {
        Node node = getVideoIdNode(movie,dbkey);
        node.getParentNode().removeChild(node);
        saveXmlDocument(getXmlFile(movie), node);
        updateMovieXmlIndices(movie);
    }

    public static ArrayList<String> getMovieIDs(Movie movie) {
        ArrayList<String> ids = new ArrayList();
        try {
            NodeList nl = (NodeList)(xPath.evaluate("//details/movie/id", getDocument(movie), XPathConstants.NODESET));
            for (int i = 0; i < nl.getLength(); i++) {
                Element ele = (Element)nl.item(i);

                if (ele.hasAttribute(moviedb)) ids.add(ele.getAttribute(moviedb));
            }
        } catch (Exception ex) {
            logger.severe(ex+": getting "+movie.getBaseFilename()+" ids");
        }
        return ids;
    }

    public static Node getMovieFileXmlNode(Movie movie, int part, String tagName) {
        Node node = null;
        try {
            node = (Node)(xPath.evaluate("//files/file/"+tagName+"[@part="+part+"]", getDocument(movie), XPathConstants.NODE));
        } catch (Exception ex) {
            logger.severe(ex+": getting "+movie.getBaseFilename()+" XML node "+tagName);
        }
        return node;
    }

    private static Node getMovieFileXmlNodeContainingPart(Movie movie, int part) {
        Node node = null;
        try {
            node = (Node)(xPath.evaluate("//files/file[@firstPart<="+part+" and lastPart>="+part+"]", getInputSource(movie), XPathConstants.NODE));
        } catch (Exception ex) {
            logger.severe(ex+": getting "+movie.getBaseFilename()+" XML node for part "+part);
        }
        return node;
    }

    public static void makeCurrentXml(Movie movie) {
        File xmlFile = getXmlFile(movie);
        try {
            if (JukeboxInterface.getYAMJRevision() >= 1793) {
                NodeList nl = (NodeList)(xPath.evaluate("//details/movie/id", DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile), XPathConstants.NODESET));
                for (int i = 0; i < nl.getLength(); i++) {
                    Element ele = (Element)nl.item(i);
                    if (ele.hasAttribute("movieDatabase")) {//ids.add(ele.getAttribute("movieDatabase"));
                        ele.setAttribute(moviedb, ele.getAttribute("movieDatabase"));
                        ele.removeAttribute("movieDatabase");
                        saveXmlDocument(xmlFile, ele);
                    }
                }
            }
        } catch (Exception ex) {
            logger.severe(ex+": converting xml file "+xmlFile.getName()+" for "+movie.getBaseFilename()+" to current: ");
        }
    }
}
