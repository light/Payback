package payback.algo;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import payback.model.Sprite;
import payback.params.RuntimeParam;

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
    /** Random values generator. */
    private Random random = new Random();

    // params  

    /**
     * The number of pixel samples used to estimate the scrolling.
     * @see #estimateScrolling()
     */
    @RuntimeParam(value = 20, minValue = 1, maxValue = 256)
    private int scrollingSamples = 20;
    /**
     * The maximum distance at which each sample is looked for, while estimating the scrolling.
     * @see #estimateScrolling()
     */
    @RuntimeParam(value = 5, minValue = 1, maxValue = 20)
    private int scrollingDistance = 5;
    /**
     * The size of the square samples used to estimate the scrolling.
     * @see #estimateScrolling()
     */
    @RuntimeParam(value = 2, minValue = 1, maxValue = 20)
    private int sampleSize = 2;
    public List<Sprite> sprites = new ArrayList<Sprite>();

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
        
        rgb[0] = rgb[1];
        rgb[1] = new int[w][h];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                rgb[1][i][j] = images[1].getRGB(i, j);
            }
        }

        
        // background movement (<0 means it goes right/bottom to left/top)
        int[] vScroll = estimateScrolling();

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                int x = i - vScroll[0];
                int y = j - vScroll[1];
                if (x >= 0 && x < w && y >= 0 && y < h) {
                    interesting[i][j] = rgb[1][i][j] != rgb[0][x][y];
                }
            }
        }

        labels = defineLabels();
        Map<Integer, List<int[]>> regroupedLabels = regroupLabels();
        sprites = createSprites(regroupedLabels);
    }

    private List<Sprite> createSprites(Map<Integer, List<int[]>> regroupedLabels) {
        List<Sprite> sprites = new ArrayList<Sprite>();
        for (Integer key : regroupedLabels.keySet()) {
            if (key.equals(0)) {
                continue;
            }
            List<int[]> pixels = regroupedLabels.get(key);
            sprites.add(new Sprite(pixels));
        }
        return sprites;
    }
    

    private Map<Integer, List<int[]>> regroupLabels() {
        Map<Integer, List<int[]>> result = new HashMap<Integer, List<int[]>>();

        for (int i = 0; i < labels.length; i++) {
            for (int j = 0; j < labels[i].length; j++) {
                Label label = labels[i][j];
                Integer value = label.value;
                List<int[]> pixList = result.get(value);
                if (pixList == null) {
                    pixList = new ArrayList<int[]>();
                    result.put(value, pixList);
                }
                pixList.add(new int[] { i, j, rgb[1][i][j] });
            }
        }

        return result;
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
    private boolean doPixel(Label[][] labels, int x, int y, int i, int j) {
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
     * Matches the pixels in the given rectangle of preceding frame, with the pixels of the same rectangle moved to <tt>(x,y)</tt> in the
     * current frame.
     * 
     * @param r
     *            the pixels in the preceding frame
     * @param x
     *            the X coordinate of the rectangle in the current frame
     * @param y
     *            the Y coordinate of the rectangle in the current frame
     * @return the number of matching pixels, between <tt>0</tt> and <tt>(r.width * r.height)</tt>
     */
    private int matchFramePixels(Rectangle r, int x, int y) {
        int score = 0;
        for (int i = 0; i < r.width; i++) {
            for (int j = 0; j < r.height; j++) {
                if (rgb[0][r.x + i][r.y + j] == rgb[1][x + i][y + j]) {
                    score++;
                }
            }
        }
        return score;
    }

    /**
     * Estimates the scrolling, based on the naively guessed deviation of a few pixels. Samples are squares of side {@value #sampleSize}
     * pixels, which may move between frames of {@value #scrollingDistance} pixels max. {@value #scrollingSamples} samples are considered.
     * 
     * @return the (x, y) scrolling distances (between the 2 last frames), as an array - one of these two values being <tt>0</tt>
     * @see #scrollingSamples the number of pixel samples
     * @see #scrollingDistance the maximum distance each sample may have moved at
     * @see #sampleSize the size of each sample
     */
    private int[] estimateScrolling() {
        Rectangle[] samples = new Rectangle[scrollingSamples];
        for (int i = 0; i < samples.length; i++) {
            int padding = scrollingDistance + sampleSize;
            int x = random.nextInt(w - 2 * padding) + padding;
            int y = random.nextInt(h - 2 * padding) + padding;
            samples[i] = new Rectangle(x, y, sampleSize, sampleSize);
        }

        int limitScore = samples.length * sampleSize * sampleSize; // best score possible
        int scoreMax = 0;
        int[] scrolling = new int[2];
        // let's prefer minimal values of scrolling
        scores: for (int absdist = 0; absdist < scrollingDistance + 1; absdist++) {
            for (int dist : new int[] { -absdist, absdist }) {
                // testing for horizontal scrolling
                int score = 0;
                for (int i = 0; i < samples.length; i++) {
                    Rectangle r = samples[i];
                    score += matchFramePixels(r, r.x + dist, r.y);
                }
                if (score > scoreMax) {
                    scoreMax = score;
                    scrolling = new int[] { dist, 0 };
                    if (limitScore == score) {
                        break scores;
                    }
                }

                // testing for vertical scrolling
                score = 0;
                for (int i = 0; i < samples.length; i++) {
                    Rectangle r = samples[i];
                    score += matchFramePixels(r, r.x, r.y + dist);
                }
                if (score > scoreMax) {
                    scoreMax = score;
                    scrolling = new int[] { 0, dist };
                    if (limitScore == score) {
                        break scores;
                    }
                }
            }
        }

        return scrolling;
    }

}
