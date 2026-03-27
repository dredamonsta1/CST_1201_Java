import javax.swing.*;

public class Galaga extends JFrame {
    public Galaga() {
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
