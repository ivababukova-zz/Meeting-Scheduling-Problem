import java.io.File;
import java.io.IOException;
import java.lang.Math;
import java.util.ArrayList;
import java.util.Scanner;

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
    int nMeetings;
    int mAgents;
    int timeslots;
    int[][] attendance; // container for the first matrix of the input file
    int[][] dist; // container for the second matrix of the input file
    ArrayList<ArrayList<Integer>> meet_attend; // each meeting and who is attending it
    // TODO do I need this?
    ArrayList<ArrayList<Integer>> agent_meet; // each agent which meetings he is attending


    // constraint vars:
    IntVar[][] agent_cal; // the calendar of each agent
    IntVar[][] agent_calT = new IntVar[timeslots][mAgents]; // transpose of agent_cal
    IntVar N;
    

    Solve(String fname) throws IOException {
        try (Scanner sc = new Scanner(new File(fname))) {
            nMeetings = sc.nextInt(); // number of meetings to be scheduled
            mAgents = sc.nextInt(); // the number of agents
            timeslots = sc.nextInt(); // timeslots

            attendance = new int[mAgents][nMeetings];
            dist = new int[nMeetings][nMeetings];

            for (int i = 0; i < mAgents; i++) {
                System.out.print("agent: " + sc.next() + "|");
                for (int j = 0; j < nMeetings; j++) {
		            attendance[i][j] = sc.nextInt();
                    System.out.print(" " + attendance[i][j]);
                }
                System.out.println("");

            }
            System.out.println("\n");  

            for (int i = 0; i < nMeetings; i++) {
                System.out.print("meeting: " + sc.next() + "|");
                for (int j = 0; j < nMeetings; j++) {
                    dist[i][j] = sc.nextInt();
                    System.out.print(" " + dist[i][j]);
                }
                System.out.println("");
            }

            solver = new Solver("meeting scheduling problem");
            // -1 if the agent doesn't have meeting allocated in the particular timeslot
            agent_cal=VF.boundedMatrix( "agents' calendar",
                                        mAgents,
                                        timeslots,
                                        -1,
                                        nMeetings-1,
                                        solver);
            int[] agentNofM = new int[mAgents];
            /*
            // load up the transpose of agent_cal:
            for (int i = 0; i < mAgents - 1; i++) {
                for (int j = 0; j < timeslots; j++) {
                    agent_calT[j][i] = agent_cal[i][j];
                }
            }
     */      
            meet_attend = new ArrayList<ArrayList<Integer>>();
            ArrayList<Integer> agents = new ArrayList<Integer>();
            for (int i = 0; i < nMeetings; i++) {
                agents = new ArrayList<Integer>();
                int n = 0;
                for (int j = 0; j < mAgents; j++) {
                    if (attendance[j][i] == 1) {
                        agents.add (n, j);
                        n++;
                    }
                }
                meet_attend.add (i, agents);
            }

            // verify meet_attend: 
            for (int i = 0; i < nMeetings; i++) {
                System.out.println("meeting: " + i + " " + meet_attend.get(i));
            }

            // each agent has exactly 1 occurence in his cal of the meetings he attends
            // all other timeslots in his cal are -1s (-1 means no meeting)
            for (int  i = 0; i< mAgents; i++) {
                N = VF.fixed(1, solver);
                agentNofM[i] = 0;
                for (int m = 0; m < nMeetings; m++) {
                    if (attendance[i][m] == 1) { // meeting m must occur in i's calendar once
                        solver.post(ICF.among(N, agent_cal[i], new int[]{m}));
                        agentNofM[i] = agentNofM[i] + 1;
                    }
                }
                // for each agent's calendar, other values apart from -1 and his meetings
                // numbers must not exist
                N = VF.fixed(timeslots-agentNofM[i],solver);
                solver.post(ICF.among(N, agent_cal[i], new int[]{-1}));
            }

            /* the rest of the problem is equiv to arc consistency problem */

            /* Constraints:
             * 1. travel: distance(2 conseq meetings) < time gap between them
             * 2. synchronise calendars:
             *     -for all agents with the same meeting: the meeting must occur
             *      at the same timeslot in their calendar
             */

            // 1: TODO this code doesn't work
            for(int i = 0; i < mAgents; i++) {
                for(int j = 0; j < timeslots - 1; j++) {
                    
                    int startj;
                    IntVar nextMeet = VF.enumerated("next meeting timeslot", -1, timeslots-1, solver);
                    for (startj = j + 1; startj < timeslots - 1; startj++) {
                        IntVar Startj = VF.fixed(startj, solver);
                        
                        if (agent_cal[i][startj].getValue() != -1) {
                            Constraint a = ICF.arithm(agent_cal[i][startj], "!=", -1);
                            Constraint b = ICF.arithm(nextMeet, "=", Startj);
                            LCF.ifThen(a,b);
                        }
                    }

                    System.out.println("agent: " + i + " current meeting timeslot: " + j + " next meeting timeslot: " + nextMeet.getValue());
                    if (nextMeet.getValue() != -1) {
                        IntVar J = VF.fixed(startj-j, solver);
                        Constraint notFree = ICF.arithm(agent_cal[i][j], "!=", -1);
                        int currM = agent_cal[i][j].getValue();
                        int nextM = agent_cal[i][startj].getValue();
                        IntVar travelT = VF.fixed(dist[currM][nextM], solver);
                        Constraint busy = ICF.arithm (travelT, "<=", J);
                        LCF.ifThen(notFree, busy);
                    }
                }
            }            

            // 2:
            for (int m = 0; m < nMeetings; m++) {
                ArrayList<Integer> agnts = meet_attend.get(m); // all agents that attend meeting m
                if (meet_attend.get(m).size() > 1) {
                    System.out.println("meeting big size: " + meet_attend.get(m).size() + " " + m);
                    for (int a = 0; a < agnts.size() - 1; a++) {                    
                        for (int m1 = 0; m1 < timeslots; m1++) {
                            Constraint a1 = ICF.arithm(agent_cal[agnts.get(a)][m1],"=",m);
                            Constraint a2 = ICF.arithm(agent_cal[agnts.get(a+1)][m1],"=",m);
                            LCF.ifThen(a1, a2);
                        }
                    }
                }
            }

        }

    }

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
        Solve msp = new Solve(args[0]);
        if (msp.solve())
            msp.result();
        else
            System.out.println(false);
        msp.stats();

    }
}
