
Exercise 2, The Meeting Scheduling Problem (prob046)
----------------------------------------------------

The meeting scheduling problem is described in cspLib http://csplib.org/ as prob046. 


Introduction
------------
The 27 instances in cspLib have been reformatted and are in the problem directory with one file for each problem.
The format of the files can be explained using the problem below. We have 10 meetings (n=10), 5 agents (m=5) and
18 time slots to schedule the meetings. We then have an m by n array X where X[i][j] = 1 iff agent_i attends
meeting_j. There is then an n by n distance array where distance[i][j] is the time to travel from meeting_i
to meeting_j. All meetings take one unit of time and the first time slot is 0 (zero). 

10 5 18

0: 0 0 0 0 0 0 0 1 0 1
1: 0 1 1 0 1 1 1 1 0 1
2: 0 0 0 0 0 1 0 1 1 0
3: 0 1 0 0 1 0 0 1 0 0
4: 1 0 1 0 0 0 1 1 1 0

0: 0 1 2 1 1 2 1 2 2 2
1: 1 0 2 2 1 1 1 1 1 2
2: 2 2 0 2 1 2 1 1 1 2
3: 1 2 2 0 1 2 1 2 2 1
4: 1 1 1 1 0 2 1 2 1 2
5: 2 1 2 2 2 0 2 1 2 2
6: 1 1 1 1 1 2 0 1 2 1
7: 2 1 1 2 2 1 1 0 2 2
8: 2 1 1 2 1 2 2 2 0 2
9: 2 2 2 1 2 2 1 2 2 0


The problem is then to schedule the meetings in the given amount of time (0 to timeSlots-1). Below is a solution
to the above problem (rProblems/10-5-30-2-00.txt) where meeting_0 starts at time 12, meeting_1 starts
at time 15, ..., meeting_9 starts at time 12. This took 24 milliseconds to solve and 11 nodes.

>> java Solve rProblems/10-5-30-2-00.txt
0 12
1 15
2 0
3 0
4 9
5 6
6 2
7 4
8 9
9 12
24[+0] millis.
11[+0] nodes

We can validate that this is a solution using the program Validate that produces a small gantt chart and
checks if the schedule is valid. NOTE: it does NOT check that the schedule is within the time window given.

>> java Validate rProblems/10-5-30-2-00.txt rProblems/solve-10-5-30-2-00.txt 
 0: ------------X (12)
 1: ---------------X (15)
 2: X (0)
 3: X (0)
 4: ---------X (9)
 5: ------X (6)
 6: --X (2)
 7: ----X (4)
 8: ---------X (9)
 9: ------------X (12)
true

Also note that the above schedule is NOT optimal! Below is an optimal schedule that uses only 13 time slots,
and program Optimize took 202 millisecond to run and explored 227 nodes.

>> java Optimize rProblems/10-5-30-2-00.txt
0 12
1 6
2 0
3 0
4 8
5 4
6 10
7 2
8 7
9 12
202[+0] millis.
227[+0] nodes


What you have to do
-------------------
 1. Code up Solve.java and test it on the cspLib instances in problems/* and the new instances
    in rProblems/*. Use the Validate program to validate your results. NOTE: there are insolvable 
    cspLib instances, and there are meetings that no one goes to!!!!

 2. Code up Optimize.java and run this on the cspLib problems and the instances rProblems/*. NOTE:
    some of these are extremely hard. Therefore modify Optimize so that it takes an optional second
    parameter, a cpu time limit in milliseconds, such that you can report best-found-so-far within
    a given amount of time, i.e.

    >> java Optimize rProblems/40-10-20-9-00.txt 10000

    will terminate after 10 seconds cpu time and report the best result found so far, and 

    >> java Optimize rProblems/40-10-20-9-00.txt

    will terminate (eventually, but maybe not in your lifetime) with the optimum solution.


 3. Investigate alternative variable ordering heuristics for the optimization problem

 4. Produce a report, no more than 3 pages that includes the following

    - a description of your model for Solve and how you modified this for Optimize
    - a description of the heuristics used and what they mean in the context of this problem
    - computational results for Solve and Optimize over the problems/* and rProblems/*
      possibly taking into consideration different heuristics and various cpu time limits
    - a discussion on an alternative model (that you should not implement, only discuss)

 5. Email me Solve.java, Optimize.java and your report.

 6. Your programs MUST be named as above and MUST run on the command line as above and MUST produce
    output in the format as above. This is essential otherwise your submission may be rejected 
    and no marks will be awarded



Patrick Prosser
    







