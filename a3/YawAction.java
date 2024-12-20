package a3;

import net.java.games.input.Component;
import net.java.games.input.Event;
import tage.GameObject;
import tage.input.action.AbstractInputAction;

import java.util.ArrayList;

public class YawAction extends AbstractInputAction {
    private final Object obj;
    private final ProtocolClient protClient;
    private final ArrayList<Component.Identifier> rightComponents = new ArrayList<>();
    public YawAction(GameObject obj, ProtocolClient p) {
        this.obj = obj;
        protClient = p;
    }
    @Override
    public void performAction(float time, Event evt) {
        float speed = getSpeed();
        if (isRightComponent(evt)) {
            speed *= -1;
        } else if (evt.getComponent().isAnalog()) {
            speed *= Math.abs(evt.getValue());
            if (evt.getValue() > 0) {
                speed *= -1;
            }
        }
        float angle = time * speed;
        ((GameObject) obj).yaw(angle);
        protClient.sendYawMessage(angle);
    }

    public YawAction addRightComponent(Component.Identifier identifier) {
        rightComponents.add(identifier);
        return this;
    }

    private boolean isRightComponent(Event evt) {
        return rightComponents.contains(evt.getComponent().getIdentifier());
    }
}
