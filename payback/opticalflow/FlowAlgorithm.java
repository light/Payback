package payback.opticalflow;

import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

public class FlowAlgorithm {
    /** The previous and current frames. */
    private BufferedImage[] images = new BufferedImage[2];
    /** The frame's dimensions. */
    private int h, w;

    /** The pixel brightness values, for the previous and current frames. */
    private float[][][] e;
    /** Computed values for each pixel. */
    private float[][] u, v, ex, ey, et, vbar, ubar, norms;
    /** Labels. */
    private Label[][] labels;

    /** A magic (constant) value. */
    private float alpha = .1f;

    /** Number of loops in the velocity computation. */
    private final static int NB_ITERATIONS = 8;

    /**
     * Compute a color's brightness value.
     *
     * @param rgb the color's RGB values as an integer (0xRRGGBB)
     */
    public static float getBrightness( int rgb ) {
        int red = (rgb >> 16) & 0xff;
        int green = (rgb >> 8) & 0xff;
        int blue = (rgb) & 0xff;
        float brightness = (float) ((.2126 * red + .7152 * green + .0722 * blue) / 255);
        return brightness;
    }

    /**
     * Update all computed values.
     *
     * @param image the current frame
     */
    public void update( BufferedImage image ) {
        image = blurImage( image );
        images[0] = images[1];
        h = image.getHeight();
        w = image.getWidth();
        BufferedImage newImage = new BufferedImage( w, h, image.getType() );
        newImage.getGraphics().drawImage( image, 0, 0, null );
        images[1] = newImage;

        if( images[0] == null ) {
            return;
        }

        if( u == null ) {
            e = new float[2][w][h];
            ex = new float[w][h];
            ey = new float[w][h];
            et = new float[w][h];
            ubar = new float[w][h];
            vbar = new float[w][h];
            u = new float[w][h];
            v = new float[w][h];
            norms = new float[w][h];
        }

        // compute the E values
        e[0] = e[1];
        e[1] = new float[w][h];
        for( int x = 0; x < w; x++ ) {
            for( int y = 0; y < h; y++ ) {
                int rgb = images[1].getRGB( x, y );
                // int r = (rgb & 0xFF0000) >> 16;
                // int g = (rgb & 0xFF00) >> 8;
                // int b = (rgb & 0xFF);
                // float[] hsbvals = Color.RGBtoHSB(r, g, b, null);
                // e[1][x][y] = hsbvals[2];
                e[1][x][y] = getBrightness( rgb );
            }
        }

        // compute the Ex, Ey, Et values
        for( int i = 0; i < w; i++ ) {
            for( int j = 0; j < h; j++ ) {
                int x = i;
                int y = j;
                if( x == 0 ) {
                    x = 1;
                }
                if( x == w - 1 ) {
                    x = w - 2;
                }
                if( y == 0 ) {
                    y = 1;
                }
                if( y == h - 1 ) {
                    y = h - 2;
                }
                ex[x][y] = 0.25f * (e[0][x][(y + 1)] - e[0][x][y] + e[0][(x + 1)][(y + 1)] - e[0][(x + 1)][y] + e[1][x][(y + 1)] - e[1][x][y] + e[1][(x + 1)][(y + 1)] - e[1][(x + 1)][y]);
                ey[x][y] = 0.25f * (e[0][(x + 1)][y] - e[0][x][y] + e[0][(x + 1)][(y + 1)] - e[0][x][(y + 1)] + e[1][(x + 1)][y] - e[1][x][y] + e[1][(x + 1)][(y + 1)] - e[1][x][(y + 1)]);
                et[x][y] = 0.25f * (e[1][x][y] - e[0][x][y] + e[1][(x + 1)][y] - e[0][(x + 1)][y] + e[1][x][(y + 1)] - e[0][x][(y + 1)] + e[1][(x + 1)][(y + 1)] - e[0][(x + 1)][(y + 1)]);
            }
        }

        for( int iter = 0; iter < NB_ITERATIONS; iter++ ) {
            float[][] un = new float[w][h];
            float[][] vn = new float[w][h];

            // compute mean velocity values
            for( int i = 0; i < w; i++ ) {
                for( int j = 0; j < h; j++ ) {
                    int x = i;
                    int y = j;
                    if( x == 0 ) {
                        x = 1;
                    }
                    if( x == w - 1 ) {
                        x = w - 2;
                    }
                    if( y == 0 ) {
                        y = 1;
                    }
                    if( y == h - 1 ) {
                        y = h - 2;
                    }
                    ubar[x][y] = 1f / 6 * (u[x - 1][y] + u[x][y + 1] + u[x + 1][y] + u[x][y - 1]) + 1f / 12 * (u[x - 1][y - 1] + u[x - 1][y + 1] + u[x + 1][y + 1] + u[x + 1][y - 1]);
                    vbar[x][y] = 1f / 6 * (v[x - 1][y] + v[x][y + 1] + v[x + 1][y] + v[x][y - 1]) + 1f / 12 * (v[x - 1][y - 1] + v[x - 1][y + 1] + v[x + 1][y + 1] + v[x + 1][y - 1]);
                }
            }

            for( int x = 0; x < w; x++ ) {
                for( int y = 0; y < h; y++ ) {
                    float exx = ex[x][y];
                    float ubarr = ubar[x][y];
                    float eyy = ey[x][y];
                    float vbarr = vbar[x][y];
                    float ett = et[x][y];
                    un[x][y] = ubarr - exx * (exx * ubarr + eyy * vbarr + ett) / (alpha * alpha + exx * exx + eyy * eyy);
                    vn[x][y] = vbarr - eyy * (exx * ubarr + eyy * vbarr + ett) / (alpha * alpha + exx * exx + eyy * eyy);
                }
            }

            u = un;
            v = vn;
        }

        // compute norms
        for( int i = 0; i < w; i++ ) {
            for( int j = 0; j < h; j++ ) {
                float uxy = u[i][j];
                float vxy = v[i][j];
                norms[i][j] = (float) Math.sqrt( uxy * uxy + vxy * vxy );
            }
        }

//        for( int x = 0; x < w; x++ ) {
//            for( int y = 0; y < h; y++ ) {
//                u[x][y] = x<50 ? -1:1;
//                v[x][y] = y<50 ? 0:1;
//                norms[x][y] = 1;
//            }
//        }

        // compute labels
        labels = new Label[w][h]; // reset labels to null
        int nextLabel = 1;
        labels[0][0] = new Label( u[0][0], v[0][0], nextLabel++ );
        for( int i = 0; i < w; i++ ) {
            for( int j = 0; j < h; j++ ) {
                if( i == 0 ) {
                    if( j == 0 )
                        continue;
                    if( ! doPixel( i, j, i, j-1 ) ) {
                        labels[i][j] = new Label( u[i][j], v[i][j], nextLabel++ );
                    }
                } else {
                    if( ! (               doPixel( i, j, i-1, j   )
                            || (j!=h-1 && doPixel( i, j, i-1, j+1 ) )
                            || (j!=0   && doPixel( i, j, i  , j-1 ) )
                            || (j!=0   && doPixel( i, j, i-1, j-1 ) ) ) ) { // gniark
                        labels[i][j] = new Label( u[i][j], v[i][j], nextLabel++ );
                    }
                }
            }
        }

    }

    /**
     * 3x3 Bock blur kernel.
     * @param image
     * @return blurred image.
     */
    public BufferedImage blurImage( BufferedImage image ) {
        ConvolveOp filter = new ConvolveOp( new Kernel( 3, 3, new float[] {
                0.111f, 0.111f, 0.111f,
                0.111f, 0.111f, 0.111f,
                0.111f, 0.111f, 0.111f
        }), ConvolveOp.EDGE_NO_OP, null );
        return filter.filter( image, null );
    }


    /**
     * Compare le vecteur en (x, y) au vecteur en (i, j) et s'ils sont suffisamment proches,
     * attribue le label  de (i, j) à (x, y) et renvoie true. Sinon, renvoie false.
     * (i, j) doit avoir un label.
     */
    private boolean doPixel( int x, int y, int i, int j ) {
        Label label = labels[i][j];
        double likeliness;
        float uu = u[x][y];
        float vv = v[x][y];
        if(label.v == 0 && label.u == 0) {
            likeliness = uu+vv;
        } else {
            likeliness = Math.abs( 1 - (label.u * uu + label.v * vv) / (Math.sqrt(label.u * label.u + label.v * label.v) * Math.sqrt( uu*uu+vv*vv )) );
            // float likeliness = Math.abs( u[i][j] * v[i + p][j + q] - u[i + p][j + q] * v[i][j] );
        }

        if( likeliness < 0.02 ) {
//            System.out.println("Compare "+x+","+y+" to "+i+","+j+" : "+likeliness+" "+"FIT");
            labels[x][y] = label;
            return true;
        } else {
//            System.out.println("Compare "+x+","+y+" to "+i+","+j+" : "+likeliness+" "+"UNFIT");
            return false;
        }
    }

    public static class Label {
        public int value;
        public float u, v;

        public Label( float u, float v, int value ) {
            this.u = u;
            this.v = v;
            this.value = value;
        }
    }

    /** Expose the computed X velocities. */
    public float[][] getU() {
        return u;
    }

    /** Expose the computed Y velocities. */
    public float[][] getV() {
        return v;
    }

    /** Expose the computed velocity norms. */
    public float[][] getNorms() {
        return norms;
    }

    /** Expose the computed labels. */
    public Label[][] getLabels() {
        return labels;
    }

}
