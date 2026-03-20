package v1.shared;

import java.io.Serializable;

public class Point implements Serializable {
    private static final long serialVersionUID = 1L;
    public int x, y;

    public Point(int x, int y){
        this.x = x;
        this.y = y;
    }

    public boolean equals(Object o){
        if(!(o instanceof Point)) return false;
        Point p = (Point)o;
        return this.x == p.x && this.y == p.y;
    }
}
