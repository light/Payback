package payback.ocr;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;

public class ColorUtil {
    public static BufferedImage processImage( BufferedImage image ) {
        IndexColorModel colorModel = (IndexColorModel) image.getColorModel();

        int mapSize = colorModel.getMapSize();
        int bpp = colorModel.getPixelSize();
        byte[] red = new byte[mapSize];
        byte[] green = new byte[mapSize];
        byte[] blue = new byte[mapSize];
        for( int i = 0; i < mapSize; i++ ) {
            if( i != 2 ) {
                red[i] = green[i] = blue[i] = 0;
            }
        }
        red[2] = green[2] = blue[2] = (byte) (255); // (1<<bpp)-1 no workie ?
        IndexColorModel newColorModel = new IndexColorModel( bpp, mapSize, red, green, blue );

        WritableRaster raster = image.copyData( null );

        return new BufferedImage( newColorModel, raster, image.isAlphaPremultiplied(), null );
    }

    public static float getBrightness( int rgb ) {
        int alpha = (rgb >> 24) & 0xff;
        int red = (rgb >> 16) & 0xff;
        int green = (rgb >> 8) & 0xff;
        int blue = (rgb) & 0xff;
        float[] hsbvals = new float[3];
        Color.RGBtoHSB( red, green, blue, hsbvals );
        float hue = hsbvals[0];
        float saturation = hsbvals[1];
        float brightness = hsbvals[2];
        return brightness;
    }

}
