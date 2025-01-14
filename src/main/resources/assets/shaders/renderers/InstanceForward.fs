/*

Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)

This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

 */

#include struct Material

#include struct PointLight

#include struct DirectionalLight

in vec2 pass_textureCoords;
in vec4 pass_position;
in mat3 TBN;

out vec4 out_Color;

uniform bool colorCorrect;
uniform Material material;
uniform vec3 cameraPosition;
uniform samplerCube irradianceMap;
uniform samplerCube preFilterEnv;
uniform sampler2D brdfLUT;
uniform bool useShadows;
uniform mat4 biasMatrix;
uniform float transparency;
uniform float exposure;
uniform float gamma;

uniform PointLight pointLights[8];
uniform int totalPointLights;

uniform DirectionalLight directionalLights[8];
uniform int totalDirectionalLights;

#include variable pi

#include function DistributionGGX

#include function GeometrySchlickGGX

#include function GeometrySmith

#include function fresnelSchlickRoughness

#include function fresnelSchlick

#include function computeShadowV2
/*
#include function calcPointLight
*/
#include function calcDirectionalLight

#include function toneMap

#include variable GLOBAL

void main() {

	vec4 diffuseF = texture(material.diffuseTex, pass_textureCoords);
	float roughness = texture(material.roughnessTex, pass_textureCoords).r;
	float metallic = texture(material.metallicTex, pass_textureCoords).r;

	diffuseF.rgb *= material.diffuse;
	roughness *= material.roughness;
	metallic *= material.metallic;
	diffuseF.a *= transparency;

	if (diffuseF.a == 0.0)
		discard;

	vec3 normal = texture(material.normalTex, pass_textureCoords).rgb;
	normal = normalize(normal * 2.0 - 1.0);
	normal = normalize(TBN * normal);

	vec3 position = pass_position.xyz;

	vec3 N = normalize(normal);
	vec3 V = normalize(cameraPosition - position);
	vec3 R = reflect(-V, N);

	vec3 F0 = vec3(0.04);
	F0 = mix(F0, diffuseF.rgb, metallic);
	vec3 Lo = vec3(0.0);

	for (int i = 0; i < totalDirectionalLights; i++) {
		Lo += calcDirectionalLight(directionalLights[i], position, diffuseF.rgb, N, V, F0,
								   roughness, metallic);
	}

	/*for (int i = 0; i < totalPointLights; i++) {
		Lo += calcPointLight(pointLights[i], position, diffuseF.rgb, N, V, F0, roughness, metallic);
	}*/

	vec3 F = fresnelSchlickRoughness(max(dot(N, V), 0.0), F0, roughness);

	vec3 kS = F;
	vec3 kD = vec3(1.0) - kS;
	kD *= 1.0 - metallic;

	vec3 irradiance = texture(irradianceMap, N).rgb;
	vec3 diffuse = irradiance * diffuseF.rgb;

	vec3 prefilteredColor = textureLod(preFilterEnv, R, roughness * MAX_REFLECTION_LOD).rgb;
	vec2 envBRDF = texture(brdfLUT, vec2(max(dot(N, V), 0.0), roughness)).rg;
	vec3 specular = prefilteredColor * (F * envBRDF.x + envBRDF.y);

	vec3 emissive = material.emissive.rgb * diffuseF.rgb;

	vec3 ambient = kD * diffuse + specular;
	vec3 color = ambient + emissive + Lo;
	if (colorCorrect) {
		vec3 final = vec3(1.0) - exp(-color * exposure);

		// Apply tone-mapping
		final = toneMap(final);

		// Apply Gamma
		vec3 whiteScale = 1.0 / toneMap(vec3(W));
		final = pow(final * whiteScale, vec3(1.0 / gamma));
		color = final;
	}

	out_Color = vec4(color, diffuseF.a);
}
