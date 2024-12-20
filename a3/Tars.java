package a3;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import tage.GameObject;
import tage.TextureImage;
import tage.shapes.AnimatedShape;

public class Tars {
    private final GameObject[] objects = new GameObject[4];
    private final AnimatedShape[] shapes = new AnimatedShape[4];
    private GameObject objectToFollow;
    private final GameObject terrain;
    private final float speed = 4;
    private boolean walking = false, startWalking;
    private float animationInterval = .01f;
    private boolean stop = false;

    public Tars(GameObject objectToFollow, GameObject terrain) {
        TextureImage texture = new TextureImage("tars.png");

        for (int i = 0; i < objects.length; i++) {
            shapes[i] = new AnimatedShape("tars" + (i + 1) + ".rkm", "tars.rks");
            shapes[i].loadAnimation("walk", "tars.rka");

            objects[i] = new GameObject(GameObject.root(), shapes[i], texture);
            objects[i].setLocalScale(new Matrix4f().scale(.5f));
            objects[i].setLocalLocation(new Vector3f(5, 0, 0));
            objects[i].getRenderStates().setModelOrientationCorrection((new Matrix4f()).rotationY((float)Math.toRadians(270)));
        }
        this.objectToFollow = objectToFollow;
        this.terrain = terrain;
    }

    public void setObjectToFollow(GameObject gameObject) {
        objectToFollow = gameObject;
    }

    public void playAnimation() {
        for (AnimatedShape shape : shapes) {
            shape.playAnimation("walk", 1, AnimatedShape.EndType.LOOP, 0);
        }
    }

    public void updateAnimation() {
        for (AnimatedShape shape : shapes) {
            shape.updateAnimation();
        }
    }

    public void stopAnimation() {
        for (AnimatedShape shape : shapes) {
            shape.stopAnimation();
        }
    }

    public Vector3f getLocation() {
        return objects[0].getWorldLocation();
    }

    public void setLocation(float x, float y, float z) {
        for (GameObject go : objects) {
            go.setLocalLocation(new Vector3f(x, y, z));
        }
    }

    public void lookAt(GameObject gameObject) {
        for (GameObject go : objects) {
            go.lookAt(gameObject);
            float pitchAngle = MyGame.getPitchAngle(go.getLocalForwardVector());
            go.pitch(-pitchAngle);
        }
    }

    public void toggleStop() {
        stop = !stop;
    }

    public void update(float deltaTime) {
        if (stop) {
            return;
        }
        Vector3f v1 = objectToFollow.getWorldLocation();
        Vector3f v2 = objects[0].getWorldLocation();
        float distance = Vector3f.distance(v1.x, 0, v1.z, v2.x, 0, v2.z);
        v2.y = terrain.getHeight(v2.x, v2.z) + terrain.getLocalLocation().y + .93f;
        float maxDistance = terrain.getLocalScale().m00();

        if (distance > 10) {
            if (Math.abs(v2.x) < maxDistance && Math.abs(v2.z) < maxDistance) {
                startWalking = true;
            }
        }
        if (startWalking && distance > 5) {
            lookAt(objectToFollow);
            if (!walking) {
                walking = true;
                playAnimation();
            }
            Vector3f forwardVector = objects[0].getLocalForwardVector();
            Vector3f temp = new Vector3f(v2);
            float s = speed * deltaTime;
            temp.add(forwardVector.x * s, 0, forwardVector.z * s);
            if (Math.abs(temp.x) < maxDistance && Math.abs(temp.z) < maxDistance) {
                v2.add(forwardVector.x * s, 0, forwardVector.z * s);
            } else {
                startWalking = false;
                walking = false;
                stopAnimation();
            }
        } else {
            if (walking) {
                walking = false;
                stopAnimation();
            }
            startWalking = false;
        }
        for (GameObject go : objects) {
            go.setLocalLocation(v2);
        }
        animationInterval -= deltaTime;
        if (animationInterval <= 0) {
            updateAnimation();
            animationInterval = .01f;
        }
    }
}
