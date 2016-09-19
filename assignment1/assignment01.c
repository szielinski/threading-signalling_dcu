/******************************************************************************
* FILE: assignment01.c
* DESCRIPTION:
*   A program that demonstrates how threads can print even and odd numbers with 
*      the help of UNIX signals.
* AUTHORS: Stephen Kerr       
*          Szymon Zielinski   
* LAST REVISED: 05/11/2011
******************************************************************************/
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <signal.h>
#include <unistd.h>

// Global variables.
int i;
int numLimit = 0;

// A data structure for passing multiple arguments to a pthread.
struct cThread_data{
  int num;
  sigset_t set;
  char * messagePtr;
};

// Cleanup handler that displays a message.
static void
sayGoodbye(void *param)
{
  char * text  = (char*) param;
  printf("%s",text);
}

/* A function for threads that takes in a number and waits for a signal 
from a signal set that was passed in as an argument. When the signal is received
it prints the number to the screen, increments it and signals the parent to let 
it know that it's done using SIGURS2. */
static void * counterThread(void *param)
{  
  struct cThread_data *data;  
  data = (struct cThread_data *) param;
  
  pthread_cleanup_push(sayGoodbye, data->messagePtr);
    
  int number = data->num;
  
  int sig;
  
  while(1)
  {    
    sigwait(&data->set, &sig);
    printf("%d\n", number);
    number += 2;
    kill(getpid(), SIGUSR2);
  }
  pthread_cleanup_pop(1);
  return ((void *)NULL);
}

int main(int argc, char *argv[])
{    
  // Get number limit from the user and parse it.
  numLimit = atoi(argv[1]);
  
  pthread_t printOdds;
  pthread_t printEvens;
  
  // Block SIGUSR1.
  sigset_t sigusr1Set;
  sigemptyset(&sigusr1Set);
  sigaddset(&sigusr1Set, SIGUSR1); 
  pthread_sigmask(SIG_BLOCK, &sigusr1Set, NULL); 
  
  // Block SIGUSR2.
  sigset_t sigusr2Set;
  sigemptyset(&sigusr2Set);
  sigaddset(&sigusr2Set, SIGUSR2);
  pthread_sigmask(SIG_BLOCK, &sigusr2Set, NULL);  
  
  // Fill the first data structure with info for the even thread
  struct cThread_data oddThreadData;
  oddThreadData.num = 1;
  oddThreadData.set = sigusr1Set;
  char goodbyeOdd[] = "Goodbye from the odd-numbers thread.\n";
  oddThreadData.messagePtr = goodbyeOdd;
  
  // Fill the second data structure with info for the odd thread
  struct cThread_data evenThreadData;  
  evenThreadData.num = 2;
  evenThreadData.set = sigusr1Set;
  char goodbyeEven[] = "Goodbye from the even-numbers thread.\n";
  evenThreadData.messagePtr = goodbyeEven;
  
  // Initialise and start threads.  
  pthread_create(&printOdds, NULL, counterThread, (void *) &oddThreadData);
  pthread_create(&printEvens, NULL, counterThread, (void *) &evenThreadData);
  
  /* Signal the threads with SIGUSR1 and wait for each one to acknowledge 
  that it received the signal before continuing. The acknowledgement will come 
  in the form of a separate signal - SIGUSR2. */
  int sig;  
  for(i = 1; i <= numLimit; i+=2)
  {    
    pthread_kill(printOdds, SIGUSR1);  
    sigwait(&sigusr2Set, &sig);  
    if(i == numLimit)
      break;
    pthread_kill(printEvens, SIGUSR1);  
    sigwait(&sigusr2Set, &sig);  
  }
    
  /* Initiate a deferred cancelation in the two threads.
  Cancelation type was not set, the default applies here (deferred). */
  pthread_cancel(printOdds);
  pthread_cancel(printEvens);
  
  // Wait for the threads to finish and clean up after them when they do.
  pthread_join(printOdds, NULL);
  pthread_join(printEvens, NULL);

  // End the program.
  pthread_exit(NULL);
}
