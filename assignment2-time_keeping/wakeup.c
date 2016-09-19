// To compile using the gcc compiler: "gcc -pthread wakeup.c -lrt"

/******************************************************************************
* FILE: wakeup.c
* DESCRIPTION:
*   A program that demonstrates how threads can handle time-keeping
* 
* AUTHORS: Stephen Kerr       
*          Szymon Zielinski   
* LAST REVISED: 09/12/2011
******************************************************************************/

#include <pthread.h>
#include <stdio.h>
#include <sys/time.h>
#include <time.h>
#include <stdlib.h>
#include <unistd.h>
#include <signal.h>

// global vars for required counting
int noPendingWakeups;
int noServedWakeups;

pthread_mutex_t global_mutex;
pthread_cond_t global_cv;

// head node of a linked list
struct node *head; 

// cleanup handler that releases the global mutex and displays a message
static void
cleanup(void *param)
{
	char * text  = (char*) param;
	printf("%s",text);
	pthread_mutex_unlock(&global_mutex);
}

// cleanup handler that displays a message
static void
sayGoodbye(void *param)
{
  char * text  = (char*) param;
  printf("%s",text);
}

// a data structure for passing multiple arguments to a pthread
struct cThread_data
{
	sigset_t set;
	char * messagePtr;
};

// a data structure for a linked list of pthreads
typedef struct node                                                
{                                         
	pthread_t thread;		// the thread associated with this node
	                      
	long int room;			// room number
	time_t wakeupTime;		// when the thread should wake
	
	struct node *next;		// reference to the next node
};    

/* 
	return a random number of seconds
	range: 'moment of calling + 5 secs' to 'moment of calling + 34 secs' 
*/
time_t getRandTime()
{
	srand(time(NULL));	
	long int r = rand()%30 + 5;
	
	time_t seconds;
	seconds = time (NULL);
	
	return (time_t) (seconds + r);
}

/* 
	This thread will take a time as a parameter and put itself to sleep right 
	after until that time hits, when it hits it will run the alarm and mark 
	itself for deletion for the cleanerThread. Needs to lock since it uses the 
	common linked list. 
*/

static void *alarmThread(void *param)
{
	struct node *supplied;
	supplied = (struct node *) param;
	
	pthread_mutex_lock(&global_mutex);	
	pthread_cleanup_push(cleanup, "");
	
	// convert time_t to struct timespec used by pthread_cond_timedwait
	time_t currentSeconds;
	currentSeconds = time (NULL);	
	
	int long timeDifference = supplied->wakeupTime - currentSeconds;
	
	struct timespec properRep = { 0 };

    clock_gettime(CLOCK_REALTIME, &properRep);
    properRep.tv_sec += timeDifference;
	
	// wait for the specified amount of time
	pthread_cond_timedwait(&global_cv, &global_mutex, &properRep);
	
	// convert time to local time
	struct tm * localTime;
	localTime = localtime ( &supplied->wakeupTime );
			
	// display the alarm message
	printf ( "Wake up: %ld %s\n", supplied->room, asctime (localTime) );	
	
	// temp node for iterating through the linked list
	struct node *temp;
	temp = head; 
	
	// find the node to be deleted
	while (	supplied->wakeupTime != temp-> wakeupTime 
			|| supplied->room != temp->room )
	{
		temp = temp->next;
	}
	
	/* set the room number to an incorrect value to let the cleaner
	know that this node needs to be deleted */
	temp->room = -1;
	
	// update global variables
	noPendingWakeups--;
	noServedWakeups++;
	
	printf ( "Expired alarms: %d\n", noServedWakeups );	
	printf ( "Pending alarms: %d\n\n", noPendingWakeups );	
	
	pthread_cleanup_pop(1);
	return ((void *)NULL);
}

/*
	This thread will clean up after all of the instances of alarmThread.
	It will delete the node associated with the thread, free its memory,
	and call pthread_join on the thread itself. It will run every 30 seconds
	and remove all of the nodes whose value of variable room == -1.
*/
static void *cleanerThread(void *param)
{	
	pthread_cleanup_push(sayGoodbye, "Goodbye from the cleaner thread.\n");
	
	// run until the program closes
	while(1)
	{			
		pthread_mutex_lock(&global_mutex);	
		
		/* need to remember the node to be deleted and the node right before it
		to ensure that the linked list remains functional when the node is 
		deleted */
		
		struct node *temp;
		temp = head; 
	
		struct node *oldTemp;
		oldTemp = head;
		
		// linear search through the whole linked list
		while(temp != NULL)
		{
			if (temp->room == -1)
			{
				oldTemp->next = temp->next;
				pthread_join(temp->thread, NULL);
				free(temp);
				temp = oldTemp->next;	
			}
			else
			{
				oldTemp = temp;
				temp = temp->next;								
			}
		}
		pthread_mutex_unlock(&global_mutex);	
		sleep(30);
	}
	
	pthread_cleanup_pop(1);
	return ((void *)NULL);
}

// this thread will keep creating new alarms and adding them to the linked list
static void *alarmAdder(void *param)
{  
	// initial state
	head = NULL;
	
	char messagePtr[] = "Goodbye from the random alarm-creating thread!\n";
	pthread_cleanup_push(sayGoodbye, messagePtr);
	
	// loop until the program ends
	while(1)
	{			
		pthread_mutex_lock(&global_mutex);		
		
		srand(time(NULL));
		int r = rand()%10000;			// generate random room
		time_t randT = getRandTime();	// generate random time
	
		// display the time to be registered
		struct tm * localTime;
		localTime = localtime ( &randT );
		printf ( "Registering: %d %s\n", r, asctime (localTime) );		
		
		// add the thread to the linked list, and start it		
		struct node *temp;
		pthread_t tempThread;
			
		// allocate space for the node, initialise its variables
		temp = (struct node*)malloc(sizeof(struct node));		
		temp -> room = r;
		temp -> thread = tempThread;
		temp -> wakeupTime = randT;
		temp -> next = head;
		head = temp;	
		
		// start the thread, pass in the alram time as a parameter
		pthread_create(&(temp->thread), NULL, alarmThread, (void *) temp);
		
		noPendingWakeups++;
			
		pthread_mutex_unlock(&global_mutex);	
		
		// wait for 1 second before scheduling another random alarm	
		sleep(1);
	}
	
	pthread_cleanup_pop(1);
	return ((void *)NULL);
}

// this thread will handle the receipt of ctrl-c (SIGINT)
static void * sigWaiter(void *param)
{  
	// parse the parameter
	struct cThread_data *data;  
	data = (struct cThread_data *) param;	
		  
	// wait for the singal (SIGINT in this case)
	int sig;
	sigwait(&data->set, &sig);	
	
	// lock the mutex - the thread WILL make changes to common alarm data
	pthread_mutex_lock(&global_mutex);	
	pthread_cleanup_push(cleanup, data->messagePtr);	
	
	// cancel all threads stored in the linked list
	struct node *temp;
	temp = head; 
	
	while(temp != NULL)
	{
		// send a cancellation request to each alaram thread
		pthread_cancel(temp->thread);
		
		// get next element
		temp = temp->next;
	}
	noPendingWakeups = 0;
	
	// singal the main to let it know that a SIGINT was received	
    kill(getpid(), SIGUSR2);
	
	// release mutex,print message
	pthread_cleanup_pop(1);
	return ((void *)NULL);
}

int main()
{
	// initialise the mutex and a condition variable
	pthread_mutex_init(&global_mutex, NULL);
	pthread_cond_init (&global_cv, NULL);
	
	// block SIGINT
	sigset_t sigintSet;
	sigemptyset(&sigintSet);
	sigaddset(&sigintSet, SIGINT); 
	pthread_sigmask(SIG_BLOCK, &sigintSet, NULL); 
	
	// block SIGUSR2
	sigset_t sigusr2Set;
	sigemptyset(&sigusr2Set);
	sigaddset(&sigusr2Set, SIGUSR2);
	pthread_sigmask(SIG_BLOCK, &sigusr2Set, NULL);  
	
	// initialise counting variables
	noPendingWakeups = 0;
	noServedWakeups = 0;
	
	// declare three main threads
	pthread_t addAlarms;		// for creating random alarams
	pthread_t sigHandler;		// for handling ctrl-c (SIGINT)
	pthread_t cleaner;			// for cleaning up after threads
	
	// sigHandler's data
	struct cThread_data sigHandlerData;
	sigHandlerData.set = sigintSet;
	char goodbyeWaiter[] = "Goodbye from the waiter thread!\n";
	sigHandlerData.messagePtr = goodbyeWaiter;
	
	// start the threads
	pthread_create(&addAlarms, NULL, alarmAdder, NULL);	
	pthread_create(&sigHandler, NULL, sigWaiter, (void *) &sigHandlerData);		
	pthread_create(&cleaner, NULL, cleanerThread, NULL);	
	
	// wait for a SIGUSR2 from sigHandler
	int sig;  
	sigwait(&sigusr2Set, &sig); 
	
	// addAlarms and cleaner are not needed anymore - program is closing
	pthread_cancel(addAlarms);
	pthread_cancel(cleaner);
	
	// make all of the threads in the linked list join here then free the memory
	struct node *temp;
	temp = head; 
	
	while(temp != NULL)
	{
		// make each thread cancelled by sigHandler join here
		pthread_join(temp->thread, NULL);
		
		// free linked list's dynamic memory
		head = temp->next;
		free(temp);
		temp = head;
	}
	
	// join the remaining two threads
	pthread_join(addAlarms, NULL);
	pthread_join(sigHandler, NULL);
	pthread_join(cleaner, NULL);
	
	pthread_mutex_destroy(&global_mutex);
	pthread_cond_destroy(&global_cv);

	// End the program.
	pthread_exit(NULL);
}
