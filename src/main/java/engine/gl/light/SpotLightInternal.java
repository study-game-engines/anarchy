package engine.gl.light;

import org.joml.Vector3f;

public class SpotLightInternal extends Light {
	public float outerFOV = 90;
	public float innerFOV = 70;
	public float radius = 10;
	
	public SpotLightInternal(Vector3f position, float outerFOV, float innerFOV, float radius, float intensity) {
		this.x = position.x;
		this.y = position.y;
		this.z = position.z;
		this.outerFOV = outerFOV;
		this.innerFOV = innerFOV;
		this.radius= radius;
		this.intensity = intensity;
	}
}
