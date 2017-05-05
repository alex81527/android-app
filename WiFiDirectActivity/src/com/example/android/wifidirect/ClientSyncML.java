package com.example.android.wifidirect;
import org.jdom.Element;

public class ClientSyncML {

	private Element rootLabel;
	private Element hdrLabel;
	private Element bodyLabel;
	private Element sessionIDLabel;
	private Element msgIDLabel;
	private Element targetlocURILabel;
	private Element sourcelocURILabel;
	private String VerDTD = "1.2";
	private String VerPROTO = "DM/1.2";
	private int CMDIDCount;
	private String MSG_ID;
	private String SESSION_ID;
	public ClientSyncML(){
		rootLabel = new Element("SyncML","SYNCML:SYNCML1.2");
		hdrLabel  = new Element("SyncHdr");
		bodyLabel  = new Element("SyncBody");
		CMDIDCount = 1;
		setSynchdr();
		rootLabel.addContent(hdrLabel);
		rootLabel.addContent(bodyLabel);
	}
	
	public void setSynchdr(){
		Element verDTDLabel  = new Element("VerDTD");
		Element verProtoLabel = new Element("VerProto");
		Element targetLabel = new Element("Target");
		Element sourceLabel = new Element("Source");
		
	    sessionIDLabel = new Element("SessionID");
		msgIDLabel = new Element("MsgID");
		targetlocURILabel = new Element("LocURI");
		sourcelocURILabel = new Element("LocURI");
		
		verDTDLabel.addContent(VerDTD);		
		verProtoLabel.addContent(VerPROTO);

		targetLabel.addContent(targetlocURILabel);
		sourceLabel.addContent(sourcelocURILabel);
		
		hdrLabel.addContent(verDTDLabel);
		hdrLabel.addContent(verProtoLabel);
		hdrLabel.addContent(sessionIDLabel);
		hdrLabel.addContent(msgIDLabel);
		hdrLabel.addContent(targetLabel);
		hdrLabel.addContent(sourceLabel);
		
	}
	
	public void setSessionID(String id){
		sessionIDLabel.addContent(id);
		SESSION_ID = id;
	}

	public void setMsgID(String id){
		msgIDLabel.addContent(id);
		MSG_ID = id;
	}
	
	public void setSourcelocURI(String uri){
		sourcelocURILabel.addContent(uri);
	}
	
	public void setTargetlocURI(String uri){
		targetlocURILabel.addContent(uri);
	}
	
	public void setSetupHdr(){
		
		Element metaLabel  = new Element("Meta");
		Element msgSizeLabel  = new Element("MaxMsgSize","syncml:metinf");
		
		msgSizeLabel.addContent("100");
		metaLabel.addContent(msgSizeLabel);
		hdrLabel.addContent(metaLabel);
	}
	
	public void setSetupBody(){
		
		setInitSessionCommand();
		
		
	}
	
	public void setInitSessionCommand(){
		Element alertLabel  = new Element("Alert");
		Element cmdIDLabel  = new Element("CmdID");
		Element dataLabel  = new Element("Data");
		
		cmdIDLabel.addContent(Integer.toString(CMDIDCount++));
		dataLabel.addContent("1200");
		
		alertLabel.addContent(cmdIDLabel);
		alertLabel.addContent(dataLabel);
		bodyLabel.addContent(alertLabel);
	}
	
	public void setClientInfo(String[] ClientData){
		Element alertLabel  = new Element("Alert");
		Element cmdIDLabel  = new Element("CmdID");
		Element dataLabel = new Element("Data");
		cmdIDLabel.addContent(Integer.toString(CMDIDCount++));
		alertLabel.addContent(cmdIDLabel);
		dataLabel.addContent("1226");
		alertLabel.addContent(dataLabel);
		
		
		for(int i = 0;i<ClientData.length;i++){
			Element itemTmpLabel  = new Element("Item");
			Element dataTmpLabel  = new Element("Data");
			Element sourceTmpLabel  = new Element("Source");
			Element locURITmpLabel  = new Element("LocURI");
			Element metaTmpLabel    = new Element("Meta");
			Element typeTmpLabel    = new Element("Type","syncml:metinf");
			switch(i){
				case 0:
					locURITmpLabel.addContent("./UE_Location/3GPP_Location/EUTRA_CI");
					break;
				case 1:
					locURITmpLabel.addContent("3GPP_Location/RSSI");
					break;	
				case 2:
					locURITmpLabel.addContent("Client_MAC");
					break;	
				case 3:
					locURITmpLabel.addContent("Client_IP");
					break;	
				case 4:
					locURITmpLabel.addContent("WD_MAC");
					break;	
			}
			
			sourceTmpLabel.addContent(locURITmpLabel);
			dataTmpLabel.addContent(ClientData[i]);
			typeTmpLabel.addContent("urn:oma:at:ext-3gpp-andsf:1.0:ue_location");
			
			
			metaTmpLabel.addContent(typeTmpLabel);
			itemTmpLabel.addContent(sourceTmpLabel);
			
			itemTmpLabel.addContent(metaTmpLabel);
			itemTmpLabel.addContent(dataTmpLabel);
			
			
			
			
			alertLabel.addContent(itemTmpLabel);
		}
		
	
		bodyLabel.addContent(alertLabel);
	}
	
	public void setAlertCommand(String[] dataStream){
		Element alert  = new Element("Alert");
		Element cmdID  = new Element("CmdID");
		Element data  = new Element("Data");
		
		cmdID.addContent(Integer.toString(CMDIDCount++));
		data.addContent("1224");
		
		alert.addContent(cmdID);
		alert.addContent(data);
		
		for(int i=0;i<dataStream.length;i++){
			Element itemTmp = new Element("Item");
			Element dataTmp = new Element("Data");
			dataTmp.addContent(dataStream[i]);
			itemTmp.addContent(dataTmp);
			alert.addContent(itemTmp);
		}
		
		bodyLabel.addContent(alert);
	}
	
	public void setResultCommand(int scenario,String cmdID,String[] data){
		Element resultsLabel = new Element("Results");
        Element msgRefLabel = new Element("MsgRef");
        Element cmdRefLabel = new Element("CmdRef");
        Element cmdIDLabel  = new Element("CmdID");
        
        
        msgRefLabel.addContent(MSG_ID);
        cmdRefLabel.addContent(cmdID);
        cmdIDLabel.addContent(Integer.toString(CMDIDCount++));
        
		resultsLabel.addContent(msgRefLabel);
		resultsLabel.addContent(cmdRefLabel);
		resultsLabel.addContent(cmdIDLabel);
		if(data.length==0){
			Element itemTmpLabel = new Element("Item");
			Element souceTmpLabel = new Element("Source");
			Element locURITmpLabel = new Element("LocURI");
			Element dataTmpLabel = new Element("Data");
			
			if (scenario == 3) {// wifi-direct mode
				locURITmpLabel.addContent("WiFi-DirectID_" + 0);
			} else {
				locURITmpLabel.addContent("Error");
			}
			dataTmpLabel.addContent("");
			souceTmpLabel.addContent(locURITmpLabel);
			itemTmpLabel.addContent(souceTmpLabel);
			itemTmpLabel.addContent(dataTmpLabel);

			resultsLabel.addContent(itemTmpLabel);
		}
			
		for (int i = 0; i < data.length; i++) {

			Element itemTmpLabel = new Element("Item");
			Element souceTmpLabel = new Element("Source");
			Element locURITmpLabel = new Element("LocURI");
			Element dataTmpLabel = new Element("Data");

			if (scenario == 3) {// wifi-direct mode
				locURITmpLabel.addContent("WiFi-DirectID_" + i);
			} else {
				locURITmpLabel.addContent("Error");
			}
			dataTmpLabel.addContent(data[i]);
			souceTmpLabel.addContent(locURITmpLabel);
			itemTmpLabel.addContent(souceTmpLabel);
			itemTmpLabel.addContent(dataTmpLabel);

			resultsLabel.addContent(itemTmpLabel);

		}
		
	    bodyLabel.addContent(resultsLabel);
        
	}
	
	public void setStatusCommand(String[] data){
		Element statusLabel = new Element("Status");
        Element msgRefLabel = new Element("MsgRef");
        Element cmdRefLabel = new Element("CmdRef");
        Element cmdIDLabel  = new Element("CmdID");
        Element cmdLabel    = new Element("Cmd");
        Element dataLabel   = new Element("Data");
        
        msgRefLabel.addContent(MSG_ID);
        cmdRefLabel.addContent(data[0]);
        cmdIDLabel.addContent(Integer.toString(CMDIDCount++));
        
        cmdLabel.addContent(data[1]);
	    dataLabel.addContent(data[2]);
	        
	    statusLabel.addContent(msgRefLabel);
	    statusLabel.addContent(cmdRefLabel);
	    statusLabel.addContent(cmdLabel);
	    statusLabel.addContent(cmdIDLabel);
	    
	    if(data[1].equals("Replace")){
        	Element targetTmpLabel = new Element("TargetRef");
        	targetTmpLabel.addContent("./DiscoveryInformation/WLAN_Location/SSID");
        	statusLabel.addContent(targetTmpLabel);
        }
	        
	    statusLabel.addContent(dataLabel);
	        
	    bodyLabel.addContent(statusLabel);
        
	}
	
	public void setfinalLabelInBody(){
		Element finalLabel = new Element("Final");
		bodyLabel.addContent(finalLabel);
	}
	
	public Element getSyncMLmessage(){
		setfinalLabelInBody();
		return rootLabel;
	}
	
	
	
}