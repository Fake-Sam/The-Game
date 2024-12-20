package tage.nodeControllers;

import org.joml.Vector3f;
import tage.GameObject;
import tage.NodeController;

/**
 * Changes color of the given game object by cycling through rgb colors
 */
public class ColorController extends NodeController {
    private int state = 0;
    private float time = 20;

    @Override
    public void apply(GameObject t) {
        float dTime = getElapsedTime();
        time -= dTime;

        if (time <= 0) {
            Vector3f color = t.getRenderStates().getColor();
            float r = color.x;
            float g = color.y;
            float b = color.z;
            float dColor = .01f;

            if (state == 0) {
                boolean st = true;
                if (r < 1) {
                    r += dColor;
                    if (r > 1) {
                        r = 1;
                    }
                    st = false;
                }
                if (g != 0) {
                    g -= dColor;
                    if (g < 0) {
                        g = 0;
                    }
                    st = false;
                }
                if (b < 0) {
                    b -= dColor;
                    if (b < 0) {
                        b = 0;
                    }
                    st = false;
                }
                if (st) {
                    state += 1;
                }
            } else if (state == 1) {
                boolean st = true;
                if (g < 1) {
                    g += dColor;
                    if (g > 1) {
                        g = 1;
                    }
                    st = false;
                }
                if (st) {
                    state += 1;
                }
            } else if (state == 2) {
                boolean st = true;
                if (r > 0) {
                    r -= dColor;
                    if (r < 0) {
                        r = 0;
                    }
                    st = false;
                }
                if (st) {
                    state += 1;
                }
            } else if (state == 3) {
                boolean st = true;
                if (b < 1) {
                    b += dColor;
                    if (b > 1) {
                        b = 1;
                    }
                    st = false;
                }
                if (st) {
                    state += 1;
                }
            } else if (state == 4) {
                boolean st = true;
                if (g > 0) {
                    g -= dColor;
                    if (g < 0) {
                        g = 0;
                    }
                    st = false;
                }
                if (st) {
                    state += 1;
                }
            } else if (state == 5) {
                boolean st = true;
                if (r < 1) {
                    r += dColor;
                    if (r > 1) {
                        r = 1;
                    }
                    st = false;
                }
                if (st) {
                    state += 1;
                }
            } else if (state == 6) {
                boolean st = true;
                if (b > 0) {
                    b -= dColor;
                    if (b < 0) {
                        b = 0;
                    }
                    st = false;
                }
                if (st) {
                    state = 1;
                }
            }
            t.getRenderStates().setColor(new Vector3f(r, g, b));
            time = 20;
        }
    }
}
