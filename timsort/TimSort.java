// ver 0.88 with debug
// added stuff to detect and reverse descending runs
// have correct binary insertion working
// probably other stuff
// most bugs fixed (famous last words)
// and GALLOP MODE


package timsort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class TimSort {

	public static void main(String[] args) {

		// generate list to test TimSort.sort
		List<Integer> intList = new ArrayList<>();		

		// fill with random Integer objects
		long seed = java.lang.System.currentTimeMillis();
		Random random = new Random();
		random.setSeed(seed);		// 1638576577933l constant seed for debug testing
		// 1638804338884l 55 + 500, 1000, dupe element testing

		// generate an array between 1000-1500 long, filled with ints from 0-1000
		for (int i = 0; i < random.nextInt(550) + 10000; i++)
			intList.add(random.nextInt(1000));

		// debug for ascending
		//		for (int i = 1; i <= 20; i++)
		//			intList.add(i);

		//		// debug test for mixed ascending/descending runs
		//		for (int i = 20; i >= 1; i--)
		//			intList.add(i);
		//		for (int i = 0; i < 44; i++)
		//			intList.add(i);

		// debug list for gallop mode
		//		for (int i = 1; i <= 50; i++)
		//			intList.add(i);
		//		for (int i = 100; i > 50; i--)
		//			intList.add(i);
		//		for (int i = 101; i <= 150; i++)
		//			intList.add(i);
		//		for (int i = 200; i > 150; i--)
		//			intList.add(i);

		// debug for long lists
		//				for (int i = 1; i <= random.nextInt(500) + 100000; i++)
		//					intList.add(i);

		// print out seed and list for debug testing
		System.out.println("seed: " + seed);

		// print unsorted list
		//		int i = 0;
		//		for (Integer element : intList) {
		//			System.out.print(element + ", ");
		//			i++;
		//			if (i % 20 == 0)
		//				System.out.println();
		//		}
		//		System.out.println("END original list");
		//		System.out.println();
		// end more debug stuff

		List<Integer> shuffledList = new ArrayList<>(intList);

		Collections.shuffle(shuffledList);
		// print shuffled list
		//		i = 0;
		//		for (Integer element : shuffledList) {
		//			System.out.print(element + ", ");
		//			i++;
		//			if (i % 20 == 0)
		//				System.out.println();
		//		}
		//		System.out.println("END shuffled list");
		//		System.out.println();

		List<Integer> bubbleList = new ArrayList<>(shuffledList);
		// old bubble sort
		long bubSortStart = System.currentTimeMillis();
		bubbleSort(bubbleList);
		long bubSortEnd = System.currentTimeMillis();

		
		List<Integer> mergeList = new ArrayList<>(shuffledList);
		// standard merge sort
		long mergeSortStart = System.currentTimeMillis();
		mergeSort(mergeList);
		long mergeSortEnd = System.currentTimeMillis();
		
		List<Integer> timList = new ArrayList<>(shuffledList);
		// commence TimSort
		long timSortStart = System.currentTimeMillis();
		List<Integer> result = sort(timList);
		long timSortEnd = System.currentTimeMillis();

		// debug thing
		//		int k = 1;
		//		System.out.println("\nResult Length: " + result.size());
		//		for (Integer element : result) {
		//			System.out.print(element + ", ");
		//			if (k % 20 == 0)
		//				System.out.println();
		//			k++;
		//		}
		// end debug thing

		List<Integer> builtInList = new ArrayList<>(shuffledList);
		long collectionSortStart = System.currentTimeMillis();
		Collections.sort(builtInList);
		long collectionSortEnd = System.currentTimeMillis();
		//		System.out.println("\n");
		//		k = 1;
		//		for (Integer inte : intList) {
		//			System.out.print(inte + ", ");
		//			if (k % 20 == 0)
		//				System.out.println();
		//			k++;
		//		}


		// check that sorted shuffled list is the same as orginal list
		boolean error = false;
		for (int j = 0; j < shuffledList.size(); j++) {
			if (shuffledList.get(j).compareTo(result.get(j)) != 0) {
				//				System.out.print("Error at element: " + j);
				//				System.out.println(", " + intList.get(j) + " != " + result.get(j));
				error = true;
			}
		}
		if (error) {
			System.out.println("\nSomething went wrong...");
		} else {
			System.out.println("\nTimSort worked!!");
		}

		System.out.println("Bubble Sort took " + (bubSortEnd - bubSortStart) + " ms to sort the array.");
		System.out.println("Merge Sort took " + (mergeSortEnd - mergeSortStart) + " ms to sort the array.");
		System.out.println("My TimSort took " + (timSortEnd - timSortStart) + " ms to sort the array.");
		System.out.println("The built in sort took " + (collectionSortEnd - collectionSortStart) + " ms to sort the array");

		System.out.println("Original intList.size(): " + intList.size());
		System.out.println("result.size(): " + result.size());

	}

	public static <T extends Comparable<T>> List<T> sort(List<T> array) {

		// generate min_run_size, based on original C implementation for Python
		int min_run_size = 0;
		int arraySize = array.size();
		if (arraySize < 64) {
			min_run_size = arraySize;
		} else {
			if ((arraySize & (arraySize - 1)) == 0 && arraySize != 0) {
				// if array.size() is a power of 2
				min_run_size = 32;
			} else {
				// calculate min_run_size
				int rValue = 0;
				while (arraySize >= 64) {
					rValue |= arraySize & 1;
					arraySize >>= 1;		// halve array size
				}
				// the goal is to get n / result as close to a power of 2
				// with n = 100, min_run_size = 50
				min_run_size = rValue + arraySize;
			}
		}

		// print stuff for debug
		//		System.out.println("array.size() = " + array.size());
		//		System.out.println("min_run_size = " + min_run_size);

		Stack<List<T>> runStack = new Stack<>();

		// find natural runs
		List<T> currentRun = new ArrayList<>();
		currentRun.add(array.get(0));

		for(int currentIndex = 1; currentIndex < array.size(); currentIndex++) {
			boolean ascending = true;

			// see if this or previous element is smaller
			int compareResult = array.get(currentIndex).compareTo(array.get(currentIndex-1));

			// count length of run
			int runLength = currentIndex;
			if (compareResult >= 0) {
				ascending = true;
				while (runLength < array.size() && array.get(runLength).compareTo(array.get(runLength - 1)) >= 0) {
					runLength++;
				}
			} else {
				ascending = false;
				while (runLength < array.size()  && array.get(runLength).compareTo(array.get(runLength - 1)) < 0) {
					runLength++;
				}
			}

			// add natural run that we found to currentRun
			while (currentIndex < runLength) {
				currentRun.add(array.get(currentIndex));
				currentIndex++;
			}

			// reverse run if that was a descending run
			if (ascending == false) {
				for (int j = 0, k = currentRun.size() - 1; j <= k; j++, k-- ) {
					T temp = currentRun.get(j);
					currentRun.set(j, currentRun.get(k));
					currentRun.set(k, temp);
				}
			}

			// bring currentRun size up to min_run_size
			if (currentRun.size() < min_run_size) {				
				// if currentRun is smaller than min_run_size, calculate endIndex to fill run to min_run_size
				int endIndex = min_run_size - currentRun.size() + currentIndex;
				while (currentIndex < array.size() && currentIndex < endIndex) {
					// insert more elements sorted into currentRun
					// until we've hit end of array or we reach min_run_size
					// currentRun = insertion(currentRun, array.get(i));		// boring, old, slow insertion sort, depreciated
					currentRun = binaryInsert(currentRun, array.get(currentIndex));
					currentIndex++;
				}
				currentIndex--;		// decrement currentIndex so outer for loop does not skip element during next iteration
				// add currentRun to runStack
				runStack.add(currentRun);
				// reset currentRun to an empty list, ending currentRun
				currentRun = new ArrayList<>();
				//				// add currentElement to currentRun, starting a new run
				//				currentRun.add(array.get(i));		// safer to decrement currentIndex as above, which might also
				// be quicker than testing for end of array and adding 
				// array[i] to new currentRun upon passing that conditional
			} else {
				// if currentRun.size() is larger than min_run_size, just add it to runStack
				runStack.add(currentRun);
				// reset currentRun to an empty list, ending currentRun 
				currentRun = new ArrayList<>();
				currentIndex--;		// decrement currentIndex so outer for loop does not skip element during next iteration
			}


			// after currentRun is added to runStack, check for merge conditions
			if (runStack.size() >= 3) {
				int zSize = 0;
				int ySize = 0;
				int xSize = 0;
				xSize = runStack.get(runStack.size() - 1).size();
				ySize = runStack.get(runStack.size() - 2).size();
				zSize = runStack.get(runStack.size() - 3).size();

				while (zSize < ySize + xSize || ySize < xSize) {
					List<T> mergedList = new ArrayList<>();
					List<T> x = runStack.pop();
					List<T> y = runStack.pop();
					List<T> z = runStack.pop();
					if (zSize < xSize) {
						// merge y with z
						mergedList.addAll(merge(y, z));
						// push x
						runStack.push(x);
						// push merged
						runStack.push(mergedList);
					} else {
						// merge y with x
						mergedList.addAll(merge(y, x));
						// push z
						runStack.push(z);
						// push merged
						runStack.push(mergedList);
					}
					// recalculate sizes of top 3 runs to see if they still meet merge criteria
					if (runStack.size() >= 3) {
						xSize = runStack.get(runStack.size() - 1).size();
						ySize = runStack.get(runStack.size() - 2).size();
						zSize = runStack.get(runStack.size() - 3).size();
					} else {
						break;
					}
				}
			}
		}

		// print lists in stack for debug testing
		//		int j = 1;
		//		for (List<T> run : runStack) {
		//			int i = 0;
		//			System.out.println("\nRun Length: " + run.size());
		//			for (T element : run) {
		//				System.out.print(element + ", ");
		//				i++;
		//				if (i % 20 == 0)
		//					System.out.println();
		//			}
		//			System.out.println("\nEND Run Num: " + j);
		//			System.out.println();
		//			j++;
		//		}
		// end debug testing
		// END RUN FINDING


		List<T> sortedResults = runStack.pop();

		for (int i = runStack.size() - 1; i >= 0; i--) {
			runStack.add(merge(sortedResults, runStack.get(i)));
			if (runStack.size() > 0) {
				sortedResults = runStack.pop();
			} 
		}
		return sortedResults;
	}



	private static <T extends Comparable<T>> List<T> merge(List<T> firstArray, List<T> secondArray) {
		int minGallop = 7;

		List<T> result = new ArrayList<>();
		int index1 = 0;
		int index2 = 0;

		int lowIndexForFirstElement = firstElementIndex(secondArray, firstArray.get(0));
		int hiIndexForLastElement = lastElementIndex(firstArray, secondArray.get(secondArray.size()-1));

		// add presorted elements of secondArray to results
		for (int i = 0; i < lowIndexForFirstElement; i++) {
			result.add(secondArray.get(i));
		}
		// set index1 so we skip presorted elements already added
		if (lowIndexForFirstElement != -1) {
			index2 = lowIndexForFirstElement;
		}

		int aCount = 0;
		int bCount = 0;

		// end loop at hiIndexForLastElement so we can add presorted tail of firstArray
		while ((index1 < firstArray.size() && index1 < hiIndexForLastElement) || index2 < secondArray.size()) {
			if (index1 == firstArray.size() || index1 == hiIndexForLastElement) {
				result.add(secondArray.get(index2++));
			} else if (index2 == secondArray.size()) {
				result.add(firstArray.get(index1++));
			} else {
				if (firstArray.get(index1).compareTo(secondArray.get(index2)) >= 0) {
					// secondArray element is smaller
					result.add(secondArray.get(index2));
					bCount++;
					aCount = 0;
					index2++;

					// GALLOP
					while (bCount > minGallop || aCount > minGallop) {
						int index2start = index2;
						int indexToGallopTo = firstElementIndex(secondArray, firstArray.get(index1));
						// add presorted elements of secondArray to results
						while (index2 < indexToGallopTo) {
							result.add(secondArray.get(index2));
							index2++;
						}
						result.add(firstArray.get(index1++));
						bCount = index2 - index2start;

						if (index2 < secondArray.size()) {
							int index1start = index1;
							indexToGallopTo = firstElementIndex(firstArray, secondArray.get(index2));
							while (index1 < indexToGallopTo) {
								result.add(firstArray.get(index1));
								index1++;
							}
							result.add(secondArray.get(index2++));		//// WORKING HERE
							aCount = index1 - index1start;
						}
					}
				} else {
					// firstArray element is smaller
					result.add(firstArray.get(index1));
					aCount++;
					bCount = 0;
					index1++;

					// GALLOP
					while (aCount > minGallop || bCount > minGallop) {
						// TODO, fix logic error here
						//sometimes in very large, totally random arrays, an element is doubled
						// alternatively, lots of elements are doubled
						int index1start = index1;
						int indexToGallopTo = firstElementIndex(firstArray, secondArray.get(index2));
						// add presorted elements of secondArray to results
						while (index1 < indexToGallopTo) {
							result.add(firstArray.get(index1));
							index1++;
						}
						result.add(secondArray.get(index2));
						aCount = index1 - index1start;
						// END section with logic error

						if (index1 < firstArray.size()) {
							int index2start = index2;
							indexToGallopTo = firstElementIndex(secondArray, firstArray.get(index1));
							while (index2 < indexToGallopTo) {
								result.add(secondArray.get(index2));
								index2++;
							}
							result.add(firstArray.get(index1));
							bCount = index2 - index2start;
						}
					}
				}
			}
		}

		// add presorted tail of second array
		if (hiIndexForLastElement != -1 && hiIndexForLastElement != firstArray.size()) {
			for (int i = hiIndexForLastElement; i < firstArray.size(); i++) {
				//			for (int i = hiIndexForLastElement; i < secondArray.size(); i++) {
				//				result.add(secondArray.get(i));
				result.add(firstArray.get(i));
			}
		}
		return result;
	}

	private static <T extends Comparable<T>> int firstElementIndex(List<T> searchedArray, T element) {
		// linear search to find lowest position for element parameter in searchedArray
		// returns -1 if correct position is not found
		int result = -1;		/* guilty until proved innocent */
		int index = 0;
		while (index < searchedArray.size()) {		// we can turn this into a while loop
			if (element.compareTo(searchedArray.get(index)) < 0) {
				result = index;
				break;
			}
			index++;
		}
		if (index == searchedArray.size()) {
			result = index - 1;
		}
		return result;
	}

	private static <T extends Comparable<T>> int lastElementIndex(List<T> searchedArray, T element) {
		// linear search to find highest position for element parameter in searchedArray
		// returns -1 if correct position is not found
		int result = -1;		/* guilty until proved innocent */
		int index = searchedArray.size() - 1;
		while (index >= 0) {		// we can turn this into a while loop
			if (element.compareTo(searchedArray.get(index)) > 0) {
				result = index + 1;
				break;
			}
			index--;
		}
		if (index == 0) {
			result = index;
		}
		return result;
	}

	// binary insert based on binary search from ch14_1 project
	private static <T extends Comparable<T>> List<T> binaryInsert(List<T> list, T data) {
		// set index bounds of zero and last index in list
		int L = 0;
		int U = list.size();
		// get index between upper and lower
		int M = (L + U) / 2;

		// loop while bounds are not equal
		while(L < U){
			// compare requested data to list[M]
			int compareResult = list.get(M).compareTo(data);
			// if the data we're looking for is "less than" list[M]
			// bring upper limit down
			if(compareResult > 0)
				U = M;
			// if the data we're looking for is "greater than" lsit[M]
			// bring lower limit up
			else
				// do not set L to M, because we already checked M and know it is not what we're looking for
				L = M + 1;
			M = (L + U) / 2;
		}
		list.add(M, data);
		return list;
	}

	// BubbleSort from Chapter 16
	public static <E extends Comparable<E>> int bubbleSort(List<E> list) {
		// generic bubble sort for objects implementing the Comparable interface

		int iterationCount = 0;
		for (int counter1 = 0; counter1 < list.size(); counter1++) {
			boolean isSorted = true;

			for (int counter2 = 0; counter2 < list.size() - 1 - counter1; counter2++) {
				if (list.get(counter2).compareTo(list.get(counter2 + 1)) > 0) {
					E temp = list.get(counter2);
					list.set(counter2, list.get(counter2 + 1));
					list.set(counter2 + 1, temp);
					isSorted = false;		// we touched some elements, the array is not sorted yet
				}
				iterationCount++;		// count iterations of comparing
			}
			if(isSorted)		// if we didn't touch anything though the last inner for loop
				break;			// everything is sorted, break the outer for loop
		}
		return iterationCount;
	}

	// mergeSort from Chapter 16
	public static <T extends Comparable<T>> List<T> mergeSort(List<T> list) {

		// base case
		if (list.size() < 2)
			return list;

		// partition into two halves, and recurse

		// copy first half of the input list into ‘half1', and sort ‘halfl'
		List<T> half1 = new ArrayList<>();
		for (int i = 0; i < list.size() / 2; i++) {
			half1.add(list.get(i));
		}
		half1 = mergeSort(half1);

		// copy first hhLf of the input list into ‘half1', and sort ‘half1'

		List<T> half2 = new ArrayList<>();

		for (int i = list.size() / 2; i < list.size(); i++) {
			half2.add(list.get(i));
		}
		half2 = mergeSort(half2);

		// merge
		return mergeHalves(half1, half2);
	}

	public static <T extends Comparable<T>> List<T> mergeHalves(List<T> half1, List<T> half2) {

		List<T> result = new ArrayList<>();
		int index1 = 0, index2 = 0;

		// loop until both halves are depleted
		while (index1 < half1.size() || index2 < half2.size()) {

			if (index1 == half1.size())
				result.add(half2.get(index2++));

			else if (index2 == half2.size())
				result.add(half1.get(index1++));

			else {
				if (half1.get(index1).compareTo(half2.get(index2)) > 0)
					result.add(half2.get(index2++));
				else
					result.add(half1.get(index1++));
			}
		}
		return result;
	}
}
