/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.pipeline.shaders;

import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;

import engine.gl.shaders.data.UniformBoolean;
import engine.gl.shaders.data.UniformSampler;

public class GaussianShader extends BasePipelineShader {

	private UniformSampler image = new UniformSampler("image");

	private UniformBoolean vertical = new UniformBoolean("vertical");

	@Override
	protected void setupShader() {
		super.setupShader();
		super.addShader(new Shader("assets/shaders/deferred/GaussianBlur.vs", GL_VERTEX_SHADER));
		super.addShader(new Shader("assets/shaders/deferred/GaussianBlur.fs", GL_FRAGMENT_SHADER));
		super.storeUniforms(image, vertical);
	}

	@Override
	protected void loadInitialData() {
		super.start();
		image.loadTexUnit(0);
		super.stop();
	}

	public void useVerticalBlur(boolean vertical) {
		this.vertical.loadBoolean(vertical);
	}

}
