package payback.view;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashSet;

import javax.swing.JFrame;
import javax.swing.JPanel;

import payback.Environment;
import vnes.AppletUI;
import vnes.vNES;

@SuppressWarnings( "serial" )
public class vvUI extends AppletUI {
    private Collection<JPanel> panels = new HashSet<JPanel>();
    private Environment environment;
    private volatile long overhead ;

    public vvUI( vNES applet ) {
        super( applet );
        applet.setSize( 256, 240 );
        applet.sound = false;

        try {
            environment = new Environment();
        } catch( Exception e ) {
            e.printStackTrace();
            System.exit( 666 );
        }
        
        addFrame(300, 0, new SpritesPanel(applet));
        addFrame(600, 0, new LabelPanel(environment));
        addFrame(900, 0, new ParamsPanel(environment.getBrute()));

    }

    @Override
    public void onEndFrame() {
        long start = System.currentTimeMillis();

        environment.update((BufferedImage) getScreenView().getImage());
        overhead = System.currentTimeMillis() - start;

        for (JPanel panel : panels) {
            start = System.currentTimeMillis();
            panel.repaint();
            setTitle(panel.getClass().getSimpleName() + " - overhead: " + (System.currentTimeMillis() - start) + "ms");
        }
    }

    private void addFrame( int x, int y, JPanel panel ) {
        JFrame jFrame = new JFrame( panel.getClass().getSimpleName() );
        jFrame.add( panel );
        jFrame.pack();
        jFrame.setLocation( x, y );
        jFrame.setVisible( true );
        panels.add( panel );
    }


}
