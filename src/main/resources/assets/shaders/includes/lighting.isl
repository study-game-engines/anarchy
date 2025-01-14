/*

Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)

This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

 */

#variable MASK
#define PBR_NONE 0.0
#define PBR_OBJECT 1.0
#define PBR_BACKGROUND 2.0
#define PBR_BACKGROUND_DYNAMIC 3.0

#define MASK_COMPARE(a, b) abs(a - b) < 0.2
#end

#struct PointLight
struct PointLight {
	vec3 position;
	vec3 color;
	float radius;
	float intensity;
	bool visible;
	samplerCubeShadow shadowMap;
	mat4 projectionMatrix;
	bool shadows;
};
#end

#struct SpotLight
struct SpotLight {
	vec3 position;
	vec3 direction;
	vec3 color;
	float radius;
	float intensity;
	bool visible;
	float outerFOV;
	float innerFOV;
	mat4 viewMatrix;
	mat4 projectionMatrix;
	sampler2DShadow shadowMap;
	bool shadows;
};
#end

#struct DirectionalLight
struct DirectionalLight {
	vec3 direction;
	vec3 color;
	float intensity;
	bool visible;
	mat4 viewMatrix;
	mat4 projectionMatrix0;
	mat4 projectionMatrix1;
	mat4 projectionMatrix2;
	mat4 projectionMatrix3;
	sampler2DArrayShadow shadowMap;
	bool shadows;
};
#end

#struct AreaLight
struct AreaLight {
	vec3 position;
	vec3 direction;
	vec3 color;
	float intensity;
	bool visible;
};
#end

#struct DynamicSky
struct DynamicSky {
	float brightness;
	float time;
	float cloudHeight;
	float cloudSpeed;
};
#end

#function DistributionGGX
float DistributionGGX(vec3 N, vec3 H, float roughness) {
	float a = roughness * roughness;
	float a2 = a * a;
	float NdotH = max(dot(N, H), 0.0);
	float NdotH2 = NdotH * NdotH;

	float nom = a2;
	float denom = (NdotH2 * (a2 - 1.0) + 1.0);
	denom = PI * denom * denom;

	return nom / denom;
}
#end

#function GeometrySchlickGGX
float GeometrySchlickGGX(float NdotV, float roughness) {
	float r = (roughness + 1.0);
	float k = (r * r) / 8.0;

	float nom = NdotV;
	float denom = NdotV * (1.0 - k) + k;

	return nom / denom;
}
#end

#function GeometrySmith
float GeometrySmith(vec3 N, vec3 V, vec3 L, float roughness) {
	float NdotV = max(dot(N, V), 0.0);
	float NdotL = max(dot(N, L), 0.0);
	float ggx2 = GeometrySchlickGGX(NdotV, roughness);
	float ggx1 = GeometrySchlickGGX(NdotL, roughness);

	return ggx1 * ggx2;
}
#end

#function fresnelSchlickRoughness
vec3 fresnelSchlickRoughness(float cosTheta, vec3 F0, float roughness) {
	return F0 + (max(vec3(1.0 - roughness), F0) - F0) * pow(1.0 - cosTheta, 5.0);
}
#end

#function fresnelSchlick
vec3 fresnelSchlick(float cosTheta, vec3 F0) {
	return F0 + (1.0 - F0) * pow(1.0 - cosTheta, 5.0);
}
#end

#function computeAmbientOcclusion
const float distanceThreshold = 1;
const int sample_count = 16;
const vec2 poisson16[] = vec2[](
	vec2(-0.94201624, -0.39906216), vec2(0.94558609, -0.76890725), vec2(-0.094184101, -0.92938870),
	vec2(0.34495938, 0.29387760), vec2(-0.91588581, 0.45771432), vec2(-0.81544232, -0.87912464),
	vec2(-0.38277543, 0.27676845), vec2(0.97484398, 0.75648379), vec2(0.44323325, -0.97511554),
	vec2(0.53742981, -0.47373420), vec2(-0.26496911, -0.41893023), vec2(0.79197514, 0.19090188),
	vec2(-0.24188840, 0.99706507), vec2(-0.81409955, 0.91437590), vec2(0.19984126, 0.78641367),
	vec2(0.14383161, -0.14100790));

float computeAmbientOcclusion(vec3 position, vec3 normal) {
	if (useAmbientOcclusion == 1) {
		float ambientOcclusion = 0;
		vec2 filterRadius = vec2(10 / resolution.x, 10 / resolution.y);
		for (int i = 0; i < sample_count; ++i) {
			vec2 sampleTexCoord = textureCoords + (poisson16[i] * (filterRadius));
			float sampleDepth = texture(gDepth, sampleTexCoord).r;
			vec3 samplePos = texture(gPosition, sampleTexCoord).rgb;
			vec3 sampleDir = normalize(samplePos - position);
			float NdotS = max(dot(normal, sampleDir), 0);
			float VPdistSP = distance(position, samplePos);
			float a = 1.0 - smoothstep(distanceThreshold, distanceThreshold * 2, VPdistSP);
			float b = NdotS;
			ambientOcclusion += (a * b) * 1.3;
		}
		return -(ambientOcclusion / sample_count) + 1;
	} else
		return 1.0;
}
#end

#function computeShadow

vec4 ShadowCoord[4];

vec2 multTex;

float lookup(vec2 offsetIn) {
	vec4 shadowTexCoord;
	vec2 offset = offsetIn * multTex;
	if (ShadowCoord[3].x > 0 && ShadowCoord[3].x < 1 && ShadowCoord[3].y > 0 &&
		ShadowCoord[3].y < 1) {
		if (ShadowCoord[2].x > 0 && ShadowCoord[2].x < 1 && ShadowCoord[2].y > 0 &&
			ShadowCoord[2].y < 1) {
			if (ShadowCoord[1].x > 0 && ShadowCoord[1].x < 1 && ShadowCoord[1].y > 0 &&
				ShadowCoord[1].y < 1) {
				if (ShadowCoord[0].x > 0 && ShadowCoord[0].x < 1 && ShadowCoord[0].y > 0 &&
					ShadowCoord[0].y < 1) {
					shadowTexCoord.xyw = ShadowCoord[0].xyz + vec3(offset.x, offset.y, 0);
					shadowTexCoord.z = 0;
					return texture(shadowMap, shadowTexCoord);
				}
				shadowTexCoord.xyw = ShadowCoord[1].xyz + vec3(offset.x, offset.y, 0);
				shadowTexCoord.z = 1;
				return texture(shadowMap, shadowTexCoord);
			}
			shadowTexCoord.xyw = ShadowCoord[2].xyz + vec3(offset.x, offset.y, 0);
			shadowTexCoord.z = 2;
			return texture(shadowMap, shadowTexCoord);
		}
		shadowTexCoord.xyw = ShadowCoord[3].xyz + vec3(offset.x, offset.y, 0);
		shadowTexCoord.z = 3;
		return texture(shadowMap, shadowTexCoord);
	}
	return 1.0;
}

float computeShadow(vec3 position) {
	if (useShadows == 1) {
		float shadow = 0.0;
		vec4 posLight = viewLightMatrix * vec4(position, 1.0);
		multTex = 1.0 / textureSize(shadowMap, 0).xy;
		ShadowCoord[0] = biasMatrix * (projectionLightMatrix[0] * posLight);
		ShadowCoord[1] = biasMatrix * (projectionLightMatrix[1] * posLight);
		ShadowCoord[2] = biasMatrix * (projectionLightMatrix[2] * posLight);
		ShadowCoord[3] = biasMatrix * (projectionLightMatrix[3] * posLight);
		for (int x = -1; x <= 1; ++x) {
			for (int y = -1; y <= 1; ++y) {
				shadow += lookup(vec2(x, y));
			}
		}
		return shadow / 9.0;
	} else
		return 1.0;
}
#end

#function getDepth
float getDepth(mat4 proj, sampler2D depth, vec2 texcoord) {
	float zndc = texture(depth, texcoord).r;
#ifdef OneToOneDepth
	zndc = zndc * 2.0 - 1.0;
#endif
	float A = proj[2][2];
	float B = proj[3][2];
	return B / (A + zndc);
}

float getDepth(mat4 proj, sampler2D depth, ivec2 texcoord) {
	float zndc = texelFetch(depth, texcoord, 0).r;
#ifdef OneToOneDepth
	zndc = zndc * 2.0 - 1.0;
#endif
	float A = proj[2][2];
	float B = proj[3][2];
	return B / (A + zndc);
}
#end

#function positionFromDepth
vec3 positionFromDepth(vec2 texCoords, float depth, mat4 invProjection, mat4 invView) {
#ifdef OneToOneDepth
	vec4 currentPosition = vec4(texCoords * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
#else
	vec4 currentPosition = vec4(texCoords * 2.0 - 1.0, depth, 1.0);
#endif
	vec4 position = invProjection * currentPosition;
	position = invView * position;
	position.xyz /= position.w;
	return position.xyz;
}
#end

#function computeAmbientOcclusionV2
const float distanceThreshold = 0.5;
const int sample_count = 16;
const vec2 poisson16[] = vec2[](
	vec2(-0.94201624, -0.39906216), vec2(0.94558609, -0.76890725), vec2(-0.094184101, -0.92938870),
	vec2(0.34495938, 0.29387760), vec2(-0.91588581, 0.45771432), vec2(-0.81544232, -0.87912464),
	vec2(-0.38277543, 0.27676845), vec2(0.97484398, 0.75648379), vec2(0.44323325, -0.97511554),
	vec2(0.53742981, -0.47373420), vec2(-0.26496911, -0.41893023), vec2(0.79197514, 0.19090188),
	vec2(-0.24188840, 0.99706507), vec2(-0.81409955, 0.91437590), vec2(0.19984126, 0.78641367),
	vec2(0.14383161, -0.14100790));

float computeAmbientOcclusion(vec2 texCoords, vec3 position, vec3 normal, sampler2D gDepth,
							  mat4 projection, mat4 invProjection, mat4 invView) {
	float ambientOcclusion = 0;
	float depth = getDepth(projection, gDepth, texCoords);
	vec2 filterRadius = vec2(0.05) / depth;
	for (int i = 0; i < sample_count; ++i) {
		vec2 sampleTexCoord = texCoords + (poisson16[i] * filterRadius);
		float depthRaw = texture(gDepth, sampleTexCoord).r;
		vec3 samplePos = positionFromDepth(sampleTexCoord, depthRaw, invProjection, invView);
		vec3 sampleDir = normalize(samplePos - position);
		float NdotS = max(dot(normal, sampleDir), 0.0);
		float VPdistSP = distance(position, samplePos);
		float a = 1.0 - smoothstep(distanceThreshold, distanceThreshold * 2, VPdistSP);
		float b = NdotS;
		ambientOcclusion += (a * b);
	}
	return max(-(ambientOcclusion / sample_count) + 1.0, 0.0);
}
#end

#function calcPointLight
vec3 calcPointLight(PointLight light, vec3 position, vec3 diffuse, vec3 N, vec3 V, vec3 F0,
					float roughness, float metallic) {
	if (!light.visible)
		return vec3(0.0);
	float distance = length(light.position - position);
	float attenuation = max(1.0 - distance / light.radius, 0.0) / distance;
	vec3 radiance = light.color * attenuation * light.intensity;
	if (radiance.r <= 0.0 && radiance.g <= 0.0 && radiance.b <= 0.0)
		return vec3(0.0);

	vec3 L = normalize(light.position - position);
	vec3 H = normalize(V + L);
	float NdotL = max(dot(N, L), 0.0);

	float NDF = DistributionGGX(N, H, roughness);
	float G = GeometrySmith(N, V, L, roughness);
	vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);

	vec3 nominator = NDF * G * F;
	float denominator = max(dot(N, V), 0.0) * NdotL + 0.001;
	vec3 brdf = nominator / denominator;

	vec3 kS = F;
	vec3 kD = vec3(1.0) - kS;
	kD *= 1.0 - metallic;

	if (light.shadows) {
		vec3 samplePos = position - light.position;

		vec3 absTarget = abs(samplePos);
		float linearDepth = max(absTarget.x, max(absTarget.y, absTarget.z));

		float A = light.projectionMatrix[2][2];
		float B = light.projectionMatrix[3][2];
		float sampleDepth = 0.5 * (-A * linearDepth + B) / linearDepth + 0.5;

		NdotL *= texture(light.shadowMap, vec4(samplePos, sampleDepth));
	}
	return (kD * diffuse / PI + brdf) * radiance * NdotL;
}
#end

#function calcSpotLight
vec3 calcSpotLight(SpotLight light, vec3 position, vec3 diffuse, vec3 N, vec3 V, vec3 F0,
				   float roughness, float metallic) {
	if (!light.visible)
		return vec3(0.0);
	vec3 L = normalize(light.position - position);

	float theta = dot(L, normalize(-light.direction));
	float epsilon = light.innerFOV - light.outerFOV;
	float intensity = clamp((theta - light.outerFOV) / epsilon, 0.0, 1.0);
	if (intensity <= 0.0)
		return vec3(0.0);

	float distance = length(light.position - position);
	float attenuation = max(1.0 - distance / light.radius, 0.0) / distance;
	vec3 radiance = light.color * attenuation * light.intensity * intensity;
	if (radiance.r <= 0.0 && radiance.g <= 0.0 && radiance.b <= 0.0)
		return vec3(0.0);

	vec3 H = normalize(V + L);
	float NdotL = max(dot(N, L), 0.0);

	float NDF = DistributionGGX(N, H, roughness);
	float G = GeometrySmith(N, V, L, roughness);
	vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);

	vec3 nominator = NDF * G * F;
	float denominator = max(dot(N, V), 0.0) * NdotL + 0.001;
	vec3 brdf = nominator / denominator;

	vec3 kS = F;
	vec3 kD = vec3(1.0) - kS;
	kD *= 1.0 - metallic;

	if (light.shadows) {
		vec4 posLight = light.viewMatrix * vec4(position, 1.0);
		vec4 shadowCoord = biasMatrix * (light.projectionMatrix * posLight);
		vec2 multTex = 1.0 / textureSize(light.shadowMap, 0).xy;
		float shadow = 0.0;
		for (int x = -1; x <= 1; ++x) {
			for (int y = -1; y <= 1; ++y) {
				vec2 offset = vec2(x, y) * multTex;
				vec3 temp = shadowCoord.xyz + vec3(offset  * shadowCoord.z, 0);
				shadow += texture(light.shadowMap, (temp / shadowCoord.w), 0);
			}
		}
		NdotL *= shadow / 9.0; // -2~2 = 25.0
	}
	return (kD * diffuse / PI + brdf) * radiance * NdotL;
}
#end

#function calcDirectionalLight
vec3 calcDirectionalLight(DirectionalLight light, vec3 position, vec3 diffuse, vec3 N, vec3 V,
						  vec3 F0, float roughness, float metallic) {
	if (!light.visible)
		return vec3(0.0);
	vec3 L = normalize(light.direction);
	vec3 H = normalize(V + L);
	float NdotL = max(dot(N, L), 0.0);

	vec3 radiance = light.color * light.intensity;

	float NDF = DistributionGGX(N, H, roughness);
	float G = GeometrySmith(N, V, L, roughness);
	vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);

	vec3 nominator = NDF * G * F;
	float denominator = max(dot(N, V), 0.0) * NdotL + 0.001;
	vec3 brdf = nominator / denominator;

	vec3 kS = F;
	vec3 kD = vec3(1.0) - kS;
	kD *= 1.0 - metallic;

	if (light.shadows)
		NdotL *= computeShadowV2(position, light);
	return (kD * diffuse / PI + brdf) * radiance * NdotL;
}
#end

#function computeShadowV2
float lookupV2(vec2 offsetIn, vec2 multTex, vec4 shadowCoord[4], sampler2DArrayShadow shdmap) {
	vec4 shadowTexCoord;
	vec2 offset = offsetIn * multTex;
	if (shadowCoord[3].x > 0 && shadowCoord[3].x < 1 && shadowCoord[3].y > 0 &&
		shadowCoord[3].y < 1) {
		if (shadowCoord[2].x > 0 && shadowCoord[2].x < 1 && shadowCoord[2].y > 0 &&
			shadowCoord[2].y < 1) {
			if (shadowCoord[1].x > 0 && shadowCoord[1].x < 1 && shadowCoord[1].y > 0 &&
				shadowCoord[1].y < 1) {
				if (shadowCoord[0].x > 0 && shadowCoord[0].x < 1 && shadowCoord[0].y > 0 &&
					shadowCoord[0].y < 1) {
					shadowTexCoord.xyw = shadowCoord[0].xyz + vec3(offset.x, offset.y, 0);
					shadowTexCoord.z = 0;
					return texture(shdmap, shadowTexCoord);
				}
				shadowTexCoord.xyw = shadowCoord[1].xyz + vec3(offset.x, offset.y, 0);
				shadowTexCoord.z = 1;
				return texture(shdmap, shadowTexCoord);
			}
			shadowTexCoord.xyw = shadowCoord[2].xyz + vec3(offset.x, offset.y, 0);
			shadowTexCoord.z = 2;
			return texture(shdmap, shadowTexCoord);
		}
		shadowTexCoord.xyw = shadowCoord[3].xyz + vec3(offset.x, offset.y, 0);
		shadowTexCoord.z = 3;
		return texture(shdmap, shadowTexCoord);
	}
	return 1.0;
}

float computeShadowV2(vec3 position, DirectionalLight light) {
	if (useShadows) {
		float shadow = 0.0;
		vec4 posLight = light.viewMatrix * vec4(position, 1.0);
		vec2 multTex = 1.0 / textureSize(light.shadowMap, 0).xy;
		vec4 shadowCoord[4];
		shadowCoord[0] = biasMatrix * (light.projectionMatrix0 * posLight);
		shadowCoord[1] = biasMatrix * (light.projectionMatrix1 * posLight);
		shadowCoord[2] = biasMatrix * (light.projectionMatrix2 * posLight);
		shadowCoord[3] = biasMatrix * (light.projectionMatrix3 * posLight);
		for (int x = -1; x <= 1; ++x) {
			for (int y = -1; y <= 1; ++y) {
				shadow += lookupV2(vec2(x, y), multTex, shadowCoord, light.shadowMap);
			}
		}
		return shadow / 9.0;
	} else
		return 1.0;
}
#end