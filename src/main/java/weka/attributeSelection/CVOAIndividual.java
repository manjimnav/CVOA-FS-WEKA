package weka.attributeSelection;

import java.util.BitSet;
import java.util.stream.IntStream;

/**
*
* @author Data Science & Big Data Lab, Pablo de Olavide University
*
* Parallel Coronavirus Optimization Algorithm
* Version 2.5 
* Academic version for a binary codification
*
* March 2020
*
*/

public class CVOAIndividual implements Comparable<CVOAIndividual> {

   protected int[] data;
   // BitSet representation of data for Weka evaluators
   protected BitSet group;
   protected double fitness;

   public CVOAIndividual(int[] data) {
       super();
       this.data = data;
       this.group = createGroup(data);
   }

   public CVOAIndividual() {
       super();
       this.fitness = Double.MAX_VALUE;
   }
   
   private BitSet createGroup(int[] data) {
	   BitSet group = new BitSet(data.length);
	   int i = 0;
	   for(int element: data) {
		   if(element>0) {
			   group.set(i);
		   }
		   i++;
	   }
	   return group;
   }

   public int[] getData() {
       return data;
   }
   
   public int[] getDataIndexes() {
	   int selectedLen = IntStream.of(data).sum();
	   int[] indexes = new int[selectedLen];
	   int count = 0;
	   for(int i=0; i<data.length; i++) {
		   if(data[i]>0) {
			   indexes[count++] = i;
		   }
	   }
	   return indexes;
   }
   
   public BitSet getGroup() {
	   return this.group;
   }

   public void setData(int[] data) {
       this.data = data;
       this.group = createGroup(data);
   }

   @Override
   public int compareTo(CVOAIndividual o) {
       if (fitness > o.getFitness()) {
           return 1;
       } else if (fitness == o.getFitness()) {
           return 0;
       } else {
           return -1;
       }
   }

   @Override
   public boolean equals(Object obj) {
	   CVOAIndividual indiv = (CVOAIndividual) obj;
       int i = 0, diff = 0;

       while (diff == 0 && i < data.length) {
           diff = data[i] - indiv.data[i];
           i++;
       }

       return diff == 0;
   }

   @Override
   public String toString() {
       String res = "";
       int i;
       if (data != null) {
           res = "[" + data[0];
           for (i = 1; i < data.length; i++) {
               res += "," + data[i];
           }

           res += "]";
       }
       //res+=" -- F = " + this.fitness;
       return res;
   }

   public double getFitness() {
       return fitness;
   }

   public void setFitness(double fitness) {
       this.fitness = fitness;
   }

}
