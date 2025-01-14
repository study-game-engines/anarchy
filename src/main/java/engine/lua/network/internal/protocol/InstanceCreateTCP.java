/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.network.internal.protocol;

import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.luaj.vm2.LuaValue;

import com.esotericsoftware.kryonet.Connection;

import engine.lua.network.UUIDSerializable;
import engine.lua.network.internal.ClientProcessable;
import engine.lua.network.internal.JSONUtil;
import engine.lua.network.internal.NonReplicatable;
import engine.lua.type.object.Instance;

public class InstanceCreateTCP implements ClientProcessable {
	public String instanceType;
	public String instanceData;
	public UUIDSerializable instanceUUID;

	private static final String C_PARENTs = "Parent";
	private static final LuaValue C_CLASSNAME = LuaValue.valueOf("ClassName");
	private static final LuaValue C_NAME = LuaValue.valueOf("Name");
	private static final LuaValue C_PARENT = LuaValue.valueOf(C_PARENTs);
	
	public InstanceCreateTCP() {
		this.instanceType = "";
		this.instanceData = "";
	}
	
	@SuppressWarnings("unchecked")
	public InstanceCreateTCP(Instance instance) {
		this.instanceType = instance.get(C_CLASSNAME).toString();
		this.instanceUUID = new UUIDSerializable(instance.getUUID());
		
		LuaValue[] fields = instance.getFieldNames();
		
		JSONObject j = new JSONObject();
		for (int i = 0; i < fields.length; i++) {
			LuaValue field = fields[i];
			
			if ( field.eq_b(C_CLASSNAME) )
				continue;
			
			// If instance is Non-Replicatable, DO NOT REPLICATE NAME OR PARENT CHANGES!
			if ( instance instanceof NonReplicatable ) {
				if ( !field.eq_b(C_NAME) && !field.eq_b(C_PARENT) ) {
					continue;
				}
			}
			
			j.put(field, JSONUtil.serializeObject(instance.get(field)));
		}
		
		this.instanceData = j.toJSONString();
	}

	@Override
	public void clientProcess(Connection Connection) {
		LuaValue toParent = LuaValue.NIL;
		//System.out.println("Attempting to create: " + instanceType);
		Instance internalInstance = (Instance) Instance.instance(instanceType);
		if ( internalInstance == null )
			return;
		
		JSONParser parser = new JSONParser();
		try {
			JSONObject obj = (JSONObject) parser.parse(instanceData);
			
			Set<?> keys = obj.keySet();
			Object[] keyArray = keys.toArray();
			for (int i = 0; i < keyArray.length; i++) {
				String field = (String) keyArray[i];
				Object jsonValue = obj.get(field);
				LuaValue value = JSONUtil.deserializeObject(jsonValue);
				//System.out.println(internalInstance + " :: " + field + " / " + value + " / " + jsonValue);
				if ( value != null ) {
					if ( field.equals(C_PARENTs) ) {
						toParent = value;
					} else {
						internalInstance.rawset(field, value);
						try{internalInstance.set(field, value);}catch(Exception e) {}
					}
				}
			}
		} catch (ParseException e) {
			System.err.println(instanceData);
			e.printStackTrace();
		}
		
		//System.out.println("PARENT: " + toParent);
		internalInstance.setUUID(this.instanceUUID.getUUID());
		internalInstance.forceSetParent(toParent);
	}
}
