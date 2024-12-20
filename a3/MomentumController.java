package a3;

import java.lang.Math;

import tage.*;

import org.joml.*;

/**
 * Handles momentum of a GameObject
 * after attaching the GameObject to the controller using the constructor, simply call updateMomentum() to apply momentum to the GameObject. Recommended to call every frame
 */
public class MomentumController {
    private GameObject target;         //the object that will have momentum applied to it
    private Vector3f momentum;      //direction and magnitude of the momentum being applied to target
    private Vector3f bumpMomentum = new Vector3f(0,0,0);  //direction and magnitude of bump momentum being applied to target
    private Vector3f rotationMomentum;  // (yaw, pitch, roll) stored like this

    private Vector3f momentumApplied = new Vector3f();   //copies in the value of momentum and then this is applied to targets location after modifications like delta time
    private Vector3f potentialMomentum = new Vector3f();    //potential new momentum

    //settings
    private boolean useRotationMomentum;    //boolean if wanting to use rotation momentum

    private float zero_threshold;   //any absolute value of a number less than this will be set to 0
    private boolean applyZero = true;      //boolean to decide if momentum should attempt to be zero'd out
    
    private float decayAllFactor = 0.05f;    //the factor at which additional momentum decreases for all components
    private float decayAllAmount = 0.8f;    //lose a base amount of speed in units/second
    private boolean applyDecayAll = true;   //boolean to decide if decayAllMomentum() should be called in updateMomentum() method

    private float decayHorizontalX = 0.2f;  //lose a base amount of speed in units/second
    private float decayHorizontalZ = 0.2f;
    private boolean applyDecayHorizontal = false;    //boolean to decide if decayHorizontalMomentum() should be called

    private float decayHorizontalBump = 4.0f;

    private float decayVerticalAmount = 0.2f;  //lose a base amount of speed in units/second
    private boolean applyDecayVertical = false;

    private float gravityStrength = 9.8f;   //strength of gravity applie in units/(second squared)
    private boolean applyGravity = true;    //boolean to decide if applyGravity() is called

    private boolean applyYawDecay = true;   //boolean to decide if yaw momentum should decay
    private float yawDecayAmount = 3.0f;    //lose a base amount of 3 degrees/second

    private boolean applyPitchDecay = true; //boolean to decide if pitch momentum should decay
    private float pitchDecayAmount = 3.0f;  //lose a base amount of 3 degrees/second

    private boolean applyRollDecay = true; //boolean to decide if roll momentum should decay
    private float rollDecayAmount = 3.0f;  //lose a base amount of 3 degrees/second

    private ProtocolClient protClient;  //for sending move packets along the network

    //===================================Constructors and update method===================================================

    /**
     * Constructor
     * @param target_GameObject the target GameObject that will have momentum applied to it
     */
    public MomentumController(GameObject target_GameObject, ProtocolClient p, boolean want_momentum_rotation) {
        protClient = p;
        target = target_GameObject;

        momentum = new Vector3f();
        momentum.set(0,0,0);    //has 0 momentum in XYZ directions

        zero_threshold = 0.05f;

        rotationMomentum = new Vector3f();
        useRotationMomentum = want_momentum_rotation;
    }

    /**
     * Constructor
     * @param target_GameObject the target GameObject that will have momentum applied to it
     * @param threshold any absolute value of a number less than this will be set to 0
     */
    public MomentumController(GameObject target_GameObject, float threshold, boolean want_momentum_rotation) {
        target = target_GameObject;

        momentum = new Vector3f();
        bumpMomentum = new Vector3f();
        momentum.set(0,0,0);    //has 0 momentum in XYZ directions
        bumpMomentum.set(0,0,0);

        zero_threshold = threshold;

        rotationMomentum = new Vector3f();
        useRotationMomentum = want_momentum_rotation;
    }

    /**
     * updates the GameObject's position based on its momentum
     * @param deltaTime time elapsed between current frame and previous frame
     */
    public void updateMomentum(double deltaTime) {
        Vector3f targetLoc = target.getLocalLocation(); //target's location

        //process decayAllMomentum
        if(applyDecayAll) {decayAllMomentum(deltaTime);}

        //process decayHorizontalMomentum
        if(applyDecayHorizontal) {decayHorizontalMomentum(deltaTime);}

        //process decayVerticalMomentum
        if(applyDecayVertical) {decayVerticalMomentum(deltaTime);}

        //process gravity
        if(applyGravity) {applyGravity(deltaTime);}

        //process decay of rotations
        if(useRotationMomentum) {
            if(applyYawDecay) {decayYaw(deltaTime);}
            if(applyPitchDecay) {decayPitch(deltaTime);}
            if(applyRollDecay) {decayRoll(deltaTime);}
            
        }
        
        //process bump momentum decay
        decayAllBumpMomentum(deltaTime);

        //process anything else in the future

        //apply momentum
        momentumApplied.set(momentum);
        targetLoc.add(momentumApplied.mul((float)deltaTime));  //adds momentum while taking into account deltaTime for uniform movement no matter the framerate
        momentumApplied.set(bumpMomentum);
        targetLoc.add(momentumApplied.mul((float)deltaTime));   //adds bump momentum

        target.setLocalLocation(targetLoc); //apply the new location
        protClient.sendMoveMessage(targetLoc);

        //apply rotation
        if(useRotationMomentum) {
            target.yawLocal((float)Math.toRadians(rotationMomentum.x()));
            target.pitchLocal((float)Math.toRadians(rotationMomentum.y()));
            target.rollLocal((float)Math.toRadians(rotationMomentum.z()));
        }
    }

    //==========================================methods that modify momentum=========================================

    /**
     * adds momentum to GameObject's current momentum
     * @param add_momentum add this to current momentum
     */
    public void addMomentum(Vector3f add_momentum) {
        momentum.add(add_momentum);
    }

    /** adds momentum to GameObject's current bump momentum */
    public void addMomentumBump(Vector3f add_momentum) {
        bumpMomentum.add(add_momentum);
    }

    /**
     * adds momentum to pitch yaw or roll
     * @param amount amount added in degrees
     * @param yaw   if want added to yaw
     * @param pitch if want added to pitch
     * @param roll  if want wadded to roll
     */
    public void addRotation(float amount, boolean yaw, boolean pitch, boolean roll) {
        if(yaw) {rotationMomentum.x += amount;}
        if(pitch) {rotationMomentum.y += amount;}
        if(roll) {rotationMomentum.z += amount;}
    }

    /**
     * adds momentum to GameObject's current momentum but will not go beyond maxSpeed
     * @param add_momentum add this to current momentum     note that this should already be multiplied by a deltaTime
     * @param maxSpeed momentum will not go above this speed
     */
    public void addMomentumMaxSpeed(Vector3f add_momentum, float maxSpeed) {
        potentialMomentum.set(momentum);
        potentialMomentum.add(add_momentum);    //now contains what momentum could potentially be if added

        //check if potential speed is more than max speed
        double potentialSpeed = Math.sqrt((Math.pow(potentialMomentum.x(),2) + Math.pow(potentialMomentum.y(),2) + Math.pow(potentialMomentum.z(),2)));
        if(potentialSpeed > maxSpeed) {
            //potential speed higher than max speed
            //cut it down
            double ratio = potentialSpeed/maxSpeed;
            potentialMomentum.x /= ratio;
            potentialMomentum.y /= ratio;
            potentialMomentum.z /= ratio;
        }

        momentum.set(potentialMomentum);

    } 

    /**
     * rotates GameObject's momentum around the world Y axis
     * @param radians rotates by this many radians
     */
    public void yawMomentumWorld(float radians) {
        momentum.rotateAxis(radians, 0, 1, 0);
    }

    /**
     * reduces momentum in all directions by the variable decayAllAmount
     * applications: air resistance when GameObject is not touching ground
     * @param deltaTime time elapsed between current and previous frame
     */
    public void decayAllMomentum(double deltaTime) {
        float amount = decayAllAmount;  //base amount to take from momentum

        //increase amount taken by a factor that increases when speed is higher     NOT IMPLEMENTED YET <===================

        //factor in delta time
        amount *= deltaTime;

        absSubMomentum(amount, true, true, true);
        //if(applyZero) {zeroThresholdAll();}
    }

    /** 
     * reduces momentum in the XZ directions by the variables decayHorizontalX and decayHorizontalZ
     * applies additional reductions by a factor dependent on speed
     * applications: friction   
     * @param deltaTime time elapsed between current and previous frame
     */
    public void decayHorizontalMomentum(double deltaTime) {
        float decayAmountX = decayHorizontalX;  //base amount
        float decayAmountZ = decayHorizontalZ;  //base amount

        //increase amount being taken from momentum based on speed      NOT IMPLEMENTED YET <================

        //factor in delta time
        decayAmountX *= deltaTime;
        decayAmountZ *= deltaTime;

        absSubMomentum(decayAmountX, true, false, false);
        absSubMomentum(decayAmountZ, false, false, true);
        //if(applyZero) {zeroThresholdX();zeroThresholdY();}
    }

    /** 
     * reduces bump momentum in the XZ directions by the variables decayHorizontalX and decayHorizontalZ
     * applies additional reductions by a factor dependent on speed (never completed)
     * applications: friction   
     * @param deltaTime time elapsed between current and previous frame
     */
    public void decayAllBumpMomentum(double deltaTime) {
        float decayAmountX = decayHorizontalBump;  //base amount
        float decayAmountZ = decayHorizontalBump;  //base amount
        float decayAmountY = decayHorizontalBump;  //base amount

        //increase amount being taken from momentum based on speed      NOT IMPLEMENTED YET <================

        //factor in delta time
        decayAmountX *= deltaTime;
        decayAmountZ *= deltaTime;
        decayAmountY *= deltaTime;

        absSubBumpMomentum(decayAmountX, true, false, false);
        absSubBumpMomentum(decayAmountZ, false, false, true);
        absSubBumpMomentum(decayAmountY, false, true, false);
        //if(applyZero) {zeroThresholdX();zeroThresholdY();}
    }

    /**
     * reduces momentum in the Y direction by the variable 
     * applications: unknown
     * THIS IS NOT TO BE USED FOR GRAVITY OR TERMINAL VELOCITY
     * @param deltaTime time elapsed between current and previous frame
     */
    public void decayVerticalMomentum(double deltaTime) {
        float decayAmountY = decayVerticalAmount;   //base amount

        //increase amount taken from momentum based on speed    NOT IMPLEMENTED YET <================

        //factor in deltaTime
        decayAmountY *= deltaTime;

        absSubMomentum(decayAmountY, false, true, false);
        //if(applyZero) {zeroThresholdY();}
    }

    /**
     * applies gravity
     * @param deltaTime time elapsed between current and previous frame
     */
    public void applyGravity(double deltaTime) {
        float gravityAmount = gravityStrength;    

        //factor in deltaTime
        gravityAmount *= deltaTime;
        
        momentum.y -= gravityAmount;
    }

    /**
     * reduces yaw
     * @param deltaTime
     */
    public void decayYaw(double deltaTime) {
        float yawDecay = yawDecayAmount;

        //facotr in deltaTime
        yawDecay *= deltaTime;

        absSubRotation(yawDecay, true, false, false);
        //System.out.println(yawDecayAmount);
        //printVector(rotationMomentum);
    }

    /**
     * reduces pitch
     * @param deltaTime
     */
    public void decayPitch(double deltaTime) {
        float pitchDecay = pitchDecayAmount;

        //factor in deltaTime
        pitchDecay *= deltaTime;

        absSubRotation(pitchDecay, false, true, false);
    }

    /**
     * reduces roll
     * @param deltaTime
     */
    public void decayRoll(double deltaTime) {
        float rollDecay = rollDecayAmount;

        //factor in deltaTime
        rollDecay *= deltaTime;

        absSubRotation(rollDecay, false, false, true);
    }

    /**
     * adds bump momentum
     * @param bump direction and magnitude of bump
     */
    public void bump(Vector3f bump) {
        resetAllMomentum();
        addMomentumBump(bump);
    }

    //=========================================methods that reset momentum in various ways================================

    /**
     * resets GameObject's momentum to 0 in the XYZ directions
     */
    public void resetAllMomentum() {
        momentum.set(0,0,0);
        bumpMomentum.set(0, 0, 0);
    }

    /**
     * resets GameObject's momentum to 0 in the X direction
     */
    public void resetXMomentum() {
        momentum.x = 0;
    }

    /**
     * resets GameObject's momentum to 0 in the Y direction
     */
    public void resetYMomentum() {
        momentum.y = 0;
    }

    /** only resets the Y momentum if its negative */
    public void resetYMomentumNegative() {
        if(momentum.y() < 0) {
            resetYMomentum();
        }
    }

    /**
     * resets GameObject's momentum to 0 in the Z direction
     */
    public void resetZMomentum() {
        momentum.z = 0;
    }

    //=====================================getter setter methods no documentaion====================================================
            //getters
    public GameObject getTarget() {return target;}
    public Vector3f getMomentum() {return momentum;}
    public boolean applyZero() {return applyZero;}

            //setters
    public void setTarget(GameObject newTarget) {
        target = newTarget;
    }
    public void setMomentum(Vector3f newMomentum) {
        momentum.set(newMomentum);
    }
    public void applyZero(boolean a) {
        applyZero = a;
    }

    /**
     * @return speed in XYZ direction
     */
    public float getSpeed() {
        return (float) Math.sqrt(Math.pow(momentum.x(),2) + Math.pow(momentum.y(),2) + Math.pow(momentum.z(),2));  //sqrt(x^2 + y^2 + z^2) = speed
    }

    /**
     * @return speed in XZ direction
     */
    public float getHorizontalSpeed() {
        return (float) Math.sqrt(Math.pow(momentum.x(),2) + Math.pow(momentum.z(), 2));
    }

    //=====================================private helper methods little to no documentation========================================

    //attempts to zero out X momentum
    private void zeroThresholdX() {
        if(momentum.x() <= zero_threshold) {
            momentum.x = 0;
        }
    }

    //attempts to zero out Y momentum
    private void zeroThresholdY() {
        if(momentum.y() <= zero_threshold) {
            momentum.y = 0;
        }
    }

    //attempts to zero out Z momentum
    private void zeroThresholdZ() {
        if(momentum.z() <= zero_threshold) {
            momentum.z = 0;
        }
    }

    //attempts to zero out all XYZ directions of momentum
    private void zeroThresholdAll() {
        zeroThresholdX();
        zeroThresholdY();
        zeroThresholdZ();
    }

    //prints a vector
    private void printVector(Vector3f v) {
        System.out.printf("%f, %f, %f\n", v.x(), v.y(), v.z());
    }

    //gets momentums specified components closer to 0 by amount
    //if the absolute value of a specified component is less than amount, set it to 0
    private void absSubMomentum(float amount, boolean x, boolean y, boolean z) {
        if(x) {
            if(Math.abs(momentum.x()) <= amount) {
                momentum.x = 0;
            }
            else {
                if(momentum.x()>0) {
                    momentum.x -= amount;
                }
                else if(momentum.x() < 0) {
                    momentum.x += amount;
                }
            }
        }

        if(y) {
            if(Math.abs(momentum.y()) <= amount) {
                momentum.y = 0;
            }
            else {
                if(momentum.y()>0) {
                    momentum.y -= amount;
                }
                else if(momentum.y() < 0) {
                    momentum.y += amount;
                }
            }
        }

        if(z) {
            if(Math.abs(momentum.z()) <= amount) {
                momentum.z = 0;
            }
            else {
                if(momentum.z()>0) {
                    momentum.z -= amount;
                }
                else if(momentum.z() < 0) {
                    momentum.z += amount;
                }
            }
        }
    }

    //gets bump momentums specified components closer to 0 by amount
    //if the absolute value of a specified component is less than amount, set it to 0
    private void absSubBumpMomentum(float amount, boolean x, boolean y, boolean z) {
        if(x) {
            if(Math.abs(bumpMomentum.x()) <= amount) {
                bumpMomentum.x = 0;
            }
            else {
                if(bumpMomentum.x()>0) {
                    bumpMomentum.x -= amount;
                }
                else if(bumpMomentum.x() < 0) {
                    bumpMomentum.x += amount;
                }
            }
        }

        if(y) {
            if(Math.abs(bumpMomentum.y()) <= amount) {
                bumpMomentum.y = 0;
            }
            else {
                if(bumpMomentum.y()>0) {
                    bumpMomentum.y -= amount;
                }
                else if(bumpMomentum.y() < 0) {
                    bumpMomentum.y += amount;
                }
            }
        }

        if(z) {
            if(Math.abs(bumpMomentum.z()) <= amount) {
                bumpMomentum.z = 0;
            }
            else {
                if(bumpMomentum.z()>0) {
                    bumpMomentum.z -= amount;
                }
                else if(bumpMomentum.z() < 0) {
                    bumpMomentum.z += amount;
                }
            }
        }
    }

    //gets rotationMomentums specified components closer to 0
    private void absSubRotation(float amount, boolean x, boolean y, boolean z) {
        if(x) {
            if(Math.abs(rotationMomentum.x()) <= amount) {
                rotationMomentum.x = 0;
            }
            else {
                if(rotationMomentum.x() > 0) {
                    rotationMomentum.x -= amount;
                }
                else if(rotationMomentum.x() < 0) {
                    rotationMomentum.x += amount;
                }
            }
        }

        if(y) {
            if(Math.abs(rotationMomentum.y()) <= amount) {
                rotationMomentum.y = 0;
            }
            else {
                if(rotationMomentum.y() > 0) {
                    rotationMomentum.y -= amount;
                }
                else if(momentum.y() < 0) {
                    rotationMomentum.y += amount;
                }
            }
        }

        if(z) {
            if(Math.abs(rotationMomentum.z()) <= amount) {
                rotationMomentum.z = 0;
            }
            else {
                if(rotationMomentum.z() > 0) {
                    rotationMomentum.z -= amount;
                }
                else if(rotationMomentum.z() < 0) {
                    rotationMomentum.z += amount;
                }
            }
        }
    }

    public Vector3f getMomentumVector() {
        return momentum;
    }

}
