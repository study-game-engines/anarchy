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

import org.joml.Matrix4f;

import engine.gl.shaders.data.UniformFloat;
import engine.gl.shaders.data.UniformMatrix4;
import engine.gl.shaders.data.UniformSampler;
import engine.gl.shaders.data.UniformVec3;
import engine.lua.type.object.insts.Camera;

public class ReflectionsShader extends BasePipelineShader {

	private UniformMatrix4 projectionMatrix = new UniformMatrix4("projectionMatrix");
	private UniformMatrix4 viewMatrix = new UniformMatrix4("viewMatrix");
	private UniformMatrix4 inverseProjectionMatrix = new UniformMatrix4("inverseProjectionMatrix");
	private UniformMatrix4 inverseViewMatrix = new UniformMatrix4("inverseViewMatrix");

	private UniformVec3 cameraPosition = new UniformVec3("cameraPosition");

	private UniformSampler gDiffuse = new UniformSampler("gDiffuse");
	private UniformSampler gNormal = new UniformSampler("gNormal");
	private UniformSampler gDepth = new UniformSampler("gDepth");
	private UniformSampler gPBR = new UniformSampler("gPBR");
	private UniformSampler gMask = new UniformSampler("gMask");

	private UniformSampler environmentCube = new UniformSampler("environmentCube");
	private UniformSampler brdfLUT = new UniformSampler("brdfLUT");
	private UniformSampler pass = new UniformSampler("pass");
	private UniformSampler reflectionTex = new UniformSampler("reflectionTex");

	private UniformSampler voxelImage = new UniformSampler("voxelImage");
	private UniformFloat voxelSize = new UniformFloat("voxelSize");
	private UniformFloat voxelOffset = new UniformFloat("voxelOffset");

	private Matrix4f projInv = new Matrix4f();

	@Override
	protected void setupShader() {
		super.setupShader();
		super.addShader(new Shader("assets/shaders/deferred/Reflections.fs", GL_FRAGMENT_SHADER));
		super.storeUniforms(projectionMatrix, viewMatrix, cameraPosition, gDiffuse, gNormal, gDepth, gPBR, gMask,
				environmentCube, brdfLUT, inverseProjectionMatrix, inverseViewMatrix, pass, reflectionTex, voxelImage,
				voxelSize, voxelOffset);
	}

	@Override
	protected void loadInitialData() {
		super.start();
		gDiffuse.loadTexUnit(0);
		gNormal.loadTexUnit(1);
		gDepth.loadTexUnit(2);
		gPBR.loadTexUnit(3);
		gMask.loadTexUnit(4);
		environmentCube.loadTexUnit(5);
		brdfLUT.loadTexUnit(6);
		pass.loadTexUnit(7);
		reflectionTex.loadTexUnit(8);
		voxelImage.loadTexUnit(9);
		super.stop();
	}

	public void loadCameraData(Camera camera, Matrix4f projection) {
		this.projectionMatrix.loadMatrix(projection);
		this.viewMatrix.loadMatrix(camera.getViewMatrixInternal());
		this.cameraPosition.loadVec3(camera.getPosition().getInternal());
		this.inverseProjectionMatrix.loadMatrix(projection.invert(projInv));
		this.inverseViewMatrix.loadMatrix(camera.getViewMatrixInverseInternal());
	}
	
	public void loadVoxelSize(float size) {
		voxelSize.loadFloat(size);
	}

	public void loadVoxelOffset(float offset) {
		voxelOffset.loadFloat(offset);
	}

}
