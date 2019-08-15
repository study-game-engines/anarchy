package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;
import engine.lua.network.internal.NonReplicatable;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class ServerConnection extends Connection implements TreeViewable,NonReplicatable {

	public ServerConnection( com.esotericsoftware.kryonet.Connection kryoConnection) {
		this();
		this.kryoConnection = kryoConnection;
		this.rawset("Address", LuaValue.valueOf(kryoConnection.getRemoteAddressTCP().getAddress().getHostAddress()));
	}
	
	public ServerConnection() {
		super("ServerConnection");
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_network_server;
	}
}
