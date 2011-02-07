package payback.view;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

import javax.swing.JPanel;

import payback.Environment;
import payback.algo.BruteAlgorithm.Label;

@SuppressWarnings("serial")
public class LabelPanel extends JPanel {
    private final Environment environment;
    private int w = 256;
    private int h = 240;
    int[] pixels;
    private BufferedImage image;

    public LabelPanel(Environment environment) {
        this.environment = environment;
        setPreferredSize(new Dimension(w, h));
        createImage();
    }

    /*
     * Creates a bufferedImage backed by a pixel array.
     */
    private void createImage() {
        pixels = new int[w * h];
        ColorModel colorModel = new DirectColorModel(24, 0x00ff0000, 0x0000ff00, 0x000000ff, 0);
        SampleModel sampleModel = colorModel.createCompatibleSampleModel(w, h);
        DataBuffer buffer = new DataBufferInt(pixels, w * h);
        WritableRaster raster = WritableRaster.createWritableRaster(sampleModel, buffer, new Point(0, 0));
        image = new BufferedImage(colorModel, raster, false, null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        boolean[][] interesting = environment.getBrute().getInteresting();
        Label[][] labels = environment.getBrute().getLabels();

        if (interesting == null || labels == null) {
            return; // avoid NPE
        }

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                // pixels[i + j * w] = interesting[i][j] ? labels[i][j].value * 3299542 : 0;
                pixels[i + j * w] = labels[i][j].value * 3299542;
            }
        }

        // doGrouping( image, u, v );

        g.drawImage(image, 0, 0, null);
    }

}