public class Example {
    int x = 5;
    public static void main(String[] args) {
        Example myObj1 = new Example();
        Example myObj2 = new Example();
        boolean equal = compare(myObj1, myObj2);
        return equal; 
    }

    boolean compare(Example obj1, Example obj2) {
        return obj1.x == obj2.x;
    }
}