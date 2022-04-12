// ver 0.6
// added stuff to detect and reverse descending runs
// have correct binary insertion working
// probably other stuff


package timsort;

import java.util.ArrayList;
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
		random.setSeed(1638576577933l);		// constant seed for debug testing
//		for (int i = 0; i < 200; i++)
//			intList.add(random.nextInt(50));
	
		// debug for ascending
		for (int i = 1; i <= 200; i++)
			intList.add(200 - i);
		
//		// debug test for descending runs
//		for (int i = 20; i >= 1; i--)
//			intList.add(i);
//		for (int i = 0; i < 44; i++)
//			intList.add(i);

		// print out seed and list for debug testing
		System.out.println("seed: " + seed);

		// print unsorted list
		int i = 0;
		for (Integer element : intList) {
			System.out.print(element + ", ");
			i++;
			if (i % 20 == 0)
				System.out.println();
		}
		System.out.println("END original list");
		System.out.println();
		// end more debug stuff

		// commence TimSort
		sort(intList);

	}

	public static <T extends Comparable<T>> List<T> sort(List<T> array) {
			
		// generate min_run_size, based on original C implementation for Python
		int min_run_size = 0;
		int n = array.size();
		if (n < 64) {
			min_run_size = n;
		} else {
			if ( (n & (n - 1)) == 0 && n != 0) {
				// if array.size() is a power of 2
				min_run_size = 32;
			} else {
				// calculate min_run_size
				int rValue = 0;
				while (n >= 64) {
					rValue |= n & 1;
					n >>= 1;		// halve array size
				}
				// the object is to get n / result as close to a power of 2
				// with n = 100, min_run_size = 50
				min_run_size = rValue + n;
			}
		}

		// print min_run_size for debug
		System.out.println("min_run_size = " + min_run_size);

		Stack<List<T>> runStack = new Stack<>();
		List<T> sortedRunStack = new ArrayList<>();		// THIS MIGHT NEED TO BE A STACK AS WELL
														// we'll get to this during the final sort/merge
		

		// find natural runs
		List<T> currentRun = new ArrayList<>();
		currentRun.add(array.get(0));

		for(int i = 1; i < array.size(); i++) {
			T currentElement = array.get(i);
			T previousElement = array.get(i-1);
			boolean ascending = true;
			
//			if (i == array.size() - 1) {
//				// end search if we're at the end of the array
//				currentRun.add(array.get(i));
//				runStack.add(currentRun);
//				break;
//			}
			
			// see if this or previous element is smaller
			int compareResult = currentElement.compareTo(previousElement);
			
			// count length of run
			// TODO the ranges using runLength can be optimized somehow
			int runLength = 1;
			if (compareResult >= 0) {
				ascending = true;
				while (i != array.size() - 1 && array.get(i + runLength).compareTo(array.get(i - 1 + runLength)) >= 0 && i + runLength < array.size() - 1) {
					runLength++;
				}
			} else {
				ascending = false;
				while (i != array.size() - 1 && array.get(i + runLength).compareTo(array.get(i - 1 + runLength)) < 0 && i + runLength < array.size() - 1) {
					runLength++;
				}
			}
			
			// add run length to currentRun
			for ( ; i < runLength; i++) {		// CHANGE TO WHILE
				currentRun.add(array.get(i));
			}
//			i--;
			
			// reverse run if descending
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
				int endIndex = min_run_size - currentRun.size() + i;
				while (i < array.size() && i < endIndex) {
					// insert more elements sorted into currentRun
					// until we've hit end of array or we reach min_run_size
//					currentRun = insertion(currentRun, array.get(i));
					currentRun = binaryInsert(currentRun, array.get(i));
					i++;
				}
				i--;		// decrement i so outer for loop does not skip this element
				// add currentRun to runStack
				runStack.add(currentRun);
				// reset currentRun to an empty list, ending currentRun
				currentRun = new ArrayList<>();
			} else {
				// if currentRun.size() is larger than min_run_size, just add it to runStack
				runStack.add(currentRun);
				// reset currentRun to an empty list, ending currentRun 
				currentRun = new ArrayList<>();
				// add currentElement to currentRun, starting a new run
				currentRun.add(array.get(i));
			}


			// after currentRun is added to runStack, check for merge conditions
			int zSize = 0;
			int ySize = 0;
			int xSize = 0;
			// check runs for size and merge if necessary
			if (runStack.size() >= 3) {
				xSize = runStack.get(runStack.size() - 1).size();
				ySize = runStack.get(runStack.size() - 2).size();
				zSize = runStack.get(runStack.size() - 3).size();
				// TODO change to while loop so this is checked until it passes size conditions
				if (zSize < ySize + xSize || ySize < xSize) {
					List<T> mergedList = new ArrayList<>();
					List<T> x = runStack.pop();
					List<T> y = runStack.pop();
					List<T> z = runStack.pop();
					if (zSize < xSize) {
						// merge y with z
						mergedList.addAll(merge(y, z));
						// push merged
						runStack.push(mergedList);
						// push x
						runStack.push(x);
					} else {
						// merge y with x
						mergedList.addAll(merge(y, x));
						// push merged
						runStack.push(mergedList);
						// push z
						runStack.push(z);
					}
				}
			}
		}

		// print lists in stack for debug testing
		int j = 1;
		for (List<T> run : runStack) {
			int i = 0;
			System.out.println("\nRun Length: " + run.size());
			for (T element : run) {
				System.out.print(element + ", ");
				i++;
				if (i % 20 == 0)
					System.out.println();
			}
			System.out.println("\nEND Run Num: " + j);
			System.out.println();
			j++;
		}
		// end debug testing


		// END RUN FINDING


		for (List<T> run : runStack) {
			sortedRunStack.addAll(insertionSort(run));
		}

		// debug thing
		int k = 1;
		System.out.println("\nsortedRunStack Length: " + sortedRunStack.size());
		for (T element : sortedRunStack) {
			//			int i = 0;
			//			for (T element : run) {
			System.out.print(element + ", ");
			//				i++;
			if (k % 20 == 0)
				System.out.println();
			//			}
			//			System.out.println("END sortedlist Num: " + k + " from runs");
			//			System.out.println();
			k++;
		}
		// eng debug thing

		List<T> sortedList = new ArrayList<>();
		//		for (List<T> run : sortedRunStack) {
		//			sortedList = mergeSort(sortedList, run);
		//		}

		return sortedList;
	}



	private static <T extends Comparable<T>> List<T> merge(List<T> firstArray, List<T> secondArray) {
		
		// TODO finish gallop mode
		int minGallop = 7;
		
		List<T> result = new ArrayList<>();
		int index1 = 0;
		int index2 = 0;

		//		// find which array has highest 1st element, and lowest last element
		//		List<T> firstArray = new ArrayList<>();
		//		List<T> secondArray = new ArrayList<>();

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
		

		// end loop at hiIndexForLastElement so we can add presorted tail of second array
		while (index1 < firstArray.size() || index2 < hiIndexForLastElement) {
			if (index1 == firstArray.size()) {
				result.add(secondArray.get(index2++));
			} else if (index2 == hiIndexForLastElement) {
				result.add(firstArray.get(index1++));
			} else {
				T element1 = firstArray.get(index1);
				T elememt2 = secondArray.get(index2);
				if (element1.compareTo(elememt2) > 0) {
					result.add(elememt2);
					// add 1 to acount
					// set bcount to 0
					index2++;
				} else {
					result.add(element1);
					// add 1 to bcount
					// set acount to 0
					index1++;
					// if (bcount > minGallop)
						// break;		// break out of while if gallop should start
				}
			}
		}
		
		/* minGallop++;
		// while (acount > minGallop || bcount > minGallop)
		 * 
		 * 
		 */
		
		// add presorted tail of second array
		if (hiIndexForLastElement != -1) {
			for (int i = hiIndexForLastElement; i < firstArray.size(); i++) {
//			for (int i = hiIndexForLastElement; i < secondArray.size(); i++) {
//				result.add(secondArray.get(i));
				result.add(firstArray.get(i));
			}
		}
		

		return result;
	}

	private static <T extends Comparable<T>> int firstElementIndex(List<T> searchedArray, T element) {
		// binary search to find lowest position for element parameter in searchedArray
		// returns -1 if correct position is not found
		int result = -1;		/* guilty until proved innocent */
		for (int i = 0; i < searchedArray.size(); i++) {		// we can turn this into a while loop
			if (element.compareTo(searchedArray.get(i)) < 0) {
				result = i;
				break;
			}

		}
		return result;
	}

	private static <T extends Comparable<T>> int lastElementIndex(List<T> searchedArray, T element) {
		// binary search to find highest position for element parameter in searchedArray
		// returns -1 if correct position is not found
		int result = -1;		/* guilty until proved innocent */
		for (int i = searchedArray.size() - 1; i >= 0; i--) {		// we can turn this into a while loop
			if (element.compareTo(searchedArray.get(i)) > 0) {
				result = i;
				break;
			}
		}

		return result;
	}

	private static <T extends Comparable<T>> List<T> insertionSort(List<T> array) {
		for (T element : array) {

		}
		//		// TODO Auto-generated method stub
		//		List<T> sortedList = new ArrayList<>();
		//		
		//		for (int i = 1; i < array.size(); i++) {
		//			T elementI = array.get(i);
		//
		//			int j = i - 1;
		//			T elementJ = array.get(j);
		//			for (; j >= 0 && elementJ.compareTo(elementI) > 0; j--) {
		//				array.set(j + 1, elementJ);
		//			}
		//			array.set(j + 1, elementI);
		//		}
		//		
		//		return array;
		return array;
	}

	private static <T extends Comparable<T>> List<T> insertion(List<T> array, T element) {
		//		for (T element : array) {
		//			
		//		}
		// TODO Auto-generated method stub
		List<T> sortedList = new ArrayList<>();

		for (int i = 0; i < array.size(); i++) {
			if (element.compareTo(array.get(i)) < 0) {
				sortedList.add(element);
				for (; i < array.size(); i++) {
					sortedList.add(array.get(i));
				}
				//				sortedList.addAll(i + 1, sortedList);
				return sortedList;
			}
			sortedList.add(array.get(i));
		}
		sortedList.add(element);

		//		return array;
		return sortedList;
	}
	
	// binary insert based on binary search from ch14_1 project
	private static <T extends Comparable<T>> List<T> binaryInsert(List<T> list, T data) {
		// TODO better comments
		
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
			else // if(compareResult < 0)
				// do not set L to M, because we already checked M and know it is not what we're looking for
				L = M + 1;
//			else {
//				// if we reach here, compareResult must equal 0 and we've found what we're looking for
//				list.add(M, data);
//				return list;
//			}
			M = (L + U) / 2;
		}
		// if we leave loop without returning M, item we're looking for is not found, return -1
		list.add(M, data);
		return list;
//		return list;		// TODO, check if getting here wihtou adding data ruins anytthing, maybe change to null
	}

}
