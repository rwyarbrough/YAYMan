/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package yayman;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.net.*;
import java.io.*;
import java.util.regex.*;
import java.util.logging.*;
import java.util.Vector;

import javax.swing.SwingWorker;
import org.teleal.cling.*;
import org.teleal.cling.registry.*;
import org.teleal.cling.model.meta.*;
import org.teleal.cling.model.message.header.*;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;

/**
 *
 * @author Nordin
 */
public class PopAPI {
    
    private DocumentBuilder db;
    private String nmtIP;
    private String nmtModel;
    private boolean apiSupported;
    private static Logger logger = YAYManView.logger;
    private static boolean searchingNmts = false;
    //private static DefaultComboBoxModel nmtListModel = new DefaultComboBoxModel();
    private static Vector<NetworkedMediaTank> nmts = new Vector();
    private static Vector<DefaultComboBoxModel> comboBoxModels = new Vector();
    private static Vector<DefaultListModel> listModels = new Vector();
    
    public PopAPI(String ip) {
        nmtIP = ip;
        try {
            db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (Exception ex) {
            logger.severe("Error getting PopAPI DocumentBuilder: "+ex);
        }

        String html = "";
        nmtModel = null;
        apiSupported = false;
        try {
            URL url = new URL("http://"+nmtIP+":8883/maintenance.html");
            BufferedReader read = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = read.readLine();
            while (line != null) {
                html += line;
                line = read.readLine();
            }

            Pattern p = Pattern.compile("\\d{2}-\\d{2}-\\d{6}-\\d{2}-POP-\\d{3}");
            Matcher m = p.matcher(html);
            if (m.find() == true) {
                String fwNum = m.group(0);
                nmtModel = fwNum.substring(fwNum.length()-3);
                if (nmtModel.equals("402")) {
                    nmtModel = "A-100";
                } else if (nmtModel.equals("403")) {
                    nmtModel = "A-110";
                } else if (nmtModel.equals("408")) {
                    nmtModel = "C-200";
                    apiSupported = true;
                } else if (nmtModel.equals("411")) {
                    nmtModel = "A-200";
                    apiSupported = true;
                }
            }
        } catch (Exception ex) {
            logger.severe("Error getting model from NMT IP ("+nmtIP+"): "+ex);
        }
    }

    public boolean isSupported() {
        return apiSupported;
    }

    public String getModel() {
        return nmtModel;
    }

    public Document request(String module, String[] param) {
        Document doc = null;
        String url = "http://"+nmtIP+":8008/"+module+"?";
        try {
            for (int i=0; i < param.length; i++) {
                if (i!=0) url +="&";
                url += "arg"+i+"="+URLEncoder.encode(param[i],"UTF-8");
            }
        
            doc = db.parse(new URL(url).openStream());
        } catch (Exception ex)  {
            logger.severe("Error requesting module "+module+" for "+param+": "+ex);
        }
        Node returnVal = doc.getElementsByTagName("returnValue").item(0);
        if (returnVal.getTextContent().equals("1")) {
            return null;
        }
        return doc;
    }

    public Document request(String module, String param) {
        return request(module,new String[] {param});
    }

    public Document setting(String[] param) {
        return request("setting", param);
    }

    public Document setting(String param) {
        return request("setting", new String[] {param});
    }

    public Document getDevices() {
        Document doc = null;
        if(isSupported()) {
            doc = request("system","list_devices");
        } else {
            doc = getDocShell();
            String html = "";
            try {
                URL url = new URL("http://"+nmtIP+":8883/start.cgi?list");
                BufferedReader read = new BufferedReader(new InputStreamReader(url.openStream()));
                String line = read.readLine();
                while (line != null) {
                    html += line;
                    line = read.readLine();
                }

                html = html.replaceAll("\\s+", " ");
                Pattern p = Pattern.compile("href=\"http://localhost.drives:8883/(.*?)/\\?home=0");
                Matcher m = p.matcher(html);
                while (m.find()) {
                    String accessPath = m.group(1);
                    String deviceName = accessPath;
                    Element deviceEle = doc.createElement("device");
                    Element accessEle = doc.createElement("accessPath");
                    accessEle.setTextContent(accessPath);
                    Element nameEle = doc.createElement("name");
                    nameEle.setTextContent(deviceName);
                    Element typeEle = doc.createElement("type");
                    if (accessPath.startsWith("USB_")) {
                        typeEle.setTextContent("usb");
                    } else if (accessPath.toLowerCase().startsWith("cd")) {
                        typeEle.setTextContent("cd rom");
                    } else {
                        typeEle.setTextContent("harddisk");
                    }
                    deviceEle.appendChild(accessEle);
                    deviceEle.appendChild(nameEle);
                    deviceEle.appendChild(typeEle);
                    addResponse(doc,deviceEle);
                }
                setReturnValue(doc,0);
            } catch (Exception ex) {
                logger.severe("Error setting hard disk options from NMT IP ("+nmtIP+"): "+ex);
            }
        }

        return doc;
    }

    public Document getShares() {
        Document doc = null;
        if (isSupported() && !isSupported()) {
            //API is broken for getting shares right now
        } else {
            doc = getDocShell();
            String html = "";
            try {
                URL url = new URL("http://"+nmtIP+":8883/network_share.html");
                BufferedReader read = new BufferedReader(new InputStreamReader(url.openStream()));
                String line = read.readLine();
                while (line != null) {
                    html += line;
                    line = read.readLine();
                }
                html = html.replaceAll("\\s+", " ");

                Pattern p = Pattern.compile("class=\"txt\">(.*?)</td>");
                int groupi = 1;
                String shareName = "";
                if (nmtModel.equals("A-200") || nmtModel.equals("C-200")) {
                    p = Pattern.compile("<option value=\"(\\d+?)\">(.+?)</option>");
                    groupi = 2;
                }

                Matcher m = p.matcher(html);
                while (m.find()) {
                    shareName = m.group(groupi);
                     if (nmtModel.equals("A-100") || nmtModel.equals("A-110")) shareName = shareName.replaceAll("(\\d+?)\\.&nbsp;", "");
                    Element shareEle = doc.createElement("networkShare");
                    Element nameEle = doc.createElement("shareName");
                    nameEle.setTextContent(shareName);
                    Element urlEle = doc.createElement("url");
                    shareEle.appendChild(nameEle);
                    shareEle.appendChild(urlEle);
                    addResponse(doc,shareEle);
                }
                setReturnValue(doc,0);
            } catch (Exception ex) {
                logger.severe("Error setting HD options from NMT IP ("+nmtIP+"): "+ex);
            }
        }

        return doc;
    }

    public Document addShare(String protocol, String host, String folder, String shareName, String user, String pass) {
        Document doc = null;
        if (isSupported() && !isSupported()) {

            String sharePath = "";

            if (protocol.equals("0")) {
                sharePath = "smb://";
            } else if (protocol.equals("1")) {
                sharePath = "nfs://";
            } else if (protocol.equals("2")) {
                sharePath = "nfs://";
            }
            sharePath += host + ":/" + folder;
            if (user == null || user.isEmpty() || user.equals("")) pass = "";
            System.out.println("addings share: "+sharePath);
            String[] params = {"add_network_shared_folder",sharePath,user,pass};
            doc = setting(params);

        } else {
            doc = getDocShell();
            String html = "";
            try {
                String path = "http://"+nmtIP+":8883/setups.cgi?";
                path += "hiDe=6";
                path += "&proto="+protocol;
                path += "&iphost="+host;
                path += "&folder="+folder;
                path += "&name="+shareName;
                path += "&user="+user;
                path += "&passwd="+pass;
                path += "&add=add";
                path += "&cancel=cancel";
                System.out.println(path);
                URL url = new URL(path);
                BufferedReader read = new BufferedReader(new InputStreamReader(url.openStream()));
                String line = read.readLine();
                while (line != null) {
                    html += line;
                    line = read.readLine();
                }
                html = html.replaceAll("\\s+", " ");
                System.out.println(html);
            } catch (Exception ex) {
                logger.severe("Error adding share: "+ex);
            }
        }
        return doc;
    }

    private Document getDocShell() {
        Document doc = db.newDocument();
        Element root = doc.createElement("theDavidBox");
        Element requestEle = doc.createElement("request");
        Element responseEle = doc.createElement("response");
        Element returnValEle = doc.createElement("returnValue");
        returnValEle.setTextContent("1");
        root.appendChild(requestEle);
        root.appendChild(responseEle);
        root.appendChild(returnValEle);
        doc.appendChild(root);
        return doc;
    }

    private void addResponse(Document doc, Element ele) {
        NodeList nl = doc.getElementsByTagName("response");
        nl.item(0).appendChild(ele);
    }

    private void setReturnValue(Document doc, int ret) {
        if (doc != null) {
            NodeList nl = doc.getElementsByTagName("returnValue");
            if (nl.getLength() > 0) nl.item(0).setTextContent(""+ret);
        }
    }

    public boolean querySuccess(Document doc) {
        NodeList nl = doc.getElementsByTagName("returnValue");
        if (nl.item(0).getTextContent().equals("0")) {
            return true;
        }

        return false;
    }

    public static void searchNmts() {
        if (searchingNmts) return;
        searchingNmts = true;
        //nmtListModel.addElement(new NetworkedMediaTank("None"));
        try {
            Logger clingLogger = Logger.getLogger("org.teleal.cling");
            ConsoleHandler ch = new ConsoleHandler() {
                @Override
                public void publish(LogRecord record) {
                    super.publish(record);
                    if (isLoggable(record)) {
                        YAYManView.logger.severe(record.getMessage());
                    }
                }
            };
            ch.setLevel(Level.SEVERE);
            clingLogger.addHandler(ch);
            clingLogger.setLevel(Level.SEVERE);
            new SwingWorker<Void,String>() {
                public Void doInBackground() {
                    try {
                        UpnpServiceImpl service = new UpnpServiceImpl();
                        RegistryListener listener = new DefaultRegistryListener() {
                            @Override
                            public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
                                super.remoteDeviceDiscoveryStarted(registry, device);
                                //System.out.println("Discovery started: "+device.getIdentity().getUdn());
                                if (device.getDetails().getModelDetails().getModelName().equals("NMT")) {
                                    String name = device.getDetails().getFriendlyName();
                                    name = name.substring(0, name.indexOf(" "));
                                    String ip = device.getIdentity().getDescriptorURL().toString();
                                    ip = ip.replace("http://","");
                                    int portindex = ip.indexOf(":");
                                    ip = ip.substring(0,portindex);
                                    NetworkedMediaTank nmt = new NetworkedMediaTank(name, ip);
                                    //nmtListModel.addElement(nmt);
                                    nmts.add(nmt);
                                    for (DefaultComboBoxModel model : comboBoxModels) {
                                        if (model.getSize() == nmts.size()) {
                                            model.addElement(nmt);
                                        } else {
                                            model.removeAllElements();
                                            model.addElement(new NetworkedMediaTank("None"));
                                            for (NetworkedMediaTank nmt2 : nmts) {
                                                model.addElement(nmt2);
                                            }
                                        }
                                    }

                                    for (DefaultListModel model : listModels) {
                                        if (model.getSize() == nmts.size()-1) {
                                            model.addElement(nmt);
                                        } else {
                                            model.removeAllElements();
                                            for (NetworkedMediaTank nmt2 : nmts) {
                                                model.addElement(nmt2);
                                            }
                                        }
                                    }
                                }

                            }
                        };
                        service.getRegistry().addListener(listener);
                        service.getControlPoint().search(new STAllHeader());

                    } catch (Exception ex) {
                        logger.severe("UPnP search error: "+ex);
                    }
                    return null;
                }
            }.execute();
        } catch (Exception ex) {
            logger.severe("UPnP search error: "+ex);
        }
    }

    public static void syncNmtsWith(DefaultComboBoxModel model) {
        model.addElement(new NetworkedMediaTank("None"));
        comboBoxModels.add(model);
        for (NetworkedMediaTank nmt : nmts) {
            model.addElement(nmt);
        }
    }

    public static void syncNmtsWith(DefaultListModel model) {
        listModels.add(model);
        for (NetworkedMediaTank nmt : nmts) {
            model.addElement(nmt);
        }
    }
}
