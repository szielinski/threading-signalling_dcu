package parkinglot;

import java.util.Random;

/*
 * A class representing a car. Has an owner, size and a position in the car park
 * after entering the car park.
 */

public class Car {
    private static final double CHANCE_OF_STUDENT = 0.75; 
    private static final double CHANCE_OF_HUMVEE = 0.25; 
    private static final String STUDENT_NAME = "student";
    private static final String LECTURER_NAME = "lecturer";
    
    private String owner;
    private int size;
    private int position;
    
    Car(String owner, int size){
        this.owner = owner;
        this.size = size;
        position = -1;
    }
    
    public String getOwner(){
        return owner;
    }
    
    public int getSize(){
        return size;
    }
    
    public int getPosition(){
        return position;
    }
    
    //used to set the car's position when it enters the car park
    public void setPosition(int position){
        this.position = position;
    }
    
    //generate a student car
    public static Car getStudentCar(){
        return new Car(STUDENT_NAME, StaticVars.STD_SIZE);
    }
    
    //generate a lecturer car. can be a standard-sized car or a humvee
    public static Car getLecturerCar(){
        Random rand = new Random();
        if(rand.nextDouble() > CHANCE_OF_HUMVEE)
            return new Car(LECTURER_NAME, StaticVars.STD_SIZE);
        return new Car(LECTURER_NAME, StaticVars.HUMVEE_SIZE);
    }
    
    //get a random car. could be a student's or a lecturer's car
    public static Car getRandom(){
        Random rand = new Random();
        if(rand.nextDouble() > CHANCE_OF_STUDENT)
            return getLecturerCar();
        return getStudentCar();
    }    
}
