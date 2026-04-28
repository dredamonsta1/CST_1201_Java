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
        MENU, SELECT, PLAYING, GAME_OVER, WIN
    } /*State is an enumeration that defines
        the different states the game can be in.
        SELECT is the player count selection screen shown after the main menu.
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

    // Player 1
    int px = W / 2 - 60, py = H - 80; /*px and py are the x and y coordinates of player 1's ship.
                                        Player 1 starts left of center near the bottom of the screen.
                                        higher the number the closer the player is to the enemy
                                        */
    boolean left, right,
            shooting; /*left, right, and shooting are boolean variables that track whether player 1
                      is currently pressing the left arrow key, right arrow key, or space bar to shoot.
                      These variables are updated in the keyPressed and keyReleased methods and used in
                      the update method to control player movement and shooting.
                      */
    int shootCooldown = 0; /*shootCooldown is an integer that tracks the cooldown time between player 1's shots.
                            When the player shoots, shootCooldown is set to a certain value (e.g., 18),
                            and the player cannot shoot again until shootCooldown counts down to 0.
                            This prevents the player from shooting too rapidly and adds a strategic element to timing shots.
                          */
    int lives = 3; /* lives is an int that tracks the number of lives for player 1 */
    int score = 0; /* score is an int that tracks player 1's score */

    // Player 2
    int px2 = W / 2 + 20, py2 = H - 80; /*px2 and py2 are the x and y coordinates of player 2's ship.
                                           Player 2 starts right of center near the bottom of the screen,
                                           beside player 1. Same py as player 1 so both ships are level.
                                          */
    boolean left2, right2,
            shooting2; /*left2, right2, and shooting2 track whether player 2 is pressing
                         the A key (left), D key (right), or W key (shoot).
                         Same mechanic as player 1's boolean flags.
                        */
    int shootCooldown2 = 0; /*shootCooldown2 tracks the cooldown between player 2's shots.
                              Same mechanic as shootCooldown — prevents rapid fire.
                             */
    int lives2 = 3; /* lives2 tracks the number of lives for player 2 */
    int score2 = 0; /* score2 tracks player 2's score separately from player 1 */

    int playerCount = 1; /* playerCount holds the number of human players (1 or 2).
                           Set on the SELECT screen before the game starts.
                           Controls whether P2 logic runs during gameplay. */

    int level = 1; /* level is an int that tracks the current level */
    /*also the int are just variables that hold integer values, these names
    could be anything but its good coding practice the give functional names */

    // Bullets — shared pool since both players shoot at the same enemies.
    // Each bullet is stored as {x, y, playerNum} where playerNum is 1 or 2
    // so we know which player earns the score when their bullet hits an enemy.
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

    // Flash effect when hit — one per player so each hit only flashes for the player that was struck
    int hitFlash = 0;
    int hitFlash2 = 0; /* hitFlash2 is the screen flash timer for player 2, same mechanic as hitFlash */

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
        enemyDir = 1;
        enemyMoveInterval = Math.max(10, 40 - (level - 1) * 5); /* controlls the speed in which the enemy goes side to side,
                                                                  smaller the number to the right of the comma the faster the
                                                                  enemy goes, the larger the number to the left of the comma
                                                                  the slower the enemy goes */
        entranceTimer = 180; /* controls the duration of the entrance animation */
        px  = W / 2 - 60; /* player 1 resets to the left of center each level */
        px2 = W / 2 + 20; /* player 2 resets to the right of center each level */

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

        if (hitFlash > 0) hitFlash--;   /* controls the duration of the hit flash effect for player 1 */
        if (hitFlash2 > 0) hitFlash2--; /* controls the duration of the hit flash effect for player 2 */

        // Entrance animation
        if (entranceTimer > -50) { /* the 0 seems to control the start location of
                                    the enemy, if set to -150 the enemy very close to player */
            entranceTimer--;
            for (Enemy en : enemies) en.y += 2;/* this enhanced for loop controls where enemy starts,
                          lower the number higher the enemy starts, higher the lower and also starts
                          ememy too low and ends game immediately */
            return; // don't do game logic yet
        }

        // Move player 1
        if (left && px > 10)
            px -= PLAYER_SPEED; /*the number to the right of the > operator is the
                                amount of pixels the player can go to the left */
        if (right && px < W - 50)
            px += PLAYER_SPEED; /*the number to the far right controls the distance the player
                                can move to the right side of screen, the lower the number the
                                further to the right or pixel location.
                                */

        // Move player 2 (only in 2-player mode and only if still alive)
        if (playerCount == 2 && lives2 > 0) {
            if (left2  && px2 > 10)     px2 -= PLAYER_SPEED; /* A key moves player 2 left */
            if (right2 && px2 < W - 50) px2 += PLAYER_SPEED; /* D key moves player 2 right */
        }

        // Player 1 shoot
        if (lives > 0 && shooting && shootCooldown <= 0) {  //when shooting  && shot cool down is >= 0 there is no delay between shots
            playerBullets.add(new int[]{px + 19, py, 1}); /*the number to the right of px controls the x position of the bullet when shot,
                                        lower the number the closer the bullet is to the left side of the ship,
                                        higher the number the closer the bullet is to the right side of the ship.
                                        The 1 at the end marks this bullet as belonging to player 1 for scoring. */
            shootCooldown = 18; /*controls the cooldown time between shots,
                                lower the number the faster the player can shoot, higher the number the slower the player can shoot */
        }
        if (shootCooldown > 0) shootCooldown--;

        // Player 2 shoot (only in 2-player mode and only if still alive)
        if (playerCount == 2 && lives2 > 0 && shooting2 && shootCooldown2 <= 0) {
            playerBullets.add(new int[]{px2 + 19, py2, 2}); /* W key fires player 2's bullet.
                                                                The 2 marks this bullet as belonging to player 2 for scoring. */
            shootCooldown2 = 18; /* same cooldown mechanic as player 1 */
        }
        if (playerCount == 2 && shootCooldown2 > 0) shootCooldown2--;

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
            for (Enemy en : enemies) { // only move non-diving enemies left/right
                if (!en.diving) {
                    en.x += enemyDir * 18;
                    if (en.x <= 20 || en.x >= W - 60) hitWall = true;
                }
            }
            if (hitWall) {
                enemyDir *= -1;
                for (Enemy en : enemies) { if (!en.diving) en.y += 12; } // when hitting wall move enemies down a row
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
            for (Enemy en : enemies) if (!en.diving) candidates.add(en); // only non-diving enemies can start diving
            if (!candidates.isEmpty()) {
                // Pick which player to dive at.
                // In 1-player mode always target P1.
                // In 2-player mode randomly pick an alive player so both stay under threat.
                int targetX, targetY;
                if (playerCount == 2 && lives > 0 && lives2 > 0) {
                    if (rng.nextBoolean()) { targetX = px;  targetY = py;  }
                    else                   { targetX = px2; targetY = py2; }
                } else if (playerCount == 2 && lives2 > 0 && lives <= 0) {
                    targetX = px2; targetY = py2; /* only player 2 alive, target them */
                } else {
                    targetX = px;  targetY = py;  /* 1-player mode, or only player 1 alive */
                }
                candidates.get(rng.nextInt(candidates.size())).startDive(targetX, targetY);
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
                    // Award points to whichever player fired the bullet — b[2] holds the player number
                    if (b[2] == 1) score  += pts;
                    else           score2 += pts;
                    ei.remove();
                    bi.remove();
                    break;
                }
            }
        }

        // Collision: enemy bullets vs players
        Iterator<int[]> ebi = enemyBullets.iterator();
        while (ebi.hasNext()) {
            int[] b = ebi.next();
            boolean hit = false;
            // Check player 1 first (only if still alive)
            if (lives > 0 && b[0] >= px && b[0] <= px + 40 && b[1] >= py && b[1] <= py + 40) {
                ebi.remove();
                loseLife1();
                hit = true;
            }
            // Check player 2 (only in 2-player mode, only if still alive, and bullet not already consumed)
            if (!hit && playerCount == 2 && lives2 > 0 && b[0] >= px2 && b[0] <= px2 + 40 && b[1] >= py2 && b[1] <= py2 + 40) {
                ebi.remove();
                loseLife2();
            }
        }

        // Collision: diving enemies vs players
        for (Enemy en : enemies) { // check collision with player for diving enemies
            // Check against player 1
            if (lives > 0 &&
                en.x + 10 < px + 38 && en.x + 26 > px &&
                en.y + 10 < py + 38 && en.y + 26 > py) {
                explosions.add(new Explosion(en.x + 18, en.y + 17));
                enemies.remove(en);
                loseLife1();
                break;
            }
            // Check against player 2 (only in 2-player mode)
            if (playerCount == 2 && lives2 > 0 &&
                en.x + 10 < px2 + 38 && en.x + 26 > px2 &&
                en.y + 10 < py2 + 38 && en.y + 26 > py2) {
                explosions.add(new Explosion(en.x + 18, en.y + 17));
                enemies.remove(en);
                loseLife2();
                break;
            }
        }

        // Update explosions
        explosions.removeIf(ex -> !ex.alive());
        for (Explosion ex : explosions) ex.update(); // updatesd explosion animation frames and size

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

    void loseLife1() {
        lives--;
        hitFlash = 45;
        /* in 1-player mode the game ends when P1 runs out of lives.
           in 2-player mode the game ends only when BOTH players are out of lives. */
        if (lives <= 0 && (playerCount == 1 || lives2 <= 0)) state = State.GAME_OVER;
    }

    void loseLife2() {
        lives2--;
        hitFlash2 = 45;
        /* game ends only when BOTH players are out of lives — if one dies the other keeps playing */
        if (lives <= 0 && lives2 <= 0) state = State.GAME_OVER;
    }

    @Override // this annotation indicates that we are overriding the paintComponent method from the JPanel class
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
        } else if (state == State.SELECT) {
            drawSelect(g2); /* player count selection screen shown after the main menu */
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

        // Controls hint — full per-player controls are shown on the SELECT screen
        g2.setFont(new Font("Courier New", Font.PLAIN, 15));
        g2.setColor(new Color(180, 180, 180));
        String[] ctrl = {"\u2190 \u2192 : MOVE", "SPACE : FIRE"};
        for (int i = 0; i < ctrl.length; i++) {
            g2.drawString(ctrl[i], (W - g2.getFontMetrics().stringWidth(ctrl[i])) / 2, 430 + i * 28);
        }

        // Draw sample enemy ship types as preview
        drawEnemyType(g2, W/2 - 130, 560, 2, (System.currentTimeMillis() / 500) % 2 == 0);
        drawEnemyType(g2, W/2 - 20,  560, 1, false);
        drawEnemyType(g2, W/2 + 90,  560, 0, false);

        g2.setFont(new Font("Courier New", Font.PLAIN, 13));
        g2.setColor(new Color(255, 220, 80));
        g2.drawString("150", W/2 - 128, 610);
        g2.setColor(new Color(80, 200, 255));
        g2.drawString("100", W/2 - 18, 610);
        g2.setColor(new Color(80, 255, 120));
        g2.drawString("80",  W/2 + 92, 610);
    }

    void drawSelect(Graphics2D g2) {
        /* draws the player count selection screen.
           playerCount holds the current highlighted choice (1 or 2).
           Left/Right arrow keys or 1/2 keys change the selection.
           Enter confirms and starts the game. */

        // Title
        g2.setFont(new Font("Courier New", Font.BOLD, 38));
        String title = "SELECT PLAYERS";
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(new Color(0, 220, 255));
        g2.drawString(title, (W - fm.stringWidth(title)) / 2, 200);

        // Divider under title
        g2.setColor(new Color(50, 50, 80));
        g2.drawLine(W/2 - 200, 215, W/2 + 200, 215);

        // Option boxes — highlight the selected one brighter
        int opt1X = W/2 - 220, opt2X = W/2 + 40, optY = 300;

        // 1 PLAYER option
        boolean p1sel = (playerCount == 1); /* true when 1-player is the current selection */
        g2.setColor(p1sel ? new Color(30, 140, 255) : new Color(60, 60, 90)); /* bright blue if selected, dim if not */
        g2.fillRoundRect(opt1X, optY - 40, 160, 80, 12, 12);
        g2.setColor(p1sel ? Color.WHITE : new Color(120, 120, 140));
        g2.setFont(new Font("Courier New", Font.BOLD, 22));
        fm = g2.getFontMetrics();
        g2.drawString("1 PLAYER", opt1X + (160 - fm.stringWidth("1 PLAYER")) / 2, optY + 8);

        // 2 PLAYERS option
        boolean p2sel = (playerCount == 2); /* true when 2-player is the current selection */
        g2.setColor(p2sel ? new Color(200, 140, 0) : new Color(60, 60, 90)); /* bright gold if selected, dim if not */
        g2.fillRoundRect(opt2X, optY - 40, 160, 80, 12, 12);
        g2.setColor(p2sel ? Color.WHITE : new Color(120, 120, 140));
        g2.setFont(new Font("Courier New", Font.BOLD, 22));
        fm = g2.getFontMetrics();
        g2.drawString("2 PLAYERS", opt2X + (160 - fm.stringWidth("2 PLAYERS")) / 2, optY + 8);

        // Arrow indicator under the selected box
        g2.setFont(new Font("Courier New", Font.BOLD, 18));
        g2.setColor(Color.WHITE);
        String arrow = "\u25B2"; /* up-pointing triangle acts as a selection pointer */
        fm = g2.getFontMetrics();
        if (p1sel) g2.drawString(arrow, opt1X + (160 - fm.stringWidth(arrow)) / 2, optY + 55);
        else       g2.drawString(arrow, opt2X + (160 - fm.stringWidth(arrow)) / 2, optY + 55);

        // Navigation hint
        g2.setFont(new Font("Courier New", Font.PLAIN, 14));
        g2.setColor(new Color(180, 180, 180));
        String nav = "\u2190 \u2192  OR  1 / 2  TO SWITCH";
        fm = g2.getFontMetrics();
        g2.drawString(nav, (W - fm.stringWidth(nav)) / 2, optY + 90);

        // Controls preview — show only relevant controls for the chosen mode
        g2.setFont(new Font("Courier New", Font.PLAIN, 14));
        int ctrlY = 460;
        g2.setColor(new Color(30, 140, 255)); /* P1 controls always shown in blue */
        String[] p1ctrl = {"P1:  \u2190 \u2192 MOVE   SPACE FIRE"};
        for (int i = 0; i < p1ctrl.length; i++)
            g2.drawString(p1ctrl[i], (W - g2.getFontMetrics().stringWidth(p1ctrl[i])) / 2, ctrlY + i * 24);

        if (playerCount == 2) {
            /* P2 controls only appear when 2-player mode is selected */
            g2.setColor(new Color(255, 180, 0)); /* P2 controls in gold */
            String[] p2ctrl = {"P2:  A D MOVE   W FIRE"};
            for (int i = 0; i < p2ctrl.length; i++)
                g2.drawString(p2ctrl[i], (W - g2.getFontMetrics().stringWidth(p2ctrl[i])) / 2, ctrlY + 28 + i * 24);
        }

        // Blinking confirm prompt
        if ((System.currentTimeMillis() / 600) % 2 == 0) {
            g2.setFont(new Font("Courier New", Font.BOLD, 20));
            g2.setColor(Color.WHITE);
            String s = "PRESS ENTER TO CONFIRM";
            fm = g2.getFontMetrics();
            g2.drawString(s, (W - fm.stringWidth(s)) / 2, 560);
        }
    }

    void drawGame(Graphics2D g2) {
        // HUD — layout adapts to player count.
        // In 1-player mode: score on left, level center, lives on right.
        // In 2-player mode: P1 info left, level center, P2 info right.
        g2.setFont(new Font("Courier New", Font.BOLD, 16));
        FontMetrics fm = g2.getFontMetrics();

        if (playerCount == 1) {
            /* 1-player HUD — simpler layout matching the original single-player style */
            g2.setColor(new Color(255, 220, 0));
            g2.drawString("SCORE: " + score, 20, 30);
            g2.setColor(new Color(255, 80, 80));
            String livesStr = "LIVES: " + (lives > 0 ? "\u2665 ".repeat(lives).trim() : "---");
            g2.drawString(livesStr, W - fm.stringWidth(livesStr) - 20, 30);
        } else {
            /* 2-player HUD */
            g2.setColor(new Color(30, 140, 255)); /* player 1 HUD drawn in blue to match their ship */
            g2.drawString("P1 SCORE: " + score, 10, 20);
            g2.drawString("P1: " + (lives > 0 ? "\u2665 ".repeat(lives).trim() : "DEAD"), 10, 38);

            g2.setColor(new Color(255, 180, 0)); /* player 2 HUD drawn in gold to match their ship */
            String p2score = "P2 SCORE: " + score2;
            String p2lives = "P2: " + (lives2 > 0 ? "\u2665 ".repeat(lives2).trim() : "DEAD");
            g2.drawString(p2score, W - fm.stringWidth(p2score) - 10, 20);
            g2.drawString(p2lives, W - fm.stringWidth(p2lives) - 10, 38);
        }

        g2.setColor(new Color(0, 220, 255));
        String lvl = "LEVEL: " + level;
        g2.drawString(lvl, (W - fm.stringWidth(lvl)) / 2, 30);

        // Divider line
        g2.setColor(new Color(50, 50, 80));
        g2.drawLine(0, 45, W, 45);

        // Player 1 ship (if alive)
        if (lives > 0) {
            if (hitFlash > 0 && (hitFlash / 5) % 2 == 0) {
                // Flicker on hit — skip drawing this frame to create the flicker effect
            } else {
                drawPlayer(g2, px, py, false); /* false = player 1, draws in blue */
            }
        }

        // Player 2 ship (only in 2-player mode and only if alive)
        if (playerCount == 2 && lives2 > 0) {
            if (hitFlash2 > 0 && (hitFlash2 / 5) % 2 == 0) {
                // Flicker on hit — skip drawing this frame to create the flicker effect
            } else {
                drawPlayer(g2, px2, py2, true); /* true = player 2, draws in gold */
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
        if (playerCount == 2 && hitFlash2 > 0) {
            int alpha = Math.min(80, hitFlash2 * 3); /* same flash mechanic for player 2 */
            g2.setColor(new Color(255, 80, 80, alpha));
            g2.fillRect(0, 0, W, H);
        }
    }

    void drawPlayer(Graphics2D g2, int x, int y, boolean isP2) {
        /* isP2 controls the color scheme:
           false = player 1 (blue), true = player 2 (gold).
           All coordinates are the same — only colors differ. */

        // Body
        int[] bx = {x+20, x+5,  x+2,  x+18, x+22, x+38, x+35};/*control look of the ships body x-coordinates */
        int[] by = {y,    y+14, y+38, y+38, y+38, y+38, y+14}; /* this is the y-coordinates for the ship's body */
        g2.setColor(isP2 ? new Color(200, 140, 0) : new Color(30, 140, 255)); /* player 1 is blue, player 2 is gold */
        g2.fillPolygon(bx, by, 7);

        // Cockpit
        g2.setColor(isP2 ? new Color(255, 220, 80) : new Color(120, 220, 255));
        g2.fillOval(x + 12, y + 6, 16, 18);
        g2.setColor(isP2 ? new Color(255, 245, 190, 180) : new Color(200, 240, 255, 180));
        g2.fillOval(x + 15, y + 8, 8, 10);

        // Wing highlights
        g2.setColor(isP2 ? new Color(255, 180, 30) : new Color(60, 180, 255));
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
        g2.setColor(isP2 ? new Color(255, 220, 150) : new Color(180, 200, 255));
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
        g2.drawString(go, tx, 260);

        g2.setFont(new Font("Courier New", Font.BOLD, 24));
        fm = g2.getFontMetrics();

        if (playerCount == 1) {
            /* 1-player game over — show single final score */
            g2.setColor(new Color(255, 220, 0));
            String sc = "FINAL SCORE: " + score;
            g2.drawString(sc, (W - fm.stringWidth(sc)) / 2, 350);
        } else {
            /* 2-player game over — show each player's score and declare a winner */
            g2.setColor(new Color(30, 140, 255)); /* player 1 score in blue */
            String p1sc = "P1 SCORE: " + score;
            g2.drawString(p1sc, (W - fm.stringWidth(p1sc)) / 2, 325);

            g2.setColor(new Color(255, 180, 0)); /* player 2 score in gold */
            String p2sc = "P2 SCORE: " + score2;
            g2.drawString(p2sc, (W - fm.stringWidth(p2sc)) / 2, 360);

            // Winner callout — compares both scores to declare a winner
            g2.setFont(new Font("Courier New", Font.BOLD, 22));
            fm = g2.getFontMetrics();
            String winner = (score > score2) ? "P1 WINS!" : (score2 > score) ? "P2 WINS!" : "TIE GAME!";
            g2.setColor(new Color(255, 220, 0));
            g2.drawString(winner, (W - fm.stringWidth(winner)) / 2, 400);
        }

        if ((System.currentTimeMillis() / 600) % 2 == 0) {
            g2.setFont(new Font("Courier New", Font.BOLD, 20));
            g2.setColor(Color.WHITE);
            String r = "PRESS ENTER TO PLAY AGAIN";
            fm = g2.getFontMetrics();
            g2.drawString(r, (W - fm.stringWidth(r)) / 2, 450);
        }
    }

    @Override public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (state == State.MENU) {
            if (k == KeyEvent.VK_ENTER) {
                state = State.SELECT; /* Enter on the menu goes to player count selection, not straight to game */
            }
        } else if (state == State.SELECT) {
            // Left/Right arrows or 1/2 keys toggle the player count selection
            if (k == KeyEvent.VK_LEFT  || k == KeyEvent.VK_1) playerCount = 1;
            if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_2) playerCount = 2;
            if (k == KeyEvent.VK_ENTER) {
                // Confirm selection and start the game
                state = State.PLAYING;
                score = 0; lives = 3; score2 = 0; lives2 = 3; level = 1; /*control the number of lives initially as the game starts */
                spawnLevel();
            }
        } else if (state == State.PLAYING) {
            // Player 1 controls
            if (k == KeyEvent.VK_LEFT)  left = true;
            if (k == KeyEvent.VK_RIGHT) right = true;
            if (k == KeyEvent.VK_SPACE) shooting = true;
            // Player 2 controls (key presses are read but only acted on in update() when playerCount == 2)
            if (k == KeyEvent.VK_A) left2     = true;
            if (k == KeyEvent.VK_D) right2    = true;
            if (k == KeyEvent.VK_W) shooting2 = true;
        } else if (state == State.GAME_OVER) {
            if (k == KeyEvent.VK_ENTER) {
                state = State.SELECT; /* go back to player select so they can switch mode on replay */
            }
        }
    }

    @Override public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        // Player 1
        if (k == KeyEvent.VK_LEFT)  left  = false;
        if (k == KeyEvent.VK_RIGHT) right = false;
        if (k == KeyEvent.VK_SPACE) shooting = false;
        // Player 2
        if (k == KeyEvent.VK_A) left2     = false;
        if (k == KeyEvent.VK_D) right2    = false;
        if (k == KeyEvent.VK_W) shooting2 = false;
    }

    @Override public void keyTyped(KeyEvent e) {}
}
