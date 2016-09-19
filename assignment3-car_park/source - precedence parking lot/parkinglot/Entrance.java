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
    
    //there are two enterances, this identifies the entrance
    boolean firstEntrance;
    
    //create a entrance that is either the first, or other entrance
    Entrance(boolean firstEntrance){
        this.firstEntrance = firstEntrance;
    }
    
    //add a car to the entrance queue
    public void add(Car car) {
        if (UnfairParkingLot.entranceLock.tryLock()) {
            try {
                entranceQueue.add(car);
            } finally {
                UnfairParkingLot.entranceLock.unlock();
            }
        }
    }

    //add random car to the entrance queue
    public void addRandCar() {
        add(Car.getRandom());
    }

    public void run() {
        entranceQueue = new LinkedList<Car>();

        while (true) {
            //try to acquire the entrance lock
            if (UnfairParkingLot.entranceLock.tryLock()) {
                
                //cars do no queue up at the entrance if the car park is full - simulation of human behaviour
                if ((UnfairParkingLot.lecturersPark.findSpace(StaticVars.STD_SIZE) != CarPark.NO_FREE_SPACES || UnfairParkingLot.studentsPark.findSpace(StaticVars.STD_SIZE) != CarPark.NO_FREE_SPACES) && entranceQueue.size() <= MAX_QUEUE_SIZE) {
                    addRandCar();
                }
                
                //if there are no cars in the queue, loop back to while
                if (entranceQueue.isEmpty()) {
                    continue;
                }

                try {
                    //try to acquire the parking lot lock
                    if (UnfairParkingLot.parkingLock.tryLock()) {
                        try {
                            Car temp = entranceQueue.peek();

                            //a section dealing with student cars
                            if (temp.getOwner().equals(Car.STUDENT_NAME)) {
                                
                                /*
                                 * Case where students are at both entrances and the students' car park is full.
                                 * In this case, try to add the student's car to the lecturer's car park
                                 */
                                if(UnfairParkingLot.studAtEntrance1 && UnfairParkingLot.studAtEntrance2){
                                    if (UnfairParkingLot.lecturersPark.addCar(temp) == false) {
                                        try {
                                            UnfairParkingLot.parkingNotFull.await();
                                        } catch (InterruptedException e) {
                                            System.out.println("An entrance thread has been interrupted while the car park was full.");
                                        }                                     
                                    } else {
                                        temp = entranceQueue.pop();
                                        System.out.println("+ A vehicle of size " + temp.getSize() / 2.0
                                                + " belonging to a " + temp.getOwner()
                                                + " has entered the LECTURER's section of the car park through an entrance and parked in position "
                                                + temp.getPosition() / 2.0);
                                        
                                        //signal the other entrance that the blocking student car has been admitted ot the car park
                                        if(firstEntrance)
                                            UnfairParkingLot.studAtEntrance1 = false;
                                        else
                                            UnfairParkingLot.studAtEntrance2 = false;
                                    }
                                    
                                } 
                                
                                /*
                                 * Case where only one entrance is occupied by a student and the students' car park is full.
                                 * In this case, signal the other entrance that you're blocked.
                                 */
                                else if (UnfairParkingLot.studentsPark.addCar(temp) == false) {
                                    try {
                                        if(firstEntrance)
                                            UnfairParkingLot.studAtEntrance1 = true;
                                        else
                                            UnfairParkingLot.studAtEntrance2 = true;
                                        UnfairParkingLot.studentParking.await();
                                    } catch (InterruptedException e) {
                                        System.out.println("An entrance thread has been interrupted while the car park was full.");
                                    }
                                } else {
                                    temp = entranceQueue.pop();
                                    System.out.println("+ A vehicle of size " + temp.getSize() / 2.0
                                            + " belonging to a " + temp.getOwner()
                                            + " has entered the STUDENT's section of the car park through an entrance and parked in position "
                                            + temp.getPosition() / 2.0);
                                    
                                    //signal the other entrance that the student car has been admitted ot the car park
                                    if(firstEntrance)
                                        UnfairParkingLot.studAtEntrance1 = false;
                                    else
                                        UnfairParkingLot.studAtEntrance2 = false;
                                }
                                UnfairParkingLot.parkingNotEmpty.signalAll();
                            } 
                            
                            /*
                             * A section dealing with lecturer cars - tries to add to
                             * the lecturer car park first, if it's full it 
                             * tries to add to the student car park
                             */
                            else {
                                if (UnfairParkingLot.lecturersPark.addCar(temp) == false) {
                                    if(UnfairParkingLot.studentsPark.addCar(temp) == false){
                                        try {
                                            UnfairParkingLot.parkingNotFull.await();
                                        } catch (InterruptedException e) {
                                            System.out.println("An entrance thread has been interrupted while the car park was full.");
                                        }                                        
                                    } else {
                                        temp = entranceQueue.pop();
                                        System.out.println("+ A vehicle of size " + temp.getSize() / 2.0
                                                + " belonging to a " + temp.getOwner()
                                                + " has entered the STUDENT's section of the car park through an entrance and parked in position "
                                                + temp.getPosition() / 2.0);
                                    }
                                } else {
                                    temp = entranceQueue.pop();
                                    System.out.println("+ A vehicle of size " + temp.getSize() / 2.0
                                            + " belonging to a " + temp.getOwner()
                                            + " has entered the LECTURER's section of the car park through an entrance and parked in position "
                                            + temp.getPosition() / 2.0);
                                }
                                UnfairParkingLot.parkingNotEmpty.signalAll();
                            }
                        } finally {
                            UnfairParkingLot.parkingLock.unlock();
                        }
                    }
                } finally {
                    UnfairParkingLot.entranceLock.unlock();
                    try {
                        Thread.sleep((int) (Math.random() * 2750));
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }
}
