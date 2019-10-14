package engine.application;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import engine.Game;
import engine.InternalGameThread;
import engine.InternalRenderThread;
import engine.gl.Pipeline;
import engine.glv2.GLRenderer;
import engine.io.Load;
import engine.lua.type.object.services.UserInputService;
import lwjgui.LWJGUI;
import lwjgui.event.ScrollEvent;
import lwjgui.geometry.Insets;
import lwjgui.geometry.Pos;
import lwjgui.gl.GenericShader;
import lwjgui.scene.Window;
import lwjgui.scene.layout.StackPane;

public abstract class Runner extends RenderableApplication {
	private static StackPane rootPane;
	private static GenericShader shader;
	
	public StackPane getRootPane() {
		return rootPane;
	}
	
	@Override
	public void initialize(String[] args) {
		// Add rendering pipeline
		//pipeline = new Pipeline();
		pipeline = new GLRenderer();
		shader = new GenericShader();
		
		// Enable LWJGUI on this window (used for UI drawing)
		Window win = LWJGUI.initialize(window);
		win.setWindowAutoDraw(false); // To make it so we control swapbuffers
		win.setWindowAutoClear(false); // To make it so we control glClear()
		
		// TEST UI
		rootPane = new StackPane();
		rootPane.setPadding(new Insets(4,4,4,4));
		rootPane.setAlignment(Pos.TOP_LEFT);
		rootPane.setBackground(null);
		win.getScene().setRoot(rootPane);
		
		// Pass user input to the user input service
		rootPane.setOnKeyPressed(event -> {
			UserInputService uis = (UserInputService) Game.getService("UserInputService");
			uis.onKeyPressed(event.getKey());
		});
		rootPane.setOnKeyReleased(event -> {
			UserInputService uis = (UserInputService) Game.getService("UserInputService");
			uis.onKeyReleased(event.getKey());
		});
		rootPane.setOnMousePressed(event -> {
			UserInputService uis = (UserInputService) Game.getService("UserInputService");
			uis.onMousePress(event.button);
		});
		rootPane.setOnMouseReleased(event -> {
			UserInputService uis = (UserInputService) Game.getService("UserInputService");
			uis.onMouseRelease(event.button);
		});
		rootPane.setOnMouseScrolled(event ->{
			UserInputService uis = (UserInputService) Game.getService("UserInputService");
			uis.onMouseScroll(((ScrollEvent)event).y > 0 ? 3 : 4 );
		});
		
		// Tell the game to run
		InternalGameThread.runLater(()->{
			
			// On first render, throw out an update.
			InternalRenderThread.runLater(()->{
				loadScene(args);
				Game.setRunning(true);
				Game.load();
				
				Game.getGame().gameUpdate(true);
			});
		});
	}

	@Override
	public void render() {
		if ( GLFW.glfwWindowShouldClose(window) )
			return;
		
		// Pipeline needs to draw our workspace
		pipeline.setRenderableWorld(Game.workspace());
		
		// Render pipeline
		pipeline.setSize(windowWidth, windowHeight);
		pipeline.render();
		
		// Set viewport
		GL11.glViewport(0, 0, viewportWidth, viewportHeight);

		// Draw pipeline's buffer to screen
		pipeline.getPipelineBuffer().render(shader, true);

		LWJGUI.render(); // Gets directly rendered on-top of buffer (in same FBO)
	}
	
	public abstract void loadScene(String[] args);

	@Override
	public void tick() {
		//
	}
}
