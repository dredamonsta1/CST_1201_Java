import java.util.List;
import java.util.Random;

class Enemy {
    int x, y, type;
    int homeX, homeY;
    boolean diving = false;
    int frame = 0;
    int frameTimer = 0;
    // Diving path
    double dx, dy, angle;
    int divePhase = 0;
    int shootTimer = 0;

    Enemy(int x, int y, int type) {
        this.x = x; this.y = y;
        this.homeX = x; this.homeY = y;
        this.type = type;
    }

    void startDive(int px, int py) {
        diving = true;
        divePhase = 0;
        double targetX = px + 20;
        double targetY = py;
        double dist = Math.sqrt((targetX - x) * (targetX - x) + (targetY - y) * (targetY - y));
        dx = (targetX - x) / dist * 3.5;
        dy = Math.abs((targetY - y) / dist * 3.5) + 1.5;
        angle = 0;
    }

    void update(int px, int py, List<int[]> bullets, Random rng, int level) {
        // Frame animation
        frameTimer++;
        if (frameTimer > 20) { frameTimer = 0; frame = 1 - frame; }

        if (diving) {
            x += (int) dx;
            y += (int) dy;
            dy += 0.07; // gravity curve

            // Shoot while diving occasionally
            shootTimer++;
            int rate = Math.max(30, 90 - level * 8);
            if (shootTimer >= rate) {
                shootTimer = 0;
                if (rng.nextInt(3) == 0) bullets.add(new int[]{x + 18, y + 35});
            }

            // If went off screen bottom, re-enter from top
            if (y > GamePanel.H + 20) {
                y = -60;
                x = homeX;
                dx = 0;
                dy = 2;
                divePhase = 1;
            }

            // Return to formation
            if (divePhase == 1) {
                int tx = homeX, ty = homeY;
                if (Math.abs(x - tx) < 5 && Math.abs(y - ty) < 5) {
                    x = tx; y = ty; diving = false; divePhase = 0;
                } else {
                    x += Integer.signum(tx - x) * 2;
                    y += Integer.signum(ty - y) * 2;
                }
            }
        }
    }
}
