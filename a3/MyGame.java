package a3;

import net.java.games.input.Event;
import tage.*;
import tage.audio.*;
import tage.input.InputManager;
import tage.input.action.AbstractInputAction;
import tage.physics.PhysicsObject;
import tage.shapes.*;
import tage.networking.IGameConnection.ProtocolType;

import java.awt.event.*;
import java.io.IOException;
import java.lang.Math;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;

import org.joml.*;

public class MyGame extends VariableFrameRateGame {
	private static Engine engine;
	private long lastFrameTime, currFrameTime;
	private double second;
	private GameObject avatar, terr, wormhole, ship, rover;
	private ObjShape shipShape, terrS, lampShape, roverShape;
	private TextureImage shipTexture, groundtx, heightMapTx, lampTexture, roverTx;
    private Light l2;
	private int frames = 0, fps = 0;;
	private InputManager im;
	private CameraOrbitController orbitController;
	private final String serverAddress;
	private final int serverPort;
	private final ProtocolType serverProtocol;
	private ProtocolClient protClient;
	private boolean isClientConnected = false, gameOver = false, lightOn = true;
	private GhostManager gm;
	private float terrainOffset;	//offset for the terrain height map
	private Tars tars;
	private float pitch = 0, prevPitch = 0;
	private IAudioManager audioManager;
    private Sound backgroundSound, gameOverSound, victorySound, wormholeSound;
	private NPC npc;
	private boolean won = false, ignoreNPC = false, renderPhysicsObjects = false;
	private GameObject[] spheres = new GameObject[4];
	private PhysicsObject[] physSpheres = new PhysicsObject[4];
	private FullObject[] fullSpheres = new FullObject[4];
	private boolean avatarChosen = false;
	private String avatarType = "rover";

	private AvatarMovementV3 avMovement;
	private FullObject avatarFull;
	private CollisionSystem collisionSystem;

	private int selectionScreenOffset = -999000;	//glitchiness occurs at 1 million so getting it closer to 0
	private Matrix4fStack mStack = new Matrix4fStack(5);

	public MyGame(String serverAddress, int serverPort, String protocol) {
		super();
		gm = new GhostManager(this);
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		if (protocol.toUpperCase().compareTo("TCP") == 0) {
			this.serverProtocol = ProtocolType.TCP;
		} else {
			this.serverProtocol = ProtocolType.UDP;
		}
	}

	public static void main(String[] args) {
		MyGame game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);
		engine = new Engine(game);
		game.initializeSystem();
		game.game_loop();
	}

	@Override
	public void loadShapes() {
		shipShape = new ImportedModel("ship.obj");
		terrS = new TerrainPlane(500);
		lampShape = new ImportedModel("lamp.obj");
		roverShape = new ImportedModel("Rover.obj");
	}

	@Override
	public void loadTextures() {
		shipTexture = new TextureImage("ship.png");
		groundtx = new TextureImage("ground.jpg");
		heightMapTx = new TextureImage("HeightMap.jpg");
		lampTexture = new TextureImage("lamp.png");
		roverTx = new TextureImage("Rover.jpg");
	}

	@Override
	public void loadSkyBoxes() {
        int skyBox = engine.getSceneGraph().loadCubeMap("stars");
		engine.getSceneGraph().setActiveSkyBoxTexture(skyBox);
		engine.getSceneGraph().setSkyBoxEnabled(true);
	}

	@Override
	public void loadSounds() {
		AudioResource r1, r2, r3, r4;
		audioManager = engine.getAudioManager();
		r2 = audioManager.createAudioResource("assets/sounds/background.wav", AudioResourceType.AUDIO_STREAM);
		r3 = audioManager.createAudioResource("assets/sounds/gameOver.wav", AudioResourceType.AUDIO_SAMPLE);
		r4 = audioManager.createAudioResource("assets/sounds/victory.wav", AudioResourceType.AUDIO_SAMPLE);
		r1 = audioManager.createAudioResource("assets/sounds/wormhole.wav", AudioResourceType.AUDIO_SAMPLE);

		backgroundSound = new Sound(r2, SoundType.SOUND_MUSIC, 60, true);
		backgroundSound.initialize(audioManager);
		backgroundSound.play();

		gameOverSound = new Sound(r3, SoundType.SOUND_EFFECT, 100, false);
		gameOverSound.initialize(audioManager);

		victorySound = new Sound(r4, SoundType.SOUND_EFFECT, 60, false);
		victorySound.initialize(audioManager);

		wormholeSound = new Sound(r1, SoundType.SOUND_EFFECT, 100, true);
		wormholeSound.initialize(audioManager);
		wormholeSound.play();

		wormholeSound.setMaxDistance(80);
		wormholeSound.setRollOff(.4f);
	}

	@Override
	public void buildObjects() {
		Matrix4f initialTranslation, initialScale, translation;
		collisionSystem = new CollisionSystem(engine.getSceneGraph().getPhysicsEngine());
		float[] vals = new float[16];

		//avatar spaceship
		ship = new GameObject(GameObject.root(), shipShape, shipTexture);
		initialTranslation = (new Matrix4f()).translation(1000000 + selectionScreenOffset, 1000000 + selectionScreenOffset, 1000000 + selectionScreenOffset);
		ship.setLocalTranslation(initialTranslation);

		//avatar rover
		rover = new GameObject(GameObject.root(), roverShape, roverTx);
		rover.getRenderStates().setModelOrientationCorrection(new Matrix4f().rotationY((float)Math.toRadians(270)));	//correction for rover model
		initialScale = new Matrix4f().scaling(0.75f);
		initialTranslation = (new Matrix4f()).translation(1000006 + selectionScreenOffset, 1000000 + selectionScreenOffset, 1000000 + selectionScreenOffset);
		rover.setLocalTranslation(initialTranslation);
		rover.setLocalScale(initialScale);

		translation = new Matrix4f(rover.getLocalTranslation());

		avatar = rover;

		//terrain plane
		terr = new GameObject(GameObject.root(), terrS, groundtx);
		terrainOffset = -35.0f;
		initialTranslation = new Matrix4f().translation(0,terrainOffset,0);
		float scale = 100;
		initialScale = (new Matrix4f()).scaling(scale);
		terr.setLocalTranslation(initialTranslation);
		terr.setLocalScale(initialScale);
		terr.setHeightMap(heightMapTx);
		terr.getRenderStates().setTiling(1);
		terr.getRenderStates().setTileFactor((int) scale / 10);

		//tars
		tars = new Tars(avatar, terr);
		tars.toggleStop();

		//npc
		translation.translation(-10.0f, -25.0f, 10.0f);
		//PhysicsObject npcPh = engine.getSceneGraph().addPhysicsCapsuleX(1.0f, toDoubleArray(translation.get(vals)), 0.75f, 2.0f);
		PhysicsObject npcPh = engine.getSceneGraph().addPhysicsSphere(1.0f, toDoubleArray(translation.get(vals)), 0.75f);
		npc = new NPC(avatar, terr, npcPh);
		FullObject npcFull = new FullObject(engine, npc.getGameObject(), npcPh);
		npcFull.setName("Chasing NPC1");
		npc.setFullObject(npcFull);
		collisionSystem.addNPC(npcFull);

		//lamp
		GameObject lamp = new GameObject(GameObject.root(), lampShape, lampTexture);
		lamp.setLocalLocation(new Vector3f(-3, 4.5f, -3));
		lamp.getRenderStates().setModelOrientationCorrection((new Matrix4f()).rotationY((float)Math.toRadians(270)));
		translation = new Matrix4f(lamp.getLocalTranslation());
		translation.translate(0, 0.0f, 0);
		PhysicsObject lampPhys = engine.getSceneGraph().addPhysicsCylinder(1.0f, toDoubleArray(translation.get(vals)), 0.30f, 2.0f);
		FullObject lampFull = new FullObject(engine, lamp, lampPhys);
		lampFull.setName("Lamp");
		collisionSystem.addObstacle(lampFull);

		//wormhole
		ObjShape shape = new Sphere();
		wormhole = new GameObject(GameObject.root(), shape);
		wormhole.getRenderStates().isEnvironmentMapped(true);
		wormhole.setLocalLocation(new Vector3f(-60, 2, 70));
		wormhole.setLocalScale(new Matrix4f().scale(4));

		translation = new Matrix4f().translation(10, 10, 10);
		//yeeting spheres
		int distance = 5;
		for (int i = 0; i < spheres.length; i++) {
			spheres[i] = new GameObject(wormhole, shape);
			spheres[i].getRenderStates().setHasSolidColor(true);
			spheres[i].getRenderStates().setColor(new Vector3f(106 / 255f, 162 / 255f, 252 / 255f));
			spheres[i].applyParentRotationToPosition(true);
			spheres[i].setLocalScale(new Matrix4f().scale(.25f));

			physSpheres[i] = engine.getSceneGraph().addPhysicsSphere(1.0f, toDoubleArray(translation.get(vals)), 0.75f);
			spheres[i].setPhysicsObject(physSpheres[i]);
			fullSpheres[i] = new FullObject(engine, spheres[i], physSpheres[i]);
			fullSpheres[i].setName("Sphere" + i);
			collisionSystem.addNPC(fullSpheres[i]);
			
		}

		spheres[0].setLocalLocation(new Vector3f(distance, 0, distance));
		spheres[1].setLocalLocation(new Vector3f(distance, 0, -distance));
		spheres[2].setLocalLocation(new Vector3f(-distance, 0, -distance));
		spheres[3].setLocalLocation(new Vector3f(-distance, 0, distance));
	}

	@Override
	public void initializeLights() {
		Light.setGlobalAmbient(0.5f, 0.5f, 0.5f);
    	Light light1 = new Light();
		light1.setLocation(new Vector3f(0, 50, 0));
		light1.setSpecular(.5f, .5f, .5f);
		light1.setLinearAttenuation(.5f);
		engine.getSceneGraph().addLight(light1);

		//spotlight
		l2 = new Light();
		l2.setType(Light.LightType.SPOTLIGHT);
		l2.setLocation(new Vector3f(-3f, 4, -.5f));
		l2.setCutoffAngle((float)Math.PI * 1.5f);
		engine.getSceneGraph().addLight(l2);
	}

	@Override
	public void initializeGame() {
		lastFrameTime = System.currentTimeMillis();
		currFrameTime = System.currentTimeMillis();

		engine.getRenderSystem().setWindowDimensions(1900, 1000);

		Camera cam = engine.getRenderSystem().getViewport("MAIN").getCamera();
		cam.setLocation(new Vector3f(1000002 + selectionScreenOffset, 1000004 + selectionScreenOffset, 999994 + selectionScreenOffset));
		cam.lookAt(1000003 + selectionScreenOffset, 1000000 + selectionScreenOffset, 1000000 + selectionScreenOffset);

		orbitController = new CameraOrbitController(
				cam,
				avatar, terr);
		orbitController.setFollowAvatar(false);

		im = engine.getInputManager();

		engine.enableGraphicsWorldRender();

		wormholeSound.setLocation(wormhole.getWorldLocation());
	}

	private void updateHUDs() {
		Viewport main = engine.getRenderSystem().getViewport("MAIN");
		float screenHeight = main.getActualHeight();
		float screenWidth = main.getActualWidth();
		Vector3f hudColor = new Vector3f(0, 1, 0);

		engine.getHUDmanager().setHUD("fps", "FPS: " + fps, hudColor,
				2, (int) screenHeight - 20);
		DecimalFormat df = new DecimalFormat("0.00");
		String str;

		if (avatarChosen) {
			str = "Velocity: " + df.format(avMovement.getSpeedMomentum());
			engine.getHUDmanager().setHUD("velocity", str, hudColor,
					2, 5);
		} else {
			str = "Select Avatar: 5 for rover, 6 for ship";
			engine.getHUDmanager().setHUD("select", str, hudColor,
					(int) screenWidth / 2 - 150, (int) screenHeight - 100);
		}
		if (gameOver) {
			if (won) {
				engine.getHUDmanager().setHUD("gameOver", "Victory!", hudColor,
						(int) screenWidth / 2, (int) screenHeight / 2);
			} else {
				engine.getHUDmanager().setHUD("gameOver", "Game Over!", hudColor,
						(int) screenWidth / 2, (int) screenHeight / 2);
			}
		}
	}

	private void updateSound() {
		Camera camera = (engine.getRenderSystem()).getViewport("MAIN").getCamera();
		audioManager.getEar().setLocation(avatar.getWorldLocation());
		audioManager.getEar().setOrientation(camera.getN(), new Vector3f(0.0f, 1.0f, 0.0f));
		backgroundSound.setLocation(avatar.getWorldLocation());
		gameOverSound.setLocation(avatar.getWorldLocation());
		victorySound.setLocation(avatar.getWorldLocation());
	}

	@Override
	public void update() {
		lastFrameTime = currFrameTime;
		currFrameTime = System.currentTimeMillis();
		double deltaTime = (double) (currFrameTime - lastFrameTime) / 1000;

		frames++;
		second += deltaTime;
		if (second >= 1) {
			second = 0;
			fps = frames;
			frames = 0;
		}
		if (gameOver) {
			updateHUDs();
			return;
		} else if (!avatarChosen) {
			float angle = .1f * (float) deltaTime;
			rover.yaw(angle);
			ship.yaw(angle);
			updateHUDs();
			return;
		}

		im.update((float) deltaTime);
		avMovement.updateMovement();
		collisionSystem.updateCollisions((float)(currFrameTime - lastFrameTime));
		orbitController.updateCameraPosition();

		updateHUDs();
		processNetworking((float) deltaTime);
		tars.update((float) deltaTime);
		npc.update((float) deltaTime);
		updateAvatarPitch();
		updateSound();
		checkGameOVer();
		updateSphereNPCs((float) deltaTime);
		checkAvatarOutsideTerrain();
	}

	private void initAvatarSelected(char selected) {
		im.update(0);
		orbitController.setFollowAvatar(true);

		if (selected == '6') {
			avatar = ship;
			avatarType = "ship";
		}
		orbitController.setAvatarToFollow(avatar);

		avatar.setLocalLocation(new Vector3f(0, 3, 0));
		avatar.setLocalRotation(new Matrix4f());

		engine.getHUDmanager().removeHUD("select");

		Matrix4f translation = new Matrix4f(ship.getLocalTranslation());
		float[] vals = new float[16];
		PhysicsObject avatarPhys = engine.getSceneGraph().addPhysicsCapsuleZ(
				1.0f, toDoubleArray(translation.get(vals)), 0.75f, 2.0f);
		avatarPhys.setBounciness(1.0f);

		avatarFull = new FullObject(engine, avatar, avatarPhys);
		avatarFull.setName("avatar");
		collisionSystem.addAvatar(avatarFull);

		setupNetworking();

		avMovement = new AvatarMovementV3(avatarFull, im, protClient, false);
		avMovement.setTerrain(terr, terrainOffset); //enables terrain to affect avatar

		avatarChosen = true;

		tars.toggleStop();
		tars.setObjectToFollow(avatar);
		npc.setObjectToFollow(avatar);
		npc.reset();
	}

	private void checkGameOVer() {
		Vector3f v1 = avatar.getWorldLocation();
		Vector3f v2 = wormhole.getWorldLocation();
		float distance = Vector3f.distance(v1.x, v1.y, v1.z, v2.x, v2.y, v2.z);

		if (distance < 5) {
			gameOver = true;
			won = true;
			victorySound.play();
		}

		//check if the npc collided with something
		if(npc.getFullObject().checkCollision()) {
			//npc collided with something
			//System.out.println("collision occured");
			if(npc.getFullObject().getCollisionType() == 0) {
				//npc collided with an avatar

				//put this code into the space where the npc collided with this games avatar
				if (!ignoreNPC) {
					gameOver = true;
					won = false;
					gameOverSound.play();
					protClient.sendDiedMessage();
				}
			}

		}
		if (gameOver) {
			backgroundSound.pause();
		}
	}

	private void updateSphereNPCs(float deltaTime) {
		wormhole.yaw((float)(Math.toRadians(180) * deltaTime));
		float offset = wormhole.getLocalScale().m00() / 2 - spheres[0].getLocalScale().m00();

		/* for (GameObject sphere : spheres) {
			Vector3f loc = sphere.getWorldLocation();
			Vector3f loc2 = sphere.getLocalLocation();
			float tHeight = terr.getHeight(loc.x, loc.y) + terrainOffset - offset;
			sphere.setLocalLocation(new Vector3f(loc2.x, tHeight, loc2.z));


			Vector3f loc3 = avatar.getWorldLocation();
			float distance = Vector3f.distance(loc.x, loc.y, loc.z,	loc3.x, loc3.y, loc3.z);

			if (ignoreNPC) {
				continue;
			}
			if (distance < 2) {
				gameOver = true;
				won = false;
				gameOverSound.play();
			}
		} */

		/* Vector3f wormholeTranslation = new Vector3f();
		wormhole.getLocalTranslation().getTranslation(wormholeTranslation);
		mStack.pushMatrix();
		mStack.translate(wormholeTranslation);
		mStack.mul(wormhole.getLocalRotation()); */

		for (int i = 0 ; i < spheres.length; i++) {
			Vector3f loc = spheres[i].getWorldLocation();
			Vector3f loc2 = spheres[i].getLocalLocation();
			float tHeight = terr.getHeight(loc.x, loc.y) + terrainOffset - offset;
			spheres[i].setLocalLocation(new Vector3f(loc2.x(), tHeight, loc2.z()));
			fullSpheres[i].updatePhysicsObjectWorld();

			Vector3f loc3 = avatar.getWorldLocation();
			float distance = Vector3f.distance(loc.x(), loc.y(), loc.z(), loc3.x(), loc3.y(), loc3.z());

			/* if(distance < 2) {
				gameOver = true;
				won = false;
				gameOverSound.play();
			} */

			//check if the sphere collided with another object
			if(fullSpheres[i].checkCollision()) {
				//check if the sphere collided with the avatar
				if(fullSpheres[i].getCollisionType() == 0) {
					//check if the avatar that collided is this clients avatar
					if(fullSpheres[i].getCollidingWith() == avatar.getPhysicsObject().getUID()) {
						//sphere collided with this clients avatar
						//check if this collision has not happened within the past 500 milliseconds
						if(System.currentTimeMillis() - fullSpheres[i].getProcessTime() > 500) {
							//collision hasnt happened within the past 500 millis
							//bump the avatar
							if (!ignoreNPC) {
								fullSpheres[i].updateProcessTime();
								avMovement.bump(avatar.getLocalLocation(), fullSpheres[i].getGameObject().getWorldLocation());
							}
						}
					}
				}
			}//average nested if hell
		}
	}

	private void checkAvatarOutsideTerrain() {
		Vector3f loc = avatar.getWorldLocation();
		float max = terr.getLocalScale().m00();
		boolean b = false;

		if (Math.abs(loc.x) > max) {
			loc.x = max * Math.abs(loc.x) / loc.x;
			b = true;
		}
		if (Math.abs(loc.z) > max) {
			loc.z = max * Math.abs(loc.z) / loc.z;
			b = true;
		}
		if (b) {
			avMovement.resetMomentum();
			avatar.setLocalLocation(loc);
		}
	}

	private void updateAvatarPitch() {
		Vector3f v1 = avatar.getWorldLocation();
		v1.y -= 0.2015f;
		if (terr.getHeight(v1.x, v1.z) + terrainOffset < v1.y - 0.2015) {
			return;
		}
		Vector3f fwd = avatar.getLocalForwardVector();
		Vector3f v2 = new Vector3f(fwd.x, fwd.y, fwd.z);
		v2.mul(1.1f);
		v2.add(v1.x, v1.y, v1.z);
		v2.y = terr.getHeight(v2.x, v2.z) + terrainOffset;
		float prevAngle = getPitchAngle(avatar.getLocalForwardVector());
		float newAngle = getPitchAngle(v1, v2);
		float pitchAngle = newAngle - prevAngle;

		if (pitchAngle != prevPitch) {
			pitch = pitchAngle;
		}
		prevPitch = pitchAngle;

		if (Math.abs(pitch) >= .01f) {
			pitchAngle = .01f * pitch / Math.abs(pitch);
		} else {
			pitchAngle = pitch;
		}

		pitch -= pitchAngle;
		if (Math.abs(pitchAngle) > 0) {
			avatar.pitch(pitchAngle);
			protClient.sendPitchMessage(pitchAngle);
		}
	}

	public static float getYawAngle(Vector3f v) {
		float angle = (float) Math.atan(Math.abs(v.x) / Math.abs(v.z));
		if (v.x >= 0 && v.z < 0) {
			angle = (float) Math.PI - angle;
		} else if (v.x <= 0 && v.z < 0) {
			angle = (float) (Math.PI) + angle;
		} else if (v.x < 0 && v.z >= 0) {
			angle = (float) Math.PI * 2 - angle;
		}
		return angle;
	}

	public static float getPitchAngle(Vector3f v) {
		double adjacent = Math.sqrt(Math.pow(v.x, 2) + Math.pow(v.z, 2));
		return (float) -Math.atan(v.y / adjacent);
	}

	public static float getPitchAngle(Vector3f v1, Vector3f v2) {
		double xDistance = v2.x - v1.x;
		double zDistance = v2.z - v1.z;
		double opposite = v2.y - v1.y;
		double adjacent = Math.sqrt(Math.pow(xDistance, 2) + Math.pow(zDistance, 2));
		return (float) -Math.atan(opposite/ adjacent);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		int key = e.getKeyCode();
		if (key == KeyEvent.VK_4) {
			engine.getRenderSystem().toggleVsync();
		} else if (key == KeyEvent.VK_3) {
			if (gameOver) {
				gameOver = false;
				engine.getHUDmanager().removeHUD("gameOver");
				backgroundSound.resume();
			}
			ignoreNPC = !ignoreNPC;
		} else if (key == KeyEvent.VK_2) {
			if (lightOn) {
				l2.setCutoffAngle(0);
				lightOn = false;
			} else {
				l2.setCutoffAngle((float)Math.PI * 1.5f);
				lightOn = true;
			}
		} else if (key == KeyEvent.VK_1) {
			renderPhysicsObjects = !renderPhysicsObjects;
			if (renderPhysicsObjects) {
				engine.enablePhysicsWorldRender();
			} else {
				engine.disablePhysicsWorldRender();
			}
		} else if (!avatarChosen && (key == KeyEvent.VK_5 || key == KeyEvent.VK_6)) {
			initAvatarSelected((char) key);
		}
		super.keyPressed(e);
	}

	public ObjShape getGhostShape(String type) {
		if (type.equals("rover")) {
			return roverShape;
		}
		return shipShape;
	}
	public TextureImage getGhostTexture(String type) {
		if (type.equals("rover")) {
			return roverTx;
		}
		return shipTexture;
	}
	public GhostManager getGhostManager() { return gm; }
	public Engine getEngine() { return engine; }

	private void setupNetworking() {
		isClientConnected = false;
		try {
			protClient = new ProtocolClient(InetAddress.getByName(serverAddress), serverPort, serverProtocol, this);
		} 	catch (UnknownHostException e) {
			e.printStackTrace();
		}	catch (IOException e) {
			e.printStackTrace();
		}
		if (protClient == null) {
			System.out.println("missing protocol host");
		}
		else {	// Send the initial join message with a unique identifier for this client
			System.out.println("sending join message to protocol host");
			protClient.sendJoinMessage(avatarType);
		}
	}

	protected void processNetworking(float elapsTime) {	// Process packets received by the client from the server
		if (protClient != null) {
			protClient.processPackets();
		}
	}

	public Vector3f getPlayerPosition() {
		return avatar.getWorldLocation();
	}

	public void setIsConnected(boolean value) {
		this.isClientConnected = value;
	}

	private class SendCloseConnectionPacketAction extends AbstractInputAction {
		@Override
		public void performAction(float time, Event evt) {
			if (protClient != null && isClientConnected) {
				protClient.sendByeMessage();
			}
		}
	}

	private double[] toDoubleArray(float[] arr) { 
		if (arr == null) return null;
		int n = arr.length;
		double[] ret = new double[n];
		for (int i = 0; i < n; i++) { 
			ret[i] = (double)arr[i];
		}
		return ret;
	}
}