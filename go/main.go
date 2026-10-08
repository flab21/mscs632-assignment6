// Multi-threaded Data Processing System (Go version)
// Usage: go run . [outputFile]
package main

import (
	"log"
	"os"
	"sync"
)

const (
	numWorkers = 4
	numTasks   = 20
)

func main() {
	log.SetFlags(log.Ltime | log.Lmicroseconds)
	log.SetOutput(os.Stdout)

	outFile := "go_results.txt"
	if len(os.Args) > 1 {
		outFile = os.Args[1]
	}

	q := NewTaskQueue(numTasks)
	store := NewResultStore()

	// tasks 7 and 15 get negative data on purpose to show error handling
	for i := 1; i <= numTasks; i++ {
		data := i * 3
		if i == 7 || i == 15 {
			data = -i
		}
		if err := q.AddTask(Task{ID: i, Data: data}); err != nil {
			log.Printf("[ERROR] [main] %v", err)
		}
	}
	log.Printf("[INFO ] [main] added %d tasks to the queue", numTasks)
	q.Close() // no more tasks coming, workers will exit when it drains

	var wg sync.WaitGroup
	for i := 1; i <= numWorkers; i++ {
		wg.Add(1)
		go worker(i, q, store, &wg)
	}
	wg.Wait() // block until every worker has called Done

	if err := store.WriteToFile(outFile); err != nil {
		log.Printf("[ERROR] [main] could not write results file: %v", err)
	} else {
		log.Printf("[INFO ] [main] results written to %s", outFile)
	}

	ok, failed, unique := store.Counts()
	log.Printf("[INFO ] [main] summary: %d succeeded, %d failed, %d/%d tasks accounted for",
		ok, failed, unique, numTasks)
	if unique == numTasks {
		log.Printf("[INFO ] [main] every task was handled exactly once")
	} else {
		log.Printf("[ERROR] [main] some tasks were missed!")
	}
}
