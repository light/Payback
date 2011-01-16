package payback.opticalflow;

import java.awt.image.BufferedImage;
import java.util.Random;

public class BruteAlgorithm {
    /** The previous and current frames. */
    private BufferedImage[] images = new BufferedImage[2];
    /** The frame's dimensions. */
    private int h, w;

    private int[][][] rgb; 
    private boolean[][] interesting;
    
    /** Labels. */
    private Label[][] labels;
    
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
            rgb = new int[3][w][h];
            interesting = new boolean[w][h];
        }
        

        int[] vScroll=resolveScrolling();

        rgb[0] = rgb[1];
        rgb[1] = new int[w][h];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                rgb[1][i][j] = images[1].getRGB(i, j);
                interesting[i][j] = rgb[0][i][j] != images[1].getRGB(clip(i + vScroll[0], 0, w - 1), clip(j + vScroll[1], 0, h - 1));
            }
        }

        // compute labels
        Label[][] tmpLabels = defineLabels();
        labels = tmpLabels;

    }

    private Label[][] defineLabels() {
        Label[][] tmpLabels = new Label[w][h]; // reset labels to null
        int nextLabel = 1;
        tmpLabels[0][0] = new Label(nextLabel++);

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                if (i == 0) {
                    if (j == 0)
                        continue;
                    if (!doPixel(tmpLabels, i, j, i, j - 1)) {
                        tmpLabels[i][j] = new Label(nextLabel++);
                    }
                } else {
                    if (!(  doPixel(tmpLabels, i, j, i - 1, j)
             | (j != h-1 && doPixel(tmpLabels, i, j, i - 1, j + 1))
             | (j != 0   && doPixel(tmpLabels, i, j, i, j - 1))
             | (j != 0   && doPixel(tmpLabels, i, j, i - 1, j - 1)))) { // gniark
                        tmpLabels[i][j] = new Label(nextLabel++);
                    }
                }
            }
        }
        
        return tmpLabels;
    }
    
    /**
     * Interesting map:
     * <pre>
     * 0 0 0 0 1
     * 0 1 0 1 0 
     * 0 0 1 0 0
     * 1 0 0 0 1
     * 1 0 0 1 0
     * </pre>
     * Expected labels:
     * <pre>
     * 0 0 0 0 1
     * 0 1 0 1 0 
     * 0 0 1 0 0
     * 2 0 0 0 3
     * 2 0 0 3 0
     * </pre>
     * 
     * (JUnit is for pussies)
     */
    public static void main(String[] args) throws Exception {
        BruteAlgorithm ba = new BruteAlgorithm();
        ba.w = ba.h = 5;
        ba.interesting = new boolean[][] {
                new boolean[] { false, false, false, false, true  },
                new boolean[] { false, true,  false, true,  false },
                new boolean[] { false, false, true,  false, false },
                new boolean[] { true,  false, false, false, true  },
                new boolean[] { true,  false, false, true,  false } };

        ba.labels = ba.defineLabels();

        System.out.println("Labels :");
        for (Label[] labels : ba.labels) {
            for (Label label : labels) {
                System.out.print(label + " ");
            }
            System.out.println();
        }
    }
    
    /**
     * Compare le vecteur en (x, y) au vecteur en (i, j) et s'ils sont suffisamment proches,
     * attribue le label  de (i, j) à (x, y) et renvoie true. Sinon, renvoie false.
     * (i, j) doit avoir un label.
     * @param labels TODO
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
    
    private int clip(int i, int min, int max) {
        return Math.min(Math.max(i, min), max);
    }

    private int[] resolveScrolling() {
        int nSamples= 20;
        int distMax = 5;
        
        int[][] pixelCoords = new int[nSamples][2];
        for (int i = 0; i < pixelCoords.length; i++) {
            pixelCoords[i][0] = new Random().nextInt(w-2*distMax)+distMax;
            pixelCoords[i][1] = new Random().nextInt(h-2*distMax)+distMax;
        }
        
        int scoreMax = 0;
        int[] offsetDuScoreMax = new int[2];
        for(int dist = -distMax; dist < distMax+1; dist ++) {
            int score=0;
            for (int i = 0; i < pixelCoords.length; i++) {
                int rgb0 = images[0].getRGB(pixelCoords[i][0], pixelCoords[i][1]); 
                int rgb1 = images[1].getRGB(pixelCoords[i][0]+dist, pixelCoords[i][1]); 
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
