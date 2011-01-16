package payback.opticalflow;

import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * A quite brutal algorithm parsing the displayed frames.
 */
public class BruteAlgorithm {
    
    /** The previous and current frames. */
    private BufferedImage[] images = new BufferedImage[2];
    /** The frame's dimensions. */
    private int h, w;
    /** The RGB values of the frames' pixels. Dimensions are <tt>2</tt> (previous and current frames), <tt>w</tt> and <tt>h</tt>. */
    private int[][][] rgb;
    /** Each pixel's estimated "interest". A pixel is "interesting" if it's part of a sprite (not part of the background). */
    private boolean[][] interesting;
    /**
     * {@link Label}s associated to each pixel of the current frame and defining a segmentation of the image. The {@link Label#value value}s
     * determine a kind of equivalence class; each class defines a group of pixels which are "quite close" (1 pixel max) to each other.
     * 
     * @see #defineLabels()
     */
    private Label[][] labels;

    // params - may be exposed for real-time tuning (?)

    /**
     * The number of pixel samples used to estimate the scrolling.
     * @see #estimateScrolling()
     */
    private int scrollingSamples = 20;
    /**
     * The maximum distance at which each sample is looked for, while estimating the scrolling.
     * @see #estimateScrolling()
     */
    private int scrollingDistance = 5;

    //
    
    public Label[][] getLabels() {
        return labels;
    }

    public boolean[][] getInteresting() {
        return interesting;
    }
    
    public static class Label {
        public int value;

        public Label(int value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.format("[%3d]", value);
        }
    }

    /**
     * Update all computed values.
     *
     * @param image the current frame
     */
    public void update( BufferedImage image ) {
        images[0] = images[1];
        h = image.getHeight();
        w = image.getWidth();
        BufferedImage newImage = new BufferedImage( w, h, image.getType() );
        newImage.getGraphics().drawImage( image, 0, 0, null );
        images[1] = newImage;

        if( images[0] == null ) {
            return;
        }

        if (rgb == null) {
            rgb = new int[2][w][h];
            interesting = new boolean[w][h];
        }
        

        int[] vScroll = estimateScrolling();

        rgb[0] = rgb[1];
        rgb[1] = new int[w][h];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                rgb[1][i][j] = images[1].getRGB(i, j);
                interesting[i][j] = rgb[0][i][j] != images[1].getRGB(clip(i + vScroll[0], 0, w - 1), clip(j + vScroll[1], 0, h - 1));
            }
        }

        labels = defineLabels();
    }

    private int clip(int i, int min, int max) {
        return Math.min(Math.max(i, min), max);
    }

    /**
     * Define {@link Label}s based on the {@link #interesting} values. "Uninteresting" pixels are given a 0-valued {@link Label}.
     * <p>
     * This method considers each pixel (top to bottom, then left to right) and its neighborhood, in this order:
     * 
     * <pre>
     * 4 3 .
     * 1 X .
     * 2 n .
     * </pre>
     * 
     * <ul>
     * <li>X: the current pixel</li>
     * <li>n: the next pixel</li>
     * <li>1-4: the neighbors, in order of parsing</li>
     * <li>.: ignored neighbors</li>
     * </ul>
     * 
     * @return an array of {@link Label}s
     * @see #labels
     * @see #interesting
     */
    private Label[][] defineLabels() {
        Label[][] tmpLabels = new Label[w][h]; // reset labels to null
        int nextLabel = 1;
        tmpLabels[0][0] = new Label(interesting[0][0] ? nextLabel++ : 0);

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                if (i == 0) {
                    if (j == 0)
                        continue;
                    if (!doPixel(tmpLabels, i, j, i, j - 1)) {
                        tmpLabels[i][j] = new Label(interesting[i][j] ? nextLabel++ : 0);
                    }
                } else {
                    if (!(  doPixel(tmpLabels, i, j, i - 1, j)
             | (j != h-1 && doPixel(tmpLabels, i, j, i - 1, j + 1))
             | (j != 0   && doPixel(tmpLabels, i, j, i, j - 1))
             | (j != 0   && doPixel(tmpLabels, i, j, i - 1, j - 1)))) { // gniark
                        tmpLabels[i][j] = new Label(interesting[i][j] ? nextLabel++ : 0);
                    }
                }
            }
        }
        
        return tmpLabels;
    }
    
    /**
     * Compares the (x, y) and (i, j) pixels: if they both are {@link #interesting} (or both ain't), then attributes the (i, j)'s label to
     * (x, y). Pixel (i, j) <i>must</i> have a {@link Label} already.
     * 
     * @param labels
     *            the labels attributed to the image's pixels
     * @return if the compared pixels are the same ({@link #interesting}-wise)
     */
    private boolean doPixel( Label[][] labels, int x, int y, int i, int j) {
        Label label = labels[i][j];
        if (interesting[x][y] == interesting[i][j]) {
            if (labels[x][y] != null) {
                label.value = labels[x][y].value;
            } else {
                labels[x][y] = label;
            }
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Estimates the scrolling, based on the naively guessed deviation of a few pixels.
     * 
     * @return the (x, y) scrolling distances (between the 2 last frames), as an array
     * @see #scrollingSamples the number of pixel samples
     * @see #scrollingDistance the maximum distance each sample may have moved at
     */
    private int[] estimateScrolling() {
        int[][] pixelCoords = new int[scrollingSamples][2];
        for (int i = 0; i < pixelCoords.length; i++) {
            pixelCoords[i][0] = new Random().nextInt(w - 2 * scrollingDistance) + scrollingDistance;
            pixelCoords[i][1] = new Random().nextInt(h - 2 * scrollingDistance) + scrollingDistance;
        }
        
        int scoreMax = 0;
        int[] offsetDuScoreMax = new int[2];
        for(int dist = -scrollingDistance; dist < scrollingDistance+1; dist ++) {
            int score = 0;
            for (int i = 0; i < pixelCoords.length; i++) {
                int rgb0 = images[0].getRGB(pixelCoords[i][0], pixelCoords[i][1]);
                int rgb1 = images[1].getRGB(pixelCoords[i][0] + dist, pixelCoords[i][1]);
                score += rgb0 == rgb1 ? 1 : 0;
            }
            if(score > scoreMax) {
                scoreMax = score;
                offsetDuScoreMax = new int[] {dist, 0};
            }

            score=0;
            for (int i = 0; i < pixelCoords.length; i++) {
                int rgb0 = images[0].getRGB(pixelCoords[i][0], pixelCoords[i][1]); 
                int rgb1 = images[1].getRGB(pixelCoords[i][0], pixelCoords[i][1]+dist); 
                score += rgb0 == rgb1 ? 1 : 0;
            }
            if(score > scoreMax) {
                scoreMax = score;
                offsetDuScoreMax = new int[] {0, dist};
            }
        }

        // let's privilege the "not scrolling" solution
        int score=0;
        for (int i = 0; i < pixelCoords.length; i++) {
            int rgb0 = images[0].getRGB(pixelCoords[i][0], pixelCoords[i][1]); 
            int rgb1 = images[1].getRGB(pixelCoords[i][0], pixelCoords[i][1]); 
            score += rgb0 == rgb1 ? 1 : 0;
        }
        if(score == scoreMax) {
            scoreMax = score;
            offsetDuScoreMax = new int[] {0, 0};
        }
        
        return offsetDuScoreMax;
    }

  }
