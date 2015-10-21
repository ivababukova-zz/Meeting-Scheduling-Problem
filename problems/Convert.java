import java.util.*;
import java.io.*;

public class Convert {

    static int[][] distance; // travel distance between meetings
    static boolean[][] X; // X[i][j] <-> agent_i is at meeting_j
    static int n,m,mtgPerAgent,domSize;
    static Scanner sc;

    static void skip(int k){
	for (int i=0;i<k;i++) sc.next();
    }

    static int intSkip(int k){
	skip(k);
	return sc.nextInt();
    }

    static boolean intersects(int mtg_j,int mtg_k){	
	for (int i=0;i<m;i++)
	    if (X[i][mtg_j] && X[i][mtg_k]) return true;
	return false;
    }

    static void writeMSP(String fname) throws IOException {
	PrintWriter out = new PrintWriter(new FileWriter(fname));
	out.println(n +" "+ m +" "+ domSize);
	out.println();
	for (int i=0;i<m;i++){
	    out.print(i +":");
	    for (int j=0;j<n;j++) if (X[i][j]) out.print(" 1"); else out.print(" 0");
	    out.println();
	}
	out.println();
	for (int i=0;i<n;i++){
	    out.print(i +":");
	    for (int j=0;j<n;j++) out.print(" "+ distance[i][j]);
	    out.println();
	}
	out.close();
    }

    static void readMSP(String fname) throws IOException {
	sc = new Scanner(new File(fname));
	int problemNo = 1;
	while (sc.hasNext()){
	    //System.out.println(sc.next() +" "+ sc.next());
	    skip(2);
	    n = intSkip(2);
	    m = intSkip(2);
	    X = new boolean[m][n];
	    mtgPerAgent = intSkip(2);
	    domSize = intSkip(8);
	    skip(2);
	    for (int i=0;i<m;i++){
		skip(2);
		for (int j=0;j<mtgPerAgent;j++) X[i][sc.nextInt()] = true;
	    }   
	    skip(3+n);
	    distance = new int[n][n];
	    for (int i=0;i<n;i++){
		skip(1);
		for (int j=0;j<n;j++) distance[i][j] = sc.nextInt();		
	    } 
	    skip(3);
	    writeMSP("problem" + problemNo +".txt");
	    problemNo++;
	}
	sc.close();
    }

    public static void main(String[] args)  throws IOException {

	readMSP(args[0]);
    }
}
