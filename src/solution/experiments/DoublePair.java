package solution.experiments;

public class DoublePair
{
    private double first;
    private double second;
    
    public DoublePair(double first, double second)
    {
        this.first = first;
        this.second = second;
    }
    
    public double getFirst()
    {
        return first;
    }
    
    public double getSecond()
    {
        return second;
    }
    
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        DoublePair that = (DoublePair) o;
        
        if (Double.compare(that.first, first) != 0) return false;
        return Double.compare(that.second, second) == 0;
    
    }
    
    public int hashCode()
    {
        int result;
        long temp;
        temp = Double.doubleToLongBits(first);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(second);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
