package imgProcessing.basic;

public class Area {

    Point leftUpper;
    Point leftLower;
    Point rigthUpper;
    Point rightLower;

    public Area() {
        leftUpper = new Point();
        leftLower= new Point();
        rigthUpper= new Point();
        rightLower= new Point();
    }

    public Area(Point leftUpper, Point leftLower, Point rigthUpper, Point rightLower) {
        this.leftUpper = leftUpper;
        this.leftLower = leftLower;
        this.rigthUpper = rigthUpper;
        this.rightLower = rightLower;
    }

    public Point getLeftUpper() {
        return leftUpper;
    }

    public void setLeftUpper(Point leftUpper) {
        this.leftUpper = leftUpper;
    }

    public Point getLeftLower() {
        return leftLower;
    }

    public void setLeftLower(Point leftLower) {
        this.leftLower = leftLower;
    }

    public Point getRigthUpper() {
        return rigthUpper;
    }

    public void setRigthUpper(Point rigthUpper) {
        this.rigthUpper = rigthUpper;
    }

    public Point getRightLower() {
        return rightLower;
    }

    public void setRightLower(Point rightLower) {
        this.rightLower = rightLower;
    }
}
