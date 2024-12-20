package a3;

import net.java.games.input.Component;
import net.java.games.input.Event;
import tage.GameObject;
import tage.input.action.AbstractInputAction;

import java.util.ArrayList;

public class PitchAction extends AbstractInputAction {
    private final Object obj;
    private final ProtocolClient protClient;
    private final ArrayList<Component.Identifier> downComponents = new ArrayList<>();
    public PitchAction(Object obj, ProtocolClient p) {
        this.obj = obj;
        protClient = p;
    }
    @Override
    public void performAction(float time, Event evt) {
        System.out.println(evt.getValue());
        float speed = getSpeed();
        if (isDownComponent(evt)) {
            speed *= -1;
        } else if (evt.getComponent().isAnalog()) {
            speed *= Math.abs(evt.getValue());
            if (evt.getValue() > 0) {
                speed *= -1;
            }
        }
        float angle = time * speed;
        ((GameObject) obj).pitch(angle);
        protClient.sendPitchMessage(angle);
    }

    public PitchAction addDownComponent(Component.Identifier identifier) {
        downComponents.add(identifier);
        return this;
    }

    private boolean isDownComponent(Event evt) {
        return downComponents.contains(evt.getComponent().getIdentifier());
    }
}
