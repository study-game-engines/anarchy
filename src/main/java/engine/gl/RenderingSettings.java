/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl;

public class RenderingSettings {

	public volatile boolean shadowsEnabled = true;
	public volatile boolean volumetricLightEnabled = false;
	public volatile boolean fxaaEnabled = true;
	public volatile boolean taaEnabled = false;
	public volatile boolean vsyncEnabled = false; // TODO: Implement vsync toggle
	// TODO: Maximize and restore windows
	public volatile boolean motionBlurEnabled = false;
	public volatile boolean depthOfFieldEnabled = false;
	public volatile boolean ssrEnabled = false;
	public volatile boolean parallaxEnabled = false;
	public volatile boolean ambientOcclusionEnabled = true;
	public volatile boolean chromaticAberrationEnabled = false;
	public volatile boolean lensFlaresEnabled = false;

}
