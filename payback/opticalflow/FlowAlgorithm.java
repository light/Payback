package payback.opticalflow;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class FlowAlgorithm {
    private BufferedImage[] images = new BufferedImage[2];
    private float[][] u, v, ex, ey, et, vbar, ubar;
    private float[][][] e ;
    private float alpha = .2f;
    private int h;
    private int w;

    public void update( BufferedImage image ) {
        images[0] = images[1];
        h = image.getHeight();
        w = image.getWidth();
        BufferedImage newImage = new BufferedImage( w, h, image.getType() );
        newImage.getGraphics().drawImage( image, 0, 0, null );
        images[1] = newImage;

        if(images[0] == null) {
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
        }

        e[0] = e[1];
        e[1] = new float[w][h];
        for( int x = 0; x < w; x++ ) {
            for( int y = 0; y < h; y++ ) {
                int rgb = images[1].getRGB( x, y );
                int r = (rgb & 0xFF0000) >> 16;
                int g = (rgb & 0xFF00) >> 8;
                int b = (rgb & 0xFF);

                float[] hsbvals = Color.RGBtoHSB( r, g, b, null );
                e[1][x][y] = hsbvals[2];
            }
        }

        for( int i = 0; i < w; i++ ) {
            for( int j = 0; j < h; j++ ) {
                int x = i; int y =j;
                if(x == 0)
                    x = 1;
                if(x == w-1)
                    x = w-2;
                if(y == 0)
                    y = 1;
                if(y == h-1)
                    y = h-2;
                ex[x][y] = 0.25f * (e[0][x][(y + 1)] - e[0][x][y] + e[0][(x + 1)][(y + 1)] - e[0][(x + 1)][y]
                                  + e[1][x][(y + 1)] - e[1][x][y] + e[1][(x + 1)][(y + 1)] - e[1][(x + 1)][y]);
                ey[x][y] = 0.25f * (e[0][(x + 1)][y] - e[0][x][y] + e[0][(x + 1)][(y + 1)] - e[0][x][(y + 1)]
                                  + e[1][(x + 1)][y] - e[1][x][y] + e[1][(x + 1)][(y + 1)] - e[1][x][(y + 1)]);
                et[x][y] = 0.25f * (e[1][x][y] - e[0][x][y] + e[1][(x + 1)][y] - e[0][(x + 1)][y]
                                  + e[1][x][(y + 1)] - e[0][x][(y + 1)] + e[1][(x + 1)][(y + 1)] - e[0][(x + 1)][(y + 1)]);
            }
        }


        for( int iter = 0; iter < 10; iter++ ) {

            for( int i = 0; i < w; i++ ) {
                for( int j = 0; j < h; j++ ) {
                    int x = i; int y =j;

                    if(x == 0)
                        x = 1;
                    if(x == w-1)
                        x = w-2;
                    if(y == 0)
                        y = 1;
                    if(y == h-1)
                        y = h-2;
                    ubar[x][y] = 1f / 6 * (u[x - 1][y] + u[x][y + 1] + u[x + 1][y] + u[x][y - 1])
                          + 1f / 12 * (u[x - 1][y - 1] + u[x - 1][y + 1] + u[x + 1][y + 1] + u[x + 1][y - 1]);

                    vbar[x][y] = 1f / 6 * (v[x - 1][y] + v[x][y + 1] + v[x + 1][y] + v[x][y - 1])
                         + 1f / 12 * (v[x - 1][y - 1] + v[x - 1][y + 1] + v[x + 1][y + 1] + v[x + 1][y - 1]);

                }
            }

            float[][] un = new float[w][h];
            float[][] vn = new float[w][h];

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
    }

    public float[][] getU() {
        return u;
    }

    public float[][] getV() {
        return v;
    }
}
