package parkinglot;

import java.util.concurrent.locks.*;

/*
 * The main class of the program, creates and handles threads, locks and
 * conditions. It also removes random cars form the car park and sends them to 
 * the exit queue - simulates cars that are leaving the car park.
 * 
 * This version of the program gives precedence to the lecturers to 70% of the
 * car park, with the remaining 30% having equal precedence.
 * 
 * If the "student" car park is full (common 30%), and there is 1 student and 1 lecturer
 * at the front of each entrance, the lecturers will always have precedence over
 * the students.
 * 
 * If the common 30% is full, and there are 2 students at the front of the 2
 * entrances, one student will have to be admitted into the "lecturer's" car
 * park, otherwise he'd be blocking the queue until more free spaces appear
 * in the common car park. Note how, because this simulates a real-world 
 * scenario, cars in the queue cannot be "shuffled around" and sorted.
 */

public class UnfairParkingLot{
    
    //one lock for each of the main thread groups
    public static Lock parkingLock;
    public static Lock entranceLock;
    public static Lock exitLock;    
    
    //one condition for each of the important states of the data structures
    public static Condition parkingNotFull;
    public static Condition parkingNotEmpty;
    public static Condition exitNotEmpty;
    
    //used to signal those waiting on the student section of the car park.
    public static Condition studentParking;
    
    //two sections of the car park, one with precedence for the lecturers.
    public static CarPark lecturersPark;   
    public static CarPark studentsPark;   
    
    //used to tell whether both entrances are blocked by waiting students
    public static boolean studAtEntrance1;
    public static boolean studAtEntrance2;
    
    public static void main(String[] args) {
        
        //fair reentrant locks - favor granting access to the longest-waiting thread
        parkingLock = new ReentrantLock(true);
        entranceLock = new ReentrantLock(true);
        exitLock = new ReentrantLock(true);
        
        parkingNotFull = parkingLock.newCondition();
        parkingNotEmpty = parkingLock.newCondition();
        studentParking = parkingLock.newCondition();
        exitNotEmpty = exitLock.newCondition();
        
        lecturersPark = new CarPark(StaticVars.LECTURER_CARPARK_SIZE);  
        studentsPark = new CarPark(StaticVars.STUDENT_CARPARK_SIZE);
        
        //intially, both entrances are empty - no student cars
        studAtEntrance1 = false;
        studAtEntrance2 = false;
        
        // the "real" entrance 1
        Entrance entrance1 = new Entrance(true);
        new Thread(entrance1).start();
        
        // an entrance that is not "entrance1"
        Entrance entrance2 = new Entrance(false);
        new Thread(entrance2).start();
        
        //init and start threads - 2 exits and 2 entrances
        Exit exit1 = new Exit();
        new Thread(exit1).start();
        
        Exit exit2 = new Exit();
        new Thread(exit2).start();
        
        //monitors and prints overall car park data periodically
        MonitorFreeSpaces monitor = new MonitorFreeSpaces();
        new Thread(monitor).start();
        
        while(true){
            if(parkingLock.tryLock()){
                try{ 
                    if(exitLock.tryLock()){
                        try{
                            //remove a car from either the student's section or the lecturer's section and send it to a random exit
                            if(Math.random() <= 0.5){
                                if(!lecturersPark.isEmpty()){
                                    Car temp = lecturersPark.removeRandomCar();
                                    if(Math.random() <= .5)
                                        exit1.add(temp);
                                    else
                                        exit2.add(temp); 

                                    System.out.println("- A vehicle of size " + temp.getSize()/2.0 
                                        + " belonging to a " +temp.getOwner() 
                                        + " parked in the LECTURER's section in position " + temp.getPosition() / 2.0
                                        + " has left the car park and went to an exit.");
                                }                                
                            }
                            else if(!studentsPark.isEmpty()){
                                Car temp = studentsPark.removeRandomCar();
                                if(Math.random() <= .5)
                                    exit1.add(temp);
                                else
                                    exit2.add(temp); 
                            
                                System.out.println("- A vehicle of size " + temp.getSize()/2.0 
                                    + " belonging to a " +temp.getOwner() 
                                    + " parked in the STUDENT's section in position " + temp.getPosition() / 2.0
                                    + " has left the car park and went to an exit.");
                            }
                            
                            //signal exits/entrances
                            parkingNotFull.signalAll();
                            studentParking.signalAll();
                            exitNotEmpty.signalAll();
                        }
                        finally{
                            exitLock.unlock();
                        }
                    }
                } 
                finally {
                    parkingLock.unlock();     
                    try{
                        Thread.sleep((int)(Math.random()*4000));                                
                    }
                    catch (InterruptedException e){}              
                }
            }            
        }
    }
}
