package payback.ocr;

import java.awt.image.BufferedImage;


public class Template {
    int width;
    int height;
    float[] values;

    public Template( int width, int height ) {
        this.width = width;
        this.height = height;
        values = new float[width * height];
    }

    public void setValue( int x, int y, float value ) {
        values[x + width * y] = value;
    }

    public float getValue( int x, int y ) {
        return values[x + width * y];
    }

    public float compare( BufferedImage image, int x, int y ) {
        float distance = 0;
        for( int j = 0; j < height; j++ ) {
            for( int i = 0; i < width; i++ ) {
                distance += Math.abs( ColorUtil.getBrightness( image.getRGB( x + i, y + j ) ) - getValue( i, j ) );
            }
        }
        return distance / (width * height);
    }

}
