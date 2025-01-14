/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package ide.layout.windows.icons;

import lwjgui.scene.Context;
import lwjgui.scene.image.Image;
import lwjgui.scene.image.ImageView;

public class Icons {
	public static final Icons icon_wat = new Icons("wat.gif");
	public static final Icons icon_cross = new Icons("Cross.png");
	public static final Icons icon_animation_data = new Icons("AnimationData.png");
	public static final Icons icon_world = new Icons("World.png");
	public static final Icons icon_folder = new Icons("Folder.png");
	public static final Icons icon_asset_folder = new Icons("AssetFolder.png");
	public static final Icons icon_asset_folder_material = new Icons("AssetFolderMaterial.png");
	public static final Icons icon_asset_folder_mesh = new Icons("AssetFolderMesh.png");
	public static final Icons icon_asset_folder_texture = new Icons("AssetFolderTexture.png");
	public static final Icons icon_asset_folder_prefab = new Icons("AssetFolderPrefab.png");
	public static final Icons icon_cut = new Icons("Cut.png");
	public static final Icons icon_copy = new Icons("Copy.png");
	public static final Icons icon_pane = new Icons("Pane.png");
	public static final Icons icon_gui = new Icons("Gui.png");
	public static final Icons icon_paste = new Icons("Paste.png");
	public static final Icons icon_new = new Icons("New.png");
	public static final Icons icon_save = new Icons("Save.png");
	public static final Icons icon_saveas = new Icons("SaveAs.png");
	public static final Icons icon_sound = new Icons("Sound.png");
	public static final Icons icon_script_service = new Icons("ScriptService.png");
	public static final Icons icon_script = new Icons("Script.png");
	public static final Icons icon_script_local = new Icons("ScriptLocal.png");
	public static final Icons icon_script_global = new Icons("ScriptGlobal.png");
	public static final Icons icon_script_css = new Icons("CSS.png");
	public static final Icons icon_scenes = new Icons("Scenes.png");
	public static final Icons icon_player_scripts = new Icons("PlayerScripts.png");
	public static final Icons icon_player_gui = new Icons("PlayerGui.png");
	public static final Icons icon_starter_player = new Icons("StarterPlayer.png");
	public static final Icons icon_workspace = new Icons("World.png");
	public static final Icons icon_weld = new Icons("Weld.png");
	public static final Icons icon_storage = new Icons("Storage.png");
	public static final Icons icon_players = new Icons("Players.png");
	public static final Icons icon_player = new Icons("Player.png");
	public static final Icons icon_play = new Icons("Play.png");
	public static final Icons icon_play_server = new Icons("PlayServer.png");
	public static final Icons icon_plus = new Icons("plus-small.png");
	public static final Icons icon_properties = new Icons("Properties.png");
	public static final Icons icon_camera = new Icons("Camera.png");
	public static final Icons icon_keyboard = new Icons("Keyboard.png");
	public static final Icons icon_network = new Icons("Network.png");
	public static final Icons icon_network_player = new Icons("NetworkPlayer.png");
	public static final Icons icon_network_server = new Icons("NetworkServer.png");
	public static final Icons icon_gameobject = new Icons("GameObject.png");
	public static final Icons icon_model = new Icons("Model.png");
	public static final Icons icon_box = new Icons("Box.png");
	public static final Icons icon_sky = new Icons("icon-sky.png");
	public static final Icons icon_mesh = new Icons("Mesh.png");
	public static final Icons icon_texture = new Icons("Texture.png");
	public static final Icons icon_material = new Icons("Material.png");
	public static final Icons icon_light = new Icons("Light.png");
	public static final Icons icon_light_spot = new Icons("LightSpot.png");
	public static final Icons icon_light_directional = new Icons("DirectionalLight.png");
	public static final Icons icon_film = new Icons("Film.png");
	public static final Icons icon_film_timeline = new Icons("FilmTimeline.png");
	public static final Icons icon_skybox = new Icons("Skybox.png");
	public static final Icons icon_undo = new Icons("Undo.png");
	public static final Icons icon_redo = new Icons("Redo.png");
	public static final Icons icon_value = new Icons("Value2.png");
	public static final Icons icon_label = new Icons("TextLabel.png");
	public static final Icons icon_vbox = new Icons("vbox.png");
	public static final Icons icon_hbox = new Icons("hbox.png");
	public static final Icons icon_text_field = new Icons("TextField.png");
	public static final Icons icon_button = new Icons("TextButton.png");
	
	private final String image;
	
	public Icons(String path) {
		image = "ide/layout/windows/icons/" + path;
	}
	
	public String getImage() {
		return image;
	}
	
	public ImageView getView() {
		ImageView iconView = new ImageView(new Image(image));
		iconView.setPrefSize(16, 16);
		return iconView;
	}
	
	public ImageView getViewWithIcon(Icons icon) {
		ImageView t = new ImageView(new Image(image)) {
			private ImageView internalIcon;
			
			{
				this.internalIcon = new ImageView(new Image(icon.getImage()));
				this.internalIcon.setPrefSize(16, 16);
			}
			
			@Override
			public void render(Context context) {
				super.render(context);
				this.internalIcon.setAbsolutePosition(this.getX()+this.getWidth()/2-1, this.getY()+this.getHeight()/2-1);
				this.internalIcon.render(context);
			}
		};
		t.setPrefSize(16,16);
		return t;
	}
}
