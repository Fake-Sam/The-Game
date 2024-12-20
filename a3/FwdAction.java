package a3;

import net.java.games.input.Component;
import net.java.games.input.Event;
import org.joml.Vector3f;
import tage.GameObject;
import tage.input.action.AbstractInputAction;

import java.util.ArrayList;

public class FwdAction extends AbstractInputAction {
    private final GameObject obj;
    private final ProtocolClient protClient;
    private final ArrayList<Component.Identifier> backComponents = new ArrayList<>();

    public FwdAction(GameObject obj, ProtocolClient p) {
        this.obj = obj;
        protClient = p;
    }
    @Override
    public void performAction(float time, Event evt) {
        Vector3f fwd, loc, newLocation;
        fwd = obj.getWorldForwardVector();
        loc = obj.getWorldLocation();
        float speed = getSpeed();
        if (isBackComponent(evt)) {
            speed *= -1;
        } else if (evt.getComponent().isAnalog()) {
            speed *= Math.abs(evt.getValue());
            if (evt.getValue() > 0) {
                speed *= -1;
            }
        }
        newLocation = loc.add(fwd.mul(time * speed * 2));
        obj.setLocalLocation(newLocation);
        protClient.sendMoveMessage(obj.getWorldLocation());
    }

    public void addBackComponent(Component.Identifier identifier) {
        backComponents.add(identifier);
    }

    private boolean isBackComponent(Event evt) {
        return backComponents.contains(evt.getComponent().getIdentifier());
    }
}
