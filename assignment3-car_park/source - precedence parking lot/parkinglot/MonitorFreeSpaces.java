package parkinglot;

/*
 * Periodically print statistics for the two car park sections.
 */

public class MonitorFreeSpaces implements Runnable{
    public void run(){        
        while(true){            
            if(UnfairParkingLot.parkingLock.tryLock()){
                try{
                    double overallSpaces = UnfairParkingLot.lecturersPark.freeSpaces() + UnfairParkingLot.studentsPark.freeSpaces();
                    int standardSpaces = UnfairParkingLot.lecturersPark.freeStandardSpaces() + UnfairParkingLot.studentsPark.freeStandardSpaces();
                    int humveeSpacesOverall = UnfairParkingLot.lecturersPark.freeHumveeSpaces() + UnfairParkingLot.studentsPark.freeHumveeSpaces();
                    
                    System.out.println("\n\nThe overall amount of vehicles that are parked: " + (UnfairParkingLot.lecturersPark.amountOfCars() + UnfairParkingLot.studentsPark.amountOfCars()));
                    System.out.println("The amount of vehicles that are parked in the STUDENT's section: " + (UnfairParkingLot.studentsPark.amountOfCars()));
                    System.out.println("The amount of vehicles that are parked in the LECTURER's section: " + (UnfairParkingLot.lecturersPark.amountOfCars()));
                    System.out.println("\nThe overall amount of free parking standard spaces that are left is: " + standardSpaces);
                    System.out.println("The overall amount of free parking humvee spaces that are left is: " + humveeSpacesOverall);
                    System.out.println("\nSTUDENT SECTION:\nThe amount of free parking standard spaces that are left is: " + UnfairParkingLot.studentsPark.freeStandardSpaces());
                    System.out.println("The amount of free parking humvee spaces that are left is: " + UnfairParkingLot.studentsPark.freeHumveeSpaces());
                    System.out.println("\nLECTURER SECTION:\nThe amount of free parking standard spaces that are left is: " + UnfairParkingLot.lecturersPark.freeStandardSpaces());
                    System.out.println("The amount of free parking humvee spaces that are left is: " + UnfairParkingLot.lecturersPark.freeHumveeSpaces());
                    System.out.println("\nThe overall loss of parking spaces due to \"fragmentation\" is: " + (overallSpaces-standardSpaces) + "\n\n");
                }
                finally{
                    UnfairParkingLot.parkingLock.unlock();
                    try{
                        Thread.sleep(30000);                                
                    }
                    catch (InterruptedException e){}
                }
            }
        }
    }
}
