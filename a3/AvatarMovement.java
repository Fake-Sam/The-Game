package a3;

import java.lang.Math;
import tage.*;
import tage.input.*;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Component.Identifier;
import net.java.games.input.Event;

import org.joml.*;

/**
 * a movement controller for the avatar
 * attach a gameObject to this to control it
 * currently does not accept custom keybinds
 * 
 * W/A to move forward/backward on keyboard
 * S/D to turn left/right on keyboard
 * 
 * A/B to move forward/backward on gamepad
 * left joystick X axis to turn left/right
 */
public class AvatarMovement {
    private InputManager im;    //input manager
    private GameObject av;      //avatar
    private double lastFrameTime, curFrameTime, elapsedTime;    //time tracking between frames
    private MomentumController momentumController;              //controls momentum of the avatar

    private Vector3f dir;   //adds this to momentum     allocated in init to avoid allocation during gameloop

    //-----settings------
    private float moveAcceleration = 20.0f;      //controls the rate at which the avatar can accelerate units / (second squared)
    private float maxMoveSpeed = 8; //controls the max movespeed of the avatar in units/second

    private boolean useRotationMomentum;    //determines if rotation is applied on input or with momentum
    private float rotationAcceleration = 5.0f; //controls the rate at whih the avatar can accelerate its rotation in degrees / (second squared)

    private float moveSpeed = 3.0f; //controls the speed at which the avatar moves in units/second
    private float turnSpeed = 90.0f; //controls the speed at which the avatar turns in degrees/second

    //-----actions-------
        //for non momentum movement
    private MoveActionLocal fwdAction;
    private MoveActionLocal bwdAction;
    private TurnActionKeyboard leftAction;
    private TurnActionKeyboard rightAction;
    private TurnActionJoystick turnActionJoystick;

        //for movement with momentum
    private MoveActionMomentumLocal fwdActionMomentum;
    private MoveActionMomentumLocal bwdActionMomentum;
    private TurnActionMomentumKeyboard leftActionMomentum;
    private TurnActionMomentumKeyboard rightActionMomentum;
    private PitchAction upPitchAction, downPitchAction;
    private TurnActionMomentumGamepad turnActionMomentumGamepad;
    private PitchActionMomentumGamepad pitchActionMomentumGamepad;

    //-------for networking--------
    private ProtocolClient protClient;

    /**
     * Constructor for if avatar movement is momentum based
     * @param inputManager takes the game's input manager
     * @param gameObject takes the gameObject that will be controlled
     * @param apply_rotation_momentum determines if the avatar will rotate on input or with momentum
     */
    public AvatarMovement(InputManager inputManager, GameObject gameObject, ProtocolClient p, boolean apply_rotation_momentum) {
        protClient = p;
        momentumController = new MomentumController(gameObject, p, true);
        dir = new Vector3f();
        im = inputManager;
        av = gameObject;

        lastFrameTime = System.currentTimeMillis();
        curFrameTime = lastFrameTime;
        elapsedTime = 0.0;

        useRotationMomentum = apply_rotation_momentum;
        
        initInputsMomentum();
    }

    /**
     * binds the inputs to actions on the gameObject
     * Not necessary to call this. The constructor calls this automatically
     */
    public void initInputs() {
        im.associateActionWithAllKeyboards(Identifier.Key.W, fwdAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Identifier.Key.S, bwdAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Identifier.Key.A, leftAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Identifier.Key.D, rightAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllGamepads(Identifier.Button._1, fwdAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN); //B
		im.associateActionWithAllGamepads(Identifier.Button._0, bwdAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN); //A
        im.associateActionWithAllGamepads(Identifier.Axis.X, turnActionJoystick, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);	//left joystick x axis
    }

    /**
     * binds the inputs to actions on the GameObject with momentum
     * Not necessary to call this. The constructor calls this automatically
     */
    public void initInputsMomentum() {
        fwdActionMomentum = new MoveActionMomentumLocal(MoveActionMomentumLocal.momentumDirection.FORWARD);
        bwdActionMomentum = new MoveActionMomentumLocal(MoveActionMomentumLocal.momentumDirection.BACKWARD);
        leftActionMomentum = new TurnActionMomentumKeyboard(TurnActionMomentumKeyboard.momentumMODE.LEFT);
        rightActionMomentum = new TurnActionMomentumKeyboard(TurnActionMomentumKeyboard.momentumMODE.RIGHT);

        turnActionMomentumGamepad = new TurnActionMomentumGamepad();
        pitchActionMomentumGamepad = new PitchActionMomentumGamepad(true);

        im.associateActionWithAllKeyboards(Identifier.Key.W, fwdActionMomentum, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Identifier.Key.S, bwdActionMomentum, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Identifier.Key.A, leftActionMomentum, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Identifier.Key.D, rightActionMomentum, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllGamepads(Identifier.Axis.X, turnActionMomentumGamepad, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);  //left jotstick x axis
        im.associateActionWithAllGamepads(Identifier.Axis.Y, pitchActionMomentumGamepad, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN); //left joystick y axis
        im.associateActionWithAllGamepads(Identifier.Button._1, fwdActionMomentum, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);   //B button on gamepad
        im.associateActionWithAllGamepads(Identifier.Button._0, bwdActionMomentum, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);   //A button on gamepad
    }

    /**
     * call this every frame
     * updates elapsed time to make movement work
     * ONLY USE THIS IF NOT USING MOMENTUM
     */
    public void updateNoMomentum() {
        curFrameTime = System.currentTimeMillis();
        elapsedTime = (curFrameTime - lastFrameTime) / 1000;
        lastFrameTime = System.currentTimeMillis();
    }

    /**
     * call this every frame
     * updates elapsed time to make momentum work
     * ONLY USE THIS IF USING MOMENTUM
     */
    public void updateMomentum() {
        curFrameTime = System.currentTimeMillis();
        elapsedTime = (curFrameTime - lastFrameTime) / 1000;
        lastFrameTime = System.currentTimeMillis();
        momentumController.updateMomentum(elapsedTime);
    }


    //================================getters, setters, and other methods==================================================
    public float getSpeedMomentum() {
        return momentumController.getSpeed();
    }

    public Vector3f getMomentumVector() {
        return momentumController.getMomentumVector();
    }

    /**
     * resets y momentum
     */
    public void resetY() {
        momentumController.resetYMomentum();
    }



    //====================================private classes for moving the object with momentum===================================
    private class MoveActionMomentumLocal extends AbstractInputAction {
        private enum momentumDirection {FORWARD, BACKWARD, LEFT, RIGHT}

        private momentumDirection direction;

        public MoveActionMomentumLocal(momentumDirection direction) {
            this.direction = direction;
        }

        @Override
        public void performAction(float time, Event e) {
            //get the direction the avatar is facing so you can apply the momentum in that direction
            //do something different for if the avatar is drifting using a drift angle or something
            dir.set(av.getWorldForwardVector());
            dir.mul(moveAcceleration * (float)elapsedTime);

            switch(direction) {
                case FORWARD:
                    //check if current momentum is more than the variable "maxMoveSpeed"    if it is then dont give the avatar any more momentum from moving
                        //NOT IMPLEMENTED YET
                            //simply get the magnitude of the momentum Vector3f in the momentum controller and check if that is greater than "maxMoveSpeed"
                    momentumController.addMomentumMaxSpeed(dir, maxMoveSpeed);
                    break;
                case BACKWARD:
                    //check if current momentum is more than the variable "maxMoveSpeed"    if it is then dont give the avatar any more momentum from moving
                        //NOT IMPLEMENTED YET
                            //simply get the magnitude of the momentum Vector3f in the momentum controller and check if that is greater than "maxMoveSpeed"
                    dir.mul(-1.0f); //reverse the momentum
                    momentumController.addMomentumMaxSpeed(dir, maxMoveSpeed);
                    break;
                case LEFT:  //not implemented  MODIFY DIR AT BEGINNING OF THIS METHOD   dir needs world left vector for this
                    break;
                case RIGHT: //not implemented  MODIFY DIR AT BEGINNING OF THIS METHOD   dir needs world right vector for this
                    
                    break;
            }
        }
    }

    /**
     * turns the avatar left or right according to world axis (global yaw)
     */
    private class TurnActionMomentumKeyboard extends AbstractInputAction {
        momentumMODE mode;
        enum momentumMODE {LEFT, RIGHT}
    
        public TurnActionMomentumKeyboard(momentumMODE m) {
            mode = m;
        }
    
        @Override
        public void performAction(float time, Event e) { 
            //determine which type of rotation is being used
            if(!useRotationMomentum) {
                float avYRotation = 0.0f;
    
                if(mode == momentumMODE.LEFT) {
                    avYRotation = (float) (elapsedTime * Math.toRadians(turnSpeed));
                    //momentumController.yawMomentumWorld((float)elapsedTime * turnSpeed);      //use this to yaw momentum along with the avatar
                }
                else if(mode == momentumMODE.RIGHT) {
                    avYRotation = (float) (elapsedTime * Math.toRadians(turnSpeed) * -1.0f);
                    //momentumController.yawMomentumWorld((float)elapsedTime * turnSpeed * -1.0f);      //use this to yaw momentum along with the avatar
                }
                protClient.sendYawMessage(avYRotation);
                av.yawWorld(avYRotation);

                protClient.sendYawMessage(avYRotation);
                av.yawWorld(avYRotation);
                momentumController.setMomentum(getMomentumVector().rotateY(avYRotation));
            }
            else {
                if(mode == momentumMODE.LEFT) {
                    momentumController.addRotation((float)elapsedTime*rotationAcceleration, true, false, false);
                }
                else if(mode == momentumMODE.RIGHT) {
                    momentumController.addRotation((float)elapsedTime*-rotationAcceleration, true, false, false);
                }
            }
            
        }
    }

    /**
     * turns the avatar left or right according to world axis with a gamepad
     */
    private class TurnActionMomentumGamepad extends AbstractInputAction {

        public TurnActionMomentumGamepad() {}

        @Override
        public void performAction(float time, Event e) {
            av.yawWorld((float) (elapsedTime * Math.toRadians(turnSpeed) * e.getValue() * -1.0f));

            //option to rotate momentum with avatar (bhop application)
            //momentumController.yawMomentumWorld(avYRotation);
        }
    }

    /** 
     * pitches the avatar with a keyboard
    */
    public class PitchAction extends AbstractInputAction {
        enum MODE {UP, DOWN}
        public PitchAction(MODE m) {
            if (m.equals(MODE.DOWN)) {
                this.setSpeed(this.getSpeed() * -1);
            }
        }
        @Override
        public void performAction(float time, Event evt) {
            float speed = getSpeed();
            if (evt.getComponent().isAnalog()) {
                speed *= Math.abs(evt.getValue());
                if (evt.getValue() > 0) {
                    speed *= -1;
                }
            }
            float angle = time * speed;
            av.pitch(angle);
            protClient.sendPitchMessage(angle);
        }
    }

    /**
     * pitches the avatar with a gamepad
     */
    public class PitchActionMomentumGamepad extends AbstractInputAction{
        private boolean invertControls; //whether to invert controls or not

        /**
         * Constructor
         * @param invert_controls true if want pitch inverted
         */
        public PitchActionMomentumGamepad(boolean invert_controls) {
            invertControls = invert_controls;
        }

        /**
         * toggles inverting controls
         */
        public void toggleInversion() {
            invertControls = !invertControls;
        }

        /**sets invertControls */
        public void setInvertControls(boolean i) {
            invertControls = i;
        }

        @Override
        public void performAction(float time, Event e) {
            int inversion = 1;
            if(invertControls) {inversion = -1;}

            //has a deadzone of 0.1 to prevent unwanted drift
            if(e.getValue() > 0.1 || e.getValue() < -0.1) {
                av.pitchLocal((float) (elapsedTime * Math.toRadians(turnSpeed) * e.getValue() * inversion));
            }
            
        }
    }


    //==========================private classes for moving the object=============================================

    /**
     * moves the avatar forward, backward, left, or right relative to the avatar
     */
    private class MoveActionLocal extends AbstractInputAction {
        private enum Direction {FORWARD, BACKWARD, LEFT, RIGHT}

        private Vector3f fwd, loc, newLocation;
        private Direction direction;

        public MoveActionLocal(Direction direction) {
            this.direction = direction;
        }

        @Override
        public void performAction(float time, Event e) {
            fwd = av.getWorldForwardVector(); 	//gets the vector of the direction the avatar is looking forward in the world space
            loc = av.getWorldLocation();        //gets the location of the dolphin in world space

            switch(direction) {
                case FORWARD:
                    newLocation = loc.add(fwd.mul((float) (elapsedTime * moveSpeed)));
                    break;
                case BACKWARD:
                    newLocation = loc.add(fwd.mul((float) (elapsedTime * moveSpeed * -1.0)));
                    break;
                case LEFT:  //not yet implemented
                    newLocation = loc.add(fwd.mul((float) (elapsedTime * moveSpeed)));  //currently makes the avatar move forward
                    break;
                case RIGHT: //not yet implemented
                    newLocation = loc.add(fwd.mul((float) (elapsedTime * moveSpeed * -1.0)));   //currently makes the avatar move backward
                    break;
            }
            

            av.setLocalLocation(newLocation);
        } 
    } //end of MoveActionLocal class

    /**
     * turns the avatar left or right according to world axis (global yaw)
     */
    private class TurnActionKeyboard extends AbstractInputAction{
        MODE mode;
        enum MODE {LEFT, RIGHT}
    
        public TurnActionKeyboard(MODE m) {
            mode = m;
        }
    
        @Override
        public void performAction(float time, Event e) { 
            float avYRotation = 0.0f;
    
            if(mode == MODE.LEFT) {
                avYRotation = (float) (elapsedTime * Math.toRadians(turnSpeed));
            }
            else if(mode == MODE.RIGHT) {
                avYRotation = (float) (elapsedTime * Math.toRadians(turnSpeed) * -1.0f);
            }

            av.yawWorld(avYRotation);
        }
    }

    

    /**
     * turns the avatar left or right according to world axis (global yaw)
     */
    private class TurnActionJoystick extends AbstractInputAction { 

        public TurnActionJoystick() {}
    
        @Override
        public void performAction(float time, Event e) { 
            float avYRotation = 0.0f;

            if(e.getValue() < -0.2) {
                //joystick held left
                avYRotation = (float) (elapsedTime * Math.toRadians(turnSpeed));
            }
            else if(e.getValue() > 0.2) {
                avYRotation = (float) (elapsedTime * Math.toRadians(turnSpeed) * -1.0f);
            } 
    
            av.yawWorld(avYRotation);
        }
    }

}
