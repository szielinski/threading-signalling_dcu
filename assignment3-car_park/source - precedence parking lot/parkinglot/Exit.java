package parkinglot;

import java.util.concurrent.locks.*;
import java.util.*;

/*
 * A thread that will simulate an exit from the car park - has a queue of cars
 * that are waiting to exit the car park and the thread allows them out.
 */

public class Exit implements Runnable{
    
    private LinkedList <Car> exitQueue;
    
    //add a car to the exit queue
    public void add(Car car){
        exitQueue.add(car);
    }
    
    public void run(){
        exitQueue = new LinkedList<Car>();
        while(true){
            
            //try to acquire the parking lot's exit lock
            if(UnfairParkingLot.exitLock.tryLock()){
                try{
                    if(exitQueue.isEmpty()){
                        try{
                            //wait while the exit queue is empty 
                            UnfairParkingLot.exitNotEmpty.await();                               
                        } catch (InterruptedException e){
                            System.out.println("An exit thread has been interrupted while the queue was empty.");
                        }
                    }  
                    else{
                        Car temp = exitQueue.pop();
                        System.out.println("| A vehicle of size " + temp.getSize()/2.0 
                                + " belonging to a " +temp.getOwner() 
                                + " has left the car park through an exit."); 
                    }
                } 
                finally{
                    UnfairParkingLot.exitLock.unlock();
                    try{
                        Thread.sleep((int)(Math.random()*4000));                                
                    }
                    catch (InterruptedException e){}
                }
            }
        }
    }
}
