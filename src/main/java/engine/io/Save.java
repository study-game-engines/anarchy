/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.io;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.nfd.NativeFileDialog;

import engine.FilePath;
import engine.Game;
import engine.InternalGameThread;
import engine.InternalRenderThread;
import engine.gl.mesh.BufferedMesh;
import engine.lua.network.internal.JSONUtil;
import engine.lua.network.internal.NonReplicatable;
import engine.lua.type.LuaValuetype;
import engine.lua.type.object.AssetLoadable;
import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeInvisible;
import engine.lua.type.object.insts.Mesh;
import engine.lua.type.object.insts.Scene;
import engine.lua.type.object.insts.SceneInternal;
import engine.util.FileIO;
import engine.util.FileUtils;
import lwjgui.geometry.Pos;
import lwjgui.scene.Window;
import lwjgui.scene.WindowHandle;
import lwjgui.scene.WindowManager;
import lwjgui.scene.WindowThread;
import lwjgui.scene.control.Button;
import lwjgui.scene.control.Label;
import lwjgui.scene.layout.BorderPane;
import lwjgui.scene.layout.HBox;
import lwjgui.scene.layout.StackPane;
import lwjgui.theme.Theme;

public class Save {
	private static LinkedList<SavedInstance> inst;
	private static HashMap<Instance, SavedInstance> instanceMap;

	public static void requestSave(Runnable after) {
		//long win = LWJGUIUtil.createOpenGLCoreWindow("Save Dialog", 300, 100, false, true);
		WindowManager.runLater(() -> {
			WindowThread thread = new WindowThread(300, 100, "Save") {
				@Override
				protected void setupHandle(WindowHandle handle) {
					super.setupHandle(handle);
					handle.canResize(false);
				}
				@Override
				protected void init(Window window) {
					super.init(window);
					// Create root pane
					BorderPane root = new BorderPane();
	
					// Create a label
					Label l = new Label("Unsaved changes. Would you like to save?");
					root.setCenter(l);
	
					StackPane bottom = new StackPane();
					bottom.setPrefHeight(32);
					bottom.setAlignment(Pos.CENTER);
					bottom.setFillToParentWidth(true);
					bottom.setBackgroundLegacy(Theme.current().getControlAlt());
					root.setBottom(bottom);
					
					HBox t = new HBox();
					t.setAlignment(Pos.CENTER);
					t.setBackground(null);
					t.setSpacing(16);
					bottom.getChildren().add(t);
	
					// Create a button
					Button b = new Button("Yes");
					b.setMinWidth(64);
					b.setOnAction(event -> {
						window.close();
						if ( save() ) {
							InternalRenderThread.runLater(()->{
								after.run();
							});
						}
					});
					t.getChildren().add(b);
	
					// Create a button
					Button b2 = new Button("No");
					b2.setMinWidth(64);
					b2.setOnAction(event -> {
						window.close();
						InternalRenderThread.runLater(()->{
							after.run();
						});
					});
					t.getChildren().add(b2);
	
					// Create a button
					Button b3 = new Button("Cancel");
					b3.setMinWidth(64);
					b3.setOnAction(event -> {
						window.close();
					});
					t.getChildren().add(b3);
					
					// Show window
					window.setScene(new lwjgui.scene.Scene(root, 300, 100));
					window.show();
				}
			};
			thread.start();
		});
	}
	
	public static void saveAsSafe() {
		WindowManager.runLater(()->{
			save(true);
		});
	}
	
	public static void saveSafe() {
		WindowManager.runLater(()->{
			save();
		});
	}
	
	public static boolean save() {
		return save(false);
	}

	@SuppressWarnings("unchecked")
	public static boolean save(boolean saveAs) {
		if ( !Game.isLoaded() )
			return false;
		
		if ( Game.isRunning() )
			return false;
		
		String path = Game.saveFile;

		// Make projects folder
		File projects = new File("Projects");
		if ( !projects.exists() ) {
			projects.mkdirs();
		}

		// Get save path if it's not already set.
		if ( path == null || path.length() == 0 || saveAs ) {
			String newPath;
			
			// Get path
			MemoryStack stack = MemoryStack.stackPush();
			PointerBuffer outPath = stack.mallocPointer(1);
			int result = NativeFileDialog.NFD_SaveDialog("json", projects.getAbsolutePath(), outPath);
			if ( result == NativeFileDialog.NFD_OKAY ) {
				newPath = outPath.getStringUTF8(0);
			} else {
				return false;
			}
			stack.pop();
			
			// Make sure path filetype is .json
			if ( !newPath.endsWith(".json") ) {
				newPath = newPath+".json";
			}
			
			path = newPath;
		}

		// Setup directory folder
		String jsonFile = FileUtils.getFileNameFromPath(path);
		String gameDirectoryPath = FileUtils.getFileDirectoryFromPath(path) + File.separator + FileUtils.getFileNameWithoutExtension(jsonFile);
		File f = new File(path);
		if ( f.exists() ) {
			gameDirectoryPath = FileUtils.getFileDirectoryFromPath(path);
		}
		File gameDirectory = new File(gameDirectoryPath);
		if ( !gameDirectory.exists() ) {
			gameDirectory.mkdirs();
		}

		// Update project path
		path = gameDirectory.getAbsolutePath() + File.separator + jsonFile;
		System.out.println(gameDirectory);
		System.out.println(path);
		
		// Resources folder
		File resourcesFolder = new File(gameDirectory + File.separator + "Resources");
		if ( !resourcesFolder.exists() ) {
			resourcesFolder.mkdir();
		}
		
		// Check if path was updated...
		if ( (Game.saveDirectory != null && Game.saveDirectory.length() > 0) && !gameDirectory.getAbsolutePath().equals(Game.saveDirectory) ) {
			File oldResources = new File(Game.saveDirectory + File.separator + "Resources");
			System.out.println("RESAVING! " + oldResources + " / " + resourcesFolder);
			
			// Copy old resources into new resources
			copyFolder(oldResources, resourcesFolder);
		}
		
		// Update filepaths
		Game.saveDirectory = gameDirectory.getAbsolutePath();
		Game.saveFile = path;
		//GLFW.glfwSetWindowTitle(IDE.window, IDE.TITLE + " [" + FileUtils.getFileDirectoryFromPath(path) + "]");
		
		// Write new resources
		writeResources(resourcesFolder);
		
		// Write scripts
		writeScripts(resourcesFolder);

		// Start saving process	
		JSONObject projectJSONInternal = getProjectJSON(true);
		JSONObject gameJSON = getInstanceJSONRecursive( true, true, Game.game());
		JSONObject saveJSON = new JSONObject();
		saveJSON.put("Version", 2.0f);
		saveJSON.put("ProjectData", projectJSONInternal);
		saveJSON.put("GameData", gameJSON);
		Game.changes = false;
		
		// Save scenes!
		writeScenes();

		return writeJSONToFile(path, saveJSON);
	}

	private static boolean writeJSONToFile(String path, JSONObject jsonData) {
		try {
			// Get scene as JSON string
			String text = jsonData.toJSONString();

			// Write to file
			File file=new File(path);
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
			bos.write(text.getBytes());
			bos.flush();
			bos.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	/**
	 * Returns the PROJECT represented as a JSON Object. The project is a little different than the game JSON.
	 * The project is global, whereas the game is temporary.
	 * @return
	 */
	public static JSONObject getProjectJSON( boolean savingToFile ) {
		return getInstanceJSONRecursive( savingToFile, true, Game.project());
	}

	/**
	 * Returns the game represented as a JSON Object.
	 * @return
	 */
	public static JSONObject getGameJSON() {
		return getInstanceJSONRecursive( false, true, Game.game());
	}
	
	/**
	 * Returns the instance and all of its children represented as JSON Objects.
	 * @param instance
	 * @return
	 */
	public static JSONObject getInstanceJSONRecursive(boolean savingLocally, boolean saveUUID, Instance instance) {
		JSONObject ret = null;
		try {
			instanceMap = new HashMap<>();
			inst = getSavedInstances( savingLocally, instance);
			ret = inst.getFirst().toJSON(savingLocally, saveUUID);
		} catch(Exception e ) {
			e.printStackTrace();
		}
		
		return ret;
	}
	
	/**
	 * Returns the instance represented as a JSON Object.
	 * @param instance
	 * @return
	 */
	public static JSONObject getInstanceJSON(boolean savingLocally, boolean saveUUID, Instance instance) {
		JSONObject ret = null;
		try {
			
			if ( !instance.isArchivable() && savingLocally )
				return null;

			ret = new SavedInstance(instance).toJSON(savingLocally, saveUUID);
		} catch(Exception e ) {
			e.printStackTrace();
		}
		
		return ret;
	}

	private static LinkedList<SavedInstance> getSavedInstances(boolean savingLocally, Instance root) {
		LinkedList<SavedInstance> ret = new LinkedList<SavedInstance>();
		
		if ( !root.isArchivable() && savingLocally )
			return ret;

		SavedInstance savedRoot = new SavedInstance(root);
		ret.add(savedRoot);
		instanceMap.put(root, savedRoot);

		List<Instance> children = root.getChildren();
		for (int i = 0; i < children.size(); i++) {
			Instance child = children.get(i);
			List<SavedInstance> t = getSavedInstances(savingLocally, child);
			
			ListIterator<SavedInstance> listIterator = t.listIterator();
			while(listIterator.hasNext()) {
				SavedInstance si = listIterator.next();
				ret.add(si);
			}
		}

		return ret;
	}

	static class SavedInstance {
		final Instance instance;
		final String uuid;

		public SavedInstance(Instance child) {
			this.instance = child;
			this.uuid = (child.getUUID()!=null?child.getUUID():Game.generateUUID()).toString();
		}

		@SuppressWarnings("unchecked")
		public JSONObject toJSON(boolean savingLocally, boolean saveUUID) {
			if ( !instance.isArchivable() && savingLocally )
				return null;

			List<Instance> children = instance.getChildren();
			JSONArray childArray = new JSONArray();
			for (int i = 0; i < children.size(); i++) {
				Instance child = children.get(i);
				if ( child instanceof TreeInvisible )
					continue;

				SavedInstance sinst = instanceMap.get(child);
				if ( sinst != null ) {
					JSONObject json = sinst.toJSON(savingLocally, saveUUID);
					if ( json != null ) {
						childArray.add(json);
					}
				}
			}

			JSONObject p = new JSONObject();
			LuaValue[] fields = instance.getFieldNames();
			for (int i = 0; i < fields.length; i++) {
				String field = fields[i].toString();
				// These are default params
				if ( field.equals("Name") || field.equals("ClassName") || field.equals("Parent") )
					continue;

				// Protect the fields of non replicatable objects being sent when running.
				if ( Game.isRunning() && instance instanceof NonReplicatable ) {
					p.put(field, fieldToJSONBlank(instance.get(field)));
				} else {
					p.put(field, fieldToJSON(instance.get(field)));
				}
				
				// Special case to NOT write "Source" only when writing a project. Instead put write it externally...
				if ( field.equals("Source") && savingLocally && instance.getField(field).getType() == LuaString.class) {
					String source = instance.get(field).tojstring();
					p.put(field, fieldToJSONBlank(instance.get(field)));
				}
			}

			JSONObject j = new JSONObject();
			if ( saveUUID )
				j.put("UUID", uuid);
			j.put("ClassName", instance.get("ClassName").toString());
			j.put("Name", instance.getName());
			j.put("Children", childArray);
			j.put("Properties", p);

			return j;
		}
	}

	@SuppressWarnings("unchecked")
	protected static Object fieldToJSONBlank(LuaValue luaValue) {
		if ( luaValue.isstring() )
			return "";
		if ( luaValue.isboolean() )
			return false;
		if ( luaValue.isnumber() )
			return 0.0;
		
		if ( luaValue instanceof Instance )
			return null;
		
		// Vectorx/Colors/etc
		if ( luaValue instanceof LuaValuetype ) {
			try {
				JSONObject t = new JSONObject();
				t.put("ClassName", ((LuaValuetype)luaValue).typename());
				t.put("Data", ((LuaValuetype)luaValue).getClass().newInstance().toJSON());

				JSONObject j = new JSONObject();
				j.put("Type", "Datatype");
				j.put("Value", t);
				return j;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return null;
	}

	protected static Object fieldToJSON(LuaValue luaValue) {
		return JSONUtil.serializeObject(luaValue);
		/*if ( luaValue.isstring() )
			return luaValue.toString();
		if ( luaValue.isboolean() )
			return luaValue.checkboolean();
		if ( luaValue.isnumber() )
			return luaValue.todouble();

		// Instances in the game
		if ( luaValue instanceof Instance ) {
			SavedInstance svd = instanceMap.get((Instance) luaValue);
			SavedReference ref = null;
			if ( svd != null ) {
				ref = svd.reference;
			} else {
				ref = new HashReference((Instance) luaValue);
			}
			
			if ( ((Instance) luaValue).getUUID() != null )
				ref.put("UUID", ((Instance) luaValue).getUUID().toString());
			
			return ref;
		}

		// Vectorx/Colors/etc
		if ( luaValue instanceof LuaValuetype ) {
			JSONObject t = new JSONObject();
			t.put("ClassName", ((LuaValuetype)luaValue).typename());
			t.put("Data", ((LuaValuetype)luaValue).toJSON());

			JSONObject j = new JSONObject();
			j.put("Type", "Datatype");
			j.put("Value", t);
			return j;
		}

		return null;*/
	}

	/*protected static SavedInstance getSavedInstance(Instance instance) {
		for (int i = 0; i < inst.size(); i++) {
			SavedInstance s = inst.get(i);
			if ( s.instance.equals(instance) ) {
				return s;
			}
		}
		return null;
	}*/
	
	private static void copyFolder(File sourceFolder, File destinationFolder) {
        //Check if sourceFolder is a directory or file
        //If sourceFolder is file; then copy the file directly to new location
        if (sourceFolder.isDirectory()) 
        {
            //Verify if destinationFolder is already present; If not then create it
            if (!destinationFolder.exists()) 
            {
                destinationFolder.mkdir();
                System.out.println("Directory created :: " + destinationFolder);
            }
             
            //Get all files from source directory
            String files[] = sourceFolder.list();
             
            //Iterate over all files and copy them to destinationFolder one by one
            for (String file : files) 
            {
                File srcFile = new File(sourceFolder, file);
                File destFile = new File(destinationFolder, file);
                 
                //Recursive function call
                copyFolder(srcFile, destFile);
            }
        }
        else
        {
            //Copy the file content from one place to another 
            try {
				Files.copy(sourceFolder.toPath(), destinationFolder.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				e.printStackTrace();
			}
            System.out.println("File copied :: " + destinationFolder);
        }
	}
	
	private static void writeScenes() {
		String scenePath = Game.saveDirectory + File.separator + "Scenes" + File.separator;
		File t = new File(scenePath);
		if ( !t.exists() )
			t.mkdirs();
		
		List<SceneInternal> unsavedScenes = Game.getUnsavedScenes();
		SceneInternal currentScene = null;
		for (int i = 0; i < unsavedScenes.size(); i++) {
			SceneInternal internalScene = unsavedScenes.get(i);
			Scene linkedScene = unsavedScenes.get(i).getScene();
			if ( linkedScene == null )
				continue;
			
			// Write what's currently in game to the scene
			if ( linkedScene == Game.project().scenes().getCurrentScene() ) {
				currentScene = internalScene;
				internalScene.storeGame();
			}
			
			// Write this scene to file
			String sPath = scenePath + linkedScene.getUUID().toString();
			JSONObject sceneJSON = getInstanceJSONRecursive(true, true, internalScene);
			System.out.println("WRITING SCENE TO FILE! " + linkedScene.getFullName());
			writeJSONToFile(sPath, sceneJSON);
			
			// Remove this unsaved scene if it's no longer loaded
			if ( linkedScene.equals(Game.project().scenes().getCurrentScene()) )
				unsavedScenes.remove(i--);
		}
		unsavedScenes.clear();
		
		if ( currentScene != null ) {
			final SceneInternal temp1 = currentScene;
			InternalGameThread.runLater(()->{
				//Game.project().scenes().rawset("CurrentScene", linkedScene);
				//Game.game().extractScene(internalScene);
				Game.game().loadScene(temp1.getScene());
			});
		}
	}

	@SuppressWarnings("unchecked")
	private static void writeScripts(File resourcesFolder) {
		// Find all scripts
		List<Instance> descendents = Game.game().getDescendants();
		List<Instance> scripts = new ArrayList<>();
		for (int i = 0; i < descendents.size(); i++) {
			Instance t = descendents.get(i);
			if ( !t.isArchivable() )
				continue;
			
			if ( t.getField(LuaValue.valueOf("Source")) != null )
				scripts.add(t);
		}
		
		// Get absolte path
		String resourcesPath = resourcesFolder.getAbsolutePath();
		if ( !Game.isLoaded() )
			return;
		
		// Get scripts folder
		File f = new File(resourcesPath + File.separator + "Scripts");
		if ( !f.exists() )
			f.mkdirs();
		
		// Write to file
		for (int i = 0; i < scripts.size(); i++) {
			Instance inst = scripts.get(i);
			if ( inst.containsField(LuaValue.valueOf("Source")) ) {
				String source = inst.get(LuaValue.valueOf("Source")).toString();
				if ( source.length() <= 0 )
					continue;
				
				BufferedWriter writer = FileIO.file_text_open_write(f.getAbsolutePath() + File.separator + inst.getUUID().toString());
				JSONObject obj = new JSONObject();
				obj.put("Source", source);
				FileIO.file_text_write_line(writer, obj.toJSONString());
				FileIO.file_text_close(writer);
			}
		}
	}

	private static void writeResources(File resourcesFolder) {
		String resourcesPath = resourcesFolder.getAbsolutePath();
		if ( !Game.isLoaded() )
			return;
		copyAssets( resourcesPath + File.separator + "Textures", Game.assets().getTextures());
		copyAssets( resourcesPath + File.separator + "Meshes", Game.assets().getMeshes());
		copyAssets( resourcesPath + File.separator + "Audio", Game.assets().getAudio());
	}
	
	@SuppressWarnings("unused")
	private static boolean deleteDirectory(File directoryToBeDeleted) {
	    File[] allContents = directoryToBeDeleted.listFiles();
	    if (allContents != null) {
	        for (File file : allContents) {
	            deleteDirectory(file);
	        }
	    }
	    return directoryToBeDeleted.delete();
	}

	private static void copyAssets(String path, List<AssetLoadable> assets) {
		File p = new File(path);
		if ( !p.exists() )
			p.mkdirs();
		
		ArrayList<String> writtenTo = new ArrayList<String>();
		
		for (int i = 0; i < assets.size(); i++) {
			AssetLoadable a = assets.get(i);
			
			String filePath = a.getFilePath();
			boolean localFile = filePath.contains(FilePath.PROJECT_IDENTIFIER);
			filePath = filePath.replace(FilePath.PROJECT_IDENTIFIER, new File(Game.saveDirectory).getAbsolutePath());
			
			// Check if the filepath is filled out
			if ( filePath != null && filePath.length() > 3 ) {
				String fileName = FileUtils.getFileNameFromPath(filePath);
				String fileNameEx = FileUtils.getFileNameWithoutExtension(fileName);
				String fileExtension = FileUtils.getFileExtension(filePath);
				String dest = path + File.separator + fileName;
	
				// Get final destination
				File d = new File(dest);
				int aa = 0;
				while ( writtenTo.contains(dest) ) {
					dest = path + File.separator + fileNameEx + aa + fileExtension;
					d = new File(dest);
					aa++;
				}
				writtenTo.add(dest);
	
				if ( filePath != null && filePath.length() > 0 ) {
					try {
						
						// Get the source file
						File s = new File(filePath);
						if ( s.exists() ) {
							Files.copy(s.toPath(), d.toPath(), StandardCopyOption.REPLACE_EXISTING);
							
							a.rawset("FilePath", dest.replace(p.getParentFile().getParentFile().getAbsolutePath(), FilePath.PROJECT_IDENTIFIER));
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} else {
				
				// Mesh without being backed by a file. Save its data to a file!
				if ( a instanceof Mesh ) {
					Mesh mesh = (Mesh) a;
					BufferedMesh bufferedMesh = mesh.getMesh();
					if ( bufferedMesh != null && bufferedMesh.getSize() > 0 ) {
						File d = getFreeFile( path, "Mesh_", ".mesh");
						if ( d != null ) {
							String dest = d.getAbsolutePath();
							BufferedMesh.Export(bufferedMesh, dest);
							a.rawset("FilePath", dest.replace(p.getParentFile().getParentFile().getAbsolutePath(), FilePath.PROJECT_IDENTIFIER));
						}
					}
				}
			}
		}
	}

	private static File getFreeFile(String path, String name, String extension) {
		File t = null;
		int a = 0;
		while ( t == null || t.exists() ) {
			t = new File(path + File.separator + name + a + extension);
			a++;
		}
		return t;
	}
}
