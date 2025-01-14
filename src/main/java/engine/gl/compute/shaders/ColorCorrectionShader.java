/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.compute.shaders;

import static org.lwjgl.opengl.GL43C.GL_COMPUTE_SHADER;

import engine.gl.compute.BaseComputeShader;
import engine.gl.shaders.data.UniformFloat;
import engine.gl.shaders.data.UniformSampler;

public class ColorCorrectionShader extends BaseComputeShader {

	private UniformSampler image = new UniformSampler("image");
	private UniformFloat exposure = new UniformFloat("exposure");
	private UniformFloat gamma = new UniformFloat("gamma");

	@Override
	protected void setupShader() {
		super.setupShader();
		super.addShader(new Shader("assets/shaders/deferred_compute/ColorCorrection.comp", GL_COMPUTE_SHADER));
		this.storeUniforms(image, exposure, gamma);
	}

	@Override
	protected void loadInitialData() {
		super.start();
		image.loadTexUnit(0);
		super.stop();
	}

	public void loadExposure(float exposure) {
		this.exposure.loadFloat(exposure);
	}

	public void loadGamma(float gamma) {
		this.gamma.loadFloat(gamma);
	}

}
