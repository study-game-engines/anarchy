/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.network.internal;

import java.lang.reflect.Method;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.lua.lib.LuaUtil;
import engine.lua.type.LuaValuetype;
import engine.lua.type.object.Instance;

public class JSONUtil {
	private static final String C_TYPE = "Type";
	private static final String C_VALUE = "Value";
	private static final String C_CLASSNAME = "ClassName";
	private static final String C_DATA = "Data";
	private static final String C_TYPE_TABLE = "Table";
	private static final String C_TYPE_REFERENCE = "Reference";
	private static final String C_TYPE_DATATYPE = "Datatype";
	
	/**
	 * Serializes a lua based object value to JSON.
	 * @param luaValue
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Object serializeObject(LuaValue luaValue) {
		if ( luaValue.isstring() )
			return luaValue.toString();
		if ( luaValue.isboolean() )
			return luaValue.checkboolean();
		if ( luaValue.isnumber() )
			return luaValue.todouble();
		
		// Instances in the game
		if ( luaValue instanceof Instance || luaValue.isnil() ) {
			String refId = null;
			if ( !luaValue.isnil() ) {
				Instance temp = (Instance)luaValue;
				if ( temp.getUUID() != null )
					refId = temp.getUUID().toString();
			}			
			JSONObject j = new JSONObject();
			j.put(C_TYPE, C_TYPE_REFERENCE);
			j.put(C_VALUE, refId);
			return j;
		}

		// Vector3/Colors/etc
		if ( luaValue instanceof LuaValuetype ) {
			JSONObject t = new JSONObject();
			t.put(C_CLASSNAME, ((LuaValuetype)luaValue).typename());
			t.put(C_DATA, ((LuaValuetype)luaValue).toJSON());

			JSONObject j = new JSONObject();
			j.put(C_TYPE, C_TYPE_DATATYPE);
			j.put(C_VALUE, t);
			return j;
		}
		
		// Lastly check for table. Both Instance and valuetype extend table so check them first!
		if ( luaValue instanceof LuaTable ) {

			JSONObject j = new JSONObject();
			j.put(C_TYPE, C_TYPE_TABLE);
			j.put(C_VALUE, LuaUtil.tableToJson((LuaTable) luaValue));
			return j;
		}

		return null;
	}
	
	/**
	 * Deserializes JSON into a lua value.
	 * @param t
	 * @return
	 */
	public static LuaValue deserializeObject(Object t) {
		if ( t == null ) {
			return LuaValue.NIL;
		}
		if ( t instanceof Boolean ) {
			return LuaValue.valueOf((Boolean)t);
		}
		if ( t instanceof Double ) {
			return LuaValue.valueOf((Double)t);
		}
		if ( t instanceof Float ) {
			return LuaValue.valueOf((Float)t);
		}
		if ( t instanceof String ) {
			return LuaValue.valueOf((String)t);
		}
		if ( t instanceof JSONObject ) {
			JSONObject j = (JSONObject)t;
			
			if ( j.get(C_TYPE).equals(C_TYPE_TABLE) ) {
				return LuaUtil.jsonToTable((JSONObject) j.get(C_VALUE));
			}
			
			if ( j.get(C_TYPE).equals(C_TYPE_REFERENCE) ) {
				String uuidStr = (String) j.get(C_VALUE);
				if ( uuidStr == null || uuidStr.length() == 0 )
					return null;
				return Game.getInstanceFromUUID(UUID.fromString(uuidStr));
			}
			
			if ( j.get(C_TYPE).equals(C_TYPE_DATATYPE) ) {
				JSONObject data = (JSONObject) j.get(C_VALUE);
				String type = (String) data.get(C_CLASSNAME);
				JSONObject temp = (JSONObject) data.get(C_DATA);
				
				Class<? extends LuaValuetype> c = LuaValuetype.DATA_TYPES.get(type);
				if ( c != null ) {
					LuaValuetype o = null;
					try {
						Method method = c.getMethod("fromJSON", JSONObject.class);
						Object ot = method.invoke(null, temp);
						o = (LuaValuetype)ot;
					}catch( Exception e ) {
						e.printStackTrace();
					}
					
					return o;
				}
			}
		}
		
		return null;
	}
}
