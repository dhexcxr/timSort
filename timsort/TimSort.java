/***************************************

M. Corey Glass
12.03.21
COP2805 70079, Fall2021
Professor Ronald Villmow
Project Name: cop2805wk15
File Name: TimSort.java
Class: TimSort

implement an algorithm, I chose TimSort

Implementation based on the following:
Design Doc by Tim Peters -
https://github.com/python/cpython/blob/main/Objects/listsort.txt

Additional interpretation and insight from Wikipedia -
https://en.wikipedia.org/wiki/Timsort

And analysis from Skerritt.blogg -
https://skerritt.blog/timsort/

 ***************************************/

// ver 1.0debug
// added stuff to detect and reverse descending runs
// have correct binary insertion working
// probably other stuff
// most bugs fixed (famous last words)
// and GALLOP MODE

// this is no longer true, major logic error in pushing merged runs back onto stack made sort 50-100x slower
		// this is slower than a standard merge sort, I think because my merge does not merge in place
		// adding that (doing less memory copying) should speed this up


// the only major feature I don't have is adjusting minGallop to make galloping easier or harder
// depending on previous gallop performance

// runs faster if firstArray section of second gallop is commented out ???


package timsort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Stack;

public class TimSort {

	public static void main(String[] args) {

		long bubSortTime = 0;
		long mergeSortTime = 0;
		long myTimSortTime = 0;
		long builtInSortTime = 0;
		
		
		// fill with random Integer objects
		long seed = java.lang.System.currentTimeMillis();
		Random random = new Random();
		random.setSeed(1639020886145l);

		for (int f = 0; f <= 100; f += 10) {
			// generate list to test TimSort.sort
			List<Integer> intList = new ArrayList<>();
			
			int count = random.nextInt(550);
//			System.out.println("count: " + count);
			
			// generate an array between 1000-1500 long, filled with ints from 0-1000
//			for (int i = 0; i < count + 1000 * 80; i++)
//				intList.add(random.nextInt(1000));
			
			for (int i = 0; i < 80000; i++)
				intList.add(random.nextInt(1000));

			// old bubble sort
			List<Integer> bubbleList = new ArrayList<>(intList);
			long bubSortStart = System.currentTimeMillis();
//			bubbleSort(bubbleList);
			long bubSortEnd = System.currentTimeMillis();
			
			// standard merge sort
			List<Integer> mergeList = new ArrayList<>(intList);
			long mergeSortStart = System.currentTimeMillis();
			mergeSort(mergeList);
			long mergeSortEnd = System.currentTimeMillis();

			// commence TimSort
			List<Integer> timList = new ArrayList<>(intList);
			long timSortStart = System.currentTimeMillis();
			List<Integer> result = sort(timList);
			long timSortEnd = System.currentTimeMillis();

			// built in sort
			List<Integer> builtInList = new ArrayList<>(intList);
			long collectionSortStart = System.currentTimeMillis();
			Collections.sort(builtInList);
			long collectionSortEnd = System.currentTimeMillis();

			// check that my TimSort results are the same built-in sort results
			Map<Integer, Integer> wrong = new LinkedHashMap<>();
			
			boolean error = false;
			for (int j = 0; j < builtInList.size(); j++) {
				if (builtInList.get(j).compareTo(result.get(j)) != 0) {
					wrong.put(j, result.get(j));
					System.out.print("Error at element: " + j);
					System.out.println(", " + builtInList.get(j) + " != " + result.get(j));
					error = true;
				}
			}
			if (error) {
				System.out.println("\nSomething went wrong...");
			} else {
				System.out.println("\nTimSort worked!!");
			}

			// print results
			System.out.println("seed: " + seed);
			System.out.println("builtInList.size(): " + builtInList.size());
			System.out.println("result.size(): " + result.size());
			System.out.println("wrong.size(): " + wrong.size() + "\n");
			
			System.out.println("Sort results from working on a list with " + intList.size() + " elements in it:");
			System.out.println("Bubble Sort took " + (bubSortEnd - bubSortStart) + " ms to sort the array.");
			System.out.println("Merge Sort took " + (mergeSortEnd - mergeSortStart) + " ms to sort the array.");
			System.out.println("My TimSort took " + (timSortEnd - timSortStart) + " ms to sort the array.");
			System.out.println("The built in TimSort took " + (collectionSortEnd - collectionSortStart) + " ms to sort the array");
			
			bubSortTime += bubSortEnd - bubSortStart;
			mergeSortTime += mergeSortEnd - mergeSortStart;
			myTimSortTime += timSortEnd - timSortStart;
			builtInSortTime += collectionSortEnd - collectionSortStart;
		}
		
		System.out.println("Bubbke Sort average over 11 runs = " + (bubSortTime / 11));
		System.out.println("Merge Sort average over 11 runs = " + (mergeSortTime / 11));
		System.out.println("My TimSort average over 11 runs = " + (myTimSortTime / 11));
		System.out.println("Built in TimSort average over 11 runs = " + (builtInSortTime / 11));
	}

	public static <T extends Comparable<T>> List<T> sort(List<T> array) {

		// generate min_run_size, based on original C implementation for Python
		int min_run_size = 0;
		int arraySize = array.size();
		if (arraySize < 64) {
			min_run_size = arraySize;
		} else {
//			if ((arraySize & (arraySize - 1)) == 0 && arraySize != 0) {
//				// if array.size() is a power of 2
//				min_run_size = 32;
//			} else {
				// calculate min_run_size
				int rValue = 0;
				while (arraySize >= 64) {
					rValue |= arraySize & 1;
					arraySize >>= 1;		// halve array size
				}
				// the goal is to get n / result as close to a power of 2
					// i.e. with n = 100, min_run_size = 50
				min_run_size = rValue + arraySize;
//			}
		}

		Stack<List<T>> runStack = new Stack<>();

//		List<T> currentRun = new ArrayList<>(min_run_size * 2);
//		currentRun.add(array.get(0));

		// find natural runs
		for(int currentIndex = 1; currentIndex < array.size(); currentIndex++) {
			boolean ascending = true;
			
			// start new run
			List<T> currentRun = new ArrayList<>(min_run_size * 2);
			currentRun.add(array.get(currentIndex - 1));

			// decide if we're in an ascending or descending run
			int compareResult = array.get(currentIndex).compareTo(array.get(currentIndex - 1));
			if (compareResult >= 0) {
				ascending = true;
				while (currentIndex < array.size() && array.get(currentIndex).compareTo(array.get(currentIndex - 1)) >= 0) {
					currentRun.add(array.get(currentIndex++));
				}
			} else {
				ascending = false;
				while (currentIndex < array.size()  && array.get(currentIndex).compareTo(array.get(currentIndex - 1)) < 0) {
					currentRun.add(array.get(currentIndex++));
				}
			}

			// reverse run if that was a descending run
			if (ascending == false && currentRun.size() > 1) {
				for (int j = 0, k = currentRun.size() - 1; j <= k; j++, k-- ) {
					T temp = currentRun.get(j);
					currentRun.set(j, currentRun.get(k));
					currentRun.set(k, temp);
				}
			}

			// bring currentRun size up to min_run_size
			if (currentRun.size() < min_run_size) {				
				// calculate endIndex needed to fill run to min_run_size
				int endIndex = min_run_size - currentRun.size() + currentIndex;
				while (currentIndex < array.size() && currentIndex < endIndex) {				
					// insert more elements sorted into currentRun
						// until we've hit end of array or we reach min_run_size
					binaryInsert(currentRun, array.get(currentIndex));
					currentIndex++;
				}
				// add currentRun to runStack
				runStack.add(currentRun);
			} else {
				// if currentRun.size() is larger than min_run_size, just add it to runStack
				runStack.add(currentRun);
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
						runStack.push(mergedList);
						// push merged
						runStack.push(z);
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
		
		// TODO make this neater
		List<T> sortedResults = runStack.pop();

		for (int i = runStack.size() - 1; i >= 0; i--) {
			runStack.add(merge(sortedResults, runStack.get(i)));
			if (runStack.size() > 0) {
				sortedResults = runStack.pop();
			} 
		}
		return sortedResults;
	}

	// TODO, sort in place, into firstArray/y, use .clone() to create shallow copy of smaller ArrayList, and .set(index, element)
	private static <T extends Comparable<T>> List<T> merge(List<T> firstArray, List<T> secondArray) {
		int minGallop = 7;

		List<T> result = new ArrayList<>(firstArray.size() + secondArray.size() + 1);
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

		int firstArrayWinCount = 0;
		int secondArrayWinCount = 0;

		// end loop at hiIndexForLastElement so we can add presorted tail of firstArray
		while ((index1 < firstArray.size() && index1 < hiIndexForLastElement) || index2 < secondArray.size()) {
			if (index1 == firstArray.size() || index1 == hiIndexForLastElement) {
				result.add(secondArray.get(index2++));
			} else if (index2 == secondArray.size()) {
				result.add(firstArray.get(index1++));
			} else {
				if (firstArray.get(index1).compareTo(secondArray.get(index2)) > 0) {
					// secondArray element is smaller
					result.add(secondArray.get(index2));
					secondArrayWinCount++;
					firstArrayWinCount = 0;
					index2++;

					// GALLOP
					while (secondArrayWinCount >= minGallop || firstArrayWinCount >= minGallop) {
						int indexToGallopTo = 0;
						if (index1 < firstArray.size()) {
							int index2start = index2;
							indexToGallopTo = modifiedBinarySearch(secondArray, firstArray.get(index1));
							// add presorted elements of secondArray to results
							while (index2 < indexToGallopTo) {
								result.add(secondArray.get(index2));
								index2++;
							}
							result.add(firstArray.get(index1++));
							secondArrayWinCount = index2 - index2start;
						} else {
							secondArrayWinCount = 0;
						}

						if (index2 < secondArray.size()) {
							int index1start = index1;
							indexToGallopTo = modifiedBinarySearch(firstArray, secondArray.get(index2));
							while (index1 < indexToGallopTo) {
								result.add(firstArray.get(index1));
								index1++;
							}
							result.add(secondArray.get(index2++));
							firstArrayWinCount = index1 - index1start;
						} else {
							firstArrayWinCount = 0;
						}
					}
				} else {
					// firstArray element is smaller
					result.add(firstArray.get(index1));
					firstArrayWinCount++;
					secondArrayWinCount = 0;
					index1++;

					// GALLOP
						// for some reason the sort is ~10% faster if we skip this gallop section
//					while (firstArrayWinCount >= minGallop || secondArrayWinCount >= minGallop) {
					while (secondArrayWinCount > minGallop) {
//						int indexToGallopTo = 0;
//						if (index2 < secondArray.size()) {
//							int index1start = index1;
//							indexToGallopTo = modifiedBinarySearch(firstArray, secondArray.get(index2));
//							// add presorted elements of firstArray to results
//							while (index1 < indexToGallopTo) {
//								result.add(firstArray.get(index1));
//								index1++;
//							}
//							result.add(secondArray.get(index2++));
//							firstArrayWinCount = index1 - index1start;
//						} else {
//							firstArrayWinCount = 0;
//						}

						if (index1 < firstArray.size()) {
							int index2start = index2;
							int indexToGallopTo = modifiedBinarySearch(secondArray, firstArray.get(index1));
							while (index2 < indexToGallopTo) {
								result.add(secondArray.get(index2));
								index2++;
							}
							result.add(firstArray.get(index1++));
							secondArrayWinCount = index2 - index2start;
						} else {
							secondArrayWinCount = 0;
						}
					}
				}
			}
		}

		// add presorted tail of first array
		if (hiIndexForLastElement != -1 && hiIndexForLastElement != firstArray.size()) {
			for (int i = hiIndexForLastElement > index1 ? hiIndexForLastElement : index1; i < firstArray.size(); i++) {
				result.add(firstArray.get(i));
			}
		}
		return result;
	}


	private static <T extends Comparable<T>> int modifiedBinarySearch(List<T> searchedArray, T element) {
		// modified binary search to find lowest position for element parameter in searchedArray
		// returns -1 if correct position is not found
		int result = -1;		/* guilty until proved innocent */
		int index = 0;
		int prevIndex = 0;
		while (index < searchedArray.size()) {
			if (element.compareTo(searchedArray.get(index)) < 0) {
				// element belongs before index, and after prevIndex
				result = firstElementIndex(searchedArray, prevIndex, index, element);
				break;
			}
			prevIndex = index;
			index = index * 2 + 1;
		}
		if (index >= searchedArray.size()) {
			result = firstElementIndex(searchedArray, prevIndex, searchedArray.size(), element);
		}
		// find element position between prevIndex and index
		return result;
	}

	private static <T extends Comparable<T>> int firstElementIndex(List<T> searchedArray, T element) {
		return firstElementIndex(searchedArray, 0, searchedArray.size(),  element);
	}

	private static <T extends Comparable<T>> int firstElementIndex(List<T> searchedArray, int startIndex, int endIndex, T element) {
		// linear search to find lowest position for element parameter in searchedArray
		// returns -1 if correct position is not found
		int result = -1;		/* guilty until proved innocent */
		int index = startIndex;
		while (index < endIndex) {
			if (element.compareTo(searchedArray.get(index)) < 0) {
				result = index;
				break;
			}
			index++;
		}
		if (index == endIndex) {
			result = index;
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
	private static <T extends Comparable<T>> void binaryInsert(List<T> list, T data) {
		int loIndex = 0;
		int hiIndex = list.size();
		int midIndex = (loIndex + hiIndex) / 2;

		while(loIndex < hiIndex){
			int compareResult = list.get(midIndex).compareTo(data);
			if(compareResult > 0) {
				hiIndex = midIndex;
			} else {
				loIndex = midIndex + 1;
			}
			midIndex = (loIndex + hiIndex) / 2;
		}
		list.add(midIndex, data);
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

			// copy first half of the input list into �half1', and sort �halfl'
			List<T> half1 = new ArrayList<>();
			for (int i = 0; i < list.size() / 2; i++) {
				half1.add(list.get(i));
			}
			half1 = mergeSort(half1);

			// copy first hhLf of the input list into �half1', and sort �half1'

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
