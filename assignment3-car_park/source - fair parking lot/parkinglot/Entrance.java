package parkinglot;

import java.util.concurrent.locks.*;
import java.util.*;

/*
 * A thread that will simulate an entrance to the car park - generates cars,
 * queues them up and admits them to the car park whenever there's space
 * and the needed locks are acquired. At most, 10 cars will line up in the
 * entrance queue.
 */

public class Entrance implements Runnable {

    private static final int MAX_QUEUE_SIZE = 10;
    private LinkedList<Car> entranceQueue;

    //add a car to the entrance queue
    public void add(Car car) {
        if (FairParkingLot.entranceLock.tryLock()) {
            try {
                entranceQueue.add(car);
            } finally {
                FairParkingLot.entranceLock.unlock();
            }
        }
    }

    //add random car to the entrance queue
    public void addRandCar() {
        add(Car.getRandom());
    }

    public void run() {
        entranceQueue = new LinkedList<Car>();

        //run indefinitely
        while (true) {
            //try to acquire the entrance lock
            if (FairParkingLot.entranceLock.tryLock()) {
                
                //cars do no queue up at the entrance if the car park is full - simulation of human behaviour
                if (FairParkingLot.cpark.findSpace(StaticVars.STD_SIZE) != CarPark.NO_FREE_SPACES && entranceQueue.size() <= MAX_QUEUE_SIZE) {
                    addRandCar();
                }
                
                //if there are no cars in the queue, loop back to while
                if(entranceQueue.isEmpty())
                    continue;

                try {
                    //try to acquire the parking lot lock
                    if (FairParkingLot.parkingLock.tryLock()) {
                        try {
                            Car temp = entranceQueue.peek();
                            
                            //if the car park is full - wait. otherwise place it in the parking lot and remove it from the entrance queue
                            if (FairParkingLot.cpark.addCar(temp) == false) {
                                try {
                                    FairParkingLot.parkingNotFull.await();
                                } catch (InterruptedException e) {
                                    System.out.println("An entrance thread has been interrupted while the car park was full.");
                                }
                            } else {
                                temp = entranceQueue.pop();
                                System.out.println("+ A vehicle of size " + temp.getSize()/2.0 
                                        + " belonging to a " +temp.getOwner() 
                                        + " has entered the car park through an entrance and parked in position "
                                        + temp.getPosition() / 2.0);
                            
                                //signal that the car park is no longer empty
                                FairParkingLot.parkingNotEmpty.signalAll();
                            }
                        } finally {
                            FairParkingLot.parkingLock.unlock();
                        }
                    }
                } finally {
                    FairParkingLot.entranceLock.unlock();
                    
                    //wait for a random amout of time - slows down the program for the simulation
                    try {
                        Thread.sleep((int)(Math.random()*2750));    
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }
}
