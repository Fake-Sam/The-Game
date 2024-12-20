package tage;

import tage.physics.PhysicsObject;
import org.joml.*;
import java.lang.Math;

/**
 * Holds both a GameObject and its Jbullet hitbox
 */
public class FullObject {
    private Engine engine;          //engine of the game
    private GameObject gObj;        //the game object
    private PhysicsObject phObj;    //the physics object
    private String name = "No name";            //name of the FullObject

    private Matrix4f mat, mat2, mat3, mat4;   //matrices used for updating GameObject location/orientation to PhysicsObject
    private AxisAngle4f aa1, aa2;             //matrix used for updating GameOject to location/orientation to PhysicsObject

    private Matrix4f localTranslation, localRotation, translation, offsetTranslation;
    private Vector3f offset;

    private Vector3f gameOffset = new Vector3f(); //offset for the GameObject to get it to line up properly with the PhysicsObject

    //useful variables for only if the class is for an npc or avatar
    private boolean isColliding = false;    //if the PhysicsObject is colliding with another
    private int collidingWith = -1;         //UID of the PhysicsObject this is colliding with
    private int collisionType = -1;         //type of PhysicsObject this is colliding with  0=avatar  1=obstacle  2=npc
    private double collisionTime = -1;      //what time the last collision happened
    private double processTime = -1;        //what time the last process occured    (currently used for preventing a collision's effects from occuring more than once within a certain time frame)

    /** give an existing GameObject and PhysicsObject */
    public FullObject(Engine engi, GameObject gameObj, PhysicsObject physicsObj) {
        engine = engi;
        gObj = gameObj;
        phObj = physicsObj;

        gObj.setPhysicsObject(phObj);
        initMatrices();
    }

    /**Give the ObjectShape and the texture for the GameObject. 
     * Give the mass, transform, radius, height, and bounciness of the physics capsule
     */
    public FullObject(  Engine engi, ObjShape obShape, TextureImage tx, 
                        float mass, double[] tempTransform, float radius, float height, float bounciness) {
        engine = engi;
        gObj = new GameObject(GameObject.root(), obShape, tx);

        phObj = engine.getSceneGraph().addPhysicsCapsuleX(mass, tempTransform, radius, height);
        phObj.setBounciness(bounciness);

        gObj.setPhysicsObject(phObj);
        initMatrices();
    }

    /** Change parent of GameObject */
    public void setParent(GameObject parent) {
        gObj.setParent(parent);
    }

    public void setName(String s) {
        name = s;
    }

    public String getName() {return name;}

    /** Get the UID of the physics object */
    public int getUID() {
        return phObj.getUID();
    }

    /** offsets the GameObject to align with the PhysicsObject */
    public void setGameObjectOffset(Vector3f offset) {
        gameOffset = offset;
    }

    public boolean getIsColliding() {return isColliding;}

    /** returns the uid of the object this collided with */
    public int getCollidingWith()   {return collidingWith;}

    /** returns the type of object this collided with 
     * <p> 
     * 0=avatar  1=obstacle  2=npc */
    public int getCollisionType()   {return collisionType;}

    /** returns the GameObject */
    public GameObject getGameObject() {
        return gObj;
    }

    /** returns the PhysicsObject */
    public PhysicsObject getPhysicsObject() {
        return phObj;
    }

    /** returns the time of the latest collision */
    public double getCollisionTime() {
        return collisionTime;
    }

    /** returns processTime */
    public double getProcessTime() {
        return processTime;
    }

    /** updates processTime to time at which this is called */
    public void updateProcessTime() {
        processTime = System.currentTimeMillis();
    }

    /** applies a force to the PhysicsObject at its center */
    public void applyForce(Vector3f force) {
        phObj.applyForce(force.x(), force.y(), force.z(), 0, 0, 0);
    }

    /** rotates the PhysicsObject NOT COMPLETE*/
    public void rotateObject(float angleDegrees) {
        mat4.set(toFloatArray(gObj.getPhysicsObject().getTransform()));
        aa2.angle = (float)Math.toRadians(angleDegrees);
    }

    /** updates the GameObjects position and orientation to that of PhysicsObject */
    public void updateGameObject() {
        mat2.identity();    //will contain the local translation
        mat3.identity();    //will contain the local rotation
        // set translation
        mat.set(toFloatArray(gObj.getPhysicsObject().getTransform()));  //contains the full transform of the PhysicsObject
        mat2.set(3,0,mat.m30());
        mat2.set(3,1,mat.m31());
        mat2.set(3,2,mat.m32());

        // set rotation
        mat.getRotation(aa1);   //extracts the rotation from the transform
        mat3.rotation(aa1);
        gObj.setLocalRotation(mat3);

        //offsetTranslation.translation(gameOffset);
        //offsetTranslation.rotate(aa1);
        //mat2.add(offsetTranslation);
        gObj.setLocalTranslation(mat2);
    }

    /** updates PhysicsObjects position and orientation to that of GameObject */
    public void updatePhysicsObject() {
        localTranslation.set(gObj.getLocalTranslation());
        localRotation.set(gObj.getLocalRotation());

        translation.set(localTranslation);
        translation.mul(localRotation);

        phObj.setTransform(toDoubleArray(translation));
    }

    /** alternate way of updating PhysicsObjects position and orientation to that of GameObject <p>
     * uses the GameObjects World Matrices rather than Local Matrices
     */
    public void updatePhysicsObjectWorld() {
        localTranslation.set(gObj.getWorldTranslation());
        localRotation.set(gObj.getWorldRotation());

        translation.set(localTranslation);
        translation.mul(localRotation);

        phObj.setTransform(toDoubleArray(translation));
    }

    /**
     * FOR NPC AND AVATAR USE ONLY <p>
     * updates collision information when a collision occurs with this object
     * @param collidedUID UID of the object this object collided with
     * @param collidedType Type of object this object collided with     0=avatar  1=obstacle  2=npc
     */
    public void collisionOccured(int collidedUID, int collidedType) {
        isColliding = true;
        collidingWith = collidedUID;
        collisionType = collidedType;
        collisionTime = System.currentTimeMillis();
    }

    /** checks if a collision occured with this object
     * if a collision occured, return true
     * flag for collision is set to false after checking to reset for next frame
     */
    public boolean checkCollision() {
        if(isColliding) {
            isColliding = false;
            return true;
        }
        return false;
    }

    //=========================================private methods=====================================
    /** initializes matrices that are used */
    private void initMatrices() {
        mat = new Matrix4f();
        mat2 = new Matrix4f().identity();
        mat3 = new Matrix4f().identity();
        aa1 = new AxisAngle4f();
        aa2 = new AxisAngle4f(0, 0, 1, 0);
        mat4 = new Matrix4f();

        localTranslation = new Matrix4f();
        localRotation = new Matrix4f();
        translation = new Matrix4f();
        offsetTranslation = new Matrix4f();
        offset = new Vector3f();
    }

    /** converts a double array to float array */
    private float[] toFloatArray(double[] arr) { 
		if (arr == null) return null;
		int n = arr.length;
		float[] ret = new float[n];
		for (int i = 0; i < n; i++) { 
			ret[i] = (float)arr[i];
		}
		return ret;
	}

    /** converts a Matrix4f to double array */
    private double[] toDoubleArray(Matrix4f m) {
		double[] a = new double[16];
		float[] b = new float[16];

		m.get(b);

		for(int i = 0; i < 16; i++) {
			a[i] = (double)b[i];
		}
		

		return a;
	}
}