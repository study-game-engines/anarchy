/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.glv2.pipeline;

import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE3;

import engine.glv2.objects.Texture;
import engine.glv2.pipeline.shaders.LensFlareModShader;
import engine.glv2.v2.DeferredPass;
import engine.glv2.v2.DeferredPipeline;
import engine.glv2.v2.RendererData;
import engine.resources.ResourcesManager;

public class LensFlareMod extends DeferredPass<LensFlareModShader> {

	private Texture lensDirt;
	private Texture lensStar;

	public LensFlareMod() {
		super("LensFlaresMod");
	}

	@Override
	public void init(int width, int height) {
		super.init(width, height);
		lensDirt = ResourcesManager.loadTextureMisc("assets/textures/lens/lens_dirt.png", null).get();
		lensStar = ResourcesManager.loadTextureMisc("assets/textures/lens/lens_star.png", null).get();
	}

	@Override
	protected LensFlareModShader setupShader() {
		return new LensFlareModShader();
	}

	@Override
	protected void setupTextures(RendererData rnd, DeferredPipeline dp, Texture[] auxTex) {
		super.activateTexture(GL_TEXTURE0, GL_TEXTURE_2D, auxTex[0].getTexture());
		super.activateTexture(GL_TEXTURE1, GL_TEXTURE_2D, lensDirt.getTexture());
		super.activateTexture(GL_TEXTURE2, GL_TEXTURE_2D, lensStar.getTexture());
		super.activateTexture(GL_TEXTURE3, GL_TEXTURE_2D, auxTex[1].getTexture());
	}

	@Override
	public void dispose() {
		super.dispose();
		lensDirt.dispose();
		lensStar.dispose();
	}

}