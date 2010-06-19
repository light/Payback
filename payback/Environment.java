package payback;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

public class Environment {
    private List<Entity> entities = new ArrayList<Entity>();

    private int background;
    private List<Rectangle> megamanSprites;
    private BufferedImage megamanImage;

    public Environment() throws Exception {
        InputStream ras = null;
        try {
            ras = getClass().getResourceAsStream( "mm.gif" );
            megamanImage = ImageIO.read( ras );
        } finally {
            if( ras != null ) {
                ras.close();
            }
        }
        background = megamanImage.getRGB( 0, 0 );
        megamanSprites = getSpriteRectangles( megamanImage, background );

    }

    /**
     * Cuts the given image into {@link Rectangle}s containing non-background pixels, based on the <tt>background</tt> color.
     * @param image the image to cut in pieces
     * @param background the background color
     * @return the computed {@link Rectangle}s as a non-null {@link List}
     */
    private List<Rectangle> getSpriteRectangles( BufferedImage image, int background ) {
        List<Rectangle> blocks = new ArrayList<Rectangle>();

        for( Rectangle r : getLines( image, background ) ) {
            blocks.addAll( getBlocks( image, r, background ) );
        }
        return blocks;
    }

    private List<Rectangle> getLines( BufferedImage image, int background ) {
        int firstY = -1;
        boolean previousRowClear = true;
        List<Rectangle> blocks = new ArrayList<Rectangle>();
        for( int y = 0; y < image.getHeight(); y++ ) {
            boolean rowClear = true;
            for( int x = 0; x < image.getWidth(); x++ ) {
                if( background != image.getRGB( x, y ) ) {
                    rowClear = false;
                    break;
                }
            }
            if( !rowClear ) {
                if( previousRowClear ) {
                    firstY = y;
                }
            } else if( !previousRowClear ) {
                blocks.add( new Rectangle( 0, firstY, image.getWidth(), y - firstY ) );
            }
            previousRowClear = rowClear;
        }
        return blocks;
    }

    private List<Rectangle> getBlocks( BufferedImage image, Rectangle line, int background ) {
        int firstX = -1;
        boolean inBlock = false;
        List<Rectangle> blocks = new ArrayList<Rectangle>();
        for( int x = line.x; x < line.x + line.width; x++ ) {
            boolean columnClear = true;
            for( int y = line.y; y < line.y + line.height; y++ ) {
                if( background != image.getRGB( x, y ) ) {
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

    public Rectangle update( BufferedImage image ) {
        Rectangle mm = findMegaman( image );
        return mm;

    }

    private Rectangle findMegaman( BufferedImage image ) {
        for( Rectangle r : megamanSprites ) {
            for( int x = 0; x < image.getWidth( null ) - r.width; x++ ) {
                hop: for( int y = 0; y < image.getHeight( null ) - r.height; y++ ) {
                    for( int rx = 0; rx < r.width; rx += 8 ) {
                        for( int ry = 0; ry < r.height; ry += 8 ) {
                            int rgb = megamanImage.getRGB( r.x + rx, r.y + ry );
                            if( rgb != background && image.getRGB( x + rx, y + ry ) != rgb ) {
                                continue hop;
                            }
                        }
                    }
                    return new Rectangle( x, y, r.width, r.height );
                }
            }
        }
        return null;
    }

}
