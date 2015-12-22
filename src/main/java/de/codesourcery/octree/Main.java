package de.codesourcery.octree;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Random;
import java.util.function.Consumer;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;

import de.codesourcery.octree.Octree.Node;

public class Main 
{
	static 
	{
		System.loadLibrary("gdx64");
	}

	private static final int POINTS = 20000;
	
	public static final int MAX_OBJS_PER_NODE = 1; // needs to be at least 2
	
	private static final float TRANSLATION = 10f;
	private static final float ROT_DEG = 1f;
	
	private static final int SCREEN_WIDTH = 320;
	private static final int SCREEN_HEIGHT = 240;
	
	private static final float WORLD_SIZE = SCREEN_WIDTH;
	
	private static final Random RND = new Random(0xdeadbeef);
	
    private PerspectiveCamera camera;
    private Octree<MyObj> tree;
	
	protected static final class MyObj implements IObjectWithPosition {

		private final Vector3 position;
		
		public MyObj(Vector3 pos) {
			this.position = new Vector3(pos);
		}
		
		@Override
		public Vector3 getPosition() {
			return position;
		}
	}
	
	protected final class MyPanel extends JPanel {

	    private boolean renderGrid = true;
	    
        private final KeyAdapter adaptor = new KeyAdapter() 
        {
            @Override
            public void keyTyped(KeyEvent e) 
            {
                boolean update = false;
                switch ( e.getKeyChar() ) 
                {
                    case 'g':
                        renderGrid = !renderGrid;
                        update = true;
                        break;
                    case 'a':
                        Vector3 tmp = new Vector3(camera.direction).nor();
                        tmp.crs( camera.up ).nor().scl( TRANSLATION );
                        camera.position.sub( tmp ); 
                        update=true; 
                        break; 
                    case 'd': 
                        tmp = new Vector3(camera.direction).nor();
                        tmp.crs( camera.up ).nor().scl( TRANSLATION );
                        camera.position.add( tmp ); 
                        update=true; 
                        break; 
                    case 'w': 
                        tmp = new Vector3(camera.direction).nor();
                        tmp.scl( TRANSLATION );
                        camera.position.add( tmp ); 
                        update=true;
                        break;
                    case 's': 
                        tmp = new Vector3(camera.direction).nor();
                        tmp.scl( TRANSLATION );
                        camera.position.sub( tmp ); 
                        update=true;        
                        break;
                    case 'r': camera.position.y += TRANSLATION; update=true; break;
                    case 'f': camera.position.y -= TRANSLATION; update=true; break;                     
                    case 'q': 
                        camera.direction.rotate( new Vector3(0,1,0) , ROT_DEG ).nor();
                        update=true; 
                        break;                      
                    case 'e': 
                        camera.direction.rotate( new Vector3(0,1,0) , -ROT_DEG ).nor();
                        update=true; 
                        break;                              
                    default:
                        break;
                }
                if ( update ) 
                {
                    repaint();
                }
            }
        };
        
        {
            setFocusable(true);
            setRequestFocusEnabled(true);
            requestFocus();
            addKeyListener( adaptor );
        }
        
        @Override
        protected void paintComponent(Graphics graphics) 
        {
            super.paintComponent(graphics);
            
            final Graphics2D g = (Graphics2D) graphics;
            
            camera.viewportWidth = getWidth();
            camera.viewportHeight = getHeight();
            camera.fieldOfView = 60f;
            camera.update();
            
            final BoundingBox bb = new BoundingBox();
            final Consumer<Node<MyObj>> c;
            if ( renderGrid ) {
                c = node -> 
                {
//                    if ( node.dataCount > 0 ) {
                        node.visitData( obj -> render(obj,g) );
                        node.populateBoundingBox( bb );
                        render( bb , g );
//                    }
                };
            } else {
                c = node -> node.visitData( obj -> render(obj,g) );
            }
            tree.visit(c);
            renderAxis( g );
        }
        
        protected void renderAxis(Graphics2D g) 
        {
            renderLine( new Vector3(0,0,0) , new Vector3( 20 ,  0 ,  0 ) , Color.GREEN , g );
            renderLine( new Vector3(0,0,0) , new Vector3( 0  , 20 ,  0 ) , Color.RED , g );
            renderLine( new Vector3(0,0,0) , new Vector3( 0  ,  0 , 20 ) , Color.BLUE , g );
        }
        
        private void renderLine(Vector3 p0,Vector3 p1,Color color,Graphics2D g) {
            Point pp0 = project( p0.x ,p0.y ,p0.z );
            Point pp1 = project( p1.x ,p1.y ,p1.z );
            g.setColor( color );
            g.drawLine( pp0.x , pp0.y , pp1.x , pp1.y );
        }
        
        protected void render(MyObj obj,Graphics2D g) 
        {
            final Vector3 pp = obj.getPosition();
            final Point p = project( pp.x,pp.y,pp.z );
            g.setColor( Color.BLUE );
            g.fillRect( p.x - 2 , p.y - 2 , 4 , 4 );
        }
        
        protected void render(BoundingBox bb,Graphics2D g) 
        {
            final Vector3 min = bb.min;
            final Vector3 max = bb.max;
            
            final Point p0 = project( min.x , max.y , max.z );
            final Point p1 = project( min.x , min.y , max.z );
            final Point p2 = project( max.x , min.y , max.z );
            final Point p3 = project( max.x , max.y , max.z );
            
            final Point p4 = project( min.x , max.y , min.z );
            final Point p5 = project( min.x , min.y , min.z );
            final Point p6 = project( max.x , min.y , min.z );
            final Point p7 = project( max.x , max.y , min.z );              
            
            g.setColor( Color.RED );
            draw(g , p0 , p1 , p2 , p3 ); // front
            draw(g , p4 , p5 , p6 , p7 ); // back
            draw(g , p4, p5 , p1 , p0 ); // left
            draw(g , p3, p2 , p6 , p7 ); // right
            draw(g , p4, p0 , p3 , p7 ); // top
            draw(g , p1, p5 , p6 , p2 ); // bottom
        }
        
        private void draw(Graphics2D g,Point...p) 
        {
            final int[] x = new int[p.length];
            final int[] y = new int[p.length];
            for ( int i = 0 ; i < p.length ; i++ )  {
                x[i] = p[i].x;
                y[i] = p[i].y;
            }
            g.drawPolygon( x , y , p.length );
        }
        
        private Point project(float x,float y,float z) 
        {
            Vector3 v = new Vector3(x,y,z);
            camera.project( v , 0 , 0 , getWidth() , getHeight() );                 
            return new Point( (int) v.x , getHeight() - (int) v.y );
        }
    	    
	}
    
    public static void main(String[] args) 
    {
        new Main().run();
    }
    
    private long setupOctree() 
    {
        final Vector3 min = new Vector3(-WORLD_SIZE/2,-WORLD_SIZE/2,-WORLD_SIZE/2);
        final Vector3 max = new Vector3( WORLD_SIZE/2, WORLD_SIZE/2, WORLD_SIZE/2);
        final BoundingBox bb = new BoundingBox(min,max);        
        final MyObj[] objs = new MyObj[POINTS];
        final Vector3 p = new Vector3();
        for ( int i = 0 ; i < POINTS ; i++ ) 
        {
            randomize(p);
            objs[i] = new MyObj( p );             
        }
          
        tree = new Octree<>( bb );

        long time = -System.currentTimeMillis();
        for ( int i = 0 ; i < POINTS ; i++ ) 
        {
            tree.add( objs[i] );
        }
        time += System.currentTimeMillis();
        return time;
    }
    
	public void run() 
	{
	    long time = Long.MAX_VALUE;
	    for ( int i = 0 ; i < 20 ; i++ ) {
	        time = Math.min( time , setupOctree() );
	        System.out.println("Time: "+time);
	    }
	    System.out.println("Best time: "+time);
	    
        camera = new PerspectiveCamera( 60, SCREEN_WIDTH , SCREEN_HEIGHT);
        camera.position.set( 0 , 0 , 450 );
        camera.near = 0.1f;
        camera.far = 1000;
		
        final JFrame frame = new JFrame();
		final MyPanel panel = new MyPanel(); 

		panel.setMinimumSize( new Dimension(SCREEN_WIDTH,SCREEN_HEIGHT));
		panel.setPreferredSize( new Dimension(SCREEN_WIDTH,SCREEN_HEIGHT));
		
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.getContentPane().add( panel );
		frame.pack();
		frame.setVisible( true );
	}
	
	private static void randomize(Vector3 p) 
	{
	    p.x = RND.nextFloat();
	    p.y = -1+RND.nextFloat()*2;
	    p.z = -1+RND.nextFloat()*2;
	    p.nor().scl( WORLD_SIZE/2 );
	}	
}