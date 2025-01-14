/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.util;

import org.joml.Vector3f;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btBvhTriangleMeshShape;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShapeZ;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.btConvexHullShape;
import com.badlogic.gdx.physics.bullet.collision.btConvexShape;
import com.badlogic.gdx.physics.bullet.collision.btCylinderShapeZ;
import com.badlogic.gdx.physics.bullet.collision.btGImpactMeshShape;
import com.badlogic.gdx.physics.bullet.collision.btShapeHull;
import com.badlogic.gdx.physics.bullet.collision.btSphereShape;
import com.badlogic.gdx.physics.bullet.collision.btTriangleIndexVertexArray;
import com.badlogic.gdx.physics.bullet.collision.btTriangleMesh;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody.btRigidBodyConstructionInfo;
import com.badlogic.gdx.physics.bullet.linearmath.btDefaultMotionState;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;

import engine.gl.mesh.BufferedMesh;
import engine.gl.mesh.Vertex;

public class PhysicsUtils {
	private static Matrix4 DEFAULT_TRANSFORM = new Matrix4().idt();
	
	public static btRigidBody box( float mass, float bouncyness, float width, float length, float height ) {
		btConvexShape boxShape = new btBoxShape(new Vector3(width/2f, length/2f, height/2f));
		return getBody( mass, bouncyness, 0.5f, boxShape );
	}

	public static btRigidBody cube( float mass, float bouncyness, float length ) {
		return box( mass, bouncyness, length, length, length );
	}

	public static btRigidBody sphere( float mass, float bouncyness, float radius ) {
		btConvexShape ballShape = new btSphereShape(radius);
		return getBody( mass, bouncyness, 0.5f, ballShape );
	}

	public static btRigidBody capsule( float mass, float bouncyness, float radius, float height ) {
		btConvexShape shape = new btCapsuleShapeZ( radius, (height*0.99f) - radius*2 );
		return getBody( mass, bouncyness, 0.5f, shape );
	}

	public static btRigidBody cylinder( float mass, float bouncyness, float radius, float height ) {
		btConvexShape shape = new btCylinderShapeZ( new Vector3(radius, radius, height ) );
		return getBody( mass, bouncyness, 0.5f, shape );
	}

	public static btRigidBody shape( float mass, float bouncyness, btConvexShape shape ) {
		return getBody( mass, bouncyness, 0.5f, shape );
	}

	public static btRigidBody getBody( float mass, float bouncyness, float friction, btCollisionShape shape ) {
		btMotionState bodyMotionState = new btDefaultMotionState(DEFAULT_TRANSFORM);
		Vector3 ballInertia = new Vector3();
		if ( mass != 0 )
			shape.calculateLocalInertia(mass, ballInertia);
		
		btRigidBodyConstructionInfo bodyInfo = new btRigidBodyConstructionInfo(mass, bodyMotionState, shape, ballInertia);
		bodyInfo.setRestitution(bouncyness);
		bodyInfo.setFriction(friction);
		bodyInfo.setAngularDamping(0.3f);
		bodyInfo.setLinearDamping(0.1f);

		btRigidBody body = new btRigidBody(bodyInfo);
		if ( mass == 0 )
			body.setActivationState(4); // DISABLE_DEACTIVATION
		body.activate(true);

		return body;
	}

	/**
	 * Will return a rigid-body based on a mesh. It is automatically added to the physics world.
	 * <br>
	 * <br>
	 * Should not be used for dynamic physics objects.
	 * @param mass
	 * @param bouncyness
	 * @param bufferedMesh
	 * @return
	 */
	public static btRigidBody mesh(float mass, float bouncyness, float friction, BufferedMesh bufferedMesh) {
		if ( mass == 0 ) {
			return getBody( mass, bouncyness, friction, meshShapeStatic( bufferedMesh, 1.0f, new Vector3f() ) );
		} else {
	        return getBody( mass, bouncyness, friction, meshShapeDynamic( bufferedMesh, 1.0f, new Vector3f() ) );
		}
	}
	
	public static btCollisionShape meshShapeStatic( BufferedMesh bufferedMesh, float scale, Vector3f offset ) {
		if ( bufferedMesh == null )
			return null;
		
		return new btBvhTriangleMeshShape(meshVertexArray(bufferedMesh, scale, offset), true);
	}
	
	public static btCollisionShape meshShapeDynamic( BufferedMesh bufferedMesh, float scale, Vector3f offset ) {
		if ( bufferedMesh == null )
			return null;

		btGImpactMeshShape meshShape = new btGImpactMeshShape(meshVertexArray(bufferedMesh, scale, offset));
		meshShape.updateBound();
		
		return meshShape;
	}
	
	private static btTriangleIndexVertexArray meshVertexArray( BufferedMesh bufferedMesh, float scale, Vector3f offset ) {
		if ( bufferedMesh == null )
			return null;
		
		btTriangleMesh mesh = new btTriangleMesh();
		Vertex[] vertices = bufferedMesh.getVertices();
		int totalVerts = vertices.length;
		int totalTris = totalVerts / 3;
		
		float offx = offset.x;
		float offy = offset.y;
		float offz = offset.z;
		
		int a = 0;
		for (int i = 0; i < totalTris; i++) {
			float[] v0 = vertices[a++].getXYZ();
			Vector3 vertex0 = new Vector3(v0[0]+offx, v0[1]+offy, v0[2]+offz);
	
			float[] v1 = vertices[a++].getXYZ();
			Vector3 vertex1 = new Vector3(v1[0]+offx, v1[1]+offy, v1[2]+offz);
	
			float[] v2 = vertices[a++].getXYZ();
			Vector3 vertex2 = new Vector3(v2[0]+offx, v2[1]+offy, v2[2]+offz);
			
			mesh.addTriangle(vertex0, vertex1, vertex2);
		}
		
		return mesh;
	}

	/**
	 * Returns a btRigidBody based on a simplified version of the supplied mesh. It is automatically added to the physics world.
	 * <br>
	 * <br>
	 * Can be used with dynamic physics objects.
	 * @param mass
	 * @param bouncyness
	 * @param mesh
	 * @return
	 */
	public static btRigidBody hull( float mass, float bouncyness, float friction, float scalar, BufferedMesh mesh, boolean simplified ) {
		// REturn rigid body
		return getBody( mass, bouncyness, friction, hullShape(mesh, scalar, simplified) );
	}
	
	public static btConvexShape hullShape( BufferedMesh mesh, float scale, boolean simplified ) {
		if ( mesh == null ) {
			return new btBoxShape( new Vector3(0.5f, 0.5f, 0.5f) );
		}

		// Create original hull
		btConvexHullShape shape = new btConvexHullShape();
		
		// Populate vertex list
		for (int i = 0; i < mesh.getSize(); i++) {
			Vertex vert = mesh.getVertex(i);
			float[] xyz = vert.getXYZ();
			shape.addPoint( new Vector3( xyz[0] * scale, xyz[1] * scale, xyz[2] * scale ) );
		}

		// Create simplified hull
		if ( simplified ) {
			btShapeHull hull = new btShapeHull(shape);
			hull.buildHull(shape.getMargin());
			shape = new btConvexHullShape(hull);
		}
		
		return shape;
	}
}
