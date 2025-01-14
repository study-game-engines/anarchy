/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.renderers;

import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11C.glDrawArrays;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE3;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.luaj.vm2.LuaValue;

import engine.gl.IObjectRenderer;
import engine.gl.IRenderingData;
import engine.gl.RendererData;
import engine.gl.Resources;
import engine.gl.entities.LayeredCubeCamera;
import engine.gl.lights.DirectionalLightCamera;
import engine.gl.lights.SpotLightCamera;
import engine.gl.mesh.animation.AnimatedModel;
import engine.gl.mesh.animation.AnimatedModelSubMesh;
import engine.gl.objects.MaterialGL;
import engine.gl.renderers.shaders.AnimInstanceDeferredShader;
import engine.lua.type.object.Instance;
import engine.lua.type.object.PrefabRenderer;
import engine.lua.type.object.insts.GameObject;
import engine.lua.type.object.insts.Material;
import engine.lua.type.object.insts.Prefab;
import engine.lua.type.object.insts.animation.AnimationController;

public class AnimInstanceRenderer implements IObjectRenderer {

	public static final int ANIMATED_INSTANCE_RENDERER = 2;

	private AnimInstanceDeferredShader shader;
	private List<Instance> instances = new ArrayList<>();
	private AnimInstanceShadowRenderer shadowRenderer;
	private AnimInstanceForwardRenderer forwardRenderer;
	private AnimInstanceCubeRenderer cubeRenderer;

	// TODO: this should NOT be here
	private static final LuaValue C_ANIMATIONCONTROLLER = LuaValue.valueOf("AnimationController");

	private Matrix4f temp = new Matrix4f();

	public AnimInstanceRenderer() {
		shader = new AnimInstanceDeferredShader();
		shader.init();
		shadowRenderer = new AnimInstanceShadowRenderer();
		forwardRenderer = new AnimInstanceForwardRenderer();
		cubeRenderer = new AnimInstanceCubeRenderer();
	}

	@Override
	public void preProcess(List<Instance> instances) {
		for (Instance entity : instances) {
			processInstance(entity);
		}
	}

	@Override
	public void render(IRenderingData rd, RendererData rnd, Vector2f resolution) {
		shader.start();
		shader.loadCamera(rd.camera, rd.projectionMatrix, resolution, rnd.rs.taaEnabled);
		shader.loadCameraPrev(rnd.previousViewMatrix, rnd.previousProjectionMatrix);
		for (Instance instance : instances) {
			renderInstance(instance);
		}
		shader.stop();
	}

	@Override
	public void renderReflections(IRenderingData rd, RendererData rnd, LayeredCubeCamera cubeCamera) {
		cubeRenderer.render(instances, rnd, cubeCamera);
	}

	@Override
	public void renderForward(IRenderingData rd, RendererData rnd) {
		forwardRenderer.render(instances, rd, rnd);
	}

	@Override
	public void renderShadow(DirectionalLightCamera camera) {
		shadowRenderer.renderShadow(instances, camera);
	}

	@Override
	public void renderShadow(SpotLightCamera camera) {
		shadowRenderer.renderShadow(instances, camera);
	}

	@Override
	public void renderShadow(LayeredCubeCamera camera) {
		shadowRenderer.renderShadow(instances, camera);
	}

	@Override
	public void end() {
		instances.clear();
	}

	private void processInstance(Instance inst) {
		AnimationController anim = (AnimationController) inst.findFirstChildOfClass(C_ANIMATIONCONTROLLER);
		if (anim == null)
			return;

		GameObject go = anim.getLinkedInstance();
		if ( go == null )
			return;
		if (go.isDestroyed())
			return;
		if (go.getParent().isnil())
			return;
		if (go.getPrefab() == null)
			return;

		AnimatedModel animatedModel = anim.getAnimatedModel();
		if (animatedModel == null)
			return;

		animatedModel.renderV2();
		instances.add(inst);
	}

	private void renderInstance(Instance inst) {
		AnimationController anim = (AnimationController) inst.findFirstChildOfClass(C_ANIMATIONCONTROLLER);
		if (anim == null)
			return;

		GameObject go = anim.getLinkedInstance();
		if ( go == null )
			return;
		if (go.isDestroyed())
			return;
		if (go.getParent().isnil())
			return;
		if (go.getPrefab() == null)
			return;
		AnimatedModel model = anim.getAnimatedModel();
		Prefab prefab = go.getPrefab();
		PrefabRenderer pfr = prefab.getPrefab();

		Matrix4f mat = go.getWorldMatrix().toJoml();
		if ( prefab.isCenterOrigin() )
			mat.translate(pfr.getAABBOffset());
		mat.scale(go.getPrefab().getScale());
		shader.loadTransformationMatrix(mat);
		shader.loadBoneMat(model.getBoneBuffer());

		Matrix4f prevMat = temp.set(go.getPreviousWorldMatrixJOML());
		prevMat.scale(go.getPrefab().getScale());
		shader.loadTransformationMatrixPrev(prevMat);
		shader.loadBoneMatPrev(model.getPreviousBoneBuffer());
		for (int i = 0; i < model.getMeshes().size(); i++) {
			AnimatedModelSubMesh mesh = model.getMeshes().get(i);

			engine.gl.objects.MaterialGL material = Resources.MATERIAL_BLANK;
			Material ECSMat = model.getMeshToModelMap().get(mesh).getMaterial();
			if (ECSMat != null) {
				MaterialGL GLMat = ECSMat.getMaterial();
				if (GLMat != null) {
					material = GLMat;
				}
			}
			float iMatTrans = 1.0f - material.getTransparency();
			float iObjTrans = 1.0f - go.getTransparency();
			float trans = iMatTrans * iObjTrans;
			if (trans != 1.0)
				continue;

			prepareMaterial(material);
			shader.loadMaterial(material);
			mesh.bind();
			glDrawArrays(GL_TRIANGLES, 0, mesh.size());
			mesh.unbind();
		}
	}

	private void prepareMaterial(engine.gl.objects.MaterialGL mat) {
		mat.getDiffuseTexture().active(GL_TEXTURE0);
		mat.getNormalTexture().active(GL_TEXTURE1);
		mat.getMetalnessTexture().active(GL_TEXTURE2);
		mat.getRoughnessTexture().active(GL_TEXTURE3);
	}

	@Override
	public void dispose() {
		shader.dispose();
		shadowRenderer.dispose();
		forwardRenderer.dispose();
		cubeRenderer.dispose();
	}

	@Override
	public int getID() {
		return ANIMATED_INSTANCE_RENDERER;
	}

}
