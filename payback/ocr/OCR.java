package payback.ocr;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class OCR {
    private static final double BRIGHTNESS_THRESHOLD = 0.5;
    private static final double SIMILARITY_THRESHOLD = 0.05;

    Map<Template, String> templates = new HashMap<Template, String>();

    public void train( BufferedImage image, String string ) throws OCRException {
        Rectangle line = getLine( image );
        List<Rectangle> blocks = getBlocks( image, line );
        if( blocks.size() != string.length() )
            throw new OCRException( "Incorrect numer of blocks (expected " + string.length() + ", found " + blocks.size() + ")" );
        for( int i = 0; i < blocks.size(); i++ ) {
            // TODO merge templates that match the same string.
            templates.put( createTemplate( image, blocks.get( i ) ), string.substring( i, i + 1 ) );
        }
    }

    public String identify( BufferedImage image ) throws OCRException {
        StringBuffer sb = new StringBuffer();
        Rectangle line = getLine( image );
        List<Rectangle> blocks = getBlocks( image, line );
        for( int i = 0; i < blocks.size(); i++ ) {
            boolean found = false;
            for( Entry<Template, String> entry : templates.entrySet() ) {
                Rectangle block = blocks.get( i );
                float distance = entry.getKey().compare( image, block.x, block.y );
                // System.out.println( "Distance " + entry.getValue() + " : " + distance );
                if( distance < SIMILARITY_THRESHOLD ) {
                    sb.append( entry.getValue() );
                    found = true;
                    break;
                }
            }
            if( !found ) {
                throw new OCRException( "No match found for block " + i );
            }
            // System.out.println();
        }
        return sb.toString();
    }

    public static Rectangle getLine( BufferedImage image ) {
        int firstY = -1, lastY = -1;
        int width = image.getWidth();
        int height = image.getHeight();
        for( int y = 0; y < height; y++ ) {
            for( int x = 0; x < width; x++ ) {
                if( ColorUtil.getBrightness( image.getRGB( x, y ) ) > BRIGHTNESS_THRESHOLD ) {
                    if( firstY == -1 )
                        firstY = y;
                    lastY = y;
                }
            }
        }

        return new Rectangle( 0, firstY, width, lastY - firstY + 1 );
    }

    public static List<Rectangle> getBlocks( BufferedImage image, Rectangle line ) {
        int firstX = -1;
        boolean inBlock = false;
        List<Rectangle> blocks = new ArrayList<Rectangle>();
        for( int x = line.x; x < line.x + line.width; x++ ) {
            boolean columnClear = true;
            for( int y = line.y; y < line.y + line.height; y++ ) {
                if( ColorUtil.getBrightness( image.getRGB( x, y ) ) > BRIGHTNESS_THRESHOLD ) {
                    columnClear = false;
                    break;
                }
            }
            if( columnClear ) {
                if( inBlock ) {
                    blocks.add( new Rectangle( firstX + 1, line.y, x - firstX - 1, line.height ) );
                }
                firstX = x;
            }
            inBlock = !columnClear;
        }

        return blocks;
    }

    private Template createTemplate( BufferedImage processedImage, Rectangle block ) {
        Template template = new Template( block.width, block.height );
        for( int y = 0; y < block.height; y++ ) {
            for( int x = 0; x < block.width; x++ ) {
                template.setValue( x, y, ColorUtil.getBrightness( processedImage.getRGB( x + block.x, y + block.y ) ) );
            }
        }
        return template;
    }
}
