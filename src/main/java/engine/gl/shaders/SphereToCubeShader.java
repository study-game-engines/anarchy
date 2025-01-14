/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.shaders;

import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;

import org.joml.Matrix4f;

import engine.gl.shaders.data.Attribute;
import engine.gl.shaders.data.UniformMatrix4;
import engine.gl.shaders.data.UniformSampler;

public class SphereToCubeShader extends ShaderProgram {

	private UniformMatrix4 projectionMatrix = new UniformMatrix4("projectionMatrix");
	private UniformMatrix4 viewMatrix = new UniformMatrix4("viewMatrix");
	private UniformSampler environmentMap = new UniformSampler("environmentMap");

	@Override
	protected void setupShader() {
		super.addShader(new Shader("assets/shaders/SphereToCube.vs", GL_VERTEX_SHADER));
		super.addShader(new Shader("assets/shaders/SphereToCube.fs", GL_FRAGMENT_SHADER));
		super.setAttributes(new Attribute(0, "position"));
		super.storeUniforms(projectionMatrix, viewMatrix, environmentMap);
	}

	@Override
	protected void loadInitialData() {
		super.start();
		environmentMap.loadTexUnit(0);
		super.stop();
	}

	public void loadviewMatrix(Matrix4f viewMatrix) {
		this.viewMatrix.loadMatrix(viewMatrix);
	}

	public void loadProjectionMatrix(Matrix4f projectionMatrix) {
		this.projectionMatrix.loadMatrix(projectionMatrix);
	}

}
