/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package ide.layout.windows;

import java.util.HashMap;
import java.util.List;

import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.InternalRenderThread;
import engine.lua.type.object.Instance;
import engine.lua.type.object.insts.Material;
import ide.layout.IdePane;
import lwjgui.geometry.Pos;
import lwjgui.paint.Color;
import lwjgui.scene.control.Label;
import lwjgui.scene.control.ScrollPane;
import lwjgui.scene.layout.FlowPane;
import lwjgui.scene.layout.StackPane;
import lwjgui.scene.layout.VBox;
import lwjgui.style.BackgroundSolid;

public class IdeMaterialViewer extends IdePane {
	
	private FlowPane materialBox;
	private ScrollPane scrollPane;
	private HashMap<Material, MaterialNode> materialToNodeMap;
	private boolean createdConnections;
	
	public static final int NODE_SIZE = 120;

	public IdeMaterialViewer() {
		super("Material Viewer", true);
		
		Game.loadEvent().connect((args)->{
			initialize();
		});
		
		// Add user controls
		StandardUserControls.bind(this);
		initialize();
	}
	
	@Override
	protected void resize() {
		super.resize();

		if ( materialBox == null )
			return;
		
		materialBox.setPrefWidth(scrollPane.getViewportWidth());
		materialBox.setPrefHeight(scrollPane.getViewportHeight());
	}
	
	public void initialize() {
		if ( Game.core() == null ) {
			InternalRenderThread.runLater(()->{
				initialize();
			});
			return;
		}
		
		if ( materialBox != null ) {
			materialBox.getItems().clear();
			materialToNodeMap.clear();
		} else {
			materialToNodeMap = new HashMap<>();
			materialBox = new FlowPane();
			materialBox.setHgap(4);
			materialBox.setVgap(4);
			materialBox.setAlignment(Pos.TOP_LEFT);
			
			scrollPane = new ScrollPane();
			scrollPane.setFillToParentHeight(true);
			scrollPane.setFillToParentWidth(true);
			this.getChildren().add(scrollPane);
			scrollPane.setContent(materialBox);
		}
		
		System.out.println("INITIALIZING MATERIAL VIEWER");
		
		// Attach materials already in game
		List<Material> materials = Game.assets().getMaterials();
		for (int i = 0; i < materials.size(); i++) {
			attachMaterial((Material) materials.get(i));
		}
		
		// Handle material creation/deletion
		if (!createdConnections) {
			
			// Material added
			createdConnections = true;
			Game.assets().descendantAddedEvent().connect((materialArgs)->{
				Instance child = (Instance) materialArgs[0];
				if ( child instanceof Material ) {
					attachMaterial((Material) child);
				}
			});
			
			// Material removed
			Game.assets().descendantRemovedEvent().connect((materialArgs)->{
				Instance child = (Instance) materialArgs[0];
				if ( child instanceof Material ) {
					dettachMaterial((Material) child);
				}
			});
			
			// Material Added (project)
			Game.project().assets().descendantAddedEvent().connect((materialArgs)->{
				Instance child = (Instance) materialArgs[0];
				if ( child instanceof Material ) {
					attachMaterial((Material) child);
				}
			});
			
			// Material removed (project)
			Game.project().assets().descendantRemovedEvent().connect((materialArgs)->{
				Instance child = (Instance) materialArgs[0];
				if ( child instanceof Material ) {
					dettachMaterial((Material) child);
				}
			});
		}
	}
	
	private void dettachMaterial(Material material) {
		MaterialNode node = materialToNodeMap.get(material);
		if ( node == null ) 
			return;
		
		materialBox.getItems().remove(node);
		materialToNodeMap.remove(material);
	}

	private void attachMaterial(Material material) {
		MaterialNode p = new MaterialNode(material);
		
		materialBox.getItems().add(p);
		materialToNodeMap.put(material, p);
	}
	
	static class MaterialNode extends VBox {
		//private LegacyPipeline materialPipeline;
		
		public MaterialNode(Material material) {
			this.setAlignment(Pos.CENTER);
			this.setMaxWidth(NODE_SIZE);
			this.setBackground(null);
			
			// Create secondary world
			/*RenderableWorld renderableWorld = new Workspace();
			((Workspace)renderableWorld).forceSetParent(LuaValue.NIL);
			
			// Setup pipeline world
			{
				// Camera
				float d = 1.33f;
				Camera camera = new Camera();
				camera.setParent((LuaValue) renderableWorld);
				camera.setPosition(new Vector3(-d,-d,d/2));
				((Workspace)renderableWorld).setCurrentCamera(camera);

				// Material object
				{
					Prefab prefab = new Prefab();
					prefab.addModel(null, material);
					prefab.forceSetParent((LuaValue) renderableWorld);
	
					GameObject obj = new GameObject();
					obj.setPrefab(prefab);
					obj.setParent((LuaValue) renderableWorld);
				}
				
				// Background object
				{
					float BG_D = 20;
					Mesh BGMESH = new Mesh();
					BGMESH.block(-BG_D, -BG_D, -BG_D);
					BGMESH.forceSetParent((LuaValue) renderableWorld);
					
					Texture BGTEXTURE = new Texture();
					//BGTEXTURE.setTexture(Resources.TEXTURE_DEBUG);
					BGTEXTURE.forceSetParent((LuaValue) renderableWorld);
					
					Material BGMATERIAL = new Material();
					BGMATERIAL.setMetalness(0.3f);
					BGMATERIAL.setRoughness(1.0f);
					BGMATERIAL.setDiffuseMap(BGTEXTURE);
					BGMATERIAL.forceSetParent((LuaValue) renderableWorld);
					
					Prefab BGPREFAB = new Prefab();
					BGPREFAB.addModel(BGMESH, BGMATERIAL);
					BGPREFAB.forceSetParent((LuaValue) renderableWorld);
					
					GameObject BG = new GameObject();
					BG.setPrefab(BGPREFAB);
					BG.forceSetParent((LuaValue) renderableWorld);
				}
			}*/
			
			StackPane backPane = new StackPane();
			backPane.setPrefSize(NODE_SIZE, NODE_SIZE);
			backPane.setBackground(new BackgroundSolid(Color.LIGHT_GRAY));
			this.getChildren().add(backPane);
			
			// OpenGL rendering pane
			/*this.oglPane = new OpenGLPane();
			this.oglPane.setPrefSize(NODE_SIZE, NODE_SIZE);
			this.oglPane.setFlipY(true);
			this.oglPane.setRendererCallback(new Renderer() {
				GenericShader shader;

				@Override
				public void render(Context context, int width, int height) {
					
					//if ( shader == null ) {
						//shader = new GenericShader();
					//}
					
					if ( materialPipeline == null && renderAllowence > 0 && Game.isLoaded() ) {
						renderAllowence -= 1;
						
						materialPipeline = new LegacyPipeline(NODE_SIZE, NODE_SIZE);
						materialPipeline.setRenderableWorld(renderableWorld);
						materialPipeline.setSize(NODE_SIZE, NODE_SIZE);
						
						// Lights
						{
							int close = 4;
							int r = 18;
							int b = 6;
							int xx = 4;
							PointLight l1 = new PointLight();
							l1.setPosition(-xx, close, xx);
							l1.setRadius(r);
							l1.setIntensity(b);
							l1.setParent( renderableWorld.getInstance() );

							PointLight l2 = new PointLight();
							l2.setPosition(xx, close, xx);
							l2.setRadius(r);
							l2.setIntensity(b);
							l2.setParent(renderableWorld.getInstance() );

							PointLight l3 = new PointLight();
							l3.setPosition(-xx, close, -xx);
							l3.setRadius(r);
							l3.setIntensity(b);
							l3.setParent(renderableWorld.getInstance() );

							PointLight l4 = new PointLight();
							l4.setPosition(xx, close, -xx);
							l4.setRadius(r);
							l4.setIntensity(b);
							l4.setParent(renderableWorld.getInstance() );

							PointLight l5 = new PointLight();
							l5.setPosition(xx, -close*2, -xx);
							l5.setRadius(r);
							l5.setIntensity(b/2);
							l5.setParent(renderableWorld.getInstance() );

							PointLight l6 = new PointLight();
							l6.setPosition(-xx, -xx, xx);
							l6.setRadius(r);
							l6.setIntensity(b*0.77f);
							l6.setParent(renderableWorld.getInstance() );
						}
						
						renderMaterial();
						return;
					}
					
					if ( materialPipeline != null ) {
						Surface surface = materialPipeline.getPipelineBuffer();
						surface.render(shader);
					}
				}
			});
			this.getChildren().add(oglPane);
			*/
			
			// Label
			Label label = new Label(material.getName());
			this.getChildren().add(label);
			
			// Update on material change
			material.changedEvent().connect((args)-> {
				if ( args[0].eq_b(LuaValue.valueOf("Name")) )
					label.setText(args[1].toString());

				renderMaterial();
			});
			
			// Update when textures change inside material
			material.materialUpdateEvent().connect((args)->{
				renderMaterial();
			});
		}

		private void renderMaterial() {
			/*InternalRenderThread.runLater(()->{
				//if ( materialPipeline == null )
				//	return;
				//materialPipeline.render();
			});*/
		}
	}

	@Override
	public void onOpen() {
		//
	}

	@Override
	public void onClose() {
		//
	}
}