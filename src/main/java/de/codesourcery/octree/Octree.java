package de.codesourcery.octree;

import java.util.function.Consumer;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;

public class Octree<T extends IObjectWithPosition> 
{
	protected static final int QUAD_0 = 0;
	protected static final int QUAD_1 = 1;
	protected static final int QUAD_2 = 2;
	protected static final int QUAD_3 = 3;
	protected static final int QUAD_4 = 4;
	protected static final int QUAD_5 = 5; 
	protected static final int QUAD_6 = 6;
	protected static final int QUAD_7 = 7;
	
	public static final int MAX_OBJS_PER_NODE = 1;
	
	protected static final class ListNode<T> 
	{
	    public ListNode<T> next;
	    public T value;
	    
	    public ListNode(T value) {
	        this.value = value;
	    }
	    
	    public ListNode(T value,ListNode<T> next) {
	        this.value = value;
	        this.next = next;
	    }
	}
	
	public static final class Node<T extends IObjectWithPosition>
	{
		public final Vector3 center = new Vector3();
		public final float halfWidth;
		
		@SuppressWarnings("unchecked")
		public final Node<T>[] children = new Node[8];
		
		public ListNode<T> data;
		public int dataCount;
		
		public Node(float x,float y,float z,float halfWidth) 
		{
			this.center.set(x,y,z);
			this.halfWidth = halfWidth;
		}
		
		public void clear() 
		{
		    data = null;
		    dataCount = 0;
		    for ( int i = 0 ; i < 8 ; i++ ) {
		        children[i]=null;
		    }
		}
		
		public void visitData(Consumer<T> visitor) 
		{
		    ListNode<T> current = data;
		    while( current != null ) {
		        visitor.accept( current.value );
		        current = current.next;
		    }
		}
		
		private void prependValue(T value) {
		    if ( data == null ) {
		        data = new ListNode<T>(value);
		    } else {
		        data = new ListNode<T>( value , data );
		    }
	        dataCount++;
		}
		
		@Override
		public String toString() 
		{
			final BoundingBox bb = populateBoundingBox( new BoundingBox() );
			final Vector3 dim = bb.getDimensions(new Vector3() );
			final Vector3 cnt = bb.getCenter(new Vector3() );
			return cnt+" [size: "+dim.x+" / "+dim.y+" / "+dim.z+"]";
		}
		
		public Node<T> findNode(Vector3 position) 
		{
			if ( this.contains( position ) ) 
			{
				for ( int i =0 ; i < 8 ; i++ ) 
				{
					if ( children[i] != null ) 
					{
						Node<T> tmp = children[i].findNode( position );
						if ( tmp != null ) {
							return tmp;
						}
					}
				}
				return this;
			}
			return null;
		}
		
		public boolean contains(Vector3 p) {
			
			return p.x >= (center.x-halfWidth) && p.x < (center.x+halfWidth) &&
				   p.y >= (center.y-halfWidth) && p.y < (center.y+halfWidth) &&
				   p.z >= (center.z-halfWidth) && p.z < (center.z+halfWidth);
		}
		
		public void visitNodes(BoundingBox area,Consumer<Node<T>> visitor) 
		{
			if ( ! intersects( area  , this ) ) {
				return;
			}
			for ( int i =0 ; i < 8 ; i++ ) 
			{
				if ( children[i] != null ) 
				{
					children[i].visitNodes( area , visitor );
				}
			}
		}
		
		public void visit(Consumer<Node<T>> visitor) 
		{
			visitor.accept( this );
			for ( int i =0 ; i < 8 ; i++ ) 
			{
				if ( children[i] != null ) 
				{
					children[i].visit( visitor );
				}
			}
		}
		
		public BoundingBox populateBoundingBox(BoundingBox bb) 
		{
			bb.set( new Vector3( center.x - halfWidth , center.y - halfWidth , center.z - halfWidth ) ,
					new Vector3( center.x + halfWidth , center.y + halfWidth , center.z + halfWidth ) );
			return bb;
		}
		
		public void add(T obj) 
		{
			final Vector3 pos = obj.getPosition();
			Node<T> node = findNode( pos );
			if ( node.dataCount == MAX_OBJS_PER_NODE ) 
			{
				node.split();
				node = node.findNode( pos );
			}
			node.prependValue( obj );
		}		
		
		public void split() 
		{
			final float newWidth = halfWidth/2;
			
			/*  1|3     ^ -z 
			 * --+--    |
			 *  2|4     +---> +x
			 *          |
			 *          |
			 *          +z
			 */
			
			// lower half of cube
			final Vector3 center4 = new Vector3(center);
			center4.add( - newWidth , -newWidth , -newWidth );
			
			final Vector3 center5 = new Vector3(center);
			center5.add( - newWidth , -newWidth , newWidth );
			
			final Vector3 center6 = new Vector3(center);
			center6.add( newWidth , -newWidth , -newWidth );
			
			final Vector3 center7 = new Vector3(center);
			center7.add( newWidth , -newWidth , newWidth );		
			
			// upper half of cube			
			final Vector3 center0 = new Vector3(center);
			center0.add( - newWidth , newWidth , -newWidth );
			
			final Vector3 center1 = new Vector3(center);
			center1.add( - newWidth , newWidth , newWidth );
			
			final Vector3 center2 = new Vector3(center);
			center2.add( newWidth , newWidth , -newWidth );
			
			final Vector3 center3 = new Vector3(center);
			center3.add( newWidth , newWidth , newWidth );

			// lower half
			children[QUAD_1] = new Node<T>( center4.x , center4.y , center4.z , newWidth );
			children[QUAD_0] = new Node<T>( center5.x , center5.y , center5.z , newWidth );
			
			children[QUAD_5] = new Node<T>( center6.x , center6.y , center6.z , newWidth );
			children[QUAD_4] = new Node<T>( center7.x , center7.y , center7.z , newWidth );
			
			// upper half
			children[QUAD_3] = new Node<T>( center0.x , center0.y , center0.z , newWidth );
			children[QUAD_2] = new Node<T>( center1.x , center1.y , center1.z , newWidth );
			children[QUAD_7] = new Node<T>( center2.x , center2.y , center2.z , newWidth );
			children[QUAD_6] = new Node<T>( center3.x , center3.y , center3.z , newWidth );
			
			ListNode<T> currentNode = data;
outer:			
			while ( currentNode != null ) 
			{
				final T obj = currentNode.value;
				final Vector3 pos = obj.getPosition();
				for (int j = 0 ; j < 8 ; j++ ) 
				{
					if ( children[j].contains( pos ) ) 
					{
						children[j].add( obj );
						currentNode = currentNode.next;
						continue outer;
					}
				}
				throw new RuntimeException("Unreachable code reached, no child contains "+obj+" @ "+pos);
			}
			data = null;
			dataCount = 0;
		}
	}

	private final Node<T> root;
	
	public Octree(BoundingBox box) 
	{
		if ( box == null ) {
			throw new IllegalArgumentException("box cannot be NULL");
		}
		final Vector3 dim = box.getDimensions( new Vector3() );
		float halfWidth = Math.max( Math.max( dim.x , dim.y ) , dim.z)/2f;
		final Vector3 cx = box.getCenter( new Vector3() );
		this.root = new Node<T>( cx.x ,cx.y , cx.z , halfWidth );
	}
	
	public void add(T obj) 
	{
		root.add( obj );
	}
	
	public void visitNodes(BoundingBox area,Consumer<Node<T>> visitor) {
		root.visitNodes( area , visitor );
	}
	
	public static boolean intersects(BoundingBox bb,Node<?> n) 
	{
		final Vector3 thisDim = bb.getDimensions( new Vector3() );
		final Vector3 thisCenter = bb.getCenter( new Vector3() );
		final Vector3 otherCenter = n.center;
		
		// separating axis theorem
		float lx = Math.abs(thisCenter.x - otherCenter.x);
		float sumx = (thisDim.x / 2.0f) + n.halfWidth;

		float ly = Math.abs(thisCenter.y - otherCenter.y);
		float sumy = (thisDim.y / 2.0f) + n.halfWidth;

		float lz = Math.abs(thisCenter.z - otherCenter.z);
		float sumz = (thisDim.z / 2.0f) + n.halfWidth;

		return (lx <= sumx && ly <= sumy && lz <= sumz);		
	}	
	
	public void visit(Consumer<Node<T>> visitor) {
		root.visit( visitor );
	}
	
	public void clear() {
	    root.clear();
	}
}