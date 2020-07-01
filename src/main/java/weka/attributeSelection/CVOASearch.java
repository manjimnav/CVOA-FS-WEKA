package weka.attributeSelection;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;		

public class CVOASearch extends ASSearch implements OptionHandler {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2076216099677055557L;
	public int MAX_THREADS = 16;
	public int SEED = 20;
	public int ITERATIONS = 12;
	public boolean MAXIMIZATION = false;
	public CVOAIndividual firstCase;
	public CVOAIndividual bestSolution;
	public String duration;

	@Override
	public int[] search(ASEvaluation ASEvaluator, Instances data) throws Exception {
		
		firstCase = new CVOAIndividual();
		if(MAXIMIZATION) {
			firstCase.setFitness(-Double.MAX_VALUE);
		}
		CVOA.initializePandemic(firstCase);

		int numAttributes = data.numAttributes() - 1; // All attributes used for selection except the class

//		ExecutorService pool = Executors.newFixedThreadPool(MAX_THREADS);
        long time = System.currentTimeMillis();
//		Collection<CVOA> concurrentCVOAs = new LinkedList<>();
//		concurrentCVOAs.add(new CVOA(numAttributes, ITERATIONS, "Strain #1", SEED, ASEvaluator));
		CVOA searcher = new CVOA(numAttributes, ITERATIONS, "Strain #1", SEED, ASEvaluator, MAXIMIZATION, MAX_THREADS);

//		List<Future<CVOAIndividual>> results = new LinkedList<Future<CVOAIndividual>>();
		bestSolution = searcher.run();


//		results = pool.invokeAll(concurrentCVOAs);
//		int i = 1;

//		System.out.println("\n************** BEST RESULTS BY STRAIN **************");
//		for (Future<CVOAIndividual> r : results) {
//			System.out.println("Best solution for strain #" + i + ":" + r.get());
//			System.out.println("Best fitness for strain #" + i + ": " + r.get().getFitness() + "\n");
//			i++;
//		}

//		pool.shutdown();

        time = System.currentTimeMillis() - time;
        duration = CVOA.DF.format(((double) time) / 60000);

        System.out.println("\n************** BEST RESULT **************");
        System.out.println("Best solution: " + bestSolution);
        System.out.println("Best fitness: " + bestSolution.getFitness());
        
        System.out.println("\nExecution time: " + duration + " mins");
        
		return bestSolution.getDataIndexes();
	}

	@Override
	public Enumeration<Option> listOptions() {
		Vector<Option> newVector = new Vector<Option>(8);

		newVector.addElement(new Option("\tMax threads.", "T", 1, "-T <num threads>"));

		newVector.addElement(new Option("\tMax iterations.", "I", 1, "-I <num iterations>"));

		newVector.addElement(new Option("\tSpecify a seed.", "S", 1, "-S <seed>"));

		newVector.addElement(new Option("\tPrint debugging output", "D", 0, "-D"));
		
		newVector.addElement(new Option("\tMaximization problem", "M", 0, "-M"));

		return newVector.elements();
	}

	@Override
	public void setOptions(String[] options) throws Exception {
		String optionString;
		String[] intOptTemp = new String[] { "T", "I", "S" };
		List<String> intOptions = Arrays.asList(intOptTemp);
		resetOptions();

		for (String option : intOptions) {

				optionString = Utils.getOption(option, options);

				if (optionString.length() == 0) {
					continue;
				}

				int optionValue = Integer.parseInt(optionString);

				if (option == "T") {
					setNumThreads(optionValue);
				} else if (option == "I") {
					setNumIterations(optionValue);
				} else if (option == "S") {
					setSeed(optionValue);
				}

		}
		setMaximizationProblem(Utils.getFlag('M', options));

	}
	
	/**
	   * returns a description of the search.
	   * 
	   * @return a description of the search as a String.
	   */
	  @Override
	  public String toString() {
		  StringBuffer FString = new StringBuffer();
		    FString.append("\tCVOA ("+ ".\n");
//		    FString.append(firstCase.toString()+"\n");
		    FString.append("\tBest set: ");
		    FString.append(bestSolution.toString()+"\n");
		    FString.append("\tMerit of best subset found: "
		            + Utils.doubleToString(Math.abs(bestSolution.getFitness()), 8, 3) + "\n");
		    FString.append("\tDuration (min): "
		            + duration + "\n");
		    
		  
		return FString.toString();
		  
	  }
	  

	@Override
	public String[] getOptions() {
		
		Vector<String> options = new Vector<String>();
		options.add("-T");
	    options.add("" + this.MAX_THREADS);

	    options.add("-S");
	    options.add("" + this.SEED);
	    
	    options.add("-I");
	    options.add("" + this.ITERATIONS);
	    
	    if(this.MAXIMIZATION) {
		    options.add("-M");
	    }

	    
		return options.toArray(new String[0]);
	}

	private void setNumThreads(int threads) {
		this.MAX_THREADS = threads;
	}

	private void setNumIterations(int iterations) {
		this.ITERATIONS = iterations;
	}

	private void setSeed(int seed) {
		this.SEED = seed;
	}
	
	private void setMaximizationProblem(boolean maximization) {
		this.MAXIMIZATION = maximization;
	}

	protected void resetOptions() {
		MAX_THREADS = 16;
		SEED = 20;
		ITERATIONS = 12;
	}

}
