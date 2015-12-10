import java.io.File;
import java.io.IOException;
import java.lang.Math;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Arrays;
import java.util.Collections;

import org.chocosolver.solver.*;
import org.chocosolver.solver.variables.*;
import org.chocosolver.solver.constraints.*;
import org.chocosolver.util.tools.ArrayUtils;


/*
 * This is a solution of the meeting scheduling problem
 * The meeting scheduling problem is described in cspLip:
 * http://csplib.org/ as prob046
 */

public class SolveDumb {

    // process input
    Solver solver;
    int nMeetings;
    int mAgents;
    int timeslots;
    int[][] attendance; // container for the first matrix of the input file
    int[][] dist; // container for the second matrix of the input file
    ArrayList<ArrayList<Integer>> meet_attend; // agent_meet[i]: all meetings agent i attends
    ArrayList<ArrayList<Integer>> agent_meet; // meet_attend[i]: all agents that attend meeting i


    IntVar[][] agent_cal; // the calendar of each agent
    IntVar ONE;
    IntVar N;
    

    SolveDUmb(String fname) throws IOException {
      try (Scanner sc = new Scanner(new File(fname))) {
        nMeetings = sc.nextInt(); // number of meetings to be scheduled
        mAgents = sc.nextInt(); // the number of agents
        timeslots = sc.nextInt(); // timeslots

        attendance = new int[mAgents][nMeetings];
        dist = new int[nMeetings][nMeetings];
        agent_meet = new ArrayList<ArrayList<Integer>>();            
        meet_attend = new ArrayList<ArrayList<Integer>>();

        // construct attendance and agent_meet arrays:
        for (int i = 0; i < mAgents; i++) {
            ArrayList<Integer> meetings = new ArrayList<Integer>();
            int n = 0;
            sc.next();
            for (int j = 0; j < nMeetings; j++) {
		        attendance[i][j] = sc.nextInt();
                if (attendance[i][j] == 1) {
                    meetings.add (n, j);
                    n++;
                }
            }
            agent_meet.add (i, meetings);
        }

        // construct meet_attend:
        for (int i = 0; i < nMeetings; i++) {
            ArrayList<Integer> agents = new ArrayList<Integer>();
            int n = 0;
            for (int j = 0; j < mAgents; j++) {
                if (attendance[j][i] == 1) {
                    agents.add (n, j);
                    n++;
                }
            }
            meet_attend.add (i, agents);
        }

        // construct dist[][] array
        for (int i = 0; i < nMeetings; i++) {
            sc.next();
            for (int j = 0; j < nMeetings; j++) {
                dist[i][j] = sc.nextInt();
            }
        }

        // print meet_attend: 
        for (int i = 0; i < nMeetings; i++) {
            System.out.println("meeting: " + i + " agents: " + meet_attend.get(i));
        }
        System.out.println("\n"); 
        // print agent_meet: 
        for (int i = 0; i < mAgents; i++) {
            System.out.println("agent: " + i + " meetings: " + agent_meet.get(i));
        } 
        System.out.println("\n"); 
            

        solver = new Solver("meeting scheduling problem");
        // -1 if the agent doesn't have meeting allocated in the particular timeslot
        agent_cal=VF.boundedMatrix( "agents' calendar",
                                    mAgents,
                                    timeslots,
                                    -1,
                                    nMeetings-1,
                                    solver);


        // each agent has exactly 1 occurence in his cal of the meetings he attends
        // all other timeslots in his cal are -1s (-1 means no meeting)
        // for each agent's calendar, other values apart from -1 and his meetings
        // numbers must not exist
        IntVar ONE = VF.fixed("one",1,solver);
        for (int  i = 0; i< mAgents; i++) {
            ArrayList<Integer> meetings = new ArrayList<Integer>();
            meetings = agent_meet.get(i);
            for (int m = 0; m < meetings.size(); m++) {
                solver.post(ICF.among(ONE, agent_cal[i], new int[]{meetings.get(m)}));
            }
            N = VF.fixed(timeslots-meetings.size(),solver);
            solver.post(ICF.among(N, agent_cal[i], new int[]{-1}));
        }

        /* Constraints:
         * 1. travel: distance(2 conseq meetings) < time gap between them
         * 2. synchronise calendars:
         *     -for all agents with the same meeting: the meeting must occur
         *      at the same timeslot in their calendar
         */

/*        // this is done so that we start from the agent with the most meetings
        Integer[] numbOfMeet_sorted = new Integer[mAgents];
        Integer[] numbOfMeet = new Integer[mAgents];
        for (int i = 0; i < mAgents; i++) {
            numbOfMeet[i] = agent_meet.get(i).size();
        }
        numbOfMeet_sorted = numbOfMeet_sort (numbOfMeet);
*/
        // 1: post travel constraints for agents with more than 1 meeting
        for (int i = 0; i<mAgents; i++) {
//            int agent = numbOfMeet_sorted[i];
            if (agent_meet.get(agent).size() > 1) {
                constrainTravel(agent);
            }
        }
          
        // 2:
        for (int m = 0; m < nMeetings; m++) {
            ArrayList<Integer> agents = meet_attend.get(m); // all agents that attend meeting m
            if (agents.size() > 1) {
                calendarSync(agents,m);
            }
        }



      }
    }
   
    // TODO this code doesn't work agent_cal[agent][slot].getValue() always returns -1
    void constrainTravel(int agent) {
      for(int slot = 0; slot < timeslots; slot++) {
          //System.out.println("agent: " + agent + " slot: " + slot + " meeting in the slot: " + agent_cal[agent][slot].getValue());
          if (agent_cal[agent][slot].getValue() != -1) {
              int nextS = -1;
              int currM = agent_cal[agent][slot].getValue();
              int nextM = -1;
              for (int nextSlot = slot + 1; nextSlot < timeslots; nextSlot++) {                   
                  if (agent_cal[agent][nextSlot].getValue() != -1) {
                      nextS = nextSlot;
                      nextM = agent_cal[agent][nextS].getValue();
                      break;
                  }
              }                    
              if (nextS != -1 && nextM != -1) {
                  IntVar SLOT_DISTANCE = VF.fixed(nextS-slot, solver);
                  IntVar TRAVEL_TIME = VF.fixed(dist[currM][nextM], solver);
                  System.out.println("agent: " + agent + " slot " + slot + " nextS: " + nextS + " slots distance " + SLOT_DISTANCE + " travel time: " + TRAVEL_TIME);
                  solver.post(ICF.arithm (SLOT_DISTANCE, "=>", TRAVEL_TIME));
              }
          }
      }
    }

    // make meeting m1 to be in the same timeslot for each agent who attends it
    void calendarSync (ArrayList<Integer> agents, int m) {
      for (int a = 0; a < agents.size() - 1; a++) {                    
          for (int m1 = 0; m1 < timeslots; m1++) {
              Constraint a1 = ICF.arithm(agent_cal[agents.get(a)][m1],"=",m);
              Constraint a2 = ICF.arithm(agent_cal[agents.get(a+1)][m1],"=",m);
              LCF.ifThen(a1, a2);
          }
      }
    }
/*  this is for speedup of applying the travel constraint
    // input: an array of integers
    //     numbOfMeet[i] = number of meetings agent i has to attend
    // output: sorted array of integers in decreasing order
    //     agent_meet_sorted[0] contains the agent that has most meetings
    //     agent_meet_sorted[mAgents] contains the agent that has least meetings   
    Integer[] numbOfMeet_sort (Integer[] numbOfMeet) {
        Arrays.sort(numbOfMeet, Collections.reverseOrder());
        System.out.println("sorted: " + Arrays.toString(numbOfMeet));
        Integer[] returned = new Integer[mAgents];
        for (int i = 0; i < mAgents; i++) {
            for (int j = 0; j < mAgents; j++) {
                if ()
            }
        }
        return returned;
    }
*/
    boolean solve() {
      return solver.findSolution();
    }

    void result() {
      for (int i = 0; i < mAgents; i++) {
        System.out.print("agent: " + i + ": |");
          for (int j = 0; j < timeslots; j++) {
              System.out.print(agent_cal[i][j].getValue() + "|");
          }
          System.out.println("\n");
      }
    }

    void stats() {
      System.out.println("nodes: " + solver.getMeasures().getNodeCount() + "   cpu: " + solver.getMeasures().getTimeCount());
    }
    
    public static void main(String[] args) throws IOException {
      SolveDumb msp = new SolveDumb(args[0]);
      if (msp.solve())
          msp.result();
      else
          System.out.println(false);
      msp.stats();

    }
}
