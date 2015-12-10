import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.Arrays;

import org.chocosolver.solver.*;
import org.chocosolver.solver.variables.*;
import org.chocosolver.solver.constraints.*;
import org.chocosolver.solver.search.loop.monitors.*;
import org.chocosolver.solver.search.strategy.IntStrategyFactory;
import org.chocosolver.solver.search.strategy.*;
import org.chocosolver.solver.search.strategy.selectors.variables.DomOverWDeg;
import org.chocosolver.solver.search.strategy.selectors.variables.*;
import org.chocosolver.solver.search.strategy.selectors.variables.ActivityBased;

/*
 * This is an optimized solution of the meeting scheduling problem
 * The meeting scheduling problem is described in cspLip:
 * http://csplib.org/ as prob046
 */

public class Optimize {

  // process input
  Solver solver;
  int nMeetings; // number of meetings to be scheduled
  int mAgents; // the number of agents
  int[][] attendance; // container for the first matrix of the input file (each agent and his meetings attendance)
  int[][] distance; // container for the second matrix of the input file (distance between meetings)
  int maxBound = 0;
  int timeLimit;
  String fname;

  /*
   * IntVar[] that contains all meetings (of length nMeetings). The value of meeting[i]
   * corresponds to the timeslot in which meeting i occurs. The value of meeting[i]
   * for 0 <= i < nMeetings is [0, timeslots).
   */
  IntVar[] meeting; 
  
  /**
   * intvar for the optimal number of timeslots. We will try to minimize this variable
   */
  IntVar timeslots;

  Optimize(String fname)  throws IOException {
      this.fname = fname;
      this.timeLimit = -1; // timeLimit is not specified from the user, make it -1
      optimize();
  }

  Optimize(String fname, int timeLimit) throws IOException {
      this.fname = fname;
      this.timeLimit = timeLimit; // make timeLimit = time limit specified by the user
      optimize();
  }

  private void optimize() throws IOException {
    try (Scanner sc = new Scanner(new File(this.fname))) {

      /* process input */
      nMeetings = sc.nextInt();
      mAgents = sc.nextInt();
      sc.nextInt();

      attendance = new int[mAgents][nMeetings]; // this is only needed to compute which meetings can be in parallel
      distance = new int[nMeetings][nMeetings]; // this is used to apply the travel contraints

      /* construct attendance matrix from input file */
      for (int i = 0; i < mAgents; i++) {
          int n = 0;
          sc.next();
          for (int j = 0; j < nMeetings; j++) {
		      attendance[i][j] = sc.nextInt();
          }
      }    

      /* construct distance matrix from input file*/
      for (int i = 0; i < nMeetings; i++) {
          sc.next();
          for (int j = 0; j < nMeetings; j++) {
              distance[i][j] = sc.nextInt();
          }
      }

      /*
       * find a path from meeting 0 to the last meeting + meeting
       * durations. The result will be our timeslots max bound
       */
      maxBound = findPath(); 

      solver = new Solver("meeting scheduling problem"); // create an instance of Solver

      timeslots = VF.bounded("optimal timeslots", 0, maxBound, solver);
      // value of meeting[i] is the timeslot in which meeting i occurs
      meeting = VF.enumeratedArray("all meetings", nMeetings, 0, maxBound, solver);

      // constraint that some meetings cannot be in parallel
      for (int m1 = 0; m1 < nMeetings; m1 ++) {
          solver.post(ICF.arithm(meeting[m1], "<=", timeslots));
          for (int m2 = m1 + 1; m2 < nMeetings; m2++) {
              boolean canbeparallel = canBeParallel(m1,m2);
              if(!canbeparallel) {
                /*
                 * for each two meetings that cannot occur in parallel,
                 * make sure that their distance is less than the difference
                 * of their timeslots
                 * i.e.: |meeting[m1] - meeting[m2]| > distance[m1][m2]
                 */
                Constraint eq1 = ICF.arithm(meeting[m1],"-",meeting[m2],">",distance[m1][m2]);
                Constraint eq2 = ICF.arithm(meeting[m2],"-",meeting[m1],">",distance[m1][m2]);
                solver.post(LCF.or(eq1,eq2));
              }
                
          }
      }
    }
  }

  // find the path and duration from meeting 0 to meeting nMeetings
  // add the duration of each meeting (1)
  // produces the maximum timeslots number that can be required for solving the problem
  int findPath() {
      int meetingsPath = 0;
      for (int m1 = 0; m1 < nMeetings - 1; m1++) {
          meetingsPath = meetingsPath + distance[m1][m1 + 1] + 1;
      }
      // System.out.println("path: " + meetingsPath);
      return meetingsPath;
  }

  /* Checks whether meeting m1 and meeting m2 can occur in parallel
   *     if yes: returns true
   *     if no: returns false 
   * meetings m1 and m2 can occur in parallel iff there does not exist any agent that has to
   * attend both of them.
   */
  boolean canBeParallel(int m1, int m2) {
      boolean notBothAttended = true;
      for (int  i = 0; i < mAgents; i++) {
          if(attendance[i][m1] == 1 && attendance[i][m2] == 1) {
              return false;
          }
      }
      return notBothAttended;
  } 

  void result() {

    // if the user has specified time limit, limit the time for solving the problem
    // to timeLimit miliseconds
    if (this.timeLimit != -1) {
        SMF.limitTime(solver,timeLimit*1000); 
    }
    // find a solution for optimal number of timeslots between 0 and maxBound
    // ResolutionPolicy.MINIMIZE means that we try to minimize the value of timelsots
    solver.findOptimalSolution(ResolutionPolicy.MINIMIZE, timeslots);
    for (int i = 0; i < nMeetings; i++) {
        System.out.println(i + " " + meeting[i].getValue()); // print solutions
    }
  }

  void stats() {
    System.out.println(timeslots + " ["+ timeslots.getLB() +","+ timeslots.getUB() +"]");
    System.out.println("nodes: " + solver.getMeasures().getNodeCount()
                    + "   cpu: " + solver.getMeasures().getTimeCount());
  }
    
  public static void main(String[] args) throws IOException {
    Optimize msp;
    if (args.length == 1 ) { // if timeLimit is specified
        msp = new Optimize(args[0]);
    }
    else {
        int timeLimit = Integer.parseInt(args[1]);
        msp = new Optimize(args[0], timeLimit);
    }
        msp.result();
    msp.stats();

  }
}
