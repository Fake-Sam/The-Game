package a3;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import tage.Engine;
import tage.FullObject;
import tage.GameObject;
import tage.physics.PhysicsObject;
import tage.audio.*;
import tage.shapes.ImportedModel;

public class NPC {
    private final GameObject npc;
    private GameObject objectToFollow;
    private final GameObject terrain;
    private int state = 0;
    private final Vector3f normalColor = new Vector3f(75, 51, 255).div(255f);
    private final Vector3f attackColor = new Vector3f(255, 0, 0).div(255f);
    private float transitionTime = .0085f;
    IAudioManager audioManager = Engine.getEngine().getAudioManager();
    private final Sound warpSound;
    private final float startSpeed = 15;
    private float speed = startSpeed;
    private FullObject npcFull;

    public NPC(GameObject objectToFollow, GameObject terrain, PhysicsObject ph) {
        ImportedModel shape = new ImportedModel("npc.obj");
        npc = new GameObject(GameObject.root(), shape);
        npc.setPhysicsObject(ph);
        this.objectToFollow = objectToFollow;
        this.terrain = terrain;
        npc.getRenderStates().setHasSolidColor(true);
        npc.getRenderStates().setColor(normalColor);
        lookUp();
        npc.setLocalLocation(new Vector3f(-10, -25, 10));

        AudioResource r = audioManager.createAudioResource("assets/sounds/warp.wav", AudioResourceType.AUDIO_SAMPLE);
        warpSound = new Sound(r, SoundType.SOUND_EFFECT, 100, false);
        warpSound.initialize(audioManager);
        warpSound.setMaxDistance(40);
        warpSound.setRollOff(.2f);
    }

    public void update(float deltaTime) {
        if (state == 0) {
            Vector3f loc = npc.getWorldLocation();
            float height = terrain.getHeight(loc.x, loc.z) - 30;
            loc.y += 5f * deltaTime;
            npc.setLocalLocation(loc);

            if (loc.y >= height) {
                state++;
            }
        } else if (state == 1) {
            transitionTime -= deltaTime;

            if (transitionTime > 0) {
                return;
            }
            transitionTime = .0085f;
            Vector3f color = npc.getRenderStates().getColor();
            color.x += .008f;

            if (color.x >= 1) {
                color = attackColor;
                state++;
                npc.lookAt(objectToFollow);
                warpSound.play();
            }
            npc.getRenderStates().setColor(color);
        } else if (state == 2) {
            Vector3f fwd = npc.getLocalForwardVector();
            Vector3f loc = npc.getWorldLocation();
            fwd.mul(speed * deltaTime);
            speed += 20f * deltaTime;
            loc.add(fwd);
            npc.setLocalLocation(loc);
            float height = terrain.getHeight(loc.x, loc.z) - 36;

            if (loc.y <= height) {
                state = 0;
                lookUp();
                npc.getRenderStates().setColor(normalColor);
                speed = startSpeed;
            }
        }
        warpSound.setLocation(npc.getWorldLocation());
        Vector3f loc = npc.getWorldLocation();
        float maxDistance = terrain.getLocalScale().m00();

        if (Math.abs(loc.x) > maxDistance || Math.abs(loc.z) > maxDistance) {
            npc.setLocalLocation(new Vector3f(0, 0, 0));
        }

        npcFull.updatePhysicsObject();
    }

    public void setObjectToFollow(GameObject go) {
        objectToFollow = go;
    }

    public void reset() {
        state = 0;
        npc.setLocalLocation(new Vector3f(-10, 0, 10));
    }

    public void setFullObject(FullObject f) {
        npcFull = f;
    }

    public Vector3f getLocation() {
        return npc.getWorldLocation();
    }

    public GameObject getGameObject() {return npc;}

    public FullObject getFullObject() {return npcFull;}

    private void lookUp() {
        npc.setLocalRotation(new Matrix4f().rotationX((float) -Math.PI / 2));
    }
}
