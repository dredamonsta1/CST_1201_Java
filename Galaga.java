import javax.swing.*;

public class Galaga extends JFrame { // The main class that sets up the game window and starts the game loop. It extends JFrame to create a window for the game.
    public Galaga() { // Constructor for the Galaga class. It initializes the game window and adds the GamePanel to it.
        setTitle("GALAGA");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        GamePanel panel = new GamePanel();
        add(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Galaga::new);
    }
}
