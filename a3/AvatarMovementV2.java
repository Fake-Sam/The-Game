package a3;
import net.java.games.input.Event;
import tage.FullObject;
import tage.GameObject;
import tage.input.InputManager;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Component.Identifier;
import tage.physics.PhysicsEngine;
import java.lang.Math;

import org.joml.*;

/**
 * A Movement Controller for the avatar
 * Uses the Physics from tage JBullet for control and collision detection
 */
public class AvatarMovementV2 {
    private FullObject fullObject;  //the object that will be controlled by the avatar
    private PhysicsEngine phEngine; //physics engine
    private InputManager im;
    private double lastFrameTime, curFrameTime, elapsedTime;    //time tracking between frames

    private boolean usingTerrain = false;   //using terrain or not
    private GameObject terrain;             //ONLY FOR TERRAIN HEIGHT
    private float terrainOffset;            //offset for terrain

    private ProtocolClient protClient;  //for server communication

    //------------settings------------------
    private float forcePower = 5;  //applies a force to the avatar in units/(second squared)   

    //---------------trackers--------------- variables used to track stats of the avatar 
    Vector3f linearSpeed;   //tracks the speed of the avatar

    //enums
    private enum DIRECTIONS {FORWARD, BACKWARD, LEFT, RIGHT}    //LEFT and RIGHT can be used for MOVING left/right OR TURNING left/right

    /** Give the FullObject, InputManager and PhysicsEngine */
    public AvatarMovementV2(FullObject fullOb, InputManager inputM, PhysicsEngine physicsEngine) {
        fullObject = fullOb;
        im = inputM;
        phEngine = physicsEngine;
        initTime();
        initInputs();

        linearSpeed = new Vector3f();
    }

    /** for if you want the avatar move along with the terrain. NOT REQUIRED FOR NORMAL MOVEMENT */
    public void setTerrain(GameObject terrainPlane, float offset) {
        terrain = terrainPlane;
        terrainOffset = offset;
    }

    /** update the avatars movement EVERY FRAME */
    public void updateMovement() {
        curFrameTime = System.currentTimeMillis();
        elapsedTime = (curFrameTime - lastFrameTime) / 1000;
        lastFrameTime = System.currentTimeMillis();
        phEngine.update((float)elapsedTime);

        //update y position if using terrain
        if(usingTerrain) {

        }
    }

    public Vector3f getSpeedVector() {
        linearSpeed.set(fullObject.getPhysicsObject().getLinearVelocity());
        return linearSpeed;
    }

    public float getSpeed() {
        float speed = 0.0f;

        speed = (linearSpeed.x() * linearSpeed.x()) + (linearSpeed.y() * linearSpeed.y()) + (linearSpeed.z() * linearSpeed.z());

        return (float)Math.sqrt(speed);
    }



    //============================private methods==========================================
    /** initialize time keeping variables */
    private void initTime() {
        curFrameTime = System.currentTimeMillis();
        lastFrameTime = System.currentTimeMillis();
        elapsedTime = 0;
    }

    /** initialize the inputs */
    private void initInputs() {
        MoveAction fwdAction = new MoveAction(DIRECTIONS.FORWARD);
        MoveAction bwdAction = new MoveAction(DIRECTIONS.BACKWARD);

        im.associateActionWithAllKeyboards(Identifier.Key.W, fwdAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Identifier.Key.S, bwdAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
    }

    //=============================private movement classes=================================
    /** applies a force to the avatar based on the player input */
    private class MoveAction extends AbstractInputAction {
        private DIRECTIONS direction;
        private Vector3f dirVector;

        /** give the direction to move */
        public MoveAction(DIRECTIONS dir) {
            direction = dir;
            dirVector = new Vector3f();
        }

        @Override
        public void performAction(float time, Event evt) {

            switch(direction) {
                case FORWARD:
                    dirVector = fullObject.getGameObject().getWorldForwardVector();
                    fullObject.applyForce(dirVector.mul((float)elapsedTime * forcePower));
                    break;
                case BACKWARD:
                    dirVector = fullObject.getGameObject().getWorldForwardVector();
                    fullObject.applyForce(dirVector.mul((float)elapsedTime * -forcePower));
                    break;
                case LEFT:
                    dirVector = fullObject.getGameObject().getWorldRightVector();
                    fullObject.applyForce(dirVector.mul((float)elapsedTime * -forcePower));
                    break;
                case RIGHT:
                    dirVector = fullObject.getGameObject().getWorldRightVector();
                    fullObject.applyForce(dirVector.mul((float)elapsedTime * forcePower));
                    break;
            }
        }

    }//end of MoveAction class

    private class TurnAction extends AbstractInputAction {
        private DIRECTIONS turnDirection;

        public TurnAction(DIRECTIONS turnDir) {
            turnDirection = turnDir;

            if(turnDirection == DIRECTIONS.FORWARD) {
                System.out.println("Error in AvatarMovementV2.java: attempted to set TurnAction to FORWARD");
            }
            else if(turnDirection == DIRECTIONS.BACKWARD) {
                System.out.println("Error in AvatarMovementV2.java: attempted to set TurnAction to BACKWARD");
            }
        }

        @Override
        public void performAction(float time, Event evt) {
            switch(turnDirection) {
                case FORWARD:
                    break;
                case BACKWARD:
                    break;
                case LEFT:
                    break;
                case RIGHT:
                    break;
            }
        }
        
    }

}
