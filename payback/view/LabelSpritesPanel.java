package payback.view;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

import payback.algo.BruteAlgorithm;
import payback.model.Sprite;

public class LabelSpritesPanel extends JPanel {
    private BruteAlgorithm brute;

    public LabelSpritesPanel(BruteAlgorithm brute) {
        this.brute = brute;
        setPreferredSize(new Dimension(256, 500));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.drawRect(0, 0, getWidth(), getHeight());
        int y = 0;
        for (Sprite sprite : brute.sprites) {
            g.drawImage(sprite.image, 0, y += sprite.image.getHeight(), null);
        }
    }

}
