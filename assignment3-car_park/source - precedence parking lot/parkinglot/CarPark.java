package parkinglot;

import java.util.*;

/*
 * Controls the logic of a car park. Contains the cars, operations on the cars
 * and all car-park-related functions. The boolean occupiedSpaces is twice
 * the size of actual spaces to allow for better, more efficient handling of 
 * vehicles that are 1.5 in size - you don't waste half a space this way if
 * a humvee is parked next to a standard-sized car.
 */

public class CarPark {   
    public static final int NO_FREE_SPACES = -1;
    
    //remember spaces that are occupied for fast search
    private boolean[] occupiedSpaces;
    //remember all cars that are parked
    private LinkedList <Car> parkedCars;
    
    //create an empty car park
    public CarPark(int size){
        occupiedSpaces = new boolean[size*2];
        parkedCars = new LinkedList<>();
        Arrays.fill(occupiedSpaces, false);
    }
    
    //amount of parked cars
    public int amountOfCars(){
        return parkedCars.size();
    }
    
    //free spaces. counts all half-spaces as well (spaces of size 0.5)
    public double freeSpaces(){
        double result = 0;
        
        for(int i = 0; i<occupiedSpaces.length; i++)
            if(occupiedSpaces[i] == false)
                result += 0.5;
        
        return result;
    }
    
    //free spaces. counts only spaces of size 1.0
    public int freeStandardSpaces(){
        int result = 0;
        
        for(int i = 0; i<occupiedSpaces.length-1; i+=2)
            if(occupiedSpaces[i] == false && occupiedSpaces[i+1] == false)
                result++;
        
        return result;
    }
    
    //free spaces. counts only spaces of size 1.5 (humvees)
    public int freeHumveeSpaces(){
        int result = 0;
        
        for(int i = 0; i<occupiedSpaces.length-2; i+=3)
            if(occupiedSpaces[i] == false && occupiedSpaces[i+1] == false && occupiedSpaces[i+2] == false)
                result++;
        
        return result;
    }
    
    //add car to the car park. if not enough space, return false; else return true;
    public boolean addCar(Car car){        
        int space = findSpace(car.getSize());
        
        if(space == NO_FREE_SPACES)
            return false;
        
        for(int i=space; i<space+car.getSize(); i++)
            occupiedSpaces[i] = true;
        
        car.setPosition(space);        
        parkedCars.add(car);
        return true;        
    }
    
    //remove and return a random car from the car park - simulate cars leaving the car park
    public Car removeRandomCar(){
        int size = parkedCars.size();
        
        Random rand = new Random();
        int position = (int)(Math.round(rand.nextDouble()*(size -1)));
        Car temp = parkedCars.remove(position);
        
        position = temp.getPosition();
        
        for(int i=position; i<position+temp.getSize(); i++){
            occupiedSpaces[i] = false;
        }
        
        return temp;
    }
    
    //is the car park empty
    public boolean isEmpty(){
        return parkedCars.size() == 0;
    }    
    
    //is a parking space in the car park empty
    public boolean isEmpty(int space){
        return !occupiedSpaces[space];
    }
    
    //finds a space big enough for the provided argument size (size = 2 for normal car and 3 for a humvee)
    public int findSpace(int size){
        for(int i=0; i<=occupiedSpaces.length-size; i++){
            boolean found = true;
            
            for(int j=i; j<i+size; j++)
                if(!isEmpty(j)){
                    found = false;
                    break;
                }
            
            if(found)
                return i;
        }
        return NO_FREE_SPACES;
    }
}
