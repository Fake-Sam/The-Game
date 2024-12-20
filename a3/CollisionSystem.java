package a3;

import tage.FullObject;
import tage.physics.PhysicsEngine;
import tage.physics.JBullet.JBulletPhysicsEngine;
import tage.physics.JBullet.JBulletPhysicsObject;

import java.util.ArrayList;

import org.joml.*;

public class CollisionSystem {
  ArrayList<FullObject> avatars;    //avatars
  ArrayList<FullObject> obstacles;  //obstacles
  ArrayList<FullObject> npcs;       //npcs and other objects with special rules
  PhysicsEngine physicsEngine;

  Vector3f v;

  //=============================================CONSTRUCTORS================================================================================
  /** constructor */
  public CollisionSystem(PhysicsEngine pe) {
    physicsEngine = pe;
    avatars = new ArrayList<FullObject>();
    obstacles = new ArrayList<FullObject>();
    npcs = new ArrayList<FullObject>();

    v = new Vector3f(0, 0.1f, 0);
  }

  //============================================METHODS TO ADD OR REMOVE OBJECTS FROM THE COLLISION SYSTEM==========================================
  public void addAvatar(FullObject av) {
    avatars.add(av);
  }

  public void addObstacle(FullObject ob) {
    obstacles.add(ob);
  }

  public void addNPC(FullObject npc) {
    npcs.add(npc);
  }

  public void removeAvatar(FullObject av) {
    avatars.remove(av);
  }

  public void removeObstacle(FullObject ob) {
    obstacles.remove(ob);
  }

  public void removeNPC(FullObject npc) {
    npcs.remove(npc);
  }

  //======================================================UPDATE METHOD============================================
  /** checks for collisions and updates FullObjects based on those collisions */
  public void updateCollisions(float deltaTime) {
    physicsEngine.update(deltaTime);
    apply();
    checkCollisions();
    adjustObstacles();
  }

  //====================================================PRIVATE METHODS==============================================
  /** checks if a collision occured */
  private void checkCollisions() {
    com.bulletphysics.dynamics.DynamicsWorld dynamicsWorld;
		com.bulletphysics.collision.broadphase.Dispatcher dispatcher;
		com.bulletphysics.collision.narrowphase.PersistentManifold manifold;
		com.bulletphysics.dynamics.RigidBody object1, object2;
		com.bulletphysics.collision.narrowphase.ManifoldPoint contactPoint;

		dynamicsWorld = ((JBulletPhysicsEngine)physicsEngine).getDynamicsWorld();
		dispatcher = dynamicsWorld.getDispatcher();
		int manifoldCount = dispatcher.getNumManifolds();
    //System.out.println(manifoldCount);

		for (int i=0; i<manifoldCount; i++) { 
			manifold = dispatcher.getManifoldByIndexInternal(i);
			object1 = (com.bulletphysics.dynamics.RigidBody)manifold.getBody0();
			object2 = (com.bulletphysics.dynamics.RigidBody)manifold.getBody1();
			JBulletPhysicsObject obj1 = JBulletPhysicsObject.getJBulletPhysicsObject(object1);
			JBulletPhysicsObject obj2 = JBulletPhysicsObject.getJBulletPhysicsObject(object2);
			for (int j = 0; j < manifold.getNumContacts(); j++) { 
				contactPoint = manifold.getContactPoint(j);
				if (contactPoint.getDistance() < 0.0f) { 
					/* System.out.println("---- hit between " + obj1 + " and " + obj2);
					System.out.println("obj1 UID: " + obj1.getUID() + "  name: " + getObjName(obj1.getUID()));
					System.out.println("obj2 UID: " + obj2.getUID() + "  name: " + getObjName(obj2.getUID())); */

          //System.out.printf("---- hit between %d: %s and %d: %s\n", obj1.getUID(), getObjName(obj1.getUID()), obj2.getUID(), getObjName(obj2.getUID()));

          updateCollidedObjects(obj1.getUID(), obj2.getUID());
					break;
				} 
			} 
		} 
  }

  /** decides what to do if two objects collided based on what those objects are */
  private void updateCollidedObjects(int uid1, int uid2) {
    int type1 = getType(uid1); //type of object uid1 is      0=avatar  1=obstacle  2=npc
    int type2 = getType(uid2); //type of object uid2 is      0=avatar  1=obstacle  2=npc

    //if either objects are npc update their FullObjects with the collision information
    if(type1 == 2) {
      npcs.get(findFullObjectNPC(uid1)).collisionOccured(uid2, type2);
    }
    if(type2 == 2) {
      npcs.get(findFullObjectNPC(uid2)).collisionOccured(uid1, type1);
    }

    //if either objects are avatar update their FullObjects with the collision information
    if(type1 == 0) {
      avatars.get(findFullObjectAvatar(uid1)).collisionOccured(uid2, type2);
    }
    if(type2 == 0) {
      avatars.get(findFullObjectAvatar(uid2)).collisionOccured(uid1, type1);
    }
  }

  /** searches for an object by its uid and returns the object's name */
  private String getObjName(int uid) {
    int index = -1;

    //serach avatars
    index = findFullObjectAvatar(uid);
    if(index != -1) {
      return avatars.get(index).getName();
    }

    //search obstacles
    index = findFullObjectObstacle(uid);
    if(index != -1) {
      return obstacles.get(index).getName();
    }

    //search npcs
    index = findFullObjectNPC(uid);
    if(index != -1) {
      return npcs.get(index).getName();
    }

    return "UID invalid";
  }

  /** checks avatars to see if there is an avatar with the uid being searched for */
  private int findFullObjectAvatar(int uid) {
    for(int i = 0; i < avatars.size(); i++) {
      if(avatars.get(i).getUID() == uid) {
        return i;
      }
    }
    return -1;
  }

  /** checks obstacles to see if there is an obstacle with the uid being searched for */
  private int findFullObjectObstacle(int uid) {
    for(int i = 0; i < obstacles.size(); i++) {
      if(obstacles.get(i).getUID() == uid) {
        return i;
      }
    }
    return -1;
  }

  /**checks npcs to see if there is an npc with the uid being searched for */
  private int findFullObjectNPC(int uid) {
    for(int i = 0; i < npcs.size(); i++) {
      if(npcs.get(i).getUID() == uid) {
        return i;
      }
    }
    return -1;
  }
  /** keeps physics objects active */
  private void apply() {
    for(FullObject a : avatars) {
      a.applyForce(v);
    }

    for(FullObject n : npcs) {
      n.applyForce(v);
    }

    /* for(FullObject o : obstacles) {
      o.applyForce(v);
    } */
  }

  //updates the position of all obstacles' GameObjects to that of their PhysicsObjects
  private void adjustObstacles() {
    for(FullObject f : obstacles) {
      f.updateGameObject();
    }
  }

  /** returns the type of FullObject the uid is
   * -1=not found
   * 0=avatar
   * 1=obstacle
   * 2=npc
   */
  private int getType(int uid) {
    int type = -1;
    if(findFullObjectAvatar(uid) != -1) {type = 0;}
    else if(findFullObjectObstacle(uid) != -1) {type = 1;}
    else if(findFullObjectNPC(uid) != -1) {type = 2;}

    return type;
  }
}
