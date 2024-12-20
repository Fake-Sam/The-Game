package a3;

import net.java.games.input.Event;
import tage.FullObject;
import tage.GameObject;
import tage.input.InputManager;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Component.Identifier;
import java.lang.Math;

import org.joml.*;

public class AvatarMovementV3 {
  private FullObject fullObject;  //the object that will be controlled by the avatar
  private InputManager im;
  private double lastFrameTime, curFrameTime, elapsedTime;    //time tracking between frames
  private MomentumController momentumController;              //controls momentum of the avatar

  private GameObject terrain;             //ONLY FOR TERRAIN HEIGHT
  private float terrainOffset;            //offset for terrain

  private ProtocolClient protClient;  //for server communication

  //allocated variables
  Vector3f dir = new Vector3f();

  //enums
  private enum DIRECTIONS {FORWARD, BACKWARD, LEFT, RIGHT}    //LEFT and RIGHT can be used for MOVING left/right OR TURNING left/right

  //settings and stuff
  boolean useTerrain = false;
  boolean useRotationMomentum;

  float moveAcceleration = 10.0f;  //how fast the avatar accelerates in units / (second squared)
  float maxMoveSpeed = 10.0f;

  float turnSpeed = 90; //how fast the avatar turns in degrees/second

  float rotationAcceleration = 10.0f; //how fast the avatar accelerates in turning in degrees / (second squared)

  private float deadzone = 0.1f;  //threshold for gamepad axis values to be accepted

  private float bumpPower = 25; //power of the bump between the avatar and another object in units/second


  //=====================================================CONSTRUCTORS AND INITIALIZERS================================================
  public AvatarMovementV3(FullObject fullOb, InputManager inputM, ProtocolClient p, boolean apply_rotation_momentum) {
    fullObject = fullOb;
    im = inputM;
    protClient = p;

    lastFrameTime = System.currentTimeMillis();
    curFrameTime = lastFrameTime;
    elapsedTime = 0.0;

    useRotationMomentum = apply_rotation_momentum;
    momentumController = new MomentumController(fullObject.getGameObject(), p, useRotationMomentum);

    initInputs();
  }

  /** for if you want the avatar move along with the terrain. NOT REQUIRED FOR NORMAL MOVEMENT */
  public void setTerrain(GameObject terrainPlane, float offset) {
    useTerrain = true;
    terrain = terrainPlane;
    terrainOffset = offset;
  }

  //===========================================================================UPDATE METHOD======================================================
  /** update the avatars movement EVERY FRAME */
  public void updateMovement() {
    curFrameTime = System.currentTimeMillis();
    elapsedTime = (curFrameTime - lastFrameTime) / 1000;
    lastFrameTime = System.currentTimeMillis();

    momentumController.updateMomentum(elapsedTime);

    //update y position if using terrain
    if(useTerrain) {
      adjustToTerrain();
    }

    //update the physics object to the gameobject's location and orientation
    fullObject.updatePhysicsObject();

    //check for collision

  }

  //==============================================OTHER METHODS======================================
  /**
   * registers the bump between the avatar and another object
   * @param avLocation  location of the avatar
   * @param otherLocation location of the object the avatar bumped into
   */
  public void bump(Vector3f avLocation, Vector3f otherLocation) {
    Vector3f bumpDir = new Vector3f();
    bumpDir.set(avLocation);
    bumpDir.sub(otherLocation);
    bumpDir.normalize();
    bumpDir.mul(bumpPower);
    momentumController.bump(bumpDir);
  }


  //==========================================================GETTERS AND SETTERS====================================================================
  public float getSpeedMomentum() {
    return momentumController.getSpeed();
  }
  public Vector3f getMomentumVector() {
    return momentumController.getMomentumVector();
  }
  public float getmoveAcceleration() {return moveAcceleration;}
  public float getMaxMoveSpeed() {return maxMoveSpeed;}
  public float getTurnSpeed() {return turnSpeed;}
  public float getRotationAcceleration() {return rotationAcceleration;}

  public void setmoveAcceleration(float accel) {moveAcceleration = accel;}
  public void setMaxMoveSpeed(float max) {maxMoveSpeed = max;}
  public void setTurnSpeed(float speed) {turnSpeed = speed;}
  public void setRotationAcceleration(float accel) {rotationAcceleration = accel;}

  /**
   * resets y momentum
   */
  public void resetY() {
    momentumController.resetYMomentumNegative();
  }

  public void resetMomentum() {
    momentumController.resetAllMomentum();
  }

  public void disableTerrain() {useTerrain = false;}
  public void enableTerrain() {
    if(terrain == null) {
      System.out.println("ERROR in AvatarMovementV3.java: Attempted to enable terrain without giving a GameObject for terrain. Terrain will stay disabled.");
      return;
    }
    useTerrain = true;
  }


  //========================================================PRIVATE METHODS===========================================================================
  private void initInputs() {
    MoveAction fwdAction = new MoveAction(DIRECTIONS.FORWARD);
    MoveAction bwdAction = new MoveAction(DIRECTIONS.BACKWARD);
    TurnAction leftAction = new TurnAction(DIRECTIONS.LEFT);
    TurnAction rightAction = new TurnAction(DIRECTIONS.RIGHT);
    TurnActionGamepad turnAction = new TurnActionGamepad();

    im.associateActionWithAllKeyboards(Identifier.Key.W, fwdAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
    im.associateActionWithAllKeyboards(Identifier.Key.S, bwdAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
    im.associateActionWithAllKeyboards(Identifier.Key.A, leftAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
    im.associateActionWithAllKeyboards(Identifier.Key.D, rightAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

    im.associateActionWithAllGamepads(Identifier.Axis.X, turnAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
    im.associateActionWithAllGamepads(Identifier.Button._1, fwdAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
    im.associateActionWithAllGamepads(Identifier.Button._0, bwdAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
  }

  /** Used when terrain is enabled. Adjusts avatar's height to that of the terrain if it is under the terrain */
  private void adjustToTerrain() {
    //check if avatar is under the terrain
		Vector3f loc = fullObject.getGameObject().getLocalLocation();
		float terrHeight = terrain.getHeight(loc.x(), loc.z()) + terrainOffset;
		if(terrHeight > loc.y() - 0.2015f) {
			resetY();
			loc.y = terrHeight + 0.2015f;
			fullObject.getGameObject().setLocalLocation(loc);
			protClient.sendMoveMessage(loc);
		}
  }


  //================================================movement classes===============================================================
  /** applies a force to the avatar based on the player input */
  private class MoveAction extends AbstractInputAction {
    private DIRECTIONS direction;

    /** give the direction to move */
    public MoveAction(DIRECTIONS dir) {
      direction = dir;
    }

    @Override
    public void performAction(float time, Event evt) {

      switch(direction) {
        case FORWARD:
          dir.set(fullObject.getGameObject().getWorldForwardVector());
          dir.mul(moveAcceleration * (float)elapsedTime);
          momentumController.addMomentumMaxSpeed(dir, maxMoveSpeed);
          break;
        case BACKWARD:
          dir.set(fullObject.getGameObject().getWorldForwardVector());
          dir.mul(-moveAcceleration * (float)elapsedTime);
          momentumController.addMomentumMaxSpeed(dir, maxMoveSpeed);
          break;
        case LEFT:
            
          break;
        case RIGHT:
            
          break;
      }
    }

  }//end of MoveAction class

  /** turns the avatar based on keyboard input
   * can choose between static rotation and momentum rotation
   */
  private class TurnAction extends AbstractInputAction {
    private DIRECTIONS turnDirection;

    public TurnAction(DIRECTIONS turnDir) {
      turnDirection = turnDir;

      if(turnDirection == DIRECTIONS.FORWARD) {
        System.out.println("Error in AvatarMovementV3.java: attempted to set TurnAction to FORWARD");
      }
      else if(turnDirection == DIRECTIONS.BACKWARD) {
        System.out.println("Error in AvatarMovementV3.java: attempted to set TurnAction to BACKWARD");
      }
    }

    @Override
    public void performAction(float time, Event evt) {
      if(useRotationMomentum) { //using rotation momentum
        switch(turnDirection) {
          case FORWARD:
            break;
          case BACKWARD:
            break;
          case LEFT:
            momentumController.addRotation((float)elapsedTime*rotationAcceleration, true, false, false);
            break;
          case RIGHT:
            momentumController.addRotation((float)elapsedTime*-rotationAcceleration, true, false, false);
            break;
        }
      }
      else {  //not using rotation momentum
        float avYRotation = 0.0f;
      
          if(turnDirection == DIRECTIONS.LEFT) {
              avYRotation = (float) (elapsedTime * Math.toRadians(turnSpeed));
          }
          else if(turnDirection == DIRECTIONS.RIGHT) {
              avYRotation = (float) (elapsedTime * Math.toRadians(turnSpeed) * -1.0f);
          }

          fullObject.getGameObject().yawWorld(avYRotation);
          momentumController.yawMomentumWorld(avYRotation);
          protClient.sendYawMessage(avYRotation);
      }
    }
    
  }

  /** turns the avatar based on gamepad input */
  private class TurnActionGamepad extends AbstractInputAction{
    public TurnActionGamepad() {}

    @Override
    public void performAction(float time, Event evt) {
      if(Math.abs(evt.getValue()) >= deadzone) {
        float avYRotation = (float) (elapsedTime * Math.toRadians(turnSpeed) * -evt.getValue());
        fullObject.getGameObject().yawWorld(avYRotation);
        momentumController.yawMomentumWorld(avYRotation);
        protClient.sendYawMessage(avYRotation);
      }
    }

    
  }



}
