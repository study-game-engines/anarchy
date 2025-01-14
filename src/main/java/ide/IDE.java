/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package ide;

import engine.ClientEngine;
import engine.Game;
import engine.InternalGameThread;
import engine.InternalRenderThread;
import engine.io.Load;
import engine.lua.type.object.ScriptBase;
import engine.lua.type.object.insts.ui.CSS;
import engine.lua.type.object.services.UserInputService;
import ide.layout.IdeLayout;
import ide.layout.windows.IdeCSSEditor;
import ide.layout.windows.IdeLuaEditor;
import lwjgui.scene.Group;
import lwjgui.scene.Scene;
import lwjgui.scene.Window;
import lwjgui.scene.WindowManager;
import lwjgui.scene.WindowThread;

public class IDE extends ClientEngine {
	public static IdeLayout layout;

	public static final String TITLE = "Anarchy Engine - IDE Build " + Game.version();

	public IDE(String[] args) {
		super(args);
	}

	@Override
	public void setupEngine() {
		renderThread.getWindow().setTitle(TITLE);
		InternalRenderThread.desiredFPS = 60;
		InternalGameThread.desiredTPS = 60;

		// Setup background pane
		Group background = new Group();
		renderThread.getWindow().getScene().setRoot(background);

		//Theme.setTheme(new ThemeDark());

		// If someone wants to load a game directly
		if (args != null && args.length > 0) {

			// Get project args
			String[] tempArgs = new String[Math.max(0, args.length - 1)];
			for (int i = 1; i < args.length; i++) {
				tempArgs[i - 1] = args[i];
			}

			// Tell game we're a client
			if (args[0].toLowerCase().equals("client")) {
				game.setServer(false); // Mark this game as a client
			}

			// Load project
			if (tempArgs.length > 0) {
				InternalRenderThread.runLater(() -> {
					InternalGameThread.runLater(() -> {
						Load.load(tempArgs[0]);
						Game.setRunning(true);
					});
				});
			}
		}

		renderThread.getPipeline().setRenderableWorld(Game.workspace());

		// Setup mane IDE layout
		layout = new IdeLayout(background);
	}

	public static void openScript(ScriptBase instance) {
		WindowManager.runLater(() -> {
			new WindowThread(instance.getName() + ".lua [" + instance.getFullName() + "]") {
				@Override
				protected void init(Window window) {
					window.setScene(new Scene(new IdeLuaEditor(instance), 500, 400));
					window.show();
				}
			}.start();
		});
	}

	public static void openCSS(CSS instance) {
		WindowManager.runLater(() -> {
			new WindowThread(instance.getName() + ".css [" + instance.getFullName() + "]") {
				@Override
				protected void init(Window window) {
					window.setScene(new Scene(new IdeCSSEditor(instance), 500, 400));
					window.show();
				}
			}.start();
		});
	}

	private boolean grabbed;

	@Override
	public void render() {
		// TODO Auto-generated method stub
	}

	@Override
	public void update() {
		if (UserInputService.lockMouse && renderThread.getPipeline().getDisplayPane().isSelectedOrDescendentSelected() && renderThread.getWindow().isFocused()) {
			if (!grabbed) {;
				grabbed = true;
				renderThread.getWindow().getMouseHandler().setGrabbed(true);
			}
		} else {
			if (grabbed) {
				renderThread.getWindow().getMouseHandler().setGrabbed(false);
				grabbed = false;
			}
		}
	}

	public static void main(String[] args) {
		new IDE(args);
	}
}