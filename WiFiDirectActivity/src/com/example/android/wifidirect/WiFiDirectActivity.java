/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wifidirect;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.jdom.Document;
import org.jdom.output.XMLOutputter;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.wifidirect.DeviceListFragment.DeviceActionListener;

/**
 * An activity that uses WiFi Direct APIs to discover and connect with available
 * devices. WiFi Direct APIs are asynchronous and rely on callback mechanism
 * using interfaces to notify the application of operation success or failure.
 * The application should also register a BroadcastReceiver for notification of
 * WiFi state related events.
 */
public class WiFiDirectActivity extends Activity implements ChannelListener, DeviceActionListener {

    public static final String TAG = "wifidirectdemo";
    private WifiP2pManager manager;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;

    private final IntentFilter intentFilter = new IntentFilter();
    private Channel channel;
    private BroadcastReceiver receiver = null;
    private String ANDSF_SERVER="140.96.172.173";
    private int ANDSF_SERVER_PORT=38765;
    private boolean D=true;
    private Socket socket_;
    private TextView text;
    private TelephonyManager tm;
    private XMLOutputter outputter = new XMLOutputter();
    private ByteArrayOutputStream ClientInitMessage = new ByteArrayOutputStream() ; 
    private PrintWriter out; 
	private BufferedReader in;
	private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
	private String input;
	private String TARGET_MAC;
	private String TARGET_IP;
	private String OP;
	
	public void tester(String[] x) {
		for(String a : x)
			sendtoUI(a);
		
		wifi_direct_demo("connect",x[0]);
	}
	
	public void log(String x){
		System.out.println(x);
		sendtoUI(x);
	}
	
    public void wifi_direct_demo(String x, String deviceAddress) {
    	
    	if(x.equals("discover")) {
	    	//******************* Discover peers ****************************************
	    	//where peers are stored: in function "onPeersAvailable" in DeviceListFragment.java 
	    	
	    	/*
	    	final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
	                .findFragmentById(R.id.frag_list);
	        fragment.onInitiateDiscovery();
	        */
	        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
	
	            @Override
	            public void onSuccess() {
	                Toast.makeText(WiFiDirectActivity.this, "Discovery Initiated",
	                        Toast.LENGTH_SHORT).show();
	            }
	
	            @Override
	            public void onFailure(int reasonCode) {
	                Toast.makeText(WiFiDirectActivity.this, "Discovery Failed : " + reasonCode,
	                        Toast.LENGTH_SHORT).show();
	            }
	        }); 
	        //*****************************************************************************
    	} 
    	else if(x.equals("connect")) {
    		try {
    			log("===Exec WiFi-Direct===");
    			log("target: "+deviceAddress);
    			log("===Exec WiFi-Direct===");;
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		//******************* Connect peers ****************************************
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = deviceAddress;
            config.wps.setup = WpsInfo.PBC;
            connect(config);
        	//*****************************************************************************
    	}

    }
       
    public void andsf_init() throws IOException{
    	Process p ;
		BufferedReader stdInput;
    	
    	//get MAC address and rssi from target associated AP 
		p = Runtime.getRuntime().exec(new String[] { "/system/bin/sh","-c","iwconfig wlan0 | grep Access | sed s/'.*Point: '//g | sed s/' '//g" });
		stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String WIFI_MAC = stdInput.readLine();
		
		p = Runtime.getRuntime().exec(new String[] { "/system/bin/sh","-c","iwconfig wlan0 | grep Signal | sed s/'.*level='//g | sed s/' dBm'//g  | sed s/' '//g" });
		stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String WIFI_RSSI = stdInput.readLine();
		
		p = Runtime.getRuntime().exec(new String[] { "/system/bin/sh","-c","busybox ifconfig wlan0 | grep 'inet addr' | sed s/'.*inet addr:'//g | sed s/'  Bcast.*'//g  | sed s/' '//g" });
		stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String MY_WIFI_IP = stdInput.readLine();
		
		p = Runtime.getRuntime().exec(new String[] { "/system/bin/sh","-c","busybox ifconfig wlan0 | grep HWaddr | sed s/'.*HWaddr '//g  | sed s/' '//g" });
		stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String MY_WIFI_MAC = stdInput.readLine();
		
		p = Runtime.getRuntime().exec(new String[] { "/system/bin/sh","-c","busybox ifconfig p2p0 | grep HWaddr | sed s/'.*HWaddr '//g  | sed s/' '//g" });
		stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String DIRECT_MAC = stdInput.readLine();
		
		if(D) {
			System.out.println("[init_wifi] WIFI_MAC "+WIFI_MAC);
			System.out.println("[init_wifi] WIFI_RSSI "+WIFI_RSSI);
			System.out.println("[init_wifi] MY_WIFI_MAC "+MY_WIFI_MAC);
			System.out.println("[init_wifi] MY_WIFI_IP "+MY_WIFI_IP);
			System.out.println("[init_wifi] DIRECT_MAC "+DIRECT_MAC);
			System.out.println("[init_wifi] Calling ANDSF_Client");
			sendtoUI("[init_wifi] WIFI_MAC "+WIFI_MAC);
			sendtoUI("[init_wifi] WIFI_RSSI "+WIFI_RSSI);
			sendtoUI("[init_wifi] MY_WIFI_MAC "+MY_WIFI_MAC);
			sendtoUI("[init_wifi] MY_WIFI_IP "+MY_WIFI_IP);
			sendtoUI("[init_wifi] DIRECT_MAC "+DIRECT_MAC);
			sendtoUI("[init_wifi] Calling ANDSF_Client");
		}
		
    	sendtoUI("[andsf_init] begin");
    	ClientSyncML a = new ClientSyncML();		
		String IMEI = tm.getDeviceId();
		String ControllerIP = ANDSF_SERVER;
		String[] UEData = {WIFI_MAC,WIFI_RSSI,MY_WIFI_MAC,MY_WIFI_IP,DIRECT_MAC};
		a.setSessionID("1");
		a.setMsgID("3");
		a.setSourcelocURI(IMEI);
		a.setTargetlocURI(ControllerIP);
		a.setSetupHdr();
		a.setSetupBody();
		a.setClientInfo(UEData);
		
		Document document = new Document(a.getSyncMLmessage());
		try {
			System.out.println("ClientMessage:");
			outputter.output(document, System.out);
			System.out.println("********************************************");
			outputter.output(document, ClientInitMessage);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		out.println(ClientInitMessage.toString());
		sendtoUI("[andsf_init] end");
    }
    
    public void clientResponse(String serverMessage) throws IOException, InterruptedException {
    	sendtoUI("[clientResponse] begin");
		XMLOutputter outputter = new XMLOutputter();
		ClientXMLParser parser = new ClientXMLParser(serverMessage);
		ClientSyncML clientSyncML = new ClientSyncML();
		ByteArrayOutputStream clientMessage = new ByteArrayOutputStream(); 
		String targetLocURI = parser.getTargetlocURI();
		String sourceLocURI = parser.getSourcelocURI();
		String sessionID    = parser.getSessionID();
		String msgID    = parser.getMsgID();
		String[][] requestData = parser.getRequestData();
		
		clientSyncML.setMsgID(msgID);
		clientSyncML.setSessionID(sessionID);
		clientSyncML.setSourcelocURI(targetLocURI);
		clientSyncML.setTargetlocURI(sourceLocURI);
		
		
		
		System.out.println("ClientParseData");
		for(int i = 0;i<requestData.length;i++){
			if(requestData[i][0]==null) break;
			for(int j = 0;j<requestData[i].length;j++){
				if(requestData[i][j]==null) break;
				System.out.print(requestData[i][j]+" ");
				
			}
			System.out.println();
		}
		System.out.println("************************");
	
		int scenario = 0;
		String[][] data = null;
		if(requestData[0][2].equals("./UE_Location/WiFi_Direct")){   //Wi-FiDirect 
			wifi_direct_demo("discover","dont care");
			return;
		}else{
			return;
		}
		
		
		
	}

    public void clientFinish(String serverMessage) {
    	sendtoUI("[clientFinish] begin");
		XMLOutputter outputter = new XMLOutputter();
		ClientXMLParser parser = new ClientXMLParser(serverMessage);
		ClientSyncML clientSyncML = new ClientSyncML();
		ByteArrayOutputStream clientMessage = new ByteArrayOutputStream(); 
		String targetLocURI = parser.getTargetlocURI();
		String sourceLocURI = parser.getSourcelocURI();
		String sessionID    = parser.getSessionID();
		String msgID    = parser.getMsgID();
		String[][] requestData = parser.getRequestData();
		
		clientSyncML.setMsgID(msgID);
		clientSyncML.setSessionID(sessionID);
		clientSyncML.setSourcelocURI(targetLocURI);
		clientSyncML.setTargetlocURI(sourceLocURI);
		
		System.out.println("ClientParseData");
		for(int i = 0;i<requestData.length;i++){
			if(requestData[i][0]==null) break;
			for(int j = 0;j<requestData[i].length;j++){
				if(requestData[i][j]==null) break;
				System.out.print(requestData[i][j]+" ");
				
			}
			System.out.println();
		}
		System.out.println("************************");
		
		OP = requestData[0][2];   
		TARGET_MAC = requestData[0][3]; 
		TARGET_IP = requestData[0][4];   
		
		DeviceDetailFragment fragment = (DeviceDetailFragment)getFragmentManager().findFragmentById(R.id.frag_detail);
		fragment.set(TARGET_MAC, TARGET_IP, OP);
		
		String[] responseData2 = {requestData[0][0],requestData[0][1],"200"};
		clientSyncML.setStatusCommand(responseData2);
		
		Document document = new Document(clientSyncML.getSyncMLmessage());
		try {
			System.out.println("clientFinishRequest:");
			outputter.output(document, System.out);
			System.out.println("********************************************");
			clientMessage = new ByteArrayOutputStream(); 
			outputter.output(document, clientMessage);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(OP.equals("2")){
			sendtoUI("OP = 2 No peers found!!!");
			return;
		}
				
		
		out.println(clientMessage.toString());
		sendtoUI("[clientFinish] end");
		wifi_direct_demo("connect", TARGET_MAC);
		
	}
    
    public void andsf_response() throws IOException{
		System.out.println("ANDSF_Response:");
		System.out.println(in.readLine());
		System.out.println(in.readLine());
		
		System.out.println("***************************");
	}
    
    public String waitServerRequest() throws IOException{
		String str = in.readLine();
		str += in.readLine();
		str += in.readLine();
		return str;
	}

    public void send_peers_info(String[] x) {
    	log("===Peers List===");
    	for(String a: x)
    		log(a);
    	log("===Peers List===");
    	sendtoUI("[andsf_receive] Get99999999999");
    	ClientXMLParser parser = new ClientXMLParser(input);
		ClientSyncML clientSyncML = new ClientSyncML();
		
		String targetLocURI = parser.getTargetlocURI();
		String sourceLocURI = parser.getSourcelocURI();
		String sessionID    = parser.getSessionID();
		String msgID    = parser.getMsgID();
		String[][] requestData = parser.getRequestData();
		
		clientSyncML.setMsgID(msgID);
		clientSyncML.setSessionID(sessionID);
		clientSyncML.setSourcelocURI(targetLocURI);
		clientSyncML.setTargetlocURI(sourceLocURI);
		
		System.out.println("ClientParseData");
		for(int i = 0;i<requestData.length;i++){
			if(requestData[i][0]==null) break;
			for(int j = 0;j<requestData[i].length;j++){
				if(requestData[i][j]==null) break;
				System.out.print(requestData[i][j]+" ");
				
			}
			System.out.println();
		}
		System.out.println("************************");
	
		int scenario = 0;
		String[][] data = null;
		
		if(requestData[0][2].equals("./UE_Location/WiFi_Direct")){   //Wi-FiDirect 
			
			scenario = 3;
		}else{
			
		}
	
		
		sendtoUI("[andsf_receive] Get");
		//responseData2 = {"1","Get","200"};  
		String[] responseData2 = {requestData[0][0],requestData[0][1],"200"};
		clientSyncML.setStatusCommand(responseData2);	

		String[] WifiData = x;
		clientSyncML.setResultCommand(scenario,requestData[0][0],WifiData);
		
		Document document = new Document(clientSyncML.getSyncMLmessage());
		XMLOutputter outputter = new XMLOutputter();
		ByteArrayOutputStream clientMessage = new ByteArrayOutputStream();
		try {
			System.out.println("ClientMessage:");
			outputter.output(document, System.out);
			System.out.println("********************************************");
			clientMessage = new ByteArrayOutputStream(); 
			outputter.output(document, clientMessage);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		out.println(clientMessage.toString());
		System.out.println("=======================================");
		System.out.println(clientMessage.toString());
		System.out.println("=======================================");
		sendtoUI("[andsf_receive]5555555555555555555555555555 end");	
    }
    /*
    @Override
	public void onConnectionInfoAvailable(final WifiP2pInfo info) {

		// After the group negotiation, we assign the group owner as the file
		// server. The file server is single threaded, single connection server
		// socket.
        try {
			if (info.groupFormed && info.isGroupOwner) {
				ServerSocket serverSocket = new ServerSocket(48765);
				Socket clientSocket = serverSocket.accept();
	
				Process p;
				BufferedReader stdInput;
				p = Runtime.getRuntime().exec(new String[] { "/system/bin/sh", "-c","su -c 'wpa_cli -i wlan0 interface | grep p2p-'" });
				stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String p2p_interface = stdInput.readLine();
				p = Runtime.getRuntime().exec(new String[] {"/system/bin/sh","-c","busybox ifconfig wlan0 | busybox grep 'inet addr' | busybox sed s/'.*inet addr:'//g | busybox sed s/'  Bcast.*'//g" });
				stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String ip = stdInput.readLine();
				
				p = Runtime.getRuntime().exec(new String[] {"/system/bin/sh","-c","busybox ifconfig "+ p2p_interface+ " | busybox grep 'inet addr' | busybox sed s/'.*inet addr:'//g | busybox sed s/'  Bcast.*'//g" });
				stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String p2p_ip = stdInput.readLine();
				String target_p2p_ip = clientSocket.getRemoteSocketAddress().toString();
				if (D) {
					System.out.println("[Go] p2p_interface: " + p2p_interface);
					sendtoUI("[Go] p2p_interface: " + p2p_interface);
				}
	
				if (OP.equals("3")) { // scenario 3
					// for skype call, an ipip tunnel needs to e established
					Runtime.getRuntime().exec(new String[] { "/system/bin/sh", "-c","su -c 'insmod /system/lib/modules/ipip.ko'" });
					if (D) {
						System.out.println("[Go] Loading ipip.ko");
						sendtoUI("[Go] Loading ipip.ko");
					}
					Runtime.getRuntime().exec(new String[] {"/system/bin/sh","-c","su -c 'busybox iptunnel add mytun mode ipip remote "+ target_p2p_ip + " local " + p2p_ip+ " dev " + p2p_interface + "'" });
					Runtime.getRuntime().exec(new String[] {"/system/bin/sh","-c","su -c 'busybox ifconfig mytun " + ip+ " pointopoint " + TARGET_IP + " up'" });
					if (D) {
						System.out.println("[Go] Tunnel established.");
						sendtoUI("[Go] Tunnel established.");
					}
				} else if (OP.equals("1")) { // scenario 4, relay node
					Runtime.getRuntime().exec(new String[] {"/system/bin/sh", "-c","su -c 'echo 1 > /proc/sys/net/ipv4/ip_forward'" });
					Runtime.getRuntime().exec(new String[] {"/system/bin/sh","-c","su -c 'iptables -t nat -A POSTROUTING -s 192.168.49.0/24 -o wlan0 -j MASQUERADE'" });
					if (D) {
						System.out.println("[Go] NAT rules established.");
						sendtoUI("[Go] NAT rules established.");
					}
				} else if (OP.equals("0")) { // scenario 4, relay client
					Runtime.getRuntime().exec(new String[] {"/system/bin/sh", "-c","su -c 'ip route del default'" });
					Runtime.getRuntime().exec(new String[] {"/system/bin/sh","-c","su -c 'ip route add default via "+ target_p2p_ip +" dev "+ p2p_interface + "'" });
					if (D) {
						System.out.println("[Client] Default route changed.");
						sendtoUI("[Client] Default route changed.");
					}	
				}
				
				clientSocket.close();
			} else if (info.groupFormed) {
				Thread.sleep(3000); //hopefully prevent client from connecting to GO when it's not yet ready
				Socket skt = new Socket("192.168.49.1", 48765);
				
				Process p;
				BufferedReader stdInput;
				p = Runtime.getRuntime().exec(new String[] { "/system/bin/sh", "-c","su -c 'wpa_cli -i wlan0 interface | grep p2p-'" });
				stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String p2p_interface_ = stdInput.readLine();
				
				p = Runtime.getRuntime().exec(new String[] {"/system/bin/sh","-c","busybox ifconfig wlan0 | busybox grep 'inet addr' | busybox sed s/'.*inet addr:'//g | busybox sed s/'  Bcast.*'//g" });
				stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String ip_ = stdInput.readLine();
				
				p = Runtime.getRuntime().exec(new String[] {"/system/bin/sh","-c","busybox ifconfig "+ p2p_interface_+ " | busybox grep 'inet addr' | busybox sed s/'.*inet addr:'//g | busybox sed s/'  Bcast.*'//g" });
				stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String p2p_ip_ = stdInput.readLine();
				
				String target_p2p_ip_ = "192.168.49.1";
				if (D) {
					System.out.println("[Client] p2p_interface: " + p2p_interface_);
					sendtoUI("[Client] p2p_interface: " + p2p_interface_);
				}
					if (OP.equals("3")) { // scenario 3
					// for skype call, an ipip tunnel needs to e established
					Runtime.getRuntime().exec(new String[] { "/system/bin/sh", "-c","su -c 'insmod /system/lib/modules/ipip.ko'" });
					if (D) {							System.out.println("[Client] Loading ipip.ko");
						sendtoUI("[Client] Loading ipip.ko");
					}
					Runtime.getRuntime().exec(new String[] {"/system/bin/sh","-c","su -c 'busybox iptunnel add mytun mode ipip remote "+ target_p2p_ip_ + " local "+ p2p_ip_ + " dev " + p2p_interface_+ "'" });
					Runtime.getRuntime().exec(new String[] {"/system/bin/sh","-c","su -c 'busybox ifconfig mytun " + ip_+ " pointopoint " + TARGET_IP+ " up'" });
					if (D) {
						System.out.println("[Client] Tunnel established.");
						sendtoUI("[Client] Tunnel established.");
					}
				} else if (OP.equals("1")) { // scenario 4, relay node
					Runtime.getRuntime().exec(new String[] { "/system/bin/sh", "-c","su -c 'echo 1 > /proc/sys/net/ipv4/ip_forward'" });
					Runtime.getRuntime().exec(new String[] {"/system/bin/sh","-c","su -c 'iptables -t nat -A POSTROUTING -s 192.168.49.0/24 -o wlan0 -j MASQUERADE'" });
					if (D) {
						System.out.println("[Client] NAT rules established.");
						sendtoUI("[Client] NAT rules established.");
					}
				} else if (OP.equals("0")) { // scenario 4, relay client
					Runtime.getRuntime().exec(new String[] { "/system/bin/sh", "-c","su -c 'ip route del default'" });
					Runtime.getRuntime().exec(new String[] {"/system/bin/sh","-c","su -c 'ip route add default via 192.168.49.1 dev "+ p2p_interface_ + "'" });
					if (D) {
						System.out.println("[Client] Default route changed.");
						sendtoUI("[Client] Default route changed.");
					}
				}
					
				skt.close();
			}
        } catch (IOException e) {
        	e.printStackTrace();
        } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	*/
    public void andsf_receive(String x) throws IOException, InterruptedException{
    	sendtoUI("[andsf_receive] begin");
    	input=x;
    	ClientXMLParser parser = new ClientXMLParser(x);
		ClientSyncML clientSyncML = new ClientSyncML();
		
		String targetLocURI = parser.getTargetlocURI();
		String sourceLocURI = parser.getSourcelocURI();
		String sessionID    = parser.getSessionID();
		String msgID    = parser.getMsgID();
		String[][] requestData = parser.getRequestData();
		
		clientSyncML.setMsgID(msgID);
		clientSyncML.setSessionID(sessionID);
		clientSyncML.setSourcelocURI(targetLocURI);
		clientSyncML.setTargetlocURI(sourceLocURI);
		
		if(requestData[0][0]==null) 
			return ;
		else{
			for(int i = 0;i<requestData.length;i++){
				if(requestData[i][0]==null) break;
				for(int j = 0;j<requestData[i].length;j++){
					if(requestData[i][j]==null) break;
					System.out.print(requestData[i][j]+" ");
				}
				System.out.println();
			}
			System.out.println("************************");
			
			
			if(requestData[0][1].equals("Get")){
				
				wifi_direct_demo("discover","dont care");

			}else if(requestData[0][1].equals("Replace")){
				sendtoUI("[andsf_receive] Replace");
				String[] responseData2 = {"1","Replace","200"};
				clientSyncML.setStatusCommand(responseData2);
				
				OP = requestData[0][2];   
				TARGET_MAC = requestData[0][3]; 
				TARGET_IP = requestData[0][4];   
				
				wifi_direct_demo("connect", TARGET_MAC);	
			}
			
			
			Document document = new Document(clientSyncML.getSyncMLmessage());
			XMLOutputter outputter = new XMLOutputter();
			ByteArrayOutputStream clientMessage = new ByteArrayOutputStream();
			try {
				System.out.println("ClientMessage:");
				outputter.output(document, System.out);
				System.out.println("********************************************");
				clientMessage = new ByteArrayOutputStream(); 
				outputter.output(document, clientMessage);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			out.println(clientMessage.toString());
		}
		sendtoUI("[andsf_receive] end");	
    }
    
    private class Cmd_Listener implements Runnable{
    	
    	@Override
		public void run(){
    		try {
    			if(D){ System.out.println("[Cmd_Listener] Creating socket"); sendtoUI("[Cmd_Listener] Creating socket");}
    			socket_ = new Socket(ANDSF_SERVER, ANDSF_SERVER_PORT);
    			out = new PrintWriter(socket_.getOutputStream(), true);
    			in = new BufferedReader(new InputStreamReader(socket_.getInputStream()));
    			if(D){ System.out.println("[Cmd_Listener] socket ok"); sendtoUI("[Cmd_Listener] socket ok");}
    			BufferedReader in = new BufferedReader(new InputStreamReader(socket_.getInputStream()));
    			
    			andsf_init();
    			andsf_response();
    			
    			while(true){
    				
    				//package 0 :from server
	    			input = waitServerRequest();
	    			
	    			//package 1 : to server
	    			andsf_init();
	    			
	    			
	    			//package 2: from server //scan peer request
	    			input = waitServerRequest();
	    			//package 3: to server  //response peer list
	    			//andsf_receive(input);
	    			clientResponse(input);
	    			
	    			//package 4: from server //selected peer
	    			input = waitServerRequest();
	    			//package3:send finish 
	    			clientFinish(input);
	    			//package4:recevie andsf command
	    			input = waitServerRequest();
	    			System.out.println("ServerRequest3");
	    			System.out.println(input);
	    			System.out.println("**************************************");
	    			break;
	    			
	    			
	    			/*
	    			if(input.equals("4")){
						if(D){
							System.out.println("Received 4 --> Starting scan wifi direct");
							sendtoUI("Received 4 --> Starting scan wifi direct");
						}
						scan_wifi_direct();
					}*/
    			}
    			/*ServerSocket serverSocket = new ServerSocket(portNumber);	
    			while (true) {
    				Socket clientSocket = serverSocket.accept();
    				new Thread(new handler(clientSocket)).start();	
    			}
    			*/
    		} catch (IOException e) {
    			System.out.println(e.getMessage());
    		} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	} 		
    }
    
    public void sendtoUI(final String x){
    	runOnUiThread(new Runnable() {
            @Override
			public void run() {
            	text.append(x+"\n");
            }
        });
    }
    
    /**
     * @param isWifiP2pEnabled the isWifiP2pEnabled to set
     */
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // add necessary intent values to be matched.

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        
        tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        
        text = (TextView) findViewById(R.id.textView1);
        text.setText("");
        text.setMovementMethod(new ScrollingMovementMethod());
        setIsWifiP2pEnabled(true);
        
        //wifi_direct_demo("discover","ddd");
        new Thread(new Cmd_Listener()).start();
    }

    /** register the BroadcastReceiver with the intent values to be matched */
    @Override
    public void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    /**
     * Remove all peers and clear all fields. This is called on
     * BroadcastReceiver receiving a state change event.
     */
    public void resetData() {
        DeviceListFragment fragmentList = (DeviceListFragment) getFragmentManager()
                .findFragmentById(R.id.frag_list);
        DeviceDetailFragment fragmentDetails = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        if (fragmentList != null) {
            fragmentList.clearPeers();
        }
        if (fragmentDetails != null) {
            fragmentDetails.resetViews();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_items, menu);
        return true;
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.atn_direct_enable:
                if (manager != null && channel != null) {

                    // Since this is the system wireless settings activity, it's
                    // not going to send us a result. We will be notified by
                    // WiFiDeviceBroadcastReceiver instead.

                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                } else {
                    Log.e(TAG, "channel or manager is null");
                }
                return true;

            case R.id.atn_direct_discover:
                if (!isWifiP2pEnabled) {
                    Toast.makeText(WiFiDirectActivity.this, R.string.p2p_off_warning,
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
                        .findFragmentById(R.id.frag_list);
                fragment.onInitiateDiscovery();
                manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(WiFiDirectActivity.this, "Discovery Initiated",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(WiFiDirectActivity.this, "Discovery Failed : " + reasonCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void showDetails(WifiP2pDevice device) {
        DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        fragment.showDetails(device);

    }

    @Override
    public void connect(WifiP2pConfig config) {
        manager.connect(channel, config, new ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(WiFiDirectActivity.this, "Connect failed. Retry.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void disconnect() {
        final DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        fragment.resetViews();
        manager.removeGroup(channel, new ActionListener() {

            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);

            }

            @Override
            public void onSuccess() {
                fragment.getView().setVisibility(View.GONE);
            }

        });
    }

    @Override
    public void onChannelDisconnected() {
        // we will try once more
        if (manager != null && !retryChannel) {
            Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show();
            resetData();
            retryChannel = true;
            manager.initialize(this, getMainLooper(), this);
        } else {
            Toast.makeText(this,
                    "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void cancelDisconnect() {

        /*
         * A cancel abort request by user. Disconnect i.e. removeGroup if
         * already connected. Else, request WifiP2pManager to abort the ongoing
         * request
         */
        if (manager != null) {
            final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
                    .findFragmentById(R.id.frag_list);
            if (fragment.getDevice() == null
                    || fragment.getDevice().status == WifiP2pDevice.CONNECTED) {
                disconnect();
            } else if (fragment.getDevice().status == WifiP2pDevice.AVAILABLE
                    || fragment.getDevice().status == WifiP2pDevice.INVITED) {

                manager.cancelConnect(channel, new ActionListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(WiFiDirectActivity.this, "Aborting connection",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(WiFiDirectActivity.this,
                                "Connect abort request failed. Reason Code: " + reasonCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

    }
}
