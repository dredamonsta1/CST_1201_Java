import java.awt.*;
import java.util.Random;

class Explosion {
    int x, y, life = 30;
    Random rng = new Random();
    int[][] particles;

    Explosion(int x, int y) {
        this.x = x; this.y = y;
        particles = new int[18][4]; // x,y,vx,vy
        for (int[] p : particles) {
            p[0] = x; p[1] = y;
            double a = rng.nextDouble() * Math.PI * 2;
            double spd = 1 + rng.nextDouble() * 4;
            p[2] = (int)(Math.cos(a) * spd);
            p[3] = (int)(Math.sin(a) * spd);
        }
    }

    boolean alive() { return life > 0; }

    void update() {
        life--;
        for (int[] p : particles) {
            p[0] += p[2]; p[1] += p[3];
        }
    }

    void draw(Graphics2D g2) {
        float ratio = life / 30f; // controls explosion fade below  10 game frezes above 90 explosions dont look the same f need to be there
        for (int[] p : particles) {
            int r = 255;
            int gr = (int)(ratio * 200);
            int b  = 0;
            int alpha = (int)(ratio * 220);
            g2.setColor(new Color(r, gr, b, alpha));
            int size = (int)(ratio * 5) + 1;
            g2.fillOval(p[0] - size/2, p[1] - size/2, size, size);
        }
        // Central flash
        if (life > 20) {
            g2.setColor(new Color(255, 255, 200, (int)(ratio * 180)));
            int s = (int)((1 - ratio) * 30 + 5);
            g2.fillOval(x - s/2, y - s/2, s, s);
        }
    }
}
