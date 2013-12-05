/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package yayman;

import java.net.*;
import java.io.*;
import java.util.logging.*;
import java.util.*;
import com.moviejukebox.MovieJukebox;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

public class WebAPI extends Thread {
    private ServerSocket serverSocket;
    private YAYManView yayman;
    public static Logger logger = Logger.getLogger("yayman");
    private int port;
    private boolean keepRunning;

    public WebAPI(YAYManView program, int p) {
        yayman = program;
        port = p;
        keepRunning = true;
        this.start();
    }

    public WebAPI(YAYManView program, String p) {
        this(program, Integer.parseInt(p));
    }

    @Override
    public void run() {
        //we are now inside our own thread separated from the gui.
        serverSocket = null;
        //Pay attention, this is where things starts to cook!
        try {
            //print/send message to the guiwindow
            //logger.fine("Trying to bind to localhost on port " + Integer.toString(port) + "...");
            //make a ServerSocket and bind it to given port,
            serverSocket = new ServerSocket(port);
            serverSocket.setPerformancePreferences(2, 1, 0);
        } catch (Exception e) { //catch any errors and print errors to gui
            logger.severe("Could not bind to port: " + e.getMessage());
            return;
        }

        //go in a infinite loop, wait for connections, process request, send response
        while (keepRunning) {
            try {
                //this call waits/blocks until someone connects to the port we
                //are listening to
                Socket connectionsocket = serverSocket.accept();
                //figure out what ipaddress the client commes from, just for show!
                InetAddress client = connectionsocket.getInetAddress();
                //and print it to gui
                logger.fine(client.getHostName() + " connected to server.");
                //Read the http request from the client from the socket interface
                //into a buffer.
                BufferedReader input = new BufferedReader(new InputStreamReader(connectionsocket.getInputStream()));
                //Prepare a outputstream from us to the client,
                //this will be used sending back our response
                //(header + requested file) to the client.
                DataOutputStream output = new DataOutputStream(connectionsocket.getOutputStream());

                //as the name suggest this method handles the http request, see further down.
                //abstraction rules
                http_handler(input, output);
            } catch (Exception e) { //catch any errors, and print them
                logger.severe("Error:" + e.getMessage());
            }
        } //go back in loop, wait for next request
        try {
            serverSocket.close();
            serverSocket = null;
        } catch (Exception ex) {
            logger.severe("Error closing socket: "+ex);
        }
    }

//our implementation of the hypertext transfer protocol
//its very basic and stripped down
    private void http_handler(BufferedReader input, DataOutputStream output) {
        int method = 0; //1 get, 2 head, 0 not supported
        String http = new String(); //a bunch of strings to hold
        String path = new String(); //the various things, what http v, what path,
        String file = new String(); //what file
        String user_agent = new String(); //what user_agent
        try {
            //This is the two types of request we can handle
            //GET /index.html HTTP/1.0
            //HEAD /index.html HTTP/1.0
            String tmp = input.readLine(); //read from the stream
            String tmp2 = new String(tmp);
            tmp.toUpperCase(); //convert it to uppercase
            if (tmp.startsWith("GET")) { //compare it is it GET
                method = 1;
            } //if we set it to method 1
            if (tmp.startsWith("HEAD")) { //same here is it HEAD
                method = 2;
            } //set method to 2

            if (method == 0) { // not supported
                try {
                    output.writeBytes(construct_http_header(501, 0));
                    output.close();
                    return;
                } catch (Exception e3) { //if some error happened catch it
                    logger.severe("error:" + e3.getMessage());
                } //and display error
            }
            //}

            //tmp contains "GET /index.html HTTP/1.0 ......."
            //find first space
            //find next space
            //copy whats between minus slash, then you get "index.html"
            //it's a bit of dirty code, but bear with me...
            int start = 0;
            int end = 0;
            for (int a = 0; a < tmp2.length(); a++) {
                if (tmp2.charAt(a) == ' ' && start != 0) {
                    end = a;
                    break;
                }
                if (tmp2.charAt(a) == ' ' && start == 0) {
                    start = a;
                }
            }
            path = tmp2.substring(start + 2, end); //fill in the path
        } catch (Exception e) {
            logger.severe("error" + e.getMessage());
        } //catch any exception

        if (path.equals("favicon.ico")) return;
        //path do now have the filename to the file it wants to open
        logger.fine("Incoming http request: " + path);

        String[] pathSplit = path.split("\\?");
        String module = path.split("\\?")[0];
        String args = "";
        if (pathSplit.length > 1) args = pathSplit[1];
        String[] argSplit = args.split("&");
        HashMap<String, String> arguments = new HashMap();
        for (int i = 0; i < argSplit.length; i++) {
            String[] arg = argSplit[i].split("=");
            String key = arg[0];
            String val = arg[0];
            if (arg.length > 1) val = arg[1];
            arguments.put(key, val);
        }

        try {
            int type_is = 0;
            //find out what the filename ends with,
            //so you can construct a the right content type
            if (path.endsWith(".zip") || path.endsWith(".exe") || path.endsWith(".tar")) {
                type_is = 3;
            }
            if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
                type_is = 1;
            }
            if (path.endsWith(".gif")) {
                type_is = 2;
            }
            //write out the header, 200 ->everything is ok we are all happy.
            output.writeBytes(construct_http_header(200, 4));

            //if it was a HEAD request, we don't print any BODY
            if (method == 1) { //1 is GET 2 is head and skips the body

                /*output.writeBytes("Module: "+module+"<br>");
                output.writeBytes("Args: <br>");
                for (String s : arguments.keySet()) {
                    output.writeBytes("&nbsp;&nbsp;&nbsp;"+s+" = "+arguments.get(s)+"<br>");
                }*/
                DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = db.newDocument();
                Element requestEle = doc.createElement("request");
                Element moduleEle = doc.createElement("module");
                moduleEle.setTextContent(module);
                requestEle.appendChild(moduleEle);
                Element argsEle = doc.createElement("arguments");

                if (module.equals("yayman")) {
                    for (String key : arguments.keySet()) {
                        Element argEle = doc.createElement("arg");
                        argEle.setAttribute("name", key);
                        argEle.setAttribute("value", arguments.get(key));
                        if (key.equals("processLibrary")) {
                            logger.fine("Servicing request to process all videos...");
                            Element responseEle = doc.createElement("response");
                            if (!yayman.busy()) {
                                argEle.setAttribute("success", "true");
                                yayman.processAllVideos();
                                responseEle.setTextContent("Began processing video library.");
                            } else {
                                logger.fine("Could not process videos. YAYMan is busy.");
                                argEle.setAttribute("success", "false");
                                responseEle.setTextContent("Could not begin processing video library; YAYMan is busy.");
                            }
                            argEle.appendChild(responseEle);
                        } else if (key.equals("version")) {
                            logger.fine("Servicing request for version info...");
                            argEle.setAttribute("success", "true");
                            Element responseEle = doc.createElement("response");
                            responseEle.setTextContent("Using YAMJ "+MovieJukebox.class.getPackage().getSpecificationVersion()+", r"+MovieJukebox.class.getPackage().getImplementationVersion()+", YAYMan "+org.jdesktop.application.Application.getInstance(yayman.YAYManApp.class).getContext().getResourceMap(YAYManApp.class).getString("Application.version"));
                            argEle.appendChild(responseEle);
                        } else if (key.equals("isBusy")) {
                            logger.fine("Servicing request for busy state...");
                            argEle.setAttribute("success", "true");
                            Element responseEle = doc.createElement("response");
                            responseEle.setTextContent(""+yayman.busy());
                            argEle.appendChild(responseEle);
                        } else if (key.equals("lastYAMJMessage")) {
                            logger.fine("Servicing request for last YAMJ log message...");
                            argEle.setAttribute("success", "true");
                            Element responseEle = doc.createElement("response");
                            responseEle.setTextContent(JukeboxInterface.getLastLogMessage());
                            argEle.appendChild(responseEle);
                        }
                        argsEle.appendChild(argEle);
                    }
                }

                requestEle.appendChild(argsEle);
                doc.appendChild(requestEle);
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                DOMSource source = new DOMSource(doc);
                //StreamResult result =  new StreamResult("yaymanhttpresponse.xml");
                StreamResult result =  new StreamResult(output);
                transformer.transform(source, result);
                /*FileInputStream requestedfile = new FileInputStream("yaymanhttpresponse.xml");
                while (true) {
                    //read the file from filestream, and print out through the
                    //client-outputstream on a byte per byte base.
                    int b = requestedfile.read();
                    if (b == -1) {
                        break; //end of file
                    }
                    output.write(b);
                }
                requestedfile.close();
                new File("yaymanhttpresponse.xml").delete();*/
            }
            //clean up the files, close open handles
            output.close();
        } catch (Exception e) {}
    }


    //this method makes the HTTP header for the response
    //the headers job is to tell the browser the result of the request
    //among if it was successful or not.
    private String construct_http_header(int return_code, int file_type) {
        String s = "HTTP/1.1 ";
        //you probably have seen these if you have been surfing the web a while
        switch (return_code) {
          case 200:
            s = s + "200 OK";
            break;
          case 400:
            s = s + "400 Bad Request";
            break;
          case 403:
            s = s + "403 Forbidden";
            break;
          case 404:
            s = s + "404 Not Found";
            break;
          case 500:
            s = s + "500 Internal Server Error";
            break;
          case 501:
            s = s + "501 Not Implemented";
            break;
        }

        s = s + "\r\n"; //other header fields,
        s = s + "Connection: close\r\n"; //we can't handle persistent connections
        //s = s + "Server: YAYMan\r\n"; //server name
        s = s + "Content-Type: text/xml\r\n";

        ////so on and so on......
        s = s + "\r\n"; //this marks the end of the httpheader
        //and the start of the body
        //ok return our newly created header!
        return s;
    }

    public void stopRunning() {
        keepRunning = false;
    }
}
