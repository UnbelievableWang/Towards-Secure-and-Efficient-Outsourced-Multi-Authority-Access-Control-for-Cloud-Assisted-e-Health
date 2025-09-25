package my_test;

import java.util.HashMap;

public class EcPolicy {
	EcMatrix A; //l x n
	HashMap<Integer,Integer> rho;// [l] -> U
	int l;
	int n;

	EcPolicy(EcMatrix A, HashMap<Integer,Integer> rho ){
		this.A=A;
		this.l=A.getRows();
		this.n=A.getCols();
		this.rho=rho;
	}
	int rho(Integer j) {

		return rho.get(j).intValue();
	}
}
