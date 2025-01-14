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

import static org.lwjgl.opengl.GL11C.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11C.GL_LINEAR;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_BORDER_COLOR;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL14C.GL_TEXTURE_COMPARE_MODE;
import static org.lwjgl.opengl.GL30C.GL_COMPARE_REF_TO_TEXTURE;
import static org.lwjgl.opengl.GL30C.GL_DEPTH_ATTACHMENT;

import engine.gl.objects.Framebuffer;
import engine.gl.objects.FramebufferBuilder;
import engine.gl.objects.Texture;
import engine.gl.objects.TextureBuilder;

public class SpotLightShadowMap {

	private Framebuffer framebuffer;
	private Texture texture;

	private int size;

	public SpotLightShadowMap(int size) {
		this.size = size;
		generateFramebuffer();
	}

	public void resize(int size) {
		this.size = size;
		disposeFramebuffer();
		generateFramebuffer();
	}

	public void bind() {
		framebuffer.bind();
	}

	public void unbind() {
		framebuffer.unbind();
	}

	public void dispose() {
		disposeFramebuffer();
	}

	@Deprecated
	public Texture getShadowMap() {
		return texture;
	}

	public Texture getTexture() {
		return texture;
	}

	private void generateFramebuffer() {
		TextureBuilder tb = new TextureBuilder();

		tb.genTexture(GL_TEXTURE_2D).bindTexture();
		tb.sizeTexture(size, size).texImage2D(0, GL_DEPTH_COMPONENT, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_BYTE, 0);
		tb.texParameteri(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		tb.texParameteri(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		tb.texParameteri(GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
		float borderColor[] = { 1.0f, 1.0f, 1.0f, 1.0f };
		tb.texParameterfv(GL_TEXTURE_BORDER_COLOR, borderColor);
		texture = tb.endTexture();

		FramebufferBuilder fb = new FramebufferBuilder();

		fb.genFramebuffer().bindFramebuffer().sizeFramebuffer(size, size);
		fb.framebufferTexture(GL_DEPTH_ATTACHMENT, texture, 0);
		framebuffer = fb.endFramebuffer();
	}

	private void disposeFramebuffer() {
		framebuffer.dispose();
		texture.dispose();
	}

}
