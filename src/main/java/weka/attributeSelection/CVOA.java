package weka.attributeSelection;

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

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;


public class CVOA implements Callable<CVOAIndividual> {

	// Lists shared by all concurrent strains
	protected static volatile List<CVOAIndividual> recovered, deaths;
	// Best solution shared by all concurrent strains
	public static volatile CVOAIndividual bestSolution;

	protected CVOAIndividual bestSolutionStrain;
	protected List<CVOAIndividual> infected;
	protected int size, max_time; // size stands for number of bits, max_time stands for iterations
	protected int time;
	protected long seed;
	protected Random rnd;
	protected String strainID;
	public static final DecimalFormat DF = new DecimalFormat("#.##");
	protected ASEvaluation evaluator;
	protected boolean isMaximization;

	// Modify these values to simulate other pandemics
	public int MIN_SPREAD = 0;
	public int MAX_SPREAD = 5;
	public int MIN_SUPERSPREAD = 6;
	public int MAX_SUPERSPREAD = 15;
	public int SOCIAL_DISTANCING = 7; // Iterations without social distancing
	public double P_ISOLATION = 0.5;
	public double P_TRAVEL = 0.1;
	public double P_REINFECTION = 0.001;
	public double SUPERSPREADER_PERC = 0.1;
	public double DEATH_PERC = 0.15;

	public CVOA(int size, int max_time, String id, int seed, ASEvaluation evaluator,
			boolean maximization, int minSpread,int maxSpread,
			int minSuperSpread, int maxSuperSpread, double pTravel, double pInfection, 
			double superSpreaderPerc,
			double deathPerc, double pIsolation, int socialDistancing) {

		initializeCommon(size, max_time, id, seed, evaluator, maximization);

		this.MIN_SPREAD = minSpread;
		this.MAX_SPREAD = maxSpread;
		this.MIN_SUPERSPREAD = minSuperSpread;
		this.MAX_SUPERSPREAD = maxSuperSpread;
		this.P_TRAVEL = pTravel;
		this.P_REINFECTION = pInfection;
		this.SUPERSPREADER_PERC = superSpreaderPerc;
		this.DEATH_PERC = deathPerc;
		this.P_ISOLATION = pIsolation;
		this.SOCIAL_DISTANCING = socialDistancing;
	}

	public CVOA(int size, int max_time, String id, int seed, ASEvaluation evaluator, 
			boolean maximization) {
		initializeCommon(size, max_time, id, seed, evaluator, maximization);
	}

	private void initializeCommon(int size, int max_time, String id, int seed, ASEvaluation evaluator, boolean maximization) {
		infected = new LinkedList<CVOAIndividual>();
		this.size = size;
		this.seed = System.currentTimeMillis() + seed;
		rnd = new Random(seed);
		this.max_time = max_time;
		this.strainID = id;
		this.evaluator = evaluator;
		this.isMaximization = maximization;
	}

	public static void initializePandemic(CVOAIndividual best) {

		bestSolution = best;

		deaths = Collections.synchronizedList(new LinkedList<CVOAIndividual>());
		// deaths = new LinkedList<CvoaIndividual>();

		recovered = Collections.synchronizedList(new LinkedList<CVOAIndividual>());
		// recovered = new LinkedList<CvoaIndividual>();

	}

	@Override
	public CVOAIndividual call() throws Exception {
		CVOAIndividual res = this.run();
		return res;
	}

	public CVOAIndividual run() throws Exception {
		CVOAIndividual pz;
		boolean epidemic = true;

		// Step 1. Infect patient zero (PZ) //
		pz = infectPZ();
		infected.add(pz);
		bestSolutionStrain = new CVOAIndividual(Arrays.copyOf(pz.getData(), size));
		Double fitnessValue = fitness(bestSolutionStrain);
		bestSolutionStrain.setFitness(fitnessValue);
		System.out.println("Patient Zero (" + strainID + "): " + pz + "\n");

		// Step 2. The main loop for the disease propagation //
		time = 0;
		while (epidemic && time < max_time) {
			propagateDisease();
			// (Un)comment this line to hide/show intermediate information
			System.out.println(strainID + " - Iteration " + time + "\nBest fitness so far: "
					+ fitness(bestSolutionStrain) + "\nInfected: " + infected.size() + "; Recovered: "
					+ recovered.size() + "; Deaths: " + deaths.size() + "\n");
			if (infected.isEmpty()) {
				epidemic = false;
			}
			time++;
		}
		System.out.println(strainID + " converged after " + time + " iterations.");

		return bestSolutionStrain;
	}

	protected void propagateDisease() throws Exception {
		int i, j, idx_super_spreader, idx_deaths, ninfected, travel_distance;
		boolean traveler;
		CVOAIndividual new_infected;
		List<CVOAIndividual> new_infected_list = new LinkedList<>();

		// Step 1. Assess fitness for each CvoaIndividual
		// Step 2. Update best global and local (strain) solutions, if proceed.
		for (CVOAIndividual x : infected) {
			Double fitnessValue = fitness(x);
			
			x.setFitness(fitnessValue);
			if (x.getFitness() < bestSolution.getFitness()) {
				bestSolution = x;
				// Uncomment if you do not want to miss bestSolution when cancelling an
				// execution
				// System.out.println("Best solution so far: " + bestSolution);
			}

			if (x.getFitness() < bestSolutionStrain.getFitness()) {
				bestSolutionStrain = x;
			}

		}
		// Step 3. Sort the infected list by fitness (ascendent).
		Collections.sort(infected);

		// Step 4. Assess indexes to point super-spreaders and deaths parts of the
		// infected list.
		idx_super_spreader = infected.size() == 1 ? 1 : (int) Math.ceil(SUPERSPREADER_PERC * infected.size());
		idx_deaths = infected.size() == 1 ? Integer.MAX_VALUE
				: infected.size() - (int) Math.ceil(DEATH_PERC * infected.size());

		// Step 5. Disease propagation.
		i = 0;
		for (CVOAIndividual x : infected) {
			// Step 5.1 If the CvoaIndividual belongs to the death part, then die
			if (i >= idx_deaths) {
				deaths.add(x);
			} else {
				// Step 5.2 Determine the number of new infected CvoaIndividuals.
				if (i < idx_super_spreader) { // This is the super-spreader!
					ninfected = MIN_SUPERSPREAD + rnd.nextInt(MAX_SUPERSPREAD - MIN_SUPERSPREAD + 1);
				} else {
					ninfected = rnd.nextInt(MAX_SPREAD + 1);
				}

				// Step 5.3 Determine whether the CvoaIndividual has traveled
				traveler = rnd.nextDouble() < P_TRAVEL;

				// Step 5.4 Determine the travel distance, which is how far is the new infected
				// CvoaIndividual.
				if (traveler) {
					travel_distance = rnd.nextInt(size + 1);
				} else {
					travel_distance = 1;
				}

				// Step 5.5 Infect
				for (j = 0; j < ninfected; j++) {
					new_infected = infect(x, travel_distance);

					// Propagate with no social distancing measures
					if (time < SOCIAL_DISTANCING) {
						if (!deaths.contains(new_infected) && !recovered.contains(new_infected)
								&& !new_infected_list.contains(new_infected) && !infected.contains(new_infected)) {
							new_infected_list.add(new_infected);
						} else if (recovered.contains(new_infected) && !new_infected_list.contains(new_infected)) {
							if (rnd.nextDouble() < P_REINFECTION) {
								new_infected_list.add(new_infected);
								recovered.remove(new_infected);
							}
						}

					}
					// After SOCIAL_DISTANCING iterations, there is a P_ISOLATION of not being
					// infected
					else {
						if (rnd.nextDouble() > P_ISOLATION) {
							if (!deaths.contains(new_infected) && !recovered.contains(new_infected)
									&& !new_infected_list.contains(new_infected) && !infected.contains(new_infected)) {
								new_infected_list.add(new_infected);
							} else if (recovered.contains(new_infected) && !new_infected_list.contains(new_infected)) {
								if (rnd.nextDouble() < P_REINFECTION) {
									new_infected_list.add(new_infected);
									recovered.remove(new_infected);
								}
							}
						} else { // Those saved by social distancing are sent to the recovered list
							if (!deaths.contains(new_infected) && !recovered.contains(new_infected)
									&& !new_infected_list.contains(new_infected) && !infected.contains(new_infected)) {
								recovered.add(new_infected);
							}
						}
					}
				}
				if (!deaths.contains(x) && !recovered.contains(x))
					recovered.add(x);
			}
			i++;
		}

		// Step 6. Update the infected list with the new infected CvoaIndividuals
		// (shared by
		// all threads).
		infected = new_infected_list;

	}

	// Infect a new CvoaIndividual by mutating as many bits as indicated by
	// travel_distance
	protected CVOAIndividual infect(CVOAIndividual individual, int travel_distance) {
		List<Integer> mutated = new LinkedList<Integer>();
		int[] res = Arrays.copyOf(individual.getData(), size);
		int i = 0, pos;

		while (i < travel_distance) {
			pos = rnd.nextInt(size);
			if (!mutated.contains(pos)) {
				res[pos] = res[pos] == 0 ? 1 : 0;
				mutated.add(pos);
				i++;
			}
		}

		return new CVOAIndividual(res);
	}

	// Optimal reached at x = 15 (In binary: 11110000...)
	public double fitness(CVOAIndividual individual) throws Exception {

		BitSet selectedAttrs = individual.getGroup();
		// make a copy if the evaluator is not thread safe
		final SubsetEvaluator theEvaluator = (this.evaluator instanceof weka.core.ThreadSafe)
				? (SubsetEvaluator) this.evaluator
				: (SubsetEvaluator) ASEvaluation.makeCopies((ASEvaluation) this.evaluator, 1)[0];

		double fitness = theEvaluator.evaluateSubset(selectedAttrs);
		if(isMaximization) {
			fitness =-fitness;
		}
		return fitness;
	}

	public static int binaryToDecimal(CVOAIndividual binary) {

		int i, res = 0;
		int[] data = binary.getData();

		if (data != null) {
			for (i = 0; i < data.length; i++) {
				res += data[i] * Math.pow(2, i);
			}
		}
		return res;
	}

	// This method could be improved if a wiser selection of PZ is done
	// It could be selected orthogonal PZs or PZs with high Hamming distance
	protected CVOAIndividual infectPZ() throws Exception {
		CVOAIndividual PZ = new CVOAIndividual();
		int[] res = new int[size];
		int i;
		int aux;

		for (i = 0; i < size; i++) {
			aux = rnd.nextInt(2);
			res[i] = aux;
		}
		PZ.setData(res);
		PZ.setFitness(fitness(PZ));
		return PZ;
	}
}
