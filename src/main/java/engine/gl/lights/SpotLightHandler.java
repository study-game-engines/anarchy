/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.lights;

import static org.lwjgl.opengl.GL11C.GL_BACK;
import static org.lwjgl.opengl.GL11C.GL_BLEND;
import static org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_FRONT;
import static org.lwjgl.opengl.GL11C.GL_LINEAR;
import static org.lwjgl.opengl.GL11C.GL_ONE;
import static org.lwjgl.opengl.GL11C.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.GL_RGB;
import static org.lwjgl.opengl.GL11C.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11C.glBlendFunc;
import static org.lwjgl.opengl.GL11C.glClear;
import static org.lwjgl.opengl.GL11C.glCullFace;
import static org.lwjgl.opengl.GL11C.glDisable;
import static org.lwjgl.opengl.GL11C.glDrawElements;
import static org.lwjgl.opengl.GL11C.glEnable;
import static org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE3;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE4;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE5;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE6;
import static org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30C.GL_RGB16F;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import engine.gl.IDeferredPipeline;
import engine.gl.RenderingSettings;
import engine.gl.lights.mesh.ConeGenerator;
import engine.gl.objects.Framebuffer;
import engine.gl.objects.FramebufferBuilder;
import engine.gl.objects.Texture;
import engine.gl.objects.TextureBuilder;
import engine.gl.objects.VAO;
import engine.gl.shaders.SpotLightShader;
import engine.lua.type.object.insts.Camera;
import engine.tasks.TaskManager;

public class SpotLightHandler implements ILightHandler<SpotLightInternal> {

	private List<SpotLightInternal> lights = new ArrayList<>();

	private VAO cone;

	private Framebuffer main;
	private Texture mainTex;

	private SpotLightShader shader;

	private int width, height;

	private Matrix4f temp = new Matrix4f();
	private Vector2f texel = new Vector2f();

	private static final Vector3f UP = new Vector3f(0, 1, 0);

	public SpotLightHandler(int width, int height) {
		this.width = width;
		this.height = height;
		this.texel.set(1f / (float) width, 1f / (float) height);
		init();
	}

	public void init() {
		shader = new SpotLightShader();
		shader.init();
		cone = ConeGenerator.create(32);
		generateFramebuffer();
	}

	public void render(Camera camera, Matrix4f projectionMatrix, IDeferredPipeline dp, RenderingSettings rs) {
		main.bind();
		glCullFace(GL_FRONT);
		glClear(GL_COLOR_BUFFER_BIT);
		glEnable(GL_BLEND);
		glBlendFunc(GL_ONE, GL_ONE);
		shader.start();
		shader.loadCameraData(camera, projectionMatrix);
		shader.loadUseShadows(rs.shadowsEnabled);
		shader.loadTexel(texel);
		dp.getDiffuseTex().active(GL_TEXTURE0);
		dp.getNormalTex().active(GL_TEXTURE2);
		dp.getDepthTex().active(GL_TEXTURE3);
		dp.getPbrTex().active(GL_TEXTURE4);
		dp.getMaskTex().active(GL_TEXTURE5);
		cone.bind();
		for (SpotLightInternal l : lights) {
			if (!l.visible)
				continue;
			temp.identity();
			temp.translate(l.position);
			temp.rotateTowards(l.direction, UP);
			temp.scaleAround(1.1f, 0.0f, 0f, 0.5f);
			float fov = (float) Math.tan(Math.toRadians(l.outerFOV * 0.5f)) * l.radius;
			temp.scale(fov, fov, l.radius);
			shader.loadTransformationMatrix(temp);
			shader.loadSpotLight(l);
			l.getShadowMap().getTexture().active(GL_TEXTURE6);
			glDrawElements(GL_TRIANGLES, cone.getIndexCount(), GL_UNSIGNED_INT, 0);
		}
		cone.unbind();
		shader.stop();
		glCullFace(GL_BACK);
		glDisable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		main.unbind();
	}

	public void resize(int width, int height) {
		this.width = width;
		this.height = height;
		this.texel.set(1f / (float) width, 1f / (float) height);
		disposeFramebuffer();
		generateFramebuffer();
	}

	public void dispose() {
		disposeFramebuffer();
		cone.dispose();
	}

	private void generateFramebuffer() {
		TextureBuilder tb = new TextureBuilder();

		tb.genTexture(GL_TEXTURE_2D).bindTexture();
		tb.sizeTexture(width, height).texImage2D(0, GL_RGB16F, 0, GL_RGB, GL_FLOAT, 0);
		tb.texParameteri(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		tb.texParameteri(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		mainTex = tb.endTexture();
		FramebufferBuilder fb = new FramebufferBuilder();

		fb.genFramebuffer().bindFramebuffer().sizeFramebuffer(width, height);
		fb.framebufferTexture(GL_COLOR_ATTACHMENT0, mainTex, 0);
		main = fb.endFramebuffer();
	}

	private void disposeFramebuffer() {
		main.dispose();
		mainTex.dispose();
	}

	public Texture getMainTex() {
		return mainTex;
	}

	@Override
	public void addLight(SpotLightInternal l) {
		if (l == null)
			return;
		TaskManager.addTaskRenderThread(() -> {
			l.init();
			lights.add(l);
		});
	}

	@Override
	public void removeLight(SpotLightInternal l) {
		if (l == null)
			return;
		TaskManager.addTaskRenderThread(() -> {
			lights.remove(l);
			l.dispose();
		});
	}

	public List<SpotLightInternal> getLights() {
		return lights;
	}

}
