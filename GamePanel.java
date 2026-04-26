import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List; 
import javax.swing.*;

class GamePanel extends JPanel implements ActionListener, KeyListener {
    static final int W = 800, H = 700; /*static final creates variables that are constants and 
                                         cannot be changed after initialization. 
                                         W is the width of the game panel. H is the height of the game panel. 
                                       */ 
    static final int PLAYER_SPEED = 5; /*player speed is the number of pixels the 
                                         player moves per frame when a movement key is pressed.
                                       */
    static final int BULLET_SPEED = 10; /*bullet speed is the number of pixels the player's bullets move 
                                          upwards per frame. Enemy bullets have a separate speed defined 
                                          in ENEMY_BULLET_SPEED. This allows for different speeds for player 
                                          and enemy projectiles, adding variety to the gameplay. 
                                        */
    static final int ENEMY_BULLET_SPEED = 4;/*enemy bullet speed is the number of pixels 
                                            the enemy's bullets move downwards per frame. 
                                            */
    static final int FPS = 60;/*frames per second  speeds up entire game if fps number is increased */

    // Game states
    enum State {
        MENU, PLAYING, GAME_OVER, WIN
    } /*State is an enumeration that defines 
        the different states the game can be in. 
       */

    State state = State.MENU; /*state is a variable that holds the current state of the game. 
                                It starts in the MENU state and changes based on player actions and game events. 
                              */
                            
    javax.swing.Timer timer; /*timer is a Swing Timer that triggers the actionPerformed method at regular 
                                intervals defined by FPS. This is the main game loop that updates the game 
                                state and repaints the screen. 
                             */                            
    Random rng = new Random(); /*rng is a Random object used for generating random numbers throughout the game, 
                                  such as for enemy behavior and explosion effects. 
                                */
    // Player
    int px = W / 2 - 20, py = H - 80; /*px and py are the x and y coordinates of the player's ship. 
                                        The player starts near the bottom center of the screen. 
                                        higher the number the closer the player i to the enemy
                                        */
    boolean left, right,
            shooting; /*left, right, and shooting are boolean variables that track whether the player 
                      is currently pressing the left arrow key, right arrow key, or space bar to shoot. 
                      These variables are updated in the keyPressed and keyReleased methods and used in 
                      the update method to control player movement and shooting. 
                      */
    int shootCooldown = 0; /*shootCooldown is an integer that tracks the cooldown time between player shots. 
                            When the player shoots, shootCooldown is set to a certain value (e.g., 18), 
                            and the player cannot shoot again until shootCooldown counts down to 0. 
                            This prevents the player from shooting too rapidly and adds a strategic element to timing shots. 
                          */
    int lives = 3; /* lives is an int that tracks the number of lives */
    int score = 0; /* score is an int that tracks the player's score */
    int level = 1; /* level is an int that tracks the current level */
    /*also the int are just variables that hold integer values, these names 
    could be anything but its good coding practice the give functional names */
    // Bullets
    List<int[]> playerBullets = new ArrayList<>();
    List<int[]> enemyBullets  = new ArrayList<>();

    // Enemies
    List<Enemy> enemies = new ArrayList<>();
    int enemyDir = 1;
    int enemyMoveTimer = 0;
    int enemyMoveInterval = 40;
    int enemyDiveTimer = 0;
    int enemyDiveCooldown = 180;

    // Stars for background
    int[][] stars = new int[120][3];

    // Explosions
    List<Explosion> explosions = new ArrayList<>();

    // Flash effect when hit
    int hitFlash = 0;

    // Entrance animation
    int entranceTimer = 180;

    GamePanel() {
        setPreferredSize(new Dimension(W, H));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        for (int i = 0; i < stars.length; i++) {
            stars[i][0] = rng.nextInt(W);
            stars[i][1] = rng.nextInt(H);
            stars[i][2] = rng.nextInt(3) + 1;
        }
        timer = new javax.swing.Timer(1000 / FPS, this);
        timer.start();
    }

    void spawnLevel() {
        enemies.clear();
        enemyBullets.clear();
        playerBullets.clear();
        explosions.clear();
        enemyDir = 01;
        enemyMoveInterval = Math.max(10, 40 - (level - 1) * 5); /* controlls the speed in which the enemy goes side to side, 
                                                                  smaller the number to the right of the comma the faster the 
                                                                  enemy goes, the larger the number to the left of the comma 
                                                                  the slower the enemy goes */
        entranceTimer = 180; /* controls the duration of the entrance animation */
        px = W / 2 - 20; /* controls the initial x position of the player */

        // 4 rows of enemies, 10 per row
        int cols = 10, rows = 4; /* controls the number of columns and rows of enemies */
        int startX = 80, startY = -200; /* controls the starting x and y position of the enemies, 
                                    lower the startY the higher the enemy starts, higher the startY the lower the enemy starts */
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int type = (r == 0) ? 2 : (r == 1) ? 1 : 0; /* controls the type of enemy in each row, 
                                                the top row will be type 2, the second row will be type 1, and the bottom two rows will be type 0 */
                enemies.add(new Enemy(startX + c * 65, startY - r * 55, type)); /* controls the spacing between enemies, 
                                                lower the number to the right of c the closer the enemies are horizontally, 
                                                higher the number to the right of r the closer the enemies are vertically */
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) { 
        if (state == State.PLAYING) update();
        repaint();
    }

    void update() {
        // Scroll stars
        for (int[] s : stars) {
            s[1] += s[2];
            if (s[1] > H) { s[1] = 0; s[0] = rng.nextInt(W); }
        }

        if (hitFlash > 0) hitFlash--; /* controls the duration of the hit flash effect */

        // Entrance animation
        if (entranceTimer > -50) { /* the 0 seems to control the start location of 
                                    the enemy, if set to -150 the enemy very close to player */
            entranceTimer--;
            for (Enemy en : enemies) en.y += 2;/* this enhanced for loop controls where enemy starts, 
                          lower the number higher the enemy starts, higher the lower and also starts 
                          ememy too low and ends game immediately */
            return; // don't do game logic yet
        }

        // Move player
        if (left && px > 10)
            px -= PLAYER_SPEED; /*the number to the right of the > operator is the 
                                amount of pixels the player can go to the left */
        if (right && px < W - 50)
            px += PLAYER_SPEED; /*the number to the far right controls the distance the player 
                                can move to the right side of screen, the lower the number the 
                                further to the right or pixel location.  
                                */

        // Shoot
        if (shooting && shootCooldown <= 0) {  //when shooting  && shot cool down is >= 0 there is no delay between shots 
            playerBullets.add(new int[]{px + 19, py}); /*the number to the right of px controls the x position of the bullet when shot, 
                                        lower the number the closer the bullet is to the left side of the ship, 
                                        higher the number the closer the bullet is to the right side of the ship */
            shootCooldown = 18; /*controls the cooldown time between shots, 
                                lower the number the faster the player can shoot, higher the number the slower the player can shoot */
        }
        if (shootCooldown > 0) shootCooldown--;

        // Move player bullets
        playerBullets.removeIf(b -> {
            b[1] -= BULLET_SPEED;
            return b[1] < 0;
        }); /*this is a lambda function in javascript would be called an arrow function, 
        b is the parameter that represents each bullet in the playerBullets list.
            */

        // Move enemy bullets
        enemyBullets.removeIf(b -> { b[1] += ENEMY_BULLET_SPEED; return b[1] > H; });

        // Move enemies left/right
        enemyMoveTimer++;
        if (enemyMoveTimer >= enemyMoveInterval) {
            enemyMoveTimer = 0;
            boolean hitWall = false;
            for (Enemy en : enemies) {
                if (!en.diving) {
                    en.x += enemyDir * 18;
                    if (en.x <= 20 || en.x >= W - 60) hitWall = true;
                }
            }
            if (hitWall) {
                enemyDir *= -1;
                for (Enemy en : enemies) { if (!en.diving) en.y += 12; }
            }
        }

        // Update diving enemies
        for (Enemy en : enemies) en.update(px, py, enemyBullets, rng, level);

        // Enemy dive trigger
        enemyDiveTimer++;
        if (enemyDiveTimer >= enemyDiveCooldown && !enemies.isEmpty()) {
            enemyDiveTimer = 0;
            enemyDiveCooldown = Math.max(60, 180 - level * 15);
            List<Enemy> candidates = new ArrayList<>();
            for (Enemy en : enemies) if (!en.diving) candidates.add(en);
            if (!candidates.isEmpty()) {
                candidates.get(rng.nextInt(candidates.size())).startDive(px, py);
            }
        }

        // Enemy random shooting
        if (!enemies.isEmpty() && rng.nextInt(120) == 0) {
            Enemy shooter = enemies.get(rng.nextInt(enemies.size()));
            enemyBullets.add(new int[]{shooter.x + 18, shooter.y + 35});
        }

        // Collision: player bullets vs enemies
        Iterator<int[]> bi = playerBullets.iterator();
        while (bi.hasNext()) {
            int[] b = bi.next();
            Iterator<Enemy> ei = enemies.iterator();
            while (ei.hasNext()) {
                Enemy en = ei.next();
                if (b[0] >= en.x && b[0] <= en.x + 36 && b[1] >= en.y && b[1] <= en.y + 35) {
                    explosions.add(new Explosion(en.x + 18, en.y + 17));
                    int pts = en.type == 2 ? 150 : en.type == 1 ? 100 : 80;
                    if (en.diving) pts *= 2;
                    score += pts;
                    ei.remove();
                    bi.remove();
                    break;
                }
            }
        }

        // Collision: enemy bullets vs player
        Iterator<int[]> ebi = enemyBullets.iterator();
        while (ebi.hasNext()) {
            int[] b = ebi.next();
            if (b[0] >= px && b[0] <= px + 40 && b[1] >= py && b[1] <= py + 40) {
                ebi.remove();
                loseLife();
            }
        }

        // Collision: diving enemies vs player
        for (Enemy en : enemies) {
            if (en.x + 10 < px + 38 && en.x + 26 > px &&
                en.y + 10 < py + 38 && en.y + 26 > py) {
                explosions.add(new Explosion(en.x + 18, en.y + 17));
                enemies.remove(en);
                loseLife();
                break;
            }
        }

        // Update explosions
        explosions.removeIf(ex -> !ex.alive());
        for (Explosion ex : explosions) ex.update();

        // Win condition
        if (enemies.isEmpty() && explosions.isEmpty()) {
            level++;
            spawnLevel();
        }

        // Enemies reach bottom
        for (Enemy en : enemies) {
            if (en.y > H - 60 && !en.diving) {
                state = State.GAME_OVER;
                return;
            }
        }
    }

    void loseLife() {
        lives--;
        hitFlash = 45;
        if (lives <= 0) state = State.GAME_OVER;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, W, H);

        // Stars
        for (int[] s : stars) {
            int bright = 120 + s[2] * 40;
            g2.setColor(new Color(bright, bright, bright));
            g2.fillOval(s[0], s[1], s[2], s[2]);
        }

        if (state == State.MENU) {
            drawMenu(g2);
        } else if (state == State.PLAYING || state == State.WIN) {
            drawGame(g2);
        } else if (state == State.GAME_OVER) {
            drawGame(g2);
            drawGameOver(g2);
        }
    }

    void drawMenu(Graphics2D g2) {
        // Title
        g2.setFont(new Font("Courier New", Font.BOLD, 72));
        String title = "GALAGA";
        FontMetrics fm = g2.getFontMetrics();
        int tx = (W - fm.stringWidth(title)) / 2;
        // Glow effect
        g2.setColor(new Color(255, 80, 0, 60));
        g2.drawString(title, tx - 3, 220);
        g2.drawString(title, tx + 3, 220);
        g2.setColor(new Color(255, 180, 0));
        g2.drawString(title, tx, 220);

        // Subtitle
        g2.setFont(new Font("Courier New", Font.BOLD, 18));
        g2.setColor(new Color(0, 220, 255));
        String sub = "DEFEND THE GALAXY";
        g2.drawString(sub, (W - g2.getFontMetrics().stringWidth(sub)) / 2, 270);

        // Blinking start
        if ((System.currentTimeMillis() / 600) % 2 == 0) {
            g2.setFont(new Font("Courier New", Font.BOLD, 22));
            g2.setColor(Color.WHITE);
            String s = "PRESS ENTER TO START";
            g2.drawString(s, (W - g2.getFontMetrics().stringWidth(s)) / 2, 370);
        }

        // Controls
        g2.setFont(new Font("Courier New", Font.PLAIN, 15));
        g2.setColor(new Color(180, 180, 180));
        String[] ctrl = {"← → : MOVE", "SPACE : FIRE"};
        for (int i = 0; i < ctrl.length; i++) {
            g2.drawString(ctrl[i], (W - g2.getFontMetrics().stringWidth(ctrl[i])) / 2, 440 + i * 28);
        }

        // Draw sample enemy ship types as preview
        drawEnemyType(g2, W/2 - 130, 530, 2, (System.currentTimeMillis() / 500) % 2 == 0);
        drawEnemyType(g2, W/2 - 20,  530, 1, false);
        drawEnemyType(g2, W/2 + 90,  530, 0, false);

        g2.setFont(new Font("Courier New", Font.PLAIN, 13));
        g2.setColor(new Color(255, 220, 80));
        g2.drawString("150", W/2 - 128, 580);
        g2.setColor(new Color(80, 200, 255));
        g2.drawString("100", W/2 - 18, 580);
        g2.setColor(new Color(80, 255, 120));
        g2.drawString("80",  W/2 + 92, 580);
    }

    void drawGame(Graphics2D g2) {
        // HUD
        g2.setFont(new Font("Courier New", Font.BOLD, 18));
        g2.setColor(new Color(255, 220, 0));
        g2.drawString("SCORE: " + score, 20, 30);
        g2.setColor(new Color(0, 220, 255));
        g2.drawString("LEVEL: " + level, W / 2 - 40, 30);
        g2.setColor(new Color(255, 80, 80));
        g2.drawString("LIVES: " + "♥ ".repeat(lives), W - 160, 30); /*  */

        // Divider line
        g2.setColor(new Color(50, 50, 80));
        g2.drawLine(0, 40, W, 40);

        // Player ship (if alive)
        if (lives > 0) {
            if (hitFlash > 0 && (hitFlash / 5) % 2 == 0) {
                // Flicker on hit
            } else {
                drawPlayer(g2, px, py);
            }
        }

        // Player bullets
        for (int[] b : playerBullets) {
            g2.setColor(new Color(255, 255, 100));
            g2.fillRect(b[0] - 2, b[1], 5, 16);
            g2.setColor(new Color(255, 200, 0, 120));
            g2.fillRect(b[0] - 4, b[1], 9, 20);
        }

        // Enemy bullets
        for (int[] b : enemyBullets) {
            g2.setColor(new Color(255, 60, 60));
            g2.fillRect(b[0] - 2, b[1], 5, 14);
            g2.setColor(new Color(255, 0, 0, 80));
            g2.fillRect(b[0] - 4, b[1] - 2, 9, 18);
        }

        // Enemies
        for (Enemy en : enemies) drawEnemyType(g2, en.x, en.y, en.type, en.frame == 1);

        // Explosions
        for (Explosion ex : explosions) ex.draw(g2);

        // Screen flash on hit
        if (hitFlash > 0) {
            int alpha = Math.min(80, hitFlash * 3); /*larger thehitFlach * number thelonger the screen flashes red */
            g2.setColor(new Color(255, 80, 80, alpha)); /* controls the color of the flash */
            g2.fillRect(0, 0, W, H); /*the numbers control section of screen that will be filled */
        }
    }

    void drawPlayer(Graphics2D g2, int x, int y) {
        // Body
        int[] bx = {x+20, x+5,  x+2,  x+18, x+22, x+38, x+35};/*control look of the ships body x-coordinates */
        int[] by = {y,    y+14, y+38, y+38, y+38, y+38, y+14}; /* this is the y-coordinates for the ship's body */
        g2.setColor(new Color(30, 140, 255));
        g2.fillPolygon(bx, by, 7);

        // Cockpit
        g2.setColor(new Color(120, 220, 255));
        g2.fillOval(x + 12, y + 6, 16, 18);
        g2.setColor(new Color(200, 240, 255, 180));
        g2.fillOval(x + 15, y + 8, 8, 10);

        // Wing highlights
        g2.setColor(new Color(60, 180, 255));
        g2.fillPolygon(new int[]{x+5, x+2, x+18}, new int[]{y+14, y+38, y+38}, 3);
        g2.fillPolygon(new int[]{x+35, x+38, x+22}, new int[]{y+14, y+38, y+38}, 3);

        // Engine glow
        if ((System.currentTimeMillis() / 80) % 2 == 0) {
            g2.setColor(new Color(255, 140, 0, 200));
            g2.fillOval(x + 13, y + 36, 14, 10);
        } else {
            g2.setColor(new Color(255, 200, 50, 150));
            g2.fillOval(x + 15, y + 37, 10, 7);
        }

        // Cannon
        g2.setColor(new Color(180, 200, 255));
        g2.fillRect(x + 17, y - 6, 6, 10);
    }

    void drawEnemyType(Graphics2D g2, int x, int y, int type, boolean frame2) {
        if (type == 2) {
            drawBossEnemy(g2, x, y, frame2);
        } else if (type == 1) {
            drawBeeEnemy(g2, x, y, frame2);
        } else {
            drawGruntEnemy(g2, x, y, frame2);
        }
    }

    void drawBossEnemy(Graphics2D g2, int x, int y, boolean f) {
        g2.setColor(new Color(255, 80, 20));
        int[] bx = {x+18, x+8, x+5, x+18, x+31, x+29};
        int[] by = {y+5, y+18, y+35, y+28, y+35, y+18};
        g2.fillPolygon(bx, by, 6);
        g2.setColor(new Color(255, 160, 0));
        if (!f) {
            g2.fillOval(x, y+8, 20, 14);
            g2.fillOval(x+16, y+8, 20, 14);
        } else {
            g2.fillOval(x-2, y+12, 20, 10);
            g2.fillOval(x+18, y+12, 20, 10);
        }
        g2.setColor(new Color(255, 255, 100));
        g2.fillOval(x+12, y+8, 6, 6);
        g2.fillOval(x+18, y+8, 6, 6);
        g2.setColor(Color.BLACK);
        g2.fillOval(x+14, y+10, 3, 3);
        g2.fillOval(x+20, y+10, 3, 3);
    }

    void drawBeeEnemy(Graphics2D g2, int x, int y, boolean f) {
        g2.setColor(new Color(30, 180, 255));
        int[] bx = {x+18, x+10, x+8, x+18, x+28, x+26};
        int[] by = {y+4, y+16, y+34, y+26, y+34, y+16};
        g2.fillPolygon(bx, by, 6);
        g2.setColor(new Color(0, 100, 200));
        if (!f) {
            g2.fillOval(x+2, y+10, 16, 12);
            g2.fillOval(x+18, y+10, 16, 12);
        } else {
            g2.fillOval(x+3, y+14, 14, 8);
            g2.fillOval(x+19, y+14, 14, 8);
        }
        g2.setColor(new Color(200, 240, 255));
        g2.fillOval(x+13, y+6, 5, 5);
        g2.fillOval(x+18, y+6, 5, 5);
        g2.setColor(Color.BLACK);
        g2.fillOval(x+14, y+8, 2, 2);
        g2.fillOval(x+19, y+8, 2, 2);
    }

    void drawGruntEnemy(Graphics2D g2, int x, int y, boolean f) {
        g2.setColor(new Color(40, 200, 80));
        int[] bx = {x+18, x+12, x+10, x+18, x+26, x+24};
        int[] by = {y+5, y+15, y+30, y+24, y+30, y+15};
        g2.fillPolygon(bx, by, 6);
        g2.setColor(new Color(20, 130, 50));
        if (!f) {
            g2.fillOval(x+4, y+12, 14, 10);
            g2.fillOval(x+18, y+12, 14, 10);
        } else {
            g2.fillOval(x+5, y+15, 12, 7);
            g2.fillOval(x+19, y+15, 12, 7);
        }
        g2.setColor(new Color(180, 255, 180));
        g2.fillOval(x+13, y+7, 5, 5);
        g2.fillOval(x+18, y+7, 5, 5);
        g2.setColor(Color.BLACK);
        g2.fillOval(x+14, y+9, 2, 2);
        g2.fillOval(x+19, y+9, 2, 2);
    }

    void drawGameOver(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, W, H);

        g2.setFont(new Font("Courier New", Font.BOLD, 58));
        String go = "GAME OVER";
        FontMetrics fm = g2.getFontMetrics();
        int tx = (W - fm.stringWidth(go)) / 2;
        g2.setColor(new Color(255, 40, 40));
        g2.drawString(go, tx, 300);

        g2.setFont(new Font("Courier New", Font.BOLD, 26));
        String sc = "FINAL SCORE: " + score;
        fm = g2.getFontMetrics();
        g2.setColor(new Color(255, 220, 0));
        g2.drawString(sc, (W - fm.stringWidth(sc)) / 2, 360);

        if ((System.currentTimeMillis() / 600) % 2 == 0) {
            g2.setFont(new Font("Courier New", Font.BOLD, 20));
            g2.setColor(Color.WHITE);
            String r = "PRESS ENTER TO PLAY AGAIN";
            fm = g2.getFontMetrics();
            g2.drawString(r, (W - fm.stringWidth(r)) / 2, 430);
        }
    }

    @Override public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (state == State.MENU) {
            if (k == KeyEvent.VK_ENTER) {
                state = State.PLAYING;
                score = 0; lives = 3; level = 1; /*control the number of lives initially as the game starts */
                spawnLevel();
            }
        } else if (state == State.PLAYING) {
            if (k == KeyEvent.VK_LEFT)  left = true;
            if (k == KeyEvent.VK_RIGHT) right = true;
            if (k == KeyEvent.VK_SPACE) shooting = true;
        } else if (state == State.GAME_OVER) {
            if (k == KeyEvent.VK_ENTER) {
                state = State.PLAYING;
                score = 0; lives = 3; level = 1; /*control the number of lives after game over and restarts */
                spawnLevel();
            }
        }
    }
    @Override public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_LEFT)  left = false;
        if (k == KeyEvent.VK_RIGHT) right = false;
        if (k == KeyEvent.VK_SPACE) shooting = false;
    }
    @Override public void keyTyped(KeyEvent e) {}
}
