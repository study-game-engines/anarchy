/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.object.insts.light;

import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;

import engine.gl.IPipeline;
import engine.gl.lights.PointLightInternal;
import engine.lua.lib.EnumType;
import engine.lua.type.NumberClampPreferred;
import engine.lua.type.data.Color3;
import engine.lua.type.data.Vector3;
import engine.lua.type.object.LightBase;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;
import lwjgui.paint.Color;

public class PointLight extends LightBase<PointLightInternal> implements TreeViewable {

	private static final LuaValue C_RADIUS = LuaValue.valueOf("Radius");
	private static final LuaValue C_SHADOWMAPSIZE = LuaValue.valueOf("ShadowMapSize");

	public PointLight() {
		super("PointLight");
		
		this.defineField(C_RADIUS, LuaValue.valueOf(8), false);
		this.getField(C_RADIUS).setClamp(new NumberClampPreferred(0, 1024, 0, 64));

		this.defineField(C_SHADOWMAPSIZE, LuaValue.valueOf(512), false);
		this.getField(C_SHADOWMAPSIZE).setEnum(new EnumType("TextureSize"));
		
		this.changedEvent().connect((args)->{
			LuaValue key = args[0];
			LuaValue value = args[1];
			
			if ( light != null ) {
				if ( key.eq_b(C_POSITION) ) {
					light.setPosition(((Vector3)value).toJoml());
				} else if ( key.eq_b(C_RADIUS) ) {
					light.radius = value.tofloat();
				} else if ( key.eq_b(C_INTENSITY) ) {
					light.intensity = value.tofloat();
				} else if ( key.eq_b(C_COLOR) ) {
					Color color = ((Color3)value).toColor();
					light.color = new Vector3f( Math.max( color.getRed(),1 )/255f, Math.max( color.getGreen(),1 )/255f, Math.max( color.getBlue(),1 )/255f );
				} else if (key.eq_b(C_SHADOWS)) {
					light.shadows = value.toboolean();
				} else if (key.eq_b(C_SHADOWMAPSIZE)) {
					light.setSize(value.toint());
				}
			}
		});
	}

	public void setRadius(float radius) {
		this.set(C_RADIUS, LuaValue.valueOf(radius));
	}

	@Override
	protected void destroyLight(IPipeline pipeline) {
		if ( light == null || pipeline == null )
			return;
		
		pipeline.getPointLightHandler().removeLight(light);
		this.light = null;
		this.pipeline = null;

		System.out.println("Destroyed light");
	}

	@Override
	protected void makeLight(IPipeline pipeline) {		
		// Add it to pipeline
			
		System.out.println("Creating pointlight! " + pipeline + " / " + light);
		if ( pipeline == null )
			return;
		
		if ( light != null )
			return;
		
		this.pipeline = pipeline;
		
		// Create light
		Vector3f pos = ((Vector3)this.get("Position")).toJoml();
		float radius = this.get(C_RADIUS).tofloat();
		float intensity = this.get("Intensity").tofloat();
		light = new PointLightInternal(pos, radius, intensity);
		
		// Color it
		Color color = ((Color3)this.get("Color")).toColor();
		light.color = new Vector3f( Math.max( color.getRed(),1 )/255f, Math.max( color.getGreen(),1 )/255f, Math.max( color.getBlue(),1 )/255f );
		
		light.visible = this.get(C_VISIBLE).toboolean();
		
		light.shadowResolution = this.get(C_SHADOWMAPSIZE).toint();

		light.shadows = this.get(C_SHADOWS).toboolean();

		pipeline.getPointLightHandler().addLight(light);
	}
	
	@Override
	public Icons getIcon() {
		return Icons.icon_light;
	}
/*
	@Override
	public void gameUpdateEvent(boolean important) {
		if ( !important )
			return;
		
		onParentChange();
	}*/
}
