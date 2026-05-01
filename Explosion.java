import java.awt.*; // The AWT (Abstract Window Toolkit) package is imported to provide classes for creating graphical user interfaces and handling graphics. In the Explosion class, it is used to draw the explosion particles and central flash on the screen.
import java.util.Random; // The Random class is imported to generate random numbers, which are used in the Explosion class to create random velocities for the explosion particles, adding variability to the explosion effect.

class Explosion { // Explosion class represents the explosion effect when an enemy is destroyed. It manages the particles and their movement, as well as the visual representation of the explosion.
    int x, y, life = 30;
    Random rng = new Random();
    int[][] particles; // particles is a 2D array that holds the properties of each particle in the explosion. Each particle has an x and y position, as well as a velocity in the x and y directions (vx, vy).

    Explosion(int x, int y) { // Constructor for the Explosion class. It initializes the explosion at the given x and y coordinates and creates particles with random velocities to simulate the explosion effect.
        this.x = x; this.y = y; // Set the initial position of the explosion to the provided x and y coordinates. This is where the explosion will originate from when it is created.
        particles = new int[18][4]; // x,y,vx,vy
        for (int[] p : particles) {
            p[0] = x; p[1] = y;
            double a = rng.nextDouble() * Math.PI * 2; // Generate a random angle (a) for the particle's velocity direction. The angle is calculated by multiplying a random double between 0 and 1 by 2 * PI, which gives a value between 0 and 360 degrees in radians. This allows the particles to be emitted in all directions around the explosion center.
            double spd = 1 + rng.nextDouble() * 4; // Generate a random speed (spd) for the particle's velocity. The speed is calculated by adding 1 to a random double between 0 and 4, resulting in a speed range of 1 to 5. This adds variability to the explosion effect, with some particles moving faster than others.
            p[2] = (int)(Math.cos(a) * spd); // Calculate the x velocity (vx) of the particle based on the random angle (a) and speed (spd). The cosine of the angle is multiplied by the speed to determine how much the particle will move in the x direction each update.
            p[3] = (int)(Math.sin(a) * spd); // Calculate the y velocity (vy) of the particle based on the random angle (a) and speed (spd). The sine of the angle is multiplied by the speed to determine how much the particle will move in the y direction each update. This allows the particles to spread out in all directions from the explosion center, creating a more dynamic and visually appealing explosion effect.
        }
    }

    boolean alive() { return life > 0; } // Method to check if the explosion is still alive. The explosion is considered alive as long as its life is greater than 0.

    void update() { // Update method for the Explosion class. It decreases the life of the explosion and updates the position of each particle based on its velocity. The explosion is considered alive as long as its life is greater than 0.
        life--;
        for (int[] p : particles) {
            p[0] += p[2]; p[1] += p[3];
        }
    }

    void draw(Graphics2D g2) { // Draw method for the Explosion class. It renders the explosion on the screen by drawing each particle as a colored circle. The color and size of the particles fade over time based on the remaining life of the explosion, creating a visual effect of an explosion dissipating.
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
        if (life > 20) { // only show flash in the first 10 frames of explosion
            g2.setColor(new Color(255, 255, 200, (int)(ratio * 180)));
            int s = (int)((1 - ratio) * 30 + 5);
            g2.fillOval(x - s/2, y - s/2, s, s);
        }
    }
}
