import java.util.*;

public class KMeans extends ClusteringAlgorithm
{
	// Number of clusters
	private int k;

	public int radius;
	
	// Dimensionality of the vectors
	private int dim;
	
	// Threshold above which the corresponding html is prefetched
	private double prefetchThreshold;
	
	// Array of k clusters, class cluster is used for easy bookkeeping
	private Cluster[] clusters;
	
	// This class represents the clusters, it contains the prototype (the mean of all it's members)
	// and memberlists with the ID's (which are Integer objects) of the datapoints that are member of that cluster.
	// You also want to remember the previous members so you can check if the clusters are stable.
	static class Cluster
	{
		float[] prototype;

		Set<Integer> currentMembers;
		Set<Integer> previousMembers;
		  
		public Cluster()
		{
			currentMembers = new HashSet<Integer>();
			previousMembers = new HashSet<Integer>();
		}
	}
	// These vectors contains the feature vectors you need; the feature vectors are float arrays.
	// Remember that you have to cast them first, since vectors return objects.
	private Vector<float[]> trainData;
	private Vector<float[]> testData;

	// Results of test()
	private double hitrate;
	private double accuracy;
	
	public KMeans(int k, Vector<float[]> trainData, Vector<float[]> testData, int dim)
	{
		this.k = k;
		this.trainData = trainData;
		this.testData = testData; 
		this.dim = dim;
		prefetchThreshold = 0.5;
		radius=k/2;
		
		// Here k new cluster are initialized
		clusters = new Cluster[k];
		for (int ic = 0; ic < k; ic++)
			clusters[ic] = new Cluster();
	}
	public void calculatePrototypes(){
		float total=0;
		for(int i = 0; i<k; i++){ ///loop through clusters
			clusters[i].prototype=new float[this.dim];
			for(int p=0; p<200; p++){ ///loop through prototype
				total = 0;
				for(int n: clusters[i].currentMembers){///loop through members of cluster
					total+=trainData.get(n)[p]; ///add up feature data of all members
				}
				clusters[i].prototype[p]=total/(float)clusters[i].currentMembers.size(); ///average feature data
			}
		}
	}
	
	public void distributeMembers() {
		
		///copy current members to previous members
		for(int i=0; i<k; i++){ 
			clusters[i].previousMembers.clear(); ///clear previousMembers
			Iterator iter = clusters[i].currentMembers.iterator();
			while(iter.hasNext()){ ///add currentMembers to previousMembers
				clusters[i].previousMembers.add((Integer)iter.next());
			}
			clusters[i].currentMembers.clear(); ///clear currentMembers
		}
		int min; ///cluster with minimum ED
		double minED; ///minimum ED
		double sum;
		///calculate ED

		for(int j=0; j<70; j++){ ///loop through members
		
			min=0;
			minED=999999;
			for(int i=0; i<k; i++){ ///check ED for every cluster
				sum=0;
				for(int k=0; k<200; k++){///calculate ED
					sum+= Math.pow((trainData.get(j)[k]-clusters[i].prototype[k]),2);
				}
				if(Math.sqrt(sum)<=minED){ ///find the minimum ED
					minED=Math.sqrt(sum);
					min=i;
				}
			}
		clusters[min].currentMembers.add(j); ///add this member to best cluster
		}
	}



	public boolean train()
	{
		for(int i=0; i<70; i++){ ///partition the data randomly
			double random = Math.random()*k;
			clusters[(int)random].currentMembers.add(i);
		}
		calculatePrototypes();
		showMembers();
	
		
		while(!stabilised()){
			distributeMembers();
			calculatePrototypes();
			showMembers();
		}
		return false;
	}
	

	private boolean stabilised() {
		for(int i=0; i<k; i++){ ///check for every cluster if current equals previous
			if(!clusters[i].currentMembers.equals(clusters[i].previousMembers)){
				return false;
			}
		}
		return true;
	}
	public boolean test(){
	double truePositive, trueNegative, falsePositive, falseNegative;
	truePositive=trueNegative=falsePositive=falseNegative=0;
	
		for(int i=0; i<k; i++){ ///loop per cluster for easy cluster recall
			for(int j: clusters[i].currentMembers){ ///loop through members of each cluster
				for(int n=0; n<200; n++){ ///loop through data
					if(clusters[i].prototype[n]>prefetchThreshold){///page is prefetched
						if(testData.get(j)[n]==1){ 	///correctly
							truePositive++;
						}else{						///incorrectly
							falsePositive++;
						}
					}else{///page is not prefetched
						if(testData.get(j)[n]==1){	///incorrectly
							falseNegative++;
						}else{						///correctly
							trueNegative++;
						}
					}
				}
			}
			
		}
		System.out.println(truePositive);
		System.out.println(trueNegative);
		System.out.println(falsePositive);
		System.out.println(falseNegative);
		
		this.hitrate=(truePositive/(truePositive+falseNegative));
		this.accuracy=((truePositive))/(truePositive+falsePositive);
		
		// iterate along all clients. Assumption: the same clients are in the same order as in the testData
		// for each client find the cluster of which it is a member
		// get the actual testData (the vector) of this client
		// iterate along all dimensions
		// and count prefetched htmls
		// count number of hits
		// count number of requests
		// set the global variables hitrate and accuracy to their appropriate value
		return true;
	}


	// The following members are called by RunClustering, in order to present information to the user
	public void showTest()
	{
		System.out.println("Prefetch threshold=" + this.prefetchThreshold);
		System.out.println("Hitrate: " + this.hitrate);
		System.out.println("Accuracy: " + this.accuracy);
		System.out.println("Hitrate+Accuracy=" + (this.hitrate + this.accuracy));
	}
	
	public void showMembers()
	{
		for (int i = 0; i < k; i++)
			System.out.println("\nMembers cluster["+i+"] :" + clusters[i].currentMembers);
	}
	public void showPreviousMembers()
	{
		for (int i = 0; i < k; i++)
			System.out.println("\nPrevious Members cluster["+i+"] :" + clusters[i].previousMembers);
	}
	
	public void showPrototypes()
	{
		for (int ic = 0; ic < k; ic++) {
			System.out.print("\nPrototype cluster["+ic+"] :");
			
			for (int ip = 0; ip < dim; ip++)
				System.out.print(clusters[ic].prototype[ip] + " ");
			
			System.out.println();
		 }
	}

	// With this function you can set the prefetch threshold.
	public void setPrefetchThreshold(double prefetchThreshold)
	{
		this.prefetchThreshold = prefetchThreshold;
	}
}
