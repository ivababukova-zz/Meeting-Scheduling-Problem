import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.Arrays;

import org.chocosolver.solver.*;
import org.chocosolver.solver.variables.*;
import org.chocosolver.solver.constraints.*;


/*
 * This is a solution of the meeting scheduling problem
 * The meeting scheduling problem is described in cspLip:
 * http://csplib.org/ as prob046
 */

public class Solve {

  // process input
  Solver solver;
  int nMeetings; // number of meetings to be scheduled
  int mAgents; // the number of agents
  int timeslots; // timeslots available
  int[][] attendance; // container for the first matrix of the input file (each agent and his meetings attendance)
  int[][] distance; // container for the second matrix of the input file (distance between meetings)

  /*
   * IntVar that contains all meetings (of length nMeetings). The value of meeting[i]
   * corresponds to the timeslot in which meeting i occurs. The value of meeting[i]
   * for 0 <= i < nMeetings is [0, timeslots).
   */
  IntVar[] meeting; 
    

  Solve(String fname) throws IOException {
    try (Scanner sc = new Scanner(new File(fname))) {

      /* process input */
      nMeetings = sc.nextInt();
      mAgents = sc.nextInt();
      timeslots = sc.nextInt();

      attendance = new int[mAgents][nMeetings]; // this is only needed to compute which meetings can be in parallel
      distance = new int[nMeetings][nMeetings]; // this is used to apply the travel contraints

      /* construct attendance matrix */
      for (int i = 0; i < mAgents; i++) {
          int n = 0;
          sc.next();
          for (int j = 0; j < nMeetings; j++) {
		      attendance[i][j] = sc.nextInt();
          }
      }    

      /* construct distance matrix */
      for (int i = 0; i < nMeetings; i++) {
          sc.next();
          for (int j = 0; j < nMeetings; j++) {
              distance[i][j] = sc.nextInt();
          }
      }

      solver = new Solver("meeting scheduling problem"); // create an instance of Solver

      // value of meeting[i] is the timeslot in which meeting i occurs
      meeting = VF.enumeratedArray("all meetings", nMeetings, 0, timeslots - 1, solver);

      // constraint that some meetings cannot be in parallel
      for (int m1 = 0; m1 < nMeetings; m1 ++) {
          for (int m2 = m1 + 1; m2 < nMeetings; m2++) {
              boolean canbeparallel = canBeParallel(m1,m2);
              if(!canbeparallel) {
                  /*
                   * for each two meetings that cannot occur in parallel, make sure that their
                   * distance is less than the difference of their timeslots
                   * i.e.: |meeting[m1] - meeting[m2]| > distance[m1][m2]
                   * By doing some calculations, this is equivalent to:
                   *     meeting[m1] - meeting[m2] > distance[m1][m2]
                   *                      OR
                   *     meeting[m2] - meeting[m1] > distance[m1][m2]
                   */
                  Constraint eq1 = ICF.arithm(meeting[m1],"-",meeting[m2],">",distance[m1][m2]);
                  Constraint eq2 = ICF.arithm(meeting[m2],"-",meeting[m1],">",distance[m1][m2]);

                  solver.post(LCF.or(eq1,eq2));
              }
          }
      }
    }
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

  boolean solve() {
    return solver.findSolution();
  }

  void result() {

    for (int i = 0; i < nMeetings; i++) {
        System.out.println(i + " " + meeting[i].getValue());
    }
  }

  void stats() {
    System.out.println("nodes: " + solver.getMeasures().getNodeCount() + "   cpu: " + solver.getMeasures().getTimeCount());
  }
    
  public static void main(String[] args) throws IOException {
    Solve msp = new Solve(args[0]);
    if (msp.solve())
        msp.result();
    else
        System.out.println(false);
    msp.stats();

  }
}
