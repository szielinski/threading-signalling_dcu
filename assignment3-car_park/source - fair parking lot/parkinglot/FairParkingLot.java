package parkinglot;

import java.util.concurrent.locks.*;

/*
 * The main class of the program, creates and handles threads, locks and
 * conditions. It also removes random cars form the car park and sends them to 
 * the exit queue - simulates cars that are leaving the car park.
 */

public class FairParkingLot{
    
    //one lock for each of the main thread groups
    public static Lock parkingLock;
    public static Lock entranceLock;
    public static Lock exitLock;    
    
    //one condition for each of the important states of the data structures
    public static Condition parkingNotFull;
    public static Condition parkingNotEmpty;
    public static Condition exitNotEmpty;
    
    public static CarPark cpark;        
    
    public static void main(String[] args) {
        
        //fair reentrant locks - favor granting access to the longest-waiting thread
        parkingLock = new ReentrantLock(true);
        entranceLock = new ReentrantLock(true);
        exitLock = new ReentrantLock(true);
        
        parkingNotFull = parkingLock.newCondition();
        parkingNotEmpty = parkingLock.newCondition();
        exitNotEmpty = exitLock.newCondition();
        
        cpark = new CarPark(StaticVars.CARPARK_SIZE);          
        
        //init and start threads - 2 exits and 2 entrances
        Entrance entrance1 = new Entrance();
        new Thread(entrance1).start();
        
        Entrance entrance2 = new Entrance();
        new Thread(entrance2).start();
        
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
                            //remove cars from the parking lot and send them to a random exit
                            if(!cpark.isEmpty()){
                                Car temp = cpark.removeRandomCar();
                                if(Math.random() <= .5)
                                    exit1.add(temp);
                                else
                                    exit2.add(temp); 
                            
                                System.out.println("- A vehicle of size " + temp.getSize()/2.0 
                                    + " belonging to a " +temp.getOwner() 
                                    + " parked in position " + temp.getPosition() / 2.0
                                    + " has left the car park and went to an exit.");
                            }
                            
                            //signal exits and entrances
                            parkingNotFull.signalAll();
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
