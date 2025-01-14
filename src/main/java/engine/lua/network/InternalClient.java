/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.network;

import java.io.IOException;
import java.util.ArrayList;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.luaj.vm2.LuaTable;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import engine.Game;
import engine.InternalGameThread;
import engine.InternalRenderThread;
import engine.io.Load;
import engine.lua.lib.LuaUtil;
import engine.lua.network.internal.ClientProcessable;
import engine.lua.network.internal.GZIPUtil;
import engine.lua.network.internal.PingRequest;
import engine.lua.network.internal.protocol.ClientConnectFinishTCP;
import engine.lua.network.internal.protocol.ClientConnectTCP;
import engine.lua.network.internal.protocol.ClientLoadMapTCP;
import engine.lua.type.object.services.Connections;
import ide.layout.windows.ErrorWindow;

public class InternalClient extends Client {
	private String worldJSON;
	private boolean blockUpdates;
	private ArrayList<ClientProcessable> packetBackQueue = new ArrayList<ClientProcessable>();
	
	public boolean connected;
	
	private static engine.lua.type.object.insts.Connection connectionInstance;
	
	public InternalClient( String ip, int port, String username, LuaTable data) {
		this.start();
		try {
			InternalRegister.register(this.getKryo());
			this.connect(5000, ip, port, port);
			connected = true;
			
			this.sendTCP(new ClientConnectTCP(username, Game.version(), LuaUtil.tableToJson(data).toString()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		this.addListener(new Listener() {
			public void received (Connection connection, Object object) {
				if ( object instanceof ClientLoadMapTCP ) {
					ClientLoadMapTCP loadPacket = (ClientLoadMapTCP) object;
					if ( loadPacket.data != null ) {
						if ( worldJSON != null ) {
							worldJSON = worldJSON + loadPacket.data;
						} else {
							worldJSON = loadPacket.data;
						}
					} else {
						if ( !loadPacket.finished ) {
							blockUpdates = true;
						} else if ( loadPacket.finished && worldJSON != null ) {
							
							// Decompress...
							int len = worldJSON.length();
							worldJSON = GZIPUtil.decompress(worldJSON);
							System.out.println("Loaded world...");
							System.out.println(worldJSON);
							System.out.println("Compressed json: " + len + " Decompressed json: " + worldJSON.length());
							
							try {
								JSONParser parser = new JSONParser();
								final JSONObject obj = (JSONObject) parser.parse(worldJSON);
								
								InternalGameThread.runLater(()->{

									InternalRenderThread.runLater(()->{
										try {
											Game.unload();
											
											// Load new game data
											if ( Load.parseJSONInto(obj, Game.game(), true) == null )
												return;
											
											Game.load();
											Game.setLoaded(true);
											
											// Create connection object
											connectionInstance = new engine.lua.type.object.insts.ServerConnection(connection);
											connectionInstance.forceSetName("ConnectionServer");
											connectionInstance.forceSetParent(Game.getService("Connections"));
											Game.connections().rawset("LocalConnection", connectionInstance);
											
											// Tell server we're finished loading
											connection.sendTCP(new ClientConnectFinishTCP());
											
											// Start game
											Game.setRunning(true);
										} catch(Exception e) {
											e.printStackTrace();
										}
									});
								});
								
							} catch (Exception e) {
								e.printStackTrace();
								new ErrorWindow("There was a problem reading this file. 001b");
							}
							
							// Do all the queued updates.
							blockUpdates = false;
							synchronized(packetBackQueue) {
								while(packetBackQueue.size() > 0) {
									packetBackQueue.get(0).clientProcess(connection);
									packetBackQueue.remove(0);
								}
							}
							
							worldJSON = null;
						}
					}
				}
				
				if ( object instanceof ClientProcessable ) {
					if ( blockUpdates ) {
						packetBackQueue.add((ClientProcessable) object);
					} else {
						((ClientProcessable)object).clientProcess(connection);
					}
				}
				
				// Ping request
				if ( object instanceof PingRequest ) {
					((PingRequest)object).process(connection);
				}
			}
			
			@Override
			public void disconnected(Connection connection) {
				Connections connections = ((Connections)Game.getService("Connections"));
				if ( connections == null )
					return;
				
				engine.lua.type.object.insts.Connection conInst = connections.getConnectionFromKryo(connection);
				if ( conInst == null )
					return;
				
				conInst.disconnect();
			}
		});
	}

	public static void sendServerTCP(Object packet) {
		if ( connectionInstance == null )
			return;
		
		if ( connectionInstance.getKryo() == null )
			return;
		
		Connection con = connectionInstance.getKryo();
		con.sendTCP(packet);
	}
	
	public static void sendServerUDP(Object packet) {
		if ( connectionInstance == null )
			return;
		
		if ( connectionInstance.getKryo() == null )
			return;
		
		Connection con = connectionInstance.getKryo();
		con.sendUDP(packet);
	}
}
