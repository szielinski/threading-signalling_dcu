package parkinglot;

/*
 * Periodically print statistics for the car park .
 */

public class MonitorFreeSpaces implements Runnable{
    public void run(){        
        while(true){            
            if(FairParkingLot.parkingLock.tryLock()){
                try{
                    double overallSpaces = FairParkingLot.cpark.freeSpaces();
                    int standardSpaces = FairParkingLot.cpark.freeStandardSpaces();
                    System.out.println("\n\nThe overall amount of vehicles that are parked: " + FairParkingLot.cpark.amountOfCars());
                    System.out.println("\nThe amount of free parking spaces that are left is: " + overallSpaces);
                    System.out.println("The amount of free parking standard spaces that are left is: " + standardSpaces);
                    System.out.println("The amount of free parking humvee spaces that are left is: " + FairParkingLot.cpark.freeHumveeSpaces());
                    System.out.println("The loss of parking spaces due to \"fragmentation\" is: " + (overallSpaces-standardSpaces) + "\n\n");
                }
                finally{
                    FairParkingLot.parkingLock.unlock();
                    try{
                        Thread.sleep(30000);                                
                    }
                    catch (InterruptedException e){}
                }
            }
        }
    }
}
