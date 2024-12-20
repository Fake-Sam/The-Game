package tage;

import net.java.games.input.Component;
import net.java.games.input.Event;
import org.joml.Vector3f;
import tage.input.IInputManager;
import tage.input.InputManager;
import tage.input.action.AbstractInputAction;

import java.util.ArrayList;

import static a3.MyGame.*;

/**
 * camera orbit controller
 */
public class CameraOrbitController {
    private final Engine engine;
    private final Camera cam;
    private GameObject gameObject;
    private final GameObject terrain;
    private float azimuth = 180;
    private float elevation = 20;
    private float radius = 10;
    private float prevYawAngle;
    private final float speed = 75; // speed of angle change and radius
    private boolean followAvatar = true;

    public CameraOrbitController(Camera camera, GameObject gameObject, GameObject terrain) {
        cam = camera;
        this.gameObject = gameObject;
        this.engine = Engine.getEngine();
        setupInputs();
        prevYawAngle = getYawAngle(gameObject.getLocalForwardVector());
        this.terrain = terrain;
    }

    /** sets up the input using input manager */
    private void setupInputs() {
        InputManager im = engine.getInputManager();
        AzimuthAction azimuthAction = new AzimuthAction();
        azimuthAction.addRightComponent(Component.Identifier.Key.RIGHT);

        im.associateActionWithAllKeyboards(
                Component.Identifier.Key.LEFT,
                azimuthAction,
                IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
        );
        im.associateActionWithAllKeyboards(
                Component.Identifier.Key.RIGHT,
                azimuthAction,
                IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
        );

        ElevationAction elevationAction = new ElevationAction();
        elevationAction.addDownComponent(Component.Identifier.Key.DOWN);

        im.associateActionWithAllKeyboards(
                Component.Identifier.Key.UP,
                elevationAction,
                IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
        );
        im.associateActionWithAllKeyboards(
                Component.Identifier.Key.DOWN,
                elevationAction,
                IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
        );

        RadiusAction radiusAction = new RadiusAction();
        radiusAction.addZommOutComponent(Component.Identifier.Key.Z);

        im.associateActionWithAllKeyboards(
                Component.Identifier.Key.Z,
                radiusAction,
                IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
        );
        im.associateActionWithAllKeyboards(
                Component.Identifier.Key.X,
                radiusAction,
                IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
        );
        im.associateActionWithAllGamepads(
                Component.Identifier.Axis.RX,
                azimuthAction,
                IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
        );
        im.associateActionWithAllGamepads(
                Component.Identifier.Axis.RY,
                elevationAction,
                IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
        );
        im.associateActionWithAllGamepads(
                Component.Identifier.Axis.Z,
                radiusAction,
                IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
        );
    }

    /** updates the camera position based on azimuth, elevation and radius */
    public void updateCameraPosition() {
        if (!followAvatar) {
            return;
        }
        double theta = Math.toRadians(azimuth);
        double phi = Math.toRadians(elevation);
        float x = radius * (float)(Math.cos(phi) * Math.sin(theta));
        float y = radius * (float)(Math.sin(phi));
        float z = radius * (float)(Math.cos(phi) * Math.cos(theta));

        Vector3f newLocation = new Vector3f(x,y,z).add(gameObject.getWorldLocation());

        float maxDistance = terrain.getLocalScale().m00();
        float terrainHeight = terrain.getHeight(newLocation.x, newLocation.z) - 35.0f;

        if (!(Math.abs(newLocation.x) <= maxDistance && Math.abs(newLocation.z) <= maxDistance)) {
            terrainHeight = -Float.MAX_VALUE;
        }

        if (newLocation.y < terrainHeight) {
            newLocation.y = terrainHeight;
        }

        cam.setLocation(newLocation);
        cam.lookAt(gameObject);

        float newAngle = getYawAngle(gameObject.getLocalForwardVector());

        if (prevYawAngle != newAngle) {
            azimuth += (float) Math.toDegrees(newAngle - prevYawAngle);
            azimuth = azimuth % 360;
        }
        prevYawAngle = newAngle;
    }

    public void setFollowAvatar(boolean b) {
        followAvatar = b;
    }

    public void setAvatarToFollow(GameObject go) {
        gameObject = go;
    }

    private class AzimuthAction extends AbstractInputAction {
        private final ArrayList<Component.Identifier> rightComponents = new ArrayList<>();
        @Override
        public void performAction(float time, Event evt) {
            float delta = speed * time;

            if (isRightComponent(evt)) {
                delta *= -1;
            } else if (evt.getComponent().isAnalog()) {
                delta *= Math.abs(evt.getValue());
                if (evt.getValue() > 0) {
                    delta *= -1;
                }
            }
            azimuth += delta;
            azimuth = azimuth % 360;
        }
        public AzimuthAction addRightComponent(Component.Identifier identifier) {
            rightComponents.add(identifier);
            return this;
        }

        private boolean isRightComponent(Event evt) {
            return rightComponents.contains(evt.getComponent().getIdentifier());
        }
    }

    private class ElevationAction extends AbstractInputAction {
        private final ArrayList<Component.Identifier> downComponents = new ArrayList<>();
        @Override
        public void performAction(float time, Event evt) {
            float delta = speed * time;

            if (isDownComponent(evt)) {
                delta *= -1;
            } else if (evt.getComponent().isAnalog()) {
                delta *= Math.abs(evt.getValue());
                if (evt.getValue() > 0) {
                    delta *= -1;
                }
            }
            elevation += delta;
            elevation = elevation % 360;
        }

        public ElevationAction addDownComponent(Component.Identifier identifier) {
            downComponents.add(identifier);
            return this;
        }

        private boolean isDownComponent(Event evt) {
            return downComponents.contains(evt.getComponent().getIdentifier());
        }

    }

    private class RadiusAction extends AbstractInputAction {
        private final ArrayList<Component.Identifier> zoomOutComponents = new ArrayList<>();

        @Override
        public void performAction(float time, Event evt) {
            float delta = speed / 18.75f * time;

            if (isZoomOutComponent(evt)) {
                delta *= -1;
            } else if (evt.getComponent().isAnalog()) {
                delta *= Math.abs(evt.getValue());
                if (evt.getValue() > 0) {
                    delta *= -1;
                }
            }
            radius += delta;
            if (radius < 2) {
                radius = 2;
            } else if (radius > 20) {
                radius = 20;
            }
        }

        private boolean isZoomOutComponent(Event evt) {
            return zoomOutComponents.contains(evt.getComponent().getIdentifier());
        }

        public RadiusAction addZommOutComponent(Component.Identifier identifier) {
            zoomOutComponents.add(identifier);
            return this;
        }
    }
}
