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

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.android.wifidirect.DeviceListFragment.DeviceActionListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener{

    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private View mContentView = null;
    private WifiP2pDevice device;
    private WifiP2pInfo info;
    ProgressDialog progressDialog = null;
	private String TARGET_MAC;
	private String TARGET_IP;
	private String OP;
	private boolean D = true;

	public void set(String a, String b, String c){
		TARGET_MAC = a;
		TARGET_IP = b;
		OP = c;
	}
	
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mContentView = inflater.inflate(R.layout.device_detail, null);
        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                        "Connecting to :" + device.deviceAddress, true, true
//                        new DialogInterface.OnCancelListener() {
//
//                            @Override
//                            public void onCancel(DialogInterface dialog) {
//                                ((DeviceActionListener) getActivity()).cancelDisconnect();
//                            }
//                        }
                        );
                ((DeviceActionListener) getActivity()).connect(config);

            }
        });

        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        ((DeviceActionListener) getActivity()).disconnect();
                    }
                });

        mContentView.findViewById(R.id.btn_start_client).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // Allow user to pick an image from Gallery or other
                        // registered apps
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
                    }
                });

        return mContentView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // User has picked an image. Transfer it to group owner i.e peer using
        // FileTransferService.
        Uri uri = data.getData();
        TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
        statusText.setText("Sending: " + uri);
        Log.d(WiFiDirectActivity.TAG, "Intent----------- " + uri);
        Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                info.groupOwnerAddress.getHostAddress());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
        getActivity().startService(serviceIntent);
    }
/*
    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        this.info = info;
        this.getView().setVisibility(View.VISIBLE);

        // The owner IP is now known.
        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text)
                + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
                        : getResources().getString(R.string.no)));

        // InetAddress from WifiP2pInfo struct.
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());

        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        if (info.groupFormed && info.isGroupOwner) {
            new FileServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text))
                    .execute();
        } else if (info.groupFormed) {
            // The other device acts as the client. In this case, we enable the
            // get file button.
            mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);
            ((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources()
                    .getString(R.string.client_text));
        }

        // hide the connect button
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
    }
*/
    
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
					//sendtoUI("[Go] p2p_interface: " + p2p_interface);
				}
	
				if (OP.equals("3")) { // scenario 3
					// for skype call, an ipip tunnel needs to e established
					Runtime.getRuntime().exec(new String[] { "/system/bin/sh", "-c","su -c 'insmod /system/lib/modules/ipip.ko'" });
					if (D) {
						System.out.println("[Go] Loading ipip.ko");
						//sendtoUI("[Go] Loading ipip.ko");
					}
					Runtime.getRuntime().exec(new String[] {"/system/bin/sh","-c","su -c 'busybox iptunnel add mytun mode ipip remote "+ target_p2p_ip + " local " + p2p_ip+ " dev " + p2p_interface + "'" });
					Runtime.getRuntime().exec(new String[] {"/system/bin/sh","-c","su -c 'busybox ifconfig mytun " + ip+ " pointopoint " + TARGET_IP + " up'" });
					if (D) {
						System.out.println("[Go] Tunnel established.");
						//sendtoUI("[Go] Tunnel established.");
					}
				} else if (OP.equals("1")) { // scenario 4, relay node
					Runtime.getRuntime().exec(new String[] {"/system/bin/sh", "-c","su -c 'echo 1 > /proc/sys/net/ipv4/ip_forward'" });
					Runtime.getRuntime().exec(new String[] {"/system/bin/sh","-c","su -c 'iptables -t nat -A POSTROUTING -s 192.168.49.0/24 -o wlan0 -j MASQUERADE'" });
					if (D) {
						System.out.println("[Go] NAT rules established.");
						//sendtoUI("[Go] NAT rules established.");
					}
				} else if (OP.equals("0")) { // scenario 4, relay client
					Runtime.getRuntime().exec(new String[] {"/system/bin/sh", "-c","su -c 'ip route del default'" });
					Runtime.getRuntime().exec(new String[] {"/system/bin/sh","-c","su -c 'ip route add default via "+ target_p2p_ip +" dev "+ p2p_interface + "'" });
					if (D) {
						System.out.println("[Client] Default route changed.");
						//sendtoUI("[Client] Default route changed.");
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
					//sendtoUI("[Client] p2p_interface: " + p2p_interface_);
				}
					if (OP.equals("3")) { // scenario 3
					// for skype call, an ipip tunnel needs to e established
					Runtime.getRuntime().exec(new String[] { "/system/bin/sh", "-c","su -c 'insmod /system/lib/modules/ipip.ko'" });
					if (D) {							System.out.println("[Client] Loading ipip.ko");
						//sendtoUI("[Client] Loading ipip.ko");
					}
					Runtime.getRuntime().exec(new String[] {"/system/bin/sh","-c","su -c 'busybox iptunnel add mytun mode ipip remote "+ target_p2p_ip_ + " local "+ p2p_ip_ + " dev " + p2p_interface_+ "'" });
					Runtime.getRuntime().exec(new String[] {"/system/bin/sh","-c","su -c 'busybox ifconfig mytun " + ip_+ " pointopoint " + TARGET_IP+ " up'" });
					if (D) {
						System.out.println("[Client] Tunnel established.");
						//sendtoUI("[Client] Tunnel established.");
					}
				} else if (OP.equals("1")) { // scenario 4, relay node
					Runtime.getRuntime().exec(new String[] { "/system/bin/sh", "-c","su -c 'echo 1 > /proc/sys/net/ipv4/ip_forward'" });
					Runtime.getRuntime().exec(new String[] {"/system/bin/sh","-c","su -c 'iptables -t nat -A POSTROUTING -s 192.168.49.0/24 -o wlan0 -j MASQUERADE'" });
					if (D) {
						System.out.println("[Client] NAT rules established.");
						//sendtoUI("[Client] NAT rules established.");
					}
				} else if (OP.equals("0")) { // scenario 4, relay client
					Runtime.getRuntime().exec(new String[] { "/system/bin/sh", "-c","su -c 'ip route del default'" });
					Runtime.getRuntime().exec(new String[] {"/system/bin/sh","-c","su -c 'ip route add default via 192.168.49.1 dev "+ p2p_interface_ + "'" });
					if (D) {
						System.out.println("[Client] Default route changed.");
						//sendtoUI("[Client] Default route changed.");
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
    
    /**
     * Updates the UI with device data
     * 
     * @param device the device to be displayed
     */
    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(device.toString());

    }

    /**
     * Clears the UI fields after a disconnect or direct mode disable operation.
     */
    public void resetViews() {
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.status_text);
        view.setText(R.string.empty);
        mContentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
        this.getView().setVisibility(View.GONE);
    }

    /**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    public static class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;
        private TextView statusText;

        /**
         * @param context
         * @param statusText
         */
        public FileServerAsyncTask(Context context, View statusText) {
            this.context = context;
            this.statusText = (TextView) statusText;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                ServerSocket serverSocket = new ServerSocket(8988);
                Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
                Socket client = serverSocket.accept();
                Log.d(WiFiDirectActivity.TAG, "Server: connection done");
                final File f = new File(Environment.getExternalStorageDirectory() + "/"
                        + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
                        + ".jpg");

                File dirs = new File(f.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                f.createNewFile();

                Log.d(WiFiDirectActivity.TAG, "server: copying files " + f.toString());
                InputStream inputstream = client.getInputStream();
                copyFile(inputstream, new FileOutputStream(f));
                serverSocket.close();
                return f.getAbsolutePath();
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                statusText.setText("File copied - " + result);
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + result), "image/*");
                context.startActivity(intent);
            }

        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            statusText.setText("Opening a server socket");
        }

    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(WiFiDirectActivity.TAG, e.toString());
            return false;
        }
        return true;
    }

}
