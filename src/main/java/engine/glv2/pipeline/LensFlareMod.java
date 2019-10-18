/*
 * This file is part of Light Engine
 * 
 * Copyright (C) 2016-2019 Lux Vacuos
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package engine.glv2.pipeline;

import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE3;

import engine.gl.Texture2D;
import engine.glv2.objects.Texture;
import engine.glv2.pipeline.shaders.LensFlareModShader;
import engine.glv2.v2.DeferredPass;
import engine.glv2.v2.DeferredPipeline;
import engine.glv2.v2.RendererData;
import engine.util.TextureUtils;

public class LensFlareMod extends DeferredPass<LensFlareModShader> {

	private Texture2D lensDirt;
	private Texture2D lensStar;

	public LensFlareMod() {
		super("LensFlaresMod");
	}

	@Override
	public void init(int width, int height) {
		super.init(width, height);
		lensDirt = TextureUtils.loadRGBATexture("assets/textures/lens/lens_dirt.png");
		lensStar = TextureUtils.loadRGBATexture("assets/textures/lens/lens_star.png");
	}

	@Override
	protected LensFlareModShader setupShader() {
		return new LensFlareModShader(name);
	}

	@Override
	protected void setupTextures(RendererData rnd, DeferredPipeline dp, Texture[] auxTex) {
		super.activateTexture(GL_TEXTURE0, GL_TEXTURE_2D, auxTex[0].getTexture());
		super.activateTexture(GL_TEXTURE1, GL_TEXTURE_2D, lensDirt.getID());
		super.activateTexture(GL_TEXTURE2, GL_TEXTURE_2D, lensStar.getID());
		super.activateTexture(GL_TEXTURE3, GL_TEXTURE_2D, auxTex[1].getTexture());
	}

	@Override
	public void dispose() {
		super.dispose();
		//lensDirt.dispose();
		//lensStar.dispose();
	}

}