/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.renderers.shaders;

import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;

import engine.gl.lights.SpotLightCamera;
import engine.gl.shaders.data.UniformMatrix4;

public class AnimInstanceSpotShadowShader extends AnimInstanceBaseShadowShader {

	private UniformMatrix4 projectionMatrix = new UniformMatrix4("projectionMatrix");
	private UniformMatrix4 viewMatrix = new UniformMatrix4("viewMatrix");

	@Override
	protected void setupShader() {
		super.setupShader();
		super.addShader(new Shader("assets/shaders/renderers/AnimInstanceSpotShadow.vs", GL_VERTEX_SHADER));
		super.storeUniforms(projectionMatrix, viewMatrix);
	}

	public void loadSpotLight(SpotLightCamera camera) {
		viewMatrix.loadMatrix(camera.getViewMatrix());
		projectionMatrix.loadMatrix(camera.getProjectionMatrix());
	}

}
