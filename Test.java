// MainApplication.java
public class MainApplication {

    public static void main(String[] args) {
        System.out.println("Hello from MainApplication!");

        // Create instances of complex classes
        ComplexClassA complexAInstance = new ComplexClassA();
        ComplexClassB complexBInstance = new ComplexClassB();

        // Call methods from complex classes
        complexAInstance.methodA1();
        complexAInstance.methodA2(5);
        complexAInstance.methodCommon();

        complexBInstance.methodB1("Java");
        complexBInstance.methodB2();
        complexBInstance.methodCommon();
        complexBInstance.methodUniqueToB();
    }
}

// ComplexClassA.java
class ComplexClassA implements CommonInterface {

    void methodA1() {
        System.out.println("Method A1 called.");
    }

    void methodA2(int x) {
		try{}
		catch(Exception e) {}
        System.out.println("Method A2 called with parameter: " + x);
    }

    @Override
    public void methodCommon() {
        System.out.println("Common method implemented in ComplexClassA.");
    }
}

// ComplexClassB.java
class ComplexClassB implements CommonInterface, UniqueInterfaceB {

    void methodB1(String str) {
		try {}
		catch(Exception e) {}
        System.out.println("Method B1 called with parameter: " + str);
    }

    void methodB2() {
        System.out.println("Method B2 called.");
    }

    @Override
    public void methodCommon() {
        System.out.println("Common method implemented in ComplexClassB.");
    }

    @Override
    public void methodUniqueToB() {
        System.out.println("Unique method implemented in ComplexClassB.");
    }
}

// CommonInterface.java
interface CommonInterface {
    void methodCommon();
}

// UniqueInterfaceB.java
interface UniqueInterfaceB {
    void methodUniqueToB();
}
