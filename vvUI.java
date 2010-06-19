import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import payback.Environment;

public class vvUI extends AppletUI {
    private JPanel spritePanel;
    private InputHandler joy1 = new vvInputHandler();
    private Environment environment;

    public vvUI( vNES applet ) {
        super( applet );

        try {
            environment = new Environment();
        } catch( Exception e ) {
            e.printStackTrace();
            System.exit( 666 );
        }
        spritePanel = new DancingSugarPlumFairiesPanel();
        showFrame( 300, spritePanel );
        showFrame( 800, new MagicAddressDivinationWizardPanel() );

    }

    private void showFrame( int x, JComponent component ) {
        JFrame jFrame = new JFrame();
        jFrame.add( component );
        jFrame.pack();
        jFrame.setLocation( x, 0 );
        jFrame.setVisible( true );
    }

    @Override
    public void imageReady( boolean skipFrame ) {
        super.imageReady( skipFrame );
        spritePanel.repaint();
    }

    // @Override
    // public InputHandler getJoy1() {
    // return joy1;
    // }

    private class DancingSugarPlumFairiesPanel extends JPanel {
        private static final double SCALE_FACTOR = 1.5;

        public DancingSugarPlumFairiesPanel() {
            setPreferredSize( new Dimension( (int) (256 * SCALE_FACTOR), (int) (240 * SCALE_FACTOR) ) );
        }

        @Override
        protected void paintComponent( Graphics g ) {
            super.paintComponent( g );
            g.setFont( new Font( "sans serif", Font.PLAIN, 10 ) );

            PPU ppu = nes.getPpu();

            // int currentNametable = ppu.ntable1[0];
            NameTable nameTable = ppu.nameTable[0];
            int baseTile = (ppu.regS == 0 ? 0 : 256);
            for( int i = 0; i < 32; i++ ) {
                for( int j = 0; j < 30; j++ ) {
                    short tile = nameTable.getTileIndex( i, j );
                    int x = i * 8;
                    int y = j * 8;
                    nes.ppu.ptTile[baseTile + tile].renderSimple( x, y, g, false, false );
                }
            }

            for( int i = 0; i < 64; i++ ) {
                int x = ppu.sprX[i];
                int y = ppu.sprY[i];
                int tile = ppu.sprTile[i];
                boolean mirrorH = nes.ppu.horiFlip[i];
                boolean mirrorV = nes.ppu.vertFlip[i];

                if( x >= 0 && x < 256 && y >= 0 && y < 240 ) {
                    nes.ppu.ptTile[tile].renderSimple( x, y, g, mirrorV, mirrorH );
                }
            }

            if( mmRect != null ) {
                g.setColor( Color.RED );
                g.drawRect( mmRect.x, mmRect.y, mmRect.width, mmRect.height );
            }

        }
    }

    private class MagicAddressDivinationWizardPanel extends JPanel {
        private static final int RAM_SIZE = 0x800;
        private short[] snapshot = new short[RAM_SIZE];
        private boolean[] matches = new boolean[RAM_SIZE];
        private JTextArea textArea;

        public MagicAddressDivinationWizardPanel() {
            setLayout( new BoxLayout( this, BoxLayout.PAGE_AXIS ) );
            add( new JButton( new StartAnalysisAction() ) );
            add( new JButton( new FindChangesAction() ) );
            add( new JButton( new ExcludeChangesAction() ) );
            textArea = new JTextArea( 20, 50 );
            add( new JScrollPane( textArea ) );
        }

        private short[] getCPUMemory() {
            short[] mem = new short[0x800];
            System.arraycopy( nes.cpuMem.mem, 0, mem, 0, RAM_SIZE );
            return mem;
        }

        private void displayMatches() {
            int matchCount = 0;
            for( int i = 0; i < matches.length; i++ ) {
                if( matches[i] )
                    matchCount++;
            }
            textArea.append( "Found " + matchCount + " potential matches.\n" );
            if( matchCount <= 25 ) {
                for( int i = 0; i < RAM_SIZE; i++ ) {
                    if( matches[i] ) {
                        textArea.append( "Address: 0x" + Integer.toHexString( i ) + ", value: 0x" + Integer.toHexString( snapshot[i] ) + "\n" );
                    }
                }
            }
            textArea.scrollRectToVisible( new Rectangle( 0, textArea.getHeight() - 2, 1, 1 ) );

        }

        private class StartAnalysisAction extends AbstractAction {

            public StartAnalysisAction() {
                super( "(Re)Start analysis" );
            }

            public void actionPerformed( ActionEvent e ) {
                snapshot = getCPUMemory();
                Arrays.fill( matches, true );
                textArea.append( "Analysis started.\n" );
            }
        }

        private class FindChangesAction extends AbstractAction {

            public FindChangesAction() {
                super( "Find addresses that have changed" );
            }

            public void actionPerformed( ActionEvent e ) {
                short[] mem = getCPUMemory();
                for( int i = 0; i < RAM_SIZE; i++ ) {
                    if( snapshot[i] == mem[i] )
                        matches[i] = false;
                }
                snapshot = mem;
                displayMatches();
            }

        }

        private class ExcludeChangesAction extends AbstractAction {

            public ExcludeChangesAction() {
                super( "Find addresses that do not have changed" );
            }

            public void actionPerformed( ActionEvent e ) {
                short[] mem = getCPUMemory();
                for( int i = 0; i < RAM_SIZE; i++ ) {
                    if( snapshot[i] != mem[i] )
                        matches[i] = false;
                }
                snapshot = mem;
                displayMatches();
            }
        }

    }

    private Rectangle mmRect;
    private long frameLimiter = 0;

    @Override
    public void onEndFrame() {
        if( frameLimiter++ % 10 == 0 ) {
            long start = System.currentTimeMillis();
            mmRect = environment.update( (BufferedImage) getScreenView().getImage() );
            System.out.println(System.currentTimeMillis()-start);
        }
    }

}
