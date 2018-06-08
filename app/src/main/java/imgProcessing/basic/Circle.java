package imgProcessing.basic;

public class Circle {
    Point absolute;
    Point s;
    int r;

    public Circle(Point s, int r) {
        this.s = s;
        this.r = r;
    }

    public Point getS() {
        return s;
    }

    public void setS(Point s) {
        this.s = s;
    }

    public int getR() {
        return r;
    }

    public void setR(int r) {
        this.r = r;
    }
}
