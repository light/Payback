package payback.model;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * A {@link Sprite} is a constant graphical element, which may be displayed on screen in various places as well as many times in a same
 * frame.
 * <p>
 * {@link Sprite}s are deduced from the observed movements on screen: some are part of the background, some take part in the game action,
 * but none is given with 100% accuracy (the source of information is an almost common pixel matrix after all!). In order to implement this
 * fact, each pixel of the sprite is given a weight, or probability of correctness. This value is stored as the alpha color component of the
 * {@link Sprite}'s {@link #image}:
 * <ul>
 * <li>Transparent pixels (alpha == 0) are surely not part of the sprite.</li>
 * <li>Opaque pixels (alpha == 255) are definitely part of the sprite</li>
 * <li>Other pixels (with their alpha component being both non-zero and non-maximal) are pixels whose membership is not totally a sure fact.
 * The higher the alpha component, the more confident one can be that the pixel is actually part of the sprite.</li>
 * <ul>
 */
public class Sprite {

    private static int DEFAULT_TRANSPARENCY = 0x80000000;

    /**
     * The graphics of this sprite. The alpha color component is used to describe the confidence one can place in each pixel, from 0 (no
     * way) up to 255 (sure thing).
     */
    public BufferedImage image;

    /**
     * Creates a {@link Sprite} with the given {@link Image}. Transparency values of the latter are kept, with the semantics explained in
     * this class' javaDoc.
     * 
     * @see Sprite the transparency semantics
     */
    public Sprite(Image image) {
        this.image = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        this.image.createGraphics().drawImage(image, 0, 0, Color.black, null);
    }

    /**
     * Creates a {@link Sprite} based on the given pixels. Pixels are defined by their (x,y) coords and their RGB color. The default pixel's
     * alpha values are set to {@link #DEFAULT_TRANSPARENCY}.
     * 
     * @param pixels
     *            the pixels of the sprite, as a collection of [x,y,rgb] arrays
     * @see Sprite the transparency semantics
     */
    public Sprite(List<int[]> pixels) {
        int minX = Integer.MAX_VALUE, maxX = 0, minY = Integer.MAX_VALUE, maxY = 0;
        for (int[] pix : pixels) {
            int x = pix[0];
            int y = pix[1];
            if (x < minX) {
                minX = x;
            }
            if (x > maxX) {
                maxX = x;
            }
            if (y < minY) {
                minY = y;
            }
            if (y > maxY) {
                maxY = y;
            }
        }
        int w = maxX - minX + 1;
        int h = maxY - minY + 1;

        // int numbands = 4; // RGBA
        // byte[][] imageData = new byte[numbands][w * h];
        // int depth[] = new int[numbands];
        // int bands[] = new int[numbands];
        // int offsets[] = new int[numbands];
        // for (int i = 0; i < numbands; i++) {
        // depth[i] = 8;
        // bands[i] = i;
        // offsets[i] = 0;
        // }
        // ComponentColorModel ccm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), depth, true, false,
        // Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
        // BandedSampleModel csm = new BandedSampleModel(DataBuffer.TYPE_BYTE, w, h, w, bands, offsets);
        // DataBuffer dataBuffer = new DataBufferByte(imageData, w * h);
        // WritableRaster wr = Raster.createWritableRaster(csm, dataBuffer, null);
        // this.image = new BufferedImage(ccm, wr, false, null);
        //
        // for (int[] pix : pixels) {
        // int x = pix[0] - minX + w * (pix[1] - minY);
        // int rgb = pix[2];
        // imageData[0][x] = (byte) ((rgb & 0x00ff0000) >> 16);
        // imageData[1][x] = (byte) ((rgb & 0x0000ff00) >> 8);
        // imageData[2][x] = (byte) (rgb & 0x000000ff);
        // imageData[3][x] = (byte) -1;
        // }

        image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        WritableRaster raster = image.getRaster();
        for (int[] pix : pixels) {
            int x = pix[0] - minX + w * (pix[1] - minY);
            int rgb = pix[2];
            raster.getDataBuffer().setElem(x, rgb & 0x00ffffff + DEFAULT_TRANSPARENCY);
        }

    }

    /**
     * Test.
     */
    public static void main(String[] args) {
        List<int[]> pixels = new ArrayList<int[]>();
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                pixels.add(new int[] { i / 3, j, Color.red.getRGB() });
                pixels.add(new int[] { 33 + i / 3, j, Color.green.getRGB() });
                pixels.add(new int[] { 66 + i / 3, j, Color.blue.getRGB() });
            }
        }
        long start = System.currentTimeMillis();
        final Sprite sprite = new Sprite(pixels);
        System.out.println("new sprite in " + (System.currentTimeMillis() - start) + "ms");
        JFrame f = new JFrame("test");
        JPanel p = new JPanel() {
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponents(g);
                g.drawImage(sprite.image, 0, 0, Color.black, null);
            }
        };
        f.add(p);
        f.setVisible(true);
        f.setBounds(50, 50, 150, 150);
    }

}
