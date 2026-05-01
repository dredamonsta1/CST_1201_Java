import java.util.List;
import java.util.Random;

class Enemy { // Enemy class represents the enemy ships in the game. It manages their position, movement, diving behavior, and shooting while diving. Each enemy has a type that can be used to differentiate between different enemy designs or behaviors.
    int x, y, type; // x and y represent the current position of the enemy on the screen. type can be used to determine the appearance or behavior of the enemy.
    int homeX, homeY; // homeX and homeY represent the original position of the enemy in the formation. When the enemy dives and then returns, it will return to this home position.
    boolean diving = false; // diving is a boolean flag that indicates whether the enemy is currently diving towards the player. When true, the enemy will move towards the player and then return to its home position after going off-screen.
    int frame = 0; // frame is used for animating the enemy sprite. It toggles between 0 and 1 to create a simple animation effect for the enemy ships.
    int frameTimer = 0; // frameTimer is used to control the timing of the animation frames. It increments each update and resets after reaching a certain threshold to switch the animation frame.
    // Diving path
    double dx, dy, angle; // dx and dy represent the velocity of the enemy in the x and y directions while diving. angle can be used to determine the orientation of the enemy during the dive, although in this implementation it is not used for rendering.
    int divePhase = 0; // divePhase is used to manage the different stages of the diving behavior. It starts at 0 when the enemy begins diving, changes to 1 when the enemy goes off-screen and starts returning to its home position, and resets to 0 when the enemy returns to its home position.
    int shootTimer = 0; // shootTimer is used to control the timing of the enemy's shooting while diving. It increments each update and resets after reaching a certain threshold to determine when the enemy should shoot a bullet towards the player.

    Enemy(int x, int y, int type) { // Constructor for the Enemy class. It initializes the enemy's position, type, and home position (the position it returns to after diving). The type parameter can be used to determine the appearance or behavior of the enemy.
        this.x = x; this.y = y;
        this.homeX = x; this.homeY = y;
        this.type = type;
    }

    void startDive(int px, int py) { // Method to start the diving behavior of the enemy. It calculates the target position based on the player's position (px, py) and sets the velocity (dx, dy) for the dive. The enemy will dive towards the player and then return to its home position after going off-screen.
        diving = true;
        divePhase = 0;
        double targetX = px + 20; // aim for the center of the player ship, which is 40 pixels wide, so add 20 to the player's x position to target the center.
        double targetY = py;
        double dist = Math.sqrt((targetX - x) * (targetX - x) + (targetY - y) * (targetY - y));
        dx = (targetX - x) / dist * 3.5; // gives consistent dive speed towards player @103.5 some enemis fall sideways and some go straight down so the speed is not the same for all enemies
        dy = Math.abs((targetY - y) / dist * 3.5) + 1.5;
        angle = 0;
    }

    void update(int px, int py, List<int[]> bullets, Random rng, int level) { // Update method for the Enemy class. It handles the animation of the enemy, the diving behavior, shooting while diving, and returning to formation after diving. The method takes the player's position (px, py), a list of bullets to add new bullets when shooting, a Random object for generating random numbers, and the current game level to adjust the shooting rate.
        // Frame animation
        frameTimer++;
        if (frameTimer > 20) { frameTimer = 0; frame = 1 - frame; }

        if (diving) { // If the enemy is diving, update its position based on its velocity (dx, dy) and apply a gravity effect to create a curved dive path. The enemy will also shoot bullets towards the player at random intervals while diving. If the enemy goes off-screen at the bottom, it will reset its position to the top and start returning to its home position. Once it returns to its home position, it will stop diving and return to formation.
            x += (int) dx; // Update the enemy's x position based on its horizontal velocity (dx). This moves the enemy left or right during the dive.
            y += (int) dy; // Update the enemy's y position based on its vertical velocity (dy). This moves the enemy downwards during the dive. The vertical velocity is increased by a small amount each update to create a gravity effect, making the dive path curved rather than straight.
            dy += 0.07; // gravity curve

            // Shoot while diving occasionally
            shootTimer++; // Increment the shoot timer to keep track of how long it has been since the last shot was fired. The enemy will shoot at random intervals based on the current game level, with a higher level resulting in more frequent shooting.
            int rate = Math.max(30, 90 - level * 8); // Calculate the shooting rate based on the current game level. The rate determines how often the enemy will shoot while diving. As the level increases, the rate decreases, making the enemy shoot more frequently. The minimum rate is set to 30 to prevent the enemy from shooting too rapidly at higher levels.
            if (shootTimer >= rate) { // If the shoot timer has reached or exceeded the calculated rate, the enemy will shoot a bullet towards the player. The shoot timer is then reset to 0 to start counting for the next shot. A new bullet is added to the bullets list with its initial position set to the center of the enemy ship (x + 18, y + 35) to create a more visually appealing shooting effect.
                shootTimer = 0; // Reset the shoot timer after shooting a bullet. This allows the enemy to shoot again after the next interval determined by the rate.
                if (rng.nextInt(3) == 0) bullets.add(new int[]{x + 18, y + 35}); // Randomly decide whether to shoot a bullet. The enemy has a 1 in 3 chance to shoot each time the shoot timer reaches the rate threshold, adding an element of unpredictability to the enemy's shooting behavior. The new bullet is added to the bullets list with its initial position set to the center of the enemy ship (x + 18, y + 35) for a more visually appealing shooting effect.
            }

            // If went off screen bottom, re-enter from top
            if (y > GamePanel.H + 20) { // If the enemy's y position exceeds the height of the game panel (GamePanel.H) plus an additional 20 pixels, it is considered to have gone off-screen at the bottom. In this case, the enemy's position is reset to the top of the screen (y = -60) and its x position is reset to its homeX position. The enemy then starts returning to its home position by setting diving to true and divePhase to 1, which will trigger the logic for returning to formation in the next updates.
                y = -60; // Reset the enemy's y position to -60, which places it above the top of the screen, allowing it to re-enter from the top during the next update cycle.
                x = homeX; // Reset the enemy's x position to its homeX position, which is the original position in the formation. This ensures that when the enemy returns to formation, it will return to its designated spot.
                dx = 0; // Reset the horizontal velocity (dx) to 0 since the enemy will no longer be diving towards the player and will instead return to its home position.
                dy = 2; // Set the vertical velocity (dy) to a constant value to control the speed at which the enemy returns to its home position. This creates a consistent return speed regardless of the distance from the home position.
                divePhase = 1; // Set the divePhase to 1 to indicate that the enemy is now in the phase of returning to formation. This will trigger the logic for moving towards the home position in the next updates until it reaches its home position and stops diving.
            }

            // Return to formation
            if (divePhase == 1) { // If the enemy is in divePhase 1, it means it is currently returning to its home position after diving. The enemy will move towards its home position (homeX, homeY) at a constant speed. If the enemy is within 5 pixels of its home position in both x and y directions, it will snap to the home position, stop diving, and reset the divePhase to 0, indicating that it has returned to formation and is no longer diving.   
                int tx = homeX, ty = homeY; // Set the target position (tx, ty) to the enemy's home position (homeX, homeY). This is the position that the enemy will move towards while returning to formation.
                if (Math.abs(x - tx) < 5 && Math.abs(y - ty) < 5) { // Check if the enemy is within 5 pixels of its home position in both x and y directions. If it is close enough to the home position, it will snap to the home position, stop diving, and reset the divePhase to 0.
                    x = tx; y = ty; diving = false; divePhase = 0; // Snap the enemy's position to its home position (tx, ty), set diving to false to indicate that it is no longer diving, and reset divePhase to 0 to indicate that it has returned to formation.
                } else {
                    x += Integer.signum(tx - x) * 2; // Move the enemy towards the target x position (tx) at a constant speed of 2 pixels per update. The Integer.signum function is used to determine the direction of movement (positive or negative) based on whether the target position is to the left or right of the current position.
                    y += Integer.signum(ty - y) * 2;// Move the enemy towards the target y position (ty) at a constant speed of 2 pixels per update. The Integer.signum function is used to determine the direction of movement (positive or negative) based on whether the target position is above or below the current position.
                }
            }
        }
    }
}
