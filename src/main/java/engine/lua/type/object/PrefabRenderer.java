/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.object;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.joml.Vector3f;

import engine.gl.mesh.BufferedMesh;
import engine.lua.type.LuaConnection;
import engine.lua.type.object.insts.Mesh;
import engine.lua.type.object.insts.Model;
import engine.lua.type.object.insts.Prefab;
import engine.util.AABBUtil;
import engine.util.Pair;

public class PrefabRenderer {
	private List<Model> models;
	private Pair<Vector3f,Vector3f> AABB;
	
	private BufferedMesh combined;
	private Prefab parent;
	
	private boolean updated;
	
	public PrefabRenderer(Prefab parent) {
		models = Collections.synchronizedList(new ArrayList<Model>());
		AABB = new Pair<Vector3f,Vector3f>(new Vector3f(), new Vector3f());
		this.parent = parent;
		
		this.parent.changedEvent().connect((args)-> {
			update();
		});
	}

	private void calculateAABB() {
		float scale = parent.getScale();
		Model[] temp = models.toArray(new Model[models.size()]);
		this.AABB = AABBUtil.prefabAABB(scale, temp);
	}
	
	public Pair<Vector3f,Vector3f> getAABB() {
		return this.AABB;
	}
	
	public Model getModel(int index) {
		return models.get(index);
	}
	
	public int size() {
		return this.models.size();
	}
	

	private HashMap<Model, LuaConnection> modelChangeMap = new HashMap<>();

	public void addModel(Model model) {
		synchronized(models) {
			models.add(model);
		}
		calculateAABB();
		updated = true;
		
		LuaConnection connection = modelChangeMap.get(model);
		if ( connection == null ) {
			// Track property change
			connection = model.changedEvent().connect((args)->{
				update();
				
				// Track mesh changed to new mesh
				Mesh mesh = model.getMesh();
				if ( mesh != null ) {
					model.getMesh().meshLoaded().connect((args2)->{
						update();
					});
				}
			});	
			
			// Track current mesh
			Mesh mesh = model.getMesh();
			if ( mesh != null ) {
				model.getMesh().meshLoaded().connect((args2)->{
					update();
				});
			}
			modelChangeMap.put(model, connection);
		}
	}
	
	public void update() {
		calculateAABB();
		updated = true;
	}

	public void removeModel(Model model) {
		synchronized(models) {
			models.remove(model);
		}
		calculateAABB();
		updated = true;
		
		LuaConnection con = modelChangeMap.get(model);
		if ( con != null ) {
			con.disconnect();
		}
		modelChangeMap.remove(model);
	}
	
	private void calculateCombined() {
		if ( combined != null ) {
			combined.cleanup();
			combined = null;
		}
		
		combined = BufferedMesh.combineMeshes(getMeshes());
		combined.scale(parent.getScale());
		updated = false;
	}

	private BufferedMesh[] getMeshes() {
		int m = 0;
		for (int i = 0; i < models.size(); i++) {
			if ( models.get(i).getMesh() != null )
				m++;
		}
		
		BufferedMesh[] meshes = new BufferedMesh[m];
		int a = 0;
		for (int i = 0; i < models.size(); i++) {
			BufferedMesh mesh = models.get(i).getMeshInternal();
			if ( mesh == null )
				continue;
			meshes[a++] = mesh;
		}
		
		return meshes;
	}

	public BufferedMesh getCombinedMesh() {
		if ( updated )
			calculateCombined();
		
		return this.combined;
	}

	public void cleanup() {
		if ( models != null )
			models.clear();
		if ( combined != null )
			combined.cleanup();
	}

	public boolean isEmpty() {
		return models.size() == 0;
	}
	
	public Prefab getParent() {
		return parent;
	}

	public Vector3f getAABBOffset() {
		return getAABB().value2().add(getAABB().value1(), new Vector3f()).mul(-0.5f);
	}
}
