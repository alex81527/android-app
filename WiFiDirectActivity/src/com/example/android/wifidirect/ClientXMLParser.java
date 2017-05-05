package com.example.android.wifidirect;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.jdom.Element;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;



public class ClientXMLParser {
	private Element RequestXml;
    private Element rootLabel;
	private Element hdrLabel;
	private Element bodyLabel;
	private String sessionID;
	private String msgID;
	private String targetlocURI;
	private String sourcelocURI;
	private String VerDTD = "1.2";
	private String VerPROTO = "DM/1.2";
	private int CMDIDCount;
	private int MAX_MSG_SIZE = 100;
	private int COMMANDNumber;
	private String[][] ClientRequestDataArray;
	public ClientXMLParser(String xmlData){
		saxBuild(xmlData);
		//setResponseXml();
		ClientRequestDataArray = new String[MAX_MSG_SIZE][MAX_MSG_SIZE];
		saxHeaderParse();
		saxBodyParse();
		
	}
	
	public void XMLParser(String xmlData){
		saxBuild(xmlData);
		//setResponseXml();
		ClientRequestDataArray = new String[MAX_MSG_SIZE][MAX_MSG_SIZE];
		COMMANDNumber = 0;
		saxHeaderParse();
		saxBodyParse();
	}
	
	public void saxBuild(String xmlData){
		Document docJDOM = new Document();
		SAXBuilder bSAX  = new SAXBuilder(false);

		try {
			docJDOM = bSAX.build(new StringReader(xmlData));
			
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		RequestXml = docJDOM.getRootElement();
		
	}
	
	public void saxHeaderParse(){

		List lstChildren = RequestXml.getChild("SyncHdr").getChildren();
        for(int i=0; i<lstChildren.size(); i++) {
        	Element elmtChild = (Element) lstChildren.get(i);
        	if(elmtChild.getName()=="SessionID"){
        		sessionID = elmtChild.getText();
        	}else if(elmtChild.getName()=="MsgID"){	
        		msgID = elmtChild.getText();
        	}else if(elmtChild.getName()=="Target"){
        		targetlocURI = getURI(elmtChild);
        	}else if(elmtChild.getName()=="Source"){
        		sourcelocURI = getURI(elmtChild);
        	}
        		
        	
        	
        }
	}
	
	public void saxBodyParse(){

		List lstChildren = RequestXml.getChild("SyncBody").getChildren();
        for(int i=0; i<lstChildren.size(); i++) {
        	Element elmtChild = (Element) lstChildren.get(i);
        	if(elmtChild.getName()=="Alert"){
        		handleCommandAlert(elmtChild);
        	}else if(elmtChild.getName()=="Get"){
        		handleCommandGet(elmtChild);
        	}else if(elmtChild.getName()=="Status"){
        		//handleCommandStatus(elmtChild);
        	}else if(elmtChild.getName()=="Replace"){
        		handleCommandReplace(elmtChild);
        	}
        		
        	
        }
	}
	
	public void handleCommandAlert(Element ele){
		
		String cmdID = null;
		
		List lstChildren = ele.getChildren();
        for(int i=0; i<lstChildren.size(); i++) {
	        Element elmtChild = (Element) lstChildren.get(i);
		    
	        if(elmtChild.getName()=="CmdID")
	        	cmdID = elmtChild.getText();
	        else if(elmtChild.getName()=="Data"){
	        	if(elmtChild.getText()=="1200"){
	        		handleServerInitSession(cmdID);
	        	}else if(elmtChild.getText()=="1224"){
	        		//handleClientEvent(cmdID,lstChildren);
        			return;
	        	}	
	        		
	       
	        }else{
	        	System.out.println("Undefined");
	        }

	        /*
	        if(!elmtChild.getChildren().toString().equals("[]"))
	        	handleCommandAlert(elmtChild);
	        */
		}
        
        
        
	}
	
	public void handleCommandGet(Element ele){
		
		int j = 2;
		ClientRequestDataArray[COMMANDNumber][1] = "Get";
		List lstChildren = ele.getChildren();
        for(int i=0; i<lstChildren.size(); i++) {
	        Element elmtChild = (Element) lstChildren.get(i);
		    
	        if(elmtChild.getName()=="CmdID")
	        	ClientRequestDataArray[COMMANDNumber][0] = elmtChild.getText();
	        else if(elmtChild.getName()=="Item"){
	        	List sourceList = elmtChild.getChildren("Target");
	        	Element data = (Element) sourceList.get(0);
	        	
	        	ClientRequestDataArray[COMMANDNumber][j++] = getURI(data);
	        	
	        }else{
	        	System.out.println("Undefined");
	        }
		
        }
        
        COMMANDNumber++;
		
	}
	
	public void handleCommandReplace(Element ele){

		int j = 2;
		ClientRequestDataArray[COMMANDNumber][1] = "Replace";
		List lstChildren = ele.getChildren();
		
        for(int i=0; i<lstChildren.size(); i++) {
	        Element elmtChild = (Element) lstChildren.get(i);
		    
	        if(elmtChild.getName()=="CmdID")
	        	ClientRequestDataArray[COMMANDNumber][0] = elmtChild.getText();
	        else if(elmtChild.getName()=="Item"){
	        	List sourceList = elmtChild.getChildren("Source");
	        	Element data = elmtChild.getChild("Data");  
	        	ClientRequestDataArray[COMMANDNumber][j++] = data.getText();
	        	
	        }else{
	        	System.out.println("Undefined!!");
	        }
		
        }
        COMMANDNumber++;
	}
	
	
	public void handleServerInitSession(String cmdID){
		Element statusLabel = new Element("Status");
        Element msgRefLabel = new Element("MsgRef");
        Element cmdRefLabel = new Element("CmdRef");
        Element cmdIDLabel  = new Element("CmdID");
        Element cmdLabel    = new Element("Cmd");
        Element dataLabel   = new Element("Data");
        
        msgRefLabel.addContent(msgID);
        cmdRefLabel.addContent(cmdID);
        cmdIDLabel.addContent(Integer.toString(CMDIDCount++));
        dataLabel.addContent("200");
        cmdLabel.addContent("Alert");
        
        statusLabel.addContent(msgRefLabel);
        statusLabel.addContent(cmdRefLabel);
        statusLabel.addContent(cmdLabel);
        statusLabel.addContent(cmdIDLabel);
        statusLabel.addContent(dataLabel);
        
        bodyLabel.addContent(statusLabel);
	}
	
	public String getTargetlocURI(){
		return targetlocURI;
	}
	
	public String getSourcelocURI(){
		return sourcelocURI;
	}
	
	public String getSessionID(){
		return sessionID;
	}
	
	public String getMsgID(){
		return msgID;
	}
	
	public String getURI(Element element){
		
		List sourceList = element.getChildren("LocURI");
		Element ele = (Element) sourceList.get(0);
		return ele.getText();
		
	}
	
	public String[][] getRequestData(){
		return ClientRequestDataArray;
	}
}
