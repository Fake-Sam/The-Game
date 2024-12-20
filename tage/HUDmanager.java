package tage;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.gl2.GLUT;
import org.joml.*;

import java.util.HashMap;

/**
* Manages up to two HUD strings, implemented as GLUT strings.
* This class is instantiated automatically by the engine.
* Note that this class utilizes deprectated OpenGL functionality.
* <p>
* The available fonts are:
* <ul>
* <li> GLUT.BITMAP_8_BY_13
* <li> GLUT.BITMAP_9_BY_15
* <li> GLUT.BITMAP_TIMES_ROMAN_10
* <li> GLUT.BITMAP_TIMES_ROMAN_24
* <li> GLUT.BITMAP_HELVETICA_10
* <li> GLUT.BITMAP_HELVETICA_12
* <li> GLUT.BITMAP_HELVETICA_18
* </ul>
* <p>
* This class includes a "kludge".  On many systems, GLUT strings ignore the glColor
* setting and uses the most recent color rendered.  Therefore, this HUD
* renderer first renders a single pixel at the desired HUD color at a
* distant location, before drawing the HUD.
* @author Scott Gordon
*/

public class HUDmanager {
	private GLCanvas myCanvas;
	private GLUT glut = new GLUT();
	private Engine engine;

	private HashMap<String, HUD> huds = new HashMap<>();

	// The constructor is called by the engine, and should not be called by the game application.
	protected HUDmanager(Engine e) {
		engine = e;
	}

	protected void setGLcanvas(GLCanvas g) { myCanvas = g; }

	protected void drawHUDs(int hcp) {
		GL4 gl4 = myCanvas.getGL().getGL4();
		GL4bc gl4bc = (GL4bc) gl4;

		for (String key : huds.keySet()) {
			HUD hud = huds.get(key);
			gl4bc.glWindowPos2d(hud.getX(), hud.getY());
			prepHUDcolor(hud.getColor(), hcp);
			glut.glutBitmapString(hud.getFont(), hud.getDisplayString());
		}
	}

	/** sets HUD to the specified text string, color, and location */
	public void setHUD(String hudName, String string, Vector3f color, int x, int y) {
		if (!huds.containsKey((hudName))) {
			huds.put(hudName, new HUD(string, color, x, y));
		} else {
			HUD hud = huds.get(hudName);
			hud.setDisplayString(string);
			hud.setColor(color.x, color.y, color.z);
			hud.setLocation(x, y);
		}
	}

	/** removes HUD based on HUD nane */
	public void removeHUD(String hudName) {
		huds.remove(hudName);
	}

	public void updateHUDString(String hudName, String str) {
		huds.get(hudName).setDisplayString(str);
	}

	public void updateHUD(String hudName, String str, int x, int y) {
		HUD hud = huds.get(hudName);
		hud.setDisplayString(str);
		hud.setLocation(x, y);
	}

	/** sets HUD font - available fonts are listed above. */
	public void setHUDFont(String hudName, int font) {
		huds.get(hudName).setFont(font);
	}

	// Kludge to ensure HUD renders with correct color - do not call from game application.
	// Draws a single dot at a distant location to set the desired HUD color.
	// Used internally by the renderer.

	private void prepHUDcolor(float[] color, int hcp)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glUseProgram(hcp);
		int hudCLoc = gl.glGetUniformLocation(hcp, "hudc");
		gl.glProgramUniform3fv(hcp, hudCLoc, 1, color, 0);
		gl.glDrawArrays(GL_POINTS,0,1);
	}

	private static class HUD {
		String displayString = "";
		float[] color = new float[3];
		int[] location = new int[2];
		int font = GLUT.BITMAP_HELVETICA_18;

		public HUD(String string, Vector3f color, int x, int y) {
			setDisplayString(string);
			setColor(color.x, color.y, color.z);
			setLocation(x, y);
		}

		public void setColor(float r, float g, float b) {
			color[0] = r;
			color[1] = g;
			color[2] = b;
		}

		public float[] getColor() {
			return color;
		}

		public void setFont(int font) {
			this.font = font;
		}

		public int getFont() {
			return font;
		}

		public void setLocation(int x, int y) {
			location[0] = x;
			location[1] = y;
		}

		public void setDisplayString(String str) {
			displayString = str;
		}

		public String getDisplayString() {
			return displayString;
		}

		public int getX() {
			return location[0];
		}

		public int getY() {
			return location[1];
		}
	}
}