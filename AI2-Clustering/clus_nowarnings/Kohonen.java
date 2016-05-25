import java.util.*;

public class Kohonen extends ClusteringAlgorithm
{
	public double radius;
	// Size of clustersmap
	private int n;

	// Number of epochs
	private int epochs;
	
	// Dimensionality of the vectors
	private int dim;
	
	// Threshold above which the corresponding html is prefetched
	private double prefetchThreshold;

	private double initialLearningRate; 
	
	// This class represents the clusters, it contains the prototype (the mean of all it's members)
	// and a memberlist with the ID's (Integer objects) of the datapoints that are member of that cluster.  
	private Cluster[][] clusters;

	// Vector which contains the train/test data
	private Vector<float[]> trainData;
	private Vector<float[]> testData;
	
	// Results of test()
	private double hitrate;
	private double accuracy;
	
	static class Cluster
	{
			float[] prototype;

			Set<Integer> currentMembers;

			public Cluster()
			{
				currentMembers = new HashSet<Integer>();
			}
	}
	
	public Kohonen(int n, int epochs, Vector<float[]> trainData, Vector<float[]> testData, int dim)
	{
		this.n = n;
		this.epochs = epochs;
		prefetchThreshold = 0.5;
		initialLearningRate = 0.8;
		this.trainData = trainData;
		this.testData = testData; 
		this.dim = dim;       
		
		Random rnd = new Random();

		// Here n*n new cluster are initialized
		clusters = new Cluster[n][n];
		for (int i = 0; i < n; i++)  {
			for (int i2 = 0; i2 < n; i2++) {
				clusters[i][i2] = new Cluster();
				clusters[i][i2].prototype = new float[dim];
			}
		}

	}
	public void calculatePrototypes(){
		float total=0;
		for(int i = 0; i<n; i++){ ///loop through clusters
			for(int j =0; j<n; j++){
				clusters[i][j].prototype=new float[this.dim];
				for(int p=0; p<200; p++){ ///loop through prototype
					total = 0;
					for(int n: clusters[i][j].currentMembers){///loop through members of cluster
						total+=trainData.get(n)[p]; ///add up feature data of all members
					}
					clusters[i][j].prototype[p]=total/(float)clusters[i][j].currentMembers.size(); ///average feature data
				}
			}
		}
		for(int i=0; i<n; i++){ ///if a cluster is empty, initialise the prototype with 0.5 everywhere
			for(int j=0; j<n; j++){
				if(clusters[i][j].currentMembers.isEmpty()){
					for(int k=0; k<200; k++){
						clusters[i][j].prototype[k]=(float)0.5;
					}
				}
			}
		}
	}
	
	
	public void examineMembers(double η, double radius){
		for(int a=0; a<70; a++){ ///loop through members
			
			int minI=0;
			int minJ=0;
			double minED=999999;
			int sum=0;
			
			for(int i=0; i<n; i++){ ///check ED for every cluster
				for(int j=0; j<n; j++){
					sum=0;
					for(int k=0; k<200; k++){///calculate ED
						sum+= Math.pow((trainData.get(a)[k]-clusters[i][j].prototype[k]),2);
					}
					if(Math.sqrt(sum)<=minED){ ///find the minimum ED
						minED=Math.sqrt(sum);
						minI=i;
						minJ=j;
					}
				}
			}
			///adjust the nodes to be more like the vector
			for(int x = 0; x<n; x++){
				for(int y = 0; y<n; y++){
					if(Math.sqrt(Math.pow((x-minI),2)+Math.pow((y-minJ),2))<radius){
						for(int b=0; b<200; b++){
							clusters[x][y].prototype[b]= (float)((1-η)*clusters[x][y].prototype[b]+η*trainData.get(a)[b]);
							
						}
						
					}
					
				}
			}
			
		}
	}

	
	public boolean train()
	{
		for(int i=0; i<70; i++){ ///partition the data randomly
			double random = Math.random()*n;
			double random2 = Math.random()*n;
			clusters[(int)random][(int)random2].currentMembers.add(i);
		}
		
		calculatePrototypes();
		showPrototypes();
		for(int i=0; i<epochs; i++){
			double radius = (n/2)*(1-(i/epochs));
			double η = 0.8*(1-(i/epochs));
			examineMembers(η, radius);
		
			
		}
		showPrototypes();
		
		//examineMembers();
		
		// Step 1: initialize map with random vectors (A good place to do this, is in the initialisation of the clusters)
		// Repeat 'epochs' times:
			// Step 2: Calculate the squareSize and the learningRate, these decrease lineary with the number of epochs.
			// Step 3: Every input vector is presented to the map (always in the same order)
			// For each vector its Best Matching Unit is found, and :
				// Step 4: All nodes within the neighbourhood of the BMU are changed, you don't have to use distance relative learning.
		// Since training kohonen maps can take quite a while, presenting the user with a progress bar would be nice
		return true;
	}
	
	public boolean test()
	{
		double truePositive = 0, trueNegative = 0, falsePositive = 0, falseNegative = 0;
		for(float[] currentVector: testData){

			int minI=0;
			int minJ=0;
			double minED=999999;
			int sum=0;


			for(int i=0; i<n; i++){ ///check ED for every cluster
				for(int j=0; j<n; j++){
					sum=0;
					for(int k=0; k<200; k++){///calculate ED
						sum+= Math.pow((currentVector[k]-clusters[i][j].prototype[k]),2);
					}
					if(Math.sqrt(sum)<=minED){ ///find the minimum ED
						minED=Math.sqrt(sum);
						minI=i;
						minJ=j;
					}
				}
			}

			for(int i=0; i<200; i++){ ///loop through data
				if(clusters[minI][minJ].prototype[i]>prefetchThreshold){///page is prefetched
					if(currentVector[i]==1){ 	///correctly
						truePositive++;
					}else{						///incorrectly
						falsePositive++;
					}
				}else{///page is not prefetched
					if(currentVector[i]==1){	///incorrectly
						falseNegative++;
					}else{						///correctly
						trueNegative++;
					}
				}
			}

		}
		// iterate along all clients
		// for each client find the cluster of which it is a member
		// get the actual testData (the vector) of this client
		// iterate along all dimensions
		// and count prefetched htmls
		// count number of hits
		// count number of requests
		// set the global variables hitrate and accuracy to their appropriate value

		System.out.println(truePositive);
		System.out.println(trueNegative);
		System.out.println(falsePositive);
		System.out.println(falseNegative);

		this.hitrate=(truePositive/(truePositive+falseNegative));
		this.accuracy=((truePositive))/(truePositive+falsePositive);

		return true;
	}


	public void showTest()
	{
		System.out.println("Initial learning Rate=" + initialLearningRate);
		System.out.println("Prefetch threshold=" + prefetchThreshold);
		System.out.println("Hitrate: " + hitrate);
		System.out.println("Accuracy: " + accuracy);
		System.out.println("Hitrate+Accuracy=" + (hitrate + accuracy));
	}
 
 
	public void showMembers()
	{
		for (int i = 0; i < n; i++)
			for (int i2 = 0; i2 < n; i2++)
				System.out.println("\nMembers cluster["+i+"]["+i2+"] :" + clusters[i][i2].currentMembers);
	}

	public void showPrototypes()
	{
		for (int i = 0; i < n; i++) {
			for (int i2 = 0; i2 < n; i2++) {
				System.out.print("\nPrototype cluster["+i+"]["+i2+"] :");
				
				for (int i3 = 0; i3 < dim; i3++)
					System.out.print(" " + clusters[i][i2].prototype[i3]);
				
				System.out.println();
			}
		}
	}

	public void setPrefetchThreshold(double prefetchThreshold)
	{
		this.prefetchThreshold = prefetchThreshold;
	}
}

